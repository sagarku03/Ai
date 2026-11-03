package com.jarvisai.assistant.security

import android.app.ActivityManager
import android.content.Context
import android.util.Log

enum class SafetyStage {
    STAGE_0_NORMAL,      // Full multimodal & Live Bidi audio enabled
    STAGE_1_FALLBACK,    // Lightweight Flash-Lite model & reduced memory window
    STAGE_2_SAFE_MODE    // Minimal safe-mode recovery after consecutive crashes or critical low RAM
}

/**
 * 3-Tier self-healing watchdog that tracks startup crashes and available device RAM
 * to prevent bootloops or OutOfMemoryError on constrained devices.
 */
class ModelSafetyGuard(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun evaluateStartupStage(): SafetyStage {
        val crashCount = prefs.getInt(KEY_CONSECUTIVE_CRASHES, 0)
        val isLowRam = isDeviceCriticallyLowOnMemory()

        val stage = when {
            crashCount >= 3 -> SafetyStage.STAGE_2_SAFE_MODE
            crashCount == 2 || isLowRam -> SafetyStage.STAGE_1_FALLBACK
            else -> SafetyStage.STAGE_0_NORMAL
        }
        Log.i(TAG, "Evaluated startup stage=$stage (consecutiveCrashes=$crashCount, lowRam=$isLowRam)")
        return stage
    }

    fun recordSessionStart() {
        val current = prefs.getInt(KEY_CONSECUTIVE_CRASHES, 0)
        prefs.edit().putInt(KEY_CONSECUTIVE_CRASHES, current + 1).apply()
    }

    fun markHealthyBootCompleted() {
        prefs.edit().putInt(KEY_CONSECUTIVE_CRASHES, 0).apply()
    }

    private fun isDeviceCriticallyLowOnMemory(): Boolean {
        return try {
            val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            memInfo.lowMemory || (memInfo.availMem / (1024 * 1024)) < 250L
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "ModelSafetyGuard"
        private const val PREFS_NAME = "jarvis_model_safety_guard"
        private const val KEY_CONSECUTIVE_CRASHES = "consecutive_unclean_starts"
    }
}
