package pl.szymtrener.offer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rabat i waznosc pakietu. Brief stacjonarny opiera na tych dwoch liczbach caly
 * argument sprzedazowy („roznica rosnie z 21% do 29%"), wiec licza sie co do punktu.
 */
class StationaryOfferServiceTest {

    private StationaryPackage pack(int sessions, int priceGr, Integer weeks) {
        StationaryPackage p = new StationaryPackage();
        p.setName("Pakiet " + sessions);
        p.setSessions(sessions);
        p.setPricePerSessionGr(priceGr);
        p.setValidityWeeks(weeks);
        return p;
    }

    @Test
    @DisplayName("rabat liczy się względem wejścia pojedynczego, w całych procentach")
    void discountAgainstSingleEntry() {
        // cennik po podwyzce z briefu: pojedynczy 210 zl, pakiety bez zmian
        int single = 21000;
        assertThat(pack(4, 17000, 6).discountPercent(single)).isEqualTo(19);
        assertThat(pack(8, 16000, 10).discountPercent(single)).isEqualTo(24);
        assertThat(pack(12, 15000, 16).discountPercent(single)).isEqualTo(29);
    }

    @Test
    @DisplayName("pakiet nie tańszy od wejścia pojedynczego nie chwali się rabatem")
    void noDiscountWhenNotCheaper() {
        assertThat(pack(1, 21000, null).discountPercent(21000)).isZero();
        assertThat(pack(4, 22000, 6).discountPercent(21000)).isZero();
        // brak ceny odniesienia nie moze dac dzielenia przez zero
        assertThat(pack(4, 17000, 6).discountPercent(0)).isZero();
    }

    @Test
    @DisplayName("kwota razem to cena za trening razy liczba treningów")
    void totalIsComputed() {
        assertThat(pack(12, 15000, 16).totalGr()).isEqualTo(180000);
        assertThat(Money.format(pack(12, 15000, 16).totalGr())).isEqualTo("1 800 zł");
        assertThat(pack(1, 21000, null).totalGr()).isEqualTo(21000);
    }

    @Test
    @DisplayName("ważność odmienia się po polsku")
    void validityIsInflected() {
        assertThat(StationaryOfferService.validityLabel(4)).isEqualTo("Ważny 4 tygodnie");
        assertThat(StationaryOfferService.validityLabel(6)).isEqualTo("Ważny 6 tygodni");
        assertThat(StationaryOfferService.validityLabel(10)).isEqualTo("Ważny 10 tygodni");
        assertThat(StationaryOfferService.validityLabel(12)).isEqualTo("Ważny 12 tygodni");
        assertThat(StationaryOfferService.validityLabel(16)).isEqualTo("Ważny 16 tygodni");
        assertThat(StationaryOfferService.validityLabel(22)).isEqualTo("Ważny 22 tygodnie");
    }

    @Test
    @DisplayName("wejście pojedyncze nie ma terminu ważności")
    void singleEntryHasNoValidity() {
        assertThat(StationaryOfferService.validityLabel(null)).isNull();
        assertThat(StationaryOfferService.validityLabel(0)).isNull();
        assertThat(pack(1, 21000, null).single()).isTrue();
        assertThat(pack(4, 17000, 6).single()).isFalse();
    }

    @Test
    @DisplayName("kwota bez jednostki służy zapisom typu „od 200 do 240 zł”")
    void amountWithoutUnit() {
        assertThat(Money.amount(20000)).isEqualTo("200");
        assertThat(Money.amount(180000)).isEqualTo("1 800");
        assertThat(Money.format(20000)).isEqualTo("200 zł");
    }
}
