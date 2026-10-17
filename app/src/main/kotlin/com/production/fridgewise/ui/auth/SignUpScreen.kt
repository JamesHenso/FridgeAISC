package com.production.fridgewise.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
fun SignUpScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onToggleConfirmPassword: () -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onSignUp: () -> Unit,
    onSignIn: () -> Unit,
    onSocialClick: (String) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(FridgeCream).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
    ) {
        AuthHero()
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 21.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create an account", fontSize = 20.sp, color = FridgeText, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(18.dp))
            AuthTextField(state.email, onEmailChange, "Enter your email", email = true)
            Spacer(Modifier.height(11.dp))
            AuthTextField(
                state.password, onPasswordChange, "Password",
                isPassword = true, passwordVisible = state.passwordVisible,
                onTogglePassword = onTogglePassword
            )
            Spacer(Modifier.height(11.dp))
            AuthTextField(
                state.confirmPassword, onConfirmPasswordChange, "Confirm password",
                isPassword = true, passwordVisible = state.confirmPasswordVisible,
                onTogglePassword = onToggleConfirmPassword
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = state.agreedToTerms,
                    onCheckedChange = onTermsChange,
                    colors = CheckboxDefaults.colors(checkedColor = FridgeGreen)
                )
                FlowRow(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
                    Text("I agree to the ", fontSize = 10.sp, color = FridgeText)
                    Text(
                        "Terms & Conditions",
                        fontSize = 10.sp,
                        color = FridgeOrange,
                        modifier = Modifier.clickable(onClick = onTermsClick)
                    )
                    Text(" and ", fontSize = 10.sp, color = FridgeText)
                    Text(
                        "Privacy Policy",
                        fontSize = 10.sp,
                        color = FridgeOrange,
                        modifier = Modifier.clickable(onClick = onPrivacyClick)
                    )
                }
            }
            AuthError(state.errorMessage)
            Spacer(Modifier.height(12.dp))
            PrimaryAuthButton("Sign Up", state.isLoading, onSignUp)
            Spacer(Modifier.height(19.dp))
            SocialAuthRow(onSocialClick)
            Spacer(Modifier.height(18.dp))
            AuthFooter(
                leading = "Already have an account? ",
                action = "Sign In",
                onClick = onSignIn
            )
        }
    }
}
