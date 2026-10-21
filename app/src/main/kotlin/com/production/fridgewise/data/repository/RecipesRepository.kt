package com.production.fridgewise.data.repository

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.production.fridgewise.BuildConfig
import com.production.fridgewise.domain.model.RecipeDetail
import com.production.fridgewise.domain.model.RecipeIngredient
import com.production.fridgewise.domain.model.Recipe
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecipesApiException(val statusCode: Int, message: String) : IOException(message)

class RecipesRepository(private val baseUrl: String = BuildConfig.API_BASE_URL) {
    suspend fun recipes(token: String, fridgeId: String?, query: String, method: String? = null,
        focusIngredientId: String? = null): List<Recipe> {
        val extraQuery = buildList {
            method?.takeIf { it.isNotBlank() }?.let { add("method=${encode(it)}") }
            focusIngredientId?.takeIf { it.isNotBlank() }?.let { add("ingredient_id=${encode(it)}") }
        }.joinToString("&").let { if (it.isEmpty()) "" else "&$it" }
        val path = if (query.isBlank() && fridgeId != null) {
            "recipes/match?fridge_id=${encode(fridgeId)}&limit=5$extraQuery"
        } else "recipes/search?name=${encode(query.trim())}&limit=50$extraQuery"
        return request(token, path).asJsonArray.map { it.asJsonObject.toRecipe() }
    }

    suspend fun cookingMethods(token: String): List<String> = request(token, "recipes/methods")
        .asJsonArray.mapNotNull { it.takeUnless { value -> value.isJsonNull }?.asString?.trim()?.takeIf(String::isNotEmpty) }
        .distinct()

    suspend fun detail(token: String, recipeId: String, fridgeId: String?): RecipeDetail {
        val suffix = fridgeId?.let { "?fridge_id=${encode(it)}" }.orEmpty()
        val root = request(token, "recipes/${encode(recipeId)}$suffix").asJsonObject
        val info = root.getAsJsonObject("info")
        val detail = root.get("detail")?.takeUnless { it.isJsonNull }?.asJsonObject
        val ingredients = root.getAsJsonArray("ingredients")?.map {
            val row = it.asJsonObject
            RecipeIngredient(row.string("ingredientId"), row.string("name") ?: "Nguyên liệu",
                row.string("quantity"), row.string("unit"), com.production.fridgewise.data.apiImageUrl(row.string("image_url"), baseUrl))
        }.orEmpty()
        val haveNeed = root.get("haveNeed")?.takeUnless { it.isJsonNull }?.asJsonObject
        fun matches(key: String): List<RecipeIngredient>? = haveNeed?.getAsJsonArray(key)?.map { value ->
            val row = value.asJsonObject
            val id = row.string("ingredientId")
            ingredients.firstOrNull { id != null && it.id == id }
                ?: RecipeIngredient(id, row.string("name") ?: "Nguyên liệu", null, null)
        }
        val video = detail?.string("videoUrl")?.takeIf { it.isNotBlank() }
            ?: info.string("video_id")?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) &&
                (info.string("video_provider") ?: "youtube").equals("youtube", ignoreCase = true) }
                ?.let { "https://www.youtube.com/watch?v=$it" }
        return RecipeDetail(
            Recipe(info.get("id").asString, info.get("name").asString, info.string("imageUrl"),
                (detail?.string("servings") ?: info.string("servings"))?.toIntOrNull(),
                (detail?.string("time") ?: info.string("time"))?.toIntOrNull(),
                detail?.string("difficulty") ?: info.string("difficulty")),
            video?.takeIf { runCatching { URL(it).protocol in setOf("https", "http") }.getOrDefault(false) },
            ingredients, detail?.string("preparation"), detail?.string("instructions"), detail?.string("servingText"),
            matches("matched"), matches("missing")
        )
    }

    suspend fun favorites(token: String): Set<String> = request(token, "favorites").asJsonArray
        .map { it.asJsonObject.get("id").asString }.toSet()

    suspend fun favoriteRecipes(token: String): List<Recipe> = request(token, "favorites").asJsonArray
        .map { it.asJsonObject.toRecipe() }

    suspend fun setFavorite(token: String, recipeId: String, favorite: Boolean) {
        if (favorite) {
            val body = JsonObject().apply { addProperty("recipe_id", recipeId) }
            request(token, "favorites", "POST", body.toString())
        } else request(token, "favorites/${encode(recipeId)}", "DELETE")
    }

    private suspend fun request(token: String, path: String, method: String = "GET", body: String? = null) = withContext(Dispatchers.IO) {
        require(baseUrl.isNotBlank()) { "Thiếu api.baseUrl trong local.properties." }
        if (token.isBlank()) throw RecipesApiException(401, "Vui lòng đăng nhập lại.")
        val connection = URL("${baseUrl.trimEnd('/')}/$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JsonParser.parseString(response).asJsonObject.string("error") }.getOrNull()
                throw RecipesApiException(status, message ?: "Không thể tải công thức (HTTP $status).")
            }
            if (response.isBlank()) JsonObject() else JsonParser.parseString(response)
        } finally { connection.disconnect() }
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun JsonObject.toRecipe() = Recipe(
        get("id").asString, get("name").asString, string("imageUrl"),
        string("servings")?.toIntOrNull(), string("time")?.toIntOrNull(), string("difficulty")
    )
    private fun JsonObject.string(key: String) = get(key)?.takeUnless { it.isJsonNull }?.asString
}
