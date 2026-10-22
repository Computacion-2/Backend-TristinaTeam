package com.icesi.cuidapetproyect.service;

import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {
    private final AppUserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (UserRole assignment : userRoleRepository.findByUser_Id(user.getId())) {
            AppRole role = assignment.getRole();
            if (!"Y".equals(role.getActiveFlag())) {
                continue;
            }
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleName()));
            for (RolePermission permission : rolePermissionRepository.findByRole_Id(role.getId())) {
                authorities.add(new SimpleGrantedAuthority(permission.getPermission().getPermissionCode()));
            }
        }
        // User implementa UserDetails; getAuthorities() devuelve los roles y permisos cargados.
        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(!"ACTIVE".equals(user.getAccountStatus()))
                .authorities(authorities)
                .build();
    }
}
