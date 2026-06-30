package com.screentracker.app

import android.content.Context

/**
 * Generates gentle, non-judgmental morning copy based on sleep session data.
 *
 * Design principles:
 * - Gets gentler on bad nights, not harsher
 * - Never grades sleep (no "excellent" / "poor" language)
 * - Shows trends, not absolute counts
 * - Tone test: would this be okay on the worst night of someone's month?
 */
object MorningCopy {

    /**
     * Generates main morning greeting based on session quality.
     * Returns null if service died (use "I lost track" instead).
     */
    fun generateGreeting(
        context: Context,
        session: SleepSession,
        analysis: SleepAnalysis?
    ): String? {
        // Check if service died during session
        if (ServiceHealthCheck.didServiceDieLastNight(context, session)) {
            return null // Caller should show "I lost track" instead
        }

        if (analysis == null) return "You rested last night."

        val hours = analysis.totalDurationMinutes / 60
        val minutes = analysis.totalDurationMinutes % 60

        // Format duration
        val durationText = when {
            minutes == 0 -> "You rested about $hours hours."
            else -> "You rested about $hours hours and $minutes minutes."
        }

        return durationText
    }

    /**
     * Generates secondary message about wake-ups.
     * Gets gentler on rough nights.
     */
    fun generateWakeMessage(
        analysis: SleepAnalysis?,
        previousSessionInterruptions: Int? = null
    ): String {
        if (analysis == null) return ""

        val count = analysis.interruptionCount

        return when {
            count == 0 -> "You settled through the night without waking."
            count == 1 -> "You woke once and settled back. That's how sleep works."
            count == 2 -> "You woke a couple of times and settled back."
            count <= 4 -> {
                // Check trend if available
                if (previousSessionInterruptions != null && count < previousSessionInterruptions) {
                    "You woke a few times — a little less than usual."
                } else {
                    "You woke a few times and settled back."
                }
            }
            count <= 7 -> "Last night was restless. Those happen."
            else -> "Last night was a hard one. Those happen. Today's a fresh start."
        }
    }

    /**
     * Returns gentle encouragement after a good stretch (multiple settled nights).
     * Only shown once per streak.
     */
    fun generateOffRampMessage(settledNightCount: Int): String? {
        return when {
            settledNightCount >= 5 -> "You've settled back to sleep on your own several nights running. You might not need me much longer — and that's the point."
            settledNightCount >= 3 -> "You're settling back to sleep more easily lately."
            else -> null
        }
    }

    /**
     * Message shown when service died during session.
     */
    fun getServiceDeathMessage(): String {
        return "I lost track last night. (The monitoring service may have been stopped by your phone's battery optimizer.)"
    }
}
