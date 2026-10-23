package com.icesi.cuidapetproyect.controller;

import com.icesi.cuidapetproyect.config.*;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({UserMvcController.class, AuthController.class})
@Import({SecurityConfig.class, PasswordConfig.class})
@WithMockUser(authorities = "MANAGE_USERS")
class UserMvcControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AppUserService userService;
    @MockitoBean AppRoleService roleService;
    @MockitoBean AppUserDetailsService userDetailsService;
    @MockitoBean AuthService authService;

    @BeforeEach
    void setUp(@Autowired WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private AppUser existingUser() {
        AppUser user = new AppUser();
        user.setId(7L);
        user.setEmail("persona@icesi.edu.co");
        user.setPasswordHash("HASH-NUNCA-VISIBLE");
        return user;
    }

    @Test
    void listRendersUsersAndCsrfWithoutPasswords() throws Exception {
        when(userService.findAll()).thenReturn(List.of(existingUser()));
        mvc.perform(get("/usuarios")).andExpect(status().isOk())
                .andExpect(view().name("usuarios/lista"))
                .andExpect(content().string(containsString("persona@icesi.edu.co")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("HASH-NUNCA-VISIBLE"))));
    }

    @Test
    void createFormShowsAvailableRoles() throws Exception {
        AppRole role = new AppRole();
        role.setId(3L);
        role.setRoleName("OWNER");
        when(roleService.findAll()).thenReturn(List.of(role));
        mvc.perform(get("/usuarios/nuevo")).andExpect(status().isOk())
                .andExpect(view().name("usuarios/formulario"))
                .andExpect(content().string(containsString("OWNER")))
                .andExpect(content().string(containsString("/usuarios/crear")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void editFormNeverContainsStoredHash() throws Exception {
        when(userService.findById(7L)).thenReturn(existingUser());
        mvc.perform(get("/usuarios/7/editar")).andExpect(status().isOk())
                .andExpect(content().string(containsString("/usuarios/7/editar")))
                .andExpect(content().string(not(containsString("HASH-NUNCA-VISIBLE"))))
                .andExpect(content().string(not(containsString("name=\"roleIds\""))));
    }

    @Test
    void createDelegatesToServiceAndRedirects() throws Exception {
        mvc.perform(post("/usuarios/crear").with(csrf()).param("email", "nuevo@icesi.edu.co")
                        .param("password", "Clave1234").param("roleIds", "3"))
                .andExpect(redirectedUrl("/usuarios")).andExpect(flash().attributeExists("message"));
        verify(userService).create(argThat(u -> u.getEmail().equals("nuevo@icesi.edu.co")
                && u.getPasswordHash().equals("Clave1234")), eq(List.of(3L)));
    }

    @Test
    void invalidCreateDoesNotSaveOrKeepPassword() throws Exception {
        var result = mvc.perform(post("/usuarios/crear").with(csrf()).param("email", "invalido")
                        .param("password", "corta"))
                .andExpect(redirectedUrl("/usuarios/nuevo"))
                .andExpect(flash().attributeExists("error")).andReturn();
        assertNull(((UserForm) result.getFlashMap().get("userForm")).getPassword());
        verifyNoInteractions(userService);
    }

    @Test
    void createRequiresPasswordAndRoles() throws Exception {
        mvc.perform(post("/usuarios/crear").with(csrf()).param("email", "nuevo@icesi.edu.co")
                        .param("roleIds", "3"))
                .andExpect(redirectedUrl("/usuarios/nuevo"));
        mvc.perform(post("/usuarios/crear").with(csrf()).param("email", "nuevo@icesi.edu.co")
                        .param("password", "Clave1234"))
                .andExpect(redirectedUrl("/usuarios/nuevo"));
        verifyNoInteractions(userService);
    }

    @Test
    void duplicateEmailShowsBusinessError() throws Exception {
        when(userService.create(any(), anyList())).thenThrow(new IllegalArgumentException("El correo ya está registrado"));
        mvc.perform(post("/usuarios/crear").with(csrf()).param("email", "nuevo@icesi.edu.co")
                        .param("password", "Clave1234").param("roleIds", "3"))
                .andExpect(redirectedUrl("/usuarios/nuevo"))
                .andExpect(flash().attribute("error", "El correo ya está registrado"));
    }

    @Test
    void updateAllowsEmptyPasswordAndDoesNotChangeRoles() throws Exception {
        mvc.perform(post("/usuarios/7/editar").with(csrf()).param("email", "persona@icesi.edu.co")
                        .param("password", "").param("accountStatus", "INACTIVE").param("roleIds", "1"))
                .andExpect(redirectedUrl("/usuarios"));
        verify(userService).update(eq(7L), argThat(u -> u.getPasswordHash().isEmpty()
                && "INACTIVE".equals(u.getAccountStatus())));
        verify(userService, never()).assignRole(anyLong(), anyLong());
    }

    @Test
    void updateRejectsInvalidStatusAndShortPassword() throws Exception {
        mvc.perform(post("/usuarios/7/editar").with(csrf()).param("email", "persona@icesi.edu.co")
                        .param("accountStatus", "INVALID"))
                .andExpect(redirectedUrl("/usuarios/7/editar"));
        mvc.perform(post("/usuarios/7/editar").with(csrf()).param("email", "persona@icesi.edu.co")
                        .param("password", "123"))
                .andExpect(redirectedUrl("/usuarios/7/editar"));
        verifyNoInteractions(userService);
    }

    @Test
    void missingUserReturnsToList() throws Exception {
        when(userService.findById(99L)).thenThrow(new RuntimeException("Usuario no encontrado"));
        mvc.perform(get("/usuarios/99/editar")).andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attribute("error", "Usuario no encontrado"));
    }

    @Test
    void deleteUsesLogicalDeletionService() throws Exception {
        mvc.perform(post("/usuarios/7/eliminar").with(csrf()))
                .andExpect(redirectedUrl("/usuarios")).andExpect(flash().attributeExists("message"));
        verify(userService).delete(7L);
    }

    @Test
    void failedDeleteShowsMessage() throws Exception {
        doThrow(new RuntimeException("El usuario ya está eliminado")).when(userService).delete(7L);
        mvc.perform(post("/usuarios/7/eliminar").with(csrf())).andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attribute("error", "El usuario ya está eliminado"));
    }

    @Test
    void permissionAndCsrfAreRequired() throws Exception {
        mvc.perform(get("/usuarios").with(user("owner").roles("OWNER"))).andExpect(status().isForbidden());
        mvc.perform(post("/usuarios/7/eliminar")).andExpect(status().isForbidden());
        mvc.perform(post("/usuarios/7/eliminar").with(csrf()).with(user("owner").roles("OWNER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }

    @Test
    void homeLinkIsVisibleOnlyWithPermission() throws Exception {
        mvc.perform(get("/")).andExpect(content().string(containsString("Gestionar usuarios")));
        mvc.perform(get("/").with(user("owner").roles("OWNER")))
                .andExpect(content().string(not(containsString("Gestionar usuarios"))));
    }
    @Test
    void rolesPageShowsAssignmentsAndOnlyUnassignedOptions() throws Exception {
        AppRole owner = new AppRole();
        owner.setId(3L);
        owner.setRoleName("OWNER");
        AppRole caregiver = new AppRole();
        caregiver.setId(2L);
        caregiver.setRoleName("CAREGIVER");
        when(userService.findById(7L)).thenReturn(existingUser());
        when(userService.findRoles(7L)).thenReturn(List.of(owner));
        when(roleService.findAll()).thenReturn(List.of(owner, caregiver));
        mvc.perform(get("/usuarios/7/roles")).andExpect(status().isOk())
                .andExpect(view().name("usuarios/roles"))
                .andExpect(model().attribute("availableRoles", List.of(caregiver)))
                .andExpect(content().string(containsString("OWNER")))
                .andExpect(content().string(containsString("CAREGIVER")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("disabled=\"disabled\"")))
                .andExpect(content().string(not(containsString("HASH-NUNCA-VISIBLE"))));
    }

    @Test
    void rolesPageHandlesNoAvailableRoles() throws Exception {
        when(userService.findById(7L)).thenReturn(existingUser());
        mvc.perform(get("/usuarios/7/roles")).andExpect(status().isOk())
                .andExpect(content().string(containsString("No hay otros roles disponibles")));
    }

    @Test
    void rolesOfMissingUserRedirectToList() throws Exception {
        when(userService.findById(99L)).thenThrow(new RuntimeException("Usuario no encontrado"));
        mvc.perform(get("/usuarios/99/roles")).andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attribute("error", "Usuario no encontrado"));
    }

    @Test
    void assignRoleDelegatesAndRedirects() throws Exception {
        mvc.perform(post("/usuarios/7/roles/asignar").with(csrf()).param("roleId", "2"))
                .andExpect(redirectedUrl("/usuarios/7/roles"))
                .andExpect(flash().attribute("message", "Rol asignado correctamente"));
        verify(userService).assignRole(7L, 2L);
    }

    @Test
    void assigningWithoutSelectionDoesNotCallService() throws Exception {
        mvc.perform(post("/usuarios/7/roles/asignar").with(csrf()))
                .andExpect(redirectedUrl("/usuarios/7/roles"))
                .andExpect(flash().attribute("error", "Selecciona un rol"));
        verifyNoInteractions(userService);
    }

    @Test
    void duplicateRoleShowsServiceError() throws Exception {
        doThrow(new RuntimeException("El usuario ya tiene este rol")).when(userService).assignRole(7L, 2L);
        mvc.perform(post("/usuarios/7/roles/asignar").with(csrf()).param("roleId", "2"))
                .andExpect(redirectedUrl("/usuarios/7/roles"))
                .andExpect(flash().attribute("error", "El usuario ya tiene este rol"));
    }

    @Test
    void removeRoleDelegatesAndRedirects() throws Exception {
        mvc.perform(post("/usuarios/7/roles/2/quitar").with(csrf()))
                .andExpect(redirectedUrl("/usuarios/7/roles"))
                .andExpect(flash().attribute("message", "Rol retirado correctamente"));
        verify(userService).removeRole(7L, 2L);
    }

    @Test
    void removingLastRoleShowsErrorEvenIfPostIsSentDirectly() throws Exception {
        doThrow(new RuntimeException("No se puede dejar al usuario sin roles")).when(userService).removeRole(7L, 3L);
        mvc.perform(post("/usuarios/7/roles/3/quitar").with(csrf()))
                .andExpect(redirectedUrl("/usuarios/7/roles"))
                .andExpect(flash().attribute("error", "No se puede dejar al usuario sin roles"));
    }

    @Test
    void roleActionsRequirePermissionAndCsrf() throws Exception {
        mvc.perform(get("/usuarios/7/roles").with(user("owner").roles("OWNER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/usuarios/7/roles/asignar").param("roleId", "2"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/usuarios/7/roles/2/quitar"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/usuarios/7/roles/asignar").with(csrf())
                        .with(user("owner").roles("OWNER")).param("roleId", "2"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/usuarios/7/roles/2/quitar").with(csrf())
                        .with(user("owner").roles("OWNER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }
}
