package com.example.genpro

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpro.ui.theme.GenProTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.io.File
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

data class AdvSkill(val cat: String, val name: String, val rank: String, val pn: Int)

class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GenProTheme {
                GameScreen()
            }
        }
    }
}

@Composable
fun GameScreen() {
    val context = LocalContext.current
    val intent = (context as? Activity)?.intent
    val heroFile = intent?.getStringExtra("HERO_FILE") ?: ""
    val isFromCharacterSheet = intent?.hasExtra("HERO_FILE") == true
    val skipRoll = intent?.getBooleanExtra("SKIP_ROLL", false) ?: false
    val initialSkillName = intent?.getStringExtra("SKILL_NAME") ?: ""
    val initialSkillRank = intent?.getStringExtra("SKILL_RANK") ?: "Brak"

    val coroutineScope = rememberCoroutineScope()

    val playSoundAction = { resId: Int, part: Int ->
        com.example.genpro.playSound(context, coroutineScope, resId, part)
    }

    val rodzajeKostek = remember { arrayOf("K100", "K2", "K4", "K6", "K8", "K10", "K12", "K20") }
    val statystyki = remember { arrayOf("Walka Wręcz", "Strzelectwo", "Krzepa", "Odporność", "Zręczność", "Inteligencja", "Siła Woli", "Charyzma", "Wiedza", "Języki") }

    val skillWW = remember { HeroLogic.skillWW }
    val skillUS = remember { HeroLogic.skillUS }
    val skillKrzepa = remember { HeroLogic.skillKrzepa }
    val skillOdpornosc = remember { HeroLogic.skillOdpornosc }
    val skillZrecznosc = remember { HeroLogic.skillZrecznosc }
    val skillInteligencja = remember { HeroLogic.skillInteligencja }
    val skillSW = remember { HeroLogic.skillSW }
    val skillCharyzma = remember { HeroLogic.skillCharyzma }
    val skillWiedza = remember { HeroLogic.skillWiedza }
    val skillJezyk = remember { HeroLogic.skillJezyk }

    val allSkills = remember {
        listOf(skillWW, skillUS, skillKrzepa, skillOdpornosc, skillZrecznosc, skillInteligencja, skillSW, skillCharyzma, skillWiedza, skillJezyk)
    }

    val readFromFile = { fileName: String ->
        try {
            context.openFileInput(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        }
    }

    val saveToFile = { fileName: String, content: String ->
        try {
            val finalContent = if (fileName.startsWith("hero_")) {
                HeroLogic.stripLegacyProgressTags(content)
            } else {
                content
            }
            context.openFileOutput(fileName, android.content.Context.MODE_PRIVATE).use {
                it.write(finalContent.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val appendToFile = { fileName: String, content: String ->
        try {
            context.openFileOutput(fileName, android.content.Context.MODE_APPEND).use {
                it.write(content.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val addToHistory: (Int, String, Int, Boolean, Int) -> Unit = { mode, result, statValue, multiDice, maxDie -> // 0: Kosci, 1: Staty, 2: PD
        val cyfryIconsA = arrayOf("𝟘", "𝟙", "𝟚", "𝟛", "𝟜", "𝟝", "𝟞", "𝟟", "𝟠", "𝟡")
        val toIconsA = { text: String ->
            text.map { if (it.isDigit()) cyfryIconsA[it - '0'] else it }.joinToString("")
        }
        val fileName = when(mode) {
            0 -> "histKosci"
            1 -> "histStat"
            else -> "histPD"
        }
        val file = File(context.filesDir, fileName)
        val now = Calendar.getInstance()
        val timeStr = toIconsA(String.format(Locale.getDefault(), "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)))
        val cleanResult = result.replace("\n", " ").replace("  ", " ")

        val metadata = when(mode) {
            0 -> "D:$multiDice:$maxDie"
            1 -> "S:$statValue"
            else -> "P"
        }
        var entry = "$timeStr |$metadata| ჻ $cleanResult\n"

        val dateStr = toIconsA(String.format(Locale.getDefault(), "%02d.%02d.%d", now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.MONTH) + 1, now.get(Calendar.YEAR)))
        val dateHeader = "꧁ $dateStr ꧂"

        var needsHeader = false
        if (!file.exists() || file.length() == 0L) {
            needsHeader = true
        } else {
            try {
                val lastLine = file.bufferedReader().useLines { it.lastOrNull { line -> line.startsWith("꧁") } }
                if (lastLine == null || !lastLine.contains(dateStr)) {
                    needsHeader = true
                }
            } catch (e: Exception) {
                needsHeader = true
            }
        }

        if (needsHeader) {
            entry = "$dateHeader\n$entry"
        }
        appendToFile(fileName, entry)
    }

    var wybranyRodzajIndex by remember { mutableIntStateOf(5) }
    val wybranaStatIndexInitial = intent?.getIntExtra("STAT_INDEX", 0) ?: 0

    // Remap index from Character Sheet to GameActivity statystyki array
    val remappedStatIndex = remember(wybranaStatIndexInitial, initialSkillName) {
        if (isFromCharacterSheet && initialSkillName.isNotEmpty()) {
            val content = if (heroFile.isNotEmpty()) readFromFile(heroFile) else ""
            val sInfo = HeroLogic.getSkillInfo(content, initialSkillName)
            val targetStat = when (sInfo.stat) {
                "WW" -> "Walka Wręcz"
                "US" -> "Strzelectwo"
                "S" -> "Krzepa"
                "ODP" -> "Odporność"
                "ZR" -> "Zręczność"
                "INT" -> "Inteligencja"
                "SW" -> "Siła Woli"
                "CHA" -> "Charyzma"
                "Wiedza" -> "Wiedza"
                "Język" -> "Języki"
                else -> ""
            }
            val idx = statystyki.indexOf(targetStat)
            if (idx != -1) idx else wybranaStatIndexInitial
        } else wybranaStatIndexInitial
    }
    
    var wybranaStatIndex by remember { mutableIntStateOf(remappedStatIndex) }

    val getDiceImage = { rodzaj: String ->
        when (rodzaj) {
            "K100" -> R.drawable.k100new
            "K2" -> R.drawable.k2black
            "K4" -> R.drawable.k4black
            "K6" -> R.drawable.k6black
            "K8" -> R.drawable.k8black
            "K10" -> R.drawable.k10black
            "K12" -> R.drawable.k12black
            "K20" -> R.drawable.k20black
            else -> R.drawable.k10white
        }
    }

    var menuApp by remember { mutableIntStateOf(intent?.getIntExtra("MENU_APP", 0) ?: 0) } // 0: Kosci, 1: Staty, 2: Awans (PD/PN)
    var showHistory by remember { mutableStateOf(false) }
    var historyEntries by remember { mutableStateOf(listOf<String>()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val heroStats = remember(heroFile, refreshTrigger) {
        if (heroFile.isNotEmpty()) {
            val content = try {
                context.openFileInput(heroFile).bufferedReader().use { it.readText() }
            } catch (e: Exception) { "" }
            val mappedStats = mutableMapOf<String, Int>()
            
            statystyki.forEach { statName ->
                mappedStats[statName] = HeroLogic.getStatValue(content, statName)
            }
            
            // Stats that might be used by UI (e.g. S, Wt) but are not in the 'statystyki' list
            val extraStats = listOf("S", "Wt", "A", "Żyw", "Sz", "Mag")
            extraStats.forEach { statName ->
                mappedStats[statName] = HeroLogic.getStatValue(content, statName)
            }

            mappedStats
        } else emptyMap<String, Int>()
    }

    BackHandler(enabled = showHistory || isFromCharacterSheet) {
        if (showHistory) {
            showHistory = false
        } else if (isFromCharacterSheet) {
            (context as? Activity)?.finish()
        }
    }

    val updateHistoryList = {
        val fileName = when(menuApp) {
            0 -> "histKosci"
            1 -> "histStat"
            else -> "histPD"
        }
        val content = readFromFile(fileName)
        historyEntries = if (content.isBlank()) listOf("Brak historii") else content.lines().filter { it.isNotBlank() }.reversed()
    }

    var seekGameValue by remember { mutableFloatStateOf(1f) }
    var wybraneKosci by remember { mutableIntStateOf(1) }
    var wynikiRzutow by remember { mutableStateOf("") }
    var currentUsedStatValue by remember { mutableIntStateOf(0) }
    var isDubletStat by remember { mutableStateOf(false) }

    // Skill Game State
    var skillGameStats by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedSkillGameStat by remember { mutableStateOf("") }
    var skillGameResults by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    
    val generateSkillGame = { statName: String ->
        val content = if (heroFile.isNotEmpty()) readFromFile(heroFile) else ""
        val lvlLine = content.lines().find { it.startsWith("POZIOM:") }
        val charLevel = lvlLine?.substringAfter("POZIOM:")?.trim()?.substringBefore(" ")?.toIntOrNull() ?: 0
        val magicValue: Int = heroStats["Mag"] ?: 0

        val skillsSection = content.substringAfter("UMIEJĘTNOŚCI (GRUPY):", "").substringBefore("\n\nCECHY FIZYCZNE:")
        val allCharSkills = mutableListOf<AdvSkill>()
        var currentCat = ""
        skillsSection.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("[")) {
                currentCat = trimmed.trim('[', ']')
            } else if (trimmed.startsWith("•") || trimmed.startsWith("  •")) {
                val skillLine = trimmed.trim(' ', '•')
                val fullName = skillLine.substringBefore(" | PN:").trim()
                val name = if (fullName.contains("(")) fullName.substringBefore(" (").trim() else fullName.trim()
                val rankFancy = if (fullName.contains("(")) fullName.substringAfter("(").substringBefore(")") else "𝖀𝖈𝖟𝖊𝖓"
                val rank = com.example.genpro.normalizeRank(rankFancy)
                allCharSkills.add(AdvSkill(currentCat, name, rank, 0))
            }
        }

        val highestRank = allCharSkills.maxOfOrNull { com.example.genpro.getRankWeight(it.rank) } ?: 0
        val highestRankName = getSkillRankName(highestRank)

        val statIdx = statystyki.indexOf(statName)
        var statSkillPool = allSkills.getOrNull(statIdx)?.toList() ?: emptyList()

        // Rules: Merge Wiedza into Inteligencja
        if (statName == "Inteligencja") {
            statSkillPool = (skillInteligencja.toList() + skillWiedza.toList()).distinct()
        }

        // Filter out "Special" and "+" traits
        val nonTrainable = listOf("Szczęście", "Bardzo odporny", "Bardzo krzepki", "Bardzo szybki", 
            "Bystry wzrok", "Czuły smak", "Czuły węch", "Czuły słuch", "Szósty zmysł",
            "Odporność na strach", "Odporność na grozę")
        
        statSkillPool = statSkillPool.filter { s -> 
            !s.endsWith("+") && 
            !nonTrainable.contains(s) && 
            !s.contains("Język") 
        }

        // Magic restriction
        if (statName == "Siła Woli" && magicValue <= 0) {
            val magicSkills = listOf("Splatanie magii", "Magia Prosta", "Magia Powszechna", "Magia Tajemna", "Wykrywanie magii", "Czynienie cudów")
            statSkillPool = statSkillPool.filter { s -> magicSkills.none { m -> s.contains(m) } }
        }
        
        val results = mutableListOf<Pair<String, String>>()
        
        // Slot 1: Highest possessed rank
        if (statSkillPool.isNotEmpty()) {
            results.add(statSkillPool.random() to highestRankName)
        }

        // Slot 2: 50% Brak, 40% Uczeń, 10% highest rank
        if (statSkillPool.isNotEmpty()) {
            val roll = Random.nextInt(100)
            val rank2 = when {
                roll < 50 -> "Brak"
                roll < 90 -> "Uczeń"
                else -> highestRankName
            }
            results.add(statSkillPool.random() to rank2)
        }

        // Level based slots
        val extraSlots = when {
            charLevel > 3 -> 2
            charLevel >= 1 -> 1
            else -> 0
        }

        repeat(extraSlots) {
            if (statSkillPool.isNotEmpty()) {
                results.add(statSkillPool.random() to "Uczeń")
            }
        }

        skillGameResults = results
    }

    LaunchedEffect(menuApp) {
        if (menuApp == 3) {
            val availableStats = statystyki.filter { it != "Języki" && it != "Wiedza" }
            skillGameStats = availableStats.shuffled().take(2)
            selectedSkillGameStat = ""
            skillGameResults = emptyList()
            wynikiRzutow = ""
        }
    }

    val portraitUri = remember(heroFile, refreshTrigger) {
        if (heroFile.isNotEmpty()) {
            val content = try {
                context.openFileInput(heroFile).bufferedReader().use { it.readText() }
            } catch (e: Exception) { "" }
            content.lines().find { it.startsWith("Portrait:") }?.substringAfter("Portrait:")?.trim()
        } else null
    }

    val preferredStats = remember(heroFile, refreshTrigger) {
        HeroLogic.getClassPreferredStats(if (heroFile.isNotEmpty()) readFromFile(heroFile) else "")
    }

    val heroName = remember(heroFile, refreshTrigger) {
        val content = if (heroFile.isNotEmpty()) readFromFile(heroFile) else ""
        // prawidłowo pobiera imie z karty postaci, w przyszłości dodać zmianę do Aktualizacji
        content.lines().find { it.startsWith("Imie:") }?.substringAfter("Imie:")?.trim() ?: "Bohater"

    }

    val heroClass = remember(heroFile, refreshTrigger) {
        val content = if (heroFile.isNotEmpty()) readFromFile(heroFile) else ""
        val line = content.lines().find { it.contains("Klasa:") } ?: ""
        if (line.contains("Klasa:")) {
             line.substringAfter("Klasa:").trim().substringBefore(",")
        } else {
            "Brak Klasy"
        }
    }

    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    var isRolling by remember { mutableStateOf(false) }
    var autoRollTriggered by remember { mutableStateOf(false) }


    var highlightedSkill by remember { mutableStateOf(initialSkillName) }
    var currentSkillRank by remember { mutableStateOf(initialSkillRank) }

    val startLevel = remember(heroFile, refreshTrigger) {
        if (heroFile.isNotEmpty()) {
            val content = readFromFile(heroFile)
            val line = content.lines().find { it.startsWith("POZIOM:") } ?: ""
            line.substringAfter("｢").substringBefore("｣").trim().toIntOrNull() ?: 0
        } else 0
    }
    val isSessionMode = startLevel > 0

    val heroGender = remember(heroFile, refreshTrigger) {
        if (heroFile.isNotEmpty()) {
            val content = readFromFile(heroFile)
            val line = content.lines().find { it.contains("Płeć:") } ?: ""
            val gender = line.substringAfter("Płeć:").substringBefore(",").trim().lowercase()
            if (gender.startsWith("k")) "kobieta" else "mężczyzna"
        } else "mężczyzna"
    }

    var lastTestedSkillName by remember { mutableStateOf("") }
    var lastTestedStatName by remember { mutableStateOf("") }
    var wasLastAttemptFailed by remember { mutableStateOf(false) }
    var wasLastAttemptSuccess by remember { mutableStateOf(false) }
    var wasLuckUsed by remember { mutableStateOf(false) }
    var lastPnGained by remember { mutableIntStateOf(0) }
    var isLastResultSuccess by remember { mutableStateOf(false) }
    var consecutiveAttemptCount by remember { mutableIntStateOf(0) }
    var isLuckImage by remember { mutableStateOf(false) }

    val currentPF = remember(heroFile, refreshTrigger) {
        val content = if (heroFile.isNotEmpty()) readFromFile(heroFile) else ""
        val line = content.lines().find { it.contains("PF:") || it.contains("🍀") }
        val valuePart = line?.substringAfter(":")?.replace("🍀", "")?.trim()
        valuePart?.toIntOrNull() ?: 0
    }

    val rollK100 = { statName: String, skillName: String, statValue: Int, mod: Int, rank: String ->
        if (!isRolling) {
            val isSecondAttempt = wasLastAttemptFailed && statName == lastTestedStatName && skillName == lastTestedSkillName && !wasLuckUsed

            println("GA475  isSecondAttempt: $isSecondAttempt = $wasLastAttemptFailed && $statName == $lastTestedStatName && $skillName == $lastTestedSkillName && $wasLuckUsed")

            highlightedSkill = skillName
            currentSkillRank = rank

            // If it's not a retry of a failed test, reset luck state
            if (!isSecondAttempt) {
                isLuckImage = false //było false
            }
            
            playSoundAction(R.raw.roll, 2)
            coroutineScope.launch {
                isRolling = true
                wynikiRzutow = " "
                
                if (menuApp == 3) {
                    skillGameResults = emptyList()
                }
                
                if (isSecondAttempt) {
                    isLuckImage = true
                    wasLuckUsed = true
                    println("!GA 496 isSecondAttempt:$isSecondAttempt isLuckImage & wasLuckUsed:$isLuckImage \n")
                    if (heroFile.isNotEmpty()) {
                        val content = readFromFile(heroFile)
                        val lines = content.lines().toMutableList()
                        val pfIdx = lines.indexOfFirst { it.contains("PF:") || it.contains("🍀") }
                        if (pfIdx != -1) {
                            val currentValLine = lines[pfIdx]
                            val currentVal = currentValLine.substringAfter(":")
                                .replace("🍀", "").trim().toIntOrNull() ?: 0

                            if (currentVal > 0) {
                                lines[pfIdx] = "PF: ${currentVal - 1}"
                                var updatedContent = lines.joinToString("\n")

                                // Revert PN gained from the previous failed attempt
                                if (lastPnGained > 0) {
                                    updatedContent = HeroLogic.addPnToSkill(updatedContent, skillName, -lastPnGained)
                                }
                                saveToFile(heroFile, updatedContent)
                                refreshTrigger++
                                // After subtracting PF, if it reaches 0, the button will be disabled via the 'enabled' prop in IconButton
                            }
                        }
                    }
                    delay(100)
                }
                
                launch {
                    rotation.snapTo(0f)
                    rotation.animateTo(
                        targetValue = 720f,
                        animationSpec = tween(durationMillis = 600)
                    )
                }
                launch {
                    scale.snapTo(0.5f)
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                
                delay(600)
                val rzut = Random.nextInt(1, 101)
                val rzutStrBase = if (rzut < 10) "0$rzut" else if (rzut == 100) "00" else rzut.toString()
                isDubletStat = rzutStrBase[0] == rzutStrBase[1]

                val modifiedStat = if (statValue > 0) statValue + mod else 0
                val advancementBonus = when (rank) {
                    "Uczeń" -> 0
                    "Adept" -> 5
                    "Ekspert" -> 10
                    "Mistrz" -> 15
                    "Arcymistrz" -> 20
                    else -> 0
                }
                val baseValueWithAdv = statValue + advancementBonus

                val isSuccess = if (skillName.isNotEmpty()) {
                    if (isDubletStat) {
                        rzut <= baseValueWithAdv
                    } else if (rank == "Brak") {
                        rzut <= modifiedStat
                    } else {
                        rzut <= modifiedStat
                    }
                } else {
                    rzut <= modifiedStat
                }

                // New logic for Criticals
                val isCriticalSuccess = (isDubletStat && isSuccess) || (rzut <= modifiedStat - 30)
                val isCriticalFailure = (isDubletStat && !isSuccess) || (rzut >= modifiedStat + 30)
                val isLuckSuccess = isSecondAttempt && isSuccess

                if (isCriticalSuccess || isLuckSuccess) {
                    val voiceRes = if (heroGender == "kobieta") R.raw.voicewoman else R.raw.voiceman
                    playSoundAction(voiceRes, 0)
                } else if (isCriticalFailure) {
                    playSoundAction(R.raw.devil, 0)
                }

                if (statName == lastTestedStatName && skillName == lastTestedSkillName) {
                    consecutiveAttemptCount++
                } else {
                    consecutiveAttemptCount = 1
                    lastTestedStatName = statName
                    lastTestedSkillName = skillName
                }

                if (isSecondAttempt) {
                    if (heroFile.isNotEmpty()) {
                        if (isSuccess) {
                            val content = readFromFile(heroFile)
                            val finalHero = HeroLogic.addPnToSkill(content, "Szczęście", 1)
                            saveToFile(heroFile, finalHero)
                            val undoInfo = if (lastPnGained > 0) " (anulowano +$lastPnGained PN $skillName)" else ""
                            addToHistory(2, "Test $statName (2. próba): +1 PN Szczęście, -1🍀$undoInfo", 0, false, 0)
                        } else {
                            addToHistory(2, "Test $statName (2. próba): Nieudany, -1🍀", 0, false, 0)
                        }
                        refreshTrigger++
                    }
                    isLuckImage = false
                }
                
                wasLastAttemptFailed = !isSuccess
                wasLastAttemptSuccess = isSuccess
                isLastResultSuccess = isSuccess
                
                if (!isSecondAttempt) {
                    lastPnGained = 0 // Reset for new tests
                }

                val diff = modifiedStat - rzut
                val diffStat = statValue - rzut
                currentUsedStatValue = modifiedStat

                var pnToAdd = 0
                var pdToAddGained = 0
                // Automatyczne dodawanie PN przy testach umiejętności
                if (skillName.isNotEmpty() && statValue > 0) {
                    val content = if (heroFile.isNotEmpty()) readFromFile(heroFile) else ""
                    val skillInfo = HeroLogic.getSkillInfo(content, skillName)
                    val preferredStats = HeroLogic.getClassPreferredStats(content)
                    val isPreferred = preferredStats.contains(statName) || (statName == "Wiedza" && preferredStats.contains("Inteligencja"))
                    
                    val isDoublet = rzut % 11 == 0 && rzut > 0
                    val isSuperSuccess = isSuccess && (diff >= 30 || isDoublet)
                    val isSuperFailure = !isSuccess && (-diff >= 30)
                    val normalizedRank = com.example.genpro.normalizeRank(rank)

                    if (skillName.contains("Język obcy")) {
                        pnToAdd = when {
                            isSuperSuccess -> statValue / 10
                            isSuccess -> 2
                            isSuperFailure -> 0
                            else -> 1
                        }
                    } else if (normalizedRank == "Brak") {
                        pnToAdd = when {
                            isDoublet -> if (rzut <= statValue) statValue / 10 else 0
                            rzut <= statValue - 30 -> 3
                            rzut <= statValue -> 2
                            rzut < statValue + 30 -> 1
                            else -> 0
                        }
                    } else {
                        pnToAdd = when {
                            isCriticalFailure -> 0
                            isCriticalSuccess -> statValue / 10
                            isSuccess -> 2
                            else -> 1
                        }
                    }

                    if (skillName.contains("Język obcy")) {
                        if (rank == "Brak") {
                            // Bonus z talentu Uzdolnienia językowe (+1 PN do testów języków obcych)
                            if (HeroLogic.hasSkill(content, "Uzdolnienia językowe") && pnToAdd > 0) {
                                pnToAdd += 1
                            }
                        } else {
                            pnToAdd = 0 // Języki obce rozwijane tylko do rangi Uczeń
                        }
                    } else if (skillName == "Uzdolnienia językowe") {
                        if (rank != "Brak") {
                            pnToAdd = 0 // Talent Uzdolnienia językowe rozwijany tylko do rangi Uczeń
                        }
                    }
                    if (isPreferred && normalizedRank != "Brak" && pnToAdd > 0) pnToAdd += 1

                    if (pnToAdd > 0 && heroFile.isNotEmpty()) {
                        val updatedLines = content.lines().toMutableList()
                        val skillsHeaderIdx = updatedLines.indexOfFirst { it.startsWith("UMIEJĘTNOŚCI (GRUPY):") }
                        if (skillsHeaderIdx != -1) {
                            // Szukamy konkretnej umiejętności po nazwie w sekcji umiejętności
                            val skillRegex = Regex("•\\s+${Regex.escape(skillName)}(\\s+\\(.*\\))?\\s+\\|\\s+PN:\\s+(\\d+)")
                            var found = false
                            for (i in skillsHeaderIdx + 1 until updatedLines.size) {
                                if (updatedLines[i].startsWith("CECHY FIZYCZNE:") || updatedLines[i].startsWith("CECHY CHARAKTERU:")) break
                                val match = skillRegex.find(updatedLines[i])
                                if (match != null) {
                                    val currentPnStr = match.groupValues[2]
                                    val currentPn = currentPnStr.toInt()
                                    if (currentPn >= 100) {
                                        found = true
                                        break
                                    }
                                    val newPn = (currentPn + pnToAdd).coerceAtMost(100)
                                    val line = updatedLines[i]
                                    val groupRange = match.groups[2]!!.range
                                    updatedLines[i] = line.substring(0, groupRange.first) + newPn + line.substring(groupRange.last + 1)
                                    saveToFile(heroFile, updatedLines.joinToString("\n"))
                                    lastPnGained = pnToAdd
                                    refreshTrigger++
                                    found = true
                                    break
                                }
                            }

                            // Jeśli umiejętności nie ma w pliku (np. ranga Brak), dopisujemy ją do odpowiedniej kategorii
                            if (!found) {
                                val fileCatName = when(skillInfo.stat) {
                                    "WW" -> "Walka Wręcz"
                                    "US" -> "Umiejętności Strzeleckie"
                                    "ZR" -> "Zręczność"
                                    "INT" -> "Inteligencja"
                                    "CHA" -> "Charyzma"
                                    "ODP" -> "Odporność"
                                    "S" -> "Krzepa"
                                    "SW" -> "Siła Woli"
                                    else -> statName
                                }
                                val catHeader = "[$fileCatName]"
                                val catIdx = updatedLines.indexOfFirst { it.trim() == catHeader }
                                if (catIdx != -1) {
                                    updatedLines.add(catIdx + 1, "  • $skillName (𝓑𝓻𝓪𝓴) | PN: $pnToAdd")
                                } else {
                                    val insertIdx = updatedLines.indexOfFirst { it.startsWith("CECHY FIZYCZNE:") }.let { if (it == -1) updatedLines.size else it }
                                    updatedLines.add(insertIdx, "[$fileCatName]")
                                    updatedLines.add(insertIdx + 1, "  • $skillName (𝓑𝓻𝓪𝓴) | PN: $pnToAdd")
                                }
                                saveToFile(heroFile, updatedLines.joinToString("\n"))
                                lastPnGained = pnToAdd
                                refreshTrigger++
                            }
                        }
                    }
                } else if (skillName.isEmpty() && statValue > 0) {
                    // Automatyczne dodawanie PD przy testach na statystykę
                    if (diffStat >= 0) {
                        pdToAddGained = (diffStat / 10).coerceAtLeast(1)
                        if (isDubletStat) pdToAddGained *= 2
                    }

                    if (pdToAddGained > 0 && heroFile.isNotEmpty()) {
                        val content = readFromFile(heroFile)
                        val updatedLines = content.lines().toMutableList()
                        val pdIdx = updatedLines.indexOfFirst { it.startsWith("PD:") || it.startsWith("PD: ") }
                        if (pdIdx != -1) {
                            val currentPd = updatedLines[pdIdx].substringAfter(":").trim().toIntOrNull() ?: 0
                            val newPd = currentPd + pdToAddGained
                            updatedLines[pdIdx] = "PD: $newPd"
                            saveToFile(heroFile, updatedLines.joinToString("\n"))
                            // Dodaj do historii PD
                            addToHistory(2, "Test $statName: +$pdToAddGained PD (Suma: $newPd)", 0, false, 0)
                            refreshTrigger++
                        }
                    }
                }

                val success = if (statValue > 0) {
                    val sign = if (diff > 0) "+" else if (diff < 0) "" else ""
                    val diffInfo = if (diff != 0) "[$sign$diff]" else ""
                    val pnInfo = if (pnToAdd > 0) " +$pnToAdd"+"𝖕𝖓" else if (isDubletStat && skillName.isNotEmpty()) {
                        if (isSuccess) " 𝖅𝖓𝖆𝖐𝖔𝖒𝖎𝖙𝖞!" else " 𝕻𝖔𝖜𝖆ż𝖓𝖆 𝕻𝖔𝖗𝖆ż𝖐𝖆!"
                    } else if (isDubletStat && skillName.isEmpty()) " 𝔇𝔲𝔟𝔩𝔢𝔱" else ""
                    val pdInfo = if (pdToAddGained > 0) " +$pdToAddGained" + "𝖕𝖉" else if (isDubletStat && skillName.isEmpty()) " 𝔇𝔲𝔟𝔩𝔢𝔱" else ""
                    val luckInfo = if (isSecondAttempt && isSuccess) " +1☘" else ""
                    "$diffInfo$pnInfo$pdInfo$luckInfo"
                } else ""

                val rankText = if (rank != "Brak") " (${getFancyRankName(rank)})" else " (𝓑𝓻𝓪𝓴)"
                val label = if (skillName.isNotEmpty()) "$statName $skillName$rankText" else statName
                wynikiRzutow = if (success.isNotEmpty()) "‹$rzutStrBase›\n$success" else "‹$rzutStrBase›"
                addToHistory(1, "$label: $wynikiRzutow", currentUsedStatValue, false, 0)
                isRolling = false
            }
        }
    }

    LaunchedEffect(heroStats) {
        if (isFromCharacterSheet && !autoRollTriggered && heroStats.isNotEmpty() && !skipRoll) {
            val statName = statystyki.getOrNull(wybranaStatIndex) ?: ""
            if (statName.isNotEmpty()) {
                val statValue = heroStats[statName] ?: 0
                val modifier = when (com.example.genpro.normalizeRank(initialSkillRank)) {
                    "Uczeń" -> 0
                    "Adept" -> 5
                    "Ekspert" -> 10
                    "Mistrz" -> 15
                    "Arcymistrz" -> 20
                    "Brak" -> if (initialSkillName.isNotEmpty()) {
                        if (initialSkillName.contains("Język obcy") || initialSkillName == "Język Klasyczny" || initialSkillName == "Język Magiczny" || initialSkillName == "Uzdolnienia językowe") 0 else -30
                    } else 0
                    else -> 0
                }
                rollK100(statName, initialSkillName, statValue, modifier, initialSkillRank)
                autoRollTriggered = true
            }
        }
    }

    val heroSkills = remember(heroFile, refreshTrigger) {
        if (heroFile.isNotEmpty()) {
            val content = readFromFile(heroFile)
            val skillsSection = content.substringAfter("UMIEJĘTNOŚCI (GRUPY):", "").substringBefore("\n\nCECHY FIZYCZNE:")
            val lines = skillsSection.lines()
            val map = mutableMapOf<String, List<Triple<String, String, Int>>>()
            var currentCat = ""
            lines.forEach { line ->
                if (line.startsWith("[")) {
                    currentCat = line.trim('[', ']')
                    // Use common mapping for category names if needed
                } else if (line.trim().startsWith("•")) {
                    val skillLine = line.trim(' ', '•')
                    val fullName = skillLine.substringBefore(" | PN:").trim()
                    val pnValue = skillLine.substringAfter("PN:", "0").trim().toIntOrNull() ?: 0

                    val name = if (fullName.contains("(")) fullName.substringBefore(" (").trim() else fullName.trim()

                    // Filter only valid skills from HeroLogic
                    if (HeroLogic.allNormalSkills.contains(name)) {
                        val rankFancy = if (fullName.contains("(")) fullName.substringAfter("(").substringBefore(")") else "𝖀𝖈𝖟𝖊𝖓"
                        val rank = com.example.genpro.normalizeRank(rankFancy)

                        // Unified mapping for categories to match statystyki array
                        val targetCat = when {
                            currentCat == "Umiejętności Strzeleckie" || currentCat == "Strzelectwo" -> "Strzelectwo"
                            currentCat == "Język" || currentCat == "Języki" -> "Języki"
                            else -> currentCat
                        }
                        map[targetCat] = (map[targetCat] ?: emptyList()) + Triple(name, rank, pnValue)
                    }
                }
            }
            // Sort each category's skills by rank weight (desc) and name (asc)
            val sortedMap = map.mapValues { (_, skills) ->
                skills.sortedWith(compareByDescending<Triple<String, String, Int>> { com.example.genpro.getRankWeight(it.second) }.thenBy { it.first })
            }
            sortedMap
        } else emptyMap()
    }

    val prevDiceIndex = (wybranyRodzajIndex - 1 + rodzajeKostek.size) % rodzajeKostek.size
    val nextDiceIndex = (wybranyRodzajIndex + 1) % rodzajeKostek.size
    val prevStatIndex = (wybranaStatIndex - 1 + statystyki.size) % statystyki.size
    val nextStatIndex = (wybranaStatIndex + 1) % statystyki.size

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            val boxScope = this
            AnimatedContent(
                targetState = if (showHistory) "Historia: ${if (menuApp == 0) "Kości" else if (menuApp == 1) "Staty" else "Awans"}"
                else if (menuApp == 0) "Kość ﹝${rodzajeKostek[wybranyRodzajIndex]}﹞"
                else if (menuApp == 1 || menuApp == 2) "🍀 $currentPF"
                else if (menuApp == 3) "$heroName 🍀$currentPF\n$heroClass"
                else "Bohater",
                transitionSpec = {
                    val isPF = (targetState.startsWith("PF:") || targetState.startsWith("🍀:")) &&
                               (initialState.startsWith("PF:") || initialState.startsWith("🍀:"))
                    if (isPF) {
                        val targetVal = targetState.substringAfter(": ").toIntOrNull() ?: 0
                        val initialVal = initialState.substringAfter(": ").toIntOrNull() ?: 0
                        if (targetVal < initialVal) {
                            (slideInVertically { height -> height } + fadeIn()) togetherWith
                                    slideOutVertically { height -> -height } + fadeOut()
                        } else {
                            (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                    slideOutVertically { height -> height } + fadeOut()
                        }
                    } else {
                        fadeIn(animationSpec = tween(330, delayMillis = 90)) togetherWith
                                fadeOut(animationSpec = tween(90))
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                modifier = Modifier.align(Alignment.TopCenter),
                label = "HeaderAnimation"
            ) { targetText ->
                Text(
                    text = targetText,
                    fontSize = 28.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(80.dp))

            IconButton(
                onClick = {
                    playSoundAction(R.raw.klik1, 0)
                    if (showHistory) {
                        showHistory = false
                    } else {
                        updateHistoryList()
                        showHistory = true
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (showHistory) Icons.Default.Close else Icons.AutoMirrored.Filled.List,
                    contentDescription = "Historia",
                    tint = Color.White
                )
            }

            if (!showHistory) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(58.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray)
                        .clickable {
                            if (heroFile.isNotEmpty()) {
                                val id = heroFile
                                    .removePrefix("hero_")
                                    .removeSuffix(".txt")
                                    .toIntOrNull() ?: 1
                                val mainIntent = Intent(context, MainActivity::class.java).apply {
                                    putExtra("MENU_APP", 10)
                                    putExtra("SELECTED_HERO_ID", id)
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                context.startActivity(mainIntent)
                            } else {
                                val mainIntent = Intent(context, MainActivity::class.java).apply {
                                    putExtra("MENU_APP", 0)
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                context.startActivity(mainIntent)
                            }
                        }
                ) {
                    if (portraitUri != null) {
                        val bitmap = remember(portraitUri) {
                            try {
                                context.contentResolver.openInputStream(Uri.parse(portraitUri))?.use {
                                    BitmapFactory.decodeStream(it)
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Portrait",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                        }
                    } else {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                if (showHistory) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp)
                    ) {
                        items(historyEntries) { entry ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                            ) {
                                Text(
                                    text = com.example.genpro.colorize(entry),
                                    fontSize = 17.sp
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                } else if (menuApp == 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Spacer(modifier = Modifier.height(26.dp))

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 56.dp)
                        ) {
                            items(9) { index ->
                                val multiplier = index + 1
                                val isSelected = wybraneKosci == multiplier
                                InputChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (!isRolling) {
                                            wybraneKosci = multiplier
                                            seekGameValue = multiplier.toFloat()
                                            playSoundAction(R.raw.klik1, 1)
                                        }
                                    },
                                    label = {
                                        Text(
                                            "${multiplier}x",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = Color.Yellow,
                                        containerColor = Color.DarkGray
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))


                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 76.dp)
                        ) {
                            itemsIndexed(rodzajeKostek) { index, rodzaj ->
                                val isSelected = wybranyRodzajIndex == index
                                InputChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (!isRolling) {
                                            wybranyRodzajIndex = index
                                            playSoundAction(R.raw.klik1, 0)
                                        }
                                    },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Image(
                                                painter = painterResource(id = getDiceImage(rodzaj)),
                                                contentDescription = null,
                                                modifier = Modifier.size(50.dp)
                                            )

                                        }
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = Color.Yellow,
                                        containerColor = Color.DarkGray
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    IconButton(
                        onClick = {
                            if (wybraneKosci > 0 && !isRolling) {
                                playSoundAction(R.raw.roll, 2)
                                coroutineScope.launch {
                                    isRolling = true
                                    wynikiRzutow = " "
                                    launch {
                                        rotation.snapTo(0f)
                                        rotation.animateTo(
                                            targetValue = 720f,
                                            animationSpec = tween(durationMillis = 600)
                                        )
                                    }
                                    launch {
                                        scale.snapTo(0.5f)
                                        scale.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                    delay(600)
                                    val maxVal = rodzajeKostek[wybranyRodzajIndex].substring(1).toInt()
                                    val rzuty = List(wybraneKosci) { Random.nextInt(1, maxVal + 1) }
                                    val counts = rzuty.groupingBy { it }.eachCount()
                                    val hasRepeat = counts.values.any { it > 1 }

                                    val losSuma = rzuty.sum()

                                    // PD Gain from multi-dice: (sum / 10) + (multipliers if repeats)
                                    var pdFromMulti = (losSuma / 10).coerceAtLeast(1)
                                    if (hasRepeat) {
                                        val maxRepeat = counts.values.maxOrNull() ?: 1
                                        pdFromMulti *= maxRepeat
                                    }

                                    val finalRzuty = rzuty.map { rzut -> "‹$rzut›" }
                                    val formattedSuma = losSuma.toString()

                                    if (hasRepeat) {
                                        playSoundAction(R.raw.fanmusic, 1)
                                    }

                                    if (wybraneKosci < 7) {
                                        val rzutyStr = finalRzuty.joinToString("   ")
                                        wynikiRzutow = if (wybraneKosci > 1) "$rzutyStr\n﹝‹$formattedSuma›﹞" else rzutyStr
                                    } else {
                                        val firstRow = finalRzuty.take(6).joinToString("   ")
                                        val secondRow = finalRzuty.drop(6).joinToString("   ")
                                        wynikiRzutow = "$firstRow\n$secondRow  ﹝‹$formattedSuma›﹞"
                                    }

                                    if (pdFromMulti > 0 && heroFile.isNotEmpty()) {
                                        val content = readFromFile(heroFile)
                                        val updatedLines = content.lines().toMutableList()
                                        val pdIdx = updatedLines.indexOfFirst { it.startsWith("PD:") || it.startsWith("PD: ") }
                                        if (pdIdx != -1) {
                                            val currentPd = updatedLines[pdIdx].substringAfter(":").trim().toIntOrNull() ?: 0
                                            val newPd = currentPd + pdFromMulti
                                            updatedLines[pdIdx] = "PD: $newPd"
                                            saveToFile(heroFile, updatedLines.joinToString("\n"))
                                            addToHistory(2, "Rzut ${wybraneKosci}${rodzajeKostek[wybranyRodzajIndex]}: +$pdFromMulti PD (Suma: $newPd)", 0, false, 0)
                                            refreshTrigger++
                                        }
                                    }

                                    addToHistory(0, "${wybraneKosci}${rodzajeKostek[wybranyRodzajIndex]}: $wynikiRzutow", 0, wybraneKosci > 1, if (wybraneKosci == 1) maxVal else 0)
                                    isRolling = false
                                }
                            }
                        },
                        modifier = Modifier.size(110.dp)
                    ) {
                        Image(
                            painter = painterResource(id = getDiceImage(rodzajeKostek[wybranyRodzajIndex])),
                            contentDescription = "Rzut kośćmi",
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(rotation.value)
                                .scale(scale.value)
                        )
                    }
                } else if (menuApp == 1) {
                    val currentStatName = statystyki[wybranaStatIndex]
                    val heroStatValue = heroStats[currentStatName] ?: 0
                    val skillsForStat = heroSkills[currentStatName] ?: emptyList()
                    val canUseLuck = wasLastAttemptFailed && currentStatName == lastTestedStatName && highlightedSkill == lastTestedSkillName && !wasLuckUsed

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Column(
                            modifier = Modifier.padding(bottom = 15.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (highlightedSkill.isNotEmpty()) highlightedSkill else currentStatName,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (highlightedSkill.isNotEmpty()) Color.Yellow else Color.White
                            )
                            if (heroStatValue > 0) {
                                val content = if (heroFile.isNotEmpty()) readFromFile(heroFile) else ""
                                val skillInfo = if (highlightedSkill.isNotEmpty()) HeroLogic.getSkillInfo(content, highlightedSkill) else null
                                
                                val actualStatName = when (skillInfo?.stat) {
                                    "WW" -> "Walka Wręcz"
                                    "US" -> "Strzelectwo"
                                    "ZR" -> "Zręczność"
                                    "INT" -> "Inteligencja"
                                    "CHA" -> "Charyzma"
                                    "ODP" -> "Odporność"
                                    "S" -> "Krzepa"
                                    "SW" -> "Siła Woli"
                                    else -> currentStatName
                                }
                                val actualStatValue = if (skillInfo != null) heroStats[actualStatName] ?: heroStatValue else heroStatValue

                                val modifier = when (currentSkillRank) {
                                    "Uczeń" -> 0
                                    "Adept" -> 5
                                    "Ekspert" -> 10
                                    "Mistrz" -> 15
                                    "Arcymistrz" -> 20
                                    "Brak" -> if (highlightedSkill.isNotEmpty()) {
                                        if (highlightedSkill.contains("Język obcy")) 0 else -30
                                    } else 0
                                    else -> 0
                                }
                                val modText = if (modifier >= 0) "+$modifier" else "$modifier"
                                val rankText = if (highlightedSkill.isNotEmpty()) " (${getFancyRankName(currentSkillRank)})" else ""

                                Text(
                                    text = "$rankText $actualStatName: $actualStatValue  [$modText]",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${actualStatValue + modifier}",
                                    color = Color.Yellow,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(30.dp))

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            itemsIndexed(statystyki) { index, stat ->
                                val isSelected = wybranaStatIndex == index
                                InputChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (!isRolling) {
                                            wybranaStatIndex = index
                                            highlightedSkill = ""
                                            currentSkillRank = "Brak"
                                            isLuckImage = false
                                            wasLastAttemptFailed = false
                                            wasLastAttemptSuccess = false
                                            wasLuckUsed = false
                                            wynikiRzutow = ""
                                            playSoundAction(R.raw.klik1, 1)
                                        }
                                    },
                                    label = {
                                        Text(
                                            stat,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = Color.Yellow,
                                        containerColor = Color.DarkGray
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (skillsForStat.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(skillsForStat) { skillTriple ->
                                    val skill = skillTriple.first
                                    val rank = skillTriple.second
                                    val pn = skillTriple.third
                                    val isHighlighted = highlightedSkill == skill

                                    if (skill != "Szczęście") {
                                        SuggestionChip(
                                            enabled = !isRolling,
                                            onClick = {
                                                isLuckImage = false
                                                wasLastAttemptFailed = false
                                                wasLastAttemptSuccess = false
                                                wasLuckUsed = false
                                                val modifier = when (rank) {
                                                    "Uczeń" -> 0
                                                    "Adept" -> 5
                                                    "Ekspert" -> 10
                                                    "Mistrz" -> 15
                                                    "Arcymistrz" -> 20
                                                    else -> 0
                                                }
                                            val content = if (heroFile.isNotEmpty()) readFromFile(heroFile) else ""
                                            val skillInfo = HeroLogic.getSkillInfo(content, skill)
                                            val actualStatName = when (skillInfo.stat) {
                                                "WW" -> "Walka Wręcz"
                                                "US" -> "Strzelectwo"
                                                "ZR" -> "Zręczność"
                                                "INT" -> "Inteligencja"
                                                "CHA" -> "Charyzma"
                                                "ODP" -> "Odporność"
                                                "S" -> "Krzepa"
                                                "SW" -> "Siła Woli"
                                                else -> currentStatName
                                            }
                                            val actualStatValue = heroStats[actualStatName] ?: heroStatValue

                                            rollK100(
                                                actualStatName,
                                                skill,
                                                actualStatValue,
                                                modifier,
                                                rank
                                            )
                                            lastTestedStatName = actualStatName
                                            lastTestedSkillName = skill
                                        },
                                            label = {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        skill,
                                                        color = if (isHighlighted) Color.Black else Color.White
                                                    )
                                                    Text(
                                                        "${getFancyRankName(rank)} | $pn pn",
                                                        fontSize = 9.sp,
                                                        color = if (isHighlighted) Color.Black.copy(
                                                            alpha = 0.7f
                                                        ) else Color.Gray
                                                    )
                                                }
                                            },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = if (isHighlighted) Color.Yellow else Color.Yellow.copy(
                                                    alpha = 0.2f
                                                ),
                                                labelColor = if (isHighlighted) Color.Black else Color.Yellow
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(allSkills[wybranaStatIndex].filter { skill -> !skillsForStat.any { it.first == skill } }) { skill ->
                                val isHighlighted = highlightedSkill == skill
                                if (skill != "Szczęście") {
                                    SuggestionChip(
                                        enabled = !isRolling,
                                        onClick = {
                                            isLuckImage = false
                                            wasLastAttemptFailed = false
                                            wasLastAttemptSuccess = false
                                            wasLuckUsed = false
                                            val modifier = if (skill.contains("Język obcy") || skill == "Język Klasyczny" || skill == "Język Magiczny" || skill == "Uzdolnienia językowe") 0 else -30
                                            
                                            val content = if (heroFile.isNotEmpty()) readFromFile(heroFile) else ""
                                            val skillInfo = HeroLogic.getSkillInfo(content, skill)
                                            val actualStatName = when (skillInfo.stat) {
                                                "WW" -> "Walka Wręcz"
                                                "US" -> "Strzelectwo"
                                                "ZR" -> "Zręczność"
                                                "INT" -> "Inteligencja"
                                                "CHA" -> "Charyzma"
                                                "ODP" -> "Odporność"
                                                "S" -> "Krzepa"
                                                "SW" -> "Siła Woli"
                                                else -> currentStatName
                                            }
                                            val actualStatValue = heroStats[actualStatName] ?: heroStatValue
                                            
                                            rollK100(
                                                actualStatName,
                                                skill,
                                                actualStatValue,
                                                modifier,
                                                "Brak"
                                            )
                                            lastTestedStatName = actualStatName
                                            lastTestedSkillName = skill
                                        },
                                        label = {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    skill,
                                                    color = if (isHighlighted) Color.Black else Color.White
                                                )
                                                val modText = if (skill.contains("Język obcy") || skill == "Język Klasyczny" || skill == "Język Magiczny" || skill == "Uzdolnienia językowe") "0" else "-30"
                                                Text(
                                                    "𝓑𝓻𝓪𝓴 | $modText",
                                                    fontSize = 9.sp,
                                                    color = if (isHighlighted) Color.Black.copy(
                                                        alpha = 0.7f
                                                    ) else Color.Gray
                                                )
                                            }
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (isHighlighted) Color.Yellow else Color.White.copy(
                                                alpha = 0.1f
                                            ),
                                            labelColor = if (isHighlighted) Color.Black else Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val imageRes = if (isLuckImage) {
                                R.drawable.button_luck
                            } else if (canUseLuck && currentPF > 0 && !isRolling) {
                                android.R.drawable.ic_menu_rotate
                            } else {
                                R.drawable.k100new
                            }

                            Image(
                                painter = painterResource(id = imageRes),
                                contentDescription = "Rzut K100",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(rotation.value)
                                    .scale(scale.value)
                                    .clickable(enabled = !isRolling) {
                                        if (canUseLuck && currentPF > 0 && !wasLuckUsed) {
                                            val content = if (heroFile.isNotEmpty()) readFromFile(heroFile) else ""
                                            val skillInfo = if (highlightedSkill.isNotEmpty()) HeroLogic.getSkillInfo(content, highlightedSkill) else null
                                            val actualStatName = when (skillInfo?.stat) {
                                                "WW" -> "Walka Wręcz"
                                                "US" -> "Strzelectwo"
                                                "ZR" -> "Zręczność"
                                                "INT" -> "Inteligencja"
                                                "CHA" -> "Charyzma"
                                                "ODP" -> "Odporność"
                                                "S" -> "Krzepa"
                                                "SW" -> "Siła Woli"
                                                else -> currentStatName
                                            }
                                            val actualStatValue = if (skillInfo != null) heroStats[actualStatName] ?: heroStatValue else heroStatValue

                                            val modifier = when (currentSkillRank) {
                                                "Uczeń" -> 0
                                                "Adept" -> 5
                                                "Ekspert" -> 10
                                                "Mistrz" -> 15
                                                "Arcymistrz" -> 20
                                                "Brak" -> if (highlightedSkill.isNotEmpty()) {
                                                    if (highlightedSkill.contains("Język obcy") || highlightedSkill == "Język Klasyczny" || highlightedSkill == "Język Magiczny" || highlightedSkill == "Uzdolnienia językowe") 0 else -30
                                                } else 0
                                                else -> 0
                                            }
                                            rollK100(
                                                actualStatName,
                                                highlightedSkill,
                                                actualStatValue,
                                                modifier,
                                                currentSkillRank
                                            )
                                        } else if (wynikiRzutow.isNotBlank() && wynikiRzutow != " ") {
                                            wynikiRzutow = ""
                                            isLuckImage = false
                                            wasLastAttemptFailed = false
                                            wasLastAttemptSuccess = false
                                            wasLuckUsed = false
                                        } else {
                                            val content = if (heroFile.isNotEmpty()) readFromFile(heroFile) else ""
                                            val skillInfo = if (highlightedSkill.isNotEmpty()) HeroLogic.getSkillInfo(content, highlightedSkill) else null
                                            val actualStatName = when (skillInfo?.stat) {
                                                "WW" -> "Walka Wręcz"
                                                "US" -> "Strzelectwo"
                                                "ZR" -> "Zręczność"
                                                "INT" -> "Inteligencja"
                                                "CHA" -> "Charyzma"
                                                "ODP" -> "Odporność"
                                                "S" -> "Krzepa"
                                                "SW" -> "Siła Woli"
                                                else -> currentStatName
                                            }
                                            val actualStatValue = if (skillInfo != null) heroStats[actualStatName] ?: heroStatValue else heroStatValue

                                            val modifier = when (currentSkillRank) {
                                                "Uczeń" -> 0
                                                "Adept" -> 5
                                                "Ekspert" -> 10
                                                "Mistrz" -> 15
                                                "Arcymistrz" -> 20
                                                "Brak" -> if (highlightedSkill.isNotEmpty()) {
                                                    if (highlightedSkill.contains("Język obcy") || highlightedSkill == "Język Klasyczny" || highlightedSkill == "Język Magiczny" || highlightedSkill == "Uzdolnienia językowe") 0 else -30
                                                } else 0
                                                else -> 0
                                            }
                                            rollK100(
                                                actualStatName,
                                                highlightedSkill,
                                                actualStatValue,
                                                modifier,
                                                currentSkillRank
                                            )
                                        }
                                    }
                            )
                        }

                        if (isLuckImage) {
                            Text(
                                text = "SZCZĘŚCIE",
                                color = Color.Green,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                else if (menuApp == 2) {
                    var charPd by remember { mutableIntStateOf(0) }
                    var charLevel by remember { mutableIntStateOf(1) }
                    var charSkills by remember { mutableStateOf(listOf<AdvSkill>()) }

                    val loadCharData = {
                        if (heroFile.isNotEmpty()) {
                            val content = readFromFile(heroFile)
                            val pdLine = content.lines().find { it.startsWith("PD:") }
                            charPd = pdLine?.substringAfter("PD:")?.trim()?.toIntOrNull() ?: 0

                            val lvlLine = content.lines().find { it.startsWith("POZIOM:") }
                            charLevel = lvlLine?.substringAfter("POZIOM:")?.trim()?.substringBefore(" ")?.toIntOrNull() ?: 1

                            val skillsSection = content.substringAfter("UMIEJĘTNOŚCI (GRUPY):", "").substringBefore("\n\nCECHY FIZYCZNE:")
                            val lines = skillsSection.lines()
                            val list = mutableListOf<AdvSkill>()
                            var currentCat = ""
                            lines.forEach { line ->
                                val trimmed = line.trim()
                                if (trimmed.startsWith("[")) {
                                    currentCat = trimmed.trim('[', ']')
                                } else if (trimmed.startsWith("•") || trimmed.startsWith("  •")) {
                                    val skillLine = trimmed.trim(' ', '•')
                                    val fullName = skillLine.substringBefore(" | PN:").trim()
                                    val pnValue = skillLine.substringAfter(" | PN:").trim().toIntOrNull() ?: 0

                                    val name = if (fullName.contains("(")) fullName.substringBefore(" (").trim() else fullName.trim()
                                    val rankFancy = if (fullName.contains("(")) fullName.substringAfter("(").substringBefore(")") else "𝖀𝖈𝖟𝖊𝖓"

                                    val rank = com.example.genpro.normalizeRank(rankFancy)
                                    if (list.none { it.name == name && it.cat == currentCat }) {
                                        list.add(AdvSkill(currentCat, name, rank, pnValue))
                                    }
                                }
                            }
                            charSkills = list.sortedWith(compareByDescending<AdvSkill> { com.example.genpro.getRankWeight(it.rank) }.thenBy { it.name })
                        }
                    }

                    val saveToHeroFile = { newPd: Int, newSkills: List<AdvSkill> ->
                        if (heroFile.isNotEmpty()) {
                            val content = readFromFile(heroFile)
                            val pdLine = content.lines().find { it.startsWith("PD:") || it.startsWith("PD: ") } ?: "PD: 0"
                            var newContent = content.replaceFirst(pdLine, "PD: $newPd")

                            val skillsHeader = "UMIEJĘTNOŚCI (GRUPY):"
                            val skillsFooter = "\n\nCECHY FIZYCZNE:"
                            val beforeSkills = newContent.substringBefore(skillsHeader) + skillsHeader + "\n"
                            val afterSkills = skillsFooter + newContent.substringAfter(skillsFooter)

                            val sb = StringBuilder()
                            newSkills.groupBy { it.cat }.forEach { (cat, skills) ->
                                sb.append("[$cat]\n")
                                skills.forEach { s ->
                                    val rankFancy = com.example.genpro.getFancyRankName(s.rank)
                                    val fullName = if (s.rank == "Brak") s.name else "${s.name} ($rankFancy)"
                                    sb.append("  • $fullName | PN: ${s.pn}\n")
                                }
                            }

                            newContent = beforeSkills + sb.toString() + afterSkills
                            saveToFile(heroFile, newContent)
                            refreshTrigger++
                        }
                    }

                    LaunchedEffect(menuApp) {
                        if (menuApp == 2) loadCharData()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 140.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Dostępne PD: ‹$charPd›", fontSize = 24.sp, modifier = Modifier.padding(8.dp))

                        val reqs = com.example.genpro.getLevelRequirements(charLevel)
                        val uCount = charSkills.count { it.rank == "Uczeń" }
                        val aCount = charSkills.count { it.rank == "Adept" }
                        val eCount = charSkills.count { it.rank == "Ekspert" }
                        val mCount = charSkills.count { it.rank == "Mistrz" }
                        val amCount = charSkills.count { it.rank == "Arcymistrz" }

                        fun formatCounter(label: String, current: Int, max: Int): String {
                            val check = if (current >= max && max > 0) "✔️" else ""
                            return "$label:$current/$max$check"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text(text = "𝖀:${uCount}/${reqs.lvlUmax}", color = Color.White, fontSize = 12.sp)
                            Text(text = formatCounter("𝕬", aCount, reqs.lvlAmax), color = Color.Yellow, fontSize = 12.sp)
                            Text(text = formatCounter("𝕰", eCount, reqs.lvlEmax), color = Color.Yellow, fontSize = 12.sp)
                            Text(text = formatCounter("𝕸", mCount, reqs.lvlMmax), color = Color.Green, fontSize = 12.sp)
                            if (reqs.lvlAMax > 0 || amCount > 0) {
                                Text(text = formatCounter("𝕬𝖗", amCount, reqs.lvlAMax), color = Color.Red, fontSize = 12.sp)
                            }
                        }

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(charSkills) { skill ->
                                val skillRanks = listOf("Brak", "Uczeń", "Adept", "Ekspert", "Mistrz", "Arcymistrz")
                                val currentRankIndex = skillRanks.indexOf(skill.rank).coerceAtLeast(0)
                                val nextRankIndex = currentRankIndex + 1
                                val upgradeCost = com.example.genpro.getSkillUpgradeCost(nextRankIndex)
                                val pnCost = com.example.genpro.getPnCost(currentRankIndex)

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    color = Color.White.copy(alpha = 0.05f),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(skill.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            val currentRankFancy = com.example.genpro.getFancyRankName(skill.rank)
                                            Text("$currentRankFancy | PN: ‹${skill.pn}›", fontSize = 14.sp)
                                        }

                                        val canUpgradeByPn = skill.pn >= 100 && nextRankIndex < skillRanks.size

                                        if (canUpgradeByPn) {
                                            val nextRankName = skillRanks[nextRankIndex]
                                            val nextRankFancy = com.example.genpro.getFancyRankName(nextRankName)

                                            Button(
                                                onClick = {
                                                    if (charPd >= upgradeCost) {
                                                        charPd -= upgradeCost
                                                        val updated = charSkills.map {
                                                            if (it.name == skill.name) it.copy(rank = nextRankName, pn = 0) else it
                                                        }
                                                        charSkills = updated
                                                        saveToHeroFile(charPd, updated)
                                                        addToHistory(2, "Awansowano ${skill.name} na $nextRankFancy (-$upgradeCost PD)", 0, false, 0)
                                                        playSoundAction(R.raw.roll, 1)
                                                    }
                                                },
                                                enabled = charPd >= upgradeCost,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    "𝓐𝔀𝓪𝓷𝓼 $nextRankFancy ($upgradeCost PD)",
                                                    fontSize = 12.sp,
                                                    color = if (charPd >= upgradeCost) Color.Yellow else Color.Gray
                                                )
                                            }
                                        } else if (nextRankIndex < skillRanks.size) {
                                            IconButton(
                                                onClick = {
                                                    if (charPd >= pnCost) {
                                                        charPd -= pnCost
                                                        val updated = charSkills.map {
                                                            if (it.name == skill.name) it.copy(pn = it.pn + 1) else it
                                                        }
                                                        charSkills = updated
                                                        saveToHeroFile(charPd, updated)
                                                        addToHistory(2, "Kupiono PN dla ${skill.name} (-$pnCost PD)", 0, false, 0)
                                                        playSoundAction(R.raw.klik1, 0)
                                                    }
                                                },
                                                enabled = charPd >= pnCost
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Dodaj PN",
                                                        tint = if (charPd >= pnCost) Color.White else Color.Gray
                                                    )
                                                    Text(
                                                        "+1𝖕𝖓 ($pnCost PD)",
                                                        fontSize = 9.sp,
                                                        color = if (charPd >= pnCost) Color.Yellow else Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else if (menuApp == 3) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                        ) {
                            val statsToDisplay = if (selectedSkillGameStat.isEmpty()) skillGameStats else listOf(selectedSkillGameStat)
                            
                            statsToDisplay.forEach { stat ->
                                val sVal = heroStats[stat] ?: 0
                                val cyfry = arrayOf("𝟘", "𝟙", "𝟚", "𝟛", "𝟜", "𝟝", "𝟞", "𝟟", "𝟠", "𝟡")
                                val fancyVal = sVal.toString().map { if (it.isDigit()) cyfry[it - '0'] else it.toString() }.joinToString("")
                                val isPreferred = preferredStats.contains(stat.trim())
                                
                                InputChip(
                                    modifier = Modifier.height(90.dp).weight(1f),
                                    selected = selectedSkillGameStat == stat,
                                    onClick = { 
                                        if (selectedSkillGameStat.isEmpty()) {
                                            selectedSkillGameStat = stat
                                            generateSkillGame(stat)
                                            wynikiRzutow = ""
                                            playSoundAction(R.raw.klik1, 0)
                                        } else if (selectedSkillGameStat == stat && wasLastAttemptFailed && currentPF > 0) {
                                            // Resetuj wybór jeśli test nieudany i mamy PF, a kliknięto w statystykę
                                            selectedSkillGameStat = ""
                                            val availableStats = statystyki.filter { it != "Języki" && it != "Wiedza" }
                                            skillGameStats = availableStats.shuffled().take(2)
                                            skillGameResults = emptyList()
                                            wynikiRzutow = ""
                                            wasLastAttemptFailed = false
                                            wasLastAttemptSuccess = false
                                            wasLuckUsed = false
                                            isLuckImage = false
                                            highlightedSkill = ""
                                            playSoundAction(R.raw.klik1, 0)
                                        }
                                    },
                                    label = { 
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Text(
                                                stat, 
                                                fontSize = 18.sp, 
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 20.sp,
                                                color = if (selectedSkillGameStat == stat && isPreferred) Color.White else if (selectedSkillGameStat == stat) Color.Black else Color.White
                                            )
                                            Text(
                                                fancyVal,
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = if (selectedSkillGameStat == stat && isPreferred) Color.White else if (selectedSkillGameStat == stat) Color.Black else Color.White
                                            )
                                        }
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = if (isPreferred) Color(0xFFA300B6) else Color.Yellow,
                                        containerColor = if (isPreferred) Color(0xFFA300B6).copy(alpha = 0.3f) else Color.DarkGray,
                                        labelColor = Color.White,
                                        selectedLabelColor = if (isPreferred) Color.White else Color.Black
                                    ),
                                    border = if (isPreferred) BorderStroke(2.dp, Color(0xFFA300B6)) else null
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(15.dp))
                        
                        if (skillGameResults.isNotEmpty() && selectedSkillGameStat.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 8.dp, start = 16.dp, end = 16.dp)
                            ) {
                                items(skillGameResults) { (skillName, rank) ->
                                    val statValue = heroStats[selectedSkillGameStat] ?: 0
                                    val modifier = when (rank) {
                                        "Uczeń" -> 0
                                        "Adept" -> 5
                                        "Ekspert" -> 10
                                        "Mistrz" -> 15
                                        "Arcymistrz" -> 20
                                        "Brak" -> if (skillName.contains("Język obcy")) 0 else -30
                                        else -> 0
                                    }
                                    
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp).padding(horizontal = 16.dp)
                                            .clickable(enabled = !isRolling) {
                                                rollK100(selectedSkillGameStat, skillName, statValue, modifier, rank)
                                            },
                                        color = Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(18.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    skillName, 
                                                    fontWeight = FontWeight.ExtraBold, 
                                                    fontSize = 18.sp,
                                                    color = Color.White
                                                )
                                                Text(
                                                    getFancyRankName(rank), 
                                                    color = Color.Yellow, 
                                                    fontSize = 15.sp,
                                                    fontStyle = FontStyle.Italic
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    "Suma: ${statValue + modifier}",
                                                    color = Color.Green,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "Podstawa: $statValue",
                                                    color = Color.Gray,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedSkillGameStat.isNotEmpty() && (isRolling || (wynikiRzutow.isNotBlank() && wynikiRzutow != " "))) {
                            // Animowana kostka i wynik rzutu na samym dole
                            Box(
                                modifier = Modifier.padding(bottom = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val canUseLuck = wasLastAttemptFailed &&
                                            selectedSkillGameStat == lastTestedStatName &&
                                            highlightedSkill == lastTestedSkillName &&
                                            highlightedSkill.isNotEmpty() &&
                                            !wasLuckUsed &&
                                            currentPF > 0

                                    val diceImage = if (canUseLuck && !isRolling) {
                                        android.R.drawable.ic_menu_rotate
                                    } else {
                                        R.drawable.k100new
                                    }

                                    Image(
                                        painter = painterResource(id = diceImage),
                                        contentDescription = "Rzut K100",
                                        modifier = Modifier
                                            .size(260.dp)
                                            .rotate(rotation.value)
                                            .scale(scale.value)
                                            .clickable(enabled = !isRolling) {
                                                val canUseLuck = wasLastAttemptFailed &&
                                                        selectedSkillGameStat == lastTestedStatName &&
                                                        highlightedSkill == lastTestedSkillName &&
                                                        highlightedSkill.isNotEmpty() &&
                                                        !wasLuckUsed &&
                                                        currentPF > 0

                                                if (canUseLuck) {
                                                    val statValue = heroStats[selectedSkillGameStat] ?: 0
                                                    val skillInfo = skillGameResults.find { it.first == highlightedSkill }
                                                    val rank = skillInfo?.second ?: "Brak"
                                                    val modifier = when (rank) {
                                                        "Uczeń" -> 0
                                                        "Adept" -> 5
                                                        "Ekspert" -> 10
                                                        "Mistrz" -> 15
                                                        "Arcymistrz" -> 20
                                                        "Brak" -> if (highlightedSkill.contains("Język obcy")) 0 else -30
                                                        else -> 0
                                                    }
                                                    rollK100(selectedSkillGameStat, highlightedSkill, statValue, modifier, rank)
                                                } else {
                                                    wynikiRzutow = ""
                                                    if (menuApp == 3) {
                                                        selectedSkillGameStat = ""
                                                        val availableStats = statystyki.filter { it != "Języki" && it != "Wiedza" }
                                                        skillGameStats = availableStats.shuffled().take(2)
                                                        skillGameResults = emptyList()
                                                        wasLastAttemptFailed = false
                                                        wasLastAttemptSuccess = false
                                                        wasLuckUsed = false
                                                        isLuckImage = false
                                                    }
                                                }
                                            }
                                    )
                                    
                                    if (!isRolling && wynikiRzutow.isNotBlank() && wynikiRzutow != " ") {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            if (menuApp == 3 && highlightedSkill.isNotEmpty()) {
                                                Text(
                                                    text = highlightedSkill,
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.Gray,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(bottom = 2.dp)
                                                )
                                            }

                                            val lines = wynikiRzutow.split("\n")
                                            if (menuApp == 3 && lines.isNotEmpty()) {
                                                Text(
                                                    text = lines[0],
                                                    fontSize = 50.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isLastResultSuccess) Color(0xFF4CAF50) else Color(0xFFF44336),
                                                    textAlign = TextAlign.Center
                                                )
                                                if (lines.size > 1) {
                                                    Text(
                                                        text = com.example.genpro.colorize(lines[1]),
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = com.example.genpro.colorize(wynikiRzutow),
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (wynikiRzutow.startsWith("‹")) (if (isLastResultSuccess) Color(0xFF4CAF50) else Color(0xFFF44336)) else Color.White,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                if (menuApp < 2) {
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Displaying bonuses from Special Items (Tools/Instruments)
                    val specialItemsInInv = remember(refreshTrigger) {
                        val inv = readFromFile("game_inventory").split(";").filter { it.isNotBlank() }
                        val allSpecial = HeroLogic.toolsSpecialist + HeroLogic.instrumentsSpecialist + HeroLogic.itemsExotic
                        allSpecial.filter { item -> inv.any { it.contains(item.name, ignoreCase = true) } }
                    }

                    if (specialItemsInInv.isNotEmpty()) {
                        Text("BONUSY Z PRZEDMIOTÓW:", color = Color.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp))
                        LazyRow(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(specialItemsInInv) { item ->
                                Surface(
                                    color = Color.Blue.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(item.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(item.bonus, color = Color.Yellow, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = com.example.genpro.colorize(
                                text = if (wynikiRzutow.isEmpty()) " " else wynikiRzutow,
                                heroMode = (menuApp == 1 || menuApp == 3),
                                heroStatValue = currentUsedStatValue,
                                isMultiDice = (menuApp == 0 && wybraneKosci > 1),
                                maxDieValue = if (menuApp == 0 && wybraneKosci == 1) {
                                    rodzajeKostek[wybranyRodzajIndex].substring(1).toIntOrNull() ?: 0
                                } else 0
                            ),
                            color = Color.Gray,
                            fontSize = if (menuApp == 3) {
                                if (isDubletStat) 36.sp else 35.sp
                            } else if (menuApp == 1) {
                                if (isDubletStat) 32.sp else 31.sp
                            } else {
                                when (wybraneKosci) {
                                    1 -> 55.sp
                                    2 -> 52.sp
                                    3 -> 48.sp
                                    4 -> 43.sp
                                    5 -> 39.sp
                                    6 -> 35.sp
                                    7 -> 32.sp
                                    8 -> 29.sp
                                    9 -> 27.sp
                                    else -> 25.sp
                                }
                            },
                            lineHeight = if (menuApp == 3) {
                                if (isDubletStat) 37.sp else 35.sp
                            } else if (menuApp == 1) {
                                if (isDubletStat) 33.sp else 31.sp
                            } else {
                                when (wybraneKosci) {
                                    1 -> 58.sp
                                    2 -> 58.sp
                                    3 -> 54.sp
                                    4 -> 49.sp
                                    5 -> 45.sp
                                    6 -> 41.sp
                                    7 -> 38.sp
                                    8 -> 35.sp
                                    9 -> 33.sp
                                    else -> 31.sp
                                }
                            },
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(140.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(with(boxScope) { Modifier.align(Alignment.BottomCenter) })
                    .padding(start = 8.dp, end = 8.dp, bottom = 50.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!showHistory) {
                    Button(
                        onClick = {
                            playSoundAction(R.raw.klik1, 0)
                            if (menuApp == 0) wybranyRodzajIndex = prevDiceIndex
                            else wybranaStatIndex = prevStatIndex
                        },
                        modifier = Modifier.weight(0.6f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        if (menuApp == 0) {
                            Icon(
                                painter = painterResource(id = getDiceImage(rodzajeKostek[prevDiceIndex])),
                                contentDescription = "Poprzedni",
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Poprzedni",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(0.6f))
                }

                Button(
                    onClick = {
                        playSoundAction(R.raw.klik1, 1)
                        menuApp = when (menuApp) {
                            0 -> 1
                            1 -> if (startLevel == 0) 3 else 0
                            3 -> 0
                            else -> 0
                        }
                        wynikiRzutow = ""
                        if (showHistory) {
                            updateHistoryList()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black)
                ) {
                    Icon(
                        painter = painterResource(
                            id = when(menuApp) {
                                0 -> R.drawable.unknowhero
                                1 -> if (startLevel == 0) R.drawable.k100new else R.drawable.k100new // Tu można dać inną ikonę dla Skill Game
                                3 -> R.drawable.k100new
                                else -> R.drawable.k100new
                            }
                        ),
                        contentDescription = "Zmień tryb",
                        modifier = Modifier.size(45.dp)
                    )
                }

                if (!showHistory) {
                    Button(
                        onClick = {
                            playSoundAction(R.raw.klik1, 0)
                            if (menuApp == 0) wybranyRodzajIndex = nextDiceIndex
                            else if (menuApp == 1) wybranaStatIndex = nextStatIndex
                        },
                        modifier = Modifier.weight(0.6f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        if (menuApp == 0) {
                            Icon(
                                painter = painterResource(id = getDiceImage(rodzajeKostek[nextDiceIndex])),
                                contentDescription = "Następny",
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Następny",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(0.6f))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun GameScreenPreview() {
    GenProTheme {
        GameScreen()
    }
}
