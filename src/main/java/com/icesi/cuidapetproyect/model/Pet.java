package com.icesi.cuidapetproyect.model;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pet")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pet_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @ManyToOne
    @JoinColumn(name = "species_id", nullable = false)
    private com.icesi.cuidapetproyect.model.PetSpecies species;

    @Column(name = "pet_name", nullable = false, length = 100)
    private String petName;

    @Column(name = "breed", length = 100)
    private String breed;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "sex", length = 12)
    private String sex;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "temperament", length = 500)
    private String temperament;

    @Column(name = "care_instructions", length = 1000)
    private String careInstructions;

    @Column(name = "medical_alerts", length = 1000)
    private String medicalAlerts;

    @Column(name = "pet_status", nullable = false, length = 20)
    private String petStatus = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "pet")
    private List<com.icesi.cuidapetproyect.model.PetPhoto> photos = new ArrayList<>();

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