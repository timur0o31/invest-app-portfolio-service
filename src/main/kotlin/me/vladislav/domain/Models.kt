package me.vladislav.domain

import kotlinx.datetime.Instant
import java.math.BigDecimal
import java.util.*

data class Account (
    val id: UUID,
    val userId: UUID,
    val balance: BigDecimal,
    val currency: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class Position (
    val id: UUID,
    val accountId: UUID,
    val instrumentId: UUID,
    val ticker: String,
    val quantity: Int,
    val averageBuyPrice: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant
)