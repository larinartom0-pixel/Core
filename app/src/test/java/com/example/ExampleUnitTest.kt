package com.example

import com.example.game.GameWorld
import com.example.model.BuildingType
import com.example.model.Direction
import com.example.model.Resource
import com.example.model.TileType
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testWorldGenerationAndCore() {
        val world = GameWorld(40, 40, seed = 42L)
        assertEquals(40, world.width)
        assertEquals(40, world.height)

        // Core exists at center
        val core = world.buildings.firstOrNull { it.type == BuildingType.CORE }
        assertNotNull(core)
        assertEquals(3, core!!.type.width)
        assertEquals(3, core.type.height)
        assertTrue(core.isFullyBuilt)

        // Initial copper balance
        val copper = world.coreResources[Resource.COPPER] ?: 0
        assertTrue("Core should have starting copper", copper >= 100)
    }

    @Test
    fun testBlueprintToggleAndCancel() {
        val world = GameWorld(40, 40)
        val testX = 10
        val testY = 10

        // Place conveyor blueprint
        val placed = world.toggleBlueprint(testX, testY, BuildingType.CONVEYOR, Direction.EAST)
        assertTrue("Blueprint should be placed", placed)
        assertEquals(1, world.blueprints.size)

        // Tap again on the same tile -> should cancel blueprint (as per user request)
        val removed = world.toggleBlueprint(testX, testY, BuildingType.CONVEYOR, Direction.EAST)
        assertFalse("Blueprint should be removed on second tap", removed)
        assertEquals(0, world.blueprints.size)
    }

    @Test
    fun testBlueprintConfirmationAndDroneConstruction() {
        val world = GameWorld(40, 40)
        val initialCopper = world.coreResources[Resource.COPPER] ?: 0

        // Place blueprint
        world.toggleBlueprint(12, 12, BuildingType.CONVEYOR, Direction.EAST)
        assertEquals(1, world.blueprints.size)
        assertEquals(1, world.getTotalBlueprintCost())

        // Confirm
        val confirmed = world.confirmBlueprints()
        assertTrue(confirmed)
        assertEquals(0, world.blueprints.size)
        assertEquals(initialCopper - 1, world.coreResources[Resource.COPPER])
        assertEquals(1, world.constructionQueue.size)

        val newBuilding = world.buildings.first { it.originX == 12 && it.originY == 12 }
        assertFalse(newBuilding.isFullyBuilt)

        // Simulate drone building loop
        for (i in 0 until 120) {
            world.update(0.05f)
        }

        assertTrue("Building should be finished by builder drone", newBuilding.isFullyBuilt)
        assertEquals(0, world.constructionQueue.size)
    }

    @Test
    fun testConveyorTransportationIntoCore() {
        val world = GameWorld(40, 40)
        val core = world.buildings.first { it.type == BuildingType.CORE }
        val initialCopper = world.coreResources[Resource.COPPER] ?: 0

        // Place a conveyor pointing directly into Core from the left (West side of Core)
        // Core is at (19, 19). Conveyor at (18, 19) facing EAST points into (19, 19)
        val conveyorX = core.originX - 1
        val conveyorY = core.originY
        world.grid[conveyorX][conveyorY] = world.grid[conveyorX][conveyorY].copy(type = TileType.DIRT)

        world.toggleBlueprint(conveyorX, conveyorY, BuildingType.CONVEYOR, Direction.EAST)
        world.confirmBlueprints()

        // Fast finish construction
        val conveyor = world.buildings.first { it.originX == conveyorX && it.originY == conveyorY }
        conveyor.buildProgress = 1.0f
        world.constructionQueue.clear()

        // Place an item on conveyor
        world.items.add(
            com.example.model.TransportedItem(
                id = "test_item",
                resource = Resource.COPPER,
                tileX = conveyorX,
                tileY = conveyorY,
                progress = 0.8f,
                direction = Direction.EAST
            )
        )

        // Update world so item moves into Core
        world.update(0.2f)

        // Item should have entered Core and increased resource count
        val updatedCopper = world.coreResources[Resource.COPPER] ?: 0
        assertEquals(initialCopper, updatedCopper) // initial copper minus 1 for build + 1 for delivered item
        assertEquals(0, world.items.size)
    }
}
