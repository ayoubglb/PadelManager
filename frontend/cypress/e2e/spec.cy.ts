describe('My First Test', () => {
  it('Visits the initial project page', () => {
    cy.visit('/');
    // L'utilisateur non authentifié est redirigé vers /login
    cy.url().should('include', '/login');
  });
});
