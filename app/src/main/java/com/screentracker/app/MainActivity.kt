package com.screentracker.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var database: EventDatabase

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startMonitoringService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = EventDatabase.getInstance(this)
        setContent {
            RestApp(
                onStartSleep = { startSleep() },
                onStopSleep = { stopSleep() },
                database = database
            )
        }
    }

    private fun checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                startMonitoringService()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startMonitoringService()
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(this, MonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }

    private fun startSleep() {
        checkPermissionsAndStart()
        val intent = Intent(this, MonitoringService::class.java).apply {
            action = MonitoringService.ACTION_SLEEP_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }

    private fun stopSleep() {
        val intent = Intent(this, MonitoringService::class.java).apply {
            action = MonitoringService.ACTION_SLEEP_END
        }
        startService(intent)
    }
}

// ── App Shell ────────────────────────────────────────────────────────────

@Composable
fun RestApp(
    onStartSleep: () -> Unit,
    onStopSleep: () -> Unit,
    database: EventDatabase
) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var isSleeping by remember { mutableStateOf(false) }
    var sleepStartTime by remember { mutableStateOf<Long?>(null) }
    var sessions by remember { mutableStateOf<List<SleepSession>>(emptyList()) }
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }

    LaunchedEffect(Unit) {
        database.sleepSessionDao().getAllSessions().collect { sessions = it }
    }

    LaunchedEffect(Unit) {
        database.eventDao().getAllEvents().collect { events = it }
    }

    LaunchedEffect(Unit) {
        val active = database.sleepSessionDao().getActiveSession()
        if (active != null) {
            isSleeping = true
            sleepStartTime = active.startTime
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = RestDesignSystem.Colors.Primary,
            onPrimary = RestDesignSystem.Colors.OnPrimary,
            background = RestDesignSystem.Colors.Canvas,
            surface = RestDesignSystem.Colors.Surface1,
            onSurface = RestDesignSystem.Colors.Ink,
            onSurfaceVariant = RestDesignSystem.Colors.InkMuted,
            outline = RestDesignSystem.Colors.Hairline
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RestDesignSystem.Colors.Canvas)
        ) {
            // Subtle radial glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                RestDesignSystem.Colors.Primary.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            radius = 800f
                        )
                    )
            )

            // Smooth crossfade between screens
            Crossfade(
                targetState = currentScreen,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                label = "screenTransition"
            ) { screen ->
                when (screen) {
                    Screen.Home -> HomeScreen(
                        isSleeping = isSleeping,
                        sleepStartTime = sleepStartTime,
                        lastSession = sessions.firstOrNull(),
                        sessionsThisWeek = sessions.count {
                            it.endTime != null && it.startTime >= weekStart()
                        },
                        onSleepToggle = {
                            if (isSleeping) {
                                onStopSleep()
                                isSleeping = false
                                sleepStartTime = null
                            } else {
                                onStartSleep()
                                isSleeping = true
                                sleepStartTime = System.currentTimeMillis()
                            }
                        }
                    )
                    Screen.Sleep -> SleepScreen(
                        sessions = sessions,
                        database = database
                    )
                    Screen.Logs -> LogsScreen(
                        events = events,
                        database = database
                    )
                }
            }

            BottomNav(
                current = currentScreen,
                onSelect = { currentScreen = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

private enum class Screen { Home, Sleep, Logs }

private fun weekStart(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

// ── Home Screen ──────────────────────────────────────────────────────────

@Composable
private fun HomeScreen(
    isSleeping: Boolean,
    sleepStartTime: Long?,
    lastSession: SleepSession?,
    sessionsThisWeek: Int,
    onSleepToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = RestDesignSystem.Spacing.XL)
            .padding(top = 80.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Wordmark
        Text(
            text = "rest",
            fontSize = RestDesignSystem.Typography.DisplayXL.fontSize,
            fontWeight = RestDesignSystem.Typography.DisplayXL.fontWeight,
            fontFamily = FontFamily.SansSerif,
            color = RestDesignSystem.Colors.Primary,
            letterSpacing = RestDesignSystem.Typography.DisplayXL.letterSpacing
        )

        Spacer(modifier = Modifier.weight(1f))

        if (!isSleeping) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Random gentle quote
                val randomQuote = remember { getRandomQuote() }
                Text(
                    text = randomQuote,
                    fontSize = RestDesignSystem.Typography.Body.fontSize,
                    color = RestDesignSystem.Colors.InkMuted,
                    letterSpacing = RestDesignSystem.Typography.Body.letterSpacing,
                    modifier = Modifier.padding(bottom = RestDesignSystem.Spacing.XXL)
                )

                // Play button
                PlayButton(isPlaying = false, onClick = onSleepToggle)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pulsing indicator
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = RestDesignSystem.Colors.Primary.copy(alpha = alpha),
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.height(RestDesignSystem.Spacing.XL))

                Text(
                    text = "Resting since ${formatTime(sleepStartTime ?: 0L)}",
                    fontSize = RestDesignSystem.Typography.BodyLG.fontSize,
                    color = RestDesignSystem.Colors.Ink.copy(alpha = 0.8f),
                    letterSpacing = RestDesignSystem.Typography.BodyLG.letterSpacing
                )

                sleepStartTime?.let {
                    ElapsedTimer(startTime = it)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Breathing exercises
                BreathingExercises()

                Spacer(modifier = Modifier.height(32.dp))

                PlayButton(isPlaying = true, onClick = onSleepToggle)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonPress"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        RestDesignSystem.Colors.Primary,
                        RestDesignSystem.Colors.Primary.copy(alpha = 0.85f)
                    )
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Use outlined icons for cleaner look
        Icon(
            imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            contentDescription = null,
            tint = RestDesignSystem.Colors.OnPrimary,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun ElapsedTimer(startTime: Long) {
    var elapsed by remember { mutableStateOf(0L) }

    LaunchedEffect(startTime) {
        while (true) {
            elapsed = System.currentTimeMillis() - startTime
            delay(1000)
        }
    }

    val hours = (elapsed / 3600000).toInt()
    val minutes = ((elapsed % 3600000) / 60000).toInt()

    Text(
        text = String.format("%d:%02d", hours, minutes),
        fontSize = 32.sp,
        fontWeight = FontWeight.Normal,
        color = RestDesignSystem.Colors.InkMuted,
        modifier = Modifier.padding(top = 16.dp)
    )
}

private fun formatTime(timestamp: Long): String {
    val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    return fmt.format(Date(timestamp))
}

private fun formatDuration(minutes: Int?): String {
    if (minutes == null || minutes == 0) return "–"
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

private fun getRandomQuote(): String {
    val quotes = listOf(
        "Waking in the night is normal.",
        "You don't have to fall asleep. Just rest.",
        "Let your shoulders drop.",
        "Sleep will come when it's ready.",
        "This moment is enough.",
        "You're doing fine.",
        "Nothing needs to be different.",
        "Rest is not performance.",
        "Your body knows what to do.",
        "There's no wrong way to rest.",
        "Let the night hold you.",
        "Sleep is not something you do."
    )
    return quotes.random()
}

// ── Sleep Screen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SleepScreen(
    sessions: List<SleepSession>,
    database: EventDatabase
) {
    var expandedSessionId by remember { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Text(
            text = "Sleep",
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            color = RestDesignSystem.Colors.Ink,
            modifier = Modifier.padding(start = RestDesignSystem.Spacing.LG, top = 56.dp, bottom = RestDesignSystem.Spacing.XL)
        )

        if (sessions.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Recent session (hero card)
                item(key = "hero") {
                    sessions.firstOrNull()?.let { latest ->
                        RecentSleepCard(session = latest, database = database)
                    }
                }

                // History
                if (sessions.size > 1) {
                    stickyHeader(key = "history_label") {
                        Text(
                            text = "HISTORY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = RestDesignSystem.Colors.InkSubtle,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RestDesignSystem.Colors.Canvas.copy(alpha = 0.95f))
                                .padding(horizontal = RestDesignSystem.Spacing.LG, vertical = 12.dp)
                        )
                    }

                    items(sessions.drop(1), key = { it.id }) { session ->
                        SleepSessionRow(
                            session = session,
                            isExpanded = expandedSessionId == session.id,
                            onClick = {
                                expandedSessionId = if (expandedSessionId == session.id) null else session.id
                            },
                            database = database,
                            onDelete = {
                                expandedSessionId = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "◐",
                fontSize = 64.sp,
                color = RestDesignSystem.Colors.Primary.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No sleep sessions yet",
                fontSize = RestDesignSystem.Typography.Body.fontSize,
                color = RestDesignSystem.Colors.InkMuted
            )
            Text(
                text = "Tap the play button to begin",
                fontSize = RestDesignSystem.Typography.Caption.fontSize,
                color = RestDesignSystem.Colors.InkSubtle,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun RecentSleepCard(session: SleepSession, database: EventDatabase) {
    var analysis by remember { mutableStateOf<SleepAnalysis?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(session.id) {
        if (session.endTime != null) {
            val screenEvents = database.eventDao().getEventsBetween(
                session.startTime, session.endTime, EventType.SCREEN_ON
            )
            analysis = SleepAnalyzer.analyze(
                session.startTime, session.endTime,
                screenEvents.map { it.timestamp }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RestDesignSystem.Spacing.LG, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Delete button in top-right corner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete session",
                    tint = RestDesignSystem.Colors.InkSubtle.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(
                        "Delete session?",
                        color = RestDesignSystem.Colors.Ink
                    )
                },
                text = {
                    Text(
                        "This will delete the sleep session and all related event logs.",
                        color = RestDesignSystem.Colors.InkMuted
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                database.sleepSessionDao().deleteSession(session.id)
                                session.endTime?.let { endTime ->
                                    database.eventDao().deleteEventsBetween(session.startTime, endTime)
                                }
                                Toast.makeText(context, "Session deleted", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                            }
                        }
                    ) {
                        Text("Delete", color = RestDesignSystem.Colors.Error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = RestDesignSystem.Colors.InkMuted)
                    }
                },
                containerColor = RestDesignSystem.Colors.Surface1
            )
        }
        // Morning greeting
        val serviceDied = ServiceHealthCheck.didServiceDieLastNight(context, session)

        if (serviceDied) {
            Text(
                text = MorningCopy.getServiceDeathMessage(),
                fontSize = RestDesignSystem.Typography.Body.fontSize,
                color = RestDesignSystem.Colors.InkMuted,
                lineHeight = (RestDesignSystem.Typography.Body.fontSize.value * 1.5).sp
            )
        } else {
            val greeting = MorningCopy.generateGreeting(context, session, analysis)
            if (greeting != null) {
                Text(
                    text = greeting,
                    fontSize = RestDesignSystem.Typography.Subhead.fontSize,
                    fontWeight = RestDesignSystem.Typography.Subhead.fontWeight,
                    color = RestDesignSystem.Colors.Ink,
                    lineHeight = (RestDesignSystem.Typography.Subhead.fontSize.value * 1.4).sp
                )
            }

            analysis?.let { a ->
                val wakeMessage = MorningCopy.generateWakeMessage(a)
                if (wakeMessage.isNotEmpty()) {
                    Text(
                        text = wakeMessage,
                        fontSize = RestDesignSystem.Typography.Body.fontSize,
                        color = RestDesignSystem.Colors.InkMuted,
                        lineHeight = (RestDesignSystem.Typography.Body.fontSize.value * 1.5).sp
                    )
                }
            }
        }

        // Date
        Text(
            text = formatSessionDate(session.startTime),
            fontSize = RestDesignSystem.Typography.Caption.fontSize,
            color = RestDesignSystem.Colors.InkSubtle
        )

        // Timeline
        analysis?.let { a ->
            SleepTimeline(
                startTime = session.startTime,
                endTime = session.endTime!!,
                cycleBlocks = a.cycleBlocks,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = timeFmt.format(Date(session.startTime)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    color = RestDesignSystem.Colors.InkSubtle.copy(alpha = 0.6f),
                    letterSpacing = 0.3.sp
                )
                Text(
                    text = timeFmt.format(Date(session.endTime)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    color = RestDesignSystem.Colors.InkSubtle.copy(alpha = 0.6f),
                    letterSpacing = 0.3.sp
                )
            }
        }

        // Stats
        analysis?.let { a ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat("${a.completedCycles}", "Cycles")
                MiniStat("${a.interruptionCount}", "Wakeups")
                MiniStat(formatDuration(a.totalDurationMinutes), "Duration")
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat(formatDuration(a.longestStretchMinutes), "Best stretch")
                MiniStat("~${formatDuration(a.totalAwakeMinutes)}", "Awake")
            }
        }

        // Breakdown
        analysis?.let { a ->
            if (a.wakeEpisodes.isNotEmpty()) {
                SleepBreakdown(
                    analysis = a,
                    sleepStart = session.startTime,
                    sleepEnd = session.endTime!!
                )
            }
        }

        HorizontalDivider(color = RestDesignSystem.Colors.Hairline)
    }
}

@Composable
private fun SleepTimeline(
    startTime: Long,
    endTime: Long,
    cycleBlocks: List<CycleBlock>,
    modifier: Modifier = Modifier
) {
    val totalDuration = endTime - startTime

    Row(
        modifier = modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        cycleBlocks.forEach { block ->
            val weight = (block.endTime - block.startTime).toFloat() / totalDuration

            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when (block.severity) {
                            GapSeverity.FULL_CYCLE -> RestDesignSystem.Colors.Primary.copy(alpha = 0.8f)
                            GapSeverity.PARTIAL -> RestDesignSystem.Colors.Primary.copy(alpha = 0.4f)
                            GapSeverity.FRAGMENTED -> RestDesignSystem.Colors.Primary.copy(alpha = 0.25f)
                            GapSeverity.RESTLESS -> RestDesignSystem.Colors.Primary.copy(alpha = 0.15f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Light,
            color = RestDesignSystem.Colors.Ink,
            letterSpacing = (-0.3).sp
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Light,
            color = RestDesignSystem.Colors.InkSubtle.copy(alpha = 0.7f),
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun SleepBreakdown(
    analysis: SleepAnalysis,
    sleepStart: Long,
    sleepEnd: Long
) {
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = "Timeline",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = RestDesignSystem.Colors.InkMuted,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Sleep start
        TimelineItem(
            icon = "◐",
            label = "Fell asleep",
            time = timeFmt.format(Date(sleepStart)),
            isFirst = true
        )

        // Combine gaps and wakes into chronological timeline
        analysis.wakeEpisodes.forEachIndexed { index, wake ->
            // Sleep stretch leading to this wake
            val gap = analysis.gaps.getOrNull(index)
            if (gap != null) {
                TimelineGap(duration = formatDuration(gap.durationMinutes))
            }

            // Wake event
            val wakeLabel = if (wake.rawEventCount > 1) {
                "Woke up · ${wake.rawEventCount} ${if (wake.rawEventCount == 1) "check" else "checks"}"
            } else {
                "Brief check"
            }

            TimelineItem(
                icon = "•",
                label = wakeLabel,
                time = timeFmt.format(Date(wake.time)),
                isFirst = false
            )
        }

        // Final gap if exists
        if (analysis.gaps.size > analysis.wakeEpisodes.size) {
            val finalGap = analysis.gaps.last()
            TimelineGap(duration = formatDuration(finalGap.durationMinutes))
        }

        // Wake up
        TimelineItem(
            icon = "◯",
            label = "Woke up",
            time = timeFmt.format(Date(sleepEnd)),
            isFirst = false,
            isLast = true
        )
    }
}

@Composable
private fun TimelineItem(
    icon: String,
    label: String,
    time: String,
    isFirst: Boolean,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Left column: icon and line
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Top line
                if (!isFirst) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(8.dp)
                            .background(RestDesignSystem.Colors.InkSubtle.copy(alpha = 0.3f))
                    )
                }

                // Icon with slight emphasis for sleep/wake
                val iconColor = when {
                    isFirst -> RestDesignSystem.Colors.Primary
                    isLast -> RestDesignSystem.Colors.Primary.copy(alpha = 0.8f)
                    else -> RestDesignSystem.Colors.InkMuted
                }

                Text(
                    text = icon,
                    fontSize = 18.sp,
                    color = iconColor,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                // Bottom line
                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(8.dp)
                            .background(RestDesignSystem.Colors.InkSubtle.copy(alpha = 0.3f))
                    )
                }
            }
        }

        // Right column: label and time
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = RestDesignSystem.Colors.Ink,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = time,
                fontSize = 12.sp,
                color = RestDesignSystem.Colors.InkSubtle,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun TimelineGap(duration: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left column: connecting line
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(32.dp)
                    .background(RestDesignSystem.Colors.InkSubtle.copy(alpha = 0.3f))
            )
        }

        // Right column: duration bar
        Row(
            modifier = Modifier
                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(1.dp)
                    .width(20.dp)
                    .background(RestDesignSystem.Colors.Primary.copy(alpha = 0.4f))
            )
            Text(
                text = duration,
                fontSize = 12.sp,
                color = RestDesignSystem.Colors.Primary.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Box(
                modifier = Modifier
                    .height(1.dp)
                    .width(20.dp)
                    .background(RestDesignSystem.Colors.Primary.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
private fun SleepSessionRow(
    session: SleepSession,
    isExpanded: Boolean,
    onClick: () -> Unit,
    database: EventDatabase,
    onDelete: () -> Unit
) {
    var analysis by remember { mutableStateOf<SleepAnalysis?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(session.id, isExpanded) {
        if (isExpanded && session.endTime != null) {
            val screenEvents = database.eventDao().getEventsBetween(
                session.startTime, session.endTime, EventType.SCREEN_ON
            )
            analysis = SleepAnalyzer.analyze(
                session.startTime, session.endTime,
                screenEvents.map { it.timestamp }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RestDesignSystem.Spacing.LG, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatSessionDate(session.startTime),
                    fontSize = 14.sp,
                    color = RestDesignSystem.Colors.Ink
                )
                Text(
                    text = formatDuration(session.totalDurationMinutes),
                    fontSize = 12.sp,
                    color = RestDesignSystem.Colors.InkMuted
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete session",
                        tint = RestDesignSystem.Colors.InkSubtle.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = RestDesignSystem.Colors.InkSubtle,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(
                        "Delete session?",
                        color = RestDesignSystem.Colors.Ink
                    )
                },
                text = {
                    Text(
                        "This will delete the sleep session and all related event logs.",
                        color = RestDesignSystem.Colors.InkMuted
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                // Delete session
                                database.sleepSessionDao().deleteSession(session.id)
                                // Delete all events during this session
                                session.endTime?.let { endTime ->
                                    database.eventDao().deleteEventsBetween(session.startTime, endTime)
                                }
                                Toast.makeText(context, "Session deleted", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                                onDelete()
                            }
                        }
                    ) {
                        Text("Delete", color = RestDesignSystem.Colors.Error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = RestDesignSystem.Colors.InkMuted)
                    }
                },
                containerColor = RestDesignSystem.Colors.Surface1
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(),
            exit = shrinkVertically(
                animationSpec = tween(200)
            ) + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                analysis?.let { a ->
                    // Timeline
                    session.endTime?.let { end ->
                        SleepTimeline(
                            startTime = session.startTime,
                            endTime = end,
                            cycleBlocks = a.cycleBlocks,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MiniStat("${a.completedCycles}", "Cycles")
                        MiniStat("${a.interruptionCount}", "Wakeups")
                        MiniStat(formatDuration(a.longestStretchMinutes), "Best")
                    }

                    // Breakdown
                    if (a.wakeEpisodes.isNotEmpty()) {
                        session.endTime?.let { end ->
                            SleepBreakdown(
                                analysis = a,
                                sleepStart = session.startTime,
                                sleepEnd = end
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatSessionDate(timestamp: Long): String {
    val fmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    return fmt.format(Date(timestamp))
}

// ── Logs Screen ──────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LogsScreen(
    events: List<Event>,
    database: EventDatabase
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var isExporting by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Group events by day
    val groupedEvents = remember(events, selectedDate) {
        val filtered = if (selectedDate == null) {
            events
        } else {
            val dayStart = selectedDate!!
            val dayEnd = dayStart + 86400000L // 24 hours
            events.filter { it.timestamp >= dayStart && it.timestamp < dayEnd }
        }

        filtered.groupBy { event ->
            val cal = Calendar.getInstance().apply { timeInMillis = event.timestamp }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.toSortedMap(reverseOrder())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = RestDesignSystem.Spacing.LG, end = RestDesignSystem.Spacing.LG, top = 56.dp, bottom = RestDesignSystem.Spacing.MD),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Logs",
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                color = RestDesignSystem.Colors.Ink
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Clear all logs button
                IconButton(
                    onClick = { showDeleteDialog = true },
                    enabled = events.isNotEmpty()
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "Clear all logs",
                        tint = if (events.isNotEmpty()) RestDesignSystem.Colors.InkMuted else RestDesignSystem.Colors.InkSubtle.copy(alpha = 0.3f)
                    )
                }

                // Export button
                IconButton(
                    onClick = {
                        scope.launch {
                            isExporting = true
                            try {
                                CsvExporter.exportAndShare(context, database)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isExporting = false
                            }
                        }
                    },
                    enabled = !isExporting
                ) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = "Export",
                        tint = RestDesignSystem.Colors.InkMuted
                    )
                }
            }
        }

        // Date filter bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RestDesignSystem.Spacing.LG, vertical = RestDesignSystem.Spacing.SM),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (selectedDate != null) RestDesignSystem.Colors.Primary else RestDesignSystem.Colors.InkMuted
                ),
                border = BorderStroke(
                    1.dp,
                    if (selectedDate != null) RestDesignSystem.Colors.Primary else RestDesignSystem.Colors.Hairline
                ),
                shape = RoundedCornerShape(RestDesignSystem.Rounded.MD)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = "Select date",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedDate?.let {
                        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "Filter by date",
                    fontSize = 14.sp
                )
            }

            if (selectedDate != null) {
                IconButton(onClick = { selectedDate = null }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear filter",
                        tint = RestDesignSystem.Colors.InkSubtle
                    )
                }
            }
        }

        // Date picker dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }) {
                        Text("OK", color = RestDesignSystem.Colors.Primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel", color = RestDesignSystem.Colors.InkMuted)
                    }
                },
                colors = DatePickerDefaults.colors(
                    containerColor = RestDesignSystem.Colors.Surface1,
                    titleContentColor = RestDesignSystem.Colors.Ink,
                    headlineContentColor = RestDesignSystem.Colors.Ink,
                    weekdayContentColor = RestDesignSystem.Colors.InkSubtle,
                    dayContentColor = RestDesignSystem.Colors.Ink,
                    selectedDayContainerColor = RestDesignSystem.Colors.Primary,
                    todayContentColor = RestDesignSystem.Colors.Primary
                )
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Clear all logs confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(
                        "Clear all logs?",
                        color = RestDesignSystem.Colors.Ink
                    )
                },
                text = {
                    Text(
                        "This will permanently delete all event logs (screen, unlock, flashlight events). Sleep sessions will not be affected.",
                        color = RestDesignSystem.Colors.InkMuted
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                database.eventDao().deleteAll()
                                Toast.makeText(context, "All logs cleared", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                            }
                        }
                    ) {
                        Text("Clear All", color = RestDesignSystem.Colors.Error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = RestDesignSystem.Colors.InkMuted)
                    }
                },
                containerColor = RestDesignSystem.Colors.Surface1
            )
        }

        if (groupedEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedDate == null) "No events yet" else "No events on this date",
                    fontSize = RestDesignSystem.Typography.Body.fontSize,
                    color = RestDesignSystem.Colors.InkMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                groupedEvents.forEach { (dayTimestamp, dayEvents) ->
                    stickyHeader(key = "header_$dayTimestamp") {
                        DayHeader(dayTimestamp)
                    }

                    items(dayEvents, key = { it.id }) { event ->
                        EventRow(event)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(timestamp: Long) {
    val dateFmt = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val yesterday = today - 86400000L

    val label = when (timestamp) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> dateFmt.format(Date(timestamp))
    }

    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = RestDesignSystem.Colors.InkMuted,
        letterSpacing = 0.5.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(RestDesignSystem.Colors.Canvas.copy(alpha = 0.95f))
            .padding(horizontal = RestDesignSystem.Spacing.LG, vertical = 12.dp)
    )
}

@Composable
private fun EventRow(event: Event) {
    val timeFmt = remember { SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault()) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RestDesignSystem.Spacing.LG, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val (eventText, eventIcon) = when (event.type) {
                    EventType.SCREEN_ON -> "Screen turned on" to "●"
                    EventType.USER_PRESENT -> "Device unlocked" to "◯"
                    EventType.FLASHLIGHT_ON -> "Flashlight on" to "◐"
                    EventType.FLASHLIGHT_OFF -> "Flashlight off" to "◑"
                    EventType.SLEEP_START -> "Sleep session started" to "◐"
                    EventType.SLEEP_END -> "Sleep session ended" to "◯"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = eventIcon,
                        fontSize = 12.sp,
                        color = RestDesignSystem.Colors.Primary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = eventText,
                        fontSize = 14.sp,
                        color = RestDesignSystem.Colors.Ink
                    )
                }
                Text(
                    text = timeFmt.format(Date(event.timestamp)),
                    fontSize = 12.sp,
                    color = RestDesignSystem.Colors.InkSubtle
                )
            }
        }

        HorizontalDivider(
            color = RestDesignSystem.Colors.Hairline,
            modifier = Modifier.padding(horizontal = RestDesignSystem.Spacing.LG)
        )
    }
}

// ── Bottom Navigation ────────────────────────────────────────────────────

@Composable
private fun BottomNav(
    current: Screen,
    onSelect: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        RestDesignSystem.Colors.Canvas.copy(alpha = 0.98f)
                    )
                )
            )
            .padding(vertical = RestDesignSystem.Spacing.MD),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NavItem(Icons.Outlined.Home, current == Screen.Home) { onSelect(Screen.Home) }
        NavItem(Icons.Outlined.NightsStay, current == Screen.Sleep) { onSelect(Screen.Sleep) }
        NavItem(Icons.Outlined.List, current == Screen.Logs) { onSelect(Screen.Logs) }
    }
}

