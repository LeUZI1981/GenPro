package com.example.genpro.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpro.R
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.example.genpro.HeroProgress
import com.example.genpro.HeroLogic

@Composable
fun GMToolsScreen(
    readFromFile: (String) -> String,
    saveToFile: (String, String) -> Unit,
    context: Context,
    onBack: () -> Unit,
    nosiferFamily: FontFamily,
    slaboFamily: FontFamily,
    playSound: (Int, Int) -> Unit,
    onGMModeChange: (Boolean) -> Unit,
    onHeroSelected: (Int) -> Unit,
    onNavigate: (Int) -> Unit,
    onEditingChange: (Boolean) -> Unit
) {
    val heroFiles = context.fileList()
        .filter { it.matches(Regex("hero_\\d+")) }
        .sortedBy { it.substringAfter("_").toInt() }

    var editingDateHeroFile by remember { mutableStateOf<String?>(null) }
    var editingGlobalDate by remember { mutableStateOf(false) }
    var editDay by remember { mutableStateOf("6") }
    var editMonth by remember { mutableStateOf("6") }
    var editYear by remember { mutableStateOf("666") }
    var editHour by remember { mutableStateOf("12") }
    var editMinute by remember { mutableStateOf("00") }
    var isEditing by remember { mutableStateOf(false) }
    var editingFileName by remember { mutableStateOf<String?>(null) }
    var editingFileContent by remember { mutableStateOf("") }

    val daysOfWeek = listOf("Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota", "Niedziela")

    fun getDayOfWeek(day: Int): String {
        return daysOfWeek[(day - 1) % 7]
    }

    if (editingDateHeroFile != null || editingGlobalDate) {
        AlertDialog(
            onDismissRequest = { 
                editingDateHeroFile = null
                editingGlobalDate = false
            },
            title = { Text(if (editingGlobalDate) "DOMYŚLNA DATA STARTOWA" else "EDYTUJ DATĘ BOHATERA") },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editDay,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) editDay = it },
                            label = { Text("Dzień") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editMonth,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) editMonth = it },
                            label = { Text("Miesiąc") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editYear,
                            onValueChange = { if (it.all { c -> c.isDigit() } || it.startsWith("-")) editYear = it },
                            label = { Text("Rok") },
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editHour,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) editHour = it },
                            label = { Text("Godz") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editMinute,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) editMinute = it },
                            label = { Text("Min") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val d = editDay.toIntOrNull() ?: 1
                    val m = editMonth.toIntOrNull() ?: 1
                    val y = editYear.toIntOrNull() ?: 666
                    val hh = editHour.padStart(2, '0')
                    val mm = editMinute.padStart(2, '0')
                    
                    if (editingGlobalDate) {
                        saveToFile("start_date_config", "$d.$m.$y | $hh:$mm")
                        editingGlobalDate = false
                    } else {
                        val fileName = editingDateHeroFile!!
                        val heroId = fileName.substringAfter("_")
                        val currentData = readFromFile(fileName)
                        
                        // Sync to HeroProgress JSON
                        val progressFile = "progress_$heroId.json"
                        try {
                            val heroProgress = try {
                                val json = context.openFileInput(progressFile).bufferedReader().use { it.readText() }
                                Json.decodeFromString<HeroProgress>(json)
                            } catch (e: Exception) {
                                HeroLogic.getHeroProgress(heroId, currentData)
                            }
                            
                            val parts = heroProgress.currentCalendar.split("|").map { it.trim() }
                            val city = parts.getOrNull(0) ?: "Nieznane Miejsce"
                            val rest = if (parts.size > 3) parts.drop(3).joinToString(" | ") else "[EXTERNAL]"
                            val newCalendarJson = "$city | $d.$m.$y | $hh:$mm | $rest"
                            
                            val updatedProgress = heroProgress.copy(currentCalendar = newCalendarJson)
                            saveToFile(progressFile, Json.encodeToString(updatedProgress))
                        } catch (e: Exception) { e.printStackTrace() }

                        // Clean and save hero text file
                        val cleanHeroData = HeroLogic.stripLegacyProgressTags(currentData)
                        saveToFile(fileName, cleanHeroData)

                        editingDateHeroFile = null
                    }
                    playSound(R.raw.klik1, 0)
                }) {
                    Text("ZAPISZ")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    editingDateHeroFile = null 
                    editingGlobalDate = false
                }) {
                    Text("ANULUJ")
                }
            }
        )
    }

    if (editingFileName != null) {
        // Editor for single file
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Edycja: ${editingFileName}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = editingFileContent,
                onValueChange = { editingFileContent = it },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                textStyle = TextStyle(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { editingFileName = null },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)
                ) {
                    Text("ANULUJ")
                }
                Button(
                    onClick = {
                        saveToFile(editingFileName!!, editingFileContent)
                        editingFileName = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("ZAPISZ")
                }
            }
        }
    } else if (isEditing) {
        // Full editor overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("EDYTOR PLIKÓW", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { isEditing = false }) {
                    Icon(Icons.Default.Close, contentDescription = "Zamknij edytor", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            val allFiles = context.fileList().map { it to readFromFile(it) }
            val sortedFiles = allFiles.filter { it.first != "music_setting" && it.first != "sounds_setting" }.sortedBy { it.first } +
                    allFiles.filter { it.first == "sounds_setting" } +
                    allFiles.filter { it.first == "music_setting" }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sortedFiles) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.first, color = Color.White, fontSize = 14.sp)
                            Text(item.second, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                        }
                        IconButton(onClick = {
                            editingFileName = item.first
                            editingFileContent = item.second
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edytuj", tint = Color.White)
                        }
                    }
                    HorizontalDivider(color = Color.DarkGray)
                }
            }
            Button(
                onClick = { isEditing = false },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("ZAMKNIJ EDYTOR")
            }
        }
    } else {
        Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("NARZĘDZIA MG", color = Color(0xFFC31EFF), fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = nosiferFamily)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val globalDate = readFromFile("start_date_config").ifEmpty { "6.06.666 | 12:00" }
                    val datePart = globalDate.substringBefore(" |").trim()
                    val dParts = datePart.split(".")
                    editDay = dParts.getOrNull(0) ?: "6"
                    editMonth = dParts.getOrNull(1) ?: "6"
                    editYear = dParts.getOrNull(2) ?: "666"

                    val timePart = if (globalDate.contains("|")) globalDate.substringAfter("|").trim() else "12:00"
                    val tParts = timePart.split(":")
                    editHour = tParts.getOrNull(0) ?: "12"
                    editMinute = tParts.getOrNull(1) ?: "00"

                    editingGlobalDate = true
                    playSound(R.raw.klik1, 0)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037)),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                //Text("USTAW DATĘ STARTOWĄ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }


            Button(
                onClick = {
                    isEditing = true
                    playSound(R.raw.klik1, 0)
                },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                //Text("EDYTOR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    onNavigate(70)
                    playSound(R.raw.klik1, 0)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64)),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("🏚", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("ZARZĄDZANIE BOHATERAMI (${heroFiles.size}/10)", color = Color.Gray, fontSize = 14.sp)
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(heroFiles) { fileName ->
                val heroData = readFromFile(fileName)
                val lines = heroData.lines()
                val name = lines.getOrNull(0) ?: "Nieznany"
                val pdLine = lines.find { it.startsWith("PD:") } ?: "PD: 0"
                val pd = pdLine.substringAfter("PD:").trim()
                
                val rasaKlasaLine = lines.getOrNull(1) ?: ""
                val rasa = rasaKlasaLine.substringAfter("Rasa: ").substringBefore(",").trim().ifEmpty { "Rasa" }
                val klasa = rasaKlasaLine.substringAfter("Klasa: ").trim().ifEmpty { "Klasa" }
                val poziomLine = lines.find { it.startsWith("POZIOM:") } ?: "POZIOM: 1"
                val poziom = poziomLine.substringAfter("POZIOM:").trim().split(" ")[0].substringBefore("(").substringBefore("｢").trim()
                
                val heroId = fileName.substringAfter("_")
                val progressFile = "progress_$heroId.json"
                val heroProgress = remember(heroData) {
                    try {
                        val json = context.openFileInput(progressFile).bufferedReader().use { it.readText() }
                        Json.decodeFromString<HeroProgress>(json)
                    } catch (e: Exception) {
                        HeroLogic.getHeroProgress(heroId, heroData)
                    }
                }

                val calParts = heroProgress.currentCalendar.split("|").map { it.trim() }
                val displayDate = if (calParts.size >= 3) {
                    val dateStr = calParts[1]
                    val timeStr = calParts[2]
                    val dayNum = dateStr.substringBefore(".").toIntOrNull() ?: 1
                    val dayName = getDayOfWeek(dayNum.coerceIn(1, 28))
                    "DATA: $dateStr | GODZINA: $timeStr | DZIEŃ: $dayName"
                } else {
                    "DATA: 6.06.666 | DZIEŃ: Poniedziałek"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    // Top part: Name, Race, Class, Level
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("$rasa $klasa (Poz: $poziom)", color = Color.LightGray, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("PD: $pd", color = Color.Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Text(displayDate, color = Color.Cyan.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom part: Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val heroId = fileName.substringAfter("_")
                                val currentData = readFromFile(fileName)
                                
                        // Sync to HeroProgress JSON
                        try {
                            val progressFile = "progress_$heroId.json"
                            val heroProgress = try {
                                val json = context.openFileInput(progressFile).bufferedReader().use { it.readText() }
                                Json.decodeFromString<HeroProgress>(json)
                            } catch (e: Exception) {
                                HeroLogic.getHeroProgress(heroId, currentData)
                            }
                            
                            val nextProgress = HeroLogic.advanceTimeMinutesProgress(heroProgress, 24 * 60)
                            saveToFile(progressFile, Json.encodeToString(nextProgress))
                        } catch (e: Exception) { e.printStackTrace() }

                                // Clean and save hero text file
                                val cleanHeroData = HeroLogic.stripLegacyProgressTags(currentData)
                                saveToFile(fileName, cleanHeroData)

                                playSound(R.raw.klik1, 0)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("+1 DZIEŃ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row {
                            IconButton(onClick = {
                                val calParts = heroProgress.currentCalendar.split("|").map { it.trim() }
                                val datePart = if (calParts.size >= 2) calParts[1] else "6.06.666"
                                val timePart = if (calParts.size >= 3) calParts[2] else "12:00"
                                
                                val dParts = datePart.split(".")
                                editDay = dParts.getOrNull(0) ?: "6"
                                editMonth = dParts.getOrNull(1) ?: "6"
                                editYear = dParts.getOrNull(2) ?: "666"
                                
                                val tParts = timePart.split(":")
                                editHour = tParts.getOrNull(0) ?: "12"
                                editMinute = tParts.getOrNull(1) ?: "00"
                                
                                editingDateHeroFile = fileName
                                playSound(R.raw.klik1, 0)
                            }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Edytuj Datę", tint = Color.Cyan)
                            }

                            IconButton(onClick = {
                                val id = fileName.substringAfter("_").toIntOrNull() ?: 1
                                onHeroSelected(id)
                                onNavigate(9) // Edycja surowych danych
                                playSound(R.raw.klik1, 0)
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edytuj Kartę", tint = Color.Cyan)
                            }

                            IconButton(onClick = {
                                val id = fileName.substringAfter("_").toIntOrNull() ?: 1
                                onHeroSelected(id)
                                onNavigate(10)
                            }) {
                                Icon(Icons.Default.Person, contentDescription = "Karta Postaci", tint = Color.Yellow)
                            }

                            IconButton(onClick = { 
                                context.deleteFile(fileName)
                                context.deleteFile("${fileName}_pd_history")
                                playSound(R.raw.roll, 1)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    saveToFile("gm_mode_setting", "off")
                    onGMModeChange(false)
                    onBack()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White )
            ) {
                Text("MG OFF", fontWeight = FontWeight.Bold)
            }
            
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("POWRÓT")
            }
        }
    }
}
}
