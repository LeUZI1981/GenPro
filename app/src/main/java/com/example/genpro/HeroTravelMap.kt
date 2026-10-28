package com.example.genpro

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.genpro.logic.RestManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.random.Random

private const val TRAVEL_MAP_SIZE = 51
private const val VIEW_SIZE = 7

private val CITY_TOWN_PREFIXES = listOf(
    "Eldor", "Sharo", "Arel", "Myth", "Froz", "Storm", "Iron", "Star", "Raven", "Whister",
    "Silver", "Dragon", "Nether", "Darn", "Gloom", "Thorn", "Rune", "Moon", "Blood", "Brom"
)
private val CITY_TOWN_SUFFIXES = listOf(
    "spin", "glen", "shire", "shord", "gard", "fell", "vale", "peak", "rog", "hood",
    "brook", "mere", "port", "bury", "myst", "watch", "reach", "crag", "ford", "haven"
)
private val VILLAGE_PREFIXES = listOf(
    "Łysa", "Kozi", "Wilcza", "Stara", "Zielony", "Mokry", "Cichy", "Niedźwiedzi", "Kamienny", "Czarny",
    "Kręty", "Borowy", "Ptasi", "Ślepy", "Jastrzębi", "Lipowy", "Dębowy", "Biały", "Krzywy", "Mały"
)
private val VILLAGE_SUFFIXES = listOf(
    "Góra", "Gaj", "Dół", "Łąka", "Staw", "Las", "Potok", "Polana", "Borek", "Róg",
    "Most", "Bród", "Wólka", "Huta", "Młyn", "Osada", "Zagroda", "Jar", "Trzęsawisko", "Kuźnia"
)

data class TravelMapExit(val x: Int, val y: Int)

object HeroTravelMapGenerator {
    fun fileName(heroId: Int): String = "hero_travel_map_$heroId.json"

    fun generate(heroId: Int): HeroTravelMap {
        val random = Random(heroId.toLong())
        val cells = MutableList(TRAVEL_MAP_SIZE * TRAVEL_MAP_SIZE) { index ->
            val x = index % TRAVEL_MAP_SIZE
            val y = index / TRAVEL_MAP_SIZE
            TravelMapCell(x, y, randomNatureBiome(random))
        }
        val settlements = mutableListOf<TravelSettlement>()
        val settlementTypes = List(3) { TravelSettlementType.CITY } +
            List(4) { TravelSettlementType.TOWN } +
            List(5) { TravelSettlementType.VILLAGE }

        settlementTypes.forEachIndexed { index, type ->
            var x = 0
            var y = 0
            var attempts = 0
            do {
                x = random.nextInt(3, TRAVEL_MAP_SIZE - 3)
                y = random.nextInt(3, TRAVEL_MAP_SIZE - 3)
                attempts++
            } while (
                attempts < 200 &&
                settlements.any {
                    val cityCell = cells.first { cell -> cell.cityId == it.id && !cell.isSettlementEntrance }
                    abs(cityCell.x - x) + abs(cityCell.y - y) < 6
                }
            )

            val id = "settlement_$index"
            val directions = listOf("Północ", "Wschód", "Południe", "Zachód")
                .shuffled(random)
                .take(type.gateCount)
                .toSet()
            val name = generateSettlementName(type, random) { candidate ->
                settlements.none { it.name == candidate }
            }
            settlements += TravelSettlement(id, name, type, directions)
            replaceCell(cells, TravelMapCell(x, y, TravelBiome.SETTLEMENT, id))
            directions.forEach { direction ->
                val (gateX, gateY) = when (direction) {
                    "Północ" -> x to y - 1
                    "Wschód" -> x + 1 to y
                    "Południe" -> x to y + 1
                    else -> x - 1 to y
                }
                replaceCell(
                    cells,
                    TravelMapCell(
                        x = gateX,
                        y = gateY,
                        biome = TravelBiome.SETTLEMENT,
                        cityId = id,
                        isSettlementEntrance = true
                    )
                )
            }
        }

        val roadCells = generateRoadNetwork(cells, settlements)
        val center = TRAVEL_MAP_SIZE / 2
        return HeroTravelMap(
            width = TRAVEL_MAP_SIZE,
            height = TRAVEL_MAP_SIZE,
            heroX = center,
            heroY = center,
            cameraX = center - 3,
            cameraY = center - 3,
            discoveredCells = setOf(key(center, center)),
            cells = cells,
            settlements = settlements,
            roadCells = roadCells
        )
    }

    private fun generateSettlementName(
        type: TravelSettlementType,
        random: Random,
        isUnique: (String) -> Boolean
    ): String {
        val (prefixes, suffixes) = if (type == TravelSettlementType.VILLAGE) {
            VILLAGE_PREFIXES to VILLAGE_SUFFIXES
        } else {
            CITY_TOWN_PREFIXES to CITY_TOWN_SUFFIXES
        }
        var attempts = 0
        var candidate: String
        do {
            candidate = prefixes.random(random) + suffixes.random(random)
            attempts++
        } while (!isUnique(candidate) && attempts < 100)
        return candidate
    }

    private fun randomNatureBiome(random: Random): TravelBiome = when (random.nextInt(100)) {
        in 0..29 -> TravelBiome.FOREST
        in 30..58 -> TravelBiome.MEADOW
        in 59..68 -> TravelBiome.LAKE
        in 69..78 -> TravelBiome.SWAMP
        in 79..88 -> TravelBiome.ROCKS
        in 89..94 -> TravelBiome.RIVER
        else -> TravelBiome.SPECJALNY
    }

    private fun replaceCell(cells: MutableList<TravelMapCell>, cell: TravelMapCell) {
        cells[cell.y * TRAVEL_MAP_SIZE + cell.x] = cell
    }

