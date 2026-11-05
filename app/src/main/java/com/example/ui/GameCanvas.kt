package com.example.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import com.example.game.GameWorld
import com.example.model.*
import kotlin.math.*

@Composable
fun GameCanvas(
    world: GameWorld,
    cameraOffsetX: Float,
    cameraOffsetY: Float,
    zoom: Float,
    tick: Long,
    showGridLines: Boolean,
    showFlowArrows: Boolean,
    selectedTool: BuildingType?,
    isDemolishMode: Boolean,
    demolishDragStart: Pair<Int, Int>?,
    demolishDragCurrent: Pair<Int, Int>?,
    conveyorDirection: Direction,
    onTapTile: (Int, Int) -> Unit,
    onDragConveyorSegment: (Int, Int, Direction) -> Unit,
    onPanCamera: (Float, Float) -> Unit,
    onUpdateDemolishDrag: (Pair<Int, Int>?, Pair<Int, Int>?) -> Unit,
    onDemolishAreaSelected: (Int, Int, Int, Int) -> Unit,
    onZoomCentered: (Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val baseTileSize = 48f

    val currentCameraOffsetX by rememberUpdatedState(cameraOffsetX)
    val currentCameraOffsetY by rememberUpdatedState(cameraOffsetY)
    val currentZoom by rememberUpdatedState(zoom)
    val currentTool by rememberUpdatedState(selectedTool)
    val currentDemolishMode by rememberUpdatedState(isDemolishMode)
    val onPanState by rememberUpdatedState(onPanCamera)
    val onTapState by rememberUpdatedState(onTapTile)
    val onDragConveyorState by rememberUpdatedState(onDragConveyorSegment)
    val onUpdateDemolishDragState by rememberUpdatedState(onUpdateDemolishDrag)
    val onDemolishAreaSelectedState by rememberUpdatedState(onDemolishAreaSelected)
    val onZoomCenteredState by rememberUpdatedState(onZoomCentered)

    val nativeTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startDownPos = down.position
                    var lastPanPos = down.position
                    var hasDragged = false
                    var lastTouchedTile: Pair<Int, Int>? = null
                    var demolishDragOrigin: Pair<Int, Int>? = null

                    val currentTileSize = baseTileSize * currentZoom
                    val downTileX = floor((startDownPos.x - currentCameraOffsetX) / currentTileSize).toInt()
                    val downTileY = floor((startDownPos.y - currentCameraOffsetY) / currentTileSize).toInt()

                    if (currentDemolishMode) {
                        demolishDragOrigin = Pair(downTileX, downTileY)
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val pointers = event.changes

                        if (pointers.size >= 2) {
                            // Pinch to zoom
                            val p1 = pointers[0]
                            val p2 = pointers[1]
                            val prevDist = hypot(p1.previousPosition.x - p2.previousPosition.x, p1.previousPosition.y - p2.previousPosition.y)
                            val currentDist = hypot(p1.position.x - p2.position.x, p1.position.y - p2.position.y)

                            if (prevDist > 10f && currentDist > 10f) {
                                val scaleFactor = currentDist / prevDist
                                val midX = (p1.position.x + p2.position.x) / 2f
                                val midY = (p1.position.y + p2.position.y) / 2f
                                onZoomCenteredState(currentZoom * scaleFactor, midX, midY)
                            }
                            pointers.forEach { it.consume() }
                            continue
                        }

                        val activePointer = pointers.firstOrNull { it.id == down.id } ?: break

                        if (activePointer.changedToUp()) {
                            if (!hasDragged) {
                                if (downTileX in 0 until world.width && downTileY in 0 until world.height) {
                                    onTapState(downTileX, downTileY)
                                }
                            } else if (currentDemolishMode && demolishDragOrigin != null) {
                                val endTileX = floor((activePointer.position.x - currentCameraOffsetX) / currentTileSize).toInt()
                                val endTileY = floor((activePointer.position.y - currentCameraOffsetY) / currentTileSize).toInt()
                                onDemolishAreaSelectedState(demolishDragOrigin.first, demolishDragOrigin.second, endTileX, endTileY)
                                onUpdateDemolishDragState(null, null)
                            }
                            activePointer.consume()
                            break
                        }

                        val currentPos = activePointer.position
                        val totalDragDist = hypot(currentPos.x - startDownPos.x, currentPos.y - startDownPos.y)

                        if (!hasDragged && totalDragDist > 14f) {
                            hasDragged = true
                        }

                        if (hasDragged) {
                            if (currentDemolishMode && demolishDragOrigin != null) {
                                val curTileX = floor((currentPos.x - currentCameraOffsetX) / currentTileSize).toInt()
                                val curTileY = floor((currentPos.y - currentCameraOffsetY) / currentTileSize).toInt()
                                onUpdateDemolishDragState(demolishDragOrigin, Pair(curTileX, curTileY))
                            } else if (currentTool == BuildingType.CONVEYOR) {
                                val curTileX = floor((currentPos.x - currentCameraOffsetX) / currentTileSize).toInt()
                                val curTileY = floor((currentPos.y - currentCameraOffsetY) / currentTileSize).toInt()
                                val currentTile = Pair(curTileX, curTileY)

                                if (currentTile != lastTouchedTile && curTileX in 0 until world.width && curTileY in 0 until world.height) {
                                    val dragDir = if (lastTouchedTile != null) {
                                        val dX = curTileX - lastTouchedTile!!.first
                                        val dY = curTileY - lastTouchedTile!!.second
                                        Direction.fromDelta(dX, dY)
                                    } else {
                                        conveyorDirection
                                    }
                                    onDragConveyorState(curTileX, curTileY, dragDir)
                                    lastTouchedTile = currentTile
                                }
                            } else {
                                // Pan camera
                                val dx = currentPos.x - lastPanPos.x
                                val dy = currentPos.y - lastPanPos.y
                                onPanState(dx, dy)
                            }
                        }

                        lastPanPos = currentPos
                        activePointer.consume()
                    }
                }
            }
    ) {
        val currentTileSize = baseTileSize * zoom
        val visibleWidth = size.width
        val visibleHeight = size.height

        // Calculate visible tile range
        val minX = floor((-cameraOffsetX) / currentTileSize).toInt().coerceIn(0, world.width - 1)
        val maxX = ceil((visibleWidth - cameraOffsetX) / currentTileSize).toInt().coerceIn(0, world.width - 1)
        val minY = floor((-cameraOffsetY) / currentTileSize).toInt().coerceIn(0, world.height - 1)
        val maxY = ceil((visibleHeight - cameraOffsetY) / currentTileSize).toInt().coerceIn(0, world.height - 1)

        // 1. Draw Terrain Grid
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                val tile = world.grid[x][y]
                val screenX = cameraOffsetX + x * currentTileSize
                val screenY = cameraOffsetY + y * currentTileSize

                drawRect(
                    color = tile.type.baseColor,
                    topLeft = Offset(screenX, screenY),
                    size = Size(currentTileSize, currentTileSize)
                )

                when (tile.type) {
                    TileType.COPPER_ORE -> drawCopperOreAccents(screenX, screenY, currentTileSize, tile.variant)
                    TileType.LEAD_ORE -> drawLeadOreAccents(screenX, screenY, currentTileSize, tile.variant)
                    TileType.COAL_ORE -> drawCoalAccents(screenX, screenY, currentTileSize, tile.variant)
                    TileType.SILVER_ORE -> drawSilverOreAccents(screenX, screenY, currentTileSize, tile.variant)
                    TileType.IRON_ORE -> drawIronOreAccents(screenX, screenY, currentTileSize, tile.variant)
                    TileType.SCRAP_DEPOSIT -> drawScrapAccents(screenX, screenY, currentTileSize, tile.variant)
                    TileType.STONE -> drawStoneAccents(screenX, screenY, currentTileSize, tile.variant)
                    TileType.ROCK_WALL -> drawRockWall(screenX, screenY, currentTileSize)
                    TileType.DIRT -> {
                        drawCircle(
                            color = tile.type.accentColor,
                            radius = currentTileSize * 0.08f,
                            center = Offset(screenX + currentTileSize * 0.35f, screenY + currentTileSize * 0.4f)
                        )
                    }
                }

                if (showGridLines) {
                    drawRect(
                        color = Color(0x12FFFFFF),
                        topLeft = Offset(screenX, screenY),
                        size = Size(currentTileSize, currentTileSize),
                        style = Stroke(width = 1f)
                    )
                }
            }
        }

        // 2. Draw Power Links (Energy Grid)
        for (link in world.powerLinks) {
            val startPx = Offset(cameraOffsetX + link.first.first * currentTileSize, cameraOffsetY + link.first.second * currentTileSize)
            val endPx = Offset(cameraOffsetX + link.second.first * currentTileSize, cameraOffsetY + link.second.second * currentTileSize)

            drawLine(
                color = Color(0x55FBBF24),
                start = startPx,
                end = endPx,
                strokeWidth = 4f * zoom,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xEEFDE047),
                start = startPx,
                end = endPx,
                strokeWidth = 1.5f * zoom,
                cap = StrokeCap.Round
            )
        }

        // 3. Draw Constructed Buildings
        for (b in world.buildings) {
            val screenX = cameraOffsetX + b.originX * currentTileSize
            val screenY = cameraOffsetY + b.originY * currentTileSize
            val bWidthPx = b.type.width * currentTileSize
            val bHeightPx = b.type.height * currentTileSize

            when (b.type) {
                BuildingType.CORE -> drawCoreBuilding(screenX, screenY, bWidthPx, bHeightPx, b.health)
                BuildingType.CONVEYOR -> drawConveyorBelt(screenX, screenY, currentTileSize, b.direction, showFlowArrows, isGhost = false)
                BuildingType.ROUTER -> drawRouter(screenX, screenY, currentTileSize, isGhost = false)
                BuildingType.JUNCTION -> drawJunction(screenX, screenY, currentTileSize, isGhost = false)
                BuildingType.COPPER_DRILL -> drawCopperDrill(screenX, screenY, bWidthPx, bHeightPx, b.drillRotation, b.buildProgress)
                BuildingType.COAL_GENERATOR -> drawCoalGenerator(screenX, screenY, currentTileSize, b.isPowered, (b.storedResources[Resource.COAL] ?: 0) > 0, isGhost = false)
                BuildingType.POWER_NODE -> drawPowerNode(screenX, screenY, currentTileSize, isGhost = false)
                BuildingType.BATTERY -> drawBattery(screenX, screenY, currentTileSize, (b.energyStored / 200f).coerceIn(0f, 1f), isGhost = false)
                BuildingType.TURRET -> drawTurret(screenX, screenY, bWidthPx, bHeightPx, b.turretRotation, b.turretAmmoCount, b.turretAmmoType, isGhost = false)
                BuildingType.IRON_SMELTER -> drawIronSmelter(screenX, screenY, bWidthPx, bHeightPx, b.craftTimer, b.isPowered, isGhost = false)
                BuildingType.AMMO_FABRICATOR -> drawAmmoFabricator(screenX, screenY, bWidthPx, bHeightPx, b.fabricatorRecipe, b.craftTimer, b.isPowered, isGhost = false)
            }

            // Construction progress bar if building
            if (!b.isFullyBuilt && !b.isMarkedForDeconstruction) {
                drawProgressBar(
                    screenX = screenX,
                    screenY = screenY - 14f * zoom,
                    width = bWidthPx,
                    height = 8f * zoom,
                    progress = b.buildProgress,
                    color = Color(0xFF38BDF8)
                )
            }

            // Health bar if damaged
            if (b.isFullyBuilt && b.health < b.type.maxHealth) {
                drawProgressBar(
                    screenX = screenX,
                    screenY = screenY - 7f * zoom,
                    width = bWidthPx,
                    height = 5f * zoom,
                    progress = b.healthRatio,
                    color = if (b.healthRatio > 0.5f) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }

            // Deconstruction overlay / progress bar
            if (b.isMarkedForDeconstruction) {
                drawRect(
                    color = Color(0x55EF4444),
                    topLeft = Offset(screenX, screenY),
                    size = Size(bWidthPx, bHeightPx)
                )
                drawRect(
                    color = Color(0xFFEF4444),
                    topLeft = Offset(screenX, screenY),
                    size = Size(bWidthPx, bHeightPx),
                    style = Stroke(width = 2.5f * zoom)
                )

                if (b.deconstructProgress > 0f) {
                    drawProgressBar(
                        screenX = screenX,
                        screenY = screenY - 14f * zoom,
                        width = bWidthPx,
                        height = 8f * zoom,
                        progress = b.deconstructProgress,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }

        // 4. Draw Blueprints (ghost previews)
        for (bp in world.blueprints) {
            val screenX = cameraOffsetX + bp.originX * currentTileSize
            val screenY = cameraOffsetY + bp.originY * currentTileSize
            val bWidthPx = bp.type.width * currentTileSize
            val bHeightPx = bp.type.height * currentTileSize

            when (bp.type) {
                BuildingType.CONVEYOR -> drawConveyorBelt(screenX, screenY, currentTileSize, bp.direction, showFlowArrows, isGhost = true)
                BuildingType.ROUTER -> drawRouter(screenX, screenY, currentTileSize, isGhost = true)
                BuildingType.JUNCTION -> drawJunction(screenX, screenY, currentTileSize, isGhost = true)
                BuildingType.COPPER_DRILL -> drawCopperDrillGhost(screenX, screenY, bWidthPx, bHeightPx)
                BuildingType.COAL_GENERATOR -> drawCoalGenerator(screenX, screenY, currentTileSize, isPowered = false, hasCoal = false, isGhost = true)
                BuildingType.POWER_NODE -> drawPowerNode(screenX, screenY, currentTileSize, isGhost = true)
                BuildingType.BATTERY -> drawBattery(screenX, screenY, currentTileSize, 0f, isGhost = true)
                BuildingType.TURRET -> drawTurret(screenX, screenY, bWidthPx, bHeightPx, 0f, 0, null, isGhost = true)
                BuildingType.IRON_SMELTER -> drawIronSmelter(screenX, screenY, bWidthPx, bHeightPx, 0f, false, isGhost = true)
                BuildingType.AMMO_FABRICATOR -> drawAmmoFabricator(screenX, screenY, bWidthPx, bHeightPx, "copper", 0f, false, isGhost = true)
                BuildingType.CORE -> {}
            }

            drawRect(
                color = Color(0x9938BDF8),
                topLeft = Offset(screenX, screenY),
                size = Size(bWidthPx, bHeightPx),
                style = Stroke(width = 2.5f * zoom)
            )
        }

        // 5. Draw Items on Conveyors / Routers / Junctions
        for (item in world.items) {
            val fromX = cameraOffsetX + (item.tileX + 0.5f) * currentTileSize
            val fromY = cameraOffsetY + (item.tileY + 0.5f) * currentTileSize
            val itemScreenX = fromX + (item.direction.dx * (item.progress - 0.5f)) * currentTileSize
            val itemScreenY = fromY + (item.direction.dy * (item.progress - 0.5f)) * currentTileSize

            drawTransportedItem(itemScreenX, itemScreenY, currentTileSize * 0.28f, item.resource)
        }

        // 6. Draw Projectiles
        for (p in world.projectiles) {
            val px = cameraOffsetX + p.posX * currentTileSize
            val py = cameraOffsetY + p.posY * currentTileSize
            drawCircle(
                color = p.color,
                radius = 3.8f * zoom,
                center = Offset(px, py)
            )
            drawCircle(
                color = Color.White,
                radius = 1.8f * zoom,
                center = Offset(px, py)
            )
        }

        // 7. Draw Units (Tal Mechs)
        for (u in world.units) {
            drawTalMech(u, cameraOffsetX, cameraOffsetY, currentTileSize, zoom)
        }

        // 8. Draw Demolish Area Drag Box
        if (demolishDragStart != null && demolishDragCurrent != null) {
            val sX = min(demolishDragStart.first, demolishDragCurrent.first)
            val eX = max(demolishDragStart.first, demolishDragCurrent.first)
            val sY = min(demolishDragStart.second, demolishDragCurrent.second)
            val eY = max(demolishDragStart.second, demolishDragCurrent.second)

            val boxScreenX = cameraOffsetX + sX * currentTileSize
            val boxScreenY = cameraOffsetY + sY * currentTileSize
            val boxWidth = (eX - sX + 1) * currentTileSize
            val boxHeight = (eY - sY + 1) * currentTileSize

            drawRect(
                color = Color(0x44EF4444),
                topLeft = Offset(boxScreenX, boxScreenY),
                size = Size(boxWidth, boxHeight)
            )
            drawRect(
                color = Color(0xFFEF4444),
                topLeft = Offset(boxScreenX, boxScreenY),
                size = Size(boxWidth, boxHeight),
                style = Stroke(width = 2f * zoom)
            )
        }

        // 9. Draw Smoke Particles
        for (smoke in world.smokeParticles) {
            val sx = cameraOffsetX + smoke.x * currentTileSize
            val sy = cameraOffsetY + smoke.y * currentTileSize
            drawCircle(
                color = Color(0xFF1E293B).copy(alpha = smoke.alpha * 0.75f),
                radius = smoke.size * zoom,
                center = Offset(sx, sy)
            )
        }

        // 10. Draw Builder Drone and Laser (Upgraded Sci-Fi Model)
        drawUpgradedBuilderDrone(world.drone, cameraOffsetX, cameraOffsetY, currentTileSize, zoom)

        // 11. Draw Sparks
        for (spark in world.sparks) {
            val sx = cameraOffsetX + spark.x * currentTileSize
            val sy = cameraOffsetY + spark.y * currentTileSize
            drawCircle(
                color = spark.color.copy(alpha = spark.alpha),
                radius = 3.5f * zoom,
                center = Offset(sx, sy)
            )
        }

        // 12. Draw Floating Texts
        drawIntoCanvas { canvas ->
            for (txt in world.floatingTexts) {
                val sx = cameraOffsetX + txt.x * currentTileSize
                val sy = cameraOffsetY + txt.y * currentTileSize
                nativeTextPaint.color = txt.color.toArgb()
                nativeTextPaint.alpha = (txt.alpha * 255).toInt().coerceIn(0, 255)
                canvas.nativeCanvas.drawText(txt.text, sx, sy, nativeTextPaint)
            }
        }
    }
}

// ----------------- ORE DRAWING HELPERS -----------------

private fun DrawScope.drawCopperOreAccents(x: Float, y: Float, size: Float, variant: Int) {
    val amber = Color(0xFFF59E0B)
    val gold = Color(0xFFFBBF24)
    val dark = Color(0xFFB45309)
    val c1 = Offset(x + size * 0.35f, y + size * 0.35f)
    val c2 = Offset(x + size * 0.7f, y + size * 0.65f)
    drawCircle(dark, size * 0.22f, c1)
    drawCircle(amber, size * 0.18f, c1)
    drawCircle(gold, size * 0.08f, Offset(c1.x - size * 0.04f, c1.y - size * 0.04f))
    drawCircle(dark, size * 0.18f, c2)
    drawCircle(amber, size * 0.14f, c2)
}

private fun DrawScope.drawLeadOreAccents(x: Float, y: Float, size: Float, variant: Int) {
    val purple = Color(0xFF8B5CF6)
    val darkViolet = Color(0xFF5B21B6)
    val c1 = Offset(x + size * 0.38f, y + size * 0.38f)
    val c2 = Offset(x + size * 0.68f, y + size * 0.62f)
    drawCircle(darkViolet, size * 0.24f, c1)
    drawCircle(purple, size * 0.18f, c1)
    drawCircle(darkViolet, size * 0.19f, c2)
    drawCircle(purple, size * 0.14f, c2)
}

private fun DrawScope.drawCoalAccents(x: Float, y: Float, size: Float, variant: Int) {
    val coalDark = Color(0xFF0F172A)
    val coalGray = Color(0xFF334155)
    val c1 = Offset(x + size * 0.35f, y + size * 0.45f)
    val c2 = Offset(x + size * 0.65f, y + size * 0.55f)
    drawRect(coalDark, Offset(c1.x - size * 0.15f, c1.y - size * 0.15f), Size(size * 0.3f, size * 0.3f))
    drawRect(coalGray, Offset(c1.x - size * 0.08f, c1.y - size * 0.08f), Size(size * 0.16f, size * 0.16f))
    drawRect(coalDark, Offset(c2.x - size * 0.12f, c2.y - size * 0.12f), Size(size * 0.24f, size * 0.24f))
}

private fun DrawScope.drawSilverOreAccents(x: Float, y: Float, size: Float, variant: Int) {
    val silverWhite = Color(0xFFF8FAFC)
    val silverShine = Color(0xFFCBD5E1)
    val silverDark = Color(0xFF475569)
    val c1 = Offset(x + size * 0.4f, y + size * 0.4f)
    val c2 = Offset(x + size * 0.7f, y + size * 0.6f)
    drawCircle(silverDark, size * 0.22f, c1)
    drawCircle(silverShine, size * 0.17f, c1)
    drawCircle(silverWhite, size * 0.08f, Offset(c1.x - size * 0.04f, c1.y - size * 0.04f))
    drawCircle(silverDark, size * 0.18f, c2)
    drawCircle(silverShine, size * 0.13f, c2)
}

private fun DrawScope.drawIronOreAccents(x: Float, y: Float, size: Float, variant: Int) {
    val rustBrown = Color(0xFF9A3412)
    val ironOrange = Color(0xFFB45309)
    val ironLight = Color(0xFFD97706)
    val c1 = Offset(x + size * 0.35f, y + size * 0.35f)
    val c2 = Offset(x + size * 0.65f, y + size * 0.65f)
    drawRect(rustBrown, Offset(c1.x - size * 0.18f, c1.y - size * 0.18f), Size(size * 0.36f, size * 0.36f))
    drawRect(ironOrange, Offset(c1.x - size * 0.12f, c1.y - size * 0.12f), Size(size * 0.24f, size * 0.24f))
    drawRect(ironLight, Offset(c1.x - size * 0.05f, c1.y - size * 0.05f), Size(size * 0.10f, size * 0.10f))
    drawRect(rustBrown, Offset(c2.x - size * 0.14f, c2.y - size * 0.14f), Size(size * 0.28f, size * 0.28f))
    drawRect(ironOrange, Offset(c2.x - size * 0.08f, c2.y - size * 0.08f), Size(size * 0.16f, size * 0.16f))
}

private fun DrawScope.drawScrapAccents(x: Float, y: Float, size: Float, variant: Int) {
    val dark = Color(0xFF334155)
    val light = Color(0xFF94A3B8)
    drawRect(dark, Offset(x + size * 0.25f, y + size * 0.3f), Size(size * 0.45f, size * 0.35f))
    drawRect(light, Offset(x + size * 0.35f, y + size * 0.4f), Size(size * 0.25f, size * 0.15f))
}

private fun DrawScope.drawStoneAccents(x: Float, y: Float, size: Float, variant: Int) {
    drawRect(Color(0xFF333E50), Offset(x + size * 0.2f, y + size * 0.2f), Size(size * 0.6f, size * 0.6f))
}

private fun DrawScope.drawRockWall(x: Float, y: Float, size: Float) {
    drawRect(Color(0xFF161F2E), Offset(x, y), Size(size, size))
    drawRect(Color(0xFF0F172A), Offset(x + size * 0.15f, y + size * 0.15f), Size(size * 0.7f, size * 0.7f))
    drawRect(Color(0xFF334155), Offset(x + size * 0.25f, y + size * 0.25f), Size(size * 0.5f, size * 0.5f), style = Stroke(width = 2f))
}

// ----------------- BUILDING DRAWING HELPERS -----------------

private fun DrawScope.drawCoreBuilding(x: Float, y: Float, width: Float, height: Float, health: Int) {
    drawRect(Color(0xFF0F172A), Offset(x, y), Size(width, height))
    drawRect(Color(0xFF1E293B), Offset(x + 4f, y + 4f), Size(width - 8f, height - 8f))
    drawRect(Color(0xFF0284C7), Offset(x + 12f, y + 12f), Size(width - 24f, height - 24f))
    drawCircle(Color(0xFF38BDF8), width * 0.22f, Offset(x + width / 2f, y + height / 2f))
    drawCircle(Color.White, width * 0.10f, Offset(x + width / 2f, y + height / 2f))
    drawRect(Color(0xFF38BDF8), Offset(x, y), Size(width, height), style = Stroke(width = 3f))
}

private fun DrawScope.drawConveyorBelt(x: Float, y: Float, size: Float, direction: Direction, showFlowArrows: Boolean, isGhost: Boolean) {
    val alpha = if (isGhost) 0.5f else 1.0f
    val baseCol = Color(0xFF334155).copy(alpha = alpha)
    val beltCol = Color(0xFF1E293B).copy(alpha = alpha)
    val arrowCol = Color(0xFFF59E0B).copy(alpha = alpha)

    drawRect(baseCol, Offset(x, y), Size(size, size))
    drawRect(beltCol, Offset(x + 4f, y + 4f), Size(size - 8f, size - 8f))

    if (showFlowArrows) {
        val cx = x + size / 2f
        val cy = y + size / 2f
        rotate(direction.angleDegrees, pivot = Offset(cx, cy)) {
            val arrowPath = Path().apply {
                moveTo(cx, cy - size * 0.32f)
                lineTo(cx + size * 0.22f, cy)
                lineTo(cx + size * 0.10f, cy)
                lineTo(cx + size * 0.10f, cy + size * 0.28f)
                lineTo(cx - size * 0.10f, cy + size * 0.28f)
                lineTo(cx - size * 0.10f, cy)
                lineTo(cx - size * 0.22f, cy)
                close()
            }
            drawPath(arrowPath, color = arrowCol)
        }
    }
}

private fun DrawScope.drawRouter(x: Float, y: Float, size: Float, isGhost: Boolean) {
    val alpha = if (isGhost) 0.5f else 1.0f
    drawRect(Color(0xFF1E293B).copy(alpha = alpha), Offset(x, y), Size(size, size))
    drawRect(Color(0xFF475569).copy(alpha = alpha), Offset(x + 4f, y + 4f), Size(size - 8f, size - 8f))
    drawCircle(Color(0xFFF59E0B).copy(alpha = alpha), size * 0.24f, Offset(x + size / 2f, y + size / 2f))
    drawCircle(Color(0xFF0F172A).copy(alpha = alpha), size * 0.12f, Offset(x + size / 2f, y + size / 2f))
}

private fun DrawScope.drawJunction(x: Float, y: Float, size: Float, isGhost: Boolean) {
    val alpha = if (isGhost) 0.5f else 1.0f
    drawRect(Color(0xFF1E293B).copy(alpha = alpha), Offset(x, y), Size(size, size))
    drawRect(Color(0xFF334155).copy(alpha = alpha), Offset(x + 3f, y + 3f), Size(size - 6f, size - 6f))
    drawRect(Color(0xFFE2E8F0).copy(alpha = alpha), Offset(x + size * 0.35f, y + 4f), Size(size * 0.3f, size - 8f))
    drawRect(Color(0xFFF59E0B).copy(alpha = alpha), Offset(x + 4f, y + size * 0.35f), Size(size - 8f, size * 0.3f))
    drawRect(Color(0xFFCBD5E1).copy(alpha = alpha), Offset(x, y), Size(size, size), style = Stroke(width = 1.5f))
}

private fun DrawScope.drawCopperDrill(x: Float, y: Float, width: Float, height: Float, rotation: Float, progress: Float) {
    drawRect(Color(0xFF0F172A), Offset(x, y), Size(width, height))
    drawRect(Color(0xFF78350F), Offset(x + 6f, y + 6f), Size(width - 12f, height - 12f))
    drawRect(Color(0xFFB45309), Offset(x + 12f, y + 12f), Size(width - 24f, height - 24f))

    val cx = x + width / 2f
    val cy = y + height / 2f
    rotate(rotation, pivot = Offset(cx, cy)) {
        drawRect(Color(0xFFF59E0B), Offset(cx - width * 0.35f, cy - height * 0.08f), Size(width * 0.7f, height * 0.16f))
        drawRect(Color(0xFFF59E0B), Offset(cx - width * 0.08f, cy - height * 0.35f), Size(width * 0.16f, height * 0.7f))
        drawCircle(Color(0xFFFDE68A), width * 0.14f, Offset(cx, cy))
        drawCircle(Color(0xFF78350F), width * 0.07f, Offset(cx, cy))
    }
}

private fun DrawScope.drawCopperDrillGhost(x: Float, y: Float, width: Float, height: Float) {
    drawRect(Color(0x55B45309), Offset(x, y), Size(width, height))
    drawRect(Color(0x99F59E0B), Offset(x, y), Size(width, height), style = Stroke(width = 2f))
}

private fun DrawScope.drawCoalGenerator(x: Float, y: Float, size: Float, isPowered: Boolean, hasCoal: Boolean, isGhost: Boolean) {
    val alpha = if (isGhost) 0.5f else 1.0f
    drawRect(Color(0xFF1E293B).copy(alpha = alpha), Offset(x, y), Size(size, size))
    drawRect(Color(0xFF334155).copy(alpha = alpha), Offset(x + 3f, y + 3f), Size(size - 6f, size - 6f))

    // Combustion chamber glow
    val fireColor = if (hasCoal) Color(0xFFF97316).copy(alpha = alpha) else Color(0xFF475569).copy(alpha = alpha)
    drawRect(fireColor, Offset(x + size * 0.25f, y + size * 0.35f), Size(size * 0.5f, size * 0.4f))
    if (hasCoal) {
        drawCircle(Color(0xFFFDE047).copy(alpha = alpha), size * 0.15f, Offset(x + size * 0.5f, y + size * 0.55f))
    }
    // Exhaust pipe
    drawCircle(Color(0xFF0F172A).copy(alpha = alpha), size * 0.12f, Offset(x + size * 0.5f, y + size * 0.22f))
    drawRect(Color(0xFF8B5CF6).copy(alpha = alpha), Offset(x, y), Size(size, size), style = Stroke(width = 1.5f))
}

private fun DrawScope.drawPowerNode(x: Float, y: Float, size: Float, isGhost: Boolean) {
    val alpha = if (isGhost) 0.5f else 1.0f
    drawRect(Color(0xFF0F172A).copy(alpha = alpha), Offset(x, y), Size(size, size))
    drawRect(Color(0xFF334155).copy(alpha = alpha), Offset(x + 4f, y + 4f), Size(size - 8f, size - 8f))

    val cx = x + size / 2f
    val cy = y + size / 2f
    // Pylon diamond
    val path = Path().apply {
        moveTo(cx, cy - size * 0.38f)
        lineTo(cx + size * 0.38f, cy)
        lineTo(cx, cy + size * 0.38f)
        lineTo(cx - size * 0.38f, cy)
        close()
    }
    drawPath(path, color = Color(0xFFFDE047).copy(alpha = alpha), style = Stroke(width = 2f))
    drawCircle(Color(0xFFFACC15).copy(alpha = alpha), size * 0.18f, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = alpha), size * 0.08f, Offset(cx, cy))
}

private fun DrawScope.drawBattery(x: Float, y: Float, size: Float, energyRatio: Float, isGhost: Boolean) {
    val alpha = if (isGhost) 0.5f else 1.0f
    drawRect(Color(0xFF0F172A).copy(alpha = alpha), Offset(x, y), Size(size, size))
    drawRect(Color(0xFF1E293B).copy(alpha = alpha), Offset(x + 3f, y + 3f), Size(size - 6f, size - 6f))

    // Battery bar inside
    val barHeight = (size - 12f) * energyRatio
    if (barHeight > 0f) {
        val barColor = if (energyRatio > 0.3f) Color(0xFF10B981) else Color(0xFFEF4444)
        drawRect(
            color = barColor.copy(alpha = alpha),
            topLeft = Offset(x + 6f, y + size - 6f - barHeight),
            size = Size(size - 12f, barHeight)
        )
    }
    drawRect(Color(0xFF8B5CF6).copy(alpha = alpha), Offset(x, y), Size(size, size), style = Stroke(width = 1.5f))
}

private fun DrawScope.drawTurret(x: Float, y: Float, width: Float, height: Float, rotation: Float, ammoCount: Int, ammoType: Resource?, isGhost: Boolean) {
    val alpha = if (isGhost) 0.5f else 1.0f
    drawRect(Color(0xFF0F172A).copy(alpha = alpha), Offset(x, y), Size(width, height))
    drawRect(Color(0xFF334155).copy(alpha = alpha), Offset(x + 5f, y + 5f), Size(width - 10f, height - 10f))

    val cx = x + width / 2f
    val cy = y + height / 2f

    // Turret ring
    drawCircle(Color(0xFF1E293B).copy(alpha = alpha), width * 0.34f, Offset(cx, cy))

    // Rotating twin cannons
    rotate(rotation, pivot = Offset(cx, cy)) {
        val cannonColor = if (ammoType == Resource.SILVER_AMMO) Color(0xFF38BDF8) else Color(0xFFF97316)
        // Barrel 1 & Barrel 2
        drawRect(cannonColor.copy(alpha = alpha), Offset(cx, cy - height * 0.14f), Size(width * 0.44f, height * 0.09f))
        drawRect(cannonColor.copy(alpha = alpha), Offset(cx, cy + height * 0.05f), Size(width * 0.44f, height * 0.09f))
        // Central dome
        drawCircle(Color(0xFF64748B).copy(alpha = alpha), width * 0.22f, Offset(cx, cy))
        drawCircle(Color(0xFFF1F5F9).copy(alpha = alpha), width * 0.10f, Offset(cx, cy))
    }

    drawRect(Color(0xFFEF4444).copy(alpha = alpha), Offset(x, y), Size(width, height), style = Stroke(width = 2f))
}

private fun DrawScope.drawIronSmelter(x: Float, y: Float, width: Float, height: Float, craftProgress: Float, isPowered: Boolean, isGhost: Boolean) {
    val alpha = if (isGhost) 0.5f else 1.0f
    drawRect(Color(0xFF0F172A).copy(alpha = alpha), Offset(x, y), Size(width, height))
    drawRect(Color(0xFF431407).copy(alpha = alpha), Offset(x + 6f, y + 6f), Size(width - 12f, height - 12f))

    val cx = x + width / 2f
    val cy = y + height / 2f

    // Molten iron vat
    val vatColor = if (isPowered) Color(0xFFEA580C) else Color(0xFF78350F)
    drawCircle(vatColor.copy(alpha = alpha), width * 0.30f, Offset(cx, cy))
    drawCircle(Color(0xFFFDE047).copy(alpha = alpha), width * 0.16f, Offset(cx, cy))

    drawRect(Color(0xFF9A3412).copy(alpha = alpha), Offset(x, y), Size(width, height), style = Stroke(width = 2f))
}

private fun DrawScope.drawAmmoFabricator(x: Float, y: Float, width: Float, height: Float, recipe: String, craftProgress: Float, isPowered: Boolean, isGhost: Boolean) {
    val alpha = if (isGhost) 0.5f else 1.0f
    drawRect(Color(0xFF0F172A).copy(alpha = alpha), Offset(x, y), Size(width, height))
    drawRect(Color(0xFF1E293B).copy(alpha = alpha), Offset(x + 6f, y + 6f), Size(width - 12f, height - 12f))

    val cx = x + width / 2f
    val cy = y + height / 2f

    val badgeColor = if (recipe == "silver") Color(0xFF38BDF8) else Color(0xFFF97316)
    drawRect(badgeColor.copy(alpha = alpha), Offset(cx - width * 0.3f, cy - height * 0.3f), Size(width * 0.6f, height * 0.6f))
    drawCircle(Color(0xFF0F172A).copy(alpha = alpha), width * 0.18f, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = alpha), width * 0.08f, Offset(cx, cy))

    drawRect(Color(0xFF38BDF8).copy(alpha = alpha), Offset(x, y), Size(width, height), style = Stroke(width = 2f))
}

