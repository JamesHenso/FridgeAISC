package com.production.fridgewise.ui.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.production.fridgewise.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    illustrationResId: Int? = null
) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.82f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "splash-scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(850),
        label = "splash-alpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(2100.milliseconds)
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(FridgeCream),
        contentAlignment = Alignment.Center
    ) {
        Image(
                painter = painterResource(R.drawable.auth_grocery_illustration),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-67).dp, y = (-67).dp)
                    .graphicsLayer { rotationZ = 18f }
            )

        Image(
            painter = painterResource(R.drawable.auth_grocery_illustration),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 67.dp, y = 67.dp)
                .graphicsLayer { rotationZ = -18f }
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale).alpha(alpha)
        ) {
            if (illustrationResId != null) {
                Image(
                    painter = painterResource(illustrationResId),
                    contentDescription = "Minh họa thực phẩm FridgeWise",
                    modifier = Modifier.size(220.dp).padding(bottom = 18.dp)
                )
            }
            FridgeWiseLogo()
        }
    }
}
