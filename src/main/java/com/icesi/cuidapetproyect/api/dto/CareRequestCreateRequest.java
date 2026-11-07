package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CareRequestCreateRequest(
        @NotNull @Positive Long ownerId,
        @NotNull @Positive Long petId,
        @NotNull @Positive Long serviceTypeId,
        @NotNull @Future LocalDateTime startAt,
        @NotNull @Future LocalDateTime endAt,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 160) String zoneName,
        @Size(max = 500) String addressReference,
        @Size(max = 1500) String requestNotes,
        @Positive @Digits(integer = 10, fraction = 2) BigDecimal maxBudget) {}
