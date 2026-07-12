package io.github.siamese_lang.ops;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FailureLabController {

    private static final int DEFAULT_SLEEP_MS = 1_000;
    private static final int MAX_SLEEP_MS = 30_000;

    private final DbSettings dbSettings;
    private final NodeIdentity nodeIdentity;
    private final Path storageRoot;

    public FailureLabController(
        DbSettings dbSettings,
        NodeIdentity nodeIdentity,
        @Value("${ops.evidence-files.root:/mnt/ops-sample/files}") String storageRoot
    ) {
        this.dbSettings = dbSettings;
        this.nodeIdentity = nodeIdentity;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @GetMapping("/ops/failure-lab")
    public String failureLab(Model model) {
        model.addAttribute("node", nodeIdentity.asMap());
        model.addAttribute("storageRoot", storageRoot.toString());
        model.addAttribute("storage", storageState().asMap());
        model.addAttribute("defaultSleepMs", DEFAULT_SLEEP_MS);
        model.addAttribute("maxSleepMs", MAX_SLEEP_MS);
        return "failure-lab";
    }

    @ResponseBody
    @GetMapping("/api/failure-lab/sleep")
    public Map<String, Object> sleep(@RequestParam(defaultValue = "1000") int millis) {
        int boundedMillis = boundedMillis(millis);
        long startedAt = System.nanoTime();
        boolean interrupted = false;

        try {
            Thread.sleep(boundedMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            interrupted = true;
        }

        Map<String, Object> response = baseResponse("ok", "was_sleep", startedAt);
        response.put("requestedMillis", millis);
        response.put("boundedMillis", boundedMillis);
        response.put("interrupted", interrupted);
        response.put("thread", Thread.currentThread().getName());
        return response;
    }

    @ResponseBody
    @GetMapping("/api/failure-lab/db-sleep")
    public ResponseEntity<Map<String, Object>> dbSleep(@RequestParam(defaultValue = "1000") int millis) {
        int boundedMillis = boundedMillis(millis);
        long startedAt = System.nanoTime();

        try (Connection connection = dbSettings.openConnection();
             PreparedStatement statement = connection.prepareStatement("select pg_sleep(?)")) {
            statement.setDouble(1, boundedMillis / 1000.0d);
            statement.execute();

            Map<String, Object> response = baseResponse("ok", "db_sleep", startedAt);
            response.put("requestedMillis", millis);
            response.put("boundedMillis", boundedMillis);
            response.put("db", Map.of(
                "url", dbSettings.maskedUrl(),
                "query", "select pg_sleep(?)"
            ));
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | SQLException ex) {
            Map<String, Object> response = baseResponse("db-unavailable", "db_sleep", startedAt);
            response.put("requestedMillis", millis);
            response.put("boundedMillis", boundedMillis);
            response.put("message", ex.getMessage());
            response.put("db", dbSettings.requiredEnvironment());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    @ResponseBody
    @GetMapping("/api/failure-lab/file-storage-check")
    public Map<String, Object> fileStorageCheck() {
        long startedAt = System.nanoTime();
        Map<String, Object> response = baseResponse("ok", "file_storage_check", startedAt);
        response.put("storageRoot", storageRoot.toString());
        response.put("storage", storageState().asMap());
        return response;
    }

    @ResponseBody
    @GetMapping("/api/failure-lab/upload-limits")
    public Map<String, Object> uploadLimits(
        @Value("${spring.servlet.multipart.max-file-size:10MB}") String maxFileSize,
        @Value("${spring.servlet.multipart.max-request-size:10MB}") String maxRequestSize
    ) {
        long startedAt = System.nanoTime();
        Map<String, Object> response = baseResponse("ok", "upload_limits", startedAt);
        response.put("maxFileSize", maxFileSize);
        response.put("maxRequestSize", maxRequestSize);
        response.put("relatedFlow", "POST /work-orders/{id}/evidence");
        return response;
    }

    private Map<String, Object> baseResponse(String status, String scenario, long startedAt) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "ops-sample-service");
        response.put("status", status);
        response.put("scenario", scenario);
        response.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        response.put("node", nodeIdentity.asMap());
        response.put("durationMs", durationMs(startedAt));
        return response;
    }

    private StorageState storageState() {
        return new StorageState(
            Files.exists(storageRoot),
            Files.isDirectory(storageRoot),
            Files.isReadable(storageRoot),
            Files.isWritable(storageRoot)
        );
    }

    private static int boundedMillis(int millis) {
        if (millis < 0) {
            return 0;
        }
        return Math.min(millis, MAX_SLEEP_MS);
    }

    private static long durationMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private record StorageState(boolean exists, boolean directory, boolean readable, boolean writable) {
        boolean ready() {
            return exists && directory && readable && writable;
        }

        Map<String, Object> asMap() {
            return Map.of(
                "exists", exists,
                "directory", directory,
                "readable", readable,
                "writable", writable,
                "ready", ready()
            );
        }
    }
}
