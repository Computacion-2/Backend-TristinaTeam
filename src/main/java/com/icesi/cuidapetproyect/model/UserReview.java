
package com.icesi.cuidapetproyect.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_review")
public class UserReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private CareBooking booking;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "reviewer_id", nullable = false)
    private AppUser reviewer;

    @ManyToOne
    @JoinColumn(name = "reviewed_user_id", nullable = false)
    private AppUser reviewedUser;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
