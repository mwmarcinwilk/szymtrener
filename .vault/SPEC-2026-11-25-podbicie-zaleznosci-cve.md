# SPEC: Podbicie zależności z podatnościami (CVE)

- **Data:** 2026-11-25
- **Sesja:** 6295df54
- **Status:** zatwierdzony plan (Marcin: „podnieś, rób wszystko”); kryteria akceptacji poniżej są kontraktem dla @security

## Context
`cve_watch.py status .` (skan 25.11 10:19): 91 podatności w 23 pakietach (18 krytycznych, 33 wysokie,
31 średnich, 9 niskich), 0 z listy CISA KEV (katalog 2026.11.24). Prawie wszystkie pakiety są zarządzane
przez `spring-boot-starter-parent` 3.5.5, więc główny ruch to podbicie parenta w linii 3.5 (3.5.16 to
najnowsza w Maven Central), a resztę domykają nadpisania wersji.

Zasada wyboru wersji: łatka w tej samej linii = najnowsza łatka; skok minor = najniższa wersja, która wg
skanera usuwa wszystkie znane podatności pakietu.

## 1. Krytyczne: Boot 3.5.16 + Tomcat
- `spring-boot-starter-parent` 3.5.5 → 3.5.16 (Framework 6.2.19, Security 6.5.11, Thymeleaf 3.1.5,
  Data 2025.0.13, Micrometer 1.15.12, Logback 1.5.34).
- `tomcat.version` 10.1.62 (Boot daje 10.1.55; poprawki w 10.1.56–10.1.58).

## 2. Wysokie i średnie, których Boot 3.5.16 nie łata
- `jackson-bom.version` 2.21.9 (Boot: 2.21.4; poprawki do 2.21.7)
- `postgresql.version` 42.7.15 (Boot: 42.7.11; CVE-2026-54291 poprawione w 42.7.12)
- `commons-lang3.version` 3.18.0 (Boot: 3.17.0; CVE-2025-48924)
- `log4j2.version` 2.25.5 (Boot: 2.24.3; CVE-2026-49844)

## 3. jsoup 1.18.3 → 1.23.2
Sanityzacja HTML z edytora (`HtmlSanitizer`: `Safelist` + `Jsoup.clean`). Przed zmianą changelog przez
Context7. Testy sanityzacji (`HtmlSanitizerTest`, `EditorBlotSanitizationTest`, `EditorHtmlTest`) nie są
poprawiane pod nowe wyniki; czerwony test = decyzja Marcina.

## Odstępstwo w trakcie (Thymeleaf 3.1.5, nie jsoup)
Pierwszy `clean verify`: `ThreadPagesRenderIT` czerwony. Łatka CVE-2026-41901 dodała odwołania do beanów
`@nazwa` do konstrukcji zakazanych w trybie ograniczonym (diff `SpringStandardExpressionUtils` 3.1.3 → 3.1.5,
`containsExternalAccess`), a `th:data-max-*="${@attachmentLimits...}"` w `fragments/thread-attachments.html`
idzie przez procesor domyślny w tym trybie. Poprawka: `th:with="lim=${@attachmentLimits}"` na tym samym
elemencie, `th:data-*` czytają `lim`. IT dostał asercje na wartości trzech atrybutów na stronie zgłoszenia i profilu klienta
(pomocnik `pickerLimits()`; JS po cichu bierze 5).
Pozostałe `@bean` w szablonach są w `th:with`/`th:text` (tryb zwykły); parametry fragmentów bez `@`/`T(`/`new`.

## Poza zakresem
- CVE-2026-47884 (XsltView) i CVE-2026-47890 (SSE z fragmentami) w `spring-webmvc`: poprawka tylko
  w Spring Framework 7.0.9 (Boot 4); 6.2.19 to ostatnia wersja 6.2 w Maven Central. W projekcie nieosiągalne:
  brak XSLT, brak `SseEmitter`/`FragmentsRendering`. Migracja na Boot 4 to osobne zadanie.
- Zmiany kodu poza `pom.xml` (wyjątek: jsoup wymusi, wtedy najpierw pytanie do Marcina).

## Kryteria akceptacji
1. `mvn -q clean verify` zielony (testy jednostkowe, IT na Postgresie z Testcontainers, `GanwilkArchitectureTest`).
2. Ponowny `cve_watch.py scan .` pokazuje tylko CVE-2026-47884 i CVE-2026-47890 (89 z 91 zamkniętych).
3. Diff ogranicza się do `pom.xml`, tego SPEC i poprawki Thymeleaf opisanej wyżej (szablon, javadoc
   `AttachmentLimits`, asercje w `ThreadPagesRenderIT`).
4. Testy sanityzacji bez zmian i zielone.
