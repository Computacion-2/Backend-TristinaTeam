package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {

    Optional<AppRole> findByRoleName(String roleName);

    boolean existsByRoleName(String roleName);
}