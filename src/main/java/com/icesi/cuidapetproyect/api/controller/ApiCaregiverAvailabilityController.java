package com.icesi.cuidapetproyect.api.controller;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.service.CaregiverAvailabilityService;
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
@RequestMapping("/api/v1/caregiver-availabilities")
@RequiredArgsConstructor
@Tag(name = "Disponibilidad de cuidadores")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso sobre el recurso"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado"),
        @ApiResponse(responseCode = "409", description = "Duplicado o relación inactiva")
})
public class ApiCaregiverAvailabilityController {
    private final CaregiverAvailabilityService service;

    @GetMapping
    @Operation(summary = "Listar disponibilidad de cuidadores visibles para el usuario")
    public List<CaregiverAvailabilityResponse> findAll(Authentication auth) {
        return service.findAll(auth);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar CaregiverAvailability por su identificador")
    public CaregiverAvailabilityResponse findById(@PathVariable @Positive Long id, Authentication auth) {
        return service.findById(id, auth);
    }

    @PostMapping
    @Operation(summary = "Crear CaregiverAvailability")
    @ApiResponse(responseCode = "201", description = "Recurso creado; URL en Location")
    public ResponseEntity<CaregiverAvailabilityResponse> create(@Valid @RequestBody CaregiverAvailabilityCreateRequest dto,
                                                      Authentication auth) {
        var response = service.create(dto, auth);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar CaregiverAvailability conservando su identificador")
    public CaregiverAvailabilityResponse update(@PathVariable @Positive Long id,
                                    @Valid @RequestBody CaregiverAvailabilityUpdateRequest dto, Authentication auth) {
        return service.update(id, dto, auth);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar CaregiverAvailability")
    @ApiResponse(responseCode = "204", description = "Recurso eliminado")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id, Authentication auth) {
        service.delete(id, auth);
        return ResponseEntity.noContent().build();
    }
}
