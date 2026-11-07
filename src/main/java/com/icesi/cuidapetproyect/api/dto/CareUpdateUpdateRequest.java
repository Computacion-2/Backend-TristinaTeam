package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record CareUpdateUpdateRequest(@NotBlank @Size(max = 1200) String updateMessage) {}
