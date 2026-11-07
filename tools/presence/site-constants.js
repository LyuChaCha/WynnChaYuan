export const LEAF = String.raw`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 21c.5-4.5 2.5-8 7-10"/><path d="M9 18c6.2 0 10.5-3.3 11-12V4h-4C7 4 4 8 4 13c0 1 0 3 2 5h3z"/></svg>`;

const BRAND_SVG = '<svg class="logo" viewBox="4 3.8 23.4 23.4" aria-hidden="true"><path transform="translate(7.2 7.4) scale(1.0)" d="M0-2.2L.55-.55 2.2 0 .55 .55 0 2.2-.55 .55-2.2 0-.55-.55z" fill="#D9F2C4"/><path transform="translate(25.6 24.2) scale(0.6)" d="M0-2.2L.55-.55 2.2 0 .55 .55 0 2.2-.55 .55-2.2 0-.55-.55z" fill="#D9F2C4"/><g transform="translate(3.07 2.24) scale(1.06)"><linearGradient id="BRAND_l7" gradientUnits="userSpaceOnUse" x1="4.5" y1="21" x2="20" y2="4"><stop offset="0" stop-color="#3A9F4C"/><stop offset=".5" stop-color="#5FC063"/><stop offset="1" stop-color="#A2DF86"/></linearGradient><radialGradient id="BRAND_r7" gradientUnits="userSpaceOnUse" cx="9.5" cy="7.6" r="9.5"><stop offset="0" stop-color="#FFFFFF" stop-opacity=".22"/><stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/></radialGradient><g transform="translate(.6 .9)" opacity=".3"><path d="M5 21c.5-4.5 2.5-8 7-10" fill="none" stroke-linecap="round" stroke-linejoin="round" stroke="#11280F" stroke-width="2"/><path d="M9 18c6.218 0 10.5-3.288 11-12v-2h-4.014c-9 0-11.986 4-12 9c0 1 0 3 2 5h3z" fill="#11280F"/><path transform="translate(5.4 19.0) rotate(26) scale(2.7)" d="M0 0C-.42-.64-1.12-.84-1.78-.62C-1.56.06-.84.4 0 0z" fill="#11280F"/></g><linearGradient id="BRAND_m8" x1="1" y1="1" x2="0" y2="0"><stop offset="0" stop-color="#3A9F4C"/><stop offset="0.4" stop-color="#55B95C"/><stop offset="1" stop-color="#A2DF86"/></linearGradient><path transform="translate(5.4 19.0) rotate(26) scale(2.7)" d="M0 0C-.42-.64-1.12-.84-1.78-.62C-1.56.06-.84.4 0 0z" fill="url(#BRAND_m8)"/><path transform="translate(5.4 19.0) rotate(26) scale(2.7)" d="M-.5-.42C-.85-.6-1.2-.66-1.5-.6" fill="none" stroke-linecap="round" stroke-linejoin="round" stroke="#FFFFFF" stroke-width=".09" opacity=".3"/><path d="M5 21c.5-4.5 2.5-8 7-10" fill="none" stroke-linecap="round" stroke-linejoin="round" stroke="url(#BRAND_l7)" stroke-width="2"/><path d="M9 18c6.218 0 10.5-3.288 11-12v-2h-4.014c-9 0-11.986 4-12 9c0 1 0 3 2 5h3z" fill="url(#BRAND_l7)"/><path d="M9 18c6.218 0 10.5-3.288 11-12v-2h-4.014c-9 0-11.986 4-12 9c0 1 0 3 2 5h3z" fill="url(#BRAND_r7)"/><path d="M14.4 5.7C9.9 6 6.5 8.1 5.7 12" fill="none" stroke-linecap="round" stroke-linejoin="round" stroke="#FFFFFF" stroke-width=".7" opacity=".4"/><path transform="translate(7.2 8.2) scale(0.5)" d="M5 .6v1.9M1 2.9h8M7.3 2.9C7 6 5 8.4 1.2 9.5M2.7 2.9C3 6 5 8.4 8.8 9.5" fill="none" stroke-linecap="round" stroke-linejoin="round" stroke="#FFFFFF" stroke-width="1.7"/><path transform="translate(12.6 8.2) scale(0.5)" d="M1.6 9.5 5 .9l3.4 8.6M2.9 6.3h4.2" fill="none" stroke-linecap="round" stroke-linejoin="round" stroke="#FFFFFF" stroke-width="1.7"/></g></svg>';

export const BRAND = (id) => BRAND_SVG.replace(/BRAND_/g, id + '-');

export const TEMPLATES = 'https://github.com/LyuChaCha/WynnChaYuan/tree/main/capes/templates';

export const ISSUES = 'https://github.com/LyuChaCha/WynnChaYuan/issues';

export const SIZES = '22×17, 44×34, 88×68, 176×136, 352×272, 704×544, 1056×816';

export const REPO = 'https://github.com/LyuChaCha/WynnChaYuan';

export const CURSEFORGE = 'https://www.curseforge.com/minecraft/mc-mods/wynnchayuan';

export const RELEASES = REPO + '/releases/latest';

export const KOFI = 'https://ko-fi.com/lyuchacha';

