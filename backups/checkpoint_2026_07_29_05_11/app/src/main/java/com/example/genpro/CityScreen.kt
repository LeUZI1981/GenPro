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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    isGMMode: Boolean = false
) {
    val context = LocalContext.current
    val fileName = "hero_$selectedHeroId"
    var heroData by remember { mutableStateOf(readFromFile(fileName)) }
    
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

    val lines = heroData.lines()
    val infoLine1 = lines.find { it.contains("Rasa:") && it.contains("Klasa:") } ?: ""
    val klasaRaw = infoLine1.substringAfter("Klasa: ").trim()
    val klasa = if (klasaRaw.contains("(")) klasaRaw.substringBefore("(").trim() else klasaRaw

    val weightLimit = getHeroWeightLimit(heroData)
    val currentWeight = calculateCurrentWeight(heroData)
    val encumbrance = getEncumbrance(currentWeight, weightLimit)

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
            Text(
                "MIASTO MROKU",
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

            CityZoneHeader("DZIELNICA HANDLOWA", Color(0xFFDAA520), nosiferFamily)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CityHubButton("ZBROJMISTRZ", Color(0xFFDAA520), Modifier.weight(1f)) {
                    playSoundAction(R.raw.klik1, 0)
                    onShopTypeChange("ZBROJMISTRZ")
                    onMenuAppChange(15)
                }
                CityHubButton("RYMARZ", Color(0xFF8B4513), Modifier.weight(1f)) {
                    playSoundAction(R.raw.klik1, 0)
                    onShopTypeChange("RYMARZ")
                    onMenuAppChange(15)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CityHubButton("PŁATNERZ", Color(0xFFC0C0C0), Modifier.weight(1f)) {
                    playSoundAction(R.raw.klik1, 0)
                    onShopTypeChange("PŁATNERZ")
                    onMenuAppChange(15)
                }
                CityHubButton("KARCZMA", Color(0xFF5D4037), Modifier.weight(1f)) {
                    playSoundAction(R.raw.klik1, 0)
                    onShopTypeChange("KARCZMA")
                    onMenuAppChange(15)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            CityHubButton("MEDYK", Color(0xFF2E7D32)) {
                playSoundAction(R.raw.klik1, 0)
                val intent = Intent(context, CombatActivity::class.java).apply {
                    putExtra("SELECTED_HERO_ID", selectedHeroId)
                    putExtra("INITIAL_TAB", 1) 
                }
                context.startActivity(intent)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ZONE: ARENA ---
            CityZoneHeader("ARENA KRWI", Color(0xFF8B0000), nosiferFamily)
            CityHubButton("TRENING BOJOWY (WW/US)", Color(0xFF8B0000)) {
                playSoundAction(R.raw.klik1, 0)
                showCombatTrainingDialog = true
            }
            Spacer(modifier = Modifier.height(8.dp))
            CityHubButton("TRENING STATYSTYK (WW/US/K/O)", Color(0xFF550000)) {
                playSoundAction(R.raw.klik1, 0)
                showTrainingDialog = "PHYSICAL"
            }
            Spacer(modifier = Modifier.height(8.dp))
            CityHubButton("WALKA O ZŁOTO (ARENA)", Color(0xFF444444)) {
                playSoundAction(R.raw.klik1, 0)
                val intent = Intent(context, CombatActivity::class.java).apply {
                    putExtra("SELECTED_HERO_ID", selectedHeroId)
                    putExtra("INITIAL_TAB", 0)
                }
                context.startActivity(intent)
            }
            Spacer(modifier = Modifier.height(8.dp))
            CityHubButton("PRÓBA KRWI (POJEDYNEK)", Color(0xFF333333)) {
                playSoundAction(R.raw.klik1, 0)
                // Trigger arena combat via CombatActivity (assumed INITIAL_TAB 0 handles combat)
                val intent = Intent(context, CombatActivity::class.java).apply {
                    putExtra("SELECTED_HERO_ID", selectedHeroId)
                    putExtra("INITIAL_TAB", 0)
                    putExtra("MODE", "DUEL")
                }
                context.startActivity(intent)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ZONE: SCHOOL ---
            CityZoneHeader("AKADEMIA WIEDZY", Color(0xFF1976D2), nosiferFamily)
            CityHubButton("SZKOŁA", Color(0xFF1976D2)) {
                playSoundAction(R.raw.klik1, 0)
                showSchoolSelection = true
            }
            Spacer(modifier = Modifier.height(8.dp))
            CityHubButton("TRENING MENTALNY (INT/SW/CHA)", Color(0xFF4682B4)) {
                playSoundAction(R.raw.klik1, 0)
                showTrainingDialog = "MENTAL"
            }
            Spacer(modifier = Modifier.height(8.dp))
            CityHubButton("TRENING SPECJALISTYCZNY", Color(0xFF6A5ACD)) {
                playSoundAction(R.raw.klik1, 0)
                showTrainingDialog = "SPECIALIST"
            }
            Spacer(modifier = Modifier.height(8.dp))
            CityHubButton("NAUKA I TRENING (LOSOWE)", Color(0xFFDAA520)) {
                playSoundAction(R.raw.klik1, 0)
                learningSkills = HeroLogic.allNormalSkills.toList().shuffled().take(6)
                showLearningDialog = true
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onBack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("POWRÓT DO MIASTA", fontWeight = FontWeight.Bold, fontFamily = slaboFamily)
            }
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
                            val statValue = lines.find { it.startsWith("$statName:") }?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
                            val heroSkillInfo = findHeroSkill(heroData, skillName)
                            val rank = heroSkillInfo?.second ?: "Brak"
                            val mod = when (rank) {
                                "Uczeń" -> 0
                                "Adept" -> 5
                                "Ekspert" -> 10
                                "Mistrz" -> 15
                                "Arcymistrz" -> 20
                                else -> -30
                            }

                            Button(
                                onClick = {
                                    if (currentMoney >= trainingCost) {
                                        val rzut = (1..100).random()
                                        val isDublet = rzut.toString().let { it.length == 2 && it[0] == it[1] } || rzut == 100
                                        val success = rzut <= (statValue + mod)
                                        
                                        val pnToAdd = when {
                                            isDublet && rzut <= statValue -> (statValue / 10).coerceAtLeast(1)
                                            (statValue + mod) - rzut >= 30 -> 3
                                            success -> 2
                                            !success -> 1
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
                                        
                                        val resultMsg = if (success) "SUKCES! (Rzut: $rzut vs ${statValue + mod})" else "PORAŻKA (Rzut: $rzut vs ${statValue + mod})"
                                        android.widget.Toast.makeText(context, "$resultMsg\n+$pnToAdd PN dla $skillName", android.widget.Toast.LENGTH_SHORT).show()
                                        playSoundAction(if (success) R.raw.levelup else R.raw.klik1, 0)
                                    }
                                },
                                enabled = currentMoney >= trainingCost,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isWW) Color(0xFF441111) else Color(0xFF111144))
                            ) {
                                Text("$skillName ($statName: $statValue, $rank)", textAlign = TextAlign.Center)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showCombatTrainingDialog = false }) { Text("ZAMKNIJ", color = Color.White) } }
            )
        }

        if (showShopSelection) {
            AlertDialog(
                onDismissRequest = { showShopSelection = false },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("DZIELNICA HANDLOWA", color = Color(0xFFDAA520), fontFamily = nosiferFamily) },
                text = {
                    Column {
                        CityHubButton("KOWAL BRONI", Color(0xFFDAA520)) {
                            onShopTypeChange("ZBROJMISTRZ")
                            onMenuAppChange(15)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        CityHubButton("PŁATNERZ", Color(0xFFDAA520)) {
                            onShopTypeChange("PŁATNERZ")
                            onMenuAppChange(15)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        CityHubButton("RYMARZ", Color(0xFF8B4513)) {
                            onShopTypeChange("RYMARZ")
                            onMenuAppChange(15)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        CityHubButton("KARCZMA", Color(0xFF5D4037)) {
                            onShopTypeChange("KARCZMA")
                            onMenuAppChange(15)
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
                                val statValue = lines.find { it.startsWith("$entry:") }?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
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
                                    Text("Trenuj $entry (Aktualnie: $statValue)")
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
                                
                                val statValue = when(statName) {
                                    "Wiedza" -> lines.find { it.startsWith("Int:") }?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
                                    else -> lines.find { it.startsWith("$statName:") }?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
                                }

                                val heroSkillInfo = findHeroSkill(heroData, skillName)
                                val rank = heroSkillInfo?.second ?: "Brak"
                                val normRank = normalizeRank(rank)
                                val mod = when (normRank) {
                                    "Uczeń" -> 0
                                    "Adept" -> 5
                                    "Ekspert" -> 10
                                    "Mistrz" -> 15
                                    "Arcymistrz" -> 20
                                    else -> -30
                                }

                                Button(
                                    onClick = {
                                        if (currentMoney >= trainingCost) {
                                            val rzut = (1..100).random()
                                            val rzutStr = if (rzut < 10) "0$rzut" else if (rzut == 100) "00" else rzut.toString()
                                            val isDublet = rzutStr[0] == rzutStr[1]
                                            
                                            val modifiedStat = statValue + mod
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
                                            android.widget.Toast.makeText(context, "$resultMsg\n+$pnToAdd PN dla $skillName", android.widget.Toast.LENGTH_SHORT).show()
                                            playSoundAction(if (success) R.raw.levelup else R.raw.klik1, 0)
                                        }
                                    },
                                    enabled = currentMoney >= trainingCost && normRank != "Mistrz" && normRank != "Arcymistrz",
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = dialogColor.copy(alpha = 0.7f))
                                ) {
                                    Text("$skillName ($statName: $statValue, $rank)", textAlign = TextAlign.Center)
                                }
                            }
                        }

                    }
                },
                confirmButton = { TextButton(onClick = { showTrainingDialog = null }) { Text("ZAMKNIJ", color = Color.White) } }
            )
        }
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
                    
                    val statValue = when(statName) {
                        "Wiedza", "Język" -> lines.find { it.startsWith("Int:") }?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
                        "Krzepa" -> lines.find { it.startsWith("K:") }?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
                        "Odporność" -> lines.find { it.startsWith("O:") }?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
                        else -> lines.find { it.startsWith("$statName:") }?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
                    }

                    Button(
                        onClick = {
                            if (currentMoney >= cost) {
                                val rzut = (1..100).random()
                                val rzutStr = if (rzut < 10) "0$rzut" else if (rzut == 100) "00" else rzut.toString()
                                val isDublet = rzutStr[0] == rzutStr[1]
                                
                                val modifiedStat = statValue + mod
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
                                android.widget.Toast.makeText(context, "$resultMsg\n+$pnToAdd PN dla $skillName", android.widget.Toast.LENGTH_SHORT).show()
                                playSoundAction(if (success) R.raw.levelup else R.raw.klik1, 0)
                            }
                        },
                        enabled = currentMoney >= cost && normRank != "Mistrz" && normRank != "Arcymistrz",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E2E))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(skillName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Ranga: $rank | Cena: ${HeroLogic.formatPrice(cost)}", fontSize = 11.sp, color = Color.Yellow)
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
