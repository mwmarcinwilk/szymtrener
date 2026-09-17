/* =====================================================================
   Zgoda na ciasteczka. Bez tego pliku wszystko działa zwykłym POST-em
   i osobną stroną /ustawienia-cookies. Tu tylko wygoda: okno zamiast
   przejścia na stronę, zakładki i zapis w tle bez przeładowania.
   ===================================================================== */
(function () {
  'use strict';

  function initTabs(panel) {
    const list = panel.querySelector('.consent-tabs');
    if (!list) return;
    const tabs = Array.from(list.querySelectorAll('[role="tab"]'));
    const sections = tabs.map(tab => document.getElementById(tab.getAttribute('aria-controls')));

    function select(index, focus) {
      tabs.forEach((tab, i) => {
        const on = i === index;
        tab.setAttribute('aria-selected', String(on));
        tab.tabIndex = on ? 0 : -1;
        sections[i].hidden = !on;
      });
      if (focus) tabs[index].focus();
    }

    tabs.forEach((tab, i) => {
      tab.addEventListener('click', () => select(i, false));
      tab.addEventListener('keydown', event => {
        const step = { ArrowRight: 1, ArrowLeft: -1 }[event.key];
        if (!step) return;
        event.preventDefault();
        select((i + step + tabs.length) % tabs.length, true);
      });
    });
    list.hidden = false;
    select(0, false);
  }

  document.querySelectorAll('.consent-panel').forEach(initTabs);

  const bar = document.querySelector('.consent-bar');
  const dialog = document.querySelector('.consent-dialog');
  if (!dialog || typeof dialog.showModal !== 'function') return;
  const dialogForm = dialog.querySelector('form');
  const toggle = dialogForm.querySelector('input[name="statystyka"]');

  /* Ten sam formularz, wysłany w tle. Przy błędzie zwykły POST, żeby wybór nie przepadł. */
  async function send(form, event) {
    const submitter = event.submitter;
    if (!submitter || form.dataset.native) return;
    event.preventDefault();
    const data = new FormData(form, submitter);
    try {
      const res = await fetch(form.action, {
        method: 'POST',
        headers: { 'X-Requested-With': 'fetch' },
        body: new URLSearchParams(data),
        credentials: 'same-origin'
      });
      if (!res.ok) throw new Error(String(res.status));
    } catch (_) {
      form.dataset.native = '1';
      form.requestSubmit(submitter);
      return;
    }
    const granted = submitter.value === 'tak' || (submitter.value === 'zapisz' && data.has('statystyka'));
    if (toggle) toggle.checked = granted;
    if (bar) bar.hidden = true;
    if (dialog.open) dialog.close();
  }

  if (bar) bar.addEventListener('submit', event => send(bar, event));
  dialogForm.addEventListener('submit', event => send(dialogForm, event));

  /* Stan przełącznika sprzed otwarcia: zamknięcie bez zapisu go przywraca. */
  let saved = null;
  document.querySelectorAll('[data-consent-open]').forEach(link => {
    link.addEventListener('click', event => {
      event.preventDefault();
      saved = toggle ? toggle.checked : null;
      dialog.showModal();
    });
  });

  function restore() {
    if (toggle && saved !== null) toggle.checked = saved;
    saved = null;
  }

  dialog.querySelector('[data-consent-close]').addEventListener('click', () => { restore(); dialog.close(); });
  dialog.addEventListener('cancel', restore);
  dialog.addEventListener('click', event => {
    if (event.target === dialog) { restore(); dialog.close(); }
  });
})();
