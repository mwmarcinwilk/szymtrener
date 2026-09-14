# SPEC: Odpowiedzi klientów z poczty w wątku rozmowy

- **Data:** 2026-09-14
- **Sesja:** b44d4c65-b5a
- **Status:** zatwierdzony plan; kryteria akceptacji poniżej są kontraktem dla @security

---

# Odpowiedzi klientów z poczty w wątku rozmowy

## Context
Wątek w panelu (`message`, ADR 2026-08-28 „Rozmowa z klientem w panelu zamiast mailto") pokazuje zgłoszenie z formularza, odpowiedzi trenera i notatki. Gdy klient odpisze na maila, wiadomość zostaje tylko w skrzynce: aplikacja nie odbiera poczty. Wątek przestaje być historią kontaktu, a to był cały sens ADR. Cel: odpowiedź klienta sama pojawia się w wątku (po lewej, jak zgłoszenie), a panel pokazuje znacznik „nowa odpowiedź" (decyzja Marcina: znacznik w panelu, bez dodatkowego maila).

Stan zastany, który to ułatwia:
- Widok wątku już rysuje `MessageDirection.IN` po lewej i `MessageChannel.EMAIL` z ikoną maila (`admin/submission-detail.html`), więc odebrana wiadomość to zwykły `Message(IN, EMAIL)`.
- Provider IMAP jest już na ścieżce klas (`org.eclipse.angus:jakarta.mail` 2.0.4 ze `spring-boot-starter-mail`), Jsoup 1.18.3 też. **Zero nowych zależności.**
- `@EnableScheduling` jest włączone (`SzymtrenerApplication`), wzorzec schedulera: `scheduler/PublishScheduler`.

## Rozwiązanie

### 1. Migracja `V11__inbound_mail.sql`
- `message.mail_message_id varchar(998)`: nagłówek `Message-ID` wiadomości wychodzącej i przychodzącej. Unikalny indeks częściowy (`where mail_message_id is not null`): ta sama poczta nie wpadnie dwa razy.
- `message.unread boolean not null default false`: tylko dla odebranych.
- `message.matched_by varchar(20)`: `REPLY` (po nagłówku) albo `ADDRESS` (po adresie nadawcy), null dla reszty.
- Stan odbioru w `app_setting` przez `SettingsService`: `mail.inbox.uidvalidity`, `mail.inbox.lastuid`.

### 2. Wysyłka zapamiętuje `Message-ID`
`MessageService.sendEmail`: po `sender.send(mime)` zapis `mime.getMessageID()` do `Message.mailMessageId`. Ten sam wiersz, żadnej zmiany zachowania wysyłki.

### 3. Odbiór: `crm/InboundMailService` + `scheduler/InboundMailScheduler`
- Co 2 minuty (`fixedDelay`), gdy `app.mail.inbox.enabled=true` (env `MAIL_INBOX_ENABLED`, domyślnie `false`: bez tego lokalnie i w testach nic nie łączy się z pocztą).
- Połączenie `imaps` (tylko SSL): host `MAIL_IMAP_HOST` (domyślnie `ssl0.ovh.net`), port `MAIL_IMAP_PORT` (993), login i hasło z istniejących `MAIL_USER`/`MAIL_PASSWORD`. Timeouty jak dla SMTP (5 s).
- **Nie używa flagi „przeczytane"**: Szymon czyta tę samą skrzynkę w Mailu na Macu, więc flaga SEEN nic nie znaczy. Postęp trzyma UID: pobieramy wiadomości z UID > `lastuid`. Zmiana `UIDVALIDITY` → start od bieżącego końca skrzynki (bez importu całej historii). Pierwsze uruchomienie tak samo: import tylko nowych.
- Skrzynka tylko do odczytu (`Folder.READ_ONLY`), nic nie jest kasowane ani przenoszone.
- Pomija wiadomości wysłane z naszego adresu (`From == MAIL_FROM`) i z nagłówkiem `Auto-Submitted` ≠ `no` (autorespondery, „jestem na urlopie").
- Błąd połączenia: log WARN, `lastuid` bez zmian, następna próba za 2 minuty. Błąd jednej wiadomości nie blokuje pozostałych (granica błędu = pojedyncza wiadomość, jak w rozwiązaniu SnakeYAML z ddd).
- Przed implementacją: Context7 dla Angus Mail (`UIDFolder.getMessagesByUID`, `getUIDValidity`, właściwości `mail.imaps.*`), zgodnie z twardą regułą.

### 4. Przypisanie do wątku: `crm/InboundMatcher` (czysta logika, bez IMAP)
1. `In-Reply-To`, potem `References` → `Message` wychodzący z tym `mail_message_id` → jego `submissionId`/`traineeId`. `matched_by = REPLY`.
2. Brak dopasowania → adres nadawcy (bez rozróżniania wielkości liter): klient (`Trainee`) z tym adresem, a gdy go nie ma, najnowsze zgłoszenie z tym adresem. `matched_by = ADDRESS`; w wątku mała etykieta „dopasowano po adresie", bo nagłówek `From` da się podrobić.
3. Nic nie pasuje → wiadomość pomijana (zostaje w skrzynce, log INFO bez treści).
- Nowe metody repozytoriów: `MessageRepository.findFirstByMailMessageIdIn(...)`, `existsByMailMessageId`, `TraineeRepository.findFirstByEmailIgnoreCase`, `SubmissionRepository.findFirstByEmailIgnoreCaseOrderByCreatedAtDesc`.

### 5. Treść: `crm/InboundMailText`
- Część `text/plain`; gdy jej brak, `text/html` → tekst przez Jsoup (`Jsoup.parse(html).wholeText()` z zachowaniem akapitów). HTML nigdy nie trafia do widoku; wątek i tak renderuje przez `th:text`.
- Ucięcie cytatu: od pierwszej linii „W dniu … napisał(a):" / „On … wrote:" / „-----Original Message-----" / bloku linii zaczynających się od `>`. Gdy po ucięciu nic nie zostaje, zapis całości.
- Limit 20 000 znaków (dłuższe ucinane z dopiskiem), załączniki pomijane (w wątku dopisek „[pominięto załączniki: 2]").
- `sentAt` = data wysłania z nagłówka, a gdy brak lub z przyszłości: czas odbioru.

### 6. Znacznik „nowa odpowiedź"
- Odebrana wiadomość ma `unread = true`.
- `AdminNav.unreadReplies()`: licznik przy „Zgłoszenia" i „Klienci" (obok istniejącego `newSubmissions`).
- Listy `admin/submissions.html` i `admin/clients.html`: kropka przy pozycji z nieprzeczytaną odpowiedzią (jedno zapytanie zbiorcze po ID z bieżącej strony, bez N+1).
- Otwarcie wątku (`AdminSubmissionController.detail`, profil klienta w `AdminTraineeController`) oznacza odpowiedzi jako przeczytane (`@Modifying update` w serwisie, nie w kontrolerze).

### 7. Konfiguracja i dokumentacja
- `application.yml`: `app.mail.inbox.{enabled,host,port}`; `AppProperties.Mail` rozszerzony o `Inbox`.
- `deploy/COOLIFY.md`: sekcja poczty dla OVH (SMTP 587 + IMAP 993) i nowe zmienne.
- `admin/settings.html`: w karcie Poczta tylko do odczytu „Odbiór odpowiedzi: włączony / wyłączony, ostatnie sprawdzenie: …".
- `.vault/CAPABILITIES.md`, ADR `memory/decisions/` (odbiór IMAP z UID zamiast webhooka / flagi SEEN).

## Kryteria akceptacji
1. Klient odpowiada na mail wysłany z panelu → w ciągu ~2 min wiadomość jest w wątku tego zgłoszenia/klienta po lewej, bez cytatu poprzedniej wiadomości.
2. Klient pisze nowego maila (bez `In-Reply-To`) z adresu zgłoszenia → trafia do wątku z etykietą „dopasowano po adresie".
3. Mail od nieznanego nadawcy, autoresponder i kopia własnej wysyłki nie tworzą wpisu.
4. Ta sama wiadomość pobrana dwukrotnie (restart, powtórzony UID) daje jeden wpis.
5. Skrzynka nie jest modyfikowana: flagi, foldery i wiadomości bez zmian (Mail na Macu widzi wszystko jak dotąd).
6. Treść HTML z maila nie jest wykonywana w panelu (`<script>`, `<img onerror>` widoczne jako tekst albo usunięte).
7. Lista i menu pokazują znacznik nowej odpowiedzi; znika po otwarciu wątku.
8. Brak `MAIL_INBOX_ENABLED` albo błąd logowania IMAP nie wywraca aplikacji ani healthchecku.

## Weryfikacja
- Testy jednostkowe (bez sieci), wiadomości budowane jako `MimeMessage` w pamięci: `InboundMatcherTest` (REPLY, References, ADDRESS, brak dopasowania, autoresponder, własny adres), `InboundMailTextTest` (plain, sam HTML, cytat PL/EN, `>`, limit, załączniki, XSS).
- `InboundMailServiceTest`: mock `Store`/`Folder` przez interfejs cienkiej fasady IMAP; UID > lastuid, zmiana UIDVALIDITY, błąd jednej wiadomości, duplikat.
- IT na Postgresie (`PostgresTestBase`, zewnętrzna pusta baza): V11, unikalny `mail_message_id`, oznaczanie jako przeczytane, licznik bez N+1.
- `mvn clean test` na JDK 21, `GanwilkArchitectureTest`, potem `/simplify`, `/code-review`, subagent `security` (w tym sonda: podrobiony `From`, HTML z maila, gigantyczna wiadomość, duplikat).
- Ręcznie na prawdziwej skrzynce OVH (po Twojej zgodzie, z lokalnie ustawionymi zmiennymi): wysyłka z panelu → odpowiedź z Maila na Macu → wpis w wątku i znacznik. Zrzuty listy i wątku (Playwright).

