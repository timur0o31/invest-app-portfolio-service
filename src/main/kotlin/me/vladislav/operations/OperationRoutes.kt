package me.vladislav.operations

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import me.vladislav.common.ValidationException
import me.vladislav.common.getUserId
import me.vladislav.common.toQueryInstant
import me.vladislav.common.toQueryLimit
import me.vladislav.common.toQueryOffset

fun Routing.operationRoutes(operationService: OperationService) {
    authenticate("auth-jwt") {
        get("/operations") {
            val type = call.request.queryParameters["type"]?.let {
                OperationType.parse(it) ?: throw ValidationException("type must be buy, sell, deposit, withdraw or reset")
            }
            val operations = operationService.listOperations(
                userId = call.getUserId(),
                type = type,
                from = call.request.queryParameters["from"].toQueryInstant(),
                to = call.request.queryParameters["to"].toQueryInstant(),
                limit = call.request.queryParameters["limit"].toQueryLimit(),
                offset = call.request.queryParameters["offset"].toQueryOffset()
            )
            call.respond(HttpStatusCode.OK, operations.map { it.toResponse() })
        }
    }
}
