package com.production.fridgewise.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.production.fridgewise.ui.theme.MyAppTheme

data class FoodItemUi(
    val id: String,
    val name: String,
    val expiryDate: String?,
    val expiryStatus: ExpiryStatus,
    val qtyDisplay: Double?,
    val unitDisplay: String?,
    val location: String? = null
)

@Composable
fun FoodItemCard(food: FoodItemUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    listOfNotNull(
                        food.qtyDisplay?.let { "$it ${food.unitDisplay.orEmpty()}".trim() },
                        food.expiryDate?.let { "HSD $it" }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            ExpiryBadge(food.expiryStatus, food.expiryDate ?: "")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FoodItemCardPreview() {
    MyAppTheme {
        FoodItemCard(
            FoodItemUi("1", "Trứng", "2026-10-15", ExpiryStatus.USE_SOON, 10.0, "quả"),
            onClick = {}
        )
    }
}
