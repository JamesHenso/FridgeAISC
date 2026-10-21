package com.production.fridgewise.data.repository

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RecipeDetailRepositoryTest {
    @Test fun readsDetailAndJoinsHaveNeedToRecipeQuantities() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var path = ""
        var auth: String? = null
        server.createContext("/") { exchange ->
            path = exchange.requestURI.toString()
            auth = exchange.requestHeaders.getFirst("Authorization")
            val body = """{"info":{"id":"r1","name":"Ba rọi xào cà chua","video_id":"abcdefghijk","video_provider":"youtube"},
                "detail":{"servings":2,"time":30,"difficulty":"Dễ","preparation":"Rửa sạch","instructions":"Xào thịt","servingText":"Dùng nóng","videoUrl":null},
                "ingredients":[{"ingredientId":"pork","name":"Thịt ba rọi","quantity":"350","unit":"g","image_url":"/assets/ba_chi.webp"},{"ingredientId":"garlic","name":"Tỏi","quantity":"2","unit":"tép"}],
                "haveNeed":{"matched":[{"ingredientId":"pork","name":"Thịt"}],"missing":[{"ingredientId":"garlic","name":"Tỏi"}]}}""".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val detail = RecipesRepository("http://127.0.0.1:${server.address.port}").detail("jwt", "r1", "fridge-1")
            assertEquals("/recipes/r1?fridge_id=fridge-1", path)
            assertEquals("Bearer jwt", auth)
            assertEquals(2, detail.recipe.servings)
            assertEquals(30, detail.recipe.minutes)
            assertEquals("350 g", detail.have!!.single().amount)
            assertEquals("http://127.0.0.1:${server.address.port}/assets/ba_chi.webp", detail.have!!.single().imageUrl)
            assertEquals("2 tép", detail.need!!.single().amount)
            assertEquals("Xào thịt", detail.instructions)
            assertEquals("https://www.youtube.com/watch?v=abcdefghijk", detail.videoUrl)
        } finally { server.stop(0) }
    }
    @Test fun toleratesMissingDetailAndDoesNotInventHaveNeedWithoutFridge() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val body = """{"info":{"id":"r1","name":"Món ăn"},"detail":null,"ingredients":[]}""".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val detail = RecipesRepository("http://127.0.0.1:${server.address.port}").detail("jwt", "r1", null)
            assertNull(detail.have)
            assertNull(detail.need)
            assertNull(detail.videoUrl)
            assertNull(detail.recipe.minutes)
            assertTrue(detail.ingredients.isEmpty())
        } finally { server.stop(0) }
    }
}
