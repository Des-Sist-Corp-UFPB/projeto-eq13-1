package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.domain.JobLocationType;
import br.ufpb.dsc.jobhub.domain.JobStatus;
import br.ufpb.dsc.jobhub.domain.ContractType;
import br.ufpb.dsc.jobhub.domain.Seniority;
import br.ufpb.dsc.jobhub.dto.JobPostForm;
import br.ufpb.dsc.jobhub.service.AuditLogService;
import br.ufpb.dsc.jobhub.service.DashboardService;
import br.ufpb.dsc.jobhub.service.JobService;
import br.ufpb.dsc.jobhub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DashboardService dashboardService;
    private final JobService jobService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public AdminController(DashboardService dashboardService, JobService jobService, UserService userService, AuditLogService auditLogService) {
        this.dashboardService = dashboardService;
        this.jobService = jobService;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:/login";
    }

    @ModelAttribute("jobStatuses")
    JobStatus[] jobStatuses() {
        return JobStatus.values();
    }

    @ModelAttribute("locationTypes")
    JobLocationType[] locationTypes() {
        return JobLocationType.values();
    }

    @ModelAttribute("seniorities")
    Seniority[] seniorities() {
        return Seniority.values();
    }

    @ModelAttribute("contractTypes")
    ContractType[] contractTypes() {
        return ContractType.values();
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboardService.stats());
        model.addAttribute("jobs", jobService.allJobs().stream().limit(6).toList());
        model.addAttribute("applications", jobService.allApplications().stream().limit(6).toList());
        return "admin/dashboard";
    }

    @GetMapping("/vagas")
    public String jobs(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String status,
                       @RequestParam(defaultValue = "") String location,
                       Model model) {
        model.addAttribute("jobs", jobService.searchAdmin(q, status, location));
        model.addAttribute("q", q);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedLocation", location);
        return "admin/jobs";
    }

    @GetMapping("/vagas/nova")
    public String newJob(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", JobPostForm.empty());
        }
        return "admin/job-form";
    }

    @PostMapping("/vagas/nova")
    public String createJob(@Valid @ModelAttribute("form") JobPostForm form,
                            BindingResult bindingResult,
                            Authentication authentication,
                            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return "admin/job-form";
        }
        try {
            var job = jobService.createByAdmin(form);
            auditLogService.log(request, authentication, "ADMIN_JOB_CREATED", "JOB_POSTING", job.getId(), "Vaga criada pelo administrador.");
            auditLogService.log(request, authentication, "JOB_PUBLISHED", "JOB_POSTING", job.getId(), "Vaga criada diretamente como publicada.");
            return "redirect:/admin/vagas";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("job.location", ex.getMessage());
            return "admin/job-form";
        }
    }

    @GetMapping("/candidaturas")
    public String applications(Model model) {
        model.addAttribute("applications", jobService.allApplications());
        return "admin/applications";
    }

    @GetMapping("/auditoria")
    public String audit(@RequestParam(defaultValue = "") String action,
                        @RequestParam(defaultValue = "") String actor,
                        @RequestParam(defaultValue = "") String entityType,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        Model model) {
        model.addAttribute("logs", auditLogService.search(action, actor, entityType, from, to));
        model.addAttribute("action", action);
        model.addAttribute("actor", actor);
        model.addAttribute("entityType", entityType);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "admin/audit";
    }

    @GetMapping("/audit")
    public String auditAlias() {
        return "redirect:/admin/auditoria";
    }

    @GetMapping("/usuarios")
    public String users(Model model) {
        model.addAttribute("users", userService.allUsers());
        return "admin/users";
    }

    @PostMapping("/vagas/{id}/publicar")
    public String publish(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        var job = jobService.publish(id);
        auditLogService.log(request, authentication, "JOB_PUBLISHED", "JOB_POSTING", job.getId(), "Vaga publicada.");
        return "redirect:/admin/vagas";
    }

    @PostMapping("/vagas/{id}/pendente")
    public String pending(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        var job = jobService.sendToPending(id);
        auditLogService.log(request, authentication, "JOB_SENT_TO_PENDING", "JOB_POSTING", job.getId(), "Vaga alterada para pendente.");
        return "redirect:/admin/vagas";
    }

    @PostMapping("/vagas/{id}/arquivar")
    public String archive(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        var job = jobService.archive(id);
        auditLogService.log(request, authentication, "JOB_ARCHIVED", "JOB_POSTING", job.getId(), "Vaga arquivada.");
        return "redirect:/admin/vagas";
    }

    @PostMapping("/vagas/{id}/remover")
    public String delete(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        var job = jobService.delete(id);
        auditLogService.log(request, authentication, "JOB_DELETED", "JOB_POSTING", job.getId(), "Vaga removida: " + job.getTitle());
        return "redirect:/admin/vagas";
    }
}
