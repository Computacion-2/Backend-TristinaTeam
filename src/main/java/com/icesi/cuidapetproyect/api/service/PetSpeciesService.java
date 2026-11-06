package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.PetSpeciesMapper;
import com.icesi.cuidapetproyect.api.security.ResourceAccess;
import com.icesi.cuidapetproyect.exception.*;
import com.icesi.cuidapetproyect.model.PetSpecies;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetSpeciesService {
    private final PetSpeciesRepository species;
    private final PetRepository pets;
    private final PetSpeciesMapper mapper;
    private final ResourceAccess access;

    public List<PetSpeciesResponse> findAll() {
        return species.findAll().stream().map(mapper::entityToDto).toList();
    }

    public PetSpeciesResponse findById(Long id) {
        return mapper.entityToDto(requireSpecies(id));
    }

    @Transactional
    public PetSpeciesResponse create(PetSpeciesCreateRequest dto, Authentication authentication) {
        access.requireAdmin(authentication);
        String name = dto.speciesName().trim();
        if (species.existsBySpeciesNameIgnoreCase(name)) {
            throw new ConflictException("Ya existe una especie con ese nombre");
        }
        PetSpecies entity = mapper.dtoToEntity(dto);
        entity.setSpeciesName(name);
        return mapper.entityToDto(species.saveAndFlush(entity));
    }

    @Transactional
    public PetSpeciesResponse update(Long id, PetSpeciesUpdateRequest dto, Authentication authentication) {
        access.requireAdmin(authentication);
        PetSpecies entity = requireSpecies(id);
        String name = dto.speciesName().trim();
        if (species.existsBySpeciesNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Ya existe una especie con ese nombre");
        }
        mapper.dtoToEntity(dto, entity);
        entity.setSpeciesName(name);
        return mapper.entityToDto(species.saveAndFlush(entity));
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        access.requireAdmin(authentication);
        PetSpecies entity = requireSpecies(id);
        if (pets.existsBySpecies_Id(id)) {
            throw new ConflictException("No se puede eliminar una especie utilizada por mascotas");
        }
        species.delete(entity);
        species.flush();
    }

    private PetSpecies requireSpecies(Long id) {
        return species.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especie no encontrada"));
    }
}
