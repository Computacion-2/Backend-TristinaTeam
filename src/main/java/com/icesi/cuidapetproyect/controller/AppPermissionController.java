package com.icesi.cuidapetproyect.controller;
import com.icesi.cuidapetproyect.model.AppPermission;
import com.icesi.cuidapetproyect.service.AppPermissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class AppPermissionController {

    private final AppPermissionService permissionService;

    // Consultar todos los permisos
    @GetMapping
    public List<AppPermission> findAll() {
        return permissionService.findAll();
    }

    // Consultar un permiso por ID
    @GetMapping("/{id}")
    public AppPermission findById(@PathVariable Long id) {
        return permissionService.findById(id);
    }

    @PostMapping("/create")
    public AppPermission create(@Valid @RequestBody AppPermission permission) {
        return permissionService.create(permission);
    }

    @PostMapping("/update/{id}")
    public AppPermission update(
            @PathVariable Long id,
            @RequestBody AppPermission permission) {

        return permissionService.update(id, permission);
    }

    // Eliminar un permiso
    @PostMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        permissionService.delete(id);
    }
}