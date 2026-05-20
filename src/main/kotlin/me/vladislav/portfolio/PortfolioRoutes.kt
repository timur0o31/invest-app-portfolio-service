package me.vladislav.portfolio

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import me.vladislav.common.getUserId

fun Routing.portfolioRoutes(portfolioService: PortfolioService) {
    authenticate("auth-jwt") {
        route("/portfolio") {
            get("/positions") {
                call.respond(HttpStatusCode.OK, portfolioService.positions(call.getUserId()))
            }
            get("/summary") {
                call.respond(
                    HttpStatusCode.OK,
                    portfolioService.summary(call.getUserId(), call.request.header(HttpHeaders.Authorization))
                )
            }
        }
    }
}
