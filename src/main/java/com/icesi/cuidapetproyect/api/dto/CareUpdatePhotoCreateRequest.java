package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record CareUpdatePhotoCreateRequest(
        @NotNull @Positive Long updateId,
        @NotBlank @Size(max = 500) @Pattern(regexp = "https?://[^\\s]+") String photoUrl) {}
