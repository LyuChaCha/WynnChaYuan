/**
 * 披風的預覽圖：跟網頁披風一覽的卡片一樣，只露出披風的背面（範本 22×17 格裡左上角那塊 10×16 格），
 * 像素放大、墊上深綠的底。Discord 機器人的 /cape 用（使用者 2026-10-16：「像網頁當中一樣預覽的樣子」）。
 *
 * Worker 沒有 canvas，所以 PNG 與 GIF 的解碼、編碼都在這裡自己做（不靠套件）：
 *   PNG：zlib 用 DecompressionStream／CompressionStream，支援 8／16 位元與調色盤（1–8 位元），不支援交錯。
 *   GIF：LZW 解碼、照每一格的處置方式（disposal）疊成整張，再逐格切、放大、重新編碼（每格自己的調色盤）。
 */

/** 卡片底色（網頁 .stage 的漸層 #1b2a21 → #131b16）。GIF 只能用一個顏色，取中間。 */
const TOP = [0x1b, 0x2a, 0x21];
const BOTTOM = [0x13, 0x1b, 0x16];
const FLAT = [0x17, 0x22, 0x1b];
const EDGE = [0x0b, 0x0f, 0x0d];      // 圖的外框（網頁 .face 的陰影那一圈）

// ---------------------------------------------------------------- 共用

async function inflate(bytes) {
  const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream('deflate'));
  return new Uint8Array(await new Response(stream).arrayBuffer());
}

async function deflate(bytes) {
  const stream = new Blob([bytes]).stream().pipeThrough(new CompressionStream('deflate'));
  return new Uint8Array(await new Response(stream).arrayBuffer());
}

const CRC_TABLE = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c >>> 0;
  }
  return t;
})();

function crc32(bytes, start, end) {
  let c = 0xffffffff;
  for (let i = start; i < end; i++) c = CRC_TABLE[(c ^ bytes[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

// ---------------------------------------------------------------- PNG

/** PNG → {width, height, rgba}。不支援的（交錯）回 null。 */
export async function decodePng(bytes) {
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  let pos = 8;
  let width = 0, height = 0, depth = 0, type = 0, interlace = 0;
  let palette = null, alpha = null;
  const idat = [];
  while (pos + 8 <= bytes.length) {
    const len = view.getUint32(pos);
    const name = String.fromCharCode(bytes[pos + 4], bytes[pos + 5], bytes[pos + 6], bytes[pos + 7]);
    const data = bytes.subarray(pos + 8, pos + 8 + len);
    if (name === 'IHDR') {
      width = view.getUint32(pos + 8);
      height = view.getUint32(pos + 12);
      depth = data[8];
      type = data[9];
      interlace = data[12];
    } else if (name === 'PLTE') palette = data;
    else if (name === 'tRNS') alpha = data;
    else if (name === 'IDAT') idat.push(data);
    else if (name === 'IEND') break;
    pos += 12 + len;
  }
  if (!width || !height || interlace) return null;
  const total = idat.reduce((n, d) => n + d.length, 0);
  const joined = new Uint8Array(total);
  let at = 0;
  for (const d of idat) {
    joined.set(d, at);
    at += d.length;
  }
  const raw = await inflate(joined);
  const channels = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[type];
  if (!channels) return null;
  const bpp = Math.max(1, (channels * depth) >> 3);          // 濾波器用的位元組距離
  const stride = Math.ceil((width * channels * depth) / 8);
  const rows = new Uint8Array(stride * height);
  let prev = new Uint8Array(stride);
  for (let y = 0; y < height; y++) {
    const filter = raw[y * (stride + 1)];
    const line = raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1));
    const out = rows.subarray(y * stride, (y + 1) * stride);
    for (let i = 0; i < stride; i++) {
      const a = i >= bpp ? out[i - bpp] : 0;
      const b = prev[i];
      const c = i >= bpp ? prev[i - bpp] : 0;
      let v = line[i];
      if (filter === 1) v += a;
      else if (filter === 2) v += b;
      else if (filter === 3) v += (a + b) >> 1;
      else if (filter === 4) {
        const p = a + b - c;
        const pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
        v += pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
      }
      out[i] = v & 0xff;
    }
    prev = out;
  }
  const rgba = new Uint8Array(width * height * 4);
  const sample = (y, i) => {
    // 第 i 個樣本（照位元深度取）；16 位元取高位那個位元組
    if (depth === 8) return rows[y * stride + i];
    if (depth === 16) return rows[y * stride + i * 2];
    const bit = i * depth;
    const byte = rows[y * stride + (bit >> 3)];
    const v = (byte >> (8 - depth - (bit & 7))) & ((1 << depth) - 1);
    return type === 3 ? v : Math.round((v * 255) / ((1 << depth) - 1));
  };
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const o = (y * width + x) * 4;
      if (type === 3) {
        const k = sample(y, x);
        rgba[o] = palette ? palette[k * 3] : 0;
        rgba[o + 1] = palette ? palette[k * 3 + 1] : 0;
        rgba[o + 2] = palette ? palette[k * 3 + 2] : 0;
        rgba[o + 3] = alpha && k < alpha.length ? alpha[k] : 255;
      } else if (type === 0 || type === 4) {
        const g = sample(y, x * channels);
        rgba[o] = rgba[o + 1] = rgba[o + 2] = g;
        rgba[o + 3] = type === 4 ? sample(y, x * channels + 1) : 255;
      } else {
        rgba[o] = sample(y, x * channels);
        rgba[o + 1] = sample(y, x * channels + 1);
        rgba[o + 2] = sample(y, x * channels + 2);
        rgba[o + 3] = type === 6 ? sample(y, x * channels + 3) : 255;
      }
    }
  }
  return { width, height, rgba };
}

