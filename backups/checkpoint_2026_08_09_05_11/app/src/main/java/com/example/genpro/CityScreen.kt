package com.example.genpro

import android.content.Context
import android.content.Intent
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

@Composable
fun CityScreen(
    selectedHeroId: Int,
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
    val fileName = "hero_$selectedHeroId"
    var heroData by remember { mutableStateOf(readFromFile(fileName)) }

    val lines = remember(heroData) { heroData.lines() }

    val settlement = remember(settlementData) {
        if (settlementData.isNotEmpty()) {
            try {
                Json.decodeFromString<SettlementInstance>(settlementData)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    var currentZoneName by remember(settlement) {
        val calendar = HeroLogic.getCalendarData(heroData)
        val parts = calendar.split("|").map { it.trim() }
        val zoneFromCalendar = if (parts.size >= 7) parts[6] else ""
        
        // Jeśli mamy dane osady, a strefa w kalendarzu jest pusta, weźmy pierwszą strefę
        val initialZone = if (zoneFromCalendar.isEmpty() && settlement != null) {
            settlement.zones.firstOrNull()?.name ?: ""
        } else {
            zoneFromCalendar
        }
        mutableStateOf(initialZone)
    }

    // Synchronizacja strefy z plikiem przy każdej zmianie (również przy pierwszym wejściu)
    LaunchedEffect(currentZoneName) {
        if (currentZoneName.isEmpty()) return@LaunchedEffect
        
        val calendar = HeroLogic.getCalendarData(heroData)
        val parts = calendar.split("|").map { it.trim() }
        val zoneInFile = if (parts.size >= 7) parts[6] else ""
        
        if (currentZoneName != zoneInFile) {
            val newParts = parts.toMutableList()
            while (newParts.size < 7) newParts.add("")
            newParts[6] = currentZoneName
            
            val newCalendar = newParts.joinToString(" | ")
            val updated = HeroLogic.updateCalendarData(heroData, newCalendar)
            
            saveToFile(fileName, updated)
            saveToFile("dataNow", newCalendar)
            heroData = updated
        }
    }

    // Pomocnicza funkcja do aktualizacji strefy w pliku
    fun changeZone(newZoneName: String) {
        val oldCalendar = HeroLogic.getCalendarData(heroData)
        val parts = oldCalendar.split("|").map { it.trim() }.toMutableList()
        
        // Upewniamy się, że mamy odpowiednią liczbę segmentów
        while (parts.size < 7) parts.add("")
        parts[6] = newZoneName
        
        val newCalendar = parts.joinToString(" | ")
        val updatedHeroData = HeroLogic.updateCalendarData(heroData, newCalendar)
        
        saveToFile(fileName, updatedHeroData)
        saveToFile("dataNow", newCalendar)
        heroData = updatedHeroData
        currentZoneName = newZoneName
        playSoundAction(R.raw.klik1, 0)
    }

    var showTrainingDialog by remember { mutableStateOf<String?>(null) }
    var showCombatTrainingDialog by remember { mutableStateOf(false) }
    var combatTrainingSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastRefreshTime by remember { mutableLongStateOf(0L) }
    var showShopSelection by remember { mutableStateOf(false) }
    var showSchoolSelection by remember { mutableStateOf(false) }
    var showLanguageSchoolDialog by remember { mutableStateOf(false) }
    var languageSchoolSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    var showUniversityDialog by remember { mutableStateOf(false) }
    var universitySkillsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastSchoolRefreshTime by remember { mutableLongStateOf(0L) }
    var showLearningDialog by remember { mutableStateOf(false) }
    var learningSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedUniversitySkill by remember { mutableStateOf<String?>(null) }
    var showWorkResultDialog by remember { mutableStateOf<HeroLogic.WorkResult?>(null) }
    var showWorkSessionResultDialog by remember { mutableStateOf<HeroLogic.WorkSessionResult?>(null) }
    var showWorkSliderDialog by remember { mutableStateOf<Triple<String, String, Int>?>(null) } // SkillName, JobType, ClosingHour

    val infoLine1 = lines.find { it.contains("Rasa:") && it.contains("Klasa:") } ?: ""
    val klasaRaw = infoLine1.substringAfter("Klasa: ").trim()
    val klasa = if (klasaRaw.contains("(")) klasaRaw.substringBefore("(").trim() else klasaRaw

    val weightLimit = getHeroWeightLimit(heroData)
    val currentWeight = calculateCurrentWeight(heroData)
    val encumbrance = getEncumbrance(currentWeight, weightLimit)

    val craftSkills = remember(heroData) {
        val skillsHeaderIdx = lines.indexOfFirst { it.startsWith("UMIEJĘTNOŚCI (GRUPY):") }
        if (skillsHeaderIdx == -1) emptyList<String>()
        else {
            val result = mutableListOf<String>()
            val craftRegex = Regex("•\\s+Rzemiosło:\\s+([^|\\s(]+)")
            for (i in skillsHeaderIdx + 1 until lines.size) {
                if (lines[i].startsWith("CECHY FIZYCZNE:") || lines[i].startsWith("CECHY CHARAKTERU:")) break
                val match = craftRegex.find(lines[i])
                if (match != null) {
                    result.add(match.groupValues[1].trim())
                }
            }
            result
        }
    }

    // --- REFRESH COMBAT SKILLS EVERY 1 MINUTE ---
    LaunchedEffect(showCombatTrainingDialog) {
        if (showCombatTrainingDialog) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRefreshTime > 60000 || combatTrainingSkills.isEmpty()) {
                val wwCount = when (klasa) {
                    "Wojownik" -> 3
                    "Łowca" -> 1
                    else -> 2
                }
                val usCount = when (klasa) {
                    "Wojownik" -> 1
                    "Łowca" -> 3
                    else -> 2
                }
                val poolWW = HeroLogic.skillWW.toList().shuffled().take(wwCount)
                val poolUS = HeroLogic.skillUS.toList().shuffled().take(usCount)
                combatTrainingSkills = (poolWW + poolUS).shuffled()
                lastRefreshTime = currentTime
            }
        }
    }

    // --- REFRESH SCHOOL SKILLS EVERY 1 MINUTE ---
    LaunchedEffect(showLanguageSchoolDialog, showUniversityDialog, showLearningDialog) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSchoolRefreshTime > 60000) {
            // Nauka i Trening (Ogólne)
            learningSkills = HeroLogic.allNormalSkills.toList().shuffled().take(6)
            
            // Szkoła Języków
            val langPool = listOf("Czytanie i pisanie") + HeroLogic.skillJezyk.toList()
            languageSchoolSkills = langPool.shuffled().take(4)
            
            // Uniwersytet
            val uniPool = listOf("Matematyka", "Prawo", "Teologia", "Medycyna", "Astronomia", "Chemia", "Farmacja", "Fizyka", "Kartografia", "Kryptografia")
            universitySkillsList = uniPool.shuffled().take(4)
            
            lastSchoolRefreshTime = currentTime
        }
    }
    val name = lines.find { 
        it.startsWith("Imię:") || it.startsWith("Bohater:") || 
        (!it.contains(":") && it.isNotBlank() && !it.startsWith(" ")) 
    }?.substringAfter(":")?.trim() ?: "Bohater"
    
    val pdLine = lines.find { it.trim().startsWith("PD:") } ?: "PD: 0"
    val pd = pdLine.substringAfter("PD:").trim().toIntOrNull() ?: 0

    val levelLine = lines.find { it.trim().startsWith("Poziom:") } ?: "Poziom: 1"
    val level = levelLine.substringAfter("Poziom:").trim().toIntOrNull() ?: 1

    // Work Session Result Dialog
    showWorkSessionResultDialog?.let { result ->
        AlertDialog(
            onDismissRequest = { showWorkSessionResultDialog = null },
            title = { Text("Podsumowanie pracy: ${result.skillName}", color = Color.White) },
            text = {
                Column {
                    Text("Przepracowano godzin: ${result.sessionsPerformed * 2}", color = Color.LightGray)
                    Text("Wykonano testów: ${result.sessionsPerformed}", color = Color.LightGray)
                    Text("Udane testy: ${result.successes}", color = Color.Green)
                    Spacer(modifier = Modifier.height(8.dp))
                    val wageFormatted = HeroLogic.formatPrice(result.totalWage)
                    Text("Zdobyto monet: $wageFormatted", color = Color(0xFFDAA520), fontWeight = FontWeight.Bold)
                    Text("Zdobyto PN: ${result.totalPn}", color = Color(0xFF1E88E5), fontWeight = FontWeight.Bold)
                    
                    if (result.interruptedByFatigue) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("PRACA PRZERWANA!", color = Color.Red, fontWeight = FontWeight.ExtraBold)
                        Text("Zasłabłeś z wycieńczenia. Straciłeś część zarobku i nie dokończyłeś zmiany.", color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWorkSessionResultDialog = null }) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = Color(0xFF2E2E2E)
        )
    }

    // Work Slider Dialog
    showWorkSliderDialog?.let { (skill, jobType, closingHour) ->
        var workHours by remember { mutableStateOf(1f) }
        val currentHourInt = HeroLogic.getCurrentHour(heroData)
        val maxAvailable = (closingHour - currentHourInt).coerceIn(1, 10)

        AlertDialog(
            onDismissRequest = { showWorkSliderDialog = null },
            title = { Text("Ile godzin chcesz pracować?", color = Color.White) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Budynek otwarty do: $closingHour:00", color = Color.LightGray)
                    Text("Aktualna godzina: $currentHourInt:00", color = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("${workHours.toInt()} h", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = workHours,
                        onValueChange = { workHours = it },
                        valueRange = 1f..maxAvailable.toFloat(),
                        steps = if (maxAvailable > 1) maxAvailable - 2 else 0
                    )
                    val baseWage = if (jobType == "Rzemiosło") 12 + (HeroLogic.getStatValue(heroData, "Int") / 10) else 10
                    val totalBase = (workHours.toInt() * baseWage)
                    val totalBaseFormatted = HeroLogic.formatPrice(totalBase)
                    
                    val fatiguePerH = if (jobType == "Rzemiosło") 7 else 5
                    val predictedFatigue = workHours.toInt() * fatiguePerH
                    
                    Text("Bazowy zarobek: $totalBaseFormatted", color = Color.LightGray)
                    Text("Przewidywane zmęczenie: +$predictedFatigue%", color = Color.Yellow, fontSize = 12.sp)
                    Text("(Możliwe premie za sukcesy w testach)", color = Color.Gray, fontSize = 10.sp)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hours = workHours.toInt()
                        val baseWage = if (jobType == "Rzemiosło") 12 + (HeroLogic.getStatValue(heroData, "Int") / 10) else 10
                        val intelligence = HeroLogic.getStatValue(heroData, "Int")
                        val fatiguePerH = if (jobType == "Rzemiosło") 7 else 5

                        val (updatedData, result) = HeroLogic.performWorkSessions(
                            heroData,
                            if (jobType == "Rzemiosło") "Rzemiosło: $skill" else skill,
                            hours,
                            baseWage,
                            intelligence,
                            fatiguePerH
                        )

                        saveToFile(fileName, updatedData)
                        heroData = updatedData
                        showWorkSessionResultDialog = result
                        showWorkSliderDialog = null
                        playSoundAction(R.raw.klik1, 0)
                    },
                    enabled = HeroLogic.getHeroFatigue(heroData) < 91
                ) {
                    val label = if (HeroLogic.getHeroFatigue(heroData) >= 91) "ZBYT ZMĘCZONY" else "ROZPOCZNIJ PRACĘ"
                    Text(label, color = if (HeroLogic.getHeroFatigue(heroData) >= 91) Color.Gray else Color.Green)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWorkSliderDialog = null }) {
                    Text("ANULUJ", color = Color.White)
                }
            },
            containerColor = Color(0xFF2E2E2E)
        )
    }

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            val headerName = settlement?.name ?: lines.find { it.startsWith("Lokalizacja:") }?.substringAfter(":")?.trim() ?: "MIASTO MROKU"
            Text(
                headerName.uppercase(),
                color = Color.Red,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = nosiferFamily,
                textAlign = TextAlign.Center
            )
            
            Text(
                "Witaj, $name | $pd PD | Poziom $level",
                color = Color.Gray,
                fontSize = 14.sp,
                fontFamily = slaboFamily
            )

            val currentHourHeader = HeroLogic.getCurrentHour(heroData)
            val currentFatigue = HeroLogic.getHeroFatigue(heroData)
            val fatigueColor = when {
                currentFatigue > 60 -> Color.Red
                currentFatigue > 30 -> Color.Yellow
                else -> Color.Green
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Godzina: $currentHourHeader:00",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Zmęczenie: $currentFatigue%",
                    color = fatigueColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Encumbrance Status
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Obciążenie: $currentWeight/$weightLimit",
                    color = encumbrance.color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    encumbrance.label,
                    color = encumbrance.color,
                    fontSize = 10.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (settlement != null) {
                val currentZone = settlement.zones.find { it.name == currentZoneName } ?: settlement.zones.firstOrNull()
                
                if (currentZone != null) {
                    val zoneColor = when (currentZone.districtType) {
                        DistrictType.MERCHANT -> Color(0xFFDAA520)
                        DistrictType.MILITARY -> Color(0xFF8B0000)
                        DistrictType.MAGIC, DistrictType.SACRED -> Color(0xFF1976D2)
                        DistrictType.CRAFT -> Color(0xFF8B4513)
                        DistrictType.RULER_SEAT -> Color(0xFFC0C0C0)
                        else -> Color.Gray
                    }

                    Text(
                        currentZone.districtType.displayName.uppercase(),
                        color = zoneColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,  //Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val specialBuilding = currentZone.specialBuilding
                    val typeUpper = specialBuilding.type.uppercase()
                    val buildingName = specialBuilding.name
                    val displayBuildingLabel = if (buildingName.contains(specialBuilding.type, ignoreCase = true)) {
                        buildingName
                    } else {
                        "${specialBuilding.type}: $buildingName"
                    }

                    // --- DZIAŁ: USŁUGI I GOŚCINA ---

                    CityHubButton("USŁUGI I GOŚCINA", Color(0xFF5D4037)) {
                        playSoundAction(R.raw.klik1, 0)
                        onShopTypeChange("LODGING_DIRECT:${currentZone.specialBuilding.type}:${currentZone.specialBuilding.name}")
                        onMenuAppChange(22)
                    }
                    
                    if (typeUpper.contains("MEDYK") || typeUpper.contains("APTEKA") || typeUpper.contains("LECZ")) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CityHubButton(displayBuildingLabel, Color(0xFF2E7D32)) {
                            playSoundAction(R.raw.klik1, 0)
                            val intent = Intent(context, CombatActivity::class.java).apply {
                                putExtra("SELECTED_HERO_ID", selectedHeroId)
                                putExtra("INITIAL_TAB", 1)
                                putExtra("MEDIC_ONLY", true)
                            }
                            context.startActivity(intent)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- DZIAŁ: HANDEL I RZEMIOSŁO ---
                    if (typeUpper.contains("KOWAL") || typeUpper.contains("ZBROJ") || typeUpper.contains("PŁATNERZ") || 
                        typeUpper.contains("RYMARZ") || typeUpper.contains("SKÓR") || typeUpper.contains("HANDEL") ||
                        typeUpper.contains("RYNEK") || typeUpper.contains("STAJNIA") || typeUpper.contains("DOKI") ||
                        typeUpper.contains("WARSZTAT") || typeUpper.contains("PRACOWNIA") || typeUpper.contains("MANUFAKTURA") ||
                        typeUpper.contains("SKLEP")) {
                        
                        CityHubButton("HANDEL I RZEMIOSŁO", Color(0xFFDAA520)) {
                            playSoundAction(R.raw.klik1, 0)
                            val shopType = when {
                                typeUpper.contains("KOWAL") || typeUpper.contains("ZBROJ") || typeUpper.contains("PŁATNERZ") -> "ZBROJMISTRZ"
                                typeUpper.contains("RYMARZ") || typeUpper.contains("SKÓR") -> "RYMARZ"
                                typeUpper.contains("STAJNIA") -> "STAJNIA"
                                typeUpper.contains("DOKI") -> "DOKI"
                                typeUpper.contains("MANUFAKTURA") || typeUpper.contains("WARSZTAT") -> "Craft"
                                else -> "Consumption"
                            }
                            onShopTypeChange("SHOP_DIRECT:$shopType:${currentZone.specialBuilding.type}:${currentZone.specialBuilding.name}")
                            onMenuAppChange(22)
                        }

                        // PRACA DLA RZEMIEŚLNIKÓW
                        if (typeUpper.contains("WARSZTAT") || typeUpper.contains("MANUFAKTURA") || typeUpper.contains("KOWAL")) {
                            val currentHour = HeroLogic.getCurrentHour(heroData)
                            val isOpen = currentHour in 5..20 // Zamknięcie o 21:00
                            
                            craftSkills.forEach { skill ->
                                Spacer(modifier = Modifier.height(8.dp))
                                CityHubButton(
                                    "PRACUJ JAKO: ${skill.uppercase()}", 
                                    if (isOpen) Color(0xFF4E342E) else Color.Gray
                                ) {
                                    if (isOpen) {
                                        showWorkSliderDialog = Triple(skill, "Rzemiosło", 21)
                                        playSoundAction(R.raw.klik1, 0)
                                    } else {
                                        android.widget.Toast.makeText(context, "Warsztat czynny w godzinach 05:00 - 21:00", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // --- DZIAŁ: GARNIZON I ARENA ---
                    if (currentZone.districtType == DistrictType.MILITARY || typeUpper.contains("ARENA") || typeUpper.contains("KOSZARY") || typeUpper.contains("AKADEMIA WOJSKOWA")) {
                    //    CityZoneHeader("GARNIZON", Color(0xFF8B0000), nosiferFamily)
                        CityHubButton(displayBuildingLabel, Color(0xFF8B0000)) {
                            playSoundAction(R.raw.klik1, 0)
                            if (typeUpper.contains("ARENA")) {
                                val intent = Intent(context, CombatActivity::class.java).apply {
                                    putExtra("SELECTED_HERO_ID", selectedHeroId)
                                    putExtra("INITIAL_TAB", 0)
                                }
                                context.startActivity(intent)
                            } else {
                                onShopTypeChange("MILITARY_DIRECT:${currentZone.specialBuilding.type}:${currentZone.specialBuilding.name}")
                                onMenuAppChange(22)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // --- DZIAŁ: NAUKA I WIEDZA ---
                    if (currentZone.districtType == DistrictType.MAGIC || currentZone.districtType == DistrictType.SACRED || typeUpper.contains("UNIWERSYTET") || typeUpper.contains("SZKOŁA")) {
                //        CityZoneHeader("NAUKA", Color(0xFF1976D2), nosiferFamily)
                        CityHubButton(displayBuildingLabel, Color(0xFF1976D2)) {
                            playSoundAction(R.raw.klik1, 0)
                            onShopTypeChange("SCIENCE_DIRECT:${currentZone.specialBuilding.type}:${currentZone.specialBuilding.name}")
                            onMenuAppChange(22)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Domyślny przycisk dla pozostałych budynków specjalnych, jeśli nie pasują do żadnej kategorii
                    if (!typeUpper.contains("KARCZMA") && !typeUpper.contains("GOSPODA") && !typeUpper.contains("MEDYK") && 
                        !typeUpper.contains("LECZ") && !typeUpper.contains("KOWAL") && !typeUpper.contains("ZBROJ") && 
                        !typeUpper.contains("PŁATNERZ") && !typeUpper.contains("RYMARZ") && !typeUpper.contains("SKÓR") && 
                        !typeUpper.contains("HANDEL") && !typeUpper.contains("RYNEK") && !typeUpper.contains("STAJNIA") && 
                        !typeUpper.contains("DOKI") && !typeUpper.contains("ARENA") && !typeUpper.contains("KOSZARY") && 
                        !typeUpper.contains("AKADEMIA") && !typeUpper.contains("SZKOŁA") && !typeUpper.contains("UNIWERSYTET") &&
                        !typeUpper.contains("WARSZTAT") && !typeUpper.contains("PRACOWNIA") && !typeUpper.contains("MANUFAKTURA") &&
                        !typeUpper.contains("SKLEP")) {
                        
                //        CityZoneHeader("INNE OBIEKTY", Color.Gray, nosiferFamily)
                        CityHubButton(displayBuildingLabel, zoneColor) {
                            playSoundAction(R.raw.klik1, 0)
                            onShopTypeChange("OTHER_DIRECT:${currentZone.specialBuilding.type}:${currentZone.specialBuilding.name}")
                            onMenuAppChange(22)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    //CityZoneHeader("PRZEJŚCIE DO INNEJ DZIELNICY", Color.DarkGray, nosiferFamily)
                    
                    // Nawigacja do sąsiadów
                    currentZone.neighbors.forEach { neighborName ->
                        val neighbor = settlement.zones.find { it.name == neighborName }
                        if (neighbor != null) {
                            val nType = neighbor.districtType.displayName
                            val nBuilding = neighbor.specialBuilding.type
                            
                            Button(
                                onClick = { changeZone(neighborName) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color.Gray)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(neighborName.uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("$nType: $nBuilding", color = Color.LightGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // --- EXIT CITY BUTTON MOVED HERE ---
                    if (currentZone.name != "Centrum") {
                        Spacer(modifier = Modifier.height(32.dp))
                        CityZoneHeader("OPUŚĆ MIASTO", Color(0xFFFFFFFF), nosiferFamily)
                        Button(
                            onClick = { onBack() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("WYJŚCIE Z MIASTA", fontWeight = FontWeight.Bold, fontFamily = slaboFamily)
                        }
                    }
                }
            } else {
                // FALLBACK TO HARDCODED UI IF NO SETTLEMENT DATA
                
                // --- DZIAŁ: HANDEL I RZEMIOSŁO ---
            //    CityZoneHeader("DZIAŁ: HANDEL I RZEMIOSŁO", Color(0xFFDAA520), nosiferFamily)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CityHubButton("HANDEL", Color(0xFFDAA520), Modifier.weight(1f)) {
                        playSoundAction(R.raw.klik1, 0)
                        onShopTypeChange("Consumption")
                        onMenuAppChange(22)
                    }
                    CityHubButton("RZEMIOSŁO", Color(0xFF8B4513), Modifier.weight(1f)) {
                        playSoundAction(R.raw.klik1, 0)
                        onShopTypeChange("RYMARZ")
                        onMenuAppChange(22)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                CityHubButton("ZBROJOWNIA", Color(0xFFC0C0C0)) {
                    playSoundAction(R.raw.klik1, 0)
                    onShopTypeChange("ZBROJMISTRZ")
                    onMenuAppChange(22)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- DZIAŁ: USŁUGI I GOŚCINA ---
            //    CityZoneHeader("DZIAŁ: USŁUGI I GOŚCINA", Color(0xFF5D4037), nosiferFamily)
                CityHubButton("KARCZMA", Color(0xFF5D4037)) {
                    playSoundAction(R.raw.klik1, 0)
                    onShopTypeChange("Lodging")
                    onMenuAppChange(22)
                }

                // PRACA W USŁUGACH (Posługiwanie, Muzyk)
                val currentHour = HeroLogic.getCurrentHour(heroData)
                val isOpenServices = currentHour in 5..22 // Zamknięcie o 23:00

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val skillMusic = lines.find { it.contains("Rzemiosło: Muzyk") } != null
                    CityHubButton(
                        "PRACUJ JAKO: MUZYK", 
                        if (isOpenServices) Color(0xFF4E342E) else Color.Gray,
                        Modifier.weight(1f)
                    ) {
                        if (isOpenServices) {
                            showWorkSliderDialog = Triple("Muzyk", "Muzyk", 23)
                            playSoundAction(R.raw.klik1, 0)
                        } else {
                            android.widget.Toast.makeText(context, "Praca dostępna w godzinach 05:00 - 23:00", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    CityHubButton(
                        "PRACUJ JAKO: POMOC", 
                        if (isOpenServices) Color(0xFF4E342E) else Color.Gray,
                        Modifier.weight(1f)
                    ) {
                        if (isOpenServices) {
                            showWorkSliderDialog = Triple("Posługiwanie", "Posługiwanie", 23)
                            playSoundAction(R.raw.klik1, 0)
                        } else {
                            android.widget.Toast.makeText(context, "Praca dostępna w godzinach 05:00 - 23:00", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                CityHubButton("MEDYK / APTEKA", Color(0xFF2E7D32)) {
                    playSoundAction(R.raw.klik1, 0)
                    val intent = Intent(context, CombatActivity::class.java).apply {
                        putExtra("SELECTED_HERO_ID", selectedHeroId)
                        putExtra("INITIAL_TAB", 1)
                        putExtra("MEDIC_ONLY", true)
                    }
                    context.startActivity(intent)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- DZIAŁ: GARNIZON I ARENA ---
            //    CityZoneHeader("DZIAŁ: GARNIZON", Color(0xFF8B0000), nosiferFamily)
                CityHubButton("TRENING BOJOWY", Color(0xFF8B0000)) {
                    playSoundAction(R.raw.klik1, 0)
                    showCombatTrainingDialog = true
                }
                Spacer(modifier = Modifier.height(8.dp))
                CityHubButton("TRENING CECH (WW/US/K/O)", Color(0xFF550000)) {
                    playSoundAction(R.raw.klik1, 0)
                    showTrainingDialog = "PHYSICAL"
                }
                Spacer(modifier = Modifier.height(8.dp))
                CityHubButton("ARENA KRWI", Color(0xFF444444)) {
                    playSoundAction(R.raw.klik1, 0)
                    val intent = Intent(context, CombatActivity::class.java).apply {
                        putExtra("SELECTED_HERO_ID", selectedHeroId)
                        putExtra("INITIAL_TAB", 0)
                    }
                    context.startActivity(intent)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- DZIAŁ: NAUKA I WIEDZA ---
            //    CityZoneHeader("DZIAŁ: NAUKA", Color(0xFF1976D2), nosiferFamily)
                CityHubButton("SZKOŁY I UNIWERSYTET", Color(0xFF1976D2)) {
                    playSoundAction(R.raw.klik1, 0)
                    showSchoolSelection = true
                }
                Spacer(modifier = Modifier.height(8.dp))
                CityHubButton("TRENING MENTALNY", Color(0xFF4682B4)) {
                    playSoundAction(R.raw.klik1, 0)
                    showTrainingDialog = "MENTAL"
                }
                Spacer(modifier = Modifier.height(8.dp))
                CityHubButton("NAUKA I TRENING (OGÓLNE)", Color(0xFFDAA520)) {
                    playSoundAction(R.raw.klik1, 0)
                    learningSkills = HeroLogic.allNormalSkills.toList().shuffled().take(6)
                    showLearningDialog = true
                }

                // --- EXIT CITY BUTTON FOR FALLBACK ---
                Spacer(modifier = Modifier.height(32.dp))
                CityZoneHeader("OPUŚĆ MIASTO", Color(0xFFFFFFFF), nosiferFamily)
                Button(
                    onClick = { onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("WYJŚCIE Z MIASTA", fontWeight = FontWeight.Bold, fontFamily = slaboFamily)
                }
            }
            }

            //Spacer(modifier = Modifier.weight(1f))
            // Spacer(modifier = Modifier.height(32.dp))

            // CityZoneHeader("OPUŚĆ MIASTO", Color(0xFFFFFFFF), nosiferFamily)
            // Button(
            //    onClick = { onBack() },
            //    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            //    modifier = Modifier.fillMaxWidth().height(80.dp),
            //    shape = RoundedCornerShape(8.dp)
            // ) {
            //    Text("WYJŚCIE Z MIASTA", fontWeight = FontWeight.Bold, fontFamily = slaboFamily)
            // }
        }

        // --- DIALOGS ---

        if (showCombatTrainingDialog) {
            val currentMoney = HeroLogic.calculateTotalCopper(heroData)
            val trainingCost = 36 // 3 Srebrne monety = 36 pensów (1s=12p)

            AlertDialog(
                onDismissRequest = { showCombatTrainingDialog = false },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("ARENA: TRENING BOJOWY", color = Color.Red, fontFamily = nosiferFamily) },
                text = {
                    Column {
                        Text("Koszt treningu: 3 Srebrne monety", color = Color.Yellow)
                        Text("Twoje fundusze: ${currentMoney / 12}s ${currentMoney % 12}p", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        combatTrainingSkills.forEach { skillName ->
                            val isWW = HeroLogic.skillWW.contains(skillName)
                            val statName = if (isWW) "WW" else "US"
                            val statValue = HeroLogic.getStatValue(heroData, statName)
                            val heroSkillInfo = findHeroSkill(heroData, skillName)
                            val rank = heroSkillInfo?.second ?: "Brak"
                            val currentPn = heroSkillInfo?.first ?: 0
                            val normRank = normalizeRank(rank)
                            val mod = when (normRank) {
                                "Uczeń" -> 0
                                "Adept" -> 5
                                "Ekspert" -> 10
                                "Mistrz" -> 15
                                "Arcymistrz" -> 20
                                else -> -30
                            }
                            
                            val modifiedStat = statValue + mod
                            val successChance = if (normRank == "Brak") {
                                (statValue / 11).coerceAtLeast(0)
                            } else {
                                modifiedStat.coerceIn(0, 100)
                            }

                            Button(
                                onClick = {
                                    if (currentMoney >= trainingCost) {
                                        val rzut = (1..100).random()
                                        val rzutStr = if (rzut < 10) "0$rzut" else if (rzut == 100) "00" else rzut.toString()
                                        val isDublet = rzutStr[0] == rzutStr[1]
                                        
                                        val success = if (normRank == "Brak") {
                                            isDublet && rzut <= statValue
                                        } else {
                                            rzut <= modifiedStat
                                        }
                                        
                                        val diff = modifiedStat - rzut
                                        val pnToAdd = when {
                                            isDublet && rzut <= statValue -> (statValue / 10).coerceAtLeast(1)
                                            diff >= 30 -> 3
                                            diff >= 0 -> 2
                                            diff < 0 -> 1
                                            else -> 0
                                        }
                                        
                                        var updated = deductMoney(heroData, trainingCost)
                                        updated = addPNToSkill(updated, skillName, statName, pnToAdd)
                                        saveToFile(fileName, updated)
                                        heroData = updated

                                        // Logowanie do historii
                                        val historyFile = "hero_${selectedHeroId}_pd_history"
                                        val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                        val historyLog = try { context.openFileInput(historyFile).bufferedReader().readText() } catch(e: Exception) { "" }
                                        saveToFile(historyFile, "$timestamp: Trening: $skillName (+$pnToAdd PN) - Koszt: ${trainingCost/12}s\n$historyLog")
                                        
                                        val resultMsg = if (success) "SUKCES! (Rzut: $rzut vs $modifiedStat)" else "PORAŻKA (Rzut: $rzut vs $modifiedStat)"
                                        android.widget.Toast.makeText(context, "$resultMsg\n+$pnToAdd PN dla $skillName (Suma: ${currentPn + pnToAdd})", android.widget.Toast.LENGTH_SHORT).show()
                                        playSoundAction(if (success) R.raw.levelup else R.raw.klik1, 0)
                                    }
                                },
                                enabled = currentMoney >= trainingCost,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isWW) Color(0xFF441111) else Color(0xFF111144))
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$skillName ($statName: $statValue)", fontWeight = FontWeight.Bold)
                                    Text("$rank | PN: $currentPn", fontSize = 11.sp)
                                    Text("Szansa: $successChance%", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showCombatTrainingDialog = false }) { Text("ZAMKNIJ", color = Color.White) } }
            )
        }

        if (showWorkResultDialog != null) {
            val result = showWorkResultDialog!!
            AlertDialog(
                onDismissRequest = { showWorkResultDialog = null },
                containerColor = Color(0xFF1A1A1A),
                title = { Text(result.title, color = Color.Yellow, fontFamily = nosiferFamily) },
                text = {
                    Column {
                        Text(result.description, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Zarobek: ${HeroLogic.formatPrice(result.totalWage)}", color = Color.Green, fontWeight = FontWeight.Bold)
                        Text("Czas: +8 godzin", color = Color.Gray, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showWorkResultDialog = null }) {
                        Text("OK", color = Color.White)
                    }
                }
            )
        }

        if (showShopSelection) {
            AlertDialog(
                onDismissRequest = { showShopSelection = false },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("DZIELNICA HANDLOWA", color = Color(0xFFDAA520), fontFamily = nosiferFamily) },
                text = {
                    Column {
                        CityHubButton("SKLEPY I KOWALE", Color(0xFFDAA520)) {
                            onShopTypeChange("Consumption")
                            onMenuAppChange(22)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        CityHubButton("GOSPODY I NOCLEGI", Color(0xFF5D4037)) {
                            onShopTypeChange("Lodging")
                            onMenuAppChange(22)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        CityHubButton("ROZRYWKA I USŁUGI", Color(0xFF8B4513)) {
                            onShopTypeChange("Entertainment")
                            onMenuAppChange(22)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showShopSelection = false }) { Text("ZAMKNIJ", color = Color.White) } }
            )
        }

        if (showSchoolSelection) {
            AlertDialog(
                onDismissRequest = { showSchoolSelection = false },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("AKADEMIA WIEDZY", color = Color(0xFF1976D2), fontFamily = nosiferFamily) },
                text = {
                    Column {
                        CityHubButton("SZKOŁA JĘZYKÓW", Color(0xFF1976D2)) {
                            showSchoolSelection = false
                            showLanguageSchoolDialog = true
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        CityHubButton("UNIWERSYTET", Color(0xFF0D47A1)) {
                            showSchoolSelection = false
                            showUniversityDialog = true
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showSchoolSelection = false }) { Text("ZAMKNIJ", color = Color.White) } }
            )
        }

        if (showLanguageSchoolDialog) {
            SchoolLearningDialog(
                title = "SZKOŁA JĘZYKÓW",
                skills = languageSchoolSkills,
                heroData = heroData,
                onDismiss = { showLanguageSchoolDialog = false },
                onUpdateHero = { 
                    heroData = it
                    saveToFile(fileName, it)
                    // Logowanie do historii
                    val historyFile = "hero_${selectedHeroId}_pd_history"
                    val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    val historyLog = try { context.openFileInput(historyFile).bufferedReader().readText() } catch(e: Exception) { "" }
                    saveToFile(historyFile, "$timestamp: Nauka w Szkole Języków\n$historyLog")
                },
                playSoundAction = playSoundAction,
                context = context,
                lines = lines,
                nosiferFamily = nosiferFamily
            )
        }

        if (showUniversityDialog) {
            SchoolLearningDialog(
                title = "UNIWERSYTET",
                skills = universitySkillsList,
                heroData = heroData,
                onDismiss = { showUniversityDialog = false },
                onUpdateHero = { 
                    heroData = it
                    saveToFile(fileName, it)
                    // Logowanie do historii
                    val historyFile = "hero_${selectedHeroId}_pd_history"
                    val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    val historyLog = try { context.openFileInput(historyFile).bufferedReader().readText() } catch(e: Exception) { "" }
                    saveToFile(historyFile, "$timestamp: Nauka na Uniwersytecie\n$historyLog")
                },
                playSoundAction = playSoundAction,
                context = context,
                lines = lines,
                nosiferFamily = nosiferFamily
            )
        }

        if (showLearningDialog) {
            SchoolLearningDialog(
                title = "NAUKA I TRENING",
                skills = learningSkills,
                heroData = heroData,
                onDismiss = { showLearningDialog = false },
                onUpdateHero = { 
                    heroData = it
                    saveToFile(fileName, it)
                    // Logowanie do historii
                    val historyFile = "hero_${selectedHeroId}_pd_history"
                    val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    val historyLog = try { context.openFileInput(historyFile).bufferedReader().readText() } catch(e: Exception) { "" }
                    saveToFile(historyFile, "$timestamp: Nauka/Trening ogólny\n$historyLog")
                },
                playSoundAction = playSoundAction,
                context = context,
                lines = lines,
                nosiferFamily = nosiferFamily
            )
        }

        if (showTrainingDialog != null) {
            val isMental = showTrainingDialog == "MENTAL"
            val isPhysical = showTrainingDialog == "PHYSICAL"
            val isSpecialist = showTrainingDialog == "SPECIALIST"
            
            val currentMoney = HeroLogic.calculateTotalCopper(heroData)
            val trainingCost = when {
                isMental -> 36 
                isPhysical -> 60 // Staty droższe: 5s = 60p
                else -> 48 // Specialist 4s
            }
            
            val dialogTitle = when {
                isMental -> "AKADEMIA: TRENING MENTALNY"
                isPhysical -> "ARENA: TRENING CECH"
                else -> "AKADEMIA: TRENING SPECJALISTYCZNY"
            }
            val dialogColor = when {
                isMental -> Color(0xFF4682B4)
                isPhysical -> Color(0xFF8B0000)
                else -> Color(0xFF6A5ACD)
            }

            val availableSkillsOrStats = remember(showTrainingDialog) {
                when {
                    isMental -> (HeroLogic.skillInteligencja.toList() + HeroLogic.skillSW.toList() + HeroLogic.skillCharyzma.toList()).shuffled().take(6)
                    isPhysical -> listOf("WW", "US", "K", "O")
                    else -> (HeroLogic.skillZrecznosc.toList() + HeroLogic.skillWiedza.toList()).shuffled().take(6)
                }
            }

            AlertDialog(
                onDismissRequest = { showTrainingDialog = null },
                containerColor = Color(0xFF1A1A1A),
                title = { Text(dialogTitle, color = dialogColor, fontFamily = nosiferFamily) },
                text = {
                    Column {
                        Text("Koszt treningu: ${trainingCost / 12} Srebrne monety", color = Color.Yellow)
                        Text("Twoje fundusze: ${currentMoney / 12}s ${currentMoney % 12}p", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        availableSkillsOrStats.forEach { entry ->
                            if (isPhysical) {
                                val statValue = HeroLogic.getStatValue(heroData, entry)
                                val successChance = statValue.coerceIn(0, 100)

                                Button(
                                    onClick = {
                                        if (currentMoney >= trainingCost) {
                                            val rzut = (1..100).random()
                                            val success = rzut <= statValue
                                            
                                            var updated = HeroLogic.deductMoney(heroData, trainingCost)
                                            if (success) {
                                                updated = applyStatTraining(updated, entry, 0) // PD cost is 0 here as we pay in gold
                                            }
                                            saveToFile(fileName, updated)
                                            heroData = updated

                                            // Logowanie do historii
                                            val historyFile = "hero_${selectedHeroId}_pd_history"
                                            val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                            val historyLog = try { context.openFileInput(historyFile).bufferedReader().readText() } catch(e: Exception) { "" }
                                            val outcome = if(success) "Sukces (+1 $entry)" else "Porażka"
                                            saveToFile(historyFile, "$timestamp: Trening cechy $entry ($outcome) - Koszt: ${trainingCost/12}s\n$historyLog")
                                            
                                            val resultMsg = if (success) "SUKCES! +1 do $entry" else "PORAŻKA"
                                            android.widget.Toast.makeText(context, "$resultMsg (Rzut: $rzut vs $statValue)", android.widget.Toast.LENGTH_SHORT).show()
                                            playSoundAction(if (success) R.raw.levelup else R.raw.klik1, 0)
                                        }
                                    },
                                    enabled = currentMoney >= trainingCost,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = dialogColor.copy(alpha = 0.7f))
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Trenuj $entry (Aktualnie: $statValue)", fontWeight = FontWeight.Bold)
                                        Text("Szansa: $successChance%", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                val skillName = entry
                                val statName = when {
                                    HeroLogic.skillInteligencja.contains(skillName) -> "Int"
                                    HeroLogic.skillSW.contains(skillName) -> "SW"
                                    HeroLogic.skillCharyzma.contains(skillName) -> "Cha"
                                    HeroLogic.skillZrecznosc.contains(skillName) -> "Zr"
                                    HeroLogic.skillWiedza.contains(skillName) -> "Wiedza"
                                    else -> "???"
                                }
                                
                                val statValue = HeroLogic.getStatValue(heroData, if (statName == "Wiedza") "Int" else statName)

                                val heroSkillInfo = findHeroSkill(heroData, skillName)
                                val rank = heroSkillInfo?.second ?: "Brak"
                                val currentPn = heroSkillInfo?.first ?: 0
                                val normRank = normalizeRank(rank)
                                val mod = when (normRank) {
                                    "Uczeń" -> 0
                                    "Adept" -> 5
                                    "Ekspert" -> 10
                                    "Mistrz" -> 15
                                    "Arcymistrz" -> 20
                                    else -> -30
                                }
                                
                                val modifiedStat = statValue + mod
                                val successChance = if (normRank == "Brak") {
                                    (statValue / 11).coerceAtLeast(0)
                                } else {
                                    modifiedStat.coerceIn(0, 100)
                                }

                                Button(
                                    onClick = {
                                        if (currentMoney >= trainingCost) {
                                            val rzut = (1..100).random()
                                            val rzutStr = if (rzut < 10) "0$rzut" else if (rzut == 100) "00" else rzut.toString()
                                            val isDublet = rzutStr[0] == rzutStr[1]
                                            
                                            val success = if (normRank == "Brak") {
                                                isDublet && rzut <= statValue
                                            } else {
                                                rzut <= modifiedStat
                                            }

                                            val diff = modifiedStat - rzut
                                            val pnToAdd = when {
                                                isDublet && rzut <= statValue -> (statValue / 10).coerceAtLeast(1)
                                                diff >= 30 -> 3
                                                diff >= 0 -> 2
                                                diff < 0 -> 1
                                                else -> 0
                                            }

                                            var updated = HeroLogic.deductMoney(heroData, trainingCost)
                                            updated = addPNToSkill(updated, skillName, if (statName == "Wiedza") "Inteligencja" else statName, pnToAdd)
                                            saveToFile(fileName, updated)
                                            heroData = updated

                                            // Logowanie do historii
                                            val historyFile = "hero_${selectedHeroId}_pd_history"
                                            val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                            val historyLog = try { context.openFileInput(historyFile).bufferedReader().readText() } catch(e: Exception) { "" }
                                            saveToFile(historyFile, "$timestamp: Trening: $skillName (+$pnToAdd PN) - Koszt: ${trainingCost/12}s\n$historyLog")

                                            val resultMsg = if (success) "SUKCES! (Rzut: $rzut vs $modifiedStat)" else "PORAŻKA (Rzut: $rzut vs $modifiedStat)"
                                            android.widget.Toast.makeText(context, "$resultMsg\n+$pnToAdd PN dla $skillName (Suma: ${currentPn + pnToAdd})", android.widget.Toast.LENGTH_SHORT).show()
                                            playSoundAction(if (success) R.raw.levelup else R.raw.klik1, 0)
                                        }
                                    },
                                    enabled = currentMoney >= trainingCost && normRank != "Mistrz" && normRank != "Arcymistrz",
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = dialogColor.copy(alpha = 0.7f))
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$skillName ($statName: $statValue)", fontWeight = FontWeight.Bold)
                                        Text("$rank | PN: $currentPn", fontSize = 11.sp)
                                        Text("Szansa: $successChance%", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                    }
                },
                confirmButton = { TextButton(onClick = { showTrainingDialog = null }) { Text("ZAMKNIJ", color = Color.White) } }
            )
        }
    }


@Composable
fun SchoolLearningDialog(
    title: String,
    skills: List<String>,
    heroData: String,
    onDismiss: () -> Unit,
    onUpdateHero: (String) -> Unit,
    playSoundAction: (Int, Int) -> Unit,
    context: Context,
    lines: List<String>,
    nosiferFamily: FontFamily?
) {
    val currentMoney = HeroLogic.calculateTotalCopper(heroData)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text(title, color = Color(0xFFDAA520), fontFamily = nosiferFamily, fontSize = 12.sp) },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Text("Fundusze: ${HeroLogic.formatPrice(currentMoney)}", color = Color.Gray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                skills.forEach { skillName ->
                    val heroSkillInfo = findHeroSkill(heroData, skillName)
                    val rank = heroSkillInfo?.second ?: "Brak"
                    val currentPn = heroSkillInfo?.first ?: 0
                    val normRank = normalizeRank(rank)
                    
                    val cost = when(normRank) {
                        "Brak" -> 5
                        "Uczeń" -> 12
                        "Adept" -> 60
                        "Ekspert" -> 480
                        else -> 999999
                    }
                    
                    val mod = when (normRank) {
                        "Uczeń" -> 0
                        "Adept" -> 5
                        "Ekspert" -> 10
                        "Mistrz" -> 15
                        "Arcymistrz" -> 20
                        else -> -30
                    }
                    
                    val statName = when {
                        HeroLogic.skillWW.contains(skillName) -> "WW"
                        HeroLogic.skillUS.contains(skillName) -> "US"
                        HeroLogic.skillKrzepa.contains(skillName) -> "Krzepa"
                        HeroLogic.skillOdpornosc.contains(skillName) -> "Odporność"
                        HeroLogic.skillZrecznosc.contains(skillName) -> "Zr"
                        HeroLogic.skillInteligencja.contains(skillName) -> "Int"
                        HeroLogic.skillSW.contains(skillName) -> "SW"
                        HeroLogic.skillCharyzma.contains(skillName) -> "Cha"
                        HeroLogic.skillWiedza.contains(skillName) -> "Wiedza"
                        HeroLogic.skillJezyk.contains(skillName) -> "Język"
                        else -> "???"
                    }
                    
                    val statValue = HeroLogic.getStatValue(heroData, statName)
                    
                    val modifiedStat = statValue + mod
                    val successChance = if (normRank == "Brak") {
                        (statValue / 11).coerceAtLeast(0)
                    } else {
                        modifiedStat.coerceIn(0, 100)
                    }

                    Button(
                        onClick = {
                            if (currentMoney >= cost) {
                                val rzut = (1..100).random()
                                val rzutStr = if (rzut < 10) "0$rzut" else if (rzut == 100) "00" else rzut.toString()
                                val isDublet = rzutStr[0] == rzutStr[1]
                                
                                val success = if (normRank == "Brak") {
                                    isDublet && rzut <= statValue
                                } else {
                                    rzut <= modifiedStat
                                }
                                val diff = modifiedStat - rzut
                                val pnToAdd = when {
                                    isDublet && rzut <= statValue -> (statValue / 10).coerceAtLeast(1)
                                    diff >= 30 -> 3
                                    diff >= 0 -> 2
                                    diff < 0 -> 1
                                    else -> 0
                                }

                                var updated = HeroLogic.deductMoney(heroData, cost)
                                val fileStatName = when(statName) {
                                    "US" -> "Umiejętności Strzeleckie"
                                    "Język" -> "Język"
                                    "Krzepa" -> "Krzepa"
                                    "Odporność" -> "Odporność"
                                    "Int" -> "Inteligencja"
                                    else -> statName
                                }
                                updated = addPNToSkill(updated, skillName, fileStatName, pnToAdd)
                                onUpdateHero(updated)
                                
                                val resultMsg = if (success) "SUKCES! (Rzut: $rzut vs $modifiedStat)" else "PORAŻKA (Rzut: $rzut vs $modifiedStat)"
                                android.widget.Toast.makeText(context, "$resultMsg\n+$pnToAdd PN dla $skillName (Suma: ${currentPn + pnToAdd})", android.widget.Toast.LENGTH_SHORT).show()
                                playSoundAction(if (success) R.raw.levelup else R.raw.klik1, 0)
                            }
                        },
                        enabled = currentMoney >= cost && normRank != "Mistrz" && normRank != "Arcymistrz",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E2E))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(skillName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Ranga: $rank | PN: $currentPn", fontSize = 11.sp, color = Color.LightGray)
                            Text("Cena: ${HeroLogic.formatPrice(cost)} | Szansa: $successChance%", fontSize = 11.sp, color = Color.Yellow)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("ZAMKNIJ", color = Color.White) } }
    )
}

@Composable
fun CityZoneHeader(text: String, color: Color, font: FontFamily?) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(text, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = font)
        HorizontalDivider(color = color.copy(alpha = 0.5f), thickness = 1.dp)
    }
}

@Composable
fun CityHubButton(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
