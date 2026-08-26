# Czego brakuje i jak to dokończyć

> **Stan na 2026-08-23.** Sekcje 0–4 zostały wykonane. Zostaje sekcja 5
> (rzeczy do przemyślenia później) i trzy pozycje wypisane niżej jako otwarte.
> Historia tego, co było zepsute i dlaczego, siedzi w `memory/solutions/`.

## Jak uruchomić

```bash
mvn clean verify         # kompilacja + 57 testów jednostkowych + 17 integracyjnych
docker compose up -d db
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

`mvn test` uruchamia tylko testy jednostkowe (bez bazy, bez Dockera).
`mvn verify` dokłada integracyjne (`*IT`) na prawdziwym Postgresie z Testcontainers.

Gdy Docker nie jest dostępny dla procesu Javy (Docker Desktop potrafi odciąć
dostęp do gniazda), wskaż gotową, **pustą** bazę:

```bash
docker run -d --name szymtrener-test-db -e POSTGRES_DB=szymtrener_test \
  -e POSTGRES_USER=szymtrener -e POSTGRES_PASSWORD=szymtrener -p 5434:5432 postgres:16-alpine

mvn verify -Dtest.db.url=jdbc:postgresql://localhost:5434/szymtrener_test \
           -Dtest.db.user=szymtrener -Dtest.db.password=szymtrener
```

---

## Co zostało otwarte

### A. Trzy prawdziwe pliki `.docx` od klienta

`DocxToHtmlConverterTest` buduje dokumenty w locie przez POI i pokrywa nagłówki
(angielskie i polskie), formatowanie znakowe, listy, tabele, cytaty i escapowanie.
To nie zastąpi prawdziwych plików: dopiero one pokażą, jak Word klienta nazywa
style nagłówków. Gdy przyjdą, wrzuć je do `src/test/resources/docx/` i dopisz
przypadek na każdy.

Jeśli nagłówki z prawdziwego pliku nie zostaną rozpoznane, wypisz `p.getStyleID()`
i dopasuj warunek w `DocxToHtmlConverter.headingLevel()`.

### B. Polski słownik full-textowy w bazie

Wyszukiwarka działa na konfiguracji `'simple'`, która nie zna polskiej odmiany:
wpisując „mięśnie" nie znajdziesz wpisu o „mięśni". Jeśli wyniki okażą się za słabe,
doinstaluj słownik polski na serwerze bazy i podmień `'simple'` w dwóch miejscach:
definicji kolumny `post.search_vector` (migracja `V1`) i w `PostRepository.search()`.
Zmiana kolumny generowanej wymaga osobnej migracji.

### C. Podgląd bloków edytora w przeglądarce

`EditorBlotSanitizationTest` sprawdza, że film, PDF i tabela przeżywają sanityzację
w stanie, w którym Quill je rozpozna, a `blog.css` ma się czego złapać. Testu
w przeglądarce nie da się tym zastąpić: otwórz zapisany wpis z filmem, PDF-em
i tabelą i sprawdź wzrokowo, czy bloki się wczytały i wyglądają jak w makiecie.

---

## Co zostało zrobione

### 0. Kompilacja i pierwszy start
Projekt kompiluje się i wstaje. Wersje POI 5.4.0, jsoup 1.18.3 i Thumbnailator 0.4.20
istnieją. Hibernate przechodzi walidację schematu (`@OrderColumn` na `summaryPoints`
nie sprawia problemów). Konto administratora zakłada się ze zmiennej `ADMIN_PASSWORD`.

Przy pierwszym uruchomieniu wyszły cztery błędy blokujące, wszystkie naprawione:
logowanie i zapis wpisu odrzucane przez CSRF, `LazyInitializationException` na widoku
artykułu i w edytorze, listy numerowane z Worda importowane jako punktowane.
Opisy w `memory/solutions/`.

### 1. Wejście na produkcję
- `deploy/szymtrener.service`, `deploy/nginx.conf`, `deploy/backup.sh`, `deploy/README.md`.
  Nginx ma jedno przekierowanie 301 (bez łańcucha) i `X-Forwarded-Proto`.
- `GET/POST /admin/haslo`: zmiana hasła, minimum 12 znaków, po zmianie sesja wygasa.
- Historia adresów wpisów (`post_slug_history`, migracja `V3`): zmiana tytułu
  opublikowanego wpisu zostawia stary adres, który odsyła **301** na nowy.
- RODO: `CleanupScheduler` kasuje `page_view` starsze niż rok (codziennie 4:00),
  `POST /admin/zgloszenia/{id}/usun` usuwa dane trwale, `GET .../dane` eksportuje je do JSON-a.
- `error/500.html` + `GlobalExceptionHandler`: błąd dostaje identyfikator, ten sam
  w logu i na ekranie.
- `favicon.ico` (16/32/48), `apple-touch-icon.png`, `icon-192/512.png`, `site.webmanifest`.
  Monogram „SD", navy `#1A2B3C` na miętowym `#4ECBA3`.

