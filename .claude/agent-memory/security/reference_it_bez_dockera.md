---
name: reference-it-bez-dockera
description: How to run the full szymtrener suite when Docker/Testcontainers is unavailable, and how parallel Maven runs pollute surefire reports
metadata:
  type: reference
---

Pełny zestaw w szymtrener = surefire (`mvn -q clean test`, JDK 21) + WSZYSTKIE `*IT` przez failsafe.
Bez Dockera IT idą na zewnętrznej, pustej bazie Postgres.app:
`createdb -h localhost <nazwa>` i `mvn verify -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dtest.db.url=jdbc:postgresql://localhost:5432/<nazwa> -Dtest.db.user=$(whoami) -Dtest.db.password=x`.
Bez `-Dit.test` failsafe bierze wszystkie IT (2026-09-14: ApplicationContextIT, ReplyTemplateMigrationIT, AdminAccountSyncIT, PostFlowIT).

Sonda na skompilowanych klasach (np. regex ze `static` pola): `target/classes` nie wystarcza, bo klasy serwisów ładują Thymeleaf/Spring; classpath z `mvn -q -o dependency:build-classpath -Dmdep.outputFile=<scratchpad>/cp.txt` (nie rusza `target/classes`), uruchamiać z `-Xss1m`.

Sondy parsera poczty: MimeMessage z surowego tekstu (`new MimeMessage(session, InputStream)`), a odpowiedź IMAP bez serwera przez `new org.eclipse.angus.mail.imap.protocol.FetchResponse(new IMAPResponse("* 1 FETCH (...)"))`. Zawsze testuj bombę QP `"=\r\n".repeat(50_000)` (Angus rekurencyjny, SOE od ~10k) i pętlę `poll` z FakeMailbox, bo `Error` omija `catch (Exception)`. Sondy w wątku z `-Xss`/1 MB: `mockStatic` otwieraj WEWNĄTRZ tego wątku (jest thread-local), inaczej scheduler łączy się z prawdziwym hostem i sonda kłamie.

**Why:** główny wątek wskazuje zwykle tylko IT „od zmiany”; to nie jest pełny zestaw. Baz nie kasujemy (hook blokuje DROP DATABASE), tylko wymieniamy nazwy w raporcie.
**How to apply:** przed werdyktem sprawdź `pgrep -fl java | grep szymtrener`, bo przegląd jakości potrafi równolegle puścić Mavena. Raporty w `target/surefire-reports` z mtime późniejszym niż koniec Twojego przebiegu pochodzą od kogoś innego (tak wyglądał fałszywy błąd Docker w ReplyTemplateMigrationIT). Licz wynik z exit code i logu własnego przebiegu. Powtórzyło się 2026-09-14 (drugi przegląd): raport ReplyTemplateMigrationIT z błędem Docker zapisany 4 s po końcu mojego `mvn clean test`, choć `pgrep` nic nie pokazał. Surefire domyślnie nie bierze `*IT`, więc taki raport w `surefire-reports` zawsze jest obcy.
