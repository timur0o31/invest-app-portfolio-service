package me.vladislav.operations

fun Operation.toResponse() = OperationResponse(
    id = id.toString(),
    orderId = orderId?.toString(),
    type = type.name.lowercase(),
    instrumentId = instrumentId,
    ticker = ticker,
    amount = amount,
    quantity = quantity,
    price = price,
    status = status.name.lowercase(),
    createdAt = createdAt.toString()
)
