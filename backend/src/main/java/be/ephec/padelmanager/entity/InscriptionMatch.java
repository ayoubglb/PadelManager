package be.ephec.padelmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inscription_match",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inscription_match_joueur",
                        columnNames = {"match_id", "joueur_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InscriptionMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_inscription_match_match"))
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "joueur_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_inscription_match_joueur"))
    private Utilisateur joueur;

    @CreationTimestamp
    @Column(name = "date_inscription", nullable = false, updatable = false)
    private LocalDateTime dateInscription;

    @Column(nullable = false)
    @Builder.Default
    private Boolean paye = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private StatutInscription statut = StatutInscription.INSCRIT;

    @Column(name = "est_organisateur", nullable = false)
    @Builder.Default
    private Boolean estOrganisateur = false;
}