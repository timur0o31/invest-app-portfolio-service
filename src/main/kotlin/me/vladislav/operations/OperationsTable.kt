package me.vladislav.operations

import me.vladislav.orders.OrdersTable
import me.vladislav.portfolio.AccountsTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object OperationsTable : UUIDTable("operations") {
    val accountId = reference("account_id", AccountsTable)
    val orderId = reference("order_id", OrdersTable).nullable()
    val type = varchar("type", 16)
    val instrumentId = long("instrument_id").nullable()
    val ticker = varchar("ticker", 20).nullable()
    val amount = decimal("amount", 18, 2)
    val quantity = integer("quantity").nullable()
    val price = decimal("price", 18, 2).nullable()
    val status = varchar("status", 16)
    val createdAt = timestamp("created_at")
}
