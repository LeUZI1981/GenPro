package com.example.genpro

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.genpro.R.font
import com.example.genpro.logic.RestManager
//import com.example.genpro.HeroLogic.shops
import kotlinx.serialization.json.Json
import kotlin.random.Random

fun BuildingInstance.withCanonicalBuildingName(): BuildingInstance {
    return if (name.contains("magazyn", ignoreCase = true)) {
        copy(name = "Magazyn")
    } else {
        this
    }
}

// Extension to bridge SettlementZone with the expected 'buildings' property
val SettlementZone.buildings: List<BuildingInstance> get() {
    val base = listOf(inn, specialBuilding) + extraBuildings
    val extras = mutableListOf<BuildingInstance>()
    if (hasWell) extras.add(BuildingInstance("Studnia", "Studnia", "Ulica", 1, emptyList(), 10.0, null, "Kamienna studnia z czystą, chłodną wodą.", true, true))
    if (hasFountain) extras.add(BuildingInstance("Fontanna", "Miejska Fontanna", "Centrum", 1, emptyList(), 20.0, null, "Ozdobna fontanna stanowiąca dumę tutejszych mieszkańców.", true, true))
    return base + extras
}

@Composable
fun BuildingsScreen(
    heroDataInitial: String,
    heroProgressInitial: HeroProgress?,
    selectedCity: String,
    currentFile: String,
    dataNowInitial: String,
    initialShopType: String? = null,
    onBack: () -> Unit,
    onNavigate: (Int) -> Unit,
    playSoundAction: (Int, Int) -> Unit,
    saveToFile: (String, String) -> Unit,
    readFromFile: (String) -> String,
    selectedHeroId: Int,
    settlementData: String = "",
    onHeroProgressChange: (HeroProgress) -> Unit = {}
) {
    val nosiferFamily = remember { FontFamily(Font(font.nosifer)) }
    var heroData by remember { mutableStateOf(heroDataInitial) }
    var heroProgress by remember { mutableStateOf(heroProgressInitial) }
    val prog = heroProgress
    val heroId = selectedHeroId
    var dataNow by remember { mutableStateOf(dataNowInitial) }
    val context = LocalContext.current

    val settlement = remember(dataNow, settlementData, heroData) {
        val parts = dataNow.split("|").map { it.trim() }
        val sd = if (settlementData.isNotEmpty()) settlementData else (if (parts.size >= 4) parts[3] else "")
        if (sd == "[EXTERNAL]") {
            val cityName = parts.firstOrNull() ?: ""
            WorldPersistence.loadSettlement(context, cityName)
        } else if (sd.isNotEmpty() && !sd.startsWith("[")) {
            val cityName = parts.firstOrNull() ?: ""
            WorldPersistence.decodeSettlementOrNull(sd)
                ?: WorldPersistence.loadSettlement(context, cityName)
        } else {
            val cityName = parts.firstOrNull() ?: ""
            if (cityName.isNotEmpty() && cityName != "Nieznane Miejsce" && cityName != "Nieznane Miasto") {
                WorldPersistence.loadSettlement(context, cityName)
            } else {
                val dataNowStr = HeroLogic.getCalendarData(heroData)
                val p = dataNowStr.split("|").map { it.trim() }
                val cityName2 = p.firstOrNull() ?: ""
                WorldPersistence.loadSettlement(context, cityName2)
            }
        }
    }

    var generatedBuilding by remember { mutableStateOf<BuildingInstance?>(null) }

    var currentRoom by remember { mutableStateOf<Room?>(null) }
    
    var showShopOverlay by remember { mutableStateOf(false) }
    var selectedBuildingShop by remember { mutableStateOf<Shop?>(null) }
    var buyMode by remember { mutableStateOf(true) }
    var selectedCategoryFilter by remember { mutableStateOf("Wszystkie") }
    var forcedCategory by remember { mutableStateOf<String?>(null) }
    val buildingInventories = remember { mutableStateMapOf<String, List<TradeItem>>() }

    var showRestPanel by remember { mutableStateOf(false) }
    var showCoinTossDialog by remember { mutableStateOf(false) }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTalkDialog by remember { mutableStateOf(false) }
    var showDiscoveryDialog by remember { mutableStateOf(false) }
    var showWorkPanel by remember { mutableStateOf(false) }
    var showTrainingDialog by remember { mutableStateOf(false) }
    var workHours by remember { mutableStateOf(1f) }

    var showHaircutDialog by remember { mutableStateOf(false) }
    var selectedHairColor by remember { mutableStateOf("") }

    var selectedTrainingSkill by remember { mutableStateOf("Walka wręcz") }
    var selectedStudySkill by remember { mutableStateOf("Wiedza ogólna") }
    var showTeleportDialog by remember { mutableStateOf(false) }
    var selectedTeleportDestination by remember { mutableStateOf("") }
    var teleportFlash by remember { mutableStateOf(false) }

    var showLibraryDialog by remember { mutableStateOf(false) }
    var libraryInventory by remember { mutableStateOf<LibraryInventory?>(null) }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var libraryAction by remember { mutableStateOf<String?>(null) }  // "BUY", "BORROW", "SELL"
    var learningHours by remember { mutableStateOf(1f) }
    var ownedBooks by remember { mutableStateOf(emptyList<Book>()) }

    val portalDestinations = remember(settlement) {
        // Portal miejski nie służy do skakania między dzielnicami, tylko między miastami
        emptyList<String>()
    }

    val worldPortalDestinations = remember(selectedHeroId, settlement) {
        val travelDataStr = try { readFromFile("hero_map_pins_${selectedHeroId}_mapworld") } catch(e: Exception) { "" }
        val regex = Regex("""name=([^,]+)""")
        val pinNames = regex.findAll(travelDataStr).map { it.groupValues[1].trim() }.filter { it != selectedCity }.toList()
        
        // Filtrujemy tylko te miasta, które faktycznie posiadają Portal Miejski
        pinNames.filter { cityName ->
            val s = WorldPersistence.loadSettlement(context, cityName)
            s?.zones?.any { zone -> 
                zone.buildings.any { it.type.contains("Portal miejski", ignoreCase = true) }
            } == true
        }
    }

    val normalizeBuildingToken: (String) -> String = { value ->
        value.lowercase()
            .replace("miejska ", "")
            .replace("miejskie ", "")
            .replace("miejski ", "")
            .replace("\\s+".toRegex(), "")
            .replace("[^a-zżźćńółęąś]".toRegex(), "")
            .trim()
    }

    val matchesRequestedBuilding: (BuildingInstance, String?, String?) -> Boolean = { building, type, name ->
        val normalizedType = normalizeBuildingToken(type ?: "")
        val normalizedName = normalizeBuildingToken(name ?: "")
        val buildingType = normalizeBuildingToken(building.type)
        val buildingName = normalizeBuildingToken(building.name)

        val directTypeMatch = normalizedType.isNotEmpty() && (
            buildingType == normalizedType ||
                buildingType.contains(normalizedType) ||
                normalizedType.contains(buildingType)
        )
        val directNameMatch = normalizedName.isNotEmpty() && (
            buildingName == normalizedName ||
                buildingName.contains(normalizedName) ||
                normalizedName.contains(buildingName)
        )
        val softTypeNameMatch = normalizedType.isNotEmpty() && normalizedName.isNotEmpty() && (
            buildingType.contains(normalizedName) ||
                buildingName.contains(normalizedType) ||
                normalizedType.contains(buildingName) ||
                normalizedName.contains(buildingType)
        )

        val isWaterFeature = normalizedType.contains("studnia") || normalizedType.contains("fontanna") || normalizedName.contains("studnia") || normalizedName.contains("fontanna")
        val buildingIsWaterFeature = buildingType.contains("studnia") || buildingType.contains("fontanna") || buildingName.contains("studnia") || buildingName.contains("fontanna")

        val typeOrNameMatch = (type.isNullOrBlank() || directTypeMatch || directNameMatch)
        val nameOrTypeMatch = (name.isNullOrBlank() || directNameMatch || directTypeMatch || softTypeNameMatch)

        (typeOrNameMatch && nameOrTypeMatch) || (isWaterFeature && buildingIsWaterFeature) || softTypeNameMatch
    }

    LaunchedEffect(initialShopType, settlement) {
        if (initialShopType != null) {
            var match: BuildingInstance? = null
            
            if (settlement != null) {
                val allBuildings = settlement.zones.flatMap { it.buildings }

                match = if (initialShopType.startsWith("_DIRECT:")) {
                    val raw = initialShopType.removePrefix("_DIRECT:")
                    val parts = raw.split(":", limit = 2)
                    val type = parts.getOrNull(0)?.trim()
                    val name = parts.getOrNull(1)?.trim()
                    val directMatch = allBuildings.find { matchesRequestedBuilding(it, type, name) }
                    val fallbackByType = allBuildings.find { it.type.equals(type, ignoreCase = true) || it.name.contains(type ?: "", ignoreCase = true) }
                    val fallbackByName = allBuildings.find { it.name.equals(name, ignoreCase = true) || it.type.contains(name ?: "", ignoreCase = true) }
                    directMatch ?: fallbackByType ?: fallbackByName
                } else if (initialShopType.startsWith("_SPECIAL:")) {
                    val parts = initialShopType.removePrefix("_SPECIAL:").split(":", limit = 2)
                    val specialType = parts.getOrNull(0)?.trim() ?: ""
                    val name = parts.getOrNull(1)?.trim()
                    allBuildings.find { it.name.equals(name, ignoreCase = true) }
                        ?: allBuildings.find { it.type.equals(specialType, ignoreCase = true) || it.name.contains(specialType, ignoreCase = true) }
                } else {
                    allBuildings.find { it.type.contains(initialShopType, ignoreCase = true) }
                        ?: allBuildings.find { it.name.contains(initialShopType, ignoreCase = true) }
                }
            }
            
            // Fallback for _SPECIAL:MAGAZYN if not found in settlement
            if (match == null && initialShopType.startsWith("_SPECIAL:MAGAZYN:")) {
                val name = initialShopType.substringAfterLast(":")
                match = BuildingInstance(
                    type = "Magazyn",
                    name = name,
                    address = "Centrum",
                    rank = 1,
                    rooms = listOf(
                        Room(name = "Lada Magazynu", area = 50.0, isLocked = false, description = "Miejsce do sprzedaży przedmiotów")
                    ),
                    totalArea = 100.0,
                    description = "Miejski Magazyn Handlowy - miejsce do sprzedaży zgromadzonych przedmiotów.",
                    discovered = true,
                    isPublicInstitution = true
                )
            }

            if (match != null) {
                generatedBuilding = match.withCanonicalBuildingName()
                currentRoom = null
                
                // Mark visited when opening directly from map
                heroProgress?.let { hp ->
                    if (!HeroLogic.isBuildingVisited(hp, selectedCity, match.name)) {
                        val newProgress = HeroLogic.markBuildingVisited(hp, selectedCity, match.name)
                        heroProgress = newProgress
                        onHeroProgressChange(newProgress)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
                        if (currentRoom != null) {
                            currentRoom = null
                        } else {
                            onNavigate(35)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            currentRoom != null -> currentRoom!!.name.uppercase()
                            generatedBuilding != null -> generatedBuilding!!.name.uppercase()

                            else -> selectedCity.uppercase()
                        },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = nosiferFamily
                    )
                    
                    Text(dataNow.split("|").lastOrNull()?.trim() ?: "", color = Color.Gray, fontSize = 12.sp)
                }
            }

            // Main Content
            Box(modifier = Modifier.weight(1f)) {
                when {
                    currentRoom != null -> {
                        RoomView(
                            room = currentRoom!!,
                            building = generatedBuilding!!,
                            heroData = heroData,
                            onAction = { action ->
                                when (action) {
                                    "TRADE" -> {
                                        val b = generatedBuilding!!
                                        selectedBuildingShop = HeroLogic.shops.find { shop ->
                                            b.type.contains(shop.name, ignoreCase = true) || 
                                            shop.name.contains(b.type, ignoreCase = true)
                                        } ?: HeroLogic.shops.find { it.name.equals("Karczma", ignoreCase = true) }
                                        forcedCategory = null
                                        showShopOverlay = true
                                    }
                                    "MEAL" -> {
                                        val b = generatedBuilding!!
                                        selectedBuildingShop = HeroLogic.shops.find { shop ->
                                            b.type.contains(shop.name, ignoreCase = true) || 
                                            shop.name.contains(b.type, ignoreCase = true) ||
                                            (b.type.contains("HOTEL", ignoreCase = true) && shop.name.equals("Karczma", ignoreCase = true))
                                        } ?: HeroLogic.shops.find { it.name.equals("Karczma", ignoreCase = true) }
                                        forcedCategory = "Posiłki"
                                        showShopOverlay = true
                                    }
                                    "REST" -> showRestPanel = true
                                    "RENT" -> showRestPanel = true
                                    "STUDY" -> showLanguageDialog = true
                                    "PORTAL" -> showTeleportDialog = true
                                    "TALK" -> showTalkDialog = true
                                    "SELL" -> {
                                        buyMode = false
                                        val b = generatedBuilding!!
                                        selectedBuildingShop = if (b.type.equals("Magazyn", ignoreCase = true)) {
                                            Shop(b.name, "MAGAZYN", emptyList(), "Miejski Magazyn Handlowy")
                                        } else {
                                            HeroLogic.shops.find { it.name.equals("Magazyn", ignoreCase = true) }
                                                ?: Shop(b.name, "MAGAZYN", emptyList(), "Miejski Magazyn Handlowy")
                                        }
                                        forcedCategory = null
                                        showShopOverlay = true
                                    }
                                    "DISCOVER" -> showDiscoveryDialog = true
                                    "WORK" -> showWorkPanel = true
                                    "TRAINING" -> showTrainingDialog = true
                                    "HAIRCUT" -> showHaircutDialog = true
                                    "LIBRARY" -> {
                                        val currentWeek = HeroLogic.getCalendarData(heroData).split("|").getOrNull(4)?.trim()?.toIntOrNull() ?: 1
                                        val key = "library_${selectedCity}"
                                        val savedInventory = readFromFile(key)
                                        libraryInventory = if (savedInventory.isNotEmpty()) {
                                            try { Json.decodeFromString<LibraryInventory>(savedInventory) } catch (e: Exception) { null }
                                        } else null
                                        
                                        if (libraryInventory == null || libraryInventory!!.lastRefreshWeek != currentWeek) {
                                            val sType = settlement?.type ?: SettlementType.VILLAGE
                                            libraryInventory = BookManager.generateLibraryBooks(sType, currentWeek)
                                            saveToFile(key, Json.encodeToString(libraryInventory!!))
                                        }
                                        showLibraryDialog = true
                                    }
                                }
                            }
                        )
                    }
                    generatedBuilding != null -> {
                        BuildingView(
                            building = generatedBuilding!!,
                            onEnterRoom = { currentRoom = it },
                            onCoinToss = { showCoinTossDialog = true }
                        )
                    }

                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "BRAK BUDYNKU",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = nosiferFamily
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Żądany obiekt: ${initialShopType ?: "brak"}",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { onNavigate(35) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDAA520))
                                ) {
                                    Text("POWRÓT DO MIASTA")
                                }
                            }
                        }
                    }

                }
            }
        }

        // Overlays
        if (showShopOverlay && generatedBuilding != null) {
            ShopDialog(
                building = generatedBuilding!!,
                buyMode = buyMode,
                onBuyModeChange = { buyMode = it },
                selectedCategory = selectedCategoryFilter,
                onCategoryChange = { selectedCategoryFilter = it },
                heroData = heroData,
                onUpdateHero = {
                    heroData = it
                    saveToFile(currentFile, it)
                },
                onDismiss = { showShopOverlay = false },
                buildingInventories = buildingInventories,
                selectedBuildingShop = selectedBuildingShop,
                forcedCategory = forcedCategory,
                dataNow = dataNow,
                heroProgress = heroProgress,
                onProgressChange = { updatedProg ->
                    heroProgress = updatedProg
                    onHeroProgressChange(updatedProg)
                    saveToFile("progress_$heroId.json", Json.encodeToString(updatedProg))
                }
            )
        }

        if (showRestPanel && generatedBuilding != null) {
            val heroProgress = heroProgress ?: return@Box // Changed return scope to Box, or can just use empty block
            RestPanel(
                building = generatedBuilding!!,
                heroData = heroData,
                currentFatigue = heroProgress.fatigue,
                onConfirm = { roomType, hours ->
                    val costPerHour = RestManager.getCostPerHour(roomType)
                    val cost = (costPerHour * hours).toInt()
                    
                    val currentMoney = HeroLogic.calculateTotalCopper(heroData)
                    if (currentMoney >= cost) {
                        val updated = HeroLogic.deductMoney(heroData, cost)
                        
                        // Oblicz Wytrzymałość = Odporność / 10
                        val stamina = (HeroLogic.getStatValue(heroData, "ODPORNOŚĆ") / 10).coerceAtLeast(1)
                        
                        // Regeneracja zmęczenia + bonus z przedmiotów
                        val baseReduction = (RestManager.getFatigueReductionPerHour(roomType, stamina) * hours).toInt()
                        val itemBonus = (RestManager.getItemBonusForRest(heroData) * hours).toInt()
                        val reduction = baseReduction + itemBonus
                        val updatedProg = HeroLogic.updateHeroFatigue(heroProgress, -reduction)
                        onHeroProgressChange(updatedProg)
                        
                        // Czas mija
                        val finalProg = HeroLogic.advanceTimeMinutesProgress(updatedProg, (hours * 60).toInt())
                        onHeroProgressChange(finalProg)
                        
                        saveToFile("progress_$selectedHeroId.json", Json.encodeToString(finalProg))
                        dataNow = finalProg.currentCalendar
                        
                        heroData = HeroLogic.stripLegacyProgressTags(updated)
                        saveToFile(currentFile, heroData)
                        showRestPanel = false
                        val hoursDisplay = hours.toInt()
                        val minDisplay = ((hours % 1) * 60).toInt()
                        val timeStr = if (minDisplay > 0) "$hoursDisplay h $minDisplay min" else "$hoursDisplay h"
                        Toast.makeText(context, "Odpoczęto $timeStr w: $roomType. Koszt: $cost miedziaków.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Brak pieniędzy! Potrzeba $cost miedziaków.", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showRestPanel = false }
            )
        }

        if (showCoinTossDialog && generatedBuilding != null) {
            CoinTossDialog(
                building = generatedBuilding!!,
                heroData = heroData,
                onConfirm = { coinType ->
                    val (gold, silver, copper) = HeroLogic.parseMoney(heroData)
                    val updated = when (coinType) {
                        CoinType.GOLD -> HeroLogic.updateMoney(heroData, gold - 1, silver, copper)
                        CoinType.SILVER -> HeroLogic.updateMoney(heroData, gold, silver - 1, copper)
                        CoinType.COPPER -> HeroLogic.updateMoney(heroData, gold, silver, copper - 1)
                    }
                    heroData = updated
                    saveToFile(currentFile, updated)
                    showCoinTossDialog = false
                    Toast.makeText(
                        context,
                        "Wrzucono ${coinType.label.lowercase()} do ${generatedBuilding!!.name}.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onDismiss = { showCoinTossDialog = false }
            )
        }

        if (showTalkDialog) {
            val skillGadanina = HeroLogic.getSkillInfo(heroData, "Gadanina").value
            val skillUrok = HeroLogic.getSkillInfo(heroData, "Urok osobisty").value
            val charisma = HeroLogic.getHeroCharisma(heroData)
            val bestSkillValue = maxOf(skillGadanina, skillUrok, charisma)
            
            AlertDialog(
                onDismissRequest = { showTalkDialog = false },
                containerColor = Color(0xFF0D251A),
                title = { Text("ROZMOWA Z MIEJSCOWYMI", color = Color.Green, fontFamily = nosiferFamily) },
                text = {
                    Column {
                        Text("Próbujesz wyciągnąć informacje od napotkanych osób.", color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Szansa powodzenia: $bestSkillValue%", color = Color.Yellow)
                    }
                },
                confirmButton = {
                    CityActionButton(text = "ROZMAWIAJ", accentColor = Color.Green, modifier = Modifier.width(150.dp)) {
                        val roll = (1..100).random()
                        if (roll <= bestSkillValue) {
                            if (settlement != null) {
                                // 1. Try buildings in the CURRENT zone first, then others
                                val currentZone = null

                                val hiddenBuildings = if (currentZone != null) {
                                    val inCurrent = currentZone.buildings.filter { !it.discovered }.map { it to currentZone }
                                    if (inCurrent.isNotEmpty()) inCurrent else {
                                        settlement.zones.flatMap { z -> z.buildings.filter { !it.discovered }.map { it to z } }
                                    }
                                } else {
                                    settlement.zones.flatMap { z -> z.buildings.filter { !it.discovered }.map { it to z } }
                                }
                                
                                if (hiddenBuildings.isNotEmpty()) {
                                    val (b, zone) = hiddenBuildings.random()
                                    val updatedZones = settlement.zones.map { z ->
                                        if (z.id == zone.id) {
                                            z.copy(
                                                inn = if (z.inn.name == b.name) z.inn.copy(discovered = true) else z.inn,
                                                specialBuilding = if (z.specialBuilding.name == b.name) z.specialBuilding.copy(discovered = true) else z.specialBuilding,
                                                extraBuildings = z.extraBuildings.map { if (it.name == b.name) it.copy(discovered = true) else it }
                                            )
                                        } else z
                                    }
                                    
                                    val updatedSettlement = settlement.copy(zones = updatedZones)
                                    
                                    // Persistent save to file
                                    WorldPersistence.saveSettlement(context, updatedSettlement)

                                    // Clear inventory cache to force refresh if needed
                                    buildingInventories.remove(b.name)

                                    val calendar = HeroLogic.getCalendarData(heroData)
                                    val parts = calendar.split("|").map { it.trim() }.toMutableList()
                                    while (parts.size < 4) parts.add("")
                                    parts[3] = "[EXTERNAL]"
                                    val newCalendar = parts.joinToString(" | ")
                                    
                                    if (prog != null) {
                                        var updatedProg = HeroLogic.updateCalendarData(prog, newCalendar)
                                        updatedProg = HeroLogic.advanceTimeMinutesProgress(updatedProg, 30)
                                        onHeroProgressChange(updatedProg)
                                        saveToFile("progress_$heroId.json", Json.encodeToString(updatedProg))
                                        saveToFile("dataNow", updatedProg.currentCalendar)
                                        dataNow = updatedProg.currentCalendar
                                    }
                                    
                                    heroData = HeroLogic.stripLegacyProgressTags(heroData)
                                    saveToFile(currentFile, heroData)
                                    
                                    Toast.makeText(context, "Dowiedziałeś się o: ${b.name} (${zone.name})!", Toast.LENGTH_LONG).show()
                                } else {
                                    // 2. Try zones
                                    val discovered = HeroLogic.getDiscoveredZones(heroProgress, selectedCity)
                                    val hiddenZones = settlement.zones.filter { !discovered.contains(it.name) }
                                    
                                    if (hiddenZones.isNotEmpty()) {
                                        val z = hiddenZones.random()
                                    if (prog != null) {
                                        var updatedProg = HeroLogic.updateDiscoveredZones(prog, selectedCity, z.name)
                                        updatedProg = HeroLogic.advanceTimeMinutesProgress(updatedProg, 30)
                                        onHeroProgressChange(updatedProg)
                                        saveToFile("progress_$heroId.json", Json.encodeToString(updatedProg))
                                        dataNow = updatedProg.currentCalendar
                                    }
                                        heroData = HeroLogic.stripLegacyProgressTags(heroData)
                                        saveToFile(currentFile, heroData)
                                        Toast.makeText(context, "Ktoś opowiedział ci o dzielnicy: ${z.name}!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Wszyscy mówią to samo. Nic nowego.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                        if (prog != null) {
                                val updatedProg = HeroLogic.advanceTimeMinutesProgress(prog, 30)
                                onHeroProgressChange(updatedProg)
                                saveToFile("progress_$heroId.json", Json.encodeToString(updatedProg))
                                dataNow = updatedProg.currentCalendar
                            }
                            Toast.makeText(context, "Rozmowa nie przyniosła ciekawych informacji.", Toast.LENGTH_SHORT).show()
                        }
                        showTalkDialog = false
                    }
                },
                dismissButton = { TextButton(onClick = { showTalkDialog = false }) { Text("ANULUJ", color = Color.Gray) } }
            )
        }

        if (showDiscoveryDialog) {
            AlertDialog(
                onDismissRequest = { showDiscoveryDialog = false },
                containerColor = Color(0xFF251A0D),
                title = { Text("ODKRYWANIE OKOLICY", color = Color.Cyan, fontFamily = nosiferFamily) },
                text = { Text("Spędzasz czas na badaniu zakamarków dzielnicy w poszukiwaniu ukrytych miejsc. Koszt: 20 miedziaków.", color = Color.White) },
                confirmButton = {
                    CityActionButton(text = "EKSPLORUJ", accentColor = Color.Cyan, modifier = Modifier.width(150.dp)) {
                        val cost = 20
                        val currentMoney = HeroLogic.calculateTotalCopper(heroData)
                        if (currentMoney >= cost) {
                            val updated = HeroLogic.deductMoney(heroData, cost)
                            if (prog != null) {
                                var updatedProg = HeroLogic.advanceTimeMinutesProgress(prog, 180) // 3 hours
                                
                                // Discovery chance during exploration
                                val roll = Random.nextDouble()
                                val successChance = 0.4
                                
                                if (settlement != null && roll < successChance) {
                                    val currentZone = null

                                    val hidden = if (currentZone != null) {
                                        val inCurrent = currentZone.buildings.filter { !it.discovered }.map { it to currentZone }
                                        if (inCurrent.isNotEmpty()) inCurrent else {
                                            settlement.zones.flatMap { z -> z.buildings.filter { !it.discovered }.map { it to z } }
                                        }
                                    } else {
                                        settlement.zones.flatMap { z -> z.buildings.filter { !it.discovered }.map { it to z } }
                                    }

                                    if (hidden.isNotEmpty()) {
                                        val (b, zone) = hidden.random()
                                        val updatedZones = settlement.zones.map { z ->
                                            if (z.id == zone.id) {
                                                z.copy(
                                                    inn = if (z.inn.name == b.name) z.inn.copy(discovered = true) else z.inn,
                                                    specialBuilding = if (z.specialBuilding.name == b.name) z.specialBuilding.copy(discovered = true) else z.specialBuilding,
                                                    extraBuildings = z.extraBuildings.map { if (it.name == b.name) it.copy(discovered = true) else it }
                                                )
                                            } else z
                                        }
                                        val updatedSettlement = settlement.copy(zones = updatedZones)
                                        
                                        // Persistent save to file
                                        WorldPersistence.saveSettlement(context, updatedSettlement)

                                        // Clear inventory cache to force refresh if needed
                                        buildingInventories.remove(b.name)

                                        val calendar = updatedProg.currentCalendar
                                        val parts = calendar.split("|").map { it.trim() }.toMutableList()
                                        while (parts.size < 4) parts.add("")
                                        parts[3] = "[EXTERNAL]"
                                        val newCalendar = parts.joinToString(" | ")
                                        updatedProg = HeroLogic.updateCalendarData(updatedProg, newCalendar)
                                        
                                        Toast.makeText(context, "Znalazłeś: ${b.name}!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Przeszukałeś okolicę, ale nie znalazłeś nic nowego.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Przeszukałeś okolicę, ale nie znalazłeś nic nowego.", Toast.LENGTH_SHORT).show()
                                }
                                onHeroProgressChange(updatedProg)
                                saveToFile("progress_$heroId.json", Json.encodeToString(updatedProg))
                                dataNow = updatedProg.currentCalendar
                            }
                            
                            heroData = HeroLogic.stripLegacyProgressTags(updated)
                            saveToFile(currentFile, heroData)
                            showDiscoveryDialog = false
                        } else {
                            Toast.makeText(context, "Brak środków na prowiant.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                dismissButton = { TextButton(onClick = { showDiscoveryDialog = false }) { Text("ANULUJ", color = Color.Gray) } }
            )
        }

        if (showLanguageDialog) {
            // Simplified training dialog
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text("TRENING I NAUKA", color = Color.White) },
                text = { Text("Rozpocząć sesję treningową?", color = Color.White) },
                confirmButton = {
                    Button(onClick = {
                        val cost = 100
                        val currentMoney = HeroLogic.calculateTotalCopper(heroData)
                        if (currentMoney >= cost) {
                            val updated = HeroLogic.deductMoney(heroData, cost)
                            if (prog != null) {
                                val updatedProg = HeroLogic.advanceTimeMinutesProgress(prog, 120 * 60)
                                onHeroProgressChange(updatedProg)
                                saveToFile("progress_$heroId.json", Json.encodeToString(updatedProg))
                                dataNow = updatedProg.currentCalendar
                            }
                            heroData = HeroLogic.stripLegacyProgressTags(updated)
                            saveToFile(currentFile, heroData)
                            showLanguageDialog = false
                            Toast.makeText(context, "Trening zakończony.", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("TRENUJ") }
                }
            )
        }

        if (showWorkPanel && generatedBuilding != null) {
            AlertDialog(
                onDismissRequest = { showWorkPanel = false },
                containerColor = Color.Black,
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                text = {
                    WorkPanel(
                        heroData = heroData,
                        buildingType = generatedBuilding!!.type,
                        workHours = workHours,
                        onHoursChange = { workHours = it },
                        buildingRank = generatedBuilding!!.rank,
                        heroProgress = heroProgress,
                        onWorkStart = { type, wage ->
                            val updated = HeroLogic.addMoney(heroData, wage)
                            val skillToTrain = when (type) {
                                "POMOCNIK" -> if (HeroLogic.hasSkill(heroData, "Górnictwo")) "Górnictwo" else "Siła"
                                "MUZYKANT" -> "Muzykalność"
                                "KUCHARZ" -> "Gotowanie"
                                "MEDYK" -> "Medycyna"
                                "STAJENNY" -> "Opieka nad zwierzętami"
                                else -> if (type.startsWith("RZEMIEŚLNIK")) {
                                    val craftType = type.substringAfter("(").substringBefore(")")
                                    "Rzemiosło: $craftType"
                                } else "Siła"
                            }

                            val hoursInt = workHours.toInt()
                            val baseVal = wage / (hoursInt.coerceAtLeast(1) * 2) // orientacyjne wyliczenie baseWage

                            val (newHero, result) = HeroLogic.performWorkSessions(
                                heroData = updated,
                                skillName = skillToTrain,
                                hours = hoursInt,
                                baseWagePerHour = baseVal,
                                statValue = HeroLogic.getStatValue(updated, if (type == "MUZYKANT") "O" else "K"),
                                heroProgress = heroProgress
                            )

                            heroData = HeroLogic.stripLegacyProgressTags(newHero)
                            saveToFile(currentFile, heroData)

                            result.updatedHeroProgress?.let {
                                heroProgress = it
                                onHeroProgressChange(it)
                                saveToFile("progress_$heroId.json", Json.encodeToString(it))
                                dataNow = it.currentCalendar
                            }

                            showWorkPanel = false
                            Toast.makeText(context, "Zakończono pracę ($type). Zarobek: ${HeroLogic.formatPrice(result.totalWage)}", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                confirmButton = { TextButton(onClick = { showWorkPanel = false }) { Text("ZAMKNIJ", color = Color.Gray) } }
            )
        }

        if (showTrainingDialog && generatedBuilding != null) {
            val typeUpper = generatedBuilding!!.type.uppercase()
            val trainingSkills = when {
                typeUpper.contains("AKADEMIA WOJSKOWA") -> (HeroLogic.skillWW + HeroLogic.skillUS + HeroLogic.skillZrecznosc).toList().shuffled().take(5)
                typeUpper.contains("SZKOŁA MAGII") -> (HeroLogic.skillSW + HeroLogic.skillInteligencja).toList().shuffled().take(5)
                typeUpper.contains("UNIWERSYTET") || typeUpper.contains("BIBLIOTEKA") || typeUpper.contains("ŚWIĄTYNIA") || typeUpper.contains("KATEDRA") || typeUpper.contains("KLASZTOR") -> {
                    val sType = settlement?.type ?: SettlementType.VILLAGE
                    val bookCount = when(sType) {
                        SettlementType.CITY -> 5
                        SettlementType.TOWN -> 3
                        else -> 0
                    }
                    if (bookCount > 0) {
                        val list = HeroLogic.skillWiedza.toList().shuffled().take(bookCount).toMutableList()
                        if (!HeroLogic.hasSkill(heroData, "Czytanie i Pisanie")) {
                            list.add(0, "Czytanie i Pisanie")
                        }
                        list
                    } else {
                        emptyList()
                    }
                }
                else -> emptyList()
            }

            val isBookReading = typeUpper.contains("UNIWERSYTET") || typeUpper.contains("BIBLIOTEKA") || typeUpper.contains("ŚWIĄTYNIA") || typeUpper.contains("KATEDRA") || typeUpper.contains("KLASZTOR")

            AlertDialog(
                onDismissRequest = { showTrainingDialog = false },
                containerColor = Color(0xFF1A1A1A),
                title = { Text(if (isBookReading) "CZYTANIE KSIĄŻEK" else "SPECJALISTYCZNE SZKOLENIE", color = Color(0xFFD32F2F), fontFamily = nosiferFamily, fontSize = 16.sp) },
                text = {
                    Column {
                        Text("Koszt: 2 srebrne monety (200 miedziaków)", color = Color.White)
                        Text(if (isBookReading) "Czas: 1 godzina" else "Czas: 3 godziny", color = Color.White)
                        Text("Dostępne tylko w godzinach 06:05 - 18:30.", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (isBookReading && trainingSkills.isEmpty()) {
                            Text("Brak dostępnych książek w tej miejscowości.", color = Color.Red, fontSize = 14.sp)
                        }
                        trainingSkills.forEach { skill ->
                            var canTrain = true
                            var reason = ""
                            
                            if (isBookReading && skill != "Czytanie i Pisanie" && !HeroLogic.hasSkill(heroData, "Czytanie i Pisanie")) {
                                canTrain = false
                                reason = " (Wymaga znajomości Czytania)"
                            }
                            
                            if (skill == "Magia" && HeroLogic.getStatValue(heroData, "MAGIA") <= 0) {
                                canTrain = false
                                reason = " (Wymaga talentu Magia > 0)"
                            }

                            Button(
                                onClick = {
                                    val cost = 200
                                    val hours = if (isBookReading && skill != "Czytanie i Pisanie") 1 else 3
                                    if (!HeroLogic.isTrainingTime(heroData)) {
                                        Toast.makeText(context, "Teraz zamknięte! Zapraszamy 06:05-18:30.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (HeroLogic.calculateTotalCopper(heroData) < cost) {
                                        Toast.makeText(context, "Brak funduszy!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    
                                    if (skill == "Czytanie i Pisanie") {
                                        val updated = HeroLogic.addMoney(heroData, -cost)
                                        if (prog != null) {
                                            var updatedProg = HeroLogic.advanceTimeMinutesProgress(prog, hours * 60)
                                            updatedProg = HeroLogic.updateHeroFatigue(updatedProg, hours * 15)
                                            onHeroProgressChange(updatedProg)
                                            saveToFile("progress_$heroId.json", Json.encodeToString(updatedProg))
                                            dataNow = updatedProg.currentCalendar
                                        }
                                        heroData = HeroLogic.stripLegacyProgressTags(updated)
                                        saveToFile(currentFile, heroData)
                                        
                                        Toast.makeText(context, "Nauczyłeś się umiejętności Czytanie i Pisanie!", Toast.LENGTH_LONG).show()
                                        showTrainingDialog = false
                                        return@Button
                                    }

                                    val skillInfo = HeroLogic.getSkillInfo(heroData, skill)
                                    val statVal = skillInfo.value
                                    val result = HeroLogic.performTrainingSessions(
                                        heroData = heroData,
                                        skillName = skill,
                                        hours = hours,
                                        statValue = statVal,
                                        costPerHour = cost / hours,
                                        fatiguePerHour = 15
                                    )
                                    
                                    val updatedHero = result.first
                                    if (prog != null) {
                                        var updatedProg = HeroLogic.advanceTimeMinutesProgress(prog, hours * 60)
                                        updatedProg = HeroLogic.updateHeroFatigue(updatedProg, hours * 15)
                                        onHeroProgressChange(updatedProg)
                                        saveToFile("progress_$heroId.json", Json.encodeToString(updatedProg))
                                        dataNow = updatedProg.currentCalendar
                                    }
                                    heroData = HeroLogic.stripLegacyProgressTags(updatedHero)
                                    saveToFile(currentFile, heroData)
                                    
                                    Toast.makeText(context, result.second.message + " Zyskano ${result.second.totalPn} PN w $skill", Toast.LENGTH_LONG).show()
                                    showTrainingDialog = false
                                },
                                enabled = canTrain,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text("TRENUJ $skill$reason")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTrainingDialog = false }) {
                        Text("ZAMKNIJ", color = Color.Gray)
                    }
                }
            )
        }

        if (showTeleportDialog) {
            TeleportDialog(
                destinations = portalDestinations + worldPortalDestinations,
                worldDestinations = worldPortalDestinations,
                selectedDestination = selectedTeleportDestination,
                onSelectDestination = { selectedTeleportDestination = it },
                heroData = heroData,
                onConfirm = {
                    val isWorldTravel = worldPortalDestinations.contains(selectedTeleportDestination)
                    val weight = HeroLogic.calculateTotalWeight(heroData)
                    val distance = if (isWorldTravel) 50 else 5
                    val cost = (weight * distance * 2).toInt()
                    val currentMoney = HeroLogic.calculateTotalCopper(heroData)
                    
                    if (currentMoney >= cost) {
                        val updated = HeroLogic.deductMoney(heroData, cost)
                        if (prog != null) {
                            var updatedProg = HeroLogic.advanceTimeMinutesProgress(prog, 60)
                            
                            val calendar = updatedProg.currentCalendar
                            val parts = calendar.split("|").map { it.trim() }.toMutableList()
                            while (parts.size < 7) parts.add("")
                            
                            if (isWorldTravel) {
                                parts[0] = selectedTeleportDestination
                                parts[6] = "Centrum"
                            } else {
                                parts[6] = selectedTeleportDestination
                            }
                            
                            val newCalendar = parts.joinToString(" | ")
                            updatedProg = HeroLogic.updateCalendarData(updatedProg, newCalendar)
                            onHeroProgressChange(updatedProg)
                            saveToFile("progress_$heroId.json", Json.encodeToString(updatedProg))
                            dataNow = newCalendar
                            
                            heroData = HeroLogic.stripLegacyProgressTags(updated)
                            saveToFile(currentFile, heroData)
                            saveToFile("dataNow", updatedProg.currentCalendar)
                        }
                        showTeleportDialog = false
                        teleportFlash = true
                        playSoundAction(10, 1)
                        
                        generatedBuilding = null
                        currentRoom = null
                        
                        Toast.makeText(context, "Teleportacja do $selectedTeleportDestination zakończona.", Toast.LENGTH_LONG).show()
                        onNavigate(35)
                    } else {
                        Toast.makeText(context, "Brak funduszy!", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showTeleportDialog = false },
                nosiferFamily = nosiferFamily
            )
        }

        if (showHaircutDialog && generatedBuilding != null) {
            HaircutDialog(
                heroData = heroData,
                heroProgress = heroProgress,
                onUpdateHero = { updated ->
                    heroData = updated
                    saveToFile(currentFile, updated)
                },
                onDismiss = { showHaircutDialog = false },
                playSoundAction = playSoundAction,
                saveToFile = saveToFile,
                onProgressChange = { updatedProg ->
                    heroProgress = updatedProg
                    onHeroProgressChange(updatedProg)
                    saveToFile("progress_$heroId.json", Json.encodeToString(updatedProg))
                }
            )
        }

        if (showLibraryDialog && libraryInventory != null) {
            LibraryDialog(
                inventory = libraryInventory!!,
                heroData = heroData,
                heroProgress = heroProgress,
                settlementType = settlement?.type ?: SettlementType.VILLAGE,
                onBuyBook = { book, hours ->
                    val totalMoney = HeroLogic.calculateTotalCopper(heroData)
                    if (totalMoney >= book.price) {
                        heroData = HeroLogic.deductMoney(heroData, book.price)
                        ownedBooks = ownedBooks + book
                        saveToFile(currentFile, heroData)
                        Toast.makeText(context, "Zakupiono: ${book.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Nie masz wystarczająco funduszy!", Toast.LENGTH_SHORT).show()
                    }
                },
                onBorrowBook = { book, hours ->
                    val costPerHalfHour = when (settlement?.type) {
                        SettlementType.CITY -> 3
                        SettlementType.TOWN -> 5
                        else -> 0
                    }
                    val borrowCost = ((hours / 0.5f).toInt() * costPerHalfHour)
                    
                    heroData = HeroLogic.deductMoney(heroData, borrowCost)
                    saveToFile(currentFile, heroData)
                    
                    val intelligence = HeroLogic.getStatValue(heroData, "I")
                    val (updatedHero, rankIncrease) = BookManager.performBookLearning(heroData, book.skill, hours, intelligence)
                    heroData = updatedHero
                    saveToFile(currentFile, heroData)
                    
                    val timeAdvance = (hours * 60).toInt()
                    heroProgress?.let { hp ->
                        val newProgress = HeroLogic.advanceTimeMinutesProgress(hp, timeAdvance)
                        heroProgress = newProgress
                        onHeroProgressChange(newProgress)
                        saveToFile("progress_$heroId.json", Json.encodeToString(newProgress))
                        dataNow = newProgress.currentCalendar
                    }
                    
                    val message = when {
                        rankIncrease > 0 -> "Nauczono się ${book.skill}! Ranga ${rankIncrease} | Nauka: ${hours.toInt()}h | Koszt: ${borrowCost}p"
                        else -> "Przeczytano: ${book.name} | Nauka: ${hours.toInt()}h | +1PN | Koszt: ${borrowCost}p"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                },
                onSellBook = { book ->
                    val sellPrice = (book.price * 0.5).toInt()
                    heroData = HeroLogic.addMoney(heroData, sellPrice)
                    ownedBooks = ownedBooks - book
                    saveToFile(currentFile, heroData)
                    Toast.makeText(context, "Sprzedano za ${HeroLogic.formatPrice(sellPrice)}", Toast.LENGTH_SHORT).show()
                },
                ownedBooks = ownedBooks,
                onDismiss = { showLibraryDialog = false }
            )
        }
    }
}

@Composable
fun CityOverview(
    settlement: SettlementInstance?,
    heroData: String,
    selectedCity: String,
    heroProgress: HeroProgress?,
    onSelectZone: (SettlementZone) -> Unit
) {
    val discoveredZones = remember(heroData, selectedCity) {
        HeroLogic.getDiscoveredZones(heroProgress, selectedCity)
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("DZIELNICE MIASTA", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(settlement?.zones?.filter { discoveredZones.contains(it.name) } ?: emptyList()) { zone ->
            val discoveredBuildingsCount = zone.buildings.count { it.discovered }
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelectZone(zone) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(zone.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("$discoveredBuildingsCount budynków", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ZoneView(zone: SettlementZone, onEnterBuilding: (BuildingInstance) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("BUDYNKI W DZIELNICY: ${zone.name}", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(zone.buildings.filter { it.discovered }) { building ->
            BuildingStreetCard(building = building, visited = true, onClick = { onEnterBuilding(building) })
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun BuildingView(
    building: BuildingInstance,
    onEnterRoom: (Room) -> Unit,
    onCoinToss: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
    BuildingBackground(building)
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (!building.imageRes.isNullOrEmpty()) {
            AsyncImage(
                model = building.imageRes,
                contentDescription = building.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Text(building.description, color = Color.LightGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
        val canTossCoin = building.type.contains("Studnia", ignoreCase = true) ||
            building.type.contains("Fontanna", ignoreCase = true) ||
            building.name.contains("Studnia", ignoreCase = true) ||
            building.name.contains("Fontanna", ignoreCase = true)
        if (canTossCoin) {
            CityActionButton(
                text = "WRZUĆ MONETĘ",
                icon = "🪙",
                accentColor = Color(0xFFB8860B),
                onClick = onCoinToss
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text("POMIESZCZENIA", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(building.rooms) { room ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEnterRoom(room) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (room.isLocked) "🔒" else "🚪",
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(room.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (room.description.isNotEmpty()) {
                                        Text(room.description, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
            }
        }
    }
    }
}

fun buildingBackgroundRes(building: BuildingInstance): Int? {
    val t = "${building.type} ${building.name}".lowercase()
    return when {
        t.contains("speluna") || t.contains("melina") -> R.drawable.budynek000
        t.contains("tawerna") -> R.drawable.budynek001
        t.contains("ober?a") -> R.drawable.budynek002
        t.contains("restauracja") -> R.drawable.budynek004
        t.contains("karczma") || t.contains("gospoda") -> R.drawable.budynek003
        t.contains("zajazd") -> R.drawable.budynek005
        t.contains("hostel") -> R.drawable.budynek011
        t.contains("zamtuz") -> R.drawable.budynek017
        t.contains("motel") -> R.drawable.budynek006
        t.contains("hotel") -> R.drawable.budynek007
        t.contains("pa?ac") -> R.drawable.budynek008
        t.contains("studnia") && t.contains("wiejsk") -> R.drawable.budynek009
        t.contains("studnia") -> R.drawable.budynek010
        t.contains("fontanna") -> R.drawable.budynek012
        t.contains("spichlerz") -> R.drawable.budynek016
        t.contains("magazyn") -> R.drawable.budynek013
        t.contains("rymarz") -> R.drawable.rymarz
        t.contains("fryzjer") -> R.drawable.fryzjer
        t.contains("kowal") -> R.drawable.budynek014
        t.contains("p?atnerz") -> R.drawable.budynek015
        t.contains("biblioteka") -> R.drawable.budynek018
        t.contains("katedra") -> R.drawable.budynek019
        t.contains("kapliczka") -> R.drawable.budynek020
        t.contains("garnizon") -> R.drawable.budynek021
        t.contains("koloseum") -> R.drawable.budynek022
        else -> null
    }
}

@Composable
fun BuildingBackground(building: BuildingInstance) {
    val res = buildingBackgroundRes(building) ?: return
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
        alpha = 0.25f
    )
}

@Composable
fun RoomView(room: Room, building: BuildingInstance, heroData: String, onAction: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
    BuildingBackground(building)
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(getRoomContextDescription(room, building), color = Color.LightGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))
        
        val actions = mutableListOf<Pair<String, Color>>()
        
        // Refactored action logic based on keywords
        val roomUpper = room.name.uppercase()
        val typeUpper = building.type.uppercase()
        
        val isTradeRoom = roomUpper.contains("BAR") || roomUpper.contains("SZYNK") || 
                         roomUpper.contains("RECEPCJA") || roomUpper.contains("SPRZEDAŻ") ||
                         roomUpper.contains("SKLEP") || roomUpper.contains("LADA")
        
        val isTradeBuilding = typeUpper.contains("SKLEP") || typeUpper.contains("KOWAL") || 
                             typeUpper.contains("PŁATNERZ") || typeUpper.contains("RYMARZ") ||
                             typeUpper.contains("APTEKA") || typeUpper.contains("MEDYK")
        
        if (isTradeRoom || isTradeBuilding) {
            actions.add("HANDEL" to Color(0xFF2E7D32))
        }

        // Sell items for warehouse/storehouse
        if (typeUpper.contains("MAGAZYN")) {
            actions.add("SPRZEDAŻ" to Color(0xFFD32F2F))
        }
        
        if (typeUpper.contains("FRYZJER") && roomUpper.contains("SALA GŁÓWNA")) {
            actions.add("FRYZURA" to Color(0xFFE6B800))
        }
        
        if (roomUpper.contains("POKÓJ") || roomUpper.contains("SALA VIP") || roomUpper.contains("DORMITORIUM") || roomUpper.contains("SYPIALNIA")) {
            actions.add("ODPOCZYNEK" to Color(0xFF7B1FA2))
        }
        
        if (typeUpper.contains("MAGA") || typeUpper.contains("ŚWIĄTYNIA") || typeUpper.contains("PORTAL") || typeUpper.contains("KATEDRA")) {
            if (roomUpper.contains("TRANZYT") || roomUpper.contains("KRYSZTAŁ") || roomUpper.contains("NAWA") || roomUpper.contains("LABORATORIUM")) {
                actions.add("PORTAL" to Color(0xFF1E88E5))
            }
        }
        
        val isPublicRoom = roomUpper.contains("SALA GŁÓWNA") || roomUpper.contains("BAR") || 
                          roomUpper.contains("RYNEK") || roomUpper.contains("PLAC") ||
                          roomUpper.contains("LOBBY") || roomUpper.contains("SZYNK") ||
                          roomUpper.contains("NAWA") || roomUpper.contains("KORYTARZ")
        
        if (isPublicRoom || typeUpper.contains("MAGAZYN")) {
            actions.add("ROZMOWA" to Color(0xFF00796B))
        }

        val isInnOrHotel = typeUpper.contains("KARCZMA") || typeUpper.contains("TAWERNA") || 
                           typeUpper.contains("GOSPODA") || typeUpper.contains("HOTEL") || 
                           typeUpper.contains("MOTEL") || typeUpper.contains("ZAJAZD")
        
        val isServiceRoom = roomUpper.contains("SALA GŁÓWNA") || roomUpper.contains("RESTAURACJA") || 
                            roomUpper.contains("LOBBY") || roomUpper.contains("RECEPCJA") || 
                            roomUpper.contains("SZYNK") || roomUpper.contains("BAR")

        if (isInnOrHotel && isServiceRoom) {
            actions.add("POSIŁEK" to Color(0xFFE64A19))
            actions.add("ODPOCZYNEK" to Color(0xFF7B1FA2))
        }

        if (roomUpper.contains("SALA TRENINGOWA") || roomUpper.contains("SALA WYKŁADOWA")) {
            if (typeUpper.contains("AKADEMIA WOJSKOWA") || typeUpper.contains("SZKOŁA MAGII") || typeUpper.contains("UNIWERSYTET") || typeUpper.contains("ŚWIĄTYNIA") || typeUpper.contains("KATEDRA") || typeUpper.contains("KLASZTOR")) {
                actions.add("SZKOLENIE" to Color(0xFFD32F2F))
            }
        }

        // Library reading room - books
        if (roomUpper.contains("CZYTELNIA")) {
            if (typeUpper.contains("BIBLIOTEKA")) {
                actions.add("KSIĄŻKI" to Color(0xFF1565C0))
            }
        }
        
        val keyName = "Klucz ${building.name} ${room.name}"
        val hasKey = heroData.contains(keyName)

        if (room.isLocked && !hasKey) {
            actions.clear()
            actions.add("OTWÓRZ (WYTRYCHY)" to Color(0xFF607D8B))
        }

        actions.forEach { (label, color) ->
            val icon = when {
                label.contains("OTWÓRZ") -> "🔒"
                label == "HANDEL" -> "💰"
                label == "SPRZEDAŻ" -> "💎"
                label == "ODPOCZYNEK" -> "🛌"
                label == "POSIŁEK" -> "🍲"
                label == "PORTAL" -> "🌀"
                label == "ROZMOWA" -> "🗣️"
                label == "FRYZURA" -> "💇"
                label == "SZKOLENIE" -> "🎓"
                label == "KSIĄŻKI" -> "📚"
                else -> ""
            }
            CityActionButton(text = label, icon = icon, accentColor = color) {
                when {
                    label.contains("OTWÓRZ") -> onAction("LOCKPICK")
                    label == "HANDEL" -> onAction("TRADE")
                    label == "SPRZEDAŻ" -> onAction("SELL")
                    label == "FRYZURA" -> onAction("HAIRCUT")
                    label == "ODPOCZYNEK" -> onAction("REST")
                    label == "PORTAL" -> onAction("PORTAL")
                    label == "ROZMOWA" -> onAction("TALK")
                    label == "POSIŁEK" -> onAction("MEAL")
                    label == "SZKOLENIE" -> onAction("TRAINING")
                    label == "KSIĄŻKI" -> onAction("LIBRARY")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Dynamic "WORK" action for specific rooms/buildings
        val isWorkBuilding = typeUpper.contains("KARCZMA") || typeUpper.contains("TAWERNA") || 
                             typeUpper.contains("MEDYK") || typeUpper.contains("KOWAL") ||
                             typeUpper.contains("PŁATNERZ") || typeUpper.contains("RYMARZ")
        
        val isMainWorkRoom = roomUpper.contains("SALA GŁÓWNA") || roomUpper.contains("GABINET") || 
                             roomUpper.contains("WARSZTAT") || roomUpper.contains("ZAPLECZE")

        if (isWorkBuilding && isMainWorkRoom) {
             CityActionButton(text = "PRACA", icon = "⚒️", accentColor = Color.Yellow) {
                 onAction("WORK")
             }
        }
    }
    }
}

@Composable
fun BuildingStreetCard(building: BuildingInstance, visited: Boolean, onClick: () -> Unit) {
    val accentColor = when {
        building.type.contains("KARCZMA", true) || building.type.contains("TAWERNA", true) -> Color(0xFF5D4037)
        building.type.contains("KOWAL", true) -> Color(0xFFDAA520)
        building.type.contains("ZAMTUZ", true) -> Color(0xFF880E4F)
        building.type.contains("MAGA", true) || building.type.contains("CZARODZIEJ", true) || building.type.contains("MAGICZNA", true) -> Color(0xFF1565C0)
        building.type.contains("KATEDRA", true) || building.type.contains("ŚWIĄTYNIA", true) || building.type.contains("KLASZTOR", true) || building.type.contains("BAZYLIKA", true) || building.type.contains("OPACTWO", true) || building.type.contains("ZAKON", true) -> Color(0xFFFFD700)
        building.type.contains("KOSZARY", true) || building.type.contains("AKADEMIA", true) || building.type.contains("GARNIZON", true) || building.type.contains("ZBROJOWNIA", true) || building.type.contains("ARENA", true) -> Color(0xFFB71C1C)
        building.type.contains("RATUSZ", true) || building.type.contains("PAŁAC", true) || building.type.contains("ZAMEK", true) || building.type.contains("DOM HANDLOWY", true) -> Color(0xFF004D40)
        else -> Color(0xFF424242)
    }

    val iconChar = when {
        building.type.contains("KATEDRA", true) || building.type.contains("BAZYLIKA", true) -> "⛪"
        building.type.contains("ŚWIĄTYNIA", true) -> "⛩️"
        building.type.contains("KOSZARY", true) || building.type.contains("GARNIZON", true) -> "🏰"
        building.type.contains("AKADEMIA", true) -> "⚔️"
        building.type.contains("MAGA", true) || building.type.contains("CZARODZIEJ", true) -> "🔮"
        building.type.contains("KARCZMA", true) || building.type.contains("TAWERNA", true) -> "🍺"
        building.type.contains("KOWAL", true) -> "⚒️"
        building.type.contains("RATUSZ", true) || building.type.contains("PAŁAC", true) -> "🏛️"
        building.type.contains("BANK", true) || building.type.contains("SKARBIEC", true) -> "💰"
        building.type.contains("ZAMTUZ", true) -> "👠"
        building.type.contains("ARENA", true) -> "🏟️"
        building.type.contains("RYMARZ", true) -> "🎒"
        building.type.contains("APTEKA", true) || building.type.contains("MEDYK", true) -> "🧪"
        else -> building.type.take(1)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                Text(iconChar, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(building.name, color = Color.White, fontWeight = FontWeight.Bold)
                Text(building.type, color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun CityActionButton(
    text: String,
    icon: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon.isNotEmpty()) {
                Text(icon, modifier = Modifier.padding(end = 8.dp))
            }
            Text(text, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }

}

enum class CoinType(val label: String) {
    GOLD("złotą monetę"),
    SILVER("srebrną monetę"),
    COPPER("miedzianą monetę")
}

@Composable
fun CoinTossDialog(
    building: BuildingInstance,
    heroData: String,
    onConfirm: (CoinType) -> Unit,
    onDismiss: () -> Unit
) {
    val (gold, silver, copper) = HeroLogic.parseMoney(heroData)
    var selectedCoin by remember { mutableStateOf<CoinType?>(null) }
    val coinCounts = mapOf(
        CoinType.GOLD to gold,
        CoinType.SILVER to silver,
        CoinType.COPPER to copper
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("WRZUĆ MONETĘ", color = Color(0xFFFFD700)) },
        text = {
            Column {
                Text(
                    "Wybierz monetę, którą chcesz wrzucić do ${building.name}.",
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                CoinType.values().forEach { coin ->
                    val count = coinCounts.getValue(coin)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = count > 0) { selectedCoin = coin },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCoin == coin,
                            onClick = { if (count > 0) selectedCoin = coin },
                            enabled = count > 0
                        )
                        Text(
                            text = "${coin.label.replaceFirstChar { it.uppercase() }} ($count)",
                            color = if (count > 0) Color.White else Color.Gray
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedCoin?.let(onConfirm) },
                enabled = selectedCoin != null && coinCounts.getValue(selectedCoin!!) > 0
            ) {
                Text("WRZUĆ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ANULUJ", color = Color.Gray) }
        }
    )
}

@Composable
fun ActionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TeleportDialog(
    destinations: List<String>,
    worldDestinations: List<String>,
    selectedDestination: String,
    onSelectDestination: (String) -> Unit,
    heroData: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    nosiferFamily: FontFamily
) {
    var expanded by remember { mutableStateOf(false) }
    val weight = HeroLogic.calculateTotalWeight(heroData)
    val isWorld = worldDestinations.contains(selectedDestination)
    val distance = if (isWorld) 50 else 5
    val cost = (weight * distance * 2).toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F0F0F),
        title = { Text("PORTAL ETERYCZNY", color = Color.Cyan, fontFamily = nosiferFamily) },
        text = {
            Column {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if(selectedDestination.isEmpty()) "Wybierz cel" else selectedDestination)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        destinations.forEach { dest ->
                            DropdownMenuItem(
                                text = { Text(dest, color = if(worldDestinations.contains(dest)) Color.Magenta else Color.White) },
                                onClick = { onSelectDestination(dest); expanded = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Masa: $weight kg", color = Color.White)
                Text("Koszt: ${HeroLogic.formatPrice(cost)}", color = Color.Yellow)
            }
        },
        confirmButton = {
            CityActionButton(text = "AKTYWUJ", accentColor = Color.Cyan, modifier = Modifier.width(120.dp)) {
                onConfirm()
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ANULUJ", color = Color.Gray) }
        }
    )
}

@Composable
fun ShopDialog(
    building: BuildingInstance,
    buyMode: Boolean,
    onBuyModeChange: (Boolean) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    heroData: String,
    onUpdateHero: (String) -> Unit,
    onDismiss: () -> Unit,
    buildingInventories: MutableMap<String, List<TradeItem>>,
    selectedBuildingShop: Shop?,
    forcedCategory: String? = null,
    dataNow: String,
    heroProgress: HeroProgress? = null,
    onProgressChange: (HeroProgress) -> Unit = {}
) {
    val weight = HeroLogic.calculateCurrentWeight(heroData)
    val maxWeight = HeroLogic.getHeroWeightLimit(heroData)
    val copper = HeroLogic.calculateTotalCopper(heroData)
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = { 
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val title = when {
                    forcedCategory != null -> forcedCategory.uppercase()
                    buyMode -> "KUPNO"
                    else -> "SPRZEDAŻ"
                }
                Text(title, color = if(buyMode) Color.Green else Color.Red, fontWeight = FontWeight.Bold)
                Text(HeroLogic.formatPrice(copper), color = Color.Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Text("Dźwigasz: $weight / $maxWeight kg", color = if (weight > maxWeight) Color.Red else Color.Cyan, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                if (forcedCategory == null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onBuyModeChange(true) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if(buyMode) Color(0xFF2E7D32) else Color(0xFF1A1A1A))
                        ) { Text("KUPUJ") }
                        Button(
                            onClick = { onBuyModeChange(false) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if(!buyMode) Color(0xFFC62828) else Color(0xFF1A1A1A))
                        ) { Text("SPRZEDAWAJ") }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val items = if (buyMode) {
                    val allItems = buildingInventories.getOrPut(building.name) {
                        HeroLogic.getShopInventory(
                            selectedBuildingShop ?: HeroLogic.shops[0],
                            building = building,
                            currentTime = dataNow
                        )
                    }
                    if (forcedCategory != null) {
                        allItems.filter { it.category == forcedCategory || (forcedCategory == "Posiłki" && it.category == "Trunki") }
                    } else {
                        allItems
                    }
                } else {
                    HeroLogic.getHeroInventory(heroData)
                }
                
                LazyColumn(modifier = Modifier.heightIn(max = 450.dp)) {
                    items(items) { item ->
                        val qualityMatch = Regex("\\[Jakość: (\\d+)%\\]").find(item.name)
                        val currentQuality = qualityMatch?.groupValues?.get(1)?.toIntOrNull() ?: 100
                        val canRepair = !buyMode && (currentQuality < 100 || item.name.contains("(Zniszczony)") || item.name.contains("(Uszkodzony)") || item.name.contains("(Znoszony)")) && 
                                       selectedBuildingShop?.name in listOf("Kowal", "Płatnerz")
                        val repairCost = if (canRepair) HeroLogic.getRepairCost(item.name, currentQuality, HeroLogic.findItemPrice(item.name)) else 0

                        ShopItemRow(
                            item = item,
                            isBuy = buyMode,
                            canRepair = canRepair,
                            repairCost = repairCost,
                            onRepair = {
                                if (copper >= repairCost) {
                                    val updated = HeroLogic.repairItem(heroData, item.name, repairCost)
                                    onUpdateHero(updated)
                                } else {
                                    Toast.makeText(context, "Brak złota na naprawę!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onAction = {
                                if (buyMode) {
                                    if (copper < item.price) {
                                        Toast.makeText(context, "Brak złota!", Toast.LENGTH_SHORT).show()
                                        return@ShopItemRow
                                    }

                                    // Check if item is a meal
                                    if (item.category == "Posiłki") {
                                        // Consume meal - restore energy (reduce fatigue)
                                        val energyRecovery = RestManager.getEnergyRecoveryFromMeal(item.price)
                                        var updated = HeroLogic.deductMoney(heroData, item.price)
                                        onUpdateHero(updated)

                                        if (heroProgress != null) {
                                            val updatedProg = HeroLogic.updateHeroFatigue(heroProgress, -energyRecovery)
                                            onProgressChange(updatedProg)
                                            Toast.makeText(context, "Spożyłeś ${item.name}. Energia: -$energyRecovery pkt", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Spożyłeś ${item.name}.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        // Normal item purchase - add to inventory
                                        val itemWeight = HeroLogic.getItemWeight(item.name)
                                        if (weight + itemWeight <= maxWeight) {
                                            var updated = HeroLogic.deductMoney(heroData, item.price)
                                            updated = HeroLogic.addItemToHero(updated, item)
                                            onUpdateHero(updated)
                                        } else {
                                            Toast.makeText(context, "Zbyt ciężkie!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    var updated = HeroLogic.addMoney(heroData, item.price / 2)
                                    updated = HeroLogic.removeItemFromHero(updated, item)
                                    onUpdateHero(updated)
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            CityActionButton(text = "ZAMKNIJ", accentColor = Color.Gray, modifier = Modifier.width(120.dp).padding(8.dp)) {
                onDismiss()
            }
        }
    )
}

@Composable
fun RestPanel(
    building: BuildingInstance,
    heroData: String,
    currentFatigue: Int,
    onConfirm: (String, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var roomType by remember { mutableStateOf("Siano w stajni") }
    var restSlots by remember { mutableStateOf(4) }
    
    val hours = restSlots * 0.5f
    val costPerHour = RestManager.getCostPerHour(roomType)
    val totalCost = (costPerHour * hours).toInt()
    
    // Oblicz Wytrzymałość = Odporność / 10
    val stamina = (HeroLogic.getStatValue(heroData, "ODPORNOŚĆ") / 10).coerceAtLeast(1)
    val baseReduction = (RestManager.getFatigueReductionPerHour(roomType, stamina) * hours).toInt()
    val itemBonus = (RestManager.getItemBonusForRest(heroData) * hours).toInt()
    val fatigueReduction = baseReduction + itemBonus
    val predictedFatigue = (currentFatigue - fatigueReduction).coerceAtLeast(0)
    
    val minutes = (restSlots * 30) % 60
    val hoursDisplay = (restSlots * 30) / 60
    val timeDisplay = if (hoursDisplay > 0) {
        if (minutes > 0) "$hoursDisplay : $minutes" else "$hoursDisplay : 00"
    } else {
        "$minutes minut"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("WYNAJEM POKOJU", color = Color.White) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Budynek: ${building.name}", color = Color.Gray, fontSize = 12.sp)
                Text("Wytrzymałość: $stamina", color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                
                // Przedmioty bonusowe
                if (HeroLogic.hasItem(heroData, "Koc")) {
                    Text("🛏️ Koc (+2 pkt/h)", color = Color(0xFFFF9800), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                
                // Wybór pokoju
                Text("Jakość:", color = Color.White)
                val options = mutableListOf("Pokój", "Wspólna sala", "Siano w stajni")
                if (building.rank >= 4) options.add("Apartament")
                
                Column {
                    options.forEach { option ->
                        val regen = RestManager.getFatigueReductionPerHour(option, stamina)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { roomType = option }
                        ) {
                            RadioButton(
                                selected = (roomType == option),
                                onClick = { roomType = option }
                            )
                            Text(option, color = Color.White, modifier = Modifier.weight(1f))
                            Text("$regen pkt/h", color = Color.Yellow, fontSize = 9.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Czas odpoczynku: $timeDisplay", color = Color.White)
                Slider(
                    value = restSlots.toFloat(),
                    onValueChange = { restSlots = it.toInt() },
                    valueRange = 1f..20f,
                    steps = 18
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Koszt: $totalCost miedziaków", color = Color.Yellow)
                Text("Regeneracja: $baseReduction pkt zmęczenia", color = Color.Green)
                if (itemBonus > 0) {
                    Text("Bonus z przedmiotów: +$itemBonus pkt", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                }
                Text("Razem: $fatigueReduction pkt zmęczenia", color = Color.Cyan, fontWeight = FontWeight.Bold)
                Text("Zmęczenie po odpoczynku: $predictedFatigue%", color = Color.Green)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(roomType, hours) }) { Text("WYNAJMIJ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ANULUJ") } }
    )
}

@Composable
fun HaircutDialog(
    heroData: String,
    heroProgress: HeroProgress?,
    onUpdateHero: (String) -> Unit,
    onDismiss: () -> Unit,
    playSoundAction: (Int, Int) -> Unit = { _, _ -> },
    saveToFile: (String, String) -> Unit = { _, _ -> },
    onProgressChange: (HeroProgress) -> Unit = {}
) {
    val context = LocalContext.current
    val hairColors = listOf("Czarny", "Brązowy", "Rudy", "Blond", "Szary", "Biały")
    var selectedColor by remember { mutableStateOf("") }
    var showColorPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF251A0D),
        title = { Text("USŁUGI FRYZJERSKIE", color = Color.Cyan) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Co chciałbyś zrobić?", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                
                // FARBOWANIE WŁOSÓW
                Button(
                    onClick = { showColorPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4513))
                ) {
                    Text("🎨 FARBOWANIE WŁOSÓW (26 miedzi)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // CZESANIE
                Button(
                    onClick = {
                val cost = 4
                val totalMoney = HeroLogic.calculateTotalCopper(heroData)
                if (totalMoney >= cost) {
                    val updated = HeroLogic.deductMoney(heroData, cost)
                    onUpdateHero(updated)
                    saveToFile("hero_${heroProgress?.heroId}", updated)
                    Toast.makeText(context, "Włosy pięknie uczesane!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else {
                    Toast.makeText(context, "Nie masz wystarczająco miedzi! (potrzebujesz 4)", Toast.LENGTH_SHORT).show()
                }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) {
                    Text("💆 CZESANIE (4 miedzi)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // ŚCINANIE
                Button(
                    onClick = {
                        val cost = 10
                        val totalMoney = HeroLogic.calculateTotalCopper(heroData)
                        if (totalMoney >= cost) {
                            var updated: String = HeroLogic.deductMoney(heroData, cost)
                            
                            if (HeroLogic.hasTraitContaining(updated, "Długie włosy")) {
                                updated = HeroLogic.removeTraitFromHero(updated, "Długie włosy")
                                updated = HeroLogic.addTraitToHero(updated, "Krótkie włosy")
                                Toast.makeText(context, "Włosy strzyżone na krótko!", Toast.LENGTH_SHORT).show()
                            } else if (HeroLogic.hasTraitContaining(updated, "Krótkie włosy")) {
                                updated = HeroLogic.removeTraitFromHero(updated, "Krótkie włosy")
                                updated = HeroLogic.addTraitToHero(updated, "Długie włosy")
                                Toast.makeText(context, "Wpuszczasz włosy!", Toast.LENGTH_SHORT).show()
                            } else {
                                updated = HeroLogic.addTraitToHero(updated, "Krótkie włosy")
                                Toast.makeText(context, "Włosy strzyżone!", Toast.LENGTH_SHORT).show()
                            }
                            
                            onUpdateHero(updated)
                            saveToFile("hero_${heroProgress?.heroId}", updated)
                        } else {
                            Toast.makeText(context, "Nie masz wystarczająco miedzi! (potrzebujesz 10)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00))
                ) {
                    Text("✂️ ŚCINANIE (10 miedzi)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // GOLENIE
                val hasBeard = HeroLogic.hasTraitContaining(heroData, "broda")
                Button(
                    onClick = {
                        val cost = 15
                        val totalMoney = HeroLogic.calculateTotalCopper(heroData)
                        if (totalMoney >= cost) {
                            var updated: String = HeroLogic.deductMoney(heroData, cost)
                            val beardTrait = HeroLogic.findTraitContaining(updated, "broda")
                            
                            if (beardTrait != null) {
                                updated = HeroLogic.removeTraitFromHero(updated, beardTrait)
                                Toast.makeText(context, "Golone! Twarz wygląda świeżo!", Toast.LENGTH_SHORT).show()
                            }
                            
                            onUpdateHero(updated)
                            saveToFile("hero_${heroProgress?.heroId}", updated)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Nie masz wystarczająco miedzi! (potrzebujesz 15)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (hasBeard) Color(0xFF616161) else Color(0xFF999999)),
                    enabled = hasBeard
                ) {
                    Text("🪒 GOLENIE (15 miedzi)")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("WYJDŹ", color = Color.Gray) } }
    )
    
    // Color picker dialog
    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            confirmButton = { TextButton(onClick = { showColorPicker = false }) { Text("ZAMKNIJ", color = Color.Cyan) } },
            dismissButton = { TextButton(onClick = { showColorPicker = false }) { Text("ANULUJ", color = Color.Gray) } },
            containerColor = Color(0xFF251A0D),
            title = { Text("WYBÓR KOLORU WŁOSÓW", color = Color.Cyan) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(hairColors) { color ->
                        Button(
                            onClick = {
                                val cost = 26
                                val totalMoney = HeroLogic.calculateTotalCopper(heroData)
                                if (totalMoney >= cost) {
                                    var updated: String = HeroLogic.deductMoney(heroData, cost)
                                    updated = HeroLogic.updateHairColor(updated, color)
                                    onUpdateHero(updated)
                                    saveToFile("hero_${heroProgress?.heroId}", updated)
                                    Toast.makeText(context, "Włosy postaci zmieniono na $color!", Toast.LENGTH_SHORT).show()
                                    showColorPicker = false
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Nie masz wystarczająco miedzi! (potrzebujesz 26)", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4513))
                        ) {
                            Text(color, color = Color.White)
                        }
                    }
                }
            }
        )
    }
}

fun getRoomContextDescription(room: Room, building: BuildingInstance): String {
    return "Jesteś w: ${room.name} (${building.name})"
}

@Composable
fun LibraryDialog(
    inventory: LibraryInventory,
    heroData: String,
    heroProgress: HeroProgress?,
    settlementType: SettlementType,
    onBuyBook: (Book, Double) -> Unit,
    onBorrowBook: (Book, Double) -> Unit,
    onSellBook: (Book) -> Unit,
    ownedBooks: List<Book>,
    onDismiss: () -> Unit
) {
    var selectedAction by remember { mutableStateOf<String?>(null) }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var learningHours by remember { mutableStateOf(3.0f) }

    // Calculate costs and availability
    val costPerHalfHour = when (settlementType) {
        SettlementType.CITY -> 3
        SettlementType.TOWN -> 5
        SettlementType.VILLAGE -> 0
    }

    val totalMoney = HeroLogic.calculateTotalCopper(heroData)
    val currentTime = heroProgress?.currentCalendar?.split("|")?.getOrNull(5)?.trim() ?: "12:00"
    val (currentHour, currentMinute) = try {
        val parts = currentTime.split(":")
        Pair(parts[0].toInt(), parts.getOrNull(1)?.toInt() ?: 0)
    } catch (e: Exception) {
        Pair(12, 0)
    }

    val borrowCost = ((learningHours / 0.5f).toInt() * costPerHalfHour)
    val canAfford = totalMoney >= borrowCost
    
    // Calculate max time before 20:00 (library closes)
    val minutesUntilClose = (20 - currentHour) * 60 - currentMinute
    val maxHoursDue = (minutesUntilClose / 60f).coerceAtLeast(0.5f)
    val isClosed = currentHour >= 20

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("BIBLIOTEKA - CZYTELNIA", color = Color(0xFF1565C0), fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                if (selectedBook == null) {
                    // Show menu
                    LazyColumn {
                        item {
                            Text("CO CHCESZ ZROBIĆ?", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        item {
                            Button(
                                onClick = { selectedAction = "BUY" },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("💰 KUP KSIĄŻKĘ")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        item {
                            Button(
                                onClick = { selectedAction = "BORROW" },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isClosed) Color(0xFF666666) else Color(0xFF1565C0)
                                ),
                                enabled = !isClosed
                            ) {
                                Text(if (isClosed) "📖 WYPOŻYCZ (ZAMKNIĘTE - godz. 20:00)" else "📖 WYPOŻYCZ (NAUKA)")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        if (ownedBooks.isNotEmpty()) {
                            item {
                                Button(
                                    onClick = { selectedAction = "SELL" },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                                ) {
                                    Text("💎 SPRZEDAJ KSIĄŻKĘ (${ownedBooks.size})")
                                }
                            }
                        }
                    }
                } else if (selectedAction == "BUY") {
                    // Show books for purchase
                    Text("DOSTĘPNE KSIĄŻKI DO KUPNA", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn {
                        items(inventory.buyingBooks) { book ->
                            BookItemButton(
                                book = book,
                                actionLabel = "Kup (${book.price}p)",
                                buttonColor = Color(0xFF2E7D32),
                                onClick = {
                                    selectedBook = book
                                }
                            )
                        }
                    }
                } else if (selectedAction == "BORROW") {
                    // Show books for borrowing
                    Text("DOSTĘPNE KSIĄŻKI DO WYPOŻYCZENIA", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Koszt: ${costPerHalfHour}p za 30 minut", color = Color.Yellow, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn {
                        items(inventory.borrowingBooks) { book ->
                            val canBorrow = BookManager.canBorrowBook(heroData, book.skill)
                            BookItemButton(
                                book = book,
                                actionLabel = if (canBorrow) "Wypożycz" else "Zablokowano",
                                buttonColor = if (canBorrow) Color(0xFF1565C0) else Color(0xFF666666),
                                onClick = {
                                    if (canBorrow) selectedBook = book
                                }
                            )
                        }
                    }
                } else if (selectedAction == "SELL") {
                    // Show owned books for selling
                    Text("TWOJE KSIĄŻKI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn {
                        items(ownedBooks) { book ->
                            val sellPrice = (book.price * 0.5).toInt()
                            BookItemButton(
                                book = book,
                                actionLabel = "Sprzedaj (${sellPrice}p)",
                                buttonColor = Color(0xFFD32F2F),
                                onClick = {
                                    selectedBook = book
                                }
                            )
                        }
                    }
                }

                if (selectedBook != null && selectedAction == "BORROW") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("CZAS NAUKI", color = Color.White, fontSize = 12.sp)
                    Text(
                        "Dostępny czas: do godz. 20:00 (~${maxHoursDue.toInt()}h)",
                        color = Color(0xFFFF9800),
                        fontSize = 10.sp
                    )
                    Slider(
                        value = learningHours,
                        onValueChange = { learningHours = it },
                        valueRange = 0.5f..maxHoursDue.coerceAtLeast(0.5f),
                        steps = 15,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canAfford && !isClosed
                    )
                    Text(
                        "${learningHours.toInt()}h ${((learningHours % 1) * 60).toInt()}m",
                        color = Color.Yellow,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("KOSZT WYPOŻYCZENIA", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Razem:", color = Color.White)
                        Text(
                            "${borrowCost}p",
                            color = if (canAfford) Color.Green else Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Masz: ${totalMoney}p",
                        color = if (canAfford) Color.LightGray else Color.Red,
                        fontSize = 10.sp
                    )
                    
                    if (!canAfford) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "❌ Brak wystarczających funduszy!",
                            color = Color.Red,
                            fontSize = 11.sp
                        )
                    }
                    
                    if (learningHours < 3) {
                        Text(
                            "⚠️ Poniżej 3h - brak testu (tylko +1PN)",
                            color = Color(0xFFFF9800),
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (selectedBook != null) {
                Button(
                    onClick = {
                        when (selectedAction) {
                            "BUY" -> onBuyBook(selectedBook!!, learningHours.toDouble())
                            "BORROW" -> {
                                if (canAfford) {
                                    onBorrowBook(selectedBook!!, learningHours.toDouble())
                                }
                            }
                            "SELL" -> onSellBook(selectedBook!!)
                        }
                        onDismiss()
                    },
                    enabled = selectedAction != "BORROW" || (canAfford && !isClosed)
                ) {
                    Text("POTWIERDŹ")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("ZAMKNIJ", color = Color.Gray) }
            }
        },
        dismissButton = {
            if (selectedBook != null) {
                TextButton(onClick = { selectedBook = null }) { Text("WRÓĆ", color = Color.Gray) }
            } else {
                TextButton(onClick = onDismiss) { Text("WYJDŹ", color = Color.Gray) }
            }
        }
    )
}

@Composable
fun BookItemButton(
    book: Book,
    actionLabel: String,
    buttonColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(book.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(book.skill, fontSize = 10.sp, color = Color.LightGray)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(actionLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
