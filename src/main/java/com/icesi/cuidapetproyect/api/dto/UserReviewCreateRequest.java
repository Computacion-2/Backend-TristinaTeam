package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record UserReviewCreateRequest(
        @NotNull @Positive Long bookingId,
        @NotNull @Positive Long reviewerId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 1000) String reviewComment) {}
