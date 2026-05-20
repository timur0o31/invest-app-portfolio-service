package me.vladislav.portfolio

fun Account.toResponse() = AccountResponse(
    id = id.toString(),
    userId = userId.toString(),
    balance = balance,
    currency = currency
)

fun Account.toDepositResponse() = DepositResponse(
    balance = balance
)

fun Account.toWithdrawResponse() = WithdrawResponse(
    balance = balance
)

fun Position.toResponse() = PositionResponse(
    id = id.toString(),
    instrumentId = instrumentId,
    ticker = ticker,
    quantity = quantity,
    averageBuyPrice = averageBuyPrice
)
