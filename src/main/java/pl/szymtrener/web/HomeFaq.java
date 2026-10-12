package pl.szymtrener.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.szymtrener.offer.OnlineOfferService.SimplePriceView;
import pl.szymtrener.offer.StationaryOfferService;

/**
 * Odpowiedzi FAQ strony glownej, ktore zawieraja kwoty. Ten sam tekst idzie do
 * widocznego FAQ i do JSON-LD FAQPage, a rozbieznosc danych strukturalnych
 * z trescia jest gorsza niz ich brak — dlatego zdania powstaja raz, tutaj.
 *
 * Do JSON-LD idzie {@link #json()}: wartosci zakodowane jak literaly JSON. Zwykle
 * [[...]] w szablonie escapuje pod HTML, wiec „\” albo nowa linia z nazwy
 * pakietu w panelu psulyby caly blok danych strukturalnych.
 */
public record HomeFaq(String prices, String validity, String pairs, String online) {

    private static final ObjectMapper JSON = new ObjectMapper();

    public static HomeFaq of(StationaryOfferService.Page stationary, String lowestMonthly,
                      SimplePriceView consult, SimplePriceView diet) {
        String monthly = lowestMonthly != null ? lowestMonthly : "149 zł";

        String prices = (stationary.priceSentence() != null ? stationary.priceSentence() + " " : "")
                + "Prowadzenie online zaczyna się od " + monthly + " miesięcznie"
                + (consult.visible() ? ", plan treningowy kosztuje " + consult.price() : "")
                + (diet.visible() ? ", a jadłospis " + diet.price() : "") + ".";

        String validity = "Im większy pakiet, tym dłużej jest ważny"
                + (stationary.validitySentence() != null ? ": " + stationary.validitySentence() : "")
                + ". Ten zapas jest policzony tak, żeby zmieściła się w nim choroba, delegacja albo tydzień,"
                + " w którym po prostu nie wyszło. Do tego raz na pakiet możesz zrobić przerwę do 3 tygodni."
                + (stationary.hasCare() ? " W Opiece Longevity możesz zamrozić współpracę do 4 tygodni w roku." : "");

        String pairs = "Tak. Dla wielu par to jedyna forma, która w ogóle mieści się w grafiku. Trenujecie"
                + " w jednym terminie, przyjeżdżacie jednym samochodem i nie musicie się wymieniać przy dzieciach."
                + " Każde z Was ćwiczy na swoim poziomie. Ceny dla par są łączne za dwie osoby"
                + (stationary.pairPackagePrice() != null
                    ? ": od " + stationary.pairPackagePrice() + " za trening w pakietach"
                        + (stationary.pairCarePrice() != null ? " i " + stationary.pairCarePrice() + " w Opiece Longevity" : "")
                    : "")
                + ".";

        String online = "Tak, prowadzę klientów z różnych miejsc w Polsce. Prowadzenie online kosztuje od "
                + monthly + " miesięcznie. Dostajesz plan ułożony pod siebie, stały kontakt ze mną i regularne"
                + " sprawdzanie postępów. Trenujesz w domu albo na dowolnej siłowni."
                + (consult.visible()
                    ? " Jeśli wolisz jednorazowy plan bez stałego prowadzenia, kosztuje " + consult.price() + "."
                    : "");

        return new HomeFaq(prices, validity, pairs, online);
    }

    /** Te same zdania jako literaly JSON (z cudzyslowami), do wstawienia przez [(...)] w JSON-LD. */
    public HomeFaq json() {
        return new HomeFaq(quote(prices), quote(validity), quote(pairs), quote(online));
    }

    /**
     * Literal JSON; kazde „<” jako \u003c, zeby ani „</script>”, ani „<!--<script”
     * z nazwy w panelu nie zmienily, gdzie przegladarka konczy blok skryptu.
     */
    static String quote(String value) {
        try {
            return JSON.writeValueAsString(value).replace("<", "\\u003c");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Nie da sie zakodowac tekstu FAQ", e);
        }
    }
}
