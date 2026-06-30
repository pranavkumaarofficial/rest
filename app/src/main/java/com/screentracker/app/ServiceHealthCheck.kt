package com.screentracker.app

import android.content.Context

/**
 * Checks if the monitoring service died during the last sleep session.
 *
 * Logic: If service was alive during session but hasn't sent a heartbeat in 30+ minutes,
 * it likely crashed or was killed by the system.
 */
object ServiceHealthCheck {

    private const val HEARTBEAT_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes

    /**
     * Returns true if service appears to have died during last active session.
     * This should only be called after a session has ended.
     */
    fun didServiceDieLastNight(context: Context, lastSession: SleepSession?): Boolean {
        if (lastSession == null || lastSession.endTime == null) return false

        val prefs = context.getSharedPreferences("rest_prefs", Context.MODE_PRIVATE)
        val lastAlive = prefs.getLong("service_last_alive", 0L)

        // If service never marked itself alive, can't determine
        if (lastAlive == 0L) return false

        // If last heartbeat was more than 30 min before session ended, service likely died
        val sessionEnd = lastSession.endTime
        val timeSinceHeartbeat = sessionEnd - lastAlive

        return timeSinceHeartbeat > HEARTBEAT_TIMEOUT_MS
    }

    /**
     * Clears the service alive marker (e.g., when service stops normally)
     */
    fun clearServiceMarker(context: Context) {
        val prefs = context.getSharedPreferences("rest_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("service_last_alive").apply()
    }
}
