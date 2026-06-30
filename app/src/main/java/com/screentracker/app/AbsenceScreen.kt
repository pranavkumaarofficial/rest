package com.screentracker.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Screen shown when user hasn't started a sleep session in 3+ days.
 * Offers two paths: "I've been okay" → graduation, or "Rough patch" → back to app.
 *
 * Design: Never sends a notification to pull user back. Passive only.
 */
@Composable
fun AbsenceScreen(
    daysSinceLastSession: Int,
    onOkayPath: () -> Unit,
    onRoughPatchPath: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Moon emoji
        Text(
            text = "🌙",
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Gentle check-in message
        Text(
            text = "You haven't needed me for a few nights.",
            fontSize = 20.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "If you've been sleeping easier — that might be the best sign there is, and you may not need me much longer.",
            fontSize = 16.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "If it's been a rough patch instead, no judgment, I'm here.",
            fontSize = 16.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Two paths
        Button(
            onClick = onOkayPath,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("I've been okay")
        }

        OutlinedButton(
            onClick = onRoughPatchPath,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Rough patch")
        }
    }
}

/**
 * Graduation screen shown when user selects "I've been okay".
 * Final off-ramp message.
 */
@Composable
fun GraduationScreen(
    onContinueAnyway: () -> Unit,
    onUnderstand: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✨",
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "That's wonderful.",
            fontSize = 24.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "The strongest version of this is the phone sleeping in another room, and you not needing an app at all.",
            fontSize = 16.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "When you're ready, that's the goal — not me.",
            fontSize = 16.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Two options
        Button(
            onClick = onUnderstand,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("I understand")
        }

        OutlinedButton(
            onClick = onContinueAnyway,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue using REST anyway")
        }
    }
}

/**
 * Utility to check if absence screen should be shown.
 * Returns number of days since last session, or null if recent session exists.
 */
fun shouldShowAbsenceScreen(sessions: List<SleepSession>): Int? {
    val lastSession = sessions.firstOrNull() ?: return null
    val daysSince = ((System.currentTimeMillis() - lastSession.startTime) / (24 * 60 * 60 * 1000)).toInt()

    return if (daysSince >= 3) daysSince else null
}
