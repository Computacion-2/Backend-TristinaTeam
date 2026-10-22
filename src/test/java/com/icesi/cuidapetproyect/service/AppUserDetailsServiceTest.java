package com.icesi.cuidapetproyect.service;

import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.*;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {
    @Mock AppUserRepository userRepository;
    @Mock UserRoleRepository userRoleRepository;
    @Mock RolePermissionRepository rolePermissionRepository;
    @InjectMocks AppUserDetailsService service;
    AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(1L);
        user.setEmail("persona@icesi.edu.co");
        user.setPasswordHash("hash-guardado");
    }

    @Test
    void loadsRolesAndPermissionsWithoutDuplicates() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        AppRole role = new AppRole();
        role.setId(2L);
        role.setRoleName("ADMIN");
        UserRole assignment = new UserRole();
        assignment.setRole(role);
        AppPermission permission = new AppPermission();
        permission.setPermissionCode("MANAGE_USERS");
        RolePermission grant = new RolePermission();
        grant.setPermission(permission);
        when(userRoleRepository.findByUser_Id(1L)).thenReturn(List.of(assignment));
        when(rolePermissionRepository.findByRole_Id(2L)).thenReturn(List.of(grant, grant));
        UserDetails details = service.loadUserByUsername(" persona@icesi.edu.co ");
        assertEquals(user.getPasswordHash(), details.getPassword());
        assertTrue(details.isEnabled());
        assertEquals(2, details.getAuthorities().size());
        assertTrue(details.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .toList().containsAll(List.of("ROLE_ADMIN", "MANAGE_USERS")));
    }

    @Test
    void missingUserCannotAuthenticate() {
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nadie@icesi.edu.co"));
    }

    @Test
    void deletedUserIsDisabled() {
        user.setAccountStatus("DELETED");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        assertFalse(service.loadUserByUsername(user.getEmail()).isEnabled());
    }

    @Test
    void inactiveRolesDoNotGrantAuthorities() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        AppRole role = new AppRole();
        role.setActiveFlag("N");
        UserRole assignment = new UserRole();
        assignment.setRole(role);
        when(userRoleRepository.findByUser_Id(1L)).thenReturn(List.of(assignment));
        assertTrue(service.loadUserByUsername(user.getEmail()).getAuthorities().isEmpty());
        verifyNoInteractions(rolePermissionRepository);
    }
}
