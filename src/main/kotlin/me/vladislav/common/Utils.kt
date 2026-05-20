package me.vladislav.common

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.datetime.Instant
import java.util.*

fun ApplicationCall.getUserId(): UUID {
    val principal = principal<JWTPrincipal>()
        ?: throw IllegalStateException("No JWT principal")

    val userId = principal.payload.getClaim("userId").asString()
        ?: throw IllegalStateException("userId missing in token")

    return UUID.fromString(userId)
}

fun String?.toQueryInstant(): Instant? {
    val value = this?.takeIf { it.isNotBlank() } ?: return null
    return value.toLongOrNull()?.let { Instant.fromEpochMilliseconds(it) }
        ?: runCatching { Instant.parse(value) }.getOrElse {
            throw ValidationException("timestamp must be unix milliseconds or ISO-8601 instant")
        }
}

fun String?.toQueryLimit(default: Int = 50, max: Int = 200): Int =
    this?.toIntOrNull()?.coerceIn(1, max) ?: default

fun String?.toQueryOffset(): Long =
    this?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