### 2. Panel
- **Klienci** (`/admin/klienci`): encja `Trainee`, lista, formularz, oraz przycisk
  „Zrób z tego klienta" na ekranie zgłoszenia (idempotentny).
- **Statystyki** (`/admin/statystyki?dni=`): cztery kafelki KPI, wykres słupkowy
  w czystym CSS, najczęstsze ścieżki, źródła ruchu i **wizyty botów AI na wierzchu**.
- **Ustawienia** (`/admin/ustawienia`): `SettingsService` z cache; rozmiar strony bloga,
  odbiorca powiadomień, przełącznik autoodpowiedzi i domyślne teksty SEO są czytane
  z bazy, a nie zaszyte w kodzie.
- Wybór zdjęcia głównego z siatki miniatur (pole z ID zostaje jako wejście awaryjne).
- Paginacja list: `fragments/admin-layout :: pager(page, baseUrl)`.
- Usuwanie: opublikowany wpis → archiwum, szkic → skasowany; plik odmawia usunięcia,
  gdy jest w użyciu, i przy usunięciu czyści `MediaService.urlCache`; zgłoszenie kasowane trwale.
- Podgląd szkicu: `GET /admin/posty/{id}/podglad`, ten sam widok co strona publiczna,
  z paskiem u góry i `noindex`.

### 3. Blog
- Wyszukiwarka: `GET /blog/szukaj?q=`, po `post.search_vector`, wyniki z `noindex`.
- `PostService.related()` **uzupełnia** listę spoza kategorii zamiast ją podmieniać.
- Kanał RSS pod `/feed.xml`, podlinkowany z `<head>` bloga.
- `post_media` wypełniane przy zapisie wpisu (jsoup po treści + okładka), co dopiero
  daje sens odmowie usunięcia pliku w użyciu.

### 4. Testy
74 testy, wszystkie zielone.

| Zakres | Plik |
|---|---|
| Import DOCX | `DocxToHtmlConverterTest` (+ `DocxFixtures`) |
| Ocena treści | `AiReadinessAnalyzerTest` |
| Adresy wpisów | `SlugUtilTest` |
| Formularze publiczne | `PublicFormControllerTest` (`@WebMvcTest`: honeypot, limit, walidacja, CSRF) |
| Bloki edytora | `EditorBlotSanitizationTest` |
| Kontekst i migracje | `ApplicationContextIT` (Testcontainers) |
| Przepływy treści | `PostFlowIT` (301 ze starego adresu, wyszukiwarka, powiązane, `post_media`, archiwizacja) |

---

## 5. Rzeczy do przemyślenia później

| Temat | Kiedy stanie się problemem |
|---|---|
| Pliki w bazie | Gdy `pg_dump` przekroczy kilka GB. Wtedy przenosiny do S3 albo na dysk, zmiana tylko w `MediaService` |
| `RateLimiter` w pamięci | Przy drugiej instancji aplikacji. Do przeniesienia do bazy albo Redisa |
| `SettingsService` cache | Ta sama sprawa: cache jest lokalny dla instancji, więc druga instancja nie zobaczy zmiany ustawień do restartu |
| `MediaService.urlCache` | Rośnie bez ograniczenia. Przy tysiącach plików zamień na cache z limitem (Caffeine) |
| Quill z CDN-u | Panel przestanie działać, gdy CDN padnie albo klient siedzi za restrykcyjnym firewallem. Pobierz `quill.js` i `quill.snow.css` do `static/vendor/` |
| Import `.doc` | Konwersja jest zgrubna. Jeśli klient przysyła stary format regularnie, rozważ LibreOffice w trybie headless |
| Wielojęzyczność | Dziś wszystko na sztywno po polsku (`lang="pl"`, formaty dat). Gdyby doszła wersja angielska: `hreflang` z self-reference + `MessageSource` |
