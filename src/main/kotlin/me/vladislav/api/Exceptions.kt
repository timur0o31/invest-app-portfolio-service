package me.vladislav.api

import java.util.UUID

class ValidationException(message: String) : RuntimeException(message)

class UserNotFoundException(
    val userId: UUID,
    message: String = "User not found with ID: $userId"
) : RuntimeException(message)

class AccountNotFoundException(
    val userId: UUID,
    message: String = "Account with user id: $userId not found"
) : RuntimeException(message)