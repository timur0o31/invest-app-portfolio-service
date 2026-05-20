package me.vladislav.portfolio

import kotlinx.datetime.Clock
import me.vladislav.common.AccountNotFoundException
import org.jetbrains.exposed.sql.*
import java.math.BigDecimal
import java.util.*

class AccountRepository {
    fun findByUserId(userId: UUID): Account? {
        return AccountsTable.select { AccountsTable.userId eq userId }
            .mapNotNull { it.toAccount() }
            .singleOrNull()
    }

    fun findById(accountId: UUID): Account? {
        return AccountsTable.select { AccountsTable.id eq accountId }
            .mapNotNull { it.toAccount() }
            .singleOrNull()
    }

    fun create(userId: UUID): Account {
        val id = UUID.randomUUID()

        AccountsTable.insert {
            it[AccountsTable.id] = id
            it[AccountsTable.userId] = userId
            it[balance] = BigDecimal.ZERO
            it[currency] = "RUB"
            it[createdAt] = Clock.System.now()
            it[updatedAt] = Clock.System.now()
        }

        return Account(
            id = id,
            userId = userId,
            balance = BigDecimal.ZERO,
            currency = "RUB",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }

    fun deposit(userId: UUID, amount: BigDecimal) {
        val updatedRows = AccountsTable.update(where = { AccountsTable.userId eq userId }) {
            with(SqlExpressionBuilder) {
                it.update(balance, balance + amount)
            }
            it[updatedAt] = Clock.System.now()
        }
        if (updatedRows == 0) {
            throw AccountNotFoundException(userId)
        }
    }

    fun updateBalance(accountId: UUID, balance: BigDecimal) {
        AccountsTable.update(where = { AccountsTable.id eq accountId }) {
            it[AccountsTable.balance] = balance
            it[updatedAt] = Clock.System.now()
        }
    }

    fun setBalance(userId: UUID, amount: BigDecimal) {
        val updatedRows = AccountsTable.update(where = { AccountsTable.userId eq userId }) {
            it[balance] = amount
            it[updatedAt] = Clock.System.now()
        }
        if (updatedRows == 0) {
            throw AccountNotFoundException(userId)
        }
    }

    fun withdraw(userId: UUID, amount: BigDecimal) {
        val updatedRows = AccountsTable.update(where = { AccountsTable.userId eq userId }) {
            with(SqlExpressionBuilder) {
                it.update(balance, balance - amount)
            }
            it[updatedAt] = Clock.System.now()
        }
        if (updatedRows == 0) {
            throw AccountNotFoundException(userId)
        }
    }

    private fun ResultRow.toAccount(): Account =
        Account(
            id = this[AccountsTable.id].value,
            userId = this[AccountsTable.userId],
            balance = this[AccountsTable.balance],
            currency = this[AccountsTable.currency],
            createdAt = this[AccountsTable.createdAt],
            updatedAt = this[AccountsTable.updatedAt]
        )
}
