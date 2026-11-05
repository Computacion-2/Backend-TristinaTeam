package com.icesi.cuidapetproyect.api.dto;

import java.time.LocalDateTime;

public record UserRoleResponse(
        Long userId, Long roleId, LocalDateTime assignedAt, Long assignedById) {
}
