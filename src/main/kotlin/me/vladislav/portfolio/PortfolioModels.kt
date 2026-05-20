package me.vladislav.portfolio

import kotlinx.datetime.Instant
import java.math.BigDecimal
import java.util.UUID

data class Account(
    val id: UUID,
    val userId: UUID,
    val balance: BigDecimal,
    val currency: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class Position(
    val id: UUID,
    val accountId: UUID,
    val instrumentId: Long,
    val ticker: String,
    val quantity: Int,
    val averageBuyPrice: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant
)
