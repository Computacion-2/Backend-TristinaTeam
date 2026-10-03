package com.icesi.cuidapet.model;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "caregiver_service")
public class CaregiverService {

    @EmbeddedId
    private CaregiverServiceId id = new CaregiverServiceId();

    @ManyToOne
    @MapsId("caregiverId")
    @JoinColumn(name = "caregiver_id", nullable = false)
    private CaregiverProfile caregiver;

    @ManyToOne
    @MapsId("serviceTypeId")
    @JoinColumn(name = "service_type_id", nullable = false)
    private ServiceType serviceType;

    @Column(name = "base_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseRate;

    @Column(name = "active_flag", nullable = false, length = 1)
    private String activeFlag = "Y";
}