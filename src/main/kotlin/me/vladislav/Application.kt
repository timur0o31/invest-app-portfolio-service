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
import me.vladislav.analytics.AnalyticsService
import me.vladislav.analytics.analyticsRoutes
import me.vladislav.common.AccountNotFoundException
import me.vladislav.common.BigDecimalSerializer
import me.vladislav.common.ErrorResponse
import me.vladislav.common.ValidationException
import me.vladislav.infrastructure.DatabaseFactory
import me.vladislav.operations.OperationRepository
import me.vladislav.operations.OperationService
import me.vladislav.operations.operationRoutes
import me.vladislav.orders.OrderRepository
import me.vladislav.orders.TradingService
import me.vladislav.orders.orderRoutes
import me.vladislav.portfolio.AccountRepository
import me.vladislav.portfolio.AccountService
import me.vladislav.portfolio.PortfolioRepository
import me.vladislav.portfolio.PortfolioService
import me.vladislav.portfolio.accountRoutes
import me.vladislav.portfolio.portfolioRoutes
import me.vladislav.quotes.QuotesClient
import org.slf4j.event.Level
import java.time.Duration
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
    val portfolioRepository = PortfolioRepository()
    val orderRepository = OrderRepository()
    val operationRepository = OperationRepository()
    val quotesBaseUrl = config.propertyOrNull("quotes.baseUrl")?.getString() ?: "http://127.0.0.1:8082"
    val quotesClient = QuotesClient(quotesBaseUrl, Duration.ofSeconds(3))

    val accountService = AccountService(accountRepository, operationRepository, portfolioRepository)
    val portfolioService = PortfolioService(accountRepository, portfolioRepository, quotesClient)
    val tradingService = TradingService(accountRepository, portfolioRepository, orderRepository, operationRepository, quotesClient)
    val operationService = OperationService(accountRepository, operationRepository)
    val analyticsService = AnalyticsService(portfolioService)

    routing {
        get("/") { call.respondText("Portfolio service is running") }
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        accountRoutes(accountService)
        portfolioRoutes(portfolioService)
        orderRoutes(tradingService)
        operationRoutes(operationService)
        analyticsRoutes(analyticsService)
    }
}
