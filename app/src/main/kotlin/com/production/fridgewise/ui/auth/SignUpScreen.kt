package com.production.fridgewise.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.production.fridgewise.domain.model.AuthUiState

@Composable
fun SignUpScreen(
    state: AuthUiState,
    onNameChange: (String) -> Unit,
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
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        AuthHero()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = colors.surfaceContainerLowest,
                border = BorderStroke(1.dp, colors.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Create an account",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    AuthTextField(state.name, onNameChange, "Fridge name", name = true)
                    AuthTextField(state.email, onEmailChange, "Enter your email", email = true)
                    AuthTextField(
                        state.password, onPasswordChange, "Password",
                        isPassword = true, passwordVisible = state.passwordVisible,
                        onTogglePassword = onTogglePassword
                    )
                    AuthTextField(
                        state.confirmPassword, onConfirmPasswordChange, "Confirm password",
                        isPassword = true, passwordVisible = state.confirmPasswordVisible,
                        onTogglePassword = onToggleConfirmPassword
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.agreedToTerms,
                            onCheckedChange = onTermsChange,
                            colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                        )
                        FlowRow(modifier = Modifier.weight(1f)) {
                            Text(
                                "I agree to the ",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                            Text(
                                "Terms & Conditions",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable(onClick = onTermsClick)
                                    .padding(vertical = 8.dp)
                            )
                            Text(
                                " and ",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                            Text(
                                "Privacy Policy",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable(onClick = onPrivacyClick)
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                    AuthError(state.errorMessage)
                    PrimaryAuthButton("Sign Up", state.isLoading, onSignUp)
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = colors.surfaceContainerLow,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SocialAuthRow(onSocialClick)
                    AuthFooter(
                        leading = "Already have an account? ",
                        action = "Sign In",
                        onClick = onSignIn
                    )
                }
            }
        }
    }
}
