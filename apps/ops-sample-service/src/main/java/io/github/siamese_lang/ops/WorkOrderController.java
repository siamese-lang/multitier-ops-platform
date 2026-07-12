package io.github.siamese_lang.ops;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkOrderController {

    private final WorkOrderRepository workOrderRepository;
    private final NodeIdentity nodeIdentity;

    public WorkOrderController(WorkOrderRepository workOrderRepository, NodeIdentity nodeIdentity) {
        this.workOrderRepository = workOrderRepository;
        this.nodeIdentity = nodeIdentity;
    }

    @GetMapping("/api/work-orders")
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) String status) {
        long startedAt = System.nanoTime();
        try {
            Map<String, Object> response = response("ok", "work_orders.list", startedAt);
            response.put("data", workOrderRepository.findAll(status));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage(), "work_orders.list", startedAt);
        } catch (IllegalStateException | SQLException ex) {
            return dbUnavailable(ex, "work_orders.list", startedAt);
        }
    }

    @GetMapping("/api/work-orders/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable long id) {
        long startedAt = System.nanoTime();
        try {
            Map<String, Object> workOrder = workOrderRepository.findById(id);
            if (workOrder == null) {
                return notFound("work order not found: " + id, "work_orders.get", startedAt);
            }
            Map<String, Object> response = response("ok", "work_orders.get", startedAt);
            response.put("data", workOrder);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | SQLException ex) {
            return dbUnavailable(ex, "work_orders.get", startedAt);
        }
    }

    @PostMapping("/api/work-orders")
    public ResponseEntity<Map<String, Object>> create(@RequestBody WorkOrderRepository.CreateWorkOrderRequest request) {
        long startedAt = System.nanoTime();
        try {
            Map<String, Object> response = response("created", "work_orders.create", startedAt);
            response.put("data", workOrderRepository.create(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage(), "work_orders.create", startedAt);
        } catch (IllegalStateException | SQLException ex) {
            return dbUnavailable(ex, "work_orders.create", startedAt);
        }
    }

    @PatchMapping("/api/work-orders/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
        @PathVariable long id,
        @RequestBody WorkOrderRepository.UpdateStatusRequest request
    ) {
        long startedAt = System.nanoTime();
        try {
            Map<String, Object> workOrder = workOrderRepository.updateStatus(id, request);
            if (workOrder == null) {
                return notFound("work order not found: " + id, "work_orders.update_status", startedAt);
            }
            Map<String, Object> response = response("updated", "work_orders.update_status", startedAt);
            response.put("data", workOrder);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage(), "work_orders.update_status", startedAt);
        } catch (IllegalStateException | SQLException ex) {
            return dbUnavailable(ex, "work_orders.update_status", startedAt);
        }
    }

    @GetMapping("/api/work-orders/{id}/events")
    public ResponseEntity<Map<String, Object>> events(@PathVariable long id) {
        long startedAt = System.nanoTime();
        try {
            Map<String, Object> workOrder = workOrderRepository.findById(id);
            if (workOrder == null) {
                return notFound("work order not found: " + id, "work_orders.events", startedAt);
            }
            Map<String, Object> response = response("ok", "work_orders.events", startedAt);
            response.put("workOrder", workOrder);
            response.put("data", workOrderRepository.findEventsByWorkOrderId(id));
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | SQLException ex) {
            return dbUnavailable(ex, "work_orders.events", startedAt);
        }
    }

    @GetMapping("/api/work-orders/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        long startedAt = System.nanoTime();
        try {
            Map<String, Object> response = response("ok", "work_orders.summary", startedAt);
            response.put("data", workOrderRepository.summary());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | SQLException ex) {
            return dbUnavailable(ex, "work_orders.summary", startedAt);
        }
    }

    @GetMapping("/api/audit-logs")
    public ResponseEntity<Map<String, Object>> auditLogs(@RequestParam(defaultValue = "20") int limit) {
        long startedAt = System.nanoTime();
        try {
            Map<String, Object> response = response("ok", "audit_logs.list", startedAt);
            response.put("data", workOrderRepository.findRecentAuditLogs(limit));
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | SQLException ex) {
            return dbUnavailable(ex, "audit_logs.list", startedAt);
        }
    }

    private Map<String, Object> response(String status, String operation, long startedAt) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "ops-sample-service");
        response.put("status", status);
        response.put("operation", operation);
        response.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        response.put("node", nodeIdentity.asMap());
        response.put("durationMs", durationMs(startedAt));
        return response;
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message, String operation, long startedAt) {
        Map<String, Object> response = response("bad-request", operation, startedAt);
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }

    private ResponseEntity<Map<String, Object>> notFound(String message, String operation, long startedAt) {
        Map<String, Object> response = response("not-found", operation, startedAt);
        response.put("message", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    private ResponseEntity<Map<String, Object>> dbUnavailable(Exception ex, String operation, long startedAt) {
        Map<String, Object> response = response("db-unavailable", operation, startedAt);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    private static long durationMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
