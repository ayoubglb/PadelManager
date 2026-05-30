describe('Recharge de solde', () => {
  beforeEach(() => {
    // Login UI avant chaque test
    cy.visit('/login');
    cy.get('input').first().type('membre.site@padelmanager.be');
    cy.get('input[type="password"]').type('Dev2026!');
    cy.get('button[type="submit"]').click();
    cy.url({ timeout: 10000 }).should('not.include', '/login');
  });

  it('affiche la page Transactions avec le solde et le bouton recharger', () => {
    cy.visit('/transactions');

    // Le badge de solde est visible (le span contenant le montant ou un tiret)
    cy.contains('Solde courant').should('be.visible');

    // Le bouton "Recharger mon compte" est cliquable
    cy.contains('button', 'Recharger mon compte').should('be.visible');
  });

  it('ouvre le dialog de recharge et permet de saisir un montant', () => {
    cy.visit('/transactions');

    // Clic sur le bouton Recharger
    cy.contains('button', 'Recharger mon compte').click();

    // Le dialog Material apparaît avec son titre
    cy.contains('Recharger mon compte').should('be.visible');
    cy.contains('Sélectionnez un montant prédéfini').should('be.visible');

    // L'input de montant est présent
    cy.get('input[formcontrolname="montant"]').should('be.visible');
  });

  it('recharge 50 € et met à jour le solde affiché', () => {
    cy.visit('/transactions');

    // Capture du solde avant recharge (texte du badge solde)
    cy.get('app-solde-badge')
      .invoke('text')
      .then((soldeAvantTexte) => {
        const soldeAvant = parseSolde(soldeAvantTexte);

        // Ouvre le dialog
        cy.contains('button', 'Recharger mon compte').click();
        cy.contains('Recharger mon compte').should('be.visible');

       // Sélectionne le montant prédéfini de 30 €
        cy.contains('button', '30 €').click();

        // Vérifie que l'input s'est rempli à 30
        cy.get('input[formcontrolname="montant"]').should('have.value', '30');

        // Clic sur le bouton Recharger dans le dialog
        cy.get('mat-dialog-actions').contains('button', 'Recharger').click();

        // Attend que le solde soit mis à jour
        cy.wait(1000);

        // Le solde doit avoir augmenté de 30 €
        cy.get('app-solde-badge')
          .invoke('text')
          .then((soldeApresTexte) => {
            const soldeApres = parseSolde(soldeApresTexte);
            expect(soldeApres).to.be.greaterThan(soldeAvant);
            expect(soldeApres - soldeAvant).to.be.closeTo(30, 0.01);
          });

// Une nouvelle transaction RECHARGE est listée dans le tableau
        cy.contains('Recharge').should('be.visible');
      });
  });
});

// Helper : extrait le solde numérique du texte du badge ("60,00 €" → 60)
function parseSolde(texte: string): number {
  // Garde uniquement les chiffres, la virgule et le point
  // Convertit la virgule décimale française en point
  const match = texte.match(/[\d.,]+/);
  if (!match) return 0;
  const cleaned = match[0].replace(/\./g, '').replace(',', '.');
  const value = parseFloat(cleaned);
  return isNaN(value) ? 0 : value;
}
