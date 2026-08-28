-- Handoff, sekcja 8b: ekran klientow ma odpowiadac na trzy pytania —
-- komu konczy sie pakiet, kto ma trening w najblizszych dniach, z kim dawno
-- nie bylo kontaktu. Dotad byla to martwa tabela, bo nie bylo z czego ich liczyc.

alter table trainee add column email           varchar(180);
alter table trainee add column phone           varchar(40);
alter table trainee add column fixed_slots     varchar(120);   -- 'pon 9:00 · czw 9:00'
alter table trainee add column last_contact_at timestamptz;
alter table trainee add column source          varchar(40);
alter table trainee add column goal_note       text;           -- „Cele i priorytety"

-- ── Sprzedane pakiety ────────────────────────────────────────────────────────
create table training_package (
    id                bigserial primary key,
    trainee_id        bigint      not null references trainee(id) on delete cascade,
    name              varchar(80) not null,       -- „Pakiet 12"
    total_sessions    integer     not null,
    -- Cena za trening w GROSZACH, tak jak w cenniku. Wartosc wspolpracy liczy sie
    -- z tego pola, wiec ulamki z NUMERIC tylko wprowadzalyby blad zaokraglenia.
    price_per_session_gr integer  not null,
    purchased_at      date        not null,
    active            boolean     not null default true,
    constraint training_package_sessions_check check (total_sessions >= 1)
);

create index idx_pkg_trainee on training_package (trainee_id, active);

-- ── Dziennik treningow ───────────────────────────────────────────────────────
-- „session" to slowo zarezerwowane w wielu narzedziach, stad training_session.
create table training_session (
    id          bigserial primary key,
    trainee_id  bigint       not null references trainee(id) on delete cascade,
    package_id  bigint       references training_package(id) on delete set null,
    starts_at   timestamptz  not null,
    title       varchar(120) not null,      -- „Siła — dolna część ciała"
    note        text,                       -- „Martwy ciąg 62,5 kg × 5"
    status      varchar(16)  not null,
    -- Odwolany trening domyslnie NIE zuzywa pakietu (handoff 8b). Trener moze
    -- to zmienic dla konkretnej sesji, gdy klient odwolal na ostatnia chwile.
    consumes_package boolean not null default true,
    constraint training_session_status_check check (status in ('PLANNED', 'DONE', 'CANCELLED'))
);

create index idx_sess_trainee on training_session (trainee_id, starts_at desc);

-- ── Pomiary ──────────────────────────────────────────────────────────────────
create table measurement (
    id         bigserial primary key,
    trainee_id bigint       not null references trainee(id) on delete cascade,
    taken_on   date         not null,
    metric     varchar(60)  not null,       -- „Masa ciała", „Martwy ciąg (5 powt.)"
    value      numeric(10,2) not null,
    unit       varchar(16)  not null,
    -- Ktory kierunek zmiany jest dobry, zalezy od metryki: masa ciala i obwody
    -- w dol, sila i czas deski w gore. Trzymamy to przy DANYCH, nie w widoku —
    -- inaczej kazdy nowy ekran musialby znac te liste od nowa.
    lower_is_better boolean not null default false
);

create index idx_meas_trainee on measurement (trainee_id, metric, taken_on);

-- Makieta pokazuje klienta z forma „Jadłospis" — usluga jednorazowa bez pakietu.
-- Dotad enum znal tylko ONLINE i ONSITE.
alter table trainee drop constraint if exists trainee_mode_check;
update trainee set mode = 'ONLINE' where mode is null;
alter table trainee add constraint trainee_mode_check check (mode in ('ONLINE', 'ONSITE', 'DIET'));
