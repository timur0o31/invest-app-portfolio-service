package me.vladislav.orders

import me.vladislav.portfolio.AccountsTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object OrdersTable : UUIDTable("orders") {
    val accountId = reference("account_id", AccountsTable)
    val instrumentId = long("instrument_id")
    val ticker = varchar("ticker", 20)
    val side = varchar("side", 8)
    val type = varchar("type", 16)
    val status = varchar("status", 16)
    val quantity = integer("quantity")
    val price = decimal("price", 18, 2)
    val amount = decimal("amount", 18, 2)
    val rejectReason = varchar("reject_reason", 255).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
