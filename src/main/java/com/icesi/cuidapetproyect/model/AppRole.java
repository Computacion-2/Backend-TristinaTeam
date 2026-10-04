package com.icesi.cuidapetproyect.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_role")
public class AppRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Column(name = "role_name", nullable = false, unique = true, length = 60)
    private String roleName;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "active_flag", nullable = false, columnDefinition = "CHAR(1)")
    private String activeFlag = "Y";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "role")
    private List<com.icesi.cuidapetproyect.model.UserRole> userRoles = new ArrayList<>();

    @OneToMany(mappedBy = "role")
    private List<com.icesi.cuidapetproyect.model.RolePermission> rolePermissions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}