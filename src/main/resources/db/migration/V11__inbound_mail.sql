-- Odbior odpowiedzi klientow z poczty do watku rozmowy.
--
-- mail_message_id: naglowek Message-ID. Wychodzacy pozwala dopasowac odpowiedz po
-- In-Reply-To/References, przychodzacy chroni przed zapisem tej samej poczty dwa razy
-- (restart w trakcie odbioru, powtorzony UID). Indeks czesciowy, bo notatki i zdarzenia
-- systemowe nie maja naglowka. 998 znakow to limit linii naglowka z RFC 5322.
alter table message add column mail_message_id varchar(998);
create unique index uq_message_mail_message_id on message (mail_message_id) where mail_message_id is not null;

-- Znacznik „nowa odpowiedz" w panelu. Tylko wiadomosci odebrane; znika po otwarciu watku.
alter table message add column unread boolean not null default false;
create index idx_message_unread on message (submission_id, trainee_id) where unread;

-- Jak odebrana wiadomosc trafila do watku: REPLY po naglowku odpowiedzi, ADDRESS po adresie
-- nadawcy. Naglowek From da sie podrobic, wiec watek pokazuje to trenerowi wprost.
alter table message add column matched_by varchar(20);
alter table message add constraint message_matched_by_check check (matched_by in ('REPLY', 'ADDRESS'));
