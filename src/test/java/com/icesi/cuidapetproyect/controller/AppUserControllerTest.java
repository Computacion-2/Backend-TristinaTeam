package com.icesi.cuidapetproyect.controller;

import com.icesi.cuidapetproyect.model.AppUser;
import com.icesi.cuidapetproyect.service.AppUserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserControllerTest {

    @Mock
    private AppUserService userService;

    @InjectMocks
    private AppUserController controller;

    private AppUser user;

    @BeforeEach
    void setUp() {

        user = new AppUser();

        user.setId(1L);
        user.setEmail("usuario.prueba@icesi.edu.co");
        user.setPasswordHash("hash_de_prueba");
        user.setAccountStatus("ACTIVE");
    }

    // 1. Consultar todos los usuarios.
    @Test
    void findAllTest() {

        when(userService.findAll())
                .thenReturn(List.of(user));

        List<AppUser> result = controller.findAll();

        assertEquals(1, result.size());
        assertEquals("usuario.prueba@icesi.edu.co",
                result.get(0).getEmail());

        verify(userService).findAll();
    }

    // 2. Consultar un usuario por ID.
    @Test
    void findByIdTest() {

        when(userService.findById(1L))
                .thenReturn(user);

        AppUser result = controller.findById(1L);

        assertEquals(1L, result.getId());

        verify(userService).findById(1L);
    }

    // 3. Crear un usuario con roles.
    @Test
    void createTest() {

        List<Long> roleIds = List.of(1L, 2L);

        when(userService.create(user, roleIds))
                .thenReturn(user);

        AppUser result = controller.create(user, roleIds);

        assertEquals("usuario.prueba@icesi.edu.co",
                result.getEmail());

        verify(userService).create(user, roleIds);
    }

    // 4. Actualizar un usuario.
    @Test
    void updateTest() {

        when(userService.update(1L, user))
                .thenReturn(user);

        AppUser result = controller.update(1L, user);

        assertEquals("ACTIVE", result.getAccountStatus());

        verify(userService).update(1L, user);
    }

    // 5. Eliminar lógicamente un usuario.
    @Test
    void deleteTest() {

        controller.delete(1L);

        verify(userService).delete(1L);
    }

    // 6. Asignar un rol a un usuario.
    @Test
    void assignRoleTest() {

        controller.assignRole(1L, 2L);

        verify(userService).assignRole(1L, 2L);
    }

    // 7. Retirar un rol de un usuario.
    @Test
    void removeRoleTest() {

        controller.removeRole(1L, 2L);

        verify(userService).removeRole(1L, 2L);
    }
}