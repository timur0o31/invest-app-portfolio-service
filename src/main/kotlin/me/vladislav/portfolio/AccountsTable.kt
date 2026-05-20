package me.vladislav.portfolio

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object AccountsTable : UUIDTable("accounts") {
    val userId = uuid("user_id").uniqueIndex()
    val balance = decimal("balance",  18, 2)
    val currency = varchar("currency", 3).default("RUB")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
