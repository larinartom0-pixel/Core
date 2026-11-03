package com.example.model

data class TransportedItem(
    val id: String,
    val resource: Resource,
    var tileX: Int,
    var tileY: Int,
    var progress: Float = 0f, // 0.0 to 1.0 along the conveyor direction
    var direction: Direction = Direction.EAST
)
