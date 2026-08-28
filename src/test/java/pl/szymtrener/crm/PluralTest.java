package pl.szymtrener.crm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Odmiana liczebnikow. Panel pokazuje te napisy przy kazdym kliencie, wiec
 * „1 pakietow" bylo widoczne od razu — a regula 12-14 to najczestszy blad.
 */
class PluralTest {

    @Test
    @DisplayName("jedynka, dwójka i piątka mają trzy różne formy")
    void threeForms() {
        assertThat(Plural.sessions(1)).isEqualTo("1 trening");
        assertThat(Plural.sessions(2)).isEqualTo("2 treningi");
        assertThat(Plural.sessions(4)).isEqualTo("4 treningi");
        assertThat(Plural.sessions(5)).isEqualTo("5 treningów");
        assertThat(Plural.sessions(0)).isEqualTo("0 treningów");
    }

    @Test
    @DisplayName("nastki biorą formę dopełniaczową mimo końcówki 2–4")
    void teensAreException() {
        assertThat(Plural.sessions(12)).isEqualTo("12 treningów");
        assertThat(Plural.sessions(13)).isEqualTo("13 treningów");
        assertThat(Plural.sessions(14)).isEqualTo("14 treningów");
        // a dwadziescia dwa juz nie
        assertThat(Plural.sessions(22)).isEqualTo("22 treningi");
        assertThat(Plural.sessions(112)).isEqualTo("112 treningów");
        assertThat(Plural.sessions(122)).isEqualTo("122 treningi");
    }

    @Test
    @DisplayName("pozostałe formy używane w panelu")
    void otherForms() {
        assertThat(Plural.packages(1)).isEqualTo("1 pakiet");
        assertThat(Plural.packages(3)).isEqualTo("3 pakiety");
        assertThat(Plural.packages(7)).isEqualTo("7 pakietów");
        assertThat(Plural.cancelled(1)).isEqualTo("1 odwołany");
        assertThat(Plural.cancelled(2)).isEqualTo("2 odwołane");
        assertThat(Plural.cancelled(5)).isEqualTo("5 odwołanych");
    }
}
