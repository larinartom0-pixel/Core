package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    showGrid: Boolean,
    onToggleGrid: (Boolean) -> Unit,
    showFlowArrows: Boolean,
    onToggleFlowArrows: (Boolean) -> Unit,
    soundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Pulsing and rotation animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val bgOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bgOffset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IndustrialDark)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Animated Sci-Fi Factory Background Grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 60f
            for (x in 0..(size.width / step).toInt()) {
                val posX = x * step
                drawLine(
                    color = Color(0x0C38BDF8),
                    start = Offset(posX, 0f),
                    end = Offset(posX, size.height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..(size.height / step).toInt()) {
                val posY = y * step + (bgOffset % step)
                drawLine(
                    color = Color(0x0C38BDF8),
                    start = Offset(0f, posY),
                    end = Offset(size.width, posY),
                    strokeWidth = 1f
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
        ) {
            // Core Reactor Animated Logo
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Outer rotating energy ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotation)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(CoreCyan, CopperAmber, Color(0xFF8B5CF6), CoreCyan)
                            ),
                            shape = CircleShape
                        )
                )

                // Inner core reactor
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color.White, CoreCyan, IndustrialDark)
                            )
                        )
                )

                Icon(
                    imageVector = Icons.Default.PrecisionManufacturing,
                    contentDescription = null,
                    tint = IndustrialDark,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = "CORE SECTOR",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "ПРОМИСЛОВЕ ЯДРО",
                color = CoreCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Feature Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeatureBadge("80x80 Карта", CoreCyan)
                FeatureBadge("Енергомережа", Color(0xFFFBBF24))
                FeatureBadge("Хвилі ворогів", Color(0xFFEF4444))
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Play Button
            Button(
                onClick = onStartGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoreCyan,
                    contentColor = IndustrialDark
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("play_game_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "НОВА ГРА",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Settings Button
            OutlinedButton(
                onClick = { showSettingsDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, IndustrialBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("main_settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = CoreCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "НАЛАШТУВАННЯ ТА ІНСТРУКЦІЯ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            showGrid = showGrid,
            onToggleGrid = onToggleGrid,
            showFlowArrows = showFlowArrows,
            onToggleFlowArrows = onToggleFlowArrows,
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onDismiss = { showSettingsDialog = false },
            onExitToMainMenu = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun FeatureBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
