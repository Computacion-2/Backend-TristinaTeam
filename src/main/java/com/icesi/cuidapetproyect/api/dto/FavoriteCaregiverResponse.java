package com.icesi.cuidapetproyect.api.dto;

import java.time.LocalDateTime;

public record FavoriteCaregiverResponse(Long ownerId, Long caregiverId, LocalDateTime createdAt) {}
