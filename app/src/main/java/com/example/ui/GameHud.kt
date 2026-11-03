package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BuildingType
import com.example.model.Direction
import com.example.model.DroneState
import com.example.model.Resource
import com.example.ui.theme.*

@Composable
fun GameHud(
    copperCount: Int,
    droneState: DroneState,
    constructionQueueCount: Int,
    blueprintsCount: Int,
    blueprintsCost: Int,
    selectedTool: BuildingType?,
    conveyorDirection: Direction,
    onSelectTool: (BuildingType?) -> Unit,
    onRotateConveyor: () -> Unit,
    onConfirmBlueprints: () -> Unit,
    onCancelBlueprints: () -> Unit,
    onCenterOnCore: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onPanBy: (Float, Float) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- TOP BAR: Resources, Drone Status & Actions ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Resource Counter Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = IndustrialSurface.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialBorder),
                modifier = Modifier.height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(CopperAmber)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Мідь: $copperCount",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Drone status pill
                    val droneColor = when (droneState) {
                        DroneState.IDLE_AT_CORE -> CoreCyan
                        DroneState.FLYING_TO_BUILD -> CopperAmber
                        DroneState.BUILDING -> TechGreen
                        DroneState.RETURNING_TO_CORE -> CoreCyan
                    }
                    val droneStatusText = when (droneState) {
                        DroneState.IDLE_AT_CORE -> "Дрон: Вільний"
                        DroneState.FLYING_TO_BUILD -> "Дрон: Летить"
                        DroneState.BUILDING -> "Дрон: Будує"
                        DroneState.RETURNING_TO_CORE -> "Дрон: Повернення"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(droneColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = droneStatusText,
                            color = droneColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Top-right action controls
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Mode indicator chip
                Surface(
                    onClick = {
                        if (selectedTool != null) onSelectTool(null)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedTool != null) CoreCyan.copy(alpha = 0.2f) else IndustrialSurface.copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedTool != null) CoreCyan else IndustrialBorder
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedTool != null) Icons.Default.Build else Icons.Default.PanTool,
                            contentDescription = null,
                            tint = if (selectedTool != null) CoreCyan else SlateDim,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedTool != null) "Будівництво" else "Огляд",
                            color = if (selectedTool != null) CoreCyan else SlateLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onCenterOnCore,
                    modifier = Modifier
                        .testTag("center_core_btn")
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(IndustrialSurface.copy(alpha = 0.92f))
                        .border(1.dp, IndustrialBorder, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Центрувати на Ядро",
                        tint = CoreCyan
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .testTag("open_settings_btn")
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(IndustrialSurface.copy(alpha = 0.92f))
                        .border(1.dp, IndustrialBorder, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Налаштування",
                        tint = Color.White
                    )
                }
            }
        }

        // --- NAVIGATION D-PAD & ZOOM (Floating Left) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom controls
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier
                        .testTag("zoom_in_btn")
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IndustrialSurface.copy(alpha = 0.92f))
                        .border(1.dp, IndustrialBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Збільшити масштаб", tint = Color.White)
                }
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier
                        .testTag("zoom_out_btn")
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IndustrialSurface.copy(alpha = 0.92f))
                        .border(1.dp, IndustrialBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Зменшити масштаб", tint = Color.White)
                }
            }

            // Directional Pan D-Pad
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = IndustrialSurface.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialBorder)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(2.dp)
                ) {
                    IconButton(
                        onClick = { onPanBy(0f, 160f) },
                        modifier = Modifier.size(36.dp).testTag("pan_up_btn")
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Вгору", tint = CoreCyan)
                    }
                    Row {
                        IconButton(
                            onClick = { onPanBy(160f, 0f) },
                            modifier = Modifier.size(36.dp).testTag("pan_left_btn")
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Вліво", tint = CoreCyan)
                        }
                        IconButton(
                            onClick = { onPanBy(-160f, 0f) },
                            modifier = Modifier.size(36.dp).testTag("pan_right_btn")
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Вправо", tint = CoreCyan)
                        }
                    }
                    IconButton(
                        onClick = { onPanBy(0f, -160f) },
                        modifier = Modifier.size(36.dp).testTag("pan_down_btn")
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Вниз", tint = CoreCyan)
                    }
                }
            }
        }

        // --- BLUEPRINT CONFIRMATION BAR (Floating Center Bottom) ---
        AnimatedVisibility(
            visible = blueprintsCount > 0,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = IndustrialSurface.copy(alpha = 0.96f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, TechGreen),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Креслеників: $blueprintsCount",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Вартість: $blueprintsCost Міді",
                            color = if (copperCount >= blueprintsCost) CopperAmber else AlertRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Confirm Build Button
                    Button(
                        onClick = onConfirmBlueprints,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TechGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .testTag("confirm_build_btn")
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Побудувати", fontWeight = FontWeight.Bold)
                    }

                    // Cancel Blueprints Button
                    OutlinedButton(
                        onClick = onCancelBlueprints,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .testTag("cancel_build_btn")
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // --- BOTTOM BUILD MENU (Right Side) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Conveyor Direction Rotation Button (visible when Conveyor tool is selected)
            AnimatedVisibility(
                visible = selectedTool == BuildingType.CONVEYOR,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                val arrowIcon = when (conveyorDirection) {
                    Direction.NORTH -> Icons.Default.ArrowUpward
                    Direction.EAST -> Icons.Default.ArrowForward
                    Direction.SOUTH -> Icons.Default.ArrowDownward
                    Direction.WEST -> Icons.Default.ArrowBack
                }
                val dirName = when (conveyorDirection) {
                    Direction.NORTH -> "Північ ⬆"
                    Direction.EAST -> "Схід ➡️"
                    Direction.SOUTH -> "Південь ⬇"
                    Direction.WEST -> "Захід ⬅"
                }

                Button(
                    onClick = onRotateConveyor,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoreCyan,
                        contentColor = IndustrialDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .testTag("rotate_conveyor_btn")
                        .height(48.dp)
                ) {
                    Icon(arrowIcon, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Напрямок: $dirName", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Building Selector Cards Row
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = IndustrialSurface.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialBorder),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Conveyor Item
                    BuildToolButton(
                        name = "Конвеєр",
                        subtext = "1x1 • 1 Cu",
                        icon = Icons.Default.SyncAlt,
                        isSelected = selectedTool == BuildingType.CONVEYOR,
                        tag = "build_conveyor_btn",
                        onClick = {
                            if (selectedTool == BuildingType.CONVEYOR) onSelectTool(null)
                            else onSelectTool(BuildingType.CONVEYOR)
                        }
                    )

                    // Copper Drill Item
                    BuildToolButton(
                        name = "Мідний бур",
                        subtext = "2x2 • 25 Cu",
                        icon = Icons.Default.Engineering,
                        isSelected = selectedTool == BuildingType.COPPER_DRILL,
                        tag = "build_drill_btn",
                        onClick = {
                            if (selectedTool == BuildingType.COPPER_DRILL) onSelectTool(null)
                            else onSelectTool(BuildingType.COPPER_DRILL)
                        }
                    )

                    // Close Tool Button (if a tool is active)
                    if (selectedTool != null) {
                        IconButton(
                            onClick = { onSelectTool(null) },
                            modifier = Modifier
                                .testTag("deselect_tool_btn")
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IndustrialCard)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Зняти виділення", tint = SlateDim)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildToolButton(
    name: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) CoreCyan else IndustrialBorder
    val bg = if (isSelected) CoreCyan.copy(alpha = 0.18f) else IndustrialCard

    Box(
        modifier = Modifier
            .testTag(tag)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = if (isSelected) CoreCyan else Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = name,
                    color = if (isSelected) CoreCyan else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = subtext,
                    color = CopperAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
