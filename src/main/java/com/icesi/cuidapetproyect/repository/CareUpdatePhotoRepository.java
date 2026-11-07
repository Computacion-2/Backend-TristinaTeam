package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.CareUpdatePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CareUpdatePhotoRepository
        extends JpaRepository<CareUpdatePhoto, Long> {

    List<CareUpdatePhoto> findByCareUpdate_Id(Long updateId);
    long countByCareUpdate_Id(Long updateId);
    boolean existsByCareUpdate_IdAndPhotoUrl(Long updateId, String photoUrl);
    boolean existsByCareUpdate_IdAndPhotoUrlAndIdNot(Long updateId, String photoUrl, Long id);
}
