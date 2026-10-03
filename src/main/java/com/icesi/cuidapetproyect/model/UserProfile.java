package com.icesi.cuidapetproyect.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "full_name", nullable = false, length = 140)
    private String fullName;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "academic_program", length = 140)
    private String academicProgram;

    @Column(name = "emergency_contact", length = 140)
    private String emergencyContact;

    @Column(name = "emergency_phone", length = 30)
    private String emergencyPhone;

    @Column(name = "profile_updated_at", nullable = false)
    private LocalDateTime profileUpdatedAt;

    @PrePersist
    @PreUpdate
    public void updateDate() {
        profileUpdatedAt = LocalDateTime.now();
    }
}