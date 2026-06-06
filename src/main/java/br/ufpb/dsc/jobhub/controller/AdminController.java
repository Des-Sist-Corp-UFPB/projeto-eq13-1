package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.service.DashboardService;
import br.ufpb.dsc.jobhub.service.JobService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DashboardService dashboardService;
    private final JobService jobService;

    public AdminController(DashboardService dashboardService, JobService jobService) {
        this.dashboardService = dashboardService;
        this.jobService = jobService;
    }

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboardService.stats());
        model.addAttribute("jobs", jobService.allJobs().stream().limit(6).toList());
        model.addAttribute("applications", jobService.allApplications().stream().limit(6).toList());
        return "admin/dashboard";
    }

    @GetMapping("/vagas")
    public String jobs(Model model) {
        model.addAttribute("jobs", jobService.allJobs());
        return "admin/jobs";
    }

    @GetMapping("/candidaturas")
    public String applications(Model model) {
        model.addAttribute("applications", jobService.allApplications());
        return "admin/applications";
    }

    @PostMapping("/vagas/{id}/publicar")
    public String publish(@PathVariable Long id) {
        jobService.publish(id);
        return "redirect:/admin/vagas";
    }

    @PostMapping("/vagas/{id}/pendente")
    public String pending(@PathVariable Long id) {
        jobService.sendToPending(id);
        return "redirect:/admin/vagas";
    }

    @PostMapping("/vagas/{id}/arquivar")
    public String archive(@PathVariable Long id) {
        jobService.archive(id);
        return "redirect:/admin/vagas";
    }

    @PostMapping("/vagas/{id}/remover")
    public String delete(@PathVariable Long id) {
        jobService.delete(id);
        return "redirect:/admin/vagas";
    }
}
