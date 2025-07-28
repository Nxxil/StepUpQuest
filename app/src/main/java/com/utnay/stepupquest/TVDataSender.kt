package com.utnay.stepupquest

import android.content.Context
import android.util.Log
import com.google.android.gms.cast.Cast
import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await

class TVDataSender(private val context: Context) {

    private var castSession: CastSession? = null
    private lateinit var castContext: CastContext
    private val gson = Gson()

    companion object {
        const val TAG = "TVDataSender"
        const val NAMESPACE = "urn:x-cast:com.utnay.stepupquest"
    }

    init {
        try {
            castContext = CastContext.getSharedInstance(context)
            setupCastListener()
        } catch (e: Exception) {
            Log.e(TAG, "Cast initialization failed: ${e.message}")
        }
    }

    private fun setupCastListener() {
        castContext.sessionManager.addSessionManagerListener(
            sessionManagerListener,
            CastSession::class.java
        )
    }

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            castSession = session
            Log.d(TAG, "Cast session started")
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            castSession = null
            Log.d(TAG, "Cast session ended")
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            castSession = session
            Log.d(TAG, "Cast session resumed")
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            Log.d(TAG, "Cast session suspended")
        }

        // Implementar otros métodos requeridos
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
    }

    suspend fun sendDataToTV(stepData: StepData): Boolean {
        return castSession?.let { session ->
            try {
                val jsonData = gson.toJson(stepData)
                session.sendMessage(NAMESPACE, jsonData).await()
                Log.d(TAG, "Data sent to TV successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send data to TV: ${e.message}")
                false
            }
        } ?: false
    }

    suspend fun sendStatsData(statsData: Map<String, Int>): Boolean {
        return castSession?.let { session ->
            try {
                val dataToSend = mapOf(
                    "type" to "stats_data",
                    "data" to statsData
                )
                val jsonData = gson.toJson(dataToSend)
                session.sendMessage(NAMESPACE, jsonData).await()
                Log.d(TAG, "Stats data sent to TV successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send stats data to TV: ${e.message}")
                false
            }
        } ?: false
    }

    fun isConnectedToTV(): Boolean {
        return castSession != null
    }
}