package com.icesi.cuidapetproyect.api;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.*;
import com.icesi.cuidapetproyect.model.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiMapperTest {
    private final AppUserMapper users = Mappers.getMapper(AppUserMapper.class);
    private final AppRoleMapper roles = Mappers.getMapper(AppRoleMapper.class);
    private final AppPermissionMapper permissions = Mappers.getMapper(AppPermissionMapper.class);
    private final UserRoleMapper userRoles = Mappers.getMapper(UserRoleMapper.class);
    private final RolePermissionMapper rolePermissions = Mappers.getMapper(RolePermissionMapper.class);

    @Test
    void userResponseDoesNotExposePasswordOrEntityRelationships() {
        AppUser user = new AppUser();
        user.setId(7L);
        user.setEmail("juan@example.com");
        user.setPasswordHash("secret-hash");
        user.setCreatedAt(LocalDateTime.of(2026, 11, 5, 12, 0));
        UserRole relation = new UserRole();
        relation.setUser(user);
        user.getUserRoles().add(relation);

        UserResponse response = users.entityToDto(user);
        var json = JsonMapper.builder().build().valueToTree(response);
        assertEquals(7L, response.id());
        assertEquals(user.getCreatedAt(), response.createdAt());
        assertEquals("juan@example.com", response.email());
        assertFalse(json.has("password"));
        assertFalse(json.has("passwordHash"));
        assertFalse(json.has("userRoles"));
        assertFalse(json.has("profile"));
        assertFalse(json.toString().contains("secret-hash"));
    }

    @Test
    void newUserKeepsServerDefaultsAndPasswordForServiceToEncode() {
        AppUser user = users.dtoToEntity(
                new UserCreateRequest("juan@example.com", "password123", List.of(2L)));
        assertEquals("ACTIVE", user.getAccountStatus());
        assertEquals("password123", user.getPasswordHash());
        assertNull(user.getId());
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());
        assertNull(user.getProfile());
        assertTrue(user.getUserRoles().isEmpty());
    }

    @Test
    void updateCanOmitPasswordAndCannotSupplyRelationships() {
        AppUser data = users.dtoToEntity(
                new UserUpdateRequest("nuevo@example.com", null, "INACTIVE"));
        assertNull(data.getPasswordHash());
        assertEquals("INACTIVE", data.getAccountStatus());
        assertNull(data.getId());
        assertTrue(data.getUserRoles().isEmpty());
    }

    @Test
    void roleCreationPreservesActiveDefaultAndUpdateMapsFlag() {
        AppRole role = roles.dtoToEntity(
                new RoleCreateRequest("OWNER", "Dueño", List.of(1L)));
        assertEquals("Y", role.getActiveFlag());
        assertTrue(role.getRolePermissions().isEmpty());
        assertNull(role.getCreatedAt());
        AppRole update = roles.dtoToEntity(new RoleUpdateRequest("OWNER", "Nueva", "N"));
        assertEquals("N", roles.entityToDto(update).activeFlag());
        assertEquals("Nueva", roles.entityToDto(update).description());
    }

    @Test
    void permissionUpdateDoesNotReplaceCode() {
        AppPermission created = permissions.dtoToEntity(
                new PermissionCreateRequest("READ_PETS", "Ver mascotas", "Lectura"));
        assertEquals("READ_PETS", permissions.entityToDto(created).permissionCode());
        AppPermission update = permissions.dtoToEntity(
                new PermissionUpdateRequest("Consultar mascotas", "Nueva"));
        assertNull(update.getPermissionCode());
        assertNull(update.getId());
        assertNull(update.getCreatedAt());
        assertEquals("Consultar mascotas", update.getPermissionName());
    }

    @Test
    void userRoleMapsCompositeKeyAndDoesNotAcceptAuditFields() {
        UserRole relation = userRoles.dtoToEntity(new UserRoleRequest(7L, 2L));
        assertEquals(new UserRoleId(7L, 2L), relation.getId());
        assertEquals(7L, relation.getUser().getId());
        assertEquals(2L, relation.getRole().getId());
        assertNull(relation.getAssignedBy());
        assertNull(relation.getAssignedAt());
        assertNull(userRoles.entityToDto(relation).assignedById());
        AppUser administrator = new AppUser();
        administrator.setId(1L);
        relation.setAssignedBy(administrator);
        relation.setAssignedAt(LocalDateTime.of(2026, 11, 5, 12, 0));
        UserRoleResponse response = userRoles.entityToDto(relation);
        assertEquals(7L, response.userId());
        assertEquals(2L, response.roleId());
        assertEquals(1L, response.assignedById());
        assertEquals(relation.getAssignedAt(), response.assignedAt());
    }

    @Test
    void rolePermissionMapsCompositeKeyAndAuditDate() {
        RolePermission relation = rolePermissions.dtoToEntity(new RolePermissionRequest(2L, 3L));
        assertEquals(new RolePermissionId(2L, 3L), relation.getId());
        assertEquals(2L, relation.getRole().getId());
        assertEquals(3L, relation.getPermission().getId());
        assertNull(relation.getGrantedAt());
        relation.setGrantedAt(LocalDateTime.of(2026, 11, 5, 12, 0));
        RolePermissionResponse response = rolePermissions.entityToDto(relation);
        assertEquals(2L, response.roleId());
        assertEquals(3L, response.permissionId());
        assertEquals(relation.getGrantedAt(), response.grantedAt());
    }
}
