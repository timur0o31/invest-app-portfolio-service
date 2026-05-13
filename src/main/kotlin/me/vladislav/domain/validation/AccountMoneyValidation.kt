package me.vladislav.domain.validation

import java.math.BigDecimal

object MoneyValidation {
    fun requirePositive(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) {
            "Amount must be > 0"
        }
    }
}