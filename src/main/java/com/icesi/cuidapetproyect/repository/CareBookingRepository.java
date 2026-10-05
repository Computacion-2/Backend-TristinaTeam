package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.CareBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CareBookingRepository
        extends JpaRepository<CareBooking, Long> {

    List<CareBooking> findByOwner_Id(Long ownerId);

    List<CareBooking> findByCaregiver_Id(Long caregiverId);

    Optional<CareBooking> findByRequest_Id(Long requestId);
}
