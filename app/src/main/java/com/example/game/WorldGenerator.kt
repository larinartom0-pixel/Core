package com.example.game

import com.example.model.Tile
import com.example.model.TileType
import kotlin.math.hypot
import kotlin.random.Random

object WorldGenerator {

    fun generateWorld(width: Int = 80, height: Int = 80, seed: Long = 1337L): Array<Array<Tile>> {
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
                if (distToCenter > 4.5f) {
                    val noise = (kotlin.math.sin(x * 0.25) + kotlin.math.cos(y * 0.25) + random.nextFloat() * 0.4).toFloat()
                    if (noise > 0.55f) {
                        grid[x][y] = Tile(x, y, TileType.STONE)
                    }
                }
            }
        }

        // Helper to place an ore cluster
        fun placeOreCluster(cx: Int, cy: Int, oreType: TileType, radius: Float) {
            val rInt = radius.toInt() + 2
            for (dx in -rInt..rInt) {
                for (dy in -rInt..rInt) {
                    val tx = cx + dx
                    val ty = cy + dy
                    if (tx in 2 until width - 2 && ty in 2 until height - 2) {
                        val d = hypot(dx.toFloat(), dy.toFloat())
                        if (d <= radius + (random.nextFloat() * 0.6f - 0.3f)) {
                            grid[tx][ty] = Tile(tx, ty, oreType)
                        }
                    }
                }
            }
        }

        // 2. Copper Ore Veins (Near, medium and far)
        val copperVeins = listOf(
            Pair(centerX - 7, centerY - 2),
            Pair(centerX + 8, centerY + 4),
            Pair(centerX + 4, centerY - 12),
            Pair(centerX - 16, centerY + 14),
            Pair(centerX + 22, centerY - 18),
            Pair(centerX - 24, centerY - 20)
        )
        for (v in copperVeins) {
            placeOreCluster(v.first, v.second, TileType.COPPER_ORE, 2.6f)
        }

        // 3. Lead Ore Veins (Свинець - purple)
        val leadVeins = listOf(
            Pair(centerX - 3, centerY + 9),
            Pair(centerX + 12, centerY - 5),
            Pair(centerX - 18, centerY - 10),
            Pair(centerX + 20, centerY + 16),
            Pair(centerX - 26, centerY + 22)
        )
        for (v in leadVeins) {
            placeOreCluster(v.first, v.second, TileType.LEAD_ORE, 2.5f)
        }

        // 4. Coal Ore Veins (Вугілля - dark charcoal for power generators)
        val coalVeins = listOf(
            Pair(centerX + 9, centerY + 10),
            Pair(centerX - 10, centerY - 11),
            Pair(centerX + 16, centerY + 3),
            Pair(centerX - 20, centerY + 20),
            Pair(centerX + 26, centerY - 12)
        )
        for (v in coalVeins) {
            placeOreCluster(v.first, v.second, TileType.COAL_ORE, 2.7f)
        }

        // 5. Silver Ore Veins (Срібло - bright silver)
        val silverVeins = listOf(
            Pair(centerX + 2, centerY - 9),
            Pair(centerX - 9, centerY + 7),
            Pair(centerX + 15, centerY - 12),
            Pair(centerX - 15, centerY + 4),
            Pair(centerX + 24, centerY + 22)
        )
        for (v in silverVeins) {
            placeOreCluster(v.first, v.second, TileType.SILVER_ORE, 2.3f)
        }

        // 6. Iron Ore Veins (Залізна руда - brown-rust for smelters)
        val ironVeins = listOf(
            Pair(centerX - 11, centerY - 3),
            Pair(centerX + 11, centerY - 15),
            Pair(centerX + 6, centerY + 16),
            Pair(centerX - 22, centerY - 16),
            Pair(centerX + 28, centerY + 8)
        )
        for (v in ironVeins) {
            placeOreCluster(v.first, v.second, TileType.IRON_ORE, 2.6f)
        }

        // 7. Scrap deposits
        val scrapCenters = listOf(
            Pair(centerX + 18, centerY - 8),
            Pair(centerX - 19, centerY + 6),
            Pair(centerX - 28, centerY + 15)
        )
        for (v in scrapCenters) {
            placeOreCluster(v.first, v.second, TileType.SCRAP_DEPOSIT, 2.0f)
        }

        // 8. Perimeter rock walls & scattered rock obstacles
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                    grid[x][y] = Tile(x, y, TileType.ROCK_WALL)
                } else {
                    val distToCenter = hypot((x - centerX).toFloat(), (y - centerY).toFloat())
                    if (distToCenter > 20f && random.nextFloat() < 0.04f) {
                        grid[x][y] = Tile(x, y, TileType.ROCK_WALL)
                    }
                }
            }
        }

        // 9. Ensure core footprint (center 9x9) is 100% buildable dirt
        for (cx in (centerX - 4)..(centerX + 4)) {
            for (cy in (centerY - 4)..(centerY + 4)) {
                if (cx in 0 until width && cy in 0 until height) {
                    grid[cx][cy] = Tile(cx, cy, TileType.DIRT)
                }
            }
        }

        return grid
    }
}
