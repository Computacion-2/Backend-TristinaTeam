package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.Pet;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface PetMapper {
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "speciesId", source = "species.id")
    PetResponse entityToDto(Pet entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "species", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "photos", ignore = true)
    @Mapping(target = "petStatus", ignore = true)
    Pet dtoToEntity(PetCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "species", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "photos", ignore = true)
    void dtoToEntity(PetUpdateRequest dto, @MappingTarget Pet entity);
}
