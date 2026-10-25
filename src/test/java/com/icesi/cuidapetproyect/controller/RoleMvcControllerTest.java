package com.icesi.cuidapetproyect.controller;

import com.icesi.cuidapetproyect.config.*;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleMvcController.class)
@Import({SecurityConfig.class, PasswordConfig.class})
@WithMockUser(authorities = "MANAGE_ROLES")
class RoleMvcControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AppRoleService roleService;
    @MockitoBean AppPermissionService permissionService;
    @MockitoBean AppUserDetailsService userDetailsService;

    @BeforeEach
    void setUp(@Autowired WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private AppRole role(Long id, String name) {
        AppRole role = new AppRole();
        role.setId(id);
        role.setRoleName(name);
        role.setActiveFlag("Y");
        return role;
    }

    private AppPermission permission(Long id, String code) {
        AppPermission permission = new AppPermission();
        permission.setId(id);
        permission.setPermissionCode(code);
        permission.setPermissionName("Permiso " + code);
        return permission;
    }

    @Test
    void listRendersRolesAndHidesDeleteForSystemRoles() throws Exception {
        when(roleService.findAll()).thenReturn(List.of(role(1L, "ADMIN"), role(4L, "PET_WALKER")));
        mvc.perform(get("/roles")).andExpect(status().isOk())
                .andExpect(view().name("roles/lista"))
                .andExpect(content().string(containsString("ADMIN")))
                .andExpect(content().string(containsString("PET_WALKER")))
                .andExpect(content().string(containsString("/roles/4/eliminar")))
                .andExpect(content().string(not(containsString("/roles/1/eliminar"))))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void emptyListShowsMessage() throws Exception {
        mvc.perform(get("/roles")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Todavía no hay roles")));
    }

    @Test
    void createFormListsPermissionsAsCheckboxes() throws Exception {
        when(permissionService.findAll()).thenReturn(List.of(permission(3L, "CREATE_REQUEST")));
        mvc.perform(get("/roles/nuevo")).andExpect(status().isOk())
                .andExpect(view().name("roles/formulario"))
                .andExpect(content().string(containsString("type=\"checkbox\"")))
                .andExpect(content().string(containsString("name=\"permissionIds\"")))
                .andExpect(content().string(containsString("CREATE_REQUEST")))
                .andExpect(content().string(containsString("/roles/crear")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void createDelegatesWithSelectedPermissions() throws Exception {
        mvc.perform(post("/roles/crear").with(csrf()).param("roleName", "pet_walker")
                        .param("description", "Paseador").param("activeFlag", "Y")
                        .param("permissionIds", "3", "4"))
                .andExpect(redirectedUrl("/roles"))
                .andExpect(flash().attribute("message", "Rol creado correctamente"));
        verify(roleService).create(argThat(r -> "pet_walker".equals(r.getRoleName())
                && "Paseador".equals(r.getDescription()) && "Y".equals(r.getActiveFlag())), eq(List.of(3L, 4L)));
    }

    @Test
    void createRequiresAtLeastOnePermission() throws Exception {
        var result = mvc.perform(post("/roles/crear").with(csrf()).param("roleName", "PET_WALKER"))
                .andExpect(redirectedUrl("/roles/nuevo"))
                .andExpect(flash().attribute("error", "Selecciona al menos un permiso")).andReturn();
        RoleForm form = (RoleForm) result.getFlashMap().get("roleForm");
        assertEquals("PET_WALKER", form.getRoleName());
        verifyNoInteractions(roleService);
    }

    @Test
    void createRejectsInvalidNameAndStatus() throws Exception {
        mvc.perform(post("/roles/crear").with(csrf()).param("roleName", "rol con espacios")
                        .param("permissionIds", "3"))
                .andExpect(redirectedUrl("/roles/nuevo"))
                .andExpect(flash().attributeExists("error"));
        mvc.perform(post("/roles/crear").with(csrf()).param("roleName", "PET_WALKER")
                        .param("activeFlag", "X").param("permissionIds", "3"))
                .andExpect(redirectedUrl("/roles/nuevo"))
                .andExpect(flash().attribute("error", "Estado del rol inválido"));
        verifyNoInteractions(roleService);
    }

    @Test
    void duplicateRoleShowsServiceMessage() throws Exception {
        when(roleService.create(any(), anyList())).thenThrow(new RuntimeException("Ya existe un rol con ese nombre"));
        mvc.perform(post("/roles/crear").with(csrf()).param("roleName", "OWNER").param("permissionIds", "3"))
                .andExpect(redirectedUrl("/roles/nuevo"))
                .andExpect(flash().attribute("error", "Ya existe un rol con ese nombre"));
    }

    @Test
    void editFormHidesPermissionsAndStatusForSystemRole() throws Exception {
        AppRole admin = role(1L, "ADMIN");
        when(roleService.findById(1L)).thenReturn(admin);
        when(roleService.isSystemRole(admin)).thenReturn(true);
        mvc.perform(get("/roles/1/editar")).andExpect(status().isOk())
                .andExpect(view().name("roles/formulario"))
                .andExpect(content().string(containsString("/roles/1/editar")))
                .andExpect(content().string(containsString("readonly")))
                .andExpect(content().string(not(containsString("name=\"permissionIds\""))))
                .andExpect(content().string(not(containsString("<select"))));
    }

    @Test
    void editFormForCustomRoleShowsStatus() throws Exception {
        when(roleService.findById(4L)).thenReturn(role(4L, "PET_WALKER"));
        mvc.perform(get("/roles/4/editar")).andExpect(status().isOk())
                .andExpect(content().string(containsString("PET_WALKER")))
                .andExpect(content().string(containsString("name=\"activeFlag\"")))
                .andExpect(content().string(containsString("<select")));
    }

    @Test
    void editMissingRoleReturnsToList() throws Exception {
        when(roleService.findById(99L)).thenThrow(new RuntimeException("Rol no encontrado"));
        mvc.perform(get("/roles/99/editar")).andExpect(redirectedUrl("/roles"))
                .andExpect(flash().attribute("error", "Rol no encontrado"));
    }

    @Test
    void invalidIdIsHandledWithoutJsonBody() throws Exception {
        mvc.perform(get("/roles/abc/editar")).andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("\"error\""))));
        verifyNoInteractions(roleService);
    }

    @Test
    void updateDelegatesAndRedirects() throws Exception {
        mvc.perform(post("/roles/4/editar").with(csrf()).param("roleName", "PET_WALKER")
                        .param("description", "Paseos diarios").param("activeFlag", "N"))
                .andExpect(redirectedUrl("/roles"))
                .andExpect(flash().attribute("message", "Rol actualizado correctamente"));
        verify(roleService).update(eq(4L), argThat(r -> "PET_WALKER".equals(r.getRoleName())
                && "N".equals(r.getActiveFlag()) && "Paseos diarios".equals(r.getDescription())));
    }

    @Test
    void failedUpdateReturnsToFormWithMessage() throws Exception {
        when(roleService.update(eq(1L), any()))
                .thenThrow(new RuntimeException("El rol ADMIN no se puede renombrar ni desactivar"));
        mvc.perform(post("/roles/1/editar").with(csrf()).param("roleName", "ADMIN").param("activeFlag", "N"))
                .andExpect(redirectedUrl("/roles/1/editar"))
                .andExpect(flash().attribute("error", "El rol ADMIN no se puede renombrar ni desactivar"));
    }

    @Test
    void deleteDelegatesAndRedirects() throws Exception {
        mvc.perform(post("/roles/4/eliminar").with(csrf()))
                .andExpect(redirectedUrl("/roles"))
                .andExpect(flash().attribute("message", "Rol eliminado correctamente"));
        verify(roleService).delete(4L);
    }

    @Test
    void deletingAssignedRoleShowsServiceMessage() throws Exception {
        doThrow(new RuntimeException("No se puede eliminar un rol asignado a usuarios"))
                .when(roleService).delete(4L);
        mvc.perform(post("/roles/4/eliminar").with(csrf()))
                .andExpect(redirectedUrl("/roles"))
                .andExpect(flash().attribute("error", "No se puede eliminar un rol asignado a usuarios"));
    }

    @Test
    void permissionAndCsrfAreRequired() throws Exception {
        mvc.perform(get("/roles").with(user("owner").roles("OWNER"))).andExpect(status().isForbidden());
        mvc.perform(get("/roles/nuevo").with(user("manager")
                        .authorities(new SimpleGrantedAuthority("MANAGE_USERS"))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/roles/4/eliminar")).andExpect(status().isForbidden());
        mvc.perform(post("/roles/crear").with(csrf()).with(user("owner").roles("OWNER"))
                        .param("roleName", "X").param("permissionIds", "3"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(roleService, permissionService);
    }

    @Test
    void permissionsPageShowsAssignedAndOnlyAvailableOptions() throws Exception {
        AppPermission createRequest = permission(3L, "CREATE_REQUEST");
        AppPermission viewReports = permission(5L, "VIEW_REPORTS");
        when(roleService.findById(4L)).thenReturn(role(4L, "PET_WALKER"));
        when(roleService.findPermissions(4L)).thenReturn(List.of(createRequest));
        when(permissionService.findAll()).thenReturn(List.of(createRequest, viewReports));
        mvc.perform(get("/roles/4/permisos")).andExpect(status().isOk())
                .andExpect(view().name("roles/permisos"))
                .andExpect(model().attribute("availablePermissions", List.of(viewReports)))
                .andExpect(content().string(containsString("CREATE_REQUEST")))
                .andExpect(content().string(containsString("VIEW_REPORTS")))
                .andExpect(content().string(containsString("/roles/4/permisos/3/quitar")))
                .andExpect(content().string(containsString("/roles/4/permisos/asignar")))
                .andExpect(content().string(containsString("disabled=\"disabled\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void permissionsPageHandlesNoAvailablePermissions() throws Exception {
        when(roleService.findById(4L)).thenReturn(role(4L, "PET_WALKER"));
        mvc.perform(get("/roles/4/permisos")).andExpect(status().isOk())
                .andExpect(content().string(containsString("No hay otros permisos disponibles")));
    }

    @Test
    void permissionsOfMissingRoleRedirectToList() throws Exception {
        when(roleService.findById(99L)).thenThrow(new RuntimeException("Rol no encontrado"));
        mvc.perform(get("/roles/99/permisos")).andExpect(redirectedUrl("/roles"))
                .andExpect(flash().attribute("error", "Rol no encontrado"));
    }

    @Test
    void assignPermissionDelegatesAndRedirects() throws Exception {
        mvc.perform(post("/roles/4/permisos/asignar").with(csrf()).param("permissionId", "5"))
                .andExpect(redirectedUrl("/roles/4/permisos"))
                .andExpect(flash().attribute("message", "Permiso asignado correctamente"));
        verify(roleService).assignPermission(4L, 5L);
    }

    @Test
    void assigningWithoutSelectionDoesNotCallService() throws Exception {
        mvc.perform(post("/roles/4/permisos/asignar").with(csrf()))
                .andExpect(redirectedUrl("/roles/4/permisos"))
                .andExpect(flash().attribute("error", "Selecciona un permiso"));
        verifyNoInteractions(roleService);
    }

    @Test
    void duplicatePermissionShowsServiceMessage() throws Exception {
        doThrow(new RuntimeException("El rol ya tiene este permiso")).when(roleService).assignPermission(4L, 3L);
        mvc.perform(post("/roles/4/permisos/asignar").with(csrf()).param("permissionId", "3"))
                .andExpect(redirectedUrl("/roles/4/permisos"))
                .andExpect(flash().attribute("error", "El rol ya tiene este permiso"));
    }

    @Test
    void removePermissionDelegatesAndRedirects() throws Exception {
        mvc.perform(post("/roles/4/permisos/3/quitar").with(csrf()))
                .andExpect(redirectedUrl("/roles/4/permisos"))
                .andExpect(flash().attribute("message", "Permiso retirado correctamente"));
        verify(roleService).removePermission(4L, 3L);
    }

    @Test
    void removingLastPermissionShowsErrorEvenIfPostIsSentDirectly() throws Exception {
        doThrow(new RuntimeException("No se puede dejar el rol sin permisos"))
                .when(roleService).removePermission(4L, 3L);
        mvc.perform(post("/roles/4/permisos/3/quitar").with(csrf()))
                .andExpect(redirectedUrl("/roles/4/permisos"))
                .andExpect(flash().attribute("error", "No se puede dejar el rol sin permisos"));
    }

    @Test
    void rolePermissionActionsRequirePermissionAndCsrf() throws Exception {
        mvc.perform(get("/roles/4/permisos").with(user("owner").roles("OWNER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/roles/4/permisos/asignar").param("permissionId", "5"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/roles/4/permisos/3/quitar"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/roles/4/permisos/3/quitar").with(csrf()).with(user("manager")
                        .authorities(new SimpleGrantedAuthority("MANAGE_USERS"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(roleService, permissionService);
    }
}
