package com.production.fridgewise

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.production.fridgewise.data.OAuthCallback
import com.production.fridgewise.data.OAuthResult
import com.production.fridgewise.ui.auth.AuthNavigation
import com.production.fridgewise.ui.theme.FridgeWiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deliverOAuthResult(intent)
        setContent {
            FridgeWiseTheme {
                AuthNavigation(
                    // After adding the illustration to res/drawable, pass its generated ID:
                    // illustrationResId = R.drawable.auth_grocery_illustration
                )
            }
        }
    }

    // The OAuth browser redirect arrives here because the manifest claims the callback URI.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deliverOAuthResult(intent)
    }

    private fun deliverOAuthResult(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "com.production.fridgewise" || uri.host != "login-callback") return
        OAuthCallback.deliver(OAuthResult(
            code = uri.getQueryParameter("code"),
            error = uri.getQueryParameter("error_description") ?: uri.getQueryParameter("error")
        ))
        // The intent survives configuration changes; drop the code so a rotation cannot replay it.
        intent.data = null
    }
}
