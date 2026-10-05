package com.icesi.cuidapetproyect.controller;
import com.icesi.cuidapetproyect.model.AppRole;
import com.icesi.cuidapetproyect.service.AppRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class AppRoleController {

    private final AppRoleService roleService;

    // Consultar todos los roles
    @GetMapping
    public List<AppRole> findAll() {
        return roleService.findAll();
    }

    // Consultar un rol por ID
    @GetMapping("/{id}")
    public AppRole findById(@PathVariable Long id) {
        return roleService.findById(id);
    }

    // Crear un rol con sus permisos
    @PostMapping("/create")
    public AppRole create(
            @Valid @RequestBody AppRole role,
            @RequestParam List<Long> permissionIds) {

        return roleService.create(role, permissionIds);
    }

    @PostMapping("/update/{id}")
    public AppRole update(
            @PathVariable Long id,
            @Valid @RequestBody AppRole role) {

        return roleService.update(id, role);
    }

    // Eliminar un rol
    @PostMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        roleService.delete(id);
    }

    // Asignar un permiso a un rol
    @PostMapping("/{roleId}/assign-permission/{permissionId}")
    public void assignPermission(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {

        roleService.assignPermission(roleId, permissionId);
    }

    // Retirar un permiso de un rol
    @PostMapping("/{roleId}/remove-permission/{permissionId}")
    public void removePermission(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {

        roleService.removePermission(roleId, permissionId);
    }
}