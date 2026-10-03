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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppRoleServiceTest {

    @Mock
    private AppRoleRepository roleRepository;

    @Mock
    private AppPermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private AppRoleService roleService;

    private AppRole role;
    private AppPermission permission;

    @BeforeEach
    void setUp() {

        role = new AppRole();
        role.setId(1L);
        role.setRoleName("OWNER");
        role.setActiveFlag("Y");

        permission = new AppPermission();
        permission.setId(1L);
        permission.setPermissionCode("CREATE_REQUEST");
        permission.setPermissionName("Crear solicitud");
    }

    // Consultar todos los roles
    @Test
    void testFindAll() {

        when(roleRepository.findAll())
                .thenReturn(List.of(role));

        List<AppRole> result = roleService.findAll();

        assertEquals(1, result.size());
        verify(roleRepository).findAll();
    }

    // Buscar un rol por su ID
    @Test
    void testFindById() {

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        AppRole result = roleService.findById(1L);

        assertEquals("OWNER", result.getRoleName());
    }

    // Buscar un rol que no existe
    @Test
    void testFindByIdNotFound() {

        when(roleRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> roleService.findById(1L));
    }

    // No permitir crear un rol sin permisos
    @Test
    void testCreateWithoutPermissions() {

        assertThrows(RuntimeException.class,
                () -> roleService.create(role, List.of()));

        verify(roleRepository, never()).save(any());
    }

    // No permitir nombres duplicados
    @Test
    void testCreateDuplicateName() {

        when(roleRepository.existsByRoleName("OWNER"))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> roleService.create(role, List.of(1L)));

        verify(roleRepository, never()).save(any());
    }

    // Crear correctamente un rol
    @Test
    void testCreateRole() {

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        when(roleRepository.save(role))
                .thenReturn(role);

        AppRole result =
                roleService.create(role, List.of(1L));

        assertNotNull(result);
        assertEquals("OWNER", result.getRoleName());

        verify(roleRepository).save(role);

        verify(rolePermissionRepository)
                .save(any(RolePermission.class));
    }

    // Actualizar un rol
    @Test
    void testUpdate() {

        AppRole data = new AppRole();
        data.setRoleName("CAREGIVER");
        data.setDescription("Rol de cuidador");
        data.setActiveFlag("Y");

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        when(roleRepository.save(role))
                .thenReturn(role);

        AppRole result = roleService.update(1L, data);

        assertEquals("CAREGIVER", result.getRoleName());

        verify(roleRepository).save(role);
    }

    // Asignar un permiso a un rol
    @Test
    void testAssignPermission() {

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        roleService.assignPermission(1L, 1L);

        verify(rolePermissionRepository)
                .save(any(RolePermission.class));
    }

    // Impedir eliminar el último permiso
    @Test
    void testRemoveLastPermission() {

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        when(rolePermissionRepository.findByRole_Id(1L))
                .thenReturn(List.of(new RolePermission()));

        when(rolePermissionRepository.existsById(
                any(RolePermissionId.class)))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> roleService.removePermission(1L, 1L));

        verify(rolePermissionRepository, never())
                .deleteById(any(RolePermissionId.class));
    }

    // No eliminar roles asignados a usuarios
    @Test
    void testDeleteAssignedRole() {

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        when(userRoleRepository.findByRole_Id(1L))
                .thenReturn(List.of(new UserRole()));

        assertThrows(RuntimeException.class,
                () -> roleService.delete(1L));

        verify(roleRepository, never()).delete(any());
    }

    // Eliminar un rol sin usuarios asignados
    @Test
    void testDeleteRole() {

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        roleService.delete(1L);

        verify(roleRepository).delete(role);
    }


    // No crear un rol con un permiso inexistente
    @Test
    void testCreatePermissionNotFound() {

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> roleService.create(role, List.of(1L)));

        verify(roleRepository, never()).save(any());
    }

    // No actualizar un rol con un nombre duplicado
    @Test
    void testUpdateDuplicateName() {

        AppRole data = new AppRole();
        data.setRoleName("CAREGIVER");

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        when(roleRepository.existsByRoleName("CAREGIVER"))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> roleService.update(1L, data));

        verify(roleRepository, never()).save(any());
    }

    // Actualizar un rol conservando su nombre
    @Test
    void testUpdateSameName() {

        AppRole data = new AppRole();
        data.setRoleName("OWNER");
        data.setDescription("Descripción actualizada");
        data.setActiveFlag("Y");

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        when(roleRepository.save(role))
                .thenReturn(role);

        AppRole result = roleService.update(1L, data);

        assertEquals("Descripción actualizada",
                result.getDescription());

        verify(roleRepository).save(role);
    }

    // No asignar un permiso inexistente
    @Test
    void testAssignPermissionNotFound() {

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> roleService.assignPermission(1L, 1L));

        verify(rolePermissionRepository, never())
                .save(any());
    }

    // No asignar dos veces el mismo permiso
    @Test
    void testAssignDuplicatePermission() {

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        when(rolePermissionRepository.existsById(
                any(RolePermissionId.class)))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> roleService.assignPermission(1L, 1L));

        verify(rolePermissionRepository, never())
                .save(any());
    }

    // No eliminar un permiso que el rol no tiene
    @Test
    void testRemovePermissionNotAssigned() {

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        assertThrows(RuntimeException.class,
                () -> roleService.removePermission(1L, 1L));

        verify(rolePermissionRepository, never())
                .deleteById(any(RolePermissionId.class));
    }

    // Eliminar un permiso cuando el rol tiene varios
    @Test
    void testRemovePermission() {

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        when(rolePermissionRepository.existsById(
                any(RolePermissionId.class)))
                .thenReturn(true);

        when(rolePermissionRepository.findByRole_Id(1L))
                .thenReturn(List.of(
                        new RolePermission(),
                        new RolePermission()
                ));

        roleService.removePermission(1L, 1L);

        verify(rolePermissionRepository)
                .deleteById(any(RolePermissionId.class));
    }

    // No permitir crear roles cuando la lista de permisos es null
    @Test
    void testCreateWithNullPermissions() {

        assertThrows(RuntimeException.class,
                () -> roleService.create(role, null));

        verify(roleRepository, never()).save(any());
    }
}