package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.ServiceType;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface ServiceTypeMapper {
    ServiceTypeResponse entityToDto(ServiceType entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "caregiverServices", ignore = true)
    @Mapping(target = "activeFlag", ignore = true)
    ServiceType dtoToEntity(ServiceTypeCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "caregiverServices", ignore = true)
    void dtoToEntity(ServiceTypeUpdateRequest dto, @MappingTarget ServiceType entity);
}
