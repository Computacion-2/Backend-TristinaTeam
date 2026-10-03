package com.icesi.cuidapetproyect.service;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppRoleService {

    private final AppRoleRepository roleRepository;
    private final AppPermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;

    // Consultar todos los roles
    public List<AppRole> findAll() {
        return roleRepository.findAll();
    }

    // Buscar un rol por su ID
    public AppRole findById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Rol no encontrado"));
    }

    // Crear un rol con sus permisos
    @Transactional
    public AppRole create(AppRole role, List<Long> permissionIds) {

        // No permitir roles sin permisos
        if (permissionIds == null || permissionIds.isEmpty()) {
            throw new RuntimeException(
                    "El rol debe tener al menos un permiso");
        }

        // Evitar nombres duplicados
        if (roleRepository.existsByRoleName(role.getRoleName())) {
            throw new RuntimeException(
                    "Ya existe un rol con ese nombre");
        }

        // Comprobar que todos los permisos existen
        List<AppPermission> permissions = permissionIds.stream()
                .distinct()
                .map(id -> permissionRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Permiso no encontrado: " + id)))
                .toList();

        // Guardar el rol
        AppRole savedRole = roleRepository.save(role);

        // Asignar los permisos
        for (AppPermission permission : permissions) {

            RolePermission rolePermission = new RolePermission();

            rolePermission.setRole(savedRole);
            rolePermission.setPermission(permission);

            rolePermissionRepository.save(rolePermission);
        }

        return savedRole;
    }

    // Actualizar la información de un rol
    @Transactional
    public AppRole update(Long id, AppRole data) {

        AppRole role = findById(id);

        // Comprobar que el nombre no esté repetido
        if (!role.getRoleName().equals(data.getRoleName())
                && roleRepository.existsByRoleName(data.getRoleName())) {
            throw new RuntimeException(
                    "Ya existe un rol con ese nombre");
        }

        role.setRoleName(data.getRoleName());
        role.setDescription(data.getDescription());
        role.setActiveFlag(data.getActiveFlag());

        return roleRepository.save(role);
    }

    // Asignar un permiso a un rol
    @Transactional
    public void assignPermission(Long roleId, Long permissionId) {

        AppRole role = findById(roleId);

        AppPermission permission = permissionRepository
                .findById(permissionId)
                .orElseThrow(() ->
                        new RuntimeException("Permiso no encontrado"));

        RolePermissionId id =
                new RolePermissionId(roleId, permissionId);

        // Evitar permisos duplicados
        if (rolePermissionRepository.existsById(id)) {
            throw new RuntimeException(
                    "El rol ya tiene este permiso");
        }

        RolePermission rolePermission = new RolePermission();

        rolePermission.setRole(role);
        rolePermission.setPermission(permission);

        rolePermissionRepository.save(rolePermission);
    }

    // Quitar un permiso de un rol
    @Transactional
    public void removePermission(Long roleId, Long permissionId) {

        findById(roleId);

        List<RolePermission> permissions =
                rolePermissionRepository.findByRole_Id(roleId);

        RolePermissionId id =
                new RolePermissionId(roleId, permissionId);

        // Comprobar que el rol tenga el permiso
        if (!rolePermissionRepository.existsById(id)) {
            throw new RuntimeException(
                    "El rol no tiene este permiso");
        }

        // No permitir que el rol se quede sin permisos
        if (permissions.size() <= 1) {
            throw new RuntimeException(
                    "No se puede dejar el rol sin permisos");
        }

        rolePermissionRepository.deleteById(id);
    }

    // Eliminar un rol
    @Transactional
    public void delete(Long id) {

        AppRole role = findById(id);

        // Impedir eliminar un rol asignado a usuarios
        if (!userRoleRepository.findByRole_Id(id).isEmpty()) {
            throw new RuntimeException(
                    "No se puede eliminar un rol asignado a usuarios");
        }

        // Eliminar primero sus relaciones con los permisos
        List<RolePermission> permissions =
                rolePermissionRepository.findByRole_Id(id);

        rolePermissionRepository.deleteAll(permissions);
        rolePermissionRepository.flush();

        // Eliminar el rol
        roleRepository.delete(role);
    }
}