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
        permission.setPermissionCode("CREATE_REQUEST");
        permission.setPermissionName("Crear solicitudes");
        permission.setDescription("Permite crear solicitudes");
    }

    private RolePermission assignment(Long roleId, AppPermission assigned) {
        AppRole role = new AppRole();
        role.setId(roleId);
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRole(role);
        rolePermission.setPermission(assigned);
        return rolePermission;
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

    // 4. Crear un permiso normalizando código, nombre y descripción
    @Test
    void testCreateNormalizesFields() {

        AppPermission data = new AppPermission();
        data.setPermissionCode("  view_reports ");
        data.setPermissionName("  Ver reportes ");
        data.setDescription("   ");

        when(permissionRepository.existsByPermissionCode("VIEW_REPORTS"))
                .thenReturn(false);
        when(permissionRepository.existsByPermissionNameIgnoreCase("Ver reportes"))
                .thenReturn(false);
        when(permissionRepository.save(data))
                .thenReturn(data);

        AppPermission result = permissionService.create(data);

        assertEquals("VIEW_REPORTS", result.getPermissionCode());
        assertEquals("Ver reportes", result.getPermissionName());
        assertNull(result.getDescription());
        verify(permissionRepository).save(data);
    }

    // 5. No crear un permiso con código duplicado
    @Test
    void testCreateDuplicateCode() {

        when(permissionRepository.existsByPermissionCode("CREATE_REQUEST"))
                .thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> permissionService.create(permission));

        assertEquals("Ya existe un permiso con ese código", ex.getMessage());
        verify(permissionRepository, never()).save(any());
    }

    // 6. No crear un permiso con nombre duplicado
    @Test
    void testCreateDuplicateName() {

        when(permissionRepository.existsByPermissionCode("CREATE_REQUEST"))
                .thenReturn(false);
        when(permissionRepository.existsByPermissionNameIgnoreCase("Crear solicitudes"))
                .thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> permissionService.create(permission));

        assertEquals("Ya existe un permiso con ese nombre", ex.getMessage());
        verify(permissionRepository, never()).save(any());
    }

    // 7. No crear un permiso sin código o con caracteres inválidos
    @Test
    void testCreateInvalidCode() {

        permission.setPermissionCode(" ");
        assertThrows(IllegalArgumentException.class,
                () -> permissionService.create(permission));

        permission.setPermissionCode("1-INVALID");
        assertThrows(IllegalArgumentException.class,
                () -> permissionService.create(permission));

        verifyNoInteractions(permissionRepository);
    }

    // 8. No crear un permiso sin nombre
    @Test
    void testCreateWithoutName() {

        permission.setPermissionName(null);

        assertThrows(IllegalArgumentException.class,
                () -> permissionService.create(permission));

        verify(permissionRepository, never()).save(any());
    }

    // 9. Actualizar nombre y descripción sin cambiar el código
    @Test
    void testUpdate() {

        AppPermission data = new AppPermission();
        data.setPermissionCode("OTRO_CODIGO");
        data.setPermissionName(" Editar solicitudes ");
        data.setDescription("Editar solicitudes de cuidado");

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));
        when(permissionRepository.existsByPermissionNameIgnoreCaseAndIdNot("Editar solicitudes", 1L))
                .thenReturn(false);
        when(permissionRepository.save(permission))
                .thenReturn(permission);

        AppPermission result =
                permissionService.update(1L, data);

        assertEquals("CREATE_REQUEST", result.getPermissionCode());
        assertEquals("Editar solicitudes", result.getPermissionName());
        assertEquals("Editar solicitudes de cuidado", result.getDescription());
    }

    // 10. No actualizar con un nombre que ya tiene otro permiso
    @Test
    void testUpdateDuplicateName() {

        AppPermission data = new AppPermission();
        data.setPermissionName("Editar solicitudes");

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));
        when(permissionRepository.existsByPermissionNameIgnoreCaseAndIdNot("Editar solicitudes", 1L))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> permissionService.update(1L, data));

        verify(permissionRepository, never()).save(any());
    }

    // 11. No actualizar un permiso inexistente
    @Test
    void testUpdateNotFound() {

        when(permissionRepository.findById(9L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> permissionService.update(9L, permission));

        verify(permissionRepository, never()).save(any());
    }

    // 12. Eliminar un permiso que no está asignado a roles
    @Test
    void testDeleteWithoutRoles() {

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));
        when(rolePermissionRepository.findByPermission_Id(1L))
                .thenReturn(List.of());

        permissionService.delete(1L);

        verify(rolePermissionRepository).deleteAll(List.of());
        verify(rolePermissionRepository).flush();
        verify(permissionRepository).delete(permission);
    }

    // 13. No eliminar el último permiso de un rol
    @Test
    void testDeleteLastPermission() {

        RolePermission onlyAssignment = assignment(5L, permission);

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));
        when(rolePermissionRepository.findByPermission_Id(1L))
                .thenReturn(List.of(onlyAssignment));
        when(rolePermissionRepository.findByRole_Id(5L))
                .thenReturn(List.of(onlyAssignment));

        assertThrows(RuntimeException.class,
                () -> permissionService.delete(1L));

        verify(rolePermissionRepository, never()).deleteAll(anyList());
        verify(permissionRepository, never()).delete(any());
    }

    // 14. Eliminar un permiso si el rol conserva otro
    @Test
    void testDeleteWithOtherPermissions() {

        AppPermission otherPermission = new AppPermission();
        otherPermission.setId(2L);

        RolePermission assignment1 = assignment(5L, permission);
        RolePermission assignment2 = assignment(5L, otherPermission);

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));
        when(rolePermissionRepository.findByPermission_Id(1L))
                .thenReturn(List.of(assignment1));
        when(rolePermissionRepository.findByRole_Id(5L))
                .thenReturn(List.of(assignment1, assignment2));

        permissionService.delete(1L);

        verify(rolePermissionRepository).deleteAll(List.of(assignment1));
        verify(rolePermissionRepository).flush();
        verify(permissionRepository).delete(permission);
    }

    // 15. No eliminar los permisos que usa la configuración de seguridad
    @Test
    void testDeleteSystemPermission() {

        permission.setPermissionCode("MANAGE_ROLES");

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> permissionService.delete(1L));

        assertEquals("No se puede eliminar un permiso requerido por el sistema", ex.getMessage());
        verifyNoInteractions(rolePermissionRepository);
        verify(permissionRepository, never()).delete(any());
    }
}
