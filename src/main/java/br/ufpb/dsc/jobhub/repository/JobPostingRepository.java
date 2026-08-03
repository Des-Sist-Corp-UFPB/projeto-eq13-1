package br.ufpb.dsc.jobhub.repository;

import br.ufpb.dsc.jobhub.domain.JobLocationType;
import br.ufpb.dsc.jobhub.domain.JobPosting;
import br.ufpb.dsc.jobhub.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    List<JobPosting> findTop6ByStatusOrderByCreatedAtDesc(JobStatus status);
    List<JobPosting> findAllByOrderByCreatedAtDesc();
    Optional<JobPosting> findByIdAndStatus(Long id, JobStatus status);
    long countByStatus(JobStatus status);
    long countByLocationTypeAndStatus(JobLocationType locationType, JobStatus status);
    long countByCreatedAtAfter(Instant createdAt);

    @Query("select coalesce(sum(j.views), 0) from JobPosting j")
    long sumViews();

    @Query("""
            select j from JobPosting j
            where j.status = br.ufpb.dsc.jobhub.domain.JobStatus.PUBLISHED
              and (:locationType is null or j.locationType = :locationType)
              and (:keyword is null or :keyword = ''
                   or lower(j.title) like lower(concat('%', :keyword, '%'))
                   or lower(j.company) like lower(concat('%', :keyword, '%'))
                   or lower(j.description) like lower(concat('%', :keyword, '%')))
            order by j.createdAt desc
            """)
    List<JobPosting> searchPublished(@Param("keyword") String keyword,
                                     @Param("locationType") JobLocationType locationType);

    @Query("""
            select j from JobPosting j
            where (:status is null or j.status = :status)
              and (:locationType is null or j.locationType = :locationType)
              and (:keyword is null or :keyword = ''
                   or lower(j.title) like lower(concat('%', :keyword, '%'))
                   or lower(j.company) like lower(concat('%', :keyword, '%')))
            order by j.createdAt desc
            """)
    List<JobPosting> searchAdmin(@Param("keyword") String keyword,
                                 @Param("status") JobStatus status,
                                 @Param("locationType") JobLocationType locationType);
}
