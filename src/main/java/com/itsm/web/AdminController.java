package com.itsm.web;

import com.itsm.domain.catalog.BusinessCriticality;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.user.*;
import com.itsm.intelligence.CorrelationEngine;
import com.itsm.repository.SlaViolationRepository;
import com.itsm.security.CurrentUserService;
import com.itsm.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/** Administrator pages: users, technicians, services, categories, SLA, settings, logs and reports. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CurrentUserService currentUser;
    private final UserService userService;
    private final CatalogService catalog;
    private final SettingsService settings;
    private final AssignmentService assignmentService;
    private final PriorityService priorityService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final ReportService reports;
    private final SlaViolationRepository violations;
    private final CorrelationEngine correlation;

    public AdminController(CurrentUserService currentUser, UserService userService, CatalogService catalog,
                           SettingsService settings, AssignmentService assignmentService, PriorityService priorityService,
                           AuditService auditService, NotificationService notificationService, ReportService reports,
                           SlaViolationRepository violations, CorrelationEngine correlation) {
        this.currentUser = currentUser;
        this.userService = userService;
        this.catalog = catalog;
        this.settings = settings;
        this.assignmentService = assignmentService;
        this.priorityService = priorityService;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.reports = reports;
        this.violations = violations;
        this.correlation = correlation;
    }

    // ================================================================== users

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.all());
        model.addAttribute("roles", Role.values());
        model.addAttribute("workloads", assignmentService.workloads());
        return "admin/users";
    }

    @PostMapping("/users")
    public String createUser(@RequestParam Role role, @RequestParam String name, @RequestParam String email,
                             @RequestParam String password, @RequestParam(required = false) String detail,
                             @RequestParam(required = false) String phone, RedirectAttributes r) {
        User user = userService.create(role, name, email, password, detail, phone);
        r.addFlashAttribute("success", role.getLabel() + " " + user.getName() + " created.");
        return user instanceof Technician ? "redirect:/admin/users/" + user.getId() : "redirect:/admin/users";
    }

    @GetMapping("/users/{id}")
    public String editUser(@PathVariable Long id, Model model) {
        User user = userService.get(id);
        model.addAttribute("user", user);
        model.addAttribute("categories", catalog.allCategories());
        if (user instanceof Technician t) {
            model.addAttribute("technician", t);
            model.addAttribute("workload", assignmentService.workloadOf(t));
        }
        return "admin/user-edit";
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id, @RequestParam String name, @RequestParam String email,
                             @RequestParam(required = false) String phone, @RequestParam(required = false) String detail,
                             @RequestParam(defaultValue = "false") boolean active, RedirectAttributes r) {
        userService.updateUser(id, name, email, phone, detail, active, currentUser.require());
        r.addFlashAttribute("success", "User updated.");
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/users/{id}/technician")
    public String updateTechnician(@PathVariable Long id, @RequestParam(required = false) Set<Long> skills,
                                   @RequestParam int maxActive, @RequestParam int experienceYears,
                                   @RequestParam(defaultValue = "false") boolean available,
                                   @RequestParam(defaultValue = "false") boolean teamLead, RedirectAttributes r) {
        userService.updateTechnician(id, skills, maxActive, experienceYears, available, teamLead);
        r.addFlashAttribute("success", "Technician profile updated.");
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/users/{id}/password")
    public String resetPassword(@PathVariable Long id, @RequestParam String password, RedirectAttributes r) {
        userService.resetPassword(id, password);
        r.addFlashAttribute("success", "Password reset.");
        return "redirect:/admin/users/" + id;
    }

    // ================================================================== services & categories

    @GetMapping("/services")
    public String services(Model model) {
        model.addAttribute("services", catalog.allServices());
        model.addAttribute("categories", catalog.allCategories());
        model.addAttribute("criticalities", BusinessCriticality.values());
        model.addAttribute("health", correlation.serviceHealth());
        return "admin/services";
    }

    @PostMapping("/services")
    public String createService(@RequestParam String name, @RequestParam(required = false) String description,
                                @RequestParam BusinessCriticality criticality,
                                @RequestParam(required = false) Long defaultCategoryId,
                                @RequestParam(required = false) Set<Long> dependsOn, RedirectAttributes r) {
        ItService s = catalog.createService(name, description, criticality, defaultCategoryId, dependsOn);
        r.addFlashAttribute("success", "Service " + s.getName() + " created.");
        return "redirect:/admin/services";
    }

    @PostMapping("/services/{id}")
    public String updateService(@PathVariable Long id, @RequestParam String name,
                                @RequestParam(required = false) String description,
                                @RequestParam BusinessCriticality criticality,
                                @RequestParam(required = false) Long defaultCategoryId,
                                @RequestParam(required = false) Set<Long> dependsOn,
                                @RequestParam(defaultValue = "false") boolean active, RedirectAttributes r) {
        catalog.updateService(id, name, description, criticality, defaultCategoryId, dependsOn, active);
        r.addFlashAttribute("success", "Service updated.");
        return "redirect:/admin/services";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", catalog.allCategories());
        return "admin/categories";
    }

    @PostMapping("/categories")
    public String createCategory(@RequestParam String code, @RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) String keywords, RedirectAttributes r) {
        catalog.createCategory(code, name, description, keywords);
        r.addFlashAttribute("success", "Category " + name + " created. It is immediately available for classification.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}")
    public String updateCategory(@PathVariable Long id, @RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) String keywords,
                                 @RequestParam(defaultValue = "false") boolean active, RedirectAttributes r) {
        catalog.updateCategory(id, name, description, keywords, active);
        r.addFlashAttribute("success", "Category updated.");
        return "redirect:/admin/categories";
    }

    // ================================================================== SLA & settings

    @GetMapping("/sla")
    public String sla(Model model) {
        model.addAttribute("defaults", catalog.defaultPolicies());
        model.addAttribute("overrides", catalog.serviceOverrides());
        model.addAttribute("services", catalog.allServices());
        model.addAttribute("priorities", Priority.values());
        return "admin/sla";
    }

    @PostMapping("/sla")
    public String saveSla(@RequestParam Priority priority, @RequestParam(required = false) Long serviceId,
                          @RequestParam int responseMinutes, @RequestParam int resolutionMinutes, RedirectAttributes r) {
        catalog.saveSlaPolicy(priority, serviceId, responseMinutes, resolutionMinutes);
        r.addFlashAttribute("success", "SLA policy saved. New incidents use it immediately.");
        return "redirect:/admin/sla";
    }

    @PostMapping("/sla/{id}/delete")
    public String deleteSla(@PathVariable Long id, RedirectAttributes r) {
        catalog.deleteSlaOverride(id);
        r.addFlashAttribute("success", "Service-specific SLA removed; the default applies.");
        return "redirect:/admin/sla";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        Map<String, String> values = new java.util.HashMap<>();
        settings.all().forEach((k, v) -> values.put(k.name(), v));
        model.addAttribute("settings", values);
        model.addAttribute("strategies", assignmentService.availableStrategies());
        model.addAttribute("calculators", priorityService.available());
        model.addAttribute("keys", SettingKey.values());
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam Map<String, String> params, RedirectAttributes r) {
        for (SettingKey key : SettingKey.values()) {
            String value = params.get(key.name());
            boolean isBoolean = "true".equals(key.getDefaultValue()) || "false".equals(key.getDefaultValue());
            if (isBoolean) {
                settings.set(key, String.valueOf("true".equals(value)));
            } else if (value != null) {
                if (key == SettingKey.ASSIGNMENT_STRATEGY && !assignmentService.isKnownStrategy(value)) {
                    continue;
                }
                settings.set(key, value);
            }
        }
        r.addFlashAttribute("success", "Settings saved. The " + assignmentService.activeStrategy().displayName()
                + " strategy is now active.");
        return "redirect:/admin/settings";
    }

    // ================================================================== monitoring

    @GetMapping("/audit")
    public String audit(@RequestParam(required = false) String incident, @RequestParam(required = false) String action,
                        @RequestParam(required = false) String actor,
                        @RequestParam(required = false) LocalDate from,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", auditService.search(incident, action, actor, from, page));
        model.addAttribute("incident", incident);
        model.addAttribute("action", action);
        model.addAttribute("actor", actor);
        model.addAttribute("from", from);
        model.addAttribute("actions", java.util.List.of("CREATED", "CLASSIFIED", "PRIORITY_SET", "PRIORITY_CHANGED",
                "ASSIGNED", "ASSIGNMENT_FAILED", "ACKNOWLEDGE", "START_WORK", "REQUEST_INFO", "PROVIDE_INFO", "RESOLVE",
                "VERIFY", "REOPEN", "CLOSE", "CLOSE_AS_DUPLICATE", "COMMENT", "AFFECTED_USER_ADDED", "SLA_WARNING",
                "SLA_BREACHED", "ESCALATED", "CORRELATED", "MAJOR_INCIDENT_SUSPECTED"));
        return "admin/audit";
    }

    @GetMapping("/violations")
    public String violations(Model model) {
        model.addAttribute("violations", violations.findAllByOrderByDetectedAtDesc());
        model.addAttribute("byPriority", reports.slaByPriority());
        return "admin/violations";
    }

    @GetMapping("/outbox")
    public String outbox(Model model) {
        model.addAttribute("messages", notificationService.outbox());
        return "admin/outbox";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("overview", reports.overview());
        model.addAttribute("workload", reports.technicianWorkload());
        model.addAttribute("services", reports.byService());
        model.addAttribute("slaByPriority", reports.slaByPriority());
        model.addAttribute("byCategory", reports.byCategory());
        model.addAttribute("byStatus", reports.byStatus());
        model.addAttribute("trend", reports.trend());
        return "admin/reports";
    }

    @GetMapping("/dependency-graph")
    public String dependencyGraph(Model model) {
        model.addAttribute("health", correlation.serviceHealth());
        return "admin/graph";
    }
}
