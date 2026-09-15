/* Panel — interakcje CRM: tryb odpowiedzi, szablony, tagi notatek.

   Wszystko, co zmienia dane, idzie zwykłym formularzem (POST + przeładowanie).
   JavaScript odpowiada tu wyłącznie za wygodę: przełączenie trybu, wstawienie
   szablonu, zaznaczenie tagu. Dzięki temu ekran działa także bez niego. */
(function () {
  'use strict';
  const $  = (s, c = document) => c.querySelector(s);
  const $$ = (s, c = document) => Array.from(c.querySelectorAll(s));

  const tx = $('#cmp-tx');

  /* ── Załączniki ────────────────────────────────────────────────────────
     Lista wybranych plików z rozmiarem i przyciskiem usunięcia. Limity pilnuje
     serwer; tu tylko ostrzeżenie, zanim trener wyśle za dużo. */
  const fileInput = $('#cmp-pliki');
  const chosen = $('#cmp-chosen');
  const picker = $('#cmp-files');
  const maxFiles = Number(picker?.dataset.maxFiles || 5);
  const maxBytes = Number(picker?.dataset.maxBytes || 20 * 1048576);
  const maxFile = Number(picker?.dataset.maxFile || 10 * 1048576);

  /* Za duzy upload odbija sie od filtra CSRF (Tomcat nie czyta wtedy pol formularza) i trener
     dostaje 403 z utrata tresci. Dlatego limity blokuja wyslanie juz w przegladarce; serwer
     i tak sprawdza je ponownie. */
  function overLimit() {
    if (!fileInput) return null;
    const files = Array.from(fileInput.files);
    const big = files.find(f => f.size > maxFile);
    if (files.length > maxFiles) return 'Najwyżej ' + maxFiles + ' plików';
    if (big) return big.name + ': więcej niż ' + size(maxFile);
    if (files.reduce((s, f) => s + f.size, 0) > maxBytes) return 'Razem ponad ' + size(maxBytes);
    return null;
  }
  fileInput?.form?.addEventListener('submit', e => {
    if (overLimit()) { e.preventDefault(); renderChosen(); chosen?.lastElementChild?.scrollIntoView({ block: 'nearest' }); }
  });
  // Ten sam format co Bytes.human() na serwerze.
  const size = b => b < 1024 ? b + ' B'
    : b < 1048576 ? Math.round(b / 1024) + ' KB'
    : (b / 1048576).toFixed(1).replace('.', ',') + ' MB';
  function renderChosen() {
    if (!chosen || !fileInput) return;
    chosen.replaceChildren();
    const files = Array.from(fileInput.files);
    files.forEach((f, i) => {
      const li = document.createElement('li');
      const name = document.createElement('span');
      name.textContent = f.name;
      const sz = document.createElement('i');
      sz.textContent = size(f.size);
      const rm = document.createElement('button');
      rm.type = 'button';
      rm.textContent = '×';
      rm.setAttribute('aria-label', 'Usuń ' + f.name);
      rm.addEventListener('click', () => {
        const dt = new DataTransfer();
        Array.from(fileInput.files).forEach((g, j) => { if (j !== i) dt.items.add(g); });
        fileInput.files = dt.files;
        renderChosen();
      });
      li.append(name, sz, rm);
      chosen.append(li);
    });
    const problem = overLimit();
    if (problem) {
      const li = document.createElement('li');
      li.className = 'err';
      li.textContent = problem;
      chosen.append(li);
    }
  }
  fileInput?.addEventListener('change', renderChosen);

  /* ── Tryb odpowiedzi: e-mail albo notatka z telefonu ──────────────────
     Tryb telefoniczny nic nie wysyła do klienta, więc podpowiedź w polu
     musi to mówić wprost — inaczej łatwo wysłać notatkę jako wiadomość. */
  const wayField = $('#cmp-way');
  const ways = $$('.way');
  ways.forEach(b => b.addEventListener('click', () => {
    ways.forEach(x => x.classList.toggle('on', x === b));
    const mode = b.dataset.way;
    if (wayField) wayField.value = mode;
    // Rozmowa telefoniczna nie wysyla plikow, wiec pole znika, a wybrane pliki sa czyszczone.
    if (picker) {
      picker.classList.toggle('off', mode === 'tel');
      if (mode === 'tel' && fileInput) { fileInput.value = ''; renderChosen(); }
    }
    if (!tx) return;
    tx.placeholder = mode === 'tel'
      ? 'Zapisz, co ustaliliście przez telefon — trafi do wątku, ale nie zostanie wysłane do klienta…'
      : tx.dataset.mailPlaceholder || 'Napisz wiadomość…';
  }));
  if (tx) tx.dataset.mailPlaceholder = tx.placeholder;

  /* ── Szablony odpowiedzi ──────────────────────────────────────────────
     Treść i podstawienia (imię, kontekst) robi serwer: szablony siedzą
     w bazie, żeby trener mógł je poprawiać bez wdrożenia. */
  const endpoint = $('.cmp-top')?.dataset.templates;
  $$('.tpl').forEach(b => b.addEventListener('click', async () => {
    if (!tx || !endpoint) return;
    const original = b.textContent;
    b.disabled = true;
    try {
      const res = await fetch(endpoint + '/' + b.dataset.tpl, { headers: { 'Accept': 'application/json' } });
      const data = await res.json();
      tx.value = data.body || '';
      tx.focus();
      tx.setSelectionRange(tx.value.length, tx.value.length);
    } catch (_) {
      b.textContent = 'Nie udało się wczytać';
      setTimeout(() => { b.textContent = original; }, 2000);
    } finally {
      b.disabled = false;
    }
  }));

  /* „Odpisz" przewija do pola odpowiedzi zamiast otwierać program pocztowy. */
  const focusBtn = $('#focus-reply');
  if (focusBtn) focusBtn.addEventListener('click', () => {
    const c = $('#composer');
    if (c) window.scrollTo({ top: c.getBoundingClientRect().top + window.scrollY - 90, behavior: 'smooth' });
    if (tx) setTimeout(() => tx.focus(), 350);
  });

  /* ── Tagi notatek ─────────────────────────────────────────────────────
     Etykieta z ukrytym polem wyboru: klik przełącza wygląd i wartość naraz,
     więc formularz działa tak samo z JS i bez niego. */
  $$('.nt-chip').forEach(label => {
    const box = label.querySelector('input[type="checkbox"]');
    if (!box) return;
    const sync = () => label.classList.toggle('on', box.checked);
    box.addEventListener('change', sync);
    sync();
  });
})();
