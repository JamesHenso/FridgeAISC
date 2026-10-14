package com.production.fridgewise.navigation

sealed class Route(val route: String) {
    // Bottom tabs (§12 order)
    data object Reels : Route("reels")
    data object Favorite : Route("favorite")
    data object Scan : Route("scan")
    data object Family : Route("family")
    data object Inventory : Route("inventory")
    // Auth / setup (no bottom bar)
    data object Login : Route("login")
    data object SetupFridge : Route("setup_fridge")
    data object Settings : Route("settings")
    data object RecipeList : Route("recipes?fridgeId={fridgeId}") {
        fun create(fridgeId: String) = "recipes?fridgeId=$fridgeId"
    }
    data object RecipeDetail : Route("recipe/{recipeId}?fridgeId={fridgeId}") {
        fun create(recipeId: String, fridgeId: String?) =
            if (fridgeId == null) "recipe/$recipeId" else "recipe/$recipeId?fridgeId=$fridgeId"
    }
    data object StorageList : Route("storage/{location}?fridgeId={fridgeId}") {
        fun create(location: String, fridgeId: String?) =
            if (fridgeId == null) "storage/$location" else "storage/$location?fridgeId=$fridgeId"
    }
    data object ConfirmItems : Route("scan/{scanId}/confirm") {
        fun create(scanId: String) = "scan/$scanId/confirm"
    }
    data object AssignStorage : Route("scan/{scanId}/assign") {
        fun create(scanId: String) = "scan/$scanId/assign"
    }
}

val BottomTabRoutes = listOf(Route.Reels, Route.Favorite, Route.Scan, Route.Family, Route.Inventory)
val NoBottomBarPrefixes = listOf("login", "setup_fridge", "scan/", "recipe/", "recipes", "storage/", "settings")
