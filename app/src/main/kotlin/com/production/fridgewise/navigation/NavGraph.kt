package com.production.fridgewise.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.production.fridgewise.ui.screens.family.FamilyScreen
import com.production.fridgewise.ui.screens.favorite.FavoriteScreen
import com.production.fridgewise.ui.screens.inventory.InventoryScreen
import com.production.fridgewise.ui.screens.login.LoginScreen
import com.production.fridgewise.ui.screens.recipes.RecipeDetailScreen
import com.production.fridgewise.ui.screens.recipes.RecipeListScreen
import com.production.fridgewise.ui.screens.reels.ReelsScreen
import com.production.fridgewise.ui.screens.scan.AssignStorageScreen
import com.production.fridgewise.ui.screens.scan.ConfirmItemsScreen
import com.production.fridgewise.ui.screens.scan.ScanScreen
import com.production.fridgewise.ui.screens.settings.SettingsScreen
import com.production.fridgewise.ui.screens.setup.SetupFridgeScreen
import com.production.fridgewise.ui.screens.storagelist.StorageListScreen

@Composable
fun StubScreen(label: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label) }
}

@Composable
fun NavGraph(navController: NavHostController, startDestination: String, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(Route.Login.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Route.Inventory.route) { popUpTo(0) }
            })
        }
        composable(Route.SetupFridge.route) {
            SetupFridgeScreen(onSetupComplete = {
                navController.navigate(Route.Inventory.route) { popUpTo(0) }
            })
        }
        composable(Route.Inventory.route) {
            InventoryScreen(
                onNavigateToStorage = { location -> navController.navigate(Route.StorageList.create(location, null)) },
                onNavigateToRecipes = { fridgeId -> navController.navigate(Route.RecipeList.create(fridgeId)) },
                onNavigateToSettings = { navController.navigate(Route.Settings.route) }
            )
        }
        composable(
            Route.StorageList.route,
            arguments = listOf(
                navArgument("location") { type = NavType.StringType },
                navArgument("fridgeId") { type = NavType.StringType; nullable = true }
            )
        ) {
            StorageListScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Route.RecipeList.route,
            arguments = listOf(navArgument("fridgeId") { type = NavType.StringType; nullable = true })
        ) { entry ->
            val fid = entry.arguments?.getString("fridgeId")
            RecipeListScreen(
                onRecipeClick = { id -> navController.navigate(Route.RecipeDetail.create(id, fid)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Route.RecipeDetail.route,
            arguments = listOf(
                navArgument("recipeId") { type = NavType.StringType },
                navArgument("fridgeId") { type = NavType.StringType; nullable = true }
            )
        ) {
            RecipeDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.Reels.route) {
            ReelsScreen(onNavigateToRecipeDetail = { id ->
                navController.navigate(Route.RecipeDetail.create(id, null))
            })
        }
        composable(Route.Favorite.route) {
            FavoriteScreen(onRecipeClick = { id ->
                navController.navigate(Route.RecipeDetail.create(id, null))
            })
        }
        composable(Route.Scan.route) {
            ScanScreen(
                onNextStep = { scanId -> navController.navigate(Route.ConfirmItems.create(scanId)) },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            Route.ConfirmItems.route,
            arguments = listOf(navArgument("scanId") { type = NavType.StringType })
        ) {
            ConfirmItemsScreen(
                onNextStep = { scanId -> navController.navigate(Route.AssignStorage.create(scanId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Route.AssignStorage.route,
            arguments = listOf(navArgument("scanId") { type = NavType.StringType })
        ) {
            AssignStorageScreen(
                onComplete = { navController.navigate(Route.Inventory.route) { popUpTo(0) } },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Route.Family.route) { FamilyScreen() }
        composable(Route.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = { navController.navigate(Route.Login.route) { popUpTo(0) } }
            )
        }
    }
}
