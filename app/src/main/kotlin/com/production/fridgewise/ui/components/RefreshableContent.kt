package com.production.fridgewise.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics

/** Shares the Material pull threshold and keeps loaded content visible during a refresh. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableContent(
    loading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (pullRefreshing: Boolean) -> Unit
) {
    var requested by remember { mutableStateOf(false) }
    LaunchedEffect(loading, requested) { if (!loading) requested = false }
    val refresh = {
        if (!loading && !requested) {
            requested = true
            onRefresh()
        }
    }
    PullToRefreshBox(
        isRefreshing = requested && loading,
        onRefresh = refresh,
        modifier = modifier.fillMaxSize().semantics {
            customActions = listOf(CustomAccessibilityAction("Refresh") { refresh(); true })
        }
    ) { content(requested) }
}
