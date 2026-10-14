package com.production.fridgewise.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import com.production.fridgewise.ui.theme.MyAppTheme

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onTabSelected: (Route) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        val items = listOf(
            Route.Reels to Pair(Icons.Default.PlayCircle, "Reels"),
            Route.Favorite to Pair(Icons.Default.Favorite, "Favorite"),
            Route.Scan to Pair(Icons.Default.QrCodeScanner, "Scan"),
            Route.Family to Pair(Icons.Default.Group, "Family"),
            Route.Inventory to Pair(Icons.Default.Inventory, "Inventory")
        )
        items.forEach { (route, icon) ->
            NavigationBarItem(
                selected = currentRoute == route.route,
                onClick = { onTabSelected(route) },
                icon = { Icon(icon.first, contentDescription = icon.second) },
                label = { Text(icon.second) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomNavBarPreview() {
    MyAppTheme { BottomNavBar(currentRoute = "inventory", onTabSelected = {}) }
}
