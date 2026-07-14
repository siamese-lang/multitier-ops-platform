package io.github.siamese_lang.ops;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpsController {

    private final DbProbe dbProbe;
    private final DbSettings dbSettings;
    private final NodeIdentity nodeIdentity;
    private final ReleaseInfo releaseInfo;
    private final Path storageRoot;

    public OpsController(
        DbProbe dbProbe,
        DbSettings dbSettings,
        NodeIdentity nodeIdentity,
        ReleaseInfo releaseInfo,
        @Value("${ops.evidence-files.root:/mnt/ops-sample/files}") String storageRoot
    ) {
        this.dbProbe = dbProbe;
        this.dbSettings = dbSettings;
        this.nodeIdentity = nodeIdentity;
        this.releaseInfo = releaseInfo;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @GetMapping("/healthz")
    public Map<String, Object> healthz() {
        Map<String, Object> response = baseResponse("up");
        response.put("check", "process");
        return response;
    }

    @GetMapping("/readyz")
    public ResponseEntity<Map<String, Object>> readyz() {
        DbProbe.DbCheckResult result = dbProbe.check();
        Map<String, Object> response = baseResponse(result.ok() ? "ready" : "not-ready");
        response.put("check", "database");
        response.put("db", Map.of(
            "ok", result.ok(),
            "message", result.message(),
            "details", result.details()
        ));
        return ResponseEntity.status(result.ok() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/api/ops/dependencies")
    public ResponseEntity<Map<String, Object>> dependencies() {
        long startedAt = System.nanoTime();

        long dbStartedAt = System.nanoTime();
        DbProbe.DbCheckResult dbResult = dbProbe.check();
        long dbLatencyMs = durationMs(dbStartedAt);

        StorageState storageState = storageState();
        boolean overallReady = dbResult.ok() && storageState.ready();

        Map<String, Object> response = baseResponse(overallReady ? "ready" : "degraded");
        response.put("check", "dependencies");
        response.put("durationMs", durationMs(startedAt));
        response.put("summary", Map.of(
            "ready", overallReady,
            "app", "UP",
            "db", dbResult.ok() ? "UP" : "DOWN",
            "dbPool", dbResult.ok() ? "UP" : "DEGRADED",
            "storage", storageState.ready() ? "UP" : "DOWN"
        ));

        Map<String, Object> dependencies = new LinkedHashMap<>();
        dependencies.put("app", appDependency());
        dependencies.put("db", dbDependency(dbResult, dbLatencyMs));
        dependencies.put("dbPool", dbPoolDependency(dbResult));
        dependencies.put("storage", storageDependency(storageState));
        response.put("dependencies", dependencies);

        return ResponseEntity.status(overallReady ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/node")
    public Map<String, Object> node() {
        Map<String, Object> response = baseResponse("up");
        response.put("node", nodeIdentity.asMap());
        return response;
    }

    @GetMapping("/version")
    public Map<String, Object> version() {
        return releaseInfo.asMap();
    }

    @GetMapping("/db/time")
    public ResponseEntity<Map<String, Object>> dbTime() {
        DbProbe.DbCheckResult result = dbProbe.check();
        Map<String, Object> response = baseResponse(result.ok() ? "up" : "db-error");
        response.put("db", Map.of(
            "ok", result.ok(),
            "message", result.message(),
            "details", result.details()
        ));
        return ResponseEntity.status(result.ok() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    private Map<String, Object> appDependency() {
        Map<String, Object> app = new LinkedHashMap<>();
        app.put("status", "UP");
        app.put("service", "ops-sample-service");
        app.put("version", nodeIdentity.version());
        app.put("node", nodeIdentity.asMap());
        app.put("release", releaseInfo.asMap());
        return app;
    }

    private Map<String, Object> dbDependency(DbProbe.DbCheckResult result, long latencyMs) {
        Map<String, Object> db = new LinkedHashMap<>();
        db.put("status", result.ok() ? "UP" : "DOWN");
        db.put("ok", result.ok());
        db.put("latencyMs", latencyMs);
        db.put("message", result.message());
        db.put("details", result.details());
        return db;
    }

    private Map<String, Object> dbPoolDependency(DbProbe.DbCheckResult dbResult) {
        Map<String, Object> pool = new LinkedHashMap<>(dbSettings.poolState());
        pool.put("status", dbResult.ok() ? "UP" : "DEGRADED");
        pool.put("interpretation", dbResult.ok()
            ? "DB-backed dependency check succeeded through the configured HikariCP pool."
            : "Pool settings are visible, but DB-backed dependency check is not healthy.");
        return pool;
    }

    private Map<String, Object> storageDependency(StorageState state) {
        Map<String, Object> storage = new LinkedHashMap<>();
        storage.put("status", state.ready() ? "UP" : "DOWN");
        storage.put("root", storageRoot.toString());
        storage.put("exists", state.exists());
        storage.put("directory", state.directory());
        storage.put("readable", state.readable());
        storage.put("writable", state.writable());
        storage.put("ready", state.ready());
        return storage;
    }

    private Map<String, Object> baseResponse(String status) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "ops-sample-service");
        response.put("status", status);
        response.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        response.put("version", nodeIdentity.version());
        response.put("node", nodeIdentity.asMap());
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

    private static long durationMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private record StorageState(boolean exists, boolean directory, boolean readable, boolean writable) {
        boolean ready() {
            return exists && directory && readable && writable;
        }
    }
}
