package me.vladislav.portfolio

import kotlinx.coroutines.Dispatchers
import me.vladislav.quotes.QuotesClient
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.util.UUID

class PortfolioService(
    private val accountRepository: AccountRepository,
    private val portfolioRepository: PortfolioRepository,
    private val quotesClient: QuotesClient
) {
    suspend fun positions(userId: UUID) =
        newSuspendedTransaction(Dispatchers.IO) {
            val account = accountRepository.findByUserId(userId) ?: accountRepository.create(userId)
            portfolioRepository.positions(account.id).map { it.toResponse() }
        }

    suspend fun summary(userId: UUID, authorization: String?): PortfolioSummaryResponse {
        val accountAndPositions = newSuspendedTransaction(Dispatchers.IO) {
            val account = accountRepository.findByUserId(userId) ?: accountRepository.create(userId)
            account to portfolioRepository.positions(account.id)
        }
        val (account, positions) = accountAndPositions
        val prices = quotesClient.currentPrices(positions.map { it.instrumentId }, authorization)

        val positionSummaries = positions.map { position ->
            val currentPrice = prices[position.instrumentId] ?: position.averageBuyPrice
            val currentValue = currentPrice.multiply(position.quantity.toBigDecimal())
            val pnl = currentPrice.minus(position.averageBuyPrice).multiply(position.quantity.toBigDecimal())
            PositionSummaryResponse(
                instrumentId = position.instrumentId,
                ticker = position.ticker,
                quantity = position.quantity,
                averageBuyPrice = position.averageBuyPrice,
                currentPrice = currentPrice,
                currentValue = currentValue,
                pnl = pnl
            )
        }

        val portfolioValue = positionSummaries.fold(BigDecimal.ZERO) { acc, position -> acc + position.currentValue }
        val totalPnl = positionSummaries.fold(BigDecimal.ZERO) { acc, position -> acc + position.pnl }

        return PortfolioSummaryResponse(
            balance = account.balance,
            portfolioValue = portfolioValue,
            totalEquity = account.balance + portfolioValue,
            totalPnl = totalPnl,
            positions = positionSummaries
        )
    }

}
