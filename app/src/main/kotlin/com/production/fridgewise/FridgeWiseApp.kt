package com.production.fridgewise

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.production.fridgewise.data.remote.AuthInterceptor
import com.production.fridgewise.data.repository.AuthRepository
import com.production.fridgewise.data.repository.AuthState
import com.production.fridgewise.navigation.BottomNavBar
import com.production.fridgewise.navigation.NavGraph
import com.production.fridgewise.navigation.NoBottomBarPrefixes
import com.production.fridgewise.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    val authInterceptor: AuthInterceptor
) : ViewModel() {
    val authState = authRepository.authState
    fun onUnauthorized() = authRepository.signOut()
}

@Composable
fun FridgeWiseApp(modifier: Modifier = Modifier, appViewModel: AppViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val authState by appViewModel.authState.collectAsState(
        initial = com.production.fridgewise.data.repository.AuthState.Loading
    )

    LaunchedEffect(Unit) {
        appViewModel.authInterceptor.unauthorizedEvent.collect {
            appViewModel.onUnauthorized()
            navController.navigate(Route.Login.route) { popUpTo(0) }
        }
    }

    val start = when (authState) {
        AuthState.Authenticated -> Route.Inventory.route
        else -> Route.Login.route
    }
    val showBottomBar = currentRoute?.let { r -> NoBottomBarPrefixes.none { r.startsWith(it) } } ?: false

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onTabSelected = {
                        navController.navigate(it.route) {
                            popUpTo(Route.Inventory.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        // ponytail: key(start) remount NavHost khi auth flip Login<->Inventory, rẻ hơn observe+conditional navigate
        androidx.compose.runtime.key(start) {
            NavGraph(
                navController = navController,
                startDestination = start,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
