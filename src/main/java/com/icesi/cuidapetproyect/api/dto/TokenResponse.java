package com.icesi.cuidapetproyect.api.dto;

import java.time.Instant;
import java.util.List;

public record TokenResponse(String accessToken, String tokenType, Instant expiresAt,
                            String email, List<String> roles) {
}
