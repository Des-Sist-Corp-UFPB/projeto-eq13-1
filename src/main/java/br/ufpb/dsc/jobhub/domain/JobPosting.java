package br.ufpb.dsc.jobhub.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 160)
    private String title;

    @NotBlank
    @Column(nullable = false, length = 140)
    private String company;

    @Email
    @NotBlank
    @Column(nullable = false, length = 190)
    private String companyEmail;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private JobLocationType locationType;

    @Column(length = 80)
    private String city;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Seniority seniority;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ContractType contractType;

    @Column(length = 80)
    private String salaryRange;

    @NotBlank
    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String requirements;

    @Column(length = 500)
    private String applyUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private JobStatus status = JobStatus.PENDING;

    @Column(nullable = false)
    private int views = 0;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "job", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<CandidateApplication> applications = new ArrayList<>();

    protected JobPosting() {
    }

    public JobPosting(String title, String company, String companyEmail, JobLocationType locationType,
                      String city, Seniority seniority, ContractType contractType, String salaryRange,
                      String description, String requirements, String applyUrl) {
        this.title = title;
        this.company = company;
        this.companyEmail = companyEmail;
        this.locationType = locationType;
        this.city = city;
        this.seniority = seniority;
        this.contractType = contractType;
        this.salaryRange = salaryRange;
        this.description = description;
        this.requirements = requirements;
        this.applyUrl = applyUrl;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void publish() { status = JobStatus.PUBLISHED; }
    public void archive() { status = JobStatus.ARCHIVED; }
    public void sendToPending() { status = JobStatus.PENDING; }
    public void incrementViews() { views++; }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getCompanyEmail() { return companyEmail; }
    public JobLocationType getLocationType() { return locationType; }
    public String getCity() { return city; }
    public Seniority getSeniority() { return seniority; }
    public ContractType getContractType() { return contractType; }
    public String getSalaryRange() { return salaryRange; }
    public String getDescription() { return description; }
    public String getRequirements() { return requirements; }
    public String getApplyUrl() { return applyUrl; }
    public JobStatus getStatus() { return status; }
    public int getViews() { return views; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<CandidateApplication> getApplications() { return applications; }
}
