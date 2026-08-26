#!/bin/bash
# Kopia zapasowa bazy — do crona, codziennie w nocy:
#   0 3 * * * /opt/szymtrener/deploy/backup.sh >> /var/log/szymtrener-backup.log 2>&1
# Haslo do bazy bierzemy z /opt/szymtrener/.env (PGPASSWORD), zeby nie bylo w pliku.
set -euo pipefail

STAMP=$(date +%Y%m%d)
DEST=/var/backups
ARCHIVE="$DEST/szymtrener-$STAMP.sql.gz"

umask 077
mkdir -p "$DEST"

pg_dump -U szymtrener szymtrener | gzip > "$ARCHIVE"

# Pusty dump to nie kopia — lepiej glosno paso niz cicho nadpisac dobra kopie.
if [ ! -s "$ARCHIVE" ]; then
    echo "BLAD: pusty dump, przerywam" >&2
    rm -f "$ARCHIVE"
    exit 1
fi

find "$DEST" -name 'szymtrener-*.sql.gz' -mtime +30 -delete

# Kopia poza serwer — inaczej to nie jest kopia zapasowa.
rsync -a "$ARCHIVE" backup@inny-serwer:/kopie/

echo "OK: $ARCHIVE ($(du -h "$ARCHIVE" | cut -f1))"
