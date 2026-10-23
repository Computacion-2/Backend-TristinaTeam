package com.icesi.cuidapetproyect.service;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppUserServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private AppRoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private AppUserService userService;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private AppUser user;
    private AppRole role;

    @BeforeEach
    void setUp() {

        user = new AppUser();
        user.setId(1L);
        user.setEmail("juan@icesi.edu.co");
        user.setPasswordHash("hash");
        user.setAccountStatus("ACTIVE");

        role = new AppRole();
        role.setId(1L);
        role.setRoleName("OWNER");
    }

    // Consultar todos los usuarios
    @Test
    void testFindAll() {

        when(userRepository.findAll())
                .thenReturn(List.of(user));

        List<AppUser> result = userService.findAll();

        assertEquals(1, result.size());
        verify(userRepository).findAll();
    }

    // Buscar un usuario existente
    @Test
    void testFindById() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        AppUser result = userService.findById(1L);

        assertEquals(user.getEmail(), result.getEmail());
    }

    // Buscar un usuario que no existe
    @Test
    void testFindByIdNotFound() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.findById(1L));
    }

    // No permitir usuarios sin roles
    @Test
    void testCreateWithoutRoles() {

        assertThrows(RuntimeException.class,
                () -> userService.create(user, List.of()));

        verify(userRepository, never()).save(any());
    }

    // No permitir correos duplicados
    @Test
    void testCreateDuplicateEmail() {

        when(userRepository.existsByEmail(user.getEmail()))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> userService.create(user, List.of(1L)));

        verify(userRepository, never()).save(any());
    }

    // Crear un usuario correctamente
    @Test
    void testCreateUser() {

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        RolePermission rolePermission = new RolePermission();

        when(rolePermissionRepository.findByRole_Id(1L))
                .thenReturn(List.of(rolePermission));

        when(userRepository.save(user))
                .thenReturn(user);

        AppUser result =
                userService.create(user, List.of(1L));

        assertNotNull(result);
        assertTrue(passwordEncoder.matches("hash", result.getPasswordHash()));

        verify(userRepository).save(user);

        verify(userRoleRepository)
                .save(any(UserRole.class));
    }

    // Actualizar los datos de un usuario
    @Test
    void testUpdate() {

        AppUser data = new AppUser();
        data.setEmail("nuevo@icesi.edu.co");
        data.setPasswordHash("nuevoHash");
        data.setAccountStatus("ACTIVE");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user))
                .thenReturn(user);

        AppUser result = userService.update(1L, data);

        assertEquals("nuevo@icesi.edu.co",
                result.getEmail());

        verify(userRepository).save(user);
    }

    // No permitir quitar el último rol
    @Test
    void testRemoveLastRole() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRoleRepository.existsById(any(UserRoleId.class)))
                .thenReturn(true);

        when(userRoleRepository.findByUser_Id(1L))
                .thenReturn(List.of(new UserRole()));

        assertThrows(RuntimeException.class,
                () -> userService.removeRole(1L, 1L));

        verify(userRoleRepository, never())
                .deleteById(any(UserRoleId.class));
    }


    // Eliminar un usuario de forma lógica
    @Test
    void testDelete() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user))
                .thenReturn(user);

        userService.delete(1L);

        assertEquals("DELETED", user.getAccountStatus());

        verify(userRepository).save(user);

        verify(userRepository, never()).delete(any());
    }



    // No crear un usuario con un rol inexistente
    @Test
    void testCreateRoleNotFound() {

        when(roleRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.create(user, List.of(1L)));

        verify(userRepository, never()).save(any());
    }

    // No crear un usuario con un rol sin permisos
    @Test
    void testCreateRoleWithoutPermissions() {

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        assertThrows(RuntimeException.class,
                () -> userService.create(user, List.of(1L)));

        verify(userRepository, never()).save(any());
    }

    // No permitir actualizar con un correo duplicado
    @Test
    void testUpdateDuplicateEmail() {

        AppUser data = new AppUser();
        data.setEmail("otro@icesi.edu.co");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByEmail(data.getEmail()))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> userService.update(1L, data));

        verify(userRepository, never()).save(any());
    }

    // Actualizar un usuario sin cambiar su correo
    @Test
    void testUpdateSameEmail() {

        AppUser data = new AppUser();
        data.setEmail(user.getEmail());
        data.setPasswordHash("nuevoHash");
        data.setAccountStatus("ACTIVE");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user))
                .thenReturn(user);

        AppUser result = userService.update(1L, data);

        assertTrue(passwordEncoder.matches("nuevoHash", result.getPasswordHash()));
        verify(userRepository).save(user);
    }

    // Asignar correctamente un rol
    @Test
    void testAssignRole() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        when(rolePermissionRepository.findByRole_Id(1L))
                .thenReturn(List.of(new RolePermission()));

        userService.assignRole(1L, 1L);

        verify(userRoleRepository)
                .save(any(UserRole.class));
    }

    // No asignar un rol que no existe
    @Test
    void testAssignRoleNotFound() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.assignRole(1L, 1L));
    }

    // No asignar un rol sin permisos
    @Test
    void testAssignRoleWithoutPermissions() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        assertThrows(RuntimeException.class,
                () -> userService.assignRole(1L, 1L));

        verify(userRoleRepository, never()).save(any());
    }

    // No asignar dos veces el mismo rol
    @Test
    void testAssignDuplicateRole() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        when(rolePermissionRepository.findByRole_Id(1L))
                .thenReturn(List.of(new RolePermission()));

        when(userRoleRepository.existsById(
                any(UserRoleId.class)))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> userService.assignRole(1L, 1L));

        verify(userRoleRepository, never()).save(any());
    }

    // No eliminar un rol que el usuario no tiene
    @Test
    void testRemoveRoleNotAssigned() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class,
                () -> userService.removeRole(1L, 1L));

        verify(userRoleRepository, never())
                .deleteById(any(UserRoleId.class));
    }

    // Eliminar un rol cuando el usuario tiene varios
    @Test
    void testRemoveRole() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRoleRepository.existsById(
                any(UserRoleId.class)))
                .thenReturn(true);

        when(userRoleRepository.findByUser_Id(1L))
                .thenReturn(List.of(
                        new UserRole(),
                        new UserRole()
                ));

        userService.removeRole(1L, 1L);

        verify(userRoleRepository)
                .deleteById(any(UserRoleId.class));
    }

    // No permitir crear usuarios cuando la lista de roles es null
    @Test
    void testCreateWithNullRoles() {

        assertThrows(RuntimeException.class,
                () -> userService.create(user, null));

        verify(userRepository, never()).save(any());
    }


    @Test
    void updateWithoutPasswordKeepsExistingHash() {
        AppUser data = new AppUser();
        data.setEmail(user.getEmail());
        data.setPasswordHash("");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        assertEquals("hash", userService.update(1L, data).getPasswordHash());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void createRejectsMissingPassword() {
        user.setPasswordHash(null);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findByRole_Id(1L)).thenReturn(List.of(new RolePermission()));
        assertThrows(IllegalArgumentException.class, () -> userService.create(user, List.of(1L)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createRejectsPasswordOverBcryptByteLimit() {
        user.setPasswordHash("á".repeat(37));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findByRole_Id(1L)).thenReturn(List.of(new RolePermission()));
        assertThrows(IllegalArgumentException.class, () -> userService.create(user, List.of(1L)));
        verify(userRepository, never()).save(any());
    }

    // No permitir eliminar dos veces al mismo usuario
    @Test
    void testDeleteAlreadyDeleted() {

        user.setAccountStatus("DELETED");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class,
                () -> userService.delete(1L));

        verify(userRepository, never()).save(any());
    }

    @Test
    void findRolesReturnsAssignedRoles() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserRole assignment = new UserRole();
        assignment.setRole(role);
        when(userRoleRepository.findByUser_Id(1L)).thenReturn(List.of(assignment));
        assertEquals(List.of(role), userService.findRoles(1L));
    }

    @Test
    void findRolesRejectsMissingUser() {
        assertThrows(RuntimeException.class, () -> userService.findRoles(999L));
        verifyNoInteractions(userRoleRepository);
    }

    @Test
    void findRolesCanReturnEmptyList() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        assertTrue(userService.findRoles(1L).isEmpty());
    }
}
