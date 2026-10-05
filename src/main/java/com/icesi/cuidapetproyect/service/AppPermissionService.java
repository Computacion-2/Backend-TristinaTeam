package com.icesi.cuidapetproyect.service;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppPermissionService {

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

        // No permitir nombres duplicados
        boolean exists = permissionRepository.findAll()
                .stream()
                .anyMatch(p -> p.getPermissionName()
                        .equalsIgnoreCase(permission.getPermissionName()));

        if (exists) {
            throw new RuntimeException(
                    "Ya existe un permiso con ese nombre");
        }

        return permissionRepository.save(permission);
    }

    // Actualizar un permiso
    @Transactional
    public AppPermission update(Long id, AppPermission data) {

        AppPermission permission = findById(id);

        // Comprobar si el nuevo nombre ya pertenece a otro permiso
        boolean exists = permissionRepository.findAll()
                .stream()
                .anyMatch(p -> !p.getId().equals(id)
                        && p.getPermissionName()
                        .equalsIgnoreCase(data.getPermissionName()));

        if (exists) {
            throw new RuntimeException(
                    "Ya existe otro permiso con ese nombre");
        }

        permission.setPermissionName(data.getPermissionName());
        permission.setDescription(data.getDescription());

        return permissionRepository.save(permission);
    }

    // Eliminar un permiso
    @Transactional
    public void delete(Long id) {

        AppPermission permission = findById(id);

        // Buscar los roles que tienen asignado el permiso
        List<RolePermission> assignments =
                rolePermissionRepository.findAll()
                        .stream()
                        .filter(rp -> rp.getPermission().getId().equals(id))
                        .toList();

        // Verificar que ningún rol se quede sin permisos
        for (RolePermission assignment : assignments) {

            Long roleId = assignment.getRole().getId();

            long totalPermissions = rolePermissionRepository.findAll()
                    .stream()
                    .filter(rp -> rp.getRole().getId().equals(roleId))
                    .count();

            if (totalPermissions <= 1) {
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
}
