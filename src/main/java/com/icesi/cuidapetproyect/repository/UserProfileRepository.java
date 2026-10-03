package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
