
const TOP = [0x1b, 0x2a, 0x21];
const BOTTOM = [0x13, 0x1b, 0x16];
const FLAT = [0x17, 0x22, 0x1b];
const EDGE = [0x0b, 0x0f, 0x0d];


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
  const bpp = Math.max(1, (channels * depth) >> 3);
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
    if (code > next) break;
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


export function layout(srcWidth, srcHeight, maxFace) {
  const cell = srcWidth / 22;
  const faceW = 10 * cell, faceH = 16 * cell;
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

export async function capePreview(bytes, type) {
  if (type === 'gif') {
    const gif = decodeGif(bytes);
    if (!gif) return null;
    // 背面最高 384 像素：Worker 的 CPU 時間算得完、Discord 也抓得動
    const box = layout(gif.width, gif.height, 384);
    const frames = gif.frames.map((f) => ({ rgb: paint(f.rgba, gif.width, gif.height, box, false), delay: f.delay }));
    return { bytes: encodeGif(box.width, box.height, frames), type: 'gif' };
  }
  const png = await decodePng(bytes);
  if (!png) return null;
  const box = layout(png.width, png.height, 512);
  return { bytes: await encodePng(box.width, box.height, paint(png.rgba, png.width, png.height, box, true)), type: 'png' };
}



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

function normaliseSkin(skin) {
  const rgba = new Uint8Array(64 * 64 * 4);
  const legacy = skin.height < 64;
  rgba.set(skin.rgba.subarray(0, 64 * skin.height * 4));
  // 細手臂：右臂背面的第四欄是空的（要在補成不透明之前看）
  const slim = !legacy && rgba[(20 * 64 + 54) * 4 + 3] === 0;
  const copy = (sx, sy, w, h, dx, dy) => {
    for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
      const s = ((sy + y) * 64 + sx + w - 1 - x) * 4, o = ((dy + y) * 64 + dx + x) * 4;
      for (let c = 0; c < 4; c++) rgba[o + c] = rgba[s + c];
    }
  };
  if (legacy) {
    for (const [u, v, du, dv] of [[0, 16, 16, 48], [40, 16, 32, 48]]) {
      copy(u + 4, v, 4, 4, du + 4, dv);
      copy(u + 8, v, 4, 4, du + 8, dv);
      copy(u, v + 4, 4, 12, du + 8, dv + 4);
      copy(u + 4, v + 4, 4, 12, du + 4, dv + 4);
      copy(u + 8, v + 4, 4, 12, du, dv + 4);
      copy(u + 12, v + 4, 4, 12, du + 12, dv + 4);
    }
    let solid = true;
    for (let y = 0; y < 16 && solid; y++) for (let x = 32; x < 64; x++) if (rgba[(y * 64 + x) * 4 + 3] < 128) { solid = false; break; }
    if (solid) for (let y = 0; y < 16; y++) for (let x = 32; x < 64; x++) rgba[(y * 64 + x) * 4 + 3] = 0;
  }
  for (const [x0, y0, w, h] of [[0, 0, 32, 16], [0, 16, 56, 16], [16, 48, 32, 16]]) {
    for (let y = y0; y < y0 + h; y++) for (let x = x0; x < x0 + w; x++) rgba[(y * 64 + x) * 4 + 3] = 255;
  }
  return { width: 64, height: 64, rgba, slim };
}

function boxFaces(x0, y0, z0, x1, y1, z1, u, v, w, h, d, tex) {
  const X = x1 - x0, Y = y1 - y0, Z = z1 - z0;
  return [
    { o: [x0, y1, z1], u: [X, 0, 0], v: [0, -Y, 0], r: [u + d, v + d, w, h], tex },
    { o: [x1, y1, z0], u: [-X, 0, 0], v: [0, -Y, 0], r: [u + 2 * d + w, v + d, w, h], tex },
    { o: [x0, y1, z0], u: [0, 0, Z], v: [0, -Y, 0], r: [u, v + d, d, h], tex },
    { o: [x1, y1, z1], u: [0, 0, -Z], v: [0, -Y, 0], r: [u + d + w, v + d, d, h], tex },
    { o: [x0, y1, z0], u: [X, 0, 0], v: [0, 0, Z], r: [u + d, v, w, d], tex },
    { o: [x0, y0, z1], u: [X, 0, 0], v: [0, 0, -Z], r: [u + d + w, v, w, d], tex },
  ];
}

