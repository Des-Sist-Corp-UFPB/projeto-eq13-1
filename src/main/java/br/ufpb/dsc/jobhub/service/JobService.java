package br.ufpb.dsc.jobhub.service;

import br.ufpb.dsc.jobhub.domain.CandidateApplication;
import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.JobLocationType;
import br.ufpb.dsc.jobhub.domain.JobPosting;
import br.ufpb.dsc.jobhub.domain.JobStatus;
import br.ufpb.dsc.jobhub.dto.CandidateApplicationForm;
import br.ufpb.dsc.jobhub.dto.JobPostForm;
import br.ufpb.dsc.jobhub.repository.CandidateApplicationRepository;
import br.ufpb.dsc.jobhub.repository.JobPostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class JobService {

    private final JobPostingRepository jobPostingRepository;
    private final CandidateApplicationRepository applicationRepository;

    public JobService(JobPostingRepository jobPostingRepository, CandidateApplicationRepository applicationRepository) {
        this.jobPostingRepository = jobPostingRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public List<JobPosting> featuredJobs() {
        return jobPostingRepository.findTop6ByStatusOrderByCreatedAtDesc(JobStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public List<JobPosting> searchPublished(String keyword, String location) {
        return jobPostingRepository.searchPublished(normalize(keyword), parseLocation(location));
    }

    @Transactional(readOnly = true)
    public List<JobPosting> searchAdmin(String keyword, String status, String location) {
        return jobPostingRepository.searchAdmin(normalize(keyword), parseStatus(status), parseLocation(location));
    }

    @Transactional
    public JobPosting publicDetails(Long id) {
        JobPosting job = jobPostingRepository.findByIdAndStatus(id, JobStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("Vaga não encontrada"));
        job.incrementViews();
        return job;
    }

    @Transactional
    public JobPosting createPending(JobPostForm form) {
        validateLocation(form.locationType(), form.city());
        JobPosting job = new JobPosting(
                trim(form.title()), trim(form.company()), trim(form.companyEmail()), form.locationType(),
                normalizeCity(form.locationType(), form.city()), form.seniority(), form.contractType(), trim(form.salaryRange()),
                trim(form.description()), trim(form.requirements()), trim(form.applyUrl())
        );
        return jobPostingRepository.save(job);
    }

    @Transactional
    public JobPosting createByAdmin(JobPostForm form) {
        JobPosting job = createPending(form);
        job.publish();
        return job;
    }

    @Transactional
    public CandidateApplication apply(Long jobId, CandidateApplicationForm form) {
        return apply(jobId, form, null);
    }

    @Transactional
    public CandidateApplication apply(Long jobId, CandidateApplicationForm form, AppUser applicantUser) {
        JobPosting job = jobPostingRepository.findByIdAndStatus(jobId, JobStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("Vaga não encontrada"));
        CandidateApplication application = new CandidateApplication(
                job, applicantUser, trim(form.applicantName()), trim(form.applicantEmail()),
                trim(form.linkedinUrl()), trim(form.message())
        );
        return applicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public List<JobPosting> allJobs() {
        return jobPostingRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<CandidateApplication> allApplications() {
        return applicationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public JobPosting findAny(Long id) {
        return jobPostingRepository.findById(id).orElseThrow();
    }

    @Transactional
    public JobPosting publish(Long id) {
        JobPosting job = jobPostingRepository.findById(id).orElseThrow();
        job.publish();
        return job;
    }

    @Transactional
    public JobPosting archive(Long id) {
        JobPosting job = jobPostingRepository.findById(id).orElseThrow();
        job.archive();
        return job;
    }

    @Transactional
    public JobPosting sendToPending(Long id) {
        JobPosting job = jobPostingRepository.findById(id).orElseThrow();
        job.sendToPending();
        return job;
    }

    @Transactional
    public JobPosting delete(Long id) {
        JobPosting job = jobPostingRepository.findById(id).orElseThrow();
        jobPostingRepository.delete(job);
        return job;
    }

    private JobLocationType parseLocation(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        try {
            return JobLocationType.valueOf(location.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private JobStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return JobStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void validateLocation(JobLocationType locationType, String city) {
        if (locationType != JobLocationType.REMOTE && (city == null || city.isBlank())) {
            throw new IllegalArgumentException("Informe a cidade para vagas híbridas ou presenciais na Paraíba.");
        }
    }

    private String normalizeCity(JobLocationType locationType, String city) {
        if (locationType == JobLocationType.REMOTE) {
            return null;
        }
        return trim(city);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
