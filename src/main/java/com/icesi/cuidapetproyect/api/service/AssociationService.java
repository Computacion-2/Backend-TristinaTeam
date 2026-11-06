package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.*;
import com.icesi.cuidapetproyect.exception.*;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import com.icesi.cuidapetproyect.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssociationService {
    private final UserRoleRepository userRoles;
    private final RolePermissionRepository rolePermissions;
    private final AppUserRepository users;
    private final AppUserService userService;
    private final AppRoleService roleService;
    private final AppPermissionService permissionService;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public List<UserRoleResponse> findUserRoles() {
        return userRoles.findAll().stream().map(userRoleMapper::entityToDto).toList();
    }

    public UserRoleResponse findUserRole(Long userId, Long roleId) {
        return userRoleMapper.entityToDto(requireUserRole(userId, roleId));
    }

    private UserRole requireUserRole(Long userId, Long roleId) {
        return userRoles.findById(new UserRoleId(userId, roleId))
                .orElseThrow(() -> new ResourceNotFoundException("Asignación de rol no encontrada"));
    }

    @Transactional
    public UserRoleResponse createUserRole(UserRoleRequest dto, String actorEmail) {
        AppUser user = userService.findById(dto.userId());
        AppRole role = roleService.findById(dto.roleId());
        if (!"Y".equals(role.getActiveFlag())) {
            throw new ConflictException("No se puede asignar un rol inactivo");
        }
        if (rolePermissions.findByRole_Id(role.getId()).isEmpty()) {
            throw new ConflictException("No se puede asignar un rol sin permisos");
        }
        UserRole entity = userRoleMapper.dtoToEntity(dto);
        if (userRoles.existsById(entity.getId())) {
            throw new ConflictException("El usuario ya tiene este rol");
        }
        entity.setUser(user);
        entity.setRole(role);
        entity.setAssignedBy(users.findByEmail(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado")));
        return userRoleMapper.entityToDto(userRoles.saveAndFlush(entity));
    }

    @Transactional
    public UserRoleResponse updateUserRole(Long userId, Long roleId, UserRoleRequest dto, String actorEmail) {
        UserRole previous = requireUserRole(userId, roleId);
        if (previous.getId().equals(new UserRoleId(dto.userId(), dto.roleId()))) {
            return userRoleMapper.entityToDto(previous);
        }
        // Insertar primero permite sustituir el único rol del mismo usuario.
        // Si retirar el anterior viola una regla, se revierte toda la transacción.
        UserRoleResponse result = createUserRole(dto, actorEmail);
        userService.removeRole(userId, roleId);
        userRoles.flush();
        return result;
    }

    @Transactional
    public void deleteUserRole(Long userId, Long roleId) {
        requireUserRole(userId, roleId);
        userService.removeRole(userId, roleId);
    }

    public List<RolePermissionResponse> findRolePermissions() {
        return rolePermissions.findAll().stream().map(rolePermissionMapper::entityToDto).toList();
    }

    public RolePermissionResponse findRolePermission(Long roleId, Long permissionId) {
        return rolePermissionMapper.entityToDto(requireRolePermission(roleId, permissionId));
    }

    private RolePermission requireRolePermission(Long roleId, Long permissionId) {
        return rolePermissions.findById(new RolePermissionId(roleId, permissionId))
                .orElseThrow(() -> new ResourceNotFoundException("Asignación de permiso no encontrada"));
    }

    @Transactional
    public RolePermissionResponse createRolePermission(RolePermissionRequest dto) {
        AppRole role = roleService.findById(dto.roleId());
        AppPermission permission = permissionService.findById(dto.permissionId());
        RolePermission entity = rolePermissionMapper.dtoToEntity(dto);
        if (rolePermissions.existsById(entity.getId())) {
            throw new ConflictException("El rol ya tiene este permiso");
        }
        entity.setRole(role);
        entity.setPermission(permission);
        return rolePermissionMapper.entityToDto(rolePermissions.saveAndFlush(entity));
    }

    @Transactional
    public RolePermissionResponse updateRolePermission(Long roleId, Long permissionId, RolePermissionRequest dto) {
        RolePermission previous = requireRolePermission(roleId, permissionId);
        if (previous.getId().equals(new RolePermissionId(dto.roleId(), dto.permissionId()))) {
            return rolePermissionMapper.entityToDto(previous);
        }
        RolePermissionResponse result = createRolePermission(dto);
        roleService.removePermission(roleId, permissionId);
        rolePermissions.flush();
        return result;
    }

    @Transactional
    public void deleteRolePermission(Long roleId, Long permissionId) {
        requireRolePermission(roleId, permissionId);
        roleService.removePermission(roleId, permissionId);
    }
}
