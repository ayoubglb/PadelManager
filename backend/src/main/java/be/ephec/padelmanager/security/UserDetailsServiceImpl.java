package be.ephec.padelmanager.security;

import be.ephec.padelmanager.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        return utilisateurRepository.findByEmailOrMatricule(login)
                .map(UtilisateurPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Aucun utilisateur trouvé pour l'identifiant : " + login
                ));
    }
}