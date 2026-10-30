import { base64Bytes } from './http.js';

export const REPO = 'LyuChaCha/WynnChaYuan';

export function utf8ToBase64(text) {
  let binary = '';
  for (const byte of new TextEncoder().encode(text)) binary += String.fromCharCode(byte);
  return btoa(binary);
}

export function base64ToUtf8(base64) {
  return new TextDecoder().decode(base64Bytes(base64.replace(/\s/g, '')));
}

const CONTROL_CHARS = new RegExp('[' + String.fromCharCode(0) + '-' + String.fromCharCode(31) + String.fromCharCode(127) + ']+', 'g');
const ZERO_WIDTH_SPACE = String.fromCharCode(0x200b);

export const withoutMentions = (text) => String(text).replace(CONTROL_CHARS, ' ').trim().replace(/@/g, '@' + ZERO_WIDTH_SPACE);

const WORD_CHAR = /[\p{L}\p{N}]/u;

export const markdownText = (text) => withoutMentions(String(text)
  .replace(/[\\`*{}[\]()<>#+!|~]/g, '\\$&')
  .replace(/_/g, (mark, at, all) => (WORD_CHAR.test(all[at - 1] ?? '') && WORD_CHAR.test(all[at + 1] ?? '') ? mark : '\\_')));

export function githubClient(env) {
  return (path, init = {}) => fetch('https://api.github.com/repos/' + REPO + path, {
    ...init,
    headers: {
      authorization: 'Bearer ' + env.GITHUB_TOKEN,
      accept: 'application/vnd.github+json',
      'user-agent': 'wynnchayuan-site',
      'content-type': 'application/json',
    },
  });
}

export class GitHubError extends Error {
  constructor(status) {
    super('github ' + status);
    this.status = status;
  }
}

async function expectStatus(response, ...accepted) {
  if (!accepted.includes(response.status)) throw new GitHubError(response.status);
  return response;
}

export function githubRepository(env) {
  const github = githubClient(env);
  return {
    async openPullFor(branch) {
      const ref = await github('/git/ref/heads/' + branch);
      if (ref.status === 404) return { exists: false, url: '' };
      await expectStatus(ref, 200);
      const open = await github('/pulls?head=' + encodeURIComponent(REPO.split('/')[0] + ':' + branch) + '&state=open');
      const list = open.status === 200 ? await open.json() : [];
      return { exists: true, url: Array.isArray(list) && list[0] ? String(list[0].html_url || '') : '' };
    },
    async createBranch(branch) {
      const head = await expectStatus(await github('/git/ref/heads/main'), 200);
      await expectStatus(await github('/git/refs', {
        method: 'POST', body: JSON.stringify({ ref: 'refs/heads/' + branch, sha: (await head.json()).object.sha }),
      }), 201);
    },
    async deleteBranch(branch) {
      try {
        const removed = await github('/git/refs/heads/' + branch, { method: 'DELETE' });
        if (removed.status !== 204) console.error(JSON.stringify({ event: 'branch cleanup failed', branch, status: removed.status }));
      } catch (error) {
        console.error(JSON.stringify({ event: 'branch cleanup failed', branch, error: String(error?.message ?? error) }));
      }
    },
    async readFile(path, ref) {
      const file = await expectStatus(await github('/contents/' + path + '?ref=' + encodeURIComponent(ref)), 200);
      const current = await file.json();
      return { text: base64ToUtf8(current.content), sha: current.sha };
    },
    async writeFile(path, { message, text, sha, branch }) {
      await expectStatus(await github('/contents/' + path, {
        method: 'PUT', body: JSON.stringify({ message, content: utf8ToBase64(text), sha, branch }),
      }), 200, 201);
    },
    async openPull({ title, head, body }) {
      const pull = await expectStatus(await github('/pulls', {
        method: 'POST', body: JSON.stringify({ title, head, base: 'main', body }),
      }), 201);
      return (await pull.json()).html_url;
    },
  };
}
