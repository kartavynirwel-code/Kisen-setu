package com.kisansetu.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VerhoeffTest {

    @Test
    void acceptsPublishedTestVectors() {
        assertThat(Verhoeff.isValid("2363")).isTrue();
        assertThat(Verhoeff.isValid("123451")).isTrue();
    }

    @Test
    void acceptsNumbersWithCorrectCheckDigit() {
        assertThat(Verhoeff.isValid("234123412346")).isTrue();
        assertThat(Verhoeff.isValid("987654321096")).isTrue();
        assertThat(Verhoeff.isValid("456789123451")).isTrue();
    }

    @Test
    void rejectsAnySingleDigitChange() {
        String valid = "234123412346";
        for (int pos = 0; pos < valid.length(); pos++) {
            for (char digit = '0'; digit <= '9'; digit++) {
                if (digit == valid.charAt(pos)) {
                    continue;
                }
                String changed = valid.substring(0, pos) + digit + valid.substring(pos + 1);
                assertThat(Verhoeff.isValid(changed)).isFalse();
            }
        }
    }

    @Test
    void rejectsAdjacentTransposition() {
        // 4 and 6 swapped at the end
        assertThat(Verhoeff.isValid("234123412364")).isFalse();
    }

    @Test
    void rejectsNonDigits() {
        assertThat(Verhoeff.isValid("23412341234A")).isFalse();
    }
}
