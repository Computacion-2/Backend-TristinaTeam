package com.icesi.cuidapetproyect.api.controller;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.AppRoleMapper;
import com.icesi.cuidapetproyect.service.AppRoleService;
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
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "roles")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso de administración"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado"),
        @ApiResponse(responseCode = "409", description = "Duplicado o regla del negocio")
})
public class ApiRoleController {
    private final AppRoleService service;
    private final AppRoleMapper mapper;

    @GetMapping
    @Operation(summary = "Consultar todos los roles")
    public List<RoleResponse> findAll() {
        return service.findAll().stream().map(mapper::entityToDto).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar Role por identificador")
    public RoleResponse findById(@PathVariable @Positive Long id) {
        return mapper.entityToDto(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear Role")
    @ApiResponse(responseCode = "201", description = "Recurso creado; Location contiene su URL")
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleCreateRequest dto) {
        var entity = service.create(mapper.dtoToEntity(dto), dto.permissionIds());
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(entity.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.entityToDto(entity));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar Role")
    public RoleResponse update(@PathVariable @Positive Long id,
                                  @Valid @RequestBody RoleUpdateRequest dto) {
        return mapper.entityToDto(service.update(id, mapper.dtoToEntity(dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar Role")
    @ApiResponse(responseCode = "204", description = "Operación completada sin cuerpo")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
