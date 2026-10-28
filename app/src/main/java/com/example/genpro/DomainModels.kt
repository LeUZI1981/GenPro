package com.example.genpro

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
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

@Serializable
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

@Serializable
enum class ZoneType(val displayName: String) {
    NATURA("Natura"),
    CYWILIZACJA("Cywilizacja")
}

@Serializable
enum class LocationTag(val tagName: String, val iconName: String) {
    FOREST("#las", "forest"),
    LAKE("#jezioro", "water"),
    RIVER("#rzeka", "water"),
    MEADOW("#łaka", "eco"),
    SWAMP("#bagno", "eco"),
    SPECJALNY("#specjalny", "map"),
    CITY("#miasto", "location_city"),
    PATH("#trakt", "map")
}

@Serializable
enum class RoadType(val displayName: String, val speedMultiplier: Float) {
    PATH("Ścieżka", 0.6f),
    ROAD("Trakt", 1.0f),
    HIGHWAY("Główny Szlak", 1.2f)
}

@Serializable
data class Road(
    val id: String,
    val startNodeId: Long,
    val endNodeId: Long,
    val type: RoadType,
    val distance: Float // Obliczany dystans lub stała wartość
)

@Serializable
enum class SettlementType(val displayName: String, val zoneCount: Int, val travelTimeMinutes: Int) {
    VILLAGE("Wieś", 1, 15),
    TOWN("Miasteczko", 5, 30),
    CITY("Miasto", 9, 60)
}

@Serializable
enum class DistrictType(val displayName: String, val description: String) {
    RULER_SEAT("Siedziba Władcy", "Pałac, ratusz, zamki i luksusowe posiadłości."),
    SACRED("Dzielnica Sakralna", "Katedry, klasztory lub kapliczki."),
    MAGIC("Dzielnica Magiczna", "Wieże magów, uniwersytety, biblioteki."),
    MERCHANT("Dzielnica Kupiecka", "Sukiennice, banki, domy handlowe."),
    CRAFT("Dzielnica Rzemieślnicza", "Warsztaty kowali, garbarzy i tkaczy."),
    RESIDENTIAL("Dzielnica Mieszkalna", "Kamienice i rzędy domów."),
    ECONOMIC("Dzielnica Gospodarcza", "Gospodarstwa, pola, sady i zabudowania hodowlane."),
    HARBOR("Dzielnica Portowa", "Doki, magazyny i tawerny portowe."),
    MILITARY("Dzielnica Wojskowa", "Koszary, arena i zbrojownie."),
    SLUMS("Slumsy", "Zniszczone chaty i kryjówki gildii.");

    val adjectivalName: String
        get() = when (this) {
            RULER_SEAT -> "siedziby władcy"
            SACRED -> "sakralnej"
            MAGIC -> "magicznej"
            MERCHANT -> "kupieckiej"
            CRAFT -> "rzemieślniczej"
            RESIDENTIAL -> "mieszkalnej"
            ECONOMIC -> "gospodarczej"
            HARBOR -> "portowej"
            MILITARY -> "wojskowej"
            SLUMS -> "slumsy"
        }
}

@Serializable
data class SettlementZone(
    val id: String,
    val name: String,
    val districtType: DistrictType,
    val inn: BuildingInstance,
    val specialBuilding: BuildingInstance,
    val extraBuildings: List<BuildingInstance> = emptyList(),
    val hasDocks: Boolean = false,
    val hasWell: Boolean = false,
    val hasFountain: Boolean = false,
    val neighbors: List<String> = emptyList()
)

@Serializable
data class SettlementInstance(
    val id: Long,
    val name: String,
    val type: SettlementType,
    val zones: List<SettlementZone>,
    val race: String = "Człowiek",
    val gateDirections: Set<String> = emptySet()
)

@Serializable
data class Book(
    val name: String,
    val skill: String,  // Format: "Wiedza o X"
    val price: Int,
    val quality: Int = 100
)

@Serializable
data class LibraryInventory(
    val borrowingBooks: List<Book> = emptyList(),
    val buyingBooks: List<Book> = emptyList(),
    val lastRefreshWeek: Int = -1
)

@Serializable
data class TravelNode(
    val id: Long,
    val name: String,
    val x: Float,
    val y: Float,
    val zone: ZoneType,
    val tags: List<LocationTag>,
    val description: String = "",
    val color: ULong = 0xFFFFFF00u,
    val settlementData: String = "", // Serializowany SettlementInstance
    val discovered: Boolean = false
)

data class GatheringAction(
    val name: String,
    val skillName: String,
    val attribute: String,
    val color: androidx.compose.ui.graphics.Color
)

@Serializable
data class HeroStay(
    val cityName: String,
    val buildingName: String,
    val expiryDateTime: String // D.M.Y HH:mm
)

@Serializable
data class HeroProperty(
    val cityName: String,
    val buildingName: String
)

@Serializable
data class HeroProgress(
    val heroId: String,
    val currentCalendar: String, // Format: City | D.M.Y | HH:mm | ...
    val travelState: String? = null,
    val visitedBuildings: Set<String> = emptySet(), // Format: "City|Building"
    val stays: List<HeroStay> = emptyList(),
    val properties: List<HeroProperty> = emptyList(),
    val fatigue: Int = 0,
    val maxFatigue: Int = 100,
    val wantedLevel: Int = 0,
    val gatheredResources: Map<String, List<String>> = emptyMap(), // Category -> List of "• Name (0.00 kg) (Quality)"
    val discoveredZones: Map<String, Set<String>> = emptyMap() // SettlementName -> Set of ZoneNames
)

@Serializable
enum class TravelBiome(val displayName: String) {
    FOREST("Las"),
    MEADOW("Łąka"),
    LAKE("Jezioro"),
    SWAMP("Bagno"),
    ROCKS("Skały"),
    RIVER("Rzeka"),
    SETTLEMENT("Osada"),
    SPECJALNY("Specjalny")
}

@Serializable
enum class TravelSettlementType(val displayName: String, val gateCount: Int) {
    CITY("Miasto", 4),
    TOWN("Miasteczko", 3),
    VILLAGE("Wioska", 2)
}

@Serializable
data class TravelMapCell(
    val x: Int,
    val y: Int,
    val biome: TravelBiome,
    val cityId: String? = null,
    val isSettlementEntrance: Boolean = false
)

@Serializable
data class TravelSettlement(
    val id: String,
    val name: String,
    val type: TravelSettlementType,
    val gateDirections: Set<String>
)

@Serializable
data class HeroTravelMap(
    val width: Int,
    val height: Int,
    val heroX: Int,
    val heroY: Int,
    val cameraX: Int,
    val cameraY: Int,
    val discoveredCells: Set<String>,
    val cells: List<TravelMapCell>,
    val settlements: List<TravelSettlement>,
    val roadCells: Set<String>,
    val previousHeroX: Int? = null,
    val previousHeroY: Int? = null
)
