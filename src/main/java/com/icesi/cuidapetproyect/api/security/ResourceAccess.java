package com.icesi.cuidapetproyect.api.security;

import com.icesi.cuidapetproyect.model.AppUser;
import com.icesi.cuidapetproyect.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceAccess {
    private final AppUserRepository users;

    public boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    public AppUser currentUser(Authentication authentication) {
        return users.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuario no disponible"));
    }

    public void requireOwnerOrAdmin(Long ownerId, Authentication authentication) {
        if (!isAdmin(authentication) && !currentUser(authentication).getId().equals(ownerId)) {
            throw new AccessDeniedException("No puede acceder a recursos de otro usuario");
        }
    }

    public void requireAdmin(Authentication authentication) {
        if (!isAdmin(authentication)) {
            throw new AccessDeniedException("Se requiere el rol ADMIN");
        }
    }
}
