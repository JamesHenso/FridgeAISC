package com.production.fridgewise.data.repository

import com.sun.net.httpserver.HttpServer
import com.google.gson.JsonParser
import com.production.fridgewise.domain.model.Fridge
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class InventoryRepositoryTest {
    @Test fun loadsFridgesFoodsAndStatsWithBearerAndEncodedFilters() = runBlocking {
        val paths = mutableListOf<String>()
        val tokens = mutableListOf<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            paths.add(exchange.requestURI.toString())
            tokens.add(exchange.requestHeaders.getFirst("Authorization"))
            val body = when (exchange.requestURI.path) {
                "/fridges" -> """[{"id":"f1","name":"Home"}]"""
                "/fridges/f1/stats" -> """{"inStock":5,"useToday":0,"useSoon":1,"cleanOut":1}"""
                else -> """[{"id":"a","name":"Tomatoes","qty_display":0.5,"unit_display":"kg","location":"fridge","expiry_date":null,"purchased_at":"2026-10-12"}]"""
            }.toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val repo = InventoryRepository("http://127.0.0.1:${server.address.port}")
            assertEquals("Home", repo.fridges("jwt").single().name)
            val food = repo.foods("jwt", "f1", "fridge", "cà & chua").single()
            assertEquals("0.5 kg", food.quantityLabel)
            assertNull(food.expiryDate)
            assertEquals(5, repo.stats("jwt", "f1").inStock)
            assertEquals(listOf("Bearer jwt", "Bearer jwt", "Bearer jwt"), tokens)
            assertEquals("/fridges/f1/foods?location=fridge&q=c%C3%A0+%26+chua", paths[1])
        } finally { server.stop(0) }
    }

    @Test fun createFridgePostsNameAndParsesCreatedFridge() = runBlocking {
        var method: String? = null
        var body = ""
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            method = exchange.requestMethod
            body = exchange.requestBody.bufferedReader().use { it.readText() }
            val bytes = """{"id":"f9","name":"Tủ bếp"}""".toByteArray()
            exchange.sendResponseHeaders(201, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val fridge = InventoryRepository("http://127.0.0.1:${server.address.port}").createFridge("jwt", "Tủ bếp")
            assertEquals(Fridge("f9", "Tủ bếp"), fridge)
            assertEquals("POST", method)
            assertEquals("Tủ bếp", JsonParser.parseString(body).asJsonObject.get("name").asString)
        } finally { server.stop(0) }
    }

    @Test fun deleteFoodSendsAuthenticatedDeleteToTheFoodPath() = runBlocking {
        var method: String? = null
        var path: String? = null
        var token: String? = null
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            method = exchange.requestMethod
            path = exchange.requestURI.path
            token = exchange.requestHeaders.getFirst("Authorization")
            val bytes = """{"deleted":1}""".toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            InventoryRepository("http://127.0.0.1:${server.address.port}").deleteFood("jwt", "food-1")
            assertEquals("DELETE", method)
            assertEquals("/fridges/foods/food-1", path)
            assertEquals("Bearer jwt", token)
        } finally { server.stop(0) }
    }

    @Test fun unauthorizedIsNotMistakenForAnEmptyInventory() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val body = """{"error":"invalid token"}""".toByteArray()
            exchange.sendResponseHeaders(401, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val result = runCatching { InventoryRepository("http://127.0.0.1:${server.address.port}").fridges("expired") }
            assertTrue(result.exceptionOrNull() is InventoryApiException)
            assertEquals(401, (result.exceptionOrNull() as InventoryApiException).statusCode)
        } finally { server.stop(0) }
    }
}
