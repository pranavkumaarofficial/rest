package com.screentracker.app

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    private val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val fileDateFmt = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())

    /**
     * Exports all data as two CSV files and launches Android share sheet.
     * Returns the number of events exported, or -1 on failure.
     */
    suspend fun exportAndShare(context: Context, database: EventDatabase): Int {
        val events = database.eventDao().getAllEventsOnce()
        val sessions = database.sleepSessionDao().getAllSessionsOnce()

        val exportDir = File(context.cacheDir, "export").apply { mkdirs() }
        val timestamp = fileDateFmt.format(Date())

        val eventsFile = writeEventsCsv(exportDir, timestamp, events)
        val sessionsFile = writeSessionsCsv(exportDir, timestamp, sessions)
        val dailyFile = writeDailySummaryCsv(exportDir, timestamp, events, sessions)

        val files = listOfNotNull(eventsFile, sessionsFile, dailyFile)
        if (files.isEmpty()) return -1

        shareFiles(context, files)
        return events.size
    }

    private fun writeEventsCsv(dir: File, timestamp: String, events: List<Event>): File? {
        if (events.isEmpty()) return null
        val file = File(dir, "rest_events_$timestamp.csv")
        file.bufferedWriter().use { w ->
            w.appendLine("id,type,date,time,timestamp_ms")
            for (e in events) {
                val dt = Date(e.timestamp)
                w.appendLine("${e.id},${e.type},${dateFmt.format(dt)},${dateTimeFmt.format(dt)},${e.timestamp}")
            }
        }
        return file
    }

    private fun writeSessionsCsv(dir: File, timestamp: String, sessions: List<SleepSession>): File? {
        if (sessions.isEmpty()) return null
        val file = File(dir, "rest_sleep_sessions_$timestamp.csv")
        file.bufferedWriter().use { w ->
            w.appendLine("id,start_date,start_time,end_date,end_time,duration_minutes,interruptions,completed_cycles,quality_score,quality_label,start_ms,end_ms")
            for (s in sessions) {
                val startDt = Date(s.startTime)
                val endDt = s.endTime?.let { Date(it) }
                w.appendLine(buildString {
                    append(s.id).append(',')
                    append(dateFmt.format(startDt)).append(',')
                    append(dateTimeFmt.format(startDt)).append(',')
                    append(endDt?.let { dateFmt.format(it) } ?: "").append(',')
                    append(endDt?.let { dateTimeFmt.format(it) } ?: "").append(',')
                    append(s.totalDurationMinutes ?: "").append(',')
                    append(s.interruptionCount ?: "").append(',')
                    append(s.completedCycles ?: "").append(',')
                    append(s.qualityScore ?: "").append(',')
                    append(s.qualityLabel ?: "").append(',')
                    append(s.startTime).append(',')
                    append(s.endTime ?: "")
                })
            }
        }
        return file
    }

    private fun writeDailySummaryCsv(
        dir: File,
        timestamp: String,
        events: List<Event>,
        sessions: List<SleepSession>
    ): File? {
        if (events.isEmpty()) return null
        val file = File(dir, "rest_daily_summary_$timestamp.csv")

        // Group events by date
        val byDay = events.groupBy { dateFmt.format(Date(it.timestamp)) }.toSortedMap()

        // Index sessions by start date
        val sessionsByDate = sessions.groupBy { dateFmt.format(Date(it.startTime)) }

        file.bufferedWriter().use { w ->
            w.appendLine("date,screen_on_count,unlock_count,flashlight_on_count,flashlight_off_count,sleep_sessions,avg_sleep_score,total_sleep_minutes,total_interruptions,total_events")
            for ((day, dayEvents) in byDay) {
                val screenOn = dayEvents.count { it.type == EventType.SCREEN_ON }
                val unlocks = dayEvents.count { it.type == EventType.USER_PRESENT }
                val flashOn = dayEvents.count { it.type == EventType.FLASHLIGHT_ON }
                val flashOff = dayEvents.count { it.type == EventType.FLASHLIGHT_OFF }

                val daySessions = sessionsByDate[day] ?: emptyList()
                val completedSessions = daySessions.filter { it.endTime != null }
                val sleepCount = completedSessions.size
                val avgScore = if (completedSessions.isNotEmpty()) {
                    completedSessions.mapNotNull { it.qualityScore }.average().toInt()
                } else null
                val totalSleepMin = completedSessions.sumOf { it.totalDurationMinutes ?: 0 }
                val totalInterruptions = completedSessions.sumOf { it.interruptionCount ?: 0 }

                w.appendLine(buildString {
                    append(day).append(',')
                    append(screenOn).append(',')
                    append(unlocks).append(',')
                    append(flashOn).append(',')
                    append(flashOff).append(',')
                    append(sleepCount).append(',')
                    append(avgScore ?: "").append(',')
                    append(if (totalSleepMin > 0) totalSleepMin else "").append(',')
                    append(if (totalInterruptions > 0) totalInterruptions else "").append(',')
                    append(dayEvents.size)
                })
            }
        }
        return file
    }

    private fun shareFiles(context: Context, files: List<File>) {
        val authority = "${context.packageName}.fileprovider"
        val uris = files.map { FileProvider.getUriForFile(context, authority, it) }

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "text/csv"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.putExtra(Intent.EXTRA_SUBJECT, "REST App Data Export")

        val chooser = Intent.createChooser(intent, "Export REST data")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
