package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record PetPhotoCreateRequest(
        @NotNull @Positive Long petId,
        @NotBlank @Size(max = 500) @Pattern(regexp = "https?://[^\\s]+") String photoUrl,
        @NotBlank @Pattern(regexp = "Y|N") String mainPhotoFlag) {
}
