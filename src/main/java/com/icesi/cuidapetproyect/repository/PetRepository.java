package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findByOwner_Id(Long ownerId);

    List<Pet> findBySpecies_Id(Long speciesId);
}
