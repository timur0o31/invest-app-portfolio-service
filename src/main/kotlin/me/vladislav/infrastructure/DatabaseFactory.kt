package me.vladislav.infrastructure

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import me.vladislav.operations.OperationsTable
import me.vladislav.orders.OrdersTable
import me.vladislav.portfolio.AccountsTable
import me.vladislav.portfolio.PositionsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


object DatabaseFactory {
    fun init(appConfig: ApplicationConfig) {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = appConfig.property("database.url").getString()
            username = appConfig.property("database.user").getString()
            password = appConfig.property("database.password").getString()
            maximumPoolSize = 5
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(AccountsTable, PositionsTable, OrdersTable, OperationsTable)
        }
    }
}
