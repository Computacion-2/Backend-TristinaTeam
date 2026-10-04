
package com.icesi.cuidapetproyect.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "incident_report")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private AppUser reporter;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "reported_user_id", nullable = false)
    private AppUser reportedUser;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "request_id", nullable = false)
    private CareRequest request;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "booking_id")
    private CareBooking booking;

    @Column(name = "report_reason", nullable = false, length = 200)
    private String reportReason;

    @Column(name = "report_details", nullable = false, length = 2000)
    private String reportDetails;

    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    @Column(name = "report_status", nullable = false, length = 20)
    private String reportStatus;

    @Column(name = "resolution_notes", length = 1500)
    private String resolutionNotes;

    @ManyToOne
    @JoinColumn(name = "resolved_by")
    private AppUser resolvedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
