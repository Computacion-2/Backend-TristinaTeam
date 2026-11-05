package com.icesi.cuidapetproyect.api;

import com.icesi.cuidapetproyect.api.security.JwtService;
import com.icesi.cuidapetproyect.service.AppUserDetailsService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.jwt.secret=dGVzdC1vbmx5LWtleS0zMi1ieXRlcy1taW5pbXVtLWxvbmc=")
@AutoConfigureMockMvc
class ApiAuthenticationTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtService tokens;
    @Autowired JwtDecoder decoder;
    @MockitoBean AppUserDetailsService users;
    private UserDetails owner;
    private UserDetails manager;

    @BeforeEach
    void setUp() {
        String hash = passwords.encode("password123");
        owner = User.withUsername("owner@example.com").password(hash).roles("OWNER").build();
        manager = User.withUsername("admin@example.com").password(hash)
                .authorities("ROLE_ADMIN", "MANAGE_USERS", "MANAGE_ROLES").build();
        when(users.loadUserByUsername(owner.getUsername())).thenReturn(owner);
        when(users.loadUserByUsername(manager.getUsername())).thenReturn(manager);
    }

    @Test
    void loginCreatesSignedTokenWithoutSessionOrPassword() throws Exception {
        var result = mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\"owner@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.roles[0]").value("OWNER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        assertNull(result.getRequest().getSession(false));
        String token = JsonMapper.builder().build().readTree(
                result.getResponse().getContentAsString()).get("accessToken").asString();
        Jwt jwt = decoder.decode(token);
        assertEquals(owner.getUsername(), jwt.getSubject());
        assertEquals("OWNER", jwt.getClaimAsStringList("roles").getFirst());
        assertNotNull(jwt.getIssuedAt());
        assertTrue(jwt.getExpiresAt().isAfter(jwt.getIssuedAt()));
        assertFalse(jwt.getClaims().containsKey("password"));
        assertFalse(jwt.getClaims().containsKey("passwordHash"));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(owner.getUsername()));
    }

    @Test
    void invalidCredentialsReturn401NotRedirectOr500() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\"owner@example.com\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void invalidInputAndMalformedJsonReturn400() throws Exception {
        for (String body : new String[]{"{}", "{", "{\"email\":\"invalido\",\"password\":\"x\"}"}) {
            mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void missingOrMalformedTokenReturnsJson401() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").exists())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    void insufficientPermissionsReturn403() throws Exception {
        String token = tokens.createToken(owner).accessToken();
        for (String path : new String[]{"/api/v1/users", "/api/v1/roles", "/api/v1/permissions"}) {
            mvc.perform(get(path).header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").exists());
        }
    }

    @Test
    void permissionsAreReloadedInsteadOfTrustingOldClaims() throws Exception {
        String token = tokens.createToken(manager).accessToken();
        UserDetails demoted = User.withUsername(manager.getUsername()).password("unused")
                .roles("OWNER").build();
        when(users.loadUserByUsername(manager.getUsername())).thenReturn(demoted);
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("OWNER"));
    }

    @Test
    void disabledOrDeletedUserCannotUsePreviouslyIssuedToken() throws Exception {
        String token = tokens.createToken(owner).accessToken();
        when(users.loadUserByUsername(owner.getUsername())).thenReturn(
                User.withUserDetails(owner).disabled(true).build());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        when(users.loadUserByUsername(owner.getUsername())).thenThrow(new UsernameNotFoundException("missing"));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        when(users.loadUserByUsername(owner.getUsername())).thenReturn(
                User.withUserDetails(owner).disabled(true).build());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\"owner@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mvcSessionDoesNotAuthenticateApiAndMvcKeepsCsrf() throws Exception {
        mvc.perform(post("/login").param("email", owner.getUsername()).param("password", "password123"))
                .andExpect(status().isForbidden());
        var login = mvc.perform(post("/login").with(csrf())
                        .param("email", owner.getUsername()).param("password", "password123"))
                .andExpect(status().is3xxRedirection()).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiTokenDoesNotAuthenticateMvc() throws Exception {
        mvc.perform(get("/").header("Authorization", "Bearer " + tokens.createToken(owner).accessToken()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    void swaggerDocumentsAuthenticationAndBearerScheme() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/me'].get").exists())
                .andExpect(jsonPath("$.paths['/users']").doesNotExist());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
