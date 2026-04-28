package be.ephec.padelmanager.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "`transaction`")
@Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_transaction_utilisateur"))
    private Utilisateur utilisateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TypeTransaction type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montant;

    @CreationTimestamp
    @Column(name = "date", nullable = false, updatable = false)
    private LocalDateTime date;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id",
            foreignKey = @ForeignKey(name = "fk_transaction_match"))
    private Match match;
}