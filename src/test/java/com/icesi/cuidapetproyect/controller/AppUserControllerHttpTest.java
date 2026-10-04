package com.icesi.cuidapetproyect.controller;

import com.icesi.cuidapetproyect.exception.GlobalExceptionHandler;
import com.icesi.cuidapetproyect.model.AppUser;
import com.icesi.cuidapetproyect.service.AppUserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AppUserControllerHttpTest {

    private MockMvc mockMvc;
    private AppUserService userService;

    @BeforeEach
    void setUp() {

        userService = mock(AppUserService.class);

        AppUserController controller =
                new AppUserController(userService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // Crea un usuario de prueba para reutilizarlo.
    private AppUser createTestUser() {

        AppUser user = new AppUser();

        user.setId(1L);
        user.setEmail("usuario.prueba@icesi.edu.co");
        user.setPasswordHash("hash_de_prueba");
        user.setAccountStatus("ACTIVE");

        return user;
    }

    // 1. Consultar todos los usuarios.
    @Test
    void findAllHttpTest() throws Exception {

        AppUser user = createTestUser();

        when(userService.findAll())
                .thenReturn(List.of(user));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email")
                        .value("usuario.prueba@icesi.edu.co"));

        verify(userService).findAll();
    }

    // 2. Consultar un usuario por ID.
    @Test
    void findByIdHttpTest() throws Exception {

        AppUser user = createTestUser();

        when(userService.findById(1L))
                .thenReturn(user);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountStatus")
                        .value("ACTIVE"));

        verify(userService).findById(1L);
    }

    // 3. Crear un usuario con roles.
    @Test
    void createHttpTest() throws Exception {

        AppUser user = createTestUser();

        when(userService.create(
                any(AppUser.class),
                eq(List.of(1L, 2L))
        )).thenReturn(user);

        mockMvc.perform(post("/users/create")
                        .param("roleIds", "1", "2")
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "usuario.prueba@icesi.edu.co",
                                    "passwordHash": "hash_de_prueba",
                                    "accountStatus": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email")
                        .value("usuario.prueba@icesi.edu.co"));

        verify(userService).create(
                any(AppUser.class),
                eq(List.of(1L, 2L))
        );
    }

    // 4. Actualizar un usuario.
    @Test
    void updateHttpTest() throws Exception {

        AppUser user = createTestUser();
        user.setEmail("usuario.actualizado@icesi.edu.co");

        when(userService.update(
                eq(1L),
                any(AppUser.class)
        )).thenReturn(user);

        mockMvc.perform(post("/users/update/1")
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "usuario.actualizado@icesi.edu.co",
                                    "passwordHash": "hash_actualizado",
                                    "accountStatus": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email")
                        .value("usuario.actualizado@icesi.edu.co"));

        verify(userService).update(
                eq(1L),
                any(AppUser.class)
        );
    }

    // 5. Eliminar lógicamente un usuario.
    @Test
    void deleteHttpTest() throws Exception {

        mockMvc.perform(post("/users/delete/1"))
                .andExpect(status().isOk());

        verify(userService).delete(1L);
    }

    // 6. Asignar un rol a un usuario.
    @Test
    void assignRoleHttpTest() throws Exception {

        mockMvc.perform(post("/users/1/assign-role/2"))
                .andExpect(status().isOk());

        verify(userService).assignRole(1L, 2L);
    }

    // 7. Retirar un rol de un usuario.
    @Test
    void removeRoleHttpTest() throws Exception {

        mockMvc.perform(post("/users/1/remove-role/2"))
                .andExpect(status().isOk());

        verify(userService).removeRole(1L, 2L);
    }

    // 8. Consultar un usuario inexistente: HTTP 404.
    @Test
    void notFoundHttpTest() throws Exception {

        when(userService.findById(999L))
                .thenThrow(
                        new RuntimeException("Usuario no encontrado")
                );

        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Usuario no encontrado"));
    }

    // 9. Intentar retirar el último rol: HTTP 409.
    @Test
    void removeLastRoleHttpTest() throws Exception {

        doThrow(new RuntimeException(
                "No se puede retirar el último rol"
        )).when(userService).removeRole(1L, 2L);

        mockMvc.perform(post("/users/1/remove-role/2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("No se puede retirar el último rol"));
    }

    // 10. Comprobar que el JSON no exponga passwordHash.
    @Test
    void passwordIsHiddenHttpTest() throws Exception {

        AppUser user = createTestUser();

        when(userService.findById(1L))
                .thenReturn(user);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email")
                        .value("usuario.prueba@icesi.edu.co"))
                .andExpect(jsonPath("$.passwordHash")
                        .doesNotExist());
    }
}