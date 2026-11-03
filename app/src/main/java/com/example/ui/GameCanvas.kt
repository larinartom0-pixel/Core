package com.example.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
    conveyorDirection: Direction,
    onTapTile: (Int, Int) -> Unit,
    onDragConveyorSegment: (Int, Int, Direction) -> Unit,
    onPanCamera: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val baseTileSize = 48f

    // Updated states so pointerInput(Unit) stays alive continuously without cancellation
    val currentCameraOffsetX by rememberUpdatedState(cameraOffsetX)
    val currentCameraOffsetY by rememberUpdatedState(cameraOffsetY)
    val currentZoom by rememberUpdatedState(zoom)
    val currentTool by rememberUpdatedState(selectedTool)
    val currentDir by rememberUpdatedState(conveyorDirection)
    val onPanState by rememberUpdatedState(onPanCamera)
    val onTapState by rememberUpdatedState(onTapTile)
    val onDragConveyorState by rememberUpdatedState(onDragConveyorSegment)

    val nativeTextPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    var prevPos = startPos
                    var totalDragDist = 0f
                    var isDrag = false
                    val touchSlop = viewConfiguration.touchSlop
                    var lastTileX = -1
                    var lastTileY = -1

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.changedToUp()) {
                            if (!isDrag && totalDragDist < touchSlop) {
                                // Tap confirmed!
                                val curTileSize = baseTileSize * currentZoom
                                val worldX = (startPos.x - currentCameraOffsetX) / curTileSize
                                val worldY = (startPos.y - currentCameraOffsetY) / curTileSize
                                val tileX = floor(worldX).toInt()
                                val tileY = floor(worldY).toInt()
                                if (tileX in 0 until world.width && tileY in 0 until world.height) {
                                    onTapState(tileX, tileY)
                                }
                            }
                            break
                        }

                        val currentPos = change.position
                        val dx = currentPos.x - prevPos.x
                        val dy = currentPos.y - prevPos.y
                        totalDragDist += hypot(dx, dy)

                        if (!isDrag && totalDragDist >= touchSlop) {
                            isDrag = true
                            if (currentTool == BuildingType.CONVEYOR) {
                                val curTileSize = baseTileSize * currentZoom
                                val worldX = (startPos.x - currentCameraOffsetX) / curTileSize
                                val worldY = (startPos.y - currentCameraOffsetY) / curTileSize
                                lastTileX = floor(worldX).toInt()
                                lastTileY = floor(worldY).toInt()
                            }
                        }

                        if (isDrag) {
                            change.consume()
                            if (currentTool == BuildingType.CONVEYOR) {
                                val curTileSize = baseTileSize * currentZoom
                                val worldX = (currentPos.x - currentCameraOffsetX) / curTileSize
                                val worldY = (currentPos.y - currentCameraOffsetY) / curTileSize
                                val currTileX = floor(worldX).toInt()
                                val currTileY = floor(worldY).toInt()

                                if (lastTileX != -1 && (currTileX != lastTileX || currTileY != lastTileY)) {
                                    val deltaTileX = currTileX - lastTileX
                                    val deltaTileY = currTileY - lastTileY
                                    val dragDir = Direction.fromDelta(deltaTileX, deltaTileY)
                                    onDragConveyorState(currTileX, currTileY, dragDir)
                                    lastTileX = currTileX
                                    lastTileY = currTileY
                                }
                            } else {
                                // Pan camera smoothly
                                onPanState(dx, dy)
                            }
                        }

                        prevPos = currentPos
                    }
                }
            }
    ) {
        // Read tick to guarantee redraw on frame update without recreating the Canvas composable
        val frameTick = tick
        val currentTileSize = baseTileSize * zoom
        val viewW = size.width
        val viewH = size.height

        // Calculate visible tile range (culling)
        val minX = max(0, floor(-cameraOffsetX / currentTileSize).toInt())
        val maxX = min(world.width - 1, ceil((viewW - cameraOffsetX) / currentTileSize).toInt())
        val minY = max(0, floor(-cameraOffsetY / currentTileSize).toInt())
        val maxY = min(world.height - 1, ceil((viewH - cameraOffsetY) / currentTileSize).toInt())

        // 1. Draw Ground Tiles
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

                // Ore or rock details
                when (tile.type) {
                    TileType.COPPER_ORE -> {
                        drawCopperOreAccents(screenX, screenY, currentTileSize, tile.variant)
                    }
                    TileType.SCRAP_DEPOSIT -> {
                        drawScrapAccents(screenX, screenY, currentTileSize, tile.variant)
                    }
                    TileType.STONE -> {
                        drawStoneAccents(screenX, screenY, currentTileSize, tile.variant)
                    }
                    TileType.ROCK_WALL -> {
                        drawRockWall(screenX, screenY, currentTileSize)
                    }
                    TileType.DIRT -> {
                        // Subtle gravel texture
                        drawCircle(
                            color = tile.type.accentColor,
                            radius = currentTileSize * 0.08f,
                            center = Offset(screenX + currentTileSize * 0.35f, screenY + currentTileSize * 0.4f)
                        )
                    }
                }

                // Grid lines if enabled
                if (showGridLines) {
                    drawRect(
                        color = Color(0x15FFFFFF),
                        topLeft = Offset(screenX, screenY),
                        size = Size(currentTileSize, currentTileSize),
                        style = Stroke(width = 1f)
                    )
                }
            }
        }

        // 2. Draw Constructed Buildings
        for (b in world.buildings) {
            val screenX = cameraOffsetX + b.originX * currentTileSize
            val screenY = cameraOffsetY + b.originY * currentTileSize
            val bWidthPx = b.type.width * currentTileSize
            val bHeightPx = b.type.height * currentTileSize

            when (b.type) {
                BuildingType.CORE -> {
                    drawCoreBuilding(screenX, screenY, bWidthPx, bHeightPx, b.health)
                }
                BuildingType.CONVEYOR -> {
                    drawConveyorBelt(screenX, screenY, currentTileSize, b.direction, showFlowArrows, isGhost = false)
                }
                BuildingType.COPPER_DRILL -> {
                    drawCopperDrill(screenX, screenY, bWidthPx, bHeightPx, b.drillRotation, b.buildProgress)
                }
            }

            // Construction progress bar if under construction
            if (!b.isFullyBuilt) {
                drawProgressBar(
                    screenX = screenX,
                    screenY = screenY - 14f * zoom,
                    width = bWidthPx,
                    height = 8f * zoom,
                    progress = b.buildProgress,
                    color = Color(0xFF38BDF8)
                )
            }
        }

        // 3. Draw Blueprints (ghost previews)
        for (bp in world.blueprints) {
            val screenX = cameraOffsetX + bp.originX * currentTileSize
            val screenY = cameraOffsetY + bp.originY * currentTileSize
            val bWidthPx = bp.type.width * currentTileSize
            val bHeightPx = bp.type.height * currentTileSize

            when (bp.type) {
                BuildingType.CONVEYOR -> {
                    drawConveyorBelt(screenX, screenY, currentTileSize, bp.direction, showFlowArrows, isGhost = true)
                }
                BuildingType.COPPER_DRILL -> {
                    drawCopperDrillGhost(screenX, screenY, bWidthPx, bHeightPx)
                }
                BuildingType.CORE -> {
                    // Not placed via player menu
                }
            }

            // Blueprint holographic border
            drawRect(
                color = Color(0x9938BDF8),
                topLeft = Offset(screenX, screenY),
                size = Size(bWidthPx, bHeightPx),
                style = Stroke(width = 2.5f * zoom)
            )
        }

        // 4. Draw Items on Conveyors
        for (item in world.items) {
            val fromX = cameraOffsetX + (item.tileX + 0.5f) * currentTileSize
            val fromY = cameraOffsetY + (item.tileY + 0.5f) * currentTileSize
            val itemScreenX = fromX + (item.direction.dx * (item.progress - 0.5f)) * currentTileSize
            val itemScreenY = fromY + (item.direction.dy * (item.progress - 0.5f)) * currentTileSize

            drawTransportedItem(itemScreenX, itemScreenY, currentTileSize * 0.28f, item.resource)
        }

        // 5. Draw Builder Drone and Construction Laser
        drawBuilderDroneAndLaser(world.drone, cameraOffsetX, cameraOffsetY, currentTileSize, zoom)

        // 6. Draw Sparks
        for (spark in world.sparks) {
            val sx = cameraOffsetX + spark.x * currentTileSize
            val sy = cameraOffsetY + spark.y * currentTileSize
            drawCircle(
                color = spark.color.copy(alpha = spark.alpha),
                radius = 3.5f * zoom,
                center = Offset(sx, sy)
            )
        }

        // 7. Draw Floating Texts
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

