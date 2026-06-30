package com.screentracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class NightMessageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val message = intent.getStringExtra("message") ?: "Rest."

        setContent {
            NightMessageScreen(message)
        }
    }

    @Composable
    private fun NightMessageScreen(message: String) {
        // Dark theme matching the app
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = RestDesignSystem.Colors.Primary,
                background = RestDesignSystem.Colors.Canvas,
                surface = RestDesignSystem.Colors.Surface1,
                onSurface = RestDesignSystem.Colors.Ink
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RestDesignSystem.Colors.Canvas)
                    .clickable(
                        interactionSource = MutableInteractionSource(),
                        indication = null
                    ) { finish() },
                contentAlignment = Alignment.Center
            ) {
                // Subtle radial glow (same as main app)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.4f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    RestDesignSystem.Colors.Primary.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                radius = 600f
                            )
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 48.dp)
                ) {
                    // Minimal moon icon (matching timeline style)
                    Text(
                        text = "◐",
                        fontSize = 72.sp,
                        color = RestDesignSystem.Colors.Primary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Message text
                    Text(
                        text = message,
                        fontSize = RestDesignSystem.Typography.Subhead.fontSize,
                        fontWeight = RestDesignSystem.Typography.Subhead.fontWeight,
                        color = RestDesignSystem.Colors.Ink,
                        textAlign = TextAlign.Center,
                        lineHeight = (RestDesignSystem.Typography.Subhead.fontSize.value * 1.5).sp,
                        letterSpacing = RestDesignSystem.Typography.Subhead.letterSpacing
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // Subtle hint to close
                    Text(
                        text = "Tap anywhere to close",
                        fontSize = RestDesignSystem.Typography.Caption.fontSize,
                        color = RestDesignSystem.Colors.InkSubtle.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