function swing(faces, py, pz, a) {
  const c = Math.cos(a), s = Math.sin(a);
  const turn = ([x, y, z]) => [x, py + (y - py) * c - (z - pz) * s, pz + (y - py) * s + (z - pz) * c];
  const dir = ([x, y, z]) => [x, y * c - z * s, y * s + z * c];
  return faces.map((f) => ({ ...f, o: turn(f.o), u: dir(f.u), v: dir(f.v) }));
}

const STRIDE = 0.3, ARM = 0.24, CAPE_TILT = 0.26;

function bodyFaces(skin) {
  const aw = skin.slim ? 3 : 4;
  const out = [];
  const part = (x0, y0, z0, x1, y1, z1, base, over, grow, py, a) => {
    let faces = boxFaces(x0, y0, z0, x1, y1, z1, base[0], base[1], x1 - x0, y1 - y0, z1 - z0, 'skin');
    faces = faces.concat(boxFaces(x0 - grow, y0 - grow, z0 - grow, x1 + grow, y1 + grow, z1 + grow,
      over[0], over[1], x1 - x0, y1 - y0, z1 - z0, 'skin'));
    out.push(...(a ? swing(faces, py, 0, a) : faces));
  };
  part(-4, 24, -4, 4, 32, 4, [0, 0], [32, 0], 0.5);
  part(-4, 12, -2, 4, 24, 2, [16, 16], [16, 32], 0.25);
  part(-4 - aw, 12, -2, -4, 24, 2, [40, 16], [40, 32], 0.25, 22, ARM);
  part(4, 12, -2, 4 + aw, 24, 2, [32, 48], [48, 48], 0.25, 22, -ARM);
  part(-4, 0, -2, 0, 12, 2, [0, 16], [0, 32], 0.25, 12, -STRIDE);
  part(0, 0, -2, 4, 12, 2, [16, 48], [0, 48], 0.25, 12, STRIDE);
  return out;
}

function capeFaces() {
  const faces = [
    { o: [5, 24, -3], u: [-10, 0, 0], v: [0, -16, 0], r: [1, 1, 10, 16], tex: 'cape' },
    { o: [-5, 24, -2], u: [10, 0, 0], v: [0, -16, 0], r: [12, 1, 10, 16], tex: 'cape', flip: true },
    { o: [5, 24, -2], u: [0, 0, -1], v: [0, -16, 0], r: [0, 1, 1, 16], tex: 'cape' },
    { o: [-5, 24, -3], u: [0, 0, 1], v: [0, -16, 0], r: [11, 1, 1, 16], tex: 'cape' },
    { o: [-5, 24, -3], u: [10, 0, 0], v: [0, 0, 1], r: [1, 0, 10, 1], tex: 'cape', flip: true },
    { o: [-5, 8, -2], u: [10, 0, 0], v: [0, 0, -1], r: [11, 0, 10, 1], tex: 'cape', flip: true },
  ];
  return swing(faces, 24, -2, CAPE_TILT);
}

const YAW = Math.PI + 0.5, PITCH = 0.12, DIST = 72;
const LIGHT = (() => {
  const l = [0.5, 0.6, 0.8];
  const n = Math.hypot(...l);
  return l.map((v) => v / n);
})();

function view([x, y, z]) {
  const cy = Math.cos(YAW), sy = Math.sin(YAW), cp = Math.cos(PITCH), sp = Math.sin(PITCH);
  y -= 16;
  const x1 = x * cy + z * sy, z1 = -x * sy + z * cy;
  return [x1, y * cp - z1 * sp, y * sp + z1 * cp];
}
function viewDir([x, y, z]) {
  const cy = Math.cos(YAW), sy = Math.sin(YAW), cp = Math.cos(PITCH), sp = Math.sin(PITCH);
  const x1 = x * cy + z * sy, z1 = -x * sy + z * cy;
  return [x1, y * cp - z1 * sp, y * sp + z1 * cp];
}

function canvas(w, h) {
  return { w, h, rgb: new Uint8Array(w * h * 3), hit: new Uint8Array(w * h), depth: new Float32Array(w * h) };
}
function cloneCanvas(c) {
  return { w: c.w, h: c.h, rgb: c.rgb.slice(), hit: c.hit.slice(), depth: c.depth.slice() };
}

