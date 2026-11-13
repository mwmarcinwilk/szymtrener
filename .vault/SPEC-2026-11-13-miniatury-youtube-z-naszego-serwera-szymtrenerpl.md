# SPEC: Miniatury YouTube z naszego serwera (szymtrener.pl)

- **Data:** 2026-11-13
- **Sesja:** 56685f36-7ec
- **Status:** zatwierdzony plan; kryteria akceptacji poniżej są kontraktem dla @security

---

# Miniatury YouTube z naszego serwera (szymtrener.pl)

## Context
Po wdrożeniu dokumentów prawnych (polityka: „dane nie wychodzą poza EOG”) przegląd security wykazał, że
miniatury filmów ładują się z `i.ytimg.com`, a strona ma `preconnect` do `i.ytimg.com` i `youtube-nocookie.com`.
Każda odsłona strony głównej i wpisu z filmem wysyła więc IP odwiedzającego do Google bez zgody.
Marcin: „miniatury powinniśmy trzymać u siebie”. Cel: zero żądań do Google przy wyświetlaniu strony;
YouTube ładuje się dopiero po kliknięciu „odtwórz” (fasada w `main.js:165`, bez zmian).

Poza zakresem: podgląd miniatur w edytorze admina (`admin-editor.js:72,244`, widzi to tylko Szymon) i sam
odtwarzacz po kliknięciu (decyzja użytkownika; ewentualny zapis w polityce to pytanie do Joanny).

## 1. Strona główna (dwa filmy na stałe)
- Pobrać `maxresdefault.jpg` dla `LcXpFy7s-GQ` i `Qq2CHz25u9I` (fallback `hqdefault.jpg`, jeśli maxres nie istnieje)
  do `static/images/yt-<id>.jpg`; sprawdzić nagłówek JPEG.
- `index.html:777,784`: `src` na `@{/images/yt-<id>.jpg}` (ścieżka z łańcuchem skrótów), usunąć `onerror` do Google.
- Usunąć `preconnect` do `i.ytimg.com` i `youtube-nocookie.com` w `index.html:13-14` i `blog/post.html:22-23`.

## 2. Wpisy bloga (filmy z edytora)
`EditorHtml.java:84` przy publikacji wstawia `<img src="https://i.ytimg.com/vi/<id>/hqdefault.jpg">`; `PostService.java:172`
zapisuje wynik (`sanitizer.clean(publication)`).
- Nowy `media/YoutubeThumbnails` (reuse `MediaService.store(bytes, name, mime, alt)`, które już deduplikuje po sumie):
  - `localize(String html)`: Jsoup, każdy `img[src^=https://i.ytimg.com/vi/]` → id z walidacją `[A-Za-z0-9_-]{11}` →
    lokalny `/media/<storageKey>`; nazwa pliku `youtube-<id>.jpg`, najpierw szukana w bibliotece
    (`MediaRepository.findFirstByOriginalName`, nowa metoda pochodna), dopiero brak = pobranie.
  - Pobranie: JDK `HttpClient` (bez nowej zależności), host na sztywno `https://i.ytimg.com/vi/<id>/hqdefault.jpg`,
    timeout 5 s, limit 1 MB, wymagane 200 + `image/jpeg` i nagłówek JPEG. Pobierający to admin przy zapisie,
    więc anonim nie może wymusić żądań serwera (brak publicznego proxy, brak SSRF).
  - Błąd pobrania: `img` usunięty (fasada zostaje z przyciskiem odtwarzania na ciemnym tle), `log.warn` z id;
    nigdy nie zostaje adres Google.
- `PostService.java:172`: `sanitizer.clean(thumbnails.localize(publication))`. Sprawdzić, że sanitizer przepuszcza `/media/...`
  w `img src` (tak jak obrazy wgrane w edytorze).
- **Istniejące wpisy**: jednorazowy, idempotentny `ApplicationRunner` (`YoutubeThumbnailBackfill`) przy starcie:
  wpisy z `content_html like '%i.ytimg.com%'` (zapytanie w `PostRepository`) → `localize` → zapis przez serwis.
  Po pierwszym przebiegu nie ma trafień. Błąd sieci nie blokuje startu (log i dalej).

## Testy
- Unit `YoutubeThumbnailsTest` (pobieranie za interfejsem, stub): zamiana na `/media/...`; drugi raz bez pobierania
  (biblioteka); zły id i odpowiedź nie-JPEG/za duża → `img` usunięty; brak `i.ytimg.com` w wyniku.
- IT: strona główna i `/blog/<wpis z filmem>` nie zawierają `ytimg` ani `youtube-nocookie` w `<head>`; obrazki
  `/images/yt-*.jpg` → 200; backfill przepisuje wpis z dawnym adresem (stub pobierania w kontekście testu).
- Istniejące testy `EditorHtml` bez zmian (sam transformer dalej generuje adres, lokalizacja to osobny krok).

## Weryfikacja
`mvn clean verify` na kopii w scratchpadzie (IntelliJ otwarte), liczby z surefire i failsafe; aplikacja na kopii
z tymczasową bazą (`claude-tmp-*`), wpis z filmem zapisany w panelu, w DevTools/Playwright zero żądań do `*.ytimg.com`
i `youtube*.com` przed kliknięciem; zrzut 390/1440 sekcji filmów; `browser_close`, sprzątanie kontenera.
Bramki: `/simplify`, `/code-review medium`, subagent `security` (nowe wyjście sieciowe serwera). `CAPABILITIES.md`.
Bez commita bez zgody.

