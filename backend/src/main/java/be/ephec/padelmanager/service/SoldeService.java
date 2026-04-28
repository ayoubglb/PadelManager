package be.ephec.padelmanager.service;

import be.ephec.padelmanager.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SoldeService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public BigDecimal calculerSolde(Long utilisateurId) {
        return transactionRepository.calculerSoldeUtilisateur(utilisateurId);
    }

    @Transactional(readOnly = true)
    public boolean disposeAuMoinsDe(Long utilisateurId, BigDecimal montantRequis) {
        return calculerSolde(utilisateurId).compareTo(montantRequis) >= 0;
    }
}