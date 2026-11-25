package com.kisansetu.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NameMatcherTest {

    @Test
    void ignoresCaseSpacingAndPunctuation() {
        assertThat(NameMatcher.matches("RAMESH   PATEL", "Ramesh Patel", 0.99)).isTrue();
        assertThat(NameMatcher.matches("Ramesh K. Patel", "Ramesh K Patel", 0.99)).isTrue();
    }

    @Test
    void ignoresWordOrder() {
        assertThat(NameMatcher.matches("Patel Ramesh", "Ramesh Patel", 0.99)).isTrue();
    }

    @Test
    void toleratesSmallSpellingDifference() {
        assertThat(NameMatcher.matches("Ramesh Patel", "Rameshh Patel", 0.88)).isTrue();
        assertThat(NameMatcher.matches("Sunita Devi", "Sunitha Devi", 0.88)).isTrue();
    }

    @Test
    void rejectsDifferentPeople() {
        assertThat(NameMatcher.matches("Ramesh Patel", "Mahesh Chandra Joshi", 0.85)).isFalse();
        assertThat(NameMatcher.matches("Sunita Devi", "Kavita Bai", 0.85)).isFalse();
    }

    @Test
    void emptyNamesNeverMatchNonEmpty() {
        assertThat(NameMatcher.similarity("", "Ramesh")).isEqualTo(0.0);
    }
}
