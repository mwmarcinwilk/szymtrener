package pl.szymtrener.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SlugUtilTest {

    @ParameterizedTest(name = "„{0}\" -> {1}")
    @CsvSource(delimiter = '|', value = {
            "Dlaczego mięśnie to polisa na życie | dlaczego-miesnie-to-polisa-na-zycie",
            "Zażółć gęślą jaźń                   | zazolc-gesla-jazn",
            "Łódź i Łomża                        | lodz-i-lomza",
            "ŁÓDŹ WIELKIMI LITERAMI              | lodz-wielkimi-literami",
            "Trening 3x w tygodniu               | trening-3x-w-tygodniu",
            "Białko: ile naprawdę potrzebujesz?  | bialko-ile-naprawde-potrzebujesz",
            "Wiele     spacji   naraz            | wiele-spacji-naraz",
            "---myślniki na brzegach---          | myslniki-na-brzegach",
    })
    @DisplayName("zamienia polskie znaki i interpunkcję na czysty adres")
    void slugifiesPolishText(String input, String expected) {
        assertThat(SlugUtil.slugify(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("pusty tytuł daje pusty slug, a nie wyjątek")
    void handlesEmptyInput(String input) {
        assertThat(SlugUtil.slugify(input)).isEmpty();
    }

    @Test
    @DisplayName("bardzo długi tytuł jest przycinany do 90 znaków i nie kończy się myślnikiem")
    void trimsLongTitles() {
        String title = "Dlaczego trening siłowy po czterdziestce jest najlepszą inwestycją w zdrowie "
                + "na kolejne dekady życia i sprawności";

        String slug = SlugUtil.slugify(title);

        assertThat(slug).hasSizeLessThanOrEqualTo(90);
        assertThat(slug).doesNotEndWith("-");
        assertThat(slug).startsWith("dlaczego-trening-silowy");
    }

    @Test
    @DisplayName("tytuł bez znaków alfanumerycznych daje pusty slug — obsługuje go uniqueSlug()")
    void handlesPunctuationOnlyTitle() {
        assertThat(SlugUtil.slugify("!!! ??? ...")).isEmpty();
    }

    @Test
    @DisplayName("ten sam tytuł zawsze daje ten sam slug")
    void isDeterministic() {
        String title = "Sarkopenia po 50-tce: co mówią badania";
        assertThat(SlugUtil.slugify(title)).isEqualTo(SlugUtil.slugify(title));
    }
}
