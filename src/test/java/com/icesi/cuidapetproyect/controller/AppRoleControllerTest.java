package com.icesi.cuidapetproyect.controller;

import com.icesi.cuidapetproyect.model.AppRole;
import com.icesi.cuidapetproyect.service.AppRoleService;

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
class AppRoleControllerTest {

    @Mock
    private AppRoleService roleService;

    @InjectMocks
    private AppRoleController controller;

    private AppRole role;

    @BeforeEach
    void setUp() {

        role = new AppRole();

        role.setId(1L);
        role.setRoleName("TEST_CAREGIVER");
        role.setDescription("Rol de prueba");
        role.setActiveFlag("Y");
    }

    // 1. Consultar todos los roles.
    @Test
    void findAllTest() {

        when(roleService.findAll())
                .thenReturn(List.of(role));

        List<AppRole> result = controller.findAll();

        assertEquals(1, result.size());
        verify(roleService).findAll();
    }

    // 2. Consultar un rol por ID.
    @Test
    void findByIdTest() {

        when(roleService.findById(1L))
                .thenReturn(role);

        AppRole result = controller.findById(1L);

        assertEquals(1L, result.getId());
        verify(roleService).findById(1L);
    }

    // 3. Crear un rol con permisos.
    @Test
    void createTest() {

        List<Long> permissionIds = List.of(2L, 3L);

        when(roleService.create(role, permissionIds))
                .thenReturn(role);

        AppRole result = controller.create(role, permissionIds);

        assertEquals("TEST_CAREGIVER", result.getRoleName());

        verify(roleService).create(role, permissionIds);
    }

    // 4. Actualizar un rol.
    @Test
    void updateTest() {

        when(roleService.update(1L, role))
                .thenReturn(role);

        AppRole result = controller.update(1L, role);

        assertEquals("Rol de prueba", result.getDescription());

        verify(roleService).update(1L, role);
    }

    // 5. Eliminar un rol.
    @Test
    void deleteTest() {

        controller.delete(1L);

        verify(roleService).delete(1L);
    }

    // 6. Asignar un permiso a un rol.
    @Test
    void assignPermissionTest() {

        controller.assignPermission(1L, 3L);

        verify(roleService).assignPermission(1L, 3L);
    }

    // 7. Retirar un permiso de un rol.
    @Test
    void removePermissionTest() {

        controller.removePermission(1L, 3L);

        verify(roleService).removePermission(1L, 3L);
    }
}