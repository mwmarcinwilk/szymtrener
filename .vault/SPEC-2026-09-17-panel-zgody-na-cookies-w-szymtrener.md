# SPEC: Panel zgody na cookies w szymtrener

- **Data:** 2026-09-17
- **Sesja:** fc51e4d6-d58
- **Status:** zatwierdzony plan; kryteria akceptacji poniżej są kontraktem dla @security

---

# Panel zgody na cookies w szymtrener

## Kontekst
Szymtrener nie ma panelu zgody, a polityka prywatności odsyła do „ustawień banera cookies”, których nie ma.
Wzorcem jest ddd (`fragments/layout.html :: consentBar / consentPanel`, `AnalyticsController`, `VisitorResolver`):
formularz renderowany na serwerze, działa bez JS, a JS tylko otwiera dialog i wysyła wybór w tle.
Evidio trzyma zgodę wyłącznie w localStorage, więc do bazy nie ma czego skopiować. To jest nowa część.

Ustalenia z Tobą:
- jedna kategoria opcjonalna: **statystyka** (jak w ddd),
- każda zgoda ma **losowy identyfikator**; osoba widzi go w ustawieniach cookies i **sama pobiera** swoje dane (JSON),
- admin w panelu: lista zgód, szukanie po ID, pobranie JSON-a, usunięcie.

Stan dziś: `AnalyticsFilter` liczy odsłony bez ciasteczek (dzienny skrót IP+UA+sól), YouTube to nocookie po kliknięciu.
Żeby zgoda na statystykę coś realnie zmieniała: **ze zgodą** odsłona dostaje stały klucz (skrót soli i ID zgody),
więc panel policzy powracających. Bez zgody zostaje obecne zachowanie, bit w bit.

## Model danych — `V13__cookie_consent.sql`
```
cookie_consent        (id bigserial, consent_key uuid unique not null, statistics boolean not null,
                       created_at timestamptz, updated_at timestamptz)
cookie_consent_change (id bigserial, consent_id bigint fk -> cookie_consent on delete cascade,
                       statistics boolean not null, source varchar(10) not null  -- BAR | DIALOG | PAGE
                       policy_version varchar(20) not null, changed_at timestamptz)
alter table page_view add column consent_id bigint references cookie_consent(id) on delete cascade;
index: cookie_consent(updated_at), cookie_consent_change(consent_id, changed_at desc)
```
Historia zmian to dowód zgody (RODO art. 7 ust. 1). Bez IP i bez user-agenta, żeby rekord nie stał się danymi, których nie potrzebujemy.

## Backend — nowy pakiet `pl.szymtrener.consent`
- `CookieConsent`, `CookieConsentChange` (encje; `@ManyToOne(fetch = LAZY)`), repozytoria z `Page<>` i zapytaniami
  `findByConsentKey`, historia przez `findByConsentIdOrderByChangedAtDesc` (bez kolekcji na encji, bez N+1).
- `ConsentCookie` — ciasteczko `st_zgoda` = sam UUID (decyzja zawsze z bazy); po HTTPS `__Host-st_zgoda` z Secure; HttpOnly, SameSite=Lax,
  182 dni. Parsowanie odrzuca wszystko, co nie jest poprawnym UUID.
- `ConsentService`
  - `record(existingKey, statistics, source)` — nowa zgoda albo nowy wpis historii istniejącej; zwraca klucz,
  - `current(request)` — ciasteczko + sprawdzenie w bazie, zapamiętane w atrybucie żądania; rekord usunięty
    przez admina = brak decyzji, więc pasek pokaże się ponownie,
  - `export(key)` — `LinkedHashMap` w stylu `SubmissionService.export()`: ID, stan, historia, odsłony powiązane z kluczem,
  - `delete(key)` — twarde usunięcie zgody, historii (kaskada) i odsłon z tym kluczem,
  - `search(q, pageable)`, `purgeOlderThan(cutoff)`.
