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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id")
    private AppUser applicantUser;

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

    @Column(name = "resume_content", columnDefinition = "bytea")
    private byte[] resumeContent;

    @Column(name = "resume_file_name", length = 255)
    private String resumeFileName;

    @Column(name = "resume_content_type", length = 100)
    private String resumeContentType;

    @Column(nullable = false)
    private Instant createdAt;

    protected CandidateApplication() {
    }

    public CandidateApplication(JobPosting job, String applicantName, String applicantEmail, String linkedinUrl, String message) {
        this(job, null, applicantName, applicantEmail, linkedinUrl, message);
    }

    public CandidateApplication(JobPosting job, AppUser applicantUser, String applicantName, String applicantEmail, String linkedinUrl, String message) {
        this.job = job;
        this.applicantUser = applicantUser;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.linkedinUrl = linkedinUrl;
        this.message = message;
        attachResumeFrom(applicantUser);
    }

    public void attachResumeFrom(AppUser user) {
        if (user == null || user.getResumeContent() == null) {
            return;
        }
        this.resumeContent = user.getResumeContent().clone();
        this.resumeFileName = user.getResumeFileName();
        this.resumeContentType = user.getResumeContentType();
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public JobPosting getJob() { return job; }
    public AppUser getApplicantUser() { return applicantUser; }
    public String getApplicantName() { return applicantName; }
    public String getApplicantEmail() { return applicantEmail; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public String getMessage() { return message; }
    public byte[] getResumeContent() { return resumeContent == null ? null : resumeContent.clone(); }
    public String getResumeFileName() { return resumeFileName; }
    public String getResumeContentType() { return resumeContentType; }
    public boolean hasResume() { return resumeContent != null && resumeContent.length > 0; }
    public Instant getCreatedAt() { return createdAt; }
}
