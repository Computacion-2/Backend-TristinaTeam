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
public class AppRoleService {

    // ADMIN administra el módulo y OWNER se asigna en el registro público.
    public static final Set<String> SYSTEM_ROLES = Set.of("ADMIN", "OWNER");

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String NAME_PATTERN = "[A-Z][A-Z0-9_]{0,59}";

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

    // Consultar los permisos asignados a un rol
    @Transactional(readOnly = true)
    public List<AppPermission> findPermissions(Long roleId) {
        findById(roleId);
        return rolePermissionRepository.findByRole_Id(roleId).stream()
                .map(RolePermission::getPermission)
                .toList();
    }

    // Crear un rol con sus permisos
    @Transactional
    public AppRole create(AppRole role, List<Long> permissionIds) {

        // No permitir roles sin permisos
        if (permissionIds == null || permissionIds.isEmpty()) {
            throw new RuntimeException(
                    "El rol debe tener al menos un permiso");
        }

        String name = normalizeName(role.getRoleName());

        // Evitar nombres duplicados
        if (roleRepository.existsByRoleName(name)) {
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

        role.setRoleName(name);
        role.setDescription(normalizeDescription(role.getDescription()));
        role.setActiveFlag(normalizeActiveFlag(role.getActiveFlag(), "Y"));

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
        String name = normalizeName(data.getRoleName());
        String activeFlag = normalizeActiveFlag(data.getActiveFlag(), role.getActiveFlag());

        // Los roles del sistema conservan su nombre y permanecen activos
        if (isSystemRole(role)
                && (!role.getRoleName().equals(name) || !"Y".equals(activeFlag))) {
            throw new RuntimeException(
                    "El rol " + role.getRoleName() + " no se puede renombrar ni desactivar");
        }

        // Comprobar que el nombre no esté repetido
        if (!role.getRoleName().equals(name)
                && roleRepository.existsByRoleName(name)) {
            throw new RuntimeException(
                    "Ya existe un rol con ese nombre");
        }

        role.setRoleName(name);
        role.setDescription(normalizeDescription(data.getDescription()));
        role.setActiveFlag(activeFlag);

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

        AppRole role = findById(roleId);

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

        // ADMIN debe conservar los permisos que dan acceso a la administración
        if (ADMIN_ROLE.equals(role.getRoleName())
                && permissions.stream()
                .map(RolePermission::getPermission)
                .anyMatch(permission -> permissionId.equals(permission.getId())
                        && AppPermissionService.SYSTEM_PERMISSIONS
                        .contains(permission.getPermissionCode()))) {
            throw new RuntimeException(
                    "El rol ADMIN debe conservar los permisos de administración");
        }

        rolePermissionRepository.deleteById(id);
    }

    // Eliminar un rol
    @Transactional
    public void delete(Long id) {

        AppRole role = findById(id);

        if (isSystemRole(role)) {
            throw new RuntimeException(
                    "No se puede eliminar el rol " + role.getRoleName());
        }

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

    public boolean isSystemRole(AppRole role) {
        return role.getRoleName() != null && SYSTEM_ROLES.contains(role.getRoleName());
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio");
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches(NAME_PATTERN)) {
            throw new IllegalArgumentException(
                    "El nombre del rol debe empezar por una letra y solo admite letras, números y guion bajo");
        }
        return normalized;
    }

    private String normalizeActiveFlag(String activeFlag, String defaultValue) {
        if (activeFlag == null || activeFlag.isBlank()) {
            return defaultValue;
        }
        if (!"Y".equals(activeFlag) && !"N".equals(activeFlag)) {
            throw new IllegalArgumentException("El estado del rol debe ser Y o N");
        }
        return activeFlag;
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
