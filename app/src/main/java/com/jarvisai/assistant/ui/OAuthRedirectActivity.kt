package com.jarvisai.assistant.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.MainActivity

/**
 * Captures OAuth 2.0 deep-link redirects (`jarvisai://oauth2redirect?code=...`)
 * and forwards the authorization code safely back to the application session.
 */
class OAuthRedirectActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleRedirectIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRedirectIntent(intent)
    }

    private fun handleRedirectIntent(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "jarvisai" && data.host == "oauth2redirect") {
            val authCode = data.getQueryParameter("code").orEmpty()
            val state = data.getQueryParameter("state").orEmpty()
            val error = data.getQueryParameter("error").orEmpty()

            val prefs = getSharedPreferences("jarvis_oauth_callbacks", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("last_oauth_code", authCode)
                .putString("last_oauth_state", state)
                .putString("last_oauth_error", error)
                .putLong("last_oauth_timestamp", System.currentTimeMillis())
                .apply()

            Log.i(TAG, "Captured OAuth callback (hasCode=${authCode.isNotEmpty()}, error=$error)")
        }

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(mainIntent)
        finish()
    }

    companion object {
        private const val TAG = "OAuthRedirectActivity"
    }
}