private fun DrawScope.drawCopperOreAccents(x: Float, y: Float, size: Float, variant: Int) {
    val amber = Color(0xFFF59E0B)
    val gold = Color(0xFFFBBF24)
    val dark = Color(0xFFB45309)

    val c1 = Offset(x + size * 0.35f, y + size * 0.35f)
    val c2 = Offset(x + size * 0.7f, y + size * 0.65f)
    val c3 = Offset(x + size * 0.3f, y + size * 0.75f)

    drawCircle(dark, size * 0.22f, c1)
    drawCircle(amber, size * 0.18f, c1)
    drawCircle(gold, size * 0.08f, Offset(c1.x - size * 0.04f, c1.y - size * 0.04f))

    drawCircle(dark, size * 0.18f, c2)
    drawCircle(amber, size * 0.14f, c2)

    if (variant % 2 == 0) {
        drawCircle(dark, size * 0.14f, c3)
        drawCircle(amber, size * 0.10f, c3)
    }
}

private fun DrawScope.drawScrapAccents(x: Float, y: Float, size: Float, variant: Int) {
    val scrapColor = Color(0xFF94A3B8)
    drawRect(
        color = scrapColor,
        topLeft = Offset(x + size * 0.3f, y + size * 0.3f),
        size = Size(size * 0.35f, size * 0.2f)
    )
}

