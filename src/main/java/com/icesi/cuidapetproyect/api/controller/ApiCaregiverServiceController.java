package com.icesi.cuidapetproyect.api.controller;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.service.CaregiverOfferingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.List;

@RestController
@RequestMapping("/api/v1/caregiver-services")
@RequiredArgsConstructor
@Tag(name = "Servicios ofrecidos por cuidadores")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso sobre el recurso"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado"),
        @ApiResponse(responseCode = "409", description = "Duplicado o relación inactiva")
})
public class ApiCaregiverServiceController {
    private final CaregiverOfferingService service;

    @GetMapping
    @Operation(summary = "Listar servicios ofrecidos por cuidadores visibles para el usuario")
    public List<CaregiverServiceResponse> findAll(Authentication auth) {
        return service.findAll(auth);
    }

    @GetMapping("/{caregiverId}/{serviceTypeId}")
    @Operation(summary = "Consultar CaregiverService por su identificador")
    public CaregiverServiceResponse findById(@PathVariable @Positive Long caregiverId, @PathVariable @Positive Long serviceTypeId, Authentication auth) {
        return service.findById(caregiverId, serviceTypeId, auth);
    }

    @PostMapping
    @Operation(summary = "Crear CaregiverService")
    @ApiResponse(responseCode = "201", description = "Recurso creado; URL en Location")
    public ResponseEntity<CaregiverServiceResponse> create(@Valid @RequestBody CaregiverServiceCreateRequest dto,
                                                      Authentication auth) {
        var response = service.create(dto, auth);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{caregiverId}/{serviceTypeId}")
                .buildAndExpand(response.caregiverId(), response.serviceTypeId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{caregiverId}/{serviceTypeId}")
    @Operation(summary = "Actualizar CaregiverService conservando su identificador")
    public CaregiverServiceResponse update(@PathVariable @Positive Long caregiverId, @PathVariable @Positive Long serviceTypeId,
                                    @Valid @RequestBody CaregiverServiceUpdateRequest dto, Authentication auth) {
        return service.update(caregiverId, serviceTypeId, dto, auth);
    }

    @DeleteMapping("/{caregiverId}/{serviceTypeId}")
    @Operation(summary = "Eliminar CaregiverService")
    @ApiResponse(responseCode = "204", description = "Recurso eliminado")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long caregiverId, @PathVariable @Positive Long serviceTypeId, Authentication auth) {
        service.delete(caregiverId, serviceTypeId, auth);
        return ResponseEntity.noContent().build();
    }
}
