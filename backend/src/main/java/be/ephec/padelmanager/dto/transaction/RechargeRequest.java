package be.ephec.padelmanager.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RechargeRequest(

        @NotNull(message = "Le montant de la recharge est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit être strictement positif")
        @Digits(integer = 8, fraction = 2,
                message = "Le montant accepte au plus 8 chiffres avant la virgule et 2 après")
        BigDecimal montant
) {
}