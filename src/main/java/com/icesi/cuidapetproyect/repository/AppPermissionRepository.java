package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.AppPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppPermissionRepository extends JpaRepository<AppPermission, Long> {

    Optional<AppPermission> findByPermissionCode(String permissionCode);

    boolean existsByPermissionCode(String permissionCode);
    boolean existsByPermissionNameIgnoreCase(String permissionName);
    boolean existsByPermissionNameIgnoreCaseAndIdNot(String permissionName, Long id);
}