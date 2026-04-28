package be.ephec.padelmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "match",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_match_terrain_dateheuredebut",
                        columnNames = {"terrain_id", "date_heure_debut"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    // Durée fixe d'un match
    public static final Duration DUREE = Duration.ofMinutes(90);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "terrain_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_match_terrain"))
    private Terrain terrain;

    @Column(name = "date_heure_debut", nullable = false)
    private LocalDateTime dateHeureDebut;

    @Column(name = "date_heure_fin", nullable = false)
    private LocalDateTime dateHeureFin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisateur_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_match_organisateur"))
    private Utilisateur organisateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TypeMatch type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatutMatch statut;

    @Column(name = "devenu_public_automatiquement", nullable = false)
    @Builder.Default
    private Boolean devenuPublicAutomatiquement = false;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;


    @Transient
    public boolean isTermine() {
        return dateHeureFin != null && dateHeureFin.isBefore(LocalDateTime.now());
    }
}