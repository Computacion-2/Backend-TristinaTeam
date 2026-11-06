package com.icesi.cuidapetproyect.api.dto;

import java.time.LocalDateTime;

public record CaregiverProfileResponse(
        Long caregiverId, String biography, Integer experienceYears, String serviceCity,
        String serviceZone, String activeFlag, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
