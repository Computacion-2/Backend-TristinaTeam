package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.ServiceTypeMapper;
import com.icesi.cuidapetproyect.api.security.ResourceAccess;
import com.icesi.cuidapetproyect.exception.*;
import com.icesi.cuidapetproyect.model.ServiceType;
import com.icesi.cuidapetproyect.repository.ServiceTypeRepository;
import com.icesi.cuidapetproyect.repository.CaregiverServiceRepository;
import com.icesi.cuidapetproyect.repository.CareRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceTypeService {
    private final ServiceTypeRepository services;
    private final CaregiverServiceRepository caregiverServices;
    private final CareRequestRepository requests;
    private final ServiceTypeMapper mapper;
    private final ResourceAccess access;

    public List<ServiceTypeResponse> findAll() {
        return services.findAll().stream().map(mapper::entityToDto).toList();
    }

    public ServiceTypeResponse findById(Long id) {
        return mapper.entityToDto(requireService(id));
    }

    @Transactional
    public ServiceTypeResponse create(ServiceTypeCreateRequest dto, Authentication authentication) {
        access.requireAdmin(authentication);
        String name = dto.serviceName().trim();
        if (services.existsByServiceNameIgnoreCase(name)) {
            throw new ConflictException("Ya existe un tipo de servicio con ese nombre");
        }
        ServiceType entity = mapper.dtoToEntity(dto);
        entity.setServiceName(name);
        return mapper.entityToDto(services.saveAndFlush(entity));
    }

    @Transactional
    public ServiceTypeResponse update(Long id, ServiceTypeUpdateRequest dto, Authentication authentication) {
        access.requireAdmin(authentication);
        ServiceType entity = requireService(id);
        String name = dto.serviceName().trim();
        if (services.existsByServiceNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Ya existe un tipo de servicio con ese nombre");
        }
        mapper.dtoToEntity(dto, entity);
        entity.setServiceName(name);
        return mapper.entityToDto(services.saveAndFlush(entity));
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        access.requireAdmin(authentication);
        ServiceType entity = requireService(id);
        if (caregiverServices.existsByServiceType_Id(id) || requests.existsByServiceType_Id(id)) {
            throw new ConflictException("No se puede eliminar un tipo de servicio utilizado por cuidadores o solicitudes");
        }
        services.delete(entity);
        // Oracle impide borrar servicios usados por cuidadores o solicitudes (409).
        services.flush();
    }

    private ServiceType requireService(Long id) {
        return services.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado"));
    }
}
