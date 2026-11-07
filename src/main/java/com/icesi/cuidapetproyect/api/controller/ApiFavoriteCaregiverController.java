package com.icesi.cuidapetproyect.api.controller;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.service.FavoriteCaregiverService;
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
@RequestMapping("/api/v1/favorite-caregivers")
@RequiredArgsConstructor
@Tag(name = "Cuidadores favoritos")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso sobre el recurso"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado"),
        @ApiResponse(responseCode = "409", description = "Duplicado o relación inactiva")
})
public class ApiFavoriteCaregiverController {
    private final FavoriteCaregiverService service;

    @GetMapping
    @Operation(summary = "Listar cuidadores favoritos visibles para el usuario")
    public List<FavoriteCaregiverResponse> findAll(Authentication auth) {
        return service.findAll(auth);
    }

    @GetMapping("/{ownerId}/{caregiverId}")
    @Operation(summary = "Consultar FavoriteCaregiver por su identificador")
    public FavoriteCaregiverResponse findById(@PathVariable @Positive Long ownerId, @PathVariable @Positive Long caregiverId, Authentication auth) {
        return service.findById(ownerId, caregiverId, auth);
    }

    @PostMapping
    @Operation(summary = "Crear FavoriteCaregiver")
    @ApiResponse(responseCode = "201", description = "Recurso creado; URL en Location")
    public ResponseEntity<FavoriteCaregiverResponse> create(@Valid @RequestBody FavoriteCaregiverCreateRequest dto,
                                                      Authentication auth) {
        var response = service.create(dto, auth);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{ownerId}/{caregiverId}")
                .buildAndExpand(response.ownerId(), response.caregiverId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{ownerId}/{caregiverId}")
    @Operation(summary = "Reemplazar el cuidador favorito conservando el propietario")
    public FavoriteCaregiverResponse update(@PathVariable @Positive Long ownerId, @PathVariable @Positive Long caregiverId,
                                    @Valid @RequestBody FavoriteCaregiverUpdateRequest dto, Authentication auth) {
        return service.update(ownerId, caregiverId, dto, auth);
    }

    @DeleteMapping("/{ownerId}/{caregiverId}")
    @Operation(summary = "Eliminar FavoriteCaregiver")
    @ApiResponse(responseCode = "204", description = "Recurso eliminado")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long ownerId, @PathVariable @Positive Long caregiverId, Authentication auth) {
        service.delete(ownerId, caregiverId, auth);
        return ResponseEntity.noContent().build();
    }
}
