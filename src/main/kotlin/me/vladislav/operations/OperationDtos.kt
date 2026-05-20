package me.vladislav.operations

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class OperationResponse(
    val id: String,
    val orderId: String? = null,
    val type: String,
    val instrumentId: Long? = null,
    val ticker: String? = null,
    @Contextual
    val amount: BigDecimal,
    val quantity: Int? = null,
    @Contextual
    val price: BigDecimal? = null,
    val status: String,
    val createdAt: String
)
