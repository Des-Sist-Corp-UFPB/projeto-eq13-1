package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.domain.ContractType;
import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.JobLocationType;
import br.ufpb.dsc.jobhub.domain.Seniority;
import br.ufpb.dsc.jobhub.dto.CandidateApplicationForm;
import br.ufpb.dsc.jobhub.dto.JobPostForm;
import br.ufpb.dsc.jobhub.service.AuditLogService;
import br.ufpb.dsc.jobhub.service.BillingService;
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
    private final BillingService billingService;

    public PublicController(JobService jobService, UserService userService,
                            AuditLogService auditLogService, BillingService billingService) {
        this.jobService = jobService;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.billingService = billingService;
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
        model.addAttribute("relatedJobs", jobService.relatedJobs(id));
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
            model.addAttribute("relatedJobs", jobService.relatedJobs(id));
            model.addAttribute("applied", false);
            return "jobs/detail";
        }
        AppUser applicantUser = userService.currentUser(authentication).orElse(null);
        var application = jobService.apply(id, form, applicantUser);
        auditLogService.log(request, authentication, "APPLICATION_SUBMITTED", "CANDIDATE_APPLICATION",
                application.getId(), "Candidatura interna enviada.");
        return "redirect:/vagas/" + id + "?applied=true";
    }

    // ── Divulgação de vagas (requer login + assinatura ativa) ────────────────

    @GetMapping("/divulgar")
    public String publishJob(Model model, Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        AppUser user = currentUser(authentication);
        if (!billingService.hasActiveSubscription(user.getEmail())) {
            redirectAttributes.addFlashAttribute("subscriptionRequired", true);
            return "redirect:/divulgar/assinar";
        }
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", prefillForm(user));
        }
        model.addAttribute("user", user);
        return "jobs/post";
    }

    @PostMapping("/divulgar")
    public String createJob(@Valid @ModelAttribute("form") JobPostForm form,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes,
                            Authentication authentication,
                            HttpServletRequest request,
                            Model model) {
        AppUser user = currentUser(authentication);
        // Dupla verificação: bloqueia mesmo acesso direto via POST
        if (!billingService.hasActiveSubscription(user.getEmail())) {
            redirectAttributes.addFlashAttribute("subscriptionRequired", true);
            return "redirect:/divulgar/assinar";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "jobs/post";
        }
        try {
            var job = jobService.createPending(form);
            auditLogService.log(request, authentication, "PUBLIC_JOB_SUBMITTED", "JOB_POSTING",
                    job.getId(), "Vaga enviada pela página pública.");
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("job.location", ex.getMessage());
            model.addAttribute("user", user);
            return "jobs/post";
        }
        redirectAttributes.addFlashAttribute("success", true);
        return "redirect:/divulgar";
    }

    @GetMapping("/divulgar/assinar")
    public String subscribe(Model model, Authentication authentication) {
        AppUser user = currentUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("subscription", billingService.findByCompanyEmail(user.getEmail()));
        return "jobs/subscribe";
    }

    @PostMapping("/divulgar/assinar/checkout")
    public String subscribeCheckout(Authentication authentication,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        AppUser user = currentUser(authentication);
        try {
            BillingService.CheckoutResult result = billingService.createCheckoutSessionForUser(user);
            auditLogService.log(request, user, "STRIPE_CHECKOUT_STARTED", "SUBSCRIPTION",
                    result.subscription().getId(), "Checkout de assinatura iniciado no Stripe.");
            return "redirect:" + result.checkoutUrl();
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("billingError", ex.getMessage());
            return "redirect:/divulgar/assinar";
        }
    }

    @GetMapping("/divulgar/assinar/sucesso")
    public String subscriptionSuccess(@RequestParam(name = "subscription", required = false) Long subscriptionId,
                                      Authentication authentication,
                                      HttpServletRequest request,
                                      Model model) {
        if (subscriptionId != null && authentication != null && authentication.isAuthenticated()) {
            try {
                AppUser user = currentUser(authentication);
                billingService.activateAfterConfirmedPayment(subscriptionId, user.getEmail());
                auditLogService.log(request, user, "STRIPE_SUBSCRIPTION_ACTIVATED",
                        "SUBSCRIPTION", subscriptionId, "Assinatura ativada após retorno do checkout do Stripe.");
            } catch (Exception ignored) {
                // assinatura pode já ter sido ativada ou não pertencer ao usuário
            }
        }
        model.addAttribute("billingMessage", "Pagamento confirmado! Sua assinatura está ativa.");
        return "admin/billing-success";
    }

    @GetMapping("/divulgar/assinar/cancelado")
    public String subscriptionCanceled(Model model) {
        model.addAttribute("billingMessage", "Checkout cancelado. Nenhuma assinatura foi ativada.");
        return "admin/billing-canceled";
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private AppUser currentUser(Authentication authentication) {
        return userService.currentUser(authentication).orElseThrow();
    }

    private JobPostForm prefillForm(AppUser user) {
        return new JobPostForm(
                "", user.getName(), user.getEmail(),
                br.ufpb.dsc.jobhub.domain.JobLocationType.REMOTE, "",
                br.ufpb.dsc.jobhub.domain.Seniority.JUNIOR,
                br.ufpb.dsc.jobhub.domain.ContractType.CLT,
                "", "", "", ""
        );
    }
}
