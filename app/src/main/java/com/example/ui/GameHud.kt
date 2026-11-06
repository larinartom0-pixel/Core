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
import androidx.compose.material.icons.automirrored.filled.RotateRight
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
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    inspectedBuildingId: String? = null,
    onDismissInspector: () -> Unit = {},
    onSetBuildingRecipe: (String, String) -> Unit = { _, _ -> },
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copperCount = coreResources[Resource.COPPER] ?: 0
    val leadCount = coreResources[Resource.LEAD] ?: 0
    val coalCount = coreResources[Resource.COAL] ?: 0
    val silverCount = coreResources[Resource.SILVER] ?: 0
    val ironOreCount = coreResources[Resource.IRON_ORE] ?: 0
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
                // Multi-Resource Badge Card with Human-Readable Labels & Icons
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IndustrialSurface.copy(alpha = 0.94f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialBorder),
                    modifier = Modifier.height(38.dp)
                ) {
                    LazyRow(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { ResourceBadge(resource = Resource.COPPER, label = "Мідь", count = copperCount) }
                        item { ResourceBadge(resource = Resource.LEAD, label = "Свинець", count = leadCount) }
                        item { ResourceBadge(resource = Resource.COAL, label = "Вугілля", count = coalCount) }
                        item { ResourceBadge(resource = Resource.SILVER, label = "Срібло", count = silverCount) }
                        if (ironOreCount > 0) {
                            item { ResourceBadge(resource = Resource.IRON_ORE, label = "Залізна руда", count = ironOreCount) }
                        }
                        item { ResourceBadge(resource = Resource.IRON_INGOT, label = "Залізні злитки", count = ironIngotCount) }
                        if (copperAmmoCount > 0) {
                            item { ResourceBadge(resource = Resource.COPPER_AMMO, label = "Мідні патрони", count = copperAmmoCount) }
                        }
                        if (silverAmmoCount > 0) {
                            item { ResourceBadge(resource = Resource.SILVER_AMMO, label = "Срібні патрони", count = silverAmmoCount) }
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

            // Technical Inspector Card when a building is tapped/selected
            val inspectedBuilding = world.buildings.firstOrNull { it.id == inspectedBuildingId }
            if (inspectedBuilding != null) {
                TechnicalInspectorCard(
                    building = inspectedBuilding,
                    onClose = onDismissInspector,
                    onSetRecipe = { newRecipe -> onSetBuildingRecipe(inspectedBuilding.id, newRecipe) }
                )
            }
        }

        // --- MINI-MAP (Top-Right Corner) ---
        val inspectedBuilding = world.buildings.firstOrNull { it.id == inspectedBuildingId }
        val miniMapTopPadding = if (inspectedBuilding != null) 215.dp else 80.dp

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = miniMapTopPadding, end = 8.dp)
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

        // --- FLOATING CONTROLS: Rotate conveyor (Left Side) ---
        if (selectedTool == BuildingType.CONVEYOR) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                IconButton(
                    onClick = onRotateConveyor,
                    modifier = Modifier
                        .size(42.dp)
                        .background(IndustrialSurface.copy(alpha = 0.94f), CircleShape)
                        .border(1.5.dp, CopperAmber, CircleShape)
                        .testTag("btn_rotate_conveyor")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = "Повернути конвеєр",
                        tint = CopperAmber,
                        modifier = Modifier.size(22.dp)
                    )
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
                        val costParts = buildList {
                            if (blueprintsCopperCost > 0) add("$blueprintsCopperCost Міді")
                            if (blueprintsLeadCost > 0) add("$blueprintsLeadCost Свинцю")
                            if (blueprintsSilverCost > 0) add("$blueprintsSilverCost Срібла")
                            if (blueprintsIronCost > 0) add("$blueprintsIronCost Заліза")
                        }
                        val costText = if (costParts.isNotEmpty()) costParts.joinToString(", ") else "0"
                        Text(
                            text = "Креслень: $blueprintsCount | Вартість: $costText",
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
                    // Resource costs chips with icons and Ukrainian labels
                    if (buildingType.costCopper > 0) CostChip(Resource.COPPER, "${buildingType.costCopper} Мідь")
                    if (buildingType.costLead > 0) CostChip(Resource.LEAD, "${buildingType.costLead} Свинець")
                    if (buildingType.costSilver > 0) CostChip(Resource.SILVER, "${buildingType.costSilver} Срібло")
                    if (buildingType.costIronIngot > 0) CostChip(Resource.IRON_INGOT, "${buildingType.costIronIngot} Злитки")
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
fun ResourceIcon(resource: Resource, modifier: Modifier = Modifier.size(16.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (resource) {
            Resource.COPPER -> {
                // Faceted Copper Ore Nugget
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.38f)
                    lineTo(w * 0.52f, h * 0.12f)
                    lineTo(w * 0.88f, h * 0.35f)
                    lineTo(w * 0.85f, h * 0.78f)
                    lineTo(w * 0.5f, h * 0.92f)
                    lineTo(w * 0.15f, h * 0.7f)
                    close()
                }
                drawPath(path, Color(0xFFD97706))
                val facet1 = Path().apply {
                    moveTo(w * 0.52f, h * 0.12f)
                    lineTo(w * 0.88f, h * 0.35f)
                    lineTo(w * 0.5f, h * 0.55f)
                    lineTo(w * 0.2f, h * 0.38f)
                    close()
                }
                drawPath(facet1, Color(0xFFFBBF24))
                drawCircle(Color(0xFFFEF3C7), w * 0.08f, Offset(w * 0.45f, h * 0.3f))
                drawPath(path, Color(0xFF78350F), style = Stroke(width = 1.2f))
            }
            Resource.LEAD -> {
                // Faceted Dense Lead Ore Crystal
                val path = Path().apply {
                    moveTo(w * 0.25f, h * 0.15f)
                    lineTo(w * 0.75f, h * 0.15f)
                    lineTo(w * 0.9f, h * 0.55f)
                    lineTo(w * 0.65f, h * 0.9f)
                    lineTo(w * 0.15f, h * 0.75f)
                    lineTo(w * 0.1f, h * 0.35f)
                    close()
                }
                drawPath(path, Color(0xFF6D28D9))
                val facet = Path().apply {
                    moveTo(w * 0.25f, h * 0.15f)
                    lineTo(w * 0.75f, h * 0.15f)
                    lineTo(w * 0.55f, h * 0.5f)
                    lineTo(w * 0.1f, h * 0.35f)
                    close()
                }
                drawPath(facet, Color(0xFFA78BFA))
                drawCircle(Color(0xFFEDE9FE), w * 0.08f, Offset(w * 0.4f, h * 0.3f))
                drawPath(path, Color(0xFF4C1D95), style = Stroke(width = 1.2f))
            }
            Resource.COAL -> {
                // Jagged Anthracite Coal Lump
                val path = Path().apply {
                    moveTo(w * 0.35f, h * 0.14f)
                    lineTo(w * 0.72f, h * 0.18f)
                    lineTo(w * 0.92f, h * 0.52f)
                    lineTo(w * 0.75f, h * 0.88f)
                    lineTo(w * 0.28f, h * 0.9f)
                    lineTo(w * 0.1f, h * 0.58f)
                    close()
                }
                drawPath(path, Color(0xFF1E293B))
                val facet = Path().apply {
                    moveTo(w * 0.35f, h * 0.14f)
                    lineTo(w * 0.72f, h * 0.18f)
                    lineTo(w * 0.5f, h * 0.55f)
                    lineTo(w * 0.1f, h * 0.58f)
                    close()
                }
                drawPath(facet, Color(0xFF475569))
                drawPath(path, Color(0xFF0F172A), style = Stroke(width = 1.2f))
            }
            Resource.SILVER -> {
                // Sparkling Silver Crystal
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.1f)
                    lineTo(w * 0.88f, h * 0.42f)
                    lineTo(w * 0.65f, h * 0.9f)
                    lineTo(w * 0.35f, h * 0.9f)
                    lineTo(w * 0.12f, h * 0.42f)
                    close()
                }
                drawPath(path, Color(0xFF94A3B8))
                val facet1 = Path().apply {
                    moveTo(w * 0.5f, h * 0.1f)
                    lineTo(w * 0.88f, h * 0.42f)
                    lineTo(w * 0.5f, h * 0.6f)
                    close()
                }
                drawPath(facet1, Color(0xFFF8FAFC))
                val facet2 = Path().apply {
                    moveTo(w * 0.5f, h * 0.1f)
                    lineTo(w * 0.12f, h * 0.42f)
                    lineTo(w * 0.5f, h * 0.6f)
                    close()
                }
                drawPath(facet2, Color(0xFFCBD5E1))
                drawCircle(Color.White, w * 0.08f, Offset(w * 0.5f, h * 0.28f))
                drawPath(path, Color(0xFF64748B), style = Stroke(width = 1.2f))
            }
            Resource.IRON_ORE -> {
                // Hematite Rusty Iron Ore
                val path = Path().apply {
                    moveTo(w * 0.3f, h * 0.16f)
                    lineTo(w * 0.8f, h * 0.22f)
                    lineTo(w * 0.92f, h * 0.64f)
                    lineTo(w * 0.6f, h * 0.9f)
                    lineTo(w * 0.16f, h * 0.72f)
                    close()
                }
                drawPath(path, Color(0xFF78350F))
                val facet = Path().apply {
                    moveTo(w * 0.3f, h * 0.16f)
                    lineTo(w * 0.8f, h * 0.22f)
                    lineTo(w * 0.55f, h * 0.55f)
                    lineTo(w * 0.16f, h * 0.72f)
                    close()
                }
                drawPath(facet, Color(0xFFB45309))
                drawLine(Color(0xFFEA580C), Offset(w * 0.35f, h * 0.35f), Offset(w * 0.65f, h * 0.55f), strokeWidth = 1.5f)
                drawPath(path, Color(0xFF451A03), style = Stroke(width = 1.2f))
            }
            Resource.IRON_INGOT -> {
                // 3D Metallic Trapezoidal Ingot / Bar
                val bottomPath = Path().apply {
                    moveTo(w * 0.1f, h * 0.68f)
                    lineTo(w * 0.24f, h * 0.32f)
                    lineTo(w * 0.76f, h * 0.32f)
                    lineTo(w * 0.9f, h * 0.68f)
                    lineTo(w * 0.84f, h * 0.82f)
                    lineTo(w * 0.16f, h * 0.82f)
                    close()
                }
                drawPath(bottomPath, Color(0xFF475569)) // Shadow base
                val topPath = Path().apply {
                    moveTo(w * 0.24f, h * 0.32f)
                    lineTo(w * 0.76f, h * 0.32f)
                    lineTo(w * 0.82f, h * 0.54f)
                    lineTo(w * 0.18f, h * 0.54f)
                    close()
                }
                drawPath(topPath, Color(0xFFE2E8F0)) // High shine top face
                val frontPath = Path().apply {
                    moveTo(w * 0.18f, h * 0.54f)
                    lineTo(w * 0.82f, h * 0.54f)
                    lineTo(w * 0.88f, h * 0.78f)
                    lineTo(w * 0.12f, h * 0.78f)
                    close()
                }
                drawPath(frontPath, Color(0xFF94A3B8)) // Beveled front face
                drawLine(Color.White, Offset(w * 0.26f, h * 0.34f), Offset(w * 0.74f, h * 0.34f), strokeWidth = 1.2f)
                drawPath(bottomPath, Color(0xFF334155), style = Stroke(width = 1.2f))
            }
            Resource.COPPER_AMMO -> {
                // Pointed Bullet + Brass Shell Casing
                drawRect(Color(0xFFD97706), Offset(w * 0.32f, h * 0.44f), Size(w * 0.36f, h * 0.44f))
                val tip = Path().apply {
                    moveTo(w * 0.32f, h * 0.44f)
                    lineTo(w * 0.5f, h * 0.12f)
                    lineTo(w * 0.68f, h * 0.44f)
                    close()
                }
                drawPath(tip, Color(0xFFFB923C))
                drawRect(Color(0xFFB45309), Offset(w * 0.26f, h * 0.84f), Size(w * 0.48f, h * 0.08f))
                drawLine(Color(0xFFFED7AA), Offset(w * 0.42f, h * 0.22f), Offset(w * 0.42f, h * 0.82f), strokeWidth = 1.2f)
            }
            Resource.SILVER_AMMO -> {
                // Pointed Armor-Piercing Cyan/Silver Bullet
                drawRect(Color(0xFF94A3B8), Offset(w * 0.32f, h * 0.44f), Size(w * 0.36f, h * 0.44f))
                val tip = Path().apply {
                    moveTo(w * 0.32f, h * 0.44f)
                    lineTo(w * 0.5f, h * 0.12f)
                    lineTo(w * 0.68f, h * 0.44f)
                    close()
                }
                drawPath(tip, Color(0xFF38BDF8))
                drawRect(Color(0xFF64748B), Offset(w * 0.26f, h * 0.84f), Size(w * 0.48f, h * 0.08f))
                drawLine(Color.White, Offset(w * 0.42f, h * 0.22f), Offset(w * 0.42f, h * 0.82f), strokeWidth = 1.2f)
            }
            Resource.SCRAP -> {
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.2f)
                    lineTo(w * 0.8f, h * 0.25f)
                    lineTo(w * 0.75f, h * 0.8f)
                    lineTo(w * 0.25f, h * 0.75f)
                    close()
                }
                drawPath(path, Color(0xFF64748B))
                drawCircle(Color(0xFF1E293B), w * 0.15f, Offset(w * 0.5f, h * 0.5f))
                drawPath(path, Color(0xFF334155), style = Stroke(width = 1.2f))
            }
        }
    }
}

