# SPEC: Nowy cennik stacjonarny, nowe teksty strony, opinia Wojtka

- **Data:** 2026-10-12
- **Sesja:** b75ec12c-2e3
- **Status:** zatwierdzony plan; kryteria akceptacji poniżej są kontraktem dla @security

---

# Nowy cennik stacjonarny, nowe teksty strony, opinia Wojtka

## Context
W `docs/` przyszły dwa dokumenty od klienta (12.10.2026):
- `Cennik_Stacjonarny_SzymTrener.docx` – nowa karta „Opieka Longevity” (rok, 2 treningi/tydz., płatność co miesiąc), para: pojedynczy 240 → 270 zł, jadłospis 129 → 299 zł, nowe produkty jednorazowe (plan + jadłospis 549 zł, aktualizacja po 3 mies. 179 zł).
- `SzymTrener_nowe_teksty_strony.docx` – tabela ZMIANA/NOWE dla 19 sekcji strony + reguły ogólne (łącznik „ - ” zamiast „–/—”, bez emoji, bez „Fit and Health Studio”, „Umów rozmowę wstępną”, menu „Ekspert” → „Podcast”, FAQPage zgodne z widocznym FAQ + P9).
Plus nowa opinia: Wojtek, Katowice, prowadzenie online · 40 lat.

Ceny, opinie i zasady odwołań żyją w bazie i są edytowalne z panelu (brief 5.3: jedno źródło dla kart i FAQ). Teksty statyczne siedzą w `templates/index.html`, `fragments/nav|footer|cookie-consent.html`, `blog/list.html`.

Decyzje użytkownika: zostaje „Cena docelowa” (Omnibus), „Najpopularniejszy” na pakiecie 8 zostaje obok „Najlepszy wybór” na Opiece, podpis Wojtka „prowadzenie online · 40 lat”.

## Kroki

### 1. Migracja `V14__longevity_care_pricing_and_copy.sql`
- `stationary_package` + kolumny: `term_months integer` (CHECK 1-60; wypełnione = rozliczenie miesięczne, `isMonthly()` wyliczane, sessions = treningi w miesiącu), `badge_text varchar(60)`, `description varchar(400)`.
- UPDATE PARA pojedynczy: 24000 → 27000 (`where price_per_session_gr = 24000`, żeby nie nadpisać ręcznej zmiany z panelu).
- INSERT: INDYWIDUALNY „Opieka Longevity” 8 × 13500, 12 mies., badge „Najlepszy wybór”, opis z dokumentu; PARA „Opieka Longevity we dwoje” 8 × 18000, analogicznie. `sort_order` 5.
- `app_setting`: reguły odwołań – UPDATE na nowe brzmienie `where value = <stare>`; nowa `stationary.rules.freeze` (zasada 4); `offer.diet.price.gr` 12900 → 29900 (warunkowo); nowe `offer.combo.price.gr` 54900, `offer.update.price.gr` 17900.
- `online_package`: badge `CENA STARTOWA` → `Cena startowa` (warunkowo).
- INSERT testimonial Wojtek (city Katowice, format „prowadzenie online”, duration „40 lat”, treść 1:1, `sort_order` = max+1, czyli na końcu listy).

### 2. Model i serwisy (`pl.szymtrener.offer`)
- `StationaryPackage`: nowe pola + gettery/settery.
- `StationaryOfferService.PackageView`: dodać `badge`, `description`, `monthly`, `yearCompare`. Dla monthly: `totalLine` = „Razem: 1 080 zł / miesiąc (8 treningów)”, `validity` = „12 miesięcy, płatność co miesiąc”, rabat liczony jak dziś (36% / 33% – sprawdzone z dokumentem). Rabaty par wyjdą 19/22/26% z istniejącego `discountPercent`.
- Nowe: rachunek roczny liczony z danych (`sessions*12` × cena Opieki vs × cena największego pakietu): „ok. 14 400 zł … 12 960 zł … 1 440 zł, czyli prawie 10 treningów gratis” (prawie/ponad + odmiana przez `crm/Plural`). Bez kopii kwot w HTML.
- `priceSentence()` przepisane na nowe O2; `rules()` + RULE_FREEZE; `cheapestPair` uwzględnia Opiekę (180).
- `OnlineOfferService`: produkty jednorazowe (`oneOff()` – lista 4 pozycji: plan = consult, jadłospis = diet, combo, update) + `oneOffFrom()` (najniższa = „od 179 zł”); `seatsLine` z odmianą „Zostały 3 z 5 miejsc w cenie startowej.” / „Zostało 1…”; `conditionLine` nowe brzmienie; `targetLine` bez zmian („Cena docelowa”).
- `SettingsService`: klucze `OFFER_COMBO_PRICE_GR`, `OFFER_UPDATE_PRICE_GR`.

