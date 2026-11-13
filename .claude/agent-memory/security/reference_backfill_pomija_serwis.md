---
name: reference-backfill-pomija-serwis
description: szymtrener - backfills/runners that set Post.contentHtml directly skip PostService.syncMediaLinks, so post_media (delete guard of media library) goes stale
metadata:
  type: reference
---

W szymtrener tabela `post_media` (z `PostService.syncMediaLinks`, wolane tylko w `PostService.save`) jest jedyną osłoną
`AdminMediaController.delete` przed usunięciem pliku użytego we wpisie (`postsUsing`). Każdy kod, który zmienia
`Post.contentHtml` poza `save()` (ApplicationRunner, migracja danych, import), a wstawia `/media/<key>` lub `/pliki/<id>`,
zostawia plik jako „nieużywany” i admin może go skasować, robiąc dziurę w opublikowanym wpisie.
Znalezione 2026-11-13 w `YoutubeThumbnailBackfill` (miniatury YouTube).

**How to apply:** przy przeglądzie każdej ścieżki zapisu treści wpisu sprawdź, czy przechodzi przez `syncMediaLinks`
(albo `save`) i czy jest IT, który to potwierdza (unit z mockiem repozytorium tego nie złapie).

Runda 2 (2026-11-13): poprawka przez `PostService.replaceContentKeepingDates` (JPQL update + syncMediaLinks) zamknęła lukę.
Wzorzec do sprawdzania przy każdym asynchronicznym zadaniu startowym, które czyta wpis, idzie do sieci i dopiero potem
zapisuje: bezwarunkowe `update ... where id = :id` nadpisze edycję admina z tego okna (lost update, Post nie ma `@Version`).
Rekomendacja: compare-and-set `and p.contentHtml = :old`, a przy 0 zmienionych wierszach pominąć sync.
