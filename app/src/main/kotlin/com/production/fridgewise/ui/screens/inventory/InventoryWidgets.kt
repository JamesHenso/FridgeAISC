package com.production.fridgewise.ui.screens.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.production.fridgewise.ui.components.ExpiryStatus
import com.production.fridgewise.ui.components.FoodItemUi
import com.production.fridgewise.ui.theme.MyAppTheme
import java.time.LocalDate

fun badgeLabel(expiry: String?, today: LocalDate = LocalDate.now()): String {
    if (expiry == null) return ""
    return try {
        val e = LocalDate.parse(expiry)
        val days = e.toEpochDay() - today.toEpochDay()
        when {
            days < 0 -> "expired"
            days == 0L -> "today"
            days == 1L -> "tomorrow"
            days <= 7 -> "in $days days"
            else -> expiry
        }
    } catch (_: Exception) { expiry }
}

@Composable
fun badgeColors(status: ExpiryStatus) = when (status) {
    ExpiryStatus.USE_TODAY -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
    ExpiryStatus.USE_SOON -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
    ExpiryStatus.NORMAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun HaveNeedToggle(
    selected: HomeTab,
    onChange: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(Modifier.padding(4.dp)) {
            HomeTab.entries.forEach { tab ->
                val active = tab == selected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = { onChange(tab) }),
                    shape = RoundedCornerShape(16.dp),
                    color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            if (tab == HomeTab.HAVE) "Have" else "Need",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (active) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    number: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(number, style = MaterialTheme.typography.headlineLarge)
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(12.dp).size(32.dp))
            }
        }
    }
}

fun statCards(stats: com.production.fridgewise.data.remote.dto.FridgeStatsResponse?) = listOf(
    Triple((stats?.inStock ?: 0).toString(), "In stock", Icons.Filled.ShoppingBasket),
    Triple((stats?.useToday ?: 0).toString(), "Use today", Icons.Filled.Today),
    Triple((stats?.useSoon ?: 0).toString(), "Use soon", Icons.Filled.Schedule),
    Triple((stats?.cleanOut ?: 0).toString(), "Clean out", Icons.Filled.DeleteOutline)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDropdown(
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(null to "All storages", "fridge" to "Fridge", "freezer" to "Freezer")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        TextField(
            value = options.first { it.first == selected }.second,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onSelect(value); expanded = false }
                )
            }
        }
    }
}

@Composable
fun HomeFoodRow(food: FoodItemUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (bg, fg) = badgeColors(food.expiryStatus)
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        food.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(food.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (food.location == "freezer") "Freezer" else "Fridge",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val label = badgeLabel(food.expiryDate)
            if (label.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(16.dp), color = bg) {
                    Text(label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge, color = fg)
                }
            }
        }
    }
}

@Composable
fun RecipesBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Recipes", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Cook with what you already have",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WidgetsPreview() {
    MyAppTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HaveNeedToggle(HomeTab.HAVE, onChange = {})
            StatCard("1", "In stock", Icons.Filled.ShoppingBasket)
        }
    }
}
