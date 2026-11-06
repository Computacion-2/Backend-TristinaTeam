package com.icesi.cuidapetproyect.api.dto;

import java.time.LocalDateTime;

public record PetPhotoResponse(
        Long id, Long petId, String photoUrl, String mainPhotoFlag, LocalDateTime uploadedAt) {
}
