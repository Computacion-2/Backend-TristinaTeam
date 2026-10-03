package com.icesi.cuidapetproyect.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "booking_status")
public class BookingStatus {

    @Id
    @Column(name = "status_code", length = 30)
    private String statusCode;

    @Column(name = "status_name", nullable = false, unique = true, length = 80)
    private String statusName;

    @OneToMany(mappedBy = "bookingStatus")
    private List<CareBooking> bookings = new ArrayList<>();
}