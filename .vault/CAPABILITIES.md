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
- `AdminAccountInitializer.syncAdminAccount(...)` — admin/ — trzyma konto zgodne z `ADMIN_EMAIL`/`ADMIN_PASSWORD`: zmiana zmiennej i restart AKTUALIZUJĄ konto (zmiana adresu zamiast duplikatu). Hasło ustawione w panelu przeżywa restart, bo porównywany jest odcisk SHA-256 ostatnio zastosowanej pary (`app_setting`, `admin.env.fingerprint`). Pilnuje tego `AdminAccountSyncIT` — cztery scenariusze, w tym „panel wygrywa, gdy env bez zmian".
- `AdminNav.currentName() / .currentInitials()` — admin/ — nazwa i inicjały zalogowanej osoby w stopce menu. Do tej pory `display_name` było zapisywane, ale NIGDZIE nie wyświetlane — stopka pokazywała adres e-mail, a inicjały były wpisane na sztywno.
- `AdminUsersController` — admin/ — `/admin/administratorzy`: dodawanie i usuwanie kont z dostępem do panelu (link z Ustawień). Trzy blokady: nie da się usunąć ostatniego konta, nie da się usunąć konta, na którym się pracuje, hasło min. 12 znaków. Edycja konta (`/admin/administratorzy/{id}`): nazwa wyświetlana, adres, hasło (puste = bez zmian) i włączenie konta; zmiana własnego adresu wylogowuje, bo adres jest loginem. Konto ze zmiennych środowiskowych jest na tym ekranie pokazane jako awaryjne wejście z instrukcją wyłączenia — inaczej „usunąłem konto, a dalej działa" wyglądałoby jak awaria.
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
- `SettingsService.MAIL_ENABLED` — główny wyłącznik poczty w Ustawieniach. Wyłączony: formularze nadal zapisują się do bazy i widać je w panelu, ale nic nie wychodzi. `MailService` kończy wtedy wcześnie i **nie ustawia `mail_error`** — to decyzja, nie awaria, więc oś czasu zgłoszenia nie może świecić na czerwono. Stan widać na pasku w Ustawieniach i na liście Zgłoszeń.
- `templates/mail/notify-trainer.html` i `confirm-client.html` — formatki wysyłane jako multipart (HTML + wersja tekstowa). Style **w atrybutach**, układ na tabelach: Gmail i Outlook wycinają `<style>` z nagłówka. Pilnuje ich `MailTemplatesTest` — inaczej błąd w szablonie wyszedłby dopiero przy prawdziwym zgłoszeniu.
- `MailConfig` — wypisuje przy starcie, co realnie wczytano (host, port, zamaskowany login, DŁUGOŚĆ hasła). Usuwa spacje z hasła aplikacji Google i mówi o tym w logu — wklejone „jak widać", w grupach po cztery, jest najczęstszą przyczyną błędu 535-5.7.8.
- `POST /admin/ustawienia/test-poczty` — przycisk „Wyślij wiadomość testową"; komunikat serwera pocztowego wraca wprost na ekran.
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

