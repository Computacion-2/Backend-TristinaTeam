package com.icesi.cuidapetproyect.controller;
import com.icesi.cuidapetproyect.model.AppUser;
import com.icesi.cuidapetproyect.service.AppUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService userService;

    // Consultar todos los usuarios
    @GetMapping
    public List<AppUser> findAll() {
        return userService.findAll();
    }

    // Consultar un usuario por su ID
    @GetMapping("/{id}")
    public AppUser findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    // Crear un usuario con sus roles
    @PostMapping("/create")
    public AppUser create(
            @Valid @RequestBody AppUser user,
            @RequestParam List<Long> roleIds) {

        return userService.create(user, roleIds);
    }

    @PostMapping("/update/{id}")
    public AppUser update(
            @PathVariable Long id,
            @Valid @RequestBody AppUser user) {

        return userService.update(id, user);
    }

    // Eliminar un usuario de forma lógica
    @PostMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    // Asignar un rol a un usuario
    @PostMapping("/{userId}/assign-role/{roleId}")
    public void assignRole(
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        userService.assignRole(userId, roleId);
    }

    // Retirar un rol de un usuario
    @PostMapping("/{userId}/remove-role/{roleId}")
    public void removeRole(
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        userService.removeRole(userId, roleId);
    }
}