package io.github.siamese_lang.ops;

import java.net.InetAddress;
import java.net.UnknownHostException;
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

    public OpsController(DbProbe dbProbe) {
        this.dbProbe = dbProbe;
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
        response.put("node", nodeInfo());
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
        response.put("version", env("APP_VERSION", "0.1.0-local"));
        return response;
    }

    private Map<String, Object> nodeInfo() {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("hostname", hostname());
        node.put("localAddress", localAddress());
        node.put("role", env("OPS_NODE_ROLE", "app"));
        node.put("tier", env("OPS_NODE_TIER", "private-was"));
        node.put("environment", env("OPS_ENVIRONMENT", "lab-full-min"));
        return node;
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "unknown";
        }
    }

    private static String localAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            return "unknown";
        }
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
