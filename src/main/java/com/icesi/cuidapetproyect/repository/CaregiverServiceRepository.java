package com.icesi.cuidapet.repository;
import com.icesi.cuidapet.model.CaregiverService;
import com.icesi.cuidapet.model.CaregiverServiceId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CaregiverServiceRepository extends JpaRepository<CaregiverService, CaregiverServiceId> {

    List<CaregiverService> findByCaregiver_Id(Long caregiverId);

    List<CaregiverService> findByServiceType_Id(Long serviceTypeId);
}