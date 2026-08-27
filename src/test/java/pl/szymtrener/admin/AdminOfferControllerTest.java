package pl.szymtrener.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zamiana zlotowek na grosze. Szymon wpisuje kwoty tak, jak je wymawia — a kazdy
 * blad w tym miejscu konczy sie zla cena na stronie, wiec warianty zapisu sa
 * sprawdzone wprost.
 */
class AdminOfferControllerTest {

    @Test
    @DisplayName("czyta kwotę niezależnie od tego, jak została wpisana")
    void parsesHumanInput() {
        assertThat(AdminOfferController.grosze("597")).isEqualTo(59700);
        assertThat(AdminOfferController.grosze("597,50")).isEqualTo(59750);
        assertThat(AdminOfferController.grosze("597.50")).isEqualTo(59750);
        assertThat(AdminOfferController.grosze("1 074")).isEqualTo(107400);
        assertThat(AdminOfferController.grosze("1 074 zł")).isEqualTo(107400);
        assertThat(AdminOfferController.grosze("  349  ")).isEqualTo(34900);
    }

    @Test
    @DisplayName("odmawia zapisu, gdy to nie jest kwota")
    void rejectsNonAmounts() {
        // null zamiast zera: cena 0 zl zapisana po cichu jest gorsza niz blad
        assertThat(AdminOfferController.grosze("")).isNull();
        assertThat(AdminOfferController.grosze("   ")).isNull();
        assertThat(AdminOfferController.grosze("do uzgodnienia")).isNull();
        assertThat(AdminOfferController.grosze("-100")).isNull();
        assertThat(AdminOfferController.grosze(null)).isNull();
    }

    @Test
    @DisplayName("wraca do pola formularza w tej samej postaci")
    void formatsBackForForm() {
        assertThat(AdminOfferController.zlote(59700)).isEqualTo("597");
        assertThat(AdminOfferController.zlote(59750)).isEqualTo("597,50");
        assertThat(AdminOfferController.zlote(107400)).isEqualTo("1074");
        assertThat(AdminOfferController.zlote(0)).isEqualTo("0");
    }

    @Test
    @DisplayName("kwota przepisana tam i z powrotem zostaje ta sama")
    void roundTrips() {
        for (int gr : new int[]{0, 12900, 34900, 59700, 107400, 178800, 238800, 999999}) {
            assertThat(AdminOfferController.grosze(AdminOfferController.zlote(gr)))
                    .as("kwota %d gr", gr).isEqualTo(gr);
        }
    }
}
