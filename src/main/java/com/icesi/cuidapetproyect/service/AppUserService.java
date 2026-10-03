package com.icesi.cuidapetproyect.service;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository userRepository;
    private final AppRoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    // Consultar todos los usuarios
    public List<AppUser> findAll() {
        return userRepository.findAll();
    }

    // Buscar un usuario por ID
    public AppUser findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Usuario no encontrado"));
    }

    // Crear un usuario con sus roles
    @Transactional
    public AppUser create(AppUser user, List<Long> roleIds) {

        // Un usuario debe tener al menos un rol
        if (roleIds == null || roleIds.isEmpty()) {
            throw new RuntimeException(
                    "El usuario debe tener al menos un rol");
        }

        // No permitir correos duplicados
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException(
                    "El correo ya está registrado");
        }

        // Comprobar que todos los roles existen
        List<AppRole> roles = roleIds.stream()
                .distinct()
                .map(id -> roleRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Rol no encontrado: " + id)))
                .toList();

        // Comprobar que los roles tienen permisos
        for (AppRole role : roles) {
            if (rolePermissionRepository
                    .findByRole_Id(role.getId()).isEmpty()) {
                throw new RuntimeException(
                        "El rol no tiene permisos: "
                                + role.getRoleName());
            }
        }

        // Guardar el usuario
        AppUser savedUser = userRepository.save(user);

        // Asignar los roles
        for (AppRole role : roles) {

            UserRole userRole = new UserRole();

            userRole.setUser(savedUser);
            userRole.setRole(role);

            userRoleRepository.save(userRole);
        }

        return savedUser;
    }

    // Actualizar los datos de un usuario
    @Transactional
    public AppUser update(Long id, AppUser data) {

        AppUser user = findById(id);

        // Comprobar que el correo no pertenece a otro usuario
        if (!user.getEmail().equals(data.getEmail())
                && userRepository.existsByEmail(data.getEmail())) {
            throw new RuntimeException(
                    "El correo ya está registrado");
        }

        user.setEmail(data.getEmail());
        user.setPasswordHash(data.getPasswordHash());
        user.setAccountStatus(data.getAccountStatus());

        return userRepository.save(user);
    }

    // Asignar un rol a un usuario
    @Transactional
    public void assignRole(Long userId, Long roleId) {

        AppUser user = findById(userId);

        AppRole role = roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new RuntimeException("Rol no encontrado"));

        // No asignar roles sin permisos
        if (rolePermissionRepository
                .findByRole_Id(roleId).isEmpty()) {
            throw new RuntimeException(
                    "No se puede asignar un rol sin permisos");
        }

        UserRoleId id = new UserRoleId(userId, roleId);

        // Evitar asignaciones duplicadas
        if (userRoleRepository.existsById(id)) {
            throw new RuntimeException(
                    "El usuario ya tiene este rol");
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);

        userRoleRepository.save(userRole);
    }

    // Quitar un rol a un usuario
    @Transactional
    public void removeRole(Long userId, Long roleId) {

        findById(userId);

        UserRoleId id = new UserRoleId(userId, roleId);

        // Verificar que el usuario tenga ese rol
        if (!userRoleRepository.existsById(id)) {
            throw new RuntimeException(
                    "El usuario no tiene este rol");
        }

        List<UserRole> roles =
                userRoleRepository.findByUser_Id(userId);

        // Impedir que se quede sin roles
        if (roles.size() <= 1) {
            throw new RuntimeException(
                    "No se puede dejar al usuario sin roles");
        }

        userRoleRepository.deleteById(id);
    }


    // Eliminar un usuario de forma lógica
    @Transactional
    public void delete(Long id) {

        // Comprobar que el usuario existe
        AppUser user = findById(id);

        // Evitar eliminarlo dos veces
        if ("DELETED".equals(user.getAccountStatus())) {
            throw new RuntimeException(
                    "El usuario ya está eliminado");
        }

        // Cambiar su estado sin borrar sus relaciones
        user.setAccountStatus("DELETED");

        // Guardar el cambio
        userRepository.save(user);
    }

}