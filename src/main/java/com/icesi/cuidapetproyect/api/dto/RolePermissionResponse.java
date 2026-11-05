package com.icesi.cuidapetproyect.api.dto;

import java.time.LocalDateTime;

public record RolePermissionResponse(
        Long roleId, Long permissionId, LocalDateTime grantedAt) {
}
