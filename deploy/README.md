# Wdrozenie

```bash
# 1. jar
mvn clean package -DskipTests
scp target/szymtrener-*.jar serwer:/opt/szymtrener/szymtrener.jar

# 2. zasoby statyczne dla nginxa (serwuje je z dysku, nie przez aplikacje)
scp -r src/main/resources/static serwer:/opt/szymtrener/static

# 3. konfiguracja
scp deploy/szymtrener.service serwer:/etc/systemd/system/
scp deploy/nginx.conf         serwer:/etc/nginx/sites-available/szymtrener
scp deploy/backup.sh          serwer:/opt/szymtrener/deploy/

# 4. na serwerze
ln -s /etc/nginx/sites-available/szymtrener /etc/nginx/sites-enabled/
certbot --nginx -d szymtrener.pl -d www.szymtrener.pl
systemctl daemon-reload && systemctl enable --now szymtrener
nginx -t && systemctl reload nginx
```

## `/opt/szymtrener/.env`

```
DB_URL=jdbc:postgresql://localhost:5432/szymtrener
DB_USER=szymtrener
DB_PASSWORD=...
SITE_URL=https://szymtrener.pl
ADMIN_EMAIL=szymtrener@gmail.com
ADMIN_PASSWORD=...        # tylko do pierwszego startu, potem usun (haslo zmienia sie w /admin/haslo)
MAIL_PASSWORD=...
ANALYTICS_SALT=...        # losowy ciag, NIE domyslny
INDEXNOW_ENABLED=true
INDEXNOW_KEY=...
```

Plik ma nalezec do uzytkownika `szymtrener` z prawami `600` — systemd czyta go jako `EnvironmentFile`.

## Poczta nie jest czescia health check

`/actuator/health` **celowo nie sprawdza SMTP**: awaria poczty nie oznacza, ze serwis
jest niesprawny, bo zgloszenie zapisuje sie w bazie niezaleznie od wysylki. Minus jest
taki, ze zepsute haslo SMTP nie zapali sie w monitoringu. Sygnalem sa kolumny w bazie:

```sql
-- zgloszenia, ktorych nie udalo sie wyslac (powinno byc puste)
select id, created_at, email, mail_error
from submission
where mail_sent = false and mail_error is not null
order by created_at desc limit 20;
```

Warto podpiac to pod cotygodniowy przeglad albo pod alert.

## Sprawdzenie po wdrozeniu

```bash
curl -I https://szymtrener.pl          # 200, bez posrednich przekierowan
curl -I http://www.szymtrener.pl       # dokladnie JEDEN skok 301 na https://szymtrener.pl
curl -sI https://szymtrener.pl | grep -i strict-transport
```
