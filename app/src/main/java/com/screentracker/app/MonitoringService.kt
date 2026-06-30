package com.screentracker.app

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MonitoringService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: EventDatabase
    private lateinit var cameraManager: CameraManager
    private var torchCallback: CameraManager.TorchCallback? = null
    private var isReceiverRegistered = false
    private var isTorchCallbackRegistered = false

    // Track used message indices for this night to avoid repeats
    private val usedMessageIndices = mutableSetOf<Int>()
    private var currentNightStartTime: Long = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    logEvent(EventType.SCREEN_ON)
                    // Send night intervention notification if sleep session active
                    sendNightInterventionIfNeeded()
                }
                Intent.ACTION_USER_PRESENT -> {
                    logEvent(EventType.USER_PRESENT)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = EventDatabase.getInstance(this)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        // Don't register receivers here - only register when sleep tracking starts
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SLEEP_START -> {
                startForeground(NOTIFICATION_ID, createNotification("Sleep tracking active"))
                handleSleepStart()
            }
            ACTION_SLEEP_END -> handleSleepEnd()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        // Clean up receivers if they're still registered
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(screenReceiver)
            } catch (e: Exception) {
                // Already unregistered
            }
            isReceiverRegistered = false
        }

        if (isTorchCallbackRegistered) {
            torchCallback?.let {
                try {
                    cameraManager.unregisterTorchCallback(it)
                } catch (e: Exception) {
                    // Already unregistered
                }
            }
            isTorchCallbackRegistered = false
        }
    }

    private fun handleSleepStart() {
        // Register receivers when sleep tracking starts (must be on main thread)
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenReceiver, filter)
            isReceiverRegistered = true
        }

        if (!isTorchCallbackRegistered) {
            torchCallback = object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    super.onTorchModeChanged(cameraId, enabled)
                    if (enabled) {
                        logEvent(EventType.FLASHLIGHT_ON)
                    } else {
                        logEvent(EventType.FLASHLIGHT_OFF)
                    }
                }
            }
            cameraManager.registerTorchCallback(torchCallback!!, null)
            isTorchCallbackRegistered = true
        }

        serviceScope.launch {
            logEvent(EventType.SLEEP_START)
            val now = System.currentTimeMillis()
            currentNightStartTime = now
            usedMessageIndices.clear() // Reset message bank for new night

            // Mark service as alive for death detection
            markServiceAlive()

            val session = SleepSession(
                startTime = now,
                endTime = null,
                totalDurationMinutes = null,
                interruptionCount = null,
                completedCycles = null,
                qualityScore = null,
                qualityLabel = null
            )
            database.sleepSessionDao().insertSession(session)
        }
    }

    private fun handleSleepEnd() {
        serviceScope.launch {
            val now = System.currentTimeMillis()
            logEvent(EventType.SLEEP_END)

            val activeSession = database.sleepSessionDao().getActiveSession()
            if (activeSession != null) {
                val screenEvents = database.eventDao().getEventsBetween(
                    activeSession.startTime, now, EventType.SCREEN_ON
                )

                val analysis = SleepAnalyzer.analyze(
                    sleepStart = activeSession.startTime,
                    sleepEnd = now,
                    screenOnTimestamps = screenEvents.map { it.timestamp }
                )

                database.sleepSessionDao().updateSession(
                    activeSession.copy(
                        endTime = now,
                        totalDurationMinutes = analysis.totalDurationMinutes,
                        interruptionCount = analysis.interruptionCount,
                        completedCycles = analysis.completedCycles,
                        qualityScore = analysis.qualityScore,
                        qualityLabel = analysis.qualityLabel
                    )
                )
            }

            // Unregister receivers when sleep tracking stops
            if (isReceiverRegistered) {
                try {
                    unregisterReceiver(screenReceiver)
                } catch (e: Exception) {
                    // Already unregistered
                }
                isReceiverRegistered = false
            }

            if (isTorchCallbackRegistered) {
                torchCallback?.let {
                    try {
                        cameraManager.unregisterTorchCallback(it)
                    } catch (e: Exception) {
                        // Already unregistered
                    }
                }
                isTorchCallbackRegistered = false
            }

            // Stop the service since tracking is no longer active
            stopForeground(true)
            stopSelf()
        }
    }

    private fun logEvent(type: EventType) {
        serviceScope.launch {
            val event = Event(
                type = type,
                timestamp = System.currentTimeMillis()
            )
            database.eventDao().insertEvent(event)

            // Update heartbeat on every event during active session
            val activeSession = database.sleepSessionDao().getActiveSession()
            if (activeSession != null) {
                markServiceAlive()
            }
        }
    }

    /**
     * Marks the service as alive by storing current timestamp.
     * Used for death detection - if service crashes, the timestamp becomes stale.
     */
    private fun markServiceAlive() {
        val prefs = getSharedPreferences("rest_prefs", MODE_PRIVATE)
        prefs.edit().putLong("service_last_alive", System.currentTimeMillis()).apply()
    }

    private fun createNotification(text: String): Notification {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("REST")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, createNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when REST is monitoring events"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Sends a heads-up night intervention notification if a sleep session is currently active.
     * Uses NightMessages to show a random gentle message.
     */
    private fun sendNightInterventionIfNeeded() {
        serviceScope.launch {
            val activeSession = database.sleepSessionDao().getActiveSession()
            if (activeSession != null) {
                // Get a random message, avoiding repeats within this night
                val (message, index) = NightMessages.getRandomMessage(usedMessageIndices)
                usedMessageIndices.add(index)

                // Create night intervention notification channel if needed
                createNightNotificationChannel()

                // Build heads-up notification
                val intent = Intent(this@MonitoringService, NightMessageActivity::class.java).apply {
                    putExtra("message", message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val pendingIntent = PendingIntent.getActivity(
                    this@MonitoringService,
                    NIGHT_NOTIFICATION_ID,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notification = NotificationCompat.Builder(this@MonitoringService, NIGHT_CHANNEL_ID)
                    .setContentTitle("REST")
                    .setContentText(message)
                    .setSmallIcon(android.R.drawable.ic_menu_view)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH) // Heads-up
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build()

                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NIGHT_NOTIFICATION_ID, notification)
            }
        }
    }

    private fun createNightNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NIGHT_CHANNEL_ID,
                "Night Interventions",
                NotificationManager.IMPORTANCE_HIGH // Required for heads-up
            ).apply {
                description = "Gentle reminders when you wake at night"
                enableVibration(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "ScreenTrackerChannel"
        private const val NOTIFICATION_ID = 1001
        private const val NIGHT_CHANNEL_ID = "NightInterventionChannel"
        private const val NIGHT_NOTIFICATION_ID = 1002
        const val ACTION_SLEEP_START = "com.screentracker.app.SLEEP_START"
        const val ACTION_SLEEP_END = "com.screentracker.app.SLEEP_END"
    }
}
