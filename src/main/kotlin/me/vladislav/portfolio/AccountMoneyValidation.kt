package me.vladislav.portfolio

import java.math.BigDecimal

object MoneyValidation {
    fun requirePositive(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) {
            "Amount must be > 0"
        }
    }

    fun requireNotNegative(amount: BigDecimal) {
        require(amount >= BigDecimal.ZERO) {
            "Amount must be >= 0"
        }
    }
}
