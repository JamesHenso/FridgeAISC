package com.production.fridgewise.data.repository

import com.production.fridgewise.data.remote.ApiService
import com.production.fridgewise.data.remote.dto.BulkUpdateRequest
import com.production.fridgewise.data.remote.dto.CommitResponse
import com.production.fridgewise.data.remote.dto.CreateScanRequest
import com.production.fridgewise.data.remote.dto.CreateScanResponse
import com.production.fridgewise.data.remote.dto.LocationAssignment
import com.production.fridgewise.data.remote.dto.LocationsRequest
import com.production.fridgewise.data.remote.dto.ScanItemRequest
import com.production.fridgewise.data.remote.dto.UpdateScanItemRequest
import com.production.fridgewise.ui.screens.scan.ScannedItemUi
import com.production.fridgewise.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(private val api: ApiService, private val http: OkHttpClient) {
    suspend fun createScan(fridgeId: String, items: List<ScanItemRequest>): Result<CreateScanResponse> =
        runCatching { api.createScan(CreateScanRequest(fridgeId, items)) }
    suspend fun updateItems(scanId: String, items: List<UpdateScanItemRequest>): Result<Unit> =
        runCatching { api.updateScanItems(scanId, BulkUpdateRequest(items)); Unit }
    suspend fun updateLocations(scanId: String, items: List<LocationAssignment>): Result<Unit> =
        runCatching { api.updateScanLocations(scanId, LocationsRequest(items)); Unit }
    suspend fun commitScan(scanId: String): Result<CommitResponse> = runCatching { api.commitScan(scanId) }

    // Ảnh → OCR (:8000) → POST /scans, trả về scanId + items kèm id
    suspend fun scanPhoto(image: ByteArray): Result<Pair<String, List<ScannedItemUi>>> = runCatching {
        val fridgeId = api.getMyFridges().firstOrNull()?.id ?: throw IllegalStateException("Chưa có tủ lạnh")
        val items = scanReceipt(image)
        if (items.isEmpty()) throw IllegalStateException("Không nhận diện được món nào")
        val res = createScan(
            fridgeId,
            items.map {
                ScanItemRequest(it.name, it.ingredientId, it.qtyDisplay, it.unitDisplay, it.purchasedAt, it.expiryDate)
            }
        ).getOrThrow()
        val ids = res.items
        res.scanId to items.mapIndexed { i, item -> item.copy(id = ids.getOrNull(i)?.id) }
    }

    // ponytail: OCR FastAPI user tự chạy (:8000), response đúng shape POST /scans → map thẳng
    suspend fun scanReceipt(image: ByteArray, fileName: String = "receipt.jpg"): List<ScannedItemUi> =
        withContext(Dispatchers.IO) {
            val base = BuildConfig.OCR_BASE_URL.trimEnd('/')
            if (base.isEmpty()) throw IllegalStateException("Thiếu ocr.baseUrl trong local.properties")
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, image.toRequestBody("image/jpeg".toMediaType()))
                .build()
            val req = Request.Builder().url("$base/api/receipts/scan").post(body).build()
            http.newCall(req).execute().use { res ->
                val json = JSONObject(res.body?.string().orEmpty().ifEmpty { "{}" })
                if (!res.isSuccessful) throw IllegalStateException("OCR thất bại (${res.code})")
                val arr = json.optJSONArray("items") ?: return@withContext emptyList()
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    ScannedItemUi(
                        name = o.optString("name"),
                        ingredientId = o.optString("ingredient_id").ifEmpty { null },
                        qtyDisplay = o.optDouble("qty_display").takeIf { !it.isNaN() },
                        unitDisplay = o.optString("unit_display").ifEmpty { null },
                        purchasedAt = o.optString("purchased_at").ifEmpty { null },
                        expiryDate = o.optString("expiry_date").ifEmpty { null }
                    )
                }
            }
        }
}
