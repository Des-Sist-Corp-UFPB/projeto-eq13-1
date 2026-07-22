package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.domain.ContractType;
import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.JobLocationType;
import br.ufpb.dsc.jobhub.domain.Seniority;
import br.ufpb.dsc.jobhub.dto.CandidateApplicationForm;
import br.ufpb.dsc.jobhub.dto.JobPostForm;
import br.ufpb.dsc.jobhub.service.AuditLogService;
import br.ufpb.dsc.jobhub.service.JobService;
import br.ufpb.dsc.jobhub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PublicController {

    private final JobService jobService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public PublicController(JobService jobService, UserService userService, AuditLogService auditLogService) {
        this.jobService = jobService;
        this.userService = userService;
        this.auditLogService = auditLogService;
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

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredJobs", jobService.featuredJobs());
        return "home";
    }

    @GetMapping("/vagas")
    public String jobs(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String location,
                       Model model) {
        model.addAttribute("jobs", jobService.searchPublished(q, location));
        model.addAttribute("q", q);
        model.addAttribute("selectedLocation", location);
        return "jobs/list";
    }

    @GetMapping("/vagas/{id}")
    public String jobDetails(@PathVariable Long id,
                             @RequestParam(required = false) String applied,
                             Model model) {
        model.addAttribute("job", jobService.publicDetails(id));
        model.addAttribute("applicationForm", CandidateApplicationForm.empty());
        model.addAttribute("applied", applied != null);
        return "jobs/detail";
    }

    @PostMapping("/vagas/{id}/candidatar")
    public String apply(@PathVariable Long id,
                        @Valid @ModelAttribute("applicationForm") CandidateApplicationForm form,
                        BindingResult bindingResult,
                        Model model,
                        Authentication authentication,
                        HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("job", jobService.publicDetails(id));
            model.addAttribute("applied", false);
            return "jobs/detail";
        }
        AppUser applicantUser = userService.currentUser(authentication).orElse(null);
        var application = jobService.apply(id, form, applicantUser);
        auditLogService.log(request, authentication, "APPLICATION_SUBMITTED", "CANDIDATE_APPLICATION", application.getId(), "Candidatura interna enviada.");
        return "redirect:/vagas/" + id + "?applied=true";
    }

    @GetMapping("/divulgar")
    public String publishJob(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", JobPostForm.empty());
        }
        return "jobs/post";
    }

    @PostMapping("/divulgar")
    public String createJob(@Valid @ModelAttribute("form") JobPostForm form,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes,
                            Authentication authentication,
                            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return "jobs/post";
        }
        try {
            var job = jobService.createPending(form);
            auditLogService.log(request, authentication, "PUBLIC_JOB_SUBMITTED", "JOB_POSTING", job.getId(), "Vaga enviada pela página pública.");
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("job.location", ex.getMessage());
            return "jobs/post";
        }
        redirectAttributes.addFlashAttribute("success", true);
        return "redirect:/divulgar";
    }
}