function draw(cv, faces, cam, sample) {
  for (const face of faces) {
    const corners = [face.o, add(face.o, face.u), add(add(face.o, face.u), face.v), add(face.o, face.v)].map(view);
    const n = viewDir(cross(face.v, face.u));
    const len = Math.hypot(...n) || 1;
    const nn = n.map((v) => v / len);
    const mid = corners.reduce((a, p) => [a[0] + p[0] / 4, a[1] + p[1] / 4, a[2] + p[2] / 4], [0, 0, 0]);
    if (nn[0] * -mid[0] + nn[1] * -mid[1] + nn[2] * (DIST - mid[2]) <= 0) continue;
    const shade = 0.62 + 0.38 * Math.max(0, nn[0] * LIGHT[0] + nn[1] * LIGHT[1] + nn[2] * LIGHT[2]);
    const st = [[0, 0], [1, 0], [1, 1], [0, 1]];
    const pts = corners.map((p, i) => {
      const w = 1 / (DIST - p[2]);
      return { x: cam.cx + cam.f * p[0] * w, y: cam.cy - cam.f * p[1] * w, w, s: st[i][0] * w, t: st[i][1] * w };
    });
    tri(cv, pts[0], pts[1], pts[2], face, shade, sample);
    tri(cv, pts[0], pts[2], pts[3], face, shade, sample);
  }
}
function add(a, b) {
  return [a[0] + b[0], a[1] + b[1], a[2] + b[2]];
}
function cross(a, b) {
  return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
}

function tri(cv, a, b, c, face, shade, sample) {
  const area = (b.x - a.x) * (c.y - a.y) - (c.x - a.x) * (b.y - a.y);
  if (Math.abs(area) < 1e-9) return;
  const minX = Math.max(0, Math.floor(Math.min(a.x, b.x, c.x))), maxX = Math.min(cv.w - 1, Math.ceil(Math.max(a.x, b.x, c.x)));
  const minY = Math.max(0, Math.floor(Math.min(a.y, b.y, c.y))), maxY = Math.min(cv.h - 1, Math.ceil(Math.max(a.y, b.y, c.y)));
  const [ru, rv, rw, rh] = face.r;
  for (let y = minY; y <= maxY; y++) {
    const py = y + 0.5;
    for (let x = minX; x <= maxX; x++) {
      const px = x + 0.5;
      const l0 = ((b.x - px) * (c.y - py) - (c.x - px) * (b.y - py)) / area;
      const l1 = ((c.x - px) * (a.y - py) - (a.x - px) * (c.y - py)) / area;
      const l2 = 1 - l0 - l1;
      if (l0 < -1e-6 || l1 < -1e-6 || l2 < -1e-6) continue;
      const w = l0 * a.w + l1 * b.w + l2 * c.w;
      const p = y * cv.w + x;
      if (cv.hit[p] && cv.depth[p] >= w) continue;
      let s = (l0 * a.s + l1 * b.s + l2 * c.s) / w, t = (l0 * a.t + l1 * b.t + l2 * c.t) / w;
      s = Math.min(Math.max(s, 0), 0.999999);
      t = Math.min(Math.max(t, 0), 0.999999);
      if (face.flip) s = 0.999999 - s;
      const col = sample(face.tex, ru + s * rw, rv + t * rh, ru, rv, rw, rh);
      if (col[3] < 128) continue;
      cv.hit[p] = 1;
      cv.depth[p] = w;
      cv.rgb[p * 3] = Math.min(255, Math.round(col[0] * shade));
      cv.rgb[p * 3 + 1] = Math.min(255, Math.round(col[1] * shade));
      cv.rgb[p * 3 + 2] = Math.min(255, Math.round(col[2] * shade));
    }
  }
}

function sampler(skin, cape) {
  const cell = cape.width / 22;
  return (tex, x, y, ru, rv, rw, rh) => {
    if (tex === 'skin') {
      const tx = Math.min(ru + rw - 1, Math.floor(x)), ty = Math.min(rv + rh - 1, Math.floor(y));
      const o = (ty * 64 + tx) * 4;
      return [skin.rgba[o], skin.rgba[o + 1], skin.rgba[o + 2], skin.rgba[o + 3]];
    }
    const tx = Math.min(Math.floor((ru + rw) * cell) - 1, cape.width - 1, Math.floor(x * cell));
    const ty = Math.min(Math.floor((rv + rh) * cell) - 1, cape.height - 1, Math.floor(y * cell));
    const o = (ty * cape.width + tx) * 4;
    return [cape.rgba[o], cape.rgba[o + 1], cape.rgba[o + 2], cape.rgba[o + 3]];
  };
}

