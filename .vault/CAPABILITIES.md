# CAPABILITIES — szymtrener
> Opisane możliwości projektu. Nowe dopisuj jedną linią: `NazwaKlasy.metoda()` — ścieżka — co robi / kiedy użyć.
> To czyta agent, zanim zbuduje coś nowego, żeby nie dublować logiki.

## Treść i blog
- `PostService.save(post, desiredSlug)` — content/PostService.java — jedyne poprawne wejście do zapisu wpisu: liczy slug, sanityzuje HTML, liczy metryki, odkłada stary adres do historii i odtwarza powiązania z plikami. Nie zapisuj wpisu przez `PostRepository.save()` z pominięciem tej metody.
- `PostService.related(post)` — content/PostService.java — 3 powiązane wpisy: najpierw z tej samej kategorii, potem UZUPEŁNIANE najnowszymi z reszty bloga (nie podmieniane).
- `PostService.search(q, page, size)` — content/PostService.java — wyszukiwarka po `post.search_vector` (tsvector + GIN). Konfiguracja `'simple'`, więc bez polskiej odmiany.
- `PostService.postsUsing(mediaId)` — content/PostService.java — tytuły wpisów używających pliku (treść + okładka). Pusta lista = plik wolno usunąć.
- `PostService.deleteOrArchive(post)` — content/PostService.java — opublikowany wpis → ARCHIVED, szkic → skasowany. Zwraca `true`, gdy naprawdę usunięto.
- `PostSlugHistoryRepository` — content/ — stare adresy wpisów; `BlogController.post()` odsyła z nich 301 zamiast 404.
- `PostMediaRepository` — content/ — powiązania wpis–plik; podstawa decyzji „czy wolno usunąć plik".
- `PostPageModel.fill(postId, model, preview)` — web/PostPageModel.java — model widoku artykułu, wspólny dla strony publicznej i podglądu szkicu. Wczytuje wpis WEWNĄTRZ swojej transakcji (open-in-view=false).

## Ocena wpisu i HTML edytora
- `SeoScoreService.evaluate(contentHtml, seoTitle, seoDesc, coverAlt)` — seo/ — dziewięć warunków widoczności przepisanych 1:1 z `score()` w makiecie `admin-editor.js`. Ta sama lista leci na żywo w edytorze i przy zapisie — dwie różne listy podważyłyby zaufanie do obu. Zwraca `Result(score, label, hint, checks)`.
- `EditorHtml.toPublication(html)` — content/ — **jedyne miejsce zamiany** reprezentacji edytora (`q-fig`/`q-vid`/`q-pdf` + ozdoby panelu) na HTML publikacji z klasami `blog.css` (`figure`, `art-video yt-facade`, `art-pdf`). Wołane z `PostService.save()` PRZED sanitizerem — odwrotna kolejność zdjęłaby atrybuty `data-*`, z których zamiana czyta.
- `EditorHtml.toEditor(publicationHtml)` — content/ — **zamiana powrotna**, wołana przy wczytywaniu wpisu do edycji (`AdminPostController.toForm`). Bez niej edycja zapisanego wpisu KASOWAŁA media: Quill rozpoznaje bloki tylko po klasach `q-*`, więc `figure` bez klasy tracił źródło i alt, a `art-video` i `art-pdf` znikały. Podpis filmu oraz nazwa i rozmiar PDF-a jadą na wrapperze jako `data-*`, bo w samej karcie publikacji ich nie widać.
- `EditorHtml.requireImageAlts(html)` — content/ — odrzuca zapis, gdy zdjęcie w treści nie ma altu (`InvalidContentException` z komunikatem dla autora). Miniatura filmu jest wyłączona z kontroli — ma pusty alt celowo.
- `AiReadinessAnalyzer.analyse(post)` — seo/ — blok CONTENT siatki AI Readiness V4; liczy `post.ai_score` przy zapisie. Nie jest pokazywany w edytorze — tam obowiązuje lista z makiety.
- `POST /admin/posty/ocena` — admin/ — ocena szkicu bez zapisu. `POST /admin/posty/autozapis` i `/{id}/autozapis` — autozapis co 15 s; pierwszy autozapis nowego wpisu zakłada szkic i zwraca `id`.

