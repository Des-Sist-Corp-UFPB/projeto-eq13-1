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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "candidate_experience")
public class CandidateExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser user;

    @NotBlank
    @Column(nullable = false, length = 140)
    private String roleTitle;

    @NotBlank
    @Column(nullable = false, length = 140)
    private String company;

    @NotNull
    @Column(nullable = false)
    private LocalDate startedOn;

    private LocalDate endedOn;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected CandidateExperience() {
    }

    public CandidateExperience(AppUser user, String roleTitle, String company, LocalDate startedOn,
                               LocalDate endedOn, String description) {
        this.user = user;
        this.roleTitle = roleTitle;
        this.company = company;
        this.startedOn = startedOn;
        this.endedOn = endedOn;
        this.description = description;
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

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public String getRoleTitle() { return roleTitle; }
    public String getCompany() { return company; }
    public LocalDate getStartedOn() { return startedOn; }
    public LocalDate getEndedOn() { return endedOn; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
