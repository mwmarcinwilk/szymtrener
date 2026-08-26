/* =====================================================================
   EDYTOR POSTA · Quill 2 + własne bloki mediów (zdjęcie, film, PDF)

   Odtworzenie makiety `js/admin-editor.js` z handoffu. Zmiany wobec wzorca są
   wyłącznie tam, gdzie makieta korzystała z danych przykładowych:
   biblioteka mediów idzie z /admin/api/media, treść startowa z bazy,
   a zapis i autozapis z panelu.

   Bloki w edytorze noszą klasy `q-fig` / `q-vid` / `q-pdf` — tak wygląda makieta.
   Zamianę na HTML publikacji (`figure`, `art-video yt-facade`, `art-pdf`) robi
   serwer w `EditorHtml`, wołany z `PostService.save()`. Tutaj nie zamieniamy nic.
   ===================================================================== */
(function () {
  'use strict';
  if (typeof Quill === 'undefined') return;
  const $ = (s, c = document) => c.querySelector(s);
  const $$ = (s, c = document) => Array.from(c.querySelectorAll(s));
  const form = $('#post-form');
  if (!form) return;

  const csrfToken = $('meta[name="_csrf"]')?.content;
  const csrfHeader = $('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
  const headers = extra => Object.assign({}, extra || {}, csrfToken ? { [csrfHeader]: csrfToken } : {});

  const ACTS = '<span class="q-acts" contenteditable="false"><button type="button" class="q-act" data-act="edit" title="Edytuj">✎</button><button type="button" class="q-act" data-act="del" title="Usuń">✕</button></span>';
  const esc = s => String(s == null ? '' : s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));


  /* Komunikaty pokazujemy w oknie panelu, nie natywnym alertem przeglądarki
     (Chrome podpisuje je „Strona … mówi" i nie da się ich ostylować).
     Dialog dostarcza admin.js; gdyby go zabrakło, schodzimy na natywne okno,
     żeby komunikat nie zniknął bez śladu. */
  function say(title, message) {
    if (window.sdDialog) return window.sdDialog.alert({ title: title, message: message });
    window.alert(title + '\n\n' + message);
    return Promise.resolve();
  }
  say.confirm = function (options) {
    if (window.sdDialog) return window.sdDialog.confirm(options);
    const list = (options.items || []).map(t => '· ' + t).join('\n');
    return Promise.resolve(window.confirm(options.message + '\n\n' + list));
  };

  /* ── blot: zdjęcie z podpisem (odpowiada <figure> na blogu) ── */
  const BlockEmbed = Quill.import('blots/block/embed');

  class FigureBlot extends BlockEmbed {
    static create(v) {
      const n = super.create();
      n.setAttribute('data-src', v.src || '');
      n.setAttribute('data-alt', v.alt || '');
      n.setAttribute('data-caption', v.caption || '');
      n.setAttribute('contenteditable', 'false');
      n.innerHTML = '<img src="' + esc(v.src) + '" alt="' + esc(v.alt) + '">' +
        (v.caption ? '<figcaption>' + esc(v.caption) + '</figcaption>' : '') +
        '<span class="q-alt' + (v.alt ? '' : ' warn') + '">' + (v.alt ? 'alt: ' + esc(v.alt) : 'brak tekstu alternatywnego') + '</span>' + ACTS;
      return n;
    }
    static value(n) {
      return { src: n.getAttribute('data-src'), alt: n.getAttribute('data-alt'), caption: n.getAttribute('data-caption') };
    }
  }
  FigureBlot.blotName = 'figureImage'; FigureBlot.tagName = 'figure'; FigureBlot.className = 'q-fig';

  /* ── blot: film (miniatura + play, jak na blogu) ── */
  class VideoBlot extends BlockEmbed {
    static create(v) {
      const n = super.create();
      n.setAttribute('data-id', v.id || '');
      n.setAttribute('data-caption', v.caption || '');
      n.setAttribute('contenteditable', 'false');
      n.innerHTML = '<span class="q-vid-thumb"><img src="https://i.ytimg.com/vi/' + esc(v.id) + '/hqdefault.jpg" alt=""><span class="q-play">▶</span></span>' +
        '<span class="q-vid-tx"><b>Film: ' + esc(v.caption || 'YouTube') + '</b><i>Osadzenie bez cookies · miniatura ładowana leniwie</i></span>' + ACTS;
      return n;
    }
    static value(n) { return { id: n.getAttribute('data-id'), caption: n.getAttribute('data-caption') }; }
  }
  VideoBlot.blotName = 'ytVideo'; VideoBlot.tagName = 'div'; VideoBlot.className = 'q-vid';

  /* ── blot: PDF do pobrania ── */
  class PdfBlot extends BlockEmbed {
    static create(v) {
      const n = super.create();
      n.setAttribute('data-name', v.name || '');
      n.setAttribute('data-label', v.label || '');
      n.setAttribute('data-meta', v.meta || '');
      n.setAttribute('data-url', v.url || '');
      n.setAttribute('data-media-id', v.id || '');
      n.setAttribute('contenteditable', 'false');
      n.innerHTML = '<span class="q-pdf-ic">PDF</span><span class="q-pdf-tx"><b>' + esc(v.label || v.name) + '</b><i>' + esc(v.name) + ' · ' + esc(v.meta) + '</i></span>' + ACTS;
      return n;
    }
    static value(n) {
      return {
        name: n.getAttribute('data-name'), label: n.getAttribute('data-label'),
        meta: n.getAttribute('data-meta'), url: n.getAttribute('data-url'),
        id: n.getAttribute('data-media-id')
      };
    }
  }
  PdfBlot.blotName = 'pdfFile'; PdfBlot.tagName = 'div'; PdfBlot.className = 'q-pdf';

  Quill.register(FigureBlot, true);
  Quill.register(VideoBlot, true);
  Quill.register(PdfBlot, true);

  /* ── inicjalizacja ── */
  const quill = new Quill('#q-editor', {
    theme: 'snow',
    placeholder: 'Zacznij od odpowiedzi na pytanie z tytułu — pierwszy akapit trafia do wyników Google i odpowiedzi AI…',
    modules: {
      toolbar: { container: '#q-toolbar' },
      history: { delay: 800, maxStack: 200, userOnly: true },
      clipboard: { matchVisual: false }
    }
  });
  window.sdQuill = quill;

  /* treść z bazy zamiast przykładowej z makiety */
  const contentField = $('#contentHtml');
  if (contentField && contentField.value.trim()) {
    quill.clipboard.dangerouslyPasteHTML(contentField.value, 'silent');
  }

  /* ── biblioteka mediów z bazy (w makiecie były stałe LIB_IMG / LIB_PDF) ── */
  let libraryCache = null;
  async function library() {
    if (!libraryCache) {
      const response = await fetch('/admin/api/media');
      libraryCache = response.ok ? await response.json() : [];
    }
    return libraryCache;
  }
  async function upload(file, alt) {
    const data = new FormData();
    data.append('file', file);
    if (alt) data.append('alt', alt);
    const response = await fetch('/admin/api/media', { method: 'POST', headers: headers(), body: data });
    if (!response.ok) throw new Error('Nie udało się wgrać pliku');
    const saved = await response.json();
    if (libraryCache) libraryCache.unshift(saved);
    return saved;
  }
  function pickFile(accept, handler) {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = accept;
    input.onchange = () => { if (input.files[0]) handler(input.files[0]); };
    input.click();
  }

  /* ══ modal wstawiania mediów ══ */
  const ov = $('#q-ov');
  let mode = 'image', editingBlot = null, pickedImg = null, pickedPdf = null;

  function openModal(kind, preset) {
    mode = kind; editingBlot = preset && preset.blot || null;
    ov.dataset.panel = kind;
    ov.classList.add('on');
    $$('.q-pane').forEach(p => p.classList.toggle('on', p.dataset.pane === kind));
    if (kind === 'image') {
      pickedImg = preset && preset.src || null;
      $('#q-alt').value = preset && preset.alt || '';
      $('#q-cap').value = preset && preset.caption || '';
      renderImages();
    }
    if (kind === 'video') {
      $('#q-url').value = preset && preset.id ? 'https://www.youtube.com/watch?v=' + preset.id : '';
      $('#q-vcap').value = preset && preset.caption || '';
      syncVideo();
    }
    if (kind === 'pdf') {
      pickedPdf = preset && preset.url || null;
      $('#q-plabel').value = preset && preset.label || '';
      renderPdfs();
    }
  }
  function closeModal() { ov.classList.remove('on'); editingBlot = null; }

  $$('[data-ins]').forEach(b => b.addEventListener('click', e => { e.preventDefault(); openModal(b.dataset.ins); }));
  $$('[data-close]').forEach(b => b.addEventListener('click', closeModal));
  ov.addEventListener('click', e => { if (e.target === ov) closeModal(); });
  document.addEventListener('keydown', e => { if (e.key === 'Escape' && ov.classList.contains('on')) closeModal(); });

  /* zakładki biblioteka / wgrywanie */
  $$('.q-tabs').forEach(g => $$('.q-tab', g).forEach(t => t.addEventListener('click', () => {
    $$('.q-tab', g).forEach(x => x.classList.remove('on'));
    t.classList.add('on');
    const pane = t.closest('.q-pane');
    $$('.q-tabbody', pane).forEach(b => b.classList.toggle('on', b.dataset.tab === t.dataset.tab));
  })));

  async function renderImages() {
    const files = (await library()).filter(f => f.kind === 'IMAGE');
    $('#q-lib').innerHTML = files.map(i =>
      '<button type="button" class="q-libi' + (i.url === pickedImg ? ' sel' : '') + '" data-src="' + esc(i.url) +
      '" data-alt="' + esc(i.alt || '') + '"><img src="' + esc(i.url) + '" alt=""><span>' + esc(i.name) + '</span></button>').join('')
      || '<p style="font-size:12px;color:var(--ink-3)">Biblioteka jest pusta — wgraj plik w drugiej zakładce.</p>';
    $$('#q-lib .q-libi').forEach(b => b.addEventListener('click', () => {
      pickedImg = b.dataset.src;
      $$('#q-lib .q-libi').forEach(x => x.classList.toggle('sel', x === b));
      if (!$('#q-alt').value.trim() && b.dataset.alt) $('#q-alt').value = b.dataset.alt;
    }));
  }

  async function renderPdfs() {
    const files = (await library()).filter(f => f.kind === 'PDF');
    $('#q-pdflib').innerHTML = files.map(p =>
      '<button type="button" class="q-libr' + (p.url === pickedPdf ? ' sel' : '') + '" data-url="' + esc(p.url) +
      '" data-id="' + p.id + '" data-name="' + esc(p.name) + '" data-meta="' + esc(p.size) +
      '"><span class="ic">PDF</span><span class="tx"><b>' + esc(p.name) + '</b><i>' + esc(p.size) + '</i></span></button>').join('')
      || '<p style="font-size:12px;color:var(--ink-3)">Brak plików PDF — wgraj jeden w drugiej zakładce.</p>';
    $$('#q-pdflib .q-libr').forEach(b => b.addEventListener('click', () => {
      pickedPdf = b.dataset.url;
      $$('#q-pdflib .q-libr').forEach(x => x.classList.toggle('sel', x === b));
    }));
  }

  $('#q-drop-img').addEventListener('click', () => pickFile('image/jpeg,image/png,image/webp', async file => {
    try {
      const saved = await upload(file, $('#q-alt').value.trim());
      pickedImg = saved.url;
      $('.q-pane[data-pane="image"] .q-tab[data-tab="lib"]').click();
      renderImages();
    } catch (e) { say('Nie udało się wgrać zdjęcia', e.message); }
  }));
  $('#q-drop-pdf').addEventListener('click', () => pickFile('application/pdf', async file => {
    try {
      const saved = await upload(file, null);
      pickedPdf = saved.url;
      $('.q-pane[data-pane="pdf"] .q-tab[data-tab="lib"]').click();
      renderPdfs();
    } catch (e) { say('Nie udało się wgrać pliku', e.message); }
  }));

  /* film — rozpoznanie adresu */
  function ytId(u) {
    const m = String(u).match(/(?:youtu\.be\/|v=|embed\/|shorts\/)([A-Za-z0-9_-]{11})/);
    return m ? m[1] : (/^[A-Za-z0-9_-]{11}$/.test(String(u).trim()) ? String(u).trim() : '');
  }
  function syncVideo() {
    const id = ytId($('#q-url').value);
    $('#q-vprev').innerHTML = id
      ? '<img src="https://i.ytimg.com/vi/' + id + '/hqdefault.jpg" alt=""><span class="ok">Rozpoznano film: ' + id + '</span>'
      : '<span class="bad">Wklej adres filmu z YouTube</span>';
    $('#q-vok').disabled = !id;
  }
  $('#q-url').addEventListener('input', syncVideo);

  /* wstawianie / aktualizacja */
  function place(name, value) {
    if (editingBlot) {
      const i = quill.getIndex(editingBlot);
      quill.deleteText(i, 1, 'user');
      quill.insertEmbed(i, name, value, 'user');
      quill.setSelection(i + 1, 0);
    } else {
      const r = quill.getSelection(true) || { index: quill.getLength() - 1 };
      quill.insertEmbed(r.index, name, value, 'user');
      quill.setSelection(r.index + 1, 0);
    }
    closeModal(); touch();
  }
  $('#q-iok').addEventListener('click', () => {
    if (!pickedImg) { say('Nie wybrano zdjęcia', 'Wskaż zdjęcie w bibliotece albo wgraj nowe w drugiej zakładce.'); return; }
    place('figureImage', { src: pickedImg, alt: $('#q-alt').value.trim(), caption: $('#q-cap').value.trim() });
  });
  $('#q-vok').addEventListener('click', () => {
    const id = ytId($('#q-url').value);
    if (id) place('ytVideo', { id: id, caption: $('#q-vcap').value.trim() });
  });
  $('#q-pok').addEventListener('click', () => {
    if (!pickedPdf) { say('Nie wybrano pliku', 'Wskaż PDF w bibliotece albo wgraj nowy w drugiej zakładce.'); return; }
    const src = (libraryCache || []).find(p => p.url === pickedPdf) || {};
    place('pdfFile', {
      url: pickedPdf, id: src.id, name: src.name || 'plik.pdf',
      label: $('#q-plabel').value.trim() || src.name, meta: src.size || 'PDF'
    });
  });

  /* akcje na wstawionych blokach */
  quill.root.addEventListener('click', e => {
    const act = e.target.closest('.q-act');
    if (!act) return;
    e.preventDefault(); e.stopPropagation();
    const node = act.closest('.q-fig, .q-vid, .q-pdf');
    const blot = Quill.find(node);
    if (!blot) return;
    if (act.dataset.act === 'del') { quill.deleteText(quill.getIndex(blot), 1, 'user'); touch(); return; }
    if (node.classList.contains('q-fig')) openModal('image', { blot: blot, src: node.dataset.src, alt: node.dataset.alt, caption: node.dataset.caption });
    else if (node.classList.contains('q-vid')) openModal('video', { blot: blot, id: node.dataset.id, caption: node.dataset.caption });
    else openModal('pdf', { blot: blot, url: node.dataset.url, label: node.dataset.label });
  });

  /* ══ import z Worda (funkcja aplikacji, poza makietą) ══ */
  $('#btn-import')?.addEventListener('click', () => pickFile('.docx,.doc', async file => {
    const status = $('#import-status');
    status.textContent = 'Wczytuję dokument…';
    try {
      const data = new FormData();
      data.append('file', file);
      const response = await fetch('/admin/api/import-docx', { method: 'POST', headers: headers(), body: data });
      const result = await response.json();
      if (!result.ok) throw new Error(result.message || 'Nie udało się wczytać dokumentu');
      const r = quill.getSelection(true);
      quill.clipboard.dangerouslyPasteHTML(r ? r.index : quill.getLength(), result.html, 'user');
      status.textContent = 'Przeniesiono treść' + (result.images ? ' i ' + result.images + ' obrazk(i)' : '') + '.';
      touch();
    } catch (e) { status.textContent = e.message; status.classList.add('msg'); }
  }));

  /* ══ statystyki, autozapis, ocena widoczności ══ */
  const elWords = $('#q-words'), elChars = $('#q-chars'), elRead = $('#q-read'), elSave = $('#q-save');
  const idField = $('#post-id'), statusField = $('#q-status');
  let dirty = false, submitting = false;

  function stats() {
    const txt = quill.getText().replace(/\s+/g, ' ').trim();
    const w = txt ? txt.split(' ').length : 0;
    elWords.textContent = w;
    elChars.textContent = txt.length;
    elRead.textContent = Math.max(1, Math.ceil(w / 200)) + ' min';
    return w;
  }
  function touch() { dirty = true; elSave.textContent = 'Niezapisane zmiany'; elSave.className = 'q-save warn'; stats(); score(); }
  function saved(label) { dirty = false; elSave.textContent = label; elSave.className = 'q-save ok'; }

  function syncHidden() {
    $('#contentHtml').value = quill.root.innerHTML;
    $('#contentDelta').value = JSON.stringify(quill.getContents());
  }

  function payload() {
    syncHidden();
    const value = id => $('#' + id)?.value?.trim() || '';
    const collect = name => $$('[name="' + name + '"]').map(f => f.value);
    return {
      id: idField.value || null,
      title: value('q-title'), slug: value('q-slug'), lead: value('q-excerpt'),
      contentHtml: $('#contentHtml').value, contentDelta: $('#contentDelta').value,
      categoryId: value('q-cat') || null, coverMediaId: value('coverMediaId') || null,
      coverAlt: value('cover-alt'), coverCaption: value('cover-caption'),
      status: value('q-status'), publishAt: value('q-when'), tags: value('q-tags'),
      seoTitle: value('seo-title'), seoDescription: value('seo-desc'),
      summaryPoints: collect('summaryPoints'),
      faqQuestions: collect('faqQuestions'), faqAnswers: collect('faqAnswers')
    };
  }

  /* Autozapis co 15 s. Nowy wpis: pierwszy autozapis zakłada szkic i zwraca id. */
  async function autosave() {
    if (!dirty || submitting) return;
    const id = idField.value;
    try {
      const response = await fetch('/admin/posty/' + (id ? id + '/autozapis' : 'autozapis'), {
        method: 'POST', headers: headers({ 'Content-Type': 'application/json' }),
        body: JSON.stringify(payload())
      });
      const result = await response.json();
      if (!result.ok) { elSave.textContent = result.reason || 'Autozapis czeka'; elSave.className = 'q-save warn'; return; }
      if (!idField.value && result.id) idField.value = result.id;
      saved('Szkic zapisany ' + String(result.savedAt).slice(11, 16));
    } catch (e) {
      elSave.textContent = 'Autozapis nie przeszedł';
      elSave.className = 'q-save err';
    }
  }
  setInterval(autosave, 15000);

  quill.on('text-change', (d, o, src) => { if (src === 'user') touch(); });
  form.addEventListener('input', touch);
  form.addEventListener('change', touch);
  form.addEventListener('submit', () => { submitting = true; dirty = false; syncHidden(); });

  document.addEventListener('keydown', e => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') { e.preventDefault(); form.requestSubmit(); }
  });
  window.addEventListener('beforeunload', e => {
    if (!dirty || submitting) return;
    e.preventDefault(); e.returnValue = '';
  });

  /* Publikacja: ostrzeżenie o niespełnionych warunkach, decyzja zostaje przy autorze. */
  $('#q-publish').addEventListener('click', async () => {
    const pending = score().filter(c => !c.ok);
    if (pending.length) {
      const ok = await say.confirm({
        title: 'Opublikować mimo braków?',
        message: 'Ten wpis nie spełnia ' + pending.length + ' z 9 warunków widoczności:',
        items: pending.map(c => c.tx),
        confirmLabel: 'Publikuj mimo to',
        cancelLabel: 'Wróć do edycji'
      });
      if (!ok) return;
    }
    statusField.value = 'PUBLISHED';
    form.requestSubmit();
  });

  /* ── ocena widoczności — lista warunków jak w makiecie ── */
  const seoT = $('#seo-title'), seoD = $('#seo-desc'), seoSlug = $('#q-slug'), coverAlt = $('#cover-alt');
  function setCount(el, val, min, max) {
    el.textContent = val;
    el.className = 'q-cnt ' + (val >= min && val <= max ? 'ok' : 'warn');
  }
  function score() {
    const html = quill.root.innerHTML;
    const words = quill.getText().replace(/\s+/g, ' ').trim().split(' ').filter(Boolean).length;
    const t = seoT.value.trim(), d = seoD.value.trim();
    const checks = [
      { ok: t.length >= 30 && t.length <= 65, tx: 'Tytuł SEO ma 30–65 znaków' },
      { ok: d.length >= 120 && d.length <= 160, tx: 'Meta opis ma 120–160 znaków' },
      { ok: /<h2/i.test(html), tx: 'Treść zawiera nagłówki H2' },
      { ok: /q-fig/.test(html), tx: 'Wstawione zdjęcie z tekstem alternatywnym' },
      { ok: !/brak tekstu alternatywnego/.test(html), tx: 'Wszystkie zdjęcia mają alt' },
      { ok: coverAlt.value.trim().length > 5, tx: 'Zdjęcie główne ma opis alt' },
      { ok: (html.match(/<a /g) || []).length >= 2, tx: 'Co najmniej 2 linki wewnętrzne' },
      { ok: words >= 600, tx: 'Objętość powyżej 600 słów (' + words + ')' },
      { ok: /\d/.test(quill.getText()), tx: 'Dane liczbowe w treści' }
    ];
    const done = checks.filter(c => c.ok).length;
    const pct = Math.round(done / checks.length * 100);
    const ring = $('#seo-ring'), turn = (pct / 100).toFixed(3);
    ring.style.background = 'conic-gradient(var(--mint) 0turn ' + turn + 'turn, var(--line-2) ' + turn + 'turn 1turn)';
    $('#seo-val').textContent = pct;
    $('#seo-label').textContent = pct >= 85 ? 'Dobra widoczność' : pct >= 60 ? 'Wymaga dopracowania' : 'Słaba widoczność';
    $('#seo-hint').textContent = done === checks.length ? 'Wszystkie warunki spełnione.' : 'Do uzupełnienia: ' + (checks.length - done) + ' z ' + checks.length + '.';
    $('#seo-list').innerHTML = checks.map(c =>
      '<div class="seo-row"><span class="ic ' + (c.ok ? 'ok' : 'wr') + '">' + (c.ok ? '✓' : '!') + '</span><span class="tx">' + esc(c.tx) + '</span></div>').join('');
    setCount($('#c-title'), t.length, 30, 65);
    setCount($('#c-desc'), d.length, 120, 160);
    $('#serp-t').textContent = t || 'Tytuł posta';
    $('#serp-d').textContent = d || 'Meta opis pojawi się tutaj.';
    $('#serp-u').textContent = 'szymtrener.pl › blog › ' + (seoSlug.value.trim() || 'adres-posta');
    return checks;
  }
  [seoT, seoD, seoSlug, coverAlt].forEach(el => el.addEventListener('input', score));

  /* tytuł -> podpowiedź adresu i tytułu SEO */
  const title = $('#q-title');
  title.addEventListener('input', () => {
    if (!seoSlug.dataset.touched) {
      seoSlug.value = title.value.toLowerCase()
        .replace(/[ąćęłńóśźż]/g, c => ({ 'ą': 'a', 'ć': 'c', 'ę': 'e', 'ł': 'l', 'ń': 'n', 'ó': 'o', 'ś': 's', 'ź': 'z', 'ż': 'z' }[c]))
        .replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 60);
    }
    if (!seoT.dataset.touched) seoT.value = title.value;
    score();
  });
  seoSlug.addEventListener('input', () => { seoSlug.dataset.touched = '1'; });
  seoT.addEventListener('input', () => { seoT.dataset.touched = '1'; });

  /* ── zdjęcie główne ── */
  (function cover() {
    const idInput = $('#coverMediaId'), box = $('#cover-set'), thumb = $('#cover-thumb');
    const pick = $('#cover-pick'), overlay = $('#cover-ov');
    function show(file) {
      if (!file) { box.hidden = true; pick.hidden = false; return; }
      thumb.src = file.url; thumb.alt = file.alt || file.name;
      box.hidden = false; pick.hidden = true;
    }
    async function open() {
      overlay.classList.add('on');
      const files = (await library()).filter(f => f.kind === 'IMAGE');
      $('#cover-lib').innerHTML = files.map(f =>
        '<button type="button" class="q-libi" data-id="' + f.id + '"><img src="' + esc(f.url) + '" alt=""><span>' +
        esc(f.name) + '</span></button>').join('')
        || '<p style="font-size:12px;color:var(--ink-3)">Biblioteka jest pusta. <a href="/admin/media" target="_blank">Wgraj zdjęcie</a>.</p>';
      $$('#cover-lib .q-libi').forEach(b => b.addEventListener('click', () => {
        const file = files.find(f => String(f.id) === b.dataset.id);
        idInput.value = file.id;
        if (!coverAlt.value.trim() && file.alt) coverAlt.value = file.alt;
        show(file); overlay.classList.remove('on'); touch();
      }));
    }
    pick.addEventListener('click', open);
    $('#cover-change').addEventListener('click', open);
    $('#cover-clear').addEventListener('click', () => { idInput.value = ''; show(null); touch(); });
    $$('[data-cover-close]').forEach(b => b.addEventListener('click', () => overlay.classList.remove('on')));
    overlay.addEventListener('click', e => { if (e.target === overlay) overlay.classList.remove('on'); });
    if (idInput.value) library().then(files => show(files.find(f => String(f.id) === idInput.value) || null));
  })();

  /* ── listy dynamiczne: „W skrócie" i FAQ ── */
  $$('.addrow').forEach(button => button.addEventListener('click', () => {
    const target = $('#' + button.dataset.target);
    const copy = target.querySelector('.rowline').cloneNode(true);
    copy.querySelectorAll('input, textarea').forEach(f => { f.value = ''; });
    target.appendChild(copy);
    bindRemovals();
  }));
  function bindRemovals() {
    $$('.rowset .rm').forEach(button => {
      button.onclick = () => {
        const set = button.closest('.rowset');
        if (set.querySelectorAll('.rowline').length > 1) button.closest('.rowline').remove();
        else set.querySelectorAll('input, textarea').forEach(f => { f.value = ''; });
        touch();
      };
    });
  }
  bindRemovals();

  stats(); score(); saved('Bez zmian');
})();
