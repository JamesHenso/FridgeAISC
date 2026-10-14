package com.production.fridgewise.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.production.fridgewise.ui.theme.MyAppTheme

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LoginContent(
        isLoading = ui.isLoading, error = ui.error,
        onEmailLogin = { e, p -> viewModel.signInWithEmail(e, p) },
        modifier = modifier
    )
}

@Composable
fun LoginContent(
    isLoading: Boolean,
    error: String?,
    onEmailLogin: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("FridgeWise", style = MaterialTheme.typography.headlineMedium)
            if (error != null) Text(error, style = MaterialTheme.typography.bodySmall)
            TextField(value = email, onValueChange = { email = it }, placeholder = { Text("Email") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            TextField(value = password, onValueChange = { password = it }, placeholder = { Text("Mật khẩu") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { onEmailLogin(email, password) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
                Text("Đăng nhập")
            }
            OutlinedButton(onClick = {}, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
                Text("Continue with Google")
            }
            TextButton(onClick = {}) { Text("Quên mật khẩu?") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    MyAppTheme { LoginContent(false, null, onEmailLogin = { _, _ -> }) }
}
