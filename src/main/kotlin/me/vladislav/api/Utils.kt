package me.vladislav.api

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

fun ApplicationCall.getUserId(): UUID {
    val principal = principal<JWTPrincipal>()
        ?: throw IllegalStateException("No JWT principal")

    val userId = principal.payload.getClaim("userId").asString()
        ?: throw IllegalStateException("userId missing in token")

    return UUID.fromString(userId)
}