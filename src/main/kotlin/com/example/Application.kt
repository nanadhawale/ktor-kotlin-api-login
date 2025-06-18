package com.example

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.createMissingTablesAndColumns
import org.jetbrains.exposed.dao.id.IntIdTable

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    connectToDB()

    transaction {
        createMissingTablesAndColumns(Users)
    }

    install(ContentNegotiation) {
        json(Json { prettyPrint = true })
    }

    routing {
        get("/") {
            call.respond(mapOf("message" to "Welcome to Kotlin Ktor API with DB!"))
        }

        post("/signup") {
            val req = call.receive<SignupRequest>()
            val existing = transaction {
                Users.select { Users.email eq req.email }.singleOrNull()
            }
            if (existing != null) {
                call.respond(mapOf("error" to "Email already registered"))
                return@post
            }

            val userId = transaction {
                Users.insertAndGetId {
                    it[name] = req.name
                    it[email] = req.email
                    it[password] = req.password
                }.value
            }

            call.respond(User(id = userId, name = req.name))
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            val user = transaction {
                Users.select {
                    (Users.email eq req.email) and (Users.password eq req.password)
                }.map {
                    User(it[Users.id].value, it[Users.name])
                }.singleOrNull()
            }

            if (user != null) {
                call.respond(user)
            } else {
                call.respond(mapOf("error" to "Invalid credentials"))
            }
        }
    }
}

fun connectToDB() {
    val rawUrl = System.getenv("DATABASE_URL")
        ?: "postgresql://postgres:pjfibPjjtbFZrZxfaqYcmnExbxUNcFgf@shortline.proxy.rlwy.net:59221/railway"

    val uri = java.net.URI("postgresql://${rawUrl.removePrefix("postgresql://")}")
    val userInfo = uri.userInfo.split(":")
    val jdbcUrl = "jdbc:postgresql://${uri.host}:${uri.port}${uri.path}"

    Database.connect(
        url = jdbcUrl,
        driver = "org.postgresql.Driver",
        user = userInfo[0],
        password = userInfo[1]
    )
}

// 🔧 Fixed table using IntIdTable so insertAndGetId() works properly
object Users : IntIdTable() {
    val name = varchar("name", 100)
    val email = varchar("email", 100).uniqueIndex()
    val password = varchar("password", 100)
}

// Updated to match IntIdTable primary key (Int)
@Serializable
data class User(val id: Int, val name: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class SignupRequest(val name: String, val email: String, val password: String)
