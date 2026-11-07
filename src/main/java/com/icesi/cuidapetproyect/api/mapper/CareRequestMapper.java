package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.CareRequest;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface CareRequestMapper {
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "petId", source = "pet.id")
    @Mapping(target = "serviceTypeId", source = "serviceType.id")
    CareRequestResponse entityToDto(CareRequest entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "serviceType", ignore = true)
    @Mapping(target = "requestStatus", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "applications", ignore = true)
    @Mapping(target = "booking", ignore = true)
    CareRequest dtoToEntity(CareRequestCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "serviceType", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "applications", ignore = true)
    @Mapping(target = "booking", ignore = true)
    void dtoToEntity(CareRequestUpdateRequest dto, @MappingTarget CareRequest entity);
}
