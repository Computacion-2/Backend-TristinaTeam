package com.icesi.cuidapetproyect.model;
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
    private com.icesi.cuidapetproyect.model.CaregiverServiceId id = new com.icesi.cuidapetproyect.model.CaregiverServiceId();

    @ManyToOne
    @MapsId("caregiverId")
    @JoinColumn(name = "caregiver_id", nullable = false)
    private CaregiverProfile caregiver;

    @ManyToOne
    @MapsId("serviceTypeId")
    @JoinColumn(name = "service_type_id", nullable = false)
    private com.icesi.cuidapetproyect.model.ServiceType serviceType;

    @Column(name = "base_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseRate;

    @Column(name = "active_flag", nullable = false, columnDefinition = "CHAR(1)")
    private String activeFlag = "Y";
}