const FILES = new Map();

function digest(text) {
  let a = 0x811c9dc5;
  let b = 0x01000193;
  for (let i = 0; i < text.length; i++) {
    const c = text.charCodeAt(i);
    a = Math.imul(a ^ c, 0x01000193) >>> 0;
    b = Math.imul(b ^ c, 0x5bd1e995) >>> 0;
  }
  return a.toString(16).padStart(8, '0') + b.toString(16).padStart(8, '0');
}

export function scriptFile(name, code) {
  const file = name + '.' + digest(code) + '.js';
  FILES.set(file, code);
  return '<script src="/js/' + file + '"></script>';
}

export function scriptSource(file) {
  return FILES.has(file) ? FILES.get(file) : null;
}

export const jsonData = (id, content) => '<script type="application/json" id="' + id + '">' + content + '</script>';

export const readJsonData = (id) => "JSON.parse(document.getElementById('" + id + "').textContent)";
