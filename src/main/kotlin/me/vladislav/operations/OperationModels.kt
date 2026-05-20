package me.vladislav.operations

import kotlinx.datetime.Instant
import me.vladislav.orders.OrderStatus
import java.math.BigDecimal
import java.util.UUID

data class Operation(
    val id: UUID,
    val accountId: UUID,
    val orderId: UUID?,
    val type: OperationType,
    val instrumentId: Long?,
    val ticker: String?,
    val amount: BigDecimal,
    val quantity: Int?,
    val price: BigDecimal?,
    val status: OrderStatus,
    val createdAt: Instant
)

enum class OperationType {
    BUY,
    SELL,
    DEPOSIT,
    WITHDRAW,
    RESET;

    companion object {
        fun parse(value: String): OperationType? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
