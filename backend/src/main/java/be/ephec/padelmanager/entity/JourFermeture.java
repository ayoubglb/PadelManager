package be.ephec.padelmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "jour_fermeture")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JourFermeture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_fermeture", nullable = false)
    private LocalDate dateFermeture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id",
            foreignKey = @ForeignKey(name = "fk_jour_fermeture_site"))
    private Site site;

    @Column(name = "raison", nullable = false, length = 200)
    private String raison;
}