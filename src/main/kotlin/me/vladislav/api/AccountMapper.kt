package me.vladislav.api

import me.vladislav.domain.Account

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