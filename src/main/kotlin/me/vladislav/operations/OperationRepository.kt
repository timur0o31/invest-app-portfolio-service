package me.vladislav.operations

import kotlinx.datetime.Instant
import me.vladislav.orders.OrderStatus
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.math.BigDecimal
import java.util.UUID

class OperationRepository {
    fun create(
        accountId: UUID,
        orderId: UUID?,
        type: OperationType,
        instrumentId: Long?,
        ticker: String?,
        amount: BigDecimal,
        quantity: Int?,
        price: BigDecimal?,
        status: OrderStatus,
        now: Instant
    ): Operation {
        val id = UUID.randomUUID()
        OperationsTable.insert {
            it[OperationsTable.id] = id
            it[OperationsTable.accountId] = accountId
            it[OperationsTable.orderId] = orderId
            it[OperationsTable.type] = type.name
            it[OperationsTable.instrumentId] = instrumentId
            it[OperationsTable.ticker] = ticker
            it[OperationsTable.amount] = amount
            it[OperationsTable.quantity] = quantity
            it[OperationsTable.price] = price
            it[OperationsTable.status] = status.name
            it[OperationsTable.createdAt] = now
        }
        return findById(id) ?: error("Operation was not created")
    }

    fun findById(id: UUID): Operation? =
        OperationsTable.select { OperationsTable.id eq id }
            .map { it.toOperation() }
            .singleOrNull()

    fun list(
        accountId: UUID,
        type: OperationType?,
        from: Instant?,
        to: Instant?,
        limit: Int,
        offset: Long
    ): List<Operation> {
        var query = OperationsTable.select { OperationsTable.accountId eq accountId }
        if (type != null) query = query.andWhere { OperationsTable.type eq type.name }
        if (from != null) query = query.andWhere { OperationsTable.createdAt greaterEq from }
        if (to != null) query = query.andWhere { OperationsTable.createdAt lessEq to }
        return query
            .orderBy(OperationsTable.createdAt, SortOrder.DESC)
            .limit(limit, offset)
            .map { it.toOperation() }
    }

    private fun ResultRow.toOperation(): Operation =
        Operation(
            id = this[OperationsTable.id].value,
            accountId = this[OperationsTable.accountId].value,
            orderId = this[OperationsTable.orderId]?.value,
            type = OperationType.valueOf(this[OperationsTable.type]),
            instrumentId = this[OperationsTable.instrumentId],
            ticker = this[OperationsTable.ticker],
            amount = this[OperationsTable.amount],
            quantity = this[OperationsTable.quantity],
            price = this[OperationsTable.price],
            status = OrderStatus.valueOf(this[OperationsTable.status]),
            createdAt = this[OperationsTable.createdAt]
        )
}
