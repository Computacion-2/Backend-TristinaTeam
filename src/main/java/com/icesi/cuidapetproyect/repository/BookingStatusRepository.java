package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingStatusRepository
        extends JpaRepository<BookingStatus, String> {
}

