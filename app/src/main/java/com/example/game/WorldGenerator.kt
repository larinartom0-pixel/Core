package com.example.game

import com.example.model.Tile
import com.example.model.TileType
import kotlin.math.hypot
import kotlin.random.Random

object WorldGenerator {

    fun generateWorld(width: Int = 40, height: Int = 40, seed: Long = 1337L): Array<Array<Tile>> {
        val random = Random(seed)
        val grid = Array(width) { x ->
            Array(height) { y ->
                Tile(x, y, TileType.DIRT)
            }
        }

        val centerX = width / 2
        val centerY = height / 2

        // 1. Generate natural Stone terrain patches
        for (x in 0 until width) {
            for (y in 0 until height) {
                val distToCenter = hypot((x - centerX).toFloat(), (y - centerY).toFloat())
                if (distToCenter > 3.5f) {
                    val noise = (kotlin.math.sin(x * 0.35) + kotlin.math.cos(y * 0.35) + random.nextFloat() * 0.4).toFloat()
                    if (noise > 0.6f) {
                        grid[x][y] = Tile(x, y, TileType.STONE)
                    }
                }
            }
        }

        // Helper to place an ore cluster
        fun placeOreCluster(cx: Int, cy: Int, oreType: TileType, radius: Float) {
            for (dx in -3..3) {
                for (dy in -3..3) {
                    val tx = cx + dx
                    val ty = cy + dy
                    if (tx in 1 until width - 1 && ty in 1 until height - 1) {
                        val d = hypot(dx.toFloat(), dy.toFloat())
                        if (d <= radius + (random.nextFloat() * 0.5f - 0.25f)) {
                            grid[tx][ty] = Tile(tx, ty, oreType)
                        }
                    }
                }
            }
        }

        // 2. Copper Ore Veins (Near and medium distances)
        val copperVeins = listOf(
            Pair(centerX - 6, centerY - 1),
            Pair(centerX + 6, centerY + 3),
            Pair(centerX + 3, centerY - 9),
            Pair(centerX - 11, centerY + 11)
        )
        for (v in copperVeins) {
            placeOreCluster(v.first, v.second, TileType.COPPER_ORE, 2.3f)
        }

        // 3. Lead Ore Veins (Свинець - purple)
        val leadVeins = listOf(
            Pair(centerX - 2, centerY + 7),
            Pair(centerX + 9, centerY - 4),
            Pair(centerX - 12, centerY - 7)
        )
        for (v in leadVeins) {
            placeOreCluster(v.first, v.second, TileType.LEAD_ORE, 2.2f)
        }

        // 4. Coal Ore Veins (Вугілля - dark charcoal)
        val coalVeins = listOf(
            Pair(centerX + 7, centerY + 8),
            Pair(centerX - 8, centerY - 8),
            Pair(centerX + 12, centerY + 2)
        )
        for (v in coalVeins) {
            placeOreCluster(v.first, v.second, TileType.COAL_ORE, 2.4f)
        }

        // 5. Silver Ore Veins (Срібло - bright silver/platinum)
        val silverVeins = listOf(
            Pair(centerX + 1, centerY - 7),
            Pair(centerX - 7, centerY + 5),
            Pair(centerX + 11, centerY - 9),
            Pair(centerX - 11, centerY + 3)
        )
        for (v in silverVeins) {
            placeOreCluster(v.first, v.second, TileType.SILVER_ORE, 2.0f)
        }

        // 6. Scrap deposits
        val scrapCenters = listOf(
            Pair(centerX + 12, centerY - 7),
            Pair(centerX - 13, centerY + 4)
        )
        for (v in scrapCenters) {
            placeOreCluster(v.first, v.second, TileType.SCRAP_DEPOSIT, 1.8f)
        }

        // 7. Perimeter rock walls
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                    grid[x][y] = Tile(x, y, TileType.ROCK_WALL)
                } else {
                    val distToCenter = hypot((x - centerX).toFloat(), (y - centerY).toFloat())
                    if (distToCenter > 15f && random.nextFloat() < 0.08f) {
                        grid[x][y] = Tile(x, y, TileType.ROCK_WALL)
                    }
                }
            }
        }

        // 8. Ensure core footprint (center 7x7) is 100% buildable and clear
        for (cx in (centerX - 3)..(centerX + 3)) {
            for (cy in (centerY - 3)..(centerY + 3)) {
                if (cx in 0 until width && cy in 0 until height) {
                    grid[cx][cy] = Tile(cx, cy, TileType.DIRT)
                }
            }
        }

        return grid
    }
}
