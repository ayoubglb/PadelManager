describe('Login - Connexion utilisateur', () => {
  beforeEach(() => {
    cy.visit('/login');
  });

  it('affiche le formulaire de connexion', () => {
    cy.get('input').should('have.length.at.least', 2);
    cy.get('input[type="password"]').should('be.visible');
    cy.get('button[type="submit"]').should('be.visible');
  });

  it('refuse une connexion avec des identifiants invalides', () => {
    cy.get('input').first().type('inconnu@example.com');
    cy.get('input[type="password"]').type('mauvaisMotDePasse');
    cy.get('button[type="submit"]').click();

    // Le snackbar Material affiche le message d'erreur
    cy.contains(/invalid|incorrect|invalide/i, { timeout: 5000 }).should('be.visible');

    // On reste sur /login
    cy.url().should('include', '/login');
  });

  it('connecte un utilisateur valide et redirige vers le planning', () => {
    cy.get('input').first().type('membre.site@padelmanager.be');
    cy.get('input[type="password"]').type('Dev2026!');
    cy.get('button[type="submit"]').click();

    // Redirection vers le planning (ou la page d'accueil par défaut après login)
    cy.url({ timeout: 10000 }).should('not.include', '/login');

    // Vérifie qu'on voit un élément qui n'est visible que quand on est connecté
    cy.contains(/planning|mes matchs|déconnexion/i, { timeout: 5000 }).should('exist');
  });
});
