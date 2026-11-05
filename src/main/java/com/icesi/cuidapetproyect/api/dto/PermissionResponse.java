package com.icesi.cuidapetproyect.api.dto;

import java.time.LocalDateTime;

public record PermissionResponse(
        Long id, String permissionCode, String permissionName, String description,
        LocalDateTime createdAt) {
}
