package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long> {

    boolean existsByServiceNameIgnoreCase(String serviceName);

    boolean existsByServiceNameIgnoreCaseAndIdNot(String serviceName, Long id);

    Optional<ServiceType> findByServiceName(String serviceName);
}
