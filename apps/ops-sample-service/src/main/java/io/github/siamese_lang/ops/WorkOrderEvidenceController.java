package io.github.siamese_lang.ops;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkOrderEvidenceController {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderEvidenceRepository evidenceRepository;
    private final NodeIdentity nodeIdentity;
    private final Path storageRoot;

    public WorkOrderEvidenceController(
        WorkOrderRepository workOrderRepository,
        WorkOrderEvidenceRepository evidenceRepository,
        NodeIdentity nodeIdentity,
        @Value("${ops.evidence-files.root:/mnt/ops-sample/files}") String storageRoot
    ) {
        this.workOrderRepository = workOrderRepository;
        this.evidenceRepository = evidenceRepository;
        this.nodeIdentity = nodeIdentity;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @PostMapping("/api/work-orders/{workOrderId}/evidence-files")
    public ResponseEntity<Map<String, Object>> createEvidenceFile(@PathVariable long workOrderId) {
        long startedAt = System.nanoTime();
        Path createdFile = null;

        try {
            Map<String, Object> workOrder = workOrderRepository.findById(workOrderId);
            if (workOrder == null) {
                return notFound("work order not found: " + workOrderId, "work_order_evidence.create", startedAt);
            }

            DirectoryState state = directoryState();
            if (!state.ready()) {
                Map<String, Object> response = response("storage-not-ready", "work_order_evidence.create", startedAt);
                response.put("storageRoot", storageRoot.toString());
                response.put("directory", state.asMap());
                response.put("message", "evidence file root is not a writable directory");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }

            String evidenceToken = UUID.randomUUID().toString();
            String fileName = "evidence-" + evidenceToken + ".txt";
            Path workOrderDirectory = safeResolve("work-order-" + workOrderId);
            Files.createDirectories(workOrderDirectory);
            createdFile = workOrderDirectory.resolve(fileName).normalize();
            assertInsideStorageRoot(createdFile);

            String payload = evidencePayload(workOrderId, evidenceToken);
            Files.writeString(
                createdFile,
                payload,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
            );

            long sizeBytes = Files.size(createdFile);
            String sha256 = sha256Hex(createdFile);
            String storagePath = toStorageRelativePath(createdFile);

            Map<String, Object> metadata = evidenceRepository.createMetadata(
                new WorkOrderEvidenceRepository.CreateEvidenceMetadataRequest(
                    workOrderId,
                    fileName,
                    storagePath,
                    sizeBytes,
                    sha256,
                    nodeIdentity.hostname()
                )
            );

            Map<String, Object> response = response("created", "work_order_evidence.create", startedAt);
            response.put("workOrder", workOrder);
            response.put("data", metadata);
            response.put("storageRoot", storageRoot.toString());
            response.put("consistency", consistency(metadata));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage(), "work_order_evidence.create", startedAt);
        } catch (IllegalStateException | SQLException ex) {
            deleteCreatedFile(createdFile);
            return dbUnavailable(ex, "work_order_evidence.create", startedAt);
        } catch (IOException ex) {
            deleteCreatedFile(createdFile);
            return storageUnavailable(ex, "work_order_evidence.create", startedAt);
        }
    }

    @GetMapping("/api/work-orders/{workOrderId}/evidence-files")
    public ResponseEntity<Map<String, Object>> listEvidenceFiles(@PathVariable long workOrderId) {
        long startedAt = System.nanoTime();
        try {
            Map<String, Object> workOrder = workOrderRepository.findById(workOrderId);
            if (workOrder == null) {
                return notFound("work order not found: " + workOrderId, "work_order_evidence.list", startedAt);
            }

            Map<String, Object> response = response("ok", "work_order_evidence.list", startedAt);
            response.put("workOrder", workOrder);
            response.put("data", evidenceRepository.findByWorkOrderId(workOrderId));
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | SQLException ex) {
            return dbUnavailable(ex, "work_order_evidence.list", startedAt);
        }
    }

    @GetMapping("/api/work-orders/{workOrderId}/evidence-files/{evidenceId}/consistency")
    public ResponseEntity<Map<String, Object>> checkConsistency(
        @PathVariable long workOrderId,
        @PathVariable long evidenceId
    ) {
        long startedAt = System.nanoTime();
        try {
            Map<String, Object> workOrder = workOrderRepository.findById(workOrderId);
            if (workOrder == null) {
                return notFound("work order not found: " + workOrderId, "work_order_evidence.consistency", startedAt);
            }

            Map<String, Object> metadata = evidenceRepository.findByWorkOrderIdAndEvidenceId(workOrderId, evidenceId);
            if (metadata == null) {
                return notFound("evidence file not found: " + evidenceId, "work_order_evidence.consistency", startedAt);
            }

            Map<String, Object> consistency = consistency(metadata);
            Map<String, Object> response = response(
                Boolean.TRUE.equals(consistency.get("consistent")) ? "consistent" : "inconsistent",
                "work_order_evidence.consistency",
                startedAt
            );
            response.put("workOrder", workOrder);
            response.put("data", metadata);
            response.put("storageRoot", storageRoot.toString());
            response.put("consistency", consistency);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | SQLException ex) {
            return dbUnavailable(ex, "work_order_evidence.consistency", startedAt);
        } catch (IOException ex) {
            return storageUnavailable(ex, "work_order_evidence.consistency", startedAt);
        }
    }

    private DirectoryState directoryState() {
        return new DirectoryState(
            Files.exists(storageRoot),
            Files.isDirectory(storageRoot),
            Files.isReadable(storageRoot),
            Files.isWritable(storageRoot)
        );
    }

    private Map<String, Object> consistency(Map<String, Object> metadata) throws IOException {
        Path file = resolveStoredPath((String) metadata.get("storagePath"));
        boolean exists = Files.exists(file);
        boolean regularFile = Files.isRegularFile(file);
        boolean readable = Files.isReadable(file);
        Long expectedSize = (Long) metadata.get("sizeBytes");
        String expectedSha256 = (String) metadata.get("sha256");

        Long actualSize = null;
        String actualSha256 = null;
        if (exists && regularFile && readable) {
            actualSize = Files.size(file);
            actualSha256 = sha256Hex(file);
        }

        boolean sizeMatches = expectedSize != null && expectedSize.equals(actualSize);
        boolean checksumMatches = expectedSha256 != null && expectedSha256.equals(actualSha256);
        boolean consistent = exists && regularFile && readable && sizeMatches && checksumMatches;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("consistent", consistent);
        response.put("fileExists", exists);
        response.put("regularFile", regularFile);
        response.put("readable", readable);
        response.put("expectedSizeBytes", expectedSize);
        response.put("actualSizeBytes", actualSize);
        response.put("sizeMatches", sizeMatches);
        response.put("expectedSha256", expectedSha256);
        response.put("actualSha256", actualSha256);
        response.put("checksumMatches", checksumMatches);
        response.put("resolvedPath", file.toString());
        return response;
    }

    private Path safeResolve(String relativePath) throws IOException {
        Path resolved = storageRoot.resolve(relativePath).normalize();
        assertInsideStorageRoot(resolved);
        return resolved;
    }

    private Path resolveStoredPath(String storagePath) throws IOException {
        if (storagePath == null || storagePath.isBlank()) {
            throw new IOException("storage path is missing in metadata");
        }
        return safeResolve(storagePath);
    }

    private void assertInsideStorageRoot(Path path) throws IOException {
        if (!path.normalize().startsWith(storageRoot)) {
            throw new IOException("resolved evidence path escapes storage root");
        }
    }

    private String toStorageRelativePath(Path file) {
        return storageRoot.relativize(file).toString().replace('\\', '/');
    }

    private String evidencePayload(long workOrderId, String evidenceToken) {
        return "service=ops-sample-service\n" +
            "operation=work_order_evidence.create\n" +
            "workOrderId=" + workOrderId + "\n" +
            "evidenceToken=" + evidenceToken + "\n" +
            "createdAt=" + OffsetDateTime.now(ZoneOffset.UTC) + "\n" +
            "node=" + nodeIdentity.hostname() + "\n" +
            "environment=" + nodeIdentity.environment() + "\n";
    }

    private static String sha256Hex(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(file));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private static void deleteCreatedFile(Path createdFile) {
        if (createdFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(createdFile);
        } catch (IOException ignored) {
            // Keep the original failure signal. Orphan-file cleanup is covered by consistency evidence.
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

    private ResponseEntity<Map<String, Object>> storageUnavailable(Exception ex, String operation, long startedAt) {
        Map<String, Object> response = response("storage-unavailable", operation, startedAt);
        response.put("storageRoot", storageRoot.toString());
        response.put("directory", directoryState().asMap());
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    private static long durationMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private record DirectoryState(boolean exists, boolean directory, boolean readable, boolean writable) {
        boolean ready() {
            return exists && directory && readable && writable;
        }

        Map<String, Object> asMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("exists", exists);
            map.put("directory", directory);
            map.put("readable", readable);
            map.put("writable", writable);
            return map;
        }
    }
}
