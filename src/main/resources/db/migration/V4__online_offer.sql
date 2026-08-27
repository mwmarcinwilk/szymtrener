-- Brief programisty v2.2 — oferta online, opinie i FAQ.
--
-- Kluczowe wymaganie briefu (2.5): ceny, etapy naboru i liczniki miejsc musza byc
-- edytowalne bez udzialu programisty i bez wdrozenia. Dlatego kazda wartosc, ktora
-- kiedykolwiek sie zmieni, jest wierszem w bazie, a nie tekstem w szablonie.

-- ── Pakiety prowadzenia online (Sciezka 2) ───────────────────────────────────
create table online_package (
    id                      bigserial primary key,
    name                    varchar(60)  not null,          -- Start / Rozwoj / Longevity
    duration_label          varchar(40)  not null,          -- „3 miesiace"
    -- Dwa poziomy cenowe. Kwoty w GROSZACH: liczby calkowite nie maja bledu
    -- zaokraglenia, a formatowanie „1 074 zl" robi widok.
    current_total_gr        integer      not null,
    current_monthly_gr      integer      not null,
    target_total_gr         integer      not null,
    target_monthly_gr       integer      not null,
    -- tryb per pakiet: brief dopuszcza, ze Longevity jest juz w cenie docelowej,
    -- a Start jeszcze w startowej
    pricing_mode            varchar(20)  not null default 'STARTOWA',
    seats_taken             integer      not null default 0,
    seats_total             integer      not null default 5,
    badge_text              varchar(60),
    badge_visible           boolean      not null default false,
    highlighted             boolean      not null default false,
    sort_order              integer      not null default 0,
    visible                 boolean      not null default true,
    constraint online_package_mode_check check (pricing_mode in ('STARTOWA', 'DOCELOWA'))
);

create index idx_online_package_order on online_package (sort_order);

-- Stan na dzis — Etap 1, cena startowa (tabela z punktu 2.3 briefu).
insert into online_package
    (name, duration_label, current_total_gr, current_monthly_gr, target_total_gr, target_monthly_gr,
     seats_taken, seats_total, badge_text, badge_visible, highlighted, sort_order) values
    ('Start',     '3 miesiące',   59700,  19900,  74700, 24900, 2, 5, 'CENA STARTOWA', true, false, 1),
    ('Rozwój',    '6 miesięcy',  107400,  17900, 137400, 22900, 2, 5, 'CENA STARTOWA', true, false, 2),
    ('Longevity', '12 miesięcy', 178800,  14900, 238800, 19900, 2, 5, 'Najlepszy wybór', true, true, 3);

-- ── Opinie klientow (sekcja 5) ───────────────────────────────────────────────
create table testimonial (
    id                 bigserial primary key,
    name               varchar(80)  not null,
    city               varchar(80),
    -- Podpis pod imieniem: brief podkresla, ze to on niesie dowod. Oba pola moga
    -- byc puste — wtedy podpis sie nie renderuje i uklad sie nie rozjezdza.
    cooperation_format varchar(80),
    duration_label     varchar(80),
    body               text         not null,
    media_id           bigint,
    sort_order         integer      not null default 0,
    visible            boolean      not null default true,
    created_at         timestamptz  not null default now()
);

create index idx_testimonial_order on testimonial (sort_order);

-- Pierwsza opinia — zgoda klienta uzyskana pisemnie, brzmienie z briefu 1:1.
insert into testimonial (name, city, cooperation_format, duration_label, body, sort_order) values
    ('Kacper', 'Łódź', 'prowadzenie online', '1,5 roku współpracy',
     'Trenowałem z Szymonem online przez półtora roku i wszedłem na zupełnie inny poziom treningu. Efekty rewelacyjne — zarówno siłowe, jak i kondycyjne. Mam zdjęciowe porównanie sylwetki i widać różnicę gołym okiem. Jestem mega zadowolony i z efektów, i z tego jak wygląda współpraca. Szczerze polecam.',
     1),
    ('Michał', 'Łódź', null, null,
     'Na współpracę z Szymonem zdecydowałem się w momencie, w którym samodzielne prowadzenie przestało przynosić efekty, a wręcz wracałem do punktu wyjścia. Praktycznie jak za dotknięciem magicznej różdżki wróciła radość z treningów. Po treningu wreszcie czuję, że wykonałem kawał dobrej roboty, a zestaw ćwiczeń jest tak dobrany, że sesję można przeprowadzić bardzo sprawnie. Dzięki jadłospisowi w końcu przestałem odczuwać nagminnie głód i przeszła mi ochota na podjadanie. Posiłki są smaczne, sycące i proste w przygotowaniu. Zarówno przy układaniu planu treningowego, jak i jadłospisu uwzględnione zostały wszystkie moje preferencje. Przeprowadziliśmy gruntowny wywiad, a stan mojego zdrowia, przebyte kontuzje i aktualne badania były ważnym punktem przygotowywanej strategii. Polecam Szymona wszystkim osobom chcącym zadbać o swoje zdrowie, a także zmienić sylwetkę. To dobry kierunek dla osób zapracowanych, którym wydaje się, że nie mają czasu na trening.',
     2);

-- ── FAQ sekcji online (sekcja 7) ─────────────────────────────────────────────
-- Osobne od FAQ w tresci wpisow blogowych (post_faq) — to jest FAQ strony oferty.
create table online_faq (
    id         bigserial primary key,
    question   varchar(300) not null,
    answer     text,
    sort_order integer      not null default 0,
    visible    boolean      not null default true
);

create index idx_online_faq_order on online_faq (sort_order);

-- Pytania z briefu. Odpowiedzi Szymon dostarczy osobno — do tego czasu pytanie
-- bez odpowiedzi nie jest pokazywane (patrz OnlineOfferService.faq()).
insert into online_faq (question, sort_order, visible) values
    ('Dla kogo jest prowadzenie online?', 1, true),
    ('Jak wygląda kontakt na co dzień?', 2, true),
    ('Ile czasu czekam na plan?', 3, true),
    ('Co jeśli nie mam siłowni w pobliżu?', 4, true),
    ('Czy mogę trenować w domu?', 5, true),
    ('Jak działa aplikacja treningowa?', 6, true),
    ('Co się dzieje po zakończeniu pakietu?', 7, true);
