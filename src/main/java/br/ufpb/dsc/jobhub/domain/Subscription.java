package br.ufpb.dsc.jobhub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

@Entity
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String company;

    @Column(nullable = false, length = 190)
    private String companyEmail;

    @Column(nullable = false, length = 120)
    private String planCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    @Column(length = 120)
    private String externalReference;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private Instant validUntil;

    protected Subscription() {
    }

    public Subscription(String company, String companyEmail, String planCode) {
        this.company = company;
        this.companyEmail = companyEmail;
        this.planCode = planCode;
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

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE && (validUntil == null || validUntil.isAfter(Instant.now()));
    }

    public void activate(Instant validUntil, String externalReference) {
        this.status = SubscriptionStatus.ACTIVE;
        this.validUntil = validUntil;
        this.externalReference = externalReference;
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELED;
    }

    public Long getId() {
        return id;
    }

    public String getCompany() {
        return company;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public String getPlanCode() {
        return planCode;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getValidUntil() {
        return validUntil;
    }
}