    private fun generateRoadNetwork(
        cells: List<TravelMapCell>,
        settlements: List<TravelSettlement>
    ): Set<String> {
        val entrances = settlements.associateWith { settlement ->
            cells.filter { it.cityId == settlement.id && it.isSettlementEntrance }
        }
        val connected = mutableSetOf(settlements.first())
        val roads = mutableSetOf<String>()

        while (connected.size < settlements.size) {
            val link = connected.flatMap { from ->
                settlements.filter { it !in connected }.mapNotNull { to ->
                    val pair = entrances.getValue(from).flatMap { start ->
                        entrances.getValue(to).map { end -> start to end }
                    }.minByOrNull { (start, end) ->
                        abs(start.x - end.x) + abs(start.y - end.y)
                    }
                    pair?.let { Triple(from, to, it) }
                }
            }.minByOrNull { (_, _, pair) ->
                abs(pair.first.x - pair.second.x) + abs(pair.first.y - pair.second.y)
            } ?: break

            val (_, destination, pair) = link
            roads += createRoadPath(pair.first.x to pair.first.y, pair.second.x to pair.second.y)
                .map { key(it.first, it.second) }
            connected += destination
        }

        // Zapewnij, że KAŻDA brama osady ma wychodzący trakt (wioska=2, miasteczko=3, miasto=4).
        val allEntrances = entrances.values.flatten()
        allEntrances.forEach { gate ->
            val gateKey = key(gate.x, gate.y)
            if (gateKey !in roads) {
                val nearestRoadPoint = roads
                    .map { cellKey -> cellKey.split(",").let { it[0].toInt() to it[1].toInt() } }
                    .minByOrNull { abs(it.first - gate.x) + abs(it.second - gate.y) }
                if (nearestRoadPoint != null) {
                    roads += createRoadPath(gate.x to gate.y, nearestRoadPoint)
                        .map { key(it.first, it.second) }
                }
            }
        }
        return roads
    }

    private fun createRoadPath(start: Pair<Int, Int>, end: Pair<Int, Int>): List<Pair<Int, Int>> {
        val path = mutableListOf(start)
        var x = start.first
        var y = start.second
        while (x != end.first) {
            x += if (end.first > x) 1 else -1
            path += x to y
        }
        while (y != end.second) {
            y += if (end.second > y) 1 else -1
            path += x to y
        }
        return path
    }
}

fun moveHeroToSettlementGate(
    heroId: Int,
    settlementName: String,
    exitDirection: String,
    readFromFile: (String) -> String,
    saveToFile: (String, String) -> Unit
): TravelMapExit? {
    val mapFile = HeroTravelMapGenerator.fileName(heroId)
    val map = runCatching {
        Json.decodeFromString<HeroTravelMap>(readFromFile(mapFile))
    }.getOrNull() ?: return null

    val settlement = map.settlements.firstOrNull { it.name == settlementName } ?: return null
    val settlementCenter = map.cells.firstOrNull {
        it.cityId == settlement.id && !it.isSettlementEntrance
    } ?: return null
    val gate = map.cells.firstOrNull {
        it.cityId == settlement.id &&
            it.isSettlementEntrance &&
            gateDirection(it, settlementCenter) == exitDirection
    } ?: return null

    val updatedMap = map.copy(
        heroX = gate.x,
        heroY = gate.y,
        cameraX = (gate.x - 2).coerceIn(0, map.width - VIEW_SIZE),
        cameraY = (gate.y - 2).coerceIn(0, map.height - VIEW_SIZE),
        discoveredCells = map.discoveredCells + key(gate.x, gate.y),
        previousHeroX = map.heroX,
        previousHeroY = map.heroY
    )
    saveToFile(mapFile, Json.encodeToString(updatedMap))
    return TravelMapExit(gate.x, gate.y)
}

/**
 * Próbuje wejść do osady na podstawie aktualnej pozycji bohatera na mapie podróży.
 * Zwraca zaktualizowany HeroProgress (z ustawionym currentCalendar wskazującym na osadę),
 * jeśli bohater aktualnie stoi na zewnętrznym polu wejściowym osady (biome SETTLEMENT + isSettlementEntrance).
 * Zwraca null, jeśli mapa podróży nie istnieje lub bohater nie stoi na takim polu.
 */
fun tryEnterSettlementFromCurrentPosition(
    context: android.content.Context,
    heroId: Int,
    heroProgress: HeroProgress,
    readFromFile: (String) -> String,
    saveToFile: (String, String) -> Unit
): HeroProgress? {
    val mapFile = HeroTravelMapGenerator.fileName(heroId)
    val map = runCatching {
        Json.decodeFromString<HeroTravelMap>(readFromFile(mapFile))
    }.getOrNull() ?: return null

    val currentCell = map.cells.firstOrNull { it.x == map.heroX && it.y == map.heroY } ?: return null
    if (currentCell.biome != TravelBiome.SETTLEMENT || !currentCell.isSettlementEntrance) return null

    val settlement = map.settlements.firstOrNull { it.id == currentCell.cityId } ?: return null
    val cityType = settlement.toSettlementType()
    val generatedSettlement = WorldPersistence.loadSettlement(context, settlement.name)
        ?: SettlementGenerator.generateSettlement(
            context = context,
            name = settlement.name,
            type = cityType,
            gateDirections = settlement.gateDirections
        )
    val cityCell = map.cells.firstOrNull { it.cityId == settlement.id && !it.isSettlementEntrance } ?: return null
    val direction = gateDirection(currentCell, cityCell)
    val calendarParts = heroProgress.currentCalendar.split("|").map { it.trim() }
    val date = calendarParts.getOrElse(1) { "1.1.2024" }
    val time = calendarParts.getOrElse(2) { "12:00" }
    val startingZone = SettlementGenerator.getStartingZoneName(generatedSettlement, direction)
    val calendar = "${settlement.name} | $date | $time | [EXTERNAL] | ${currentCell.x} | ${currentCell.y} | $startingZone | $direction | ${cityType.name}"
    val updatedProgress = heroProgress.copy(currentCalendar = calendar)
    saveToFile("progress_$heroId.json", Json.encodeToString(updatedProgress))
    return updatedProgress
}


