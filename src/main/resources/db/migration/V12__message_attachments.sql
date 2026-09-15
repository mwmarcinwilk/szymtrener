-- Zalaczniki w watku rozmowy: wyslane z panelu i odebrane od klientow.
--
-- Osobno od media_file, bo biblioteka mediow jest publiczna (/media, /pliki, cache nginx),
-- a tu leza dane osobowe, czesto wyniki badan. Pobiera je tylko zalogowany admin.
-- Kaskada od message: usuniecie zgloszenia albo klienta usuwa tez jego pliki (RODO).
-- Bajty w osobnej tabeli, zeby lista plikow w watku ich nie zaciagala (wzorzec media_blob).
create table message_attachment (
    id            bigserial primary key,
    message_id    bigint not null references message(id) on delete cascade,
    original_name varchar(255) not null,
    mime_type     varchar(100) not null,
    size_bytes    bigint not null,
    sha256        varchar(64) not null,
    created_at    timestamptz not null default now()
);
create index idx_message_attachment_message on message_attachment (message_id);

create table message_attachment_blob (
    attachment_id bigint primary key references message_attachment(id) on delete cascade,
    data          bytea not null
);
