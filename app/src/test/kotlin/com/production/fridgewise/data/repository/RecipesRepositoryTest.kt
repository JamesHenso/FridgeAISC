package com.production.fridgewise.data.repository

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RecipesRepositoryTest {
    @Test fun mapsCardsEncodesSearchAndSendsFavoriteMutations() = runBlocking {
        val requests = mutableListOf<Triple<String, String, String>>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            assertEquals("Bearer jwt", exchange.requestHeaders.getFirst("Authorization"))
            requests.add(Triple(exchange.requestMethod, exchange.requestURI.toString(), exchange.requestBody.bufferedReader().readText()))
            val body = if (exchange.requestMethod == "GET") """[{"id":"r1","name":"Cơm chiên","imageUrl":"https://example.com/food.jpg","servings":2,"time":30,"difficulty":null}]""" else "{}"
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val repo = RecipesRepository("http://127.0.0.1:${server.address.port}")
            val card = repo.recipes("jwt", "f1", "").single()
            assertEquals("Cơm chiên", card.name)
            assertEquals(2, card.servings)
            assertEquals(30, card.minutes)
            assertNull(card.difficulty)
            repo.recipes("jwt", "f1", "cơm & trứng")
            repo.recipes("jwt", null, "")
            assertEquals(setOf("r1"), repo.favorites("jwt"))
            val favorite = repo.favoriteRecipes("jwt").single()
            assertEquals("r1", favorite.id)
            assertEquals("Cơm chiên", favorite.name)
            assertEquals(2, favorite.servings)
            assertEquals(30, favorite.minutes)
            repo.setFavorite("jwt", "r1", true)
            repo.setFavorite("jwt", "r1", false)
            assertEquals("/recipes/match?fridge_id=f1&limit=5", requests[0].second)
            assertEquals("/recipes/search?name=c%C6%A1m+%26+tr%E1%BB%A9ng&limit=50", requests[1].second)
            assertEquals("/recipes/search?name=&limit=50", requests[2].second)
            assertEquals("/favorites", requests[3].second)
            assertEquals("/favorites", requests[4].second)
            assertEquals(Triple("POST", "/favorites", """{"recipe_id":"r1"}"""), requests[5])
            assertEquals("DELETE", requests[6].first)
            assertEquals("/favorites/r1", requests[6].second)
        } finally { server.stop(0) }
    }

    @Test fun reportsUnauthorizedInsteadOfAnEmptyList() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val body = """{"error":"invalid token"}""".toByteArray()
            exchange.sendResponseHeaders(401, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val error = runCatching { RecipesRepository("http://127.0.0.1:${server.address.port}").recipes("jwt", "f1", "") }.exceptionOrNull()
            assertTrue(error is RecipesApiException)
            assertEquals(401, (error as RecipesApiException).statusCode)
        } finally { server.stop(0) }
    }
}
