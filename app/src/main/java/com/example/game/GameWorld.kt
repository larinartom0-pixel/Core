package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.model.*
import java.util.UUID
import kotlin.math.*

class GameWorld(
    val width: Int = 80,
    val height: Int = 80,
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

    // Enemies, projectiles, particles & power
    val units = mutableListOf<UnitInstance>()
    val projectiles = mutableListOf<Projectile>()
    val smokeParticles = mutableListOf<SmokeParticle>()
    val powerLinks = mutableListOf<Pair<Pair<Float, Float>, Pair<Float, Float>>>()

    // Wave system: waves spawn every 180 seconds (3 minutes)
    var waveNumber: Int = 1
    var waveCountdownSeconds: Float = 180f

    val coreResources = mutableMapOf(
        Resource.COPPER to 160,
        Resource.LEAD to 90,
        Resource.COAL to 40,
        Resource.SILVER to 35,
        Resource.IRON_ORE to 20,
        Resource.IRON_INGOT to 25,
        Resource.COPPER_AMMO to 15,
        Resource.SILVER_AMMO to 0,
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
            if (existing.type == BuildingType.CONVEYOR && type == BuildingType.CONVEYOR) {
                if (existing.direction != direction) {
                    blueprints.removeAll { it.id == existing.id }
                    blueprints.add(existing.copy(direction = direction))
                    addFloatingText("↻ Повернуто", tileX + 0.5f, tileY + 0.5f, Color(0xFF38BDF8))
                    return true
                }
            }
            blueprints.removeAll { it.id == existing.id }
            addFloatingText("✕ Скасовано", tileX + 0.5f, tileY + 0.5f, Color(0xFFEF4444))
            return false
        }

        var originX = tileX.coerceIn(0, width - type.width)
        var originY = tileY.coerceIn(0, height - type.height)

        if (type.width > 1 || type.height > 1) {
            val candidateOrigins = mutableListOf<Pair<Int, Int>>()
            // Prioritize the tapped origin directly if valid
            if (isTileBuildable(originX, originY, type) && !overlapsAnyBlueprint(originX, originY, type)) {
                candidateOrigins.add(Pair(originX, originY))
            } else {
                for (ox in (tileX - type.width + 1)..tileX) {
                    for (oy in (tileY - type.height + 1)..tileY) {
                        if (ox in 0..(width - type.width) && oy in 0..(height - type.height) &&
                            type.occupies(ox, oy, tileX, tileY) &&
                            isTileBuildable(ox, oy, type) &&
                            !overlapsAnyBlueprint(ox, oy, type)
                        ) {
                            candidateOrigins.add(Pair(ox, oy))
                        }
                    }
                }
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

        val placedDirection = if (type == BuildingType.CONVEYOR) {
            getIncomingConveyorDirection(originX, originY) ?: direction
        } else {
            direction
        }

        val bp = Blueprint(
            id = UUID.randomUUID().toString(),
            type = type,
            originX = originX,
            originY = originY,
            direction = placedDirection,
            isValid = true
        )
        blueprints.add(bp)
        addFloatingText("+ ${type.displayName}", originX + type.width / 2f, originY + type.height / 2f, Color(0xFF38BDF8))
        return true
    }

    /**
     * Checks if any adjacent conveyor or blueprint is pointing into (tileX, tileY)
     * to continue the logistics flow smoothly.
     */
    fun getIncomingConveyorDirection(tileX: Int, tileY: Int): Direction? {
        val bWest = getBuildingAt(tileX - 1, tileY)
        val bpWest = getBlueprintAt(tileX - 1, tileY)
        if (bWest?.type == BuildingType.CONVEYOR && bWest.direction == Direction.EAST) return Direction.EAST
        if (bpWest?.type == BuildingType.CONVEYOR && bpWest.direction == Direction.EAST) return Direction.EAST

        val bNorth = getBuildingAt(tileX, tileY - 1)
        val bpNorth = getBlueprintAt(tileX, tileY - 1)
        if (bNorth?.type == BuildingType.CONVEYOR && bNorth.direction == Direction.SOUTH) return Direction.SOUTH
        if (bpNorth?.type == BuildingType.CONVEYOR && bpNorth.direction == Direction.SOUTH) return Direction.SOUTH

        val bEast = getBuildingAt(tileX + 1, tileY)
        val bpEast = getBlueprintAt(tileX + 1, tileY)
        if (bEast?.type == BuildingType.CONVEYOR && bEast.direction == Direction.WEST) return Direction.WEST
        if (bpEast?.type == BuildingType.CONVEYOR && bpEast.direction == Direction.WEST) return Direction.WEST

        val bSouth = getBuildingAt(tileX, tileY + 1)
        val bpSouth = getBlueprintAt(tileX, tileY + 1)
        if (bSouth?.type == BuildingType.CONVEYOR && bSouth.direction == Direction.NORTH) return Direction.NORTH
        if (bpSouth?.type == BuildingType.CONVEYOR && bpSouth.direction == Direction.NORTH) return Direction.NORTH

        return null
    }

    /**
     * Add conveyor along drag line.
     * Preserves built buildings and allows rotating/reorienting blueprint conveyors without converting to junction!
     */
    fun addConveyorAt(tileX: Int, tileY: Int, direction: Direction) {
        if (tileX !in 0 until width || tileY !in 0 until height) return

        val existingBuilding = getBuildingAt(tileX, tileY)
        if (existingBuilding != null) {
            // Already built building here -> NEVER destroy it during dragging/blueprint!
            return
        }

        val existingBp = blueprints.firstOrNull { it.occupies(tileX, tileY) }
        if (existingBp != null) {
            if (existingBp.type == BuildingType.CONVEYOR) {
                // Allow rotating/reorienting blueprint conveyor smoothly
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
    fun getTotalBlueprintLeadCost(): Int = blueprints.sumOf { it.costLead }
    fun getTotalBlueprintSilverCost(): Int = blueprints.sumOf { it.costSilver }
    fun getTotalBlueprintIronCost(): Int = blueprints.sumOf { it.costIronIngot }

    fun confirmBlueprints(): Boolean {
        val totalCu = getTotalBlueprintCopperCost()
        val totalPb = getTotalBlueprintLeadCost()
        val totalAg = getTotalBlueprintSilverCost()
        val totalFe = getTotalBlueprintIronCost()

        val availCu = coreResources[Resource.COPPER] ?: 0
        val availPb = coreResources[Resource.LEAD] ?: 0
        val availAg = coreResources[Resource.SILVER] ?: 0
        val availFe = coreResources[Resource.IRON_INGOT] ?: 0

        if (availCu < totalCu) {
            addFloatingText("Недостатньо міді! Потрібно $totalCu", coreCenterX, coreCenterY - 2f, Color(0xFFEF4444))
            return false
        }
        if (availPb < totalPb) {
            addFloatingText("Недостатньо свинцю! Потрібно $totalPb", coreCenterX, coreCenterY - 2f, Color(0xFFEF4444))
            return false
        }
        if (availAg < totalAg) {
            addFloatingText("Недостатньо срібла! Потрібно $totalAg", coreCenterX, coreCenterY - 2f, Color(0xFFEF4444))
            return false
        }
        if (availFe < totalFe) {
            addFloatingText("Недостатньо заліза! Потрібно $totalFe", coreCenterX, coreCenterY - 2f, Color(0xFFEF4444))
            return false
        }

        coreResources[Resource.COPPER] = availCu - totalCu
        if (totalPb > 0) coreResources[Resource.LEAD] = availPb - totalPb
        if (totalAg > 0) coreResources[Resource.SILVER] = availAg - totalAg
        if (totalFe > 0) coreResources[Resource.IRON_INGOT] = availFe - totalFe

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

    fun toggleMarkForDeconstruction(tileX: Int, tileY: Int): Boolean {
        val b = getBuildingAt(tileX, tileY) ?: return false
        if (b.type == BuildingType.CORE) {
            addFloatingText("Ядро не можна знести!", b.centerX, b.centerY, Color(0xFFEF4444))
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
        updatePowerNetwork(dt)
        updateIndustryMachines(dt)
        updateTurrets(dt)
        updateLogistics(dt)
        updateWavesAndEnemies(dt)
        updateProjectiles(dt)
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

                val deconstructSpeed = 1.5f / deconstructBuilding.type.buildTimeSeconds
                deconstructBuilding.deconstructProgress += deconstructSpeed * dt

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
                    coreResources[Resource.COPPER] = (coreResources[Resource.COPPER] ?: 0) + deconstructBuilding.type.costCopper
                    if (deconstructBuilding.type.costLead > 0) {
                        coreResources[Resource.LEAD] = (coreResources[Resource.LEAD] ?: 0) + deconstructBuilding.type.costLead
                    }
                    if (deconstructBuilding.type.costSilver > 0) {
                        coreResources[Resource.SILVER] = (coreResources[Resource.SILVER] ?: 0) + deconstructBuilding.type.costSilver
                    }
                    if (deconstructBuilding.type.costIronIngot > 0) {
                        coreResources[Resource.IRON_INGOT] = (coreResources[Resource.IRON_INGOT] ?: 0) + deconstructBuilding.type.costIronIngot
                    }

                    // Refund stored resources inside building
                    for ((res, count) in deconstructBuilding.storedResources) {
                        if (count > 0) {
                            coreResources[res] = (coreResources[res] ?: 0) + count
                        }
                    }
                    if (deconstructBuilding.turretAmmoType != null && deconstructBuilding.turretAmmoCount > 0) {
                        coreResources[deconstructBuilding.turretAmmoType!!] = (coreResources[deconstructBuilding.turretAmmoType!!] ?: 0) + deconstructBuilding.turretAmmoCount
                    }

                    // Refund items still on this building
                    val itemsOnBuilding = items.filter { deconstructBuilding.occupies(it.tileX, it.tileY) }
                    for (item in itemsOnBuilding) {
                        coreResources[item.resource] = (coreResources[item.resource] ?: 0) + 1
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
                    constructionQueue.remove(targetBuilding.id)
                    drone.isFiringLaser = false
                }
            }
        } else {
            if (currentTargetId != null && (targetBuilding == null || targetBuilding.isFullyBuilt)) {
                constructionQueue.remove(currentTargetId)
            }

            // Return to core orbit when idle
            drone.isFiringLaser = false
            drone.isDeconstructing = false
            val distToCore = hypot(coreCenterX - drone.posX, coreCenterY - drone.posY)
            if (distToCore > 3.0f) {
                drone.state = DroneState.RETURNING_TO_CORE
                val dirX = (coreCenterX - drone.posX) / distToCore
                val dirY = (coreCenterY - drone.posY) / distToCore
                drone.posX += dirX * 6.5f * dt
                drone.posY += dirY * 6.5f * dt
            } else {
                drone.state = DroneState.IDLE_AT_CORE
                drone.idleAngle += 1.4f * dt
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
                val itemsPerSecond = (oreCount / 4.0f) * 1.2f
                building.drillRotation = (building.drillRotation + dt * 200f * oreCount) % 360f
                building.drillTimer += dt * itemsPerSecond

                if (building.drillTimer >= 1.0f) {
                    val minedResource = oresFound.first()
                    val outputSuccess = outputResourceFromBuilding(building, minedResource)
                    if (outputSuccess) {
                        building.drillTimer = 0f
                    }
                }
            }
        }
    }

    /**
     * Power network:
     * Nodes connect in 4 cardinal directions up to 5 tiles away.
     * Generators burn 2 coal/s -> 20 power/s.
     * Batteries buffer up to 200 power.
     * Smelters consume 3 power/s, Fabricators 3-5 power/s.
     */
    private fun updatePowerNetwork(dt: Float) {
        powerLinks.clear()

        val powerNodes = buildings.filter { it.isFullyBuilt && it.type == BuildingType.POWER_NODE }
        val generators = buildings.filter { it.isFullyBuilt && it.type == BuildingType.COAL_GENERATOR }
        val batteries = buildings.filter { it.isFullyBuilt && it.type == BuildingType.BATTERY }
        val consumers = buildings.filter { it.isFullyBuilt && (it.type == BuildingType.IRON_SMELTER || it.type == BuildingType.AMMO_FABRICATOR) }

        // Find connections between power nodes (up to 5 tiles orthogonally)
        for (i in powerNodes.indices) {
            val a = powerNodes[i]
            for (j in (i + 1) until powerNodes.size) {
                val b = powerNodes[j]
                val dx = abs(a.originX - b.originX)
                val dy = abs(a.originY - b.originY)
                val isOrthogonal5 = (dx <= 5 && dy == 0) || (dy <= 5 && dx == 0)
                if (isOrthogonal5) {
                    powerLinks.add(Pair(Pair(a.centerX, a.centerY), Pair(b.centerX, b.centerY)))
                }
            }
        }

        // Connect producers & consumers to nodes within 5 tiles
        for (node in powerNodes) {
            for (gen in generators) {
                if (hypot(node.centerX - gen.centerX, node.centerY - gen.centerY) <= 5.5f) {
                    powerLinks.add(Pair(Pair(node.centerX, node.centerY), Pair(gen.centerX, gen.centerY)))
                }
            }
            for (bat in batteries) {
                if (hypot(node.centerX - bat.centerX, node.centerY - bat.centerY) <= 5.5f) {
                    powerLinks.add(Pair(Pair(node.centerX, node.centerY), Pair(bat.centerX, bat.centerY)))
                }
            }
            for (con in consumers) {
                if (hypot(node.centerX - con.centerX, node.centerY - con.centerY) <= 5.5f) {
                    powerLinks.add(Pair(Pair(node.centerX, node.centerY), Pair(con.centerX, con.centerY)))
                }
            }
        }

        // Also direct generator-consumer adjacency connection if no node in between
        for (gen in generators) {
            for (con in consumers) {
                if (hypot(gen.centerX - con.centerX, gen.centerY - con.centerY) <= 3.5f) {
                    powerLinks.add(Pair(Pair(gen.centerX, gen.centerY), Pair(con.centerX, con.centerY)))
                }
            }
        }

        // Calculate power generation from Coal Generators
        var totalPowerGen = 0f
        for (gen in generators) {
            val coal = gen.storedResources[Resource.COAL] ?: 0
            if (coal > 0) {
                totalPowerGen += 20f * dt
                gen.drillTimer += dt * 2.0f // burn timer
                if (gen.drillTimer >= 1.0f) {
                    gen.drillTimer = 0f
                    val newCoal = coal - 1
                    if (newCoal <= 0) gen.storedResources.remove(Resource.COAL)
                    else gen.storedResources[Resource.COAL] = newCoal
                }
                gen.isPowered = true
            } else {
                gen.isPowered = false
            }
        }

        // Calculate power demand from consumers
        var totalPowerDemand = 0f
        for (con in consumers) {
            val demandRate = if (con.type == BuildingType.IRON_SMELTER) 3f else if (con.fabricatorRecipe == "silver") 5f else 3f
            totalPowerDemand += demandRate * dt
        }

        // Handle battery storage / deficit
        val totalBatteryCapacity = batteries.size * 200f
        var currentBatteryStorage = batteries.sumOf { it.energyStored.toDouble() }.toFloat()

        if (totalPowerGen >= totalPowerDemand) {
            val surplus = totalPowerGen - totalPowerDemand
            if (batteries.isNotEmpty() && surplus > 0f) {
                val chargePerBattery = surplus / batteries.size
                for (bat in batteries) {
                    bat.energyStored = (bat.energyStored + chargePerBattery).coerceAtMost(200f)
                }
            }
            consumers.forEach { it.isPowered = true }
        } else {
            val deficit = totalPowerDemand - totalPowerGen
            if (currentBatteryStorage >= deficit && batteries.isNotEmpty()) {
                val dischargePerBattery = deficit / batteries.size
                for (bat in batteries) {
                    bat.energyStored = (bat.energyStored - dischargePerBattery).coerceAtLeast(0f)
                }
                consumers.forEach { it.isPowered = true }
            } else {
                // Not enough power
                for (bat in batteries) bat.energyStored = 0f
                val satisfaction = if (totalPowerDemand > 0f) (totalPowerGen + currentBatteryStorage) / totalPowerDemand else 0f
                consumers.forEach { it.isPowered = satisfaction > 0.45f }
            }
        }
    }

    private fun updateIndustryMachines(dt: Float) {
        for (b in buildings) {
            if (!b.isFullyBuilt) continue

            when (b.type) {
                BuildingType.IRON_SMELTER -> {
                    // Try to unload already crafted iron ingots to output conveyors or adjacent buildings
                    val ingots = b.storedResources[Resource.IRON_INGOT] ?: 0
                    if (ingots > 0) {
                        if (outputResourceFromBuilding(b, Resource.IRON_INGOT)) {
                            val rem = ingots - 1
                            if (rem <= 0) b.storedResources.remove(Resource.IRON_INGOT)
                            else b.storedResources[Resource.IRON_INGOT] = rem
                        }
                    }

                    // Craft iron ingots if inputs and power are present and internal buffer has space (< 20)
                    val ore = b.storedResources[Resource.IRON_ORE] ?: 0
                    val coal = b.storedResources[Resource.COAL] ?: 0
                    val currentIngots = b.storedResources[Resource.IRON_INGOT] ?: 0

                    if (ore >= 3 && coal >= 1 && b.isPowered && currentIngots < 20) {
                        b.craftTimer += dt
                        if (b.craftTimer >= 1.0f) {
                            b.craftTimer = 0f
                            b.storedResources[Resource.IRON_ORE] = ore - 3
                            b.storedResources[Resource.COAL] = coal - 1
                            b.storedResources[Resource.IRON_INGOT] = currentIngots + 1
                            addFloatingText("+1 Залізний злиток", b.centerX, b.centerY, Resource.IRON_INGOT.color)
                            // Also try immediate output
                            if (outputResourceFromBuilding(b, Resource.IRON_INGOT)) {
                                val rem = b.storedResources[Resource.IRON_INGOT] ?: 0
                                if (rem <= 1) b.storedResources.remove(Resource.IRON_INGOT)
                                else b.storedResources[Resource.IRON_INGOT] = rem - 1
                            }
                        }
                    } else if (currentIngots >= 20 || !b.isPowered || ore < 3 || coal < 1) {
                        b.craftTimer = (b.craftTimer - dt * 0.5f).coerceAtLeast(0f)
                    }
                }
                BuildingType.AMMO_FABRICATOR -> {
                    val isSilver = b.fabricatorRecipe == "silver"
                    val targetAmmo = if (isSilver) Resource.SILVER_AMMO else Resource.COPPER_AMMO

                    // Try to unload already crafted ammo
                    val storedAmmo = b.storedResources[targetAmmo] ?: 0
                    if (storedAmmo > 0) {
                        if (outputResourceFromBuilding(b, targetAmmo)) {
                            val rem = storedAmmo - 1
                            if (rem <= 0) b.storedResources.remove(targetAmmo)
                            else b.storedResources[targetAmmo] = rem
                        }
                    }

                    val cu = b.storedResources[Resource.COPPER] ?: 0
                    val pb = b.storedResources[Resource.LEAD] ?: 0
                    val coal = b.storedResources[Resource.COAL] ?: 0
                    val ag = b.storedResources[Resource.SILVER] ?: 0
                    val currentAmmo = b.storedResources[targetAmmo] ?: 0

                    val canCraft = if (isSilver) {
                        pb >= 2 && ag >= 3 && coal >= 2 && b.isPowered && currentAmmo < 30
                    } else {
                        cu >= 3 && pb >= 1 && coal >= 2 && b.isPowered && currentAmmo < 30
                    }

                    if (canCraft) {
                        b.craftTimer += dt
                        if (b.craftTimer >= 1.2f) {
                            b.craftTimer = 0f
                            if (isSilver) {
                                b.storedResources[Resource.LEAD] = pb - 2
                                b.storedResources[Resource.SILVER] = ag - 3
                                b.storedResources[Resource.COAL] = coal - 2
                            } else {
                                b.storedResources[Resource.COPPER] = cu - 3
                                b.storedResources[Resource.LEAD] = pb - 1
                                b.storedResources[Resource.COAL] = coal - 2
                            }
                            b.storedResources[targetAmmo] = currentAmmo + 1
                            addFloatingText("+1 ${targetAmmo.displayName}", b.centerX, b.centerY, targetAmmo.color)
                            // Also try immediate output
                            if (outputResourceFromBuilding(b, targetAmmo)) {
                                val rem = b.storedResources[targetAmmo] ?: 0
                                if (rem <= 1) b.storedResources.remove(targetAmmo)
                                else b.storedResources[targetAmmo] = rem - 1
                            }
                        }
                    } else if (currentAmmo >= 30 || !b.isPowered) {
                        b.craftTimer = (b.craftTimer - dt * 0.5f).coerceAtLeast(0f)
                    }
                }
                else -> {}
            }
        }
    }

    private fun updateTurrets(dt: Float) {
        val livingEnemies = units.filter { it.isAlive && it.isEnemy }

        for (turret in buildings) {
            if (turret.type != BuildingType.TURRET || !turret.isFullyBuilt) continue

            // Find closest enemy within 6.5 tiles
            val target = livingEnemies
                .map { u -> u to hypot(u.posX - turret.centerX, u.posY - turret.centerY) }
                .filter { it.second <= 6.5f }
                .minByOrNull { it.second }?.first

            if (target != null) {
                // Rotate towards enemy
                val dx = target.posX - turret.centerX
                val dy = target.posY - turret.centerY
                turret.turretRotation = (atan2(dy, dx) * 180f / Math.PI.toFloat())

                turret.turretFireTimer += dt
                if (turret.turretFireTimer >= 0.22f && turret.turretAmmoCount > 0 && turret.turretAmmoType != null) {
                    turret.turretFireTimer = 0f
                    val ammo = turret.turretAmmoType!!
                    turret.turretAmmoCount--
                    if (turret.turretAmmoCount <= 0) {
                        turret.turretAmmoType = null
                    }

                    val damage = if (ammo == Resource.SILVER_AMMO) 24 else 10
                    val bulletColor = if (ammo == Resource.SILVER_AMMO) Color(0xFF38BDF8) else Color(0xFFFB923C)

                    projectiles.add(
                        Projectile(
                            posX = turret.centerX,
                            posY = turret.centerY,
                            targetX = target.posX,
                            targetY = target.posY,
                            speed = 12f,
                            damage = damage,
                            color = bulletColor,
                            isEnemy = false,
                            targetUnitId = target.id
                        )
                    )
                }
            } else {
                turret.turretFireTimer = (turret.turretFireTimer + dt).coerceAtMost(0.22f)
            }
        }
    }

    private fun updateWavesAndEnemies(dt: Float) {
        waveCountdownSeconds -= dt
        if (waveCountdownSeconds <= 0f) {
            spawnWave()
            waveCountdownSeconds = 180f // 3 minutes
            waveNumber++
        }

        val enemiesToRemove = mutableListOf<UnitInstance>()

        for (unit in units) {
            if (!unit.isAlive) {
                enemiesToRemove.add(unit)
                continue
            }

            // Find target building (factories/generators/turrets/smelters/drills first, then core)
            val targets = buildings.filter { it.isFullyBuilt && it.health > 0 }
            val preferredTarget = targets.filter { it.type != BuildingType.CORE }
                .minByOrNull { hypot(it.centerX - unit.posX, it.centerY - unit.posY) }
                ?: targets.firstOrNull { it.type == BuildingType.CORE }

            if (preferredTarget != null) {
                unit.targetBuildingId = preferredTarget.id
                val tx = preferredTarget.centerX
                val ty = preferredTarget.centerY
                val dist = hypot(tx - unit.posX, ty - unit.posY)

                val dirX = (tx - unit.posX) / (dist + 0.001f)
                val dirY = (ty - unit.posY) / (dist + 0.001f)
                unit.rotation = (atan2(dirY, dirX) * 180f / Math.PI.toFloat())

                if (dist > unit.type.attackRange) {
                    unit.posX += dirX * unit.type.moveSpeed * dt
                    unit.posY += dirY * unit.type.moveSpeed * dt
                    unit.walkAnimTime += dt * 7f
                    unit.isFiring = false
                } else {
                    // Attack building
                    unit.isFiring = true
                    unit.fireTimer += dt
                    if (unit.fireTimer >= unit.type.fireRateSeconds) {
                        unit.fireTimer = 0f
                        projectiles.add(
                            Projectile(
                                posX = unit.posX,
                                posY = unit.posY,
                                targetX = tx,
                                targetY = ty,
                                speed = 9f,
                                damage = unit.type.damagePerShot,
                                color = Color(0xFFEF4444),
                                isEnemy = true,
                                targetBuildingId = preferredTarget.id
                            )
                        )
                    }
                }
            } else {
                unit.isFiring = false
            }
        }

        units.removeAll(enemiesToRemove)
    }

    fun spawnWave() {
        val count = 1 + (waveNumber - 1) / 2
        addFloatingText("⚠ УВАГА: Хвиля $waveNumber наступає!", coreCenterX, coreCenterY - 3f, Color(0xFFEF4444))

        for (i in 0 until count) {
            val edge = (Math.random() * 4).toInt()
            val (sx, sy) = when (edge) {
                0 -> Pair(2f + (Math.random() * (width - 4)).toFloat(), 2f) // North
                1 -> Pair(2f + (Math.random() * (width - 4)).toFloat(), (height - 3).toFloat()) // South
                2 -> Pair(2f, 2f + (Math.random() * (height - 4)).toFloat()) // West
                else -> Pair((width - 3).toFloat(), 2f + (Math.random() * (height - 4)).toFloat()) // East
            }

            units.add(
                UnitInstance(
                    type = UnitType.TAL_MECH,
                    posX = sx,
                    posY = sy,
                    health = UnitType.TAL_MECH.maxHealth,
                    isEnemy = true
                )
            )
        }
    }

    private fun updateProjectiles(dt: Float) {
        val projectilesToRemove = mutableListOf<Projectile>()

        for (p in projectiles) {
            p.lifetime -= dt
            if (p.lifetime <= 0f) {
                projectilesToRemove.add(p)
                continue
            }

            val dx = p.targetX - p.posX
            val dy = p.targetY - p.posY
            val dist = hypot(dx, dy)

            if (dist <= 0.6f) {
                // Hit!
                projectilesToRemove.add(p)
                spawnSparks(p.targetX, p.targetY, p.color, 4)

                if (p.isEnemy) {
                    val b = buildings.firstOrNull { it.id == p.targetBuildingId }
                    if (b != null) {
                        b.health -= p.damage
                        if (b.health <= 0) {
                            spawnSparks(b.centerX, b.centerY, Color(0xFFEF4444), 12)
                            addFloatingText("Знищено!", b.centerX, b.centerY, Color(0xFFEF4444))
                            buildings.remove(b)
                        }
                    }
                } else {
                    val u = units.firstOrNull { it.id == p.targetUnitId }
                    if (u != null) {
                        u.health -= p.damage
                        if (u.health <= 0) {
                            spawnSparks(u.posX, u.posY, Color(0xFFF97316), 14)
                            addFloatingText("+Брухт", u.posX, u.posY, Color(0xFF94A3B8))
                            coreResources[Resource.SCRAP] = (coreResources[Resource.SCRAP] ?: 0) + 10
                        }
                    }
                }
            } else {
                val move = p.speed * dt
                p.posX += (dx / dist) * move
                p.posY += (dy / dist) * move
            }
        }

        projectiles.removeAll(projectilesToRemove)
    }

    private fun spawnSparks(x: Float, y: Float, color: Color, count: Int) {
        for (i in 0 until count) {
            val angle = (Math.random() * 2 * Math.PI).toFloat()
            val spd = 1.5f + (Math.random() * 2.5f).toFloat()
            sparks.add(
                SparkParticle(
                    x = x,
                    y = y,
                    vx = cos(angle) * spd,
                    vy = sin(angle) * spd,
                    color = color
                )
            )
        }
    }

    fun canBuildingAcceptResource(building: BuildingInstance, resource: Resource): Boolean {
        if (!building.isFullyBuilt) return false
        return when (building.type) {
            BuildingType.CORE -> true
            BuildingType.COAL_GENERATOR -> {
                resource == Resource.COAL && (building.storedResources[Resource.COAL] ?: 0) < 25
            }
            BuildingType.TURRET -> {
                (resource == Resource.COPPER_AMMO || resource == Resource.SILVER_AMMO) &&
                        (building.turretAmmoType == null || building.turretAmmoType == resource) &&
                        building.turretAmmoCount < 30
            }
            BuildingType.IRON_SMELTER -> {
                when (resource) {
                    Resource.IRON_ORE -> (building.storedResources[Resource.IRON_ORE] ?: 0) < 25
                    Resource.COAL -> (building.storedResources[Resource.COAL] ?: 0) < 20
                    else -> false
                }
            }
            BuildingType.AMMO_FABRICATOR -> {
                when (resource) {
                    Resource.COPPER, Resource.LEAD, Resource.COAL, Resource.SILVER -> {
                        (building.storedResources[resource] ?: 0) < 25
                    }
                    else -> false
                }
            }
            else -> false
        }
    }

    fun deliverResourceToBuilding(building: BuildingInstance, resource: Resource, fxX: Float = building.centerX, fxY: Float = building.centerY): Boolean {
        if (!canBuildingAcceptResource(building, resource)) return false
        when (building.type) {
            BuildingType.CORE -> {
                coreResources[resource] = (coreResources[resource] ?: 0) + 1
                addFloatingText("+1 ${resource.displayName}", fxX, fxY, resource.color)
                return true
            }
            BuildingType.COAL_GENERATOR -> {
                val cur = building.storedResources[Resource.COAL] ?: 0
                building.storedResources[Resource.COAL] = cur + 1
                addFloatingText("+1 Вугілля", fxX, fxY, Resource.COAL.color)
                return true
            }
            BuildingType.TURRET -> {
                building.turretAmmoType = resource
                building.turretAmmoCount++
                addFloatingText("+1 ${resource.displayName}", fxX, fxY, resource.color)
                return true
            }
            BuildingType.IRON_SMELTER -> {
                val cur = building.storedResources[resource] ?: 0
                building.storedResources[resource] = cur + 1
                addFloatingText("+1 ${resource.displayName}", fxX, fxY, resource.color)
                return true
            }
            BuildingType.AMMO_FABRICATOR -> {
                val cur = building.storedResources[resource] ?: 0
                building.storedResources[resource] = cur + 1
                addFloatingText("+1 ${resource.displayName}", fxX, fxY, resource.color)
                return true
            }
            else -> return false
        }
    }

    private fun outputResourceFromBuilding(building: BuildingInstance, resource: Resource): Boolean {
        val borderTiles = mutableListOf<Pair<Int, Int>>()
        for (dx in 0 until building.type.width) {
            borderTiles.add(Pair(building.originX + dx, building.originY - 1)) // North border
            borderTiles.add(Pair(building.originX + dx, building.originY + building.type.height)) // South border
        }
        for (dy in 0 until building.type.height) {
            borderTiles.add(Pair(building.originX - 1, building.originY + dy)) // West border
            borderTiles.add(Pair(building.originX + building.type.width, building.originY + dy)) // East border
        }

        for (tile in borderTiles) {
            val b = getBuildingAt(tile.first, tile.second) ?: continue
            if (!b.isFullyBuilt) continue

            when (b.type) {
                BuildingType.CONVEYOR -> {
                    // Do not output onto a conveyor pointing directly INTO this building
                    val pointsIntoBuilding = building.occupies(tile.first + b.direction.dx, tile.second + b.direction.dy)
                    if (pointsIntoBuilding) continue

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
                        val dir = Direction.fromDelta(tile.first - (building.originX + 1), tile.second - (building.originY + 1))
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
                else -> {
                    // Direct adjacent feeding into other machines
                    if (b.id != building.id && canBuildingAcceptResource(b, resource)) {
                        deliverResourceToBuilding(b, resource, tile.first + 0.5f, tile.second + 0.5f)
                        return true
                    }
                }
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

                    // Collision check with item ahead
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

                        if (nextBuilding != null && nextBuilding.isFullyBuilt) {
                            if (canBuildingAcceptResource(nextBuilding, item.resource)) {
                                deliverResourceToBuilding(nextBuilding, item.resource, nextX + 0.5f, nextY + 0.5f)
                                itemsToRemove.add(item)
                            } else if (nextBuilding.type == BuildingType.CONVEYOR || nextBuilding.type == BuildingType.ROUTER || nextBuilding.type == BuildingType.JUNCTION) {
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
                }

                BuildingType.ROUTER -> {
                    if (item.progress < 1.0f) {
                        item.progress = (item.progress + dt * internalSpeed).coerceAtMost(1.0f)
                    }

                    if (item.progress >= 1.0f) {
                        val neighbors = listOf(
                            Pair(item.tileX, item.tileY - 1) to Direction.NORTH,
                            Pair(item.tileX + 1, item.tileY) to Direction.EAST,
                            Pair(item.tileX, item.tileY + 1) to Direction.SOUTH,
                            Pair(item.tileX - 1, item.tileY) to Direction.WEST
                        )

                        val validOutputs = neighbors.filter { (pos, dir) ->
                            val b = getBuildingAt(pos.first, pos.second)
                            if (b == null || !b.isFullyBuilt) return@filter false
                            if (canBuildingAcceptResource(b, item.resource)) return@filter true
                            if (b.type == BuildingType.CONVEYOR) {
                                return@filter b.direction != dir.opposite()
                            }
                            if (b.type == BuildingType.ROUTER || b.type == BuildingType.JUNCTION) return@filter true
                            false
                        }

                        if (validOutputs.isNotEmpty()) {
                            val chosenIndex = (currentBuilding.routerOutputIndex % validOutputs.size)
                            val (targetPos, targetDir) = validOutputs[chosenIndex]
                            val targetBuilding = getBuildingAt(targetPos.first, targetPos.second)

                            if (targetBuilding != null && canBuildingAcceptResource(targetBuilding, item.resource)) {
                                deliverResourceToBuilding(targetBuilding, item.resource, targetPos.first + 0.5f, targetPos.second + 0.5f)
                                itemsToRemove.add(item)
                                currentBuilding.routerOutputIndex++
                            } else if (targetBuilding?.type == BuildingType.CONVEYOR || targetBuilding?.type == BuildingType.ROUTER || targetBuilding?.type == BuildingType.JUNCTION) {
                                val targetBlocked = items.any { other ->
                                    other.tileX == targetPos.first && other.tileY == targetPos.second && other.progress < 0.30f
                                }
                                if (!targetBlocked) {
                                    item.tileX = targetPos.first
                                    item.tileY = targetPos.second
                                    item.progress = 0f
                                    item.direction = if (targetBuilding.type == BuildingType.CONVEYOR) targetBuilding.direction else targetDir
                                    currentBuilding.routerOutputIndex++
                                }
                            }
                        }
                    }
                }

                BuildingType.JUNCTION -> {
                    if (item.progress < 1.0f) {
                        item.progress = (item.progress + dt * internalSpeed).coerceAtMost(1.0f)
                    }

                    if (item.progress >= 1.0f) {
                        val nextX = item.tileX + item.direction.dx
                        val nextY = item.tileY + item.direction.dy
                        val nextBuilding = getBuildingAt(nextX, nextY)

                        if (nextBuilding != null && nextBuilding.isFullyBuilt) {
                            if (canBuildingAcceptResource(nextBuilding, item.resource)) {
                                deliverResourceToBuilding(nextBuilding, item.resource, nextX + 0.5f, nextY + 0.5f)
                                itemsToRemove.add(item)
                            } else if (nextBuilding.type == BuildingType.CONVEYOR || nextBuilding.type == BuildingType.ROUTER || nextBuilding.type == BuildingType.JUNCTION) {
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

        // Spawn smoke from damaged buildings (< 50% HP)
        for (b in buildings) {
            if (b.isFullyBuilt && b.health < b.type.maxHealth * 0.5f && Math.random() < 0.25) {
                smokeParticles.add(
                    SmokeParticle(
                        x = b.centerX + (Math.random() * 0.6f - 0.3f).toFloat(),
                        y = b.centerY + (Math.random() * 0.6f - 0.3f).toFloat(),
                        vx = (Math.random() * 0.4f - 0.2f).toFloat(),
                        vy = -0.6f - (Math.random() * 0.4f).toFloat(),
                        size = 6f + (Math.random() * 6f).toFloat()
                    )
                )
            }
        }

        // Spawn smoke from damaged units (< 50% HP)
        for (u in units) {
            if (u.isAlive && u.health < u.type.maxHealth * 0.5f && Math.random() < 0.25) {
                smokeParticles.add(
                    SmokeParticle(
                        x = u.posX,
                        y = u.posY,
                        vx = (Math.random() * 0.4f - 0.2f).toFloat(),
                        vy = -0.5f - (Math.random() * 0.4f).toFloat(),
                        size = 5f + (Math.random() * 5f).toFloat()
                    )
                )
            }
        }

        // Update smoke particles
        val smokeToRemove = mutableListOf<SmokeParticle>()
        for (smoke in smokeParticles) {
            smoke.lifetime -= dt
            smoke.x += smoke.vx * dt
            smoke.y += smoke.vy * dt
            smoke.size += dt * 7f
            smoke.alpha = (smoke.lifetime / 1.2f).coerceIn(0f, 1f)
            if (smoke.lifetime <= 0f) {
                smokeToRemove.add(smoke)
            }
        }
        smokeParticles.removeAll(smokeToRemove)
    }
}
