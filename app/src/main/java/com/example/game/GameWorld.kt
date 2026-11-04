package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.model.*
import java.util.UUID
import kotlin.math.*

class GameWorld(
    val width: Int = 40,
    val height: Int = 40,
    seed: Long = 1337L
) {
    val grid: Array<Array<Tile>> = WorldGenerator.generateWorld(width, height, seed)
    val buildings = mutableListOf<BuildingInstance>()
    val blueprints = mutableListOf<Blueprint>()
    val items = mutableListOf<TransportedItem>()
    val floatingTexts = mutableListOf<FloatingText>()
    val sparks = mutableListOf<SparkParticle>()
    val constructionQueue = mutableListOf<String>()
    val deconstructionQueue = mutableListOf<String>()

    val coreResources = mutableMapOf(
        Resource.COPPER to 150,
        Resource.LEAD to 40,
        Resource.COAL to 25,
        Resource.SILVER to 30,
        Resource.SCRAP to 0
    )

    val coreCenterX: Float = (width / 2).toFloat()
    val coreCenterY: Float = (height / 2).toFloat()

    val drone = BuilderDrone(
        posX = coreCenterX,
        posY = coreCenterY,
        targetX = coreCenterX,
        targetY = coreCenterY
    )

    init {
        // Spawn the starting Core (3x3) in the center
        val coreOriginX = width / 2 - 1
        val coreOriginY = height / 2 - 1
        val core = BuildingInstance(
            id = "core_main",
            type = BuildingType.CORE,
            originX = coreOriginX,
            originY = coreOriginY,
            direction = Direction.NORTH,
            buildProgress = 1.0f
        )
        buildings.add(core)
    }

    fun getBuildingAt(x: Int, y: Int): BuildingInstance? {
        return buildings.firstOrNull { it.occupies(x, y) }
    }

    fun getBlueprintAt(x: Int, y: Int): Blueprint? {
        return blueprints.firstOrNull { it.occupies(x, y) }
    }

    fun isTileBuildable(x: Int, y: Int, type: BuildingType): Boolean {
        if (x < 0 || y < 0 || x + type.width > width || y + type.height > height) return false
        for (dx in 0 until type.width) {
            for (dy in 0 until type.height) {
                val tx = x + dx
                val ty = y + dy
                if (grid[tx][ty].type.isSolid) return false
                if (getBuildingAt(tx, ty) != null) return false
            }
        }
        return true
    }

    fun overlapsAnyBlueprint(originX: Int, originY: Int, type: BuildingType): Boolean {
        return blueprints.any { bp ->
            for (dx in 0 until type.width) {
                for (dy in 0 until type.height) {
                    if (bp.occupies(originX + dx, originY + dy)) return@any true
                }
            }
            false
        }
    }

    /**
     * Toggles a blueprint at (tileX, tileY):
     * If there's an existing blueprint at this tile, remove it.
     * Otherwise, create a new blueprint.
     */
    fun toggleBlueprint(tileX: Int, tileY: Int, type: BuildingType, direction: Direction): Boolean {
        val existing = getBlueprintAt(tileX, tileY)
        if (existing != null) {
            blueprints.removeAll { it.id == existing.id }
            addFloatingText("✕ Скасовано", tileX + 0.5f, tileY + 0.5f, Color(0xFFEF4444))
            return false
        }

        var originX = tileX.coerceIn(0, width - type.width)
        var originY = tileY.coerceIn(0, height - type.height)

        if (type.width > 1 || type.height > 1) {
            val candidateOrigins = listOf(
                Pair(originX, originY),
                Pair(tileX - 1, tileY),
                Pair(tileX, tileY - 1),
                Pair(tileX - 1, tileY - 1)
            ).filter { (ox, oy) ->
                ox in 0..(width - type.width) && oy in 0..(height - type.height) &&
                        type.occupies(ox, oy, tileX, tileY) &&
                        isTileBuildable(ox, oy, type) &&
                        !overlapsAnyBlueprint(ox, oy, type)
            }

            if (candidateOrigins.isNotEmpty()) {
                val best = candidateOrigins.first()
                originX = best.first
                originY = best.second
            } else {
                addFloatingText("Тут не можна будувати!", tileX + 0.5f, tileY + 0.5f, Color(0xFFEF4444))
                return false
            }
        } else {
            if (!isTileBuildable(originX, originY, type) || overlapsAnyBlueprint(originX, originY, type)) {
                addFloatingText("Клітинка зайнята!", tileX + 0.5f, tileY + 0.5f, Color(0xFFEF4444))
                return false
            }
        }

        val bp = Blueprint(
            id = UUID.randomUUID().toString(),
            type = type,
            originX = originX,
            originY = originY,
            direction = direction,
            isValid = true
        )
        blueprints.add(bp)
        addFloatingText("+ ${type.displayName}", originX + type.width / 2f, originY + type.height / 2f, Color(0xFF38BDF8))
        return true
    }

    /**
     * Add conveyor along drag line.
     * Auto-junction: If crossing an existing perpendicular conveyor, automatically replace it with a Junction!
     */
    fun addConveyorAt(tileX: Int, tileY: Int, direction: Direction) {
        if (tileX !in 0 until width || tileY !in 0 until height) return

        // Check if there's already an existing building at this tile
        val existingBuilding = getBuildingAt(tileX, tileY)
        if (existingBuilding != null && existingBuilding.type == BuildingType.CONVEYOR) {
            val isExistingVertical = existingBuilding.direction == Direction.NORTH || existingBuilding.direction == Direction.SOUTH
            val isNewVertical = direction == Direction.NORTH || direction == Direction.SOUTH
            if (isExistingVertical != isNewVertical) {
                // Perpendicular crossing! Convert to Junction
                buildings.remove(existingBuilding)
                blueprints.removeAll { it.originX == tileX && it.originY == tileY }
                blueprints.add(
                    Blueprint(
                        id = UUID.randomUUID().toString(),
                        type = BuildingType.JUNCTION,
                        originX = tileX,
                        originY = tileY,
                        direction = direction,
                        isValid = true
                    )
                )
                addFloatingText("+ Перехрестя", tileX + 0.5f, tileY + 0.5f, Color(0xFF38BDF8))
                return
            }
            return
        }

        val existingBp = getBlueprintAt(tileX, tileY)
        if (existingBp != null) {
            if (existingBp.type == BuildingType.CONVEYOR) {
                val isBpVertical = existingBp.direction == Direction.NORTH || existingBp.direction == Direction.SOUTH
                val isNewVertical = direction == Direction.NORTH || direction == Direction.SOUTH
                if (isBpVertical != isNewVertical) {
                    // Turn blueprint into Junction
                    blueprints.removeAll { it.id == existingBp.id }
                    blueprints.add(
                        Blueprint(
                            id = UUID.randomUUID().toString(),
                            type = BuildingType.JUNCTION,
                            originX = tileX,
                            originY = tileY,
                            direction = direction,
                            isValid = true
                        )
                    )
                    addFloatingText("+ Перехрестя", tileX + 0.5f, tileY + 0.5f, Color(0xFF38BDF8))
                    return
                }
                if (existingBp.direction != direction) {
                    blueprints.removeAll { it.id == existingBp.id }
                    blueprints.add(existingBp.copy(direction = direction))
                }
            }
            return
        }

        if (isTileBuildable(tileX, tileY, BuildingType.CONVEYOR)) {
            blueprints.add(
                Blueprint(
                    id = UUID.randomUUID().toString(),
                    type = BuildingType.CONVEYOR,
                    originX = tileX,
                    originY = tileY,
                    direction = direction,
                    isValid = true
                )
            )
        }
    }

    fun getTotalBlueprintCopperCost(): Int = blueprints.sumOf { it.costCopper }
    fun getTotalBlueprintSilverCost(): Int = blueprints.sumOf { it.costSilver }

    fun confirmBlueprints(): Boolean {
        val totalCu = getTotalBlueprintCopperCost()
        val totalAg = getTotalBlueprintSilverCost()
        val availCu = coreResources[Resource.COPPER] ?: 0
        val availAg = coreResources[Resource.SILVER] ?: 0

        if (availCu < totalCu) {
            addFloatingText("Недостатньо міді! Потрібно $totalCu", coreCenterX, coreCenterY - 2f, Color(0xFFEF4444))
            return false
        }
        if (availAg < totalAg) {
            addFloatingText("Недостатньо срібла! Потрібно $totalAg", coreCenterX, coreCenterY - 2f, Color(0xFFEF4444))
            return false
        }

        coreResources[Resource.COPPER] = availCu - totalCu
        if (totalAg > 0) {
            coreResources[Resource.SILVER] = availAg - totalAg
        }
        addFloatingText("-$totalCu Cu" + if (totalAg > 0) " -$totalAg Ag" else "", coreCenterX, coreCenterY - 1.5f, Color(0xFFF59E0B))

        for (bp in blueprints) {
            val building = BuildingInstance(
                id = bp.id,
                type = bp.type,
                originX = bp.originX,
                originY = bp.originY,
                direction = bp.direction,
                buildProgress = 0f
            )
            buildings.add(building)
            constructionQueue.add(building.id)
        }
        blueprints.clear()
        return true
    }

    fun cancelBlueprints() {
        blueprints.clear()
    }

    // --- Deconstruction / Demolish Methods ---

    fun toggleMarkForDeconstruction(tileX: Int, tileY: Int): Boolean {
        val b = getBuildingAt(tileX, tileY) ?: return false
        if (b.type == BuildingType.CORE) {
            addFloatingText("Ядро не можна знести!", tileX + 0.5f, tileY + 0.5f, Color(0xFFEF4444))
            return false
        }
        b.isMarkedForDeconstruction = !b.isMarkedForDeconstruction
        if (b.isMarkedForDeconstruction) {
            addFloatingText("На знесення", b.centerX, b.centerY, Color(0xFFEF4444))
        } else {
            addFloatingText("Скасовано", b.centerX, b.centerY, Color(0xFF94A3B8))
        }
        return b.isMarkedForDeconstruction
    }

    fun markAreaForDeconstruction(x1: Int, y1: Int, x2: Int, y2: Int): Int {
        val minX = min(x1, x2)
        val maxX = max(x1, x2)
        val minY = min(y1, y2)
        val maxY = max(y1, y2)

        var markedCount = 0
        for (b in buildings) {
            if (b.type == BuildingType.CORE) continue
            val touches = (b.originX <= maxX && (b.originX + b.type.width - 1) >= minX) &&
                    (b.originY <= maxY && (b.originY + b.type.height - 1) >= minY)
            if (touches && !b.isMarkedForDeconstruction) {
                b.isMarkedForDeconstruction = true
                markedCount++
            }
        }
        return markedCount
    }

    fun getMarkedForDeconstructionCount(): Int {
        return buildings.count { it.isMarkedForDeconstruction && it.type != BuildingType.CORE }
    }

    fun confirmDeconstruction(): Int {
        var count = 0
        for (b in buildings) {
            if (b.isMarkedForDeconstruction && b.type != BuildingType.CORE) {
                if (!deconstructionQueue.contains(b.id)) {
                    deconstructionQueue.add(b.id)
                    count++
                }
            }
        }
        return count
    }

    fun cancelDeconstruction() {
        for (b in buildings) {
            b.isMarkedForDeconstruction = false
            b.deconstructProgress = 0f
        }
        deconstructionQueue.clear()
    }

    fun addFloatingText(text: String, x: Float, y: Float, color: Color) {
        floatingTexts.add(
            FloatingText(
                id = UUID.randomUUID().toString(),
                text = text,
                x = x,
                y = y,
                color = color
            )
        )
    }

    fun update(dt: Float) {
        updateDrone(dt)
        updateDrills(dt)
        updateLogistics(dt)
        updateVisualEffects(dt)
    }

    private fun updateDrone(dt: Float) {
        // Priority 1: Deconstruction tasks (1.5x speed)
        val currentDeconstructId = deconstructionQueue.firstOrNull()
        val deconstructBuilding = if (currentDeconstructId != null) buildings.firstOrNull { it.id == currentDeconstructId } else null

        if (deconstructBuilding != null) {
            val tx = deconstructBuilding.centerX
            val ty = deconstructBuilding.centerY
            val dist = hypot(tx - drone.posX, ty - drone.posY)

            if (dist > 1.2f) {
                drone.state = DroneState.FLYING_TO_DECONSTRUCT
                drone.isFiringLaser = false
                drone.isDeconstructing = true
                val speed = 8.5f
                val dirX = (tx - drone.posX) / dist
                val dirY = (ty - drone.posY) / dist
                drone.posX += dirX * speed * dt
                drone.posY += dirY * speed * dt
            } else {
                drone.state = DroneState.DECONSTRUCTING
                drone.isFiringLaser = true
                drone.isDeconstructing = true
                drone.laserBeamX = tx
                drone.laserBeamY = ty

                // Deconstruction is 1.5x faster than building
                val deconstructSpeed = 1.5f / deconstructBuilding.type.buildTimeSeconds
                deconstructBuilding.deconstructProgress += deconstructSpeed * dt

                // Orange/red dismantling sparks
                if (Math.random() < 0.45) {
                    val angle = (Math.random() * 2 * Math.PI).toFloat()
                    val sparkSpeed = 1.8f + (Math.random() * 2f).toFloat()
                    sparks.add(
                        SparkParticle(
                            x = tx,
                            y = ty,
                            vx = cos(angle) * sparkSpeed,
                            vy = sin(angle) * sparkSpeed,
                            color = Color(0xFFEF4444)
                        )
                    )
                }

                if (deconstructBuilding.deconstructProgress >= 1.0f) {
                    // Refund 100% resources back to Core
                    val cuRefund = deconstructBuilding.type.costCopper
                    val agRefund = deconstructBuilding.type.costSilver
                    coreResources[Resource.COPPER] = (coreResources[Resource.COPPER] ?: 0) + cuRefund
                    if (agRefund > 0) {
                        coreResources[Resource.SILVER] = (coreResources[Resource.SILVER] ?: 0) + agRefund
                    }

                    // Refund any items that were still located on this building directly into the Core!
                    val itemsOnBuilding = items.filter { deconstructBuilding.occupies(it.tileX, it.tileY) }
                    for (item in itemsOnBuilding) {
                        coreResources[item.resource] = (coreResources[item.resource] ?: 0) + 1
                        addFloatingText("+1 ${item.resource.displayName}", item.tileX + 0.5f, item.tileY + 0.5f, item.resource.color)
                    }
                    items.removeAll(itemsOnBuilding)

                    buildings.remove(deconstructBuilding)
                    deconstructionQueue.remove(deconstructBuilding.id)
                    drone.isFiringLaser = false
                    drone.isDeconstructing = false
                }
            }
            return
        }

        // Priority 2: Construction tasks
        val currentTargetId = constructionQueue.firstOrNull()
        val targetBuilding = if (currentTargetId != null) buildings.firstOrNull { it.id == currentTargetId } else null

        if (targetBuilding != null && !targetBuilding.isFullyBuilt) {
            val tx = targetBuilding.centerX
            val ty = targetBuilding.centerY
            val dist = hypot(tx - drone.posX, ty - drone.posY)

            if (dist > 1.2f) {
                drone.state = DroneState.FLYING_TO_BUILD
                drone.isFiringLaser = false
                drone.isDeconstructing = false
                val speed = 7.5f
                val dirX = (tx - drone.posX) / dist
                val dirY = (ty - drone.posY) / dist
                drone.posX += dirX * speed * dt
                drone.posY += dirY * speed * dt
            } else {
                drone.state = DroneState.BUILDING
                drone.isFiringLaser = true
                drone.isDeconstructing = false
                drone.laserBeamX = tx
                drone.laserBeamY = ty

                val buildSpeed = 1f / targetBuilding.type.buildTimeSeconds
                targetBuilding.buildProgress = (targetBuilding.buildProgress + buildSpeed * dt).coerceAtMost(1f)

                if (Math.random() < 0.45) {
                    val angle = (Math.random() * 2 * Math.PI).toFloat()
                    val sparkSpeed = 1.5f + (Math.random() * 2f).toFloat()
                    sparks.add(
                        SparkParticle(
                            x = tx,
                            y = ty,
                            vx = cos(angle) * sparkSpeed,
                            vy = sin(angle) * sparkSpeed,
                            color = Color(0xFF38BDF8)
                        )
                    )
                }

                if (targetBuilding.isFullyBuilt) {
                    addFloatingText("✓ Збудовано!", tx, ty - 0.5f, Color(0xFF10B981))
                    constructionQueue.remove(targetBuilding.id)
                    drone.isFiringLaser = false
                }
            }
        } else {
            if (currentTargetId != null && (targetBuilding == null || targetBuilding.isFullyBuilt)) {
                constructionQueue.remove(currentTargetId)
            }

            // Return to core and orbit gently
            val distToCore = hypot(coreCenterX - drone.posX, coreCenterY - drone.posY)
            if (distToCore > 2.5f) {
                drone.state = DroneState.RETURNING_TO_CORE
                drone.isFiringLaser = false
                drone.isDeconstructing = false
                val speed = 6.0f
                val dirX = (coreCenterX - drone.posX) / distToCore
                val dirY = (coreCenterY - drone.posY) / distToCore
                drone.posX += dirX * speed * dt
                drone.posY += dirY * speed * dt
            } else {
                drone.state = DroneState.IDLE_AT_CORE
                drone.isFiringLaser = false
                drone.isDeconstructing = false
                drone.idleAngle = (drone.idleAngle + dt * 1.6f) % (2f * Math.PI.toFloat())
                val targetOrbitX = coreCenterX + cos(drone.idleAngle) * 1.8f
                val targetOrbitY = coreCenterY + sin(drone.idleAngle) * 1.8f
                drone.posX += (targetOrbitX - drone.posX) * 3f * dt
                drone.posY += (targetOrbitY - drone.posY) * 3f * dt
            }
        }
    }

    private fun updateDrills(dt: Float) {
        for (building in buildings) {
            if (building.type != BuildingType.COPPER_DRILL || !building.isFullyBuilt) continue

            // Inspect underlying 2x2 tiles for any ore
            val oresFound = mutableListOf<Resource>()
            for (dx in 0 until building.type.width) {
                for (dy in 0 until building.type.height) {
                    val tx = building.originX + dx
                    val ty = building.originY + dy
                    if (tx in 0 until width && ty in 0 until height) {
                        val drop = grid[tx][ty].type.resourceDrop
                        if (drop != null) {
                            oresFound.add(drop)
                        }
                    }
                }
            }

            if (oresFound.isNotEmpty()) {
                val oreCount = oresFound.size
                // Rate: exactly 1.2 items per second if placed on all 4 ore tiles!
                // If fewer tiles, proportional: (oreCount / 4.0) * 1.2 items/sec
                val itemsPerSecond = (oreCount / 4.0f) * 1.2f

                building.drillRotation = (building.drillRotation + dt * 200f * oreCount) % 360f
                building.drillTimer += dt * itemsPerSecond

                if (building.drillTimer >= 1.0f) {
                    val minedResource = oresFound.first()
                    val outputSuccess = outputMinedResource(building, minedResource)
                    if (outputSuccess) {
                        building.drillTimer = 0f
                    }
                }
            }
        }
    }

    private fun outputMinedResource(drill: BuildingInstance, resource: Resource): Boolean {
        val borderTiles = mutableListOf<Pair<Int, Int>>()
        for (dx in 0 until drill.type.width) {
            borderTiles.add(Pair(drill.originX + dx, drill.originY - 1)) // North border
            borderTiles.add(Pair(drill.originX + dx, drill.originY + drill.type.height)) // South border
        }
        for (dy in 0 until drill.type.height) {
            borderTiles.add(Pair(drill.originX - 1, drill.originY + dy)) // West border
            borderTiles.add(Pair(drill.originX + drill.type.width, drill.originY + dy)) // East border
        }

        // 1. Check for adjacent conveyor, router, or junction with available space
        for (tile in borderTiles) {
            val b = getBuildingAt(tile.first, tile.second) ?: continue
            if (!b.isFullyBuilt) continue

            when (b.type) {
                BuildingType.CONVEYOR -> {
                    val hasSpace = items.none { it.tileX == tile.first && it.tileY == tile.second && it.progress < 0.35f }
                    if (hasSpace) {
                        items.add(
                            TransportedItem(
                                id = UUID.randomUUID().toString(),
                                resource = resource,
                                tileX = tile.first,
                                tileY = tile.second,
                                progress = 0.05f,
                                direction = b.direction
                            )
                        )
                        return true
                    }
                }
                BuildingType.ROUTER, BuildingType.JUNCTION -> {
                    val hasSpace = items.none { it.tileX == tile.first && it.tileY == tile.second && it.progress < 0.35f }
                    if (hasSpace) {
                        // Direction pointing from drill to building
                        val dir = Direction.fromDelta(tile.first - (drill.originX + 1), tile.second - (drill.originY + 1))
                        items.add(
                            TransportedItem(
                                id = UUID.randomUUID().toString(),
                                resource = resource,
                                tileX = tile.first,
                                tileY = tile.second,
                                progress = 0.05f,
                                direction = dir
                            )
                        )
                        return true
                    }
                }
                BuildingType.CORE -> {
                    coreResources[resource] = (coreResources[resource] ?: 0) + 1
                    addFloatingText("+1 ${resource.displayName}", tile.first + 0.5f, tile.second + 0.5f, resource.color)
                    return true
                }
                else -> {}
            }
        }

        return false
    }

    private fun updateLogistics(dt: Float) {
        val conveyorSpeed = 2.4f
        val internalSpeed = 3.2f
        val itemsToRemove = mutableListOf<TransportedItem>()

        for (item in items) {
            val currentBuilding = getBuildingAt(item.tileX, item.tileY)
            if (currentBuilding == null || !currentBuilding.isFullyBuilt) {
                itemsToRemove.add(item)
                continue
            }

            when (currentBuilding.type) {
                BuildingType.CONVEYOR -> {
                    item.direction = currentBuilding.direction

                    // Collision with item ahead
                    val itemAhead = items.firstOrNull { other ->
                        other.id != item.id && other.tileX == item.tileX && other.tileY == item.tileY &&
                                other.progress > item.progress && (other.progress - item.progress) < 0.28f
                    }
                    if (itemAhead != null) continue

                    if (item.progress < 1.0f) {
                        item.progress = (item.progress + dt * conveyorSpeed).coerceAtMost(1.0f)
                    }

                    if (item.progress >= 1.0f) {
                        val nextX = item.tileX + item.direction.dx
                        val nextY = item.tileY + item.direction.dy
                        val nextBuilding = getBuildingAt(nextX, nextY)

                        if (nextBuilding != null && nextBuilding.type == BuildingType.CORE) {
                            coreResources[item.resource] = (coreResources[item.resource] ?: 0) + 1
                            addFloatingText("+1 ${item.resource.displayName}", nextX + 0.5f, nextY + 0.5f, item.resource.color)
                            itemsToRemove.add(item)
                        } else if (nextBuilding != null && nextBuilding.isFullyBuilt) {
                            val nextBlocked = items.any { other ->
                                other.tileX == nextX && other.tileY == nextY && other.progress < 0.30f
                            }
                            if (!nextBlocked) {
                                item.tileX = nextX
                                item.tileY = nextY
                                item.progress = 0f
                                if (nextBuilding.type == BuildingType.CONVEYOR) {
                                    item.direction = nextBuilding.direction
                                }
                            }
                        }
                    }
                }

                BuildingType.ROUTER -> {
                    if (item.progress < 1.0f) {
                        item.progress = (item.progress + dt * internalSpeed).coerceAtMost(1.0f)
                    }

                    if (item.progress >= 1.0f) {
                        // Router distributes evenly across all available outputs
                        val neighbors = listOf(
                            Pair(item.tileX, item.tileY - 1) to Direction.NORTH,
                            Pair(item.tileX + 1, item.tileY) to Direction.EAST,
                            Pair(item.tileX, item.tileY + 1) to Direction.SOUTH,
                            Pair(item.tileX - 1, item.tileY) to Direction.WEST
                        )

                        val validOutputs = neighbors.filter { (pos, dir) ->
                            val b = getBuildingAt(pos.first, pos.second)
                            if (b == null || !b.isFullyBuilt) return@filter false
                            if (b.type == BuildingType.CORE) return@filter true
                            if (b.type == BuildingType.CONVEYOR) {
                                return@filter b.direction != dir.opposite()
                            }
                            if (b.type == BuildingType.ROUTER || b.type == BuildingType.JUNCTION) return@filter true
                            false
                        }

                        if (validOutputs.isNotEmpty()) {
                            val chosenIndex = currentBuilding.routerOutputIndex % validOutputs.size
                            val (targetPos, targetDir) = validOutputs[chosenIndex]
                            val targetBuilding = getBuildingAt(targetPos.first, targetPos.second)

                            if (targetBuilding != null && targetBuilding.type == BuildingType.CORE) {
                                coreResources[item.resource] = (coreResources[item.resource] ?: 0) + 1
                                addFloatingText("+1 ${item.resource.displayName}", targetPos.first + 0.5f, targetPos.second + 0.5f, item.resource.color)
                                itemsToRemove.add(item)
                                currentBuilding.routerOutputIndex++
                            } else {
                                val targetBlocked = items.any { other ->
                                    other.tileX == targetPos.first && other.tileY == targetPos.second && other.progress < 0.30f
                                }
                                if (!targetBlocked) {
                                    item.tileX = targetPos.first
                                    item.tileY = targetPos.second
                                    item.progress = 0f
                                    item.direction = if (targetBuilding?.type == BuildingType.CONVEYOR) targetBuilding.direction else targetDir
                                    currentBuilding.routerOutputIndex++
                                }
                            }
                        }
                    }
                }

                BuildingType.JUNCTION -> {
                    // Item continues straight through along its original direction!
                    if (item.progress < 1.0f) {
                        item.progress = (item.progress + dt * internalSpeed).coerceAtMost(1.0f)
                    }

                    if (item.progress >= 1.0f) {
                        val nextX = item.tileX + item.direction.dx
                        val nextY = item.tileY + item.direction.dy
                        val nextBuilding = getBuildingAt(nextX, nextY)

                        if (nextBuilding != null && nextBuilding.type == BuildingType.CORE) {
                            coreResources[item.resource] = (coreResources[item.resource] ?: 0) + 1
                            addFloatingText("+1 ${item.resource.displayName}", nextX + 0.5f, nextY + 0.5f, item.resource.color)
                            itemsToRemove.add(item)
                        } else if (nextBuilding != null && nextBuilding.isFullyBuilt) {
                            val nextBlocked = items.any { other ->
                                other.tileX == nextX && other.tileY == nextY && other.progress < 0.30f
                            }
                            if (!nextBlocked) {
                                item.tileX = nextX
                                item.tileY = nextY
                                item.progress = 0f
                                if (nextBuilding.type == BuildingType.CONVEYOR) {
                                    item.direction = nextBuilding.direction
                                }
                            }
                        }
                    }
                }

                else -> {}
            }
        }

        items.removeAll(itemsToRemove)
    }

    private fun updateVisualEffects(dt: Float) {
        val textsToRemove = mutableListOf<FloatingText>()
        for (txt in floatingTexts) {
            txt.lifetime -= dt
            txt.y -= dt * 0.7f
            if (txt.lifetime <= 0f) {
                textsToRemove.add(txt)
            }
        }
        floatingTexts.removeAll(textsToRemove)

        val sparksToRemove = mutableListOf<SparkParticle>()
        for (spark in sparks) {
            spark.lifetime -= dt
            spark.x += spark.vx * dt
            spark.y += spark.vy * dt
            spark.vy += dt * 1.5f
            if (spark.lifetime <= 0f) {
                sparksToRemove.add(spark)
            }
        }
        sparks.removeAll(sparksToRemove)
    }
}
