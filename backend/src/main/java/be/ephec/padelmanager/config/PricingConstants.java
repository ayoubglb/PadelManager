package be.ephec.padelmanager.config;

import java.math.BigDecimal;

public final class PricingConstants {

    public static final BigDecimal PRIX_MATCH = new BigDecimal("60.00");
    public static final BigDecimal PART_JOUEUR = new BigDecimal("15.00");
    public static final int NB_JOUEURS_MAX = 4;

    private PricingConstants() {
        // classe utilitaire, pas d'instance
    }
}