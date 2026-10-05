package com.icesi.cuidapetproyect.model;
import lombok.*;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteCaregiverId implements Serializable {

    private Long owner;
    private Long caregiver;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FavoriteCaregiverId that)) return false;

        return Objects.equals(owner, that.owner)
                && Objects.equals(caregiver, that.caregiver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, caregiver);
    }
}