@Composable
private fun CostChip(resource: Resource, text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = resource.color.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, resource.color.copy(alpha = 0.6f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            ResourceIcon(resource = resource, modifier = Modifier.size(12.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ResourceBadge(resource: Resource, label: String, count: Int, maxCapacity: Int? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(IndustrialDark.copy(alpha = 0.55f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        ResourceIcon(resource = resource, modifier = Modifier.size(15.dp))
        Text(
            text = if (maxCapacity != null) "$label: $count/$maxCapacity" else "$label: $count",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TechnicalInspectorCard(
    building: BuildingInstance,
    onClose: () -> Unit,
    onSetRecipe: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = IndustrialSurface.copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CoreCyan.copy(alpha = 0.8f)),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .testTag("technical_inspector_card")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Icon, Name, Type, Coordinates, Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CoreCyan.copy(alpha = 0.2f))
                            .border(1.dp, CoreCyan, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (building.type.category) {
                                BuildingCategory.POWER -> Icons.Default.Bolt
                                BuildingCategory.DEFENSE -> Icons.Default.Shield
                                BuildingCategory.INDUSTRY -> Icons.Default.PrecisionManufacturing
                                BuildingCategory.MINING -> Icons.Default.Build
                                BuildingCategory.LOGISTICS -> Icons.Default.Engineering
                            },
                            contentDescription = null,
                            tint = CoreCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = building.type.displayName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${building.type.category.displayName} • Позиція: [${building.originX}, ${building.originY}]",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(28.dp)
                        .background(IndustrialDark.copy(alpha = 0.6f), CircleShape)
                        .testTag("btn_close_inspector")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрити",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // HP and Power Status Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // HP Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(IndustrialDark)
                        .border(0.5.dp, IndustrialBorder, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(building.healthRatio)
                            .background(
                                if (building.healthRatio > 0.5f) Color(0xFF10B981)
                                else if (building.healthRatio > 0.25f) Color(0xFFF59E0B)
                                else Color(0xFFEF4444)
                            )
                    )
                    Text(
                        text = "❤ ${building.health} / ${building.type.maxHealth} HP",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                // Power indicator badge
                when (building.type) {
                    BuildingType.COAL_GENERATOR -> {
                        val hasCoal = (building.storedResources[Resource.COAL] ?: 0) > 0
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (hasCoal) Color(0x3310B981) else Color(0x33EF4444),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (hasCoal) Color(0xFF10B981) else Color(0xFFEF4444))
                        ) {
                            Text(
                                text = if (hasCoal) "⚡ Генерує 20/с" else "⚠ Немає вугілля",
                                color = if (hasCoal) Color(0xFF34D399) else Color(0xFFF87171),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    BuildingType.BATTERY -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0x3338BDF8),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Text(
                                text = "⚡ ${(building.energyStored).toInt()} / 200",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    BuildingType.POWER_NODE -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0x33FDE047),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE047))
                        ) {
                            Text(
                                text = "⚡ Радіус 5 кл.",
                                color = Color(0xFFFDE047),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    BuildingType.IRON_SMELTER -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (building.isPowered) Color(0x3310B981) else Color(0x33EF4444),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (building.isPowered) Color(0xFF10B981) else Color(0xFFEF4444))
                        ) {
                            Text(
                                text = if (building.isPowered) "⚡ Живиться (3/с)" else "⚠ Немає струму",
                                color = if (building.isPowered) Color(0xFF34D399) else Color(0xFFF87171),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    BuildingType.AMMO_FABRICATOR -> {
                        val reqPower = if (building.fabricatorRecipe == "silver") 5 else 3
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (building.isPowered) Color(0x3310B981) else Color(0x33EF4444),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (building.isPowered) Color(0xFF10B981) else Color(0xFFEF4444))
                        ) {
                            Text(
                                text = if (building.isPowered) "⚡ Живиться (${reqPower}/с)" else "⚠ Немає струму",
                                color = if (building.isPowered) Color(0xFF34D399) else Color(0xFFF87171),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    else -> {}
                }
            }

            // Internal Storage / Inventory
            when (building.type) {
                BuildingType.COAL_GENERATOR -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Паливо:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        ResourceBadge(Resource.COAL, "Вугілля", building.storedResources[Resource.COAL] ?: 0, maxCapacity = 25)
                    }
                }
                BuildingType.IRON_SMELTER -> {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Сховище сировини та виходу:", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item { ResourceBadge(Resource.IRON_ORE, "Залізна руда", building.storedResources[Resource.IRON_ORE] ?: 0, maxCapacity = 25) }
                            item { ResourceBadge(Resource.COAL, "Вугілля", building.storedResources[Resource.COAL] ?: 0, maxCapacity = 20) }
                            item { ResourceBadge(Resource.IRON_INGOT, "Готові злитки", building.storedResources[Resource.IRON_INGOT] ?: 0, maxCapacity = 20) }
                        }
                    }
                }
                BuildingType.AMMO_FABRICATOR -> {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Ресурси в буфері:", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item { ResourceBadge(Resource.COPPER, "Мідь", building.storedResources[Resource.COPPER] ?: 0, maxCapacity = 25) }
                            item { ResourceBadge(Resource.LEAD, "Свинець", building.storedResources[Resource.LEAD] ?: 0, maxCapacity = 25) }
                            item { ResourceBadge(Resource.COAL, "Вугілля", building.storedResources[Resource.COAL] ?: 0, maxCapacity = 25) }
                            item { ResourceBadge(Resource.SILVER, "Срібло", building.storedResources[Resource.SILVER] ?: 0, maxCapacity = 25) }
                            item { ResourceBadge(Resource.COPPER_AMMO, "Мідні набої", building.storedResources[Resource.COPPER_AMMO] ?: 0, maxCapacity = 30) }
                            item { ResourceBadge(Resource.SILVER_AMMO, "Срібні набої", building.storedResources[Resource.SILVER_AMMO] ?: 0, maxCapacity = 30) }
                        }
                    }
                }
                BuildingType.TURRET -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Боєзапас:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        if (building.turretAmmoType != null && building.turretAmmoCount > 0) {
                            ResourceBadge(building.turretAmmoType!!, building.turretAmmoType!!.displayName, building.turretAmmoCount, maxCapacity = 30)
                        } else {
                            Text("Порожньо (підведіть мідні або срібні набої)", color = Color(0xFFF59E0B), fontSize = 10.sp)
                        }
                    }
                }
                else -> {}
            }

            // Recipe / Behavior / Progress
            when (building.type) {
                BuildingType.IRON_SMELTER -> {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Рецепт: 3 Залізна руда + 1 Вугілля + 3⚡ → 1 Залізний злиток",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(IndustrialDark)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((building.craftTimer / 1.0f).coerceIn(0f, 1f))
                                    .background(Color(0xFF38BDF8))
                            )
                        }
                    }
                }
                BuildingType.AMMO_FABRICATOR -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Рецепт виробництва:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val isCopper = building.fabricatorRecipe == "copper"
                            val isSilver = building.fabricatorRecipe == "silver"

                            Button(
                                onClick = { onSetRecipe("copper") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCopper) CopperAmber else IndustrialDark,
                                    contentColor = if (isCopper) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = if (isCopper) "✓ Мідні набої" else "Мідні набої",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { onSetRecipe("silver") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSilver) SilverGray else IndustrialDark,
                                    contentColor = if (isSilver) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = if (isSilver) "✓ Срібні набої" else "Срібні набої",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val recipeText = if (building.fabricatorRecipe == "silver") {
                            "Вимоги: 3 Срібло + 2 Свинець + 2 Вугілля + 5⚡ → 1 Срібний набій (24 урону)"
                        } else {
                            "Вимоги: 3 Мідь + 1 Свинець + 2 Вугілля + 3⚡ → 1 Мідний набій (10 урону)"
                        }
                        Text(recipeText, color = Color(0xFFCBD5E1), fontSize = 10.sp)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(IndustrialDark)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((building.craftTimer / 1.2f).coerceIn(0f, 1f))
                                    .background(if (building.fabricatorRecipe == "silver") SilverGray else CopperAmber)
                            )
                        }
                    }
                }
                BuildingType.TURRET -> {
                    Text(
                        text = "Параметри: Радіус 6.5 кл. • Скорострільність: 4.5 вистрілів/с\nМідні набої: 10 урону • Срібні набої: 24 урону",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )
                }
                BuildingType.CONVEYOR -> {
                    Text(
                        text = "Напрямок: ${building.direction.displayName} • Швидкість: 2.4 клітинки/сек",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )
                }
                BuildingType.ROUTER -> {
                    Text(
                        text = "Рівномірно розподіляє ресурси між усіма підключеними конвеєрами, генераторами або заводами.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )
                }
                BuildingType.JUNCTION -> {
                    Text(
                        text = "Пропускає перпендикулярні конвеєрні потоки без змішування ресурсів.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )
                }
                BuildingType.COPPER_DRILL -> {
                    Text(
                        text = "Видобуває корисні копалини під собою (1.2 руди/сек на повній жилі).",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )
                }
                BuildingType.CORE -> {
                    Text(
                        text = "Головне Ядро. Зберігає всі ресурси бази та керує будівельним дроном.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )
                }
                else -> {}
            }
        }
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
