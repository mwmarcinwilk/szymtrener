-- Poprawki tresci szablonow odpowiedzi z V8: wielka litera po powitaniu z wykrzyknikiem,
-- bez polpauz jako lacznikow, {kontekst} w osobnym akapicie (MessageService.fillBody
-- wycina te linie, gdy klient nic nie napisal o treningu).
--
-- Warunek na DOKLADNA tresc z V8: szablon, ktory trener zdazyl poprawic w panelu,
-- zostaje nietkniety. Nadpisanie jego wlasnych slow byloby gorsze niz stara interpunkcja.

update reply_template set body = 'Cześć {imie}!

Dzięki za zgłoszenie.

Napisałeś o swoim treningu: „{kontekst}”.

Proponuję 20 minut rozmowy, żebym poznał Twój tydzień i historię treningową. Bez zobowiązań, po prostu sprawdzimy, czy mogę Ci realnie pomóc.

Pasuje Ci czwartek o 18:00 albo piątek o 17:30?

Pozdrawiam,
Szymon'
where code = 'first' and body = 'Cześć {imie}, dzięki za zgłoszenie.

Przeczytałem, co napisałeś — {kontekst}. Proponuję 20 minut rozmowy, żebym poznał Twój tydzień i historię treningową. Bez zobowiązań, po prostu sprawdzimy, czy mogę Ci realnie pomóc.

Pasuje Ci czwartek 18:00 albo piątek 17:30?

Pozdrawiam,
Szymon';

update reply_template set body = 'Cześć {imie}!

Mam wolne terminy na rozmowę wstępną:

• czwartek 18:00
• piątek 17:30
• sobota 10:00

Daj znać, który Ci pasuje, a zadzwonię o ustalonej godzinie.

Pozdrawiam,
Szymon'
where code = 'terms' and body = 'Cześć {imie},

mam wolne terminy na rozmowę wstępną:

• czwartek 18:00
• piątek 17:30
• sobota 10:00

Daj znać, który Ci pasuje — zadzwonię o ustalonej godzinie.

Pozdrawiam,
Szymon';

update reply_template set body = 'Cześć {imie}!

Przesyłam warunki współpracy online:

• Prowadzenie online: od 149 zł miesięcznie
  spersonalizowany plan, aplikacja treningowa na telefon, stały kontakt, analiza postępów

• Jadłospis dietetyczny: od 129 zł
  jednorazowa rozpiska dopasowana do Twoich potrzeb

Najwięcej efektu dają pierwsze trzy miesiące pracy. Wtedy budujemy nawyk i bazę siłową.

Pozdrawiam,
Szymon'
where code = 'price' and body = 'Cześć {imie},

przesyłam warunki współpracy online:

• Prowadzenie online — od 149 zł miesięcznie
  spersonalizowany plan, aplikacja treningowa na telefon, stały kontakt, analiza postępów

• Jadłospis dietetyczny — od 129 zł
  jednorazowa rozpiska dopasowana do Twoich potrzeb

Najwięcej efektu daje pierwsze trzy miesiące pracy — wtedy budujemy nawyk i bazę siłową.

Pozdrawiam,
Szymon';

update reply_template set body = 'Cześć {imie}!

Wracam do naszej rozmowy, bo nie chcę, żeby temat przepadł. Jeśli to nie jest dobry moment, po prostu daj znać, a odezwę się później.

Jeśli chcesz zacząć, mam wolne terminy w tym tygodniu.

Pozdrawiam,
Szymon'
where code = 'ping' and body = 'Cześć {imie},

wracam do naszej rozmowy — nie chcę, żeby temat przepadł. Jeśli to nie jest dobry moment, po prostu daj znać i odezwę się później.

Jeśli chcesz zacząć, mam wolne terminy w tym tygodniu.

Pozdrawiam,
Szymon';

update reply_template set body = 'Cześć {imie}!

Dzięki za zaufanie. Po Twoim opisie widzę, że w tym momencie nie jestem najlepszą osobą do pomocy: {powod}.

Polecam skonsultować się z {kierunek}. Jeśli sytuacja się zmieni, chętnie wrócę do rozmowy.

Pozdrawiam,
Szymon'
where code = 'no' and body = 'Cześć {imie},

dzięki za zaufanie. Po Twoim opisie widzę, że w tym momencie nie jestem najlepszą osobą do pomocy — {powod}.

Polecam skonsultować się z {kierunek}. Jeśli sytuacja się zmieni, chętnie wrócę do rozmowy.

Pozdrawiam,
Szymon';
