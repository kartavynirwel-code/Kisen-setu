package com.kisansetu.status;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Twilio calls this for every incoming SMS / WhatsApp message and sends our TwiML reply back to the farmer.
 * Not behind login (Twilio cannot log in); instead every request must carry a valid X-Twilio-Signature.
 */
@RestController
@RequestMapping("/api/webhooks")
public class TwilioWebhookController {

    private final StatusLookupService lookup;
    private final String authToken;
    private final String publicUrl;

    public TwilioWebhookController(StatusLookupService lookup,
                                   @Value("${app.twilio.auth-token:}") String authToken,
                                   @Value("${app.twilio.webhook-url:}") String publicUrl) {
        this.lookup = lookup;
        this.authToken = authToken;
        this.publicUrl = publicUrl;
    }

    @PostMapping(value = "/twilio", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> receive(HttpServletRequest request,
                                          @RequestHeader(value = "X-Twilio-Signature", required = false) String signature) {
        if (authToken.isBlank() || publicUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build(); // tracker not configured
        }
        Map<String, String> params = new TreeMap<>();
        request.getParameterMap().forEach((name, values) -> params.put(name, values.length > 0 ? values[0] : ""));

        if (!TwilioSignatureVerifier.isValid(authToken, publicUrl, params, signature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String reply = lookup.reply(params.getOrDefault("From", ""), params.getOrDefault("Body", ""));
        return ResponseEntity.ok(TwiML.message(reply));
    }
}
