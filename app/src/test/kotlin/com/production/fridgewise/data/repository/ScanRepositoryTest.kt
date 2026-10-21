package com.production.fridgewise.data.repository

import com.google.gson.JsonParser
import com.production.fridgewise.domain.model.ScanMode
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ScanRepositoryTest {
    @Test fun uploadsReceiptCreatesDraftAndEditsUsingScanItemId() = runBlocking {
        val requests = mutableListOf<Triple<String, String, String>>()
        val authHeaders = mutableListOf<String?>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests.add(Triple(exchange.requestMethod, exchange.requestURI.path, exchange.requestBody.bufferedReader().readText()))
            authHeaders.add(exchange.requestHeaders.getFirst("Authorization"))
            val body = when (exchange.requestURI.path) {
                "/api/receipts/scan" -> """{"items":[{"name":"Cà chua","ingredient_id":"ingredient-1","qty_display":"0,5","unit_display":"kg","purchased_at":"2026-10-15","expiry_date":null}]}"""
                "/scans" -> """{"scanId":"scan-1","inserted":1,"items":[{"id":"scan-item-1","name":"Cà chua"}]}"""
                else -> """{"updated":1}"""
            }.toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val url = "http://127.0.0.1:${server.address.port}"
            val repo = ScanRepository(url, url)
            val items = repo.recognize(ScanMode.RECEIPT, "image/png") { "receipt-bytes".byteInputStream() }
            assertEquals(0.5, items.items.single().quantity)
            assertNull(items.items.single().expiryDate)
            assertTrue(requests[0].third.contains("name=\"file\""))
            assertTrue(requests[0].third.contains("Content-Type: image/png"))
            assertTrue(requests[0].third.contains("receipt-bytes"))
            assertNull(authHeaders[0])
            val draft = repo.createDraft("jwt", "fridge-1", items)
            assertEquals("scan-item-1", draft.items.single().id)
            val createBody = JsonParser.parseString(requests[1].third).asJsonObject
            assertEquals("fridge-1", createBody.get("fridge_id").asString)
            assertEquals("ingredient-1", createBody.getAsJsonArray("items")[0].asJsonObject.get("ingredient_id").asString)
            repo.updateItem("jwt", draft.scanId, draft.items.single().copy(quantity = 2.0))
            assertEquals("PUT", requests[2].first)
            assertEquals("/scans/scan-1/items", requests[2].second)
            val edited = JsonParser.parseString(requests[2].third).asJsonObject.getAsJsonArray("items")[0].asJsonObject
            assertEquals("scan-item-1", edited.get("id").asString)
            assertEquals(2.0, edited.get("qty_display").asDouble, 0.0)
            assertEquals("Bearer jwt", authHeaders[1])
            assertEquals("Bearer jwt", authHeaders[2])
        } finally { server.stop(0) }
    }
    @Test fun preservesReceiptQuantitiesThroughDraftAndUiLabel() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val payload = """{"items":[
            {"name":"Cá lăng","ingredient_id":"bf973d79-277f-44ed-86b2-b296022f77a5","qty_display":"0.732","unit_display":"kg","purchased_at":"2026-10-12","expiry_date":"2026-10-12"},
            {"name":"Bún tươi","ingredient_id":"80c78714-4039-4ac2-8a74-2f71ee6bc041","qty_display":"1","unit_display":"gói","purchased_at":"2026-10-12","expiry_date":"2026-10-12"}
        ]}"""
        var submitted = ""
        server.createContext("/") { exchange ->
            val input = exchange.requestBody.bufferedReader().readText()
            val response = if (exchange.requestURI.path == "/scans") {
                submitted = input
                """{"scanId":"scan-1","items":[{"id":"fish","name":"Cá lăng"},{"id":"noodles","name":"Bún tươi"}]}"""
            } else payload
            val bytes = response.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val url = "http://127.0.0.1:${server.address.port}"
            val repository = ScanRepository(url, url)
            val recognized = repository.recognize(ScanMode.RECEIPT, "image/jpeg") { "receipt".byteInputStream() }
            val draft = repository.createDraft("token", "fridge", recognized)
            assertEquals(listOf("0.732 kg", "1 gói"), draft.items.map { it.quantityLabel })
            val sent = JsonParser.parseString(submitted).asJsonObject.getAsJsonArray("items")
            assertEquals(0.732, sent[0].asJsonObject.get("qty_display").asDouble, 0.0)
            assertEquals(1.0, sent[1].asJsonObject.get("qty_display").asDouble, 0.0)
        } finally { server.stop(0) }
    }
    @Test fun assignsScanItemLocationsThenCommitsWithoutBody() = runBlocking {
        val requests = mutableListOf<Triple<String, String, String>>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests.add(Triple(exchange.requestMethod, exchange.requestURI.path, exchange.requestBody.bufferedReader().readText()))
            val response = if (exchange.requestURI.path.endsWith("locations")) """{"updated":2}""" else """{"inserted":2,"already":false}"""
            val bytes = response.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val repo = ScanRepository("http://127.0.0.1:${server.address.port}", "")
            repo.assignLocations("jwt", "scan-1", mapOf("item-a" to "freezer", "item-b" to "fridge"))
            repo.commit("jwt", "scan-1")
            assertEquals("PUT", requests[0].first)
            assertEquals("/scans/scan-1/locations", requests[0].second)
            val items = JsonParser.parseString(requests[0].third).asJsonObject.getAsJsonArray("items")
            assertEquals("item-a", items[0].asJsonObject.get("id").asString)
            assertEquals("freezer", items[0].asJsonObject.get("location").asString)
            assertEquals("fridge", items[1].asJsonObject.get("location").asString)
            assertEquals(Triple("POST", "/scans/scan-1/commit", ""), requests[1])
        } finally { server.stop(0) }
    }
    @Test fun rejectsUnsupportedImagesBeforeOpeningStream() = runBlocking {
        val result = runCatching { ScanRepository("", "").recognize(ScanMode.RECEIPT, "image/webp") { error("Must not open") } }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("JPG"))
    }
}
