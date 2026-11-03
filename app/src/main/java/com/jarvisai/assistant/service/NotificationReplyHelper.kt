package com.jarvisai.assistant.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

data class CachedReplyAction(
    val key: String,
    val packageName: String,
    val senderTitle: String,
    val messageText: String,
    val actionIntent: PendingIntent,
    val remoteInputs: Array<RemoteInput>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Extracts and caches inline `RemoteInput` reply actions from incoming messaging notifications
 * (`com.whatsapp`, `com.whatsapp.w4b`, `org.telegram.messenger`, etc.) so JARVIS can send
 * user-approved direct replies via `RemoteInput.addResultsToIntent()`.
 */
object NotificationReplyHelper {

    private const val TAG = "NotifReplyHelper"
    private val SUPPORTED_MESSAGING_PACKAGES = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "com.google.android.apps.messaging"
    )

    private val replyCache = ConcurrentHashMap<String, CachedReplyAction>()

    fun inspectAndCacheReplyAction(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg !in SUPPORTED_MESSAGING_PACKAGES) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (sender.isEmpty() || text.isEmpty()) return

        val actions = notification.actions ?: return
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            val pendingIntent = action.actionIntent ?: continue
            if (remoteInputs.isNotEmpty()) {
                val cached = CachedReplyAction(
                    key = sbn.key,
                    packageName = pkg,
                    senderTitle = sender,
                    messageText = text,
                    actionIntent = pendingIntent,
                    remoteInputs = remoteInputs
                )
                replyCache[sender.lowercase()] = cached
                replyCache[sbn.key] = cached
                Log.d(TAG, "Cached RemoteInput reply action for sender='$sender' ($pkg)")
                break
            }
        }
    }

    fun sendInlineReply(
        context: Context,
        senderOrNotificationKey: String,
        replyText: String,
        userConfirmed: Boolean
    ): Result<String> {
        if (!userConfirmed) {
            return Result.failure(IllegalStateException("User confirmation is required before sending an inline notification reply."))
        }
        val cleanReply = replyText.trim()
        if (cleanReply.isEmpty()) {
            return Result.failure(IllegalArgumentException("Reply text cannot be empty."))
        }

        val target = replyCache[senderOrNotificationKey]
            ?: replyCache[senderOrNotificationKey.lowercase()]
            ?: replyCache.values.firstOrNull {
                it.senderTitle.contains(senderOrNotificationKey, ignoreCase = true)
            }
            ?: return Result.failure(NoSuchElementException("No active replyable notification found for '$senderOrNotificationKey'."))

        return try {
            val localIntent = Intent()
            val resultsBundle = Bundle()
            for (remoteInput in target.remoteInputs) {
                resultsBundle.putCharSequence(remoteInput.resultKey, cleanReply)
            }
            RemoteInput.addResultsToIntent(target.remoteInputs, localIntent, resultsBundle)
            target.actionIntent.send(context, 0, localIntent)
            Result.success("Sent inline reply to ${target.senderTitle}: \"$cleanReply\"")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch RemoteInput reply: ${e.message}")
            Result.failure(e)
        }
    }
}
