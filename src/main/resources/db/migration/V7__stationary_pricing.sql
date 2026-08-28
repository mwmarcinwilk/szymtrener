-- Brief „Ceny treningow stacjonarnych" v1.0.
--
-- Punkt 5.3: ceny, waznosc i zasady odwolan maja byc edytowalne bez programisty,
-- w tym samym miejscu co ceny online. Punkt 5.3 zdanie drugie: cennik wystepuje
-- na stronie DWA razy (sekcja oferty i FAQ) i obie instancje musza czerpac
-- z jednego zrodla, zeby nie rozjechaly sie przy kolejnej zmianie.

create table stationary_package (
    id                    bigserial primary key,
    -- INDYWIDUALNY albo PARA. Para to cena LACZNA za dwie osoby.
    kind                  varchar(20)  not null,
    name                  varchar(80)  not null,
    -- Liczba treningow w pakiecie; 1 = wejscie pojedyncze.
    sessions              integer      not null,
    -- Cena za JEDEN trening, w groszach. Kwota „razem" i rabat sa liczone,
    -- nie przechowywane: trzy kolumny opisujace to samo rozjezdzaja sie
    -- przy pierwszej zmianie ceny.
    price_per_session_gr  integer      not null,
    -- Waznosc pakietu w tygodniach; NULL dla wejscia pojedynczego.
    validity_weeks        integer,
    featured              boolean      not null default false,
    sort_order            integer      not null default 0,
    visible               boolean      not null default true,
    constraint stationary_package_kind_check check (kind in ('INDYWIDUALNY', 'PARA')),
    constraint stationary_package_sessions_check check (sessions >= 1)
);

create index idx_stationary_package_order on stationary_package (kind, sort_order);

-- Ceny wg rekomendacji 3.1: pojedyncze wejscie 190 -> 210 zl, pakiety bez zmian.
-- Waznosc wg punktu 4: skaluje sie z wielkoscia pakietu i ma zapas na zycie
-- (8 treningow 6 -> 10 tygodni, 12 treningow 6 -> 16 tygodni).
insert into stationary_package
    (kind, name, sessions, price_per_session_gr, validity_weeks, featured, sort_order) values
    ('INDYWIDUALNY', 'Pojedynczy trening',   1,  21000, null,  false, 1),
    ('INDYWIDUALNY', 'Pakiet 4 treningów',   4,  17000,    6,  false, 2),
    ('INDYWIDUALNY', 'Pakiet 8 treningów',   8,  16000,   10,  true,  3),
    ('INDYWIDUALNY', 'Pakiet 12 treningów', 12,  15000,   16,  false, 4),
    ('PARA',         'Pojedynczy trening',   1,  24000, null,  false, 1),
    ('PARA',         'Pakiet 4 treningów',   4,  22000,    6,  false, 2),
    ('PARA',         'Pakiet 8 treningów',   8,  21000,   10,  true,  3),
    ('PARA',         'Pakiet 12 treningów', 12,  20000,   16,  false, 4);

-- Zasady odwolan i pauzy (punkt 4). Brief 5.1: „informacja o pauzie ma byc
-- wyeksponowana, nie w regulaminie — to argument sprzedazowy, nie zapis prawny".
-- Dlatego zwykly tekst edytowalny z panelu, a nie osobna podstrona.
insert into app_setting ("key", "value") values
    ('stationary.rules.cancel', 'Odwołanie do 24 godzin przed treningiem nie ma żadnych konsekwencji. Ustalamy nowy termin.'),
    ('stationary.rules.late',   'Odwołanie później niż 24 godziny przed treningiem oznacza, że wejście przepada.'),
    ('stationary.rules.pause',  'Raz na pakiet możesz zrobić pauzę do 3 tygodni, zgłoszoną z góry: choroba, wyjazd, delegacja. Pauza zatrzymuje bieg ważności pakietu.')
on conflict ("key") do nothing;
