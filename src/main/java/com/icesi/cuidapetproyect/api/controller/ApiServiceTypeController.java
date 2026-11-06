package com.icesi.cuidapetproyect.api.controller;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.service.ServiceTypeService;
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
@RequestMapping("/api/v1/service-types")
@RequiredArgsConstructor
@Tag(name = "Tipos de servicio", description = "Lectura autenticada; escritura ADMIN")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso sobre el recurso"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado"),
        @ApiResponse(responseCode = "409", description = "Duplicado o conflicto de relaciones")
})
public class ApiServiceTypeController {
    private final ServiceTypeService service;

    @GetMapping
    @Operation(summary = "Consultar tipos de servicio disponibles")
    public List<ServiceTypeResponse> findAll(Authentication authentication) {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar ServiceType por identificador")
    public ServiceTypeResponse findById(@PathVariable @Positive Long id, Authentication authentication) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Crear ServiceType")
    @ApiResponse(responseCode = "201", description = "Recurso creado; URL en Location")
    public ResponseEntity<ServiceTypeResponse> create(@Valid @RequestBody ServiceTypeCreateRequest dto,
                                                  Authentication authentication) {
        var response = service.create(dto, authentication);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar ServiceType")
    public ServiceTypeResponse update(@PathVariable @Positive Long id,
                                   @Valid @RequestBody ServiceTypeUpdateRequest dto,
                                   Authentication authentication) {
        return service.update(id, dto, authentication);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar ServiceType")
    @ApiResponse(responseCode = "204", description = "Recurso eliminado")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id, Authentication authentication) {
        service.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
