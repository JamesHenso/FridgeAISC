package com.production.fridgewise.domain.model

data class Recipe(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val servings: Int? = null,
    val minutes: Int? = null,
    val difficulty: String? = null
)

data class RecipesUiState(
    val recipes: List<Recipe> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val updatingFavorites: Set<String> = emptySet(),
    val favoritesReady: Boolean = false,
    val query: String = "",
    val cookingMethods: List<String> = emptyList(),
    val selectedMethod: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val needsSignIn: Boolean = false
)
