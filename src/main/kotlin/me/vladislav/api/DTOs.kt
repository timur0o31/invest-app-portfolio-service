package me.vladislav.api

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class AccountResponse(
    val id: String,
    val userId: String,
    @Contextual
    val balance: BigDecimal,
    val currency: String
)

@Serializable
data class DepositRequest(
    @Contextual
    val amount: BigDecimal
)

@Serializable
data class DepositResponse(
    @Contextual
    val balance: BigDecimal
)

@Serializable
data class WithdrawRequest(
    @Contextual
    val amount: BigDecimal
)

@Serializable
data class WithdrawResponse(
    @Contextual
    val balance: BigDecimal
)

@Serializable
data class PortfolioSummaryResponse(
    @Contextual
    val balance: BigDecimal, // Свободные средства
    @Contextual
    val portfolioValue: BigDecimal, // Текущая стоимость всех позиций
    @Contextual
    val totalEquity: BigDecimal, // Полная стоимость аккаунта.
    @Contextual
    val totalPnl: BigDecimal, // Общая прибыль/убыток по всем позициям
    val positions: List<PositionSummaryResponse>
)

@Serializable
data class PositionSummaryResponse(
    val ticker: String,
    val quantity: Int,
    @Contextual
    val averageBuyPrice: BigDecimal,
    @Contextual
    val currentPrice: BigDecimal,
    @Contextual
    val currentValue: BigDecimal, // Текущая стоимость позиции
    @Contextual
    val pnl: BigDecimal
)

@Serializable
data class PositionResponse(
    val id: String,
    val instrumentId: String,
    val ticker: String,
    val quantity: Int,
    @Contextual
    val averageBuyPrice: BigDecimal
)

@Serializable
data class ErrorResponse(
    val message: String
)
