package com.production.fridgewise.ui.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.saveable.rememberSaveable
import com.production.fridgewise.ui.inventory.InventoryRoute
import com.production.fridgewise.navigation.AuthDestination

@Composable
fun AuthNavigation(
    illustrationResId: Int? = null,
    authViewModel: AuthViewModel = viewModel()
) {
    val state by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var destination by rememberSaveable { mutableStateOf(AuthDestination.SPLASH) }
    var direction by remember { mutableStateOf(1) }

    fun navigate(target: AuthDestination) {
        direction = if (target == AuthDestination.SIGN_UP) 1 else -1
        destination = target
        authViewModel.clearError()
    }

    MaterialTheme {
        AnimatedContent(
            targetState = destination,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (slideInHorizontally(
                    animationSpec = tween(420),
                    initialOffsetX = { fullWidth -> if (direction > 0) fullWidth / 3 else -fullWidth / 3 }
                ) + fadeIn(animationSpec = tween(320)))
                    .togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(360),
                            targetOffsetX = { fullWidth -> if (direction > 0) -fullWidth / 4 else fullWidth / 4 }
                        ) + fadeOut(animationSpec = tween(260))
                    )
            },
            label = "auth-screen-transition"
        ) { screen ->
            when (screen) {
                AuthDestination.INVENTORY -> {
                    val token = authViewModel.accessToken
                    if (token != null) {
                        InventoryRoute(token, onSignIn = { navigate(AuthDestination.SIGN_IN) })
                    } else {
                        androidx.compose.runtime.LaunchedEffect(Unit) { navigate(AuthDestination.SIGN_IN) }
                    }
                }
                AuthDestination.SPLASH -> SplashScreen(
                    onFinished = { navigate(AuthDestination.SIGN_IN) },
                    illustrationResId = illustrationResId
                )
                AuthDestination.SIGN_IN -> SignInScreen(
                    state = state,
                    onEmailChange = authViewModel::updateEmail,
                    onPasswordChange = authViewModel::updatePassword,
                    onTogglePassword = authViewModel::togglePasswordVisibility,
                    onRememberChange = authViewModel::toggleRememberMe,
                    onSignIn = {
                        authViewModel.signIn {
                            navigate(AuthDestination.INVENTORY)
                        }
                    },
                    onSignUp = { navigate(AuthDestination.SIGN_UP) },
                    onForgotPassword = {
                        Toast.makeText(context, "Hãy kết nối chức năng khôi phục mật khẩu với Auth.", Toast.LENGTH_LONG).show()
                    },
                    onSocialClick = {
                        Toast.makeText(context, "$it chưa được cấu hình.", Toast.LENGTH_SHORT).show()
                    }
                )
                AuthDestination.SIGN_UP -> SignUpScreen(
                    state = state,
                    onEmailChange = authViewModel::updateEmail,
                    onPasswordChange = authViewModel::updatePassword,
                    onConfirmPasswordChange = authViewModel::updateConfirmPassword,
                    onTogglePassword = authViewModel::togglePasswordVisibility,
                    onToggleConfirmPassword = authViewModel::toggleConfirmPasswordVisibility,
                    onTermsChange = authViewModel::toggleTerms,
                    onSignUp = {
                        authViewModel.signUp {
                            Toast.makeText(context, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSignIn = { navigate(AuthDestination.SIGN_IN) },
                    onSocialClick = {
                        Toast.makeText(context, "$it chưa được cấu hình.", Toast.LENGTH_SHORT).show()
                    },
                    onTermsClick = {
                        Toast.makeText(context, "Thêm màn hình điều khoản tại đây.", Toast.LENGTH_SHORT).show()
                    },
                    onPrivacyClick = {
                        Toast.makeText(context, "Thêm màn hình chính sách bảo mật tại đây.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
