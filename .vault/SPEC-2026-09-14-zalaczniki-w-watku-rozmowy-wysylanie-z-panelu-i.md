# SPEC: Załączniki w wątku rozmowy: wysyłanie z panelu i odbiór od klientów

- **Data:** 2026-09-14
- **Sesja:** b44d4c65-b5a
- **Status:** zatwierdzony plan; kryteria akceptacji poniżej są kontraktem dla @security

---

# Załączniki w wątku rozmowy: wysyłanie z panelu i odbiór od klientów

## Context
Marcin chce dołączać pliki do odpowiedzi z panelu (zgłoszenie i klient) i widzieć w wątku pliki, które klient odeśle mailem. Dziś:
- `message.attachment_id → media_file` istnieje, ale każdy wywołujący podaje `null`; `sendEmail` nie dokleja pliku do maila.
- Odbiór IMAP (`crm/InboundMail`) tylko liczy załączniki i dopisuje „[pominięto załączniki: N]".
- Biblioteka mediów (`media_file` + `media_blob`) jest **publiczna**: `/media/**` i `/pliki/{id}` bez autoryzacji, kolejne ID, cache nginx 30 dni, lista w `/admin/media`, deduplikacja po sha256, brak sprawdzania treści pliku. Usunięcie zgłoszenia/klienta nie kasuje plików.

Załączniki od klientów to dane osobowe (często wyniki badań), więc **nie mogą trafić do biblioteki mediów**. Pliki trzymamy jak dotąd w Postgresie (kontener bezstanowy, backup `pg_dump` obejmuje pliki; `Dockerfile`, `deploy/COOLIFY.md`), ale w osobnych, prywatnych tabelach.

## Rozwiązanie

### 1. Migracja `V12__message_attachments.sql`
- `message_attachment(id bigserial, message_id bigint not null references message(id) on delete cascade, original_name varchar(255), mime_type varchar(100), size_bytes bigint, sha256 char(64), created_at)` + indeks po `message_id`.
- `message_attachment_blob(attachment_id bigint primary key references message_attachment(id) on delete cascade, data bytea not null)` — osobno, żeby lista w wątku nie ciągnęła bajtów (wzorzec `media_blob`).
- Kaskady: usunięcie zgłoszenia/klienta → `message` → załączniki i bajty (RODO). Stara kolumna `message.attachment_id` zostaje nieużywana (nie edytujemy wydanych migracji; bez danych).

### 2. `crm/AttachmentPolicy` (czysta logika, testowana jednostkowo)
- Rozpoznanie typu **po treści** (magiczne bajty: PDF, JPEG, PNG, GIF, WEBP, HEIC, ZIP/OOXML, OLE doc/xls), nie po nazwie ani nagłówku przeglądarki/maila. Bez nowych zależności (Tika niepotrzebna).
- Wysyłka z panelu: dozwolone PDF, JPG, PNG, WEBP, HEIC, DOCX/XLSX, DOC/XLS, TXT/CSV; do 5 plików, 10 MB na plik, 20 MB łącznie (limit uploadu 25 MB z `application.yml`, nginx 30 m).
- Odbiór: zapisujemy każdy typ poza wykonywalnymi/aktywnymi (exe, bat, cmd, js, vbs, scr, msi, jar, html/htm, svg) — te zostają tylko w skrzynce z dopiskiem; do 10 plików i 15 MB na plik, 25 MB na wiadomość; przekroczenie → dopisek „[plik.zip, 40 MB: za duży, jest w skrzynce]".
- Bezpieczna nazwa: bez ścieżek, znaków sterujących, max 150 znaków, zachowane polskie znaki.

### 3. `crm/AttachmentService`
- `store(messageId, name, bytes)` / `list(messageIds)` (jedno zapytanie na wątek, bez N+1) / `load(id)`.
- Encje `MessageAttachment`, `MessageAttachmentBlob`, repozytoria w `crm`.

