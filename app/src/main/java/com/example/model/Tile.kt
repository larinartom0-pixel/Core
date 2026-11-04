package com.example.model

import androidx.compose.ui.graphics.Color

enum class TileType(
    val displayName: String,
    val isSolid: Boolean,
    val resourceDrop: Resource?,
    val baseColor: Color,
    val accentColor: Color
) {
    DIRT(
        displayName = "Грунт",
        isSolid = false,
        resourceDrop = null,
        baseColor = Color(0xFF1E2530),
        accentColor = Color(0xFF252E3C)
    ),
    STONE(
        displayName = "Скелястий пласт",
        isSolid = false,
        resourceDrop = null,
        baseColor = Color(0xFF283140),
        accentColor = Color(0xFF333E50)
    ),
    COPPER_ORE(
        displayName = "Мідна жила",
        isSolid = false,
        resourceDrop = Resource.COPPER,
        baseColor = Color(0xFF2E2C2A),
        accentColor = Color(0xFFD97706)
    ),
    LEAD_ORE(
        displayName = "Жила свинцю",
        isSolid = false,
        resourceDrop = Resource.LEAD,
        baseColor = Color(0xFF252230),
        accentColor = Color(0xFF8B5CF6)
    ),
    COAL_ORE(
        displayName = "Поклади вугілля",
        isSolid = false,
        resourceDrop = Resource.COAL,
        baseColor = Color(0xFF181B20),
        accentColor = Color(0xFF334155)
    ),
    SILVER_ORE(
        displayName = "Срібна жила",
        isSolid = false,
        resourceDrop = Resource.SILVER,
        baseColor = Color(0xFF252C36),
        accentColor = Color(0xFFE2E8F0)
    ),
    SCRAP_DEPOSIT(
        displayName = "Поклади брухту",
        isSolid = false,
        resourceDrop = Resource.SCRAP,
        baseColor = Color(0xFF29323D),
        accentColor = Color(0xFF64748B)
    ),
    ROCK_WALL(
        displayName = "Скелястий моноліт",
        isSolid = true,
        resourceDrop = null,
        baseColor = Color(0xFF121820),
        accentColor = Color(0xFF1C2430)
    );
}

data class Tile(
    val x: Int,
    val y: Int,
    val type: TileType,
    val variant: Int = (x * 31 + y * 17) and 0x7
)
