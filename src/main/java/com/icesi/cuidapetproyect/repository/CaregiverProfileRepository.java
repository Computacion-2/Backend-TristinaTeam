package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.CaregiverProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CaregiverProfileRepository extends JpaRepository<CaregiverProfile, Long> {

    List<CaregiverProfile> findByServiceCity(String city);
}
