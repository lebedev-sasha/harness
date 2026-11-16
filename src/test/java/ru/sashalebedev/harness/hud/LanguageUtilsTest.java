package ru.sashalebedev.harness.hud;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.sashalebedev.harness.hud.LanguageUtils.plural;

class LanguageUtilsTest {

    @Test
    void oneGivesSingularForm() {
        assertThat(plural(1, "бин", "бина", "бинов")).isEqualTo("1 бин");
        assertThat(plural(21, "бин", "бина", "бинов")).isEqualTo("21 бин");
        assertThat(plural(101, "бин", "бина", "бинов")).isEqualTo("101 бин");
    }

    @Test
    void twoThreeFourGiveFewForm() {
        assertThat(plural(2, "бин", "бина", "бинов")).isEqualTo("2 бина");
        assertThat(plural(3, "бин", "бина", "бинов")).isEqualTo("3 бина");
        assertThat(plural(4, "бин", "бина", "бинов")).isEqualTo("4 бина");
    }

    @Test
    void fiveAndBeyondGiveManyForm() {
        assertThat(plural(5, "бин", "бина", "бинов")).isEqualTo("5 бинов");
        assertThat(plural(0, "бин", "бина", "бинов")).isEqualTo("0 бинов");
        assertThat(plural(10, "бин", "бина", "бинов")).isEqualTo("10 бинов");
    }

    @Test
    void elevenToFourteenAlwaysGiveManyForm() {
        assertThat(plural(11, "бин", "бина", "бинов")).isEqualTo("11 бинов");
        assertThat(plural(12, "бин", "бина", "бинов")).isEqualTo("12 бинов");
        assertThat(plural(14, "бин", "бина", "бинов")).isEqualTo("14 бинов");
        assertThat(plural(111, "бин", "бина", "бинов")).isEqualTo("111 бинов");
    }
}
