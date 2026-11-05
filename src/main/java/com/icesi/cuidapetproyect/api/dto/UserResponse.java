package com.icesi.cuidapetproyect.api.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id, String email, String accountStatus,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
