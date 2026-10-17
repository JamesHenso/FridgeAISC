package com.production.fridgewise.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.production.fridgewise.domain.model.AuthUiState

@Composable
fun SignInScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onRememberChange: (Boolean) -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    onSocialClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(FridgeCream).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
    ) {
        AuthHero()
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sign in to your account", fontSize = 20.sp, color = FridgeText, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(23.dp))
            AuthTextField(state.email, onEmailChange, "Enter your email", email = true)
            Spacer(Modifier.height(12.dp))
            AuthTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = "Enter your password",
                isPassword = true,
                passwordVisible = state.passwordVisible,
                onTogglePassword = onTogglePassword
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RememberRow(state.rememberMe, onRememberChange)
                Text(
                    "Forgot Password?",
                    color = FridgeOrange,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable(onClick = onForgotPassword)
                )
            }
            AuthError(state.errorMessage)
            Spacer(Modifier.height(14.dp))
            PrimaryAuthButton("Sign In", state.isLoading, onSignIn)
            Spacer(Modifier.height(22.dp))
            SocialAuthRow(onSocialClick)
            Spacer(Modifier.height(22.dp))
            AuthFooter(
                leading = "Don't have an account? ",
                action = "Sign Up",
                onClick = onSignUp
            )
        }
    }
}

@Composable
internal fun AuthHero() {
    Column(
        modifier = Modifier.fillMaxWidth().height(205.dp).background(FridgeCream).padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        FridgeWiseLogo()
        Spacer(Modifier.height(16.dp))
        Text(
            "Fresh food. Less waste.",
            color = FridgeGreen,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun AuthFooter(leading: String, action: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(leading, color = FridgeMuted, fontSize = 11.sp)
        Text(
            action,
            color = FridgeOrange,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}
