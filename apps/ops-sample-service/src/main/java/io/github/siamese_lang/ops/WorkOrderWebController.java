package io.github.siamese_lang.ops;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WorkOrderWebController {

    private static final List<String> STATUSES = List.of("OPEN", "IN_PROGRESS", "DONE", "FAILED", "CANCELLED");
    private static final List<String> PRIORITIES = List.of("LOW", "NORMAL", "HIGH", "CRITICAL");

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderEvidenceRepository evidenceRepository;
    private final NodeIdentity nodeIdentity;

    public WorkOrderWebController(
        WorkOrderRepository workOrderRepository,
        WorkOrderEvidenceRepository evidenceRepository,
        NodeIdentity nodeIdentity
    ) {
        this.workOrderRepository = workOrderRepository;
        this.evidenceRepository = evidenceRepository;
        this.nodeIdentity = nodeIdentity;
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
        try {
            Map<String, Object> workOrder = workOrderRepository.findById(id);
            if (workOrder == null) {
                return error(model, "Work order not found", "work order not found: " + id);
            }
            model.addAttribute("workOrder", workOrder);
            model.addAttribute("events", workOrderRepository.findEventsByWorkOrderId(id));
            model.addAttribute("evidenceFiles", evidenceRepository.findByWorkOrderId(id));
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
}
