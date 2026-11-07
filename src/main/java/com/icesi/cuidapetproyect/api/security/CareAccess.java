package com.icesi.cuidapetproyect.api.security;

import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CareAccess {
    private final ResourceAccess access;
    private final CaregiverAccess caregivers;
    private final CaregiverProfileRepository profiles;
    private final CareApplicationRepository applications;

    public Long currentUserId(Authentication auth) {
        return access.currentUser(auth).getId();
    }

    public boolean isAdmin(Authentication auth) {
        return access.isAdmin(auth);
    }

    public boolean isActiveCaregiver(Long userId) {
        return profiles.findById(userId).map(caregivers::isAvailable).orElse(false);
    }

    public boolean canReadRequest(CareRequest request, Authentication auth) {
        if (access.isAdmin(auth)) return true;
        Long userId = currentUserId(auth);
        if (request.getOwner().getId().equals(userId)) return true;
        if (applications.existsByRequest_IdAndCaregiver_Id(request.getId(), userId)) return true;
        return "OPEN".equals(request.getRequestStatus()) && isActiveCaregiver(userId);
    }

    public void requireReadRequest(CareRequest request, Authentication auth) {
        if (!canReadRequest(request, auth)) {
            throw new AccessDeniedException("La solicitud no está disponible para este usuario");
        }
    }

    public boolean canReadApplication(CareApplication application, Authentication auth) {
        if (access.isAdmin(auth)) return true;
        Long userId = currentUserId(auth);
        return application.getCaregiver().getId().equals(userId)
                || application.getRequest().getOwner().getId().equals(userId);
    }

    public void requireReadApplication(CareApplication application, Authentication auth) {
        if (!canReadApplication(application, auth)) {
            throw new AccessDeniedException("La postulación no está disponible para este usuario");
        }
    }
}
