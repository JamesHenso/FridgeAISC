package com.production.fridgewise.ui.screens.reels

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.production.fridgewise.ui.components.EmptyState
import com.production.fridgewise.ui.components.ErrorState
import com.production.fridgewise.ui.components.LoadingState
import com.production.fridgewise.ui.theme.MyAppTheme

// ponytail: video_id là YouTube ID → WebView IFrame embed, khỏi thêm player dependency
private fun embedHtml(videoId: String) = """
<html><body style="margin:0;background:black">
<iframe width="100%" height="100%" src="https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&rel=0"
frameborder="0" allow="autoplay; encrypted-media" allowfullscreen></iframe>
</body></html>
""".trimIndent()

@Composable
private fun ReelVideo(videoId: String?, imageUrl: String?, active: Boolean, modifier: Modifier = Modifier) {
    var failed by androidx.compose.runtime.remember(videoId) { androidx.compose.runtime.mutableStateOf(false) }
    var started by androidx.compose.runtime.remember(videoId) { androidx.compose.runtime.mutableStateOf(false) }
    // ponytail: autoplay kèm tiếng bị WebView chặn → thumbnail + play, tap mới nạp player; rời trang thì hủy
    if (!active || videoId.isNullOrEmpty() || failed || !started) {
        Box(modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            if (active && !videoId.isNullOrEmpty() && !failed) {
                androidx.compose.material3.IconButton(
                    onClick = { started = true },
                    modifier = Modifier.align(Alignment.Center).size(88.dp)
                ) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        drawCircle(color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
                        val w = size.width
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w * 0.38f, w * 0.28f)
                                lineTo(w * 0.70f, w * 0.50f)
                                lineTo(w * 0.38f, w * 0.72f)
                                close()
                            },
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
        return
    }
    val context = LocalContext.current
    val webView = remember(videoId) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) failed = true
                }
            }
            webChromeClient = android.webkit.WebChromeClient()
            loadDataWithBaseURL("https://www.youtube.com", embedHtml(videoId), "text/html", "utf-8", null)
        }
    }
    DisposableEffect(videoId) { onDispose { webView.destroy() } }
    AndroidView(factory = { webView }, modifier = modifier.fillMaxSize())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    onNavigateToRecipeDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReelsViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Scaffold(modifier = modifier) { innerPadding ->
        when {
            ui.isLoading -> LoadingState(Modifier.padding(innerPadding))
            ui.error != null -> ErrorState(ui.error ?: "", viewModel::load, Modifier.padding(innerPadding))
            ui.reels.isEmpty() -> EmptyState("Chưa có video", Modifier.padding(innerPadding))
            else -> {
                val pager = rememberPagerState(pageCount = { ui.reels.size })
                VerticalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    beyondViewportPageCount = 1
                ) { page ->
                    val reel = ui.reels[page]
                    Box(Modifier.fillMaxSize()) {
                        ReelVideo(reel.videoId, reel.imageUrl, active = page == pager.currentPage)
                        Column(
                            Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(reel.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                listOfNotNull(
                                    reel.servings?.let { "Khẩu phần $it" },
                                    reel.time?.let { "${it}p" },
                                    reel.difficulty,
                                    reel.likeCount?.let { "♥ $it" }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.likeReel(reel.id) }) {
                                    Text(if (reel.isLiked) "Đã thích" else "Thích")
                                }
                                Button(onClick = { onNavigateToRecipeDetail(reel.id) }) { Text("Nấu ngay") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReelsPreview() {
    MyAppTheme { EmptyState("Chưa có video") }
}
