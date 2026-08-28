/* Panel — interakcje CRM: tryb odpowiedzi, szablony, tagi notatek.

   Wszystko, co zmienia dane, idzie zwykłym formularzem (POST + przeładowanie).
   JavaScript odpowiada tu wyłącznie za wygodę: przełączenie trybu, wstawienie
   szablonu, zaznaczenie tagu. Dzięki temu ekran działa także bez niego. */
(function () {
  'use strict';
  const $  = (s, c = document) => c.querySelector(s);
  const $$ = (s, c = document) => Array.from(c.querySelectorAll(s));

  const tx = $('#cmp-tx');

  /* ── Tryb odpowiedzi: e-mail albo notatka z telefonu ──────────────────
     Tryb telefoniczny nic nie wysyła do klienta, więc podpowiedź w polu
     musi to mówić wprost — inaczej łatwo wysłać notatkę jako wiadomość. */
  const wayField = $('#cmp-way');
  const ways = $$('.way');
  ways.forEach(b => b.addEventListener('click', () => {
    ways.forEach(x => x.classList.toggle('on', x === b));
    const mode = b.dataset.way;
    if (wayField) wayField.value = mode;
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
