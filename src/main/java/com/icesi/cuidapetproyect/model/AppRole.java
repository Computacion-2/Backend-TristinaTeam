package com.icesi.cuidapet.model;
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

    @Column(name = "active_flag", nullable = false, length = 1)
    private String activeFlag = "Y";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "role")
    private List<UserRole> userRoles = new ArrayList<>();

    @OneToMany(mappedBy = "role")
    private List<RolePermission> rolePermissions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}