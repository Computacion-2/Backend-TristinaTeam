package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.CareApplication;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface CareApplicationMapper {
    @Mapping(target = "requestId", source = "request.id")
    @Mapping(target = "caregiverId", source = "caregiver.id")
    CareApplicationResponse entityToDto(CareApplication entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "request", ignore = true)
    @Mapping(target = "caregiver", ignore = true)
    @Mapping(target = "applicationStatus", ignore = true)
    @Mapping(target = "responseReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "booking", ignore = true)
    CareApplication dtoToEntity(CareApplicationCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "request", ignore = true)
    @Mapping(target = "caregiver", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "booking", ignore = true)
    void dtoToEntity(CareApplicationUpdateRequest dto, @MappingTarget CareApplication entity);
}
