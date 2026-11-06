package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.UserProfileMapper;
import com.icesi.cuidapetproyect.api.security.ResourceAccess;
import com.icesi.cuidapetproyect.exception.*;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {
    private final UserProfileRepository profiles;
    private final AppUserRepository users;
    private final UserProfileMapper mapper;
    private final ResourceAccess access;

    public List<UserProfileResponse> findAll(Authentication authentication) {
        if (access.isAdmin(authentication)) {
            return profiles.findAll().stream().map(mapper::entityToDto).toList();
        }
        return profiles.findById(access.currentUser(authentication).getId())
                .stream().map(mapper::entityToDto).toList();
    }

    public UserProfileResponse findById(Long userId, Authentication authentication) {
        access.requireOwnerOrAdmin(userId, authentication);
        return mapper.entityToDto(requireProfile(userId));
    }

    @Transactional
    public UserProfileResponse create(UserProfileCreateRequest dto, Authentication authentication) {
        access.requireOwnerOrAdmin(dto.userId(), authentication);
        AppUser user = users.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new ConflictException("El usuario debe estar activo para crear su perfil");
        }
        if (profiles.existsById(dto.userId())) {
            throw new ConflictException("El usuario ya tiene un perfil");
        }
        UserProfile profile = mapper.dtoToEntity(dto);
        profile.setUser(user);
        return mapper.entityToDto(profiles.saveAndFlush(profile));
    }

    @Transactional
    public UserProfileResponse update(Long userId, UserProfileUpdateRequest dto, Authentication authentication) {
        access.requireOwnerOrAdmin(userId, authentication);
        UserProfile profile = requireProfile(userId);
        mapper.dtoToEntity(dto, profile);
        return mapper.entityToDto(profiles.saveAndFlush(profile));
    }

    @Transactional
    public void delete(Long userId, Authentication authentication) {
        access.requireOwnerOrAdmin(userId, authentication);
        profiles.delete(requireProfile(userId));
        profiles.flush();
    }

    private UserProfile requireProfile(Long userId) {
        return profiles.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));
    }
}
