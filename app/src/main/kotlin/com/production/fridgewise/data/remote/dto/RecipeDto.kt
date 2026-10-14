package com.production.fridgewise.data.remote.dto

data class RecipeCardResponse(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val category: String?,
    val method: String?,
    val like_count: Int?,
    val video_id: String?,
    val video_provider: String?,
    val servings: Int?,
    val time: Int?,
    val difficulty: String?
)

data class IngredientResponse(
    val ingredientId: String?,
    val name: String,
    val quantity: String?,
    val unit: String?,
    val qty_base: Double?,
    val base_unit: String?
)

data class RecipeDetailInfo(
    val servings: Int?,
    val time: Int?,
    val difficulty: String?,
    val videoUrl: String?,
    val preparation: String?,
    val instructions: String?,
    val servingText: String?
)

data class HaveNeedResponse(
    val matched: List<IngredientResponse>,
    val missing: List<IngredientResponse>
)

data class RecipeDetailResponse(
    val info: RecipeCardResponse,
    val ingredients: List<IngredientResponse>,
    val detail: RecipeDetailInfo?,
    val haveNeed: HaveNeedResponse?
)
