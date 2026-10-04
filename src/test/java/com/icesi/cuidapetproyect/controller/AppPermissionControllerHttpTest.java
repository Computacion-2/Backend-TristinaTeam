package com.icesi.cuidapetproyect.controller;

import com.icesi.cuidapetproyect.model.AppPermission;
import com.icesi.cuidapetproyect.service.AppPermissionService;
import com.icesi.cuidapetproyect.exception.GlobalExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AppPermissionControllerHttpTest {

    private MockMvc mockMvc;
    private AppPermissionService permissionService;

    @BeforeEach
    void setUp() {

        permissionService = mock(AppPermissionService.class);

        AppPermissionController controller =
                new AppPermissionController(permissionService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // 1. Consultar todos los permisos.
    @Test
    void findAllHttpTest() throws Exception {

        mockMvc.perform(get("/permissions"))
                .andExpect(status().isOk());

        verify(permissionService).findAll();
    }

    // 2. Consultar un permiso por ID.
    @Test
    void findByIdHttpTest() throws Exception {

        AppPermission permission = new AppPermission();
        permission.setId(1L);
        permission.setPermissionCode("TEST");
        permission.setPermissionName("Permiso de prueba");

        when(permissionService.findById(1L))
                .thenReturn(permission);

        mockMvc.perform(get("/permissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.permissionCode").value("TEST"));
    }

    // 3. Crear un permiso.
    @Test
    void createHttpTest() throws Exception {

        AppPermission permission = new AppPermission();
        permission.setId(1L);
        permission.setPermissionCode("TEST");
        permission.setPermissionName("Permiso de prueba");

        when(permissionService.create(any(AppPermission.class)))
                .thenReturn(permission);

        mockMvc.perform(post("/permissions/create")
                        .contentType("application/json")
                        .content("""
                                {
                                    "permissionCode": "TEST",
                                    "permissionName": "Permiso de prueba",
                                    "description": "Descripción de prueba"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionCode").value("TEST"));
    }

    // 4. Actualizar un permiso.
    @Test
    void updateHttpTest() throws Exception {

        AppPermission permission = new AppPermission();
        permission.setId(1L);
        permission.setPermissionCode("TEST");
        permission.setPermissionName("Permiso actualizado");

        when(permissionService.update(eq(1L), any(AppPermission.class)))
                .thenReturn(permission);

        mockMvc.perform(post("/permissions/update/1")
                        .contentType("application/json")
                        .content("""
                                {
                                    "permissionName": "Permiso actualizado",
                                    "description": "Nueva descripción"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionName")
                        .value("Permiso actualizado"));
    }

    // 5. Eliminar un permiso.
    @Test
    void deleteHttpTest() throws Exception {

        mockMvc.perform(post("/permissions/delete/1"))
                .andExpect(status().isOk());

        verify(permissionService).delete(1L);
    }

    // 6. Consultar un permiso que no existe.
    @Test
    void notFoundHttpTest() throws Exception {

        when(permissionService.findById(999L))
                .thenThrow(new RuntimeException("Permiso no encontrado"));

        mockMvc.perform(get("/permissions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Permiso no encontrado"));
    }
}