### 3. Panel
- `admin/offer-stationary` + `fragments/stationary-rows.html` + `AdminOfferController.saveStationary`: pola „Rozliczenie miesięczne”, „Okres (mies.)”, „Plakietka”, „Opis”; pole reguły „zamrożenie”.
- `admin/offer.html` + kontroler: formularz cen „Plan + jadłospis” i „Aktualizacja po 3 mies.”; komunikat dla diety bez „np. 129”.

### 4. Szablony (teksty znak w znak wg dokumentu)
- `index.html`: wszystkie sekcje 0–17; karta Opieki z `badge`, klasami `.highlight` (plakietka z panelu, styl jak `.pack.hi`) i `.wide` (pozycja miesięczna na cały rząd), wspólny fragment `fragments/price-card :: card`, link „Co zawiera Opieka Longevity” → `#opieka-longevity`; nowy blok z listą 6 punktów i rachunkiem; ścieżka A jako mini cennik 4 pozycji; FAQ: 3 tabele, nowe O1–O8, P9; JSON-LD FAQPage zsynchronizowany, nazwa organizacji bez „Fit and Health”, `priceRange` z danych; hero „2007 / W branży od”; „Przewiń”; marquee; „Wybieram Start”.
- Emoji (`.ic`) → inline SVG z nowego fragmentu `fragments/icons.html` (sprite `<symbol>`, kreska `currentColor`), CSS rozmiaru w `styles.css`.
- `nav.html` (Podcast, „Umów rozmowę”, „Zadzwoń albo napisz:”), `footer.html` + stopki w `polityka-prywatnosci.html`, `ustawienia-cookies.html` („Łódź i online”, „Wróć na górę”), `cookie-consent.html` („podszywaniem się”), sticky CTA.
- `blog/list.html` + `BlogController` (meta = nowy lead), pusty stan, CTA.
- `main.js`: etykieta wyboru „Wybrany pakiet:”, `PATH_LABELS.KONSULTACJA` = „Plan na start”.
- `JsonLdService.organization()` i `SeoController.llms()`: „ - ” zamiast „–”, bez „Fit and Health”.
- Data „Aktualizacja” w stopce: `max(ostatni wpis, data publikacji tekstów)` – stała w `HomeController`.

### 5. Testy
- `StationaryOfferServiceTest`: Opieka (total/miesiąc, ważność, rabat 36/33), rachunek roczny (14 400 / 12 960 / 1 440 / „prawie 10 treningów”), nowe `priceSentence`, rabaty par 19/22/26.
- `OnlineOfferServiceTest`: odmiana licznika miejsc, nowe `conditionLine`, `oneOffFrom`.
- IT renderujące `/` do `</html>` (istniejący lub nowy): obecne „Opieka Longevity”, „270 zł”, „299 zł”, „Wojtek”, P9; brak „Fit and Health”, „Umów konsultację”, emoji z listy, „ – ”/„—” w widocznym copy (poza treścią opinii klientów), poprawny JSON w `ld+json`. Asercje na `getContentAsString(UTF_8)`, sonda na wersji sprzed zmian (czerwona).

## Weryfikacja
- `mvn clean verify` (surefire + failsafe, liczby z obu raportów), `GanwilkArchitectureTest`.
- Uruchomienie lokalne + Playwright: zrzuty 390 / 768 / 1440 sekcji Oferta, We dwoje, Online, FAQ; konsola bez błędów; porównanie z dokumentem.
- `/simplify`, `/code-review`, subagent `security`.
- Dopisać do `.vault/CAPABILITIES.md` (Opieka Longevity, produkty jednorazowe), SPEC w tym pliku.
- Bez commita bez zgody.


## Zmiany po bramkach (/simplify, /code-review, security)
- Jeden odczyt cennika na stronę: `StationaryOfferService.page()` → rekord `Page`.
- Zdania FAQ z kwotami w `web.HomeFaq` (raz, dla treści i JSON-LD); do JSON-LD idą zakodowane Jacksonem (`[(${faqJson...})]`), bo `[[...]]` escapuje pod HTML.
- `Plural` przeniesiony z `crm` do `common` (bez cyklu), zamiast kopii w `offer`.
- Mini cennik i tabela FAQ produktów jednorazowych respektują widoczność planu i jadłospisu.
- `termMonths` 1-60 (panel + CHECK), `yearCompare` zwraca null przy przepełnieniu zamiast 500.
- Świadomie bez zmian: „Cena docelowa” (Omnibus), zakodowane w copy „4 tygodnie”, „2 razy w tygodniu”, „12 miesięcy” (zatwierdzony tekst klienta).
