package com.jarvisai.assistant.service

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log
import com.example.MainActivity

class JarvisVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return JarvisVoiceInteractionSession(this)
    }
}

class JarvisVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.i(TAG, "Assist session invoked (flags=$showFlags). Launching JARVIS HUD...")
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("EXTRA_FROM_SYSTEM_ASSIST", true)
        }
        context.startActivity(launchIntent)
        LiveVoiceForegroundService.startServiceIfNeeded(context)
        finish()
    }

    override fun onHandleAssist(
        data: Bundle?,
        structure: AssistStructure?,
        content: AssistContent?
    ) {
        super.onHandleAssist(data, structure, content)
        val pkg = structure?.activityComponent?.packageName.orEmpty()
        val title = structure?.let {
            if (it.windowNodeCount > 0) it.getWindowNodeAt(0).title?.toString().orEmpty() else ""
        }.orEmpty()
        if (pkg.isNotBlank()) {
            ConversationContextManager.setActiveForegroundApp(
                appName = ConversationContextManager.resolveReadableAppName(pkg, title),
                packageName = pkg,
                windowTitle = title,
                verified = true
            )
        }
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)
        Log.d(TAG, "Received system assist screenshot (${screenshot?.width}x${screenshot?.height})")
    }

    companion object {
        private const val TAG = "JarvisAssistSession"
    }
}
