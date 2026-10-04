package com.icesi.cuidapetproyect.controller;

import com.icesi.cuidapetproyect.model.AppPermission;
import com.icesi.cuidapetproyect.service.AppPermissionService;

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
class AppPermissionControllerTest {

    @Mock
    private AppPermissionService permissionService;

    @InjectMocks
    private AppPermissionController controller;

    private AppPermission permission;

    @BeforeEach
    void setUp() {

        permission = new AppPermission();

        permission.setId(1L);
        permission.setPermissionCode("TEST_PERMISSION");
        permission.setPermissionName("Permiso de prueba");
        permission.setDescription("Descripción de prueba");
    }

    // 1. Probar la consulta de todos los permisos.
    @Test
    void findAllTest() {

        when(permissionService.findAll())
                .thenReturn(List.of(permission));

        List<AppPermission> result = controller.findAll();

        assertEquals(1, result.size());

        verify(permissionService).findAll();
    }

    // 2. Probar la consulta de un permiso por ID.
    @Test
    void findByIdTest() {

        when(permissionService.findById(1L))
                .thenReturn(permission);

        AppPermission result = controller.findById(1L);

        assertEquals(1L, result.getId());

        verify(permissionService).findById(1L);
    }

    // 3. Probar la creación de un permiso.
    @Test
    void createTest() {

        when(permissionService.create(permission))
                .thenReturn(permission);

        AppPermission result = controller.create(permission);

        assertEquals("TEST_PERMISSION", result.getPermissionCode());

        verify(permissionService).create(permission);
    }

    // 4. Probar la actualización de un permiso.
    @Test
    void updateTest() {

        when(permissionService.update(1L, permission))
                .thenReturn(permission);

        AppPermission result = controller.update(1L, permission);

        assertEquals("Permiso de prueba", result.getPermissionName());

        verify(permissionService).update(1L, permission);
    }

    // 5. Probar la eliminación de un permiso.
    @Test
    void deleteTest() {

        controller.delete(1L);

        verify(permissionService).delete(1L);
    }
}