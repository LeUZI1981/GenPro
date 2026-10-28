package com.example.genpro

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage

@Composable
fun CharacterInfoAdvancementScreen(
    selectedHeroId: Int,
    nosiferFamily: FontFamily,
    slaboFamily: FontFamily,
    onMenuAppChange: (Int) -> Unit,
    readFromFile: (String) -> String,
    saveToFile: (String, String) -> Unit,
    playSoundAction: (Int, Int) -> Unit,
    context: Context,
    isGMMode: Boolean = false,
    onHeroProgressChange: (HeroProgress) -> Unit = {}
) {
    val currentFile = "hero_$selectedHeroId"
    var heroData by remember { mutableStateOf(readFromFile(currentFile)) }
    var lines by remember { mutableStateOf(heroData.lines()) }

    // Odświeżanie danych po wznowieniu aktywności
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val freshData = readFromFile(currentFile)
                heroData = freshData
                lines = freshData.lines()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Parsing logic moved inside or passed as needed
    val name = HeroLogic.getHeroName(heroData)

    val rasa = HeroLogic.getHeroRace(heroData)
    val rawKlasa = HeroLogic.getHeroClass(heroData)
    val klasa = rawKlasa.substringBefore("(").trim()
    val plecSymbol = HeroLogic.getHeroGenderSymbol(heroData)
    val currentPd = HeroLogic.getHeroPd(heroData)

    val startLevel = HeroLogic.getHeroStartLevel(heroData)
    val level = HeroLogic.getHeroExperienceLevel(heroData)
    val isGraMode = heroData.contains("｢0｣")

    val reqsForLevel = getLevelRequirements(level)
    val lvlUmax = reqsForLevel.lvlUmax
    val lvlAmax = reqsForLevel.lvlAmax
    val lvlEmax = reqsForLevel.lvlEmax
    val lvlMmax = reqsForLevel.lvlMmax
    val lvlAMax = reqsForLevel.lvlAMax

    // Wyliczanie wszystkich posiadanych umiejętności dla globalnych wymogów
    val allPossessedSkills = mutableListOf<Pair<String, String>>()
    val specialAbilities = mutableListOf<String>()
    var isReadingSkills = false
    var isReadingSpecials = false
    lines.forEach { line ->
        val trimmed = line.trim()
        if (trimmed.contains("UMIEJĘTNOŚCI (GRUPY):")) { isReadingSkills = true; isReadingSpecials = false }
        else if (trimmed.contains("ZDOLNOŚCI:")) { isReadingSpecials = true; isReadingSkills = false }
        else if ((isReadingSkills || isReadingSpecials) && (trimmed.startsWith("CECHY FIZYCZNE:") || trimmed.startsWith("CECHY CHARAKTERU:") || trimmed.startsWith("EKWIPUNEK:"))) {
            isReadingSkills = false
            isReadingSpecials = false
        }
        else if (isReadingSkills && line.contains("•")) {
            val clean = line.replace("•", "").trim()
            val hasRank = clean.contains("(")
            val rawName = if (hasRank) clean.substringBefore("(").trim() else clean.substringBefore("|").trim()
            val nameS = if (rawName.contains(" [")) rawName.substringBefore(" [").trim() else rawName
            val rankText = if (hasRank) clean.substringAfter("(").substringBefore(")").trim() else "Uczeń"
            val rank = if (rankText == "✔️") "✔️" else normalizeRank(rankText)
            
            allPossessedSkills.add(nameS to rank)
        }
        else if (isReadingSpecials && line.contains("•")) {
            specialAbilities.add(line.replace("•", "").trim())
        }
    }

    val uniqueSkillsHighestRank = allPossessedSkills
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, ranks) -> 
            if (ranks.any { it == "✔️" }) 1 
            else ranks.maxOfOrNull { HeroLogic.getRankWeight(it) } ?: 0 
        }

    val countU = uniqueSkillsHighestRank.values.count { it == 1 } + specialAbilities.size
    val countA = uniqueSkillsHighestRank.values.count { it == 2 }
    val countE = uniqueSkillsHighestRank.values.count { it == 3 }
    val countM = uniqueSkillsHighestRank.values.count { it == 4 }
    val countAM = uniqueSkillsHighestRank.values.count { it == 5 }

    fun getSkillPN(skillName: String): Int {
        val skillLine = lines.find { line ->
            val cleaned = line.replace("•", "").trim()
            val skillNameInLine = if (cleaned.contains("(")) cleaned.substringBefore("(").trim() else cleaned.substringBefore("|").trim()
            skillNameInLine.equals(skillName, ignoreCase = true)
        }
        return if (skillLine != null && skillLine.contains("PN:")) {
            val pnPart = skillLine.substringAfter("PN:").trim().takeWhile { it.isDigit() }
            pnPart.toIntOrNull() ?: 0
        } else 0
    }

    val uniqueSkillsList = uniqueSkillsHighestRank.map { (name, weight) ->
        val actualRank = allPossessedSkills.find { it.first == name && it.second == "✔️" }?.second
        val rank = actualRank ?: when(weight) {
            5 -> "Arcymistrz"
            4 -> "Mistrz"
            3 -> "Ekspert"
            2 -> "Adept"
            1 -> "Uczeń"
            else -> "Brak"
        }
        val pn = getSkillPN(name)
        Triple(name, rank, pn)
    }.filter { it.second != "Brak" }

    val isSkillRequirementMet = (countU) >= lvlUmax &&
                               (countA) >= lvlAmax &&
                               (countE) >= lvlEmax &&
                               (countM + countAM) >= lvlMmax

    val statCaps = getStatCaps(klasa, level)
    val plusMaxWW = statCaps.plusMaxWW; val plusMaxUS = statCaps.plusMaxUS; val plusMaxKrz = statCaps.plusMaxKrz; val plusMaxOdp = statCaps.plusMaxOdp
    val plusMaxZr = statCaps.plusMaxZr; val plusMaxInt = statCaps.plusMaxInt; val plusMaxSW = statCaps.plusMaxSW; val plusMaxCha = statCaps.plusMaxCha
    val plusMaxAtk = statCaps.plusMaxAtk; val plusMaxSz = statCaps.plusMaxSz; val plusMaxZw = statCaps.plusMaxZw; val plusMaxMag = statCaps.plusMaxMag

    val statAbbreviations = listOf("WW", "US", "K", "Odp", "Zr", "Int", "SW", "Cha")
    val primaryStatFullNames = listOf("Walka Wręcz", "Strzelectwo", "Krzepa", "Odporność", "Zręczność", "Inteligencja", "Siła Woli", "Charyzma")

    val primaryStats = statAbbreviations.map { HeroLogic.getStatValue(heroData, it).toString() }

    val isManualEntry = lines.any { it.trim() == "MANUAL: true" }

    val allPrimaryStatsMaxed = (0..7).all { idx ->
        val advancesBought = HeroLogic.getStatPlusValue(heroData, statAbbreviations[idx])
        val maxAdvance = when(idx) {
            0 -> plusMaxWW; 1 -> plusMaxUS; 2 -> plusMaxKrz; 3 -> plusMaxOdp
            4 -> plusMaxZr; 5 -> plusMaxInt; 6 -> plusMaxSW; 7 -> plusMaxCha
            else -> 0
        }
        advancesBought >= maxAdvance
    }

    val allSecStatsMaxed = listOf("Ataki", "Szybkość", "Żywotność", "Magia").all { fullName ->
        val abbr = when(fullName) { "Ataki"->"A"; "Szybkość"->"Sz"; "Żywotność"->"Żyw"; "Magia"->"M"; else->"" }
        val maxAdvance = when(abbr) {
            "A" -> plusMaxAtk; "Sz" -> plusMaxSz; "Żyw" -> plusMaxZw; "M" -> plusMaxMag; else -> 0
        }
        if (maxAdvance <= 0) return@all true

        val advancesBought = HeroLogic.getStatPlusValue(heroData, fullName)
        advancesBought >= maxAdvance
    }

    fun getSkillRank(skillName: String): Int {
        return uniqueSkillsHighestRank.entries.find { it.key.equals(skillName, ignoreCase = true) }?.value ?: 0
    }

    fun getRankBonus(skillName: String): Int {
        return HeroLogic.getRankBonus(lines, skillName)
    }

    fun getWeaponTraitsAbbr(invLine: String): String {
        val traits = mutableListOf<String>()
        val traitMap = mapOf(
            "Druzgocący" to "D",
            "Szybki" to "Sz",
            "Powolny" to "P",
            "Ogłuszający" to "O",
            "Precyzyjny" to "Pr",
            "Przebijający" to "PP",
            "Odpryskowy" to "Od",
            "Ciężki" to "C",
            "Lekki" to "L",
            "Wyważony" to "W"
        )
        traitMap.forEach { (full, abbr) ->
            if (invLine.contains(full, ignoreCase = true)) {
                traits.add(abbr)
            }
        }
        return traits.joinToString(",")
    }

    var expandedStatIndex by remember { mutableIntStateOf(-1) }
    var expandedSecondaryStat by remember { mutableStateOf<String?>(null) }
    var activeSkillSlider by remember { mutableStateOf<String?>(null) }
    var showArmorDetails by remember { mutableStateOf(false) }
    var skillSliderValue by remember { mutableFloatStateOf(0f) }
    var statSliderValue by remember { mutableFloatStateOf(1f) }
    var damageSliderValue by remember { mutableFloatStateOf(0f) }
    var damageFeedback by remember { mutableStateOf("") }

    LaunchedEffect(expandedStatIndex, expandedSecondaryStat) {
        statSliderValue = 1f
        activeSkillSlider = null
    }

    val portraitLine = lines.find { it.startsWith("Portrait: ") }
    val portraitUriString = portraitLine?.substringAfter("Portrait: ")?.trim()
    val cechyLine = lines.find { it.startsWith("CECHY CHARAKTERU:") } ?: ""
    val cechyIndex = lines.indexOf(cechyLine)
    val cechyCharakteru = if (cechyIndex != -1) {
        lines.drop(cechyIndex + 1).takeWhile { it.startsWith("•") }.map { it.trim(' ', '•') }
    } else emptyList()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(bottom = 16.dp)) {
        // Portrait Header - Full height of info section
        AnimatedVisibility(visible = expandedStatIndex == -1 && expandedSecondaryStat == null) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .offset(y = (-30).dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    // Portrait Background
                    if (!portraitUriString.isNullOrEmpty()) {
                        AsyncImage(
                            model = portraitUriString,
                            contentDescription = "Portrait Background",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            alpha = 0.6f
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.unknowhero),
                            contentDescription = "Default Portrait Background",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            alpha = 0.4f
                        )
                    }

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    ),
                                )
                            )
                    )

                    // Imię, Rasa, Klasa, PD
                    Column(modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)) {
                        Text(
                            name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = nosiferFamily,
                            maxLines = 1
                        )
                        Text(
                            "$rasa  ✩$level${if (isGraMode) " GRA" else ""}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontFamily = slaboFamily,
                            fontWeight = FontWeight.Medium
                        )
                        Text("$klasa  $plecSymbol", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text("$currentPd" + "𝖕𝖉", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                    ) {
                        IconButton( ///Do kości
                            onClick = {
                                playSoundAction(R.raw.klik1, 0)
                                val currentFile = "hero_$selectedHeroId"
                                val intent = Intent(context, GameActivity::class.java).apply {
                                    putExtra("HERO_FILE", currentFile)
                                    putExtra("MENU_APP", 1) // 1: Statystyki
                                    putExtra("SKIP_ROLL", true) // Nie wykonuj automatycznego rzutu
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Text("🎲", color = Color.White, fontSize = 20.sp)
                        }
                        IconButton(
                            onClick = { onMenuAppChange(20) }
                        ) {
                            Text("📝", color = Color.White, fontSize = 20.sp)
                        }
                        IconButton(
                            onClick = { onMenuAppChange(21) }
                        ) {
                            Text("🎒", color = Color.White, fontSize = 20.sp)
                        }
                        IconButton(
                            onClick = { onMenuAppChange(31) }
                        ) {
                            Text("📜", color = Color.White, fontSize = 20.sp)
                        }
                        if (isGraMode) {
                            IconButton(
                                onClick = {
                                    val progressFile = "progress_$selectedHeroId.json"
                                    val progressJson = readFromFile(progressFile)
                                    val progress = if (progressJson.isNotEmpty()) {
                                        runCatching { kotlinx.serialization.json.Json.decodeFromString<HeroProgress>(progressJson) }.getOrNull()
                                    } else null
                                    val finalProgress = progress ?: HeroLogic.getHeroProgress(selectedHeroId.toString(), heroData)
                                    val entered = tryEnterSettlementFromCurrentPosition(
                                        context, selectedHeroId, finalProgress, readFromFile, saveToFile
                                    )
                                    onHeroProgressChange(entered ?: finalProgress)
                                    onMenuAppChange(if (entered != null) 35 else 61)
                                }
                            ) {
                                Text("🏕️", color = Color.White, fontSize = 20.sp)
                            }
                            IconButton(
                                onClick = { onMenuAppChange(61) }
                            ) {
                                Text("50", color = Color.White, fontSize = 20.sp)
                            }
                            IconButton(
                                onClick = { onMenuAppChange(61) }
                            ) {
                                Text("61", color = Color.White, fontSize = 20.sp)
                            }


                        }
                        else {
                            IconButton(
                                onClick = { onMenuAppChange(30) }
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.mapworld),
                                    contentDescription = "Mapa Świata",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    playSoundAction(R.raw.klik1, 0)
                                    val currentFile = "hero_$selectedHeroId"
                                    val intent = Intent(context, GameActivity::class.java).apply {
                                        putExtra("HERO_FILE", currentFile)
                                        putExtra("MENU_APP", 3) // 1: skillGame
                                        putExtra("SKIP_ROLL", true) // Nie wykonuj automatycznego rzutu
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("𓀊", color = Color.White, fontSize = 20.sp)
                            }
                        }
                    }

                    // Cechy fizyczne
                    val physicalTraitsMap = lines.filter { it.contains(":") }.associate {
                        val key = it.substringBefore(":").replace(Regex("[•*]"), "").trim().lowercase()
                        key to it.substringAfter(":").trim()
                    }
                    val traitStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = slaboFamily,
                        lineHeight = 8.sp,
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 9.dp, end = 10.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy((1).dp)
                    ) {
                        val ageStr = physicalTraitsMap["wiek"] ?: physicalTraitsMap["age"] ?: ""
                        if (ageStr.isNotEmpty()) {
                            val ageInt = ageStr.filter { it.isDigit() }.toIntOrNull()
                            if (ageInt != null) {
                                val suffix = when {
                                    ageInt % 100 in 11..14 -> "lat"
                                    ageInt % 10 in 2..4 -> "lata"
                                    else -> "lat"
                                }
                                Text("$ageInt $suffix", style = traitStyle)
                            } else {
                                Text(ageStr, style = traitStyle)
                            }
                        }
                        (physicalTraitsMap["wzrost"] ?: physicalTraitsMap["height"])?.let {
                            Text(
                                "Wzrost: $it",
                                style = traitStyle
                            )
                        }
                        (physicalTraitsMap["waga"] ?: physicalTraitsMap["weight"])?.let {
                            Text(
                                "Waga: $it",
                                style = traitStyle
                            )
                        }

                        val hair = physicalTraitsMap.entries.find {
                            it.key.contains("włos") || it.key.contains("wlos") || it.key.contains(
                                "hair"
                            )
                        }?.value
                        val eyes = physicalTraitsMap.entries.find {
                            it.key.contains("ocz") || it.key.contains(
                                "eye"
                            )
                        }?.value

                        hair?.let { Text("Włosy: $it", style = traitStyle) }
                        eyes?.let { Text("Oczy: $it", style = traitStyle) }

                        // Cechy szczególne
                        if (cechyCharakteru.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                cechyCharakteru.forEach { cecha ->
                                    Text(
                                        cecha,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontFamily = slaboFamily,
                                        textAlign = TextAlign.End,
                                        lineHeight = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(1.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))
            }
        }

        // Primary Stats Buttons
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            statAbbreviations.forEachIndexed { index, abbr ->
                val advancesBought = HeroLogic.getStatPlusValue(heroData, abbr)
                val maxAdvance = when(index) {
                    0 -> plusMaxWW; 1 -> plusMaxUS; 2 -> plusMaxKrz; 3 -> plusMaxOdp
                    4 -> plusMaxZr; 5 -> plusMaxInt; 6 -> plusMaxSW; 7 -> plusMaxCha
                    else -> 0
                }
                val isMaxed = maxAdvance > 0 && advancesBought >= maxAdvance
                val isDevelopable = true // Główne statystyki są zawsze widoczne jako możliwe do rozwoju
                val canUpgrade = maxAdvance > 0 && !isMaxed && (isSkillRequirementMet || isGMMode)
                
                val containerColor = if (expandedStatIndex == index) {
                    if (canUpgrade) Color.Gray else Color.DarkGray
                } else Color.Black
                
                val abbrColor = if (expandedStatIndex == index && canUpgrade) Color.Black else Color.White
                val statTextColor = if (expandedStatIndex == index) {
                    if (canUpgrade) Color.Green else Color.Yellow
                } else {
                    when {
                        advancesBought >= 5 -> Color(0xFF2E7D32)
                        advancesBought == 4 -> Color(0xFF4CAF50)
                        advancesBought == 3 -> Color(0xFF8BC34A)
                        advancesBought == 2 -> Color(0xFFCDDC39)
                        advancesBought == 1 -> Color(0xFFFFEB3B)
                        isMaxed -> Color(0xFF9000FF)
                        else -> Color.Gray
                    }
                }

                    Button(
                        onClick = {
                            playSoundAction(R.raw.page, 0)
                            expandedStatIndex = if (expandedStatIndex == index) -1 else index
                            expandedSecondaryStat = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(1.dp)
                            .height(55.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = Color.White)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(abbr, fontSize = 10.sp, color = abbrColor)
                            Text(primaryStats.getOrNull(index) ?: "0", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = statTextColor)
                            if (maxAdvance > 0 || advancesBought > 0) {
                                Row(
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .width(24.dp)
                                        .height(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    for (i in 0 until maxOf(maxAdvance, advancesBought)) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(if (i < advancesBought) Color.Green else Color.DarkGray)
                                        )
                                    }
                                }
                            }
                        }
                    }
            }
        }

        val secondaryAbbr = listOf("A", "Sz", "S", "Wt", "Żyw", "PO", "PP", "M")

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            secondaryAbbr.forEach { abbr ->
                if (abbr.isNotEmpty()) {
                    val statVal = HeroLogic.getStatValue(heroData, abbr)

                    val fullName = when (abbr) { "A"->"Ataki"; "Sz"->"Szybkość"; "Żyw"->"Żywotność"; "M"->"Magia"; "S"->"Siła"; "Wt"->"Wytrzymałość"; else -> "" }
                    val maxAdv = when (abbr) { "A" -> plusMaxAtk; "Sz" -> plusMaxSz; "Żyw" -> plusMaxZw; "M" -> plusMaxMag; else -> 0 }
                    val advBought = HeroLogic.getStatPlusValue(heroData, fullName)
                    val isMaxed = maxAdv > 0 && advBought >= maxAdv
                    val isDevelopable = listOf("A", "Sz", "Żyw", "M").contains(abbr)
                    val isExpandable = isDevelopable || abbr == "S" || abbr == "Wt"
                    val canUpgrade = isDevelopable && maxAdv > 0 && !isMaxed && ((isSkillRequirementMet && allPrimaryStatsMaxed) || isGMMode)
                    
                    val containerColor = if (expandedSecondaryStat == abbr) {
                        if (canUpgrade) Color.Gray else Color.DarkGray
                    } else Color.Black

                    val abbrColor = if (expandedSecondaryStat == abbr && canUpgrade) Color.Black else Color.White //.copy(alpha = 0.7f)
                    val statTextColor = if (expandedSecondaryStat == abbr) {
                        if (canUpgrade) Color.Green else Color.Yellow
                    } else {
                        when {
                            advBought >= 5 -> Color(0xFF2E7D32)
                            advBought == 4 -> Color(0xFF4CAF50)
                            advBought == 3 -> Color(0xFF8BC34A)
                            advBought == 2 -> Color(0xFFCDDC39)
                            advBought == 1 -> Color(0xFFFFEB3B)
                            isMaxed -> Color(0xFF9000FF)
                            else -> Color.Gray
                        }
                    }

                    Button(
                        onClick = {
                            if (isExpandable) {
                                playSoundAction(R.raw.page, 0)
                                expandedSecondaryStat = if (expandedSecondaryStat == abbr) null else abbr
                                expandedStatIndex = -1
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(1.dp)
                            .height(45.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = Color.White)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(abbr, fontSize = 10.sp, color = abbrColor)
                            Text("$statVal", fontSize = 16.sp, color = statTextColor)
                            if (isDevelopable && (maxAdv > 0 || advBought > 0)) {
                                Row(
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .width(24.dp)
                                        .height(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    for (i in 0 until maxOf(maxAdv, advBought)) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(if (i < advBought) Color.Green else Color.DarkGray)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier
                        .weight(1f)
                        .padding(1.dp))
                }
            }
        }



    if (expandedSecondaryStat != null) {
        val abbr = expandedSecondaryStat!!
        val fullName = when (abbr) {
            "A" -> "Ataki"
            "Sz" -> "Szybkość"
            "Żyw" -> "Żywotność"
            "M" -> "Magia"
            else -> ""
        }

        val currentVal = HeroLogic.getStatValue(heroData, fullName)
        val baseVal = HeroLogic.getStatBaseValue(heroData, fullName)
        val advancesBought = HeroLogic.getStatPlusValue(heroData, fullName)

        // DYNAMICZNE POBIERANIE LIMITÓW
        val currentLvlNow = HeroLogic.getHeroExperienceLevel(heroData)
        val currentCapsNow = getStatCaps(klasa, currentLvlNow)
        val maxAdvance = when(abbr) {
            "A" -> currentCapsNow.plusMaxAtk
            "Sz" -> currentCapsNow.plusMaxSz
            "Żyw" -> currentCapsNow.plusMaxZw
            "M" -> currentCapsNow.plusMaxMag
            else -> 0
        }

        Column(modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 450.dp)
            .padding(horizontal = 16.dp)
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp)
            .verticalScroll(rememberScrollState())) {
            // Advancement Button at the TOP
            if (((isSkillRequirementMet && allPrimaryStatsMaxed && advancesBought < maxAdvance) || isGMMode) && maxAdvance > 0) {
                val nextStatCost = (advancesBought + 1) * 100
                val maxBuyable = if (isGMMode) 10 else {
                    var remainingPd = currentPd
                    var count = 0
                    var currentAdvances = advancesBought
                    while (count < (maxAdvance - advancesBought)) {
                        val cost = (currentAdvances + 1) * 100
                        if (remainingPd >= cost) {
                            remainingPd -= cost
                            count++
                            currentAdvances++
                        } else break
                    }
                    count
                }

                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp)) {
                    val selectedBuy = statSliderValue.toInt().coerceAtLeast(1).coerceAtMost(maxBuyable.coerceAtLeast(1))
                    val totalCost = if (isGMMode) 0 else {
                        var sum = 0
                        for (i in 0 until selectedBuy) {
                            sum += (advancesBought + 1 + i) * 100
                        }
                        sum
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Rozwój: $fullName (+${selectedBuy})", color = Color.Cyan, fontSize = 12.sp)
                        Text("Koszt: $totalCost PD", color = if (isGMMode || currentPd >= totalCost) Color.Green else Color.Red, fontSize = 11.sp)
                    }

                    if (maxBuyable > 1) {
                        Slider(
                            value = statSliderValue,
                            onValueChange = { statSliderValue = it },
                            valueRange = 1f..maxBuyable.toFloat(),
                            steps = maxBuyable - 2,
                            colors = SliderDefaults.colors(thumbColor = Color.Cyan, activeTrackColor = Color.Cyan)
                        )
                    }

                    val canAfford = isGMMode || (selectedBuy > 0 && currentPd >= totalCost)

                    Button(
                        onClick = {
                            if (canAfford) {
                                playSoundAction(R.raw.klik2, 1)
                                val updatedLines = lines.toMutableList()
                                val pdIdx = updatedLines.indexOfFirst { it.startsWith("PD:") }
                                val cost = if (isGMMode) 0 else {
                                    var sum = 0
                                    for (i in 0 until selectedBuy) {
                                        sum += (advancesBought + 1 + i) * 100
                                    }
                                    sum
                                }
                                val newPd = currentPd - cost
                                if (pdIdx != -1) updatedLines[pdIdx] = "PD: $newPd"

                                val historyFile = "hero_${selectedHeroId}_pd_history"
                                val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                val historyEntry = "$timestamp: ${currentPd}𝖕𝖉 - ${cost}𝖕𝖉 = ${newPd}𝖕𝖉 [+${selectedBuy} $fullName]\n"
                                saveToFile(historyFile, historyEntry + readFromFile(historyFile))

                                val secIdx = updatedLines.indexOfFirst { it.contains("STATYSTYKI DRUGORZĘDNE:", ignoreCase = true) }
                                if (secIdx != -1) {
                                    val lineRelativeIdx = updatedLines.drop(secIdx).indexOfFirst { 
                                        val clean = it.trim().removePrefix("•").trim()
                                        clean.startsWith(fullName, ignoreCase = true) 
                                    }
                                    if (lineRelativeIdx != -1) {
                                        val actualLineIdx = secIdx + lineRelativeIdx
                                        val currentLine = updatedLines[actualLineIdx]
                                        val newVal = currentVal + selectedBuy
                                        val baseValLine = currentLine.substringAfter("Base:").substringBefore("|").trim()
                                        
                                        // Wyciągamy Plus: odpornie na dopiski (np. bonusy w nawiasach)
                                        val plusPart = currentLine.substringAfter("Plus:").trim().split(" ", "|").firstOrNull() ?: "0"
                                        val plusValLine = plusPart.toIntOrNull() ?: 0
                                        
                                        // Zachowujemy resztę linii po wartości Plus (np. "(+2 Hełm)")
                                        val suffix = currentLine.substringAfter("Plus:").trim().substringAfter(plusPart, "")
                                        
                                        val prefix = if (currentLine.trim().startsWith("•")) "• " else ""
                                        updatedLines[actualLineIdx] = "$prefix$fullName: $newVal | Base: $baseValLine | Plus: ${plusValLine + selectedBuy}$suffix"
                                    }
                                }
                                val finalHeroData = updatedLines.joinToString("\n")
                                val cleanHeroData = HeroLogic.stripLegacyProgressTags(finalHeroData)
                                saveToFile(currentFile, cleanHeroData)
                                lines = updatedLines
                                heroData = finalHeroData
                                statSliderValue = 1f
                                
                                // Check if all stats (primary and secondary) are now maxed and skills met
                                val updatedAllPrimaryMaxed = (0..7).all { idx ->
                                    val sAbbr = statAbbreviations[idx]
                                    val sFull = primaryStatFullNames[idx]
                                    val sLine = updatedLines.find { line ->
                                        val clean = line.trim().removePrefix("•").trim()
                                        line.contains("Base:", ignoreCase = true) && 
                                        (clean.startsWith("$sAbbr:", ignoreCase = true) || clean.startsWith("$sFull:", ignoreCase = true))
                                    }
                                    val advBought = sLine?.substringAfter("Plus:")?.trim()?.split(" ")?.firstOrNull()?.toIntOrNull() ?: 0
                                    val mAdv = when(idx) {
                                        0 -> plusMaxWW; 1 -> plusMaxUS; 2 -> plusMaxKrz; 3 -> plusMaxOdp
                                        4 -> plusMaxZr; 5 -> plusMaxInt; 6 -> plusMaxSW; 7 -> plusMaxCha
                                        else -> 0
                                    }
                                    advBought >= mAdv
                                }
                                val updatedAllSecMaxed = listOf("Ataki", "Szybkość", "Żywotność", "Magia").all { fName ->
                                    val ab = when(fName) { "Ataki"->"A"; "Szybkość"->"Sz"; "Żywotność"->"Żyw"; "Magia"->"M"; else->"" }
                                    val mAdv = when(ab) { "A" -> plusMaxAtk; "Sz" -> plusMaxSz; "Żyw" -> plusMaxZw; "M" -> plusMaxMag; else -> 0 }
                                    if (mAdv <= 0) return@all true
                                    val sLine = updatedLines.find { line ->
                                        val clean = line.trim().removePrefix("•").trim()
                                        clean.startsWith("$fName:", ignoreCase = true) || clean.startsWith("$ab:", ignoreCase = true)
                                    }
                                    val advBought = sLine?.substringAfter("Plus:")?.trim()?.split(" ")?.firstOrNull()?.toIntOrNull() ?: 0
                                    advBought >= mAdv
                                }

                                if (updatedAllPrimaryMaxed && updatedAllSecMaxed && isSkillRequirementMet && !isGMMode) {
                                    expandedSecondaryStat = null
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008800))
                    ) {
                        Text("PODNIEŚ (+${selectedBuy})  [$advancesBought" + (if (isGMMode) "" else "/$maxAdvance]"), color = Color.Yellow, fontSize = 12.sp)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$fullName: $baseVal + $advancesBought = $currentVal", color = Color.Cyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                val displayStars = maxOf(maxAdvance, advancesBought)
                for (i in 0 until displayStars) {
                    Text(
                        text = if (i < advancesBought) "⭐" else "★",
                        color = if (i < advancesBought) Color.Green else Color.Red,
                        fontSize = 15.sp
                    )
                }
            }

            val inventoryLines = lines.dropWhile { !it.contains("EKWIPUNEK:", ignoreCase = true) }.drop(1).takeWhile { it.trim().startsWith("•") || it.trim().isBlank() }
            val equippedItems = inventoryLines.filter { it.contains("🅔") || it.contains("[e]", ignoreCase = true) || it.contains("[E]") }

            if (abbr == "A" || abbr == "Ataki") {
                Spacer(modifier = Modifier.height(8.dp))
                data class WeaponData(val name: String, val kX: Int, val isRanged: Boolean, val range: Int = 0, val type: DamageType = DamageType.OBUCHOWA)

                fun getWeaponSkillName(weapon: WeaponData): String {
                    val wName = weapon.name
                    if (HeroLogic.WW4nazwa.contains(wName)) return "Broń egzotyczna"
                    if (weapon.isRanged) {
                        return when (weapon.type) {
                            DamageType.MIOTANA -> "Broń miotana"
                            DamageType.PROCHOWA -> if (wName.contains("Pistolet") || wName.contains("Rewolwer") || wName.contains("Kacza stopa")) "Broń prochowa krótka" else "Broń prochowa długa"
                            DamageType.ENERGIA -> "Broń egzotyczna"
                            else -> {
                                val skill = "Broń: $wName"
                                if (allPossessedSkills.any { it.first.equals(skill, true) }) skill else "Strzelectwo"
                            }
                        }
                    }
                    return when {
                        wName.contains("Kastet", true) || wName.contains("Bijatyka", true) -> "Bijatyka"
                        weapon.type == DamageType.SIECZNA -> "Broń bojowa sieczna"
                        weapon.type == DamageType.OBUCHOWA -> "Broń bojowa obuchowa"
                        weapon.type == DamageType.KLUTA -> "Broń bojowa kłuta"
                        else -> "Broń prosta"
                    }
                }

                fun findInEquipped(itemName: String): String? {
                    val clean = itemName.trim().lowercase()
                    return equippedItems.find { it.lowercase().contains(clean) }
                }

                val allKnownWeapons = mutableListOf<WeaponData>()
                HeroLogic.WW0nazwa.forEachIndexed { i, n -> allKnownWeapons.add(WeaponData(n, HeroLogic.WW0kX[i], false, type = DamageType.OBUCHOWA)) }
                HeroLogic.WW1nazwa.forEachIndexed { i, n -> allKnownWeapons.add(WeaponData(n, HeroLogic.WW1kX[i], false, type = DamageType.SIECZNA)) }
                HeroLogic.WW2nazwa.forEachIndexed { i, n -> allKnownWeapons.add(WeaponData(n, HeroLogic.WW2kX[i], false, type = DamageType.OBUCHOWA)) }
                HeroLogic.WW3nazwa.forEachIndexed { i, n -> allKnownWeapons.add(WeaponData(n, HeroLogic.WW3kX[i], false, type = DamageType.KLUTA)) }
                HeroLogic.WW4nazwa.forEachIndexed { i, n -> allKnownWeapons.add(WeaponData(n, HeroLogic.WW4kX[i], false, type = DamageType.SIECZNA)) }
                HeroLogic.US0nazwa.forEachIndexed { i, n -> allKnownWeapons.add(WeaponData(n, HeroLogic.US0kX[i], true, HeroLogic.US0range[i], type = DamageType.MIOTANA)) }
                HeroLogic.US1nazwa.forEachIndexed { i, n -> allKnownWeapons.add(WeaponData(n, HeroLogic.US1kX[i], true, HeroLogic.US1range[i], type = DamageType.DYSTANSOWA)) }
                HeroLogic.US2nazwa.forEachIndexed { i, n -> allKnownWeapons.add(WeaponData(n, HeroLogic.US2kX[i], true, HeroLogic.US2range[i], type = DamageType.DYSTANSOWA)) }
                HeroLogic.US3nazwa.forEachIndexed { i, n -> allKnownWeapons.add(WeaponData(n, HeroLogic.US3kX[i], true, HeroLogic.US3range[i], type = DamageType.DYSTANSOWA)) }
                HeroLogic.US4nazwa.forEachIndexed { i, n -> allKnownWeapons.add(WeaponData(n, HeroLogic.US4kX[i], true, HeroLogic.US4range[i], type = DamageType.PROCHOWA)) }
                HeroLogic.US5nazwa.forEachIndexed { i, n -> allKnownWeapons.add(WeaponData(n, HeroLogic.US5kX[i], true, HeroLogic.US5range[i], type = DamageType.ENERGIA)) }

                val equipped = allKnownWeapons.filter { findInEquipped(it.name) != null }.distinctBy { it.name }

                val agility = primaryStats.getOrNull(4)?.toIntOrNull() ?: 0
                val dodgeSkillBonus = getRankBonus("Uniki")
                val dodgePrefixBonus = if (equippedItems.any { it.contains("Zwinn", ignoreCase = true) }) 5 else 0
                val dodgeChance = agility + dodgeSkillBonus + dodgePrefixBonus
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    
                    Button(
                        onClick = {
                            playSoundAction(R.raw.roll, 1)
                            val roll = (1..100).random()
                            val isCrit = roll % 11 == 0 && roll <= dodgeChance
                            val isFumble = (roll % 11 == 0 || roll == 100) && roll > dodgeChance

                            if (roll <= dodgeChance) {
                                if (isCrit) playSoundAction(R.raw.levelup, 1)
                                damageFeedback = (if (isCrit) "KRYTYCZNY UNIK! " else "UNIK UDANY! ") + "(‹$roll› <= $dodgeChance%) |S:$dodgeChance|"
                            } else {
                                if (isFumble) playSoundAction(R.raw.klik2, 1)
                                damageFeedback = (if (isFumble) "PECHOWY UNIK! " else "UNIK NIEUDANY! ") + "(‹$roll› > $dodgeChance%) |S:$dodgeChance|"
                            }
                        },
                        modifier = Modifier.height(26.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF440044))
                    ) {
                        Text("UNIKI: $dodgeChance% ($agility+$dodgeSkillBonus+$dodgePrefixBonus)", fontSize = 12.sp, color = Color.White)
                    }
                }

                if (equipped.isEmpty()) {
                    Text("Brak wyekwipowanej broni", color = Color.Gray, fontSize = 10.sp)
                }

                equipped.forEach { weapon ->
                    val invLine = findInEquipped(weapon.name) ?: ""
                    val cleanInvName = invLine.replace("🅔", "").replace("[e]", "").replace("[E]", "").trim()
                    
                    val isWyv = invLine.contains("Wyważon", ignoreCase = true)
                    val prefixBonus = if (isWyv) 5 else 0
                    
                    val skillName = getWeaponSkillName(weapon)
                    val skillBonus = getRankBonus(skillName)
                    val baseChance = if (weapon.isRanged) (primaryStats.getOrNull(1)?.toIntOrNull() ?: 0) else (primaryStats.getOrNull(0)?.toIntOrNull() ?: 0)
                    val hitChance = baseChance + skillBonus + prefixBonus
                    
                    val sVal = (primaryStats.getOrNull(2)?.toIntOrNull() ?: 0) / 10
                    val dieSize = weapon.kX * 2
                    val totalBonus = sVal

                    val categoryLabel = when (skillName) {
                        "Broń bojowa sieczna" -> "Bojowa sieczna"
                        "Broń bojowa obuchowa" -> "Bojowa obuchowa"
                        "Broń bojowa kłuta" -> "Bojowa kłuta"
                        "Broń prosta" -> "Prosta"
                        "Broń egzotyczna" -> "Egzotyczna"
                        else -> skillName.replace("Broń: ", "").replace("Strzelectwo", "Dystansowa")
                    }

                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                        .padding(4.dp)) {
                        Text(cleanInvName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                val traitsAbbr = getWeaponTraitsAbbr(invLine)
                                val traitsDisplay = if (traitsAbbr.isNotEmpty()) " ($traitsAbbr)" else ""
                                val breakdown = if (prefixBonus != 0) "$baseChance+$skillBonus+$prefixBonus" else "$baseChance+$skillBonus"
                                Text("$categoryLabel | k$dieSize + $sVal(S) | $hitChance%$traitsDisplay [$breakdown]", color = Color.Gray, fontSize = 10.sp)
                            }
                            Row {
                                Button(
                                    onClick = {
                                        playSoundAction(R.raw.roll, 1)
                                        val roll = (1..100).random()
                                        val isCrit = roll % 11 == 0 && roll <= hitChance
                                        val isFumble = (roll % 11 == 0 || roll == 100) && roll > hitChance

                                        if (roll <= hitChance) {
                                            if (isCrit) playSoundAction(R.raw.levelup, 1)
                                            val dmgRoll = if (dieSize > 0) (1..dieSize).random() else 0
                                            val total = dmgRoll + totalBonus
                                            damageFeedback = (if (isCrit) "KRYTYK! " else "TRAFIONO! ") +
                                                    "(‹$roll› <= $hitChance%) -> ‹$dmgRoll› + $totalBonus = ‹$total› obr. |S:$hitChance|"
                                        } else {
                                            if (isFumble) playSoundAction(R.raw.klik2, 1)
                                            damageFeedback = (if (isFumble) "PECH! " else "PUDŁO! ") +
                                                    "(‹$roll› > $hitChance%) |S:$hitChance|"
                                        }
                                    },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF660000))
                                ) {
                                    Text("ATAK $hitChance%", fontSize = 10.sp, color = Color.Yellow)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = {
                                        playSoundAction(R.raw.roll, 1)
                                        val roll = if (dieSize > 0) (1..dieSize).random() else 0
                                        val total = roll + totalBonus
                                        damageFeedback = "Rzut na dmg: ‹$roll› + $totalBonus = ‹$total› obr. |D:false:$dieSize|"
                                    },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                ) {
                                    Text("🎲 k$dieSize", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
                if (damageFeedback.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(colorize(damageFeedback), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }

            if (abbr == "Żyw" || abbr == "Żywotność") {
                Spacer(modifier = Modifier.height(8.dp))
                val zyw = currentVal
                val toughnessValue = (primaryStats.getOrNull(3)?.toIntOrNull() ?: 0) / 10
                


                // Damage Slider
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Siła trafienia: ", color = Color.White, fontSize = 12.sp)
                        Text("${damageSliderValue.toInt()}", color = if (damageSliderValue < 0) Color.Green else Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        //Spacer(modifier = Modifier.width(8.dp))
                        Text("Wytrzymałość: $toughnessValue", color = Color.Yellow, fontSize = 10.sp)
                        Button(
                            onClick = { showArmorDetails = !showArmorDetails },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (showArmorDetails) Color.LightGray else Color.DarkGray)
                        ) {
                            Text(if (showArmorDetails) "RANY" else "PANCERZ", fontSize = 9.sp, color = if (showArmorDetails) Color.DarkGray else Color.LightGray)
                        }
                    }
                    Slider(
                        value = damageSliderValue,
                        onValueChange = { damageSliderValue = it },
                        valueRange = -10f..30f,
                        steps = 39,
                        modifier = Modifier.height(24.dp)
                    )
                    if (damageFeedback.isNotEmpty()) {
                        Text(colorize(damageFeedback), fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }

                val locations = listOf(
                    "Głowa" to (zyw * 0.75).toInt(),
                    "Ręka Prawa" to zyw,
                    "Ręka Lewa" to zyw,
                    "Korpus" to (zyw * 1.5).toInt(),
                    "Noga Prawa" to (zyw * 1.1).toInt(),
                    "Noga Lewa" to (zyw * 1.1).toInt()
                )
                
                val locToArmor = mapOf(
                    "Głowa" to "Głowa",
                    "Ręka Prawa" to "Ramiona",
                    "Ręka Lewa" to "Ramiona",
                    "Korpus" to "Korpus",
                    "Noga Prawa" to "Nogi",
                    "Noga Lewa" to "Nogi"
                )

                val equippedArmor= (HeroLogic.armorLight + HeroLogic.armorMedium + HeroLogic.armorHeavy).filter { armor ->
                    equippedItems.any { it.lowercase().contains(armor.name.lowercase()) }
                }.map { armor ->
                    val invLine = equippedItems.find { it.lowercase().contains(armor.name.lowercase()) } ?: ""
                    val dur = if (invLine.contains("Dur:")) {
                        invLine.substringAfter("Dur:").substringBefore("%").trim().toIntOrNull() ?: 100
                    } else 100
                    armor.copy(durability = dur)
                }.toMutableList()

                // Fallback for legacy armor names
                val legacyArmorMap = mapOf(
                    "Głowa" to HeroLogic.pancGłowaNazwa,
                    "Korpus" to HeroLogic.pancKorpusNazwa,
                    "Ramiona" to HeroLogic.pancRękaNazwa,
                    "Nogi" to HeroLogic.pancNogaNazwa
                )

                legacyArmorMap.forEach { (loc, names) ->
                    names.forEachIndexed { index, legacyName ->
                        val cleanLegacy = legacyName.trim()
                        if (cleanLegacy != "𝓑𝓻𝓪𝓴" && cleanLegacy.isNotEmpty() && equippedItems.any { it.lowercase().contains(cleanLegacy.lowercase()) }) {
                            if (equippedArmor.none { it.name == cleanLegacy }) {
                                val (pps, ppo, ppk) = when(loc) {
                                    "Głowa" -> when(index) {
                                        1 -> Triple(1, 1, 0); 2 -> Triple(2, 1, 1); 3 -> Triple(3, 1, 1); 4 -> Triple(2, 1, 2); 5, 6 -> Triple(3, 2, 3); else -> Triple(0, 0, 0)
                                    }
                                    "Korpus" -> when(index) {
                                        1, 2 -> Triple(1, 1, 0); 3, 4 -> Triple(2, 1, 1); 5, 6, 7 -> Triple(3, 1, 1); 8, 9 -> Triple(2, 1, 2); 10, 11 -> Triple(3, 2, 3); else -> Triple(0, 0, 0)
                                    }
                                    "Ramiona" -> when(index) {
                                        1 -> Triple(1, 1, 0); 2 -> Triple(2, 1, 1); 3 -> Triple(3, 1, 1); 4 -> Triple(2, 1, 2); 5 -> Triple(3, 2, 3); else -> Triple(0, 0, 0)
                                    }
                                    "Nogi" -> when(index) {
                                        1 -> Triple(1, 1, 0); 2 -> Triple(2, 1, 1); 3 -> Triple(3, 1, 1); 4 -> Triple(2, 1, 2); 5 -> Triple(3, 2, 3); else -> Triple(0, 0, 0)
                                    }
                                    else -> Triple(0, 0, 0)
                                }
                                if (pps > 0 || ppo > 0 || ppk > 0) {
                                    val invLine = equippedItems.find { it.lowercase().contains(cleanLegacy.lowercase()) } ?: ""
                                    val dur = if (invLine.contains("Dur:")) {
                                        invLine.substringAfter("Dur:").substringBefore("%").trim().toIntOrNull() ?: 100
                                    } else 100
                                    equippedArmor.add(ArmorPiece(cleanLegacy, listOf(loc), pps, ppo, ppk, 0, durability = dur))
                                }
                            }
                        }
                    }
                }
                
                locations.forEach { (locName, maxLocHp) ->
                    val locLine = lines.find { it.startsWith("LOKACJA $locName:") }
                    val currentLocHp = locLine?.substringAfter(":")?.substringBefore("/")?.trim()?.toIntOrNull() ?: maxLocHp
                    
                    val armorPiece = equippedArmor.find { it.locations.contains(locToArmor[locName]) }
                    val ppS = armorPiece?.ppS ?: 0
                    val ppO = armorPiece?.ppO ?: 0
                    val ppK = armorPiece?.ppK ?: 0

                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                        .padding(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(locName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                if (showArmorDetails) {
                                                    val totS = toughnessValue + ppS
                                                    val totO = toughnessValue + ppO
                                                    val totK = toughnessValue + ppK

                                                    if (armorPiece != null) {
                                                        Text(armorPiece.name, color = Color.Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        Text("PP: $ppS/$ppO/$ppK | Ochr: $totS/$totO/$totK", color = Color.Cyan, fontSize = 10.sp)
                                                        val dur = armorPiece.durability
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("Stan: $dur%", color = if(dur < 50) Color.Red else Color.Gray, fontSize = 9.sp)
                                                            if (dur < 100) {
                                                                val diff = (90 - dur).coerceAtLeast(0)
                                                                val mult = if (armorPiece.name.lowercase().contains("skór")) 2 else if (armorPiece.name.lowercase().contains("płyt") || armorPiece.name.lowercase().contains("kirys")) 4 else 3
                                                                val costInCopper = diff * mult
                                                                val sCost = costInCopper / 12
                                                                val pCost = costInCopper % 12
                                                                val costText = if (sCost > 0) "${sCost}s ${pCost}p" else "${pCost}p"
                                                                val canAfford = HeroLogic.calculateTotalCopper(heroData) >= costInCopper

                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                TextButton(onClick = {
                                                                    val updatedLines = lines.toMutableList()
                                                                    val invIdx = updatedLines.indexOfFirst { it.contains(armorPiece.name, ignoreCase = true) && (it.contains("🅔") || it.contains("[E]")) }
                                                                    if (invIdx != -1) {
                                                                        val line = updatedLines[invIdx]
                                                                        val newLine = if (line.contains("Dur:")) {
                                                                            line.replace(Regex("Dur:\\s*\\d+%"), "Dur: 100%")
                                                                        } else {
                                                                            "$line | Dur: 100%"
                                                                        }
                                                                        updatedLines[invIdx] = newLine
                                                                    }
                                                                    
                                                                    // Update currency
                                                                    val newHeroData = deductMoney(updatedLines.joinToString("\n"), costInCopper)
                                                                    saveToFile(currentFile, newHeroData)
                                                                    heroData = newHeroData
                                                                    lines = newHeroData.lines()

                                                                    // Update game_equipment synchronization
                                                                    val eqFile = "game_equipment_$selectedHeroId"
                                                                    val currentEqRaw = readFromFile(eqFile)
                                                                    if (currentEqRaw.isNotEmpty()) {
                                                                        val currentEq = currentEqRaw.split(";").toMutableList()
                                                                        val itemIdx = currentEq.indexOfFirst { it.contains(armorPiece.name, ignoreCase = true) }
                                                                        if (itemIdx != -1) {
                                                                            var cName = currentEq[itemIdx].replace("Zniszczony", "").replace("Uszkodzony", "").replace("Znoszony", "").trim()
                                                                            cName = cName.replace(Regex("\\[Jakość: \\d+%\\]"), "[Jakość: 90%]").trim()
                                                                            if (!cName.contains("Jakość:")) cName += " [Jakość: 90%]"
                                                                            currentEq[itemIdx] = cName
                                                                            saveToFile(eqFile, currentEq.joinToString(";"))
                                                                        }
                                                                    }

                                                                    damageFeedback = "Naprawiono ${armorPiece.name} ($costText)!"
                                                                    val historyFile = "hero_${selectedHeroId}_pd_history"
                                                                    val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                                                    saveToFile(historyFile, "$timestamp: Naprawa: ${armorPiece.name} (-$costText)\n" + readFromFile(historyFile))
                                                                    playSoundAction(R.raw.klik1, 0)
                                                                }, enabled = canAfford, modifier = Modifier.height(20.dp), contentPadding = PaddingValues(0.dp)) {
                                                                    Text("NAPRAW ($costText)", fontSize = 8.sp, color = if(canAfford) Color.Yellow else Color.Gray)
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Text("Brak pancerza | Ochr: $toughnessValue/$toughnessValue/$toughnessValue", color = Color.DarkGray, fontSize = 10.sp)
                                                    }
                                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$currentLocHp / $maxLocHp",
                                    color = if (currentLocHp < maxLocHp / 2) Color.Red else if (currentLocHp < maxLocHp) Color.Yellow else Color.Green,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                if (!showArmorDetails) {
                                        listOf("S", "O", "K").forEach { type ->
                                        Button(
                                            onClick = {
                                                val dmg = damageSliderValue.toInt()
                                                val updatedLines = lines.toMutableList()
                                                var changed = false

                                                if (dmg < 0) {
                                                    val healAmount = -dmg
                                                    val newVal = (currentLocHp + healAmount).coerceAtMost(maxLocHp)
                                                    val idx = updatedLines.indexOfFirst { it.startsWith("LOKACJA $locName:") }
                                                    if (idx != -1) updatedLines[idx] = "LOKACJA $locName: $newVal / $maxLocHp"
                                                    else updatedLines.add("LOKACJA $locName: $newVal / $maxLocHp")

                                                    damageFeedback = "Leczenie $locName! +$healAmount"
                                                    changed = true
                                                } else {
                                                    val pp = when(type) { "S" -> ppS; "O" -> ppO; "K" -> ppK; else -> 0 }
                                                    val finalDmg = (dmg - toughnessValue - pp).coerceAtLeast(0)
                                                    
                                                    if (dmg > toughnessValue && armorPiece != null) {
                                                        val invIdx = updatedLines.indexOfFirst { it.contains(armorPiece.name, ignoreCase = true) && (it.contains("🅔") || it.contains("[E]")) }
                                                        if (invIdx != -1) {
                                                            val line = updatedLines[invIdx]
                                                            val currentDur = if (line.contains("Dur:")) {
                                                                line.substringAfter("Dur:").substringBefore("%").trim().toIntOrNull() ?: 100
                                                            } else 100
                                                            val loss = if (finalDmg > 0) 5 else 2
                                                            val newDur = (currentDur - loss).coerceAtLeast(0)
                                                            updatedLines[invIdx] = if (line.contains("Dur:")) {
                                                                line.replace(Regex("Dur:\\s*\\d+%"), "Dur: $newDur%")
                                                            } else {
                                                                    "$line | Dur: $newDur%"
                                                            }
                                                            changed = true
                                                        }
                                                    }

                                                    if (finalDmg > 0) {
                                                        val idx = updatedLines.indexOfFirst { it.startsWith("LOKACJA $locName:") }
                                                        val newVal = (currentLocHp - finalDmg).coerceAtLeast(0)
                                                        if (idx != -1) updatedLines[idx] = "LOKACJA $locName: $newVal / $maxLocHp"
                                                        else updatedLines.add("LOKACJA $locName: $newVal / $maxLocHp")
                                                        
                                                        damageFeedback = "Rana $type w $locName! -$finalDmg (Wt:$toughnessValue + PP:$pp)"
                                                        changed = true
                                                    } else {
                                                        damageFeedback = "Pancerz i Wytrzymałość odbiły cios!"
                                                    }
                                                }

                                                if (changed) {
                                                    saveToFile(currentFile, updatedLines.joinToString("\n"))
                                                    lines = updatedLines
                                                    heroData = updatedLines.joinToString("\n")
                                                }
                                            },
                                            modifier = Modifier
                                                .size(26.dp)
                                                .padding(horizontal = 1.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                        ) {
                                            Text(type, fontSize = 9.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (abbr == "Sz" || abbr == "Szybkość") {
                // Tactical and Strategic Movement
                Spacer(modifier = Modifier.height(8.dp))
                val sz = currentVal
                
                Text("RUCH STRATEGICZNY (km/h):", color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                val stratMoves = listOf(
                    "Ostrożnie" to (sz * 0.5),
                    "Normalnie" to (sz * 1.0),
                    "Biegiem" to (sz * 2.0)
                )
                stratMoves.forEach { (name, speed) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, color = Color.White, fontSize = 12.sp)
                        Text("${if(speed % 1.0 == 0.0) speed.toInt() else speed} km/h", color = Color.Yellow, fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("TAKTYCZNY (metry/runda):", color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                val tactMoves = listOf(
                    "Ostrożny (Ruch/Odwrót)" to sz,
                    "Normalny (Szarża)" to (sz * 2),
                    "Szybki (Bieg)" to (sz * 4)
                )
                tactMoves.forEach { (name, dist) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, color = Color.White, fontSize = 12.sp)
                        Text("$dist m", color = Color.Yellow, fontSize = 12.sp)
                    }
                }
            }

            if (abbr == "Wt" || abbr == "Wytrzymałość") {
                Spacer(modifier = Modifier.height(8.dp))
                // Improved Weight and Inventory Details
                val totalWeight = calculateCurrentWeight(heroData)
                
                Text("OBCIĄŻENIE I EKWIPUNEK:", color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Łączna waga:", color = Color.White, fontSize = 12.sp)
                    Text("$totalWeight j.", color = Color.Yellow, fontSize = 12.sp)
                }
                val loadLimit = HeroLogic.getHeroWeightLimit(heroData)
                Text("Limit obciążenia: $loadLimit j.", color = if (totalWeight > loadLimit) Color.Red else Color.Gray, fontSize = 10.sp)
                
                val enc = HeroLogic.getEncumbrance(totalWeight, loadLimit)
                if (enc != EncumbranceLevel.LEKKIE) {
                    Text("STATUS: ${enc.label}", color = enc.color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Kary: ${enc.penalties}", color = enc.color, fontSize = 9.sp)
                }
            }
        }


    }

    // --- SKILL PN SLIDER (FIXED POSITION) ---
        if (activeSkillSlider != null && expandedStatIndex != -1) {
            val sName = activeSkillSlider!!
            // Find current PN for this skill from ownedSkills
            val targetCats = mapOf(0 to listOf("Walka Wręcz"), 1 to listOf("Umiejętności Strzeleckie"), 2 to listOf("Krzepa"), 3 to listOf("Odporność"), 4 to listOf("Zręczność"), 5 to listOf("Inteligencja", "Wiedza", "Język"), 6 to listOf("Siła Woli"), 7 to listOf("Charyzma"))[expandedStatIndex] ?: emptyList()
            var currentSkillPn = 0
            var currentSkillRank = "Brak"
            
            var tmpCat = ""
            lines.forEach { line ->
                val tl = line.trim()
                if (tl.startsWith("[") && tl.endsWith("]")) tmpCat = tl.removeSurrounding("[", "]")
                else if (line.contains("•") && targetCats.contains(tmpCat) && line.contains(sName)) {
                    val clean = line.replace("•", "").trim()
                    val hasRank = clean.contains("(")
                    val rawName = if (hasRank) clean.substringBefore("(").trim() else clean.substringBefore("|").trim()
                    val matchName = if (rawName.contains(" [")) rawName.substringBefore(" [").trim() else rawName
                    if (matchName == sName) {
                        currentSkillPn = clean.substringAfter("PN:").trim().toIntOrNull() ?: 0
                        currentSkillRank = if (hasRank) normalizeRank(clean.substringAfter("(").substringBefore(")")) else "Uczeń"
                    }
                }
            }

            val currentRankWeight = HeroLogic.getRankWeight(currentSkillRank)
            val pnCost = if (isGMMode) 0 
                         else if (sName.contains("Język obcy") && currentSkillRank == "Brak") 1 
                         else getPnCost(currentRankWeight)
            val maxPurchasable = if (isGMMode) (100 - currentSkillPn) else (currentPd / pnCost).coerceAtMost(100 - currentSkillPn)
            if (maxPurchasable > 0 || currentSkillPn > 0) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        val displayRank = getFancyRank(currentSkillRank)
                        Text("$sName ($displayRank)", color = Color.White, fontSize = 11.sp)
                        Text("${skillSliderValue.toInt()}" + "𝖕𝖓", color = Color.Yellow, fontSize = 15.sp)
                        Text("$currentPd" + "-" + "${((skillSliderValue - currentSkillPn).toInt() * pnCost)}" + "=" + "${(currentPd - (skillSliderValue - currentSkillPn).toInt() * pnCost)}" + "𝖕𝖉", color = Color.Red, fontSize = 11.sp)
                    }
                    Slider(
                        value = skillSliderValue.coerceIn(currentSkillPn.toFloat(), (currentSkillPn + maxPurchasable).toFloat()),
                        onValueChange = { if (it >= currentSkillPn) skillSliderValue = it },
                        valueRange = currentSkillPn.toFloat()..(currentSkillPn + maxPurchasable).toFloat(),
                        onValueChangeFinished = {
                            val amountToAdd = (skillSliderValue - currentSkillPn).toInt()
                            if (amountToAdd > 0) {
                                val updatedLines = lines.toMutableList()
                                val pdIdx = updatedLines.indexOfFirst { it.startsWith("PD:") }
                                val cost = if (isGMMode) 0 else amountToAdd * pnCost
                                val newPd = currentPd - cost
                                if (pdIdx != -1) updatedLines[pdIdx] = "PD: $newPd"

                                val historyFile = "hero_${selectedHeroId}_pd_history"
                                val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                val historyEntry = "$timestamp: ${currentPd}𝖕𝖉 - ${cost}𝖕𝖉 = ${newPd}𝖕𝖉 [+${amountToAdd}𝖕𝖓 $sName]\n"
                                saveToFile(historyFile, historyEntry + readFromFile(historyFile))
                                
                                val skillLineIdx = updatedLines.indexOfFirst { line -> 
                                    line.contains("•") && line.contains(sName) && 
                                    normalizeRank(if (line.contains("(")) line.substringAfter("(").substringBefore(")") else "Uczeń") == currentSkillRank
                                }
                                
                                if (skillLineIdx != -1) {
                                    val line = updatedLines[skillLineIdx]
                                    val currentPnInLine = line.substringAfter("PN:").trim().toIntOrNull() ?: 0
                                    val newPnTotal = currentPnInLine + amountToAdd
                                    
                                    if (sName == "Szczęście" && currentSkillRank == "Brak" && newPnTotal >= 100) {
                                        val line = updatedLines[skillLineIdx]
                                        val beforePn = line.substringBefore("PN:")
                                        val lineWithRank = if (beforePn.contains("(")) {
                                            beforePn.replace(Regex("\\(.*\\)"), "(✔️)")
                                        } else {
                                            beforePn.trim() + " (✔️) "
                                        }
                                        updatedLines[skillLineIdx] = "${lineWithRank}| PN: 0"
                                        
                                        android.widget.Toast.makeText(context, "Zdobyto nową zdolność: Szczęście!", android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        updatedLines[skillLineIdx] = line.substringBefore("PN:") + "PN: $newPnTotal"
                                    }
                                } else if (currentSkillRank == "Brak") {
                                    val catName = targetCats.find { cat ->
                                        val pool = when(cat) {
                                            "Walka Wręcz" -> HeroLogic.skillWW; "Umiejętności Strzeleckie" -> HeroLogic.skillUS; "Krzepa" -> HeroLogic.skillKrzepa; "Odporność" -> HeroLogic.skillOdpornosc
                                            "Zręczność" -> HeroLogic.skillZrecznosc; "Inteligencja" -> HeroLogic.skillInteligencja; "Wiedza" -> HeroLogic.skillWiedza; "Język" -> HeroLogic.skillJezyk
                                            "Siła Woli" -> HeroLogic.skillSW; "Charyzma" -> HeroLogic.skillCharyzma; else -> emptyArray()
                                        }
                                        pool.any { p -> (if (p.contains(" [")) p.substringBefore(" [").trim() else p) == sName }
                                    } ?: targetCats.firstOrNull() ?: "Inne"
                                    
                                    if (sName == "Szczęście" && amountToAdd >= 100) {
                                        val newLine = "  • Szczęście (✔️) | PN: 0"
                                        val headerIdx = updatedLines.indexOfFirst { it.trim() == "[$catName]" }
                                        if (headerIdx != -1) {
                                            updatedLines.add(headerIdx + 1, newLine)
                                        } else {
                                            val groupsIdx = updatedLines.indexOfFirst { it.contains("UMIEJĘTNOŚCI (GRUPY):") }
                                            if (groupsIdx != -1) {
                                                updatedLines.add(groupsIdx + 1, "[$catName]")
                                                updatedLines.add(groupsIdx + 2, newLine)
                                            } else {
                                                updatedLines.add(newLine)
                                            }
                                        }
                                        android.widget.Toast.makeText(context, "Zdobyto nową zdolność: Szczęście!", android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        val groupsIdx = updatedLines.indexOf("UMIEJĘTNOŚCI (GRUPY):")
                                        if (groupsIdx != -1) {
                                            var catIdx = updatedLines.indexOf("[$catName]")
                                            if (catIdx == -1) { 
                                                val pdIdx2 = updatedLines.indexOfFirst { it.startsWith("PD:") }
                                                val manualIdx = updatedLines.indexOfFirst { it.startsWith("MANUAL:") }
                                                val insertAt = (if (manualIdx != -1) manualIdx else if (pdIdx2 != -1) pdIdx2 else groupsIdx) + 1
                                                updatedLines.add(insertAt, "[$catName]")
                                                catIdx = insertAt
                                            }
                                            updatedLines.add(catIdx + 1, "  • $sName (𝓑𝓻𝓪𝓴) | PN: $amountToAdd")
                                        }
                                    }
                                }
                                val finalHeroData = updatedLines.joinToString("\n")
                                val cleanHeroData = HeroLogic.stripLegacyProgressTags(finalHeroData)
                                saveToFile(currentFile, cleanHeroData)
                                lines = updatedLines
                                heroData = finalHeroData
                            }
                            activeSkillSlider = null
                        },
                        colors = SliderDefaults.colors(thumbColor = Color.Yellow, activeTrackColor = Color.Yellow)
                    )
                }
            }
        }

        // Stat / Level Upgrade Slider

        // Skills list
        AnimatedVisibility(visible = expandedStatIndex == -1 && expandedSecondaryStat == null, modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(8.dp)
                .verticalScroll(rememberScrollState())) {
                // Dynamiczne sprawdzenie warunków dla aktualnego poziomu zapobiegające "podwójnemu awansowi"
                val levelNow = HeroLogic.getHeroExperienceLevel(heroData)
                val capsNow = getStatCaps(klasa, levelNow)
                val reqsNow = getLevelRequirements(levelNow)
                
                // Ponowne przeliczenie wymogów umiejętności dla aktualnego (być może właśnie zmienionego) poziomu
                val isSkillMetNow = countU >= reqsNow.lvlUmax &&
                                   countA >= reqsNow.lvlAmax &&
                                   countE >= reqsNow.lvlEmax &&
                                   countM + countAM >= reqsNow.lvlMmax

                val primaryMaxedNow = (0..7).all { idx ->
                    val advancesBought = HeroLogic.getStatPlusValue(heroData, statAbbreviations[idx])
                    val maxForLvl = when(idx) {
                        0 -> capsNow.plusMaxWW; 1 -> capsNow.plusMaxUS; 2 -> capsNow.plusMaxKrz; 3 -> capsNow.plusMaxOdp
                        4 -> capsNow.plusMaxZr; 5 -> capsNow.plusMaxInt; 6 -> capsNow.plusMaxSW; 7 -> capsNow.plusMaxCha
                        else -> 0
                    }
                    advancesBought >= maxForLvl
                }
                
                val secMaxedNow = listOf("Ataki", "Szybkość", "Żywotność", "Magia").all { fullName ->
                    val abbr = when(fullName) { "Ataki"->"A"; "Szybkość"->"Sz"; "Żywotność"->"Żyw"; "Magia"->"M"; else->"" }
                    val maxForLvl = when(abbr) {
                        "A" -> capsNow.plusMaxAtk; "Sz" -> capsNow.plusMaxSz; "Żyw" -> capsNow.plusMaxZw; "M" -> capsNow.plusMaxMag; else -> 0
                    }
                    if (maxForLvl <= 0) true
                    else HeroLogic.getStatPlusValue(heroData, fullName) >= maxForLvl
                }

                val canReallyLevelUp = ((primaryMaxedNow && secMaxedNow && isSkillMetNow) || isGMMode) && levelNow < 10
                
                if (canReallyLevelUp) {
                    val nextLevelCost = if (isGMMode) 0 else if (levelNow == 0) 50 else levelNow * 100
                    val canAffordLevel = isGMMode || (currentPd >= nextLevelCost)
                    Box(
                        modifier = Modifier
                            .clickable(enabled = canAffordLevel) {
                                if (!canAffordLevel) return@clickable

                                playSoundAction(R.raw.levelup, 1)
                                val updatedLines = lines.toMutableList()
                                val pdIdx = updatedLines.indexOfFirst { it.startsWith("PD:") }
                                val newPd = currentPd - nextLevelCost
                                if (pdIdx != -1) updatedLines[pdIdx] = "PD: $newPd"

                                val historyFile = "hero_${selectedHeroId}_pd_history"
                                val timestamp = java.text.SimpleDateFormat(
                                    "dd.MM HH:mm",
                                    java.util.Locale.getDefault()
                                ).format(java.util.Date())
                                val historyEntry =
                                    "$timestamp: ${currentPd}𝖕𝖉 - ${nextLevelCost}𝖕𝖉 = ${newPd}𝖕𝖉 [Poziom ${levelNow + 1}]\n"
                                saveToFile(historyFile, historyEntry + readFromFile(historyFile))

                                val lvlIdx = updatedLines.indexOfFirst { it.startsWith("POZIOM:") }
                                if (lvlIdx != -1) {
                                    val currentPoziomLine = updatedLines[lvlIdx]
                                    val newLevel = levelNow + 1
                                    val suffix =
                                        if (currentPoziomLine.contains("｣")) currentPoziomLine.substringAfter(
                                            "｣"
                                        ).trim().replace("GRA", "").trim() else ""
                                    val finalSuffix =
                                        if (suffix.isEmpty()) "SESJA FABULARNA" else suffix
                                    updatedLines[lvlIdx] =
                                        "POZIOM: $newLevel ｢$startLevel｣ $finalSuffix".trim()
                                    if (startLevel == 0) {
                                        updatedLines.removeAll { it.trim() == "MANUAL: true" }
                                    }
                                }
                                val newData = updatedLines.joinToString("\n")
                                val cleanNewData = HeroLogic.stripLegacyProgressTags(newData)
                                saveToFile(currentFile, cleanNewData)
                                heroData = newData
                                lines = updatedLines
                                expandedStatIndex = -1
                                expandedSecondaryStat = null
                            }
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = if (canAffordLevel) R.drawable.ramkawhite else R.drawable.ramkablack),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                            alpha = if (canAffordLevel) 1f else 0.5f
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "AWANS NA POZIOM ${level + 1}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canAffordLevel) Color.Green else Color.Gray,
                                fontFamily = nosiferFamily
                            )
                            Text(
                                "KOSZT $nextLevelCost PD",
                                fontSize = 14.sp,
                                color = if (canAffordLevel) Color.Yellow else Color.Red,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("𝕌𝕞𝕚𝕖𝕛𝕖𝕥𝕟𝕠𝕤𝕔𝕚:", color = Color.White, fontWeight = FontWeight.Medium , fontSize = 15.sp)   //Umiejętności
                    Row {
                        if (lvlMmax + lvlAMax > 0) {
                            val currentM = countM + countAM
                            Text("𝕸 $currentM/$lvlMmax ", color = if (currentM >= lvlMmax) Color.Magenta else Color.DarkGray, fontSize = 10.sp)
                        }
                        if (lvlEmax > 0) {
                            val currentE = countE
                            Text("𝕰 $currentE/$lvlEmax ", color = if (currentE >= lvlEmax) Color.Blue else Color.DarkGray, fontSize = 10.sp)
                        }
                        if (lvlAmax > 0) {
                            val currentA = countA
                            Text("𝕬 $currentA/$lvlAmax ", color = if (currentA >= lvlAmax) Color.Cyan else Color.DarkGray, fontSize = 10.sp)
                        }
                        val currentU = countU
                        Text("𝖀 $currentU/$lvlUmax", color = if (currentU >= lvlUmax) Color.LightGray else Color.DarkGray, fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                if (specialAbilities.isNotEmpty()) {
                    specialAbilities.forEach { ability ->
                        val isTrueSpecial = HeroLogic.skillSpecjalny.any { it.equals(ability, ignoreCase = true) }
                        val currentRank = uniqueSkillsList.find { it.first.equals(ability, ignoreCase = true) }?.second ?: "Brak"
                        val displayRank = if (isTrueSpecial) {
                            if (currentRank.contains("✔️")) "✔️" else getFancyRank("Uczeń")
                        } else "Specjalna"
                        val skillPN = getSkillPN(ability)
                        
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)) {
                            Text("★ $ability", color = if (isTrueSpecial) Color.Green else Color.Yellow, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(displayRank, color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                }

                uniqueSkillsList.sortedWith(compareByDescending<Triple<String, String, Int>> { HeroLogic.getRankWeight(it.second) }.thenBy { it.first }).forEach { (sName, sRank, sPn) ->
                    val rWeight = HeroLogic.getRankWeight(sRank)
                    val rColor = when(rWeight) {
                        1 -> Color.Gray
                        2 -> Color.LightGray
                        3 -> Color.Cyan
                        4 -> Color.Blue
                        5 -> Color.Magenta         //Red
                        else -> Color.Red
                    }
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)) {
                        Text("• $sName", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("$sPn 𝖕𝖓  ", color = Color.LightGray, fontSize = 12.sp)
                        //Było: Text("﹝$sPn 𝖕𝖓﹞ ", color = Color.LightGray, fontSize = 12.sp)
                        Text(getFancyRank(sRank), color = rColor, fontSize = 12.sp)
                    }
                }
            }
        }

        // Expanded Skills list
        AnimatedVisibility(visible = expandedStatIndex != -1, modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(8.dp)
                .verticalScroll(rememberScrollState())) {
                val statAbbr = statAbbreviations.getOrNull(expandedStatIndex) ?: ""
                val fullNameStat = primaryStatFullNames.getOrNull(expandedStatIndex) ?: ""
                val currentVal = HeroLogic.getStatValue(heroData, statAbbr)
                val baseVal = HeroLogic.getStatBaseValue(heroData, statAbbr)
                val totalAdvances = HeroLogic.getStatPlusValue(heroData, statAbbr)
                
                // DYNAMICZNE POBIERANIE LIMITÓW DLA AKTUALNEGO POZIOMU
                val currentLvl = HeroLogic.getHeroExperienceLevel(heroData)
                val currentCaps = getStatCaps(klasa, currentLvl)
                val maxAdvanceForLevel = when(expandedStatIndex) { 
                    0 -> currentCaps.plusMaxWW; 1 -> currentCaps.plusMaxUS; 2 -> currentCaps.plusMaxKrz; 3 -> currentCaps.plusMaxOdp; 
                    4 -> currentCaps.plusMaxZr; 5 -> currentCaps.plusMaxInt; 6 -> currentCaps.plusMaxSW; 7 -> currentCaps.plusMaxCha; else -> 0 
                }

                // --- ADVANCEMENT PURCHASE OPTION (ABOVE HEADER) ---
                if ((totalAdvances < maxAdvanceForLevel && (isSkillRequirementMet || isManualEntry || isGMMode)) || (isGMMode)) {
                    val maxBuyable = if (isGMMode) 20 else {
                        var remainingPd = currentPd
                        var count = 0
                        var currentAdvances = totalAdvances
                        while (count < (maxAdvanceForLevel - totalAdvances)) {
                            val cost = (currentAdvances + 1) * 100
                            if (remainingPd >= cost) {
                                remainingPd -= cost
                                count++
                                currentAdvances++
                            } else break
                        }
                        count
                    }

                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)) {
                        val selectedBuy = statSliderValue.toInt().coerceAtLeast(1).coerceAtMost(maxBuyable.coerceAtLeast(1))
                        val totalCost = if (isGMMode) 0 else {
                            var sum = 0
                            for (i in 0 until selectedBuy) {
                                sum += (totalAdvances + 1 + i) * 100
                            }
                            sum
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Rozwinięcie: +${selectedBuy * 5}", color = Color.Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Koszt: $totalCost PD", color = if (isGMMode || currentPd >= totalCost) Color.Green else Color.Red, fontSize = 11.sp)
                        }
                        
                        if (!isManualEntry) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row {
                                    if (lvlAMax > 0) Text("𝕬𝕸 $countAM/$lvlAMax ", color = if (countAM >= lvlAMax) Color.Green else Color.Yellow, fontSize = 10.sp)
                                    if (lvlMmax > 0) Text("𝕸 $countM/$lvlMmax ", color = if (countM >= lvlMmax) Color.Green else Color.Yellow, fontSize = 10.sp)
                                    if (lvlEmax > 0) Text("𝕰 $countE/$lvlEmax ", color = if (countE >= lvlEmax) Color.Green else Color.Yellow, fontSize = 10.sp)
                                    if (lvlAmax > 0) Text("𝕬 $countA/$lvlAmax ", color = if (countA >= lvlAmax) Color.Green else Color.Yellow, fontSize = 10.sp)
                                    Text("𝖀 $countU/$lvlUmax", color = if (countU >= lvlUmax) Color.Green else Color.Yellow, fontSize = 8.sp)
                                }
                            }
                        } else {
                            Text("MANUAL ENTRY - WYMOGI POMINIĘTE", color = Color.Gray, fontSize = 9.sp)
                        }

                        if (maxBuyable > 1) {
                            Slider(
                                value = statSliderValue,
                                onValueChange = { statSliderValue = it },
                                valueRange = 1f..maxBuyable.toFloat(),
                                steps = maxBuyable - 2,
                                colors = SliderDefaults.colors(thumbColor = Color.Cyan, activeTrackColor = Color.Cyan)
                            )
                        }

                        val canAfford = isGMMode || (selectedBuy > 0 && currentPd >= totalCost)

                        Button(
                            onClick = {
                                if (canAfford && (isSkillRequirementMet || isManualEntry || isGMMode)) {
                                    playSoundAction(R.raw.klik2, 1)
                                    val updatedLines = lines.toMutableList()
                                    val pdIdx = updatedLines.indexOfFirst { it.startsWith("PD:") }
                                    val cost = if (isGMMode) 0 else {
                                        var sum = 0
                                        for (i in 0 until selectedBuy) {
                                            sum += (totalAdvances + 1 + i) * 100
                                        }
                                        sum
                                    }
                                    val newPd = currentPd - cost
                                    if (pdIdx != -1) updatedLines[pdIdx] = "PD: $newPd"

                                    val fullNameStat = primaryStatFullNames.getOrNull(expandedStatIndex) ?: ""
                                    val historyFile = "hero_${selectedHeroId}_pd_history"
                                    val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                    val historyEntry = "$timestamp: ${currentPd}𝖕𝖉 - ${cost}𝖕𝖉 = ${newPd}𝖕𝖉 [+${selectedBuy * 5} $fullNameStat]\n"
                                    saveToFile(historyFile, historyEntry + readFromFile(historyFile))

                                    val statsIdx = updatedLines.indexOfFirst { it.contains("STATYSTYKI GŁÓWNE:", ignoreCase = true) }
                                    if (statsIdx != -1) {
                                        val lineIdx = statsIdx + 1 + expandedStatIndex
                                        val currentLine = updatedLines[lineIdx]
                                        val newVal = currentVal + (selectedBuy * 5)
                                        val baseValLine = currentLine.substringAfter("Base:").substringBefore("|").trim()
                                        
                                        val plusPart = currentLine.substringAfter("Plus:").trim().split(" ", "|").firstOrNull() ?: "0"
                                        val plusValLine = plusPart.toIntOrNull() ?: 0
                                        val suffix = currentLine.substringAfter("Plus:").trim().substringAfter(plusPart)
                                        
                                        val label = currentLine.substringBefore(":").trim()
                                        updatedLines[lineIdx] = "$label: $newVal | Base: $baseValLine | Plus: ${plusValLine + selectedBuy}$suffix"
                                        
                                        if (expandedStatIndex == 2 || expandedStatIndex == 3) {
                                            val secIdx = updatedLines.indexOfFirst { it.contains("STATYSTYKI DRUGORZĘDNE:", ignoreCase = true) }
                                            if (secIdx != -1) {
                                                val target = if (expandedStatIndex == 2) "Siła" else "Wytrzymałość"
                                                val sLineRelativeIdx = updatedLines.drop(secIdx).indexOfFirst { 
                                                    val clean = it.trim().removePrefix("•").trim()
                                                    clean.startsWith(target, ignoreCase = true) || 
                                                    clean.startsWith(if(target=="Siła") "S:" else "Wt:", ignoreCase = true)
                                                }
                                                if (sLineRelativeIdx != -1) {
                                                    val actualSecIdx = secIdx + sLineRelativeIdx
                                                    val secLine = updatedLines[actualSecIdx]
                                                    val sBase = secLine.substringAfter("Base:").substringBefore("|").trim()
                                                    val sPlusPart = secLine.substringAfter("Plus:").trim().split(" ", "|").firstOrNull() ?: "0"
                                                    val sPlusVal = sPlusPart.toIntOrNull() ?: 0
                                                    val sSuffix = secLine.substringAfter("Plus:").trim().substringAfter(sPlusPart)

                                                    val prefix = if (secLine.trim().startsWith("•")) "• " else ""
                                                    updatedLines[actualSecIdx] = "$prefix$target: ${newVal / 10} | Base: $sBase | Plus: $sPlusVal$sSuffix"
                                                }
                                            }
                                        }
                                    }
                                    
                                    val finalHeroData = updatedLines.joinToString("\n")
                                    val cleanHeroData = HeroLogic.stripLegacyProgressTags(finalHeroData)
                                    saveToFile(currentFile, cleanHeroData)
                                    heroData = finalHeroData
                                    lines = updatedLines
                                    statSliderValue = 1f
                                    activeSkillSlider = null

                                    // Check if all stats (primary and secondary) are now maxed and skills met
                                    val currentLvlNow = HeroLogic.getHeroExperienceLevel(heroData)
                                    val currentCapsNow = getStatCaps(klasa, currentLvlNow)
                                    
                                    val updatedAllPrimaryMaxed = (0..7).all { idx ->
                                        val sLine = updatedLines.find { it.contains("Base:") && (it.startsWith(statAbbreviations[idx]) || it.startsWith(primaryStatFullNames[idx])) }
                                        val advBought = sLine?.substringAfter("Plus:")?.trim()?.split(" ", "|")?.firstOrNull()?.toIntOrNull() ?: 0
                                        val mAdv = when(idx) {
                                            0 -> currentCapsNow.plusMaxWW; 1 -> currentCapsNow.plusMaxUS; 2 -> currentCapsNow.plusMaxKrz; 3 -> currentCapsNow.plusMaxOdp
                                            4 -> currentCapsNow.plusMaxZr; 5 -> currentCapsNow.plusMaxInt; 6 -> currentCapsNow.plusMaxSW; 7 -> currentCapsNow.plusMaxCha
                                            else -> 0
                                        }
                                        advBought >= mAdv
                                    }
                                    val updatedAllSecMaxed = listOf("Ataki", "Szybkość", "Żywotność", "Magia").all { fName ->
                                        val ab = when(fName) { "Ataki"->"A"; "Szybkość"->"Sz"; "Żywotność"->"Żyw"; "Magia"->"M"; else->"" }
                                        val mAdv = when(ab) { "A" -> currentCapsNow.plusMaxAtk; "Sz" -> currentCapsNow.plusMaxSz; "Żyw" -> currentCapsNow.plusMaxZw; "M" -> currentCapsNow.plusMaxMag; else -> 0 }
                                        if (mAdv <= 0) return@all true
                                        val sLine = updatedLines.find { it.startsWith("$fName:") }
                                        val advBought = sLine?.substringAfter("Plus:")?.trim()?.toIntOrNull() ?: 0
                                        advBought >= mAdv
                                    }

                                    if (updatedAllPrimaryMaxed && !isGMMode) {
                                        expandedStatIndex = -1
                                        if (!updatedAllSecMaxed) {
                                            expandedSecondaryStat = "Żyw"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            enabled = canAfford && (isSkillRequirementMet || isManualEntry || isGMMode),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006600)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("KUP ROZWINIĘCIE ($totalAdvances" + (if (isGMMode) "" else "/$maxAdvanceForLevel") + ")", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Yellow)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val fullName = primaryStatFullNames.getOrNull(expandedStatIndex) ?: ""
                    Text("$fullName: $baseVal + ${totalAdvances * 5} = $currentVal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    val displayStars = maxOf(maxAdvanceForLevel, totalAdvances)
                    for (i in 0 until displayStars) {
                        Text(
                            text = if (i < totalAdvances) "⭐" else "★",
                            color = if (i < totalAdvances) Color.Green else Color.Red,
                            fontSize = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val categoryMapping = mapOf(
                    0 to listOf("Walka Wręcz"),
                    1 to listOf("Umiejętności Strzeleckie"),
                    2 to listOf("Krzepa"),
                    3 to listOf("Odporność"),
                    4 to listOf("Zręczność"),
                    5 to listOf("Inteligencja", "Wiedza", "Język"),
                    6 to listOf("Siła Woli"),
                    7 to listOf("Charyzma")
                )

                val targetCats = categoryMapping[expandedStatIndex] ?: emptyList()
                val ownedSkills = mutableListOf<Triple<String, String, Int>>()

                val specialAbilitiesList = HeroLogic.skillSpecjalny.toList()

                var currentCat = ""
                var isStillInSkills = true
                lines.forEach { line ->
                    val trimmedLine = line.trim()
                    if (trimmedLine.startsWith("EKWIPUNEK:") || trimmedLine.startsWith("CECHY FIZYCZNE:")) {
                        isStillInSkills = false
                    }
                    if (isStillInSkills) {
                        if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                            currentCat = trimmedLine.removeSurrounding("[", "]")
                        } else if (line.contains("•")) {
                            val clean = line.replace("•", "").trim()
                            val hasRank = clean.contains("(")
                            val rawName = if (hasRank) clean.substringBefore("(").trim() else clean.substringBefore("|").trim()
                            val sName = if (rawName.contains(" [")) rawName.substringBefore(" [").trim() else rawName
                            
                            if (targetCats.contains(currentCat) || (expandedStatIndex == 6 && specialAbilitiesList.contains(sName))) {
                                val rankFancy = if (hasRank) clean.substringAfter("(").substringBefore(")").trim() else "𝖀𝖈𝖟𝖊𝖓"
                                var rank = normalizeRank(rankFancy)
                                var pn = clean.substringAfter("PN:").trim().toIntOrNull() ?: 0
                                
                                if (sName == "Szczęście" && rank == "Brak" && pn >= 100) {
                                    rank = "Uczeń"
                                    pn = 0
                                }
                                
                                val existingIdx = ownedSkills.indexOfFirst { it.first == sName }
                                if (existingIdx != -1) {
                                    if (HeroLogic.getRankWeight(rank) > HeroLogic.getRankWeight(ownedSkills[existingIdx].second)) {
                                        ownedSkills[existingIdx] = Triple(sName, rank, pn)
                                    }
                                } else {
                                    ownedSkills.add(Triple(sName, rank, pn))
                                }
                            }
                        }
                    }
                }

                val allPossibleForTarget = targetCats.flatMap { cat ->
                    when (cat) {
                        "Walka Wręcz" -> HeroLogic.skillWW.asIterable()
                        "Umiejętności Strzeleckie" -> HeroLogic.skillUS.asIterable()
                        "Krzepa" -> HeroLogic.skillKrzepa.asIterable()
                        "Odporność" -> HeroLogic.skillOdpornosc.asIterable()
                        "Zręczność" -> HeroLogic.skillZrecznosc.asIterable()
                        "Inteligencja" -> HeroLogic.skillInteligencja.asIterable()
                        "Siła Woli" -> HeroLogic.skillSW.asIterable()
                        "Charyzma" -> HeroLogic.skillCharyzma.asIterable()
                        "Wiedza" -> HeroLogic.skillWiedza.asIterable()
                        "Język" -> HeroLogic.skillJezyk.asIterable()
                        else -> emptyList()
                    }
                }.map { if (it.contains(" [")) it.substringBefore(" [").trim() else it }.distinct()

                val finalSkills = (ownedSkills + allPossibleForTarget.filter { p -> ownedSkills.none { it.first == p } }.map { Triple(it, "Brak", 0) })
                    .filter { (sName, sRank, sPn) ->
                        if (specialAbilitiesList.contains(sName)) {
                            expandedStatIndex == 6 && (sRank != "Brak" || sPn > 0)
                        } else true
                    }
                    .sortedWith(compareByDescending<Triple<String, String, Int>> { HeroLogic.getRankWeight(it.second) }.thenBy { it.first })

                finalSkills.forEach { (sName, sRank, sPn) ->
                    val isSpecial = specialAbilitiesList.contains(sName)
                    val currentRankWeight = HeroLogic.getRankWeight(sRank)
                    
                    val isJezykSkill = HeroLogic.skillJezyk.any { 
                        val clean = if (it.contains(" [")) it.substringBefore(" [").trim() else it
                        clean == sName 
                    } && !sName.contains("Język obcy")
                    
                    val isRestricted = (sName == "Silny cios" || sName == "Splatanie magii" || sName == "Wyczucie kierunku" || 
                                       sName == "Czytanie i pisanie" || sName == "Uzdolnienia językowe" || 
                                       sName == "Widzenie w ciemności" || isJezykSkill) && currentRankWeight >= 1

                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        val displayRank = getFancyRank(sRank)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sName, color = if (sRank == "Brak") Color.Gray else if (isSpecial) Color(0xFFBB86FC) else Color.White, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(displayRank, color = Color.Gray, fontSize = 10.sp)
                                if (sPn > 0) {
                                    LinearProgressIndicator(progress = { sPn / 100f },
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(4.dp)
                                            .padding(horizontal = 4.dp),
                                        color = if (sPn >= 100) Color.Green else Color.Yellow,
                                        trackColor = Color.DarkGray
                                    )
                                    Text("${sPn}𝖕𝖓", color = Color.Yellow, fontSize = 9.sp)
                                }
                            }
                        }
                        
                        if (!isSpecial && !isRestricted) {
                            IconButton(onClick = {
                                playSoundAction(R.raw.klik1, 1)
                                val intent = Intent(context, GameActivity::class.java).apply {
                                    putExtra("HERO_FILE", currentFile); putExtra("MENU_APP", 1); putExtra("STAT_INDEX", expandedStatIndex); putExtra("SKILL_NAME", sName); putExtra("SKILL_RANK", sRank)
                                }
                                context.startActivity(intent)
                            }, modifier = Modifier.size(30.dp)) { Text("🎲", fontSize = 16.sp) }
                        } else {
                             Spacer(modifier = Modifier.size(30.dp))
                        }

                        val nextRank = if (currentRankWeight < 5) getFancyRank(HeroLogic.skillRanks[currentRankWeight + 1]) else null
                        
                        // New language dependency check
                        val isLanguagePismo = sName.startsWith("Język obcy: Pismo")
                        val hasMowaDependency = if (isLanguagePismo) {
                            val languageName = sName.substringAfter("Język obcy: Pismo").trim()
                            val dependencyName = "Język obcy: Mowa $languageName"
                            allPossessedSkills.any { it.first == dependencyName && HeroLogic.getRankWeight(it.second) >= 1 }
                        } else true

                        val awansCost = if (isGMMode) 0 else if (sName.contains("Język obcy") && currentRankWeight == 0) 100 else getSkillUpgradeCost(currentRankWeight + 1)
                        val canAwans = !isRestricted && hasMowaDependency && (sPn >= 100 || isGMMode) && nextRank != null && (isGMMode || currentPd >= awansCost)

                        if (!isSpecial && !isRestricted) {
                            IconButton(onClick = {
                                if (canAwans) {
                                    playSoundAction(R.raw.klik1, 1)
                                    val updatedLines = lines.toMutableList()
                                    val pdIdx = updatedLines.indexOfFirst { it.startsWith("PD:") }
                                    val newPd = if (isGMMode) currentPd else currentPd - awansCost
                                    if (pdIdx != -1) updatedLines[pdIdx] = "PD: $newPd"

                                    val historyFile = "hero_${selectedHeroId}_pd_history"
                                    val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                    val historyEntry = "$timestamp: ${currentPd}𝖕𝖉 - ${awansCost}𝖕𝖉 = ${newPd}𝖕𝖉 [Awans $sName: $nextRank]\n"
                                    saveToFile(historyFile, historyEntry + readFromFile(historyFile))

                                    val skillLineIdx = updatedLines.indexOfFirst { line ->
                                        line.contains("•") && line.contains(sName) &&
                                                (normalizeRank(if (line.contains("(")) line.substringAfter("(").substringBefore(")") else "Uczeń") == sRank)
                                    }
                                    if (skillLineIdx != -1) {
                                        val line = updatedLines[skillLineIdx]
                                        val currentRankInLine = if (line.contains("(")) line.substringAfter("(").substringBefore(")") else ""
                                        val newLine = if (currentRankInLine.isNotEmpty()) {
                                            line.replace("($currentRankInLine)", "($nextRank)")
                                        } else {
                                            val beforePipe = line.substringBefore("|").trim()
                                            val pipePart = if (line.contains("|")) " |" + line.substringAfter("|") else " | PN: 0"
                                            "$beforePipe ($nextRank)$pipePart"
                                        }.replace(Regex("PN:\\s*\\d+"), "PN: 0")
                                        updatedLines[skillLineIdx] = newLine
                                        
                                        // Dla Krzepa+ i Odporność+ dodaj +1 do odpowiedniej statystyki
                                        if (sName == "Krzepa+") {
                                            val krzepLineIdx = updatedLines.indexOfFirst { line ->
                                                val clean = line.trim().removePrefix("•").trim()
                                                clean.startsWith("K:") || clean.startsWith("Krzepa:")
                                            }
                                            if (krzepLineIdx != -1) {
                                                val line = updatedLines[krzepLineIdx]
                                                val afterColon = line.substringAfter(":")
                                                val currentVal = afterColon.substringBefore("|").substringBefore("(").trim().toIntOrNull() ?: 0
                                                val newVal = currentVal + 1
                                                val updatedLine = if (line.contains(":")) {
                                                    val prefix = line.substringBefore(":")
                                                    val suffix = if (line.contains("|")) " |${line.substringAfter("|")}" else ""
                                                    "$prefix: $newVal$suffix"
                                                } else line
                                                updatedLines[krzepLineIdx] = updatedLine
                                            }
                                        } else if (sName == "Odporność+") {
                                            val odpLineIdx = updatedLines.indexOfFirst { line ->
                                                val clean = line.trim().removePrefix("•").trim()
                                                clean.startsWith("O:") || clean.startsWith("Odporność:") || clean.startsWith("Odp:")
                                            }
                                            if (odpLineIdx != -1) {
                                                val line = updatedLines[odpLineIdx]
                                                val afterColon = line.substringAfter(":")
                                                val currentVal = afterColon.substringBefore("|").substringBefore("(").trim().toIntOrNull() ?: 0
                                                val newVal = currentVal + 1
                                                val updatedLine = if (line.contains(":")) {
                                                    val prefix = line.substringBefore(":")
                                                    val suffix = if (line.contains("|")) " |${line.substringAfter("|")}" else ""
                                                    "$prefix: $newVal$suffix"
                                                } else line
                                                updatedLines[odpLineIdx] = updatedLine
                                            }
                                        }
                                        
                                        // Remove any duplicate lower ranks of the same skill
                                        val skillNameClean = sName
                                        val iterator = updatedLines.listIterator()
                                        while (iterator.hasNext()) {
                                            val idx = iterator.nextIndex()
                                            val l = iterator.next()
                                            if (idx != skillLineIdx && l.contains("•") && l.contains(skillNameClean)) {
                                                val otherRankText = if (l.contains("(")) l.substringAfter("(").substringBefore(")") else "Uczeń"
                                                val otherRank = normalizeRank(otherRankText)
                                                if (HeroLogic.getRankWeight(otherRank) < HeroLogic.getRankWeight(normalizeRank(nextRank))) {
                                                    iterator.remove()
                                                }
                                            }
                                        }
                                    } else if (sRank == "Brak") {
                                        // Skill not in file, find correct category to insert
                                        val catToInsert = targetCats.find { cat ->
                                            val list = when (cat) {
                                                "Walka Wręcz" -> HeroLogic.skillWW.toList()
                                                "Umiejętności Strzeleckie" -> HeroLogic.skillUS.toList()
                                                "Krzepa" -> HeroLogic.skillKrzepa.toList()
                                                "Odporność" -> HeroLogic.skillOdpornosc.toList()
                                                "Zręczność" -> HeroLogic.skillZrecznosc.toList()
                                                "Inteligencja" -> HeroLogic.skillInteligencja.toList()
                                                "Siła Woli" -> HeroLogic.skillSW.toList()
                                                "Charyzma" -> HeroLogic.skillCharyzma.toList()
                                                "Wiedza" -> HeroLogic.skillWiedza.toList()
                                                "Język" -> HeroLogic.skillJezyk.toList()
                                                else -> emptyList()
                                            }.map { if (it.contains(" [")) it.substringBefore(" [").trim() else it }
                                            list.contains(sName)
                                        } ?: targetCats.firstOrNull()

                                        if (catToInsert != null) {
                                            val headerIdx = updatedLines.indexOfFirst { it.trim() == "[$catToInsert]" }
                                            if (headerIdx != -1) {
                                                updatedLines.add(headerIdx + 1, " • $sName ($nextRank) | PN: 0")
                                            } else {
                                                // Category header not found, try to find UMIEJĘTNOŚCI (GRUPY):
                                                val mainHeaderIdx = updatedLines.indexOfFirst { it.contains("UMIEJĘTNOŚCI (GRUPY):") }
                                                if (mainHeaderIdx != -1) {
                                                    updatedLines.add(mainHeaderIdx + 1, "[$catToInsert]")
                                                    updatedLines.add(mainHeaderIdx + 2, " • $sName ($nextRank) | PN: 0")
                                                } else {
                                                    // Last resort: append at the end of known skills or file
                                                    updatedLines.add(" • $sName ($nextRank) | PN: 0")
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Dla Krzepa+ i Odporność+ dodaj +1 do odpowiedniej statystyki (gdy dodawana nowa umiejętność)
                                    if (sName == "Krzepa+" && sRank == "Brak") {
                                        val krzepLineIdx = updatedLines.indexOfFirst { line ->
                                            val clean = line.trim().removePrefix("•").trim()
                                            clean.startsWith("K:") || clean.startsWith("Krzepa:")
                                        }
                                        if (krzepLineIdx != -1) {
                                            val line = updatedLines[krzepLineIdx]
                                            val afterColon = line.substringAfter(":")
                                            val currentVal = afterColon.substringBefore("|").substringBefore("(").trim().toIntOrNull() ?: 0
                                            val newVal = currentVal + 1
                                            val updatedLine = if (line.contains(":")) {
                                                val prefix = line.substringBefore(":")
                                                val suffix = if (line.contains("|")) " |${line.substringAfter("|")}" else ""
                                                "$prefix: $newVal$suffix"
                                            } else line
                                            updatedLines[krzepLineIdx] = updatedLine
                                        }
                                    } else if (sName == "Odporność+" && sRank == "Brak") {
                                        val odpLineIdx = updatedLines.indexOfFirst { line ->
                                            val clean = line.trim().removePrefix("•").trim()
                                            clean.startsWith("O:") || clean.startsWith("Odporność:") || clean.startsWith("Odp:")
                                        }
                                        if (odpLineIdx != -1) {
                                            val line = updatedLines[odpLineIdx]
                                            val afterColon = line.substringAfter(":")
                                            val currentVal = afterColon.substringBefore("|").substringBefore("(").trim().toIntOrNull() ?: 0
                                            val newVal = currentVal + 1
                                            val updatedLine = if (line.contains(":")) {
                                                val prefix = line.substringBefore(":")
                                                val suffix = if (line.contains("|")) " |${line.substringAfter("|")}" else ""
                                                "$prefix: $newVal$suffix"
                                            } else line
                                            updatedLines[odpLineIdx] = updatedLine
                                        }
                                    }
                                    
                                    val finalHeroData = updatedLines.joinToString("\n")
                                    val cleanHeroData = HeroLogic.stripLegacyProgressTags(finalHeroData)
                                    saveToFile(currentFile, cleanHeroData)
                                    lines = updatedLines
                                    heroData = finalHeroData
                                    activeSkillSlider = null
                                } else {
                                    if (activeSkillSlider == sName) activeSkillSlider = null else {
                                        activeSkillSlider = sName
                                        skillSliderValue = sPn.toFloat()
                                    }
                                }
                            }, modifier = Modifier.height(30.dp), colors = IconButtonDefaults.iconButtonColors(containerColor = if (canAwans) Color.Magenta else Color.DarkGray)) {
                                Text(if (canAwans) "𝓐𝔀𝓪𝓷𝓼" else "𝖕𝖓", fontSize = if (canAwans) 11.sp else 14.sp, color = Color.White)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))


            }
        }
    }
}
