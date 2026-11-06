package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Resource
import com.example.ui.theme.IndustrialDark

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onExitToMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showInGameSettings by remember { mutableStateOf(false) }

    // Intercept back button to open settings or confirm exit
    BackHandler {
        showInGameSettings = true
    }

    // Subscribe to state flows
    val tick by viewModel.tickState.collectAsStateWithLifecycle()
    val coreResources by viewModel.coreResources.collectAsStateWithLifecycle()
    val blueprintsCount by viewModel.blueprintsCount.collectAsStateWithLifecycle()
    val blueprintsCopperCost by viewModel.blueprintsCopperCost.collectAsStateWithLifecycle()
    val blueprintsLeadCost by viewModel.blueprintsLeadCost.collectAsStateWithLifecycle()
    val blueprintsSilverCost by viewModel.blueprintsSilverCost.collectAsStateWithLifecycle()
    val blueprintsIronCost by viewModel.blueprintsIronCost.collectAsStateWithLifecycle()
    val markedForDemolishCount by viewModel.markedForDemolishCount.collectAsStateWithLifecycle()
    val droneState by viewModel.droneState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedTool by viewModel.selectedTool.collectAsStateWithLifecycle()
    val conveyorDirection by viewModel.conveyorDirection.collectAsStateWithLifecycle()
    val fabricatorRecipe by viewModel.fabricatorRecipe.collectAsStateWithLifecycle()
    val isDemolishMode by viewModel.isDemolishMode.collectAsStateWithLifecycle()
    val waveNumber by viewModel.waveNumber.collectAsStateWithLifecycle()
    val waveCountdownSeconds by viewModel.waveCountdownSeconds.collectAsStateWithLifecycle()
    val showGrid by viewModel.showGrid.collectAsStateWithLifecycle()
    val showFlowArrows by viewModel.showFlowArrows.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val inspectedBuildingId by viewModel.inspectedBuildingId.collectAsStateWithLifecycle()

    var screenWidth by remember { mutableFloatStateOf(1080f) }
    var screenHeight by remember { mutableFloatStateOf(2160f) }

    val world = viewModel.world

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IndustrialDark)
            .onSizeChanged { size ->
                if (size.width > 0 && size.height > 0) {
                    screenWidth = size.width.toFloat()
                    screenHeight = size.height.toFloat()
                }
            }
    ) {
        // Render Canvas (Canvas stays mounted and handles touch gestures smoothly without recreation)
        GameCanvas(
            world = world,
            cameraOffsetX = viewModel.cameraOffsetX,
            cameraOffsetY = viewModel.cameraOffsetY,
            zoom = viewModel.zoom,
            tick = tick,
            showGridLines = showGrid,
            showFlowArrows = showFlowArrows,
            selectedTool = selectedTool,
            isDemolishMode = isDemolishMode,
            demolishDragStart = viewModel.demolishDragStart,
            demolishDragCurrent = viewModel.demolishDragCurrent,
            conveyorDirection = conveyorDirection,
            inspectedBuildingId = inspectedBuildingId,
            onTapTile = { x, y -> viewModel.onTapTile(x, y) },
            onDragConveyorSegment = { x, y, dir -> viewModel.onDragConveyorSegment(x, y, dir) },
            onPanCamera = { dx, dy -> viewModel.onPanCamera(dx, dy) },
            onUpdateDemolishDrag = { start, curr ->
                viewModel.demolishDragStart = start
                viewModel.demolishDragCurrent = curr
            },
            onDemolishAreaSelected = { x1, y1, x2, y2 ->
                viewModel.onDemolishAreaSelected(x1, y1, x2, y2)
            },
            onZoomCentered = { newZoom, _, _ ->
                viewModel.zoomCentered(newZoom, screenWidth, screenHeight)
            }
        )

        // Render HUD
        GameHud(
            coreResources = coreResources,
            droneState = droneState,
            blueprintsCount = blueprintsCount,
            blueprintsCopperCost = blueprintsCopperCost,
            blueprintsLeadCost = blueprintsLeadCost,
            blueprintsSilverCost = blueprintsSilverCost,
            blueprintsIronCost = blueprintsIronCost,
            markedForDemolishCount = markedForDemolishCount,
            selectedTool = selectedTool,
            isDemolishMode = isDemolishMode,
            conveyorDirection = conveyorDirection,
            selectedCategory = selectedCategory,
            fabricatorRecipe = fabricatorRecipe,
            waveNumber = waveNumber,
            waveCountdownSeconds = waveCountdownSeconds,
            world = world,
            cameraOffsetX = viewModel.cameraOffsetX,
            cameraOffsetY = viewModel.cameraOffsetY,
            zoom = viewModel.zoom,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            onSelectCategory = { viewModel.selectCategory(it) },
            onSelectTool = { viewModel.selectTool(it) },
            onToggleDemolishMode = { viewModel.toggleDemolishMode() },
            onRotateConveyor = { viewModel.rotateConveyor() },
            onConfirmBlueprints = { viewModel.confirmBlueprints() },
            onCancelBlueprints = { viewModel.cancelBlueprints() },
            onConfirmDemolish = { viewModel.confirmDemolish() },
            onCancelDemolish = { viewModel.cancelDemolish() },
            onSelectFabricatorRecipe = { viewModel.setFabricatorRecipe(it) },
            onMiniMapTap = { wx, wy -> viewModel.panCameraTo(wx, wy, screenWidth, screenHeight) },
            onCenterOnCore = { viewModel.centerOnCore(screenWidth, screenHeight) },
            onZoomIn = { viewModel.onZoomIn(screenWidth, screenHeight) },
            onZoomOut = { viewModel.onZoomOut(screenWidth, screenHeight) },
            inspectedBuildingId = inspectedBuildingId,
            onDismissInspector = { viewModel.dismissInspector() },
            onSetBuildingRecipe = { bId, rec -> viewModel.setBuildingRecipe(bId, rec) },
            onOpenSettings = { showInGameSettings = true }
        )

        if (showInGameSettings) {
            SettingsDialog(
                showGrid = showGrid,
                onToggleGrid = { viewModel.toggleGrid(it) },
                showFlowArrows = showFlowArrows,
                onToggleFlowArrows = { viewModel.toggleFlowArrows(it) },
                soundEnabled = soundEnabled,
                onToggleSound = { viewModel.toggleSound(it) },
                onDismiss = { showInGameSettings = false },
                onExitToMainMenu = {
                    showInGameSettings = false
                    viewModel.exitToMainMenu()
                    onExitToMainMenu()
                }
            )
        }
    }
}
