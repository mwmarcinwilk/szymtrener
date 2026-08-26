# szymtrener.pl — architektura aplikacji

Dokument opisuje, jak zbudowany jest boilerplate: z czego się składa, dlaczego tak,
i co trzeba zrobić dalej. Ostatnia sekcja odpowiada na pytanie „jak oceniana jest strona”.

---

## 1. Skąd wychodzimy

Masz gotowy, dopracowany landing page (HTML/CSS/JS) i makiety bloga oraz panelu.
Zadanie: dołożyć do tego blog z edytorem, panel administracyjny i obsługę formularzy —
bez zmiany wyglądu i bez utraty tego, co strona ma dziś dobrze zrobione pod widoczność w AI.

Trzy decyzje ustawiły całą resztę:

| Decyzja | Wybór | Konsekwencja |
|---|---|---|
| Renderowanie | **Thymeleaf SSR** wszędzie, także w panelu | Boty AI nie wykonują JavaScriptu — SSR jest warunkiem widoczności, nie preferencją |
| Pliki (zdjęcia, PDF) | **W bazie** (`media_blob`, `bytea`) | Jedna kopia zapasowa, brak rozjazdu dysk↔rekord; koszt: ruch idzie przez aplikację |
| Landing page | **Przeniesiony 1:1** do szablonu | Wygląd identyczny; zmieniły się tylko ścieżki zasobów i podpięcie formularzy |

---

## 2. Stos

| Warstwa | Wybór | Dlaczego akurat to |
|---|---|---|
| Runtime | Java 21, Spring Boot 3.5.5 | LTS, wirtualne wątki dostępne, długie wsparcie |
| Widoki | Thymeleaf + `thymeleaf-extras-springsecurity6` | HTML zostaje HTML-em; makiety przenoszą się prawie bez zmian |
| Baza | PostgreSQL 16 | `bytea`, `jsonb`, `tsvector` — wszystko czego potrzeba, bez dodatkowych usług |
| Migracje | Flyway | Schemat zmienia się wyłącznie migracją; `ddl-auto=validate` pilnuje zgodności |
| Bezpieczeństwo | Spring Security, BCrypt, CSRF | Panel za logowaniem, formularze publiczne z tokenem CSRF |
| Edytor | Quill 2 + 3 własne bloki | Jedyny popularny edytor, w którym można zdefiniować własny format wyjściowy |
| Import z Worda | Apache POI (`poi-ooxml`, `poi-scratchpad`) | Własne mapowanie DOCX → HTML bloga |
| Czyszczenie HTML | jsoup | Biała lista tagów; to samo narzędzie liczy metryki treści |
| Zdjęcia | Thumbnailator | Skalowanie do 1600 px przy wgrywaniu |
| Poczta | Spring Mail (JavaMail) | SMTP hostingu, bez usług trzecich |

Świadomie **nie** ma tu: Reacta ani żadnego SPA (patrz bramka G4 w sekcji 8), zewnętrznej
analityki, CDN-a na start, S3, Redisa, Dockera na produkcji. Każde z nich można dołożyć,
gdy pojawi się realny powód.

---

## 3. Mapa pakietów

```
pl.szymtrener
├── config/        AppProperties, SecurityConfig, WebConfig
├── common/        SlugUtil (polskie znaki), NotFoundException
├── content/       Post, Category, Author, PostFaq, PostService,
│                  HtmlSanitizer, ContentMetrics, PostView (DTO widoku)
├── media/         MediaFile + MediaBlob, MediaService, MediaController
├── docimport/     DocxToHtmlConverter, LegacyDocConverter, DocImportService
├── submission/    Submission, SubmissionService, MailService, RateLimiter
├── analytics/     PageView, AnalyticsFilter (własna statystyka, bez ciasteczek)
├── seo/           JsonLdService, SeoController, IndexNowService, AiReadinessAnalyzer
├── scheduler/     PublishScheduler (publikacja zaplanowanych wpisów)
├── web/           HomeController, BlogController, PublicFormController
└── admin/         AdminController, AdminPostController, AdminApiController,
                   AdminSubmissionController, AdminMediaController, PostForm
```

Zasada podziału: pakiet = obszar domeny, nie warstwa techniczna. Nie ma katalogów
`services/`, `dto/`, `mappers/` — encja, repozytorium i serwis jednego tematu leżą razem.

---

## 4. Model danych

