---
name: reference-it-bez-dockera
description: How to run the full szymtrener suite when Docker/Testcontainers is unavailable, and how parallel Maven runs pollute surefire reports
metadata:
  type: reference
---

Od 2026-09-17 Docker działa: `mvn -DargLine="-Dapi.version=1.43" clean verify` (JDK 21 corretto) puszcza surefire + wszystkie IT na Testcontainers (wtedy 204 + 46, po zgodzie cookies 204 + 48), ok. 15 s na kopii w scratchpadzie (`rsync --exclude target --exclude .git`). Komendę z `rm -rf` i `cd` uprawnienia odrzucają: nowy katalog kopii + `mvn -f <kopia>/pom.xml`; wynik licz z XML w `target/*-reports`, nie z `-q` logu. Główny wątek podaje `clean test` i raportuje sam surefire jako „pełny zestaw” — to pomija nowe IT.

Pełny zestaw w szymtrener = surefire (`mvn -q clean test`, JDK 21) + WSZYSTKIE `*IT` przez failsafe.
Bez Dockera IT idą na zewnętrznej, pustej bazie Postgres.app:
`createdb -h localhost <nazwa>` i `mvn verify -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dtest.db.url=jdbc:postgresql://localhost:5432/<nazwa> -Dtest.db.user=$(whoami) -Dtest.db.password=x`.
Bez `-Dit.test` failsafe bierze wszystkie IT (2026-09-14: ApplicationContextIT, ReplyTemplateMigrationIT, AdminAccountSyncIT, PostFlowIT).

Sonda na skompilowanych klasach (np. regex ze `static` pola): `target/classes` nie wystarcza, bo klasy serwisów ładują Thymeleaf/Spring; classpath z `mvn -q -o dependency:build-classpath -Dmdep.outputFile=<scratchpad>/cp.txt` (nie rusza `target/classes`), uruchamiać z `-Xss1m`.

Sondy parsera poczty: MimeMessage z surowego tekstu (`new MimeMessage(session, InputStream)`), a odpowiedź IMAP bez serwera przez `new org.eclipse.angus.mail.imap.protocol.FetchResponse(new IMAPResponse("* 1 FETCH (...)"))`. Zawsze testuj bombę QP `"=\r\n".repeat(50_000)` (Angus rekurencyjny, SOE od ~10k) i pętlę `poll` z FakeMailbox, bo `Error` omija `catch (Exception)`. Sondy w wątku z `-Xss`/1 MB: `mockStatic` otwieraj WEWNĄTRZ tego wątku (jest thread-local), inaczej scheduler łączy się z prawdziwym hostem i sonda kłamie.

Sondy nazw plików/uploadu: kopiuj `target/classes` do scratchpadu PRZED `mvn verify` (ten przebudowuje klasy), `rm -rf` bywa zablokowane, więc kopiuj do nowego katalogu. Zawsze próbuj nazw z końcową kropką/spacją (`evil.js.`) i znakami Cf (U+202E, U+200B): `\p{Cntrl}` ich nie łapie. Do tego NBSP/U+3000 na końcu (Java `\s` i `strip()` ich nie zdejmują) oraz „rozszerzenie" dłuższe niż limit nazwy (ujemny `substring`). Fuzz nazw (50k, seed stały): wyrocznia musi obcinać także POCZĄTKOWE kropki, inaczej `.hta` → `hta` (bez rozszerzenia, niegroźne) daje ~3% fałszywych „obejść”. Cięcie do limitu po `char` potrafi rozciąć parę surogatów (emoji) — drobne, nie blokuje.

Od 2026-09-17 `~/.docker-java.properties` ma api.version=1.43, więc `-DargLine` zbędny (204 + 50 po springdoc). Sonda ścieżek dokumentacji/zasobów: IT w kopii z pętlą `mvc.perform(get(p))` i wydrukiem statusu; zawsze warianty-rodzeństwa (`/v3/api-docs.yaml`), `/webjars/<lib>/**`, końcowy `/`, wielkość liter. springdoc: `.yaml` omija matcher `/v3/api-docs/**`. Sondy wariantów ścieżek (wielkość liter, `;`, `%2e`) rób na RANDOM_PORT surowym socketem: MockMvc nie ma dekodowania Tomcata. 404 Boota echo-uje `path`, więc „body zawiera X" daje fałszywy wyciek.

Szablony Thymeleaf: `th:data-*`/`th:on*`/preprocessing `__${}__` to tryb ograniczony, `T(`/`new` rzuca TemplateProcessingException w połowie strony (status 200). `th:text` z `T(` jest dozwolony (7 miejsc w admin/*). Skan: perl -0777 po `th:[a-z:-]+="..."` (atrybuty bywają wieloliniowe). Beany `${@x}` są dozwolone. Sonda IT: podmień szablon z kopii, uruchom `-Dit.test=...`, przywróć, potem `mvn -q process-resources`, bo verify wgrał zepsuty szablon do `target/classes` (2026-09-15).

**Why:** główny wątek wskazuje zwykle tylko IT „od zmiany”; to nie jest pełny zestaw. Baz nie kasujemy (hook blokuje DROP DATABASE), tylko wymieniamy nazwy w raporcie.
**How to apply:** przed werdyktem sprawdź `pgrep -fl java | grep szymtrener`, bo przegląd jakości potrafi równolegle puścić Mavena. Raporty w `target/surefire-reports` z mtime późniejszym niż koniec Twojego przebiegu pochodzą od kogoś innego (tak wyglądał fałszywy błąd Docker w ReplyTemplateMigrationIT). Licz wynik z exit code i logu własnego przebiegu. Powtórzyło się 2026-09-14 (drugi przegląd): raport ReplyTemplateMigrationIT z błędem Docker zapisany 4 s po końcu mojego `mvn clean test`, choć `pgrep` nic nie pokazał. Surefire domyślnie nie bierze `*IT`, więc taki raport w `surefire-reports` zawsze jest obcy.
