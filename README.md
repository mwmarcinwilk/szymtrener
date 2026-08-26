# szymtrener.pl

Strona, blog i panel trenera Szymona Domagały. Java 21 + Spring Boot 3.5 + PostgreSQL,
widoki w Thymeleafie (SSR), edytor Quill 2 z importem z Worda.

Opis architektury i decyzji: **[ARCHITEKTURA.md](ARCHITEKTURA.md)**.
Czego brakuje i jak to dokończyć: **[DO_ZROBIENIA.md](DO_ZROBIENIA.md)**.

---

## Uruchomienie

Potrzebne: JDK 21, Maven 3.9+, PostgreSQL 16 (albo Docker do samej bazy).

```bash
# 1. zmienne środowiskowe (raz)
cp .env.example .env

# 2. baza
set -a; . ./.env; set +a
docker compose up -d db

# 3. start
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

`set -a; . ./.env; set +a` zamiast `export $(cat .env | xargs)`: to drugie wykłada się
na pustych wartościach i na wartościach ze spacją, a w `.env` masz oba przypadki.

Zmienne trzeba załadować w **każdej nowej** sesji terminala przed `mvn spring-boot:run`.

Domyślne `.env.example` stawia bazę na porcie **5433**, żeby nie zderzać się z lokalnym
Postgresem (Postgres.app, Homebrew) na 5432. Jeśli nic nie zajmuje 5432, możesz ustawić
`DB_PORT=5432` i `DB_URL=jdbc:postgresql://localhost:5432/szymtrener`.

Objaw zderzenia portów jest mylący: aplikacja łączy się z **cudzym** Postgresem i wywala
się na starcie, np. `FATAL: Postgres.app rejected "trust" authentication`. To nie błąd
konfiguracji aplikacji, tylko trafienie w nie tę bazę.

### Wariant bez Dockera: własny, lokalny Postgres

Jeśli masz już Postgresa na 5432 (Postgres.app, Homebrew), załóż rolę i bazy ręcznie
i pomiń `docker compose` w ogóle:

```bash
psql -d postgres <<'SQL'
create role szymtrener with login password 'szymtrener';
create database szymtrener      owner szymtrener;
create database szymtrener_test owner szymtrener;   -- pod `mvn verify`
SQL
```

W `.env` ustaw wtedy `DB_URL=jdbc:postgresql://localhost:5432/szymtrener`
(`DB_PORT` dotyczy wyłącznie kontenera, tutaj nic nie robi).

**Rola musi mieć hasło**, nawet jeśli `pg_hba.conf` ma `trust`. Postgres.app blokuje
aplikacjom połączenia bez hasła i odpowiada wtedy
`FATAL: Postgres.app rejected "trust" authentication` z podpowiedzią o uprawnieniach.
Wygląda to na problem z konfiguracją Springa, a jest polityką Postgres.app.

Testy integracyjne na tej samej bazie:

```bash
mvn verify -Dtest.db.url=jdbc:postgresql://localhost:5432/szymtrener_test \
           -Dtest.db.user=szymtrener -Dtest.db.password=szymtrener
```

- strona: http://localhost:8080
- panel: http://localhost:8080/admin/logowanie

Konto administratora zakłada się przy pierwszym starcie z `ADMIN_EMAIL` i `ADMIN_PASSWORD`.
Bez `ADMIN_PASSWORD` aplikacja wystartuje, ale zapisze w logu ostrzeżenie i nie utworzy konta.

Flyway tworzy schemat sam przy starcie (`V1__schema.sql`, `V2__seed.sql`, `V3__slug_history.sql`). Hibernate ma
`ddl-auto=validate` — schemat zmienia się **wyłącznie** przez nową migrację.

---

## Zmienne środowiskowe

| Zmienna | Domyślnie | Opis |
|---|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | localhost:5432/szymtrener | Połączenie z bazą |
| `SITE_URL` | https://szymtrener.pl | Podstawa adresów kanonicznych, JSON-LD i sitemapy |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | szymtrener@gmail.com / — | Konto zakładane przy pierwszym starcie |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USER` / `MAIL_PASSWORD` | ssl0.ovh.net:587 | SMTP |
| `MAIL_RECIPIENT` | szymtrener@gmail.com | Dokąd idą zgłoszenia z formularzy |
| `INDEXNOW_ENABLED` / `INDEXNOW_KEY` | false / — | Ping do Bing po publikacji |
| `ANALYTICS_SALT` | — | Sól do skrótów sesji; **zmień na produkcji** |

---

## Struktura

```
src/main/java/pl/szymtrener/     kod (podział wg obszarów domeny, nie warstw)
src/main/resources/
  db/migration/                  Flyway — jedyne źródło schematu
  templates/                     Thymeleaf: index, blog/, admin/, fragments/
  static/css|js|images/          zasoby z makiet, przeniesione 1:1
ARCHITEKTURA.md                  decyzje projektowe + siatka oceniania
```

---

## Adresy

| Adres | Co robi |
|---|---|
| `/` | Strona główna (treść 1:1 z makiety) |
| `/blog`, `/blog/kategoria/{slug}`, `/blog/{slug}` | Blog, filtr kategorii, wpis |
| `/polityka-prywatnosci` | Polityka prywatności |
| `/media/**`, `/pliki/{id}/**` | Pliki z bazy (zdjęcia; PDF-y z licznikiem pobrań) |
| `/robots.txt`, `/sitemap.xml`, `/llms.txt` | Generowane z bazy |
| `POST /api/zgloszenia/online`, `/kontakt` | Formularze (JSON + token CSRF) |
| `/admin`, `/admin/posty`, `/admin/media`, `/admin/zgloszenia` | Panel |

---

---

## Testy

```bash
mvn test      # 57 testów jednostkowych: bez bazy, bez Dockera, ~10 s
mvn verify    # dodatkowo 17 integracyjnych na prawdziwym Postgresie (Testcontainers)
```

Przed wypuszczeniem zmiany uruchamiaj `mvn verify`, nie samo `mvn test`.

Gdy Testcontainers nie widzi Dockera (Docker Desktop potrafi odciąć dostęp do gniazda
procesowi Javy, objaw: `Could not find a valid Docker environment`), wskaż gotową
i **pustą** bazę:

```bash
docker run -d --name szymtrener-test-db -e POSTGRES_DB=szymtrener_test \
  -e POSTGRES_USER=szymtrener -e POSTGRES_PASSWORD=szymtrener -p 5434:5432 postgres:16-alpine

mvn verify -Dtest.db.url=jdbc:postgresql://localhost:5434/szymtrener_test \
           -Dtest.db.user=szymtrener -Dtest.db.password=szymtrener
```

Zakres testów: patrz tabela w `DO_ZROBIENIA.md`.
