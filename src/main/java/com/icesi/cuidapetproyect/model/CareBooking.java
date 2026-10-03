
package com.icesi.cuidapetproyect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "care_booking")
public class CareBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private CareRequest request;

    @OneToOne
    @JoinColumn(name = "accepted_application_id", nullable = false, unique = true)
    private CareApplication acceptedApplication;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @ManyToOne
    @JoinColumn(name = "caregiver_id", nullable = false)
    private CaregiverProfile caregiver;

    @Column(name = "agreed_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal agreedRate;

    @Column(name = "exact_address", nullable = false, length = 500)
    private String exactAddress;

    @ManyToOne
    @JoinColumn(name = "booking_status_code", nullable = false)
    private BookingStatus bookingStatus;

    @Column(name = "owner_confirmed_flag", nullable = false,
            columnDefinition = "CHAR(1)")
    private String ownerConfirmedFlag = "N";

    @Column(name = "caregiver_done_flag", nullable = false,
            columnDefinition = "CHAR(1)")
    private String caregiverDoneFlag = "N";

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @PrePersist
    public void prePersist() {
        if (acceptedAt == null) {
            acceptedAt = LocalDateTime.now();
        }
    }
}
