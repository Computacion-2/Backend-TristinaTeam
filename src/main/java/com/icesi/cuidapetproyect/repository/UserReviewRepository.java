
package com.icesi.cuidapetproyect.repository;

import com.icesi.cuidapetproyect.model.UserReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserReviewRepository
        extends JpaRepository<UserReview, Long> {

    List<UserReview> findByReviewedUser_Id(Long userId);

    List<UserReview> findByBooking_Id(Long bookingId);
}
