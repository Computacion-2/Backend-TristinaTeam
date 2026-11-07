package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.CareBooking;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface CareBookingMapper {
    @Mapping(target = "requestId", source = "request.id")
    @Mapping(target = "acceptedApplicationId", source = "acceptedApplication.id")
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "caregiverId", source = "caregiver.id")
    @Mapping(target = "bookingStatusCode", source = "bookingStatus.statusCode")
    CareBookingResponse entityToDto(CareBooking entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "request", ignore = true)
    @Mapping(target = "acceptedApplication", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "caregiver", ignore = true)
    @Mapping(target = "agreedRate", ignore = true)
    @Mapping(target = "bookingStatus", ignore = true)
    @Mapping(target = "ownerConfirmedFlag", ignore = true)
    @Mapping(target = "caregiverDoneFlag", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "acceptedAt", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    CareBooking dtoToEntity(CareBookingCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "request", ignore = true)
    @Mapping(target = "acceptedApplication", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "caregiver", ignore = true)
    @Mapping(target = "agreedRate", ignore = true)
    @Mapping(target = "bookingStatus", ignore = true)
    @Mapping(target = "ownerConfirmedFlag", ignore = true)
    @Mapping(target = "caregiverDoneFlag", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "acceptedAt", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    void dtoToEntity(CareBookingUpdateRequest dto, @MappingTarget CareBooking entity);
}
