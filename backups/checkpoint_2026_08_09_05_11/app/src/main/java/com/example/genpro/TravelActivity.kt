package com.example.genpro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpro.ui.theme.GenProTheme
import android.content.Context
import java.util.Locale
import java.io.File
import kotlin.random.Random
import kotlinx.serialization.json.Json

class TravelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val heroIdString = intent.getStringExtra("HERO_ID") ?: "-1"
        val heroId = heroIdString.toIntOrNull() ?: -1
        val destination = intent.getStringExtra("DESTINATION") ?: "Nieznany cel"
        val destinationId = intent.getLongExtra("DESTINATION_ID", -1L)
        val mapId = intent.getStringExtra("MAP_ID") ?: "mapworld"
        val totalHours = intent.getIntExtra("TRAVEL_TIME", 24)
        val roadTypeName = intent.getStringExtra("ROAD_TYPE") ?: "ROAD"
        val zoneTypeName = intent.getStringExtra("ZONE_TYPE") ?: "NATURA"
        val tagsString = intent.getStringExtra("TAGS") ?: ""
        
        setContent {
            GenProTheme {
                TravelScreen(
                    heroId = heroId,
                    destination = destination,
                    destinationId = destinationId,
                    mapId = mapId,
                    totalHours = totalHours,
                    roadType = try { RoadType.valueOf(roadTypeName) } catch(e: Exception) { RoadType.ROAD },
                    zoneType = try { ZoneType.valueOf(zoneTypeName) } catch(e: Exception) { ZoneType.NATURA },
                    tags = tagsString.split(",").mapNotNull { t -> try { LocationTag.valueOf(t) } catch(e: Exception) { null } },
                    onFinish = {
                        val intent = Intent(this@TravelActivity, MainActivity::class.java).apply {
                            putExtra(MainActivity.EXTRA_MENU_APP, 50)
                            putExtra(MainActivity.EXTRA_HERO_ID, heroId)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun TravelScreen(
    heroId: Int, 
    destination: String, 
    destinationId: Long,
    mapId: String,
    totalHours: Int, 
    roadType: RoadType,
    zoneType: ZoneType,
    tags: List<LocationTag>,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var currentProgress by remember { mutableIntStateOf(0) }
    var logText by remember { mutableStateOf("Wyruszasz w drogę do $destination...") }
    var currentLocationName by remember { mutableStateOf(roadType.displayName) }
    var isEventActive by remember { mutableStateOf(false) }
    var eventType by remember { mutableStateOf("") }
    var showRestPanel by remember { mutableStateOf(false) }
    var restHours by remember { mutableFloatStateOf(8f) }

    // Odczyt aktualnej godziny dla TravelActivity
    val heroFile = "hero_$heroId"
    val heroDataInit = try {
        context.openFileInput(heroFile).bufferedReader().use { it.readText() }
    } catch (e: Exception) { "" }

    var heroData by remember { mutableStateOf(heroDataInit) }
    var dataNowStr by remember { mutableStateOf(HeroLogic.getCalendarData(heroData)) }
    val timeOfDay = if (dataNowStr.contains("|")) HeroLogic.getTimeOfDay(dataNowStr) else ""

    val progressPercent = (currentProgress.toFloat() / totalHours.toFloat()).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "PODRÓŻ DO: $destination",
                color = Color(0xFFDAA520),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            if (timeOfDay.isNotEmpty()) {
                Text(
                    "Pora: $timeOfDay",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${roadType.displayName} | ${zoneType.displayName}", 
                color = Color.Gray, 
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Pasek postępu
            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = if (zoneType == ZoneType.CYWILIZACJA) Color.Blue else Color.Green,
                trackColor = Color.DarkGray
            )
            Text("${currentProgress}h / ${totalHours}h", color = Color.White, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(32.dp))

            // Ilustracja/Ikona lokacji
            Card(
                modifier = Modifier.size(200.dp),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    val displayLoc = if (isEventActive) currentLocationName else {
                        if (tags.isNotEmpty()) tags.random().tagName else roadType.displayName
                    }
                    Text(
                        displayLoc.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Log podróży
            Text(
                logText,
                color = Color.LightGray,
                modifier = Modifier.fillMaxWidth().height(100.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            if (!isEventActive) {
                Button(
                    onClick = {
                        // Prędkość zależy od typu drogi oraz umiejętności bohatera
                        val navRank = HeroLogic.getSkillRank(heroData.lines(), "Nawigacja")
                        val survivalRank = HeroLogic.getSkillRank(heroData.lines(), "Sztuka przetrwania")
                        
                        val baseStep = (2..4).random()
                        val skillBonus = if (roadType == RoadType.PATH) survivalRank else navRank
                        
                        val actualStep = ((baseStep + skillBonus) * roadType.speedMultiplier).toInt().coerceAtLeast(1)
                        currentProgress = (currentProgress + actualStep).coerceAtMost(totalHours)
                        
                        if (currentProgress >= totalHours) {
                            logText = "Dotarłeś do celu: $destination!"
                            currentLocationName = destination
                            
                            // Aktualizacja czasu...
                            try {
                                val newTime = HeroLogic.advanceTime(dataNowStr, totalHours)
                                val parts = newTime.split("|").map { it.trim() }
                                val datePart = parts.getOrNull(1) ?: ""
                                val timePart = parts.getOrNull(2) ?: ""

                                var settlementJson = ""
                                // --- GENEROWANIE OSADY JEŚLI PUSTA ---
                                if (zoneType == ZoneType.CYWILIZACJA && destinationId != -1L) {
                                    if (!WorldPersistence.exists(context, destination)) {
                                        val sType = if (tags.contains(LocationTag.CITY)) SettlementType.CITY else SettlementType.TOWN
                                        SettlementGenerator.generateSettlement(context, destination, sType)
                                    }
                                }

                                val finalDateStr = "$destination | $datePart | $timePart | " // Puste settlementJson w hero save
                                
                                // WYDOBYCIE KOORDYNATÓW Z MAPY JEŚLI MOŻLIWE
                                var finalX = "500"
                                var finalY = "500"
                                if (destinationId != -1L) {
                                    val pinsFileName = "hero_map_pins_${heroId}_$mapId"
                                    val currentPinsData = try {
                                        context.openFileInput(pinsFileName).bufferedReader().use { it.readText() }
                                    } catch (e: Exception) { "" }
                                    val nodes = decodeNodes(currentPinsData)
                                    val targetNode = nodes.find { it.id == destinationId }
                                    if (targetNode != null) {
                                        finalX = targetNode.x.toInt().toString()
                                        finalY = targetNode.y.toInt().toString()
                                    }
                                }
                                
                                val finalCalendarWithCoords = "$finalDateStr | $finalX | $finalY | ${zoneType.name}"
                                val updatedHeroData = HeroLogic.updateCalendarData(heroData, finalCalendarWithCoords)
                                context.openFileOutput(heroFile, Context.MODE_PRIVATE).use {
                                    it.write(updatedHeroData.toByteArray())
                                }
                                heroData = updatedHeroData
                                dataNowStr = finalCalendarWithCoords
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            // Generator zdarzeń zależny od strefy
                            val baseChance = if (zoneType == ZoneType.NATURA) 30 else 20
                            val roll = (1..100).random()
                            
                            when {
                                roll <= baseChance -> {
                                    isEventActive = true
                                    if (zoneType == ZoneType.NATURA) {
                                        val loc = HeroLogic.generateLocation("NATURA")
                                        currentLocationName = loc
                                        logText = "Natura: $loc. Bohater zatrzymuje się i tworzy obozowisko."
                                        eventType = "Obozowisko"
                                    } else {
                                        val loc = HeroLogic.generateLocation("CYWILIZACJA")
                                        currentLocationName = loc
                                        logText = "Cywilizacja: $loc. Posterunek lub miejsce odpoczynku."
                                        eventType = "Postój"
                                    }
                                }
                                roll <= baseChance + 5 -> {
                                    isEventActive = true
                                    eventType = "Zasadzka"
                                    currentLocationName = "Niebezpieczeństwo!"
                                    logText = "Z krzaków wyskakują zbójcy! To zasadzka!"
                                }
                                roll <= baseChance + 15 -> {
                                    isEventActive = true
                                    eventType = "Wędrowiec"
                                    currentLocationName = "Spotkanie"
                                    logText = "Na horyzoncie widzisz innego podróżnego. Chcesz zagadać?"
                                }
                                else -> {
                                    logText = "Podróż przebiega spokojnie przez ${roadType.displayName.lowercase()}..."
                                    currentLocationName = roadType.displayName
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037)),
                    enabled = currentProgress < totalHours
                ) {
                    Text("KONTYNUUJ PODRÓŻ")
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = {
                        if (eventType == "Zasadzka" || eventType == "Przygoda") {
                            val intent = Intent(context, CombatActivity::class.java).apply {
                                putExtra("SELECTED_HERO_ID", heroId)
                                putExtra("COMBAT_TYPE", "AMBUSH")
                            }
                            context.startActivity(intent)
                        } else if (eventType == "Obozowisko" || eventType == "Postój") {
                            showRestPanel = true
                        }
                        
                        if (eventType != "Obozowisko" && eventType != "Postój") {
                            isEventActive = false
                            logText = "Ruszasz w dalszą drogę po zdarzeniu: $eventType."
                        }
                    }) {
                        Text(if (eventType == "Zasadzka" || eventType == "Przygoda") "WALCZ!" else if(eventType == "Obozowisko" || eventType == "Postój") "ODPOCZNIJ" else "Zbadaj")
                    }
                    Button(onClick = {
                        isEventActive = false
                        logText = "Omijasz $eventType i jedziesz dalej."
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text("Ignoruj")
                    }
                }
            }

            if (currentProgress >= totalHours) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("WEJDŹ DO MIASTA")
                }
            }
        }
    }

    if (showRestPanel) {
        TravelRestPanel(
            restHours = restHours,
            onHoursChange = { restHours = it },
            onConfirm = {
                try {
                    val hours = restHours.toInt()
                    val newTime = HeroLogic.advanceTime(dataNowStr, hours)
                    val updatedHeroData = HeroLogic.updateCalendarData(heroData, newTime)
                    context.openFileOutput(heroFile, Context.MODE_PRIVATE).use {
                        it.write(updatedHeroData.toByteArray())
                    }
                    heroData = updatedHeroData
                    dataNowStr = newTime
                    logText = "Odpocząłeś $hours h. Czujesz się lepiej."
                } catch (e: Exception) { e.printStackTrace() }
                showRestPanel = false
                isEventActive = false
            },
            onDismiss = { showRestPanel = false }
        )
    }
}

@Composable
fun TravelRestPanel(
    restHours: Float,
    onHoursChange: (Float) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("Odpoczynek", color = Color(0xFF7B1FA2), fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ile godzin chcesz odpoczywać?", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text("${restHours.toInt()} h", color = Color.Cyan, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = restHours,
                    onValueChange = { onHoursChange(it) },
                    valueRange = 4f..8f,
                    steps = 3,
                    onValueChangeFinished = onConfirm,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF7B1FA2),
                        activeTrackColor = Color(0xFF7B1FA2)
                    )
                )
                Text("Puść suwak, aby potwierdzić", color = Color.Gray, fontSize = 10.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("ANULUJ", color = Color.White) }
        }
    )
}
