package com.production.fridgewise.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.production.fridgewise.ui.theme.MyAppTheme

enum class ExpiryStatus { USE_TODAY, USE_SOON, NORMAL }

@Composable
fun ExpiryBadge(status: ExpiryStatus, label: String, modifier: Modifier = Modifier) {
    val colors = when (status) {
        ExpiryStatus.USE_TODAY -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.error,
            labelColor = MaterialTheme.colorScheme.onError
        )
        ExpiryStatus.USE_SOON -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            labelColor = MaterialTheme.colorScheme.onTertiary
        )
        ExpiryStatus.NORMAL -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = colors,
        modifier = modifier.padding(0.dp)
    )
}

fun expiryStatusOf(expiryDate: String?, today: String = ""): ExpiryStatus {
    if (expiryDate == null) return ExpiryStatus.NORMAL
    if (today.isEmpty() || expiryDate <= today) return ExpiryStatus.USE_TODAY
    return ExpiryStatus.NORMAL
}

@Preview(showBackground = true)
@Composable
private fun ExpiryBadgePreview() {
    MyAppTheme {
        androidx.compose.foundation.layout.Column {
            ExpiryBadge(ExpiryStatus.USE_TODAY, "Use today")
            ExpiryBadge(ExpiryStatus.USE_SOON, "Use soon")
            ExpiryBadge(ExpiryStatus.NORMAL, "HSD 20/10")
        }
    }
}
