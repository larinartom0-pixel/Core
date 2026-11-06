package com.example

import com.example.game.GameWorld
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testWorldGenerationAndCore() {
        val world = GameWorld(80, 80, seed = 42L)
        assertEquals(80, world.width)
        assertEquals(80, world.height)

        // Core exists at center
        val core = world.buildings.firstOrNull { it.type == BuildingType.CORE }
        assertNotNull(core)
        assertEquals(3, core!!.type.width)
        assertEquals(3, core.type.height)
        assertTrue(core.isFullyBuilt)

        // Initial resources balance
        val copper = world.coreResources[Resource.COPPER] ?: 0
        val lead = world.coreResources[Resource.LEAD] ?: 0
        val silver = world.coreResources[Resource.SILVER] ?: 0
        val coal = world.coreResources[Resource.COAL] ?: 0
        assertTrue("Core should have starting copper", copper >= 100)
        assertTrue("Core should have starting lead", lead >= 40)
        assertTrue("Core should have starting silver", silver >= 20)
        assertTrue("Core should have starting coal", coal >= 20)
    }

    @Test
    fun testBlueprintToggleAndCancel() {
        val world = GameWorld(80, 80)
        val testX = 40
        val testY = 44
        world.grid[testX][testY] = Tile(testX, testY, TileType.DIRT)

        // Place conveyor blueprint
        val placed = world.toggleBlueprint(testX, testY, BuildingType.CONVEYOR, Direction.EAST)
        assertTrue("Blueprint should be placed", placed)
        assertEquals(1, world.blueprints.size)

        // Tap again on the same tile -> should cancel blueprint
        val removed = world.toggleBlueprint(testX, testY, BuildingType.CONVEYOR, Direction.EAST)
        assertFalse("Blueprint should be removed on second tap", removed)
        assertEquals(0, world.blueprints.size)
    }

    @Test
    fun testPreserveExistingBuildingOnConveyorDrag() {
        val world = GameWorld(80, 80)
        val x = 40
        val y = 45
        world.grid[x][y] = Tile(x, y, TileType.DIRT)

        // First place a vertical conveyor
        world.toggleBlueprint(x, y, BuildingType.CONVEYOR, Direction.SOUTH)
        world.confirmBlueprints()
        val conv = world.buildings.first { it.originX == x && it.originY == y }
        conv.buildProgress = 1.0f
        world.constructionQueue.clear()

        // Drag a horizontal conveyor crossing this tile
        world.addConveyorAt(x, y, Direction.EAST)

        // Built building must remain intact and NOT be deleted
        val stillExists = world.buildings.firstOrNull { it.originX == x && it.originY == y }
        assertNotNull("Existing building must be preserved", stillExists)
        assertEquals(BuildingType.CONVEYOR, stillExists!!.type)
        assertEquals(Direction.SOUTH, stillExists.direction)
    }

    @Test
    fun testPowerGridAndCoalGenerator() {
        val world = GameWorld(80, 80)
        val genX = 40
        val genY = 45

        // Clear terrain
        for (x in (genX - 2)..(genX + 6)) {
            for (y in (genY - 2)..(genY + 4)) {
                world.grid[x][y] = Tile(x, y, TileType.DIRT)
            }
        }

        // Place Coal Generator, Power Node and Battery
        assertTrue(world.toggleBlueprint(genX, genY, BuildingType.COAL_GENERATOR, Direction.NORTH))
        assertTrue(world.toggleBlueprint(genX + 2, genY, BuildingType.POWER_NODE, Direction.NORTH))
        assertTrue(world.toggleBlueprint(genX + 4, genY, BuildingType.BATTERY, Direction.NORTH))
        assertTrue(world.confirmBlueprints())

        for (b in world.buildings) {
            b.buildProgress = 1.0f
        }
        world.constructionQueue.clear()

        val gen = world.buildings.first { it.type == BuildingType.COAL_GENERATOR }
        val battery = world.buildings.first { it.type == BuildingType.BATTERY }

        // Feed coal into generator
        gen.storedResources[Resource.COAL] = 10

        assertEquals(0f, battery.energyStored, 0.01f)

        // Run power simulation
        for (i in 0 until 60) {
            world.update(0.05f) // 3 seconds
        }

        // Generator should have burned coal and battery should be charged
        assertTrue("Battery should accumulate energy from generator", battery.energyStored > 10f)
        assertTrue("Generator should have consumed coal", (gen.storedResources[Resource.COAL] ?: 0) < 10)
    }

    @Test
    fun testTurretFiringAtEnemy() {
        val world = GameWorld(80, 80)
        val turretX = 40
        val turretY = 45

        for (x in (turretX - 2)..(turretX + 6)) {
            for (y in (turretY - 2)..(turretY + 4)) {
                world.grid[x][y] = Tile(x, y, TileType.DIRT)
            }
        }

        // Place Turret
        assertTrue(world.toggleBlueprint(turretX, turretY, BuildingType.TURRET, Direction.NORTH))
        assertTrue(world.confirmBlueprints())
        val turret = world.buildings.first { it.type == BuildingType.TURRET }
        turret.buildProgress = 1.0f
        world.constructionQueue.clear()

        // Load copper ammo into turret
        turret.turretAmmoType = Resource.COPPER_AMMO
        turret.turretAmmoCount = 20

        // Spawn Tal mech enemy nearby
        val enemy = UnitInstance(
            type = UnitType.TAL_MECH,
            posX = turretX + 3.0f,
            posY = turretY.toFloat(),
            health = 100,
            isEnemy = true
        )
        world.units.add(enemy)

        val initialAmmo = turret.turretAmmoCount

        // Update world so turret targets and shoots enemy
        for (i in 0 until 30) {
            world.update(0.05f)
        }

        assertTrue("Turret should have fired ammo at enemy", turret.turretAmmoCount < initialAmmo)
        assertTrue("Projectiles should be spawned or enemy damaged", world.projectiles.isNotEmpty() || enemy.health < 100)
    }

    @Test
    fun testWaveSpawning() {
        val world = GameWorld(80, 80)
        assertEquals(0, world.units.size)

        world.waveNumber = 1
        world.spawnWave()

        assertEquals("Wave 1 should spawn 1 Tal mech", 1, world.units.size)
        val mech = world.units.first()
        assertEquals(UnitType.TAL_MECH, mech.type)
        assertTrue(mech.isEnemy)
        assertTrue(mech.isAlive)
    }

    @Test
    fun testIronSmelter() {
        val world = GameWorld(80, 80)
        val smelterX = 40
        val smelterY = 45

        for (x in (smelterX - 2)..(smelterX + 6)) {
            for (y in (smelterY - 2)..(smelterY + 4)) {
                world.grid[x][y] = Tile(x, y, TileType.DIRT)
            }
        }

        // Place Generator & Power Node to power the smelter
        world.toggleBlueprint(smelterX - 2, smelterY, BuildingType.COAL_GENERATOR, Direction.NORTH)
        world.toggleBlueprint(smelterX, smelterY, BuildingType.IRON_SMELTER, Direction.NORTH)
        // Add output conveyor
        world.toggleBlueprint(smelterX + 2, smelterY, BuildingType.CONVEYOR, Direction.EAST)
        assertTrue(world.confirmBlueprints())

        for (b in world.buildings) {
            b.buildProgress = 1.0f
        }
        world.constructionQueue.clear()

        val gen = world.buildings.first { it.type == BuildingType.COAL_GENERATOR }
        gen.storedResources[Resource.COAL] = 20

        val smelter = world.buildings.first { it.type == BuildingType.IRON_SMELTER }
        smelter.storedResources[Resource.IRON_ORE] = 6
        smelter.storedResources[Resource.COAL] = 4

        // Run simulation for 1.1 seconds (one craft cycle takes 1.0s)
        for (i in 0 until 22) {
            world.update(0.05f)
        }

        // Smelter should have consumed 3 iron ore and produced 1 iron ingot on the conveyor
        val itemsProduced = world.items.count { it.resource == Resource.IRON_INGOT }
        assertTrue("Should produce iron ingot on conveyor", itemsProduced > 0)
        assertEquals(3, smelter.storedResources[Resource.IRON_ORE])
    }

    @Test
    fun testConveyorDragLineOrientation() {
        val world = GameWorld(80, 80)
        for (x in 18..25) {
            for (y in 18..25) {
                world.grid[x][y] = Tile(x, y, TileType.DIRT)
            }
        }

        // Simulate conveyor drag from (20, 20) to (21, 20)
        val dX = 21 - 20
        val dY = 20 - 20
        val dir = Direction.fromDelta(dX, dY)
        world.addConveyorAt(20, 20, dir)
        world.addConveyorAt(21, 20, dir)

        val firstBp = world.blueprints.firstOrNull { it.originX == 20 && it.originY == 20 }
        val secondBp = world.blueprints.firstOrNull { it.originX == 21 && it.originY == 20 }

        assertNotNull("First conveyor blueprint should exist", firstBp)
        assertNotNull("Second conveyor blueprint should exist", secondBp)
        assertEquals(Direction.EAST, firstBp!!.direction)
        assertEquals(Direction.EAST, secondBp!!.direction)
    }

    @Test
    fun testRouterFeedsCoalGenerator() {
        val world = GameWorld(80, 80)
        val rx = 30
        val ry = 30
        world.grid[rx][ry] = Tile(rx, ry, TileType.DIRT)
        world.grid[rx + 1][ry] = Tile(rx + 1, ry, TileType.DIRT)

        // Place Router at (30, 30) and Coal Generator at (31, 30)
        world.toggleBlueprint(rx, ry, BuildingType.ROUTER, Direction.NORTH)
        world.toggleBlueprint(rx + 1, ry, BuildingType.COAL_GENERATOR, Direction.NORTH)
        world.confirmBlueprints()

        for (b in world.buildings) {
            b.buildProgress = 1.0f
        }
        world.constructionQueue.clear()

        val gen = world.buildings.first { it.type == BuildingType.COAL_GENERATOR }
        assertEquals(0, gen.storedResources[Resource.COAL] ?: 0)

        // Inject coal item inside the router
        world.items.add(
            TransportedItem(
                id = "item_coal_test",
                resource = Resource.COAL,
                tileX = rx,
                tileY = ry,
                progress = 0.95f,
                direction = Direction.EAST
            )
        )

        // Update world logistics
        for (i in 0 until 5) {
            world.update(0.05f)
        }

        // Router should have delivered the coal item directly into the adjacent generator
        assertEquals(1, gen.storedResources[Resource.COAL] ?: 0)
        assertEquals(0, world.items.size)
    }

    @Test
    fun testSmelterInternalBufferingWithoutConveyor() {
        val world = GameWorld(80, 80)
        val sx = 30
        val sy = 30
        for (x in (sx - 2)..(sx + 3)) {
            for (y in (sy - 2)..(sy + 3)) {
                world.grid[x][y] = Tile(x, y, TileType.DIRT)
            }
        }

        // Place powered generator and standalone Iron Smelter without output conveyor
        world.toggleBlueprint(sx - 2, sy, BuildingType.COAL_GENERATOR, Direction.NORTH)
        world.toggleBlueprint(sx, sy, BuildingType.IRON_SMELTER, Direction.NORTH)
        world.confirmBlueprints()

        for (b in world.buildings) {
            b.buildProgress = 1.0f
        }
        world.constructionQueue.clear()

        val gen = world.buildings.first { it.type == BuildingType.COAL_GENERATOR }
        gen.storedResources[Resource.COAL] = 20

        val smelter = world.buildings.first { it.type == BuildingType.IRON_SMELTER }
        smelter.storedResources[Resource.IRON_ORE] = 3
        smelter.storedResources[Resource.COAL] = 1

        // Run simulation for 1.2s (cycle = 1.0s)
        for (i in 0 until 24) {
            world.update(0.05f)
        }

        // Smelter should have crafted into its internal buffer even without any output conveyor!
        assertEquals(1, smelter.storedResources[Resource.IRON_INGOT] ?: 0)
        assertEquals(0, smelter.storedResources[Resource.IRON_ORE] ?: 0)
        assertEquals(0, smelter.storedResources[Resource.COAL] ?: 0)
    }
}
