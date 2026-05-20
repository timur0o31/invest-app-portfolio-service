package me.vladislav.portfolio

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.vladislav.common.getUserId

fun Routing.accountRoutes(accountService: AccountService) {
    authenticate("auth-jwt") {
        route("/accounts") {
            get {
                val userId = call.getUserId()
                val response = accountService.getOrCreateAccount(userId).toResponse()
                call.respond(HttpStatusCode.OK, response)
            }
            post("/deposit") {
                val userId = call.getUserId()
                val request = call.receive<DepositRequest>()
                val response = accountService.deposit(userId, request.amount).toResponse()
                call.respond(HttpStatusCode.OK, response)
            }
            post("/reset") {
                val userId = call.getUserId()
                val request = runCatching { call.receive<ResetRequest>() }.getOrDefault(ResetRequest())
                val response = accountService.reset(userId, request.amount).toResponse()
                call.respond(HttpStatusCode.OK, response)
            }
            post("/withdraw") {
                val userId = call.getUserId()
                val request = call.receive<WithdrawRequest>()
                val response = accountService.withdraw(userId, request.amount).toWithdrawResponse()
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
