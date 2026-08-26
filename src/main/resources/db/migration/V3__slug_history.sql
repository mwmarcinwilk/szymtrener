-- Zmiana tytulu opublikowanego wpisu przeliczala slug i stary adres zwracal 404 —
-- a to dokladnie ten adres, ktory bot cytujacy mogl juz zapamietac.
-- Trzymamy stare adresy i odsylamy z nich 301 na aktualny.
create table post_slug_history (
    slug       text primary key,
    post_id    bigint not null references post(id) on delete cascade,
    created_at timestamptz not null default now()
);
create index idx_slug_history_post on post_slug_history (post_id);
