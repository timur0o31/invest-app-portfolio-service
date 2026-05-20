package me.vladislav.portfolio

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
data class ResetRequest(
    @Contextual
    val amount: BigDecimal? = null
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
    val balance: BigDecimal,
    @Contextual
    val portfolioValue: BigDecimal,
    @Contextual
    val totalEquity: BigDecimal,
    @Contextual
    val totalPnl: BigDecimal,
    val positions: List<PositionSummaryResponse>
)

@Serializable
data class PositionSummaryResponse(
    val instrumentId: Long,
    val ticker: String,
    val quantity: Int,
    @Contextual
    val averageBuyPrice: BigDecimal,
    @Contextual
    val currentPrice: BigDecimal,
    @Contextual
    val currentValue: BigDecimal,
    @Contextual
    val pnl: BigDecimal
)

@Serializable
data class PositionResponse(
    val id: String,
    val instrumentId: Long,
    val ticker: String,
    val quantity: Int,
    @Contextual
    val averageBuyPrice: BigDecimal
)
