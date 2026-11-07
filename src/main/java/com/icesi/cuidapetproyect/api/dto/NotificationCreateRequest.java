package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record NotificationCreateRequest(
        @NotNull @Positive Long userId,
        @NotBlank @Size(max = 60) @Pattern(regexp = "[A-Z][A-Z0-9_]*") String notificationType,
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 1000) String messageText,
        @Size(max = 60) @Pattern(regexp = "[A-Z][A-Z0-9_]*") String referenceEntity,
        @Positive Long referenceId) {}
