package com.kisansetu.upload;

import com.kisansetu.auth.CurrentUser;
import com.kisansetu.domain.ClaimBatch;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final BatchService service;
    private final ErrorReportWriter reportWriter;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchSummary> upload(@RequestParam("file") MultipartFile file,
                                               @RequestParam(name = "dryRun", defaultValue = "false") boolean dryRun,
                                               @AuthenticationPrincipal Jwt jwt) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidUploadException("No file was uploaded");
        }
        try (InputStream in = file.getInputStream()) {
            ClaimBatch batch = service.processUpload(file.getOriginalFilename(), in, dryRun, CurrentUser.id(jwt));
            return ResponseEntity.created(URI.create("/api/batches/" + batch.getId())).body(BatchSummary.from(batch));
        }
    }

    @GetMapping("/{id}")
    public BatchSummary get(@PathVariable Long id) {
        return BatchSummary.from(service.get(id));
    }

    @GetMapping("/{id}/errors")
    public List<ErrorItem> errors(@PathVariable Long id) {
        return service.errorsOf(id).stream().map(ErrorItem::from).toList();
    }

    @GetMapping("/{id}/error-report")
    public ResponseEntity<byte[]> errorReport(@PathVariable Long id) throws IOException {
        byte[] body = reportWriter.write(service.errorsOf(id));
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("batch-" + id + "-errors.xlsx").build().toString())
                .body(body);
    }
}
