package com.icesi.cuidapetproyect.api.controller;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.service.CaregiverProfileService;
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
@RequestMapping("/api/v1/caregiver-profiles")
@RequiredArgsConstructor
@Tag(name = "Perfiles de cuidadores", description = "Directorio activo; escritura propia o ADMIN")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso sobre el recurso"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado"),
        @ApiResponse(responseCode = "409", description = "Duplicado o conflicto de relaciones")
})
public class ApiCaregiverProfileController {
    private final CaregiverProfileService service;

    @GetMapping
    @Operation(summary = "Consultar perfiles de cuidadores disponibles")
    public List<CaregiverProfileResponse> findAll(Authentication authentication) {
        return service.findAll(authentication);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar CaregiverProfile por identificador")
    public CaregiverProfileResponse findById(@PathVariable @Positive Long id, Authentication authentication) {
        return service.findById(id, authentication);
    }

    @PostMapping
    @Operation(summary = "Crear CaregiverProfile")
    @ApiResponse(responseCode = "201", description = "Recurso creado; URL en Location")
    public ResponseEntity<CaregiverProfileResponse> create(@Valid @RequestBody CaregiverProfileCreateRequest dto,
                                                  Authentication authentication) {
        var response = service.create(dto, authentication);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.caregiverId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar CaregiverProfile")
    public CaregiverProfileResponse update(@PathVariable @Positive Long id,
                                   @Valid @RequestBody CaregiverProfileUpdateRequest dto,
                                   Authentication authentication) {
        return service.update(id, dto, authentication);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar CaregiverProfile")
    @ApiResponse(responseCode = "204", description = "Recurso eliminado")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id, Authentication authentication) {
        service.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
