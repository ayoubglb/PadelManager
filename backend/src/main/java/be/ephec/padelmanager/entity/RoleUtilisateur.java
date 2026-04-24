package be.ephec.padelmanager.entity;

public enum RoleUtilisateur {

    MEMBRE_GLOBAL("G",  false, false),
    MEMBRE_SITE  ("S",  false, true),
    MEMBRE_LIBRE ("L",  false, false),
    ADMIN_SITE   ("AS", true,  true),
    ADMIN_GLOBAL ("AG", true,  false);

    private final String prefixeMatricule;
    private final boolean administrateur;
    private final boolean siteRattachementRequis;

    RoleUtilisateur(String prefixeMatricule, boolean administrateur, boolean siteRattachementRequis) {
        this.prefixeMatricule = prefixeMatricule;
        this.administrateur = administrateur;
        this.siteRattachementRequis = siteRattachementRequis;
    }


    public String getPrefixeMatricule() {
        return prefixeMatricule;
    }

    public boolean isAdministrateur() {
        return administrateur;
    }

    public boolean exigeSiteRattachement() {
        return siteRattachementRequis;
    }
}