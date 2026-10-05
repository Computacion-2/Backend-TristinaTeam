package com.icesi.cuidapetproyect.repository;
import com.icesi.cuidapetproyect.model.FavoriteCaregiver;
import com.icesi.cuidapetproyect.model.FavoriteCaregiverId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FavoriteCaregiverRepository
        extends JpaRepository<FavoriteCaregiver, FavoriteCaregiverId> {

    List<FavoriteCaregiver> findByOwner_Id(Long ownerId);
}
