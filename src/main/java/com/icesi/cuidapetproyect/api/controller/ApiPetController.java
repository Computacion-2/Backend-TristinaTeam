package com.icesi.cuidapetproyect.api.controller;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.service.PetService;
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
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
@Tag(name = "mascotas", description = "Propietario o ADMIN")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso sobre el recurso"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado"),
        @ApiResponse(responseCode = "409", description = "Duplicado o conflicto de relaciones")
})
public class ApiPetController {
    private final PetService service;

    @GetMapping
    @Operation(summary = "Consultar mascotas visibles para el usuario")
    public List<PetResponse> findAll(Authentication authentication) {
        return service.findAll(authentication);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar Pet por identificador")
    public PetResponse findById(@PathVariable @Positive Long id, Authentication authentication) {
        return service.findById(id, authentication);
    }

    @PostMapping
    @Operation(summary = "Crear Pet")
    @ApiResponse(responseCode = "201", description = "Recurso creado; Location contiene su URL")
    public ResponseEntity<PetResponse> create(@Valid @RequestBody PetCreateRequest dto,
                                                  Authentication authentication) {
        var response = service.create(dto, authentication);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar Pet")
    public PetResponse update(@PathVariable @Positive Long id,
                                   @Valid @RequestBody PetUpdateRequest dto,
                                   Authentication authentication) {
        return service.update(id, dto, authentication);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar Pet")
    @ApiResponse(responseCode = "204", description = "Recurso eliminado")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id, Authentication authentication) {
        service.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
