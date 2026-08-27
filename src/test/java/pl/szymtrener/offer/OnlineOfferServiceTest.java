package pl.szymtrener.offer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Formatowanie kwot i regula przelaczania ceny startowej na docelowa.
 * Brief jest w tych punktach bardzo konkretny, wiec sa sprawdzane wprost.
 */
class OnlineOfferServiceTest {

    private OnlinePackage pack(int seatsTaken, int seatsTotal, PricingMode mode) {
        OnlinePackage p = new OnlinePackage();
        p.setName("Longevity");
        p.setDurationLabel("12 miesięcy");
        p.setCurrentTotalGr(178800);
        p.setCurrentMonthlyGr(14900);
        p.setTargetTotalGr(238800);
        p.setTargetMonthlyGr(19900);
        p.setSeatsTaken(seatsTaken);
        p.setSeatsTotal(seatsTotal);
        p.setPricingMode(mode);
        return p;
    }

    @Test
    @DisplayName("kwoty mają spację jako separator tysięcy")
    void formatsThousands() {
        // spacja nierozdzielajaca, nie zwykla — kwota nie moze sie zlamac na koncu wiersza
        assertThat(OnlineOfferService.money(107400)).isEqualTo("1\u00A0074\u00A0zł");
        assertThat(OnlineOfferService.money(178800)).isEqualTo("1\u00A0788\u00A0zł");
        assertThat(OnlineOfferService.money(59700)).isEqualTo("597\u00A0zł");
        assertThat(OnlineOfferService.money(19900)).isEqualTo("199\u00A0zł");
    }

    @Test
    @DisplayName("dopóki są wolne miejsca, obowiązuje cena startowa")
    void keepsStartingPriceWhileSeatsRemain() {
        OnlinePackage p = pack(2, 5, PricingMode.STARTOWA);

        assertThat(p.seatsLeft()).isEqualTo(3);
        assertThat(p.effectiveMode()).isEqualTo(PricingMode.STARTOWA);
    }

    @Test
    @DisplayName("zero wolnych miejsc przełącza na cenę docelową automatycznie")
    void switchesToTargetWhenSeatsRunOut() {
        OnlinePackage p = pack(5, 5, PricingMode.STARTOWA);

        assertThat(p.seatsLeft()).isZero();
        assertThat(p.effectiveMode()).isEqualTo(PricingMode.DOCELOWA);
    }

    @Test
    @DisplayName("tryb docelowy ustawiony ręcznie wygrywa nawet przy wolnych miejscach")
    void manualTargetModeWins() {
        assertThat(pack(0, 5, PricingMode.DOCELOWA).effectiveMode()).isEqualTo(PricingMode.DOCELOWA);
    }

    @Test
    @DisplayName("po zamknięciu naboru znikają wszystkie oznaczenia ceny startowej")
    void hidesPromotionalMarkersWhenSeatsRunOut() {
        OnlinePackage p = pack(5, 5, PricingMode.STARTOWA);
        p.setBadgeText("CENA STARTOWA");
        p.setBadgeVisible(true);
        p.setBadgePromotional(true);

        OnlineOfferService.PackageView v = OnlineOfferService.toView(p);

        assertThat(v.price()).isEqualTo("2\u00A0388\u00A0zł");   // cena docelowa
        assertThat(v.badge()).isNull();
        assertThat(v.targetLine()).isNull();
        assertThat(v.seatsLine()).isNull();
        assertThat(v.conditionLine()).isNull();
    }

    @Test
    @DisplayName("plakietka wyróżnienia zostaje także po przejściu na cenę docelową")
    void keepsNonPromotionalBadge() {
        OnlinePackage p = pack(5, 5, PricingMode.STARTOWA);
        p.setBadgeText("Najlepszy wybór");
        p.setBadgeVisible(true);
        p.setBadgePromotional(false);

        assertThat(OnlineOfferService.toView(p).badge()).isEqualTo("Najlepszy wybór");
    }

    @Test
    @DisplayName("w cenie startowej widać komplet: badge, cenę docelową, miejsca i warunek")
    void showsFullStartingPriceBlock() {
        OnlinePackage p = pack(2, 5, PricingMode.STARTOWA);
        p.setBadgeText("CENA STARTOWA");
        p.setBadgeVisible(true);

        OnlineOfferService.PackageView v = OnlineOfferService.toView(p);

        assertThat(v.price()).isEqualTo("1\u00A0788\u00A0zł");
        assertThat(v.badge()).isEqualTo("CENA STARTOWA");
        assertThat(v.targetLine()).isEqualTo("Cena docelowa: 2\u00A0388\u00A0zł (199\u00A0zł/mies.)");
        assertThat(v.seatsLine()).isEqualTo("Zostało 3 z 5 miejsc w cenie startowej");
        assertThat(v.conditionLine()).isNotNull();
    }

    @Test
    @DisplayName("podpis opinii składa się tylko z pól, które są wypełnione")
    void signatureSkipsEmptyParts() {
        Testimonial full = new Testimonial();
        full.setCooperationFormat("prowadzenie online");
        full.setDurationLabel("1,5 roku współpracy");
        assertThat(full.signature()).isEqualTo("prowadzenie online · 1,5 roku współpracy");

        Testimonial bare = new Testimonial();
        assertThat(bare.signature()).isNull();

        Testimonial onlyFormat = new Testimonial();
        onlyFormat.setCooperationFormat("prowadzenie online");
        assertThat(onlyFormat.signature()).isEqualTo("prowadzenie online");
    }

    @Test
    @DisplayName("pytanie bez odpowiedzi nie trafia na stronę")
    void unansweredFaqIsHidden() {
        OnlineFaq q = new OnlineFaq();
        q.setQuestion("Dla kogo jest prowadzenie online?");
        assertThat(q.answered()).isFalse();

        q.setAnswer("Dla osób 35–55 lat.");
        assertThat(q.answered()).isTrue();
    }
}
