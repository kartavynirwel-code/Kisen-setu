package com.kisansetu.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kisansetu.AbstractApiTest;
import com.kisansetu.TestcontainersConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Dry-run uploads of the 100-row sample (85 valid rows, 15 rows with one deliberate error each, see
 * samples/EXPECTED_ERRORS.md): counts, error list, downloadable report and error handling.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class BatchApiIntegrationTest extends AbstractApiTest {

    private MockMultipartFile sampleCsv() throws IOException {
        byte[] content = new ClassPathResource("sample_claims.csv").getContentAsByteArray();
        return new MockMultipartFile("file", "sample_claims.csv", "text/csv", content);
    }

    @Test
    void dryRunValidatesSampleFileAndExposesErrorsAndReport() throws Exception {
        String auth = bearer(adminToken());

        MvcResult created = mockMvc.perform(multipart("/api/batches").file(sampleCsv())
                        .param("dryRun", "true").header("Authorization", auth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRows").value(100))
                .andExpect(jsonPath("$.validRows").value(85))
                .andExpect(jsonPath("$.errorRows").value(15))
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andReturn();
        long id = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(get("/api/batches/" + id).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("sample_claims.csv"));

        mockMvc.perform(get("/api/batches/" + id + "/errors").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(15))
                .andExpect(jsonPath("$[0].row").value(8))
                .andExpect(jsonPath("$[0].code").value("AADHAAR_CHECKSUM"));

        MvcResult report = mockMvc.perform(get("/api/batches/" + id + "/error-report").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();
        byte[] xlsx = report.getResponse().getContentAsByteArray();
        assertThat(xlsx).startsWith((byte) 0x50, (byte) 0x4B); // "PK": a zip container, i.e. a real .xlsx

        Integer saved = jdbc.queryForObject("SELECT COUNT(*) FROM claim", Integer.class);
        assertThat(saved).isEqualTo(0); // a dry run saves nothing but the batch and its errors
    }

    @Test
    void fileWithMissingColumnsIsRejectedWith400() throws Exception {
        MockMultipartFile bad = new MockMultipartFile("file", "bad.csv", "text/csv",
                "kcc_account_no,full_name\nKCC1,Ramesh\n".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/batches").file(bad).header("Authorization", bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Missing required column")));
    }

    @Test
    void unsupportedFileTypeIsRejectedWith400() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("file", "claims.pdf", "application/pdf", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/batches").file(pdf).header("Authorization", bearer(adminToken())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownBatchReturns404() throws Exception {
        String auth = bearer(adminToken());
        mockMvc.perform(get("/api/batches/999999").header("Authorization", auth)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/batches/999999/errors").header("Authorization", auth)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/batches/999999/error-report").header("Authorization", auth)).andExpect(status().isNotFound());
    }
}
