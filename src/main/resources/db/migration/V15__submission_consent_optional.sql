-- Formularze kontaktowe nie zbieraja juz zgody na przetwarzanie danych: nowa polityka
-- prywatnosci (11.2026) opiera ich obsluge na art. 6 ust. 1 lit. b i f RODO, a zgoda
-- wymagana do wyslania formularza nie bylaby dobrowolna. Nowe zgloszenia maja null,
-- stare zachowuja date zgody.
alter table submission alter column consent_at drop not null;
