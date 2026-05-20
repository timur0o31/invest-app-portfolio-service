package me.vladislav.orders

import kotlinx.datetime.Instant
import java.math.BigDecimal
import java.util.UUID

data class Order(
    val id: UUID,
    val accountId: UUID,
    val instrumentId: Long,
    val ticker: String,
    val side: OrderSide,
    val type: OrderType,
    val status: OrderStatus,
    val quantity: Int,
    val price: BigDecimal,
    val amount: BigDecimal,
    val rejectReason: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class OrderSide {
    BUY,
    SELL;

    companion object {
        fun parse(value: String): OrderSide? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

enum class OrderType {
    MARKET;

    companion object {
        fun parse(value: String): OrderType? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

enum class OrderStatus {
    FILLED,
    REJECTED;

    companion object {
        fun parse(value: String): OrderStatus? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
