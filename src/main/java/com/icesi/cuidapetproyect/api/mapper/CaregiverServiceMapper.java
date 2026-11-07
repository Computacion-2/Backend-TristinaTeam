package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.CaregiverService;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface CaregiverServiceMapper {
    @Mapping(target = "caregiverId", source = "caregiver.id")
    @Mapping(target = "serviceTypeId", source = "serviceType.id")
    CaregiverServiceResponse entityToDto(CaregiverService entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caregiver", ignore = true)
    @Mapping(target = "serviceType", ignore = true)
    @Mapping(target = "activeFlag", ignore = true)
    CaregiverService dtoToEntity(CaregiverServiceCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caregiver", ignore = true)
    @Mapping(target = "serviceType", ignore = true)
    void dtoToEntity(CaregiverServiceUpdateRequest dto, @MappingTarget CaregiverService entity);
}
