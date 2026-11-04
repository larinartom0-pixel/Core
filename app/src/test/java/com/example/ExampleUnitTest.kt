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

        // Initial copper & silver balance
        val copper = world.coreResources[Resource.COPPER] ?: 0
        val silver = world.coreResources[Resource.SILVER] ?: 0
        assertTrue("Core should have starting copper", copper >= 100)
        assertTrue("Core should have starting silver", silver >= 20)
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
        assertEquals(1, world.getTotalBlueprintCopperCost())

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

    @Test
    fun testAutoJunctionConversion() {
        val world = GameWorld(40, 40)
        val x = 15
        val y = 15

        // First place a vertical conveyor
        world.toggleBlueprint(x, y, BuildingType.CONVEYOR, Direction.SOUTH)
        world.confirmBlueprints()
        val conv = world.buildings.first { it.originX == x && it.originY == y }
        conv.buildProgress = 1.0f
        world.constructionQueue.clear()

        // Drag a horizontal conveyor crossing this tile
        world.addConveyorAt(x, y, Direction.EAST)

        // Should automatically convert existing conveyor into a Junction blueprint
        val junctionBp = world.blueprints.firstOrNull { it.originX == x && it.originY == y }
        assertNotNull("Should create a junction blueprint at intersection", junctionBp)
        assertEquals(BuildingType.JUNCTION, junctionBp!!.type)
    }

    @Test
    fun testDeconstructionRefund() {
        val world = GameWorld(40, 40)
        val x = 10
        val y = 10

        // Place a router
        world.toggleBlueprint(x, y, BuildingType.ROUTER, Direction.EAST)
        world.confirmBlueprints()
        val router = world.buildings.first { it.originX == x && it.originY == y }
        router.buildProgress = 1.0f
        world.constructionQueue.clear()

        val copperBeforeDemolish = world.coreResources[Resource.COPPER] ?: 0

        // Mark for deconstruction
        world.toggleMarkForDeconstruction(x, y)
        assertEquals(1, world.getMarkedForDeconstructionCount())

        // Confirm demolish
        world.confirmDeconstruction()

        // Simulate drone dismantling loop
        for (i in 0 until 120) {
            world.update(0.05f)
        }

        // Router should be removed and 100% copper refunded
        val remaining = world.buildings.firstOrNull { it.originX == x && it.originY == y }
        assertNull("Building should be dismantled", remaining)
        val copperAfterDemolish = world.coreResources[Resource.COPPER] ?: 0
        assertEquals(copperBeforeDemolish + BuildingType.ROUTER.costCopper, copperAfterDemolish)
    }

    @Test
    fun testMarkedBuildingContinuesWorkingAndItemsPreserved() {
        val world = GameWorld(40, 40)
        val convX = 12
        val convY = 12

        // Place a conveyor
        world.toggleBlueprint(convX, convY, BuildingType.CONVEYOR, Direction.EAST)
        world.confirmBlueprints()
        val conv = world.buildings.first { it.originX == convX && it.originY == convY }
        conv.buildProgress = 1.0f
        world.constructionQueue.clear()

        // Put an item on it
        world.items.add(
            com.example.model.TransportedItem(
                id = "item_1",
                resource = Resource.COPPER,
                tileX = convX,
                tileY = convY,
                progress = 0.2f,
                direction = Direction.EAST
            )
        )

        // Mark conveyor for deconstruction (e.g. selected with trash tool)
        world.toggleMarkForDeconstruction(convX, convY)
        assertTrue("Conveyor is marked for deconstruction", conv.isMarkedForDeconstruction)
        assertTrue("Conveyor is still fully built and operational", conv.isFullyBuilt)

        // Update world a few ticks
        world.update(0.1f)

        // The item must NOT have disappeared!
        assertEquals("Item must still exist on conveyor", 1, world.items.size)
        assertTrue("Item progress must have advanced", world.items[0].progress > 0.2f)
    }

    @Test
    fun testDeconstructionItemRefund() {
        val world = GameWorld(40, 40)
        val convX = 14
        val convY = 14

        // Place a conveyor
        world.toggleBlueprint(convX, convY, BuildingType.CONVEYOR, Direction.EAST)
        world.confirmBlueprints()
        val conv = world.buildings.first { it.originX == convX && it.originY == convY }
        conv.buildProgress = 1.0f
        world.constructionQueue.clear()

        val copperBefore = world.coreResources[Resource.COPPER] ?: 0

        // Put an item on the conveyor
        world.items.add(
            com.example.model.TransportedItem(
                id = "item_refund",
                resource = Resource.COPPER,
                tileX = convX,
                tileY = convY,
                progress = 0.5f,
                direction = Direction.EAST
            )
        )

        // Mark and confirm deconstruct
        world.toggleMarkForDeconstruction(convX, convY)
        world.confirmDeconstruction()

        // Run until deconstruction is finished
        for (i in 0 until 120) {
            world.update(0.05f)
        }

        // Conveyor should be gone, and BOTH building cost (1 Cu) AND item on conveyor (1 Cu) should be refunded!
        val copperAfter = world.coreResources[Resource.COPPER] ?: 0
        assertEquals("Should refund building cost (1) + item (1) = +2 Cu", copperBefore + 2, copperAfter)
        assertEquals("Items list should be empty", 0, world.items.size)
    }

    @Test
    fun testDrillMiningRate() {
        val world = GameWorld(40, 40)
        val drillX = 5
        val drillY = 5

        // Place copper ores on all 4 tiles
        for (dx in 0 until 2) {
            for (dy in 0 until 2) {
                world.grid[drillX + dx][drillY + dy] = world.grid[drillX + dx][drillY + dy].copy(type = TileType.COPPER_ORE)
            }
        }

        // Place drill
        world.toggleBlueprint(drillX, drillY, BuildingType.COPPER_DRILL, Direction.EAST)
        world.confirmBlueprints()
        val drill = world.buildings.first { it.originX == drillX && it.originY == drillY }
        drill.buildProgress = 1.0f
        world.constructionQueue.clear()

        // Place a conveyor line adjacent to drill's East border (drill is 2x2 at (5,5))
        for (cx in (drillX + 2)..(drillX + 15)) {
            world.grid[cx][drillY] = world.grid[cx][drillY].copy(type = TileType.DIRT)
            world.toggleBlueprint(cx, drillY, BuildingType.CONVEYOR, Direction.EAST)
        }
        world.confirmBlueprints()
        for (b in world.buildings) {
            b.buildProgress = 1.0f
        }
        world.constructionQueue.clear()

        // Count mined items over 10 seconds
        val itemsStart = world.items.size
        for (i in 0 until 200) { // 200 * 0.05s = 10.0 seconds
            world.update(0.05f)
        }

        // At 1.2 items/sec for 10s, should be around 12 items mined (allowing small timing delta)
        val itemsProduced = world.items.size - itemsStart
        assertTrue("Expected ~12 items in 10s at 1.2 items/sec, got $itemsProduced", itemsProduced in 10..14)
    }
}