### 4. Wysyłka z panelu
- Formularze w `admin/submission-detail.html` i `admin/client-profile.html`: `enctype="multipart/form-data"`, `<input type="file" name="pliki" multiple>`; `admin-crm.js` pokazuje wybrane pliki z rozmiarem i pozwala usunąć przed wysłaniem. Tryb „telefon" ukrywa pole.
- Kontrolery (`AdminSubmissionController.message`, `AdminTraineeController.message`): `@RequestParam(required = false) List<MultipartFile> pliki` → walidacja przez `AttachmentPolicy` przed wysyłką (błąd = flash, nic nie wychodzi).
- `MessageService.sendEmail(..., List<OutgoingFile> files)`: `helper.addAttachment(nazwa, ByteArrayResource, typ z treści)`; zapis `Message` + załączników w tej samej transakcji, także przy FAILED (trener widzi, co próbował wysłać). Parametr `attachmentId` usunięty.
- `GlobalExceptionHandler`: `MaxUploadSizeExceededException` na ścieżkach panelu → powrót z komunikatem „Pliki są za duże (maks. 20 MB)" zamiast strony błędu.

### 5. Odbiór od klientów
- `InboundMail.parse` zbiera części-załączniki z bajtami (czytanie z limitem `readNBytes(limit + 1)`, bez ładowania większych), wg reguł `AttachmentPolicy`.
- Pomijane jako „ozdoby": obrazki inline z `Content-ID` mniejsze niż 30 KB (logo w stopce). Wklejone zdjęcie inline większe od progu jest zapisywane.
- `InboundMailService` → `MessageService.recordInbound` zapisuje załączniki razem z wiadomością (jedna transakcja). Istniejące zabezpieczenia (StackOverflowError, próby, UID) obejmują też dekodowanie załączników.

### 6. Pobieranie i podgląd
- `GET /admin/zalaczniki/{id}` (tylko `ROLE_ADMIN` — już zapewnia `/admin/**`), `@Operation`:
  - `Content-Disposition: attachment` z nazwą przez `ContentDisposition.builder(...).filename(name, UTF_8)`,
  - `Content-Type` z rozpoznanej treści, dla nieznanych `application/octet-stream`,
  - `X-Content-Type-Options: nosniff`, `Cache-Control: private, no-store`.
- `GET /admin/zalaczniki/{id}/podglad` tylko dla obrazów JPEG/PNG/WEBP/GIF (po treści) z `inline` — do miniatur w wątku. Inne typy → 404.
- Wątek (oba widoki): pod treścią dymka lista plików (ikona, nazwa, rozmiar, link „Pobierz"), obrazy jako miniatury; wspólny fragment `fragments/thread-attachments`. Styl z istniejących tokenów `admin.css`.

### 7. Dokumentacja
- `.vault/CAPABILITIES.md`, `deploy/COOLIFY.md` (pliki w bazie, wpływ na rozmiar backupu), ADR w `memory/decisions/` (prywatne tabele zamiast biblioteki mediów).

## Kryteria akceptacji
1. Trener wysyła z panelu mail z 1–5 plikami; klient dostaje je jako załączniki, a w wątku widać je pod wiadomością z możliwością pobrania.
2. Plik niedozwolony (np. `.exe` przemianowany na `.pdf`) albo za duży jest odrzucony przed wysyłką z czytelnym komunikatem; nic nie wychodzi.
3. Odpowiedź klienta z PDF/zdjęciem pokazuje plik w wątku; plik za duży lub wykonywalny zostaje w skrzynce z dopiskiem.
4. Logo ze stopki maila klienta nie pojawia się jako załącznik.
5. Pliki pobiera wyłącznie zalogowany admin; brak publicznego URL, brak cache, `nosniff`, `attachment`.
6. Podgląd inline tylko dla obrazów rozpoznanych po treści; SVG/HTML nigdy inline.
7. Usunięcie zgłoszenia lub klienta usuwa jego załączniki z bazy.
8. Załączniki z wątku nie pojawiają się w bibliotece mediów.

## Weryfikacja
- Jednostkowe: `AttachmentPolicyTest` (magiczne bajty, podmieniona nazwa, limity, nazwy), `InboundMailTest` (załącznik zapisany, za duży, wykonywalny, logo inline), `ReplySendTest` (część z załącznikiem w MIME), kontroler pobierania (nagłówki).
- IT na Postgresie (zewnętrzna pusta baza): zapis + pobranie, 302/403 bez logowania, kaskada przy usunięciu zgłoszenia, odbiór z `FakeMailbox` z załącznikiem.
- `mvn clean test` (JDK 21), `GanwilkArchitectureTest` + `git status archunit_store`, `/simplify`, `/code-review`, subagent `security` (sondy: podmiana typu, bomba w załączniku, nazwa z `../` i CRLF, SVG, dostęp bez sesji).
- Zrzuty wątku z załącznikami i formularza z plikami (1440 / 390).

