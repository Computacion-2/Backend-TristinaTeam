package com.icesi.cuidapetproyect.api.controller;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.AppPermissionMapper;
import com.icesi.cuidapetproyect.service.AppPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "permissions")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso de administración"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado"),
        @ApiResponse(responseCode = "409", description = "Duplicado o regla del negocio")
})
public class ApiPermissionController {
    private final AppPermissionService service;
    private final AppPermissionMapper mapper;

    @GetMapping
    @Operation(summary = "Consultar todos los permissions")
    public List<PermissionResponse> findAll() {
        return service.findAll().stream().map(mapper::entityToDto).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar Permission por identificador")
    public PermissionResponse findById(@PathVariable @Positive Long id) {
        return mapper.entityToDto(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear Permission")
    @ApiResponse(responseCode = "201", description = "Recurso creado; Location contiene su URL")
    public ResponseEntity<PermissionResponse> create(@Valid @RequestBody PermissionCreateRequest dto) {
        var entity = service.create(mapper.dtoToEntity(dto));
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(entity.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.entityToDto(entity));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar Permission")
    public PermissionResponse update(@PathVariable @Positive Long id,
                                  @Valid @RequestBody PermissionUpdateRequest dto) {
        return mapper.entityToDto(service.update(id, mapper.dtoToEntity(dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar Permission")
    @ApiResponse(responseCode = "204", description = "Operación completada sin cuerpo")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
