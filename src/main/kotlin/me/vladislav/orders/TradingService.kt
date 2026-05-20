package me.vladislav.orders

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.vladislav.common.ValidationException
import me.vladislav.operations.OperationRepository
import me.vladislav.operations.OperationType
import me.vladislav.portfolio.Account
import me.vladislav.portfolio.AccountRepository
import me.vladislav.portfolio.PortfolioRepository
import me.vladislav.quotes.QuoteUnavailableException
import me.vladislav.quotes.QuotesClient
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.util.UUID

class TradingService(
    private val accountRepository: AccountRepository,
    private val portfolioRepository: PortfolioRepository,
    private val orderRepository: OrderRepository,
    private val operationRepository: OperationRepository,
    private val quotesClient: QuotesClient
) {
    suspend fun createOrder(userId: UUID, request: OrderRequest, authorization: String?): Order {
        val side = OrderSide.parse(request.side) ?: throw ValidationException("side must be buy or sell")
        val type = OrderType.parse(request.type) ?: throw ValidationException("type must be market")
        val quantity = request.qty ?: request.quantity ?: throw ValidationException("qty is required")
        if (type != OrderType.MARKET) throw ValidationException("Only market orders are supported")
        if (quantity <= 0) throw ValidationException("qty must be positive")

        val ticker = request.ticker?.takeIf { it.isNotBlank() } ?: request.instrumentId.toString()
        val price = runCatching {
            quotesClient.marketPrice(request.instrumentId, side, authorization)
        }.getOrElse { error ->
            if (error is QuoteUnavailableException) BigDecimal.ZERO else throw error
        }

        return newSuspendedTransaction(Dispatchers.IO) {
            val account = accountRepository.findByUserId(userId) ?: accountRepository.create(userId)
            if (price <= BigDecimal.ZERO) {
                return@newSuspendedTransaction reject(
                    account.id,
                    request.instrumentId,
                    ticker,
                    side,
                    quantity,
                    price,
                    "PRICE_UNAVAILABLE"
                )
            }

            when (side) {
                OrderSide.BUY -> buy(account, request.instrumentId, ticker, quantity, price)
                OrderSide.SELL -> sell(account, request.instrumentId, ticker, quantity, price)
            }
        }
    }

    suspend fun listOrders(userId: UUID, status: OrderStatus?, from: Instant?, to: Instant?): List<Order> =
        newSuspendedTransaction(Dispatchers.IO) {
            val account = accountRepository.findByUserId(userId) ?: accountRepository.create(userId)
            orderRepository.list(account.id, status, from, to)
        }

    suspend fun getOrder(userId: UUID, orderId: UUID): Order? =
        newSuspendedTransaction(Dispatchers.IO) {
            val account = accountRepository.findByUserId(userId) ?: accountRepository.create(userId)
            orderRepository.findById(orderId)?.takeIf { it.accountId == account.id }
        }

    private fun buy(account: Account, instrumentId: Long, ticker: String, quantity: Int, price: BigDecimal): Order {
        val amount = price.multiply(quantity.toBigDecimal())
        if (account.balance < amount) {
            return reject(account.id, instrumentId, ticker, OrderSide.BUY, quantity, price, "INSUFFICIENT_FUNDS")
        }

        accountRepository.updateBalance(account.id, account.balance - amount)
        portfolioRepository.buy(account.id, instrumentId, ticker, quantity, price)
        return fill(account.id, instrumentId, ticker, OrderSide.BUY, quantity, price, amount.negate())
    }

    private fun sell(account: Account, instrumentId: Long, ticker: String, quantity: Int, price: BigDecimal): Order {
        val position = portfolioRepository.findPosition(account.id, instrumentId)
        if (position == null || position.quantity < quantity) {
            return reject(account.id, instrumentId, ticker, OrderSide.SELL, quantity, price, "INSUFFICIENT_POSITION")
        }

        val amount = price.multiply(quantity.toBigDecimal())
        portfolioRepository.sell(account.id, instrumentId, quantity)
        accountRepository.updateBalance(account.id, account.balance + amount)
        return fill(account.id, instrumentId, ticker, OrderSide.SELL, quantity, price, amount)
    }

    private fun fill(
        accountId: UUID,
        instrumentId: Long,
        ticker: String,
        side: OrderSide,
        quantity: Int,
        price: BigDecimal,
        operationAmount: BigDecimal
    ): Order {
        val now = Clock.System.now()
        val amount = price.multiply(quantity.toBigDecimal())
        val order = orderRepository.create(
            accountId = accountId,
            instrumentId = instrumentId,
            ticker = ticker,
            side = side,
            type = OrderType.MARKET,
            status = OrderStatus.FILLED,
            quantity = quantity,
            price = price,
            amount = amount,
            rejectReason = null,
            now = now
        )
        operationRepository.create(
            accountId = accountId,
            orderId = order.id,
            type = if (side == OrderSide.BUY) OperationType.BUY else OperationType.SELL,
            instrumentId = instrumentId,
            ticker = ticker,
            amount = operationAmount,
            quantity = quantity,
            price = price,
            status = OrderStatus.FILLED,
            now = now
        )
        return order
    }

    private fun reject(
        accountId: UUID,
        instrumentId: Long,
        ticker: String,
        side: OrderSide,
        quantity: Int,
        price: BigDecimal,
        reason: String
    ): Order {
        val now = Clock.System.now()
        val amount = price.multiply(quantity.toBigDecimal())
        val order = orderRepository.create(
            accountId = accountId,
            instrumentId = instrumentId,
            ticker = ticker,
            side = side,
            type = OrderType.MARKET,
            status = OrderStatus.REJECTED,
            quantity = quantity,
            price = price,
            amount = amount,
            rejectReason = reason,
            now = now
        )
        operationRepository.create(
            accountId = accountId,
            orderId = order.id,
            type = if (side == OrderSide.BUY) OperationType.BUY else OperationType.SELL,
            instrumentId = instrumentId,
            ticker = ticker,
            amount = BigDecimal.ZERO,
            quantity = quantity,
            price = price,
            status = OrderStatus.REJECTED,
            now = now
        )
        return order
    }
}
