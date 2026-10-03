package com.icesi.cuidapet.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "caregiver_profile")
public class CaregiverProfile {

    @Id
    @Column(name = "caregiver_id")
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "caregiver_id")
    private AppUser user;

    @Column(name = "biography", nullable = false, length = 1200)
    private String biography;

    @Column(name = "experience_years", nullable = false)
    private Integer experienceYears = 0;

    @Column(name = "service_city", nullable = false, length = 100)
    private String serviceCity;

    @Column(name = "service_zone", length = 160)
    private String serviceZone;

    @Column(name = "active_flag", nullable = false, length = 1)
    private String activeFlag = "Y";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "caregiver")
    private List<CaregiverService> services = new ArrayList<>();

    @OneToMany(mappedBy = "caregiver")
    private List<CaregiverAvailability> availabilities = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}