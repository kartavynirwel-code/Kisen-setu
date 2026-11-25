package com.kisansetu.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class HmacAndKeysTest {

    @Test
    void matchesRfc4231TestCase2() {
        String mac = Hmac.sha256Hex("Jefe".getBytes(StandardCharsets.UTF_8), "what do ya want for nothing?");
        assertThat(mac).isEqualTo("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843");
    }

    @Test
    void aadhaarHashIsDeterministicKeyedAndHidesTheNumber() {
        AadhaarHasher a = new AadhaarHasher(new byte[32]);
        byte[] otherKey = new byte[32];
        otherKey[0] = 1;
        AadhaarHasher b = new AadhaarHasher(otherKey);

        assertThat(a.hash("234123412346")).isEqualTo(a.hash("234123412346"));
        assertThat(a.hash("234123412346").equals(b.hash("234123412346"))).isFalse();
        assertThat(a.hash("234123412346").equals(a.hash("234123412347"))).isFalse();
        assertThat(a.hash("234123412346").contains("234123412346")).isFalse();
        assertThat(a.hash("234123412346").length()).isEqualTo(64);
    }

    @Test
    void hasherRejectsShortKey() {
        assertThatThrownBy(() -> new AadhaarHasher(new byte[16])).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void keysDecodeAndRejectWeakOrMalformedValues() {
        String good = Base64.getEncoder().encodeToString(new byte[32]);
        assertThat(Keys.decode(good, 32, "K").length).isEqualTo(32);

        assertThatThrownBy(() -> Keys.decode("", 32, "APP_X")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_X is not set");
        assertThatThrownBy(() -> Keys.decode("***", 32, "APP_X")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not valid base64");
        assertThatThrownBy(() -> Keys.decode(Base64.getEncoder().encodeToString(new byte[8]), 32, "APP_X"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("at least 32 bytes");
    }

    @Test
    void passwordPolicy() {
        PasswordPolicy.check("correct-horse-battery");
        assertThatThrownBy(() -> PasswordPolicy.check("short")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PasswordPolicy.check(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PasswordPolicy.check("x".repeat(73))).isInstanceOf(IllegalArgumentException.class);
    }
}
