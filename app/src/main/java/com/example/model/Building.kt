package com.example.model

enum class BuildingCategory(
    val id: String,
    val displayName: String
) {
    LOGISTICS("logistics", "Логістика"),
    MINING("mining", "Видобуток"),
    POWER("power", "Енергія"),
    INDUSTRY("industry", "Заводи"),
    DEFENSE("defense", "Оборона")
}

enum class BuildingType(
    val id: String,
    val displayName: String,
    val category: BuildingCategory,
    val width: Int,
    val height: Int,
    val costCopper: Int = 0,
    val costLead: Int = 0,
    val costSilver: Int = 0,
    val costIronIngot: Int = 0,
    val buildTimeSeconds: Float,
    val maxHealth: Int,
    val description: String
) {
    CORE(
        id = "core",
        displayName = "Ядро",
        category = BuildingCategory.LOGISTICS,
        width = 3,
        height = 3,
        costCopper = 0,
        costLead = 0,
        costSilver = 0,
        costIronIngot = 0,
        buildTimeSeconds = 0f,
        maxHealth = 4000,
        description = "Головний штаб бази. Зберігає ресурси, керує дронами та є ціллю ворогів."
    ),
    CONVEYOR(
        id = "conveyor",
        displayName = "Конвеєр",
        category = BuildingCategory.LOGISTICS,
        width = 1,
        height = 1,
        costCopper = 1,
        costLead = 0,
        costSilver = 0,
        costIronIngot = 0,
        buildTimeSeconds = 0.4f,
        maxHealth = 80,
        description = "Транспортує ресурси у вибраному напрямку до Ядра, генераторів або заводів."
    ),
    ROUTER(
        id = "router",
        displayName = "Маршрутизатор",
        category = BuildingCategory.LOGISTICS,
        width = 1,
        height = 1,
        costCopper = 8,
        costLead = 0,
        costSilver = 0,
        costIronIngot = 0,
        buildTimeSeconds = 0.6f,
        maxHealth = 120,
        description = "Приймає ресурси з будь-якого входу і рівномірно розподіляє між вільними виходами."
    ),
    JUNCTION(
        id = "junction",
        displayName = "Перехрестя",
        category = BuildingCategory.LOGISTICS,
        width = 1,
        height = 1,
        costCopper = 8,
        costLead = 0,
        costSilver = 3,
        costIronIngot = 0,
        buildTimeSeconds = 0.6f,
        maxHealth = 120,
        description = "Пропускає два перпендикулярні конвеєрні потоки без змішування ресурсів."
    ),
    COPPER_DRILL(
        id = "copper_drill",
        displayName = "Мідний бур",
        category = BuildingCategory.MINING,
        width = 2,
        height = 2,
        costCopper = 25,
        costLead = 0,
        costSilver = 0,
        costIronIngot = 0,
        buildTimeSeconds = 2.5f,
        maxHealth = 350,
        description = "Видобуває руду з жил 2x2 зі швидкістю 1.2 руди/сек при повному контакті."
    ),
    COAL_GENERATOR(
        id = "coal_generator",
        displayName = "Вугільний генератор",
        category = BuildingCategory.POWER,
        width = 1,
        height = 1,
        costCopper = 15,
        costLead = 30,
        costSilver = 0,
        costIronIngot = 0,
        buildTimeSeconds = 2.0f,
        maxHealth = 300,
        description = "Спалює 2 вугілля на секунду з власного сховища, виробляючи 20 енергії/сек."
    ),
    POWER_NODE(
        id = "power_node",
        displayName = "Енерговузол",
        category = BuildingCategory.POWER,
        width = 1,
        height = 1,
        costCopper = 12,
        costLead = 0,
        costSilver = 6,
        costIronIngot = 0,
        buildTimeSeconds = 1.0f,
        maxHealth = 140,
        description = "З'єднує лінії електромережі на відстань до 5 клітинок у 4 напрямках (↑←↓→)."
    ),
    BATTERY(
        id = "battery",
        displayName = "Акумулятор",
        category = BuildingCategory.POWER,
        width = 1,
        height = 1,
        costCopper = 30,
        costLead = 30,
        costSilver = 5,
        costIronIngot = 0,
        buildTimeSeconds = 1.8f,
        maxHealth = 250,
        description = "Накопичує до 200 одиниць надлишкової енергії та віддає її при дефіциті."
    ),
    TURRET(
        id = "turret",
        displayName = "Турель",
        category = BuildingCategory.DEFENSE,
        width = 2,
        height = 2,
        costCopper = 35,
        costLead = 40,
        costSilver = 10,
        costIronIngot = 0,
        buildTimeSeconds = 3.0f,
        maxHealth = 500,
        description = "Швидкострільна оборонна турель 2x2. Заряджається мідними (10 урону) або срібними (24 урону) патронами (до 30 шт.)."
    ),
    IRON_SMELTER(
        id = "iron_smelter",
        displayName = "Плавильня заліза",
        category = BuildingCategory.INDUSTRY,
        width = 2,
        height = 2,
        costCopper = 20,
        costLead = 45,
        costSilver = 15,
        costIronIngot = 0,
        buildTimeSeconds = 3.2f,
        maxHealth = 450,
        description = "Плавить залізні злитки. Тратить 3 залізні руди, 1 вугілля та 3 енергії в секунду."
    ),
    AMMO_FABRICATOR(
        id = "ammo_fabricator",
        displayName = "Конструктор патронів",
        category = BuildingCategory.INDUSTRY,
        width = 3,
        height = 3,
        costCopper = 50,
        costLead = 60,
        costSilver = 0,
        costIronIngot = 20,
        buildTimeSeconds = 4.0f,
        maxHealth = 650,
        description = "Виробляє мідні або срібні патрони на вибір. Потребує ресурси та електроенергію."
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
    var storedResources: MutableMap<Resource, Int> = mutableMapOf(),
    // Power properties
    var energyStored: Float = 0f,
    var isPowered: Boolean = true,
    // Turret properties
    var turretRotation: Float = 0f,
    var turretFireTimer: Float = 0f,
    var turretAmmoType: Resource? = null,
    var turretAmmoCount: Int = 0,
    // Crafting / Production properties
    var craftTimer: Float = 0f,
    var fabricatorRecipe: String = "copper" // "copper" or "silver"
) {
    val isFullyBuilt: Boolean get() = buildProgress >= 0.999f
    val healthRatio: Float get() = (health.toFloat() / type.maxHealth).coerceIn(0f, 1f)

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
    val costLead: Int get() = type.costLead
    val costSilver: Int get() = type.costSilver
    val costIronIngot: Int get() = type.costIronIngot
}
