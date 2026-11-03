package com.jarvisai.assistant.service

import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.util.Log

/**
 * OS-level Default Digital Assistant entry point (bound with BIND_VOICE_INTERACTION).
 * Triggered when the user long-presses the power/home button or swipes from the corner.
 */
class JarvisVoiceInteractionService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()
        Log.i(TAG, "JARVIS VoiceInteractionService ready.")
    }

    fun triggerAssistSession(args: Bundle = Bundle()) {
        showSession(args, VoiceInteractionSession.SHOW_WITH_ASSIST or VoiceInteractionSession.SHOW_WITH_SCREENSHOT)
    }

    companion object {
        private const val TAG = "JarvisVoiceInteract"
    }
}