/** RGB → PNG（不透明，每列不濾波；預覽圖是大塊同色，壓得很小）。 */
export async function encodePng(width, height, rgb) {
  const raw = new Uint8Array((width * 3 + 1) * height);
  for (let y = 0; y < height; y++) raw.set(rgb.subarray(y * width * 3, (y + 1) * width * 3), y * (width * 3 + 1) + 1);
  const z = await deflate(raw);
  const chunk = (name, data) => {
    const out = new Uint8Array(12 + data.length);
    const v = new DataView(out.buffer);
    v.setUint32(0, data.length);
    for (let i = 0; i < 4; i++) out[4 + i] = name.charCodeAt(i);
    out.set(data, 8);
    v.setUint32(8 + data.length, crc32(out, 4, 8 + data.length));
    return out;
  };
  const head = new Uint8Array(13);
  const hv = new DataView(head.buffer);
  hv.setUint32(0, width);
  hv.setUint32(4, height);
  head[8] = 8;
  head[9] = 2;
  const parts = [new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', head), chunk('IDAT', z), chunk('IEND', new Uint8Array(0))];
  const out = new Uint8Array(parts.reduce((n, p) => n + p.length, 0));
  let at = 0;
  for (const p of parts) {
    out.set(p, at);
    at += p.length;
  }
  return out;
}

// ---------------------------------------------------------------- GIF

function lzwDecode(data, minSize, pixels) {
  const out = new Uint8Array(pixels);
  const clear = 1 << minSize;
  const end = clear + 1;
  const prefix = new Int16Array(4096);
  const suffix = new Uint8Array(4096);
  const first = new Uint8Array(4096);
  const length = new Uint16Array(4096);
  for (let i = 0; i < clear; i++) {
    prefix[i] = -1;
    suffix[i] = i;
    first[i] = i;
    length[i] = 1;
  }
  let size = minSize + 1;
  let next = end + 1;
  let prev = -1;
  let o = 0;
  let acc = 0, bits = 0, i = 0;
  const emit = (code) => {
    // 從字串的尾巴往回填
    let c = code;
    for (let k = length[code] - 1; k >= 0; k--) {
      if (o + k < pixels) out[o + k] = suffix[c];
      c = prefix[c];
    }
    o += length[code];
  };
  while (o < pixels) {
    while (bits < size && i < data.length) {
      acc |= data[i++] << bits;
      bits += 8;
    }
    if (bits < size) break;
    const code = acc & ((1 << size) - 1);
    acc >>>= size;
    bits -= size;
    if (code === clear) {
      size = minSize + 1;
      next = end + 1;
      prev = -1;
      continue;
    }
    if (code === end) break;
    if (prev === -1) {
      if (code >= clear) break;
      emit(code);
      prev = code;
      continue;
    }
    if (code > next) break;                    // 壞掉的資料
    const known = code < next;
    if (next < 4096) {
      prefix[next] = prev;
      suffix[next] = known ? first[code] : first[prev];
      first[next] = first[prev];
      length[next] = length[prev] + 1;
      next++;
      if (next >= 1 << size && size < 12) size++;
    }
    emit(code);
    prev = code;
  }
  return out;
}

/** GIF → {width, height, frames: [{rgba, delay}]}，每一格都是疊好的整張。 */
export function decodeGif(bytes) {
  if (bytes.length < 13) return null;
  const width = bytes[6] | (bytes[7] << 8);
  const height = bytes[8] | (bytes[9] << 8);
  const flags = bytes[10];
  let pos = 13;
  let global = null;
  if (flags & 0x80) {
    const n = 3 * (1 << ((flags & 7) + 1));
    global = bytes.subarray(pos, pos + n);
    pos += n;
  }
  const canvas = new Uint8Array(width * height * 4);
  const frames = [];
  let gce = { disposal: 0, transparent: -1, delay: 10 };
  const blocks = () => {
    const parts = [];
    while (pos < bytes.length) {
      const n = bytes[pos++];
      if (n === 0) break;
      parts.push(bytes.subarray(pos, pos + n));
      pos += n;
    }
    const total = parts.reduce((s, p) => s + p.length, 0);
    const out = new Uint8Array(total);
    let at = 0;
    for (const p of parts) {
      out.set(p, at);
      at += p.length;
    }
    return out;
  };
  while (pos < bytes.length) {
    const block = bytes[pos++];
    if (block === 0x3b) break;
    if (block === 0x21) {
      const label = bytes[pos++];
      const data = blocks();
      if (label === 0xf9 && data.length >= 4) {
        gce = { disposal: (data[0] >> 2) & 7, transparent: data[0] & 1 ? data[3] : -1, delay: data[1] | (data[2] << 8) };
      }
      continue;
    }
    if (block !== 0x2c) return frames.length ? { width, height, frames } : null;
    const fx = bytes[pos] | (bytes[pos + 1] << 8);
    const fy = bytes[pos + 2] | (bytes[pos + 3] << 8);
    const fw = bytes[pos + 4] | (bytes[pos + 5] << 8);
    const fh = bytes[pos + 6] | (bytes[pos + 7] << 8);
    const local = bytes[pos + 8];
    pos += 9;
    let palette = global;
    if (local & 0x80) {
      const n = 3 * (1 << ((local & 7) + 1));
      palette = bytes.subarray(pos, pos + n);
      pos += n;
    }
    const minSize = bytes[pos++];
    const indices = lzwDecode(blocks(), minSize, fw * fh);
    const before = gce.disposal === 3 ? canvas.slice() : null;
    // 交錯的列順序
    const order = [];
    if (local & 0x40) {
      for (const [start, step] of [[0, 8], [4, 8], [2, 4], [1, 2]]) for (let y = start; y < fh; y += step) order.push(y);
    } else for (let y = 0; y < fh; y++) order.push(y);
    for (let r = 0; r < fh; r++) {
      const y = fy + order[r];
      if (y >= height) continue;
      for (let x = 0; x < fw; x++) {
        const k = indices[r * fw + x];
        if (k === gce.transparent || !palette) continue;
        const cx = fx + x;
        if (cx >= width) continue;
        const o = (y * width + cx) * 4;
        canvas[o] = palette[k * 3];
        canvas[o + 1] = palette[k * 3 + 1];
        canvas[o + 2] = palette[k * 3 + 2];
        canvas[o + 3] = 255;
      }
    }
    frames.push({ rgba: canvas.slice(), delay: gce.delay });
    if (gce.disposal === 2) {
      for (let y = fy; y < Math.min(height, fy + fh); y++) canvas.fill(0, (y * width + fx) * 4, (y * width + Math.min(width, fx + fw)) * 4);
    } else if (gce.disposal === 3 && before) canvas.set(before);
    gce = { disposal: 0, transparent: -1, delay: 10 };
  }
  return frames.length ? { width, height, frames } : null;
}

function lzwEncode(indices, minSize) {
  const out = [];
  let acc = 0, bits = 0;
  const clear = 1 << minSize;
  const end = clear + 1;
  let size = minSize + 1;
  let next = end + 1;
  let dict = new Map();
  const emit = (code) => {
    acc |= code << bits;
    bits += size;
    while (bits >= 8) {
      out.push(acc & 0xff);
      acc >>>= 8;
      bits -= 8;
    }
  };
  emit(clear);
  let cur = indices[0];
  for (let i = 1; i < indices.length; i++) {
    const k = indices[i];
    const key = cur * 256 + k;
    const hit = dict.get(key);
    if (hit !== undefined) {
      cur = hit;
      continue;
    }
    emit(cur);
    if (next < 4096) {
      dict.set(key, next++);
      if (next > 1 << size && size < 12) size++;
    } else {
      emit(clear);
      dict = new Map();
      size = minSize + 1;
      next = end + 1;
    }
    cur = k;
  }
  emit(cur);
  emit(end);
  if (bits > 0) out.push(acc & 0xff);
  return out;
}

/** 一串 RGB 的格子 → GIF（每格自己的調色盤，最多 256 色；超過的照最近的顏色併）。 */
export function encodeGif(width, height, frames, loop = true) {
  const out = [];
  const push = (...v) => out.push(...v);
  const word = (v) => push(v & 0xff, (v >> 8) & 0xff);
  push(...'GIF89a'.split('').map((c) => c.charCodeAt(0)));
  word(width);
  word(height);
  push(0, 0, 0);
  if (loop) push(0x21, 0xff, 11, ...'NETSCAPE2.0'.split('').map((c) => c.charCodeAt(0)), 3, 1, 0, 0, 0);
  for (const { rgb, delay } of frames) {
    const colours = new Map();
    const list = [];
    const indices = new Uint8Array(width * height);
    for (let p = 0; p < width * height; p++) {
      const key = (rgb[p * 3] << 16) | (rgb[p * 3 + 1] << 8) | rgb[p * 3 + 2];
      let k = colours.get(key);
      if (k === undefined) {
        if (list.length < 256) {
          k = list.length;
          list.push(key);
        } else {
          // 超過 256 色：拿最接近的那一個
          let best = 0, dist = Infinity;
          for (let j = 0; j < list.length; j++) {
            const c = list[j];
            const d = ((c >> 16) - rgb[p * 3]) ** 2 + (((c >> 8) & 0xff) - rgb[p * 3 + 1]) ** 2 + ((c & 0xff) - rgb[p * 3 + 2]) ** 2;
            if (d < dist) {
              dist = d;
              best = j;
            }
          }
          k = best;
        }
        colours.set(key, k);
      }
      indices[p] = k;
    }
    let bitsPer = 1;
    while (1 << bitsPer < list.length) bitsPer++;
    push(0x21, 0xf9, 4, 0, delay & 0xff, (delay >> 8) & 0xff, 0, 0);
    push(0x2c);
    word(0);
    word(0);
    word(width);
    word(height);
    push(0x80 | (bitsPer - 1));
    for (let j = 0; j < 1 << bitsPer; j++) {
      const c = list[j] || 0;
      push(c >> 16, (c >> 8) & 0xff, c & 0xff);
    }
    const minSize = Math.max(2, bitsPer);
    push(minSize);
    const data = lzwEncode(indices, minSize);
    for (let i = 0; i < data.length; i += 255) {
      const piece = data.slice(i, i + 255);
      push(piece.length, ...piece);
    }
    push(0);
  }
  push(0x3b);
  return new Uint8Array(out);
}

// ---------------------------------------------------------------- 切出背面、放大、墊底

/** 預覽圖的版面：背面多大、放大幾倍、整張多大。 */
export function layout(srcWidth, srcHeight, maxFace) {
  const cell = srcWidth / 22;                 // 範本是 22 格寬
  const faceW = 10 * cell, faceH = 16 * cell;
  // 背面放大到 320 像素高左右、最高 maxFace；小張的像素圖用整數倍，像素才方正
  const target = Math.min(maxFace, Math.max(320, faceH));
  const scale = faceH * 2 <= target ? Math.floor(target / faceH) : target / faceH;
  const w = Math.max(1, Math.round(faceW * scale)), h = Math.max(1, Math.round(faceH * scale));
  const padX = Math.round(w * 0.32), padY = Math.round(h * 0.1);
  return { cell, w, h, padX, padY, width: w + padX * 2, height: h + padY * 2 };
}

function paint(rgba, srcWidth, srcHeight, box, gradient) {
  const { cell, w, h, padX, padY, width, height } = box;
  const rgb = new Uint8Array(width * height * 3);
  for (let y = 0; y < height; y++) {
    const t = height > 1 ? y / (height - 1) : 0;
    const bg = gradient ? TOP.map((v, i) => Math.round(v + (BOTTOM[i] - v) * t)) : FLAT;
    for (let x = 0; x < width; x++) {
      const o = (y * width + x) * 3;
      let r = bg[0], g = bg[1], b = bg[2];
      const fx = x - padX, fy = y - padY;
      const inFace = fx >= 0 && fx < w && fy >= 0 && fy < h;
      if (inFace) {
        const sx = Math.min(srcWidth - 1, Math.floor(cell + (fx * 10 * cell) / w));
        const sy = Math.min(srcHeight - 1, Math.floor(cell + (fy * 16 * cell) / h));
        const s = (sy * srcWidth + sx) * 4;
        const a = rgba[s + 3] / 255;
        // 透明的地方墊外框的深色（網頁 .face 的底色）
        r = Math.round(rgba[s] * a + EDGE[0] * (1 - a));
        g = Math.round(rgba[s + 1] * a + EDGE[1] * (1 - a));
        b = Math.round(rgba[s + 2] * a + EDGE[2] * (1 - a));
      } else if (fx >= -2 && fx < w + 2 && fy >= -2 && fy < h + 2) {
        r = EDGE[0];
        g = EDGE[1];
        b = EDGE[2];
      }
      rgb[o] = r;
      rgb[o + 1] = g;
      rgb[o + 2] = b;
    }
  }
  return rgb;
}

/**
 * 披風圖檔 → 預覽圖。
 *
 * @param bytes 原圖（PNG 或 GIF）
 * @param type 'png' 或 'gif'
 * @returns {bytes, type} 或 null（解不開）
 */
export async function capePreview(bytes, type) {
  if (type === 'gif') {
    const gif = decodeGif(bytes);
    if (!gif) return null;
    // GIF 的每一格都要切與編碼：背面最高 384 像素，Worker 算得完、Discord 也抓得動
    const box = layout(gif.width, gif.height, 384);
    const frames = gif.frames.map((f) => ({ rgb: paint(f.rgba, gif.width, gif.height, box, false), delay: f.delay }));
    return { bytes: encodeGif(box.width, box.height, frames), type: 'gif' };
  }
  const png = await decodePng(bytes);
  if (!png) return null;
  const box = layout(png.width, png.height, 512);
  return { bytes: await encodePng(box.width, box.height, paint(png.rgba, png.width, png.height, box, true)), type: 'png' };
}

// ---------------------------------------------------------------- 人物：skin 穿著披風，正面＋背面

/*
 * 使用者 2026-10-17：「像網站預覽展示 skin＋披風和正反面（如果 GIF 能動的話）」。
 * 照 Minecraft 的貼圖位置把 skin 平貼成正面與背面（16×32 格，每格 P 像素），
 * 背面蓋上披風的外側（範本 (1,1) 那塊 10×16 格，跟披風一覽的卡片同一塊），
 * 正面在腿的兩旁露出披風內側（(12,1) 那塊，從前面看是左右相反的）。
 * 細手臂（Alex）照 skin 本身判斷；舊式 64×32 的 skin 左手左腳是右邊鏡射過來的。
 */

/** 沒有 skin 時的灰色人偶（跟網頁立體檢視的木偶同一組顏色）。 */
function mannequin() {
  const rgba = new Uint8Array(64 * 64 * 4);
  const fill = (x0, y0, w, h, c) => {
    for (let y = y0; y < y0 + h; y++) for (let x = x0; x < x0 + w; x++) {
      const o = (y * 64 + x) * 4;
      rgba[o] = c[0]; rgba[o + 1] = c[1]; rgba[o + 2] = c[2]; rgba[o + 3] = 255;
    }
  };
  fill(0, 0, 32, 16, [0xa4, 0xae, 0xa7]);
  fill(0, 16, 64, 16, [0x8f, 0x9a, 0x93]);
  fill(16, 48, 32, 16, [0x7d, 0x88, 0x81]);
  fill(0, 16, 16, 16, [0x7d, 0x88, 0x81]);
  return { width: 64, height: 64, rgba };
}

/** 一個面要畫在哪：[skin x, skin y, 寬, 高, 人物 x, 人物 y, 左右翻]（單位都是 skin 像素）。 */
function parts(skin) {
  const legacy = skin.height < 64;
  const px = (x, y) => skin.rgba[((y * skin.width) + x) * 4 + 3];
  const aw = !legacy && px(54, 20) === 0 ? 3 : 4;        // 細手臂：右臂背面的第四欄是空的
  const L = 4 - aw;                                       // 細手臂往身體靠一格
  const front = [
    [8, 8, 8, 8, 4, 0], [20, 20, 8, 12, 4, 8],
    [44, 20, aw, 12, L, 8], [4, 20, 4, 12, 4, 20],
  ];
  const back = [
    [24, 8, 8, 8, 4, 0], [32, 20, 8, 12, 4, 8],
    [52 - (4 - aw), 20, aw, 12, 12, 8], [12, 20, 4, 12, 8, 20],
  ];
  if (legacy) {
    front.push([44, 20, aw, 12, 12, 8, true], [4, 20, 4, 12, 8, 20, true]);
    back.push([52, 20, aw, 12, L, 8, true], [12, 20, 4, 12, 4, 20, true]);
    front.push([40, 8, 8, 8, 4, 0]);                      // 帽子層
    back.push([56, 8, 8, 8, 4, 0]);
  } else {
    front.push([36, 52, aw, 12, 12, 8], [20, 52, 4, 12, 8, 20]);
    back.push([44 - (4 - aw), 52, aw, 12, L, 8], [28, 52, 4, 12, 4, 20]);
    // 外層（帽子、外套、袖子、褲管）
    front.push([40, 8, 8, 8, 4, 0], [20, 36, 8, 12, 4, 8], [44, 36, aw, 12, L, 8], [52, 52, aw, 12, 12, 8],
      [4, 36, 4, 12, 4, 20], [4, 52, 4, 12, 8, 20]);
    back.push([56, 8, 8, 8, 4, 0], [32, 36, 8, 12, 4, 8], [52 - (4 - aw), 36, aw, 12, 12, 8],
      [60 - (4 - aw), 52, aw, 12, L, 8], [12, 36, 4, 12, 8, 20], [12, 52, 4, 12, 4, 20]);
  }
  return { front, back };
}

/** 把 skin 的幾個面拼成 16×32 的人物（RGBA）。 */
function figure(skin, list) {
  const out = new Uint8Array(16 * 32 * 4);
  for (const [sx, sy, w, h, dx, dy, flip] of list) {
    for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
      const tx = sx + (flip ? w - 1 - x : x), ty = sy + y;
      if (tx >= skin.width || ty >= skin.height) continue;
      const s = (ty * skin.width + tx) * 4;
      const a = skin.rgba[s + 3] / 255;
      if (!a) continue;
      const o = ((dy + y) * 16 + dx + x) * 4;
      for (let c = 0; c < 3; c++) out[o + c] = Math.round(skin.rgba[s + c] * a + out[o + c] * (1 - a));
      out[o + 3] = Math.max(out[o + 3], skin.rgba[s + 3]);
    }
  }
  return out;
}

