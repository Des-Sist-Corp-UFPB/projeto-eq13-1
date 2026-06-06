package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.domain.ContractType;
import br.ufpb.dsc.jobhub.domain.JobLocationType;
import br.ufpb.dsc.jobhub.domain.Seniority;
import br.ufpb.dsc.jobhub.dto.CandidateApplicationForm;
import br.ufpb.dsc.jobhub.dto.JobPostForm;
import br.ufpb.dsc.jobhub.service.JobService;
import jakarta.validation.Valid;
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

    public PublicController(JobService jobService) {
        this.jobService = jobService;
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
                        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("job", jobService.publicDetails(id));
            model.addAttribute("applied", false);
            return "jobs/detail";
        }
        jobService.apply(id, form);
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
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "jobs/post";
        }
        jobService.createPending(form);
        redirectAttributes.addFlashAttribute("success", true);
        return "redirect:/divulgar";
    }
}
