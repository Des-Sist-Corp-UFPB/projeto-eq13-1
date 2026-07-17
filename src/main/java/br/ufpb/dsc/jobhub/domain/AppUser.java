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
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 140)
    private String name;

    @Email
    @NotBlank
    @Column(nullable = false, length = 190)
    private String email;

    @Column(length = 80)
    private String username;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private UserRole role = UserRole.ROLE_USER;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(columnDefinition = "text")
    private String biography;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ThemePreference themePreference = ThemePreference.LIGHT;

    @Column(columnDefinition = "bytea")
    private byte[] photoContent;

    @Column(length = 100)
    private String photoContentType;

    @Column(columnDefinition = "bytea")
    private byte[] resumeContent;

    @Column(length = 255)
    private String resumeFileName;

    @Column(length = 100)
    private String resumeContentType;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CandidateExperience> experiences = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected AppUser() {
    }

    public AppUser(String name, String email, String username, String passwordHash, UserRole role, AuthProvider provider) {
        this.name = name;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.provider = provider;
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

    public void updateGoogleProfile(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    public void updateProfile(String name, String biography) {
        this.name = name;
        this.biography = biography;
    }

    public void updatePhoto(byte[] content, String contentType) {
        this.photoContent = content;
        this.photoContentType = contentType;
    }

    public void updateResume(byte[] content, String fileName, String contentType) {
        this.resumeContent = content;
        this.resumeFileName = fileName;
        this.resumeContentType = contentType;
    }

    public void changeTheme(ThemePreference themePreference) {
        this.themePreference = themePreference;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public AuthProvider getProvider() { return provider; }
    public boolean isEnabled() { return enabled; }
    public String getBiography() { return biography; }
    public ThemePreference getThemePreference() { return themePreference; }
    public byte[] getPhotoContent() { return photoContent; }
    public String getPhotoContentType() { return photoContentType; }
    public byte[] getResumeContent() { return resumeContent; }
    public String getResumeFileName() { return resumeFileName; }
    public String getResumeContentType() { return resumeContentType; }
    public List<CandidateExperience> getExperiences() { return experiences; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
