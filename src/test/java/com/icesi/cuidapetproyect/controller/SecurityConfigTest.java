package com.icesi.cuidapetproyect.controller;

import com.icesi.cuidapetproyect.config.*;
import com.icesi.cuidapetproyect.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AppUserController.class, AppRoleController.class, AppPermissionController.class})
@Import({SecurityConfig.class, PasswordConfig.class})
class SecurityConfigTest {
    @Autowired MockMvc mvc;
    @MockitoBean AppUserDetailsService userDetailsService;
    @MockitoBean AppUserService userService;
    @MockitoBean AppRoleService roleService;
    @MockitoBean AppPermissionService permissionService;

    @BeforeEach
    void configureSecurity(@Autowired WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void anonymousVisitorsMustLogin() throws Exception {
        for (String path : List.of("/", "/users", "/roles", "/permissions", "/usuarios", "/permisos")) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
    }

    @Test
    void ownerCannotManageUsersRolesOrPermissions() throws Exception {
        for (String path : List.of("/users", "/roles", "/permissions", "/usuarios", "/permisos")) {
            mvc.perform(get(path).with(user("owner").roles("OWNER")))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(userService, roleService, permissionService);
    }

    @Test
    void requiredPermissionsAllowOnlyTheirOwnModule() throws Exception {
        mvc.perform(get("/users").with(user("manager")
                        .authorities(new SimpleGrantedAuthority("MANAGE_USERS"))))
                .andExpect(status().isOk());
        for (String path : List.of("/roles", "/permissions")) {
            mvc.perform(get(path).with(user("manager")
                            .authorities(new SimpleGrantedAuthority("MANAGE_ROLES"))))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/roles").with(user("manager")
                        .authorities(new SimpleGrantedAuthority("MANAGE_USERS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void modifyingUsersRequiresPermissionAndCsrf() throws Exception {
        mvc.perform(post("/users/delete/1").with(user("manager")
                        .authorities(new SimpleGrantedAuthority("MANAGE_USERS"))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/users/delete/1").with(csrf()).with(user("owner").roles("OWNER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService);
        mvc.perform(post("/users/delete/1").with(csrf()).with(user("manager")
                        .authorities(new SimpleGrantedAuthority("MANAGE_USERS"))))
                .andExpect(status().isOk());
        verify(userService).delete(1L);
    }
}
