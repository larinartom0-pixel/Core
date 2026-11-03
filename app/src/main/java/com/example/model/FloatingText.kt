package com.example.model

import androidx.compose.ui.graphics.Color

data class FloatingText(
    val id: String,
    val text: String,
    var x: Float,
    var y: Float,
    val color: Color,
    var lifetime: Float = 1.2f, // seconds
    val maxLifetime: Float = 1.2f
) {
    val alpha: Float get() = (lifetime / maxLifetime).coerceIn(0f, 1f)
}

data class SparkParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var lifetime: Float = 0.4f,
    val maxLifetime: Float = 0.4f
) {
    val alpha: Float get() = (lifetime / maxLifetime).coerceIn(0f, 1f)
}
