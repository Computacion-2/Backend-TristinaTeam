package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record UserUpdateRequest(
        @NotBlank @Email @Size(max = 160) String email,
        @Size(min = 8, max = 72) String password,
        @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE|SUSPENDED|DELETED") String accountStatus) {
}
