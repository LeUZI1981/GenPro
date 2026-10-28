package com.example.genpro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CapacitySummaryItem(icon: String, label: String, current: Int, limit: Int, unit: String = "") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 18.sp)
        val limitStr = if (limit >= 999) "-" else limit.toString()
        Text("$label: $current/$limitStr$unit", color = if (limit > 0 && current >= limit && label == "Suma") Color.Red else Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun WorkJobRow(title: String, requirement: String, isMet: Boolean, wagePerHour: Int, hours: Int, onWork: () -> Unit) {
    val totalWage = wagePerHour * hours
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(if (isMet) Color(0xFF1A1A1A) else Color(0xFF221111), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (isMet) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
            Text(requirement, color = if (isMet) Color.Green else Color.Red, fontSize = 11.sp)
            Text("Płaca: ${HeroLogic.formatPrice(totalWage)}", color = Color.Yellow, fontSize = 12.sp)
        }
        Button(
            onClick = onWork,
            enabled = isMet,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))
        ) {
            Text("PRACUJ", fontSize = 10.sp)
        }
    }
}

@Composable
fun WorkPanel(
    heroData: String,
    workHours: Float,
    onHoursChange: (Float) -> Unit,
    onWorkStart: (String, Int) -> Unit
) {
    val charisma = HeroLogic.getHeroCharisma(heroData)
    val musicRank = HeroLogic.getSkillRank(heroData, "Muzykalność")
    val hasAnimalCare = HeroLogic.hasSkill(heroData, "Opieka nad zwierzętami")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color(0xFF111111), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text("DORYWCZE PRACE", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Czas pracy: ${workHours.toInt()}h", color = Color.White)
        Slider(
            value = workHours,
            onValueChange = onHoursChange,
            valueRange = 1f..8f,
            steps = 6,
            colors = SliderDefaults.colors(thumbColor = Color.Yellow, activeTrackColor = Color.Yellow)
        )

        Spacer(modifier = Modifier.height(16.dp))

        WorkJobRow(
            title = "Posługiwanie klientom",
            requirement = "Char: 40 (Masz: $charisma)",
            isMet = charisma >= 40,
            wagePerHour = 2,
            hours = workHours.toInt()
        ) { onWorkStart("Posługiwanie", 2 * workHours.toInt()) }

        WorkJobRow(
            title = "Muzyk",
            requirement = "Muzykalność (Uczeń) (Masz: ${musicRank ?: "Brak"})",
            isMet = musicRank != null,
            wagePerHour = 5,
            hours = workHours.toInt()
        ) { onWorkStart("Muzyk", 5 * workHours.toInt()) }

        WorkJobRow(
            title = "Stajenny",
            requirement = "Opieka nad zwierzętami (Masz: ${if(hasAnimalCare) "Tak" else "Brak"})",
            isMet = hasAnimalCare,
            wagePerHour = 3,
            hours = workHours.toInt()
        ) { onWorkStart("Stajenny", 3 * workHours.toInt()) }
    }
}

@Composable
fun WalletItem(symbol: String, value: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(symbol, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(4.dp))
        Text(value.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ManualCoinRow(
    label: String,
    current: Int,
    max: Int,
    playSoundAction: (Int, Int) -> Unit,
    onUpdate: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, modifier = Modifier.width(80.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (current > 0) { onUpdate(current - 1); playSoundAction(R.raw.klik1, 0) } }) {
                Text("➖", color = Color.Cyan)
            }
            Text(current.toString(), color = Color.Yellow, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
            IconButton(onClick = { if (current < max) { onUpdate(current + 1); playSoundAction(R.raw.klik1, 0) } }) {
                Text("➕", color = Color.Cyan)
            }
            Text("/ $max", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun ShopItemRow(
    item: TradeItem, 
    isBuy: Boolean, 
    canRepair: Boolean = false, 
    repairCost: Int = 0, 
    onRepair: () -> Unit = {}, 
    onAction: () -> Unit
) {
    val qualityMatch = Regex("\\[Jakość: (\\d+)%\\]").find(item.name)
    val quality = qualityMatch?.groupValues?.get(1)?.toIntOrNull() ?: 100
    val displayName = item.name.replace(Regex("\\[Jakość: \\d+%\\]"), "").trim()

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.category, color = Color.Gray, fontSize = 11.sp)
                    if (quality < 100) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Jakość: $quality%", color = if (quality < 50) Color.Red else Color.Yellow, fontSize = 11.sp)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val price = if (isBuy) item.price else item.price / 2
                Text(HeroLogic.formatPrice(price), color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row {
                    if (canRepair) {
                        Button(
                            onClick = onRepair,
                            modifier = Modifier.height(32.dp).padding(end = 4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4527A0))
                        ) {
                            Text("NAPRAW (${HeroLogic.formatPrice(repairCost)})", fontSize = 9.sp)
                        }
                    }
                    
                    Button(
                        onClick = onAction,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isBuy) Color(0xFF2E7D32) else Color(0xFFC62828))
                    ) {
                        Text(if (isBuy) "KUP" else "SPRZEDAJ", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
