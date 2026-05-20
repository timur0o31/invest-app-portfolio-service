package me.vladislav.orders

fun Order.toResponse() = OrderResponse(
    id = id.toString(),
    instrumentId = instrumentId,
    ticker = ticker,
    side = side.name.lowercase(),
    type = type.name.lowercase(),
    status = status.name.lowercase(),
    quantity = quantity,
    price = price,
    amount = amount,
    rejectReason = rejectReason,
    createdAt = createdAt.toString()
)
