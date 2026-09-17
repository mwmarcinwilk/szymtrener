-- Zgoda na ciasteczka z panelu na stronie publicznej.
--
-- Osoba jest anonimowa, wiec jedynym powiazaniem z nia jest losowy consent_key, trzymany
-- w ciasteczku st_zgoda. Po nim osoba sama pobiera swoje dane, a admin je wyszukuje i usuwa.
-- Bez IP i bez user-agenta: rekord ma dowodzic zgody, nie identyfikowac czlowieka.
create table cookie_consent (
    id           bigserial primary key,
    consent_key  uuid not null unique,
    statistics   boolean not null,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);
create index idx_cookie_consent_updated on cookie_consent (updated_at);

-- Kazda decyzja osobno (RODO art. 7 ust. 1: trzeba umiec wykazac, na co i kiedy sie zgodzono).
-- Kaskada: usuniecie zgody kasuje cala jej historie.
create table cookie_consent_change (
    id              bigserial primary key,
    consent_id      bigint not null references cookie_consent(id) on delete cascade,
    statistics      boolean not null,
    source          varchar(10) not null,
    policy_version  varchar(20) not null,
    changed_at      timestamptz not null default now()
);
create index idx_cookie_consent_change_consent on cookie_consent_change (consent_id, changed_at desc);

-- Odslona ze zgoda na statystyke wskazuje zgode. session_hash zostaje dzienny jak dotad, wiec
-- liczba sesji sie nie zmienia; powracajacych liczy sie po consent_id. Kaskada: usuniecie zgody
-- (przez admina albo retencje) kasuje tez jej odslony, a zmiana ANALYTICS_SALT niczego nie zrywa.
alter table page_view add column consent_id bigint references cookie_consent(id) on delete cascade;
create index idx_pv_consent on page_view (consent_id, viewed_at) where consent_id is not null;
