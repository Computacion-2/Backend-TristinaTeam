package com.icesi.cuidapetproyect.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_permission")
public class AppPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long id;

    @NotBlank(message = "El código del permiso es obligatorio")
    @Column(name = "permission_code", nullable = false, unique = true, length = 100)
    private String permissionCode;

    @NotBlank(message = "El código del permiso es obligatorio")
    @Column(name = "permission_name", nullable = false, length = 120)
    private String permissionName;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "permission")
    private List<com.icesi.cuidapetproyect.model.RolePermission> rolePermissions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}