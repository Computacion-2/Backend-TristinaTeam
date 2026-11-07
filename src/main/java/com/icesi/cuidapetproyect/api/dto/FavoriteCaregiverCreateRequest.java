package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record FavoriteCaregiverCreateRequest(@NotNull @Positive Long ownerId, @NotNull @Positive Long caregiverId) {}
