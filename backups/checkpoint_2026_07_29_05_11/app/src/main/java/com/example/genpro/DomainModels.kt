package com.example.genpro

import androidx.compose.ui.graphics.Color

enum class BodyPart(val displayName: String, val affectedLocations: List<String>) {
    HEAD("Głowa", listOf("Głowa")),
    TORSO("Korpus", listOf("Korpus")),
    RIGHT_ARM("Prawa Ręka", listOf("Prawa Ręka", "Ramiona")),
    LEFT_ARM("Lewa Ręka", listOf("Lewa Ręka", "Ramiona")),
    RIGHT_LEG("Prawa Noga", listOf("Prawa Noga", "Nogi")),
    LEFT_LEG("Lewa Noga", listOf("Lewa Noga", "Nogi"));

    companion object {
        fun fromString(name: String): BodyPart? {
            return values().find { it.displayName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
        }
    }
}

enum class ItemCategory(val displayName: String) {
    MONEY("Monety"),
    CONTAINER("Pojemniki"),
    WEAPON_MELEE("Broń ręczna"),
    WEAPON_RANGED("Broń strzelecka"),
    AMMUNITION("Pociski"),
    ARMOR("Pancerz"),
    CLOTHING("Odzienie"),
    JEWELRY("Biżuteria"),
    INSTRUMENT("Instrument"),
    TOOL("Narzędzie"),
    BOOK("Księga"),
    ASSET("Majątek"),
    POTION("Mikstura"),
    ANIMAL("Zwierzęta")
}

enum class Currency(val displayName: String, val abbreviation: String, val copperValue: Int, val symbol: String = "⛀") {
    GOLD_CROWN("Złota korona", "zk", 480),
    SILVER_SHILLING("Srebrny szyling", "s", 12),
    COPPER_PENNY("Miedziany pens", "p", 1);

    companion object {
        fun fromLine(line: String): Currency? {
            return when {
                line.contains("Złota korona") || line.contains("(zk)") -> GOLD_CROWN
                line.contains("Srebrny szyling") || line.contains("(s)") -> SILVER_SHILLING
                line.contains("Miedziany pens") || line.contains("(p)") -> COPPER_PENNY
                else -> null
            }
        }
    }
}

// --- Nowy Schemat: Architektura Danych i Mapowanie (Fundament) ---

enum class ZoneType(val displayName: String) {
    NATURA("Natura"),
    CYWILIZACJA("Cywilizacja")
}

enum class LocationTag(val tagName: String, val iconName: String) {
    FOREST("#las", "forest"),
    LAKE("#jezioro", "water"),
    MEADOW("#łaka", "eco"),
    CITY("#miasto", "location_city"),
    PATH("#trakt", "map")
}

enum class RoadType(val displayName: String, val speedMultiplier: Float) {
    PATH("Ścieżka", 0.6f),
    ROAD("Trakt", 1.0f),
    HIGHWAY("Główny Szlak", 1.2f)
}

data class Road(
    val id: String,
    val startNodeId: Long,
    val endNodeId: Long,
    val type: RoadType,
    val distance: Float // Obliczany dystans lub stała wartość
)

data class TravelNode(
    val id: Long,
    val name: String,
    val x: Float,
    val y: Float,
    val zone: ZoneType,
    val tags: List<LocationTag>,
    val description: String = "",
    val color: ULong = 0xFFFFFF00u // Domyślnie żółty w formacie ULong dla Color
)
