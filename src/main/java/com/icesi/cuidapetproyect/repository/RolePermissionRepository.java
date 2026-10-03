package com.icesi.cuidapet.repository;
import com.icesi.cuidapet.model.RolePermission;
import com.icesi.cuidapet.model.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findByRole_Id(Long roleId);

    List<RolePermission> findByPermission_Id(Long permissionId);
}