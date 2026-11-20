(function() {
  const input = document.getElementById('code-input');
  const hl = document.getElementById('highlight');
  const hlCode = document.getElementById('highlight-code');
  const gutter = document.getElementById('gutter');
  let currentLang = 'kt';
  let saveTimer = null;

  const KEYWORDS = new Set([
    'package','import','class','interface','object','fun','val','var','if','else','when',
    'for','while','return','true','false','null','override','private','public','internal',
    'protected','suspend','data','sealed','enum','companion','try','catch','finally','throw',
    'void','int','boolean','String','final','static','extends','implements','new','this',
    'function','const','let','async','await','export','default','from','class','widget',
    'BuildContext','Widget','StatelessWidget','StatefulWidget','State','plugins','dependencies'
  ]);

  function escapeHtml(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  function highlightText(src) {
    const escaped = escapeHtml(src);
    if (currentLang === 'xml' || currentLang === 'html') {
      return escaped
        .replace(/(&lt;!--[\s\S]*?--&gt;)/g, '<span class="tok-cmt">$1</span>')
        .replace(/(&lt;\/?[a-zA-Z0-9_:-]+)/g, '<span class="tok-tag">$1</span>')
        .replace(/(".*?")/g, '<span class="tok-str">$1</span>');
    }
    return escaped
      .replace(/(\/\/[^\n]*)/g, '<span class="tok-cmt">$1</span>')
      .replace(/("([^"\\]|\\.)*"|'([^'\\]|\\.)*')/g, '<span class="tok-str">$1</span>')
      .replace(/\b(\d+(?:\.\d+)?)\b/g, '<span class="tok-num">$1</span>')
      .replace(/\b([A-Za-z_][A-Za-z0-9_]*)\b/g, (m) => KEYWORDS.has(m) ? '<span class="tok-kw">' + m + '</span>' : m);
  }

  function updateView() {
    const val = input.value;
    hlCode.innerHTML = highlightText(val) + '\n';
    const lines = val.split('\n').length;
    let nums = '';
    for (let i = 1; i <= lines; i++) nums += i + '\n';
    gutter.textContent = nums;
  }

  function syncScroll() {
    hl.scrollTop = input.scrollTop;
    hl.scrollLeft = input.scrollLeft;
    gutter.scrollTop = input.scrollTop;
  }

  input.addEventListener('input', () => {
    updateView();
    if (window.MangoBridge) {
      window.MangoBridge.onContentChanged(input.value);
      clearTimeout(saveTimer);
      saveTimer = setTimeout(() => window.MangoBridge.onAutoSave(input.value), 1200);
    }
  });

  input.addEventListener('scroll', syncScroll);

  document.getElementById('symbol-bar').addEventListener('click', (e) => {
    const btn = e.target.closest('button');
    if (!btn) return;
    const ins = btn.getAttribute('data-ins') || '';
    const start = input.selectionStart;
    const end = input.selectionEnd;
    input.value = input.value.substring(0, start) + ins + input.value.substring(end);
    input.selectionStart = input.selectionEnd = start + ins.length;
    input.focus();
    updateView();
    if (window.MangoBridge) window.MangoBridge.onContentChanged(input.value);
  });

  window.MangoEditor = {
    setContent: function(text, lang) {
      currentLang = (lang || 'kt').toLowerCase();
      input.value = text || '';
      updateView();
      input.scrollTop = 0;
      syncScroll();
    },
    getContent: function() {
      return input.value;
    },
    setFontSize: function(px) {
      const s = Math.max(10, Math.min(26, Number(px) || 14)) + 'px';
      input.style.fontSize = s;
      hl.style.fontSize = s;
      gutter.style.fontSize = s;
    }
  };

  updateView();
})();
