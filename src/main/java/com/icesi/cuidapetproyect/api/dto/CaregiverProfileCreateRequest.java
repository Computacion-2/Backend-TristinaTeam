package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record CaregiverProfileCreateRequest(
        @NotNull @Positive Long caregiverId,
        @NotBlank @Size(max = 1200) String biography,
        @NotNull @Min(0) @Max(999) Integer experienceYears,
        @NotBlank @Size(max = 100) String serviceCity,
        @Size(max = 160) String serviceZone) {
}
