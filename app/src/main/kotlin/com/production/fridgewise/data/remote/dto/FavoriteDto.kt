package com.production.fridgewise.data.remote.dto

data class LikeRequest(val recipe_id: String)

data class LikeResponse(val liked: Boolean, val already: Boolean)

data class UnlikeResponse(val unliked: Boolean, val removed: Boolean)
