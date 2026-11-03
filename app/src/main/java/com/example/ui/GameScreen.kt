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
    val selectedTool by viewModel.selectedTool.collectAsStateWithLifecycle()
    val conveyorDirection by viewModel.conveyorDirection.collectAsStateWithLifecycle()
    val showGrid by viewModel.showGrid.collectAsStateWithLifecycle()
    val showFlowArrows by viewModel.showFlowArrows.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()

    var screenWidth by remember { mutableFloatStateOf(1080f) }
    var screenHeight by remember { mutableFloatStateOf(2160f) }

    val world = viewModel.world
    val copperCount = world.coreResources[Resource.COPPER] ?: 0
    val blueprintsCount = world.blueprints.size
    val blueprintsCost = world.getTotalBlueprintCost()

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
            conveyorDirection = conveyorDirection,
            onTapTile = { x, y -> viewModel.onTapTile(x, y) },
            onDragConveyorSegment = { x, y, dir -> viewModel.onDragConveyorSegment(x, y, dir) },
            onPanCamera = { dx, dy -> viewModel.onPanCamera(dx, dy) }
        )

        // Render HUD
        GameHud(
            copperCount = copperCount,
            droneState = world.drone.state,
            constructionQueueCount = world.constructionQueue.size,
            blueprintsCount = blueprintsCount,
            blueprintsCost = blueprintsCost,
            selectedTool = selectedTool,
            conveyorDirection = conveyorDirection,
            onSelectTool = { viewModel.selectTool(it) },
            onRotateConveyor = { viewModel.rotateConveyor() },
            onConfirmBlueprints = { viewModel.confirmBlueprints() },
            onCancelBlueprints = { viewModel.cancelBlueprints() },
            onCenterOnCore = { viewModel.centerOnCore(screenWidth, screenHeight) },
            onZoomIn = { viewModel.onZoomIn() },
            onZoomOut = { viewModel.onZoomOut() },
            onPanBy = { dx, dy -> viewModel.onPanCamera(dx, dy) },
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
