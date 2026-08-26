# Wdrożenie na VPS przez Coolify

Kontener jest **bezstanowy** — pliki wgrywane w panelu leżą w bazie (`media_blob`,
kolumna `bytea`), nie na dysku. Nie podpinaj wolumenu; kopia zapasowa to sam `pg_dump`.

## 1. Baza danych

W Coolify: **New Resource → Database → PostgreSQL 16**.

Coolify wystawi połączenie jako jeden link:

```
postgres://uzytkownik:haslo@nazwa-bazy:5432/baza
```

Aplikacja przyjmuje go **wprost** — wystarczy przekazać jako `DATABASE_URL`.
Rozbija go `DatabaseUrlEnvironmentPostProcessor` na `spring.datasource.{url,username,password}`,
bo sterownik JDBC nie akceptuje schematu `postgres://` ani danych logowania w adresie.
Obsłużone są też: brak portu (domyślnie 5432), parametry typu `?sslmode=require`
i hasło ze znakami specjalnymi (zakodowane procentowo, np. `p%40ss` → `p@ss`).

Jeśli wolisz rozbić to samodzielnie, podaj zamiast tego `DB_URL` w formacie
`jdbc:postgresql://host:5432/baza` plus `DB_USER` i `DB_PASSWORD` — taki adres
przechodzi bez tłumaczenia.

## 2. Aplikacja

**New Resource → Application → Dockerfile**, wskaż to repozytorium.
Coolify sam wykryje `Dockerfile` w katalogu głównym. Port: **8080**.

Healthcheck jest w obrazie (`/actuator/health`), więc Coolify przełączy ruch dopiero
na zdrowy kontener. Pierwszy start robi migracje Flyway, stąd `start-period` 60 s.

## 3. Zmienne środowiskowe

Ustaw w Coolify (zakładka *Environment Variables*). Żadna z nich nie może trafić do repozytorium.

| Zmienna | Wartość | Uwagi |
|---|---|---|
| `DATABASE_URL` | link z zasobu Postgres | Coolify podstawia go zmienną `${...}` z bazy |
| `SITE_URL` | `https://szymtrener.pl` | adresy kanoniczne, JSON-LD, sitemapa; **musi być https** |
| `ADMIN_EMAIL` | `szymtrener@gmail.com` | login do panelu |
| `ADMIN_PASSWORD` | mocne hasło | zakłada konto **tylko przy pierwszym starcie** — patrz niżej |
| `ANALYTICS_SALT` | losowy ciąg | sól do skrótów sesji, zmień na produkcji |
| `MAIL_HOST` | `smtp.gmail.com` | |
| `MAIL_PORT` | `587` | STARTTLS |
| `MAIL_USER` | adres Gmail | |
| `MAIL_PASSWORD` | hasło aplikacji (16 znaków) | **nie** zwykłe hasło do konta |
| `MAIL_RECIPIENT` | `szymtrener@gmail.com` | dokąd idą zgłoszenia |
| `INDEXNOW_ENABLED` | `true` | powiadamianie Bing po publikacji |
| `INDEXNOW_KEY` | losowy ciąg 32 znaków | |

`ADMIN_PASSWORD` działa wyłącznie przy zakładaniu pierwszego konta. Późniejsza zmiana
tej zmiennej **nie zmieni hasła** — aplikacja wypisze o tym ostrzeżenie w logu.
Hasło zmienia się w panelu (`/admin/haslo`).

## 4. Poczta przez Gmail

1. Włącz weryfikację dwuetapową na koncie Google — bez niej nie ma haseł aplikacji.
2. Konto Google → Bezpieczeństwo → **Hasła aplikacji** → wygeneruj hasło.
3. Wklej je do `MAIL_PASSWORD` (bez spacji).

`MAIL_FROM` zostaw puste — nadawcą będzie `MAIL_USER`. Gmail **nie pozwala wysyłać
z obcego adresu**: jeśli podasz `kontakt@szymtrener.pl`, a nie masz go dodanego
w Gmailu jako „Wysyłaj jako" i zweryfikowanego, Google podmieni nadawcę na konto
uwierzytelnione albo odrzuci wiadomość.

Limit Gmaila to ok. 500 wiadomości na dobę — dla formularza kontaktowego z zapasem.

Awaria poczty **nie gubi zgłoszenia**: trafia ono do bazy przed wysyłką, a błąd
ląduje w kolumnie `submission.mail_error` i jest widoczny w panelu na osi czasu
zgłoszenia. Z tego samego powodu wskaźnik zdrowia poczty jest wyłączony
(`management.health.mail.enabled: false`) — problem z SMTP nie może wywracać
healthchecku i blokować wdrożenia.

## 5. Domena i HTTPS

W Coolify ustaw domenę `szymtrener.pl`; certyfikat Let's Encrypt zestawi się sam.
Dodaj przekierowanie `www` → bez `www` (jeden skok — łańcuch trzech i więcej
przekierowań kosztuje 15 punktów w ocenie widoczności AI).

Aplikacja ma `server.forward-headers-strategy: native`, więc za proxy poprawnie
rozpoznaje HTTPS i buduje kanoniczne adresy.

## 6. Po wdrożeniu — sprawdź

```bash
curl -s https://szymtrener.pl/actuator/health          # {"status":"UP"}
curl -s https://szymtrener.pl/robots.txt | head -5     # Allow dla botów cytujących
curl -s https://szymtrener.pl/blog/<slug> | grep "<h1" # treść w HTML bez JS (bramka G4)
```

Ostatnie polecenie jest najważniejsze: jeśli nie zwróci tytułu, treść nie jest
renderowana po stronie serwera i cała widoczność w AI spada do maksymalnie 30/100.

## 7. Kopie zapasowe

Włącz w Coolify automatyczny `pg_dump` bazy (retencja 30 dni). To wystarczy —
razem z bazą kopiują się wszystkie wgrane pliki, bo leżą w niej jako `bytea`.
