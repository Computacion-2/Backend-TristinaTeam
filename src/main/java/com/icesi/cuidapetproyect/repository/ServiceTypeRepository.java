package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long> {

    Optional<ServiceType> findByServiceName(String serviceName);
}