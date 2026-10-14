package com.production.fridgewise.data.repository

import com.production.fridgewise.data.remote.ApiService
import com.production.fridgewise.data.remote.dto.RecipeCardResponse
import com.production.fridgewise.data.remote.dto.RecipeDetailResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(private val api: ApiService) {
    suspend fun getMatchedRecipes(fridgeId: String, limit: Int = 10): Result<List<RecipeCardResponse>> =
        runCatching { api.matchRecipes(fridgeId, limit) }
    suspend fun searchRecipes(category: String? = null, method: String? = null, name: String? = null, limit: Int? = null): Result<List<RecipeCardResponse>> =
        runCatching { api.searchRecipes(category, method, name, limit) }
    suspend fun getRecipeDetail(recipeId: String, fridgeId: String? = null): Result<RecipeDetailResponse> =
        runCatching { api.getRecipeDetail(recipeId, fridgeId) }
    suspend fun getReels(limit: Int? = null): Result<List<RecipeCardResponse>> =
        runCatching { api.getReels(limit) }
}
