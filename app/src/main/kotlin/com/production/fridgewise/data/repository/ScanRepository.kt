package com.production.fridgewise.data.repository

import com.google.gson.*
import com.production.fridgewise.BuildConfig
import com.production.fridgewise.domain.model.ReceiptDraft
import com.production.fridgewise.domain.model.ReceiptItem
import java.io.InputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class ScanRepository(
    private val backendUrl: String = BuildConfig.API_BASE_URL,
    private val ocrUrl: String = BuildConfig.OCR_BASE_URL
) {
    suspend fun recognize(mimeType: String, openImage: () -> InputStream): List<ReceiptItem> = withContext(Dispatchers.IO) {
        require(mimeType in setOf("image/jpeg", "image/png")) { "Vui lòng chọn ảnh JPG hoặc PNG." }
        require(ocrUrl.isNotBlank()) { "Thiếu ocr.baseUrl trong local.properties." }
        val boundary = "FridgeWise-${UUID.randomUUID()}"
        val connection = connect(ocrUrl, "api/receipts/scan", "POST")
        try {
            connection.readTimeout = 120_000
            connection.doOutput = true
            connection.setChunkedStreamingMode(8192)
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.outputStream.use { output ->
                val extension = if (mimeType == "image/png") "png" else "jpg"
                output.write(("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"receipt.$extension\"\r\n" +
                    "Content-Type: $mimeType\r\n\r\n").toByteArray())
                openImage().use { it.copyTo(output) }
                output.write("\r\n--$boundary--\r\n".toByteArray())
            }
            val result = response(connection).asJsonObject.getAsJsonArray("items")
                ?: throw IOException("OCR trả về dữ liệu không hợp lệ.")
            currentCoroutineContext().ensureActive()
            result.map {
                val row = it.asJsonObject
                val name = row.string("name").orEmpty()
                val ingredient = row.string("ingredient_id").orEmpty()
                require(name.isNotBlank() && ingredient.isNotBlank()) { "OCR chưa nhận diện được nguyên liệu. Vui lòng chụp lại hóa đơn." }
                val quantity = row.string("qty_display")?.replace(',', '.')?.toDoubleOrNull()?.takeIf { n -> n.isFinite() && n >= 0 }
                ReceiptItem(name, ingredient, quantity,
                    row.string("unit_display"), row.string("purchased_at"), row.string("expiry_date"))
            }
        } finally { connection.disconnect() }
    }

    suspend fun createDraft(token: String, fridgeId: String, items: List<ReceiptItem>): ReceiptDraft {
        require(fridgeId.isNotBlank()) { "Vui lòng chọn tủ lạnh trước khi quét hóa đơn." }
        require(items.isNotEmpty()) { "Không có thực phẩm để tạo bản nháp." }
        val body = JsonObject().apply {
            addProperty("fridge_id", fridgeId)
            add("items", JsonArray().apply { items.forEach { add(it.json(includeIdentity = true)) } })
        }
        val result = request(token, "scans", "POST", body).asJsonObject
        val saved = result.getAsJsonArray("items").map { it.asJsonObject }.groupBy { it.get("name").asString }
            .mapValues { it.value.toMutableList() }
        val mapped = items.map { item ->
            val row = saved[item.name]?.takeIf { it.isNotEmpty() }?.removeAt(0)
                ?: throw IOException("Không nhận được mã thực phẩm trong bản nháp.")
            item.copy(id = row.get("id").asString)
        }
        return ReceiptDraft(result.get("scanId").asString, mapped)
    }

    suspend fun updateItem(token: String, scanId: String, item: ReceiptItem) {
        require(!item.id.isNullOrBlank()) { "Thực phẩm chưa có trong bản nháp." }
        request(token, "scans/${encode(scanId)}/items", "PUT", JsonObject().apply {
            add("items", JsonArray().apply { add(item.json(includeIdentity = false)) })
        })
    }

    suspend fun assignLocations(token: String, scanId: String, locations: Map<String, String>) {
        require(locations.isNotEmpty() && locations.all { (id, location) -> id.isNotBlank() && location in setOf("freezer", "fridge") })
        val result = request(token, "scans/${encode(scanId)}/locations", "PUT", JsonObject().apply {
            add("items", JsonArray().apply {
                locations.forEach { (id, location) -> add(JsonObject().apply {
                    addProperty("id", id)
                    addProperty("location", location)
                }) }
            })
        }).asJsonObject
        check(result.get("updated")?.asInt == locations.size) { "Chưa lưu đủ vị trí thực phẩm. Vui lòng thử lại." }
    }

    suspend fun commit(token: String, scanId: String) {
        val result = request(token, "scans/${encode(scanId)}/commit", "POST", null).asJsonObject
        check(result.has("inserted") && result.has("already")) { "Chưa xác nhận được kết quả thêm vào Inventory. Vui lòng thử lại." }
    }

    private suspend fun request(token: String, path: String, method: String, body: JsonObject?) = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "Vui lòng đăng nhập lại." }
        require(backendUrl.isNotBlank()) { "Thiếu api.baseUrl trong local.properties." }
        val connection = connect(backendUrl, path, method)
        try {
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            response(connection)
        } finally { connection.disconnect() }
    }

    private fun connect(base: String, path: String, method: String) = (URL("${base.trimEnd('/')}/$path").openConnection() as HttpURLConnection).apply {
        requestMethod = method
        instanceFollowRedirects = false
        connectTimeout = 15_000
        readTimeout = 30_000
        setRequestProperty("Accept", "application/json")
    }
    private fun response(connection: HttpURLConnection): JsonElement {
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (status !in 200..299) {
            val message = runCatching { JsonParser.parseString(text).asJsonObject.let { it.string("error") ?: it.string("detail") } }.getOrNull()
            throw IOException(if (status == 401) "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại." else message ?: "Không xử lý được hóa đơn (HTTP $status).")
        }
        return JsonParser.parseString(text)
    }
    private fun ReceiptItem.json(includeIdentity: Boolean) = JsonObject().apply {
        if (includeIdentity) { addProperty("name", name); addProperty("ingredient_id", ingredientId) }
        else addProperty("id", id)
        addProperty("qty_display", quantity)
        addProperty("unit_display", unit)
        addProperty("purchased_at", purchasedAt)
        addProperty("expiry_date", expiryDate)
    }
    private fun JsonObject.string(key: String) = get(key)?.takeUnless { it.isJsonNull }?.asString
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
}