private fun DrawScope.drawStoneAccents(x: Float, y: Float, size: Float, variant: Int) {
    drawLine(
        color = Color(0xFF333E50),
        start = Offset(x + size * 0.2f, y + size * 0.3f),
        end = Offset(x + size * 0.8f, y + size * 0.7f),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawRockWall(x: Float, y: Float, size: Float) {
    drawRect(
        color = Color(0xFF0F141C),
        topLeft = Offset(x, y),
        size = Size(size, size)
    )
    drawRect(
        color = Color(0xFF1E2634),
        topLeft = Offset(x + size * 0.1f, y + size * 0.1f),
        size = Size(size * 0.8f, size * 0.8f)
    )
}

private fun DrawScope.drawCoreBuilding(x: Float, y: Float, width: Float, height: Float, health: Int) {
    // Outer heavy plating
    drawRect(
        color = Color(0xFF131924),
        topLeft = Offset(x, y),
        size = Size(width, height)
    )
    drawRect(
        color = Color(0xFF1E2838),
        topLeft = Offset(x + width * 0.06f, y + height * 0.06f),
        size = Size(width * 0.88f, height * 0.88f)
    )

    // Corner reinforced brackets
    val bracketSize = width * 0.18f
    val bracketColor = Color(0xFF334155)
    drawRect(bracketColor, Offset(x, y), Size(bracketSize, bracketSize))
    drawRect(bracketColor, Offset(x + width - bracketSize, y), Size(bracketSize, bracketSize))
    drawRect(bracketColor, Offset(x, y + height - bracketSize), Size(bracketSize, bracketSize))
    drawRect(bracketColor, Offset(x + width - bracketSize, y + height - bracketSize), Size(bracketSize, bracketSize))

    // Central core reactor ring
    val center = Offset(x + width / 2f, y + height / 2f)
    drawCircle(
        color = Color(0xFF0F172A),
        radius = width * 0.32f,
        center = center
    )
    drawCircle(
        color = Color(0xFF0284C7),
        radius = width * 0.26f,
        center = center
    )
    drawCircle(
        color = Color(0xFF38BDF8),
        radius = width * 0.20f,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = width * 0.10f,
        center = center
    )

    // Tech lines / solar conduits
    drawLine(
        color = Color(0xFF38BDF8),
        start = Offset(x + width * 0.15f, center.y),
        end = Offset(x + width * 0.85f, center.y),
        strokeWidth = 3f
    )
    drawLine(
        color = Color(0xFF38BDF8),
        start = Offset(center.x, y + height * 0.15f),
        end = Offset(center.x, y + height * 0.85f),
        strokeWidth = 3f
    )
}

private fun DrawScope.drawConveyorBelt(
    x: Float,
    y: Float,
    size: Float,
    direction: Direction,
    showArrows: Boolean,
    isGhost: Boolean
) {
    val alpha = if (isGhost) 0.5f else 1.0f
    val beltColor = Color(0xFF27303F).copy(alpha = alpha)
    val railColor = Color(0xFF475569).copy(alpha = alpha)
    val arrowColor = Color(0xFF38BDF8).copy(alpha = if (isGhost) 0.6f else 0.85f)

    // Base belt
    drawRect(beltColor, Offset(x, y), Size(size, size))

    // Rails along sides perpendicular to direction
    when (direction) {
        Direction.NORTH, Direction.SOUTH -> {
            drawRect(railColor, Offset(x, y), Size(size * 0.12f, size))
            drawRect(railColor, Offset(x + size * 0.88f, y), Size(size * 0.12f, size))
        }
        Direction.EAST, Direction.WEST -> {
            drawRect(railColor, Offset(x, y), Size(size, size * 0.12f))
            drawRect(railColor, Offset(x, y + size * 0.88f), Size(size, size * 0.12f))
        }
    }

    // Directional chevron arrow
    if (showArrows || isGhost) {
        val center = Offset(x + size / 2f, y + size / 2f)
        rotate(degrees = direction.angleDegrees, pivot = center) {
            val arrowPath = Path().apply {
                moveTo(center.x, center.y - size * 0.28f)
                lineTo(center.x + size * 0.22f, center.y + size * 0.15f)
                lineTo(center.x, center.y + size * 0.05f)
                lineTo(center.x - size * 0.22f, center.y + size * 0.15f)
                close()
            }
            drawPath(arrowPath, color = arrowColor, style = Fill)
        }
    }
}

private fun DrawScope.drawCopperDrill(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    rotation: Float,
    buildProgress: Float
) {
    val center = Offset(x + width / 2f, y + height / 2f)

    // Housing frame
    drawRect(
        color = Color(0xFF1E2530),
        topLeft = Offset(x, y),
        size = Size(width, height)
    )
    drawRect(
        color = Color(0xFF334155),
        topLeft = Offset(x + width * 0.08f, y + height * 0.08f),
        size = Size(width * 0.84f, height * 0.84f)
    )

    // Corner yellow hazard marks
    val hazardSize = width * 0.22f
    drawRect(Color(0xFFD97706), Offset(x, y), Size(hazardSize, hazardSize * 0.5f))
    drawRect(Color(0xFFD97706), Offset(x + width - hazardSize, y), Size(hazardSize, hazardSize * 0.5f))

    // Center circular drill chamber
    drawCircle(Color(0xFF0F141C), width * 0.36f, center)

    // Rotating drill teeth
    rotate(degrees = rotation, pivot = center) {
        val bladeLength = width * 0.32f
        val bladeWidth = width * 0.09f
        val bladeColor = Color(0xFFF59E0B)

        drawRect(
            color = bladeColor,
            topLeft = Offset(center.x - bladeWidth / 2f, center.y - bladeLength),
            size = Size(bladeWidth, bladeLength * 2f)
        )
        drawRect(
            color = bladeColor,
            topLeft = Offset(center.x - bladeLength, center.y - bladeWidth / 2f),
            size = Size(bladeLength * 2f, bladeWidth)
        )
    }

    // Drill center cap
    drawCircle(Color(0xFF1E293B), width * 0.14f, center)
    drawCircle(Color(0xFFF59E0B), width * 0.08f, center)
}

private fun DrawScope.drawCopperDrillGhost(x: Float, y: Float, width: Float, height: Float) {
    drawRect(
        color = Color(0x66F59E0B),
        topLeft = Offset(x, y),
        size = Size(width, height)
    )
    drawCircle(
        color = Color(0x88F59E0B),
        radius = width * 0.35f,
        center = Offset(x + width / 2f, y + height / 2f),
        style = Stroke(width = 3f)
    )
}

private fun DrawScope.drawProgressBar(
    screenX: Float,
    screenY: Float,
    width: Float,
    height: Float,
    progress: Float,
    color: Color
) {
    // Background bar
    drawRect(
        color = Color(0xCC0F172A),
        topLeft = Offset(screenX, screenY),
        size = Size(width, height)
    )
    // Fill
    drawRect(
        color = color,
        topLeft = Offset(screenX, screenY),
        size = Size(width * progress.coerceIn(0f, 1f), height)
    )
    // Border
    drawRect(
        color = Color.White,
        topLeft = Offset(screenX, screenY),
        size = Size(width, height),
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawTransportedItem(x: Float, y: Float, radius: Float, resource: Resource) {
    val center = Offset(x, y)
    // Shadow
    drawCircle(Color(0x66000000), radius * 1.15f, Offset(x + 2f, y + 2f))

    // Faceted nugget
    drawCircle(resource.color, radius, center)
    drawCircle(Color(0xFFFDE68A), radius * 0.45f, Offset(x - radius * 0.25f, y - radius * 0.25f))
}

private fun DrawScope.drawBuilderDroneAndLaser(
    drone: BuilderDrone,
    cameraOffsetX: Float,
    cameraOffsetY: Float,
    tileSize: Float,
    zoom: Float
) {
    val droneScreenX = cameraOffsetX + drone.posX * tileSize
    val droneScreenY = cameraOffsetY + drone.posY * tileSize

    // Laser beam if building
    if (drone.isFiringLaser) {
        val targetScreenX = cameraOffsetX + drone.laserBeamX * tileSize
        val targetScreenY = cameraOffsetY + drone.laserBeamY * tileSize

        // Outer glow beam
        drawLine(
            color = Color(0x8838BDF8),
            start = Offset(droneScreenX, droneScreenY),
            end = Offset(targetScreenX, targetScreenY),
            strokeWidth = 7f * zoom,
            cap = StrokeCap.Round
        )
        // Core beam
        drawLine(
            color = Color.White,
            start = Offset(droneScreenX, droneScreenY),
            end = Offset(targetScreenX, targetScreenY),
            strokeWidth = 2.5f * zoom,
            cap = StrokeCap.Round
        )

        // Impact spark ring
        drawCircle(
            color = Color(0xFF38BDF8),
            radius = 6f * zoom,
            center = Offset(targetScreenX, targetScreenY)
        )
        drawCircle(
            color = Color.White,
            radius = 3f * zoom,
            center = Offset(targetScreenX, targetScreenY)
        )
    }

    // Drone body
    val droneRadius = 14f * zoom
    // Engine glow
    drawCircle(
        color = Color(0x660284C7),
        radius = droneRadius * 1.4f,
        center = Offset(droneScreenX, droneScreenY)
    )

    // Triangular sleek chassis
    val dronePath = Path().apply {
        moveTo(droneScreenX, droneScreenY - droneRadius)
        lineTo(droneScreenX + droneRadius * 0.85f, droneScreenY + droneRadius * 0.75f)
        lineTo(droneScreenX, droneScreenY + droneRadius * 0.4f)
        lineTo(droneScreenX - droneRadius * 0.85f, droneScreenY + droneRadius * 0.75f)
        close()
    }
    drawPath(dronePath, color = Color(0xFF1E293B), style = Fill)
    drawPath(dronePath, color = Color(0xFF38BDF8), style = Stroke(width = 2f * zoom))

    // Central cockpit light
    drawCircle(
        color = Color(0xFF38BDF8),
        radius = droneRadius * 0.28f,
        center = Offset(droneScreenX, droneScreenY)
    )
}
