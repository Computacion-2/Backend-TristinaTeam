package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.PetPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PetPhotoRepository extends JpaRepository<PetPhoto, Long> {

    List<PetPhoto> findByPet_Owner_Id(Long ownerId);

    boolean existsByPet_IdAndPhotoUrl(Long petId, String photoUrl);

    boolean existsByPet_IdAndPhotoUrlAndIdNot(Long petId, String photoUrl, Long id);

    List<PetPhoto> findByPet_Id(Long petId);
}
