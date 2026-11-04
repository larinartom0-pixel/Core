package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
    coreResources: Map<Resource, Int>,
    droneState: DroneState,
    blueprintsCount: Int,
    blueprintsCopperCost: Int,
    blueprintsSilverCost: Int,
    markedForDemolishCount: Int,
    selectedTool: BuildingType?,
    isDemolishMode: Boolean,
    conveyorDirection: Direction,
    onSelectTool: (BuildingType?) -> Unit,
    onToggleDemolishMode: () -> Unit,
    onRotateConveyor: () -> Unit,
    onConfirmBlueprints: () -> Unit,
    onCancelBlueprints: () -> Unit,
    onConfirmDemolish: () -> Unit,
    onCancelDemolish: () -> Unit,
    onCenterOnCore: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copperCount = coreResources[Resource.COPPER] ?: 0
    val silverCount = coreResources[Resource.SILVER] ?: 0
    val leadCount = coreResources[Resource.LEAD] ?: 0
    val coalCount = coreResources[Resource.COAL] ?: 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- TOP BAR: Multi-Resource Inventory, Drone Status & Actions ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Resource Counter Card (Mindustry style mineral badges)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = IndustrialSurface.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialBorder),
                modifier = Modifier.height(44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Copper
                    ResourceBadge(label = "Cu", count = copperCount, color = CopperAmber)
                    // Silver
                    ResourceBadge(label = "Ag", count = silverCount, color = Color(0xFFE2E8F0))
                    // Lead
                    ResourceBadge(label = "Pb", count = leadCount, color = Color(0xFF8B5CF6))
                    // Coal
                    ResourceBadge(label = "C", count = coalCount, color = Color(0xFF64748B))

                    Spacer(modifier = Modifier.width(4.dp))

                    // Drone status pill
                    val (droneColor, droneStatusText) = when (droneState) {
                        DroneState.IDLE_AT_CORE -> Pair(CoreCyan, "Дрон: Вільний")
                        DroneState.FLYING_TO_BUILD -> Pair(CopperAmber, "Дрон: Летить")
                        DroneState.BUILDING -> Pair(TechGreen, "Дрон: Будує")
                        DroneState.FLYING_TO_DECONSTRUCT -> Pair(AlertRed, "Дрон: Летить")
                        DroneState.DECONSTRUCTING -> Pair(AlertRed, "Дрон: Зносить")
                        DroneState.RETURNING_TO_CORE -> Pair(CoreCyan, "Дрон: База")
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(droneColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = droneStatusText,
                            color = droneColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Top-right action controls
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onCenterOnCore,
                    modifier = Modifier
                        .testTag("center_core_btn")
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IndustrialSurface.copy(alpha = 0.92f))
                        .border(1.dp, IndustrialBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Центрувати на Ядро",
                        tint = CoreCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .testTag("open_settings_btn")
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IndustrialSurface.copy(alpha = 0.92f))
                        .border(1.dp, IndustrialBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Налаштування",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- SMOOTH ZOOM CONTROLS (Floating Left, Centered Zoom) ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier
                    .testTag("zoom_in_btn")
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(IndustrialSurface.copy(alpha = 0.94f))
                    .border(1.dp, IndustrialBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Збільшити масштаб", tint = Color.White)
            }
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier
                    .testTag("zoom_out_btn")
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(IndustrialSurface.copy(alpha = 0.94f))
                    .border(1.dp, IndustrialBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Зменшити масштаб", tint = Color.White)
            }
        }

        // --- RIGHT SIDE: COMPACT BUILDING INFO PANEL (when a building is selected) ---
        AnimatedVisibility(
            visible = selectedTool != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
        ) {
            if (selectedTool != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = IndustrialSurface.copy(alpha = 0.96f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CoreCyan),
                    modifier = Modifier.widthIn(max = 230.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedTool.displayName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            IconButton(
                                onClick = { onSelectTool(null) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Закрити", tint = SlateDim, modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(
                            text = "Розмір: ${selectedTool.width}x${selectedTool.height}",
                            color = SlateDim,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Cost
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${selectedTool.costCopper} Cu",
                                color = CopperAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            if (selectedTool.costSilver > 0) {
                                Text(
                                    text = "${selectedTool.costSilver} Ag",
                                    color = Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = selectedTool.description,
                            color = SlateLight,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )

                        // Conveyor direction rotate button
                        if (selectedTool == BuildingType.CONVEYOR) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onRotateConveyor,
                                colors = ButtonDefaults.buttonColors(containerColor = CoreCyan, contentColor = IndustrialDark),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                val arrowIcon = when (conveyorDirection) {
                                    Direction.NORTH -> Icons.Default.ArrowUpward
                                    Direction.EAST -> Icons.AutoMirrored.Filled.ArrowForward
                                    Direction.SOUTH -> Icons.Default.ArrowDownward
                                    Direction.WEST -> Icons.AutoMirrored.Filled.ArrowBack
                                }
                                Icon(arrowIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Повернути", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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
                .padding(bottom = 74.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = IndustrialSurface.copy(alpha = 0.96f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, TechGreen),
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Креслеників: $blueprintsCount",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Вартість: $blueprintsCopperCost Cu" + if (blueprintsSilverCost > 0) ", $blueprintsSilverCost Ag" else "",
                            color = if (copperCount >= blueprintsCopperCost && silverCount >= blueprintsSilverCost) CopperAmber else AlertRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onConfirmBlueprints,
                        colors = ButtonDefaults.buttonColors(containerColor = TechGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_build_btn").height(40.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Побудувати", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onCancelBlueprints,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("cancel_build_btn").height(40.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // --- DEMOLISH CONFIRMATION BAR ---
        AnimatedVisibility(
            visible = markedForDemolishCount > 0,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 74.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = IndustrialSurface.copy(alpha = 0.96f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AlertRed),
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Знесення: $markedForDemolishCount об'єктів",
                            color = AlertRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Повернення 100% ресурсів",
                            color = TechGreen,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onConfirmDemolish,
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_demolish_btn").height(40.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Знести", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onCancelDemolish,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateDim),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("cancel_demolish_btn").height(40.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // --- BOTTOM-RIGHT CORNER COMPACT BUILD & DEMOLISH BAR ---
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = IndustrialSurface.copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialBorder),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Conveyor
                CornerToolIconButton(
                    icon = Icons.Default.SyncAlt,
                    label = "Конвеєр",
                    isSelected = selectedTool == BuildingType.CONVEYOR,
                    activeColor = CoreCyan,
                    tag = "tool_conveyor_btn",
                    onClick = {
                        if (selectedTool == BuildingType.CONVEYOR) onSelectTool(null) else onSelectTool(BuildingType.CONVEYOR)
                    }
                )

                // 2. Router (Маршрутизатор)
                CornerToolIconButton(
                    icon = Icons.Default.CallSplit,
                    label = "Маршрутизатор",
                    isSelected = selectedTool == BuildingType.ROUTER,
                    activeColor = CoreCyan,
                    tag = "tool_router_btn",
                    onClick = {
                        if (selectedTool == BuildingType.ROUTER) onSelectTool(null) else onSelectTool(BuildingType.ROUTER)
                    }
                )

                // 3. Junction (Перехрестя)
                CornerToolIconButton(
                    icon = Icons.Default.Shuffle,
                    label = "Перехрестя",
                    isSelected = selectedTool == BuildingType.JUNCTION,
                    activeColor = Color(0xFFE2E8F0),
                    tag = "tool_junction_btn",
                    onClick = {
                        if (selectedTool == BuildingType.JUNCTION) onSelectTool(null) else onSelectTool(BuildingType.JUNCTION)
                    }
                )

                // 4. Copper Drill (Мідний бур)
                CornerToolIconButton(
                    icon = Icons.Default.PrecisionManufacturing,
                    label = "Мідний бур",
                    isSelected = selectedTool == BuildingType.COPPER_DRILL,
                    activeColor = CopperAmber,
                    tag = "tool_drill_btn",
                    onClick = {
                        if (selectedTool == BuildingType.COPPER_DRILL) onSelectTool(null) else onSelectTool(BuildingType.COPPER_DRILL)
                    }
                )

                // 5. Demolish (Мусорка)
                CornerToolIconButton(
                    icon = Icons.Default.Delete,
                    label = "Знесення",
                    isSelected = isDemolishMode,
                    activeColor = AlertRed,
                    tag = "tool_demolish_btn",
                    onClick = onToggleDemolishMode
                )
            }
        }
    }
}

@Composable
private fun ResourceBadge(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label: $count",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun CornerToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    val bg = if (isSelected) activeColor.copy(alpha = 0.22f) else IndustrialCard
    val border = if (isSelected) activeColor else IndustrialBorder

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .testTag(tag)
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(10.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}
