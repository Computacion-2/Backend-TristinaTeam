package com.icesi.cuidapet.repository;
import com.icesi.cuidapet.model.UserRole;
import com.icesi.cuidapet.model.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUser_Id(Long userId);

    List<UserRole> findByRole_Id(Long roleId);
}