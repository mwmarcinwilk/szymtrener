-- Handoff, sekcja 8a: CRM zgloszenia.
--
-- Sedno zmiany: przycisk „Odpisz" przestaje otwierac program pocztowy. Rozmowa
-- toczy sie w panelu, wychodzi przez JavaMail, a KOPIA kazdej wiadomosci zostaje
-- w watku — trener widzi historie bez zagladania do Gmaila.

-- ── Watek rozmowy ────────────────────────────────────────────────────────────
create table message (
    id            bigserial primary key,
    submission_id bigint references submission(id) on delete cascade,
    trainee_id    bigint references trainee(id) on delete cascade,
    direction     varchar(10) not null,   -- IN (od klienta) | OUT (od trenera)
    -- FORM: pierwsze zgloszenie, EMAIL: poczta, PHONE: notatka z rozmowy,
    -- SYSTEM: cienka linia w watku (zmiana etapu, nieudana wysylka)
    channel       varchar(10) not null,
    body          text not null,
    attachment_id bigint references media_file(id) on delete set null,
    sent_at       timestamptz not null default now(),
    mail_status   varchar(20),            -- SENT | FAILED | null dla notatek
    constraint message_direction_check check (direction in ('IN', 'OUT')),
    constraint message_channel_check check (channel in ('FORM', 'EMAIL', 'PHONE', 'SYSTEM')),
    -- Wiadomosc zawsze nalezy do zgloszenia albo do klienta. Po konwersji
    -- zgloszenia na klienta watek dostaje oba, wiec nie jest to XOR.
    constraint message_owner_check check (submission_id is not null or trainee_id is not null)
);

create index idx_msg_sub on message (submission_id, sent_at);
create index idx_msg_trainee on message (trainee_id, sent_at);

-- ── Szablony odpowiedzi ──────────────────────────────────────────────────────
-- W makiecie siedza w JavaScripcie. W bazie, bo trener ma je poprawiac sam,
-- bez wdrozenia — dokladnie jak ceny.
create table reply_template (
    id         bigserial primary key,
    code       varchar(40) not null unique,
    label      varchar(80) not null,
    body       text not null,
    sort_order integer not null default 0
);

insert into reply_template (code, label, body, sort_order) values
('first', 'Pierwsza odpowiedź',
 'Cześć {imie}, dzięki za zgłoszenie.

Przeczytałem, co napisałeś — {kontekst}. Proponuję 20 minut rozmowy, żebym poznał Twój tydzień i historię treningową. Bez zobowiązań, po prostu sprawdzimy, czy mogę Ci realnie pomóc.

Pasuje Ci czwartek 18:00 albo piątek 17:30?

Pozdrawiam,
Szymon', 1),
('terms', 'Propozycja terminu',
 'Cześć {imie},

mam wolne terminy na rozmowę wstępną:

• czwartek 18:00
• piątek 17:30
• sobota 10:00

Daj znać, który Ci pasuje — zadzwonię o ustalonej godzinie.

Pozdrawiam,
Szymon', 2),
-- Kwoty w tym szablonie sa przykladowe i trener podmienia je recznie. Nie
-- wstawiamy tu cen z panelu: mail wychodzi raz, a cena moze sie zmienic pozniej.
('price', 'Wycena i pakiety',
 'Cześć {imie},

przesyłam warunki współpracy online:

• Prowadzenie online — od 149 zł miesięcznie
  spersonalizowany plan, aplikacja treningowa na telefon, stały kontakt, analiza postępów

• Jadłospis dietetyczny — od 129 zł
  jednorazowa rozpiska dopasowana do Twoich potrzeb

Najwięcej efektu daje pierwsze trzy miesiące pracy — wtedy budujemy nawyk i bazę siłową.

Pozdrawiam,
Szymon', 3),
('ping', 'Przypomnienie',
 'Cześć {imie},

wracam do naszej rozmowy — nie chcę, żeby temat przepadł. Jeśli to nie jest dobry moment, po prostu daj znać i odezwę się później.

Jeśli chcesz zacząć, mam wolne terminy w tym tygodniu.

Pozdrawiam,
Szymon', 4),
('no', 'Grzeczna odmowa',
 'Cześć {imie},

dzięki za zaufanie. Po Twoim opisie widzę, że w tym momencie nie jestem najlepszą osobą do pomocy — {powod}.

Polecam skonsultować się z {kierunek}. Jeśli sytuacja się zmieni, chętnie wrócę do rozmowy.

Pozdrawiam,
Szymon', 5);

-- ── Etapy zgloszenia i przypomnienia ─────────────────────────────────────────
-- Daty wejscia w etap: sciezka w panelu pokazuje pod nazwa kroku, KIEDY sie wydarzyl.
alter table submission add column contacted_at   timestamptz;
alter table submission add column call_booked_at timestamptz;
alter table submission add column converted_at   timestamptz;
alter table submission add column archived_at    timestamptz;

-- Przypomnienia rozwiazuja glowny problem: zgloszenie, o ktorym trener zapomnial.
alter table submission add column remind_at   timestamptz;
alter table submission add column remind_done boolean not null default false;

create index idx_submission_remind on submission (remind_at) where remind_done = false;

-- ── Notatki: przypinanie, tagi, powiazanie z klientem ────────────────────────
alter table submission_note add column pinned  boolean not null default false;
alter table submission_note add column tags    varchar(120);
alter table submission_note add column trainee_id bigint references trainee(id) on delete cascade;

-- Notatka nalezy do zgloszenia albo do klienta; po konwersji dostaje oba.
alter table submission_note alter column submission_id drop not null;