@Composable
fun HeroTravelMapScreen(
    heroId: Int,
    heroProgress: HeroProgress,
    readFromFile: (String) -> String,
    saveToFile: (String, String) -> Unit,
    onHeroProgressChange: (HeroProgress) -> Unit,
    onEnterCity: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val mapFile = HeroTravelMapGenerator.fileName(heroId)
    var map by remember(heroId) {
        val saved = readFromFile(mapFile)
        mutableStateOf(
            runCatching { Json.decodeFromString<HeroTravelMap>(saved) }
                .getOrElse {
                    HeroTravelMapGenerator.generate(heroId).also {
                        saveToFile(mapFile, Json.encodeToString(it))
                    }
                }
        )
    }

    LaunchedEffect(map.settlements) {
        map.settlements.forEach { settlement ->
            if (!WorldPersistence.exists(context, settlement.name)) {
                SettlementGenerator.generateSettlement(
                    context = context,
                    name = settlement.name,
                    type = settlement.toSettlementType(),
                    gateDirections = settlement.gateDirections
                )
            }
        }
    }

    fun persist(updatedMap: HeroTravelMap, updatedProgress: HeroProgress = heroProgress) {
        map = updatedMap
        saveToFile(mapFile, Json.encodeToString(updatedMap))
        saveToFile("progress_$heroId.json", Json.encodeToString(updatedProgress))
        onHeroProgressChange(updatedProgress)
    }

    fun moveTo(cell: TravelMapCell) {
        if (abs(cell.x - map.heroX) + abs(cell.y - map.heroY) != 1) return
        
        // Sprawdzenie zmęczenia - jeśli >= 90% max, zablokuj ruch
        val maxFatigue = heroProgress.maxFatigue.coerceAtLeast(1)
        val currentFatigue = heroProgress.fatigue
        val fatiguePercent = (currentFatigue.toFloat() / maxFatigue) * 100
        
        if (fatiguePercent >= 90f) {
            Toast.makeText(context, "Jesteś zbyt zmęczony!", Toast.LENGTH_LONG).show()
            return
        }
        
        val currentCell = map.cells.first { it.x == map.heroX && it.y == map.heroY }
        val isCurrentDeadEnd = currentCell.biome == TravelBiome.LAKE || currentCell.biome == TravelBiome.RIVER
        val isOnRoad = key(cell.x, cell.y) in map.roadCells

        // Jeśli bohater jest na ślepym zaułku (Jezioro/Rzeka), pozwól tylko na cofnięcie się
        if (isCurrentDeadEnd && !isOnRoad) {
            val canGoBack = map.previousHeroX == cell.x && map.previousHeroY == cell.y
            if (!canGoBack) {
                Toast.makeText(context, "Dalsza droga niemożliwa! Zawróć.", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        val isDiscovered = key(cell.x, cell.y) in map.discoveredCells

        
        // Wybierz tempo automatycznie
        val pace = if (isDiscovered) TravelPace.NORMAL else TravelPace.CAUTIOUS
        
        // Oblicz wpływ podróży
        val travelResult = TravelMechanics.calculateTravelTile(
            heroData = readFromFile("hero_$heroId"),
            pace = pace,
            targetCell = cell,
            isDiscovered = isDiscovered,
            isOnRoad = isOnRoad
        )

        
        // Zastosuj podróż do postępu
        var updatedProgress = TravelMechanics.applyTravelToProgress(
            currentProgress = heroProgress,
            travelResult = travelResult,
            readFromFile = { readFromFile(it) }
        )

        // Jeśli kafelek jest nowo odkryty, generuj zasoby z biomu
        if (!isDiscovered) {
            val (category, itemName) = HeroLogic.generateTileDiscoveryResources(cell.biome) ?: Pair(null, null)
            if (category != null && itemName != null) {
                val resourceString = HeroLogic.generateDiscoveryResourceString(itemName)
                updatedProgress = HeroLogic.updateGatheredResources(updatedProgress, category, resourceString)
                val heroFile = "hero_$heroId"
                val heroData = readFromFile(heroFile)
                val updatedHeroData = HeroLogic.addItemToHero(
                    heroData,
                    TradeItem(
                        name = itemName,
                        price = HeroLogic.findItemPrice(itemName),
                        category = "Zbiory"
                    )
                )
                saveToFile(heroFile, updatedHeroData)
                Toast.makeText(context, "Zebrano: $itemName!", Toast.LENGTH_SHORT).show()
            }
        }
        
        var cameraX = map.cameraX
        var cameraY = map.cameraY
        if (cell.x == cameraX) cameraX = (cell.x - 1).coerceAtLeast(0)
        if (cell.x == cameraX + VIEW_SIZE - 1) cameraX = (cell.x - 3).coerceAtMost(map.width - VIEW_SIZE)
        if (cell.y == cameraY) cameraY = (cell.y - 1).coerceAtLeast(0)
        if (cell.y == cameraY + VIEW_SIZE - 1) cameraY = (cell.y - 3).coerceAtMost(map.height - VIEW_SIZE)

        // Zapamiętaj poprzednią pozycję
        persist(
            map.copy(
                heroX = cell.x,
                heroY = cell.y,
                cameraX = cameraX,
                cameraY = cameraY,
                discoveredCells = map.discoveredCells + key(cell.x, cell.y),
                previousHeroX = map.heroX,
                previousHeroY = map.heroY
            ),
            updatedProgress
        )
    }

    val currentCell = map.cells.first { it.x == map.heroX && it.y == map.heroY }
    val accessibleGate = currentCell.takeIf {
        it.biome == TravelBiome.SETTLEMENT && it.isSettlementEntrance
    }
    val calendarParts = heroProgress.currentCalendar.split("|").map { it.trim() }
    val time = calendarParts.getOrNull(2)?.takeIf { it.matches(Regex("\\d{2}:\\d{2}")) } ?: "12:00"
    val hour = time.substringBefore(":").toIntOrNull() ?: 12
    val dayPeriod = when (hour) {
        in 5..7 -> "Świt"
        in 8..11 -> "Rano"
        in 12..13 -> "Południe"
        in 14..17 -> "Popołudnie"
        in 18..21 -> "Wieczór"
        else -> "Noc"
    }
    val weatherName = calendarParts.getOrNull(4)
    val weather = HeroLogic.Weather.values().firstOrNull { it.name == weatherName }
        ?: HeroLogic.Weather.SUNNY
    val maxFatigue = heroProgress.maxFatigue.coerceAtLeast(0)
    val fatigue = heroProgress.fatigue.coerceIn(0, maxFatigue)
    val fatigueRatio = if (maxFatigue == 0) 0f else fatigue.toFloat() / maxFatigue

    val portraitUri = remember(heroId) {
        val heroData = readFromFile("hero_$heroId")
        heroData.lines().find { it.startsWith("Portrait:") }?.substringAfter("Portrait:")?.trim()
    }

    ///////////     WIDOK ALERTÓW
    var showCampMenu by remember { mutableStateOf(false) }
    var showRestPanel by remember { mutableStateOf(false) }
    var showBookPanel by remember { mutableStateOf(false) }
    var restHours by remember { mutableStateOf(8f) }

    val isInnOnRoad = remember(currentCell, heroId, map.roadCells) {
        if (key(currentCell.x, currentCell.y) !in map.roadCells) false
        else {
            val seed = currentCell.x.toLong() * 1000 + currentCell.y.toLong() + heroId.toLong()
            Random(seed).nextInt(100) < 15
        }
    }

    var activeInn by remember(currentCell) { mutableStateOf<BuildingInstance?>(null) }
    var activeRoom by remember { mutableStateOf<Room?>(null) }
    var showInnShop by remember { mutableStateOf(false) }
    var innBuyMode by remember { mutableStateOf(true) }
    var selectedInnShop by remember { mutableStateOf<Shop?>(null) }
    var innForcedCategory by remember { mutableStateOf<String?>(null) }
    var showInnRestPanel by remember { mutableStateOf(false) }
    var showInnWorkPanel by remember { mutableStateOf(false) }
    var innWorkHours by remember { mutableStateOf(1f) }
    val innInventories = remember { mutableStateMapOf<String, List<TradeItem>>() }
    var heroDataState by remember(heroId) { mutableStateOf(readFromFile("hero_$heroId")) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF202020), RoundedCornerShape(8.dp))
                        .padding(vertical = 7.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("GODZINA", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(time, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(dayPeriod, color = Color(0xFFDAA520), fontSize = 11.sp)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF202020), RoundedCornerShape(8.dp))
                        .padding(vertical = 7.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("POGODA", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(weather.icon, color = Color.White, fontSize = 20.sp)
                    Text(weather.displayName, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF202020), RoundedCornerShape(8.dp))
                        .padding(vertical = 7.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ZMĘCZENIE", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "$fatigue / $maxFatigue",
                        color = if (fatigueRatio >= 0.75f) Color(0xFFFF7043) else Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = { (fatigue.toFloat() / maxFatigue).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = if (fatigue > (maxFatigue * 0.7)) Color.Red else Color.Green
                    )
                }
            }
            Text(
                currentCell.biome.displayName,
                color = biomeColor(currentCell.biome),
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Komunikat ostrzeżenia dla ślepego zaułka
            val isDeadEndBiomeWarning = currentCell.biome == TravelBiome.LAKE || currentCell.biome == TravelBiome.RIVER
            val currentCellKeyWarning = key(currentCell.x, currentCell.y)
            val isDeadEndWarning = isDeadEndBiomeWarning && currentCellKeyWarning !in map.roadCells
            val message = ""
            if (isDeadEndWarning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF8B0000), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "⤦ Tylko droga powrotna!",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF8B0000), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$message",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Komunikat ostrzeżenia dla krytycznego zmęczenia
            val maxFatigueWarning = heroProgress.maxFatigue.coerceAtLeast(1)
            val currentFatigueWarning = heroProgress.fatigue
            val fatiguePercentWarning = (currentFatigueWarning.toFloat() / maxFatigueWarning) * 100
            /*
            if (fatiguePercentWarning >= 90f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFD32F2F), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "KRYTYCZNE ZMĘCZENIE!",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

             */

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(VIEW_SIZE) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(VIEW_SIZE) { column ->
                            val cell = map.cells.first {
                                it.x == map.cameraX + column && it.y == map.cameraY + row
                            }
                            val discovered = key(cell.x, cell.y) in map.discoveredCells
                            val isCurrent = cell.x == map.heroX && cell.y == map.heroY
                            val isNeighbor = abs(cell.x - map.heroX) + abs(cell.y - map.heroY) == 1
                            val currentCellBiome = currentCell.biome
                            val currentCellKey = key(currentCell.x, currentCell.y)
                            val isDeadEndBiome = currentCellBiome == TravelBiome.LAKE ||
                                                 currentCellBiome == TravelBiome.RIVER
                            val isDeadEnd = isDeadEndBiome && currentCellKey !in map.roadCells
                            val isBackRoute = isDeadEnd && cell.x == map.previousHeroX && cell.y == map.previousHeroY
                            val canClickCell = isNeighbor && (!isDeadEnd || isBackRoute)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(
                                        if (discovered) biomeColor(cell.biome) else Color.Black,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        if (isCurrent) 3.dp else 1.dp,
                                        if (isCurrent) Color.White else if (isNeighbor) Color.LightGray else Color.DarkGray,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable(enabled = canClickCell) { moveTo(cell) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (discovered && cell.biome != TravelBiome.SETTLEMENT) {
                                    biomeTileImage(cell)?.let { imageRes ->
                                        Image(
                                            painter = painterResource(imageRes),
                                            contentDescription = cell.biome.displayName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                    }
                                }
                                if (discovered && key(cell.x, cell.y) in map.roadCells) {
                                    RoadOverlay(cell = cell, roadCells = map.roadCells)
                                }
                                if (discovered && cell.biome == TravelBiome.SETTLEMENT) {
                                    settlementImageRes(map, cell)?.let { imageRes ->
                                        Image(
                                            painter = painterResource(imageRes),
                                            contentDescription = "Osada",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                    }
                                }
                                if (isCurrent) {
                                    if (portraitUri != null) {
                                        val bitmap = remember(portraitUri) {
                                            try {
                                                context.contentResolver.openInputStream(Uri.parse(portraitUri))?.use {
                                                    BitmapFactory.decodeStream(it)
                                                }
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Portret bohatera",
                                                modifier = Modifier
                                                    .fillMaxSize(0.65f)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(R.drawable.unknowhero),
                                                contentDescription = "Domyślny portret",
                                                modifier = Modifier
                                                    .fillMaxSize(0.65f)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    } else {
                                        Image(
                                            painter = painterResource(R.drawable.unknowhero),
                                            contentDescription = "Domyślny portret",
                                            modifier = Modifier
                                                .fillMaxSize(0.65f)
                                                .clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (accessibleGate != null) {
                Button(
                    onClick = {
                        val gate = accessibleGate
                        val settlement = map.settlements.first { it.id == gate.cityId }
                        val cityType = settlement.toSettlementType()
                        val generatedSettlement = WorldPersistence.loadSettlement(context, settlement.name)
                            ?: SettlementGenerator.generateSettlement(
                                context = context,
                                name = settlement.name,
                                type = cityType,
                                gateDirections = settlement.gateDirections
                            )
                        val cityCell = map.cells.first { it.cityId == settlement.id && !it.isSettlementEntrance }
                        val direction = gateDirection(gate, cityCell)
                        val calendarParts = heroProgress.currentCalendar.split("|").map { it.trim() }
                        val date = calendarParts.getOrElse(1) { "1.1.2024" }
                        val time = calendarParts.getOrElse(2) { "12:00" }
                        val startingZone = SettlementGenerator.getStartingZoneName(
                            generatedSettlement,
                            direction
                        )
                        val calendar = "${settlement.name} | $date | $time | [EXTERNAL] | ${gate.x} | ${gate.y} | $startingZone | $direction | ${cityType.name}"
                        val updatedProgress = heroProgress.copy(currentCalendar = calendar)
                        persist(
                            map.copy(
                                heroX = gate.x,
                                heroY = gate.y,
                                discoveredCells = map.discoveredCells + key(gate.x, gate.y)
                            ),
                            updatedProgress
                        )
                        onEnterCity()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDAA520)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("WEJDŹ DO MIASTA", fontWeight = FontWeight.Bold)
                }
            } else {
                val maxFatigue = heroProgress.maxFatigue.coerceAtLeast(1)
                val currentFatigue = heroProgress.fatigue
                val fatiguePercent = (currentFatigue.toFloat() / maxFatigue) * 100
                val isCriticalFatigue = fatiguePercent >= 90f
                
                GatheringActionsForBiome(
                    biome = currentCell.biome,
                    enabled = !isCriticalFatigue,
                    onGather = { action ->
                        val result = HeroLogic.performGathering(
                            context,
                            heroId,
                            action,
                            readFromFile,
                            saveToFile,
                            heroProgress,
                            onHeroProgressChange
                        )
                        Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                    }
                )
                
                if (isCriticalFatigue) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val buttonText = if (isInnOnRoad) "🍺 KARCZMA" else "⛺ OBÓZ"
                    Button(
                        onClick = {
                            if (isInnOnRoad) {
                                val seed = currentCell.x.toLong() * 1000 + currentCell.y.toLong() + heroId.toLong()
                                activeInn = BuildingGenerator.generateBuilding("Karczma", targetRank = 2, seed = seed)
                            } else {
                                showCampMenu = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Text("$buttonText (OBOWIĄZKOWE)", fontWeight = FontWeight.Bold)
                    }
                } else {
                    val buttonText = if (isInnOnRoad) "🍺 KARCZMA" else "⛺ OBÓZ"
                    Button(
                        onClick = {
                            if (isInnOnRoad) {
                                val seed = currentCell.x.toLong() * 1000 + currentCell.y.toLong() + heroId.toLong()
                                activeInn = BuildingGenerator.generateBuilding("Karczma", targetRank = 2, seed = seed)
                            } else {
                                showCampMenu = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isInnOnRoad) Color(0xFF8B4513) else Color(0xFF6D4C41)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Text(buttonText)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("POWRÓT DO KARTY")
            }
        }

        if (showCampMenu) {
            val maxFatigue = heroProgress.maxFatigue.coerceAtLeast(1)
            val currentFatigue = heroProgress.fatigue
            val fatiguePercent = (currentFatigue.toFloat() / maxFatigue) * 100
            val isCriticalFatigue = fatiguePercent >= 90f
            
            AlertDialog(
                onDismissRequest = { showCampMenu = false },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("Obóz", color = Color(0xFFDAA520), fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Button(
                            onClick = {
                                showCampMenu = false
                                restHours = 8f
                                showRestPanel = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) { Text("Odpoczynek") }
                        
                        if (!isCriticalFatigue) {
                            Button(
                                onClick = {
                                    showCampMenu = false
                                    showBookPanel = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) { Text("Czytanie książki") }
                        }
                        
                        Button(
                            onClick = {
                                showCampMenu = false
                                val result = HeroLogic.performCampCooking(
                                    heroId, readFromFile, saveToFile, heroProgress, onHeroProgressChange
                                )
                                Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) { Text("Gotowanie") }
                        
                        if (!isCriticalFatigue) {
                            Button(
                                onClick = {
                                    showCampMenu = false
                                    val result = HeroLogic.performTrapSetting(
                                        heroId, readFromFile, saveToFile, heroProgress, onHeroProgressChange
                                    )
                                    Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D4C41)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) { Text("Zastawienie pułapek") }
                        }
                        
                        if (isCriticalFatigue) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF8B0000), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Dostępne tylko: Odpoczynek i Gotowanie",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showCampMenu = false }) { Text("ANULUJ", color = Color.White) }
                }
            )
        }

        if (showRestPanel) {
            AlertDialog(
                onDismissRequest = { showRestPanel = false },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("Odpoczynek", color = Color(0xFF00A510), fontWeight = FontWeight.Bold) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ile czasu chcesz odpoczywać?", color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        val fileName = "hero_$heroId"
                        val heroData = readFromFile(fileName)
                        val lines = heroData.lines().toMutableList()
                        val maxFatigue = heroProgress.maxFatigue.coerceAtLeast(0)
                        val fatigue = heroProgress.fatigue.coerceIn(0, maxFatigue)

                        val minutes = (restHours.toInt() * 30) % 60
                        val hoursDisplay = (restHours.toInt() * 30) / 60
                        val timeDisplay = if (hoursDisplay > 0) {
                            if (minutes > 0) "$hoursDisplay : $minutes" else "$hoursDisplay : 00"
                        } else {
                            "$minutes minut"
                        }

                        // Oblicz Wytrzymałość = Odporność / 10, regeneracja = Wytrzymałość x 2
                        val stamina = (HeroLogic.getStatValue(heroData, "ODPORNOŚĆ") / 10).coerceAtLeast(1)
                        val baseRecovery = restHours * stamina
                        val itemBonus = restHours * (RestManager.getItemBonusForCamp(heroData) /2)
                        val fatigueRecovery = baseRecovery + itemBonus
                        val newFatigue = (fatigue - fatigueRecovery).toInt()
                        val fatigueRatio = if (maxFatigue == 0) 0f else (fatigue - fatigueRecovery).toFloat() / maxFatigue

                        Text(timeDisplay, color = Color.Cyan, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Text("$fatigue ➔ $newFatigue (${(fatigueRatio * 100).toInt()}%)", color = Color.Green, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        //  TESTY   Text("STAMINA: $stamina BR:$baseRecovery ItemBomus:$itemBonus FR:$fatigueRecovery", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = restHours,
                            onValueChange = { restHours = it },
                            valueRange = 1.5f..20f,
                            steps = 18,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF7B1FA2),
                                activeTrackColor = Color(0xFF00A510)
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showRestPanel = false
                        val hours = restHours * 0.5f
                        val result = HeroLogic.performCampRest(
                            heroId, hours.toInt(), readFromFile, saveToFile, heroProgress, onHeroProgressChange
                        )
                        Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                    }) { Text("ODPOCZNIJ", color = Color(0xFF00A510), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showRestPanel = false }) { Text("ANULUJ", color = Color.White) }
                }
            )
        }

        if (showBookPanel) {
            val heroData = readFromFile("hero_$heroId")
            val books = HeroLogic.getHeroBooks(heroData)
            AlertDialog(
                onDismissRequest = { showBookPanel = false },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("Czytanie książki", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        if (books.isEmpty()) {
                            Text("Nie posiadasz żadnej książki w ekwipunku.", color = Color.White)
                        } else {
                            books.forEach { book ->
                                Button(
                                    onClick = {
                                        showBookPanel = false
                                        val result = HeroLogic.performBookReading(
                                            heroId, book, readFromFile, saveToFile, heroProgress, onHeroProgressChange
                                        )
                                        Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                ) { Text(book) }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showBookPanel = false }) { Text("ZAMKNIJ", color = Color.White) }
                }
            )
        }

        if (activeInn != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(Color(0xFF1A1A1A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (activeRoom != null) {
                                    activeRoom = null
                                } else {
                                    activeInn = null
                                }
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = (activeRoom?.name ?: activeInn?.name ?: "KARCZMA").uppercase(),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (activeRoom != null) {
                            RoomView(
                                room = activeRoom!!,
                                building = activeInn!!,
                                heroData = heroDataState,
                                onAction = { action ->
                                    when (action) {
                                        "TRADE" -> {
                                            val b = activeInn!!
                                            selectedInnShop = HeroLogic.shops.find { shop ->
                                                b.type.contains(shop.name, ignoreCase = true) ||
                                                        shop.name.contains(b.type, ignoreCase = true)
                                            } ?: HeroLogic.shops.find { it.name.equals("Karczma", ignoreCase = true) }
                                            innForcedCategory = null
                                            showInnShop = true
                                        }
                                        "MEAL" -> {
                                            val b = activeInn!!
                                            selectedInnShop = HeroLogic.shops.find { shop ->
                                                b.type.contains(shop.name, ignoreCase = true) ||
                                                        shop.name.contains(b.type, ignoreCase = true)
                                            } ?: HeroLogic.shops.find { it.name.equals("Karczma", ignoreCase = true) }
                                            innForcedCategory = "Posiłki"
                                            showInnShop = true
                                        }
                                        "REST" -> showInnRestPanel = true
                                        "WORK" -> showInnWorkPanel = true
                                    }
                                }
                            )
                        } else {
                            BuildingView(
                                building = activeInn!!,
                                onEnterRoom = { activeRoom = it }
                            )
                        }
                    }
                }

                if (showInnShop) {
                    ShopDialog(
                        building = activeInn!!,
                        buyMode = innBuyMode,
                        onBuyModeChange = { innBuyMode = it },
                        selectedCategory = "Wszystkie",
                        onCategoryChange = { },
                        heroData = heroDataState,
                        onUpdateHero = {
                            heroDataState = it
                            saveToFile("hero_$heroId", it)
                        },
                        onDismiss = { showInnShop = false },
                        buildingInventories = innInventories,
                        selectedBuildingShop = selectedInnShop,
                        forcedCategory = innForcedCategory,
                        dataNow = heroProgress.currentCalendar,
                        heroProgress = heroProgress,
                        onProgressChange = { updatedProg ->
                            onHeroProgressChange(updatedProg)
                            saveToFile("progress_$heroId.json", Json.encodeToString(updatedProg))
                        }
                    )
                }

                if (showInnRestPanel) {
                    RestPanel(
                        building = activeInn!!,
                        heroData = heroDataState,
                        currentFatigue = heroProgress.fatigue,
                        onConfirm = { roomType, hours ->
                            val costPerHour = RestManager.getCostPerHour(roomType)
                            val cost = (costPerHour * hours).toInt()
                            val currentMoney = HeroLogic.calculateTotalCopper(heroDataState)
                            if (currentMoney >= cost) {
                                val updated = HeroLogic.deductMoney(heroDataState, cost)
                                val stamina = (HeroLogic.getStatValue(updated, "ODPORNOŚĆ") / 10).coerceAtLeast(1)
                                val baseReduction = (RestManager.getFatigueReductionPerHour(roomType, stamina) * hours).toInt()
                                val itemBonus = (RestManager.getItemBonusForRest(updated) * hours).toInt()
                                val reduction = baseReduction + itemBonus
                                val updatedProg = HeroLogic.updateHeroFatigue(heroProgress, -reduction)
                                val finalProg = HeroLogic.advanceTimeMinutesProgress(updatedProg, (hours * 60).toInt())
                                
                                onHeroProgressChange(finalProg)
                                saveToFile("progress_$heroId.json", Json.encodeToString(finalProg))
                                heroDataState = HeroLogic.stripLegacyProgressTags(updated)
                                saveToFile("hero_$heroId", heroDataState)
                                showInnRestPanel = false
                                Toast.makeText(context, "Odpoczęto. Koszt: $cost miedziaków.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Brak pieniędzy!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDismiss = { showInnRestPanel = false }
                    )
                }

                if (showInnWorkPanel) {
                    AlertDialog(
                        onDismissRequest = { showInnWorkPanel = false },
                        containerColor = Color.Black,
                        properties = DialogProperties(usePlatformDefaultWidth = false),
                        text = {
                            WorkPanel(
                                heroData = heroDataState,
                                buildingType = activeInn!!.type,
                                workHours = innWorkHours,
                                onHoursChange = { innWorkHours = it },
                                buildingRank = activeInn!!.rank,
                                heroProgress = heroProgress,
                                onWorkStart = { type, wage ->
                                    val updated = HeroLogic.addMoney(heroDataState, wage)
                                    val skillToTrain = when (type) {
                                        "POMOCNIK" -> if (HeroLogic.hasSkill(updated, "Górnictwo")) "Górnictwo" else "Siła"
                                        "MUZYKANT" -> "Muzykalność"
                                        "KUCHARZ" -> "Gotowanie"
                                        "MEDYK" -> "Medycyna"
                                        "STAJENNY" -> "Opieka nad zwierzętami"
                                        else -> "Siła"
                                    }
                                    val hoursInt = innWorkHours.toInt()
                                    val baseVal = wage / (hoursInt.coerceAtLeast(1) * 2)
                                    val (newHero, result) = HeroLogic.performWorkSessions(
                                        heroData = updated,
                                        skillName = skillToTrain,
                                        hours = hoursInt,
                                        baseWagePerHour = baseVal,
                                        statValue = HeroLogic.getStatValue(updated, if (type == "MUZYKANT") "O" else "K"),
                                        heroProgress = heroProgress
                                    )
                                    heroDataState = HeroLogic.stripLegacyProgressTags(newHero)
                                    saveToFile("hero_$heroId", heroDataState)
                                    result.updatedHeroProgress?.let {
                                        onHeroProgressChange(it)
                                        saveToFile("progress_$heroId.json", Json.encodeToString(it))
                                    }
                                    showInnWorkPanel = false
                                    Toast.makeText(context, "Zakończono pracę. Zarobek: ${HeroLogic.formatPrice(result.totalWage)}", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        confirmButton = { TextButton(onClick = { showInnWorkPanel = false }) { Text("ZAMKNIJ", color = Color.Gray) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoadOverlay(cell: TravelMapCell, roadCells: Set<String>) {
    val connections = buildSet {
        if (key(cell.x, cell.y - 1) in roadCells) add("N")
        if (key(cell.x + 1, cell.y) in roadCells) add("E")
        if (key(cell.x, cell.y + 1) in roadCells) add("S")
        if (key(cell.x - 1, cell.y) in roadCells) add("W")
    }
    if (connections.isEmpty()) return

    val (imageRes, rotation) = when {
        connections.size == 4 -> R.drawable.roadcross to 0f
        connections.size == 3 -> R.drawable.roadcrosst to tJunctionRotation(connections)
        connections.contains("N") && connections.contains("S") -> R.drawable.roadcrossi to 0f
        connections.contains("E") && connections.contains("W") -> R.drawable.roadcrossi to 90f
        connections.size >= 2 -> R.drawable.roadcrossne to cornerRotation(connections)
        else -> R.drawable.roadcrossi to if (connections.contains("N") || connections.contains("S")) 0f else 90f
    }
    Image(
        painter = painterResource(imageRes),
        contentDescription = "Trakt",
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxSize().rotate(rotation)
    )
}

private fun cornerRotation(connections: Set<String>): Float = when {
    "N" in connections && "E" in connections -> 0f
    "E" in connections && "S" in connections -> 90f
    "S" in connections && "W" in connections -> 180f
    else -> 270f
}

// Baza roadcrosst.png: ścieżki Zachód-Południe-Wschód (brak Północy) = 0°.
private fun tJunctionRotation(connections: Set<String>): Float = when {
    "N" !in connections -> 0f
    "E" !in connections -> 90f
    "S" !in connections -> 180f
    else -> 270f
}

@Composable
private fun GatheringActionsForBiome(biome: TravelBiome, enabled: Boolean = true, onGather: (GatheringAction) -> Unit) {
    val actions = when (biome) {
        TravelBiome.FOREST, TravelBiome.MEADOW -> listOf(
            GatheringAction("Zioła", "Zielarstwo", "INT", Color(0xFF43A047)),
            GatheringAction("Łowiectwo", "Łowiectwo", "INT", Color(0xFF8D6E63))
        )
        TravelBiome.LAKE -> listOf(
            GatheringAction("Rybołówstwo", "Sztuka przetrwania", "INT", Color(0xFF0255D1))
        )
        TravelBiome.SWAMP -> listOf(
            GatheringAction("Zioła", "Zielarstwo", "INT", Color(0xFF43A047)),
            GatheringAction("Grzyby", "Wiedza o grzybach", "INT", Color(0xFF556B2F))
        )
        TravelBiome.ROCKS -> listOf(
            GatheringAction("Minerały", "Metalurgia", "INT", Color(0xFF757575))
        )
        TravelBiome.RIVER -> listOf(
            GatheringAction("Rybołówstwo", "Sztuka przetrwania", "INT", Color(0xFF6288D1))
        )
        TravelBiome.SPECJALNY -> listOf(
            GatheringAction("Magia", "Magia prosta", "SW", Color(0xFF6233A1))
        )
        TravelBiome.SETTLEMENT -> emptyList()
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            Button(
                onClick = { onGather(action) },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = action.color),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 3.dp)
            ) {
                Text("${action.name.uppercase()}")  //było jeszcze  (2h)
            }
        }
    }
}

private fun key(x: Int, y: Int): String = "$x,$y"

private fun settlementImageRes(map: HeroTravelMap, cell: TravelMapCell): Int? {
    val settlement = map.settlements.firstOrNull { it.id == cell.cityId } ?: return null
    return when (settlement.type) {
        TravelSettlementType.VILLAGE -> R.drawable.wioska
        TravelSettlementType.TOWN -> R.drawable.miasteczko
        TravelSettlementType.CITY -> R.drawable.miasto
    }
}

private val FOREST_TILES = listOf(
    R.drawable.teren_las_1, R.drawable.teren_las_2, R.drawable.teren_las_3,
    R.drawable.teren_las_6, R.drawable.teren_las_7, R.drawable.teren_las_8,
    R.drawable.teren_las_4, R.drawable.teren_las_5, R.drawable.teren_las_9
)
private val MEADOW_TILES = listOf(
    R.drawable.teren_laka_1, R.drawable.teren_laka_2, R.drawable.teren_laka_3,
    R.drawable.teren_laka_6, R.drawable.teren_laka_7, R.drawable.teren_laka_8,
    R.drawable.teren_laka_4, R.drawable.teren_laka_5
)
private val LAKE_TILES = listOf(
    R.drawable.teren_jezioro_1, R.drawable.teren_jezioro_2, R.drawable.teren_jezioro_3,
    R.drawable.teren_jezioro_6, R.drawable.teren_jezioro_7, R.drawable.teren_jezioro_8,
    R.drawable.teren_jezioro_4, R.drawable.teren_jezioro_5
)
private val SWAMP_TILES = listOf(
    R.drawable.teren_bagno_1, R.drawable.teren_bagno_2, R.drawable.teren_bagno_3,
    R.drawable.teren_bagno_6, R.drawable.teren_bagno_7, R.drawable.teren_bagno_8,
    R.drawable.teren_bagno_4, R.drawable.teren_bagno_5
)
private val ROCKS_TILES = listOf(
    R.drawable.teren_skaly_1, R.drawable.teren_skaly_2, R.drawable.teren_skaly_3,
    R.drawable.teren_skaly_6, R.drawable.teren_skaly_7, R.drawable.teren_skaly_8,
    R.drawable.teren_skaly_4, R.drawable.teren_skaly_5
)
private val RIVER_TILES = listOf(
    R.drawable.teren_rzeka_1, R.drawable.teren_rzeka_2, R.drawable.teren_rzeka_3,
    R.drawable.teren_rzeka_4, R.drawable.teren_rzeka_5, R.drawable.teren_rzeka_6
)
private val SPECJALNY_TILES = listOf(R.drawable.teren_specjalny_portal)


/** Zwraca deterministyczny (stały dla danej komórki) obraz tekstury biomu, jeden z 5 wariantów. */
private fun biomeTileImage(cell: TravelMapCell): Int? {
    val variants = when (cell.biome) {
        TravelBiome.FOREST -> FOREST_TILES
        TravelBiome.MEADOW -> MEADOW_TILES
        TravelBiome.LAKE -> LAKE_TILES
        TravelBiome.SWAMP -> SWAMP_TILES
        TravelBiome.ROCKS -> ROCKS_TILES
        TravelBiome.RIVER -> RIVER_TILES
        TravelBiome.SPECJALNY -> SPECJALNY_TILES
        TravelBiome.SETTLEMENT -> return null
    }
    val index = Math.floorMod(cell.x * 31 + cell.y * 17, variants.size)
    return variants[index]
}

private fun biomeColor(biome: TravelBiome): Color = when (biome) {
    TravelBiome.FOREST -> Color(0xFF2E7D32)
    TravelBiome.MEADOW -> Color(0xFF8BC34A)
    TravelBiome.LAKE -> Color(0xFF1976D2)
    TravelBiome.SWAMP -> Color(0xFF105010)
    TravelBiome.ROCKS -> Color(0xAA616161)
    TravelBiome.RIVER -> Color(0xFF2196F3)
    TravelBiome.SPECJALNY -> Color(0x77FFFFFF)
    TravelBiome.SETTLEMENT -> Color(0x77FFFFFF)
}

private fun gateDirection(gate: TravelMapCell, city: TravelMapCell): String = when {
    gate.y < city.y -> "Północ"
    gate.x > city.x -> "Wschód"
    gate.y > city.y -> "Południe"
    else -> "Zachód"
}

private fun TravelSettlement.toSettlementType(): SettlementType = when (type) {
    TravelSettlementType.CITY -> SettlementType.CITY
    TravelSettlementType.TOWN -> SettlementType.TOWN
    TravelSettlementType.VILLAGE -> SettlementType.VILLAGE
}
