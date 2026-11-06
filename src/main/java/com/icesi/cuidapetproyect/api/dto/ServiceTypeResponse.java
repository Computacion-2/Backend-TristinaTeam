package com.icesi.cuidapetproyect.api.dto;

import java.time.LocalDateTime;

public record ServiceTypeResponse(
        Long id, String serviceName, String description, String activeFlag, LocalDateTime createdAt) {
}
