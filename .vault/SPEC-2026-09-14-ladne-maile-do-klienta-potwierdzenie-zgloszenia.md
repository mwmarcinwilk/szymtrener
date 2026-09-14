# SPEC: Ładne maile do klienta: potwierdzenie zgłoszenia i odpowiedzi z panelu

- **Data:** 2026-09-14
- **Sesja:** b44d4c65-b5a
- **Status:** zatwierdzony plan; kryteria akceptacji poniżej są kontraktem dla @security

---

# Ładne maile do klienta: potwierdzenie zgłoszenia i odpowiedzi z panelu

## Context
Marcin zgłosił dwa problemy z mailami wychodzącymi do klienta:
1. **Automatyczne potwierdzenie** (`templates/mail/confirm-client.html` + wersja tekstowa w `MailService.plainAutoReply`) wita pełnym polem imię+nazwisko („Cześć Jak Kowalski,") i zaczyna akapit małą literą („dziękuję…"), choć powitanie jest osobnym nagłówkiem. Treść NIE jest zatwierdzona przez klienta — można ją poprawiać.
2. **Odpowiedź z panelu** (`MessageService.sendEmail`) wychodzi jako goły tekst (`helper.setText(body, false)`), bez formatki. Szablon „Pierwsza odpowiedź" (seed w `V8__crm_thread.sql`) ma `{kontekst}` w środku zdania; gdy zgłoszenie nie ma opisu treningu, wychodzi „Przeczytałem, co napisałeś — . Proponuję…". Imię bierze kontroler (`AdminSubmissionController.template`, l. 164) bez wielkiej litery. W szablonach są też półpauzy jako łączniki.

Cel: oba rodzaje maili wyglądają jak jedna, spójna formatka marki; tekst czysty, bez pustych wstawek.

## Zmiany

### A. Wspólne części formatki
- Nowy `templates/mail/parts.html` z fragmentami Thymeleaf: `header` (granatowy pasek „Szymon Domagała · Trener personalny · Trener Longevity") i `contact` (adres, telefon, link do strony). Wycięte z `confirm-client.html`, style nadal w atrybutach, układ tabelowy (patrz CAPABILITIES l. 41).
- `confirm-client.html` używa fragmentów (wygląd bez zmian).

### B. Potwierdzenie zgłoszenia
- `Submission.firstName()` (`@Transient`, obok `initials()`): pierwszy wyraz pola `name`, pierwsza litera wielka (locale pl), pusty łańcuch dla braku imienia.
- `confirm-client.html` i `MailService.plainAutoReply`: „Cześć Marta!" + „Dziękuję za wiadomość…". Usunąć komentarz „zatwierdzona przez klienta — nie przepisywać". Stopka bez półpauzy: „Jeśli to nie Ty, po prostu ją zignoruj."

### C. Odpowiedź z panelu w formatce
- Nowy `templates/mail/reply.html`: header, treść wiadomości, `contact`. Treść jest dzielona w Javie na akapity (pusta linia) i linie; szablon renderuje je `th:each` + `th:text` z `<br>` między liniami. **Nigdy `th:utext`**: treść pisze człowiek w panelu i ma być escapowana.
- `MessageService.sendEmail`: multipart, `helper.setText(body, templates.process("mail/reply", ctx))` (tekst = dokładnie to, co wpisał trener; HTML = formatka). Wstrzyknięcie `SpringTemplateEngine` jak w `MailService`. Brak zmian w zapisie do wątku i obsłudze błędów.
- Pole `name` w `sendEmail` jest dziś nieużywane; zostaje bez zmian.

### D. Szablony odpowiedzi
- `MessageService.fill`: zdanie zawierające `{kontekst}` jest usuwane, gdy kontekst jest pusty; linia, w której nic nie zostało, znika razem z nadmiarowym odstępem (zmiana po /code-review: wycinanie całej linii/akapitu zabierało sąsiednie zdania w szablonach poprawionych przez trenera). Placeholdery wypełniane ręcznie (`{powod}`, `{kierunek}`) zostają widoczne jak dotąd.
- `AdminSubmissionController.template`: `s.firstName()` zamiast ręcznego splitu.
- Migracja **`V10__reply_templates_copy.sql`**: `UPDATE reply_template SET body = <nowy> WHERE code = ? AND body = <treść z V8>` dla 5 szablonów. Szablony poprawione już przez trenera w panelu zostają nietknięte. Nowe treści: wielka litera po powitaniu, bez półpauz, `{kontekst}` w osobnym akapicie w cudzysłowie, np.:

  ```
  Cześć {imie}!

  Dzięki za zgłoszenie.

  Napisałeś o swoim treningu: „{kontekst}”.

  Proponuję 20 minut rozmowy, żebym poznał Twój tydzień i historię treningową. Bez zobowiązań, po prostu sprawdzimy, czy mogę Ci realnie pomóc.

  Pasuje Ci czwartek o 18:00 albo piątek o 17:30?

  Pozdrawiam,
  Szymon
  ```
  Pozostałe cztery: to samo (wielka litera, przecinki/dwukropki zamiast „—"), sens bez zmian.
- `admin/settings.html` l. 187: dopisać, że linia z `{kontekst}` znika, gdy klient nic nie napisał.

### E. Dokumentacja
- `.vault/CAPABILITIES.md`: `Submission.firstName()`, `mail/reply.html` + `parts.html`, reguła usuwania linii z pustym `{kontekst}`.

## Kryteria akceptacji
1. Potwierdzenie dla „jan kowalski" zaczyna się „Cześć Jan!" i „Dziękuję za wiadomość" (HTML i tekst); bez imienia „Cześć!".
   _Zmiana w trakcie implementacji (2026-09-14): wykrzyknik zamiast przecinka, bo po „Cześć Jan," polska norma wymaga małej litery, a zgłoszenie dotyczyło właśnie wielkiej. To samo w szablonach odpowiedzi (sekcja D)._
2. Odpowiedź z panelu (zgłoszenie i klient) wychodzi jako multipart z formatką; tekstowa część równa treści z panelu.
3. `<script>` w treści odpowiedzi jest w HTML-u escapowany.
4. „Pierwsza odpowiedź" dla zgłoszenia bez opisu treningu nie zawiera „—", „: „”" ani pustej wstawki.
5. Szablon zmieniony wcześniej przez trenera nie jest nadpisywany przez V10.
6. W wysyłanych szablonach i formatkach brak półpauzy jako łącznika.

## Weryfikacja
- `MailTemplatesTest`: firstName + wielka litera; render `reply.html` (akapity, `<br>`, escapowanie).
- Test jednostkowy `MessageService.fill` (pusty/niepusty kontekst, `{powod}` zostaje) — mock repozytorium.
- IT na `PostgresTestBase`: po migracjach `first` ma nową treść; wariant z ręcznie zmienionym body przed V10 niemożliwy w IT bez hacka, więc sprawdzić warunek `WHERE body =` przez test zmieniający wiersz i powtórny `UPDATE` z pliku, albo przegląd SQL (decyzja przy implementacji, najprostsza działająca).
- `mvn -q clean test` (w tym `GanwilkArchitectureTest`), potem `/simplify`, `/code-review`, subagent `security`.
- Render obu formatek do pliku HTML i zrzut przez Playwright (390 / 768 / 1440) jako pętla wizualna.

