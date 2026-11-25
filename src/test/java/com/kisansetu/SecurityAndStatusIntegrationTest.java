package com.kisansetu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kisansetu.status.TwilioSignatureVerifier;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

/** Phase 4 and 5 end to end: login and roles, saved claims with encrypted data, status updates, SMS/WhatsApp tracker. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class SecurityAndStatusIntegrationTest extends AbstractApiTest {

    private static final String WEBHOOK_URL = "http://localhost/api/webhooks/twilio";
    private static final String TWILIO_TOKEN = "test-auth-token";

    private MockMultipartFile sample() throws Exception {
        return new MockMultipartFile("file", "sample_claims.csv", "text/csv",
                new ClassPathResource("sample_claims.csv").getContentAsByteArray());
    }

    private void uploadSampleAs(String token) throws Exception {
        mockMvc.perform(multipart("/api/batches").file(sample()).header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMMITTED"))
                .andExpect(jsonPath("$.validRows").value(85));
    }

    private String createOperator() throws Exception {
        mockMvc.perform(post("/api/users").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Operator1\",\"password\":\"operator-password-1\",\"role\":\"CSC_OPERATOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("operator1"));
        return login("operator1", "operator-password-1");
    }

    private String webhook(String from, String body, String signature) throws Exception {
        Map<String, String> params = Map.of("From", from, "Body", body);
        String sig = signature != null ? signature : TwilioSignatureVerifier.sign(TWILIO_TOKEN, WEBHOOK_URL, params);
        return mockMvc.perform(post("/api/webhooks/twilio").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("From", from).param("Body", body).header("X-Twilio-Signature", sig))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // ---------- authentication and roles ----------

    @Test
    void apiRequiresLoginAndWrongPasswordIsRejected() throws Exception {
        mockMvc.perform(get("/api/claims")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/claims").header("Authorization", "Bearer not.a.token")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password-xx\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void repeatedFailedLoginsAreLockedOut() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"ghost\",\"password\":\"wrong-password-xx\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ghost\",\"password\":\"wrong-password-xx\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void operatorCanUploadButNotManageUsersOrClaimStatus() throws Exception {
        String operator = createOperator();

        mockMvc.perform(get("/api/claims").header("Authorization", bearer(operator))).andExpect(status().isOk());
        mockMvc.perform(post("/api/users").header("Authorization", bearer(operator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"another\",\"password\":\"another-password-1\",\"role\":\"CSC_OPERATOR\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/claims/1/status").header("Authorization", bearer(operator))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void weakPasswordAndDuplicateUsernameAreRejected() throws Exception {
        String admin = bearer(adminToken());
        mockMvc.perform(post("/api/users").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"weak\",\"password\":\"short\",\"role\":\"CSC_OPERATOR\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/users").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ADMIN\",\"password\":\"long-enough-password\",\"role\":\"CSC_OPERATOR\"}"))
                .andExpect(status().isConflict());
    }

    // ---------- committing claims ----------

    @Test
    void commitSavesValidRowsEncryptedAndRejectsTheSameFileTheSecondTime() throws Exception {
        String operator = createOperator();
        uploadSampleAs(operator);

        mockMvc.perform(get("/api/claims").header("Authorization", bearer(operator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(85));

        // Aadhaar and bank account are never stored in clear
        String aadhaar = jdbc.queryForObject("SELECT aadhaar_enc FROM farmer LIMIT 1", String.class);
        String bank = jdbc.queryForObject("SELECT bank_account_enc FROM farmer LIMIT 1", String.class);
        assertThat(aadhaar).startsWith("v1:");
        assertThat(aadhaar.matches("\\d{12}")).isFalse();
        assertThat(bank).startsWith("v1:");
        String hash = jdbc.queryForObject("SELECT aadhaar_hash FROM farmer LIMIT 1", String.class);
        assertThat(hash.length()).isEqualTo(64);

        // the same file again: every previously saved claim is now a duplicate, nothing new is saved
        MvcResult again = mockMvc.perform(multipart("/api/batches").file(sample()).header("Authorization", bearer(operator)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.validRows").value(0))
                .andReturn();
        long id = ((Number) JsonPath.read(again.getResponse().getContentAsString(), "$.id")).longValue();
        mockMvc.perform(get("/api/batches/" + id + "/errors").header("Authorization", bearer(operator)))
                .andExpect(jsonPath("$[?(@.code == 'DUPLICATE_CLAIM_STORED')]").isNotEmpty());
        Integer claims = jdbc.queryForObject("SELECT COUNT(*) FROM claim", Integer.class);
        assertThat(claims).isEqualTo(85);
    }

    // ---------- claim status ----------

    @Test
    void adminUpdatesClaimStatusAndRejectionNeedsAReason() throws Exception {
        uploadSampleAs(adminToken());
        String admin = bearer(adminToken());
        long claimId = jdbc.queryForObject("SELECT MIN(id) FROM claim", Long.class);

        mockMvc.perform(patch("/api/claims/" + claimId + "/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/claims/" + claimId + "/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\",\"rejectionReason\":\"Land record not found\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Land record not found"));

        mockMvc.perform(get("/api/claims?status=REJECTED").header("Authorization", admin))
                .andExpect(jsonPath("$.total").value(1));

        mockMvc.perform(patch("/api/claims/" + claimId + "/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejectionReason").doesNotExist());

        mockMvc.perform(patch("/api/claims/999999/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isNotFound());
    }

    // ---------- SMS / WhatsApp tracker ----------

    @Test
    void trackerAnswersOnlyTheRegisteredNumberAndOnlyForSignedRequests() throws Exception {
        uploadSampleAs(adminToken());
        Map<String, Object> farmer = jdbc.queryForMap("SELECT kcc_account_no, mobile FROM farmer LIMIT 1");
        String kcc = (String) farmer.get("kcc_account_no");
        String mobile = (String) farmer.get("mobile");

        String reply = webhook("whatsapp:+91" + mobile, "STATUS " + kcc, null);
        assertThat(reply).contains("KCC " + kcc);
        assertThat(reply).contains("PENDING");

        // a different number gets the same reply as an account that does not exist
        String otherNumber = "whatsapp:+91" + (mobile.startsWith("9") ? "8" : "9") + mobile.substring(1);
        String wrongNumber = webhook(otherNumber, "STATUS " + kcc, null);
        String noSuchAccount = webhook(otherNumber, "STATUS MP9999999999", null);
        assertThat(wrongNumber).contains("No claim found");
        assertThat(wrongNumber).isEqualTo(noSuchAccount);

        assertThat(webhook("whatsapp:+919000000001", "hello", null)).contains("Send: STATUS");

        // unsigned, wrongly signed: refused
        mockMvc.perform(post("/api/webhooks/twilio").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("From", "whatsapp:+91" + mobile).param("Body", "STATUS " + kcc))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/webhooks/twilio").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("From", "whatsapp:+91" + mobile).param("Body", "STATUS " + kcc)
                        .header("X-Twilio-Signature", "AAAAAAAAAAAAAAAAAAAAAAAAAAA="))
                .andExpect(status().isForbidden());
    }

    @Test
    void trackerRepliesWithTheRejectionReason() throws Exception {
        uploadSampleAs(adminToken());
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT c.id, f.kcc_account_no, f.mobile FROM claim c JOIN farmer f ON f.id = c.farmer_id LIMIT 1");
        mockMvc.perform(patch("/api/claims/" + row.get("id") + "/status").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\",\"rejectionReason\":\"IFSC does not match the bank\"}"))
                .andExpect(status().isOk());

        String reply = webhook("+91" + row.get("mobile"), "STATUS " + row.get("kcc_account_no"), null);

        assertThat(reply).contains("REJECTED - IFSC does not match the bank");
    }

    @Test
    void trackerIsRateLimitedPerPhoneNumber() throws Exception {
        String from = "whatsapp:+919111111111";
        for (int i = 0; i < 10; i++) {
            assertThat(webhook(from, "hello", null)).contains("Send: STATUS");
        }
        assertThat(webhook(from, "hello", null)).contains("Too many requests");
    }
}
