package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record UserProfileCreateRequest(
        @NotNull @Positive Long userId,
        @NotBlank @Size(max = 140) String fullName,
        @Size(max = 500) String photoUrl,
        @Size(max = 30) String phoneNumber,
        @Size(max = 100) String city,
        @Size(max = 140) String academicProgram,
        @Size(max = 140) String emergencyContact,
        @Size(max = 30) String emergencyPhone) {
}
