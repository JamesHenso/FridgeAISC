package com.production.fridgewise.data.repository

import com.production.fridgewise.data.remote.ApiService
import com.production.fridgewise.data.remote.dto.FoodItemResponse
import com.production.fridgewise.data.remote.dto.FridgeResponse
import com.production.fridgewise.data.remote.dto.FridgeStatsResponse
import com.production.fridgewise.data.remote.dto.JoinResponse
import com.production.fridgewise.data.remote.dto.MemberResponse
import com.production.fridgewise.data.remote.dto.RenameFridgeRequest
import com.production.fridgewise.data.remote.dto.UpdateFoodRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FridgeRepository @Inject constructor(private val api: ApiService) {
    suspend fun getMyFridges(): Result<List<FridgeResponse>> = runCatching { api.getMyFridges() }
    suspend fun getFoods(fridgeId: String, location: String? = null, query: String? = null): Result<List<FoodItemResponse>> =
        runCatching { api.getFoods(fridgeId, location, query) }
    suspend fun getStats(fridgeId: String): Result<FridgeStatsResponse> = runCatching { api.getStats(fridgeId) }
    suspend fun renameFridge(fridgeId: String, name: String): Result<Unit> = runCatching {
        api.renameFridge(fridgeId, RenameFridgeRequest(name)); Unit
    }
    suspend fun updateFood(foodId: String, request: UpdateFoodRequest): Result<Unit> = runCatching {
        api.updateFood(foodId, request); Unit
    }
    suspend fun deleteFood(foodId: String): Result<Unit> = runCatching { api.deleteFood(foodId); Unit }
    suspend fun getMembers(fridgeId: String): Result<List<MemberResponse>> = runCatching { api.getMembers(fridgeId) }
    suspend fun joinFridge(code: String): Result<JoinResponse> = runCatching {
        api.joinFridge(com.production.fridgewise.data.remote.dto.JoinRequest(code))
    }
}
