package me.vladislav.operations

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import me.vladislav.portfolio.AccountRepository
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class OperationService(
    private val accountRepository: AccountRepository,
    private val operationRepository: OperationRepository
) {
    suspend fun listOperations(
        userId: UUID,
        type: OperationType?,
        from: Instant?,
        to: Instant?,
        limit: Int,
        offset: Long
    ): List<Operation> =
        newSuspendedTransaction(Dispatchers.IO) {
            val account = accountRepository.findByUserId(userId) ?: accountRepository.create(userId)
            operationRepository.list(account.id, type, from, to, limit, offset)
        }
}
