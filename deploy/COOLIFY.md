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
Obsłużone są też: brak portu (domyślnie 5432), parametry typu `?sslmode=require`,
podkreślnik w nazwie hosta, schemat pisany wielkimi literami i hasło ze znakami
specjalnymi zakodowanymi procentowo (`p%40ss%2F1` → `p@ss/1`). Znaki `@`, `:` i `+`
mogą stać w haśle także bez kodowania. `%` również, chyba że stoją po nim dwie cyfry
szesnastkowe: `abc%12def` zostanie odczytane jako kod, więc wpisz `abc%2512def`.

Pusta pozycja w nazwie hosta (`db,` albo `,db`) zatrzymuje start, bo sterownik
potraktowałby ją jako localhost.

Link `postgres://`, którego nie da się rozebrać (brak hosta, port nie jest liczbą,
nieznany schemat), zatrzymuje start komunikatem z nazwą zmiennej. Hasła w komunikacie nie ma.

Aplikacja nie wstanie też, gdy przed ostatnim `@` w linku stoi `/`, `?` albo `#`.
Taki link da się odczytać na dwa sposoby: `db/baza?app=x@prod` to baza na hoście `db`
albo login `db` i hasło `baza?app=x` na hoście `prod`. Zgadywanie mogłoby wysłać
hasło na obcy host. Te trzy znaki w haśle zakoduj (`/` → `%2F`, `?` → `%3F`, `#` → `%23`),
`@` w parametrach jako `%40`, albo użyj `DB_URL`/`DB_USER`/`DB_PASSWORD`.
Hasła generowane przez Coolify są alfanumeryczne, więc tego nie dotyczą.

Zepsuty `DATABASE_URL` nie przełącza się na `DB_URL`: usuń zmienną, której nie używasz.

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
| `ADMIN_PASSWORD` | mocne hasło, min. 12 znaków | zmiana + restart aktualizuje konto — patrz niżej |
| `ANALYTICS_SALT` | losowy ciąg | sól do skrótów sesji, zmień na produkcji |
| `MAIL_HOST` | `smtp.gmail.com` | |
| `MAIL_PORT` | `587` | STARTTLS |
| `MAIL_USER` | adres Gmail | |
| `MAIL_PASSWORD` | hasło aplikacji (16 znaków) | **nie** zwykłe hasło do konta |
| `MAIL_RECIPIENT` | `szymtrener@gmail.com` | dokąd idą zgłoszenia |
| `MAIL_INBOX_ENABLED` | `true` | odbiór odpowiedzi klientów do wątku (domyślnie wyłączony) |
| `MAIL_IMAP_HOST` | `ssl0.ovh.net` | Gmail: `imap.gmail.com` |
| `MAIL_IMAP_PORT` | `993` | tylko IMAPS |
| `INDEXNOW_ENABLED` | `true` | powiadamianie Bing po publikacji |
| `INDEXNOW_KEY` | losowy ciąg 32 znaków | |

### Konto administratora

`ADMIN_EMAIL` i `ADMIN_PASSWORD` są **źródłem prawdy**. Zmiana którejkolwiek z nich
i restart aplikacji aktualizują istniejące konto: nowy adres zastępuje stary,
nowe hasło nadpisuje poprzednie. Nie powstaje drugie konto.

Hasło zmienione w panelu (`/admin/haslo`) **przeżywa restart**, dopóki nie ruszysz
zmiennych. Aplikacja pamięta odcisk ostatnio zastosowanej pary email+hasło
(`app_setting`, klucz `admin.env.fingerprint`) i sięga do konta tylko wtedy,
gdy zmienne faktycznie się zmieniły.

Jeśli wcześniej zmieniałeś `ADMIN_EMAIL` na starej wersji aplikacji, w bazie mogło
zostać drugie konto ze starym adresem. Aplikacja ostrzeże o tym w logu. Usuniesz je tak:

```sql
DELETE FROM admin_user WHERE email <> 'aktualny@adres';
```

## 4. Poczta przez OVH (MX Plan)

| Zmienna | Wartość |
|---|---|
| `MAIL_HOST` | `ssl0.ovh.net` |
| `MAIL_PORT` | `587` (STARTTLS; 465 wymagałby `mail.smtp.ssl.enable`) |
| `MAIL_USER` | pełny adres skrzynki, np. `kontakt@szymtrener.pl` |
| `MAIL_PASSWORD` | hasło skrzynki z panelu OVH, **bez spacji** (MailConfig usuwa spacje z każdego hasła) |
| `MAIL_FROM` | puste albo ten sam adres co `MAIL_USER` |
| `MAIL_INBOX_ENABLED` | `true` |
| `MAIL_IMAP_HOST` / `MAIL_IMAP_PORT` | `ssl0.ovh.net` / `993` |

Email Pro i Exchange mają inny host (panel OVH → E-maile). DNS domeny: SPF `v=spf1 include:mx.ovh.com ~all`,
DKIM włączony w panelu OVH, DMARC na start `p=none`.

### Odbiór odpowiedzi klientów

Co dwie minuty aplikacja czyta INBOX tylko do odczytu (bez zmiany flag, nic nie przenosi ani nie kasuje).
Odpowiedź na mail wysłany z panelu trafia do wątku po nagłówku `In-Reply-To`; nowy mail z adresu
zgłoszenia lub klienta trafia tam z etykietą „dopasowano po adresie". Mail od nieznanego nadawcy,
autoresponder i lista mailingowa zostają tylko w skrzynce. Pierwsze uruchomienie nie importuje historii.
Wynik ostatniego sprawdzenia widać w Ustawieniach → Poczta.

### Załączniki w wątku

Pliki wysłane z panelu i odebrane od klientów leżą w bazie (`message_attachment_blob`), jak media —
backup `pg_dump` je obejmuje, a baza rośnie o ich rozmiar. Nie ma dla nich publicznego adresu:
pobiera je tylko zalogowany admin (`/admin/zalaczniki/{id}`). Usunięcie zgłoszenia lub klienta
usuwa jego pliki. Limit uploadu z panelu: 20 MB (nginx `client_max_body_size 30m` wystarcza).

## 4a. Poczta przez Gmail

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
