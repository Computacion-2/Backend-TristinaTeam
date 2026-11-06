package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.PetSpecies;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface PetSpeciesMapper {
    PetSpeciesResponse entityToDto(PetSpecies entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pets", ignore = true)
    @Mapping(target = "activeFlag", ignore = true)
    PetSpecies dtoToEntity(PetSpeciesCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pets", ignore = true)
    void dtoToEntity(PetSpeciesUpdateRequest dto, @MappingTarget PetSpecies entity);
}