const stroke = (paths) => '<svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" '
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
  tooltip: stroke('<rect x="4.5" y="3.5" width="15" height="17" rx="2.5"/><path d="M8.5 8h7"/><path d="M8.5 12h7"/><path d="M8.5 16h4"/>'),
  tree: stroke('<circle cx="12" cy="5" r="2"/><circle cx="6" cy="19" r="2"/><circle cx="18" cy="19" r="2"/><path d="M12 7v4"/><path d="M6 17v-1.5a2.5 2.5 0 0 1 2.5-2.5h7a2.5 2.5 0 0 1 2.5 2.5V17"/>'),
  dialogue: stroke('<path d="M5.5 5h13A1.5 1.5 0 0 1 20 6.5v8a1.5 1.5 0 0 1-1.5 1.5H11l-4.5 3.5V16h-1A1.5 1.5 0 0 1 4 14.5v-8A1.5 1.5 0 0 1 5.5 5z"/><path d="M8 9.5h8"/><path d="M8 12.5h5"/>'),
  tracker: stroke('<path d="M10 6.5h10"/><path d="M10 12h10"/><path d="M10 17.5h10"/><path d="m4 6.5 1.5 1.5L8 5.5"/><path d="m4 12 1.5 1.5L8 11"/><path d="M4.5 17.5h3"/>'),
  nameplate: stroke('<rect x="5.5" y="3" width="13" height="5" rx="1.5"/><circle cx="12" cy="13" r="2.5"/><path d="M7 21c.6-2.8 2.6-4.5 5-4.5s4.4 1.7 5 4.5"/>'),
  chat: stroke('<path d="M5.5 4h8A1.5 1.5 0 0 1 15 5.5v6a1.5 1.5 0 0 1-1.5 1.5H9.5L6 15.5V13h-.5A1.5 1.5 0 0 1 4 11.5v-6A1.5 1.5 0 0 1 5.5 4z"/><path d="M18 9h.5A1.5 1.5 0 0 1 20 10.5v6a1.5 1.5 0 0 1-1.5 1.5H18v2.5L14.5 18h-4A1.5 1.5 0 0 1 9 16.5V16"/>'),
  window: stroke('<rect x="3.5" y="4.5" width="17" height="15" rx="2"/><path d="M3.5 9h17"/><path d="M9.5 9v10.5"/>'),
  swords: stroke('<path d="M14.5 17.5 3.5 6.5v-3h3l11 11"/><path d="m13 19 6-6"/><path d="m16 16 4 4"/><path d="m19 21 2-2"/><path d="M14.5 6.5 17.5 3.5h3v3l-3 3"/><path d="m5 14 4 4"/><path d="m7 17-3 3"/><path d="m3 19 2 2"/>'),
  compass: stroke('<circle cx="12" cy="12" r="8.5"/><path d="m15.5 8.5-2 5-5 2 2-5z"/>'),
  title: stroke('<path d="M5 7V5h14v2"/><path d="M12 5v14"/><path d="M9.5 19h5"/>'),
  refresh: stroke('<path d="M19.5 10.5A7.5 7.5 0 0 0 6 7.2L4.5 9"/><path d="M4.5 4.5V9H9"/><path d="M4.5 13.5A7.5 7.5 0 0 0 18 16.8l1.5-1.8"/><path d="M19.5 19.5V15H15"/>'),
  search: stroke('<circle cx="11" cy="11" r="6.5"/><path d="m20 20-4.4-4.4"/>'),
  pen: stroke('<path d="M4.5 19.5h4L19 9a2.1 2.1 0 0 0-3-3L5.5 16.5z"/><path d="m14.5 7.5 3 3"/>'),
  layers: stroke('<path d="m12 4 8.5 4.5L12 13 3.5 8.5z"/><path d="m3.5 12.5 8.5 4.5 8.5-4.5"/><path d="m3.5 16 8.5 4.5 8.5-4.5"/>'),
  panel: stroke('<rect x="3.5" y="4.5" width="17" height="15" rx="2"/><path d="M14.5 4.5v15"/><path d="M17 8.5h1"/><path d="M17 11.5h1"/>'),
  camera: stroke('<path d="M4 9A1.5 1.5 0 0 1 5.5 7.5h2L9 5.5h6l1.5 2h2A1.5 1.5 0 0 1 20 9v8.5a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 17.5z"/><circle cx="12" cy="13" r="3"/>'),
  sliders: stroke('<path d="M4 7h9"/><path d="M17 7h3"/><path d="M4 17h3"/><path d="M11 17h9"/><circle cx="15" cy="7" r="2"/><circle cx="9" cy="17" r="2"/>'),
  shield: stroke('<path d="M12 3.5 19 6v5.5c0 4.2-3 7.6-7 9-4-1.4-7-4.8-7-9V6z"/><path d="M9.5 12h5"/>'),
  plug: stroke('<path d="M9 3.5v4"/><path d="M15 3.5v4"/><path d="M6.5 7.5h11V11a5.5 5.5 0 0 1-11 0z"/><path d="M12 16.5v4"/>'),
  cup: stroke('<path d="M4.5 9h11v4.5a5 5 0 0 1-5 5h-1a5 5 0 0 1-5-5z"/><path d="M15.5 10.5h1.5a2.5 2.5 0 0 1 0 5h-1.6"/><path d="M8 3.5v2.5"/><path d="M11.5 3.5v2.5"/>'),
  branch: stroke('<circle cx="6.5" cy="5.5" r="2"/><circle cx="6.5" cy="18.5" r="2"/><circle cx="17.5" cy="7.5" r="2"/><path d="M6.5 7.5v9"/><path d="M17.5 9.5c0 3.5-2.5 4.5-5.5 5S7 16 6.5 16.5"/>'),
  box: stroke('<path d="m12 3.5 8 4v9l-8 4-8-4v-9z"/><path d="m4 7.5 8 4 8-4"/><path d="M12 11.5v9"/>'),
};