// ----------------- UNIT & DRONE DRAWING HELPERS -----------------

private fun DrawScope.drawTalMech(unit: UnitInstance, cameraOffsetX: Float, cameraOffsetY: Float, tileSize: Float, zoom: Float) {
    val screenX = cameraOffsetX + unit.posX * tileSize
    val screenY = cameraOffsetY + unit.posY * tileSize
    val r = 16f * zoom

    // Animated robotic walking legs
    val legOffset = sin(unit.walkAnimTime) * 6f * zoom
    drawLine(
        color = Color(0xFF450A0A),
        start = Offset(screenX - r * 0.4f, screenY - r * 0.2f),
        end = Offset(screenX - r * 0.5f, screenY + r * 0.8f + legOffset),
        strokeWidth = 3f * zoom,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF450A0A),
        start = Offset(screenX + r * 0.4f, screenY - r * 0.2f),
        end = Offset(screenX + r * 0.5f, screenY + r * 0.8f - legOffset),
        strokeWidth = 3f * zoom,
        cap = StrokeCap.Round
    )

    // Rotating armored torso
    rotate(unit.rotation, pivot = Offset(screenX, screenY)) {
        // Blaster cannons
        val blasterColor = if (unit.isFiring) Color(0xFFFCA5A5) else Color(0xFF991B1B)
        drawRect(blasterColor, Offset(screenX + r * 0.3f, screenY - r * 0.6f), Size(r * 0.8f, r * 0.25f))
        drawRect(blasterColor, Offset(screenX + r * 0.3f, screenY + r * 0.35f), Size(r * 0.8f, r * 0.25f))

        // Torso armor
        drawCircle(Color(0xFF7F1D1D), r * 0.7f, Offset(screenX, screenY))
        drawCircle(Color(0xFFEF4444), r * 0.35f, Offset(screenX, screenY))
    }

    // Health bar above mech
    val hbWidth = 32f * zoom
    val hbHeight = 4f * zoom
    val hbX = screenX - hbWidth / 2f
    val hbY = screenY - r * 1.5f
    drawRect(Color(0xFF0F172A), Offset(hbX, hbY), Size(hbWidth, hbHeight))
    drawRect(Color(0xFFEF4444), Offset(hbX, hbY), Size(hbWidth * unit.healthRatio, hbHeight))
}

