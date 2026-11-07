package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.CareUpdatePhoto;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface CareUpdatePhotoMapper {
    @Mapping(target = "updateId", source = "careUpdate.id")
    CareUpdatePhotoResponse entityToDto(CareUpdatePhoto entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "careUpdate", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    CareUpdatePhoto dtoToEntity(CareUpdatePhotoCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "careUpdate", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    void dtoToEntity(CareUpdatePhotoUpdateRequest dto, @MappingTarget CareUpdatePhoto entity);
}
