
package com.icesi.cuidapetproyect.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "care_application")
public class CareApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "request_id", nullable = false)
    private CareRequest request;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "caregiver_id", nullable = false)
    private CaregiverProfile caregiver;

    @Column(name = "proposed_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal proposedRate;

    @Column(name = "application_message", length = 1000)
    private String applicationMessage;

    @Column(name = "application_status", nullable = false, length = 20)
    private String applicationStatus = "PENDING";

    @Column(name = "response_reason", length = 500)
    private String responseReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "acceptedApplication")
    private CareBooking booking;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
