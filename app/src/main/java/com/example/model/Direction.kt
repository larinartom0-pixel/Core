package com.example.model

enum class Direction(val dx: Int, val dy: Int, val angleDegrees: Float) {
    NORTH(0, -1, 0f),
    EAST(1, 0, 90f),
    SOUTH(0, 1, 180f),
    WEST(-1, 0, 270f);

    fun next(): Direction = when (this) {
        NORTH -> EAST
        EAST -> SOUTH
        SOUTH -> WEST
        WEST -> NORTH
    }

    fun previous(): Direction = when (this) {
        NORTH -> WEST
        WEST -> SOUTH
        SOUTH -> EAST
        EAST -> NORTH
    }

    fun opposite(): Direction = when (this) {
        NORTH -> SOUTH
        SOUTH -> NORTH
        EAST -> WEST
        WEST -> EAST
    }

    companion object {
        fun fromDelta(dx: Int, dy: Int): Direction {
            return if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
                if (dx >= 0) EAST else WEST
            } else {
                if (dy >= 0) SOUTH else NORTH
            }
        }
    }
}
