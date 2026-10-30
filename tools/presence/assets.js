import { tagOf } from './site.js';
import { FONTS } from './demo-fonts.js';
import { TEXTURES } from './demo-textures.js';
import { CAPE_TEMPLATES } from './cape-templates.js';
import { APPLE_TOUCH, FAVICON, ICON_192 } from './icons.js';
import { no } from './http.js';

export const ICONS = {
  '/favicon.ico': FAVICON,
  '/favicon.png': FAVICON,
  '/icon-192.png': ICON_192,
  '/apple-touch-icon.png': APPLE_TOUCH,
};

export function icon(base64) {
  const raw = atob(base64);
  const bytes = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i);
  return new Response(bytes, {
    headers: {
      'content-type': 'image/png',
      'cache-control': 'public, max-age=86400',
      'x-content-type-options': 'nosniff',
    },
  });
}

export function page(html, lang, status = 200) {
  return new Response(html, {
    status,
    headers: {
      'content-type': 'text/html; charset=utf-8',
      'content-language': tagOf(lang),
      'cache-control': 'no-store',
      vary: 'Cookie, Accept-Language',
      'referrer-policy': 'no-referrer',
      'x-content-type-options': 'nosniff',
      'x-frame-options': 'DENY',
      'content-security-policy': "default-src 'self'; "
        + "img-src 'self' data: blob: https://raw.githubusercontent.com https://minotar.net; "
        + "style-src 'unsafe-inline'; script-src 'unsafe-inline' https://cdn.jsdelivr.net; "
        + "connect-src 'self'; base-uri 'none'; form-action 'none'",
    },
  });
}

export function font(path) {
  const found = /^\/font\/px-([a-z]{2})\.ttf$/.exec(path);
  const one = found && Object.hasOwn(FONTS, found[1]) ? FONTS[found[1]] : null;
  if (!one) return no('not found', 404);
  const bytes = Uint8Array.from(atob(one.data), (c) => c.charCodeAt(0));
  return new Response(bytes, {
    status: 200,
    headers: {
      'content-type': 'font/ttf',
      'cache-control': 'public, max-age=31536000, immutable',
      'x-content-type-options': 'nosniff',
    },
  });
}

export function texture(path) {
  const from = path.startsWith('/template/') ? CAPE_TEMPLATES : TEXTURES;
  const name = path.slice(path.indexOf('/', 1) + 1);
  const one = Object.hasOwn(from, name) ? from[name] : null;
  if (!one) return no('not found', 404);
  const bytes = Uint8Array.from(atob(one.data), (c) => c.charCodeAt(0));
  return new Response(bytes, {
    status: 200,
    headers: {
      'content-type': one.type,
      'cache-control': 'public, max-age=31536000, immutable',
      'x-content-type-options': 'nosniff',
    },
  });
}
