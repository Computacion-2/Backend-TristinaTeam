package com.icesi.cuidapetproyect.api.controller;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.AppUserMapper;
import com.icesi.cuidapetproyect.service.AppUserService;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "users")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso de administración"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado"),
        @ApiResponse(responseCode = "409", description = "Duplicado o regla del negocio")
})
public class ApiUserController {
    private final AppUserService service;
    private final AppUserMapper mapper;

    @GetMapping
    @Operation(summary = "Consultar todos los users")
    public List<UserResponse> findAll() {
        return service.findAll().stream().map(mapper::entityToDto).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar User por identificador")
    public UserResponse findById(@PathVariable @Positive Long id) {
        return mapper.entityToDto(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear User")
    @ApiResponse(responseCode = "201", description = "Recurso creado; Location contiene su URL")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest dto) {
        var entity = service.create(mapper.dtoToEntity(dto), dto.roleIds());
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(entity.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.entityToDto(entity));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar User")
    public UserResponse update(@PathVariable @Positive Long id,
                                  @Valid @RequestBody UserUpdateRequest dto) {
        return mapper.entityToDto(service.update(id, mapper.dtoToEntity(dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario de forma lógica (estado DELETED)")
    @ApiResponse(responseCode = "204", description = "Operación completada sin cuerpo")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
