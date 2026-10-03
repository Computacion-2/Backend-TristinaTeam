
package com.icesi.cuidapetproyect.repository;

import com.icesi.cuidapetproyect.model.CareUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareUpdateRepository
        extends JpaRepository<CareUpdate, Long> {

    List<CareUpdate> findByBooking_Id(Long bookingId);
}
