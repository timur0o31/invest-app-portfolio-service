package me.vladislav.analytics

import kotlinx.datetime.Clock
import me.vladislav.portfolio.PortfolioService
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class AnalyticsService(
    private val portfolioService: PortfolioService
) {
    suspend fun value(userId: UUID, period: String, authorization: String?): PortfolioValueResponse {
        val summary = portfolioService.summary(userId, authorization)
        return PortfolioValueResponse(
            period = period,
            points = listOf(
                PortfolioValuePointResponse(
                    ts = Clock.System.now().toString(),
                    cash = summary.balance,
                    portfolioValue = summary.portfolioValue,
                    totalEquity = summary.totalEquity,
                    totalPnl = summary.totalPnl
                )
            )
        )
    }

    suspend fun allocation(userId: UUID, period: String, authorization: String?): PortfolioAllocationResponse {
        val summary = portfolioService.summary(userId, authorization)
        if (summary.portfolioValue <= BigDecimal.ZERO) {
            return PortfolioAllocationResponse(period = period, items = emptyList())
        }

        return PortfolioAllocationResponse(
            period = period,
            items = summary.positions.map {
                AllocationItemResponse(
                    instrumentId = it.instrumentId,
                    ticker = it.ticker,
                    value = it.currentValue,
                    percent = it.currentValue
                        .multiply(BigDecimal("100"))
                        .divide(summary.portfolioValue, 4, RoundingMode.HALF_UP)
                        .toDouble()
                )
            }
        )
    }
}
