package com.production.fridgewise.data.repository

import com.google.gson.*
import com.production.fridgewise.BuildConfig
import com.production.fridgewise.domain.model.ReceiptDraft
import com.production.fridgewise.domain.model.ReceiptItem
import com.production.fridgewise.domain.model.OcrScanResult
import com.production.fridgewise.domain.model.ScanMode
import com.production.fridgewise.domain.model.IngredientCandidate
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
    suspend fun recognize(mode: ScanMode, mimeType: String, openImage: () -> InputStream): OcrScanResult = withContext(Dispatchers.IO) {
        require(mimeType in setOf("image/jpeg", "image/png")) { "Vui lòng chọn ảnh JPG hoặc PNG." }
        require(ocrUrl.isNotBlank()) { "Thiếu ocr.baseUrl trong local.properties." }
        val boundary = "FridgeWise-${UUID.randomUUID()}"
        val connection = connect(ocrUrl, if (mode == ScanMode.RECEIPT) "api/receipts/scan" else "api/products/scan", "POST")
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
            val root = response(connection).asJsonObject
            val result = root.getAsJsonArray("items")
                ?: throw IOException("OCR trả về dữ liệu không hợp lệ.")
            currentCoroutineContext().ensureActive()
            val items = result.map {
                val row = it.asJsonObject
                val name = row.string("name").orEmpty()
                require(name.isNotBlank()) { "OCR chưa nhận diện được tên sản phẩm." }
                val quantity = row.string("qty_display")?.replace(',', '.')?.toDoubleOrNull()?.takeIf { n -> n.isFinite() && n >= 0 }
                ReceiptItem(name, row.string("ingredient_id"), quantity,
                    row.string("unit_display"), row.string("purchased_at"), row.string("expiry_date"),
                    rawName = row.string("raw_name"), unitPriceVnd = row.long("unit_price_vnd"),
                    priceVnd = row.long("price_vnd"), sourceType = row.string("source_type"),
                    matchStatus = row.string("match_status"),
                    candidates = row.getAsJsonArray("candidates")?.mapNotNull { candidate ->
                        val value = candidate.asJsonObject
                        val id = value.string("ingredient_id")
                        val candidateName = value.string("name")
                        if (id != null && candidateName != null) IngredientCandidate(candidateName, id) else null
                    } ?: emptyList())
            }
            OcrScanResult(items, root.getAsJsonArray("excluded_items")?.mapNotNull {
                it.asJsonObject.string("raw_name") } ?: emptyList(), root.string("merchant"),
                root.long("total_vnd"), root.long("subtotal_vnd"), root.long("discount_vnd"),
                root.long("line_total_vnd"), root.long("reconciliation_delta_vnd"), mode)
        } finally { connection.disconnect() }
    }

    suspend fun createDraft(token: String, fridgeId: String, scan: OcrScanResult): ReceiptDraft {
        val items = scan.items
        require(fridgeId.isNotBlank()) { "Vui lòng chọn tủ lạnh trước khi quét hóa đơn." }
        require(items.isNotEmpty()) { "Không có thực phẩm để tạo bản nháp." }
        val body = JsonObject().apply {
            addProperty("fridge_id", fridgeId)
            addProperty("source_type", if (scan.mode == ScanMode.RECEIPT) "receipt" else "product")
            addProperty("merchant", scan.merchant)
            addProperty("receipt_total_vnd", scan.totalVnd)
            addProperty("receipt_subtotal_vnd", scan.subtotalVnd)
            addProperty("receipt_discount_vnd", scan.discountVnd)
            addProperty("receipt_line_total_vnd", scan.lineTotalVnd)
            add("items", JsonArray().apply { items.forEach { add(it.json(includeIdentity = true)) } })
        }
        val result = request(token, "scans", "POST", body).asJsonObject
        val saved = result.getAsJsonArray("items").map { it.asJsonObject }.groupBy { it.get("name").asString }
            .mapValues { it.value.toMutableList() }
        val mapped = items.map { item ->
            val row = saved[item.name]?.takeIf { it.isNotEmpty() }?.removeAt(0)
                ?: throw IOException("Không nhận được mã thực phẩm trong bản nháp.")
            item.copy(id = row.get("id").asString, imageUrl = com.production.fridgewise.data.apiImageUrl(row.string("image_url"), backendUrl))
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
        if (!includeIdentity) addProperty("id", id)
        addProperty("name", name)
        addProperty("ingredient_id", ingredientId)
        addProperty("qty_display", quantity)
        addProperty("unit_display", unit)
        addProperty("purchased_at", purchasedAt)
        addProperty("expiry_date", expiryDate)
        addProperty("raw_name", rawName)
        addProperty("unit_price_vnd", unitPriceVnd)
        addProperty("price_vnd", priceVnd)
        addProperty("source_type", sourceType)
        addProperty("match_status", matchStatus)
    }
    private fun JsonObject.string(key: String) = get(key)?.takeUnless { it.isJsonNull }?.asString
    private fun JsonObject.long(key: String) = get(key)?.takeUnless { it.isJsonNull }?.asLong
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
}
