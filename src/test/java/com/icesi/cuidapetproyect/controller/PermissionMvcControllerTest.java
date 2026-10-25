package com.icesi.cuidapetproyect.controller;

import com.icesi.cuidapetproyect.config.*;
import com.icesi.cuidapetproyect.model.AppPermission;
import com.icesi.cuidapetproyect.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
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

@WebMvcTest(PermissionMvcController.class)
@Import({SecurityConfig.class, PasswordConfig.class})
@WithMockUser(authorities = "MANAGE_ROLES")
class PermissionMvcControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AppPermissionService permissionService;
    @MockitoBean AppUserDetailsService userDetailsService;

    @BeforeEach
    void setUp(@Autowired WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private AppPermission permission(Long id, String code, String name) {
        AppPermission permission = new AppPermission();
        permission.setId(id);
        permission.setPermissionCode(code);
        permission.setPermissionName(name);
        return permission;
    }

    @Test
    void listRendersPermissionsAndHidesDeleteForSystemPermissions() throws Exception {
        when(permissionService.findAll()).thenReturn(List.of(
                permission(1L, "MANAGE_USERS", "Gestionar usuarios"),
                permission(3L, "CREATE_REQUEST", "Crear solicitudes")));
        mvc.perform(get("/permisos")).andExpect(status().isOk())
                .andExpect(view().name("permisos/lista"))
                .andExpect(content().string(containsString("MANAGE_USERS")))
                .andExpect(content().string(containsString("Crear solicitudes")))
                .andExpect(content().string(containsString("/permisos/3/eliminar")))
                .andExpect(content().string(not(containsString("/permisos/1/eliminar"))))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void emptyListShowsMessage() throws Exception {
        mvc.perform(get("/permisos")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Todavía no hay permisos")));
    }

    @Test
    void createFormRendersCodeFieldAndCsrf() throws Exception {
        mvc.perform(get("/permisos/nuevo")).andExpect(status().isOk())
                .andExpect(view().name("permisos/formulario"))
                .andExpect(content().string(containsString("name=\"permissionCode\"")))
                .andExpect(content().string(containsString("/permisos/crear")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void createDelegatesToServiceAndRedirects() throws Exception {
        mvc.perform(post("/permisos/crear").with(csrf()).param("permissionCode", "view_reports")
                        .param("permissionName", "Ver reportes").param("description", "Consulta de reportes"))
                .andExpect(redirectedUrl("/permisos"))
                .andExpect(flash().attribute("message", "Permiso creado correctamente"));
        verify(permissionService).create(argThat(p -> "view_reports".equals(p.getPermissionCode())
                && "Ver reportes".equals(p.getPermissionName())
                && "Consulta de reportes".equals(p.getDescription())));
    }

    @Test
    void createRequiresCodeAndName() throws Exception {
        mvc.perform(post("/permisos/crear").with(csrf()).param("permissionName", "Ver reportes"))
                .andExpect(redirectedUrl("/permisos/nuevo"))
                .andExpect(flash().attribute("error", "El código del permiso es obligatorio"));
        mvc.perform(post("/permisos/crear").with(csrf()).param("permissionCode", "VIEW_REPORTS"))
                .andExpect(redirectedUrl("/permisos/nuevo"))
                .andExpect(flash().attribute("error", "El nombre del permiso es obligatorio"));
        verifyNoInteractions(permissionService);
    }

    @Test
    void createRejectsInvalidCode() throws Exception {
        var result = mvc.perform(post("/permisos/crear").with(csrf()).param("permissionCode", "VER-REPORTES")
                        .param("permissionName", "Ver reportes"))
                .andExpect(redirectedUrl("/permisos/nuevo"))
                .andExpect(flash().attributeExists("error")).andReturn();
        PermissionForm form = (PermissionForm) result.getFlashMap().get("permissionForm");
        assertEquals("Ver reportes", form.getPermissionName());
        verifyNoInteractions(permissionService);
    }

    @Test
    void duplicateCodeShowsServiceMessage() throws Exception {
        when(permissionService.create(any())).thenThrow(new RuntimeException("Ya existe un permiso con ese código"));
        mvc.perform(post("/permisos/crear").with(csrf()).param("permissionCode", "CREATE_REQUEST")
                        .param("permissionName", "Otro nombre"))
                .andExpect(redirectedUrl("/permisos/nuevo"))
                .andExpect(flash().attribute("error", "Ya existe un permiso con ese código"));
    }

    @Test
    void databaseErrorsAreNotExposed() throws Exception {
        when(permissionService.create(any())).thenThrow(new DataIntegrityViolationException("ORA-00001"));
        mvc.perform(post("/permisos/crear").with(csrf()).param("permissionCode", "CREATE_REQUEST")
                        .param("permissionName", "Otro nombre"))
                .andExpect(redirectedUrl("/permisos/nuevo"))
                .andExpect(flash().attribute("error", "No fue posible completar la operación. Inténtalo de nuevo."));
    }

    @Test
    void editFormShowsCodeAsReadOnly() throws Exception {
        when(permissionService.findById(3L)).thenReturn(permission(3L, "CREATE_REQUEST", "Crear solicitudes"));
        mvc.perform(get("/permisos/3/editar")).andExpect(status().isOk())
                .andExpect(view().name("permisos/formulario"))
                .andExpect(content().string(containsString("/permisos/3/editar")))
                .andExpect(content().string(containsString("CREATE_REQUEST")))
                .andExpect(content().string(containsString("readonly")))
                .andExpect(content().string(not(containsString("name=\"permissionCode\""))));
    }

    @Test
    void editMissingPermissionReturnsToList() throws Exception {
        when(permissionService.findById(99L)).thenThrow(new RuntimeException("Permiso no encontrado"));
        mvc.perform(get("/permisos/99/editar")).andExpect(redirectedUrl("/permisos"))
                .andExpect(flash().attribute("error", "Permiso no encontrado"));
    }

    @Test
    void updateDelegatesWithoutChangingCode() throws Exception {
        mvc.perform(post("/permisos/3/editar").with(csrf()).param("permissionCode", "HACKED")
                        .param("permissionName", "Crear solicitudes de cuidado"))
                .andExpect(redirectedUrl("/permisos"))
                .andExpect(flash().attribute("message", "Permiso actualizado correctamente"));
        verify(permissionService).update(eq(3L), argThat(p -> p.getPermissionCode() == null
                && "Crear solicitudes de cuidado".equals(p.getPermissionName())));
    }

    @Test
    void invalidUpdateReturnsToForm() throws Exception {
        mvc.perform(post("/permisos/3/editar").with(csrf()).param("permissionName", " "))
                .andExpect(redirectedUrl("/permisos/3/editar"))
                .andExpect(flash().attribute("error", "El nombre del permiso es obligatorio"));
        verifyNoInteractions(permissionService);
    }

    @Test
    void failedUpdateShowsServiceMessage() throws Exception {
        when(permissionService.update(eq(3L), any())).thenThrow(new RuntimeException("Ya existe otro permiso con ese nombre"));
        mvc.perform(post("/permisos/3/editar").with(csrf()).param("permissionName", "Gestionar roles"))
                .andExpect(redirectedUrl("/permisos/3/editar"))
                .andExpect(flash().attribute("error", "Ya existe otro permiso con ese nombre"));
    }

    @Test
    void deleteDelegatesAndRedirects() throws Exception {
        mvc.perform(post("/permisos/3/eliminar").with(csrf()))
                .andExpect(redirectedUrl("/permisos"))
                .andExpect(flash().attribute("message", "Permiso eliminado correctamente"));
        verify(permissionService).delete(3L);
    }

    @Test
    void failedDeleteShowsServiceMessage() throws Exception {
        doThrow(new RuntimeException("No se puede eliminar el último permiso de un rol"))
                .when(permissionService).delete(3L);
        mvc.perform(post("/permisos/3/eliminar").with(csrf()))
                .andExpect(redirectedUrl("/permisos"))
                .andExpect(flash().attribute("error", "No se puede eliminar el último permiso de un rol"));
    }

    @Test
    void permissionAndCsrfAreRequired() throws Exception {
        mvc.perform(get("/permisos").with(user("owner").roles("OWNER"))).andExpect(status().isForbidden());
        mvc.perform(get("/permisos").with(user("manager").authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("MANAGE_USERS"))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/permisos/3/eliminar")).andExpect(status().isForbidden());
        mvc.perform(post("/permisos/crear").with(csrf()).with(user("owner").roles("OWNER"))
                        .param("permissionCode", "X").param("permissionName", "X"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(permissionService);
    }
}
