package com.icesi.cuidapet.repository;
import com.icesi.cuidapet.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
