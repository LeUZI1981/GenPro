package com.example.genpro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.genpro.HeroLogic
import com.example.genpro.MainActivity
import com.example.genpro.R
import com.example.genpro.SettlementGenerator
import com.example.genpro.SettlementInstance
import com.example.genpro.SettlementType
import com.example.genpro.DistrictType
import com.example.genpro.getLevelRequirements
import com.example.genpro.getStatCaps
import kotlinx.serialization.json.Json
import kotlin.random.Random

var poziomStartowy = 0
@Composable
fun CharacterSummary(
    heroName: String,
    heroPortraitUri: String?,
    selectedEquipment: Set<String>,
    wygenerowaneStaty: List<Triple<String, IntArray, Int>>?,
    wybranaKlasaIndex: Int,
    wybranaRasaIndex: Int,
    wybranaPlecIndex: Int,
    wybranyWiekIndex: Int,
    wybranyPoziom: Int,
    cechyFizyczne: Map<String, String>,
    wylosowaneCechyCharakteru: List<String>,
    wylosowaneSpecjalne: List<String>,
    wylosowaneNormalne: List<String>,
    wybranyAdept: String,
    statyDrugorzedne: Map<String, Int>,
    listaWiek: Array<String>,
    slaboFamily: FontFamily,
    lobsterFamily: FontFamily,
    isGMMode: Boolean,
    readFromFile: (String) -> String,
    saveToFile: (String, String) -> Unit,
    playSound: (Int, Int) -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val daysOfWeek = listOf("Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota", "Niedziela")
    fun getDayOfWeek(day: Int): String = daysOfWeek[(day - 1) % 7]
    val rasa = MainActivity.listaRasa[wybranaRasaIndex]
    val plec = MainActivity.listaGender[wybranaPlecIndex]
    val wiek = listaWiek[wybranyWiekIndex]

    val dostepneKlasy = MainActivity.listaKlasa.filterIndexed { index, _ ->
        val statIdx = when(index) {
            0 -> 0 // Wojownik -> WW
            1 -> 4 // Łotr -> Zręczność
            2 -> 1 // Łowca -> Strzelectwo
            3 -> 5 // Uczony -> Inteligencja
            4 -> 7 // Kapłan -> Charyzma
            5 -> 6 // Mag -> Siła Woli
            else -> 0
        }
        val stat = wygenerowaneStaty?.getOrNull(statIdx)
        if (stat != null) (stat.second[1] + stat.second[2]) > 11 else false
    }
    val klasa = dostepneKlasy.getOrNull(wybranaKlasaIndex) ?: "Brak"

    val statCaps = getStatCaps(klasa, wybranyPoziom)
    val plusMaxWW = statCaps.plusMaxWW; val plusMaxUS = statCaps.plusMaxUS; val plusMaxKrz = statCaps.plusMaxKrz; val plusMaxOdp = statCaps.plusMaxOdp
    val plusMaxZr = statCaps.plusMaxZr; val plusMaxInt = statCaps.plusMaxInt; val plusMaxSW = statCaps.plusMaxSW; val plusMaxCha = statCaps.plusMaxCha
    val plusMaxAtk = statCaps.plusMaxAtk; val plusMaxSz = statCaps.plusMaxSz; val plusMaxZw = statCaps.plusMaxZw; val plusMaxMag = statCaps.plusMaxMag

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 0.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val plecSymbol = if (plec == "Mężczyzna") "♂️" else "♀️"
        Text(
            text = heroName,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = lobsterFamily
        )
        Text(
            text = "$rasa $plecSymbol $klasa",
            color = Color.Yellow,
            fontSize = 16.sp,
            fontFamily = slaboFamily
        )
/*
        if (heroPortraitUri != null) {
            Spacer(modifier = Modifier.height(8.dp))
            AsyncImage(
                model = heroPortraitUri,
                contentDescription = "Portrait",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = painterResource(id = R.drawable.unknowhero),
                contentDescription = "Default Portrait",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )
        }

 */

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray)

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                wygenerowaneStaty?.filterIndexed { index, _ -> index < 8 }?.forEach { stat ->
                    val bonus = stat.second.getOrNull(3) ?: 0
                    val statColor = if (bonus > 0) Color.Yellow else if (bonus < 0) Color.Red else Color.White
                    
                    val maxAdvances = when(stat.first) {
                        "WW" -> plusMaxWW; "US" -> plusMaxUS; "K" -> plusMaxKrz; "Odp" -> plusMaxOdp
                        "Zr" -> plusMaxZr; "Int" -> plusMaxInt; "SW" -> plusMaxSW; "Cha" -> plusMaxCha
                        else -> 0
                    }
                    val currentAdvances = when(stat.first) {
                        "WW" -> plusMaxWW; "US" -> plusMaxUS; "K" -> plusMaxKrz; "Odp" -> plusMaxOdp
                        "Zr" -> plusMaxZr; "Int" -> plusMaxInt; "SW" -> plusMaxSW; "Cha" -> plusMaxCha
                        else -> 0
                    }
                    val totalWithAdvances = stat.third + (currentAdvances * 5)
                    
                    val k1 = stat.second.getOrNull(1) ?: 0
                    val k2 = stat.second.getOrNull(2) ?: 0
                    val diceSum = k1 + k2
                    val rollColor = when {
                        diceSum < 6 -> Color.Red
                        diceSum > 16 -> Color.Yellow
                        else -> Color.White
                    }

                    val valueColor = when {
                        currentAdvances < maxAdvances -> Color(0xFF33FF00)
                        else -> rollColor
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val fullName = when(stat.first) {
                            "WW" -> "Walka Wręcz"
                            "US" -> "Strzelectwo"
                            "K" -> "Krzepa"
                            "Odp" -> "Odporność"
                            "Zr" -> "Zręczność"
                            "Int" -> "Inteligencja"
                            "SW" -> "Siła Woli"
                            "Cha" -> "Charyzma"
                            else -> stat.first
                        }
                        Text(fullName, color = if (statColor != Color.White) statColor else Color.LightGray, fontSize = 12.sp)
                        Text("$totalWithAdvances", color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))
            VerticalDivider(color = Color.DarkGray, modifier = Modifier.height(220.dp))
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                val displayedSecKeys = mutableSetOf<String>()
                statyDrugorzedne.forEach { (name, value) ->
                    val cleanName = when (name) {
                        "S" -> "Siła"
                        "Wt" -> "Wytrzymałość"
                        "Sz" -> "Szybkość"
                        "Żyw" -> "Żywotność"
                        "A" -> "Ataki"
                        "M" -> "Magia"
                        "PO" -> "Obłęd"
                        "PP" -> "Przeznaczenie"
                        else -> name
                    }
                    if (displayedSecKeys.contains(cleanName)) return@forEach
                    displayedSecKeys.add(cleanName)
                    
                    val maxAdvance = when(cleanName) {
                        "Ataki" -> plusMaxAtk
                        "Szybkość" -> plusMaxSz
                        "Żywotność" -> plusMaxZw
                        "Magia" -> plusMaxMag
                        else -> 0
                    }
                    val currentAdvance = when(cleanName) {
                        "Ataki" -> plusMaxAtk
                        "Szybkość" -> plusMaxSz
                        "Żywotność" -> plusMaxZw
                        "Magia" -> plusMaxMag
                        else -> 0
                    }
                    val totalWithAdvance = value + currentAdvance

                    val isDevelopable = listOf("Ataki", "Szybkość", "Żywotność", "Magia").contains(cleanName)
                    val valueColor = when {
                        isDevelopable && currentAdvance < maxAdvance -> Color(0xFF3366FF)
                        isDevelopable -> Color(0xFFBBBB00)
                        else -> Color.White
                    }

                    val traitColor = when {
                        name.contains("Sz") || name.contains("Szybkość") -> {
                            if (wylosowaneCechyCharakteru.any { it.contains("Wysok") }) Color.Yellow
                            else if (wylosowaneCechyCharakteru.any { it.contains("Nisk") || it.contains("Kulaw") }) Color.Red
                            else Color.White
                        }
                        name.contains("Żyw") || name.contains("Żywotność") -> {
                            if (wylosowaneCechyCharakteru.any { it.contains("Otył") }) Color.Yellow
                            else if (wylosowaneCechyCharakteru.any { it.contains("Chud") }) Color.Red
                            else Color.White
                        }
                        name.contains("Wt") || name.contains("Wytrzymałość") || name.contains("Siła") || name.contains("S") -> {
                            if (wylosowaneCechyCharakteru.any { it.contains("Potężna") }) Color.Yellow
                            else if (wylosowaneCechyCharakteru.any { it.contains("Mizerna") }) Color.Red
                            else Color.White
                        }
                        else -> Color.White
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(cleanName, color = if (traitColor != Color.White) traitColor else Color.LightGray, fontSize = 12.sp)
                        Text("$totalWithAdvance", color = if (valueColor != Color.White) valueColor else traitColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray)

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text("CECHY FIZYCZNE", color = Color.Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                cechyFizyczne.forEach { (key, value) ->
                    Text("$key: $value", color = Color.White, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text("CECHY CHARAKTERYSTYCZNE", color = Color.Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                wylosowaneCechyCharakteru.forEach { cecha ->
                    val cleanCecha = cecha.substringBefore("(").trim()
                    val traitColor = when {
                        cecha.contains("+") || cecha.contains("Atrakcyjna") || cecha.contains("Czysty głos") ||
                                cecha.contains("Bardzo białe zęby") || cecha.contains("Charyzmatyczne oczy") ||
                                cecha.contains("Zgrabna") || cecha.contains("Szeroka klatka") ||
                                cecha.contains("Wysoki") || cecha.contains("Otył") || cecha.contains("Potężna") -> Color.Green

                        cecha.contains("-") || cecha.contains("Szpetny") || cecha.contains("Kulaw") ||
                                cecha.contains("Ślepota") || cecha.contains("Jedno Oko") || cecha.contains("Jedna Ręka") ||
                                cecha.contains("Szyderczy") || cecha.contains("Blizny") ||
                                cecha.contains("Szrama") || cecha.contains("Niezgrabne") ||
                                cecha.contains("Niski") || cecha.contains("Chud") || cecha.contains("Mizerna") -> Color.Red

                        else -> Color.White
                    }
                    Text(cleanCecha, color = traitColor, fontSize = 11.sp)
                }


                Spacer(modifier = Modifier.height(6.dp))

                Text("UMIEJĘTNOŚCI", color = Color.Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                
                fun getRankWeight(s: String): Int = when {
                    s.contains("𝕸𝖎𝖘𝖙𝖗𝖟") -> 1
                    s.contains("𝕰𝖐𝖘𝖕𝖊𝖗𝖙") -> 2
                    s.contains("𝕬𝖉𝖊𝖕𝖙") -> 3
                    else -> 4
                }

                val sortedSkills = wylosowaneNormalne.map { s ->
                    val base = s.substringBefore(" (").trim()
                    if (base == wybranyAdept) "$base (𝕬𝖉𝖊𝖕𝖙)" else s
                }.sortedWith(compareBy({ getRankWeight(it) }, { it }))

                wylosowaneSpecjalne.sorted().forEach { spec ->
                    Text("• $spec", color = Color.Green, fontSize = 13.sp, modifier = Modifier.padding(vertical = 1.dp))
                }

                sortedSkills.forEach { skill ->
                    val isAdept = skill.contains("𝕬𝖉𝖊𝖕𝖙")
                    Text("• $skill", color = if (isAdept) Color.White else Color.LightGray, fontSize = 13.sp, modifier = Modifier.padding(vertical = 1.dp))
                }


                Spacer(modifier = Modifier.height(6.dp))
                Text("EKWIPUNEK", color = Color.Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                selectedEquipment.forEach { item ->
                    if (item.startsWith("⛀")) {
                        val coinColor = when {
                            item.contains("Złota") -> Color(0xFFFFD700)
                            item.contains("Srebrny") -> Color(0xFFC0C0C0)
                            item.contains("Miedziany") -> Color(0xFFCD7F32)
                            else -> Color.Yellow
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ", color = Color.White, fontSize = 11.sp)
                            Text(item.take(1), color = coinColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(item.drop(1).trim(), color = Color.White, fontSize = 11.sp)
                        }
                    } else {
                        Text("• $item", color = Color.White, fontSize = 11.sp)
                    }
                }
                

            }
        }

        Button(
            onClick = {
                val summary = StringBuilder()
                val pdStart = (wybranyPoziom * 200) + (1..100).random()
                poziomStartowy = wybranyPoziom

                summary.append("Imie: $heroName\n")
                summary.append("Rasa: $rasa, Klasa: $klasa\n")
                summary.append("Płeć: $plec, Wiek: $wiek\n\n")
                if (heroPortraitUri != null) {
                    summary.append("Portrait: $heroPortraitUri\n")
                }
                summary.append("POZIOM: $wybranyPoziom ｢$wybranyPoziom｣\n")
                summary.append("PD: $pdStart\n")
                if (wygenerowaneStaty != null && wygenerowaneStaty!!.any { it.second[1] == 0 && it.second[2] == 0 }) {
                    summary.append("MANUAL: true\n")
                }
                summary.append("\nSTATYSTYKI GŁÓWNE:\n")
                wygenerowaneStaty?.take(8)?.forEachIndexed { idx, it ->
                    val plusVal = when(idx) {
                        0 -> plusMaxWW; 1 -> plusMaxUS; 2 -> plusMaxKrz; 3 -> plusMaxOdp
                        4 -> plusMaxZr; 5 -> plusMaxInt; 6 -> plusMaxSW; 7 -> plusMaxCha
                        else -> 0
                    }
                    val currentVal = it.third + (plusVal * 5)
                    summary.append("${it.first}: $currentVal | Base: ${it.third} | Plus: $plusVal\n")
                }
                summary.append("\nSTATYSTYKI DRUGORZĘDNE:\n")
                val orderedSecStats = listOf(
                    "Ataki" to ((statyDrugorzedne["A"] ?: statyDrugorzedne["Ataki"] ?: 1) + plusMaxAtk),
                    "Szybkość" to ((statyDrugorzedne["Sz"] ?: statyDrugorzedne["Szybkość"] ?: 4) + plusMaxSz),
                    "Siła" to ((wygenerowaneStaty?.get(2)?.third ?: 0) / 10 + (plusMaxKrz * 5 / 10)),
                    "Wytrzymałość" to ((wygenerowaneStaty?.get(3)?.third ?: 0) / 10 + (plusMaxOdp * 5 / 10)),
                    "Żywotność" to ((statyDrugorzedne["Żyw"] ?: statyDrugorzedne["Żywotność"] ?: 10) + plusMaxZw),
                    "Obłęd" to (statyDrugorzedne["PO"] ?: statyDrugorzedne["Obłęd"] ?: 0),
                    "Przeznaczenie" to (statyDrugorzedne["PP"] ?: statyDrugorzedne["Przeznaczenie"] ?: 0),
                    "Magia" to ((statyDrugorzedne["M"] ?: statyDrugorzedne["Magia"] ?: 0) + plusMaxMag)
                )
                orderedSecStats.forEach { (k, v) -> 
                    val baseVal = when(k) {
                        "Ataki" -> statyDrugorzedne["A"] ?: statyDrugorzedne["Ataki"] ?: 1
                        "Szybkość" -> statyDrugorzedne["Sz"] ?: statyDrugorzedne["Szybkość"] ?: 4
                        "Siła" -> (wygenerowaneStaty?.get(2)?.third ?: 0) / 10
                        "Wytrzymałość" -> (wygenerowaneStaty?.get(3)?.third ?: 0) / 10
                        "Żywotność" -> statyDrugorzedne["Żyw"] ?: statyDrugorzedne["Żywotność"] ?: 10
                        "Magia" -> statyDrugorzedne["M"] ?: statyDrugorzedne["Magia"] ?: 0
                        else -> v
                    }
                    val plusVal = when(k) {
                        "Ataki" -> plusMaxAtk; "Szybkość" -> plusMaxSz; "Żywotność" -> plusMaxZw; "Magia" -> plusMaxMag
                        "Siła" -> plusMaxKrz * 5 / 10; "Wytrzymałość" -> plusMaxOdp * 5 / 10
                        else -> 0
                    }
                    if (listOf("Ataki", "Szybkość", "Siła", "Wytrzymałość", "Żywotność", "Magia").contains(k)) {
                        summary.append("$k: $v | Base: $baseVal | Plus: $plusVal\n")
                    } else {
                        summary.append("$k: $v\n")
                    }
                }
                
                summary.append("\nUMIEJĘTNOŚCI SPECJALNE:\n")
                wylosowaneSpecjalne.sorted().forEach { summary.append("• $it\n") }

                summary.append("\nUMIEJĘTNOŚCI (GRUPY):\n")
                val allCats = listOf(
                    "Charyzma" to HeroLogic.skillCharyzma,
                    "Inteligencja" to HeroLogic.skillInteligencja,
                    "Język" to HeroLogic.skillJezyk,
                    "Krzepa" to HeroLogic.skillKrzepa,
                    "Odporność" to HeroLogic.skillOdpornosc,
                    "Siła Woli" to HeroLogic.skillSW,
                    "Umiejętności Strzeleckie" to HeroLogic.skillUS,
                    "Walka Wręcz" to HeroLogic.skillWW,
                    "Wiedza" to HeroLogic.skillWiedza,
                    "Zręczność" to HeroLogic.skillZrecznosc
                ).sortedBy { it.first }

                allCats.forEach { (catName, pool) ->
                    val inThisCat = wylosowaneNormalne.map { s ->
                        val base = s.substringBefore(" (").trim()
                        if (base == wybranyAdept) {
                            if (s.contains("𝕸𝖎𝖘𝖙𝖗𝖟") || s.contains("𝕰𝖐𝖘𝖕𝖊𝖗𝖙")) s
                            else "$base (𝕬𝖉𝖊𝖕𝖙)"
                        } else s
                    }.filter { s ->
                        val base = s.substringBefore(" (").trim()
                        pool.any { p ->
                            if (p.contains("[")) {
                                p.substringBefore(" [").trim() == base.substringBefore(" (").trim()
                            } else {
                                p == base
                            }
                        }
                    }.sorted()

                    if (inThisCat.isNotEmpty()) {
                        summary.append("[$catName]\n")
                        inThisCat.forEach { summary.append("  • $it | PN: 0\n") }
                    }
                }

                summary.append("\nCECHY FIZYCZNE:\n")
                cechyFizyczne.forEach { (k, v) -> summary.append("$k: $v\n") }
                summary.append("\nCECHY CHARAKTERU:\n")
                wylosowaneCechyCharakteru.forEach { summary.append("• $it\n") }

                summary.append("\nEKWIPUNEK:\n")
                selectedEquipment.forEach { item ->
                    if (item.startsWith("⛀")) {
                        summary.append("   • $item\n")
                    } else {
                        summary.append("   • $item\n")
                    }
                }

                val globalDate = readFromFile("start_date_config").ifEmpty { "6.06.666 | 12:00" }
                val datePart = globalDate.substringBefore(" |").trim()
                val timePart = if (globalDate.contains("|")) globalDate.substringAfter("|").trim() else "12:00"
                
                val d = datePart.substringBefore(".").toIntOrNull() ?: 6
                val dayName = getDayOfWeek(d.coerceIn(1, 28))
                
                // Initialize hero in Altdorf, Sacred District, at the Inn (discovered)
                val startingCity = "Altdorf"
                val startingZone = "Dzielnica Sakralna"
                val startingSettlement = SettlementGenerator.generateSettlement(context, startingCity, com.example.genpro.SettlementType.CITY, rasa)
                
                // Mark Sacred District as discovered and Inn in that district as discovered
                val sacredZone = startingSettlement.zones.find { it.districtType == com.example.genpro.DistrictType.SACRED }
                val finalZoneName = sacredZone?.name ?: "Centrum"
                
                // Ensure the Inn is discovered
                val updatedZones = startingSettlement.zones.map { zone ->
                    if (zone.districtType == DistrictType.SACRED) {
                        zone.copy(inn = zone.inn.copy(discovered = true))
                    } else zone
                }
                val finalSettlement = startingSettlement.copy(zones = updatedZones)
                
                val settlementJson = Json.encodeToString(SettlementInstance.serializer(), finalSettlement)
                val calendarLine = "$startingCity | $datePart | $timePart | $settlementJson | $dayName | Pogoda: Słonecznie | $finalZoneName"
                
                summary.append("\nCALENDAR: $calendarLine\n")
                summary.append("Lokacja: $calendarLine\n")
                summary.append("OdkryteMiejsca_$startingCity: $finalZoneName\n")

                val maxSlots = if (isGMMode) 10 else 5
                var slotToSave = "hero_1"
                for (i in 1..maxSlots) {
                    if (!context.fileList().contains("hero_$i")) {
                        slotToSave = "hero_$i"
                        break
                    }
                }
                saveToFile(slotToSave, summary.toString())
                playSound(R.raw.roll, 1)
                onComplete()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black)
        ) {
            Text("ZAPISZ POSTAĆ", fontWeight = FontWeight.Bold)
        }
    }
}
