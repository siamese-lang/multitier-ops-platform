package io.github.siamese_lang.ops;

import java.sql.SQLException;
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

    public WorkOrderController(WorkOrderRepository workOrderRepository) {
        this.workOrderRepository = workOrderRepository;
    }

    @GetMapping("/api/work-orders")
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) String status) {
        try {
            Map<String, Object> response = response("ok");
            response.put("data", workOrderRepository.findAll(status));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (IllegalStateException | SQLException ex) {
            return dbUnavailable(ex);
        }
    }

    @GetMapping("/api/work-orders/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable long id) {
        try {
            Map<String, Object> workOrder = workOrderRepository.findById(id);
            if (workOrder == null) {
                return notFound("work order not found: " + id);
            }
            Map<String, Object> response = response("ok");
            response.put("data", workOrder);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | SQLException ex) {
            return dbUnavailable(ex);
        }
    }

    @PostMapping("/api/work-orders")
    public ResponseEntity<Map<String, Object>> create(@RequestBody WorkOrderRepository.CreateWorkOrderRequest request) {
        try {
            Map<String, Object> response = response("created");
            response.put("data", workOrderRepository.create(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (IllegalStateException | SQLException ex) {
            return dbUnavailable(ex);
        }
    }

    @PatchMapping("/api/work-orders/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
        @PathVariable long id,
        @RequestBody WorkOrderRepository.UpdateStatusRequest request
    ) {
        try {
            Map<String, Object> workOrder = workOrderRepository.updateStatus(id, request);
            if (workOrder == null) {
                return notFound("work order not found: " + id);
            }
            Map<String, Object> response = response("updated");
            response.put("data", workOrder);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (IllegalStateException | SQLException ex) {
            return dbUnavailable(ex);
        }
    }

    @GetMapping("/api/work-orders/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        try {
            Map<String, Object> response = response("ok");
            response.put("data", workOrderRepository.summary());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | SQLException ex) {
            return dbUnavailable(ex);
        }
    }

    private static Map<String, Object> response(String status) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "ops-sample-service");
        response.put("status", status);
        return response;
    }

    private static ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> response = response("bad-request");
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }

    private static ResponseEntity<Map<String, Object>> notFound(String message) {
        Map<String, Object> response = response("not-found");
        response.put("message", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    private static ResponseEntity<Map<String, Object>> dbUnavailable(Exception ex) {
        Map<String, Object> response = response("db-unavailable");
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
