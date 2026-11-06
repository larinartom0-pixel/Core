package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.GameWorld
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen {
    MAIN_MENU,
    IN_GAME
}

class GameViewModel : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
    val currentScreen = _currentScreen.asStateFlow()

    private var _world = GameWorld()
    val world: GameWorld get() = _world

    // State trigger to notify Compose of frame updates
    private val _tickState = MutableStateFlow(0L)
    val tickState = _tickState.asStateFlow()

    // Reactive HUD States: will trigger Compose recomposition in real time
    private val _coreResources = MutableStateFlow<Map<Resource, Int>>(_world.coreResources.toMap())
    val coreResources = _coreResources.asStateFlow()

    private val _blueprintsCount = MutableStateFlow(0)
    val blueprintsCount = _blueprintsCount.asStateFlow()

    private val _blueprintsCopperCost = MutableStateFlow(0)
    val blueprintsCopperCost = _blueprintsCopperCost.asStateFlow()

    private val _blueprintsLeadCost = MutableStateFlow(0)
    val blueprintsLeadCost = _blueprintsLeadCost.asStateFlow()

    private val _blueprintsSilverCost = MutableStateFlow(0)
    val blueprintsSilverCost = _blueprintsSilverCost.asStateFlow()

    private val _blueprintsIronCost = MutableStateFlow(0)
    val blueprintsIronCost = _blueprintsIronCost.asStateFlow()

    private val _markedForDemolishCount = MutableStateFlow(0)
    val markedForDemolishCount = _markedForDemolishCount.asStateFlow()

    private val _droneState = MutableStateFlow(DroneState.IDLE_AT_CORE)
    val droneState = _droneState.asStateFlow()

    private val _waveNumber = MutableStateFlow(1)
    val waveNumber = _waveNumber.asStateFlow()

    private val _waveCountdownSeconds = MutableStateFlow(180f)
    val waveCountdownSeconds = _waveCountdownSeconds.asStateFlow()

    // Camera (Reactive Compose States)
    var cameraOffsetX by mutableFloatStateOf(0f)
        private set
    var cameraOffsetY by mutableFloatStateOf(0f)
        private set
    var zoom by mutableFloatStateOf(1.0f)
        private set

    // Tool & Category selection
    private val _selectedCategory = MutableStateFlow(BuildingCategory.LOGISTICS)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedTool = MutableStateFlow<BuildingType?>(null)
    val selectedTool = _selectedTool.asStateFlow()

    private val _conveyorDirection = MutableStateFlow(Direction.EAST)
    val conveyorDirection = _conveyorDirection.asStateFlow()

    private val _fabricatorRecipe = MutableStateFlow("copper")
    val fabricatorRecipe = _fabricatorRecipe.asStateFlow()

    // Demolish / Deconstruction tool
    private val _isDemolishMode = MutableStateFlow(false)
    val isDemolishMode = _isDemolishMode.asStateFlow()

    // Building Technical Inspector
    private val _inspectedBuildingId = MutableStateFlow<String?>(null)
    val inspectedBuildingId = _inspectedBuildingId.asStateFlow()

    var demolishDragStart by mutableStateOf<Pair<Int, Int>?>(null)
    var demolishDragCurrent by mutableStateOf<Pair<Int, Int>?>(null)

    // Settings
    private val _showGrid = MutableStateFlow(true)
    val showGrid = _showGrid.asStateFlow()

    private val _showFlowArrows = MutableStateFlow(true)
    val showFlowArrows = _showFlowArrows.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled = _soundEnabled.asStateFlow()

    private var gameLoopJob: Job? = null

    init {
        centerOnCore(1080f, 2160f)
        syncHudState()
    }

    fun startNewGame() {
        _world = GameWorld()
        _selectedTool.value = null
        _isDemolishMode.value = false
        _conveyorDirection.value = Direction.EAST
        _selectedCategory.value = BuildingCategory.LOGISTICS
        _fabricatorRecipe.value = "copper"
        syncHudState()
        centerOnCore(1080f, 2160f)
        _currentScreen.value = AppScreen.IN_GAME
        startGameLoop()
    }

    fun exitToMainMenu() {
        _currentScreen.value = AppScreen.MAIN_MENU
        gameLoopJob?.cancel()
    }

    fun syncHudState() {
        _coreResources.value = _world.coreResources.toMap()
        _blueprintsCount.value = _world.blueprints.size
        _blueprintsCopperCost.value = _world.getTotalBlueprintCopperCost()
        _blueprintsLeadCost.value = _world.getTotalBlueprintLeadCost()
        _blueprintsSilverCost.value = _world.getTotalBlueprintSilverCost()
        _blueprintsIronCost.value = _world.getTotalBlueprintIronCost()
        _markedForDemolishCount.value = _world.getMarkedForDeconstructionCount()
        _droneState.value = _world.drone.state
        _waveNumber.value = _world.waveNumber
        _waveCountdownSeconds.value = _world.waveCountdownSeconds
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                _world.update(dt)
                _tickState.value = now

                // Check and update reactive HUD flows
                if (_coreResources.value != _world.coreResources) {
                    _coreResources.value = _world.coreResources.toMap()
                }
                if (_blueprintsCount.value != _world.blueprints.size) {
                    _blueprintsCount.value = _world.blueprints.size
                    _blueprintsCopperCost.value = _world.getTotalBlueprintCopperCost()
                    _blueprintsLeadCost.value = _world.getTotalBlueprintLeadCost()
                    _blueprintsSilverCost.value = _world.getTotalBlueprintSilverCost()
                    _blueprintsIronCost.value = _world.getTotalBlueprintIronCost()
                }
                val demCount = _world.getMarkedForDeconstructionCount()
                if (_markedForDemolishCount.value != demCount) {
                    _markedForDemolishCount.value = demCount
                }
                if (_droneState.value != _world.drone.state) {
                    _droneState.value = _world.drone.state
                }
                if (_waveNumber.value != _world.waveNumber) {
                    _waveNumber.value = _world.waveNumber
                }
                _waveCountdownSeconds.value = _world.waveCountdownSeconds

                delay(16L) // ~60 FPS
            }
        }
    }

    fun onPanCamera(dx: Float, dy: Float) {
        cameraOffsetX += dx
        cameraOffsetY += dy
    }

    fun panCameraTo(worldX: Float, worldY: Float, screenWidth: Float, screenHeight: Float) {
        val tileSize = 48f * zoom
        cameraOffsetX = screenWidth / 2f - worldX * tileSize
        cameraOffsetY = screenHeight / 2f - worldY * tileSize
    }

    /**
     * Smooth zoom centered on the screen instead of the top-left corner
     */
    fun onZoomIn(screenWidth: Float, screenHeight: Float) {
        zoomCentered(zoom * 1.25f, screenWidth, screenHeight)
    }

    fun onZoomOut(screenWidth: Float, screenHeight: Float) {
        zoomCentered(zoom / 1.25f, screenWidth, screenHeight)
    }

    fun zoomCentered(newZoom: Float, screenWidth: Float, screenHeight: Float) {
        val clampedZoom = newZoom.coerceIn(0.4f, 3.0f)
        if (clampedZoom == zoom) return

        val baseTileSize = 48f
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f

        val worldX = (centerX - cameraOffsetX) / (baseTileSize * zoom)
        val worldY = (centerY - cameraOffsetY) / (baseTileSize * zoom)

        zoom = clampedZoom

        cameraOffsetX = centerX - worldX * (baseTileSize * zoom)
        cameraOffsetY = centerY - worldY * (baseTileSize * zoom)
    }

    fun centerOnCore(screenWidth: Float, screenHeight: Float) {
        val tileSize = 48f * zoom
        val corePxX = _world.coreCenterX * tileSize
        val corePxY = _world.coreCenterY * tileSize
        cameraOffsetX = screenWidth / 2f - corePxX
        cameraOffsetY = screenHeight / 2f - corePxY
    }

    fun selectCategory(category: BuildingCategory) {
        _selectedCategory.value = category
        _isDemolishMode.value = false
        _inspectedBuildingId.value = null
    }

    fun selectTool(tool: BuildingType?) {
        _selectedTool.value = tool
        if (tool != null) {
            _isDemolishMode.value = false
            _selectedCategory.value = tool.category
            _inspectedBuildingId.value = null
        }
    }

    fun dismissInspector() {
        _inspectedBuildingId.value = null
    }

    fun setBuildingRecipe(buildingId: String, recipe: String) {
        val b = _world.buildings.firstOrNull { it.id == buildingId }
        if (b != null) {
            b.fabricatorRecipe = recipe
        }
        _fabricatorRecipe.value = recipe
    }

    fun setFabricatorRecipe(recipe: String) {
        _fabricatorRecipe.value = recipe
        for (b in _world.buildings) {
            if (b.type == BuildingType.AMMO_FABRICATOR) {
                b.fabricatorRecipe = recipe
            }
        }
    }

    fun toggleDemolishMode() {
        _isDemolishMode.value = !_isDemolishMode.value
        if (_isDemolishMode.value) {
            _selectedTool.value = null
            _inspectedBuildingId.value = null
        }
    }

    fun rotateConveyor() {
        _conveyorDirection.value = _conveyorDirection.value.next()
    }

    fun onTapTile(tileX: Int, tileY: Int) {
        if (_isDemolishMode.value) {
            _world.toggleMarkForDeconstruction(tileX, tileY)
            syncHudState()
            return
        }
        val tool = _selectedTool.value
        if (tool == null) {
            // No tool selected: inspect building or dismiss if tapping empty terrain
            val b = _world.getBuildingAt(tileX, tileY)
            _inspectedBuildingId.value = b?.id
            return
        }
        _inspectedBuildingId.value = null
        _world.toggleBlueprint(tileX, tileY, tool, _conveyorDirection.value)
        syncHudState()
    }

    fun onDragConveyorSegment(tileX: Int, tileY: Int, direction: Direction) {
        if (_selectedTool.value == BuildingType.CONVEYOR) {
            _world.addConveyorAt(tileX, tileY, direction)
            syncHudState()
        }
    }

    fun onDemolishAreaSelected(x1: Int, y1: Int, x2: Int, y2: Int) {
        _world.markAreaForDeconstruction(x1, y1, x2, y2)
        syncHudState()
    }

    fun confirmBlueprints() {
        _world.confirmBlueprints()
        syncHudState()
    }

    fun cancelBlueprints() {
        _world.cancelBlueprints()
        syncHudState()
    }

    fun confirmDemolish() {
        _world.confirmDeconstruction()
        syncHudState()
    }

    fun cancelDemolish() {
        _world.cancelDeconstruction()
        syncHudState()
    }

    fun toggleGrid(enabled: Boolean) {
        _showGrid.value = enabled
    }

    fun toggleFlowArrows(enabled: Boolean) {
        _showFlowArrows.value = enabled
    }

    fun toggleSound(enabled: Boolean) {
        _soundEnabled.value = enabled
    }
}
