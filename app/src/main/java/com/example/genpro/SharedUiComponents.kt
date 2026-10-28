package com.example.genpro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
fun WorkJobRow(title: String, requirement: String, isMet: Boolean, wagePerHour: Int, hours: Int, isTooTired: Boolean = false, onWork: () -> Unit) {
    val totalWage = wagePerHour * hours
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(if (isMet && !isTooTired) Color(0xFF1A1A1A) else Color(0xFF221111), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (isMet && !isTooTired) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
            Text(
                if (isTooTired) "Zbyt zmęczony" else requirement, 
                color = if (isTooTired) Color.Red else if (isMet) Color.Green else Color.Red, 
                fontSize = 11.sp
            )
            Text("Płaca: ${HeroLogic.formatPrice(totalWage)}", color = Color.Yellow, fontSize = 12.sp)
        }
        Button(
            onClick = onWork,
            enabled = isMet && !isTooTired,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))
        ) {
            Text("PRACUJ", fontSize = 10.sp)
        }
    }
}

@Composable
fun WorkPanel(
    heroData: String,
    buildingType: String = "",
    workHours: Float,
    onHoursChange: (Float) -> Unit,
    onWorkStart: (String, Int) -> Unit,
    buildingRank: Int = 1,
    heroProgress: HeroProgress? = null
) {
    val charisma = HeroLogic.getHeroCharisma(heroData)
    val musicRank = HeroLogic.getSkillRank(heroData, "Muzykalność")
    val cookRank = HeroLogic.getSkillRank(heroData, "Gotowanie")
    val hasAnimalCare = HeroLogic.hasSkill(heroData, "Opieka nad zwierzętami")
    val strength = HeroLogic.getHeroStrength(heroData)
    val gender = HeroLogic.getHeroGender(heroData)
    
    val currentFatigue = heroProgress?.fatigue ?: 0
    val maxFatigue = heroProgress?.maxFatigue ?: 100
    val fatiguePerHour = 10
    val predictedFatigue = (currentFatigue + (workHours.toInt() * fatiguePerHour)).coerceAtMost(maxFatigue)
    val isTooTired = currentFatigue >= (maxFatigue * 0.9).toInt()
    val willBeTooTired = predictedFatigue >= (maxFatigue * 0.9).toInt()
    
    val buildingRankBonus = (buildingRank - 1) * 2
    val hasInstrument = heroData.lines().any { it.contains("Lutnia", ignoreCase = true) || it.contains("Harfa", ignoreCase = true) || it.contains("Lira", ignoreCase = true) || it.contains("Mandora", ignoreCase = true) || it.contains("Psalterium", ignoreCase = true) || it.contains("Flet", ignoreCase = true) || it.contains("Dudy", ignoreCase = true) || it.contains("Szałamaja", ignoreCase = true) || it.contains("Róg sygnałowy", ignoreCase = true) || it.contains("Okaryna", ignoreCase = true) || it.contains("Bębenek", ignoreCase = true) || it.contains("Tamburyn", ignoreCase = true) || it.contains("Kołatka", ignoreCase = true) || it.contains("Czynele", ignoreCase = true) || it.contains("Kocioł", ignoreCase = true) }

    val isInn = buildingType in listOf("Karczma", "Tawerna", "Gospoda", "Oberża", "Speluna")
    val isMedic = buildingType.contains("MEDYK", true) || buildingType.contains("APTEKA", true)

    val lines = heroData.lines()
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .heightIn(max = 550.dp)
            .background(Color(0xFF111111), RoundedCornerShape(12.dp))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("DORYWCZE PRACE", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isTooTired) {
            Text("⚠️ ZMĘCZENIE NA MAKSIMUM!", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Jesteś zbyt zmęczony by pracować. Musisz się odespocząć!", color = Color.Red, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        Text("Czas pracy: ${workHours.toInt()}h", color = Color.White)
        Slider(
            value = workHours,
            onValueChange = { if (!willBeTooTired) onHoursChange(it) else if (it.toInt() < workHours.toInt()) onHoursChange(it) },
            valueRange = 1f..12f,
            steps = 10,
            colors = SliderDefaults.colors(
                thumbColor = if (willBeTooTired) Color.Red else Color.Yellow, 
                activeTrackColor = if (willBeTooTired) Color.Red else Color.Yellow
            ),
            enabled = !isTooTired
        )
        
        Text(
            "Przewidywane zmęczenie po pracy: $predictedFatigue / $maxFatigue (${(predictedFatigue * 100) / maxFatigue}%)",
            color = when {
                willBeTooTired -> Color.Red
                predictedFatigue > (maxFatigue * 0.7) -> Color.Yellow
                else -> Color.Green
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        val charRankBonus = HeroLogic.getRankBonus(heroData.lines(), "Charyzma")
        val baseWage = (6..11).random() + (buildingRank - 1) * 2
        val wagePerHourServing = baseWage + charRankBonus

        WorkJobRow(
            title = "Posługiwanie klientom",
            requirement = "Char: 30 (Masz: $charisma)",
            isMet = charisma >= 30,
            wagePerHour = wagePerHourServing,
            hours = workHours.toInt(),
            isTooTired = isTooTired,
            onWork = { onWorkStart("Charyzma", wagePerHourServing * workHours.toInt()) }
        )

        if (isInn) {
            val cookRankInt = HeroLogic.getSkillRank(heroData.lines(), "Gotowanie")
            val jobTitle = if (gender == "F") "Kucharka" else "Kucharz"
            val cookWage = 35 + (cookRankInt * 5) + buildingRankBonus
            WorkJobRow(
                title = jobTitle,
                requirement = "Gotowanie (Uczeń) (Masz: ${cookRank ?: "Brak"})",
                isMet = cookRankInt >= 1,
                wagePerHour = cookWage,
                hours = workHours.toInt(),
                isTooTired = isTooTired,
                onWork = { onWorkStart("Gotowanie", cookWage * workHours.toInt()) }
            )
        }

        val musicSkillBonus = HeroLogic.getSkillRank(heroData.lines(), "Muzyka") * 10
        val musicBaseWage = 40 + musicSkillBonus + buildingRankBonus
        val musicWageWithInstrument = if (hasInstrument) musicBaseWage + 10 else musicBaseWage
        
        WorkJobRow(
            title = "Muzyk",
            requirement = "Muzyka (Dowolna) (Masz: ${musicRank ?: "Brak"})${if(hasInstrument) " ✦ Instrument" else ""}",
            isMet = musicRank != null,
            wagePerHour = musicWageWithInstrument,
            hours = workHours.toInt(),
            isTooTired = isTooTired,
            onWork = { onWorkStart("Muzyka", musicWageWithInstrument * workHours.toInt()) }
        )

        WorkJobRow(
            title = "Stajenny",
            requirement = "Opieka nad zwierzętami (Masz: ${if(hasAnimalCare) "Tak" else "Brak"})",
            isMet = hasAnimalCare,
            wagePerHour = 25 + buildingRankBonus,
            hours = workHours.toInt(),
            isTooTired = isTooTired,
            onWork = { onWorkStart("Stajenny", (25 + buildingRankBonus) * workHours.toInt()) }
        )
        
        WorkJobRow(
            title = "Praca fizyczna",
            requirement = "Siła: 30 (Masz: $strength)",
            isMet = strength >= 30,
            wagePerHour = 15 + buildingRankBonus,
            hours = workHours.toInt(),
            isTooTired = isTooTired,
            onWork = { onWorkStart("Praca fizyczna", (15 + buildingRankBonus) * workHours.toInt()) }
        )

        WorkJobRow(
            title = "Pomocnik rzemieślnika",
            requirement = "Int: 30 (Masz: ${HeroLogic.getStatValue(heroData, "Int")})",
            isMet = HeroLogic.getStatValue(heroData, "Int") >= 30,
            wagePerHour = 18 + buildingRankBonus,
            hours = workHours.toInt(),
            isTooTired = isTooTired,
            onWork = { onWorkStart("Pomocnik", (18 + buildingRankBonus) * workHours.toInt()) }
        )

        if (craftSkills.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("RZEMIOSŁO", color = Color.Cyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            craftSkills.forEach { skill ->
                val workshopWage = 50 + buildingRankBonus
                WorkJobRow(
                    title = "Warsztat: $skill",
                    requirement = "Posiadasz umiejętność",
                    isMet = true,
                    wagePerHour = workshopWage,
                    hours = workHours.toInt(),
                    isTooTired = isTooTired,
                    onWork = { onWorkStart("Rzemiosło: $skill", workshopWage * workHours.toInt()) }
                )
            }
        }

        if (isMedic) {
            val chirRank = HeroLogic.getSkillRank(heroData.lines(), "Chirurgia")
            val leczenieRank = HeroLogic.getSkillRank(heroData.lines(), "Leczenie ran")
            val chirWage = 70 + (chirRank * 20) + buildingRankBonus
            val woundWage = 50 + (leczenieRank * 15) + buildingRankBonus
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("USŁUGI MEDYCZNE", color = Color(0xFFE91E63), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            
            WorkJobRow(
                title = "Zabiegi chirurgiczne",
                requirement = "Chirurgia (Uczeń) (Masz: ${chirRank})",
                isMet = chirRank >= 1,
                wagePerHour = chirWage,
                hours = workHours.toInt(),
                isTooTired = isTooTired,
                onWork = { onWorkStart("Chirurgia", chirWage * workHours.toInt()) }
            )
            
            WorkJobRow(
                title = "Opatrywanie rannych",
                requirement = "Leczenie ran (Uczeń) (Masz: ${leczenieRank})",
                isMet = leczenieRank >= 1,
                wagePerHour = woundWage,
                hours = workHours.toInt(),
                isTooTired = isTooTired,
                onWork = { onWorkStart("Leczenie ran", woundWage * workHours.toInt()) }
            )
        }
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
    val weight = HeroLogic.getItemWeight(item.name)
    val qualityMatch = Regex("\\[Jakość: (\\d+)%\\]").find(item.name)
    val quality = qualityMatch?.groupValues?.get(1)?.toIntOrNull() ?: 100
    val displayName = item.name.replace(Regex("\\[Jakość: \\d+%\\]"), "").trim()

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        border = BorderStroke(1.dp, if(isBuy) Color.Green.copy(alpha = 0.3f) else Color.Red.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).clickable { onAction() }) {
                Text(displayName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${weight}kg | ${item.category}", color = Color.Gray, fontSize = 11.sp)
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
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

@Composable
fun CityActionButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: String = "",
    accentColor: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF121212),
            contentColor = accentColor
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon.isNotEmpty()) {
                Text(icon, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                fontSize = 14.sp
            )
        }
    }
}