```
admin_user                        konto do panelu
author ──< author_same_as         autor + profile zewnętrzne (JSON-LD Person)
category ──< post
post ──< post_tag
     ──< post_summary_point       „W skrócie" — pola, nie HTML
     ──< post_faq                 FAQ — pola, nie HTML
     ──< post_media
     ──> media_file               zdjęcie główne
media_file ──1:1── media_blob     metadane osobno, bajty osobno
submission ──< submission_note    zgłoszenia z formularzy + notatki
trainee                           klienci (tabela gotowa, ekran do zrobienia)
page_view                         własna statystyka odwiedzin
app_setting                       ustawienia edytowalne z panelu
```

Trzy rzeczy warte uwagi:

**`media_blob` osobno od `media_file`.** Lista mediów w panelu robi `select` po metadanych
i nigdy nie ciągnie megabajtów. Bajty pobiera tylko kontroler, który je serwuje.

**„W skrócie" i FAQ jako tabele, nie fragmenty HTML.** Gdyby autor wpisywał je w treść,
JSON-LD `FAQPage` musiałby być pisany ręcznie i po pierwszej edycji rozjechałby się z tym,
co widać na stronie. Tak jak jest teraz, widoczny HTML i dane strukturalne pochodzą z tego
samego rekordu i rozjechać się nie mogą.

**`content_delta` obok `content_html`.** Delta to źródło prawdy edytora (Quill wczytuje ją
bez strat), HTML to gotowa treść dla przeglądarki i bota. Zapisujemy oba.

---

## 5. Przepływy

### Publikacja wpisu

```
Panel → PostForm → HtmlSanitizer (biała lista) → ContentMetrics (słowa, czas czytania,
wideo/PDF) → zapis → AiReadinessAnalyzer (ocena treści) → IndexNow (jeśli PUBLISHED)
```

Wpis zaplanowany czeka ze statusem `SCHEDULED`; `PublishScheduler` co minutę przepuszcza te,
których termin minął, i pinguje IndexNow. Nie ma osobnej kolejki — tabela `post` jest kolejką.

### Formularz ze strony

```
przeglądarka (JSON + token CSRF) → PublicFormController → walidacja + honeypot + limit 5/h/IP
→ zapis w bazie → MailService (@Async): powiadomienie do trenera + autoodpowiedź
```

Kolejność jest celowa: **najpierw baza, potem poczta**. Gdy SMTP nie odpowie, zgłoszenie i tak
jest zapisane, a błąd ląduje w `submission.mail_error`. Web3Forms z makiety zostało usunięte —
dane klientów zostają na własnym serwerze.

### Import z Worda

```
.docx → POI (XWPFDocument) → mapowanie na tagi bloga → obrazki do biblioteki mediów
→ HtmlSanitizer → HTML wklejony w miejscu kursora w edytorze
```

Przenoszone są: nagłówki (Heading 1–3 → H2–H4, bo H1 to tytuł wpisu), pogrubienie, kursywa,
podkreślenie, przekreślenie, listy punktowane i numerowane, hiperłącza, tabele (pierwszy wiersz
jako `<th scope="col">`), obrazki i cytaty. **Świadomie nie przenosimy** czcionek, rozmiarów,
kolorów i wyrównania — o wyglądzie decyduje styl bloga, nie ustawienia z Worda.

Napisany jest własny konwerter zamiast gotowej biblioteki (docx4j, xdocreport), bo te
produkują HTML pełen stylów inline i tagów, których `blog.css` nie zna — i tak trzeba by je
czyścić. Tutaj od razu powstaje tylko to, co blog renderuje.

Stary format `.doc` obsługuje `WordToHtmlConverter` z POI — wynik jest zgrubny, użytkownik
dostaje komunikat, żeby zapisać plik jako `.docx`.

### Edytor: trzy własne bloki

Quill 2 domyślnie nie umie tego, co pokazuje makieta artykułu. Dopisane są trzy formaty:

| Blok | Co generuje | Dlaczego tak |
|---|---|---|
| `youtubeFacade` | `div.art-video.yt-facade[data-id]` z miniaturą | Zamiast iframe'a (~1 MB skryptów i ciasteczka stron trzecich) — odtwarzacz wstawia się po kliknięciu |
| `pdfFile` | `div.art-pdf-block` z `a.art-pdf` do `/pliki/{id}/…` | Pobrania są liczone, plik ma stały adres |
| `dataTable` | prawdziwy `<table>` z `<th scope="col">` | Quill 2 nie ma tabel, a dane w tabeli są wyraźnie lepiej wyciągane przez modele niż ta sama treść w akapicie |

