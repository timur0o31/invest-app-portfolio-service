package me.vladislav.orders

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class OrderRequest(
    val instrumentId: Long,
    val side: String,
    val type: String = "market",
    val qty: Int? = null,
    val quantity: Int? = null,
    val ticker: String? = null
)

@Serializable
data class OrderResponse(
    val id: String,
    val instrumentId: Long,
    val ticker: String,
    val side: String,
    val type: String,
    val status: String,
    val quantity: Int,
    @Contextual
    val price: BigDecimal,
    @Contextual
    val amount: BigDecimal,
    val rejectReason: String? = null,
    val createdAt: String
)
