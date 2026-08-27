-- Brief v2.2, punkt 3: „w tresci maila ma sie znalezc informacja, z ktorej sciezki
-- (1 czy 2) i z ktorego pakietu klient kliknal CTA — Szymon musi wiedziec, o czym
-- rozmawiac, zanim oddzwoni".
--
-- Zapisujemy to przy zgloszeniu, a nie tylko wysylamy mailem: po miesiacu widac
-- w panelu, ktora sciezka faktycznie konwertuje.

alter table submission add column offer_path    varchar(40);
alter table submission add column offer_package varchar(60);

comment on column submission.offer_path is 'KONSULTACJA / PROWADZENIE — z ktorego CTA przyszlo zgloszenie';
comment on column submission.offer_package is 'Nazwa pakietu, gdy klient kliknal CTA w konkretnej karcie';
