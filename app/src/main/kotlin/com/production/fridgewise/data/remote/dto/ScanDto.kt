package com.production.fridgewise.data.remote.dto

data class ScanItemRequest(
    val name: String,
    val ingredient_id: String?,
    val qty_display: Double?,
    val unit_display: String?,
    val purchased_at: String?,
    val expiry_date: String?
)

data class CreateScanRequest(
    val fridge_id: String,
    val items: List<ScanItemRequest>
)

data class ScanItemId(val id: String, val name: String)

data class CreateScanResponse(
    val scanId: String,
    val inserted: Int,
    val items: List<ScanItemId>
)

data class UpdateScanItemRequest(
    val id: String,
    val qty_display: Double? = null,
    val unit_display: String? = null,
    val purchased_at: String? = null,
    val expiry_date: String? = null
)

data class BulkUpdateRequest(val items: List<UpdateScanItemRequest>)

data class LocationAssignment(val id: String, val location: String)

data class LocationsRequest(val items: List<LocationAssignment>)

data class CommitResponse(val inserted: Int, val already: Boolean)
