package com.example.genpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpro.BuildingGenerator
import com.example.genpro.BuildingInstance
import com.example.genpro.R

@Composable
fun BuildingGeneratorScreen(
    onBack: () -> Unit,
    nosiferFamily: FontFamily,
    slaboFamily: FontFamily,
    playSound: (Int, Int) -> Unit,
    saveToFile: (String, String) -> Unit,
    readFromFile: (String) -> String
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        var selectedCategory by remember { mutableStateOf("Gospody") }
        var generatedBuilding by remember { mutableStateOf<BuildingInstance?>(null) }

        val categories = listOf("Gospody", "Hotele", "Zamtuz")
        val gospodyTypes = listOf("Karczma", "Tawerna", "Oberża")
        val hoteleTypes = listOf("Hotel", "Motel", "Pensjonat")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "GENERATOR LOKALI",
                color = Color.Yellow,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = nosiferFamily
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { 
                            selectedCategory = cat
                            generatedBuilding = null
                            playSound(R.raw.klik1, 0)
                        },
                        label = { Text(cat, color = if (selectedCategory == cat) Color.White else Color.Gray) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD32F2F)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Type selection based on category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = when (selectedCategory) {
                    "Gospody" -> gospodyTypes
                    "Hotele" -> hoteleTypes
                    else -> listOf("Zamtuz")
                }

                types.forEach { type ->
                    Button(
                        onClick = {
                            generatedBuilding = BuildingGenerator.generateBuilding(type)
                            playSound(R.raw.roll, 0)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64))
                    ) {
                        Text(type, fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Result Area
            Box(modifier = Modifier.weight(1f)) {
                generatedBuilding?.let { building ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                BuildingGenerator.getRankStars(building.rank),
                                color = Color.Yellow,
                                fontSize = 20.sp
                            )
                            IconButton(onClick = {
                                generatedBuilding = BuildingGenerator.generateBuilding(building.type)
                                playSound(R.raw.roll, 0)
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Losuj ponownie", tint = Color.LightGray)
                            }
                        }

                        Text(
                            building.name,
                            color = Color.Cyan,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = slaboFamily
                        )
                        
                        Text(
                            "Typ: ${building.type}",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.5f))

                        building.innDetails?.let { details ->
                            InfoRow("Kondygnacje", if (details.floors > 1) "${details.floors} (piętrowy)" else "Parterowy")
                            InfoRow("Pojemność", "${details.seatingCapacity} osób")
                            InfoRow("Stoły", "${details.tables} dużych, ${details.smallTables} małych")
                            
                            if (details.barDescription.isNotEmpty()) {
                                InfoRow("Szynkwas", details.barDescription)
                            }
                            
                            InfoRow("Muzyka", if (details.hasMusicians) "Bard / Muzycy na żywo" else "Brak stałej oprawy")
                            InfoRow("Obsługa", details.serviceQuality)
                            
                            if (building.rank > 3) {
                                InfoRow("Ochrona", details.security)
                            }

                            if (details.smell.isNotEmpty() && (building.type in gospodyTypes || building.type == "Karczma" || building.type == "Tawerna" || building.type == "Oberża")) {
                                InfoRow("Zapach", details.smell)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "OPIS LOKALU:",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        
                        val description = if (building.description.isEmpty()) BuildingGenerator.generateDescription(building) else building.description
                        
                        Text(
                            description,
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        
                        if (building.rooms.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("POMIESZCZENIA:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            building.rooms.forEach { room ->
                                Text("• ${room.name} (${room.area.toInt()} m²)", color = Color.LightGray, fontSize = 14.sp)
                            }
                        }

                        if (building.inventory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("ASORTYMENT:", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            building.inventory.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${item.name} (${item.quality}%)", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    Text("Kup: ${item.buyPrice} zł", color = Color.Green, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sprzed: ${item.sellPrice} zł", color = Color.Red, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Wybierz lokal do wygenerowania", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("POWRÓT")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.LightGray, fontSize = 16.sp)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}
