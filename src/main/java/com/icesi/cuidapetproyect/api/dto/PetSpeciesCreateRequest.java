package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record PetSpeciesCreateRequest(
        @NotBlank @Size(max = 80) String speciesName) {
}