Każdy blok to `div` z klasą markerową i `contenteditable="false"` — dzięki temu Quill potrafi
go odczytać z powrotem przy edycji istniejącego wpisu, a `HtmlSanitizer` przepuszcza dokładnie
te atrybuty i nic więcej.

---

## 6. Co aplikacja robi sama pod kątem widoczności w AI

To jest sedno tego boilerplate'u: rzeczy, o których autor nie powinien pamiętać, dzieją się
same przy każdym wpisie.

| Element | Realizacja |
|---|---|
| SSR | Thymeleaf — pełna treść w surowym HTML, bez JS |
| `robots.txt` | Generowany, z jawnym `Allow` dla 10 botów (Tier-1 i Tier-2), `Disallow` tylko `/admin/` i `/api/` |
| `sitemap.xml` | Generowana z bazy, `lastmod` z `updated_at` — nowy wpis pojawia się natychmiast |
| IndexNow | Ping po publikacji i po przejściu wpisu zaplanowanego |
| JSON-LD | `BlogPosting`, `BreadcrumbList`, `FAQPage`, `Person` (+ `sameAs`), `Organization` — budowane z tych samych obiektów, z których renderuje się HTML |
| `canonical`, OG, `lang="pl"` | W każdym szablonie, adresy absolutne z `app.site-url` |
| `dateModified` | Z `post.updated_at`, także w stopce strony głównej |
| Byline autora | Z tabeli `author` — sekcja o autorze pod artykułem i `Person` w JSON-LD |
| Paginacja bloga | Zwykłe linki `?strona=`, nie „doładuj więcej" — crawler przechodzi dalej |
| TTFB | Kompresja włączona, zasoby statyczne z `Cache-Control`, pliki z `immutable` + ETag |
| Wizyty botów AI | `AnalyticsFilter` liczy osobno GPTBota, ClaudeBota, PerplexityBota… |

Reszta zależy od tekstu — i to właśnie mierzy `AiReadinessAnalyzer`, pokazując ocenę na żywo
w prawej kolumnie edytora (pierścień + lista kryteriów z konkretną wskazówką).

Analizator liczy **wyłącznie blok CONTENT** siatki (41 pkt, przeskalowane do 100). To celowe:
technikalia i infrastrukturę aplikacja spełnia dla każdego wpisu tak samo, więc wliczanie ich
do oceny redakcyjnej tylko zawyżałoby wynik i niczego nie podpowiadało.

---

## 7. Wdrożenie

Docelowo VPS z Ubuntu, bez Dockera na produkcji:

```
nginx (443, TLS z Let's Encrypt)
  ├── /css /js /images   → pliki z dysku, cache 30 dni
  └── reszta             → proxy_pass http://127.0.0.1:8080
systemd: szymtrener.service (java -jar, Restart=always, EnvironmentFile z sekretami)
postgresql 16 na localhost
cron: pg_dump codziennie + kopia poza serwer
```

Ważne przy nginx: `proxy_set_header X-Forwarded-Proto https` — aplikacja ma
`forward-headers-strategy: native`, dzięki czemu `canonical` i JSON-LD zawsze mają `https`.

Ponieważ pliki są w bazie, `pg_dump` to kompletna kopia zapasowa serwisu. Gdy biblioteka
mediów urośnie na tyle, że dump zacznie przeszkadzać, przenosiny na dysk albo S3 sprowadzają
się do jednej klasy — `MediaService` jest jedynym miejscem, które dotyka bajtów.

---

## 8. Jak oceniana jest strona (siatka V4)

Skrót algorytmu z `ALGORYTM_OCENIANIA.md`, żeby wiedzieć, na co patrzy audyt.

**Trzy bloki sumujące się do 100 punktów:**

| Blok | Pkt | Co zawiera |
|---|---:|---|
| CONTENT | 41 | Odpowiedź po H1 (6), odpowiedź w każdej sekcji (4), gęstość danych (8), ekstrahowalne fragmenty (5), autorytatywne cytowania (6), aktualność (8), + 4 kategorie po 1 pkt |
| TECHNICAL | 32 | Dostępność dla botów AI (15), Schema.org (7), `sameAs` (4), sygnały encji (2), + 4 kategorie po 1 pkt |
| INFRASTRUCTURE | 27 | TTFB (5), strona „O nas" (5), autorzy i E-E-A-T (5), język i hreflang (2), IndexNow (2), + 8 kategorii po 1 pkt |

