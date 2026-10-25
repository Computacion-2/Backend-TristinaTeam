package com.icesi.cuidapetproyect.controller;

import com.icesi.cuidapetproyect.exception.GlobalExceptionHandler;
import com.icesi.cuidapetproyect.model.AppRole;
import com.icesi.cuidapetproyect.service.AppRoleService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AppRoleControllerHttpTest {

    private MockMvc mockMvc;
    private AppRoleService roleService;

    @BeforeEach
    void setUp() {

        roleService = mock(AppRoleService.class);

        AppRoleController controller =
                new AppRoleController(roleService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // 1. Consultar todos los roles.
    @Test
    void findAllHttpTest() throws Exception {

        AppRole role = new AppRole();
        role.setId(1L);
        role.setRoleName("TEST_CAREGIVER");

        when(roleService.findAll())
                .thenReturn(List.of(role));

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].roleName")
                        .value("TEST_CAREGIVER"));

        verify(roleService).findAll();
    }

    // 2. Consultar un rol por ID.
    @Test
    void findByIdHttpTest() throws Exception {

        AppRole role = new AppRole();
        role.setId(1L);
        role.setRoleName("TEST_CAREGIVER");

        when(roleService.findById(1L))
                .thenReturn(role);

        mockMvc.perform(get("/api/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.roleName")
                        .value("TEST_CAREGIVER"));

        verify(roleService).findById(1L);
    }

    // 3. Crear un rol con permisos.
    @Test
    void createHttpTest() throws Exception {

        AppRole role = new AppRole();
        role.setId(1L);
        role.setRoleName("TEST_CAREGIVER");
        role.setDescription("Rol de prueba");
        role.setActiveFlag("Y");

        when(roleService.create(
                any(AppRole.class),
                eq(List.of(2L, 3L))
        )).thenReturn(role);

        mockMvc.perform(post("/api/roles/create")
                        .param("permissionIds", "2", "3")
                        .contentType("application/json")
                        .content("""
                                {
                                    "roleName": "TEST_CAREGIVER",
                                    "description": "Rol de prueba",
                                    "activeFlag": "Y"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.roleName")
                        .value("TEST_CAREGIVER"));

        verify(roleService).create(
                any(AppRole.class),
                eq(List.of(2L, 3L))
        );
    }

    // 4. Actualizar un rol.
    @Test
    void updateHttpTest() throws Exception {

        AppRole role = new AppRole();
        role.setId(1L);
        role.setRoleName("ROL_ACTUALIZADO");
        role.setDescription("Descripción actualizada");

        when(roleService.update(
                eq(1L),
                any(AppRole.class)
        )).thenReturn(role);

        mockMvc.perform(post("/api/roles/update/1")
                        .contentType("application/json")
                        .content("""
                                {
                                    "roleName": "ROL_ACTUALIZADO",
                                    "description": "Descripción actualizada"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleName")
                        .value("ROL_ACTUALIZADO"));

        verify(roleService).update(
                eq(1L),
                any(AppRole.class)
        );
    }

    // 5. Eliminar un rol.
    @Test
    void deleteHttpTest() throws Exception {

        mockMvc.perform(post("/api/roles/delete/1"))
                .andExpect(status().isOk());

        verify(roleService).delete(1L);
    }

    // 6. Asignar un permiso a un rol.
    @Test
    void assignPermissionHttpTest() throws Exception {

        mockMvc.perform(
                post("/api/roles/1/assign-permission/3")
        ).andExpect(status().isOk());

        verify(roleService).assignPermission(1L, 3L);
    }

    // 7. Retirar un permiso de un rol.
    @Test
    void removePermissionHttpTest() throws Exception {

        mockMvc.perform(
                post("/api/roles/1/remove-permission/3")
        ).andExpect(status().isOk());

        verify(roleService).removePermission(1L, 3L);
    }

    // 8. Consultar un rol que no existe: HTTP 404.
    @Test
    void notFoundHttpTest() throws Exception {

        when(roleService.findById(999L))
                .thenThrow(
                        new RuntimeException("Rol no encontrado")
                );

        mockMvc.perform(get("/api/roles/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Rol no encontrado"));
    }

    // 9. Intentar retirar el último permiso: HTTP 409.
    @Test
    void removeLastPermissionHttpTest() throws Exception {

        doThrow(new RuntimeException(
                "No se puede retirar el último permiso"
        )).when(roleService).removePermission(1L, 3L);

        mockMvc.perform(
                        post("/api/roles/1/remove-permission/3")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("No se puede retirar el último permiso"));
    }
}