package com.icesi.cuidapetproyect.service;

import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.AppRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AppUserService userService;
    @Mock AppRoleRepository roleRepository;
    @InjectMocks AuthService authService;

    @Test
    void registerAlwaysUsesOwnerAndActiveStatus() {
        AppRole owner = new AppRole();
        owner.setId(3L);
        owner.setRoleName("OWNER");
        when(roleRepository.findByRoleName("OWNER")).thenReturn(Optional.of(owner));
        when(userService.create(any(), eq(List.of(3L))))
                .thenAnswer(invocation -> invocation.getArgument(0));
        AppUser result = authService.register(" persona@icesi.edu.co ", "Clave1234");
        assertEquals("persona@icesi.edu.co", result.getEmail());
        assertEquals("ACTIVE", result.getAccountStatus());
        verify(userService).create(any(AppUser.class), eq(List.of(3L)));
    }

    @Test
    void registerRejectsShortOrMissingPassword() {
        assertThrows(IllegalArgumentException.class, () -> authService.register("a@b.co", "123"));
        assertThrows(IllegalArgumentException.class, () -> authService.register("a@b.co", null));
        verifyNoInteractions(userService, roleRepository);
    }

    @Test
    void registerRejectsMissingOwner() {
        assertThrows(IllegalStateException.class, () -> authService.register("a@b.co", "Clave1234"));
        verifyNoInteractions(userService);
    }

    @Test
    void registerRejectsInactiveOwner() {
        AppRole owner = new AppRole();
        owner.setActiveFlag("N");
        when(roleRepository.findByRoleName("OWNER")).thenReturn(Optional.of(owner));
        assertThrows(IllegalStateException.class, () -> authService.register("a@b.co", "Clave1234"));
        verifyNoInteractions(userService);
    }
}