private fun DrawScope.drawUpgradedBuilderDrone(
    drone: BuilderDrone,
    cameraOffsetX: Float,
    cameraOffsetY: Float,
    tileSize: Float,
    zoom: Float
) {
    val droneScreenX = cameraOffsetX + drone.posX * tileSize
    val droneScreenY = cameraOffsetY + drone.posY * tileSize

    if (drone.isFiringLaser) {
        val targetScreenX = cameraOffsetX + drone.laserBeamX * tileSize
        val targetScreenY = cameraOffsetY + drone.laserBeamY * tileSize

        val beamGlowColor = if (drone.isDeconstructing) Color(0x88EF4444) else Color(0x8838BDF8)
        val beamCoreColor = if (drone.isDeconstructing) Color(0xFFFCA5A5) else Color.White

        drawLine(beamGlowColor, Offset(droneScreenX, droneScreenY), Offset(targetScreenX, targetScreenY), strokeWidth = 8f * zoom, cap = StrokeCap.Round)
        drawLine(beamCoreColor, Offset(droneScreenX, droneScreenY), Offset(targetScreenX, targetScreenY), strokeWidth = 3f * zoom, cap = StrokeCap.Round)
        drawCircle(if (drone.isDeconstructing) Color(0xFFEF4444) else Color(0xFF38BDF8), radius = 7f * zoom, center = Offset(targetScreenX, targetScreenY))
        drawCircle(Color.White, radius = 3.5f * zoom, center = Offset(targetScreenX, targetScreenY))
    }

    val r = 16f * zoom

    // Thruster engine glow trails
    val thrusterColor = if (drone.isDeconstructing) Color(0x99EF4444) else Color(0x990284C7)
    drawCircle(thrusterColor, r * 1.4f, Offset(droneScreenX, droneScreenY))

    // Swept-wing hull
    val hullPath = Path().apply {
        moveTo(droneScreenX, droneScreenY - r * 1.1f)
        lineTo(droneScreenX + r * 1.1f, droneScreenY + r * 0.7f)
        lineTo(droneScreenX + r * 0.3f, droneScreenY + r * 0.4f)
        lineTo(droneScreenX, droneScreenY + r * 0.8f)
        lineTo(droneScreenX - r * 0.3f, droneScreenY + r * 0.4f)
        lineTo(droneScreenX - r * 1.1f, droneScreenY + r * 0.7f)
        close()
    }
    drawPath(hullPath, color = Color(0xFF0F172A), style = Fill)
    drawPath(hullPath, color = if (drone.isDeconstructing) Color(0xFFEF4444) else Color(0xFF38BDF8), style = Stroke(width = 2.2f * zoom))

    // Twin jet turbines
    drawRect(Color(0xFF334155), Offset(droneScreenX - r * 0.9f, droneScreenY - r * 0.1f), Size(r * 0.35f, r * 0.6f))
    drawRect(Color(0xFF334155), Offset(droneScreenX + r * 0.55f, droneScreenY - r * 0.1f), Size(r * 0.35f, r * 0.6f))

    // Glowing optic core
    drawCircle(if (drone.isDeconstructing) Color(0xFFEF4444) else Color(0xFF38BDF8), radius = r * 0.32f, center = Offset(droneScreenX, droneScreenY))
    drawCircle(Color.White, radius = r * 0.14f, center = Offset(droneScreenX, droneScreenY))
}

private fun DrawScope.drawTransportedItem(x: Float, y: Float, radius: Float, resource: Resource) {
    drawCircle(Color(0xFF0F172A), radius * 1.2f, Offset(x, y))
    drawCircle(resource.color, radius, Offset(x, y))
    drawCircle(Color.White.copy(alpha = 0.55f), radius * 0.35f, Offset(x - radius * 0.25f, y - radius * 0.25f))
}

private fun DrawScope.drawProgressBar(screenX: Float, screenY: Float, width: Float, height: Float, progress: Float, color: Color) {
    drawRect(Color(0xFF0F172A), Offset(screenX, screenY), Size(width, height))
    drawRect(color, Offset(screenX + 1f, screenY + 1f), Size((width - 2f) * progress.coerceIn(0f, 1f), height - 2f))
    drawRect(Color.White.copy(alpha = 0.4f), Offset(screenX, screenY), Size(width, height), style = Stroke(width = 1f))
}