/**
 * 畫一張：左正面、右背面，底下各一圈影子。
 *
 * @param skin {width, height, rgba}
 * @param cape {width, height, rgba} 披風範本（22×17 格）
 * @param P 每個 skin 像素畫成幾格
 * @param gradient 底色用漸層（PNG）還是單色（GIF 的調色盤有限）
 */
function paintFigure(skin, faces, cape, P, gradient) {
  const front = figure(skin, faces.front), back = figure(skin, faces.back);
  const gap = 4 * P, top = 3 * P;
  const W = 16 * P * 2 + gap * 3, H = 32 * P + top * 2;
  const rgb = new Uint8Array(W * H * 3);
  const cell = cape.width / 22;
  const put = (x, y, r, g, b, a) => {
    const o = (y * W + x) * 3;
    rgb[o] = Math.round(r * a + rgb[o] * (1 - a));
    rgb[o + 1] = Math.round(g * a + rgb[o + 1] * (1 - a));
    rgb[o + 2] = Math.round(b * a + rgb[o + 2] * (1 - a));
  };
  for (let y = 0; y < H; y++) {
    const t = H > 1 ? y / (H - 1) : 0;
    const bg = gradient ? TOP.map((v, i) => Math.round(v + (BOTTOM[i] - v) * t)) : FLAT;
    for (let x = 0; x < W; x++) rgb.set(bg, (y * W + x) * 3);
  }
  // 影子：腳下一圈壓暗的橢圓
  for (const ox of [gap, gap * 2 + 16 * P]) {
    const cx = ox + 8 * P, cy = top + 32 * P, rx = 7 * P, ry = 1.4 * P;
    for (let y = Math.floor(cy - ry); y <= Math.ceil(cy + ry); y++) for (let x = Math.floor(cx - rx); x <= Math.ceil(cx + rx); x++) {
      if (x < 0 || y < 0 || x >= W || y >= H) continue;
      const d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2;
      if (d <= 1) put(x, y, 0, 0, 0, 0.35 * (1 - d));
    }
  }
  // 披風：外側 (1,1)、內側 (12,1)，10×16 格，掛在肩膀（人物座標 x 3..13、y 8..24）
  const capeAt = (ox, inner, fx, fy) => {
    const u = fx / (10 * P), v = fy / (16 * P);
    const cx = inner ? 12 + (1 - u) * 10 : 1 + u * 10;
    const sx = Math.min(cape.width - 1, Math.floor(cx * cell)), sy = Math.min(cape.height - 1, Math.floor((1 + v * 16) * cell));
    const s = (sy * cape.width + sx) * 4;
    return [cape.rgba[s], cape.rgba[s + 1], cape.rgba[s + 2], cape.rgba[s + 3] / 255];
  };
  const drawCape = (ox, inner) => {
    for (let fy = 0; fy < 16 * P; fy++) for (let fx = 0; fx < 10 * P; fx++) {
      const [r, g, b, a] = capeAt(ox, inner, fx, fy);
      if (a) put(ox + 3 * P + fx, top + 8 * P + fy, r, g, b, a);
    }
  };
  const drawFigure = (ox, fig) => {
    for (let y = 0; y < 32 * P; y++) for (let x = 0; x < 16 * P; x++) {
      const o = ((y / P | 0) * 16 + (x / P | 0)) * 4;
      if (fig[o + 3]) put(ox + x, top + y, fig[o], fig[o + 1], fig[o + 2], 1);
    }
  };
  drawCape(gap, true);                       // 正面：披風在身後，只從腿的兩旁露出來
  drawFigure(gap, front);
  drawFigure(gap * 2 + 16 * P, back);        // 背面：披風蓋在身上
  drawCape(gap * 2 + 16 * P, false);
  return { width: W, height: H, rgb };
}

