package io.github.siamese_lang.ops;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
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
    private final int tomcatMaxThreads;
    private final int tomcatMinSpareThreads;
    private final int tomcatAcceptCount;
    private final String tomcatConnectionTimeout;

    public FailureLabController(
        DbSettings dbSettings,
        NodeIdentity nodeIdentity,
        @Value("${ops.evidence-files.root:/mnt/ops-sample/files}") String storageRoot,
        @Value("${server.tomcat.threads.max:200}") int tomcatMaxThreads,
        @Value("${server.tomcat.threads.min-spare:10}") int tomcatMinSpareThreads,
        @Value("${server.tomcat.accept-count:100}") int tomcatAcceptCount,
        @Value("${server.tomcat.connection-timeout:20s}") String tomcatConnectionTimeout
    ) {
        this.dbSettings = dbSettings;
        this.nodeIdentity = nodeIdentity;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.tomcatMaxThreads = tomcatMaxThreads;
        this.tomcatMinSpareThreads = tomcatMinSpareThreads;
        this.tomcatAcceptCount = tomcatAcceptCount;
        this.tomcatConnectionTimeout = tomcatConnectionTimeout;
    }

    @GetMapping("/ops/failure-lab")
    public String failureLab(Model model) {
        model.addAttribute("node", nodeIdentity.asMap());
        model.addAttribute("storageRoot", storageRoot.toString());
        model.addAttribute("storage", storageState().asMap());
        model.addAttribute("wasRuntime", wasRuntimeState());
        model.addAttribute("dbPool", dbSettings.poolState());
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
        response.put("wasRuntime", wasRuntimeState());
        return response;
    }

    @ResponseBody
    @GetMapping("/api/failure-lab/was-runtime")
    public Map<String, Object> wasRuntime() {
        long startedAt = System.nanoTime();
        Map<String, Object> response = baseResponse("ok", "was_runtime", startedAt);
        response.put("wasRuntime", wasRuntimeState());
        response.put("dbPool", dbSettings.poolState());
        return response;
    }

    @ResponseBody
    @GetMapping("/api/failure-lab/db-sleep")
    public ResponseEntity<Map<String, Object>> dbSleep(@RequestParam(defaultValue = "1000") int millis) {
        int boundedMillis = boundedMillis(millis);
        return executeDbSleep("db_sleep", millis, boundedMillis);
    }

    @ResponseBody
    @GetMapping("/api/failure-lab/db-hold")
    public ResponseEntity<Map<String, Object>> dbHold(@RequestParam(defaultValue = "10") int seconds) {
        int boundedSeconds = Math.max(0, Math.min(seconds, MAX_SLEEP_MS / 1_000));
        return executeDbSleep("db_connection_hold", seconds, boundedSeconds * 1_000);
    }

    @ResponseBody
    @GetMapping("/api/failure-lab/db-pool")
    public Map<String, Object> dbPool() {
        long startedAt = System.nanoTime();
        Map<String, Object> response = baseResponse("ok", "db_pool", startedAt);
        response.put("db", Map.of("url", dbSettings.maskedUrl()));
        response.put("pool", dbSettings.poolState());
        return response;
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

    private ResponseEntity<Map<String, Object>> executeDbSleep(String scenario, int requestedValue, int boundedMillis) {
        long startedAt = System.nanoTime();
        Map<String, Object> poolBefore = dbSettings.poolState();

        try (Connection connection = dbSettings.openConnection();
             PreparedStatement statement = connection.prepareStatement("select pg_sleep(?)")) {
            statement.setDouble(1, boundedMillis / 1000.0d);
            statement.execute();

            Map<String, Object> response = baseResponse("ok", scenario, startedAt);
            response.put("requestedValue", requestedValue);
            response.put("boundedMillis", boundedMillis);
            response.put("thread", Thread.currentThread().getName());
            response.put("wasRuntime", wasRuntimeState());
            response.put("db", Map.of(
                "url", dbSettings.maskedUrl(),
                "query", "select pg_sleep(?)"
            ));
            response.put("poolBefore", poolBefore);
            response.put("poolAfter", dbSettings.poolState());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | SQLException ex) {
            Map<String, Object> response = baseResponse("db-unavailable", scenario, startedAt);
            response.put("requestedValue", requestedValue);
            response.put("boundedMillis", boundedMillis);
            response.put("message", ex.getMessage());
            response.put("wasRuntime", wasRuntimeState());
            response.put("db", dbSettings.requiredEnvironment());
            response.put("poolBefore", poolBefore);
            response.put("poolAfter", dbSettings.poolState());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    private Map<String, Object> wasRuntimeState() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        Runtime runtime = Runtime.getRuntime();

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("container", "Spring Boot embedded Tomcat");
        settings.put("maxThreads", tomcatMaxThreads);
        settings.put("minSpareThreads", tomcatMinSpareThreads);
        settings.put("acceptCount", tomcatAcceptCount);
        settings.put("connectionTimeout", tomcatConnectionTimeout);
        settings.put("source", Map.of(
            "maxThreads", "SERVER_TOMCAT_THREADS_MAX",
            "minSpareThreads", "SERVER_TOMCAT_THREADS_MIN_SPARE",
            "acceptCount", "SERVER_TOMCAT_ACCEPT_COUNT",
            "connectionTimeout", "SERVER_TOMCAT_CONNECTION_TIMEOUT"
        ));

        Map<String, Object> threads = new LinkedHashMap<>();
        threads.put("currentThread", Thread.currentThread().getName());
        threads.put("liveThreadCount", threadBean.getThreadCount());
        threads.put("daemonThreadCount", threadBean.getDaemonThreadCount());
        threads.put("peakThreadCount", threadBean.getPeakThreadCount());
        threads.put("totalStartedThreadCount", threadBean.getTotalStartedThreadCount());

        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("availableProcessors", runtime.availableProcessors());
        jvm.put("freeMemoryBytes", runtime.freeMemory());
        jvm.put("totalMemoryBytes", runtime.totalMemory());
        jvm.put("maxMemoryBytes", runtime.maxMemory());

        Map<String, Object> runtimeState = new LinkedHashMap<>();
        runtimeState.put("settings", settings);
        runtimeState.put("threads", threads);
        runtimeState.put("jvm", jvm);
        return runtimeState;
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
