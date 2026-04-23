package be.ephec.padelmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "utilisateur",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_utilisateur_matricule", columnNames = "matricule"),
                @UniqueConstraint(name = "uk_utilisateur_email",     columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_utilisateur_site_rattachement", columnList = "site_rattachement_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "matricule", nullable = false, length = 10)
    private String matricule;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "telephone", nullable = false, length = 30)
    private String telephone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private RoleUtilisateur role;

    @Column(name = "site_rattachement_id")
    private Long siteRattachementId;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "date_inscription", nullable = false, updatable = false)
    private LocalDateTime dateInscription;
}