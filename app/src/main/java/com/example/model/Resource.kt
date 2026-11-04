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
        description = "Базовий індустріальний ресурс для конвеєрів та споруд."
    ),
    LEAD(
        id = "lead",
        displayName = "Свинець",
        color = Color(0xFF8B5CF6),
        description = "Важкий метал, використовується для електроніки та захисних систем."
    ),
    COAL(
        id = "coal",
        displayName = "Вугілля",
        color = Color(0xFF334155),
        description = "Горюче паливо для виробництва енергії та металургії."
    ),
    SILVER(
        id = "silver",
        displayName = "Срібло",
        color = Color(0xFFE2E8F0),
        description = "Високопровідний благородний метал для передової логістики та перехресть."
    ),
    SCRAP(
        id = "scrap",
        displayName = "Брухт",
        color = Color(0xFF94A3B8),
        description = "Уламки металу, придатні для вторинної переробки."
    );
}
