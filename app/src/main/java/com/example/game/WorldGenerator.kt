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

        // 2. Generate natural Copper Ore Veins (near and medium distances)
        val copperVeinCenters = listOf(
            Pair(centerX - 6, centerY - 1),
            Pair(centerX + 6, centerY + 3),
            Pair(centerX - 2, centerY + 7),
            Pair(centerX + 3, centerY - 8),
            Pair(centerX - 10, centerY - 8),
            Pair(centerX + 11, centerY + 9),
            Pair(centerX - 11, centerY + 11)
        )

        for (vein in copperVeinCenters) {
            val vx = vein.first
            val vy = vein.second
            val radius = 2.4f + random.nextFloat() * 0.8f
            for (dx in -3..3) {
                for (dy in -3..3) {
                    val tx = vx + dx
                    val ty = vy + dy
                    if (tx in 1 until width - 1 && ty in 1 until height - 1) {
                        val d = hypot(dx.toFloat(), dy.toFloat())
                        if (d <= radius + (random.nextFloat() * 0.6f - 0.3f)) {
                            grid[tx][ty] = Tile(tx, ty, TileType.COPPER_ORE)
                        }
                    }
                }
            }
        }

        // 3. Generate some scrap deposits
        val scrapCenters = listOf(
            Pair(centerX + 12, centerY - 7),
            Pair(centerX - 12, centerY + 3)
        )
        for (vein in scrapCenters) {
            val vx = vein.first
            val vy = vein.second
            for (dx in -2..2) {
                for (dy in -2..2) {
                    val tx = vx + dx
                    val ty = vy + dy
                    if (tx in 1 until width - 1 && ty in 1 until height - 1) {
                        if (hypot(dx.toFloat(), dy.toFloat()) <= 1.8f) {
                            grid[tx][ty] = Tile(tx, ty, TileType.SCRAP_DEPOSIT)
                        }
                    }
                }
            }
        }

        // 4. Generate rock walls around perimeter and scattered clusters
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                    grid[x][y] = Tile(x, y, TileType.ROCK_WALL)
                } else {
                    val distToCenter = hypot((x - centerX).toFloat(), (y - centerY).toFloat())
                    if (distToCenter > 14f && random.nextFloat() < 0.08f) {
                        grid[x][y] = Tile(x, y, TileType.ROCK_WALL)
                    }
                }
            }
        }

        // 5. Ensure core footprint (center 7x7) is 100% buildable and clear
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
