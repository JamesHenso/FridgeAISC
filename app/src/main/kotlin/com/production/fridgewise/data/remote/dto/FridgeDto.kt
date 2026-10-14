package com.production.fridgewise.data.remote.dto

data class FridgeResponse(
    val id: String,
    val name: String,
    val invite_code: String?,
    val role: String
)

data class FoodItemResponse(
    val id: String,
    val name: String,
    val ingredient_id: String?,
    val qty_display: Double?,
    val unit_display: String?,
    val qty_base: Double?,
    val base_unit: String?,
    val purchased_at: String?,
    val expiry_date: String?,
    val location: String?
)

data class FridgeStatsResponse(
    val inStock: Int,
    val useToday: Int,
    val useSoon: Int,
    val cleanOut: Int
)

data class MemberResponse(
    val user_id: String,
    val role: String,
    val name: String?,
    val email: String?
)

data class JoinRequest(val code: String)

data class JoinResponse(
    val fridgeId: String,
    val already: Boolean
)

data class RenameFridgeRequest(val name: String)

data class UpdateFoodRequest(
    val name: String? = null,
    val qty_display: Double? = null,
    val unit_display: String? = null,
    val expiry_date: String? = null,
    val location: String? = null
)

data class UpdatedResponse(val updated: Int)

data class DeletedResponse(val deleted: Int)
