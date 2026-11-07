package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record UserReviewUpdateRequest(
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 1000) String reviewComment) {}