/**
 * 披風＋skin → 人物預覽（正面、背面）。GIF 的披風逐格畫，照樣會動。
 *
 * @param capeBytes 披風原圖
 * @param type 'png' 或 'gif'
 * @param skinBytes skin 的 PNG；沒有（抓不到）就用灰色人偶
 */
export async function figurePreview(capeBytes, type, skinBytes) {
  let skin = null;
  if (skinBytes) {
    try {
      skin = await decodePng(skinBytes);
    } catch (e) {
      skin = null;
    }
  }
  if (!skin || skin.width !== 64 || (skin.height !== 64 && skin.height !== 32)) skin = mannequin();
  const faces = parts(skin);
  if (type === 'gif') {
    const gif = decodeGif(capeBytes);
    if (!gif) return null;
    let size = null;
    const frames = gif.frames.map((f) => {
      const pic = paintFigure(skin, faces, { width: gif.width, height: gif.height, rgba: f.rgba }, 8, false);
      size = pic;
      return { rgb: pic.rgb, delay: f.delay };
    });
    return { bytes: encodeGif(size.width, size.height, frames), type: 'gif' };
  }
  const png = await decodePng(capeBytes);
  if (!png) return null;
  const pic = paintFigure(skin, faces, png, 12, true);
  return { bytes: await encodePng(pic.width, pic.height, pic.rgb), type: 'png' };
}
