package com.kisansetu.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StatusTrackerTest {

    @Test
    void phoneNumbersCompareOnLastTenDigits() {
        assertThat(PhoneNumbers.last10("whatsapp:+919876543210")).isEqualTo("9876543210");
        assertThat(PhoneNumbers.last10("+91 98765-43210")).isEqualTo("9876543210");
        assertThat(PhoneNumbers.last10("12345")).isEqualTo("");
        assertThat(PhoneNumbers.sameNumber("whatsapp:+919876543210", "9876543210")).isTrue();
        assertThat(PhoneNumbers.sameNumber("whatsapp:+919876543211", "9876543210")).isFalse();
        assertThat(PhoneNumbers.sameNumber("", "")).isFalse();
        assertThat(PhoneNumbers.sameNumber(null, "9876543210")).isFalse();
    }

    @Test
    void parsesStatusCommands() {
        assertThat(StatusCommandParser.kccAccount("STATUS MP2025000101")).isEqualTo(Optional.of("MP2025000101"));
        assertThat(StatusCommandParser.kccAccount("  status   mp2025000101 ")).isEqualTo(Optional.of("MP2025000101"));
        assertThat(StatusCommandParser.kccAccount("MP2025000101")).isEqualTo(Optional.of("MP2025000101"));
    }

    @Test
    void everythingElseIsNotACommand() {
        assertThat(StatusCommandParser.kccAccount("")).isEqualTo(Optional.empty());
        assertThat(StatusCommandParser.kccAccount(null)).isEqualTo(Optional.empty());
        assertThat(StatusCommandParser.kccAccount("hello there")).isEqualTo(Optional.empty());
        assertThat(StatusCommandParser.kccAccount("STATUS")).isEqualTo(Optional.empty());
        assertThat(StatusCommandParser.kccAccount("HELP")).isEqualTo(Optional.empty());
        assertThat(StatusCommandParser.kccAccount("STATUS ab")).isEqualTo(Optional.empty());
        assertThat(StatusCommandParser.kccAccount("STATUS MP2025000101 extra")).isEqualTo(Optional.empty());
    }

    @Test
    void statusMessageListsEachClaim() {
        String text = StatusMessages.status("MP2025000101", List.of(
                new ClaimStatusLine("IS", "2025-2026", "APPROVED", null),
                new ClaimStatusLine("PRI", "2025-2026", "REJECTED", "Aadhaar name does not match KYC"),
                new ClaimStatusLine("IS", "2024-2025", "PENDING", null)));

        assertThat(text.contains("KCC MP2025000101")).isTrue();
        assertThat(text.contains("IS 2025-2026: APPROVED")).isTrue();
        assertThat(text.contains("PRI 2025-2026: REJECTED - Aadhaar name does not match KYC")).isTrue();
        assertThat(text.contains("IS 2024-2025: PENDING")).isTrue();
    }

    @Test
    void reasonShownOnlyForRejectedAndLongReasonsAreShortened() {
        String longReason = "x".repeat(300);
        String text = StatusMessages.status("MP2025000101", List.of(
                new ClaimStatusLine("IS", "2025-2026", "PENDING", "should not appear"),
                new ClaimStatusLine("PRI", "2025-2026", "REJECTED", longReason)));

        assertThat(text.contains("should not appear")).isFalse();
        assertThat(text.contains("x".repeat(300))).isFalse();
        assertThat(text.contains("...")).isTrue();
    }

    @Test
    void messageStaysWithinLimitEvenWithManyClaims() {
        List<ClaimStatusLine> many = java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> new ClaimStatusLine("IS", "20" + (10 + i % 80) + "-2026", "PENDING", null)).toList();
        String text = StatusMessages.status("MP2025000101", many);
        assertThat(text.length() <= StatusMessages.MAX_LENGTH + 80).isTrue();
        assertThat(text.contains("more claims not shown")).isTrue();
    }

    @Test
    void noClaimsMessage() {
        assertThat(StatusMessages.status("MP2025000101", List.of()).contains("No claim has been filed")).isTrue();
    }

    @Test
    void twimlEscapesXmlAndStripsControlCharacters() {
        String xml = TwiML.message("a & b <c> \"d\" 'e'\u0001\nnext");
        assertThat(xml.contains("a &amp; b &lt;c&gt; &quot;d&quot; &apos;e&apos;\nnext")).isTrue();
        assertThat(xml.contains("\u0001")).isFalse();
        assertThat(xml.startsWith("<?xml")).isTrue();
    }

    @Test
    void twilioSignatureMatchesDocumentedExample() {
        Map<String, String> params = Map.of("CallSid", "CA1234567890ABCDE", "Caller", "+14158675310",
                "Digits", "1234", "From", "+14158675310", "To", "+18005551212");
        String url = "https://mycompany.com/myapp.php?foo=1&bar=2";

        assertThat(TwilioSignatureVerifier.sign("12345", url, params)).isEqualTo("GvWf1cFY/Q7PnoempGyD5oXAezc=");
        assertThat(TwilioSignatureVerifier.isValid("12345", url, params, "GvWf1cFY/Q7PnoempGyD5oXAezc=")).isTrue();
    }

    @Test
    void twilioSignatureRejectsTamperingMissingHeaderAndEmptyToken() {
        Map<String, String> params = Map.of("From", "whatsapp:+919876543210", "Body", "STATUS MP2025000101");
        String url = "https://kisansetu.example.com/api/webhooks/twilio";
        String good = "2v4C9cTHrOVaUOStV2dbFLshLh0=";

        assertThat(TwilioSignatureVerifier.sign("test-auth-token", url, params)).isEqualTo(good);
        assertThat(TwilioSignatureVerifier.isValid("test-auth-token", url, params, good)).isTrue();

        Map<String, String> changed = Map.of("From", "whatsapp:+919876543299", "Body", "STATUS MP2025000101");
        assertThat(TwilioSignatureVerifier.isValid("test-auth-token", url, changed, good)).isFalse();
        assertThat(TwilioSignatureVerifier.isValid("test-auth-token", url + "x", params, good)).isFalse();
        assertThat(TwilioSignatureVerifier.isValid("other-token", url, params, good)).isFalse();
        assertThat(TwilioSignatureVerifier.isValid("test-auth-token", url, params, null)).isFalse();
        assertThat(TwilioSignatureVerifier.isValid("", url, params, good)).isFalse();
    }
}
