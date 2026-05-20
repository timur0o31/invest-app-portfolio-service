package me.vladislav.orders

import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.math.BigDecimal
import java.util.UUID

class OrderRepository {
    fun create(
        accountId: UUID,
        instrumentId: Long,
        ticker: String,
        side: OrderSide,
        type: OrderType,
        status: OrderStatus,
        quantity: Int,
        price: BigDecimal,
        amount: BigDecimal,
        rejectReason: String?,
        now: Instant
    ): Order {
        val id = UUID.randomUUID()
        OrdersTable.insert {
            it[OrdersTable.id] = id
            it[OrdersTable.accountId] = accountId
            it[OrdersTable.instrumentId] = instrumentId
            it[OrdersTable.ticker] = ticker
            it[OrdersTable.side] = side.name
            it[OrdersTable.type] = type.name
            it[OrdersTable.status] = status.name
            it[OrdersTable.quantity] = quantity
            it[OrdersTable.price] = price
            it[OrdersTable.amount] = amount
            it[OrdersTable.rejectReason] = rejectReason
            it[OrdersTable.createdAt] = now
            it[OrdersTable.updatedAt] = now
        }
        return findById(id) ?: error("Order was not created")
    }

    fun findById(id: UUID): Order? =
        OrdersTable.select { OrdersTable.id eq id }
            .map { it.toOrder() }
            .singleOrNull()

    fun list(
        accountId: UUID,
        status: OrderStatus?,
        from: Instant?,
        to: Instant?
    ): List<Order> {
        var query = OrdersTable.select { OrdersTable.accountId eq accountId }
        if (status != null) query = query.andWhere { OrdersTable.status eq status.name }
        if (from != null) query = query.andWhere { OrdersTable.createdAt greaterEq from }
        if (to != null) query = query.andWhere { OrdersTable.createdAt lessEq to }
        return query.orderBy(OrdersTable.createdAt, SortOrder.DESC).map { it.toOrder() }
    }

    private fun ResultRow.toOrder(): Order =
        Order(
            id = this[OrdersTable.id].value,
            accountId = this[OrdersTable.accountId].value,
            instrumentId = this[OrdersTable.instrumentId],
            ticker = this[OrdersTable.ticker],
            side = OrderSide.valueOf(this[OrdersTable.side]),
            type = OrderType.valueOf(this[OrdersTable.type]),
            status = OrderStatus.valueOf(this[OrdersTable.status]),
            quantity = this[OrdersTable.quantity],
            price = this[OrdersTable.price],
            amount = this[OrdersTable.amount],
            rejectReason = this[OrdersTable.rejectReason],
            createdAt = this[OrdersTable.createdAt],
            updatedAt = this[OrdersTable.updatedAt]
        )
}
