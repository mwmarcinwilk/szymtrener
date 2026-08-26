/* =====================================================================
   Panel administracyjny — zachowania wspólne dla wszystkich ekranów.

   W makiecie panel był jedną stroną z sekcjami `.view` przełączanymi w JS.
   W aplikacji każdy ekran ma własny adres i własny szablon, więc zgodnie ze
   zleceniem (sekcja 7) zostaje tu tylko: hamburger, zamykanie menu i okna
   dialogowe. Przełączanie widoków, mapa tytułów i localStorage są usunięte —
   podświetlenie aktywnej pozycji i stan filtrów renderuje serwer.
   ===================================================================== */
(function () {
  'use strict';
  const $ = (s, c = document) => c.querySelector(s);
  const $$ = (s, c = document) => Array.from(c.querySelectorAll(s));

  /* ── menu na wąskim ekranie ── */
  const burger = $('#burger');
  if (burger) burger.addEventListener('click', () => document.body.classList.toggle('nav-open'));
  document.addEventListener('click', e => {
    if (document.body.classList.contains('nav-open') && !e.target.closest('.side') && !e.target.closest('#burger')) {
      document.body.classList.remove('nav-open');
    }
  });

  /* ═══════════════════════════════════════════════════════════════
     OKNA DIALOGOWE

     Zastępują natywne alert() i confirm(). Natywne okna przeglądarki
     wychodzą poza szatę panelu (Chrome podpisuje je „Strona … mówi"),
     nie da się ich stylować ani przetłumaczyć, a na komunikat o kilku
     niespełnionych warunkach nie ma w nich miejsca.

     Zwracają Promise, więc wołający czyta je tak samo jak natywne:
       if (await sdDialog.confirm({ … })) { … }
     ═══════════════════════════════════════════════════════════════ */
  const dialog = (function () {
    let open = null;

    function build({ title, message, items, confirmLabel, cancelLabel, danger, kind }) {
      const overlay = document.createElement('div');
      overlay.className = 'q-ov on';
      overlay.setAttribute('role', 'dialog');
      overlay.setAttribute('aria-modal', 'true');

      const box = document.createElement('div');
      box.className = 'q-mod q-dlg';
      overlay.appendChild(box);

      const head = document.createElement('div');
      head.className = 'q-mh';
      const heading = document.createElement('h3');
      heading.textContent = title;
      heading.id = 'sd-dlg-title';
      overlay.setAttribute('aria-labelledby', heading.id);
      head.appendChild(heading);

      const close = document.createElement('button');
      close.type = 'button';
      close.className = 'x';
      close.setAttribute('aria-label', 'Zamknij');
      close.textContent = '✕';
      head.appendChild(close);
      box.appendChild(head);

      const body = document.createElement('div');
      body.className = 'q-mb';
      if (message) {
        const p = document.createElement('p');
        p.textContent = message;      // textContent, nie innerHTML — treść bywa z serwera
        body.appendChild(p);
      }
      if (items && items.length) {
        const list = document.createElement('ul');
        items.forEach(text => {
          const li = document.createElement('li');
          li.textContent = text;
          list.appendChild(li);
        });
        body.appendChild(list);
      }
      box.appendChild(body);

      const foot = document.createElement('div');
      foot.className = 'q-mf';
      let cancel = null;
      if (kind === 'confirm') {
        cancel = document.createElement('button');
        cancel.type = 'button';
        cancel.className = 'btn btn-o';
        cancel.textContent = cancelLabel || 'Anuluj';
        foot.appendChild(cancel);
      }
      const accept = document.createElement('button');
      accept.type = 'button';
      accept.className = 'btn ' + (danger ? 'btn-danger' : 'btn-p');
      accept.textContent = confirmLabel || (kind === 'confirm' ? 'Potwierdź' : 'OK');
      foot.appendChild(accept);
      box.appendChild(foot);

      return { overlay, accept, cancel, close };
    }

    function show(options) {
      // jedno okno naraz — drugie wywołanie zamyka poprzednie zamiast je nakładać
      if (open) open.finish(false);

      const parts = build(options);
      const previouslyFocused = document.activeElement;
      document.body.appendChild(parts.overlay);
      document.body.style.overflow = 'hidden';

      return new Promise(resolve => {
        function finish(result) {
          if (!open) return;
          open = null;
          document.removeEventListener('keydown', onKey, true);
          parts.overlay.remove();
          document.body.style.overflow = '';
          if (previouslyFocused && previouslyFocused.focus) previouslyFocused.focus();
          resolve(result);
        }
        function onKey(e) {
          if (e.key === 'Escape') { e.preventDefault(); finish(false); return; }
          if (e.key !== 'Tab') return;
          // uwięzienie fokusu w oknie: bez tego Tab ucieka na stronę pod spodem
          const focusable = [parts.close, parts.cancel, parts.accept].filter(Boolean);
          const first = focusable[0], last = focusable[focusable.length - 1];
          if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
          else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
        }

        open = { finish };
        parts.accept.addEventListener('click', () => finish(true));
        if (parts.cancel) parts.cancel.addEventListener('click', () => finish(false));
        parts.close.addEventListener('click', () => finish(false));
        parts.overlay.addEventListener('click', e => { if (e.target === parts.overlay) finish(false); });
        document.addEventListener('keydown', onKey, true);
        // Przy akcji nieodwracalnej fokus startuje na „Anuluj": Enter odruchowo
        // wciśnięty zaraz po otwarciu okna nie może skasować danych.
        (options.danger && parts.cancel ? parts.cancel : parts.accept).focus();
      });
    }

    return {
      alert: options => show(Object.assign({ title: 'Komunikat', kind: 'alert' }, options)),
      confirm: options => show(Object.assign({ title: 'Potwierdź', kind: 'confirm' }, options))
    };
  })();

  window.sdDialog = dialog;

  /* ── potwierdzenia przy akcjach nieodwracalnych ──
     Formularz oznacza się atrybutem data-confirm (i opcjonalnie data-confirm-title
     oraz data-confirm-ok). Zamiast onsubmit="return confirm(…)", bo Thymeleaf nie
     wpuszcza zmiennych tekstowych wprost do atrybutów zdarzeń. */
  document.addEventListener('submit', e => {
    const form = e.target.closest('form[data-confirm]');
    if (!form || form.dataset.confirmed === '1') return;
    e.preventDefault();
    dialog.confirm({
      title: form.dataset.confirmTitle || 'Potwierdź',
      message: form.dataset.confirm,
      confirmLabel: form.dataset.confirmOk || 'Usuń',
      danger: true
    }).then(ok => {
      if (!ok) return;
      form.dataset.confirmed = '1';
      form.submit();
    });
  });
})();
