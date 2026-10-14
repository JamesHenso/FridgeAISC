package com.production.fridgewise.data.remote

import com.production.fridgewise.data.remote.dto.BulkUpdateRequest
import com.production.fridgewise.data.remote.dto.CommitResponse
import com.production.fridgewise.data.remote.dto.CreateScanRequest
import com.production.fridgewise.data.remote.dto.CreateScanResponse
import com.production.fridgewise.data.remote.dto.DeletedResponse
import com.production.fridgewise.data.remote.dto.FoodItemResponse
import com.production.fridgewise.data.remote.dto.FridgeResponse
import com.production.fridgewise.data.remote.dto.FridgeStatsResponse
import com.production.fridgewise.data.remote.dto.JoinRequest
import com.production.fridgewise.data.remote.dto.JoinResponse
import com.production.fridgewise.data.remote.dto.LikeRequest
import com.production.fridgewise.data.remote.dto.LikeResponse
import com.production.fridgewise.data.remote.dto.LocationsRequest
import com.production.fridgewise.data.remote.dto.MemberResponse
import com.production.fridgewise.data.remote.dto.RecipeCardResponse
import com.production.fridgewise.data.remote.dto.RecipeDetailResponse
import com.production.fridgewise.data.remote.dto.RenameFridgeRequest
import com.production.fridgewise.data.remote.dto.UnlikeResponse
import com.production.fridgewise.data.remote.dto.UpdateFoodRequest
import com.production.fridgewise.data.remote.dto.UpdatedResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("fridges")
    suspend fun getMyFridges(): List<FridgeResponse>

    @GET("fridges/{id}/foods")
    suspend fun getFoods(
        @Path("id") fridgeId: String,
        @Query("location") location: String? = null,
        @Query("q") query: String? = null
    ): List<FoodItemResponse>

    @GET("fridges/{id}/stats")
    suspend fun getStats(@Path("id") fridgeId: String): FridgeStatsResponse

    @PATCH("fridges/{id}")
    suspend fun renameFridge(@Path("id") fridgeId: String, @Body body: RenameFridgeRequest): UpdatedResponse

    @PATCH("fridges/foods/{id}")
    suspend fun updateFood(@Path("id") foodId: String, @Body body: UpdateFoodRequest): UpdatedResponse

    @DELETE("fridges/foods/{id}")
    suspend fun deleteFood(@Path("id") foodId: String): DeletedResponse

    @GET("fridges/{id}/members")
    suspend fun getMembers(@Path("id") fridgeId: String): List<MemberResponse>

    @POST("fridges/join")
    suspend fun joinFridge(@Body body: JoinRequest): JoinResponse

    @POST("scans")
    suspend fun createScan(@Body body: CreateScanRequest): CreateScanResponse

    @PUT("scans/{id}/items")
    suspend fun updateScanItems(@Path("id") scanId: String, @Body body: BulkUpdateRequest): UpdatedResponse

    @PUT("scans/{id}/locations")
    suspend fun updateScanLocations(@Path("id") scanId: String, @Body body: LocationsRequest): UpdatedResponse

    @POST("scans/{id}/commit")
    suspend fun commitScan(@Path("id") scanId: String): CommitResponse

    @GET("recipes/match")
    suspend fun matchRecipes(
        @Query("fridge_id") fridgeId: String,
        @Query("limit") limit: Int? = null
    ): List<RecipeCardResponse>

    @GET("recipes/search")
    suspend fun searchRecipes(
        @Query("category") category: String? = null,
        @Query("method") method: String? = null,
        @Query("name") name: String? = null,
        @Query("limit") limit: Int? = null
    ): List<RecipeCardResponse>

    @GET("recipes/reels")
    suspend fun getReels(@Query("limit") limit: Int? = null): List<RecipeCardResponse>

    @GET("recipes/{id}")
    suspend fun getRecipeDetail(
        @Path("id") recipeId: String,
        @Query("fridge_id") fridgeId: String? = null
    ): RecipeDetailResponse

    @GET("favorites")
    suspend fun getFavorites(): List<RecipeCardResponse>

    @POST("favorites")
    suspend fun likeRecipe(@Body body: LikeRequest): LikeResponse

    @DELETE("favorites/{recipeId}")
    suspend fun unlikeRecipe(@Path("recipeId") recipeId: String): UnlikeResponse
}
