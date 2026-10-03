package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.CaregiverAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaregiverAvailabilityRepository extends JpaRepository<CaregiverAvailability, Long> {

    List<CaregiverAvailability> findByCaregiver_Id(Long caregiverId);

    List<CaregiverAvailability> findByDayOfWeek(Integer dayOfWeek);
}