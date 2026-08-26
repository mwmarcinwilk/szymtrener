-- =====================================================================
-- szymtrener.pl — schemat bazowy
-- Zasada: schemat zmienia wylacznie Flyway. hibernate ddl-auto=validate.
-- =====================================================================

-- ─── Konto do panelu ────────────────────────────────────────────────
create table admin_user (
    id            bigserial primary key,
    email         text        not null unique,
    password_hash text        not null,
    display_name  text        not null,
    role          text        not null default 'ADMIN',
    enabled       boolean     not null default true,
    last_login_at timestamptz,
    created_at    timestamptz not null default now()
);

-- ─── Autor tresci (E-E-A-T: byline + Person schema) ─────────────────
create table author (
    id          bigserial primary key,
    slug        text not null unique,
    name        text not null,
    job_title   text,
    bio         text,
    photo_path  text,
    email       text,
    created_at  timestamptz not null default now()
);

-- sameAs do profili zewnetrznych — wchodzi do JSON-LD Person
create table author_same_as (
    author_id bigint not null references author(id) on delete cascade,
    url       text   not null,
    primary key (author_id, url)
);

-- ─── Pliki (tresc trzymana w bazie) ─────────────────────────────────
create table media_file (
    id             bigserial primary key,
    storage_key    text not null unique,      -- 2026/08/uuid.jpg — czesc URL-a
    original_name  text not null,
    mime_type      text not null,
    kind           text not null check (kind in ('IMAGE','PDF','OTHER')),
    size_bytes     bigint not null,
    width          integer,
    height         integer,
    page_count     integer,
    alt_text       text,
    title          text,
    checksum       text,                      -- sha-256, deduplikacja wgran
    download_count bigint not null default 0,
    created_at     timestamptz not null default now()
);
create index idx_media_created on media_file (created_at desc);
create index idx_media_kind    on media_file (kind);

-- bajty w osobnej tabeli: listing mediow nigdy nie ciagnie zawartosci
create table media_blob (
    media_id bigint primary key references media_file(id) on delete cascade,
    data     bytea not null
);

-- ─── Kategorie bloga ────────────────────────────────────────────────
create table category (
    id          bigserial primary key,
    slug        text not null unique,
    name        text not null,
    description text,
    sort_order  integer not null default 0
);

-- ─── Wpisy ──────────────────────────────────────────────────────────
create table post (
    id               bigserial primary key,
    slug             text not null unique,
    title            text not null,
    lead             text,                     -- answer-first, 40–80 slow
    content_html     text not null default '',
    content_delta    jsonb,                    -- zrodlo prawdy edytora Quill
    category_id      bigint references category(id) on delete set null,
    author_id        bigint references author(id) on delete set null,
    cover_media_id   bigint references media_file(id) on delete set null,
    cover_alt        text,
    cover_caption    text,
    status           text not null default 'DRAFT'
                     check (status in ('DRAFT','SCHEDULED','PUBLISHED','ARCHIVED')),
    publish_at       timestamptz,
    published_at     timestamptz,
    seo_title        text,
    seo_description  text,
    reading_minutes  integer not null default 0,
    word_count       integer not null default 0,
    view_count       bigint  not null default 0,
    ai_score         integer,                  -- ocena wg siatki V4 (blok CONTENT)
    has_video        boolean not null default false,
    has_pdf          boolean not null default false,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now(),
    search_vector    tsvector generated always as (
                         to_tsvector('simple',
                             coalesce(title,'') || ' ' || coalesce(lead,''))
                     ) stored
);
create index idx_post_published on post (published_at desc) where status = 'PUBLISHED';
create index idx_post_status    on post (status);
create index idx_post_schedule  on post (publish_at) where status = 'SCHEDULED';
create index idx_post_category  on post (category_id);
create index idx_post_search    on post using gin (search_vector);

create table post_tag (
    post_id bigint not null references post(id) on delete cascade,
    tag     text   not null,
    primary key (post_id, tag)
);
create index idx_post_tag_tag on post_tag (tag);

-- „W skrocie" — BLUF jako pole strukturalne, nie dowolny HTML
create table post_summary_point (
    post_id  bigint  not null references post(id) on delete cascade,
    position integer not null,
    text     text    not null,
    primary key (post_id, position)
);

-- FAQ jako dane => widoczna tresc i JSON-LD FAQPage nigdy sie nie rozjada
create table post_faq (
    id       bigserial primary key,
    post_id  bigint  not null references post(id) on delete cascade,
    position integer not null,
    question text    not null,
    answer   text    not null
);
create index idx_post_faq_post on post_faq (post_id, position);

-- pliki uzyte we wpisie (PDF-y, zdjecia w tresci) — do licznikow i porzadkow
create table post_media (
    post_id  bigint not null references post(id) on delete cascade,
    media_id bigint not null references media_file(id) on delete cascade,
    role     text   not null default 'INLINE',
    primary key (post_id, media_id, role)
);

-- ─── Zgloszenia z formularzy ────────────────────────────────────────
create table submission (
    id               bigserial primary key,
    type             text not null check (type in ('ONLINE','CONTACT')),
    name             text not null,
    email            text not null,
    phone            text,
    city             text,
    current_training text,
    goal             text,
    equipment        text,
    source           text,
    interest         text,
    message          text,
    consent_at       timestamptz not null,
    status           text not null default 'NEW'
                     check (status in ('NEW','IN_CONTACT','CALL_BOOKED','CLIENT','ARCHIVED')),
    call_at          timestamptz,
    ip_hash          text,
    user_agent       text,
    mail_sent        boolean not null default false,
    mail_error       text,
    created_at       timestamptz not null default now()
);
create index idx_submission_created on submission (created_at desc);
create index idx_submission_status  on submission (status);

create table submission_note (
    id            bigserial primary key,
    submission_id bigint not null references submission(id) on delete cascade,
    author        text   not null,
    body          text   not null,
    created_at    timestamptz not null default now()
);
create index idx_note_submission on submission_note (submission_id, created_at desc);

-- ─── Klienci ────────────────────────────────────────────────────────
create table trainee (
    id            bigserial primary key,
    submission_id bigint references submission(id) on delete set null,
    name          text not null,
    city          text,
    age           integer,
    mode          text check (mode in ('ONLINE','ONSITE')),
    started_at    date,
    plan_name     text,
    session_count integer not null default 0,
    status        text not null default 'ACTIVE',
    created_at    timestamptz not null default now()
);

-- ─── Wlasna statystyka (bez ciasteczek stron trzecich) ──────────────
create table page_view (
    id           bigserial primary key,
    path         text not null,
    referrer     text,
    session_hash text,          -- sha-256(ip+ua+sol+data), bez danych osobowych
    device       text,
    is_bot       boolean not null default false,
    bot_name     text,          -- GPTBot, ClaudeBot, PerplexityBot…
    viewed_at    timestamptz not null default now()
);
create index idx_pv_time on page_view (viewed_at desc);
create index idx_pv_path on page_view (path, viewed_at desc);
create index idx_pv_bot  on page_view (bot_name) where is_bot = true;

-- ─── Ustawienia edytowalne z panelu ─────────────────────────────────
create table app_setting (
    key        text primary key,
    value      text,
    updated_at timestamptz not null default now()
);
