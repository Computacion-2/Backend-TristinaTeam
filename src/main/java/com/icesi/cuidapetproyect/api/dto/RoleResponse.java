package com.icesi.cuidapetproyect.api.dto;

import java.time.LocalDateTime;

public record RoleResponse(
        Long id, String roleName, String description, String activeFlag,
        LocalDateTime createdAt) {
}
