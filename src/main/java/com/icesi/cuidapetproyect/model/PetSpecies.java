package com.icesi.cuidapetproyect.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pet_species")
public class PetSpecies {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "species_id")
    private Long id;

    @Column(name = "species_name", nullable = false, unique = true, length = 80)
    private String speciesName;

    @Column(name = "active_flag", nullable = false, columnDefinition = "CHAR(1)")
    private String activeFlag = "Y";

    @OneToMany(mappedBy = "species")
    private List<Pet> pets = new ArrayList<>();
}