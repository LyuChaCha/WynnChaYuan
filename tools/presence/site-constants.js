export const LEAF = String.raw`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 21c.5-4.5 2.5-8 7-10"/><path d="M9 18c6.2 0 10.5-3.3 11-12V4h-4C7 4 4 8 4 13c0 1 0 3 2 5h3z"/></svg>`;

export const TEMPLATES = 'https://github.com/LyuChaCha/WynnChaYuan/tree/main/capes/templates';

export const ISSUES = 'https://github.com/LyuChaCha/WynnChaYuan/issues';

export const SIZES = '22×17, 44×34, 88×68, 176×136, 352×272, 704×544, 1056×816';

export const REPO = 'https://github.com/LyuChaCha/WynnChaYuan';

export const CURSEFORGE = 'https://www.curseforge.com/minecraft/mc-mods/wynnchayuan';

export const RELEASES = REPO + '/releases/latest';

export const KOFI = 'https://ko-fi.com/lyuchacha';

const stroke = (paths) => '<svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" '
  + 'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + paths + '</svg>';

export const ICON = {
  get: stroke('<path d="M12 4v11"/><path d="m7 11 5 5 5-5"/><path d="M5 20h14"/>'),
  heart: stroke('<path d="M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7a4.3 4.3 0 0 1 7.5 2.8C19.5 15.4 12 20 12 20z"/>'),
  down: stroke('<path d="m7 10 5 5 5-5"/>'),
  out: stroke('<path d="M8 16 16 8"/><path d="M9.5 8H16v6.5"/>'),
  go: stroke('<path d="M5 12h14"/><path d="m13 6 6 6-6 6"/>'),
  flag: stroke('<path d="M6 21V4"/><path d="M6 5h12l-2.5 4 2.5 4H6"/>'),
  exit: stroke('<path d="M9 20H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h3"/><path d="m15 16 4-4-4-4"/><path d="M19 12H9"/>'),
  globe: stroke('<circle cx="12" cy="12" r="8.5"/><path d="M3.5 12h17"/><path d="M12 3.5c2.3 2.5 3.4 5.3 3.4 8.5s-1.1 6-3.4 8.5c-2.3-2.5-3.4-5.3-3.4-8.5s1.1-6 3.4-8.5z"/>'),
  menu: stroke('<path d="M4 7h16"/><path d="M4 12h16"/><path d="M4 17h16"/>'),
  user: stroke('<circle cx="12" cy="8.5" r="3.5"/><path d="M5 20c.8-3.6 3.6-5.5 7-5.5s6.2 1.9 7 5.5"/>'),
  check: stroke('<path d="m5 12.5 4.5 4.5L19 7.5"/>'),
  account: stroke('<circle cx="12" cy="12" r="8.5"/><circle cx="12" cy="10" r="2.8"/><path d="M7 17.6c1.1-1.9 2.9-2.9 5-2.9s3.9 1 5 2.9"/>'),
  admin: stroke('<path d="M12 3.5 19 6v5.5c0 4.2-3 7.6-7 9-4-1.4-7-4.8-7-9V6z"/><path d="m9 12 2 2 4-4"/>'),
};