**Pięć bramek pass/fail. Niezdanie którejkolwiek ogranicza wynik do 30 punktów:**

| # | Bramka | Warunek |
|---|---|---|
| G1 | Dostępność dla botów | brak `Disallow: /` dla `*` i Googlebota |
| G2 | Brak noindex | brak `noindex`/`nosnippet` w meta i w `X-Robots-Tag` |
| G3 | HTTP 200 | strona odpowiada statusem 200 |
| G4 | **Renderowanie SSR** | treść widoczna w surowym HTML bez JS (≥50 słów, dla SPA ≥100) |
| G5 | HTTPS | działa po HTTPS |

G4 jest powodem, dla którego cała aplikacja jest SSR. Strona w Reakcie bez renderowania
po stronie serwera nie przekroczy 30 punktów niezależnie od tego, jak dobra jest jej treść.

**Kara −15 punktów** za łańcuch przekierowań ≥3 skoków lub pętlę — boty cytujące przerywają
połączenie po około trzech skokach. Praktyczny wniosek dla nginx: `http → https → docelowy adres`
i koniec, żadnych dodatkowych przekierowań `www`/slash po drodze.

**Drabina za `robots.txt` (15 pkt, pierwsze dopasowanie wygrywa):**

| Sytuacja | Pkt |
|---|---:|
| jawny `Allow` dla ≥3 botów Tier-1 | 15 |
| jawny `Allow` dla 1–2 botów Tier-1 | 13 |
| zablokowane tylko boty treningowe | 12 |
| plik jest, brak reguł dot. botów AI | 10 |
| bot Tier-1 zablokowany na podścieżkach | 8 |
| **brak pliku / błąd pobrania** | 5 |
| bot Tier-1 zablokowany na `/` | 0 |

Dlatego `robots.txt` w aplikacji wymienia boty z nazwy zamiast poprzestać na `User-agent: *`.
Blokowanie samych botów treningowych nie jest karane — to legalna ochrona kosztów.

**`llms.txt` daje 0 punktów** (adopcja poniżej 1%, brak potwierdzonego wpływu). Endpoint
jest w aplikacji jako higiena, ale nie ma co po nim oczekiwać wyniku.

**Etykiety:** 85–100 EXCELLENT · 70–84 GOOD · 50–69 MODERATE · 30–49 WEAK · 15–29 POOR · 0–14 CRITICAL.

---

## 9. Kolejność prac

Pełna lista braków z instrukcjami: **[DO_ZROBIENIA.md](DO_ZROBIENIA.md)**.

1. **Uruchomienie i kompilacja** — `mvn clean compile`, poprawki (patrz README, sekcja o ryzykach), pierwszy wpis od zera.
2. **Ekran klientów** — tabela `trainee` jest, brakuje encji i widoku; makieta w `Panel administracyjny.html`.
3. **Ekran statystyk** — dane są w `page_view`, repozytorium ma już zapytania (`topPaths`, `topReferrers`, `botVisits`).
4. **Ekran ustawień** — `app_setting` z panelu zamiast zmiennych środowiskowych.
5. **Wybór zdjęcia głównego z biblioteki** — dziś wpisuje się ID pliku; do zamiany na modal z siatką mediów.
6. **Wyszukiwarka na blogu** — kolumna `search_vector` (GIN) czeka gotowa.
7. **Testy** — najpierw import DOCX i sanitizer, bo tam najłatwiej o regresję.

---

## 10. Odstępstwa od makiet

| Rzecz | Makieta | Aplikacja | Powód |
|---|---|---|---|
| Nawigacja na blogu | 5 pozycji | 8, jak na stronie głównej | Jedno źródło nawigacji + więcej linków wewnętrznych |
| Filtry kategorii | `<button>` | `<a href>` | Filtrowanie ma działać bez JS i być indeksowalne |
| „Pokaż starsze wpisy" | `<button>` | link `?strona=2` | Crawler musi mieć dokąd przejść |
| Miniatura YouTube | `maxresdefault` + `onerror` | `hqdefault` | `hqdefault` istnieje zawsze; `onerror` to inline JS, którego sanitizer nie przepuszcza |
| Panel: wyszukiwarka i dzwonek | w makiecie | pominięte | Nie mają jeszcze czego szukać ani o czym powiadamiać |