## Panel
- `AdminController.changePassword()` — admin/ — `GET/POST /admin/haslo`; min. 12 znaków, po zmianie unieważnia sesję.
- `AdminPostController.preview(id)` — admin/ — `GET /admin/posty/{id}/podglad`: szkic w wyglądzie strony, z noindex.
- `AdminStatsController` — admin/ — `/admin/statystyki?dni=`; KPI z trendem, wykres CSS, top ścieżki, źródła, urządzenia i **wizyty botów AI**.
- `AnalyticsView` — analytics/ — przelicza surowe wyniki `PageViewRepository` na słupki, wiersze i trend (30 dni kontra 30 dni wcześniej). Wspólne dla pulpitu i statystyk — nie licz tego drugi raz w kontrolerze.
- `AdminNav.publishedPosts() / .newSubmissions()` — admin/ — liczniki przy pozycjach menu, wołane z szablonu przez `${@adminNav...}`. Świadomie NIE `@ControllerAdvice`: advice wchodzi do każdego wycinka `@WebMvcTest` i wywraca testy kontrolerów publicznych.
- `AdminPostController.persist(form)` — admin/ — jedyna ścieżka zapisu wpisu z panelu; dzielą ją formularz i autozapis.
- `PostStatus.label()/.badge()`, `SubmissionStatus.label()/.badge()`, `Post.dateLabel()`, `Submission.initials()` — etykiety i klasy plakietek gotowe do wyświetlenia. Szablony nie tłumaczą statusów ani nie formatują dat samodzielnie.
- `AdminTraineeController` + `TraineeService.fromSubmission(id)` — admin/, crm/ — kartoteka klientów i konwersja zgłoszenia na klienta (idempotentna).
- `AdminSettingsController` — admin/ — `/admin/ustawienia`; zapisuje przez `SettingsService`.
- `SettingsService.get/getInt/getBoolean/set` — settings/ — ustawienia z `app_setting` z cache. Klucze jako stałe w klasie. Używane przez `BlogController` (rozmiar strony, SEO) i `MailService` (odbiorca, autoodpowiedź).
- `window.sdDialog.alert({title, message})` / `.confirm({title, message, items, confirmLabel, cancelLabel, danger})` — static/js/admin.js — okna w szacie panelu zamiast natywnych `alert`/`confirm` przeglądarki. Zwracają Promise. Mają uwięziony fokus, Esc i klik w tło anulują, a przy `danger: true` fokus startuje na „Anuluj", żeby Enter nie skasował danych.
- `<form data-confirm="…" data-confirm-title="…" data-confirm-ok="…">` — admin.js przechwytuje submit i pyta w oknie panelu. Używaj tego zamiast `onsubmit="return confirm(…)"`; Thymeleaf i tak nie wpuszcza zmiennych tekstowych do atrybutów zdarzeń.
- `fragments/admin-layout :: pager(page, baseUrl)` — templates/fragments/ — paginacja list w panelu; `baseUrl` może już mieć parametry.
- `SubmissionService.export(id)` / `.delete(id)` — submission/ — RODO art. 15 i 17: komplet danych zgłoszenia do JSON-a i twarde usunięcie.

## Infrastruktura
- `GlobalExceptionHandler` — common/ — nadaje błędowi identyfikator, loguje go i pokazuje użytkownikowi; HTML albo JSON zależnie od ścieżki. Przepuszcza dalej wyjątki, które znają swój status: z `@ResponseStatus` (np. `NotFoundException`) oraz implementujące `ErrorResponse` (np. `NoResourceFoundException` → 404, nie 500).
- `HtmlSanitizer.clean(html)` — content/ — biała lista pokrywająca się z `blog.css`, ostatni etap zapisu treści. Ozdoby edytora zdejmuje wcześniej `EditorHtml`; sanitizer wycina je drugi raz jako zabezpieczenie. Kontraktu pilnuje `EditorHtmlTest`.
- Quill leży w `static/vendor/quill/` — zlecenie zabrania ładowania go z CDN.
- **Wersjonowanie zasobów statycznych** — `spring.web.resources.chain.strategy.content` w `application.yml` + odwołania przez `th:href="@{/css/…}"` / `th:src="@{/js/…}"`. Adres pliku zawiera skrót jego treści, więc 30-dniowy cache jest bezpieczny. Nowy plik CSS/JS podpinaj TYM sposobem — zwykłe `href="/js/x.js"` nie zostanie przepisane i poprawka nie dotrze do przeglądarki przez miesiąc.
- `templates/error.html` — ogólna strona błędu dla statusów bez własnego szablonu (`error/404.html`, `error/500.html`). Bez niej Spring szuka widoku „error", nie znajduje go i zamiast czystego 403 rzuca `TemplateInputException` — błąd w miejscu obsługi błędu.
- `CleanupScheduler.purgeOldPageViews()` — scheduler/ — codziennie 4:00 kasuje `page_view` starsze niż rok (retencja RODO).
- `SeoController.feed()` — seo/ — `/feed.xml`, kanał RSS z 20 ostatnich wpisów.
- `MediaService.delete(id)` — media/ — usuwa plik, zawartość i wpis z `urlCache`. Wywołuj DOPIERO po `PostService.postsUsing()`.
- `PostgresTestBase` — src/test/ — baza testów integracyjnych; Testcontainers albo zewnętrzna baza przez `-Dtest.db.url`.
- `DatabaseUrlEnvironmentPostProcessor` — config/ — przyjmuje JEDEN link `DATABASE_URL` z Coolify (`postgres://user:pass@host/db`) i rozbija go na `spring.datasource.{url,username,password}`. Obsługuje brak portu, `?sslmode=`, hasło zakodowane procentowo i przepuszcza gotowe `jdbc:`. **Musi** być w `META-INF/spring.factories` — jako `@Component` odpaliłby się po utworzeniu DataSource.
- `Dockerfile` — obraz pod Coolify: budowanie wielostopniowe, warstwy Spring Boota, użytkownik bez roota, strefa Europe/Warsaw, healthcheck `/actuator/health`. Kontener bezstanowy, bo pliki siedzą w bazie (`media_blob`) — nie podpinaj wolumenu.
- `deploy/COOLIFY.md` — komplet zmiennych środowiskowych, konfiguracja Gmaila (hasło aplikacji, ograniczenie nadawcy) i lista rzeczy do sprawdzenia po wdrożeniu.
- `deploy/` — jednostka systemd, konfiguracja nginx (jedno przekierowanie 301, `X-Forwarded-Proto`), skrypt kopii zapasowej.
