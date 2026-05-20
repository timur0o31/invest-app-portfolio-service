package me.vladislav.orders

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import me.vladislav.common.ErrorResponse
import me.vladislav.common.ValidationException
import me.vladislav.common.getUserId
import me.vladislav.common.toQueryInstant
import java.util.UUID

fun Routing.orderRoutes(tradingService: TradingService) {
    authenticate("auth-jwt") {
        route("/orders") {
            post {
                val order = tradingService.createOrder(
                    userId = call.getUserId(),
                    request = call.receive<OrderRequest>(),
                    authorization = call.request.header(HttpHeaders.Authorization)
                )
                call.respond(HttpStatusCode.OK, order.toResponse())
            }

            get {
                val status = call.request.queryParameters["status"]?.let {
                    OrderStatus.parse(it) ?: throw ValidationException("status must be filled or rejected")
                }
                val orders = tradingService.listOrders(
                    userId = call.getUserId(),
                    status = status,
                    from = call.request.queryParameters["from"].toQueryInstant(),
                    to = call.request.queryParameters["to"].toQueryInstant()
                )
                call.respond(HttpStatusCode.OK, orders.map { it.toResponse() })
            }

            get("/{id}") {
                val orderId = call.parameters["id"]?.let(UUID::fromString)
                    ?: throw ValidationException("id must be uuid")
                val order = tradingService.getOrder(call.getUserId(), orderId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Order not found"))
                call.respond(HttpStatusCode.OK, order.toResponse())
            }
        }
    }
}
