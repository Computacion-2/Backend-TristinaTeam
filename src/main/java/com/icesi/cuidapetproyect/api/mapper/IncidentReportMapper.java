package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.IncidentReport;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface IncidentReportMapper {
    @Mapping(target = "reporterId", source = "reporter.id")
    @Mapping(target = "reportedUserId", source = "reportedUser.id")
    @Mapping(target = "requestId", source = "request.id")
    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "resolvedById", source = "resolvedBy.id")
    IncidentReportResponse entityToDto(IncidentReport entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reporter", ignore = true)
    @Mapping(target = "reportedUser", ignore = true)
    @Mapping(target = "request", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "reportStatus", ignore = true)
    @Mapping(target = "resolutionNotes", ignore = true)
    @Mapping(target = "resolvedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    IncidentReport dtoToEntity(IncidentReportCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reporter", ignore = true)
    @Mapping(target = "reportedUser", ignore = true)
    @Mapping(target = "request", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "resolvedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    void dtoToEntity(IncidentReportUpdateRequest dto, @MappingTarget IncidentReport entity);
}
