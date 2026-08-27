/* =====================================================================
   SZYMON DOMAGAŁA · main.js
   Interakcje: nawigacja, menu mobilne, reveal, liczniki,
   parallax, magnetyczne przyciski, pasek postępu, formularz
   ===================================================================== */
(function () {
  'use strict';

  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const $  = (s, c = document) => c.querySelector(s);
  const $$ = (s, c = document) => Array.from(c.querySelectorAll(s));

  /* ─── HERO: split title into words for staggered reveal ─── */
  (function splitHero() {
    const title = $('.hero-title');
    if (!title) return;
    title.querySelectorAll('.line').forEach(line => {
      const html = line.innerHTML;
      // keep <em> blocks intact; split on spaces at text level
      const tmp = document.createElement('div');
      tmp.innerHTML = html;
      const out = [];
      let i = 0;
      tmp.childNodes.forEach(node => {
        if (node.nodeType === 3) {
          node.textContent.split(/(\s+)/).forEach(tok => {
            if (tok.trim() === '') { return; }
            out.push(`<span class="word"><span style="--i:${i++}">${tok}</span></span>`);
          });
        } else {
          const cls = node.className ? ' ' + node.className : '';
          out.push(`<span class="word${cls}"><span style="--i:${i++}">${node.textContent}</span></span>`);
        }
      });
      line.innerHTML = out.join('');
    });
  })();

  // trigger hero entrance
  const hero = $('#hero');
  if (hero) requestAnimationFrame(() => requestAnimationFrame(() => hero.classList.add('ready')));

  /* ─── NAV: scrolled state + hide on scroll down ─── */
  const nav = $('#main-nav');
  let lastY = window.scrollY;
  function onNavScroll() {
    const y = window.scrollY;
    nav.classList.toggle('scrolled', y > 40);
    if (y > 420 && y > lastY && !document.body.classList.contains('menu-open')) {
      nav.classList.add('hidden');
    } else {
      nav.classList.remove('hidden');
    }
    lastY = y;
  }

  /* ─── SCROLL PROGRESS BAR ─── */
  const progress = $('.scroll-progress');
  function onProgress() {
    const h = document.documentElement.scrollHeight - window.innerHeight;
    const p = h > 0 ? window.scrollY / h : 0;
    if (progress) progress.style.transform = `scaleX(${p})`;
  }

  /* ─── PARALLAX (hero photo + action band) ─── */
  const heroImg = $('.hero-photo img');
  const bandImg = $('.action-band-img img');
  function onParallax() {
    if (reduceMotion) return;
    const y = window.scrollY;
    if (heroImg && y < window.innerHeight * 1.2) {
      heroImg.style.transform = `translateY(${y * 0.12}px) scale(1.04)`;
    }
    if (bandImg) {
      const band = $('.action-band');
      const r = band.getBoundingClientRect();
      if (r.top < window.innerHeight && r.bottom > 0) {
        const offset = (window.innerHeight - r.top) * 0.06;
        bandImg.style.transform = `translateY(${-offset}px)`;
      }
    }
  }

  /* rAF-throttled scroll handler */
  let ticking = false;
  window.addEventListener('scroll', () => {
    if (!ticking) {
      window.requestAnimationFrame(() => {
        onNavScroll(); onProgress(); onParallax();
        ticking = false;
      });
      ticking = true;
    }
  }, { passive: true });
  onNavScroll(); onProgress();

  /* ─── REVEAL ON SCROLL ─── */
  const revealEls = $$('[data-reveal], .stagger');
  if ('IntersectionObserver' in window && !reduceMotion) {
    const io = new IntersectionObserver((entries) => {
      entries.forEach(e => {
        if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target); }
      });
    }, { threshold: 0.12, rootMargin: '0px 0px -8% 0px' });
    revealEls.forEach(el => io.observe(el));
  } else {
    revealEls.forEach(el => el.classList.add('in'));
  }

  /* ─── COUNTERS ─── */
  function animateCount(el) {
    const raw = el.dataset.count;
    const num = parseFloat(raw);
    if (isNaN(num)) { el.textContent = raw; return; }
    const suffix = el.dataset.suffix || '';
    const dur = 1400, start = performance.now();
    function step(now) {
      const t = Math.min((now - start) / dur, 1);
      const eased = 1 - Math.pow(1 - t, 3);
      const val = Math.round(num * eased);
      el.textContent = val + suffix;
      if (t < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
  }
  const counters = $$('[data-count]');
  if ('IntersectionObserver' in window && !reduceMotion) {
    const cio = new IntersectionObserver((entries) => {
      entries.forEach(e => { if (e.isIntersecting) { animateCount(e.target); cio.unobserve(e.target); } });
    }, { threshold: 0.6 });
    counters.forEach(el => cio.observe(el));
  } else {
    counters.forEach(el => { el.textContent = el.dataset.count + (el.dataset.suffix || ''); });
  }

  /* ─── MAGNETIC BUTTONS ─── */
  if (!reduceMotion && window.matchMedia('(pointer:fine)').matches) {
    $$('.btn-primary, .nav-cta .btn').forEach(btn => {
      btn.addEventListener('mousemove', (e) => {
        const r = btn.getBoundingClientRect();
        const x = e.clientX - r.left - r.width / 2;
        const y = e.clientY - r.top - r.height / 2;
        btn.style.transform = `translate(${x * 0.22}px, ${y * 0.32}px)`;
      });
      btn.addEventListener('mouseleave', () => { btn.style.transform = ''; });
    });
  }

  /* ─── MOBILE MENU ─── */
  const ham = $('#hamburger');
  const menu = $('#mobile-menu');
  function closeMenu() { document.body.classList.remove('menu-open'); }
  if (ham) {
    ham.addEventListener('click', () => document.body.classList.toggle('menu-open'));
    $$('#mobile-menu a').forEach(a => a.addEventListener('click', closeMenu));
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeMenu(); });
  }

  /* ─── YOUTUBE FACADE (klik = załaduj film) ─── */
  $$('.yt-facade').forEach(f => {
    const load = () => {
      if (f.classList.contains('loaded')) return;
      const id = f.dataset.id;
      const ifr = document.createElement('iframe');
      ifr.src = `https://www.youtube-nocookie.com/embed/${id}?autoplay=1&rel=0`;
      ifr.title = f.getAttribute('aria-label') || 'YouTube';
      ifr.setAttribute('allow', 'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share');
      ifr.allowFullscreen = true;
      f.innerHTML = '';
      f.appendChild(ifr);
      f.classList.add('loaded');
    };
    f.addEventListener('click', load);
    f.addEventListener('keydown', e => {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); load(); }
    });
  });

  /* ─── OFFER TABS ─── */
  window.showTab = function (id, btn) {
    $$('.offer-panel').forEach(p => p.classList.remove('active'));
    $$('.tab-btn').forEach(b => b.classList.remove('active'));
    const panel = $('#panel-' + id);
    if (panel) panel.classList.add('active');
    btn.classList.add('active');
  };

  /* ─── BACK TO TOP ─── */
  const backTop = $('#backTop');
  if (backTop) {
    backTop.addEventListener('click', () => window.scrollTo({ top: 0, behavior: reduceMotion ? 'auto' : 'smooth' }));
    window.addEventListener('scroll', () => backTop.classList.toggle('show', window.scrollY > 500), { passive: true });
  }

  /* ─── FORMULARZE -> wlasne API (Spring + JavaMail) ───
     Zgloszenie najpierw ladue w bazie, dopiero potem idzie poczta,
     wiec awaria SMTP nie oznacza utraty kontaktu. */
  const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

  /* ── Kontekst oferty przenoszony do formularza ──────────────────────
     Brief 3: Szymon ma wiedzieć, z której ścieżki i z którego pakietu
     przyszło zgłoszenie, ZANIM oddzwoni. CTA niosą to w data-*, a tutaj
     ląduje w ukrytych polach formularza. */
  const PATH_LABELS = {
    KONSULTACJA: 'Konsultacja + Plan treningowy',
    PROWADZENIE: 'Prowadzenie online 1:1'
  };

  const pathField = $('#o-path');
  const packField = $('#o-package');
  const pickBox   = $('#of-pick');
  const pickValue = $('#of-pick-value');

  function setOfferContext(path, pack) {
    if (!pathField || !packField) return;
    pathField.value = path || '';
    packField.value = pack || '';
    if (!pickBox || !pickValue) return;
    const label = PATH_LABELS[path] || '';
    if (!label) { pickBox.hidden = true; return; }
    pickValue.textContent = pack ? label + ' · pakiet ' + pack : label;
    pickBox.hidden = false;
  }

  document.querySelectorAll('a[data-path]').forEach(link => {
    link.addEventListener('click', () => {
      setOfferContext(link.dataset.path, link.dataset.package);
    });
  });

  const pickClear = $('#of-pick-clear');
  if (pickClear) pickClear.addEventListener('click', () => setOfferContext('', ''));

  /* ── Sticky CTA na telefonie ────────────────────────────────────────
     Pokazujemy dopiero, gdy klient dotarł do oferty, i chowamy przy
     formularzu — inaczej pasek zasłaniałby pola, które ma wypełnić. */
  const sticky = $('#sticky-cta');
  const offerSection = $('#online');
  const formSection = $('#online-form');
  if (sticky && offerSection && formSection && 'IntersectionObserver' in window) {
    let pastOffer = false, atForm = false;
    const sync = () => sticky.classList.toggle('on', pastOffer && !atForm);

    new IntersectionObserver(([entry]) => {
      // boundingClientRect.top < 0 → sekcja oferty jest już nad ekranem
      pastOffer = entry.isIntersecting || entry.boundingClientRect.top < 0;
      sync();
    }, { threshold: 0 }).observe(offerSection);

    new IntersectionObserver(([entry]) => {
      atForm = entry.isIntersecting;
      sync();
    }, { threshold: 0 }).observe(formSection);
  }

  window.handleFormSubmit = async function (e) {
    e.preventDefault();
    const form = e.target;
    const endpoint = form.dataset.endpoint;
    const online = form.classList.contains('of-form');
    const ok  = $(online ? '#of-success' : '#form-success');
    const err = $(online ? '#of-error'   : '#form-error');
    const btn = form.querySelector('button[type="submit"]');
    if (ok)  ok.style.display = 'none';
    if (err) err.style.display = 'none';

    const payload = {};
    form.querySelectorAll('[name]').forEach(field => {
      payload[field.name] = field.type === 'checkbox' ? field.checked : field.value.trim();
    });

    const original = btn.innerHTML;
    btn.disabled = true;
    btn.textContent = 'Wysyłanie…';
    try {
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: Object.assign(
          { 'Content-Type': 'application/json', 'Accept': 'application/json' },
          csrfToken ? { [csrfHeader]: csrfToken } : {}
        ),
        body: JSON.stringify(payload)
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok || !data.ok) throw new Error(data.message || 'Błąd wysyłki');
      if (ok) ok.style.display = 'block';
      form.reset();
      // reset() czyści też ukryte pola, więc plakietka wyboru musi zniknąć razem z nimi
      if (online && pickBox) pickBox.hidden = true;
    } catch (_) {
      if (err) err.style.display = 'block';
    } finally {
      btn.disabled = false;
      btn.innerHTML = original;
    }
  };

})();
