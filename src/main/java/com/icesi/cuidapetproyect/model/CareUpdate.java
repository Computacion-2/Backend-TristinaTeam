
package com.icesi.cuidapetproyect.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "care_update")
public class CareUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "update_id")
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private CareBooking booking;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "caregiver_id", nullable = false)
    private CaregiverProfile caregiver;

    @Column(name = "update_message", nullable = false, length = 1200)
    private String updateMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "careUpdate")
    private List<CareUpdatePhoto> photos = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
