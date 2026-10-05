package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.CareApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CareApplicationRepository
        extends JpaRepository<CareApplication, Long> {

    List<CareApplication> findByRequest_Id(Long requestId);

    List<CareApplication> findByCaregiver_Id(Long caregiverId);
}
