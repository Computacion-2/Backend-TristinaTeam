package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.CaregiverAvailability;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface CaregiverAvailabilityMapper {
    @Mapping(target = "caregiverId", source = "caregiver.id")
    CaregiverAvailabilityResponse entityToDto(CaregiverAvailability entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caregiver", ignore = true)
    @Mapping(target = "activeFlag", ignore = true)
    CaregiverAvailability dtoToEntity(CaregiverAvailabilityCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caregiver", ignore = true)
    void dtoToEntity(CaregiverAvailabilityUpdateRequest dto, @MappingTarget CaregiverAvailability entity);
}
