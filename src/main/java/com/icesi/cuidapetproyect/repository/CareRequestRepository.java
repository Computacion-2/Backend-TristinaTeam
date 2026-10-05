package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.CareRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CareRequestRepository
        extends JpaRepository<CareRequest, Long> {

    List<CareRequest> findByOwner_Id(Long ownerId);
}
