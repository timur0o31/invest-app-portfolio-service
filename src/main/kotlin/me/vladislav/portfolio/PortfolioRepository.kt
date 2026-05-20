package me.vladislav.portfolio

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class PortfolioRepository {
    fun positions(accountId: UUID): List<Position> =
        PositionsTable.select { PositionsTable.accountId eq accountId }
            .map { it.toPosition() }

    fun findPosition(accountId: UUID, instrumentId: Long): Position? =
        PositionsTable
            .select { (PositionsTable.accountId eq accountId) and (PositionsTable.instrumentId eq instrumentId) }
            .map { it.toPosition() }
            .singleOrNull()

    fun buy(accountId: UUID, instrumentId: Long, ticker: String, quantity: Int, price: BigDecimal): Position {
        val existing = findPosition(accountId, instrumentId)
        val now = Clock.System.now()

        if (existing == null) {
            val id = UUID.randomUUID()
            PositionsTable.insert {
                it[PositionsTable.id] = id
                it[PositionsTable.accountId] = accountId
                it[PositionsTable.instrumentId] = instrumentId
                it[PositionsTable.ticker] = ticker
                it[PositionsTable.quantity] = quantity
                it[averageBuyPrice] = price
                it[createdAt] = now
                it[updatedAt] = now
            }
            return findPosition(accountId, instrumentId) ?: error("Position was not created")
        }

        val newQuantity = existing.quantity + quantity
        val totalCost = existing.averageBuyPrice
            .multiply(existing.quantity.toBigDecimal())
            .plus(price.multiply(quantity.toBigDecimal()))
        val newAverage = totalCost.divide(newQuantity.toBigDecimal(), 2, RoundingMode.HALF_UP)

        PositionsTable.update(
            where = { (PositionsTable.accountId eq accountId) and (PositionsTable.instrumentId eq instrumentId) }
        ) {
            it[PositionsTable.ticker] = ticker
            it[PositionsTable.quantity] = newQuantity
            it[averageBuyPrice] = newAverage
            it[updatedAt] = now
        }

        return findPosition(accountId, instrumentId) ?: error("Position was not updated")
    }

    fun sell(accountId: UUID, instrumentId: Long, quantity: Int): Position? {
        val existing = findPosition(accountId, instrumentId) ?: return null
        val newQuantity = existing.quantity - quantity

        if (newQuantity <= 0) {
            PositionsTable.deleteWhere {
                (PositionsTable.accountId eq accountId) and (PositionsTable.instrumentId eq instrumentId)
            }
            return null
        }

        PositionsTable.update(
            where = { (PositionsTable.accountId eq accountId) and (PositionsTable.instrumentId eq instrumentId) }
        ) {
            it[PositionsTable.quantity] = newQuantity
            it[updatedAt] = Clock.System.now()
        }

        return findPosition(accountId, instrumentId)
    }

    fun clearPositions(accountId: UUID) {
        PositionsTable.deleteWhere { PositionsTable.accountId eq accountId }
    }

    private fun ResultRow.toPosition(): Position =
        Position(
            id = this[PositionsTable.id].value,
            accountId = this[PositionsTable.accountId].value,
            instrumentId = this[PositionsTable.instrumentId],
            ticker = this[PositionsTable.ticker],
            quantity = this[PositionsTable.quantity],
            averageBuyPrice = this[PositionsTable.averageBuyPrice],
            createdAt = this[PositionsTable.createdAt],
            updatedAt = this[PositionsTable.updatedAt]
        )
}
