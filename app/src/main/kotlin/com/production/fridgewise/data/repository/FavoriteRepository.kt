package com.production.fridgewise.data.repository

import com.production.fridgewise.data.remote.ApiService
import com.production.fridgewise.data.remote.dto.LikeRequest
import com.production.fridgewise.data.remote.dto.LikeResponse
import com.production.fridgewise.data.remote.dto.RecipeCardResponse
import com.production.fridgewise.data.remote.dto.UnlikeResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(private val api: ApiService) {
    suspend fun getFavorites(): Result<List<RecipeCardResponse>> = runCatching { api.getFavorites() }
    suspend fun likeRecipe(recipeId: String): Result<LikeResponse> =
        runCatching { api.likeRecipe(LikeRequest(recipeId)) }
    suspend fun unlikeRecipe(recipeId: String): Result<UnlikeResponse> =
        runCatching { api.unlikeRecipe(recipeId) }
}
