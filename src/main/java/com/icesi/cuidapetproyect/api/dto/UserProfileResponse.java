package com.icesi.cuidapetproyect.api.dto;

import java.time.*;

public record UserProfileResponse(
        Long userId, String fullName, String photoUrl, String phoneNumber,
        String city, String academicProgram, String emergencyContact, String emergencyPhone,
        LocalDateTime profileUpdatedAt) {
}
