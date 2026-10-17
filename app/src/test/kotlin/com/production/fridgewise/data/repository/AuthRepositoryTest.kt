package com.production.fridgewise.data.repository

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class AuthRepositoryTest {
    @Test
    fun signInSendsCredentialsAndApiKeyToPasswordEndpoint() = runBlocking {
        withServer(200, """{"access_token":"token","refresh_token":"refresh","user":{"id":"user-1"}}""") { repository, requests ->
            val password = "p\"ass\\wordé123"
            assertTrue(repository.signIn("person@example.com", password).isSuccess)
            assertEquals("token", repository.accessToken)
            val request = requests.single()
            assertEquals("POST", request.method)
            assertEquals("/auth/v1/token?grant_type=password", request.path)
            assertEquals("test-anon-key", request.apiKey)
            assertTrue(request.contentType.startsWith("application/json"))
            assertEquals("person@example.com", JsonParser.parseString(request.body).asJsonObject.get("email").asString)
            assertEquals(password, JsonParser.parseString(request.body).asJsonObject.get("password").asString)
        }
    }

    @Test
    fun signUpSucceedsWhenEmailConfirmationMeansNoSessionIsReturned() = runBlocking {
        withServer(200, """{"id":"user-1","email":"person@example.com","confirmation_sent_at":"2026-10-15T00:00:00Z"}""") { repository, requests ->
            assertTrue(repository.signUp("person@example.com", "password123").isSuccess)
            assertNull(repository.accessToken)
            assertEquals("/auth/v1/signup", requests.single().path)
            assertEquals("person@example.com", JsonParser.parseString(requests.single().body).asJsonObject.get("email").asString)
            assertEquals("password123", JsonParser.parseString(requests.single().body).asJsonObject.get("password").asString)
        }
    }

    @Test
    fun authErrorsAreReturnedForTheExistingUi() = runBlocking {
        for (field in listOf("msg", "error_description", "message")) {
            withServer(400, """{"$field":"Invalid login credentials"}""") { repository, _ ->
                val result = repository.signIn("person@example.com", "wrong")
                assertTrue(result.isFailure)
                assertEquals("Invalid login credentials", result.exceptionOrNull()?.message)
            }
        }
    }

    @Test
    fun nonJsonErrorsStillProduceAReadableFailure() = runBlocking {
        withServer(503, "<html>Unavailable</html>") { repository, _ ->
            val result = repository.signUp("person@example.com", "password123")
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("503"))
        }
    }

    private suspend fun withServer(
        status: Int,
        body: String,
        block: suspend (AuthRepository, List<Request>) -> Unit
    ) {
        val requests = java.util.Collections.synchronizedList(mutableListOf<Request>())
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests.add(Request(
                exchange.requestMethod,
                exchange.requestURI.toString(),
                exchange.requestHeaders.getFirst("apikey"),
                exchange.requestHeaders.getFirst("Content-Type"),
                exchange.requestBody.bufferedReader().use { it.readText() }
            ))
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            block(AuthRepository("http://127.0.0.1:${server.address.port}/", "test-anon-key"), requests)
        } finally {
            server.stop(0)
        }
    }

    private data class Request(
        val method: String,
        val path: String,
        val apiKey: String,
        val contentType: String,
        val body: String
    )
}
