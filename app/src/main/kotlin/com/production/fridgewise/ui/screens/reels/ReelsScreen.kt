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
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.production.fridgewise.ui.components.EmptyState
import com.production.fridgewise.ui.components.ErrorState
import com.production.fridgewise.ui.components.LoadingState
import com.production.fridgewise.ui.theme.MyAppTheme

/**
 * Plays a YouTube video using android-youtube-player library.
 * Shows thumbnail with play button when inactive; loads player when active and user taps play.
 */
@Composable
private fun ReelVideo(
    videoId: String?,
    imageUrl: String?,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    var started by androidx.compose.runtime.remember(videoId) {
        androidx.compose.runtime.mutableStateOf(false)
    }

    // Show thumbnail + play button when not started or no videoId
    if (!active || videoId.isNullOrEmpty() || !started) {
        Box(modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (active && !videoId.isNullOrEmpty()) {
                androidx.compose.material3.IconButton(
                    onClick = { started = true },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(88.dp)
                ) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f)
                        )
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

    // Active + started → load YouTubePlayerView
    val safeVideoId = videoId // smart-cast assured non-null from guard above
    AndroidView(
        factory = { ctx ->
            YouTubePlayerView(ctx).apply {
                enableAutomaticInitialization = false
                val iFrameOptions = IFramePlayerOptions.Builder(ctx)
                    .controls(1)
                    .rel(0)
                    .build()
                initialize(
                    object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            youTubePlayer.loadVideo(safeVideoId, 0f)
                        }
                    },
                    iFrameOptions
                )
            }
        },
        modifier = modifier.fillMaxSize(),
        onRelease = { view -> view.release() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    onNavigateToRecipeDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReelsViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Scaffold(modifier = modifier) { innerPadding ->
        when {
            ui.isLoading -> LoadingState(Modifier.padding(innerPadding))
            ui.error != null -> ErrorState(
                ui.error ?: "",
                viewModel::load,
                Modifier.padding(innerPadding)
            )
            ui.reels.isEmpty() -> EmptyState(
                "Chưa có video",
                Modifier.padding(innerPadding)
            )
            else -> {
                val pager = rememberPagerState(pageCount = { ui.reels.size })
                VerticalPager(
                    state = pager,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    beyondViewportPageCount = 1
                ) { page ->
                    val reel = ui.reels[page]
                    Box(Modifier.fillMaxSize()) {
                        ReelVideo(
                            reel.videoId,
                            reel.imageUrl,
                            active = page == pager.currentPage
                        )
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                reel.name,
                                style = MaterialTheme.typography.titleLarge
                            )
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
                                OutlinedButton(
                                    onClick = { viewModel.likeReel(reel.id) }
                                ) {
                                    Text(if (reel.isLiked) "Đã thích" else "Thích")
                                }
                                Button(
                                    onClick = {
                                        onNavigateToRecipeDetail(reel.id)
                                    }
                                ) {
                                    Text("Nấu ngay")
                                }
                            }
                            if (reel.videoId != null) {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        context.startActivity(
                                            android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(
                                                    "https://www.youtube.com/watch?v=${reel.videoId}"
                                                )
                                            )
                                        )
                                    }
                                ) {
                                    Text("Mở trong YouTube")
                                }
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
