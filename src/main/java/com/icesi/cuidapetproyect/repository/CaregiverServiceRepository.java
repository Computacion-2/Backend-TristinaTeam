package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.CaregiverService;
import com.icesi.cuidapetproyect.model.CaregiverServiceId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CaregiverServiceRepository extends JpaRepository<CaregiverService, CaregiverServiceId> {

    List<CaregiverService> findByCaregiver_Id(Long caregiverId);

    List<CaregiverService> findByServiceType_Id(Long serviceTypeId);
}