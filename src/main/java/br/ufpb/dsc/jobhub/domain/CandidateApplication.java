package br.ufpb.dsc.jobhub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

@Entity
public class CandidateApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPosting job;

    @NotBlank
    @Column(nullable = false, length = 140)
    private String applicantName;

    @Email
    @NotBlank
    @Column(nullable = false, length = 190)
    private String applicantEmail;

    @Column(length = 500)
    private String linkedinUrl;

    @Column(columnDefinition = "text")
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    protected CandidateApplication() {
    }

    public CandidateApplication(JobPosting job, String applicantName, String applicantEmail, String linkedinUrl, String message) {
        this.job = job;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.linkedinUrl = linkedinUrl;
        this.message = message;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public JobPosting getJob() { return job; }
    public String getApplicantName() { return applicantName; }
    public String getApplicantEmail() { return applicantEmail; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
