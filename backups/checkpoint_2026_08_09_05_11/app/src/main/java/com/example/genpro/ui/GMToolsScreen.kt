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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpro.R

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
    onNavigate: (Int) -> Unit
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

    val daysOfWeek = listOf("Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota", "Niedziela")

    fun getDayOfWeek(day: Int): String {
        return daysOfWeek[(day - 1) % 7]
    }

    fun advanceDate(currentDateStr: String): String {
        // Format: DATA: 6.06.666 | GODZINA: 12:00 | DZIEŃ: Poniedziałek
        val regex = Regex("DATA: (\\d+)\\.(\\d+)\\.(\\d+)")
        val timeRegex = Regex("GODZINA: (\\d+):(\\d+)")
        
        val match = regex.find(currentDateStr) ?: return "DATA: 6.06.666 | GODZINA: 12:00 | DZIEŃ: Poniedziałek"
        val timeMatch = timeRegex.find(currentDateStr)
        
        var d = match.groupValues[1].toInt()
        var m = match.groupValues[2].toInt()
        var y = match.groupValues[3].toInt()
        val hour = timeMatch?.groupValues?.get(1) ?: "12"
        val minute = timeMatch?.groupValues?.get(2) ?: "00"

        d++
        if (d > 28) {
            d = 1
            m++
        }
        if (m > 12) {
            m = 1
            y++
        }

        val newDayName = getDayOfWeek(d)
        return "DATA: $d.$m.$y | GODZINA: $hour:$minute | DZIEŃ: $newDayName"
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
                    val dayName = getDayOfWeek(d.coerceIn(1, 28))
                    val newDateStr = "DATA: $d.$m.$y | GODZINA: $hh:$mm | DZIEŃ: $dayName"
                    
                    if (editingGlobalDate) {
                        saveToFile("start_date_config", "$d.$m.$y | $hh:$mm")
                        editingGlobalDate = false
                    } else {
                        val currentData = readFromFile(editingDateHeroFile!!)
                        val lines = currentData.lines().toMutableList()
                        val dateIdx = lines.indexOfFirst { it.startsWith("DATA:") }
                        if (dateIdx != -1) {
                            lines[dateIdx] = newDateStr
                        } else {
                            lines.add(newDateStr)
                        }
                        saveToFile(editingDateHeroFile!!, lines.joinToString("\n"))
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("NARZĘDZIA MG", color = Color.Red, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = nosiferFamily)
        Spacer(modifier = Modifier.height(16.dp))

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
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("USTAW DATĘ STARTOWĄ NOWEJ GRY", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("ZARZĄDZANIE BOHATERAMI (${heroFiles.size}/10)", color = Color.Gray, fontSize = 14.sp)
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(heroFiles) { fileName ->
                val heroData = readFromFile(fileName)
                val lines = heroData.lines().toMutableList()
                val name = lines.getOrNull(0) ?: "Nieznany"
                val pdLine = lines.find { it.startsWith("PD:") } ?: "PD: 0"
                val pd = pdLine.substringAfter("PD:").trim()
                
                val rasaKlasaLine = lines.getOrNull(1) ?: ""
                val rasa = rasaKlasaLine.substringAfter("Rasa: ").substringBefore(",").trim().ifEmpty { "Rasa" }
                val klasa = rasaKlasaLine.substringAfter("Klasa: ").trim().ifEmpty { "Klasa" }
                val poziomLine = lines.find { it.startsWith("POZIOM:") } ?: "POZIOM: 1"
                val poziom = poziomLine.substringAfter("POZIOM:").trim().split(" ")[0].substringBefore("(").substringBefore("｢").trim()
                
                val dateIdx = lines.indexOfFirst { it.startsWith("DATA:") }
                val dateLine = if (dateIdx != -1) lines[dateIdx] else "DATA: 6.06.666 | DZIEŃ: Poniedziałek"

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
                    
                    Text(dateLine, color = Color.Cyan.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom part: Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val nextDate = advanceDate(dateLine)
                                if (dateIdx != -1) {
                                    lines[dateIdx] = nextDate
                                } else {
                                    lines.add(nextDate)
                                }
                                saveToFile(fileName, lines.joinToString("\n"))
                                playSound(R.raw.klik1, 0)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("+1 DZIEŃ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row {
                            IconButton(onClick = {
                                val parts = dateLine.split("|")
                                val datePart = parts.find { it.contains("DATA:") }?.substringAfter("DATA:")?.trim() ?: "6.06.666"
                                val timePart = parts.find { it.contains("GODZINA:") }?.substringAfter("GODZINA:")?.trim() ?: "12:00"
                                
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
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
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
