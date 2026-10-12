---
name: thymeleaf-jsonld-escaping
description: Jak Thymeleaf (HTML mode, th:inline="text") escapuje [[...]] w <script type="application/ld+json"> i jak to sondować
metadata:
  type: reference
---

Sprawdzone 2026-10-12 sondą (SpringTemplateEngine, StringTemplateResolver, HTML mode):
`[[...]]` w `<script type="application/ld+json" th:inline="text">` robi escaping HTML:
`<`→`&lt;`, `"`→`&quot;` → brak wyjścia z </script> (brak XSS), ALE `\` i znak nowej linii
przechodzą surowo → `\` na końcu wartości lub `\n` łamie JSON (cały blok JSON-LD odrzucony),
a `"` w treści widać w danych jako literał `&quot;`. Poprawnie: `th:inline="javascript"`
(escaping JS/JSON) albo serializacja Jacksonem w Javie.

Sonda: `mvn dependency:build-classpath`, potem `java -cp ... Probe.java` z SpringTemplateEngine
(zwykły TemplateEngine wymaga OGNL, którego nie ma na classpath).
