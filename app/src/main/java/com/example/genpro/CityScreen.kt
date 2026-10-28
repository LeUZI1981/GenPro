package com.example.genpro

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Composable
fun CityScreen(
    heroProgress: HeroProgress?,
    onHeroProgressChange: (HeroProgress) -> Unit,
    nosiferFamily: FontFamily? = null,
    slaboFamily: FontFamily? = null,
    lobsterFamily: FontFamily? = null,
    onBack: () -> Unit,
    onMenuAppChange: (Int) -> Unit,
    onShopTypeChange: (String) -> Unit,
    readFromFile: (String) -> String,
    saveToFile: (String, String) -> Unit,
    playSoundAction: (Int, Int) -> Unit,
    isGMMode: Boolean = false,
    settlementData: String = ""
) {
    val context = LocalContext.current
    if (heroProgress == null) return

    val heroId = heroProgress.heroId
    val fileName = "hero_$heroId"
    var heroData by remember { mutableStateOf(readFromFile(fileName)) }

    val settlement = remember(settlementData, heroProgress) {
        val calendar = heroProgress.currentCalendar
        val parts = calendar.split("|").map { it.trim() }
        val cityNameFromCalendar = parts.firstOrNull() ?: "Nieznane Miasto"
        val travelSettlementType = parts.getOrNull(8)
            ?.let { typeName -> runCatching { SettlementType.valueOf(typeName) }.getOrNull() }
            ?: SettlementType.CITY
        
        val travelDataStr = readFromFile("hero_map_pins_${heroId}_mapworld")
        val settlementRace = if (settlementData.startsWith("[EXTERNAL_RACE:")) {
            settlementData.substringAfter("RACE:").substringBefore("]")
        } else if (travelDataStr.contains("rasy Krasnolud") && travelDataStr.contains(cityNameFromCalendar)) {
            "Krasnolud"
        } else if (travelDataStr.contains("rasy Elf") && travelDataStr.contains(cityNameFromCalendar)) {
            "Elf"
        } else {
            "Człowiek"
        }

        val existing = if (settlementData.isNotEmpty() && settlementData != "[EXTERNAL]" && !settlementData.startsWith("[EXTERNAL_RACE:")) {
            WorldPersistence.decodeSettlementOrNull(settlementData)
                ?: WorldPersistence.loadSettlement(context, cityNameFromCalendar)
        } else {
            WorldPersistence.loadSettlement(context, cityNameFromCalendar)
        }

        // Fix: Ensure we don't use a settlement literal named "Nieznane Miasto" or "Nieznane Miejsce"
        val finalGenName = if (cityNameFromCalendar == "Nieznane Miasto" || cityNameFromCalendar == "Nieznane Miejsce" || cityNameFromCalendar.isBlank()) {
            EmpireWorldGenerator.generateEmpireName(travelSettlementType, settlementRace)
        } else {
            cityNameFromCalendar
        }
        
        var generated = if (existing != null && (existing.name == "Nieznane Miasto" || existing.name == "Nieznane Miejsce")) {
            existing.copy(name = finalGenName)
        } else {
            existing ?: SettlementGenerator.generateSettlement(context, finalGenName, travelSettlementType, settlementRace)
        }
        
        if (existing == null || existing.name != generated.name) {
            WorldPersistence.saveSettlement(context, generated)
        }

        // Jeśli nazwa w kalendarzu była nieznana, sprawdzamy czy możemy ją już ujawnić
        val finalCityName = generated.name
        val discoveredZones = heroProgress.discoveredZones[finalCityName] ?: emptySet()
        
        val settlementJson = "[EXTERNAL]"
        val mutableParts = parts.toMutableList()
        while (mutableParts.size < 8) mutableParts.add("")
        
        var calendarUpdated = false
        // Poznanie nazwy miasta dopiero po odkryciu min. 2 dzielnic lub jeśli już jest odkryta (np. przez test rozmowy)
        if (discoveredZones.size >= 2) {
            if (mutableParts[0] != finalCityName) {
                mutableParts[0] = finalCityName
                calendarUpdated = true
            }
        } else if (mutableParts[0] == "Nieznane Miejsce" || mutableParts[0].isEmpty()) {
            mutableParts[0] = "Nieznane Miasto"
            calendarUpdated = true
        }
        
        if (mutableParts[3] != settlementJson) {
            mutableParts[3] = settlementJson
            calendarUpdated = true
        }
        
        val arrivalDirection = if (mutableParts.size >= 8 && mutableParts[7].isNotEmpty()) mutableParts[7] else null
        if (mutableParts[6].isEmpty() || mutableParts[6] == "Centrum") {
            mutableParts[6] = SettlementGenerator.getStartingZoneName(generated, arrivalDirection)
            calendarUpdated = true
        }
        
        if (calendarUpdated) {
            val newCalendar = mutableParts.joinToString(" | ")
            val updatedProgress = heroProgress.copy(currentCalendar = newCalendar)
            onHeroProgressChange(updatedProgress)
        }
        
        generated
    }

    var currentZoneName by remember(settlement) {
        val calendar = heroProgress.currentCalendar
        val parts = calendar.split("|").map { it.trim() }
        val zoneFromCalendar = if (parts.size >= 7) parts[6] else ""
        
        // Failsafe: sprawdzenie czy dzielnica faktycznie istnieje w osadzie
        val zoneExists = settlement?.zones?.any { it.name == zoneFromCalendar } == true
        
        val initialZone = if ((zoneFromCalendar.isEmpty() || !zoneExists) && settlement != null) {
            val arrivalDir = parts.getOrNull(7).orEmpty()
            val startingZone = SettlementGenerator.getStartingZoneName(settlement, arrivalDir)
            
            // Aktualizacja danych bohatera o wyznaczoną dzielnicę startową
            val mutableParts = parts.toMutableList()
            while (mutableParts.size < 7) mutableParts.add("")
            mutableParts[6] = startingZone
            val newCalendar = mutableParts.joinToString(" | ")
            val updatedProgress = heroProgress.copy(currentCalendar = newCalendar)
            onHeroProgressChange(updatedProgress)
            
            startingZone
        } else {
            zoneFromCalendar
        }
        mutableStateOf(initialZone)
    }
    var showBurglaryResultDialog by remember { mutableStateOf<HeroLogic.BurglaryResult?>(null) }
    var showGuardEncounterDialog by remember { mutableStateOf<HeroLogic.GuardEncounter?>(null) }
    var showWorkSessionResultDialog by remember { mutableStateOf<HeroLogic.WorkSessionResult?>(null) }
    var showWorkSliderDialog by remember { mutableStateOf<Triple<String, String, Int>?>(null) }
    var showTrainingResultDialog by remember { mutableStateOf<HeroLogic.TrainingResult?>(null) }
    var showLuckResultDialog by remember { mutableStateOf<String?>(null) }
    var showTalkDialog by remember { mutableStateOf(false) }
    var showDiscoveryDialog by remember { mutableStateOf(false) }
    val currentHour = remember(heroData) { HeroLogic.getCurrentHour(heroData) }
    val currentMinute = remember(heroData) {
        val calendar = HeroLogic.getCalendarData(heroData)
        val timePart = calendar.split("|").getOrNull(2)?.trim() ?: "12:00"
        timePart.split(":").getOrNull(1)?.toIntOrNull() ?: 0
    }
    val totalMinutes = currentHour * 60 + currentMinute

    val talkDuration = remember(totalMinutes) {
        val isDay = totalMinutes in (6 * 60 + 15)..(18 * 60 + 45)
        if (isDay) (5..11).random() else (9..17).random()
    }

    val discoveryDuration = remember(totalMinutes) {
        val isDay = totalMinutes in (6 * 60)..(19 * 60 + 15)
        if (isDay) (15..21).random() else (29..47).random()
    }

    val settlementState = remember(settlement) { mutableStateOf(settlement) }

    fun changeZone(newZoneName: String) {
        val currentFatigue = heroProgress.fatigue
        val maxFatigue = heroProgress.maxFatigue.coerceAtLeast(100)
        
        if (currentFatigue >= (maxFatigue * 0.9).toInt()) {
            Toast.makeText(context, "Jesteś zbyt zmęczony by się poruszać! Musisz się odespocząć.", Toast.LENGTH_LONG).show()
            return
        }
        
        val oldCalendar = heroProgress.currentCalendar
        val parts = oldCalendar.split("|").map { it.trim() }.toMutableList()
        while (parts.size < 7) parts.add("")
        
        val cityName = parts[0]
        
        // Strefy z mapy podróży tworzą graf sąsiedztwa, a nie zawsze siatkę 3x3.
        val travelTime = if (newZoneName in (settlement?.zones?.find { it.name == currentZoneName }?.neighbors ?: emptyList())) {
            (15..20).random()
        } else {
            (5..10).random()
        }
        
        var updatedProgress = HeroLogic.advanceTimeMinutesProgress(heroProgress, travelTime)
        val calendarWithTime = updatedProgress.currentCalendar
        val isNight = HeroLogic.isNight(calendarWithTime)
        
        val updatedParts = calendarWithTime.split("|").map { it.trim() }.toMutableList()
        while (updatedParts.size < 7) updatedParts.add("")
        updatedParts[6] = newZoneName
        
        val newCalendar = updatedParts.joinToString(" | ")
        updatedProgress = updatedProgress.copy(currentCalendar = newCalendar)
        
        // Odkrywanie dzielnicy
        val cityNameForDiscovery = settlement?.name ?: cityName
        updatedProgress = HeroLogic.updateDiscoveredZones(updatedProgress, cityNameForDiscovery, newZoneName)
        
        // Logika automatycznego poznania nazwy miasta przy 2 odkrytych dzielnicach
        val updatedSettlementZones = updatedProgress.discoveredZones[cityNameForDiscovery] ?: emptySet()
        if (updatedSettlementZones.size >= 2) {
            val calendar = updatedProgress.currentCalendar
            val p = calendar.split("|").map { it.trim() }.toMutableList()
            if (p.isNotEmpty() && (p[0] == "Nieznane Miasto" || p[0] == "Nieznane Miejsce" || p[0].isEmpty())) {
                p[0] = cityNameForDiscovery
                updatedProgress = updatedProgress.copy(currentCalendar = p.joinToString(" | "))
                Toast.makeText(context, "Teraz już wiesz - to miasto nazywa się $cityNameForDiscovery!", Toast.LENGTH_SHORT).show()
            }
        }
        
        val currentZoneObj = settlement?.zones?.find { it.name == newZoneName }
        val encounter = HeroLogic.checkGuardEncounter(heroData, isNight, currentZoneObj?.districtType)
        
        if (encounter != null) {
            showGuardEncounterDialog = encounter
            if (encounter.fine > 0) {
                heroData = HeroLogic.deductMoney(heroData, encounter.fine)
                saveToFile(fileName, heroData)
            }
            if (encounter.fatigue > 0) {
                updatedProgress = HeroLogic.updateHeroFatigue(updatedProgress, encounter.fatigue)
            }
            if (encounter.wantedIncrease != 0) {
                updatedProgress = HeroLogic.updateWantedLevel(updatedProgress, encounter.wantedIncrease)
            }
            
            if (encounter.isArrested) {
                updatedProgress = HeroLogic.advanceTimeMinutesProgress(updatedProgress, 6 * 60)
            }
        }
        
        saveToFile("progress_$heroId.json", Json.encodeToString(updatedProgress))
        onHeroProgressChange(updatedProgress)
        
        // Logika odkrywania Fontanny i Studni
        settlement?.let { currentSettlement ->
            val zoneIndex = currentSettlement.zones.indexOfFirst { it.name == newZoneName }
            if (zoneIndex != -1) {
                val zone = currentSettlement.zones[zoneIndex]
                var updatedZone = zone
                var somethingDiscovered = false
                var discoveredName = ""
                
                // Centrum (Kupiecka) -> Fontanna (40%)
                if (zone.districtType == DistrictType.MERCHANT && !zone.hasFountain) {
                    if (Random.nextInt(1, 101) <= 40) {
                        updatedZone = updatedZone.copy(hasFountain = true)
                        somethingDiscovered = true
                        discoveredName = "Miejska Fontanna"
                    }
                }
                // Mieszkalna / Slumsy -> Studnia (70%)
                if (
                    currentSettlement.type != SettlementType.VILLAGE &&
                    (zone.districtType == DistrictType.RESIDENTIAL || zone.districtType == DistrictType.SLUMS) &&
                    !zone.hasWell
                ) {
                    if (Random.nextInt(1, 101) <= 70) {
                        updatedZone = updatedZone.copy(hasWell = true)
                        somethingDiscovered = true
                        discoveredName = "Miejska Studnia"
                    }
                }
                
                // Odkrywanie pozostałych budynków (20% szans przy wejściu na każdy nieodkryty)
                val newExtras = updatedZone.extraBuildings.map { b ->
                    if (!b.discovered && Random.nextInt(1, 101) <= 20) {
                        somethingDiscovered = true
                        discoveredName = b.name
                        b.copy(discovered = true)
                    } else b
                }
                var newInn = updatedZone.inn
                if (!updatedZone.inn.discovered && Random.nextInt(1, 101) <= 20) {
                    somethingDiscovered = true
                    discoveredName = updatedZone.inn.name
                    newInn = updatedZone.inn.copy(discovered = true)
                }
                var newSpec = updatedZone.specialBuilding
                if (!updatedZone.specialBuilding.discovered && Random.nextInt(1, 101) <= 20) {
                    somethingDiscovered = true
                    discoveredName = updatedZone.specialBuilding.name
                    newSpec = updatedZone.specialBuilding.copy(discovered = true)
                }
                
                if (somethingDiscovered) {
                    updatedZone = updatedZone.copy(extraBuildings = newExtras, inn = newInn, specialBuilding = newSpec)
                    val newZones = currentSettlement.zones.toMutableList()
                    newZones[zoneIndex] = updatedZone
                    val updatedSettlement = currentSettlement.copy(zones = newZones)
                    WorldPersistence.saveSettlement(context, updatedSettlement)
                    Toast.makeText(context, "Odkryłeś nowy obiekt: $discoveredName!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        onHeroProgressChange(updatedProgress)
        currentZoneName = newZoneName
        playSoundAction(R.raw.klik1, 0)
    }

    val (gold, silver, copper) = HeroLogic.parseMoney(heroData)
    val weightLimit = HeroLogic.getHeroWeightLimit(heroData)
    val currentWeight = calculateCurrentWeight(heroData)
    val encumbrance = HeroLogic.getEncumbrance(currentWeight, weightLimit)
    val currentFatigue = heroProgress.fatigue
    val weather = HeroLogic.getWeather(heroData)

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        val calendarData = heroProgress.currentCalendar
        val discoveredZones = heroProgress.discoveredZones[settlement?.name ?: ""] ?: emptySet()

            // HEADER with District Name and Description
            val currentZone = settlementState.value?.zones?.find { it.name == currentZoneName }
            
            // Logika sprawdzania czy nazwa miasta jest znana na podstawie kalendarza
            val calendarParts = calendarData.split("|").map { it.trim() }
            val cityNameFromCalendar = calendarParts.firstOrNull() ?: ""
            val isCityKnown = cityNameFromCalendar.isNotEmpty() && 
                             cityNameFromCalendar != "Nieznane Miejsce" && 
                             cityNameFromCalendar != "Nieznane Miasto"
            
            val gateZones = settlement?.gateDirections ?: emptySet()
            val isGate = currentZoneName in gateZones
            val gateDisplayText = if (isGate) {
                if (isCityKnown) "Brama $currentZoneName $cityNameFromCalendar" else "Brama $currentZoneName"
            } else {
                currentZone?.name ?: currentZoneName
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF151515))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {



            // BYŁA KOLUMNA

            // MAIN AREA - Vertical Layout
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {


                if (currentZone != null) {
                    val zoneColor = getDistrictColor(currentZone.districtType)
                    Text(
                        gateDisplayText.uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = nosiferFamily,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        currentZone.districtType.displayName,
                        color = zoneColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val description = HeroLogic.generateDistrictDescription(
                        if (isCityKnown) (settlement?.name ?: "") else "Nieznane Miasto",
                        currentZone.name,
                        currentZone.districtType,
                        calendarData,
                        weather
                    )

                    Text(
                        description,
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontFamily = slaboFamily,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // BUILDING CHIPS (3 ROWS)
                if (currentZone != null) {
                    val heroName = HeroLogic.getHeroName(heroData)
                    val heroClass = HeroLogic.getHeroClass(heroData)
                    val isMagic = HeroLogic.isMagicClass(heroData)
                    val gateZones = settlement?.gateDirections ?: emptySet()
                    val isGateZone = currentZoneName in gateZones
                    
                    val rawBuildings = (listOf(currentZone.specialBuilding, currentZone.inn) + currentZone.extraBuildings)
                        .map { it.withCanonicalBuildingName() }
                        .toMutableList()
                    
                    // Magazyn dla Portu i Kupieckiej
                    if (currentZone.districtType == DistrictType.HARBOR || currentZone.districtType == DistrictType.MERCHANT) {
                        rawBuildings.add(BuildingInstance("Magazyn", "Magazyn", rank = 2, rooms = emptyList(), totalArea = 100.0, isPublicInstitution = true, discovered = true))
                    }
                    // Fontanna i Studnia
                    if (currentZone.hasFountain) {
                        rawBuildings.add(BuildingInstance("Obiekt", "Miejska Fontanna", rank = 1, rooms = emptyList(), totalArea = 10.0, isPublicInstitution = true, discovered = true))
                    }
                    if (currentZone.hasWell) {
                        rawBuildings.add(BuildingInstance("Obiekt", "Miejska Studnia", rank = 1, rooms = emptyList(), totalArea = 5.0, isPublicInstitution = true, discovered = true))
                    }
                    
                    val visibleBuildings = rawBuildings.filter { b ->
                        if (b.discovered || HeroLogic.hasVisitedBuilding(heroProgress, settlement?.name ?: "", b.name)) return@filter true
                        if (settlement?.type == SettlementType.VILLAGE) return@filter false
                        
                        // Portal/Magic logic
                        if (b.type.contains("Portal", true)) {
                            val chance = if (isMagic) 0.8 else 0.2
                            val seed = (b.name + heroName).hashCode().toLong()
                            return@filter kotlin.random.Random(seed).nextDouble() < chance
                        }
                        
                        val seed = (b.name + heroName + currentZone.name).hashCode().toLong()
                        val roll = kotlin.random.Random(seed).nextInt(1, 101)
                        
                        val baseChance = when (currentZone.districtType) {
                            DistrictType.RULER_SEAT -> if (b.type.contains("Pałac", true)) 90 else if (b.type.contains("Ratusz", true)) 100 else 50
                            DistrictType.MERCHANT -> if (b.type.contains("Rynek", true) || b.type.contains("Sukiennice", true)) 100 else 50
                            DistrictType.SLUMS -> if (b.type.contains("Sierociniec", true)) 100 else 50
                            DistrictType.MILITARY -> if (b.type.contains("Koszary", true) || b.type.contains("Garnizon", true) || b.type.contains("Akademia", true)) 100 else 50
                            DistrictType.MAGIC -> if (b.type.contains("Wieża", true) || b.type.contains("Kuźnia Run", true)) 100 else 50
                            DistrictType.SACRED -> if (b.type.contains("Katedra", true) || b.type.contains("Świątynia", true) || b.type.contains("Bazylika", true)) 100 else 50
                            else -> 50
                        }
                        val bonus = if (heroClass == "Łotr" && (b.type.contains("Gildia", true) || b.type.contains("Złodziej", true))) 80 else 0
                        roll <= (baseChance + bonus)
                    }

                    // Row 1: Main (> 85% or visited)
                    val row1 = visibleBuildings.filter { b ->
                        val baseChance = when (currentZone.districtType) {
                            DistrictType.RULER_SEAT -> if (b.type.contains("Pałac", true)) 90 else if (b.type.contains("Ratusz", true)) 100 else 50
                            DistrictType.MERCHANT -> if (b.type.contains("Rynek", true) || b.type.contains("Sukiennice", true)) 100 else 50
                            DistrictType.MILITARY -> if (b.type.contains("Koszary", true) || b.type.contains("Garnizon", true) || b.type.contains("Akademia", true)) 100 else 50
                            DistrictType.MAGIC -> if (b.type.contains("Wieża", true) || b.type.contains("Kuźnia Run", true)) 100 else 50
                            DistrictType.SACRED -> if (b.type.contains("Katedra", true) || b.type.contains("Świątynia", true) || b.type.contains("Bazylika", true)) 100 else 50
                            else -> 50
                        }
                        baseChance > 85 || HeroLogic.hasVisitedBuilding(heroProgress, settlement?.name ?: "", b.name)
                    }

                    // Row 2: Gastro + Zamtuz
                    val gastroTypes = listOf("Karczma", "Tawerna", "Gospoda", "Oberża", "Hotel", "Motel", "Speluna")
                    val row2 = visibleBuildings.filter { b ->
                        val isGastro = gastroTypes.any { b.type.contains(it, true) }
                        val isZamtuz = b.type.contains("Zamtuz", true) && (
                            currentZone.districtType == DistrictType.SLUMS || 
                            (currentZone.districtType == DistrictType.MILITARY && (b.name + heroName).hashCode().let { Math.abs(it) % 100 < 20 })
                        )
                        (isGastro || isZamtuz) && !row1.contains(b)
                    }

                    // Row 3: Others (Smith, Apothecary, Transport at Gates)
                    val row3 = visibleBuildings.filter { b ->
                        val isOther = listOf("Kowal", "Apteka", "Medyk", "Płatnerz", "Rymarz", "Świątynia", "Portal", "Uniwersytet", "Fontanna", "Studnia", "Koszary", "Garnizon", "Zbrojownia", "Warsztat").any { b.type.contains(it, true) || b.name.contains(it, true) }
                        val isTransport = (b.type.contains("Stajnia", true) || b.type.contains("Wózkownia", true)) && isGateZone
                        (isOther || isTransport || b.type == "Magazyn") && !row1.contains(b) && !row2.contains(b)
                    }

                    @Composable
                    fun BuildingRow(title: String, list: List<BuildingInstance>, color: Color) {
                        if (list.isNotEmpty()) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    title,
                                    color = color.copy(alpha = 0.7f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 10.sp,
                                    maxLines = 2,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                androidx.compose.foundation.lazy.LazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(list.size) { index ->
                                        val b = list[index]
                                        val isVisited = HeroLogic.isBuildingVisited(heroProgress, settlement?.name ?: "", b.name)
                                        val chipColor = if (isVisited) Color(0xFFFFD700) else Color.Gray
                                        
                                        Surface(
                                            modifier = Modifier.clickable {
                                                playSoundAction(R.raw.klik1, 0)

                                                val directType = when {
                                                    b.type.equals("Obiekt", true) && b.name.contains("Studnia", true) -> "Studnia"
                                                    b.type.equals("Obiekt", true) && b.name.contains("Fontanna", true) -> "Fontanna"
                                                    b.type.equals("Obiekt", true) && b.name.contains("Magazyn", true) -> "Magazyn"
                                                    b.type.isBlank() && b.name.contains("Studnia", true) -> "Studnia"
                                                    b.type.isBlank() && b.name.contains("Fontanna", true) -> "Fontanna"
                                                    b.type.isBlank() && b.name.contains("Magazyn", true) -> "Magazyn"
                                                    b.name == "Miejski Magazyn" -> "Magazyn"
                                                    else -> b.type
                                                }

                                                if (directType.equals("Magazyn", true)) {
                                                    onShopTypeChange("_SPECIAL:MAGAZYN:Magazyn")
                                                } else {
                                                    onShopTypeChange("_DIRECT:${directType}:${b.name}")
                                                }
                                                onMenuAppChange(22)
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color(0xFF1A1A1A),
                                            border = BorderStroke(1.dp, chipColor.copy(alpha = 0.5f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                val showStars = gastroTypes.any { b.type.contains(it, true) } || b.type.contains("Zamtuz", true)
                                                if (showStars) {
                                                    Text("★".repeat(b.rank), color = chipColor, fontSize = 7.sp, lineHeight = 8.sp)
                                                }
                                                Text(b.name, color = chipColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    BuildingRow("BUDYNKI GŁÓWNE", row1, Color(0xFFDA7037))
                    BuildingRow("GOŚCINA I ROZRYWKA", row2, Color(0xFFDAA037))
                    BuildingRow("USŁUGI I HANDEL", row3, Color(0xFFDAB520))


                    // ACTION BUTTONS (Praca, Trening, Półświat)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- AKTYWNOŚCI ---
                        ActionCategoryHeader("AKTYWNOŚCI", Color(0xFFAA9320))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CityHubButton("ROZMOWA\n($talkDuration min)", Color(0xBBDA7037), modifier = Modifier.weight(1f)) {
                                playSoundAction(R.raw.klik1, 0)
                                showTalkDialog = true
                            }
                            CityHubButton("ODKRYWANIE\n($discoveryDuration min)", Color(0xBBF57C00), modifier = Modifier.weight(1f)) {
                                playSoundAction(R.raw.klik1, 0)
                                showDiscoveryDialog = true
                            }
                        }

                        // --- CRIME ---
                        val canBurgle = currentZone.districtType in listOf(DistrictType.RESIDENTIAL, DistrictType.MERCHANT, DistrictType.CRAFT, DistrictType.SLUMS)
                        if (canBurgle && HeroLogic.canAttemptLockpicking(heroData)) {
                            Spacer(modifier = Modifier.height(12.dp))
                            ActionCategoryHeader("PÓŁŚWIATEK", Color(0xFF333333))
                            CityHubButton("WŁAMANIE", Color(0xFF222222)) {
                                val result = HeroLogic.performBurglary(
                                    heroData = heroData,
                                    heroProgress = heroProgress,
                                    buildingName = currentZone.specialBuilding.name,
                                    cityName = settlementState.value?.name ?: "",
                                    roomName = "Główne pomieszczenie",
                                    currentHour = HeroLogic.getCurrentHour(heroData),
                                    buildingType = currentZone.specialBuilding.type,
                                    rank = currentZone.specialBuilding.rank,
                                    districtType = currentZone.districtType
                                )
                                heroData = result.updatedHeroData
                                saveToFile(fileName, heroData)
                                if (result.updatedHeroProgress != null) {
                                    onHeroProgressChange(result.updatedHeroProgress)
                                    saveToFile("progress_$heroId.json", Json.encodeToString(result.updatedHeroProgress))
                                    saveToFile("dataNow", result.updatedHeroProgress.currentCalendar)
                                }
                                showBurglaryResultDialog = result
                                playSoundAction(R.raw.klik1, 0)
                            }
                        }
                    }
                    // Exit Logic
                    if (isGateZone) {
                        Button(
                            onClick = {
                                val exit = moveHeroToSettlementGate(
                                    heroId = heroId.toIntOrNull() ?: return@Button,
                                    settlementName = settlement?.name ?: cityNameFromCalendar,
                                    exitDirection = currentZoneName,
                                    readFromFile = readFromFile,
                                    saveToFile = saveToFile
                                )
                                if (exit != null) {
                                    val calendarParts = heroProgress.currentCalendar
                                        .split("|")
                                        .map { it.trim() }
                                        .toMutableList()
                                    while (calendarParts.size < 8) calendarParts.add("")
                                    calendarParts[4] = exit.x.toString()
                                    calendarParts[5] = exit.y.toString()
                                    calendarParts[6] = currentZoneName
                                    calendarParts[7] = currentZoneName
                                    val updatedProgress = heroProgress.copy(
                                        currentCalendar = calendarParts.joinToString(" | ")
                                    )
                                    saveToFile("progress_$heroId.json", Json.encodeToString(updatedProgress))
                                    onHeroProgressChange(updatedProgress)
                                }
                                onMenuAppChange(61)
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0C0C0)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(gateDisplayText.uppercase(), color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    SettlementNavigationGrid(
                        settlement = settlementState.value,
                        currentZoneName = currentZoneName,
                        discoveredZones = discoveredZones,
                        onZoneSelected = { changeZone(it) },
                        heroProgress = heroProgress
                    )
                }
            }

            // BOTTOM STATUS BAR
            Surface(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                color = Color(0xFF1A1A1A),
                border = BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Time and Weather
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(weather.icon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            val time = calendarData.split("|").getOrNull(2)?.trim() ?: "--:--"
                            Text(time, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(weather.displayName, color = Color.Gray, fontSize = 9.sp)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(160.dp)) {
                        // Money
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MoneyItem(gold, "zk", Color(0xFFFFD700))
                            MoneyItem(silver, "s", Color(0xFFC0C0C0))
                            MoneyItem(copper, "p", Color(0xFFCD7F32))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Fatigue and Weight
                        val maxFatigue = heroProgress.maxFatigue.coerceAtLeast(100)
                        LinearProgressIndicator(
                            progress = { (currentFatigue.toFloat() / maxFatigue).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = if (currentFatigue > (maxFatigue * 0.7)) Color.Red else Color.Green
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Zmęczenie: ",
                                color = Color.White,
                                fontSize = 7.sp
                            )
                            Text(
                                "$currentFatigue / $maxFatigue",
                                color = if (currentFatigue > (maxFatigue * 0.7)) Color.Red else Color.White,
                                fontSize = 7.sp
                            )

                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${currentWeight.toInt()} / ${weightLimit}㎏",
                                color = encumbrance.color,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Menu Buttons
                //    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(18.dp)) {
                        IconButton(onClick = { onMenuAppChange(10) }) {
                            Text("📜", fontSize = 16.sp)
                        }
                        IconButton(onClick = { onMenuAppChange(21) }) {
                            Text("🎒", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS (Copied from original for functionality) ---
    // Guard Encounter
    showGuardEncounterDialog?.let { encounter ->
        AlertDialog(
            onDismissRequest = { showGuardEncounterDialog = null },
            title = { Text(encounter.title, color = if (encounter.isArrested) Color.Red else Color.Yellow, fontFamily = nosiferFamily) },
            text = {
                Column {
                    Text(encounter.description, color = Color.White)
                    if (encounter.fine > 0) Text("Grzywna: ${HeroLogic.formatPrice(encounter.fine)}", color = Color.Red, fontWeight = FontWeight.Bold)
                    if (encounter.fatigue > 0) Text("Zmęczenie: +${encounter.fatigue}%", color = Color.Yellow)
                    if (encounter.wantedIncrease > 0) Text("List gończy: +${encounter.wantedIncrease}", color = Color(0xFFB71C1C))
                }
            },
            confirmButton = { TextButton(onClick = { showGuardEncounterDialog = null }) { Text("OK", color = Color.White) } },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    // Burglary Result
    showBurglaryResultDialog?.let { result ->
        AlertDialog(
            onDismissRequest = { showBurglaryResultDialog = null },
            title = { Text(if (result.success) "SUKCES!" else if (result.caught) "PRZYŁAPANY!" else "PORAŻKA", color = if (result.success) Color.Green else Color.Red, fontFamily = nosiferFamily) },
            text = {
                Column {
                    Text(result.message, color = Color.White)
                    if (result.success) {
                        Text("Łup: ${HeroLogic.formatPrice(result.lootMoney)}", color = Color(0xFFDAA520), fontWeight = FontWeight.Bold)
                        result.lootItems.forEach { Text("• $it", color = Color.White, fontSize = 12.sp) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBurglaryResultDialog = null }) { Text("OK", color = Color.White) } },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    // Work Slider
    showWorkSliderDialog?.let { (skill, type, baseVal) ->
        var hours by remember { mutableStateOf(4) }
        AlertDialog(
            onDismissRequest = { showWorkSliderDialog = null },
            title = { Text(if(type == "WORK") "PRACA: $skill" else "TRENING: $skill", color = Color.White, fontFamily = nosiferFamily) },
            text = {
                Column {
                    Text("Czas: $hours h", color = Color.White)
                    Slider(
                        value = hours.toFloat(),
                        onValueChange = { hours = it.toInt() },
                        valueRange = 1f..12f,
                        steps = 11,
                        colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
                    )
                    Text(
                        if(type == "WORK") "Przewidywany zarobek: ${HeroLogic.formatPrice(hours * baseVal)}"
                        else "Koszt treningu: ${HeroLogic.formatPrice(hours * baseVal)}",
                        color = Color.Yellow, fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (type == "WORK") {
                        val (newHero, result) = HeroLogic.performWorkSessions(heroData, skill, hours, baseVal, HeroLogic.getStatValue(heroData, "K"), heroProgress = heroProgress)
                        heroData = HeroLogic.stripLegacyProgressTags(newHero)
                        saveToFile(fileName, heroData)
                        result.updatedHeroProgress?.let {
                            onHeroProgressChange(it)
                            saveToFile("progress_$heroId.json", Json.encodeToString(it))
                        }
                        showWorkSessionResultDialog = result
                    } else {
                        val (newHero, result) = HeroLogic.performTrainingSessions(heroData, skill, hours, HeroLogic.getStatValue(heroData, "WW"), baseVal, heroProgress = heroProgress)
                        heroData = HeroLogic.stripLegacyProgressTags(newHero)
                        saveToFile(fileName, heroData)
                        result.updatedHeroProgress?.let {
                            onHeroProgressChange(it)
                            saveToFile("progress_$heroId.json", Json.encodeToString(it))
                        }
                        showTrainingResultDialog = result
                    }
                    showWorkSliderDialog = null
                }) { Text("ROZPOCZNIJ", color = Color.Green, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showWorkSliderDialog = null }) { Text("ANULUJ", color = Color.White) }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    // Work Session Result
    showWorkSessionResultDialog?.let { result ->
        AlertDialog(
            onDismissRequest = { showWorkSessionResultDialog = null },
            title = { Text("WYNIK PRACY", color = Color.Yellow, fontFamily = nosiferFamily) },
            text = {
                Column {
                    Text("Zarobek: ${HeroLogic.formatPrice(result.totalWage)}", color = Color.Green, fontWeight = FontWeight.Bold)
                    Text("PN: +${result.totalPn} ($result.skillName)", color = Color.White)
                    Text("Sukcesy: ${result.successes}/${result.sessionsPerformed}", color = Color.Gray)
                    if (result.criticalSuccesses > 0) Text("Krytyczne sukcesy: ${result.criticalSuccesses}", color = Color.Cyan)
                    if (result.criticalFailures > 0) Text("Krytyczne porażki: ${result.criticalFailures}", color = Color.Red)
                    if (result.interruptedByFatigue) Text("PRZERWANO Z POWODU WYCZERPANIA!", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = { TextButton(onClick = { showWorkSessionResultDialog = null }) { Text("OK", color = Color.White) } },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    // Training Result
    showTrainingResultDialog?.let { result ->
        AlertDialog(
            onDismissRequest = { showTrainingResultDialog = null },
            title = { Text("WYNIK TRENINGU", color = Color.Cyan, fontFamily = nosiferFamily) },
            text = {
                Column {
                    Text("PN: +${result.totalPn} ($result.skillName)", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Sukcesy: ${result.successes}/${result.sessionsPerformed}", color = Color.Gray)
                    if (result.criticalSuccesses > 0) Text("Krytyczne sukcesy: ${result.criticalSuccesses}", color = Color.Cyan)
                    if (result.criticalFailures > 0) Text("Krytyczne porażki: ${result.criticalFailures}", color = Color.Red)
                    if (result.fatigueInterrupted) Text("PRZERWANO Z POWODU ZMĘCZENIA!", color = Color.Red, fontWeight = FontWeight.Bold)
                    Text(result.message, color = Color.LightGray, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showTrainingResultDialog = null }) { Text("OK", color = Color.White) } },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    // Luck Result
    showLuckResultDialog?.let { msg ->
        AlertDialog(
            onDismissRequest = { showLuckResultDialog = null },
            title = { Text("LOS", color = Color.White, fontFamily = nosiferFamily) },
            text = { Text(msg, color = Color.White) },
            confirmButton = { TextButton(onClick = { showLuckResultDialog = null }) { Text("OK", color = Color.White) } },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    if (showTalkDialog) {
        val skillGadaninaInfo = HeroLogic.getSkillInfo(heroData, "Gadanina")
        val skillUrokInfo = HeroLogic.getSkillInfo(heroData, "Urok osobisty")
        val skillKrasomostwoInfo = HeroLogic.getSkillInfo(heroData, "Krasomóstwo")

        fun getModifier(rank: String): Int {
            return when (rank) {
                "Adept" -> 5
                "Ekspert" -> 10
                "Mistrz" -> 15
                "Arcymistrz" -> 20
                else -> 0
            }
        }

        val valGadanina = if (skillGadaninaInfo.rank != "Brak") skillGadaninaInfo.value + getModifier(skillGadaninaInfo.rank) else 0
        val valUrok = if (skillUrokInfo.rank != "Brak") skillUrokInfo.value + getModifier(skillUrokInfo.rank) else 0
        val valKrasomostwo = if (skillKrasomostwoInfo.rank != "Brak") skillKrasomostwoInfo.value + getModifier(skillKrasomostwoInfo.rank) else 0

        val bestSkillValue = maxOf(valGadanina, valUrok, valKrasomostwo)
        val allSkillsBrak = skillGadaninaInfo.rank == "Brak" && skillUrokInfo.rank == "Brak" && skillKrasomostwoInfo.rank == "Brak"
        val effectiveSuccessChance = if (allSkillsBrak) 20 else bestSkillValue
        
        AlertDialog(
            onDismissRequest = { showTalkDialog = false },
            containerColor = Color(0xFF0D251A),
            title = { Text("ROZMOWA Z MIEJSCOWYMI", color = Color.Green, fontFamily = nosiferFamily) },
            text = {
                Column {
                    Text("Próbujesz wyciągnąć informacje od napotkanych osób.", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (allSkillsBrak) {
                        Text("Nie posiadasz umiejętności towarzyskich, polegasz na szczęściu.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        Text("Twoje umiejętności socjalne (z modyfikatorami rang):", color = Color.Gray, fontSize = 12.sp)
                        if (valGadanina > 25) {
                            Text("• Gadanina: $valGadanina% (${skillGadaninaInfo.rank})", color = Color.White, fontSize = 13.sp) }
                        if (valUrok > 25) {
                            Text("• Urok osobisty: $valUrok% (${skillUrokInfo.rank})", color = Color.White, fontSize = 13.sp)
                        }
                        if (valKrasomostwo > 25){
                            Text("• Krasomóstwo: $valKrasomostwo% (${skillKrasomostwoInfo.rank})", color = Color.White, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Szansa powodzenia: $effectiveSuccessChance%", color = Color.Yellow)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val roll = (1..100).random()
                    if (roll <= effectiveSuccessChance) {
                        if (settlementState.value != null) {
                            val currentZone = settlement?.zones?.find { it.name == currentZoneName }
                            val hiddenBuildings = if (currentZone != null) {
                                val inCurrent = currentZone.extraBuildings.filter { !it.discovered }.map { it to currentZone } +
                                               (if (!currentZone.inn.discovered) listOf(currentZone.inn to currentZone) else emptyList()) +
                                               (if (!currentZone.specialBuilding.discovered) listOf(currentZone.specialBuilding to currentZone) else emptyList()) +
                                               (if (currentZone.districtType == DistrictType.MERCHANT && !currentZone.hasFountain) listOf(BuildingInstance(type = "Fontanna", name = "Miejska Fontanna", rank = 1, rooms = emptyList(), totalArea = 0.0, discovered = true) to currentZone) else emptyList()) +
                                               (if ((currentZone.districtType == DistrictType.RESIDENTIAL || currentZone.districtType == DistrictType.SLUMS) && !currentZone.hasWell) listOf(BuildingInstance(type = "Studnia", name = "Miejska Studnia", rank = 1, rooms = emptyList(), totalArea = 0.0, discovered = true) to currentZone) else emptyList())
                                
                                if (inCurrent.isNotEmpty()) inCurrent else {
                                    settlement.zones.flatMap { z ->
                                        z.extraBuildings.filter { !it.discovered }.map { it to z } +
                                        (if (!z.inn.discovered) listOf(z.inn to z) else emptyList()) +
                                        (if (!z.specialBuilding.discovered) listOf(z.specialBuilding to z) else emptyList()) +
                                        (if (z.districtType == DistrictType.MERCHANT && !z.hasFountain) listOf(BuildingInstance(type = "Fontanna", name = "Miejska Fontanna", rank = 1, rooms = emptyList(), totalArea = 0.0, discovered = true) to z) else emptyList()) +
                                        (if ((z.districtType == DistrictType.RESIDENTIAL || z.districtType == DistrictType.SLUMS) && !z.hasWell) listOf(BuildingInstance(type = "Studnia", name = "Miejska Studnia", rank = 1, rooms = emptyList(), totalArea = 0.0, discovered = true) to z) else emptyList())
                                    }
                                }
                            } else {
                                settlement!!.zones.flatMap { z ->
                                    z.extraBuildings.filter { !it.discovered }.map { it to z } +
                                    (if (!z.inn.discovered) listOf(z.inn to z) else emptyList()) +
                                    (if (!z.specialBuilding.discovered) listOf(z.specialBuilding to z) else emptyList()) +
                                    (if (z.districtType == DistrictType.MERCHANT && !z.hasFountain) listOf(BuildingInstance(type = "Fontanna", name = "Miejska Fontanna", rank = 1, rooms = emptyList(), totalArea = 0.0, discovered = true) to z) else emptyList()) +
                                    (if ((z.districtType == DistrictType.RESIDENTIAL || z.districtType == DistrictType.SLUMS) && !z.hasWell) listOf(BuildingInstance(type = "Studnia", name = "Miejska Studnia", rank = 1, rooms = emptyList(), totalArea = 0.0, discovered = true) to z) else emptyList())
                                }
                            }
                            
                            if (hiddenBuildings.isNotEmpty()) {
                                val (b, zone) = hiddenBuildings.random()
                                val updatedZones = settlement!!.zones.map { z ->
                                    if (z.id == zone.id) {
                                        z.copy(
                                            inn = if (z.inn.name == b.name) z.inn.copy(discovered = true) else z.inn,
                                            specialBuilding = if (z.specialBuilding.name == b.name) z.specialBuilding.copy(discovered = true) else z.specialBuilding,
                                            extraBuildings = z.extraBuildings.map { if (it.name == b.name) it.copy(discovered = true) else it },
                                            hasFountain = if (b.type == "Fontanna") true else z.hasFountain,
                                            hasWell = if (b.type == "Studnia") true else z.hasWell
                                        )
                                    } else z
                                }
                                
                                val updatedSettlement = settlement!!.copy(zones = updatedZones)
                                WorldPersistence.saveSettlement(context, updatedSettlement)

                                val calendar = heroProgress.currentCalendar
                                val parts = calendar.split("|").map { it.trim() }.toMutableList()
                                while (parts.size < 4) parts.add("")
                                parts[3] = "[EXTERNAL]"
                                
                                val newCalendar = parts.joinToString(" | ")
                                var updatedProgress = heroProgress.copy(currentCalendar = newCalendar)
                                updatedProgress = HeroLogic.advanceTimeMinutesProgress(updatedProgress, talkDuration)
                                onHeroProgressChange(updatedProgress)
                                saveToFile("dataNow", updatedProgress.currentCalendar)
                                
                                Toast.makeText(context, "Dowiedziałeś się o: ${b.name} (${zone.name})!", Toast.LENGTH_LONG).show()
                            } else {
                                // 2. Try zones
                                val cityNameForDiscovery = settlement.name
                                val discovered = heroProgress.discoveredZones[cityNameForDiscovery] ?: emptySet()
                                val hiddenZones = settlement.zones.filter { !discovered.contains(it.name) }
                                
                                if (hiddenZones.isNotEmpty()) {
                                    val z = hiddenZones.random()
                                    val updatedSettlementZones = discovered + z.name
                                    val updatedDiscoveredZones = heroProgress.discoveredZones.toMutableMap()
                                    updatedDiscoveredZones[cityNameForDiscovery] = updatedSettlementZones
                                    
                                    var updatedProgress = heroProgress.copy(discoveredZones = updatedDiscoveredZones)
                                    
                                    // Logika automatycznego poznania nazwy miasta przy 4 odkrytych dzielnicach lub udanej rozmowie o nazwie miasta
                                    val shouldDiscoverCityName = updatedSettlementZones.size >= 4 || (1..100).random() <= 40
                                    if (shouldDiscoverCityName) {
                                        val calendar = updatedProgress.currentCalendar
                                        val parts = calendar.split("|").map { it.trim() }.toMutableList()
                                        if (parts.isNotEmpty() && (parts[0] == "Nieznane Miasto" || parts[0] == "Nieznane Miejsce" || parts[0].isEmpty())) {
                                            parts[0] = cityNameForDiscovery
                                            updatedProgress = updatedProgress.copy(currentCalendar = parts.joinToString(" | "))
                                            Toast.makeText(context, "Mieszkaniec wygadał że miasto nazywa się $cityNameForDiscovery!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Ktoś opowiedział ci o dzielnicy: ${z.name}!", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Ktoś opowiedział ci o dzielnicy: ${z.name}!", Toast.LENGTH_LONG).show()
                                    }
                                    
                                    updatedProgress = HeroLogic.advanceTimeMinutesProgress(updatedProgress, talkDuration)
                                    onHeroProgressChange(updatedProgress)
                                    saveToFile("dataNow", updatedProgress.currentCalendar)
                                } else {
                                    Toast.makeText(context, "Wszyscy mówią to samo. Nic nowego.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        val updatedProgress = HeroLogic.advanceTimeMinutesProgress(heroProgress, talkDuration)
                        onHeroProgressChange(updatedProgress)
                        saveToFile("dataNow", updatedProgress.currentCalendar)
                        Toast.makeText(context, "Mieszkańcy Cię ignorują.", Toast.LENGTH_SHORT).show()
                    }
                    showTalkDialog = false
                }) { Text("ROZMAWIAJ") }
            },
            dismissButton = { TextButton(onClick = { showTalkDialog = false }) { Text("ANULUJ") } }
        )
    }

    if (showDiscoveryDialog) {
        AlertDialog(
            onDismissRequest = { showDiscoveryDialog = false },
            containerColor = Color(0xFF251A0D),
            title = { Text("ODKRYWANIE OKOLICY", color = Color.Cyan, fontFamily = nosiferFamily) },
            text = { Text("Chcesz spędzić $discoveryDuration minut na badaniu zakamarków dzielnicy?", color = Color.White) },
            confirmButton = {
                Button(onClick = {
                    var updatedProgress = HeroLogic.advanceTimeMinutesProgress(heroProgress, discoveryDuration)
                    
                    val roll = Random.nextDouble()
                    val successChance = 0.4
                    
                    if (settlement != null && roll < successChance) {
                        val currentZone = settlement.zones.find { it.name == currentZoneName }
                        val hidden = if (currentZone != null) {
                            val inCurrent = currentZone.extraBuildings.filter { !it.discovered }.map { it to currentZone } +
                                           (if (!currentZone.inn.discovered) listOf(currentZone.inn to currentZone) else emptyList()) +
                                           (if (!currentZone.specialBuilding.discovered) listOf(currentZone.specialBuilding to currentZone) else emptyList()) +
                                           (if (currentZone.districtType == DistrictType.MERCHANT && !currentZone.hasFountain) listOf(BuildingInstance(type = "Fontanna", name = "Miejska Fontanna", rank = 1, rooms = emptyList(), totalArea = 0.0, discovered = true) to currentZone) else emptyList()) +
                                           (if ((currentZone.districtType == DistrictType.RESIDENTIAL || currentZone.districtType == DistrictType.SLUMS) && !currentZone.hasWell) listOf(BuildingInstance(type = "Studnia", name = "Miejska Studnia", rank = 1, rooms = emptyList(), totalArea = 0.0, discovered = true) to currentZone) else emptyList())
                            
                            if (inCurrent.isNotEmpty()) inCurrent else {
                                settlement.zones.flatMap { z ->
                                    z.extraBuildings.filter { !it.discovered }.map { it to z } +
                                    (if (!z.inn.discovered) listOf(z.inn to z) else emptyList()) +
                                    (if (!z.specialBuilding.discovered) listOf(z.specialBuilding to z) else emptyList()) +
                                    (if (z.districtType == DistrictType.MERCHANT && !z.hasFountain) listOf(BuildingInstance(type = "Fontanna", name = "Miejska Fontanna", rank = 1, rooms = emptyList(), totalArea = 0.0, discovered = true) to z) else emptyList()) +
                                    (if ((z.districtType == DistrictType.RESIDENTIAL || z.districtType == DistrictType.SLUMS) && !z.hasWell) listOf(BuildingInstance(type = "Studnia", name = "Miejska Studnia", rank = 1, rooms = emptyList(), totalArea = 0.0, discovered = true) to z) else emptyList())
                                }
                            }
                        } else {
                            settlement.zones.flatMap { z ->
                                z.extraBuildings.filter { !it.discovered }.map { it to z } +
                                (if (!z.inn.discovered) listOf(z.inn to z) else emptyList()) +
                                (if (!z.specialBuilding.discovered) listOf(z.specialBuilding to z) else emptyList()) +
                                (if (z.districtType == DistrictType.MERCHANT && !z.hasFountain) listOf(BuildingInstance(type = "Fontanna", name = "Miejska Fontanna", rank = 1, rooms = emptyList(), totalArea = 0.0, discovered = true) to z) else emptyList()) +
                                (if ((z.districtType == DistrictType.RESIDENTIAL || z.districtType == DistrictType.SLUMS) && !z.hasWell) listOf(BuildingInstance(type = "Studnia", name = "Miejska Studnia", rank = 1, rooms = emptyList(), totalArea = 0.0, discovered = true) to z) else emptyList())
                            }
                        }

                        if (hidden.isNotEmpty()) {
                            val (b, zone) = hidden.random()
                            val updatedZones = settlement.zones.map { z ->
                                if (z.id == zone.id) {
                                    z.copy(
                                        inn = if (z.inn.name == b.name) z.inn.copy(discovered = true) else z.inn,
                                        specialBuilding = if (z.specialBuilding.name == b.name) z.specialBuilding.copy(discovered = true) else z.specialBuilding,
                                        extraBuildings = z.extraBuildings.map { if (it.name == b.name) it.copy(discovered = true) else it },
                                        hasFountain = if (b.type == "Fontanna") true else z.hasFountain,
                                        hasWell = if (b.type == "Studnia") true else z.hasWell
                                    )
                                } else z
                            }
                            val updatedSettlement = settlement.copy(zones = updatedZones)
                            WorldPersistence.saveSettlement(context, updatedSettlement)

                            val calendar = updatedProgress.currentCalendar
                            val parts = calendar.split("|").map { it.trim() }.toMutableList()
                            while (parts.size < 4) parts.add("")
                            parts[3] = "[EXTERNAL]"
                            val newCalendar = parts.joinToString(" | ")
                            updatedProgress = updatedProgress.copy(currentCalendar = newCalendar)
                            
                            Toast.makeText(context, "Znalazłeś: ${b.name}!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Przeszukałeś okolicę, ale nie znalazłeś nic nowego.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Przeszukałeś okolicę, ale nie znalazłeś nic nowego.", Toast.LENGTH_SHORT).show()
                    }
                    
                    onHeroProgressChange(updatedProgress)
                    saveToFile("dataNow", updatedProgress.currentCalendar)
                    showDiscoveryDialog = false
                }) { Text("EKSPLORUJ") }
            },
            dismissButton = { TextButton(onClick = { showDiscoveryDialog = false }) { Text("ANULUJ") } }
        )
    }
}

@Composable
private fun SettlementNavigationGrid(
    settlement: SettlementInstance?,
    currentZoneName: String,
    discoveredZones: Set<String>,
    onZoneSelected: (String) -> Unit,
    heroProgress: HeroProgress? = null
) {
    val currentFatigue = heroProgress?.fatigue ?: 0
    val maxFatigue = (heroProgress?.maxFatigue ?: 100).coerceAtLeast(100)
    val isTooTired = currentFatigue >= (maxFatigue * 0.9).toInt()
    
    Column(
        modifier = Modifier.padding(16.dp).widthIn(max = 300.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isTooTired) {
            Text(
                "⚠️ ZBYT ZMĘCZONY!",
                color = Color.Red,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Podróż po dzielnicach niemożliwa.",
                color = Color.Red,
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            val cityGridOrder = listOf(
                "Północny-Zachód", "Północ", "Północny-Wschód",
                "Zachód", "Centrum", "Wschód",
                "Południowy-Zachód", "Południe", "Południowy-Wschód"
            )
            val directionalOrder = listOf("Północ", "Zachód", "Centrum", "Wschód", "Południe")
            val zones = settlement?.zones.orEmpty()
            val zoneNames = when (settlement?.type) {
                SettlementType.CITY -> cityGridOrder.filter { name -> zones.any { it.name == name } }
                else -> directionalOrder.filter { name -> zones.any { it.name == name } }
            }
            val neighbors = settlement?.let {
                SettlementGenerator.calculateNeighbors(currentZoneName, it.type, it.gateDirections)
            }.orEmpty()
            
            Text(
                "EKSPLORACJA: ${settlement?.type?.displayName?.uppercase() ?: "OSADA"}",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                zoneNames.chunked(3).forEach { rowZones ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowZones.forEach { zoneName ->
                            val zone = zones.first { it.name == zoneName }
                            val isDiscovered = zoneName in discoveredZones
                            val isCurrent = zoneName == currentZoneName
                            val isNeighbor = zoneName in neighbors
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(
                                        if (isCurrent) Color.Green.copy(alpha = 0.2f)
                                        else if (isNeighbor) Color.DarkGray.copy(alpha = 0.5f)
                                        else Color(0xFF1A1A1A),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        width = if (isCurrent) 2.dp else 1.dp,
                                        color = if (isCurrent) Color.Green else if (isNeighbor) Color.Gray else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable(enabled = isNeighbor) { onZoneSelected(zoneName) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        zoneName.replace("-", "\n"),
                                        color = if (isDiscovered) Color.White else Color.Gray,
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 10.sp
                                    )
                                    Text(
                                        zone.districtType.displayName,
                                        color = getDistrictColor(zone.districtType).copy(alpha = if (isDiscovered) 1f else 0.6f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        lineHeight = 12.sp
                                    )
                                    if (!isDiscovered) Text("👣", fontSize = 8.sp)
                                }
                            }
                        }
                        repeat(3 - rowZones.size) {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

private fun getDistrictColor(type: DistrictType): Color = when (type) {
    DistrictType.MERCHANT -> Color(0xFFDAA520)
    DistrictType.MILITARY -> Color(0xFF8B0000)
    DistrictType.MAGIC, DistrictType.SACRED -> Color(0xFF1976D2)
    DistrictType.CRAFT -> Color(0xFF8B4513)
    DistrictType.ECONOMIC -> Color(0xFF558B2F)
    DistrictType.RULER_SEAT -> Color(0xFFC0C0C0)
    DistrictType.SLUMS -> Color(0xFF4E342E)
    DistrictType.HARBOR -> Color(0xFF006064)
    else -> Color.Gray
}

private fun hasCategory(buildings: List<BuildingInstance>, category: String): Boolean = buildings.any { b ->
    val t = b.type.uppercase()
    when(category) {
        "SERVICE" -> t.contains("KARCZMA") || t.contains("GOSPODA") || t.contains("TAWERNA") || t.contains("HOTEL") || t.contains("MEDYK") || t.contains("ŁAŹNIA")
        "TRADE" -> t.contains("KOWAL") || t.contains("RYNEK") || t.contains("WARSZTAT") || t.contains("SKLEP") || t.contains("PŁATNERZ") || t.contains("RYMARZ")
        "MILITARY" -> t.contains("KOSZARY") || t.contains("ARENA") || t.contains("GARNIZON")
        "SCIENCE" -> t.contains("UNIWERSYTET") || t.contains("ŚWIĄTYNIA") || t.contains("BIBLIOTEKA")
        else -> false
    }
}

private fun hasSpecialType(buildings: List<BuildingInstance>, types: List<String>): Boolean = buildings.any { b ->
    types.any { type -> b.type.uppercase().contains(type) }
}

@Composable
fun ActionCategoryHeader(text: String, color: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp, top = 8.dp)) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        HorizontalDivider(color = color.copy(alpha = 0.5f), thickness = 1.dp)
    }
}

@Composable
fun MoneyItem(value: Int, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("⛀", color = color, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(2.dp))
        Text("$value $label", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CityHubButton(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 12.sp)
    }
}
