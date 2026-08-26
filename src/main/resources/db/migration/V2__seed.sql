-- Dane startowe: autor, kategorie, ustawienia.
-- Konto administratora zaklada AdminAccountInitializer ze zmiennych srodowiskowych
-- (nie trzymamy hasha hasla w migracji w repozytorium).

insert into author (slug, name, job_title, bio, photo_path, email) values
('szymon-domagala', 'Szymon Domagała', 'Trener personalny, Trener Longevity',
 'Trener personalny i Trener Longevity, Instruktor Sportu z tytułem Polskiej Akademii Sportu. W branży fitness od 2007 roku, od 2015 prowadzi własne studio w Łodzi. Specjalizuje się w treningu osób 35–55 lat. Ekspert podcastu „Dzień Dobry Długowieczność”.',
 '/images/szymon-portret.jpeg', 'szymtrener@gmail.com');

insert into author_same_as (author_id, url)
select id, u from author, unnest(array[
  'https://www.youtube.com/@DzienDobryDlugowiecznosc'
]) as u where slug = 'szymon-domagala';

insert into category (slug, name, description, sort_order) values
('longevity',  'Longevity',  'Jak trenować, żeby zostać sprawnym na kolejne dekady.', 1),
('trening',    'Trening',    'Praktyka treningu siłowego: plany, progresja, technika.', 2),
('odzywianie', 'Odżywianie', 'Białko, kalorie i nawyki żywieniowe osoby trenującej.', 3),
('zdrowie',    'Zdrowie',    'Regeneracja, sen, ból pleców i praca przy biurku.', 4);

insert into app_setting (key, value) values
('mail.notify.trainer', 'true'),
('mail.autoreply',      'true'),
('blog.page.size',      '9'),
('seo.default.title',   'Blog o treningu i długowieczności | Szymon Domagała'),
('seo.default.desc',    'Blog trenera Szymona Domagały: trening siłowy po 40-tce, sarkopenia, regeneracja i długowieczność.');
