
package com.icesi.cuidapetproyect.service;

import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppPermissionServiceTest {

    @Mock
    private AppPermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private AppPermissionService permissionService;

    private AppPermission permission;

    @BeforeEach
    void setUp() {
        permission = new AppPermission();
        permission.setId(1L);
        permission.setPermissionName("CREATE_REQUEST");
        permission.setDescription("Crear solicitudes");
    }

    // 1. Consultar todos los permisos
    @Test
    void testFindAll() {

        when(permissionRepository.findAll())
                .thenReturn(List.of(permission));

        List<AppPermission> result = permissionService.findAll();

        assertEquals(1, result.size());
        verify(permissionRepository).findAll();
    }

    // 2. Buscar un permiso existente
    @Test
    void testFindById() {

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        AppPermission result = permissionService.findById(1L);

        assertEquals(1L, result.getId());
    }

    // 3. Buscar un permiso inexistente
    @Test
    void testFindByIdNotFound() {

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> permissionService.findById(1L));
    }

    // 4. Crear un permiso correctamente
    @Test
    void testCreate() {

        when(permissionRepository.findAll())
                .thenReturn(List.of());

        when(permissionRepository.save(permission))
                .thenReturn(permission);

        AppPermission result = permissionService.create(permission);

        assertEquals("CREATE_REQUEST",
                result.getPermissionName());

        verify(permissionRepository).save(permission);
    }

    // 5. No crear un permiso con nombre duplicado
    @Test
    void testCreateDuplicateName() {

        when(permissionRepository.findAll())
                .thenReturn(List.of(permission));

        assertThrows(RuntimeException.class,
                () -> permissionService.create(permission));

        verify(permissionRepository, never()).save(any());
    }

    // 6. Actualizar correctamente un permiso
    @Test
    void testUpdate() {

        AppPermission data = new AppPermission();
        data.setPermissionName("EDIT_REQUEST");
        data.setDescription("Editar solicitudes");

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        when(permissionRepository.findAll())
                .thenReturn(List.of(permission));

        when(permissionRepository.save(permission))
                .thenReturn(permission);

        AppPermission result =
                permissionService.update(1L, data);

        assertEquals("EDIT_REQUEST",
                result.getPermissionName());

        assertEquals("Editar solicitudes",
                result.getDescription());
    }

    // 7. No actualizar con un nombre que ya existe
    @Test
    void testUpdateDuplicateName() {

        AppPermission otherPermission = new AppPermission();
        otherPermission.setId(2L);
        otherPermission.setPermissionName("EDIT_REQUEST");

        AppPermission data = new AppPermission();
        data.setPermissionName("EDIT_REQUEST");

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        when(permissionRepository.findAll())
                .thenReturn(List.of(permission, otherPermission));

        assertThrows(RuntimeException.class,
                () -> permissionService.update(1L, data));

        verify(permissionRepository, never()).save(any());
    }

    // 8. Eliminar un permiso que no está asignado a roles
    @Test
    void testDeleteWithoutRoles() {

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        when(rolePermissionRepository.findAll())
                .thenReturn(List.of());

        permissionService.delete(1L);

        verify(rolePermissionRepository)
                .deleteAll(anyList());

        verify(rolePermissionRepository).flush();

        verify(permissionRepository).delete(permission);
    }

    // 9. No eliminar el último permiso de un rol
    @Test
    void testDeleteLastPermission() {

        AppRole role = new AppRole();
        role.setId(1L);

        RolePermission assignment = new RolePermission();
        assignment.setRole(role);
        assignment.setPermission(permission);

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        when(rolePermissionRepository.findAll())
                .thenReturn(List.of(assignment));

        assertThrows(RuntimeException.class,
                () -> permissionService.delete(1L));

        verify(permissionRepository, never()).delete(any());
    }

    // 10. Eliminar un permiso si el rol tiene otro
    @Test
    void testDeleteWithOtherPermissions() {

        AppRole role = new AppRole();
        role.setId(1L);

        AppPermission otherPermission = new AppPermission();
        otherPermission.setId(2L);

        RolePermission assignment1 = new RolePermission();
        assignment1.setRole(role);
        assignment1.setPermission(permission);

        RolePermission assignment2 = new RolePermission();
        assignment2.setRole(role);
        assignment2.setPermission(otherPermission);

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        when(rolePermissionRepository.findAll())
                .thenReturn(List.of(assignment1, assignment2));

        permissionService.delete(1L);

        verify(rolePermissionRepository)
                .deleteAll(List.of(assignment1));

        verify(rolePermissionRepository).flush();

        verify(permissionRepository).delete(permission);
    }


    @Test
    void testUpdateWithDifferentPermission() {

        AppPermission otherPermission = new AppPermission();
        otherPermission.setId(2L);
        otherPermission.setPermissionName("DELETE_REQUEST");

        AppPermission data = new AppPermission();
        data.setPermissionName("EDIT_REQUEST");
        data.setDescription("Editar solicitudes");

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        when(permissionRepository.findAll())
                .thenReturn(List.of(permission, otherPermission));

        when(permissionRepository.save(permission))
                .thenReturn(permission);

        AppPermission result = permissionService.update(1L, data);

        assertEquals("EDIT_REQUEST", result.getPermissionName());
        assertEquals("Editar solicitudes", result.getDescription());

        verify(permissionRepository).save(permission);
    }

}
