package com.jarvisai.assistant.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity

/**
 * Wakes the screen and requests standard OS Keyguard dismissal for scheduled
 * autonomous routines or hands-free voice wake events.
 */
class UnlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "JarvisAI:UnlockWakeLock"
        )
        wakeLock?.acquire(5_000L)

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && keyguardManager != null) {
            keyguardManager.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        Log.i(TAG, "Keyguard dismissed successfully.")
                        finish()
                    }

                    override fun onDismissCancelled() {
                        Log.w(TAG, "Keyguard dismissal cancelled by user.")
                        finish()
                    }

                    override fun onDismissError() {
                        Log.e(TAG, "Keyguard dismissal error.")
                        finish()
                    }
                }
            )
        } else {
            finish()
        }
    }

    companion object {
        private const val TAG = "JarvisUnlockActivity"
    }
}
