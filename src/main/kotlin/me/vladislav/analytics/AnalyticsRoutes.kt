package me.vladislav.analytics

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import me.vladislav.common.getUserId

fun Routing.analyticsRoutes(analyticsService: AnalyticsService) {
    authenticate("auth-jwt") {
        route("/analytics/portfolio") {
            get("/value") {
                val period = call.request.queryParameters["period"] ?: "1d"
                call.respond(
                    HttpStatusCode.OK,
                    analyticsService.value(
                        userId = call.getUserId(),
                        period = period,
                        authorization = call.request.header(HttpHeaders.Authorization)
                    )
                )
            }

            get("/allocation") {
                val period = call.request.queryParameters["period"] ?: "1d"
                call.respond(
                    HttpStatusCode.OK,
                    analyticsService.allocation(
                        userId = call.getUserId(),
                        period = period,
                        authorization = call.request.header(HttpHeaders.Authorization)
                    )
                )
            }
        }
    }
}
