package com.icesi.cuidapetproyect.api.controller;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.service.AssociationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/user-roles")
@RequiredArgsConstructor
@Tag(name = "user-roles")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Identificadores inválidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso de administración"),
        @ApiResponse(responseCode = "404", description = "Asociación o entidad no encontrada"),
        @ApiResponse(responseCode = "409", description = "Duplicado o regla del negocio")
})
public class ApiUserRoleController {
    private final AssociationService service;

    @GetMapping
    @Operation(summary = "Consultar todas las asociaciones UserRole")
    public List<UserRoleResponse> findAll() {
        return service.findUserRoles();
    }

    @GetMapping("/{userId}/{roleId}")
    @Operation(summary = "Consultar asociación por su clave compuesta")
    public UserRoleResponse findById(@PathVariable @Positive Long userId,
                                     @PathVariable @Positive Long roleId) {
        return service.findUserRole(userId, roleId);
    }

    @PostMapping
    @Operation(summary = "Crear asociación UserRole")
    @ApiResponse(responseCode = "201", description = "Asociación creada")
    public ResponseEntity<UserRoleResponse> create(@Valid @RequestBody UserRoleRequest dto, Principal principal) {
        var response = service.createUserRole(dto, principal.getName());
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{userId}/{roleId}").buildAndExpand(response.userId(), response.roleId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{userId}/{roleId}")
    @Operation(summary = "Sustituir asociación de forma transaccional",
            description = "La URL identifica la asociación anterior y el cuerpo la nueva. Si una regla falla, no se cambia ninguna.")
    public UserRoleResponse update(@PathVariable @Positive Long userId,
                                    @PathVariable @Positive Long roleId,
                                    @Valid @RequestBody UserRoleRequest dto, Principal principal) {
        return service.updateUserRole(userId, roleId, dto, principal.getName());
    }

    @DeleteMapping("/{userId}/{roleId}")
    @Operation(summary = "Eliminar asociación conservando las reglas de roles y permisos")
    @ApiResponse(responseCode = "204", description = "Asociación eliminada")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long userId,
                                      @PathVariable @Positive Long roleId) {
        service.deleteUserRole(userId, roleId);
        return ResponseEntity.noContent().build();
    }
}
