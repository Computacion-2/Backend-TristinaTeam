package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.PetSpecies;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PetSpeciesRepository extends JpaRepository<PetSpecies, Long> {

    Optional<PetSpecies> findBySpeciesName(String speciesName);
}
