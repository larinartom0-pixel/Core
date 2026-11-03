package com.example.model

enum class BuildingType(
    val id: String,
    val displayName: String,
    val width: Int,
    val height: Int,
    val costCopper: Int,
    val buildTimeSeconds: Float,
    val maxHealth: Int,
    val description: String
) {
    CORE(
        id = "core",
        displayName = "Ядро",
        width = 3,
        height = 3,
        costCopper = 0,
        buildTimeSeconds = 0f,
        maxHealth = 4000,
        description = "Головний штаб бази. Зберігає ресурси та координує дронів-будівельників."
    ),
    CONVEYOR(
        id = "conveyor",
        displayName = "Конвеєр",
        width = 1,
        height = 1,
        costCopper = 1,
        buildTimeSeconds = 0.5f,
        maxHealth = 80,
        description = "Транспортує видобуті ресурси у заданому напрямку до Ядра чи переробників."
    ),
    COPPER_DRILL(
        id = "copper_drill",
        displayName = "Мідний бур",
        width = 2,
        height = 2,
        costCopper = 25,
        buildTimeSeconds = 3.0f,
        maxHealth = 350,
        description = "Видобуває руду з підстилаючих жил (2x2) та вивантажує її на прилеглі конвеєри."
    );

    fun occupies(originX: Int, originY: Int, checkX: Int, checkY: Int): Boolean {
        return checkX in originX until (originX + width) && checkY in originY until (originY + height)
    }
}

data class BuildingInstance(
    val id: String,
    val type: BuildingType,
    val originX: Int,
    val originY: Int,
    var direction: Direction = Direction.NORTH,
    var health: Int = type.maxHealth,
    var buildProgress: Float = 1.0f, // 1.0 = fully built, 0.0 = just started
    var drillRotation: Float = 0f,
    var drillTimer: Float = 0f,
    var storedResources: MutableMap<Resource, Int> = mutableMapOf()
) {
    val isFullyBuilt: Boolean get() = buildProgress >= 0.999f

    fun occupies(checkX: Int, checkY: Int): Boolean {
        return type.occupies(originX, originY, checkX, checkY)
    }

    val centerX: Float get() = originX + type.width / 2f
    val centerY: Float get() = originY + type.height / 2f
}

data class Blueprint(
    val id: String,
    val type: BuildingType,
    val originX: Int,
    val originY: Int,
    val direction: Direction,
    val isValid: Boolean = true
) {
    fun occupies(checkX: Int, checkY: Int): Boolean {
        return type.occupies(originX, originY, checkX, checkY)
    }

    val costCopper: Int get() = type.costCopper
}
