// 精簡 MD5 實作（Web Crypto 不支援 MD5），僅用於 B站 WBI 簽名。輸入為 ASCII/UTF-8 字串。
(function (g) {
  function md5(str) {
    const bytes = new TextEncoder().encode(str);
    const len = bytes.length;
    const nBlocks = ((len + 8) >>> 6) + 1;
    const w = new Uint32Array(nBlocks * 16);
    for (let i = 0; i < len; i++) w[i >> 2] |= bytes[i] << ((i % 4) * 8);
    w[len >> 2] |= 0x80 << ((len % 4) * 8);
    w[nBlocks * 16 - 2] = (len * 8) >>> 0;
    w[nBlocks * 16 - 1] = Math.floor(len / 0x20000000);

    const S = [7, 12, 17, 22, 5, 9, 14, 20, 4, 11, 16, 23, 6, 10, 15, 21];
    const K = new Uint32Array(64);
    for (let i = 0; i < 64; i++) K[i] = Math.floor(Math.abs(Math.sin(i + 1)) * 0x100000000);

    let a0 = 0x67452301, b0 = 0xefcdab89, c0 = 0x98badcfe, d0 = 0x10325476;
    for (let blk = 0; blk < w.length; blk += 16) {
      let A = a0, B = b0, C = c0, D = d0;
      for (let i = 0; i < 64; i++) {
        let F, gi;
        if (i < 16) { F = (B & C) | (~B & D); gi = i; }
        else if (i < 32) { F = (D & B) | (~D & C); gi = (5 * i + 1) % 16; }
        else if (i < 48) { F = B ^ C ^ D; gi = (3 * i + 5) % 16; }
        else { F = C ^ (B | ~D); gi = (7 * i) % 16; }
        const s = S[(i >> 4) * 4 + (i % 4)];
        F = (F + A + K[i] + w[blk + gi]) >>> 0;
        A = D; D = C; C = B;
        B = (B + ((F << s) | (F >>> (32 - s)))) >>> 0;
      }
      a0 = (a0 + A) >>> 0; b0 = (b0 + B) >>> 0; c0 = (c0 + C) >>> 0; d0 = (d0 + D) >>> 0;
    }
    return [a0, b0, c0, d0]
      .map((x) => {
        let h = '';
        for (let i = 0; i < 4; i++) h += ((x >>> (i * 8)) & 0xff).toString(16).padStart(2, '0');
        return h;
      })
      .join('');
  }
  g.bdlMd5 = md5;
})(typeof globalThis !== 'undefined' ? globalThis : this);