- `ConsentController` (publiczny, wzór `AnalyticsController` z ddd):
  - `GET /ustawienia-cookies` — strona z panelem, ID zgody i linkiem do pobrania,
  - `POST /zgoda-cookies` (`wybor`=tak|nie|zapisz, `statystyka`, `powrot`, `zrodlo`) — redirect albo 204 dla `fetch`;
    `safeRedirect()` przeniesione 1:1 z ddd (odrzuca `//`, `\`, `/admin`, znaki sterujące, klamry),
  - `GET /ustawienia-cookies/moje-dane` — JSON wyłącznie dla klucza z ciasteczka tej przeglądarki; brak = 404.
  - Limit nowych rekordów na IP: osobna instancja limitera, żeby bot nie zapchał tabeli ani nie zjadł limitu formularzy.
    `RateLimiter` rozszerzam minimalnie (konstruktor z limitem i oknem; obecny bean zostaje 5/h).
- `ConsentView` jako bean `@cookieConsent` dla szablonów (wzorzec `AdminNav`, świadomie bez `@ControllerAdvice`).
- `AnalyticsFilter.record()` — przy zgodzie na statystykę `consentId = id zgody`; `sessionHash` zostaje dziennym skrótem, więc licznik sesji się nie zmienia.
- `AdminConsentController` — `GET /admin/zgody-cookies?q=&strona=` (paginacja, `fragments/admin-layout :: pager`),
  `GET /admin/zgody-cookies/{key}/dane`, `POST /admin/zgody-cookies/{key}/usun` z logiem kto usunął.
  Kontroler bez repozytorium i bez `@Transactional`. `@Operation` pominięte: projekt nie ma springdoc (żaden endpoint go nie ma), decyzja należy do Marcina.
- `AdminStatsController` + `PageViewRepository` — jeden kafelek „Powracający (za zgodą)”:
  klucze ze zgodą widziane w ≥ 2 różnych dniach zakresu.
- `CleanupScheduler` — zgody bez zmiany od 365 dni kasowane o 4:00 (ciasteczko żyje 182 dni, drugie pół roku to dowód).

## Front publiczny (wygląd szymtrener, nie ddd)
- `templates/fragments/cookie-consent.html` — `bar` i `panel(mode)` (dialog / strona), zakładki Zgoda · Szczegóły · O plikach cookies,
  tabela faktycznych ciasteczek: `__Host-st_zgoda`, `JSESSIONID` (CSRF formularzy i panel). Oba przyciski paska mają tę samą wagę.
- Dołączone w `fragments/footer.html` (index, blog, 404/500) i w `polityka-prywatnosci.html`; w stopce link „Ustawienia cookies”.
- `templates/ustawienia-cookies.html` — nav w stylu `legal-nav`, panel w trybie strony, ID zgody + „Pobierz moje dane”.
- `static/css/styles.css` — sekcja `CONSENT` na istniejących tokenach (`--navy-deep`, `--mint`, `--border`, `--radius`,
  `.btn-primary` / `.btn-outline`), przełącznik w mięcie, pasek na dole bez overlaya, `<dialog>` z `prefers-reduced-motion`.
- `static/js/main.js` — otwarcie dialogu, zakładki, wysłanie przez `fetch` z nagłówkiem CSRF z `<meta name="_csrf">`.
  Bez JS wszystko działa zwykłym POST-em.
- `polityka-prywatnosci.html` — sekcja „Pliki cookies” przepisana na stan faktyczny (dziś mówi o marketingowych, których nie ma).
  Copy wg `design-standards` / anty-slop, bez półpauz.

## Panel admina
- `templates/admin/consents.html` — szukajka po ID, tabela (ID, statystyka tak/nie, udzielona, ostatnia zmiana),
  akcje „Pobierz JSON” i „Usuń” przez `data-confirm` (`admin.js`), komunikat po usunięciu.
- `fragments/admin-layout.html` — pozycja „Zgody cookies” w grupie „Analiza”.

## Testy
- `ConsentServiceIT` (Postgres): nowa zgoda, zmiana dopisuje historię, eksport, usunięcie kasuje historię i odsłony, retencja.
- `ConsentControllerIT`: atrybuty ciasteczka, brak CSRF = 403, `safeRedirect` (`//evil`, `/admin`, TAB, `{}`),
  `moje-dane` bez ciasteczka / z podrobionym UUID / z cudzym nieistniejącym = 404, z prawdziwym = JSON,
  anonim na `/admin/zgody-cookies` = logowanie, usunięcie przez admina, limit nowych rekordów.
- Render do `</html>`: `/`, `/polityka-prywatnosci`, `/ustawienia-cookies`, `/admin/zgody-cookies`, `/admin/statystyki`;
  pasek jest bez decyzji, znika po decyzji, wraca po usunięciu rekordu.
- `AnalyticsFilter`: ze zgodą ten sam klucz w dwóch dniach, bez zgody skrót jak dotąd.
- `GanwilkArchitectureTest` bez nowych naruszeń; `StylesheetStructure`-style bilans klamr w `styles.css`, jeśli test istnieje.

## Weryfikacja
1. `mvn -q clean test` na kopii, jeśli IntelliJ ma projekt otwarty.
2. Aplikacja lokalnie + Playwright: pasek na 375 px i desktopie, dialog, tryb bez JS, zmiana decyzji, pobranie JSON,
   w panelu wyszukanie ID, pobranie, usunięcie, pasek wraca. Zrzuty do pętli wizualnej.
3. `/simplify`, `/code-review`, subagent `security`.
4. `.vault/SPEC-cookie-consent.md` (ten plan), linie w `.vault/CAPABILITIES.md`, odświeżenie INVENTORY, `record-solution`.


## Zmiany względem planu po bramkach (2026-09-17)
- `/code-review`: sesja CSRF tworzona w stopce urywała długie strony → `spring.thymeleaf.servlet.produce-partial-output-while-processing: false`.
- `/code-review` + security: powiązanie odsłon przez `page_view.consent_id` z kaskadą (zamiast skrótu w `session_hash`): licznik sesji bez zmian, retencja i usunięcie kasują odsłony, zmiana soli nic nie zrywa.
- Security: limit 120 zapisów/h na adres (nowe i zmiany razem); ta sama decyzja nie dopisuje historii i nie odnawia ciasteczka; `__Host-` po HTTPS.
- Testy: `ConsentServiceIT` (6), `ConsentControllerIT` (14). Pełny przebieg: `mvn -DargLine="-Dapi.version=1.43" clean verify`.

## Rozszerzenie: dokumentacja OpenAPI (decyzja Marcina, 2026-09-17)
Kontekst: AGENTS.md wymaga `@Operation` na nowym endpoincie, projekt nie miał springdoc. Marcin: „dodaj”.
- `springdoc-openapi-starter-webmvc-ui` 2.8.17 (linia 2.8 dla Boot 3.5), wersja przypięta w `<springdoc.version>`.
- Dokumentacja pod `/admin/api-docs` (JSON, `.yaml`, `swagger-config`) i `/admin/swagger-ui.html`.

Kryteria akceptacji:
1. Anonim nie dostaje żadnej postaci dokumentacji: `/admin/api-docs*`, domyślne `/v3/api-docs*` (także `.yaml`), `/swagger-ui*`, `/webjars/swagger-ui/**` (sonda w `OpenApiDocsIT`, czerwona na starej konfiguracji).
2. Admin widzi 6 endpointów zgody z `summary` i kodami odpowiedzi zgodnymi z zachowaniem (JSON i YAML), Swagger UI się ładuje.
3. Nowe zależności bez znanych podatności (OSV), pełny `mvn clean verify` zielony.
4. Starsze kontrolery (19 plików) bez `@Operation`: poza zakresem, do osobnej decyzji.
