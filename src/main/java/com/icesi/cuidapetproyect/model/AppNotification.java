
package com.icesi.cuidapetproyect.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_notification")
public class AppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "notification_type", nullable = false, length = 60)
    private String notificationType;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "message_text", nullable = false, length = 1000)
    private String messageText;

    @Column(name = "reference_entity", length = 60)
    private String referenceEntity;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "read_flag", nullable = false,
            columnDefinition = "CHAR(1)")
    private String readFlag = "N";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
