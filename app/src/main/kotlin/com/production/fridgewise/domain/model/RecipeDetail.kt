package com.production.fridgewise.domain.model

data class RecipeIngredient(val id: String?, val name: String, val quantity: String?, val unit: String?, val imageUrl: String? = null) {
    val amount: String get() = listOfNotNull(quantity?.takeIf { it.isNotBlank() }, unit?.takeIf { it.isNotBlank() }).joinToString(" ").ifBlank { "Chưa có định lượng" }
}
data class RecipeDetail(
    val recipe: Recipe,
    val videoUrl: String?,
    val ingredients: List<RecipeIngredient>,
    val preparation: String?,
    val instructions: String?,
    val servingText: String?,
    val have: List<RecipeIngredient>?,
    val need: List<RecipeIngredient>?
)
