const btn = document.getElementById('open');
const hint = document.getElementById('hint');

chrome.tabs.query({ active: true, currentWindow: true }).then(([tab]) => {
  if (!tab?.url || !/^https:\/\/www\.bilibili\.com\/video\//.test(tab.url)) return;
  btn.disabled = false;
  hint.textContent = '也可以直接按頁面右下角的粉紅色「下載」按鈕。';
  btn.onclick = async () => {
    try {
      await chrome.tabs.sendMessage(tab.id, { type: 'openPanel' });
      window.close();
    } catch {
      hint.textContent = '頁面尚未載入擴充功能，請重新整理頁面後再試。';
    }
  };
});
