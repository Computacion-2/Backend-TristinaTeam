package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PetRepository extends JpaRepository<Pet, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pet p where p.id = :id")
    Optional<Pet> findForUpdate(@Param("id") Long id);

    boolean existsBySpecies_Id(Long speciesId);

    List<Pet> findByOwner_Id(Long ownerId);

    List<Pet> findBySpecies_Id(Long speciesId);
}
