package br.ufpb.dsc.jobhub.service;

import br.ufpb.dsc.jobhub.domain.CandidateApplication;
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

    @Transactional
    public JobPosting publicDetails(Long id) {
        JobPosting job = jobPostingRepository.findByIdAndStatus(id, JobStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("Vaga nao encontrada"));
        job.incrementViews();
        return job;
    }

    @Transactional
    public JobPosting createPending(JobPostForm form) {
        JobPosting job = new JobPosting(
                trim(form.title()), trim(form.company()), trim(form.companyEmail()), form.locationType(),
                trim(form.city()), form.seniority(), form.contractType(), trim(form.salaryRange()),
                trim(form.description()), trim(form.requirements()), trim(form.applyUrl())
        );
        return jobPostingRepository.save(job);
    }

    @Transactional
    public CandidateApplication apply(Long jobId, CandidateApplicationForm form) {
        JobPosting job = jobPostingRepository.findByIdAndStatus(jobId, JobStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("Vaga nao encontrada"));
        CandidateApplication application = new CandidateApplication(
                job, trim(form.applicantName()), trim(form.applicantEmail()),
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

    @Transactional
    public void publish(Long id) {
        jobPostingRepository.findById(id).orElseThrow().publish();
    }

    @Transactional
    public void archive(Long id) {
        jobPostingRepository.findById(id).orElseThrow().archive();
    }

    @Transactional
    public void sendToPending(Long id) {
        jobPostingRepository.findById(id).orElseThrow().sendToPending();
    }

    @Transactional
    public void delete(Long id) {
        jobPostingRepository.deleteById(id);
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

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