@Composable
private fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navScale"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier.scale(scale)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) RestDesignSystem.Colors.Primary else RestDesignSystem.Colors.InkSubtle,
            modifier = Modifier.size(24.dp)
        )
    }
}

// ── Breathing Exercises ──────────────────────────────────────────────────

enum class BreathingType(
    val displayName: String,
    val icon: String,
    val phases: List<BreathPhase>
) {
    BOX(
        "Box",
        "□",
        listOf(
            BreathPhase("Inhale", 4000),
            BreathPhase("Hold", 4000),
            BreathPhase("Exhale", 4000),
            BreathPhase("Hold", 4000)
        )
    ),
    EXTENDED_EXHALE(
        "Extended",
        "▭",
        listOf(
            BreathPhase("Inhale", 4000),
            BreathPhase("Hold", 2000),
            BreathPhase("Exhale", 6000),
            BreathPhase("Hold", 2000)
        )
    ),
    FOUR_SEVEN_EIGHT(
        "4-7-8",
        "△",
        listOf(
            BreathPhase("Inhale", 4000),
            BreathPhase("Hold", 7000),
            BreathPhase("Exhale", 8000)
        )
    )
}

data class BreathPhase(val label: String, val durationMs: Int)

@Composable
private fun BreathingExercises(modifier: Modifier = Modifier) {
    var selectedExercise by remember { mutableStateOf<BreathingType?>(null) }
    var isActive by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = selectedExercise,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
        },
        label = "breathingTransition"
    ) { exercise ->
        if (exercise == null) {
            BreathingSelector(
                onSelect = { type ->
                    selectedExercise = type
                    isActive = true
                },
                modifier = modifier
            )
        } else {
            BreathingAnimation(
                type = exercise,
                isActive = isActive,
                onStop = {
                    isActive = false
                    selectedExercise = null
                },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun BreathingSelector(
    onSelect: (BreathingType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Breathing",
            fontSize = 13.sp,
            fontWeight = FontWeight.Light,
            color = RestDesignSystem.Colors.InkMuted.copy(alpha = 0.7f),
            letterSpacing = 0.8.sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BreathingType.entries.forEach { type ->
                BreathingOption(
                    type = type,
                    onClick = { onSelect(type) }
                )
            }
        }
    }
}

@Composable
private fun BreathingOption(
    type: BreathingType,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable(
            onClick = onClick,
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        )
    ) {
        Text(
            text = type.icon,
            fontSize = 32.sp,
            color = RestDesignSystem.Colors.Primary.copy(alpha = 0.7f),
            fontWeight = FontWeight.Light
        )
        Text(
            text = type.displayName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Light,
            color = RestDesignSystem.Colors.InkSubtle.copy(alpha = 0.6f),
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun BreathingAnimation(
    type: BreathingType,
    isActive: Boolean,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCycleDuration = type.phases.sumOf { it.durationMs }.toLong()
    var startTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Reset start time when becoming active
    LaunchedEffect(isActive) {
        if (isActive) {
            startTime = System.currentTimeMillis()
        }
    }

    // Continuously recompose to update animation
    var currentTime by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                if (isActive) {
                    val elapsed = System.currentTimeMillis() - startTime
                    currentTime = elapsed % totalCycleDuration
                }
            }
        }
    }

    // Calculate current phase
    var accumulatedTime = 0L
    var currentPhaseIndex = 0
    var currentPhase = type.phases.first()
    var phaseStartTime = 0L

    for ((index, phase) in type.phases.withIndex()) {
        if (currentTime < accumulatedTime + phase.durationMs) {
            currentPhase = phase
            currentPhaseIndex = index
            phaseStartTime = accumulatedTime
            break
        }
        accumulatedTime += phase.durationMs
    }

    val phaseProgress = ((currentTime - phaseStartTime).toFloat() / currentPhase.durationMs).coerceIn(0f, 1f)
    val remainingSeconds = ((currentPhase.durationMs - (currentTime - phaseStartTime)) / 1000f).toInt().coerceAtLeast(0)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Animation canvas
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            BreathingVisualization(
                type = type,
                progress = phaseProgress,
                currentPhaseIndex = currentPhaseIndex
            )
        }

        // Phase label and countdown
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = currentPhase.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = RestDesignSystem.Colors.Ink,
                letterSpacing = (-0.1).sp
            )
            Text(
                text = "${remainingSeconds}s",
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
                color = RestDesignSystem.Colors.InkSubtle.copy(alpha = 0.6f),
                letterSpacing = 0.3.sp
            )
        }

        // Stop button
        TextButton(
            onClick = onStop,
            colors = ButtonDefaults.textButtonColors(
                contentColor = RestDesignSystem.Colors.InkSubtle
            )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Stop",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Stop",
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
private fun BreathingVisualization(
    type: BreathingType,
    progress: Float,
    currentPhaseIndex: Int
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val shapeSize = size.minDimension * 0.6f

        // Draw the shape path
        val path = when (type) {
            BreathingType.BOX -> createSquarePath(center, shapeSize)
            BreathingType.EXTENDED_EXHALE -> createRectanglePath(center, shapeSize)
            BreathingType.FOUR_SEVEN_EIGHT -> createTrianglePath(center, shapeSize)
        }

        // Draw shape outline
        drawPath(
            path = path,
            color = RestDesignSystem.Colors.Primary.copy(alpha = 0.2f).toArgb().let { Color(it) },
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // Calculate dot position
        val dotPosition = calculateDotPosition(type, currentPhaseIndex, progress, center, shapeSize)

        // Draw moving dot with glow
        drawCircle(
            color = RestDesignSystem.Colors.Primary.copy(alpha = 0.3f).toArgb().let { Color(it) },
            radius = 12f,
            center = dotPosition
        )
        drawCircle(
            color = RestDesignSystem.Colors.Primary.toArgb().let { Color(it) },
            radius = 6f,
            center = dotPosition
        )
    }
}

private fun createSquarePath(center: Offset, size: Float): androidx.compose.ui.graphics.Path {
    return androidx.compose.ui.graphics.Path().apply {
        val halfSize = size / 2f
        moveTo(center.x - halfSize, center.y - halfSize)
        lineTo(center.x + halfSize, center.y - halfSize)
        lineTo(center.x + halfSize, center.y + halfSize)
        lineTo(center.x - halfSize, center.y + halfSize)
        close()
    }
}

private fun createRectanglePath(center: Offset, size: Float): androidx.compose.ui.graphics.Path {
    return androidx.compose.ui.graphics.Path().apply {
        val width = size * 0.7f
        val height = size
        moveTo(center.x - width / 2f, center.y - height / 2f)
        lineTo(center.x + width / 2f, center.y - height / 2f)
        lineTo(center.x + width / 2f, center.y + height / 2f)
        lineTo(center.x - width / 2f, center.y + height / 2f)
        close()
    }
}

private fun createTrianglePath(center: Offset, size: Float): androidx.compose.ui.graphics.Path {
    return androidx.compose.ui.graphics.Path().apply {
        val halfSize = size / 2f
        moveTo(center.x, center.y - halfSize)
        lineTo(center.x + halfSize, center.y + halfSize)
        lineTo(center.x - halfSize, center.y + halfSize)
        close()
    }
}

private fun calculateDotPosition(
    type: BreathingType,
    phaseIndex: Int,
    progress: Float,
    center: Offset,
    shapeSize: Float
): Offset {
    return when (type) {
        BreathingType.BOX -> {
            val halfSize = shapeSize / 2f
            when (phaseIndex) {
                0 -> Offset(center.x - halfSize + progress * shapeSize, center.y - halfSize) // Top edge
                1 -> Offset(center.x + halfSize, center.y - halfSize + progress * shapeSize) // Right edge
                2 -> Offset(center.x + halfSize - progress * shapeSize, center.y + halfSize) // Bottom edge
                else -> Offset(center.x - halfSize, center.y + halfSize - progress * shapeSize) // Left edge
            }
        }
        BreathingType.EXTENDED_EXHALE -> {
            val width = shapeSize * 0.7f
            val height = shapeSize
            when (phaseIndex) {
                0 -> Offset(center.x - width / 2f + progress * width, center.y - height / 2f)
                1 -> Offset(center.x + width / 2f, center.y - height / 2f + progress * height * 0.2f)
                2 -> Offset(center.x + width / 2f - progress * width, center.y + height / 2f)
                else -> Offset(center.x - width / 2f, center.y + height / 2f - progress * height * 0.2f)
            }
        }
        BreathingType.FOUR_SEVEN_EIGHT -> {
            val halfSize = shapeSize / 2f
            when (phaseIndex) {
                0 -> { // Inhale - left to top
                    val startX = center.x - halfSize
                    val startY = center.y + halfSize
                    val endX = center.x
                    val endY = center.y - halfSize
                    Offset(startX + (endX - startX) * progress, startY + (endY - startY) * progress)
                }
                1 -> { // Hold - stay at top
                    Offset(center.x, center.y - halfSize)
                }
                else -> { // Exhale - top to right
                    val startX = center.x
                    val startY = center.y - halfSize
                    val endX = center.x + halfSize
                    val endY = center.y + halfSize
                    Offset(startX + (endX - startX) * progress, startY + (endY - startY) * progress)
                }
            }
        }
    }
}
