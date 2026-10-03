package com.icesi.cuidapet.model;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class CaregiverServiceId implements Serializable {

    @Column(name = "caregiver_id")
    private Long caregiverId;

    @Column(name = "service_type_id")
    private Long serviceTypeId;
}