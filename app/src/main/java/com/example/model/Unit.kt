package com.example.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class UnitType(
    val id: String,
    val displayName: String,
    val maxHealth: Int,
    val moveSpeed: Float, // tiles per second
    val attackRange: Float, // in tiles
    val fireRateSeconds: Float, // seconds per attack
    val damagePerShot: Int,
    val isFlying: Boolean = false
) {
    TAL_MECH(
        id = "tal_mech",
        displayName = "Тал",
        maxHealth = 180,
        moveSpeed = 1.9f,
        attackRange = 4.2f,
        fireRateSeconds = 0.32f, // rapid fire
        damagePerShot = 4,
        isFlying = false
    )
}

data class UnitInstance(
    val id: String = UUID.randomUUID().toString(),
    val type: UnitType,
    var posX: Float,
    var posY: Float,
    var health: Int = type.maxHealth,
    var targetBuildingId: String? = null,
    var fireTimer: Float = 0f,
    var rotation: Float = 0f,
    var walkAnimTime: Float = 0f,
    var isFiring: Boolean = false,
    var isEnemy: Boolean = true
) {
    val isAlive: Boolean get() = health > 0
    val healthRatio: Float get() = (health.toFloat() / type.maxHealth).coerceIn(0f, 1f)
}

data class Projectile(
    val id: String = UUID.randomUUID().toString(),
    var posX: Float,
    var posY: Float,
    val targetX: Float,
    val targetY: Float,
    val speed: Float, // tiles per sec
    val damage: Int,
    val color: Color,
    val isEnemy: Boolean,
    val targetUnitId: String? = null,
    val targetBuildingId: String? = null,
    var lifetime: Float = 2.0f
) {
    var progress: Float = 0f
}

data class SmokeParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    var alpha: Float = 0.7f,
    var size: Float = 6f,
    var lifetime: Float = 1.2f
)
