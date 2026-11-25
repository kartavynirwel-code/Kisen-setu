package com.kisansetu.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class FieldCipherTest {

    private static byte[] key(int fill) {
        byte[] k = new byte[32];
        Arrays.fill(k, (byte) fill);
        return k;
    }

    private final FieldCipher cipher = new FieldCipher(key(7));

    @Test
    void roundTrips() {
        String token = cipher.encrypt("234123412346", "farmer.aadhaar");
        assertThat(cipher.decrypt(token, "farmer.aadhaar")).isEqualTo("234123412346");
    }

    @Test
    void ciphertextIsVersionedAndHidesThePlaintext() {
        String token = cipher.encrypt("234123412346", "farmer.aadhaar");
        assertThat(token.startsWith("v1:")).isTrue();
        assertThat(token.contains("234123412346")).isFalse();
    }

    @Test
    void sameValueEncryptsDifferentlyEachTime() {
        String a = cipher.encrypt("234123412346", "farmer.aadhaar");
        String b = cipher.encrypt("234123412346", "farmer.aadhaar");
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void ciphertextCannotBeMovedToAnotherColumn() {
        String token = cipher.encrypt("12345678901", "farmer.bank_account");
        assertThatThrownBy(() -> cipher.decrypt(token, "farmer.aadhaar"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not decrypt");
    }

    @Test
    void wrongKeyFails() {
        String token = cipher.encrypt("234123412346", "farmer.aadhaar");
        FieldCipher other = new FieldCipher(key(8));
        assertThatThrownBy(() -> other.decrypt(token, "farmer.aadhaar")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void tamperedCiphertextFails() {
        String token = cipher.encrypt("234123412346", "farmer.aadhaar");
        byte[] raw = Base64.getDecoder().decode(token.substring(3));
        raw[raw.length - 1] ^= 1;
        String tampered = "v1:" + Base64.getEncoder().encodeToString(raw);
        assertThatThrownBy(() -> cipher.decrypt(tampered, "farmer.aadhaar")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsGarbageAndWrongKeyLength() {
        assertThatThrownBy(() -> cipher.decrypt("not-a-token", "x")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cipher.decrypt("v1:AAAA", "x")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new FieldCipher(new byte[16])).isInstanceOf(IllegalArgumentException.class);
    }
}
