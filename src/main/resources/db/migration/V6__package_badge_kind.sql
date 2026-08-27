-- Brief v2.2, punkt 2.4: po zamknieciu naboru zalozycielskiego badge „CENA STARTOWA"
-- ma zniknac razem z linia ceny docelowej, licznikiem miejsc i zdaniem o opinii.
--
-- Ale plakietka „Najlepszy wybor" na Longevity NIE jest oznaczeniem promocyjnym
-- i po zamknieciu naboru ma zostac. Bez tego rozroznienia albo znika za duzo,
-- albo za malo — stad osobna flaga zamiast zgadywania z tresci napisu.

alter table online_package
    add column badge_promotional boolean not null default true;

comment on column online_package.badge_promotional is
    'true = plakietka dotyczy ceny startowej i znika po zamknieciu naboru';

-- „Najlepszy wybor" to wyroznienie pakietu, nie promocja.
update online_package set badge_promotional = false where highlighted = true;
