package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.PetPhoto;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface PetPhotoMapper {
    @Mapping(target = "petId", source = "pet.id")
    PetPhotoResponse entityToDto(PetPhoto entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    PetPhoto dtoToEntity(PetPhotoCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    void dtoToEntity(PetPhotoUpdateRequest dto, @MappingTarget PetPhoto entity);
}