## Oferta online (brief programisty v2.2)
- `OnlineOfferService` — offer/ — jedyne miejsce, które zamienia dane pakietów na to, co widzi klient. `packages()` zwraca `PackageView` z gotowymi napisami; `money(grosze)` formatuje kwotę spacjami NIEROZDZIELAJĄCYMI („1 074 zł" — nie łamie się na końcu wiersza). `lowestMonthly()` daje cenę „od X" do JSON-LD i FAQ, żeby żadna kopia ceny nie została w HTML.
- `OnlinePackage` — kwoty w GROSZACH (`int`), dwa poziomy: startowy i docelowy. `effectiveMode()` sam przechodzi na cenę docelową, gdy `seatsLeft() == 0` — bez klikania w panelu. `badgePromotional` odróżnia plakietkę „CENA STARTOWA" (znika po zamknięciu naboru) od „Najlepszy wybór" (zostaje).
- `Testimonial.signature()` — podpis pod imieniem z formatu współpracy i czasu trwania; oba pola mogą być puste, wtedy podpis się nie renderuje. `OnlineFaq.answered()` — pytanie bez odpowiedzi nie trafia na stronę.
- `AdminOfferController` — admin/ — `/admin/oferta`: ceny obu ścieżek, liczniki miejsc, tryb ceny, opinie i FAQ. Kwoty wpisuje się w ZŁOTYCH; `grosze(String)` / `zlote(int)` robią zamianę i odrzucają wejście, którego nie da się odczytać (null zamiast cichego zera).
- Cena Ścieżki 1 (konsultacja + plan) siedzi w `SettingsService.OFFER_CONSULT_PRICE_GR` — jedna kwota, bez własnej tabeli.
- `Submission.offerPath` / `offerPackage` + `offerContext()` — z którego CTA przyszło zgłoszenie. Widać to w mailu do trenera i w karcie zgłoszenia. Ustawiają je ukryte pola formularza, wypełniane przez `data-path` / `data-package` na przyciskach (`main.js`).
- Sekcje strony: `#online` (dwie ścieżki + pakiety), `#opinie`, `#faq-online` (akordeon na `<details>` — działa bez JS i czytają go boty). Każda znika w całości, gdy nie ma czego pokazać — bez placeholderów „wkrótce".

## Cennik stacjonarny (brief „Ceny treningów stacjonarnych" v1.0)
- `StationaryOfferService` — offer/ — cennik gotowy do wyświetlenia plus zdania do FAQ. `priceSentence()` i tabela cennika w FAQ powstają z tych samych liczb co karty w sekcji oferty: brief 5.3 wymaga jednego źródła, bo cennik jest na stronie dwa razy.
- `StationaryPackage` — trzyma TYLKO cenę za jeden trening. Kwota „razem" (`totalGr()`) i rabat (`discountPercent(cenaPojedynczego)`) są liczone; trzy kolumny opisujące tę samą cenę rozjeżdżają się przy pierwszej zmianie. Rabat liczy się w obrębie rodzaju: cena pary jest łączna za dwie osoby, więc porównanie z ceną indywidualną nie miałoby sensu.
- `validityWeeks` = null oznacza wejście bez terminu, nie zero tygodni. `StationaryOfferService.validityLabel()` odmienia: „4 tygodnie", „6 tygodni", „22 tygodnie".
- `Money.format(gr)` / `Money.amount(gr)` — offer/ — wspólne formatowanie kwot dla oferty online i stacjonarnej. `format` dokleja „zł" po spacji nierozdzielającej, `amount` zwraca samą kwotę do zapisów typu „od 200 do 240 zł". `OnlineOfferService.money()` deleguje tutaj.
- `AdminOfferController` obsługuje dwa ekrany pod jedną pozycją menu: `/admin/oferta` (online) i `/admin/oferta/stacjonarnie` (ceny, ważność, zasady odwołań). Cennik stacjonarny edytuje się w wierszu, bez wchodzenia na osobny ekran — najczęstsza operacja to poprawienie jednej kwoty.
- `fragments/stationary-rows :: rows(lista)` — wiersze cennika w panelu, wspólne dla treningów indywidualnych i par.
- `SettingsService` + klucze `stationary.rules.{cancel,late,pause}` — zasady odwołań i pauzy jako zwykły tekst z panelu. Pokazują się przy cenniku i w FAQ, bo brief traktuje pauzę jako argument sprzedażowy, nie zapis regulaminu.
- Sekcje strony: `#oferta` (cennik indywidualny + zasady), `#pary` (własna ścieżka z opisem bariery logistycznej), `#zdalnie` (online i dietetyka). Zakładki `showTab` zniknęły razem z kodem JS i CSS — pary przestały być drugą zakładką w cenniku.
