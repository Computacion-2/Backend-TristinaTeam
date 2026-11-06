package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.CaregiverProfile;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface CaregiverProfileMapper {
    @Mapping(target = "caregiverId", source = "id")
    CaregiverProfileResponse entityToDto(CaregiverProfile entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "availabilities", ignore = true)
    @Mapping(target = "activeFlag", ignore = true)
    CaregiverProfile dtoToEntity(CaregiverProfileCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "availabilities", ignore = true)
    void dtoToEntity(CaregiverProfileUpdateRequest dto, @MappingTarget CaregiverProfile entity);
}
