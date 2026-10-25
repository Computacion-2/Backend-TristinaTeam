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
import static org.mockito.ArgumentMatchers.anyList;
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
        role.setRoleName("PET_SITTER");
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

        assertEquals("PET_SITTER", result.getRoleName());
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

        when(roleRepository.existsByRoleName("PET_SITTER"))
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
        assertEquals("PET_SITTER", result.getRoleName());

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
        data.setRoleName("PET_SITTER");
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

    private AppRole systemRole(String name) {
        AppRole systemRole = new AppRole();
        systemRole.setId(9L);
        systemRole.setRoleName(name);
        systemRole.setActiveFlag("Y");
        return systemRole;
    }

    private RolePermission assignment(AppRole assignedRole, AppPermission assignedPermission) {
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRole(assignedRole);
        rolePermission.setPermission(assignedPermission);
        return rolePermission;
    }

    // Consultar los permisos de un rol
    @Test
    void testFindPermissions() {

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));
        when(rolePermissionRepository.findByRole_Id(1L))
                .thenReturn(List.of(assignment(role, permission)));

        assertEquals(List.of(permission), roleService.findPermissions(1L));
    }

    // Consultar los permisos de un rol inexistente
    @Test
    void testFindPermissionsRoleNotFound() {

        when(roleRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> roleService.findPermissions(99L));

        verifyNoInteractions(rolePermissionRepository);
    }

    // Crear un rol normalizando nombre, descripción y estado
    @Test
    void testCreateNormalizesFields() {

        AppRole data = new AppRole();
        data.setRoleName("  pet_walker ");
        data.setDescription("  ");
        data.setActiveFlag(null);

        when(roleRepository.existsByRoleName("PET_WALKER"))
                .thenReturn(false);
        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));
        when(roleRepository.save(data))
                .thenReturn(data);

        AppRole result = roleService.create(data, List.of(1L, 1L));

        assertEquals("PET_WALKER", result.getRoleName());
        assertNull(result.getDescription());
        assertEquals("Y", result.getActiveFlag());
        verify(rolePermissionRepository, times(1)).save(any(RolePermission.class));
    }

    // No crear un rol con un nombre inválido
    @Test
    void testCreateInvalidName() {

        role.setRoleName("rol con espacios");

        assertThrows(IllegalArgumentException.class,
                () -> roleService.create(role, List.of(1L)));

        verifyNoInteractions(roleRepository);
    }

    // Conservar el estado cuando la actualización no lo envía
    @Test
    void testUpdateKeepsActiveFlagWhenMissing() {

        role.setActiveFlag("N");

        AppRole data = new AppRole();
        data.setRoleName("PET_SITTER");
        data.setActiveFlag(null);

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));
        when(roleRepository.save(role))
                .thenReturn(role);

        assertEquals("N", roleService.update(1L, data).getActiveFlag());
    }

    // Rechazar estados distintos de Y o N
    @Test
    void testUpdateInvalidActiveFlag() {

        AppRole data = new AppRole();
        data.setRoleName("PET_SITTER");
        data.setActiveFlag("X");

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        assertThrows(IllegalArgumentException.class,
                () -> roleService.update(1L, data));

        verify(roleRepository, never()).save(any());
    }

    // No renombrar ni desactivar los roles del sistema
    @Test
    void testUpdateSystemRoleRestrictions() {

        AppRole admin = systemRole("ADMIN");

        AppRole renamed = new AppRole();
        renamed.setRoleName("SUPERADMIN");
        renamed.setActiveFlag("Y");

        AppRole disabled = new AppRole();
        disabled.setRoleName("ADMIN");
        disabled.setActiveFlag("N");

        when(roleRepository.findById(9L))
                .thenReturn(Optional.of(admin));

        assertThrows(RuntimeException.class,
                () -> roleService.update(9L, renamed));
        assertThrows(RuntimeException.class,
                () -> roleService.update(9L, disabled));

        verify(roleRepository, never()).save(any());
    }

    // Permitir cambiar la descripción de un rol del sistema
    @Test
    void testUpdateSystemRoleDescription() {

        AppRole owner = systemRole("OWNER");

        AppRole data = new AppRole();
        data.setRoleName("owner");
        data.setDescription("Propietario de mascotas registrado");
        data.setActiveFlag("Y");

        when(roleRepository.findById(9L))
                .thenReturn(Optional.of(owner));
        when(roleRepository.save(owner))
                .thenReturn(owner);

        AppRole result = roleService.update(9L, data);

        assertEquals("OWNER", result.getRoleName());
        assertEquals("Propietario de mascotas registrado", result.getDescription());
    }

    // No eliminar los roles del sistema
    @Test
    void testDeleteSystemRole() {

        when(roleRepository.findById(9L))
                .thenReturn(Optional.of(systemRole("OWNER")));

        assertThrows(RuntimeException.class,
                () -> roleService.delete(9L));

        verifyNoInteractions(userRoleRepository);
        verify(rolePermissionRepository, never()).deleteAll(anyList());
        verify(roleRepository, never()).delete(any());
    }

    // ADMIN no puede perder los permisos de administración
    @Test
    void testAdminKeepsManagementPermissions() {

        AppRole admin = systemRole("ADMIN");

        AppPermission manageRoles = new AppPermission();
        manageRoles.setId(2L);
        manageRoles.setPermissionCode("MANAGE_ROLES");

        when(roleRepository.findById(9L))
                .thenReturn(Optional.of(admin));
        when(rolePermissionRepository.findByRole_Id(9L))
                .thenReturn(List.of(assignment(admin, manageRoles), assignment(admin, permission)));
        when(rolePermissionRepository.existsById(any(RolePermissionId.class)))
                .thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roleService.removePermission(9L, 2L));

        assertEquals("El rol ADMIN debe conservar los permisos de administración", ex.getMessage());
        verify(rolePermissionRepository, never()).deleteById(any(RolePermissionId.class));
    }

    // ADMIN sí puede perder permisos que no son de administración
    @Test
    void testAdminCanLoseOtherPermissions() {

        AppRole admin = systemRole("ADMIN");

        AppPermission manageRoles = new AppPermission();
        manageRoles.setId(2L);
        manageRoles.setPermissionCode("MANAGE_ROLES");

        when(roleRepository.findById(9L))
                .thenReturn(Optional.of(admin));
        when(rolePermissionRepository.findByRole_Id(9L))
                .thenReturn(List.of(assignment(admin, manageRoles), assignment(admin, permission)));
        when(rolePermissionRepository.existsById(any(RolePermissionId.class)))
                .thenReturn(true);

        roleService.removePermission(9L, 1L);

        verify(rolePermissionRepository).deleteById(new RolePermissionId(9L, 1L));
    }
}
