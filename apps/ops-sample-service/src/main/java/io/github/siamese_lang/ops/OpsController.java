package io.github.siamese_lang.ops;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpsController {

    private final DbProbe dbProbe;
    private final NodeIdentity nodeIdentity;

    public OpsController(DbProbe dbProbe, NodeIdentity nodeIdentity) {
        this.dbProbe = dbProbe;
        this.nodeIdentity = nodeIdentity;
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

    @GetMapping("/node")
    public Map<String, Object> node() {
        Map<String, Object> response = baseResponse("up");
        response.put("node", nodeIdentity.asMap());
        return response;
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

    private Map<String, Object> baseResponse(String status) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "ops-sample-service");
        response.put("status", status);
        response.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        response.put("version", nodeIdentity.version());
        response.put("node", nodeIdentity.asMap());
        return response;
    }
}
