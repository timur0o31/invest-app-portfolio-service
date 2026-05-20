package me.vladislav.portfolio

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object PositionsTable : UUIDTable("positions") {
    val accountId = reference("account_id", AccountsTable)
    val instrumentId = long("instrument_id")
    val ticker = varchar("ticker", 20)
    val quantity = integer("quantity")
    val averageBuyPrice = decimal("average_buy_price", 18, 2)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
