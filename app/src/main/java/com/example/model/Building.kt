package com.example.model

enum class BuildingType(
    val id: String,
    val displayName: String,
    val width: Int,
    val height: Int,
    val costCopper: Int,
    val costSilver: Int = 0,
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
        costSilver = 0,
        buildTimeSeconds = 0f,
        maxHealth = 4000,
        description = "Головний штаб бази. Зберігає всі види ресурсів та керує дронами."
    ),
    CONVEYOR(
        id = "conveyor",
        displayName = "Конвеєр",
        width = 1,
        height = 1,
        costCopper = 1,
        costSilver = 0,
        buildTimeSeconds = 0.4f,
        maxHealth = 80,
        description = "Транспортує ресурси у вибраному напрямку до Ядра або будівель."
    ),
    ROUTER(
        id = "router",
        displayName = "Маршрутизатор",
        width = 1,
        height = 1,
        costCopper = 8,
        costSilver = 0,
        buildTimeSeconds = 0.6f,
        maxHealth = 120,
        description = "Приймає ресурси з будь-якого входу і рівномірно розподіляє між вільними виходами."
    ),
    JUNCTION(
        id = "junction",
        displayName = "Перехрестя",
        width = 1,
        height = 1,
        costCopper = 8,
        costSilver = 3,
        buildTimeSeconds = 0.6f,
        maxHealth = 120,
        description = "Дозволяє двом перпендикулярним конвеєрним лініям перетинатися без змішування ресурсів."
    ),
    COPPER_DRILL(
        id = "copper_drill",
        displayName = "Мідний бур",
        width = 2,
        height = 2,
        costCopper = 25,
        costSilver = 0,
        buildTimeSeconds = 2.5f,
        maxHealth = 350,
        description = "Видобуває руду з жил 2x2. При розміщенні на всіх 4 тайлах видає 1.2 руди/сек."
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
    var buildProgress: Float = 1.0f, // 1.0 = fully built
    var isMarkedForDeconstruction: Boolean = false,
    var deconstructProgress: Float = 0f, // 0.0 to 1.0
    var drillRotation: Float = 0f,
    var drillTimer: Float = 0f,
    var routerOutputIndex: Int = 0,
    var storedResources: MutableMap<Resource, Int> = mutableMapOf()
) {
    val isFullyBuilt: Boolean get() = buildProgress >= 0.999f && !isMarkedForDeconstruction

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
    val costSilver: Int get() = type.costSilver
}
