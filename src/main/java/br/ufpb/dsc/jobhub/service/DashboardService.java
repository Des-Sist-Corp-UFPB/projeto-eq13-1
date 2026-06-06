package br.ufpb.dsc.jobhub.service;

import br.ufpb.dsc.jobhub.domain.JobLocationType;
import br.ufpb.dsc.jobhub.domain.JobStatus;
import br.ufpb.dsc.jobhub.dto.DashboardStats;
import br.ufpb.dsc.jobhub.repository.CandidateApplicationRepository;
import br.ufpb.dsc.jobhub.repository.JobPostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class DashboardService {

    private final JobPostingRepository jobPostingRepository;
    private final CandidateApplicationRepository applicationRepository;

    public DashboardService(JobPostingRepository jobPostingRepository, CandidateApplicationRepository applicationRepository) {
        this.jobPostingRepository = jobPostingRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        Instant lastWeek = Instant.now().minus(7, ChronoUnit.DAYS);
        return new DashboardStats(
                jobPostingRepository.count(),
                jobPostingRepository.countByStatus(JobStatus.PUBLISHED),
                jobPostingRepository.countByStatus(JobStatus.PENDING),
                jobPostingRepository.countByStatus(JobStatus.ARCHIVED),
                applicationRepository.count(),
                jobPostingRepository.countByLocationTypeAndStatus(JobLocationType.REMOTE, JobStatus.PUBLISHED),
                jobPostingRepository.countByLocationTypeAndStatus(JobLocationType.PRESENTIAL_PB, JobStatus.PUBLISHED),
                jobPostingRepository.countByCreatedAtAfter(lastWeek),
                applicationRepository.countByCreatedAtAfter(lastWeek),
                jobPostingRepository.sumViews()
        );
    }
}
