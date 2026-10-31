package com.itsm.web;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import com.itsm.intelligence.CorrelationEngine;
import com.itsm.intelligence.SlaRisk;
import com.itsm.intelligence.SlaRiskPredictor;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.SlaViolationRepository;
import com.itsm.security.CurrentUserService;
import com.itsm.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Each role lands on its own home page, with a different layout and colour theme. */
@Controller
public class HomeController {

    private final CurrentUserService currentUser;
    private final IncidentService incidentService;
    private final IncidentRepository incidents;
    private final SlaRiskPredictor riskPredictor;
    private final AssignmentService assignmentService;
    private final ReportService reports;
    private final CorrelationEngine correlation;
    private final SlaViolationRepository violations;
    private final UserService userService;

    public HomeController(CurrentUserService currentUser, IncidentService incidentService, IncidentRepository incidents,
                          SlaRiskPredictor riskPredictor, AssignmentService assignmentService, ReportService reports,
                          CorrelationEngine correlation, SlaViolationRepository violations, UserService userService) {
        this.currentUser = currentUser;
        this.incidentService = incidentService;
        this.incidents = incidents;
        this.riskPredictor = riskPredictor;
        this.assignmentService = assignmentService;
        this.reports = reports;
        this.correlation = correlation;
        this.violations = violations;
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Model model) {
        User me = currentUser.require();
        return switch (me.getRole()) {
            case EMPLOYEE -> employeeHome(me, model);
            case TECHNICIAN -> technicianHome((Technician) me, model);
            case ADMIN -> adminDashboard(model);
        };
    }

    private String employeeHome(User me, Model model) {
        List<Incident> mine = incidentService.reportedOrWatchedBy(me);
        model.addAttribute("incidents", mine);
        model.addAttribute("openCount", mine.stream().filter(Incident::isUnresolved).count());
        model.addAttribute("needsMe", mine.stream()
                .filter(i -> me.isReporterOf(i) && (i.getStatus() == IncidentStatus.AWAITING_INFO || i.getStatus() == IncidentStatus.RESOLVED))
                .toList());
        model.addAttribute("resolvedCount", mine.stream().filter(i -> !i.isUnresolved()).count());
        model.addAttribute("serviceHealth", correlation.serviceHealth());
        return "employee/home";
    }

    private String technicianHome(Technician me, Model model) {
        List<Incident> queue = incidentService.queueOf(me);
        Map<Long, SlaRisk> risk = riskPredictor.assess(queue);
        List<Incident> sorted = queue.stream()
                .sorted(Comparator.comparingInt((Incident i) -> risk.containsKey(i.getId()) ? -risk.get(i.getId()).score() : 0))
                .toList();
        model.addAttribute("queue", sorted);
        model.addAttribute("risk", risk);
        model.addAttribute("highRiskCount", risk.values().stream().filter(r -> r.level() == SlaRisk.Level.HIGH).count());
        model.addAttribute("technician", me);
        model.addAttribute("workload", assignmentService.workloadOf(me));
        model.addAttribute("resolvedByMe", incidents.findByAssigneeOrderByUpdatedAtDesc(me).stream()
                .filter(i -> !i.isUnresolved()).limit(8).toList());
        if (me.isTeamLead()) {
            List<Incident> unassigned = incidents.findByStatusIn(List.of(IncidentStatus.NEW, IncidentStatus.CLASSIFIED));
            model.addAttribute("unassigned", unassigned);
        }
        return "tech/home";
    }

    private String adminDashboard(Model model) {
        List<Incident> open = incidents.findByStatusIn(IncidentStatus.UNRESOLVED);
        Map<Long, SlaRisk> risk = riskPredictor.assess(open);
        model.addAttribute("overview", reports.overview());
        model.addAttribute("risk", risk);
        model.addAttribute("atRisk", open.stream()
                .filter(i -> risk.containsKey(i.getId()) && risk.get(i.getId()).level() != SlaRisk.Level.LOW)
                .sorted(Comparator.comparingInt((Incident i) -> -risk.get(i.getId()).score()))
                .limit(8).toList());
        model.addAttribute("unassigned", open.stream().filter(i -> i.getAssignee() == null).toList());
        model.addAttribute("serviceHealth", correlation.serviceHealth());
        model.addAttribute("workload", reports.technicianWorkload());
        model.addAttribute("recentViolations", violations.findAllByOrderByDetectedAtDesc().stream().limit(6).toList());
        model.addAttribute("strategy", assignmentService.activeStrategy());
        return "admin/dashboard";
    }

    @PostMapping("/tech/availability")
    public String toggleAvailability(@RequestParam boolean available, RedirectAttributes redirect) {
        User me = currentUser.require();
        if (me instanceof Technician t) {
            userService.setAvailability(t, available);
            redirect.addFlashAttribute("success", available ? "You are now available for new incidents" : "You will not receive new incidents");
        }
        return "redirect:/";
    }
}
