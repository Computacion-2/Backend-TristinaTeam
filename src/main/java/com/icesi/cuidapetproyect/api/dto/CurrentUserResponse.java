package com.icesi.cuidapetproyect.api.dto;

import java.util.List;

public record CurrentUserResponse(String email, List<String> roles, List<String> permissions) {
}
