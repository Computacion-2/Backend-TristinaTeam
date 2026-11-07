package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CaregiverAvailabilityUpdateRequest(@NotNull @Min(1) @Max(7) Integer dayOfWeek,
        @NotBlank @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]") String startTime,
        @NotBlank @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]") String endTime,
        LocalDate validFrom, LocalDate validUntil, @NotBlank @Pattern(regexp = "Y|N") String activeFlag) {}