function frame(faces, height, ss) {
  let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
  for (const f of faces) {
    for (const q of [f.o, add(f.o, f.u), add(add(f.o, f.u), f.v), add(f.o, f.v)]) {
      const p = view(q);
      const w = 1 / (DIST - p[2]);
      minX = Math.min(minX, p[0] * w); maxX = Math.max(maxX, p[0] * w);
      minY = Math.min(minY, -p[1] * w); maxY = Math.max(maxY, -p[1] * w);
    }
  }
  const pad = Math.round(height * 0.09);
  const f = (height - pad * 2) / (maxY - minY);
  const width = Math.max(Math.round(height * 0.72), Math.round((maxX - minX) * f) + pad * 2);
  const cam = { f: f * ss, cx: (width / 2 - ((minX + maxX) / 2) * f) * ss, cy: (pad - minY * f) * ss };
  return { width, height, cam };
}

function finish(cv, width, height, ss, gradient, cam) {
  const rgb = new Uint8Array(width * height * 3);
  const feet = view([0, 0, 0]);
  const fw = 1 / (DIST - feet[2]);
  const sx = (cam.cx + cam.f * feet[0] * fw) / ss, sy = (cam.cy - cam.f * feet[1] * fw) / ss;
  const rx = cam.f * fw * 9 / ss, ry = Math.max(2, rx * 0.22);
  const n = ss * ss;
  for (let y = 0; y < height; y++) {
    const t = height > 1 ? y / (height - 1) : 0;
    const bg = gradient ? TOP.map((v, i) => v + (BOTTOM[i] - v) * t) : FLAT;
    for (let x = 0; x < width; x++) {
      const d = ((x + 0.5 - sx) / rx) ** 2 + ((y + 0.5 - sy) / ry) ** 2;
      const dark = d < 1 ? 1 - 0.4 * (1 - d) : 1;
      let r = 0, g = 0, b = 0;
      for (let j = 0; j < ss; j++) for (let i = 0; i < ss; i++) {
        const p = (y * ss + j) * cv.w + x * ss + i;
        if (cv.hit[p]) {
          r += cv.rgb[p * 3]; g += cv.rgb[p * 3 + 1]; b += cv.rgb[p * 3 + 2];
        } else {
          r += bg[0] * dark; g += bg[1] * dark; b += bg[2] * dark;
        }
      }
      const o = (y * width + x) * 3;
      rgb[o] = Math.round(r / n);
      rgb[o + 1] = Math.round(g / n);
      rgb[o + 2] = Math.round(b / n);
    }
  }
  return rgb;
}

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
  skin = normaliseSkin(skin);
  const body = bodyFaces(skin), cape = capeFaces();
  if (type === 'gif') {
    const gif = decodeGif(capeBytes);
    if (!gif) return null;
    // GIF 不超取樣：調色盤只有 256 色，邊緣的混色會吃掉顏色
    const box = frame(body.concat(cape), 360, 1);
    const base = canvas(box.width, box.height);
    draw(base, body, box.cam, sampler(skin, { width: gif.width, height: gif.height, rgba: gif.frames[0].rgba }));
    const frames = gif.frames.map((f) => {
      const cv = cloneCanvas(base);
      draw(cv, cape, box.cam, sampler(skin, { width: gif.width, height: gif.height, rgba: f.rgba }));
      return { rgb: finish(cv, box.width, box.height, 1, false, box.cam), delay: f.delay };
    });
    return { bytes: encodeGif(box.width, box.height, frames), type: 'gif' };
  }
  const png = await decodePng(capeBytes);
  if (!png) return null;
  const ss = 2;
  const box = frame(body.concat(cape), 480, ss);
  const cv = canvas(box.width * ss, box.height * ss);
  draw(cv, body.concat(cape), box.cam, sampler(skin, png));
  return { bytes: await encodePng(box.width, box.height, finish(cv, box.width, box.height, ss, true, box.cam)), type: 'png' };
}
