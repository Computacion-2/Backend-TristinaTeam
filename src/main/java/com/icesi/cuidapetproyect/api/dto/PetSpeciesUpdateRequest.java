package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record PetSpeciesUpdateRequest(
        @NotBlank @Size(max = 80) String speciesName,
        @NotBlank @Pattern(regexp = "Y|N") String activeFlag) {
}
