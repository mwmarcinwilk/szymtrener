# SPEC: Dokumenty prawne na szymtrener.pl (regulamin, polityka, formularz, zasady odwołań, czcionki)

- **Data:** 2026-11-13
- **Sesja:** 56685f36-7ec
- **Status:** zatwierdzony plan; kryteria akceptacji poniżej są kontraktem dla @security

---

# Dokumenty prawne na szymtrener.pl (regulamin, polityka, formularz, zasady odwołań, czcionki)

## Context
Joanna przygotowała dla Szymona komplet dokumentów (docs/dokumenty, obowiązują od 1.11.2026). Na stronę idą dwa:
Regulamin świadczenia usług (z załącznikami 1 i 2) i Polityka prywatności. Porównanie ze stroną wykazało
rozbieżności, rozstrzygnięte przez Szymona i Joannę (13.11):
- odwołanie treningu **12 h** (strona mówi 24 h; regulamin zostaje bez zmian),
- późne odwołanie: tekst strony jak w Regulaminie Studia §3 ust. 4,
- przerwa w pakiecie do 3 tygodni: **znika ze strony**,
- obowiązkowy checkbox zgody w formularzach → informacja o przetwarzaniu (podstawa art. 6 ust. 1 lit. b/f),
- zdanie „dane nie wychodzą poza EOG” zostaje.
Decyzje Marcina: strony HTML + „Pobierz PDF”; tekst polityki Joanny 1:1 + nasz akapit o cookies;
czcionki przenosimy z Google na nasz serwer (inaczej IP każdego odwiedzającego idzie do Google bez zgody).

Poza zakresem: oznaczanie opinii zdobytych za cenę startową (regulamin §4), dokumenty offline Szymona.

## 1. Regulamin i polityka (HTML + PDF)
- **Nowa strona `/regulamin`**: `templates/regulamin.html` na wzór `polityka-prywatnosci.html` (`legal-nav`,
  `legal-wrap`, własna stopka, `cookie-consent :: consent`). Treść 1:1 z docx: §1–§11 jako `h2`, ustępy jako `ol`
  (numeracja dziesiętna jak w Wordzie, bo tekst odwołuje się do „ust. 4”), podpunkty a), b) jako `ol type="a"`,
  Załącznik 1 (pouczenie) i Załącznik 2 (formularz odstąpienia) z kotwicami `#zalacznik-1`, `#zalacznik-2`.
- `HomeController` (`web/HomeController.java:77`): handler `regulamin()` jak `privacy()` (`year`, `canonical`
  przez `props.absolute`), z `@Operation`, jeśli `privacy()` go ma (wymóg AGENTS.md dla nowego endpointu).
- **Polityka**: `polityka-prywatnosci.html` dostaje tekst Joanny 1:1 (8 sekcji, numerowane ustępy). W sekcji
  „Pliki cookies” pod jej punktami zostaje nasz obecny akapit o `__Host-st_zgoda`, `JSESSIONID`, statystyce na serwerze
  i 12 miesiącach. Nowa data „Ostatnia aktualizacja”.
- **PDF**: oba pliki w `static/dokumenty/regulamin.pdf` i `static/dokumenty/polityka-prywatnosci.pdf`, wydrukowane
  headless Chrome (`/Applications/Google Chrome.app`, `--print-to-pdf`) z działającej aplikacji, więc treść PDF = HTML.
  Styl `@media print` w `styles.css` ukrywa nawigację, stopkę, baner cookies i przycisk PDF. Komenda generowania
  trafia do `CAPABILITIES.md`, żeby po zmianie tekstu przegenerować PDF.
- Na obu stronach pod datą link „Pobierz PDF” (`download`).
- **Stopka**: link „Regulamin” w `fragments/footer.html:12` (index, blog, 404, 500) i w stopkach na sztywno
  w `polityka-prywatnosci.html:135`, `ustawienia-cookies.html` (~:53) oraz w nowym `regulamin.html`.
- Sitemap: `SeoController.java:97` dodać `/regulamin` (priorytet 0.2).

## 2. Formularze kontaktowe bez checkboxa zgody
- `index.html:744` i `:1032`: usunąć checkbox `consent`, w jego miejsce krótka informacja (styl `consent-text`):
  „Administratorem danych z formularza jest Szymon Domagała. Wykorzystam je tylko do odpowiedzi i rozmowy o współpracy.
  Szczegóły w Polityce prywatności.” (copy bez półpauz, `anty-slop.md`).
- `submission/FormRequests.java:23,33`: usunąć pole `consent` z `@AssertTrue`.
- `Submission.consentAt` (`Submission.java:34`, domyślne `Instant.now()`): nowe zgłoszenia nie mają już zgody.
  Migracja **`V15__submission_consent_optional.sql`**: `alter table submission alter column consent_at drop not null`;
  w encji `nullable = true`, bez wartości domyślnej. Stare wiersze zachowują datę zgody.
  `SubmissionService.java:181` (`zgoda_rodo`): sprawdzić, czy mapa przyjmuje null, i wpisywać tylko, gdy jest.

