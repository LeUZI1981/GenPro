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

class TravelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val heroIdString = intent.getStringExtra("HERO_ID") ?: "-1"
        val heroId = heroIdString.toIntOrNull() ?: -1
        val destination = intent.getStringExtra("DESTINATION") ?: "Nieznany cel"
        val totalHours = intent.getIntExtra("TRAVEL_TIME", 24)
        val roadTypeName = intent.getStringExtra("ROAD_TYPE") ?: "ROAD"
        val zoneTypeName = intent.getStringExtra("ZONE_TYPE") ?: "NATURA"
        val tagsString = intent.getStringExtra("TAGS") ?: ""
        
        setContent {
            GenProTheme {
                TravelScreen(
                    heroId = heroId,
                    destination = destination,
                    totalHours = totalHours,
                    roadType = try { RoadType.valueOf(roadTypeName) } catch(e: Exception) { RoadType.ROAD },
                    zoneType = try { ZoneType.valueOf(zoneTypeName) } catch(e: Exception) { ZoneType.NATURA },
                    tags = tagsString.split(",").mapNotNull { t -> try { LocationTag.valueOf(t) } catch(e: Exception) { null } },
                    onFinish = { finish() }
                )
            }
        }
    }
}

@Composable
fun TravelScreen(
    heroId: Int, 
    destination: String, 
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
                        // Prędkość zależy od typu drogi
                        val baseStep = (2..4).random()
                        val actualStep = (baseStep * roadType.speedMultiplier).toInt().coerceAtLeast(1)
                        currentProgress = (currentProgress + actualStep).coerceAtMost(totalHours)
                        
                        if (currentProgress >= totalHours) {
                            logText = "Dotarłeś do celu: $destination!"
                            currentLocationName = destination
                            
                            // Aktualizacja czasu...
                            try {
                                val nowStr = context.openFileInput("dataNow").bufferedReader().use { it.readText() }
                                val datePart = if (nowStr.contains("|")) nowStr.substringBefore("|").trim() else nowStr
                                val timePart = if (nowStr.contains("|")) nowStr.substringAfter("|").trim() else "12:00"
                                
                                val dParts = datePart.split(".")
                                var d = dParts.getOrNull(0)?.toIntOrNull() ?: 6
                                var m = dParts.getOrNull(1)?.toIntOrNull() ?: 6
                                var y = dParts.getOrNull(2)?.toIntOrNull() ?: 666
                                
                                val tParts = timePart.split(":")
                                var hh = tParts.getOrNull(0)?.toIntOrNull() ?: 12
                                val mm = tParts.getOrNull(1)?.toIntOrNull() ?: 0
                                
                                // Dodaj godziny podróży
                                hh += totalHours
                                
                                // Przelicz nadmiarowe godziny na dni
                                while (hh >= 24) {
                                    hh -= 24
                                    d++
                                    // Uproszczony kalendarz (28 dni w miesiącu)
                                    if (d > 28) {
                                        d = 1
                                        m++
                                    }
                                    if (m > 12) {
                                        m = 1
                                        y++
                                    }
                                }
                                
                                val newTimeStr = String.format(Locale.getDefault(), "%02d:%02d", hh, mm)
                                val newDateStr = "$d.$m.$y | $newTimeStr"
                                
                                context.openFileOutput("dataNow", Context.MODE_PRIVATE).use {
                                    it.write(newDateStr.toByteArray())
                                }
                                println("Zaktualizowano czas podróży: $newDateStr")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            // Generator zdarzeń zależny od strefy
                            val baseChance = if (zoneType == ZoneType.NATURA) 20 else 10
                            val roll = (1..100).random()
                            
                            when {
                                roll <= baseChance -> {
                                    isEventActive = true
                                    if (zoneType == ZoneType.NATURA) {
                                        if (tags.contains(LocationTag.FOREST)) {
                                            eventType = "Zasadzka"
                                            currentLocationName = "Gęsty Las"
                                            logText = "Z cienia drzew wychodzą postacie z łukami!"
                                        } else {
                                            eventType = "Dzicz"
                                            currentLocationName = "Obozowisko"
                                            logText = "Znajdujesz dobre miejsce na odpoczynek w dziczy."
                                        }
                                    } else {
                                        eventType = "Patrol"
                                        currentLocationName = "Posterunek"
                                        logText = "Straż dróg zatrzymuje Cię do rutynowej kontroli."
                                    }
                                }
                                roll <= baseChance + 10 -> {
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
                        }
                        
                        isEventActive = false
                        logText = "Ruszasz w dalszą drogę po zdarzeniu: $eventType."
                    }) {
                        Text(if (eventType == "Zasadzka" || eventType == "Przygoda") "WALCZ!" else "Zbadaj / Odpocznij")
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
}
