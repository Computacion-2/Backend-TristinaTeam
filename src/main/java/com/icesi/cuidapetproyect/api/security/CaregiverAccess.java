package com.icesi.cuidapetproyect.api.security;

import com.icesi.cuidapetproyect.exception.*;
import com.icesi.cuidapetproyect.model.CaregiverProfile;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CaregiverAccess {
    private final CaregiverProfileRepository profiles;
    private final UserRoleRepository roles;
    private final ResourceAccess access;

    public boolean isAvailable(CaregiverProfile profile) {
        return "Y".equals(profile.getActiveFlag())
                && "ACTIVE".equals(profile.getUser().getAccountStatus())
                && roles.findByUser_Id(profile.getId()).stream().anyMatch(assignment ->
                    "CAREGIVER".equals(assignment.getRole().getRoleName())
                    && "Y".equals(assignment.getRole().getActiveFlag()));
    }

    public CaregiverProfile requireAvailable(Long id) {
        CaregiverProfile profile = profiles.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de cuidador no encontrado"));
        if (!isAvailable(profile)) {
            throw new ConflictException("Se requiere un perfil de cuidador activo con usuario y rol CAREGIVER activos");
        }
        return profile;
    }

    public boolean canRead(CaregiverProfile profile, boolean active, Authentication authentication) {
        return access.isAdmin(authentication)
                || access.currentUser(authentication).getId().equals(profile.getId())
                || (active && isAvailable(profile));
    }

    public void requireRead(CaregiverProfile profile, boolean active, Authentication authentication) {
        if (!canRead(profile, active, authentication)) {
            throw new AccessDeniedException("El recurso del cuidador no está disponible");
        }
    }
}
