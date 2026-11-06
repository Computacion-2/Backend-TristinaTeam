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
@RequestMapping("/api/v1/role-permissions")
@RequiredArgsConstructor
@Tag(name = "role-permissions")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Identificadores inválidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso de administración"),
        @ApiResponse(responseCode = "404", description = "Asociación o entidad no encontrada"),
        @ApiResponse(responseCode = "409", description = "Duplicado o regla del negocio")
})
public class ApiRolePermissionController {
    private final AssociationService service;

    @GetMapping
    @Operation(summary = "Consultar todas las asociaciones RolePermission")
    public List<RolePermissionResponse> findAll() {
        return service.findRolePermissions();
    }

    @GetMapping("/{roleId}/{permissionId}")
    @Operation(summary = "Consultar asociación por su clave compuesta")
    public RolePermissionResponse findById(@PathVariable @Positive Long roleId,
                                     @PathVariable @Positive Long permissionId) {
        return service.findRolePermission(roleId, permissionId);
    }

    @PostMapping
    @Operation(summary = "Crear asociación RolePermission")
    @ApiResponse(responseCode = "201", description = "Asociación creada")
    public ResponseEntity<RolePermissionResponse> create(@Valid @RequestBody RolePermissionRequest dto) {
        var response = service.createRolePermission(dto);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{roleId}/{permissionId}").buildAndExpand(response.roleId(), response.permissionId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{roleId}/{permissionId}")
    @Operation(summary = "Sustituir asociación de forma transaccional",
            description = "La URL identifica la asociación anterior y el cuerpo la nueva. Si una regla falla, no se cambia ninguna.")
    public RolePermissionResponse update(@PathVariable @Positive Long roleId,
                                    @PathVariable @Positive Long permissionId,
                                    @Valid @RequestBody RolePermissionRequest dto) {
        return service.updateRolePermission(roleId, permissionId, dto);
    }

    @DeleteMapping("/{roleId}/{permissionId}")
    @Operation(summary = "Eliminar asociación conservando las reglas de roles y permisos")
    @ApiResponse(responseCode = "204", description = "Asociación eliminada")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long roleId,
                                      @PathVariable @Positive Long permissionId) {
        service.deleteRolePermission(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }
}
