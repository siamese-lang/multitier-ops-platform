package io.github.siamese_lang.ops;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WorkOrderWebController {

    private static final List<String> STATUSES = List.of("OPEN", "IN_PROGRESS", "DONE", "FAILED", "CANCELLED");
    private static final List<String> PRIORITIES = List.of("LOW", "NORMAL", "HIGH", "CRITICAL");

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderEvidenceRepository evidenceRepository;
    private final NodeIdentity nodeIdentity;
    private final Path storageRoot;

    public WorkOrderWebController(
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

    @GetMapping("/")
    public String root() {
        return "redirect:/work-orders";
    }

    @GetMapping("/work-orders")
    public String list(@RequestParam(required = false) String status, Model model) {
        addCommonModel(model);
        model.addAttribute("selectedStatus", status == null ? "" : status);
        try {
            model.addAttribute("workOrders", workOrderRepository.findAll(status));
            model.addAttribute("summary", workOrderRepository.summary());
            model.addAttribute("auditLogs", workOrderRepository.findRecentAuditLogs(10));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("workOrders", List.of());
            model.addAttribute("auditLogs", List.of());
            model.addAttribute("error", ex.getMessage());
        } catch (IllegalStateException | SQLException ex) {
            model.addAttribute("workOrders", List.of());
            model.addAttribute("auditLogs", List.of());
            model.addAttribute("error", "DB-backed work-order view is unavailable: " + ex.getMessage());
        }
        return "work-orders";
    }

    @GetMapping("/work-orders/new")
    public String newForm(Model model) {
        addCommonModel(model);
        model.addAttribute("priority", "NORMAL");
        model.addAttribute("requester", "service-desk");
        model.addAttribute("assignee", "ops-admin");
        return "work-order-form";
    }

    @PostMapping("/work-orders")
    public String create(
        @RequestParam String title,
        @RequestParam(defaultValue = "NORMAL") String priority,
        @RequestParam(defaultValue = "service-desk") String requester,
        @RequestParam(required = false) String assignee,
        @RequestParam(required = false) String description,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> created = workOrderRepository.create(
                new WorkOrderRepository.CreateWorkOrderRequest(title, priority, assignee, requester, description)
            );
            redirectAttributes.addFlashAttribute("success", "Work order created.");
            return "redirect:/work-orders/" + created.get("id");
        } catch (IllegalArgumentException ex) {
            addCommonModel(model);
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("title", title);
            model.addAttribute("priority", priority);
            model.addAttribute("requester", requester);
            model.addAttribute("assignee", assignee);
            model.addAttribute("description", description);
            return "work-order-form";
        } catch (IllegalStateException | SQLException ex) {
            return error(model, "Work order create failed", ex.getMessage());
        }
    }

    @GetMapping("/work-orders/{id}")
    public String detail(@PathVariable long id, Model model) {
        addCommonModel(model);
        model.addAttribute("storageRoot", storageRoot.toString());
        try {
            Map<String, Object> workOrder = workOrderRepository.findById(id);
            if (workOrder == null) {
                return error(model, "Work order not found", "work order not found: " + id);
            }
            model.addAttribute("workOrder", workOrder);
            model.addAttribute("events", workOrderRepository.findEventsByWorkOrderId(id));
            model.addAttribute("evidenceFiles", evidenceRepository.findByWorkOrderId(id));
            model.addAttribute("evidenceDirectory", directoryState().asMap());
            return "work-order-detail";
        } catch (IllegalStateException | SQLException ex) {
            return error(model, "Work order detail unavailable", ex.getMessage());
        }
    }

    @PostMapping("/work-orders/{id}/status")
    public String updateStatus(
        @PathVariable long id,
        @RequestParam String status,
        @RequestParam(defaultValue = "ops-admin") String actor,
        @RequestParam(required = false) String message,
        RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> updated = workOrderRepository.updateStatus(
                id,
                new WorkOrderRepository.UpdateStatusRequest(status, message, actor)
            );
            if (updated == null) {
                redirectAttributes.addFlashAttribute("error", "Work order not found: " + id);
                return "redirect:/work-orders";
            }
            redirectAttributes.addFlashAttribute("success", "Status updated to " + updated.get("status") + ".");
            return "redirect:/work-orders/" + id;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/work-orders/" + id;
        } catch (IllegalStateException | SQLException ex) {
            redirectAttributes.addFlashAttribute("error", "Status update failed: " + ex.getMessage());
            return "redirect:/work-orders";
        }
    }

    @PostMapping("/work-orders/{id}/evidence")
    public String uploadEvidence(
        @PathVariable long id,
        @RequestParam("file") MultipartFile file,
        RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> metadata = createUploadedEvidenceFile(id, file);
            redirectAttributes.addFlashAttribute("success", "Evidence file uploaded: " + metadata.get("fileName"));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (IllegalStateException | SQLException ex) {
            redirectAttributes.addFlashAttribute("error", "Evidence upload failed because DB is unavailable: " + ex.getMessage());
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute("error", "Evidence upload failed because file storage is unavailable: " + ex.getMessage());
        }
        return "redirect:/work-orders/" + id;
    }

    @GetMapping("/work-orders/{id}/evidence/{evidenceId}/download")
    public ResponseEntity<Resource> downloadEvidence(
        @PathVariable long id,
        @PathVariable long evidenceId
    ) {
        try {
            Map<String, Object> workOrder = workOrderRepository.findById(id);
            if (workOrder == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "work order not found: " + id);
            }
            Map<String, Object> metadata = evidenceRepository.findByWorkOrderIdAndEvidenceId(id, evidenceId);
            if (metadata == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "evidence file not found: " + evidenceId);
            }

            Path file = resolveStoredPath((String) metadata.get("storagePath"));
            if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "stored evidence file is missing or unreadable");
            }

            String fileName = sanitizeDownloadFileName((String) metadata.get("fileName"));
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(file))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IllegalStateException | SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "DB-backed evidence download failed", ex);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "file storage download failed", ex);
        }
    }

    private Map<String, Object> createUploadedEvidenceFile(long workOrderId, MultipartFile file) throws SQLException, IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("evidence file is required");
        }

        Map<String, Object> workOrder = workOrderRepository.findById(workOrderId);
        if (workOrder == null) {
            throw new IllegalArgumentException("work order not found: " + workOrderId);
        }

        DirectoryState state = directoryState();
        if (!state.ready()) {
            throw new IOException("evidence file root is not a writable directory: " + storageRoot);
        }

        String originalFileName = sanitizeStoredFileName(file.getOriginalFilename());
        String storedFileName = "upload-" + UUID.randomUUID() + "-" + originalFileName;
        Path workOrderDirectory = safeResolve("work-order-" + workOrderId);
        Files.createDirectories(workOrderDirectory);
        Path createdFile = workOrderDirectory.resolve(storedFileName).normalize();
        assertInsideStorageRoot(createdFile);

        try {
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, createdFile);
            }

            long sizeBytes = Files.size(createdFile);
            String sha256 = sha256Hex(createdFile);
            String storagePath = toStorageRelativePath(createdFile);

            return evidenceRepository.createMetadata(
                new WorkOrderEvidenceRepository.CreateEvidenceMetadataRequest(
                    workOrderId,
                    originalFileName,
                    storagePath,
                    sizeBytes,
                    sha256,
                    nodeIdentity.hostname()
                )
            );
        } catch (SQLException | RuntimeException | IOException ex) {
            deleteCreatedFile(createdFile);
            throw ex;
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

    private static String sanitizeStoredFileName(String originalFileName) {
        String normalized = originalFileName == null || originalFileName.isBlank() ? "evidence.bin" : originalFileName.trim();
        normalized = normalized.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash >= 0) {
            normalized = normalized.substring(lastSlash + 1);
        }
        normalized = normalized.replaceAll("[^A-Za-z0-9._-]", "_");
        if (normalized.isBlank() || normalized.equals(".") || normalized.equals("..")) {
            return "evidence.bin";
        }
        return normalized.length() > 120 ? normalized.substring(normalized.length() - 120) : normalized;
    }

    private static String sanitizeDownloadFileName(String fileName) {
        String normalized = sanitizeStoredFileName(fileName);
        return normalized.replace("\"", "_");
    }

    private static String sha256Hex(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hash = digest.digest();
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

    private void addCommonModel(Model model) {
        model.addAttribute("statusOptions", STATUSES);
        model.addAttribute("priorityOptions", PRIORITIES);
        model.addAttribute("node", nodeIdentity.asMap());
    }

    private String error(Model model, String title, String message) {
        addCommonModel(model);
        model.addAttribute("errorTitle", title);
        model.addAttribute("errorMessage", message);
        return "work-order-error";
    }

    private record DirectoryState(boolean exists, boolean directory, boolean readable, boolean writable) {
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
