# szymtrener

Instrukcja dla agentów kodujących. Format AGENTS.md czyta ponad 30 narzędzi
(Codex, Cursor, Copilot, Gemini CLI, Zed, Aider). Claude Code czyta `CLAUDE.md`,
który importuje ten plik.

## Stack
- Java 21 / Spring Boot 3.5.5
- Pakiet bazowy: `pl.szymtrener`
- Build: Maven

## Komendy

```bash
mvn -q clean test                                  # pełne testy
mvn -q -Dtest=NazwaTestu test                      # jeden test
mvn -q -Dtest=GanwilkArchitectureTest test         # reguły architektury
mvn -Pmutation-testing test-compile pitest:mutationCoverage   # siła testów (cel 70%)
```

## Bramki, które muszą przejść
- **Reguły architektury** (`GanwilkArchitectureTest`) są ZAMROŻONE: `archunit_store/` trzyma
  stan zastany, build pada tylko na NOWYCH naruszeniach. Katalogu nie kasuj i commituj go.
- **Zmiana encji wymaga migracji Flyway** (`V<n>__opis.sql`). Nie edytuj migracji już wydanej.
- **Nowy endpoint wymaga `@Operation`**.
- Kontroler nie sięga wprost do repozytorium ani nie nosi `@Transactional`.
- `findAll()` bez `Pageable` nie leci do widoku ani do odpowiedzi HTTP.

## Gdzie co jest
- `.vault/INVENTORY.md` — spis możliwości wygenerowany z kodu (nie edytuj ręcznie)
- `.vault/CAPABILITIES.md` — opisy możliwości
- `.vault/SPEC-*.md` — zatwierdzone plany zadań; kryteria akceptacji do weryfikacji
- `.vault/TODO.md`, `.vault/SESSION.md` — kolejka zadań i stan przerwanej sesji

## Szerszy kontekst
Pełne reguły jakości i bezpieczeństwa żyją w `~/vault-os`: `CLAUDE.md` (zawsze aktywne),
`.claude/skills/` (na żądanie), `.claude/rules/` (ładowane po ścieżkach plików).
