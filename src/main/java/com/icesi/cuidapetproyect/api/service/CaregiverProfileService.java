package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.CaregiverProfileMapper;
import com.icesi.cuidapetproyect.api.security.ResourceAccess;
import com.icesi.cuidapetproyect.exception.*;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaregiverProfileService {
    private final CaregiverProfileRepository profiles;
    private final AppUserRepository users;
    private final UserRoleRepository userRoles;
    private final CaregiverProfileMapper mapper;
    private final ResourceAccess access;

    public List<CaregiverProfileResponse> findAll(Authentication authentication) {
        if (access.isAdmin(authentication)) {
            return profiles.findAll().stream().map(mapper::entityToDto).toList();
        }
        Long ownId = access.currentUser(authentication).getId();
        return profiles.findAll().stream()
                .filter(profile -> ownId.equals(profile.getId()) || isPublic(profile))
                .map(mapper::entityToDto).toList();
    }

    public CaregiverProfileResponse findById(Long id, Authentication authentication) {
        CaregiverProfile profile = requireProfile(id);
        if (!access.isAdmin(authentication) && !isPublic(profile)
                && !access.currentUser(authentication).getId().equals(id)) {
            throw new AccessDeniedException("El perfil de cuidador no está disponible");
        }
        return mapper.entityToDto(profile);
    }

    @Transactional
    public CaregiverProfileResponse create(CaregiverProfileCreateRequest dto, Authentication authentication) {
        access.requireOwnerOrAdmin(dto.caregiverId(), authentication);
        AppUser user = users.findById(dto.caregiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        requireEligibleUser(user);
        if (profiles.existsById(dto.caregiverId())) {
            throw new ConflictException("El cuidador ya tiene un perfil");
        }
        CaregiverProfile profile = mapper.dtoToEntity(dto);
        profile.setUser(user);
        return mapper.entityToDto(profiles.saveAndFlush(profile));
    }

    @Transactional
    public CaregiverProfileResponse update(Long id, CaregiverProfileUpdateRequest dto, Authentication authentication) {
        access.requireOwnerOrAdmin(id, authentication);
        CaregiverProfile profile = requireProfile(id);
        if ("Y".equals(dto.activeFlag())) requireEligibleUser(profile.getUser());
        mapper.dtoToEntity(dto, profile);
        return mapper.entityToDto(profiles.saveAndFlush(profile));
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        access.requireOwnerOrAdmin(id, authentication);
        profiles.delete(requireProfile(id));
        profiles.flush();
    }

    private boolean hasCaregiverRole(Long userId) {
        return userRoles.findByUser_Id(userId).stream().anyMatch(assignment ->
                "CAREGIVER".equals(assignment.getRole().getRoleName())
                        && "Y".equals(assignment.getRole().getActiveFlag()));
    }

    private boolean isPublic(CaregiverProfile profile) {
        return "Y".equals(profile.getActiveFlag()) && "ACTIVE".equals(profile.getUser().getAccountStatus())
                && hasCaregiverRole(profile.getId());
    }

    private void requireEligibleUser(AppUser user) {
        if (!"ACTIVE".equals(user.getAccountStatus()) || !hasCaregiverRole(user.getId())) {
            throw new ConflictException("Se requiere un usuario activo con el rol CAREGIVER activo");
        }
    }

    private CaregiverProfile requireProfile(Long id) {
        return profiles.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de cuidador no encontrado"));
    }
}
