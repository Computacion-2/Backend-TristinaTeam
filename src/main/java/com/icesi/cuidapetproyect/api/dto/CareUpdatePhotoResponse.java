package com.icesi.cuidapetproyect.api.dto;

import java.time.LocalDateTime;

public record CareUpdatePhotoResponse(Long id, Long updateId, String photoUrl, LocalDateTime uploadedAt) {}
