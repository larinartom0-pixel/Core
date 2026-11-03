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

    val coreResources = mutableMapOf(
        Resource.COPPER to 150,
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
            return false // removed
        }

        // Try primary origin: tapped tile as top-left
        var originX = tileX.coerceIn(0, width - type.width)
        var originY = tileY.coerceIn(0, height - type.height)

        if (type.width > 1 || type.height > 1) {
            // Check candidate placements covering the tapped tile
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
     * Add conveyor along drag line
     */
    fun addConveyorAt(tileX: Int, tileY: Int, direction: Direction) {
        if (tileX !in 0 until width || tileY !in 0 until height) return
        val existingBp = getBlueprintAt(tileX, tileY)
        if (existingBp != null) {
            // Replace with updated direction if needed
            if (existingBp.direction != direction && existingBp.type == BuildingType.CONVEYOR) {
                blueprints.removeAll { it.id == existingBp.id }
                blueprints.add(existingBp.copy(direction = direction))
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

    fun getTotalBlueprintCost(): Int {
        return blueprints.sumOf { it.costCopper }
    }

    fun confirmBlueprints(): Boolean {
        val totalCost = getTotalBlueprintCost()
        val available = coreResources[Resource.COPPER] ?: 0
        if (available < totalCost) {
            addFloatingText("Недостатньо міді! Потрібно $totalCost", coreCenterX, coreCenterY - 2f, Color(0xFFEF4444))
            return false
        }

        // Deduct cost
        coreResources[Resource.COPPER] = available - totalCost
        addFloatingText("-$totalCost Міді", coreCenterX, coreCenterY - 1.5f, Color(0xFFF59E0B))

        // Convert blueprints to buildings
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
        updateConveyors(dt)
        updateVisualEffects(dt)
    }

    private fun updateDrone(dt: Float) {
        val currentTargetId = constructionQueue.firstOrNull()
        val targetBuilding = if (currentTargetId != null) buildings.firstOrNull { it.id == currentTargetId } else null

        if (targetBuilding != null && !targetBuilding.isFullyBuilt) {
            val tx = targetBuilding.centerX
            val ty = targetBuilding.centerY
            val dist = hypot(tx - drone.posX, ty - drone.posY)

            if (dist > 1.2f) {
                // Fly toward target
                drone.state = DroneState.FLYING_TO_BUILD
                drone.isFiringLaser = false
                val speed = 7.5f
                val dirX = (tx - drone.posX) / dist
                val dirY = (ty - drone.posY) / dist
                drone.posX += dirX * speed * dt
                drone.posY += dirY * speed * dt
            } else {
                // In range: build with laser!
                drone.state = DroneState.BUILDING
                drone.isFiringLaser = true
                drone.laserBeamX = tx
                drone.laserBeamY = ty

                val buildSpeed = 1f / targetBuilding.type.buildTimeSeconds
                targetBuilding.buildProgress = (targetBuilding.buildProgress + buildSpeed * dt).coerceAtMost(1f)

                // Emit sparks
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
            // Clean up completed queue items
            if (currentTargetId != null && (targetBuilding == null || targetBuilding.isFullyBuilt)) {
                constructionQueue.remove(currentTargetId)
            }

            // Return to core and orbit gently
            val distToCore = hypot(coreCenterX - drone.posX, coreCenterY - drone.posY)
            if (distToCore > 2.5f) {
                drone.state = DroneState.RETURNING_TO_CORE
                drone.isFiringLaser = false
                val speed = 6.0f
                val dirX = (coreCenterX - drone.posX) / distToCore
                val dirY = (coreCenterY - drone.posY) / distToCore
                drone.posX += dirX * speed * dt
                drone.posY += dirY * speed * dt
            } else {
                drone.state = DroneState.IDLE_AT_CORE
                drone.isFiringLaser = false
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

            // Count underlying copper ore tiles
            var copperTileCount = 0
            for (dx in 0 until building.type.width) {
                for (dy in 0 until building.type.height) {
                    val tx = building.originX + dx
                    val ty = building.originY + dy
                    if (tx in 0 until width && ty in 0 until height && grid[tx][ty].type == TileType.COPPER_ORE) {
                        copperTileCount++
                    }
                }
            }

            if (copperTileCount > 0) {
                // Rotate drill animation
                building.drillRotation = (building.drillRotation + dt * 260f * copperTileCount) % 360f
                building.drillTimer += dt * (0.6f + copperTileCount * 0.45f)

                if (building.drillTimer >= 1.4f) {
                    // Try to output copper
                    val outputSuccess = outputDrillResource(building, Resource.COPPER)
                    if (outputSuccess) {
                        building.drillTimer = 0f
                    }
                }
            }
        }
    }

    private fun outputDrillResource(drill: BuildingInstance, resource: Resource): Boolean {
        // Collect adjacent tiles around 2x2 drill
        val borderTiles = mutableListOf<Pair<Int, Int>>()
        for (dx in 0 until drill.type.width) {
            borderTiles.add(Pair(drill.originX + dx, drill.originY - 1)) // North border
            borderTiles.add(Pair(drill.originX + dx, drill.originY + drill.type.height)) // South border
        }
        for (dy in 0 until drill.type.height) {
            borderTiles.add(Pair(drill.originX - 1, drill.originY + dy)) // West border
            borderTiles.add(Pair(drill.originX + drill.type.width, drill.originY + dy)) // East border
        }

        // 1. Check for adjacent conveyor that has space
        for (tile in borderTiles) {
            val b = getBuildingAt(tile.first, tile.second)
            if (b != null && b.type == BuildingType.CONVEYOR && b.isFullyBuilt) {
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
        }

        // 2. Direct feed into Core if adjacent
        for (tile in borderTiles) {
            val b = getBuildingAt(tile.first, tile.second)
            if (b != null && b.type == BuildingType.CORE) {
                coreResources[resource] = (coreResources[resource] ?: 0) + 1
                addFloatingText("+1 ${resource.displayName}", tile.first + 0.5f, tile.second + 0.5f, resource.color)
                return true
            }
        }

        return false
    }

    private fun updateConveyors(dt: Float) {
        val speed = 2.4f // tiles per second
        val itemsToRemove = mutableListOf<TransportedItem>()

        for (item in items) {
            val currentBuilding = getBuildingAt(item.tileX, item.tileY)
            if (currentBuilding == null || currentBuilding.type != BuildingType.CONVEYOR || !currentBuilding.isFullyBuilt) {
                // Conveyor was removed or invalid
                itemsToRemove.add(item)
                continue
            }

            item.direction = currentBuilding.direction

            // Check if blocked by another item ahead on the same conveyor tile
            val itemAhead = items.firstOrNull { other ->
                other.id != item.id && other.tileX == item.tileX && other.tileY == item.tileY &&
                        other.progress > item.progress && (other.progress - item.progress) < 0.28f
            }

            if (itemAhead != null) {
                // Wait behind item
                continue
            }

            if (item.progress < 1.0f) {
                item.progress = (item.progress + dt * speed).coerceAtMost(1.0f)
            }

            if (item.progress >= 1.0f) {
                // Try to move to next tile
                val nextX = item.tileX + item.direction.dx
                val nextY = item.tileY + item.direction.dy

                val nextBuilding = getBuildingAt(nextX, nextY)
                if (nextBuilding != null && nextBuilding.type == BuildingType.CORE) {
                    // Delivered to core!
                    coreResources[item.resource] = (coreResources[item.resource] ?: 0) + 1
                    addFloatingText("+1 ${item.resource.displayName}", nextX + 0.5f, nextY + 0.5f, item.resource.color)
                    itemsToRemove.add(item)
                } else if (nextBuilding != null && nextBuilding.type == BuildingType.CONVEYOR && nextBuilding.isFullyBuilt) {
                    // Check if entrance to next conveyor is clear
                    val nextBlocked = items.any { other ->
                        other.tileX == nextX && other.tileY == nextY && other.progress < 0.28f
                    }
                    if (!nextBlocked) {
                        item.tileX = nextX
                        item.tileY = nextY
                        item.direction = nextBuilding.direction
                        item.progress = 0f
                    }
                }
            }
        }

        items.removeAll(itemsToRemove)
    }

    private fun updateVisualEffects(dt: Float) {
        // Floating texts
        val textsToRemove = mutableListOf<FloatingText>()
        for (txt in floatingTexts) {
            txt.lifetime -= dt
            txt.y -= dt * 0.7f // float upward
            if (txt.lifetime <= 0f) {
                textsToRemove.add(txt)
            }
        }
        floatingTexts.removeAll(textsToRemove)

        // Sparks
        val sparksToRemove = mutableListOf<SparkParticle>()
        for (spark in sparks) {
            spark.lifetime -= dt
            spark.x += spark.vx * dt
            spark.y += spark.vy * dt
            spark.vy += dt * 1.5f // gentle gravity
            if (spark.lifetime <= 0f) {
                sparksToRemove.add(spark)
            }
        }
        sparks.removeAll(sparksToRemove)
    }
}
