package com.jarvisai.assistant.service

import android.content.Context
import android.util.Log
import com.jarvisai.assistant.data.local.JarvisDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Periodic background knowledge maintenance worker that prunes expired temporary memories
 * and maintains the local JARVIS knowledge base in Room without requiring extra dependencies.
 */
object KnowledgeSyncWorker {

    private const val TAG = "KnowledgeSyncWorker"
    private const val SYNC_INTERVAL_MS = 12L * 60L * 60L * 1000L // 12 hours

    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var periodicJob: Job? = null

    fun schedulePeriodicSync(context: Context) {
        if (periodicJob?.isActive == true) return
        val appContext = context.applicationContext
        periodicJob = workerScope.launch {
            while (isActive) {
                runSyncOnce(appContext)
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    suspend fun runSyncOnce(context: Context): Int {
        return try {
            val dao = JarvisDatabase.getInstance(context.applicationContext).jarvisDao()
            val purgedCount = dao.purgeExpiredMemories(System.currentTimeMillis())
            Log.i(TAG, "KnowledgeSyncWorker completed: purged $purgedCount expired memories.")
            purgedCount
        } catch (e: Exception) {
            Log.e(TAG, "KnowledgeSyncWorker failed: ${e.message}")
            0
        }
    }
}
