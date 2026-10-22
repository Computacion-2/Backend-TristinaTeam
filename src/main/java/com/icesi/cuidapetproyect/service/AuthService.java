package com.icesi.cuidapetproyect.service;

import com.icesi.cuidapetproyect.model.AppRole;
import com.icesi.cuidapetproyect.model.AppUser;
import com.icesi.cuidapetproyect.repository.AppRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserService userService;
    private final AppRoleRepository roleRepository;

    @Transactional
    public AppUser register(String email, String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
        // El registro público siempre asigna OWNER, nunca un rol elegido por el visitante.
        AppRole role = roleRepository.findByRoleName("OWNER")
                .filter(value -> "Y".equals(value.getActiveFlag()))
                .orElseThrow(() -> new IllegalStateException("El rol OWNER no está disponible"));
        AppUser user = new AppUser();
        user.setEmail(email.trim());
        user.setPasswordHash(password);
        user.setAccountStatus("ACTIVE");
        // AppUserService aplica BCrypt antes de guardar y comprueba los permisos del rol.
        return userService.create(user, List.of(role.getId()));
    }
}
