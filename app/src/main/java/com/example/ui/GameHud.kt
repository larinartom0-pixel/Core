package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameWorld
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.isActive
import kotlin.math.max

@Composable
fun GameHud(
    coreResources: Map<Resource, Int>,
    droneState: DroneState,
    blueprintsCount: Int,
    blueprintsCopperCost: Int,
    blueprintsLeadCost: Int,
    blueprintsSilverCost: Int,
    blueprintsIronCost: Int,
    markedForDemolishCount: Int,
    selectedTool: BuildingType?,
    isDemolishMode: Boolean,
    conveyorDirection: Direction,
    selectedCategory: BuildingCategory,
    fabricatorRecipe: String,
    waveNumber: Int,
    waveCountdownSeconds: Float,
    world: GameWorld,
    cameraOffsetX: Float,
    cameraOffsetY: Float,
    zoom: Float,
    screenWidth: Float,
    screenHeight: Float,
    onSelectCategory: (BuildingCategory) -> Unit,
    onSelectTool: (BuildingType?) -> Unit,
    onToggleDemolishMode: () -> Unit,
    onRotateConveyor: () -> Unit,
    onConfirmBlueprints: () -> Unit,
    onCancelBlueprints: () -> Unit,
    onConfirmDemolish: () -> Unit,
    onCancelDemolish: () -> Unit,
    onSelectFabricatorRecipe: (String) -> Unit,
    onMiniMapTap: (Float, Float) -> Unit,
    onCenterOnCore: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copperCount = coreResources[Resource.COPPER] ?: 0
    val leadCount = coreResources[Resource.LEAD] ?: 0
    val coalCount = coreResources[Resource.COAL] ?: 0
    val silverCount = coreResources[Resource.SILVER] ?: 0
    val ironIngotCount = coreResources[Resource.IRON_INGOT] ?: 0
    val copperAmmoCount = coreResources[Resource.COPPER_AMMO] ?: 0
    val silverAmmoCount = coreResources[Resource.SILVER_AMMO] ?: 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- TOP BAR: Multi-Resource Inventory, Wave Status & Settings ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Multi-Resource Badge Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IndustrialSurface.copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialBorder),
                    modifier = Modifier.height(38.dp)
                ) {
                    LazyRow(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { ResourceBadge(label = "Cu", count = copperCount, color = CopperAmber) }
                        item { ResourceBadge(label = "Pb", count = leadCount, color = Color(0xFF8B5CF6)) }
                        item { ResourceBadge(label = "C", count = coalCount, color = Color(0xFF94A3B8)) }
                        item { ResourceBadge(label = "Ag", count = silverCount, color = Color(0xFFE2E8F0)) }
                        item { ResourceBadge(label = "Fe", count = ironIngotCount, color = Color(0xFFCBD5E1)) }
                        if (copperAmmoCount > 0) {
                            item { ResourceBadge(label = "Cu-Пат", count = copperAmmoCount, color = Color(0xFFFB923C)) }
                        }
                        if (silverAmmoCount > 0) {
                            item { ResourceBadge(label = "Ag-Пат", count = silverAmmoCount, color = Color(0xFF38BDF8)) }
                        }
                    }
                }

                // Top Right Utility Buttons (Center on core & Settings)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCenterOnCore,
                        modifier = Modifier
                            .size(36.dp)
                            .background(IndustrialSurface.copy(alpha = 0.92f), CircleShape)
                            .border(1.dp, IndustrialBorder, CircleShape)
                            .testTag("btn_center_core")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "До Ядра",
                            tint = CoreCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(36.dp)
                            .background(IndustrialSurface.copy(alpha = 0.92f), CircleShape)
                            .border(1.dp, IndustrialBorder, CircleShape)
                            .testTag("btn_open_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Налаштування",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Wave Indicator Pill
            val minutes = (waveCountdownSeconds / 60).toInt()
            val seconds = (waveCountdownSeconds % 60).toInt()
            val timeFormatted = String.format("%02d:%02d", minutes, seconds)
            val isImminent = waveCountdownSeconds < 25f

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isImminent) Color(0xDD7F1D1D) else IndustrialSurface.copy(alpha = 0.88f),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isImminent) Color(0xFFEF4444) else IndustrialBorder),
                modifier = Modifier
                    .align(Alignment.Start)
                    .height(26.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isImminent) Color(0xFFEF4444) else CopperAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Хвиля $waveNumber: $timeFormatted",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (world.units.count { it.isAlive && it.isEnemy } > 0) {
                        Text(
                            text = "• Ворогів: ${world.units.count { it.isAlive && it.isEnemy }}",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- MINI-MAP (Top-Right Corner) ---
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 8.dp)
        ) {
            MiniMap(
                world = world,
                cameraOffsetX = cameraOffsetX,
                cameraOffsetY = cameraOffsetY,
                zoom = zoom,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                onTapCoordinates = onMiniMapTap,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IndustrialSurface.copy(alpha = 0.90f))
                    .border(1.dp, IndustrialBorder, RoundedCornerShape(8.dp))
            )
        }

        // --- FLOATING CONTROLS: Zoom & Rotate (Left Side) ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier
                    .size(38.dp)
                    .background(IndustrialSurface.copy(alpha = 0.92f), CircleShape)
                    .border(1.dp, IndustrialBorder, CircleShape)
                    .testTag("btn_zoom_in")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Збільшити", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            IconButton(
                onClick = onZoomOut,
                modifier = Modifier
                    .size(38.dp)
                    .background(IndustrialSurface.copy(alpha = 0.92f), CircleShape)
                    .border(1.dp, IndustrialBorder, CircleShape)
                    .testTag("btn_zoom_out")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Зменшити", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            if (selectedTool == BuildingType.CONVEYOR) {
                IconButton(
                    onClick = onRotateConveyor,
                    modifier = Modifier
                        .size(38.dp)
                        .background(IndustrialSurface.copy(alpha = 0.92f), CircleShape)
                        .border(1.dp, CopperAmber, CircleShape)
                        .testTag("btn_rotate_conveyor")
                ) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Повернути", tint = CopperAmber, modifier = Modifier.size(20.dp))
                }
            }
        }

        // --- BOTTOM DOCK: Compact Building Info & Categorized Building Selector ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Compact Info Window (Non-intrusive floating strip)
            if (selectedTool != null) {
                CompactBuildingInfoCard(
                    buildingType = selectedTool,
                    fabricatorRecipe = fabricatorRecipe,
                    onSelectFabricatorRecipe = onSelectFabricatorRecipe,
                    onClose = { onSelectTool(null) }
                )
            }

            // 2. Blueprint / Demolish Confirmation Actions
            if (blueprintsCount > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IndustrialSurface.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoreCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Креслень: $blueprintsCount | Вартість: ${blueprintsCopperCost}Cu" +
                                    (if (blueprintsLeadCost > 0) " ${blueprintsLeadCost}Pb" else "") +
                                    (if (blueprintsSilverCost > 0) " ${blueprintsSilverCost}Ag" else "") +
                                    (if (blueprintsIronCost > 0) " ${blueprintsIronCost}Fe" else ""),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onCancelBlueprints,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Скасувати", color = Color(0xFFEF4444), fontSize = 12.sp)
                            }
                            Button(
                                onClick = onConfirmBlueprints,
                                colors = ButtonDefaults.buttonColors(containerColor = CoreCyan),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Будувати", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (markedForDemolishCount > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IndustrialSurface.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "На знесення: $markedForDemolishCount (100% повернення)",
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onCancelDemolish,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Скасувати", color = Color.White, fontSize = 12.sp)
                            }
                            Button(
                                onClick = onConfirmDemolish,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Знести", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Category Selector Tabs (Logistics, Mining, Power, Industry, Defense)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = IndustrialSurface.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryTabButton("Логістика", BuildingCategory.LOGISTICS, selectedCategory == BuildingCategory.LOGISTICS && !isDemolishMode) {
                        onSelectCategory(BuildingCategory.LOGISTICS)
                    }
                    CategoryTabButton("Видобуток", BuildingCategory.MINING, selectedCategory == BuildingCategory.MINING && !isDemolishMode) {
                        onSelectCategory(BuildingCategory.MINING)
                    }
                    CategoryTabButton("Енергія", BuildingCategory.POWER, selectedCategory == BuildingCategory.POWER && !isDemolishMode) {
                        onSelectCategory(BuildingCategory.POWER)
                    }
                    CategoryTabButton("Заводи", BuildingCategory.INDUSTRY, selectedCategory == BuildingCategory.INDUSTRY && !isDemolishMode) {
                        onSelectCategory(BuildingCategory.INDUSTRY)
                    }
                    CategoryTabButton("Оборона", BuildingCategory.DEFENSE, selectedCategory == BuildingCategory.DEFENSE && !isDemolishMode) {
                        onSelectCategory(BuildingCategory.DEFENSE)
                    }
                    // Demolish Trash Tool
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDemolishMode) Color(0xFFEF4444) else Color.Transparent)
                            .clickable { onToggleDemolishMode() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Знесення",
                            tint = if (isDemolishMode) Color.White else Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 4. Building Icons Row (Custom Miniatures)
            val availableBuildings = BuildingType.values().filter { it.category == selectedCategory && it != BuildingType.CORE }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = IndustrialSurface.copy(alpha = 0.96f),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(availableBuildings) { building ->
                        val isSelected = selectedTool == building && !isDemolishMode
                        BuildingIconButton(
                            building = building,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelected) onSelectTool(null) else onSelectTool(building)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ----------------- SUB-COMPONENTS -----------------

@Composable
private fun CategoryTabButton(
    label: String,
    category: BuildingCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) CoreCyan.copy(alpha = 0.25f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) CoreCyan else Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun BuildingIconButton(
    building: BuildingType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) CoreCyan else IndustrialBorder
    val bgColor = if (isSelected) CoreCyan.copy(alpha = 0.20f) else IndustrialDark

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp)
            .width(54.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawMiniatureBuilding(building, size.width, size.height)
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = building.displayName,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) CoreCyan else Color.White,
            maxLines = 1
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMiniatureBuilding(
    building: BuildingType,
    w: Float,
    h: Float
) {
    when (building) {
        BuildingType.CONVEYOR -> {
            drawRect(Color(0xFF334155), Offset(0f, 0f), Size(w, h))
            drawRect(Color(0xFF1E293B), Offset(3f, 3f), Size(w - 6f, h - 6f))
            val path = Path().apply {
                moveTo(w / 2f, h * 0.25f)
                lineTo(w * 0.75f, h * 0.65f)
                lineTo(w * 0.25f, h * 0.65f)
                close()
            }
            drawPath(path, color = Color(0xFFF59E0B))
        }
        BuildingType.ROUTER -> {
            drawRect(Color(0xFF1E293B), Offset(0f, 0f), Size(w, h))
            drawRect(Color(0xFF475569), Offset(3f, 3f), Size(w - 6f, h - 6f))
            drawCircle(Color(0xFFF59E0B), w * 0.25f, Offset(w / 2f, h / 2f))
            drawCircle(Color(0xFF0F172A), w * 0.12f, Offset(w / 2f, h / 2f))
        }
        BuildingType.JUNCTION -> {
            drawRect(Color(0xFF1E293B), Offset(0f, 0f), Size(w, h))
            drawRect(Color(0xFFE2E8F0), Offset(w * 0.35f, 2f), Size(w * 0.3f, h - 4f))
            drawRect(Color(0xFFF59E0B), Offset(2f, h * 0.35f), Size(w - 4f, h * 0.3f))
        }
        BuildingType.COPPER_DRILL -> {
            drawRect(Color(0xFF78350F), Offset(0f, 0f), Size(w, h))
            drawRect(Color(0xFFB45309), Offset(4f, 4f), Size(w - 8f, h - 8f))
            drawCircle(Color(0xFFF59E0B), w * 0.28f, Offset(w / 2f, h / 2f))
            drawCircle(Color(0xFFFDE68A), w * 0.14f, Offset(w / 2f, h / 2f))
        }
        BuildingType.COAL_GENERATOR -> {
            drawRect(Color(0xFF1E293B), Offset(0f, 0f), Size(w, h))
            drawRect(Color(0xFFF97316), Offset(w * 0.25f, h * 0.35f), Size(w * 0.5f, h * 0.45f))
            drawCircle(Color(0xFFFDE047), w * 0.16f, Offset(w / 2f, h * 0.55f))
            drawCircle(Color(0xFF0F172A), w * 0.12f, Offset(w / 2f, h * 0.20f))
        }
        BuildingType.POWER_NODE -> {
            drawRect(Color(0xFF0F172A), Offset(0f, 0f), Size(w, h))
            val path = Path().apply {
                moveTo(w / 2f, h * 0.15f)
                lineTo(w * 0.85f, h / 2f)
                lineTo(w / 2f, h * 0.85f)
                lineTo(w * 0.15f, h / 2f)
                close()
            }
            drawPath(path, color = Color(0xFFFDE047), style = Stroke(width = 2f))
            drawCircle(Color(0xFFFACC15), w * 0.2f, Offset(w / 2f, h / 2f))
        }
        BuildingType.BATTERY -> {
            drawRect(Color(0xFF0F172A), Offset(0f, 0f), Size(w, h))
            drawRect(Color(0xFF10B981), Offset(4f, h * 0.35f), Size(w - 8f, h * 0.5f))
            drawRect(Color(0xFF8B5CF6), Offset(0f, 0f), Size(w, h), style = Stroke(width = 1.5f))
        }
        BuildingType.TURRET -> {
            drawRect(Color(0xFF0F172A), Offset(0f, 0f), Size(w, h))
            drawRect(Color(0xFFF97316), Offset(w / 2f, h * 0.35f), Size(w * 0.42f, h * 0.12f))
            drawRect(Color(0xFFF97316), Offset(w / 2f, h * 0.55f), Size(w * 0.42f, h * 0.12f))
            drawCircle(Color(0xFF64748B), w * 0.32f, Offset(w / 2f, h / 2f))
            drawCircle(Color(0xFFF1F5F9), w * 0.16f, Offset(w / 2f, h / 2f))
        }
        BuildingType.IRON_SMELTER -> {
            drawRect(Color(0xFF431407), Offset(0f, 0f), Size(w, h))
            drawCircle(Color(0xFFEA580C), w * 0.32f, Offset(w / 2f, h / 2f))
            drawCircle(Color(0xFFFDE047), w * 0.16f, Offset(w / 2f, h / 2f))
        }
        BuildingType.AMMO_FABRICATOR -> {
            drawRect(Color(0xFF0F172A), Offset(0f, 0f), Size(w, h))
            drawRect(Color(0xFF38BDF8), Offset(w * 0.2f, h * 0.2f), Size(w * 0.6f, h * 0.6f))
            drawCircle(Color(0xFF0F172A), w * 0.18f, Offset(w / 2f, h / 2f))
        }
        BuildingType.CORE -> {}
    }
}

@Composable
private fun CompactBuildingInfoCard(
    buildingType: BuildingType,
    fabricatorRecipe: String,
    onSelectFabricatorRecipe: (String) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = IndustrialSurface.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, CoreCyan.copy(alpha = 0.8f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${buildingType.displayName} (${buildingType.width}x${buildingType.height})",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Resource costs chips
                    if (buildingType.costCopper > 0) CostChip("${buildingType.costCopper}Cu", CopperAmber)
                    if (buildingType.costLead > 0) CostChip("${buildingType.costLead}Pb", Color(0xFF8B5CF6))
                    if (buildingType.costSilver > 0) CostChip("${buildingType.costSilver}Ag", Color(0xFFE2E8F0))
                    if (buildingType.costIronIngot > 0) CostChip("${buildingType.costIronIngot}Fe", Color(0xFFCBD5E1))
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = buildingType.description,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    maxLines = 2
                )

                // If Ammo Fabricator, offer recipe toggle buttons
                if (buildingType == BuildingType.AMMO_FABRICATOR) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Рецепт:", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (fabricatorRecipe == "copper") Color(0xFFF97316) else Color(0xFF334155))
                                .clickable { onSelectFabricatorRecipe("copper") }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Мідні (10 урон)", color = Color.White, fontSize = 9.sp)
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (fabricatorRecipe == "silver") Color(0xFF38BDF8) else Color(0xFF334155))
                                .clickable { onSelectFabricatorRecipe("silver") }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Срібні (24 урон)", color = Color(0xFF0F172A), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Закрити", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CostChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun ResourceBadge(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Text(
            text = "$label: $count",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MiniMap(
    world: GameWorld,
    cameraOffsetX: Float,
    cameraOffsetY: Float,
    zoom: Float,
    screenWidth: Float,
    screenHeight: Float,
    onTapCoordinates: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var miniMapFrame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { frameNanos ->
                miniMapFrame = frameNanos
            }
        }
    }

    Canvas(
        modifier = modifier.pointerInput(world.width, world.height) {
            detectTapGestures { offset ->
                val tapWorldX = (offset.x / size.width) * world.width
                val tapWorldY = (offset.y / size.height) * world.height
                onTapCoordinates(tapWorldX, tapWorldY)
            }
        }
    ) {
        @Suppress("UNUSED_VARIABLE")
        val _frame = miniMapFrame

        val mapW = size.width
        val mapH = size.height

        // Background
        drawRect(Color(0xFF0B0F19), Offset(0f, 0f), Size(mapW, mapH))

        // Buildings (green pixels)
        for (b in world.buildings) {
            val bx = (b.originX.toFloat() / world.width) * mapW
            val by = (b.originY.toFloat() / world.height) * mapH
            val bw = max(2f, (b.type.width.toFloat() / world.width) * mapW)
            val bh = max(2f, (b.type.height.toFloat() / world.height) * mapH)

            val col = if (b.type == BuildingType.CORE) CoreCyan else Color(0xFF10B981)
            drawRect(col, Offset(bx, by), Size(bw, bh))
        }

        // Enemies (red dots)
        for (u in world.units) {
            if (u.isAlive && u.isEnemy) {
                val ux = (u.posX / world.width) * mapW
                val uy = (u.posY / world.height) * mapH
                drawCircle(Color(0xFFEF4444), 2.5f, Offset(ux, uy))
            }
        }

        // Builder Drone (cyan dot)
        val dx = (world.drone.posX / world.width) * mapW
        val dy = (world.drone.posY / world.height) * mapH
        drawCircle(Color(0xFF38BDF8), 2f, Offset(dx, dy))

        // Camera Viewport Box (cyan stroke)
        val baseTileSize = 48f
        val currentTileSize = baseTileSize * zoom
        val viewWorldLeft = ((-cameraOffsetX) / currentTileSize).coerceAtLeast(0f)
        val viewWorldTop = ((-cameraOffsetY) / currentTileSize).coerceAtLeast(0f)
        val viewWorldRight = ((screenWidth - cameraOffsetX) / currentTileSize).coerceAtMost(world.width.toFloat())
        val viewWorldBottom = ((screenHeight - cameraOffsetY) / currentTileSize).coerceAtMost(world.height.toFloat())

        val boxX = (viewWorldLeft / world.width) * mapW
        val boxY = (viewWorldTop / world.height) * mapH
        val boxW = ((viewWorldRight - viewWorldLeft) / world.width) * mapW
        val boxH = ((viewWorldBottom - viewWorldTop) / world.height) * mapH

        drawRect(
            color = Color(0xFF38BDF8),
            topLeft = Offset(boxX, boxY),
            size = Size(boxW.coerceAtLeast(4f), boxH.coerceAtLeast(4f)),
            style = Stroke(width = 1.2f)
        )
    }
}
