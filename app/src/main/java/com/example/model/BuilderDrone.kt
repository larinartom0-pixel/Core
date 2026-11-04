package com.example.model

enum class DroneState {
    IDLE_AT_CORE,
    FLYING_TO_BUILD,
    BUILDING,
    FLYING_TO_DECONSTRUCT,
    DECONSTRUCTING,
    RETURNING_TO_CORE
}

data class BuilderDrone(
    var posX: Float,
    var posY: Float,
    var targetX: Float = posX,
    var targetY: Float = posY,
    var state: DroneState = DroneState.IDLE_AT_CORE,
    var targetBuildingId: String? = null,
    var laserBeamX: Float = posX,
    var laserBeamY: Float = posY,
    var isFiringLaser: Boolean = false,
    var isDeconstructing: Boolean = false,
    var idleAngle: Float = 0f
)
