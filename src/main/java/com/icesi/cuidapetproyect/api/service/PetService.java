package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.PetMapper;
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
public class PetService {
    private final PetRepository pets;
    private final PetSpeciesRepository species;
    private final AppUserRepository users;
    private final PetMapper mapper;
    private final ResourceAccess access;

    public List<PetResponse> findAll(Authentication authentication) {
        List<Pet> visible = access.isAdmin(authentication) ? pets.findAll()
                : pets.findByOwner_Id(access.currentUser(authentication).getId());
        return visible.stream().map(mapper::entityToDto).toList();
    }

    public PetResponse findById(Long id, Authentication authentication) {
        return mapper.entityToDto(requireOwnedPet(id, authentication));
    }

    @Transactional
    public PetResponse create(PetCreateRequest dto, Authentication authentication) {
        access.requireOwnerOrAdmin(dto.ownerId(), authentication);
        AppUser owner = users.findById(dto.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("Propietario no encontrado"));
        if (!"ACTIVE".equals(owner.getAccountStatus())) {
            throw new ConflictException("El propietario debe estar activo");
        }
        PetSpecies petSpecies = requireSpecies(dto.speciesId());
        requireActiveSpecies(petSpecies);
        Pet pet = mapper.dtoToEntity(dto);
        pet.setOwner(owner);
        pet.setSpecies(petSpecies);
        return mapper.entityToDto(pets.saveAndFlush(pet));
    }

    @Transactional
    public PetResponse update(Long id, PetUpdateRequest dto, Authentication authentication) {
        Pet pet = requireOwnedPet(id, authentication);
        PetSpecies petSpecies = requireSpecies(dto.speciesId());
        if (!pet.getSpecies().getId().equals(petSpecies.getId())) {
            requireActiveSpecies(petSpecies);
        }
        // El propietario y los campos de auditoría no se reciben en el DTO de actualización.
        mapper.dtoToEntity(dto, pet);
        pet.setSpecies(petSpecies);
        return mapper.entityToDto(pets.saveAndFlush(pet));
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        pets.delete(requireOwnedPet(id, authentication));
        // Las referencias de solicitudes existentes impiden el borrado y producen 409.
        pets.flush();
    }

    private Pet requireOwnedPet(Long id, Authentication authentication) {
        Pet pet = pets.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        access.requireOwnerOrAdmin(pet.getOwner().getId(), authentication);
        return pet;
    }

    private PetSpecies requireSpecies(Long id) {
        return species.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especie no encontrada"));
    }

    private void requireActiveSpecies(PetSpecies petSpecies) {
        if (!"Y".equals(petSpecies.getActiveFlag())) {
            throw new ConflictException("No se puede asignar una especie inactiva");
        }
    }
}
