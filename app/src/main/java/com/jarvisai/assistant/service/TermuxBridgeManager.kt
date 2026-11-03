package com.jarvisai.assistant.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class TermuxCommandResult(
    val sessionId: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val errorMessage: String = ""
) {
    val isSuccess: Boolean get() = exitCode == 0 && errorMessage.isEmpty()
}

/**
 * Dispatches commands to Termux (`com.termux.app.RunCommandService`) using
 * `com.termux.permission.RUN_COMMAND` and awaits the correlated result via `TermuxResultReceiver`.
 */
object TermuxBridgeManager {

    private const val TAG = "TermuxBridgeManager"
    private const val TERMUX_PACKAGE = "com.termux"
    private const val TERMUX_SERVICE_CLASS = "com.termux.app.RunCommandService"
    private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"

    private const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    private const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"

    private val pendingSessions = ConcurrentHashMap<String, CompletableDeferred<TermuxCommandResult>>()

    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(TERMUX_PACKAGE, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun executeCommand(
        context: Context,
        executablePath: String = "/data/data/com.termux/files/usr/bin/bash",
        arguments: Array<String> = emptyArray(),
        workDir: String = "/data/data/com.termux/files/home",
        background: Boolean = true,
        timeoutMs: Long = 30_000L
    ): TermuxCommandResult {
        val sessionId = UUID.randomUUID().toString()
        if (!isTermuxInstalled(context)) {
            return TermuxCommandResult(
                sessionId = sessionId,
                stdout = "",
                stderr = "",
                exitCode = -1,
                errorMessage = "Termux app (com.termux) is not installed on this device."
            )
        }

        val deferred = CompletableDeferred<TermuxCommandResult>()
        pendingSessions[sessionId] = deferred

        val callbackIntent = Intent(context, TermuxResultReceiver::class.java).apply {
            action = TermuxResultReceiver.ACTION_TERMUX_EXEC_RESULT
            putExtra(TermuxResultReceiver.EXTRA_EXEC_SESSION_ID, sessionId)
        }

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val resultPendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId.hashCode(),
            callbackIntent,
            pendingFlags
        )

        val runIntent = Intent(ACTION_RUN_COMMAND).apply {
            component = ComponentName(TERMUX_PACKAGE, TERMUX_SERVICE_CLASS)
            putExtra(EXTRA_COMMAND_PATH, executablePath)
            putExtra(EXTRA_ARGUMENTS, arguments)
            putExtra(EXTRA_WORKDIR, workDir)
            putExtra(EXTRA_BACKGROUND, background)
            putExtra(EXTRA_PENDING_INTENT, resultPendingIntent)
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(runIntent)
            } else {
                context.startService(runIntent)
            }

            withTimeoutOrNull(timeoutMs) {
                deferred.await()
            } ?: TermuxCommandResult(
                sessionId = sessionId,
                stdout = "",
                stderr = "",
                exitCode = -1,
                errorMessage = "Timed out waiting for Termux command result after ${timeoutMs}ms."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Termux command dispatch failed: ${e.message}")
            TermuxCommandResult(
                sessionId = sessionId,
                stdout = "",
                stderr = "",
                exitCode = -1,
                errorMessage = "Failed to start Termux RunCommandService: ${e.localizedMessage}"
            )
        } finally {
            pendingSessions.remove(sessionId)
        }
    }

    internal fun onCommandResultReceived(result: TermuxCommandResult) {
        pendingSessions.remove(result.sessionId)?.complete(result)
    }
}
