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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class, AppUserController.class, AppRoleController.class, AppPermissionController.class})
@Import({SecurityConfig.class, PasswordConfig.class})
class AuthControllerSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockitoBean AuthService authService;
    @MockitoBean AppUserDetailsService userDetailsService;
    @MockitoBean AppUserService userService;
    @MockitoBean AppRoleService roleService;
    @MockitoBean AppPermissionService permissionService;

    @BeforeEach
    void configureSecurity(@Autowired WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void publicFormsRenderWithCssAndCsrf() throws Exception {
        for (String path : List.of("/login", "/register")) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("/css/styles.css")))
                    .andExpect(content().string(containsString("name=\"_csrf\"")));
        }
        mvc.perform(get("/css/styles.css")).andExpect(status().isOk());
    }

    @Test
    void registrationRedirectsAndIgnoresInjectedRoles() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("email", "new@icesi.edu.co")
                        .param("password", "Clave1234").param("roleIds", "1").param("accountStatus", "ADMIN"))
                .andExpect(redirectedUrl("/login?registered"));
        verify(authService).register("new@icesi.edu.co", "Clave1234");
    }

    @Test
    void invalidRegistrationRedirectsWithoutSavingPassword() throws Exception {
        var result = mvc.perform(post("/register").with(csrf()).param("email", "invalid")
                        .param("password", "123"))
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attributeExists("error")).andReturn();
        RegistrationForm form = (RegistrationForm) result.getFlashMap().get("registrationForm");
        org.junit.jupiter.api.Assertions.assertNull(form.getPassword());
        verifyNoInteractions(authService);
    }

    @Test
    void duplicateEmailReturnsUsefulMessage() throws Exception {
        when(authService.register("used@icesi.edu.co", "Clave1234"))
                .thenThrow(new IllegalArgumentException("El correo ya está registrado"));
        mvc.perform(post("/register").with(csrf()).param("email", "used@icesi.edu.co")
                        .param("password", "Clave1234"))
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("error", "El correo ya está registrado"));
    }

    @Test
    void postWithoutCsrfIsRejectedEvenOnPublicRegistration() throws Exception {
        mvc.perform(post("/register").param("email", "new@icesi.edu.co").param("password", "Clave1234"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/users/delete/1").with(user("admin")
                        .authorities(new SimpleGrantedAuthority("MANAGE_USERS"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(authService, userService);
    }

    @Test
    void loginAuthenticatesUsingEncodedPassword() throws Exception {
        when(userDetailsService.loadUserByUsername("owner@icesi.edu.co"))
                .thenReturn(User.withUsername("owner@icesi.edu.co").password(encoder.encode("Clave1234"))
                        .roles("OWNER").build());
        mvc.perform(post("/login").with(csrf()).param("email", "owner@icesi.edu.co")
                        .param("password", "Clave1234"))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("owner@icesi.edu.co"));
    }

    @Test
    void wrongPasswordAndDisabledAccountCannotLogin() throws Exception {
        when(userDetailsService.loadUserByUsername("owner@icesi.edu.co"))
                .thenReturn(User.withUsername("owner@icesi.edu.co").password(encoder.encode("Clave1234"))
                        .roles("OWNER").build());
        mvc.perform(post("/login").with(csrf()).param("email", "owner@icesi.edu.co").param("password", "wrong"))
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
        when(userDetailsService.loadUserByUsername("owner@icesi.edu.co"))
                .thenReturn(User.withUsername("owner@icesi.edu.co").password(encoder.encode("Clave1234"))
                        .disabled(true).roles("OWNER").build());
        mvc.perform(post("/login").with(csrf()).param("email", "owner@icesi.edu.co").param("password", "Clave1234"))
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
    }

    @Test
    void homeShowsCurrentUserAndLogoutEndsSession() throws Exception {
        mvc.perform(get("/").with(user("owner@icesi.edu.co").roles("OWNER")))
                .andExpect(status().isOk()).andExpect(view().name("home"))
                .andExpect(content().string(containsString("owner@icesi.edu.co")));
        mvc.perform(post("/logout").with(csrf()).with(user("owner")))
                .andExpect(redirectedUrl("/login?logout")).andExpect(unauthenticated());
    }
}
