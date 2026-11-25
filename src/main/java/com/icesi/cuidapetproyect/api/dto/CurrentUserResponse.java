package com.icesi.cuidapetproyect.api.dto;

import java.util.List;

public record CurrentUserResponse(Long userId, String email, List<String> roles, List<String> permissions) {
}
