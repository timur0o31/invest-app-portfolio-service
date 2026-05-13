package me.vladislav

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import me.vladislav.api.*
import me.vladislav.data.AccountRepository
import me.vladislav.data.DatabaseFactory
import me.vladislav.domain.AccountService
import org.slf4j.event.Level
import javax.naming.AuthenticationException

fun Application.module() {
    val config = environment.config

    install(CallLogging) { level = Level.INFO }
    install(ContentNegotiation) {
        json(
            Json {
                serializersModule = SerializersModule {
                    contextual(BigDecimalSerializer)
                }
            }
        )
    }

    install(StatusPages) {
        exception<AccountNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(cause.message ?: "Account not found")
            )
        }
        exception<ValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(cause.message ?: "Validation failed")
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(cause.message ?: "Invalid request")
            )
        }
        exception<Throwable> { call, cause ->
            cause.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Internal server error")
            )
        }
    }

    val jwtSecret = config.property("jwt.secret").getString()
    val jwtIssuer = config.property("jwt.issuer").getString()

    authentication {
        jwt("auth-jwt") {

            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .build()
            )

            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()

                if (userId != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                throw AuthenticationException("Invalid or missing token")
            }
        }
    }

    DatabaseFactory.init(environment.config)
    val accountRepository = AccountRepository()
    val accountService = AccountService(accountRepository)

    routing {
        get("/") { call.respondText("Portfolio service is running") }
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        accountRoutes(accountService)
    }
}