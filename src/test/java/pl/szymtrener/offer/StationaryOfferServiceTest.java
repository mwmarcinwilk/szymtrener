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

    private StationaryPackage care(int perSessionGr) {
        StationaryPackage p = pack(8, perSessionGr, null);
        p.setName("Opieka Longevity");
        p.setTermMonths(12);
        return p;
    }

    @Test
    @DisplayName("rabaty z cennika 10/2026 zgadzają się co do punktu z dokumentem klienta")
    void discountsMatchOctoberPriceList() {
        // pary po zmianie pojedynczego treningu 240 -> 270 zl
        int pairSingle = 27000;
        assertThat(pack(4, 22000, 6).discountPercent(pairSingle)).isEqualTo(19);
        assertThat(pack(8, 21000, 10).discountPercent(pairSingle)).isEqualTo(22);
        assertThat(pack(12, 20000, 16).discountPercent(pairSingle)).isEqualTo(26);
        // Opieka Longevity
        assertThat(care(13500).discountPercent(21000)).isEqualTo(36);
        assertThat(care(18000).discountPercent(pairSingle)).isEqualTo(33);
    }

    @Test
    @DisplayName("rok w Opiece kontra rok w pakietach po 12: kwoty z dokumentu, liczone z cen")
    void yearCompareFromPrices() {
        StationaryOfferService.YearCompare one = StationaryOfferService.yearCompare(care(13500), pack(12, 15000, 16));
        assertThat(one.packageSessions()).isEqualTo(12);
        assertThat(one.packagesYear()).isEqualTo("14\u00A0400\u00A0zł");
        assertThat(one.careYear()).isEqualTo("12\u00A0960\u00A0zł");
        assertThat(one.difference()).isEqualTo("1\u00A0440\u00A0zł");
        assertThat(one.freeSessions()).isEqualTo("prawie 10 treningów");

        StationaryOfferService.YearCompare pair = StationaryOfferService.yearCompare(care(18000), pack(12, 20000, 16));
        assertThat(pair.packagesYear()).isEqualTo("19\u00A0200\u00A0zł");
        assertThat(pair.careYear()).isEqualTo("17\u00A0280\u00A0zł");
    }

    @Test
    @DisplayName("Opieka droższa od pakietu nie chwali się różnicą")
    void noYearCompareWhenCareIsNotCheaper() {
        assertThat(StationaryOfferService.yearCompare(care(15000), pack(12, 15000, 16))).isNull();
    }

    @Test
    @DisplayName("różnica w treningach: równa wielokrotność, „prawie” i „ponad”")
    void differenceInSessions() {
        assertThat(StationaryOfferService.inSessions(144000, 15000)).isEqualTo("prawie 10 treningów");
        assertThat(StationaryOfferService.inSessions(150000, 15000)).isEqualTo("10 treningów");
        assertThat(StationaryOfferService.inSessions(30000, 15000)).isEqualTo("2 treningi");
        assertThat(StationaryOfferService.inSessions(32000, 15000)).isEqualTo("ponad 2 treningi");
        assertThat(StationaryOfferService.inSessions(15000, 15000)).isEqualTo("1 trening");
    }

    @Test
    @DisplayName("Opieka ma długość umowy i płatność co miesiąc zamiast ważności w tygodniach")
    void termLabel() {
        assertThat(StationaryOfferService.termLabel(12)).isEqualTo("12 miesięcy, płatność co miesiąc");
        assertThat(StationaryOfferService.termLabel(3)).isEqualTo("3 miesiące, płatność co miesiąc");
        assertThat(StationaryOfferService.termLabel(null)).isEqualTo("płatność co miesiąc");
    }

    @Test
    @DisplayName("nawias o Opiece w zdaniu FAQ idzie z długości umowy i liczby treningów w miesiącu")
    void careTermsFromData() {
        assertThat(StationaryOfferService.careTerms(care(13500))).isEqualTo("rok współpracy, 2 treningi w tygodniu");
        StationaryPackage half = care(13500);
        half.setTermMonths(6);
        half.setSessions(12);
        assertThat(StationaryOfferService.careTerms(half)).isEqualTo("6 miesięcy współpracy, 3 treningi w tygodniu");
        half.setSessions(10);
        assertThat(StationaryOfferService.careTerms(half)).isEqualTo("6 miesięcy współpracy, 10 treningów w miesiącu");
    }

    @Test
    @DisplayName("pozycja jest miesięczna dokładnie wtedy, gdy ma długość umowy")
    void monthlyFollowsTerm() {
        assertThat(care(13500).isMonthly()).isTrue();
        assertThat(pack(12, 15000, 16).isMonthly()).isFalse();
    }

    @Test
    @DisplayName("nazwa Opieki odmienia się w zdaniu o cenach, inna nazwa zostaje bez zmian")
    void locative() {
        assertThat(StationaryOfferService.locative("Opieka Longevity")).isEqualTo("Opiece Longevity");
        assertThat(StationaryOfferService.locative("Abonament roczny")).isEqualTo("Abonament roczny");
    }

    @Test
    @DisplayName("absurdalna długość umowy z panelu nie wywraca strony: rachunek roczny znika")
    void yearCompareSurvivesOverflow() {
        StationaryPackage huge = care(13500);
        huge.setTermMonths(120_000);
        assertThat(StationaryOfferService.yearCompare(huge, pack(12, 15000, 16))).isNull();
    }

    private StationaryOfferService serviceWith(StationaryPackage... all) {
        StationaryPackageRepository repo = org.mockito.Mockito.mock(StationaryPackageRepository.class);
        org.mockito.Mockito.when(repo.findAllByOrderByKindAscSortOrderAsc()).thenReturn(java.util.List.of(all));
        return new StationaryOfferService(repo, org.mockito.Mockito.mock(pl.szymtrener.settings.SettingsService.class));
    }

    private StationaryPackage of(StationaryKind kind, StationaryPackage p) {
        p.setKind(kind);
        return p;
    }

    @Test
    @DisplayName("sama para bez pakietów: zdanie o cenach nie mówi „od 270 do 270 zł”")
    void pairSentenceNeedsCheaperOption() {
        StationaryOfferService.Page page = serviceWith(
                of(StationaryKind.INDYWIDUALNY, pack(1, 21000, null)),
                of(StationaryKind.PARA, pack(1, 27000, null))).page();
        assertThat(page.priceSentence()).isEqualTo("Pojedynczy trening w studiu kosztuje 210\u00A0zł.");
    }

    @Test
    @DisplayName("Opieka tylko w cenniku par też włącza blok, do którego linkuje karta")
    void careOnlyForPairsCountsAsCare() {
        StationaryOfferService.Page page = serviceWith(
                of(StationaryKind.INDYWIDUALNY, pack(1, 21000, null)),
                of(StationaryKind.PARA, care(18000))).page();
        assertThat(page.hasCare()).isTrue();
    }

    @Test
    @DisplayName("rachunek roczny i zdanie o cenach opisują tę samą, najtańszą Opiekę")
    void yearCompareUsesSameCareAsSentence() {
        StationaryPackage shorter = care(14000);
        shorter.setName("Opieka krótka");
        shorter.setTermMonths(6);
        StationaryOfferService.Page page = serviceWith(
                of(StationaryKind.INDYWIDUALNY, pack(1, 21000, null)),
                of(StationaryKind.INDYWIDUALNY, pack(12, 15000, 16)),
                of(StationaryKind.INDYWIDUALNY, shorter),
                of(StationaryKind.INDYWIDUALNY, care(13500))).page();
        assertThat(page.priceSentence()).contains("w Opiece Longevity (rok współpracy, 2 treningi w tygodniu) do 135\u00A0zł");
        assertThat(page.careCompare().careYear()).isEqualTo("12\u00A0960\u00A0zł");
    }
}
