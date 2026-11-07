package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.*;
import com.icesi.cuidapetproyect.api.security.*;
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
public class FavoriteCaregiverService {
    private final FavoriteCaregiverRepository repository;
    private final AppUserRepository users;
    private final FavoriteCaregiverMapper mapper;
    private final ResourceAccess access;
    private final CaregiverAccess caregivers;

    public List<FavoriteCaregiverResponse> findAll(Authentication auth) {
        var entities = access.isAdmin(auth) ? repository.findAll()
                : repository.findByOwner_Id(access.currentUser(auth).getId());
        return entities.stream().map(mapper::entityToDto).toList();
    }

    public FavoriteCaregiverResponse findById(Long ownerId, Long caregiverId, Authentication auth) {
        access.requireOwnerOrAdmin(ownerId, auth);
        return mapper.entityToDto(require(ownerId, caregiverId));
    }

    @Transactional
    public FavoriteCaregiverResponse create(FavoriteCaregiverCreateRequest dto, Authentication auth) {
        access.requireOwnerOrAdmin(dto.ownerId(), auth);
        requireDifferentPeople(dto.ownerId(), dto.caregiverId());
        var owner = users.findById(dto.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!"ACTIVE".equals(owner.getAccountStatus())) throw new ConflictException("El usuario debe estar activo");
        var profile = caregivers.requireAvailable(dto.caregiverId());
        if (repository.existsById(new FavoriteCaregiverId(dto.ownerId(), dto.caregiverId()))) {
            throw new ConflictException("El cuidador ya está en favoritos");
        }
        var entity = mapper.dtoToEntity(dto);
        entity.setOwner(owner);
        entity.setCaregiver(profile);
        return mapper.entityToDto(repository.saveAndFlush(entity));
    }

    @Transactional
    public FavoriteCaregiverResponse update(Long ownerId, Long caregiverId,
                                            FavoriteCaregiverUpdateRequest dto, Authentication auth) {
        access.requireOwnerOrAdmin(ownerId, auth);
        var previous = require(ownerId, caregiverId);
        requireDifferentPeople(ownerId, dto.caregiverId());
        var profile = caregivers.requireAvailable(dto.caregiverId());
        if (caregiverId.equals(dto.caregiverId())) return mapper.entityToDto(previous);
        if (repository.existsById(new FavoriteCaregiverId(ownerId, dto.caregiverId()))) {
            throw new ConflictException("El nuevo cuidador ya está en favoritos");
        }
        // La clave compuesta no se modifica: se reemplaza la asociación en una sola transacción.
        var replacement = mapper.dtoToEntity(new FavoriteCaregiverCreateRequest(ownerId, dto.caregiverId()));
        replacement.setOwner(previous.getOwner());
        replacement.setCaregiver(profile);
        replacement.setCreatedAt(previous.getCreatedAt());
        replacement = repository.saveAndFlush(replacement);
        repository.delete(previous);
        repository.flush();
        return mapper.entityToDto(replacement);
    }

    @Transactional
    public void delete(Long ownerId, Long caregiverId, Authentication auth) {
        access.requireOwnerOrAdmin(ownerId, auth);
        repository.delete(require(ownerId, caregiverId));
        repository.flush();
    }

    private FavoriteCaregiver require(Long ownerId, Long caregiverId) {
        return repository.findById(new FavoriteCaregiverId(ownerId, caregiverId))
                .orElseThrow(() -> new ResourceNotFoundException("Favorito no encontrado"));
    }

    private void requireDifferentPeople(Long ownerId, Long caregiverId) {
        if (ownerId.equals(caregiverId)) {
            throw new ConflictException("No puede agregar su propio perfil a favoritos");
        }
    }
}
