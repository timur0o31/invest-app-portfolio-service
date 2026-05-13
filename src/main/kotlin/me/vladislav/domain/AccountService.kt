package me.vladislav.domain

import kotlinx.coroutines.Dispatchers
import me.vladislav.api.AccountNotFoundException
import me.vladislav.data.AccountRepository
import me.vladislav.domain.validation.MoneyValidation
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.util.*

class AccountService(
    private val accountRepository: AccountRepository
) {

    suspend fun getOrCreateAccount(userId: UUID): Account =
        newSuspendedTransaction(Dispatchers.IO) {
            accountRepository.findByUserId(userId) ?: accountRepository.create(userId)
        }

    suspend fun deposit(userId: UUID, amount: BigDecimal): Account {
        MoneyValidation.requirePositive(amount)

        return newSuspendedTransaction(Dispatchers.IO) {
            accountRepository.deposit(userId, amount)
            accountRepository.findByUserId(userId) ?: throw AccountNotFoundException(userId)
        }
    }

    suspend fun withdraw(userId: UUID, amount: BigDecimal): Account {
        MoneyValidation.requirePositive(amount)

        return newSuspendedTransaction(Dispatchers.IO) {
            accountRepository.withdraw(userId, amount)
            accountRepository.findByUserId(userId) ?: throw AccountNotFoundException(userId)
        }
    }

}