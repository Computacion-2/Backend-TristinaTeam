package com.icesi.cuidapetproyect.api.dto;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, Long userId, String notificationType, String title, String messageText,
        String referenceEntity, Long referenceId, String readFlag, LocalDateTime createdAt, LocalDateTime readAt) {}
