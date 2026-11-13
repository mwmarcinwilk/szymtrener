-- Zasady odwolan zgodne z Regulaminem Studia obowiazujacym od 1.11.2026 (decyzja Szymona
-- z 13.11.2026): odwolanie najpozniej 12 h przed treningiem, pozne odwolanie rozliczane tylko,
-- gdy terminu nie dalo sie oddac komus innemu, bez przerwy w pakiecie.
-- Nadpisuje BEZWARUNKOWO, takze tekst zmieniony w panelu: strona nie moze obiecywac
-- czegos innego niz regulamin, ktory jest czescia umowy z klientem.
insert into app_setting ("key", "value") values
    ('stationary.rules.cancel', 'Odwołujesz trening co najmniej 12 godzin wcześniej? Nic się nie dzieje, ustalamy nowy termin w czasie ważności pakietu.'),
    ('stationary.rules.late',   'Odwołanie później niż 12 godzin przed treningiem albo nieobecność bez uprzedzenia: jeśli nie zdążę oddać tej godziny komuś innemu, trening liczy się jako wykorzystany.'),
    ('stationary.rules.freeze', 'W Opiece Longevity możesz zamrozić współpracę łącznie do 4 tygodni. Wystarczy wcześniej zgłosić i ustalić termin. Umowa wydłuża się o czas przerwy, raty zostają bez zmian.')
on conflict ("key") do update set "value" = excluded."value";

delete from app_setting where "key" = 'stationary.rules.pause';
