-- Cennik stacjonarny v1.0 (październik 2026) i „nowe teksty strony” z 12.10.2026.
--
-- Opieka Longevity to roczne zobowiazanie rozliczane CO MIESIAC: 8 treningow
-- w miesiacu za stala kwote. Dotychczasowy model (liczba wejsc + waznosc
-- w tygodniach) tego nie opisze, stad dlugosc umowy w miesiacach.
-- Kwota „razem” dalej jest liczona z ceny za trening, nie przechowywana.

alter table stationary_package
    add column term_months integer,
    add column badge_text  varchar(60),
    add column description varchar(400),
    add constraint stationary_package_term_check check (term_months is null or term_months between 1 and 60);

comment on column stationary_package.term_months is
    'wypelnione = rozliczenie miesieczne: sessions to treningi w MIESIACU, kwota razem jest miesieczna';

-- Pojedynczy trening dla pary 240 -> 270 zl. Warunek na stara cene: jesli Szymon
-- zmienil ja juz w panelu, migracja nie nadpisuje jego decyzji.
update stationary_package set price_per_session_gr = 27000
 where kind = 'PARA' and sessions = 1 and price_per_session_gr = 24000;

insert into stationary_package
    (kind, name, sessions, price_per_session_gr, validity_weeks, featured, sort_order,
     term_months, badge_text, description) values
    ('INDYWIDUALNY', 'Opieka Longevity', 8, 13500, null, false, 5,
     12, 'Najlepszy wybór',
     '2 treningi w tygodniu w stałych terminach. Płacisz co miesiąc, nie za cały rok z góry.'),
    ('PARA', 'Opieka Longevity we dwoje', 8, 18000, null, false, 5,
     12, 'Najlepszy wybór',
     '2 treningi w tygodniu w stałych terminach, wszystko to, co w Opiece Longevity, tylko dla Was dwojga.');

-- Zasady odwolan w nowym brzmieniu. Tylko tam, gdzie stoi jeszcze tekst z V7.
update app_setting set "value" = 'Odwołujesz trening co najmniej 24 godziny wcześniej? Nic się nie dzieje, ustalamy nowy termin.'
 where "key" = 'stationary.rules.cancel'
   and "value" = 'Odwołanie do 24 godzin przed treningiem nie ma żadnych konsekwencji. Ustalamy nowy termin.';
update app_setting set "value" = 'Odwołanie później niż 24 godziny przed treningiem oznacza, że trening przepada. Wiem, że brzmi to surowo, ale ta godzina była zarezerwowana tylko dla Ciebie.'
 where "key" = 'stationary.rules.late'
   and "value" = 'Odwołanie później niż 24 godziny przed treningiem oznacza, że wejście przepada.';
update app_setting set "value" = 'Raz na pakiet możesz zrobić przerwę do 3 tygodni (choroba, wyjazd, delegacja). Wystarczy dać znać wcześniej. Na czas przerwy ważność pakietu się zatrzymuje.'
 where "key" = 'stationary.rules.pause'
   and "value" = 'Raz na pakiet możesz zrobić pauzę do 3 tygodni, zgłoszoną z góry: choroba, wyjazd, delegacja. Pauza zatrzymuje bieg ważności pakietu.';

insert into app_setting ("key", "value") values
    ('stationary.rules.freeze', 'W Opiece Longevity zamiast przerwy w pakiecie możesz zamrozić współpracę do 4 tygodni w roku. Też wystarczy zgłosić to wcześniej.'),
    -- Produkty jednorazowe: plan treningowy (offer.consult.*) i jadlospis
    -- (offer.diet.*) juz sa; dochodza zestaw i aktualizacja po 3 miesiacach.
    ('offer.combo.price.gr',  '54900'),
    ('offer.update.price.gr', '17900')
on conflict ("key") do nothing;

-- Jadlospis 129 -> 299 zl. Wiersz istnieje tylko, gdy cene zapisano w panelu;
-- bez niego obowiazuje domyslna z kodu, ktora tez idzie na 299 zl.
update app_setting set "value" = '29900'
 where "key" = 'offer.diet.price.gr' and "value" = '12900';

update online_package set badge_text = 'Cena startowa' where badge_text = 'CENA STARTOWA';

-- Opinia Wojtka, brzmienie od klienta 1:1.
insert into testimonial (name, city, cooperation_format, duration_label, body, sort_order) values
    ('Wojtek', 'Katowice', 'prowadzenie online', '40 lat',
     'Współpraca z Szymonem, mimo że zdalna, to była najlepsza decyzja dla mojego zdrowia i sylwetki. Efekty z pewnością przerosły moje oczekiwania. Plany treningowe dopasowane do moich indywidualnych możliwości, w połączeniu z dietą, pozwoliły mi zrzucić 25 zbędnych kilogramów w niespełna pół roku. Z czystym sumieniem polecam każdemu ten model współpracy - głównie ze względu na elastyczność i opiekę specjalisty, jakim niewątpliwie w swoim fachu jest trener Szymon :)',
     (select coalesce(max(sort_order), 0) + 1 from testimonial));
