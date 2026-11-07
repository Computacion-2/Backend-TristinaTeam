package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.BookingStatus;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface BookingStatusMapper {
    BookingStatusResponse entityToDto(BookingStatus entity);

    @Mapping(target = "bookings", ignore = true)
    BookingStatus dtoToEntity(BookingStatusCreateRequest dto);

    @Mapping(target = "statusCode", ignore = true)
    @Mapping(target = "bookings", ignore = true)
    void dtoToEntity(BookingStatusUpdateRequest dto, @MappingTarget BookingStatus entity);
}
