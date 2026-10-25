package com.icesi.cuidapetproyect.service;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppPermissionService {

    // Permisos que usa SecurityConfig; eliminarlos dejaría el módulo sin administración.
    public static final Set<String> SYSTEM_PERMISSIONS = Set.of("MANAGE_USERS", "MANAGE_ROLES");

    private static final String CODE_PATTERN = "[A-Z][A-Z0-9_]{0,99}";

    private final AppPermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    // Consultar todos los permisos
    public List<AppPermission> findAll() {
        return permissionRepository.findAll();
    }

    // Buscar un permiso por su ID
    public AppPermission findById(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Permiso no encontrado"));
    }

    // Crear un permiso
    @Transactional
    public AppPermission create(AppPermission permission) {

        String code = normalizeCode(permission.getPermissionCode());
        String name = normalizeName(permission.getPermissionName());

        // El código es la autoridad que recibe Spring Security y debe ser único
        if (permissionRepository.existsByPermissionCode(code)) {
            throw new RuntimeException(
                    "Ya existe un permiso con ese código");
        }

        // No permitir nombres duplicados
        if (permissionRepository.existsByPermissionNameIgnoreCase(name)) {
            throw new RuntimeException(
                    "Ya existe un permiso con ese nombre");
        }

        permission.setPermissionCode(code);
        permission.setPermissionName(name);
        permission.setDescription(normalizeDescription(permission.getDescription()));

        return permissionRepository.save(permission);
    }

    // Actualizar el nombre y la descripción de un permiso; el código no cambia
    @Transactional
    public AppPermission update(Long id, AppPermission data) {

        AppPermission permission = findById(id);
        String name = normalizeName(data.getPermissionName());

        // Comprobar si el nuevo nombre ya pertenece a otro permiso
        if (permissionRepository.existsByPermissionNameIgnoreCaseAndIdNot(name, id)) {
            throw new RuntimeException(
                    "Ya existe otro permiso con ese nombre");
        }

        permission.setPermissionName(name);
        permission.setDescription(normalizeDescription(data.getDescription()));

        return permissionRepository.save(permission);
    }

    // Eliminar un permiso
    @Transactional
    public void delete(Long id) {

        AppPermission permission = findById(id);

        if (SYSTEM_PERMISSIONS.contains(permission.getPermissionCode())) {
            throw new RuntimeException(
                    "No se puede eliminar un permiso requerido por el sistema");
        }

        // Buscar los roles que tienen asignado el permiso
        List<RolePermission> assignments =
                rolePermissionRepository.findByPermission_Id(id);

        // Verificar que ningún rol se quede sin permisos
        for (RolePermission assignment : assignments) {

            Long roleId = assignment.getRole().getId();

            if (rolePermissionRepository.findByRole_Id(roleId).size() <= 1) {
                throw new RuntimeException(
                        "No se puede eliminar el último permiso de un rol");
            }
        }

        // Eliminar primero las relaciones con los roles
        rolePermissionRepository.deleteAll(assignments);
        rolePermissionRepository.flush();

        // Eliminar el permiso
        permissionRepository.delete(permission);
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código del permiso es obligatorio");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches(CODE_PATTERN)) {
            throw new IllegalArgumentException(
                    "El código debe empezar por una letra y solo admite letras, números y guion bajo");
        }
        return normalized;
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del permiso es obligatorio");
        }
        return name.trim();
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
