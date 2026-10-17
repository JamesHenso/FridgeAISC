package com.production.fridgewise.data.repository

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.production.fridgewise.BuildConfig
import com.production.fridgewise.domain.model.Food
import com.production.fridgewise.domain.model.Fridge
import com.production.fridgewise.domain.model.InventoryStats
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InventoryApiException(val statusCode: Int, message: String) : IOException(message)

class InventoryRepository(private val baseUrl: String = BuildConfig.API_BASE_URL) {
    suspend fun fridges(token: String): List<Fridge> = get("fridges", token).asJsonArray.map {
        val row = it.asJsonObject
        Fridge(row.get("id").asString, row.get("name").asString)
    }

    suspend fun foods(token: String, fridgeId: String, location: String? = null, query: String = ""): List<Food> {
        val filters = buildList {
            location?.let { add("location=${encode(it)}") }
            if (query.isNotBlank()) add("q=${encode(query.trim())}")
        }.joinToString("&")
        val path = "fridges/${encode(fridgeId)}/foods" + if (filters.isEmpty()) "" else "?$filters"
        return get(path, token).asJsonArray.map {
            val row = it.asJsonObject
            Food(
                id = row.get("id").asString,
                name = row.get("name").asString,
                quantity = row.get("qty_display")?.takeUnless { value -> value.isJsonNull }?.asDouble,
                unit = row.string("unit_display"), location = row.string("location"),
                purchasedAt = row.string("purchased_at"), expiryDate = row.string("expiry_date")
            )
        }
    }

    suspend fun stats(token: String, fridgeId: String): InventoryStats {
        val row = get("fridges/${encode(fridgeId)}/stats", token).asJsonObject
        return InventoryStats(row.get("inStock").asInt, row.get("useToday").asInt,
            row.get("useSoon").asInt, row.get("cleanOut").asInt)
    }

    private suspend fun get(path: String, token: String): JsonElement = withContext(Dispatchers.IO) {
        require(baseUrl.isNotBlank()) { "Thiếu api.baseUrl trong local.properties." }
        if (token.isBlank()) throw InventoryApiException(401, "Vui lòng đăng nhập lại.")
        val connection = URL("${baseUrl.trimEnd('/')}/$path").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JsonParser.parseString(body).asJsonObject.string("error") }.getOrNull()
                throw InventoryApiException(status, message ?: "Không thể tải Inventory (HTTP $status).")
            }
            JsonParser.parseString(body)
        } finally { connection.disconnect() }
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun JsonObject.string(key: String): String? = get(key)?.takeUnless { it.isJsonNull }?.asString
}
