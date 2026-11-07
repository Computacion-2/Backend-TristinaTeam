package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.CareUpdate;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface CareUpdateMapper {
    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "caregiverId", source = "caregiver.id")
    CareUpdateResponse entityToDto(CareUpdate entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "caregiver", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "photos", ignore = true)
    CareUpdate dtoToEntity(CareUpdateCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "caregiver", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "photos", ignore = true)
    void dtoToEntity(CareUpdateUpdateRequest dto, @MappingTarget CareUpdate entity);
}
