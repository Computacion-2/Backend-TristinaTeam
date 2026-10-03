package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.PetPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PetPhotoRepository extends JpaRepository<PetPhoto, Long> {

    List<PetPhoto> findByPet_Id(Long petId);
}
