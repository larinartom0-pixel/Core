package com.example.model

import androidx.compose.ui.graphics.Color

enum class Resource(
    val id: String,
    val displayName: String,
    val color: Color,
    val description: String
) {
    COPPER(
        id = "copper",
        displayName = "Мідь",
        color = Color(0xFFF59E0B),
        description = "Базовий індустріальний ресурс для будівництва та логістики"
    ),
    SCRAP(
        id = "scrap",
        displayName = "Брухт",
        color = Color(0xFF94A3B8),
        description = "Уламки металу, придатні для переробки"
    );
}
