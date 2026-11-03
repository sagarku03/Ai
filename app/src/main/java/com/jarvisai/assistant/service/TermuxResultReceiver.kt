package com.jarvisai.assistant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives asynchronous stdout, stderr, and exitCode bundles returned by Termux
 * RunCommandService and correlates them via `exec_session_id`.
 */
class TermuxResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val sessionId = intent.getStringExtra(EXTRA_EXEC_SESSION_ID).orEmpty()
        val resultBundle = intent.getBundleExtra(TERMUX_EXTRA_RESULT_BUNDLE)

        val stdout = resultBundle?.getString(TERMUX_EXTRA_STDOUT, "").orEmpty()
        val stderr = resultBundle?.getString(TERMUX_EXTRA_STDERR, "").orEmpty()
        val exitCode = resultBundle?.getInt(TERMUX_EXTRA_EXIT_CODE, -1) ?: -1
        val errMsg = resultBundle?.getString(TERMUX_EXTRA_ERRMSG, "").orEmpty()

        Log.i(TAG, "Termux result received for session=$sessionId (exitCode=$exitCode)")
        TermuxBridgeManager.onCommandResultReceived(
            TermuxCommandResult(
                sessionId = sessionId,
                stdout = stdout,
                stderr = stderr,
                exitCode = exitCode,
                errorMessage = errMsg
            )
        )
    }

    companion object {
        private const val TAG = "TermuxResultReceiver"
        const val ACTION_TERMUX_EXEC_RESULT = "com.jarvisai.assistant.ACTION_TERMUX_EXEC_RESULT"
        const val EXTRA_EXEC_SESSION_ID = "exec_session_id"

        const val TERMUX_EXTRA_RESULT_BUNDLE = "result"
        const val TERMUX_EXTRA_STDOUT = "stdout"
        const val TERMUX_EXTRA_STDERR = "stderr"
        const val TERMUX_EXTRA_EXIT_CODE = "exitCode"
        const val TERMUX_EXTRA_ERRMSG = "errmsg"
    }
}
