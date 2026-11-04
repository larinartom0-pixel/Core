package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.GameWorld
import com.example.model.BuildingType
import com.example.model.Direction
import com.example.model.Resource
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

    // Camera (Reactive Compose States)
    var cameraOffsetX by mutableFloatStateOf(0f)
        private set
    var cameraOffsetY by mutableFloatStateOf(0f)
        private set
    var zoom by mutableFloatStateOf(1.0f)
        private set

    // Tool selection
    private val _selectedTool = MutableStateFlow<BuildingType?>(null)
    val selectedTool = _selectedTool.asStateFlow()

    private val _conveyorDirection = MutableStateFlow(Direction.EAST)
    val conveyorDirection = _conveyorDirection.asStateFlow()

    // Demolish / Deconstruction tool
    private val _isDemolishMode = MutableStateFlow(false)
    val isDemolishMode = _isDemolishMode.asStateFlow()

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
    }

    fun startNewGame() {
        _world = GameWorld()
        _selectedTool.value = null
        _isDemolishMode.value = false
        _conveyorDirection.value = Direction.EAST
        centerOnCore(1080f, 2160f)
        _currentScreen.value = AppScreen.IN_GAME
        startGameLoop()
    }

    fun exitToMainMenu() {
        _currentScreen.value = AppScreen.MAIN_MENU
        gameLoopJob?.cancel()
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

                delay(16L) // ~60 FPS
            }
        }
    }

    fun onPanCamera(dx: Float, dy: Float) {
        cameraOffsetX += dx
        cameraOffsetY += dy
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
        val clampedZoom = newZoom.coerceIn(0.5f, 3.0f)
        if (clampedZoom == zoom) return

        val baseTileSize = 48f
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f

        // World coordinate currently at screen center
        val worldX = (centerX - cameraOffsetX) / (baseTileSize * zoom)
        val worldY = (centerY - cameraOffsetY) / (baseTileSize * zoom)

        zoom = clampedZoom

        // Adjust offsets so world point remains at center
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

    fun selectTool(tool: BuildingType?) {
        _selectedTool.value = tool
        if (tool != null) {
            _isDemolishMode.value = false
        }
    }

    fun toggleDemolishMode() {
        _isDemolishMode.value = !_isDemolishMode.value
        if (_isDemolishMode.value) {
            _selectedTool.value = null
        }
    }

    fun rotateConveyor() {
        _conveyorDirection.value = _conveyorDirection.value.next()
    }

    fun onTapTile(tileX: Int, tileY: Int) {
        if (_isDemolishMode.value) {
            _world.toggleMarkForDeconstruction(tileX, tileY)
            return
        }
        val tool = _selectedTool.value ?: return
        _world.toggleBlueprint(tileX, tileY, tool, _conveyorDirection.value)
    }

    fun onDragConveyorSegment(tileX: Int, tileY: Int, direction: Direction) {
        if (_selectedTool.value == BuildingType.CONVEYOR) {
            _world.addConveyorAt(tileX, tileY, direction)
        }
    }

    fun onDemolishAreaSelected(x1: Int, y1: Int, x2: Int, y2: Int) {
        _world.markAreaForDeconstruction(x1, y1, x2, y2)
    }

    fun confirmBlueprints() {
        _world.confirmBlueprints()
    }

    fun cancelBlueprints() {
        _world.cancelBlueprints()
    }

    fun confirmDemolish() {
        _world.confirmDeconstruction()
    }

    fun cancelDemolish() {
        _world.cancelDeconstruction()
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