## 3. Zasady odwołań (stacjonarnie)
- Migracja **`V16__stationary_rules_12h_no_pause.sql`**, nadpisuje BEZWARUNKOWO (zgodność z regulaminem jest ważniejsza
  niż edycja w panelu; upsert `on conflict do update`):
  - `stationary.rules.cancel`: „Odwołujesz trening co najmniej 12 godzin wcześniej? Nic się nie dzieje, ustalamy nowy
    termin w czasie ważności pakietu.”
  - `stationary.rules.late`: „Odwołanie później niż 12 godzin przed treningiem albo nieobecność bez uprzedzenia: jeśli nie
    zdążę oddać tej godziny komuś innemu, trening liczy się jako wykorzystany.”
  - `stationary.rules.freeze`: „W Opiece Longevity możesz zamrozić współpracę łącznie do 4 tygodni. Wystarczy wcześniej
    zgłosić i ustalić termin. Umowa wydłuża się o czas przerwy, raty zostają bez zmian.”
  - `delete from app_setting where "key" = 'stationary.rules.pause'`.
- Kod: usunąć `RULE_PAUSE` z `offer/StationaryOfferService.java:28-31,220`, pole pauzy z `AdminOfferController.java:354-357,
  467-474` i `admin/offer-stationary.html:85-99`.
- `web/HomeFaq.java:33`: wyrzucić zdanie „Do tego raz na pakiet możesz zrobić przerwę do 3 tygodni.”
- `index.html:418-424` i `:921-922`: przykładowy tekst w `th:text` na 12 h i poprawiony komentarz o pauzie.
- `CAPABILITIES.md:80`: klucze bez `pause`.

## 4. Czcionki u siebie
- Pliki woff2 (OFL) z pakietów Fontsource na jsDelivr: Montserrat variable (wght 400–900) i Lora variable
  (normal + italic), podzbiory `latin` + `latin-ext` (polskie znaki). Nazwy plików sprawdzam listą pakietu przed
  pobraniem, nie z pamięci. Do `static/fonts/` razem z `OFL.txt`.
- `static/css/fonts.css` z `@font-face` (`font-display: swap`, `unicode-range` jak w Google CSS).
- W 9 szablonach (`index`, `blog/list`, `blog/post`, `polityka-prywatnosci`, `ustawienia-cookies`, `regulamin`,
  `fragments/admin-layout`, `error.html`, `error/404`, `error/500`) linki i `preconnect` do Google → `<link fonts.css>`.
- Sprawdzić nagłówek CSP w `SecurityConfig`; jeśli dopuszcza `fonts.googleapis.com`/`fonts.gstatic.com`, usunąć.

## Testy
- IT (wzór `web/HomePageCopyIT.java:49`, render do `</html>`):
  - `/regulamin` 200, zawiera `§ 11`, `ZAŁĄCZNIK NR 2`, link do PDF;
  - `/polityka-prywatnosci` zawiera nowe „umówienie rozmowy” i zachowane `__Host-st_zgoda`;
  - stopka strony głównej i polityki zawiera `/regulamin`;
  - `/dokumenty/regulamin.pdf` i `/dokumenty/polityka-prywatnosci.pdf` → 200, `application/pdf`;
  - strona główna: brak `name="consent"`, jest link do polityki w informacji; reguły zawierają „12 godzin”,
    brak „24 godzin” i „przerw”; FAQ (JSON-LD) bez „przerwę do 3 tygodni”;
  - `/sitemap.xml` zawiera `/regulamin`;
  - `/css/fonts.css` 200, żaden wyrenderowany szablon publiczny nie zawiera `fonts.googleapis.com`.
- `PublicFormControllerTest` (`:73,88,179,186`): zgłoszenie bez `consent` przechodzi; test wymaganej zgody usunięty.
- Test unit/IT `HomeFaq` (jeśli istnieje) bez zdania o przerwie.

## Weryfikacja
1. `mvn clean verify`: liczby z `surefire-reports` I `failsafe-reports`, plus `GanwilkArchitectureTest`.
2. Aplikacja lokalnie (skill `run`; kontener z `--label claude.tmp=1`, zdjęty po użyciu). Pętla wizualna
   `design-standards`: zrzuty 390/768/1440 dla `/regulamin`, `/polityka-prywatnosci`, formularzy (oba), bloku zasad
   i porównanie czcionek przed i po; potem `browser_close`.
3. Wygenerować PDF-y z działającej aplikacji, obejrzeć (numeracja §, załączniki, brak nawigacji i banera).
4. `/simplify`, `/code-review medium`, subagent `security`. `CAPABILITIES.md` (nowa strona, PDF, czcionki),
   `SPEC-2026-11-13-dokumenty-prawne.md` w `.vault/`. Bez commita bez zgody.

