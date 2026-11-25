package com.kisansetu.status;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifies the X-Twilio-Signature header: base64(HMAC-SHA1(authToken, url + each POST parameter name and value,
 * sorted by name)). HMAC-SHA1 is what the Twilio protocol mandates; it is used here only as a keyed MAC.
 */
public final class TwilioSignatureVerifier {

    private TwilioSignatureVerifier() {
    }

    public static String sign(String authToken, String url, Map<String, String> params) {
        StringBuilder data = new StringBuilder(url);
        new TreeMap<>(params).forEach((name, value) -> data.append(name).append(value));
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(authToken.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA1 is not available", e);
        }
    }

    public static boolean isValid(String authToken, String url, Map<String, String> params, String signatureHeader) {
        if (signatureHeader == null || authToken == null || authToken.isBlank()) {
            return false;
        }
        byte[] expected = sign(authToken, url, params).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signatureHeader.getBytes(StandardCharsets.UTF_8));
    }
}
