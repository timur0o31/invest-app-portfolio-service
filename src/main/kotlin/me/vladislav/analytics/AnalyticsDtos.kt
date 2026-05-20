package me.vladislav.analytics

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class PortfolioValuePointResponse(
    val ts: String,
    @Contextual
    val cash: BigDecimal,
    @Contextual
    val portfolioValue: BigDecimal,
    @Contextual
    val totalEquity: BigDecimal,
    @Contextual
    val totalPnl: BigDecimal
)

@Serializable
data class PortfolioValueResponse(
    val period: String,
    val points: List<PortfolioValuePointResponse>
)

@Serializable
data class AllocationItemResponse(
    val instrumentId: Long,
    val ticker: String,
    @Contextual
    val value: BigDecimal,
    val percent: Double
)

@Serializable
data class PortfolioAllocationResponse(
    val period: String,
    val items: List<AllocationItemResponse>
)
