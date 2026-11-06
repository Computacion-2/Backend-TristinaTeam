package com.icesi.cuidapetproyect.api;

import com.icesi.cuidapetproyect.api.security.JwtService;
import com.icesi.cuidapetproyect.api.dto.RolePermissionRequest;
import com.icesi.cuidapetproyect.api.service.AssociationService;
import com.icesi.cuidapetproyect.exception.ConflictException;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import com.icesi.cuidapetproyect.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.transaction.TestTransaction;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.jwt.secret=dGVzdC1vbmx5LWtleS0zMi1ieXRlcy1taW5pbXVtLWxvbmc=")
@AutoConfigureMockMvc
@Transactional
class ApiCrudIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserService userService;
    @Autowired AppRoleService roleService;
    @Autowired AppPermissionService permissionService;
    @Autowired JwtService tokens;
    @Autowired AssociationService associations;
    @Autowired UserRoleRepository userRoles;
    @Autowired RolePermissionRepository rolePermissions;
    @Autowired AppUserRepository users;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean AppUserDetailsService details;
    private String authorization;
    private String suffix;
    private AppUser actor;
    private AppRole roleA, roleB, roleC;
    private AppPermission permissionA, permissionB, permissionC;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        permissionA = permission("A");
        permissionB = permission("B");
        permissionC = permission("C");
        roleA = role("A");
        roleB = role("B");
        roleC = role("C");
        actor = new AppUser();
        actor.setEmail("test-" + suffix + "@example.com");
        actor.setPasswordHash("password123");
        actor = userService.create(actor, List.of(roleA.getId()));
        var principal = User.withUsername(actor.getEmail()).password("unused")
                .authorities("ROLE_ADMIN", "MANAGE_USERS", "MANAGE_ROLES").build();
        when(details.loadUserByUsername(actor.getEmail())).thenReturn(principal);
        authorization = "Bearer " + tokens.createToken(principal).accessToken();
    }

    private AppPermission permission(String label) {
        AppPermission permission = new AppPermission();
        permission.setPermissionCode("TEST_" + label + suffix);
        permission.setPermissionName("Prueba " + label + suffix);
        return permissionService.create(permission);
    }

    @AfterEach
    void cleanCommittedFixture() {
        // La prueba de rollback necesita transacciones reales independientes.
        // Las demás pruebas revierten automáticamente sus datos.
        if (!TestTransaction.isActive() && actor != null) {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                userRoles.deleteAll(userRoles.findByUser_Id(actor.getId()));
                userRoles.flush();
                users.deleteById(actor.getId());
                users.flush();
                for (AppRole role : List.of(roleA, roleB, roleC)) roleService.delete(role.getId());
                for (AppPermission permission : List.of(permissionA, permissionB, permissionC)) {
                    permissionService.delete(permission.getId());
                }
            });
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failedReplacementRollsBackNewAssociationAndKeepsPreviousOne() {
        assertThrows(ConflictException.class, () -> associations.updateRolePermission(
                roleA.getId(), permissionA.getId(),
                new RolePermissionRequest(roleB.getId(), permissionB.getId())));
        assertTrue(rolePermissions.existsById(new RolePermissionId(roleA.getId(), permissionA.getId())));
        assertFalse(rolePermissions.existsById(new RolePermissionId(roleB.getId(), permissionB.getId())));
    }

    private AppRole role(String label) {
        AppRole role = new AppRole();
        role.setRoleName("TEST_" + label + suffix);
        return roleService.create(role, List.of(permissionA.getId()));
    }

    private long createdId(String route, String json) throws Exception {
        var result = mvc.perform(post(route).header("Authorization", authorization)
                        .contentType("application/json").content(json))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber()).andReturn();
        return JsonMapper.builder().build().readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void userCrudUsesDtosAndLogicalDelete() throws Exception {
        String email = "new-" + suffix + "@example.com";
        long id = createdId("/api/v1/users", """
                {"email":"%s","password":"password123","roleIds":[%d]}
                """.formatted(email, roleA.getId()));
        mvc.perform(get("/api/v1/users").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(content().string(not(containsString("passwordHash"))));
        mvc.perform(get("/api/v1/users/" + id).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(put("/api/v1/users/" + id).header("Authorization", authorization)
                        .contentType("application/json").content("""
                        {"email":"%s","password":null,"accountStatus":"INACTIVE"}
                        """.formatted(email)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accountStatus").value("INACTIVE"));
        mvc.perform(delete("/api/v1/users/" + id).header("Authorization", authorization))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get("/api/v1/users/" + id).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accountStatus").value("DELETED"));
    }

    @Test
    void roleCrud() throws Exception {
        long id = createdId("/api/v1/roles", """
                {"roleName":"TEST_NEW_%s","description":"Prueba","permissionIds":[%d]}
                """.formatted(suffix, permissionA.getId()));
        mvc.perform(get("/api/v1/roles").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(get("/api/v1/roles/" + id).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activeFlag").value("Y"));
        mvc.perform(put("/api/v1/roles/" + id).header("Authorization", authorization)
                        .contentType("application/json").content("""
                        {"roleName":"TEST_NEW_%s","description":"Editado","activeFlag":"N"}
                        """.formatted(suffix)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activeFlag").value("N"));
        mvc.perform(delete("/api/v1/roles/" + id).header("Authorization", authorization))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/roles/" + id).header("Authorization", authorization))
                .andExpect(status().isNotFound());
    }

    @Test
    void permissionCrudKeepsCode() throws Exception {
        String code = "TEST_NEW_" + suffix;
        long id = createdId("/api/v1/permissions", """
                {"permissionCode":"%s","permissionName":"Nuevo %s"}
                """.formatted(code, suffix));
        mvc.perform(get("/api/v1/permissions").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(get("/api/v1/permissions/" + id).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.permissionCode").value(code));
        mvc.perform(put("/api/v1/permissions/" + id).header("Authorization", authorization)
                        .contentType("application/json").content("""
                        {"permissionName":"Editado %s","description":"Cambio"}
                        """.formatted(suffix)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.permissionCode").value(code));
        mvc.perform(delete("/api/v1/permissions/" + id).header("Authorization", authorization))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/permissions/" + id).header("Authorization", authorization))
                .andExpect(status().isNotFound());
    }

    @Test
    void userRoleCrudUsesCompositeKeyAndAuthenticatedActor() throws Exception {
        String route = "/api/v1/user-roles/" + actor.getId() + "/";
        mvc.perform(post("/api/v1/user-roles").header("Authorization", authorization)
                        .contentType("application/json").content("""
                        {"userId":%d,"roleId":%d}
                        """.formatted(actor.getId(), roleB.getId())))
                .andExpect(status().isCreated()).andExpect(header().string("Location", endsWith(route + roleB.getId())))
                .andExpect(jsonPath("$.assignedById").value(actor.getId()))
                .andExpect(jsonPath("$.assignedAt").isNotEmpty());
        mvc.perform(get("/api/v1/user-roles").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(get(route + roleB.getId()).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roleId").value(roleB.getId()));
        mvc.perform(put(route + roleB.getId()).header("Authorization", authorization)
                        .contentType("application/json").content("""
                        {"userId":%d,"roleId":%d}
                        """.formatted(actor.getId(), roleC.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roleId").value(roleC.getId()));
        mvc.perform(get(route + roleB.getId()).header("Authorization", authorization))
                .andExpect(status().isNotFound());
        mvc.perform(delete(route + roleC.getId()).header("Authorization", authorization))
                .andExpect(status().isNoContent());
        mvc.perform(delete(route + roleA.getId()).header("Authorization", authorization))
                .andExpect(status().isConflict());
    }

    @Test
    void rolePermissionCrudProtectsLastPermission() throws Exception {
        String route = "/api/v1/role-permissions/" + roleA.getId() + "/";
        mvc.perform(post("/api/v1/role-permissions").header("Authorization", authorization)
                        .contentType("application/json").content("""
                        {"roleId":%d,"permissionId":%d}
                        """.formatted(roleA.getId(), permissionB.getId())))
                .andExpect(status().isCreated()).andExpect(header().string("Location", endsWith(route + permissionB.getId())))
                .andExpect(jsonPath("$.grantedAt").isNotEmpty());
        mvc.perform(get("/api/v1/role-permissions").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(get(route + permissionB.getId()).header("Authorization", authorization))
                .andExpect(status().isOk());
        mvc.perform(put(route + permissionB.getId()).header("Authorization", authorization)
                        .contentType("application/json").content("""
                        {"roleId":%d,"permissionId":%d}
                        """.formatted(roleA.getId(), permissionC.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.permissionId").value(permissionC.getId()));
        mvc.perform(delete(route + permissionC.getId()).header("Authorization", authorization))
                .andExpect(status().isNoContent());
        mvc.perform(delete(route + permissionA.getId()).header("Authorization", authorization))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidInputMissingResourceAndDuplicatesHaveSpecificStatus() throws Exception {
        mvc.perform(get("/api/v1/users/-1").header("Authorization", authorization))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/users/999999999").header("Authorization", authorization))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/users").header("Authorization", authorization)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/users").header("Authorization", authorization)
                        .contentType("application/json").content("""
                        {"email":"%s","password":"password123","roleIds":[%d]}
                        """.formatted(actor.getEmail(), roleA.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void ownerCannotAccessAnyOfTheFiveControllers() throws Exception {
        var owner = User.withUsername("owner@example.com").password("unused").roles("OWNER").build();
        when(details.loadUserByUsername(owner.getUsername())).thenReturn(owner);
        String token = "Bearer " + tokens.createToken(owner).accessToken();
        for (String path : List.of("users", "roles", "permissions", "user-roles", "role-permissions")) {
            mvc.perform(get("/api/v1/" + path).header("Authorization", token))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/" + path).header("Authorization", token)
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void swaggerIncludesFiveOperationsForEachEntity() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var paths = JsonMapper.builder().build().readTree(result.getResponse().getContentAsString()).get("paths");
        for (String collection : List.of("users", "roles", "permissions", "user-roles", "role-permissions")) {
            String path = "/api/v1/" + collection;
            String detail = switch (collection) {
                case "user-roles" -> path + "/{userId}/{roleId}";
                case "role-permissions" -> path + "/{roleId}/{permissionId}";
                default -> path + "/{id}";
            };
            org.junit.jupiter.api.Assertions.assertTrue(paths.get(path).has("get"));
            org.junit.jupiter.api.Assertions.assertTrue(paths.get(path).has("post"));
            for (String method : List.of("get", "put", "delete")) {
                org.junit.jupiter.api.Assertions.assertTrue(paths.get(detail).has(method));
            }
        }
    }
}
