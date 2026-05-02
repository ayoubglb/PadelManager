package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.transaction.RechargeRequest;
import be.ephec.padelmanager.dto.transaction.TransactionDTO;
import be.ephec.padelmanager.entity.Transaction;
import be.ephec.padelmanager.entity.TypeTransaction;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.mapper.TransactionMapper;
import be.ephec.padelmanager.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionDTO recharger(RechargeRequest requete, Utilisateur membre) {
        validerCompteActif(membre);

        Transaction transaction = Transaction.builder()
                .utilisateur(membre)
                .type(TypeTransaction.RECHARGE)
                .montant(requete.montant())
                .build();

        Transaction enregistree = transactionRepository.save(transaction);

        log.info("Recharge effectuée : utilisateur={}, montant={}€, transaction={}",
                membre.getMatricule(), requete.montant(), enregistree.getId());

        return transactionMapper.toDto(enregistree);
    }

    private void validerCompteActif(Utilisateur membre) {
        if (Boolean.FALSE.equals(membre.getActive())) {
            throw new IllegalArgumentException("Compte utilisateur désactivé");
        }
    }

    // Liste les transactions de l'utilisateur authentifié avec filtres
    @Transactional(readOnly = true)
    public List<TransactionDTO> consulterMesTransactions(Utilisateur utilisateur,
                                                         TypeTransaction type,
                                                         LocalDate dateDebut,
                                                         LocalDate dateFin) {
        LocalDateTime debut = dateDebut != null ? dateDebut.atStartOfDay() : null;
        LocalDateTime fin = dateFin != null ? dateFin.atTime(LocalTime.MAX) : null;

        return transactionRepository
                .findMesTransactions(utilisateur.getId(), type, debut, fin).stream()
                .map(transactionMapper::toDto)
                .toList();
    }


}