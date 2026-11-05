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
        description = "Важкий метал, використовується для електроніки, турелей та акумуляторів."
    ),
    COAL(
        id = "coal",
        displayName = "Вугілля",
        color = Color(0xFF475569),
        description = "Горюче паливо для виробництва електроенергії та виплавки заліза."
    ),
    SILVER(
        id = "silver",
        displayName = "Срібло",
        color = Color(0xFFE2E8F0),
        description = "Високопровідний благородний метал для передової логістики, енерговузлів та патронів."
    ),
    IRON_ORE(
        id = "iron_ore",
        displayName = "Залізна руда",
        color = Color(0xFFB45309),
        description = "Необроблена руда для переплавки у високоміцні залізні злитки."
    ),
    IRON_INGOT(
        id = "iron_ingot",
        displayName = "Залізний злиток",
        color = Color(0xFFCBD5E1),
        description = "Очищений метал для фабрикації передової зброї та складних заводів."
    ),
    COPPER_AMMO(
        id = "copper_ammo",
        displayName = "Мідні патрони",
        color = Color(0xFFFB923C),
        description = "Швидкострільні боєприпаси базового калібру для турелей."
    ),
    SILVER_AMMO(
        id = "silver_ammo",
        displayName = "Срібні патрони",
        color = Color(0xFF38BDF8),
        description = "Посилені енергетично-срібні боєприпаси з високим бронебійним уроном."
    ),
    SCRAP(
        id = "scrap",
        displayName = "Брухт",
        color = Color(0xFF94A3B8),
        description = "Уламки металу, придатні для вторинної переробки."
    );
}
