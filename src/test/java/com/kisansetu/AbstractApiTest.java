package com.kisansetu;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Shared helpers for API tests. Each subclass adds the Spring annotations itself. */
public abstract class AbstractApiTest {

    protected static final String ADMIN_USER = "admin";
    protected static final String ADMIN_PASSWORD = "test-admin-password-123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    /** Tests share one database, so start each from the same state: no data, only the seeded admin. */
    @BeforeEach
    void cleanDatabase() {
        for (String table : new String[] {"claim", "land_record", "kyc_record", "farmer", "validation_error",
                "claim_batch", "audit_log"}) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("DELETE FROM app_user WHERE username <> ?", ADMIN_USER);
    }

    protected String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    protected String adminToken() throws Exception {
        return login(ADMIN_USER, ADMIN_PASSWORD);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
