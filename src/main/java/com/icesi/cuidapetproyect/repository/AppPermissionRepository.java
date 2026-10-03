package com.icesi.cuidapet.repository;
import com.icesi.cuidapet.model.AppPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppPermissionRepository extends JpaRepository<AppPermission, Long> {

    Optional<AppPermission> findByPermissionCode(String permissionCode);

    boolean existsByPermissionCode(String permissionCode);
}