package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record FavoriteCaregiverUpdateRequest(@NotNull @Positive Long caregiverId) {}
