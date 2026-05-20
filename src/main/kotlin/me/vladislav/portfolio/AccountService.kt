package me.vladislav.portfolio

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import me.vladislav.common.AccountNotFoundException
import me.vladislav.common.ValidationException
import me.vladislav.operations.OperationRepository
import me.vladislav.operations.OperationType
import me.vladislav.orders.OrderStatus
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.util.*

class AccountService(
    private val accountRepository: AccountRepository,
    private val operationRepository: OperationRepository,
    private val portfolioRepository: PortfolioRepository
) {
    private val defaultResetBalance = BigDecimal("1000000.00")

    suspend fun getOrCreateAccount(userId: UUID): Account =
        newSuspendedTransaction(Dispatchers.IO) {
            accountRepository.findByUserId(userId) ?: accountRepository.create(userId)
        }

    suspend fun deposit(userId: UUID, amount: BigDecimal): Account {
        MoneyValidation.requirePositive(amount)

        return newSuspendedTransaction(Dispatchers.IO) {
            accountRepository.findByUserId(userId) ?: accountRepository.create(userId)
            accountRepository.deposit(userId, amount)
            val account = accountRepository.findByUserId(userId) ?: throw AccountNotFoundException(userId)
            operationRepository.create(
                accountId = account.id,
                orderId = null,
                type = OperationType.DEPOSIT,
                instrumentId = null,
                ticker = null,
                amount = amount,
                quantity = null,
                price = null,
                status = OrderStatus.FILLED,
                now = Clock.System.now()
            )
            account
        }
    }

    suspend fun reset(userId: UUID, amount: BigDecimal?): Account {
        val resetAmount = amount ?: defaultResetBalance
        MoneyValidation.requireNotNegative(resetAmount)

        return newSuspendedTransaction(Dispatchers.IO) {
            val account = accountRepository.findByUserId(userId) ?: accountRepository.create(userId)
            portfolioRepository.clearPositions(account.id)
            accountRepository.updateBalance(account.id, resetAmount)
            val updated = accountRepository.findById(account.id) ?: throw AccountNotFoundException(userId)
            operationRepository.create(
                accountId = updated.id,
                orderId = null,
                type = OperationType.RESET,
                instrumentId = null,
                ticker = null,
                amount = resetAmount,
                quantity = null,
                price = null,
                status = OrderStatus.FILLED,
                now = Clock.System.now()
            )
            updated
        }
    }

    suspend fun withdraw(userId: UUID, amount: BigDecimal): Account {
        MoneyValidation.requirePositive(amount)

        return newSuspendedTransaction(Dispatchers.IO) {
            val before = accountRepository.findByUserId(userId) ?: throw AccountNotFoundException(userId)
            if (before.balance < amount) throw ValidationException("Insufficient funds")
            accountRepository.withdraw(userId, amount)
            val account = accountRepository.findByUserId(userId) ?: throw AccountNotFoundException(userId)
            operationRepository.create(
                accountId = account.id,
                orderId = null,
                type = OperationType.WITHDRAW,
                instrumentId = null,
                ticker = null,
                amount = amount.negate(),
                quantity = null,
                price = null,
                status = OrderStatus.FILLED,
                now = Clock.System.now()
            )
            account
        }
    }

}
