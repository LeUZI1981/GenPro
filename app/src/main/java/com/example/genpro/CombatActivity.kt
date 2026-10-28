package com.example.genpro

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpro.ui.theme.GenProTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

// Data classes and Enums removed - now in UiUtils.kt

// Type aliases or minimal wrappers if absolutely needed for source compatibility, 
// but preferred to use centralized models directly.
typealias CombatArmorPiece = ArmorPiece

class CombatActivity : ComponentActivity() {
    private var saveTrigger: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GenProTheme {
            CombatScreen(onSaveTriggerCreated = { trigger -> 
                saveTrigger = trigger 
            })
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveTrigger?.invoke()
    }

    override fun onStop() {
        super.onStop()
        saveTrigger?.invoke()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveTrigger = null
    }
}

@Composable
fun StatBadge(label: String, value: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 8.sp, color = color, fontWeight = FontWeight.Bold)
            Text(value.toString(), fontSize = 10.sp, color = Color.White)
        }
    }
}

@Composable
fun StatusIndicators(
    modifier: Modifier = Modifier,
    isPoisoned: Boolean = false,
    isBurning: Boolean = false,
    isFrozen: Boolean = false,
    isShocked: Boolean = false,
    isBlinded: Boolean = false
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (isPoisoned) Text("🤢", fontSize = 10.sp)
        if (isBurning) Text("🔥", fontSize = 10.sp)
        if (isFrozen) Text("❄️", fontSize = 10.sp)
        if (isShocked) Text("⚡", fontSize = 10.sp)
        if (isBlinded) Text("🕶️", fontSize = 10.sp)
    }
}

@Composable
fun CombatScreen(onSaveTriggerCreated: (() -> Unit) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Stan walki i deklaracje wyprzedzające (przeniesione na górę, aby uniknąć błędów zakresu)
    var isCombatOver by remember { mutableStateOf(false) }
    var canReroll by remember { mutableStateOf(false) }
    var lastEnemyRoll by remember { mutableIntStateOf(100) }
    var lastEnemySL by remember { mutableIntStateOf(0) }
    var lastEnemySuccess by remember { mutableStateOf(false) }
    var lastPenalty by remember { mutableIntStateOf(0) }
    var difficultyModifier by remember { mutableIntStateOf(0) }
    var playerIsPoisoned by remember { mutableStateOf(false) }
    var playerIsBurning by remember { mutableStateOf(false) }
    var playerIsFrozen by remember { mutableStateOf(false) }
    var playerIsShocked by remember { mutableStateOf(false) }
    var playerIsBlinded by remember { mutableStateOf(false) }
    var isGameOverAnimation by remember { mutableStateOf(false) }

    // New states for skill limits
    var dodgesUsed by remember { mutableIntStateOf(0) }
    var parriesUsed by remember { mutableIntStateOf(0) }
    var targetChangesUsed by remember { mutableIntStateOf(0) }

    var checkCombatEnd: () -> Unit by remember { mutableStateOf({ }) }
    var allyTurn: () -> Unit by remember { mutableStateOf({ }) }
    var enemyTurn: () -> Unit by remember { mutableStateOf({ }) }
    var resolveAttack: (Boolean, Boolean) -> Unit by remember { mutableStateOf({ _, _ -> }) }

    val playSoundAction = { resId: Int, part: Int ->
        com.example.genpro.playSound(context, coroutineScope, resId, part)
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

    val readFromFile = { fileName: String ->
        try {
            context.openFileInput(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        }
    }

    val currentHeroId = (context as? ComponentActivity)?.intent?.getIntExtra("SELECTED_HERO_ID", -1)
    val suffix = if (currentHeroId != -1) "_$currentHeroId" else ""
    val heroFile = "hero_$currentHeroId"

    val heroData = remember { readFromFile(heroFile) }
    val heroLines = remember { heroData.lines() }

    val getSkillRank = { skillName: String ->
        HeroLogic.getSkillRank(heroLines, skillName)
    }

    val getRankBonus = { skillName: String ->
        HeroLogic.getRankBonus(heroLines, skillName)
    }

    LaunchedEffect(Unit) {
        // Rejestracja zostanie wykonana po zainicjalizowaniu saveQuickSave
    }

    var playerGold by remember { mutableIntStateOf(0) }
    var playerSilver by remember { mutableIntStateOf(0) }
    var playerCopper by remember { mutableIntStateOf(0) }

    LaunchedEffect(heroData) {
        heroLines.forEach { line ->
            if (line.contains("⛀")) {
                val v = line.trim().split("  ").last().toIntOrNull() ?: 0
                if (line.contains("Złota")) playerGold = v
                else if (line.contains("Srebrny")) playerSilver = v
                else if (line.contains("Miedziany")) playerCopper = v
            }
        }
    }

    val heroName = remember {
        heroLines.getOrNull(0)?.let {
            if (it.contains(":")) it.substringAfter(":").trim() else it.trim()
        }?.ifEmpty { "Bezimienny" } ?: "Bezimienny"
    }

    val heroLevel = remember {
        heroLines.find { it.trim().startsWith("POZIOM:") }?.substringAfter(":")?.trim()
            ?.substringBefore(" ")?.toIntOrNull() ?: 1
    }

    val playerRace = remember {
        heroLines.find { it.contains("Rasa:", ignoreCase = true) }
            ?.substringAfter("Rasa:")?.substringBefore(",")?.trim() ?: "Człowiek"
    }

    // Player Stats & Skills
    val heroStats = remember {
        val stats = mutableMapOf<String, Int>()
        val statsSection = heroLines.indexOfFirst { it.trim() == "STATYSTYKI GŁÓWNE:" }
        if (statsSection != -1) {
            val primaryStatFullNames = listOf(
                "Walka Wręcz",
                "Strzelectwo",
                "Krzepa",
                "Odporność",
                "Zręczność",
                "Inteligencja",
                "Siła Woli",
                "Charyzma"
            )
            heroLines.drop(statsSection + 1).take(8).forEachIndexed { index, line ->
                val total = line.substringAfter(":").substringBefore("|").trim().toIntOrNull() ?: 20
                stats[primaryStatFullNames[index]] = total
            }
        }
        val secStatsSection = heroLines.indexOfFirst { it.trim() == "STATYSTYKI DRUGORZĘDNE:" }
        if (secStatsSection != -1) {
            heroLines.drop(secStatsSection + 1).takeWhile { it.contains(":") }.forEach { line ->
                val name = line.substringBefore(":").trim()
                val total = line.substringAfter(":").substringBefore("|").trim().toIntOrNull() ?: 0
                stats[name] = total
            }
        }
        stats
    }

    val hasQuickReflexes = remember { heroData.contains("Szybki refleks", ignoreCase = true) }
    val hasLuckSkill = remember { heroData.contains("Szczęście", ignoreCase = true) }
    val hasLightningReflexes =
        remember { heroData.contains("Błyskawiczny refleks", ignoreCase = true) }
    val hasWarriorBorn = remember { heroData.contains("Urodzony wojownik", ignoreCase = true) }
    val hasBrawling = remember { heroData.contains("Bijatyka", ignoreCase = true) }
    val hasWrestling = remember { heroData.contains("Zapasy", ignoreCase = true) }
    val hasRapidReload = remember { heroData.contains("Szybkie przeładowanie", ignoreCase = true) }

    val fatePoints = remember {
        heroLines.find { it.contains("Punkty Przeznaczenia") }?.substringAfter(":")
            ?.substringBefore("|")?.trim()?.toIntOrNull() ?: 2
    }

    var luckPoints by remember { mutableIntStateOf(if (hasLuckSkill) fatePoints * 2 else fatePoints) }

    val playerInjuries = remember {
        mutableStateListOf<String>().apply {
            val raw = readFromFile("game_injuries$suffix")
            if (raw.isNotEmpty()) addAll(raw.split(";").filter { it.isNotBlank() })
        }
    }

    val playerEquipment = remember {
        val eq = mutableListOf<String>()
        var isReadingEq = false
        heroLines.forEach { line ->
            val t = line.trim()
            if (t.startsWith("EKWIPUNEK:")) isReadingEq = true
            else if (isReadingEq && (t.startsWith("HISTORIA:") || t.startsWith("CECHY") || t.startsWith(
                    "NOTATKI"
                ) || (t.isEmpty() && eq.isNotEmpty()))
            ) {
                isReadingEq = false
            } else if (isReadingEq && line.contains("•")) {
                eq.add(line.replace("•", "").trim())
            }
        }
        eq.toMutableStateList()
    }

    val playerInventory = playerEquipment

    val activeArmor = remember(playerEquipment.toList()) {
        val armorMap = BodyArmor()
        playerEquipment.forEach { line ->
            if (line.contains("🅔") || line.contains("🛡️")) {
                HeroLogic.parseArmorPiece(line, playerRace)?.let {
                    armorMap.pieces.add(it)
                }
            }
        }
        armorMap
    }

    // Weapons database
    val weaponDb = HeroLogic.weaponDb

    val playerHpMax = remember(activeArmor) {
        heroLines.find { it.contains("Żywotność:") || it.contains("Żyw:") }?.let { line ->
            val base = line.substringAfter(":").substringBefore("|").trim().toIntOrNull() ?: 10
            val plus = line.substringAfter("Plus:").trim().toIntOrNull() ?: 0
            val suffixBonus = activeArmor.pieces.sumOf { it.suffix.vitalityBonus }
            base + plus + suffixBonus
        } ?: (12 + activeArmor.pieces.sumOf { it.suffix.vitalityBonus })
    }

    val maxHead = (playerHpMax * 0.75).toInt()
    val maxTorso = (playerHpMax * 1.5).toInt()
    val maxArm = playerHpMax
    val maxLeg = (playerHpMax * 1.1).toInt()

    var playerBodyParts by remember {
        val hpLine = heroLines.find { it.trim().startsWith("LOKACJE_HP:") }
        val savedHps = hpLine?.substringAfter(":")?.trim()?.split(",")?.mapNotNull { it.trim().toIntOrNull() }

        mutableStateOf(
            BodyParts(
                head = savedHps?.getOrNull(0) ?: readFromFile("game_hp_head$suffix").toIntOrNull() ?: maxHead,
                torso = savedHps?.getOrNull(1) ?: readFromFile("game_hp_torso$suffix").toIntOrNull() ?: maxTorso,
                rightArm = savedHps?.getOrNull(2) ?: readFromFile("game_hp_r_arm$suffix").toIntOrNull() ?: maxArm,
                leftArm = savedHps?.getOrNull(3) ?: readFromFile("game_hp_l_arm$suffix").toIntOrNull() ?: maxArm,
                rightLeg = savedHps?.getOrNull(4) ?: readFromFile("game_hp_r_leg$suffix").toIntOrNull() ?: maxLeg,
                leftLeg = savedHps?.getOrNull(5) ?: readFromFile("game_hp_l_leg$suffix").toIntOrNull() ?: maxLeg,
                maxHead = maxHead,
                maxTorso = maxTorso,
                maxRArm = maxArm,
                maxLArm = maxArm,
                maxRLeg = maxLeg,
                maxLLeg = maxLeg
            )
        )
    }

    // WH4e Mechanics: Advantage & Momentum
    var playerAdvantage by remember { mutableIntStateOf(0) }
    var playerMomentum by remember { mutableIntStateOf(0) } // Pęd dla walki wręcz
    val enemyAdvantage = remember { mutableStateMapOf<Int, Int>() } // ID wroga -> Przewaga

    val calculateSL = { roll: Int, target: Int ->
        (target / 10) - (roll / 10)
    }

    val rollDamageDie = { die: String, crushing: Boolean, mighty: Boolean, log: SnapshotStateList<String> ->
        val parts = die.lowercase().split("k")
        if (parts.size == 2) {
            val count = parts[0].trim().toIntOrNull() ?: 1
            val sides = parts[1].trim().toIntOrNull() ?: 3
            var total = 0
            var maxCount = 0

            repeat(count) {
                var r = Random.nextInt(1, sides + 1)
                if (crushing) {
                    val r2 = Random.nextInt(1, sides + 1)
                    if (r2 > r) r = r2
                }
                total += r
                if (r == sides) maxCount++
            }

            if (mighty && maxCount > 0) {
                var extra = 0
                repeat(maxCount) {
                    var exp = Random.nextInt(1, sides + 1)
                    extra += exp
                }
                total += extra
                if (extra > 0) log.add(0, "⚡ POTĘŻNY CIOS! Dodatkowe obrażenia: +$extra")
            }
            total
        } else 0
    }

    val reverseRoll = { roll: Int ->
        if (roll == 100) 100
        else {
            val tens = roll / 10
            val ones = roll % 10
            (ones * 10) + tens
        }
    }

    var activeWeapon by remember(playerEquipment.toList()) {
        val equippedLine = playerEquipment.find { it.contains("🅔") }
        val initialWeapon = equippedLine?.let { HeroLogic.parseWeapon(it) }
        mutableStateOf<CombatWeapon?>(initialWeapon)
    }

    val getWeaponSkillName = { weapon: CombatWeapon? ->
        HeroLogic.getWeaponSkillName(weapon)
    }


    // Effective stats with injuries
    val playerWW = remember(playerInjuries.size, hasWarriorBorn, activeWeapon, heroLines) {
        var base = (heroStats["Walka Wręcz"] ?: 30) + (if (hasWarriorBorn) 5 else 0)

        // Dodanie bonusu z rangi umiejętności
        val skillName = getWeaponSkillName(activeWeapon)
        base += HeroLogic.getRankBonus(heroLines, skillName)

        activeWeapon?.let {
            if (it.rangedType == RangedType.NONE) base += it.prefix.wwBonus
        }
        playerInjuries.forEach { name ->
            criticalInjuriesTable.find { it.name == name && it.stat == "Walka Wręcz" }
                ?.let { base += it.penalty }
        }
        base
    }

    val playerUS = remember(playerInjuries.size, activeWeapon, heroLines) {
        var base = (heroStats["Strzelectwo"] ?: 30)

        // Dodanie bonusu z rangi umiejętności
        val skillName = getWeaponSkillName(activeWeapon)
        base += HeroLogic.getRankBonus(heroLines, skillName)

        activeWeapon?.let {
            if (it.rangedType != RangedType.NONE) base += it.prefix.usBonus
        }
        playerInjuries.forEach { name ->
            criticalInjuriesTable.find { it.name == name && it.stat == "Strzelectwo" }
                ?.let { base += it.penalty }
        }
        base
    }

    val playerT = remember(playerInjuries.size, activeWeapon, heroLines) {
        var base = (heroStats["Odporność"] ?: 30) + (activeWeapon?.prefix?.odpornoscBonus ?: 0)
        playerInjuries.forEach { name ->
            criticalInjuriesTable.find { it.name == name && (it.stat == "Wytrzymałość" || it.stat == "Odporność") }
                ?.let { base += it.penalty }
        }
        val rank = HeroLogic.getSkillRank(heroLines, "Odporność+")
        (base / 10) + rank
    }

    val playerS = remember(playerInjuries.size, activeWeapon, heroLines) {
        var base = (heroStats["Krzepa"] ?: 30) + (activeWeapon?.prefix?.krzepaBonus ?: 0)
        playerInjuries.forEach { name ->
            criticalInjuriesTable.find { it.name == name && it.stat == "Krzepa" }
                ?.let { base += it.penalty }
        }
        val rank = getSkillRank("Krzepa+")
        (base / 10) + rank
    }
    val playerAgility = (heroStats["Zręczność"] ?: 30) + (activeWeapon?.prefix?.zrecznoscBonus ?: 0)
    val playerSpeed = (heroStats["Szybkość"] ?: heroStats["Ruch"]
    ?: 4) + activeArmor.pieces.sumOf { it.suffix.speedBonus }
    val playerWill = (heroStats["Siła Woli"] ?: 30) + (activeWeapon?.prefix?.silaWoliBonus ?: 0)

    val playerI = (heroStats["Inicjatywa"] ?: (heroStats["Zręczność"]
        ?: 30)) + (if (hasLightningReflexes) 5 else 0) + (activeWeapon?.prefix?.initBonus ?: 0)
    val playerDodge = remember(playerAgility, activeWeapon, playerInjuries.size) {
        var base = playerAgility
        activeWeapon?.let {
            base += it.prefix.dodgeBonus
        }
        playerInjuries.forEach { name ->
            criticalInjuriesTable.find { it.name == name && (it.stat == "Walka Wręcz" || it.stat == "Zręczność") }
                ?.let { if (it.location == "Noga") base += it.penalty }
        }
        base
    }
    val playerMove = playerSpeed


    var actionsLeft by remember { mutableIntStateOf(2) }
    var isWeaponLoaded by remember { mutableStateOf(true) }
    var reloadProgress by remember { mutableIntStateOf(0) }
    val combatLog = remember { mutableStateListOf<String>() }

    val isRanked = { skillName: String ->
        getSkillRank(skillName) > 0
    }

    val dodgeRank = remember(heroLines) { getSkillRank("Uniki") }
    val parryRank = remember(heroLines) { getSkillRank("Parowanie") }
    val quickReflexesRank = remember(heroLines) { getSkillRank("Szybki refleks") }
    val dualWieldRank = remember(heroLines) { getSkillRank("Walka dwoma broniami") }
    val toughnessSkillRank = remember(heroLines) { getSkillRank("Odporność+") }
    val strengthSkillRank = remember(heroLines) { getSkillRank("Krzepa+") }
    val poisonResRank = remember(heroLines) { getSkillRank("Odporność na Trucizny") }
    val fireResRank = remember(heroLines) { getSkillRank("Odporność na Ogień") }
    val coldResRank = remember(heroLines) { getSkillRank("Odporność na Zimno") }
    val lightningResRank = remember(heroLines) { getSkillRank("Odporność na Pioruny") }

    val availableAbilities = remember(heroLines, activeWeapon, playerWW, isWeaponLoaded) {
        val list = mutableListOf<String>()
        val wwMinus30 = playerWW - 30

        val weaponSkillName = getWeaponSkillName(activeWeapon)
        val isUczeńWeapon = isRanked(weaponSkillName)

        if (heroLines.any {
                it.contains(
                    "Mierzony strzał",
                    ignoreCase = true
                )
            }) list.add("Mierzony strzał")

        val hasStunningWeapon = activeWeapon?.traits?.contains("Ogłuszający", true) == true
        if (hasStunningWeapon && (isUczeńWeapon || isRanked("Ogłuszanie") || wwMinus30 >= 11)) {
            list.add("Ogłuszenie")
        }

        if (isUczeńWeapon || isRanked("Finta") || wwMinus30 >= 11) list.add("Finta")
        if (isUczeńWeapon || isRanked("Riposta") || wwMinus30 >= 11) list.add("Riposta")
        if (isUczeńWeapon || isRanked("Akrobatyczny atak") || isRanked("Akrobata")) list.add("Akrobatyczny atak")

        if (heroLines.any { it.contains("Rozbrojenie", ignoreCase = true) }) list.add("Rozbrojenie")
        if (activeWeapon?.traits?.contains("Defensywny", true) == true) list.add("Postawa obronna")

        // Dodawanie akcji przeładowania dla broni dystansowej
        if (activeWeapon != null && activeWeapon!!.rangedType != RangedType.NONE && activeWeapon!!.reloadActions > 0 && !isWeaponLoaded) {
            list.add("Przeładowanie")
        }

        list
    }

    var activeAbility by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(availableAbilities) {
        if (activeAbility != null && !availableAbilities.contains(activeAbility)) {
            activeAbility = null
        }
    }

    var isEnemySelectionOpen by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf((context as? ComponentActivity)?.intent?.getIntExtra("INITIAL_TAB", 0) ?: 0) }
    val isMedicOnly = remember { (context as? ComponentActivity)?.intent?.getBooleanExtra("MEDIC_ONLY", false) ?: false }

    // Multi-enemy State
    val enemies = remember { mutableStateListOf<Enemy>() }

    var totalExpGained by remember { mutableIntStateOf(0) }
    var playerDoublesCount by remember { mutableIntStateOf(0) }
    var playerWoundsTaken by remember { mutableIntStateOf(0) }

    var enemyCountToSpawn by remember { mutableFloatStateOf(1f) }
    var lastSpawnedCount by remember { mutableIntStateOf(1) }

    var rollInitiative: () -> Unit by remember { mutableStateOf({ }) }

    val setupEnemies = { template: EnemyTemplate, count: Int ->
        enemies.clear()
        enemyAdvantage.clear()
        totalExpGained = 0
        playerDoublesCount = 0
        playerWoundsTaken = 0
        lastSpawnedCount = count
        for (i in 1..count) {
            val body = BodyParts(template.w, template.w)
            val generatedWeapon = when (template.enemyClass) {
                "Wojownik" -> listOf("Miecz", "Topór", "Młot bojowy", "Tasak", "Szabla", "Buława", "Nadziak").random()
                "Łowca" -> listOf("Łuk", "Kusza", "Długi łuk").random()
                "Łotr" -> listOf("Sztylet", "Miecz krótki", "Kastet", "Rapier").random()
                else -> "Broń"
            }
            val generatedType = when {
                generatedWeapon.contains("Miecz") || generatedWeapon.contains("Topór") || 
                generatedWeapon.contains("Tasak") || generatedWeapon.contains("Halabarda") ||
                generatedWeapon.contains("Szabla") -> DamageType.SIECZNA

                generatedWeapon.contains("Młot") || generatedWeapon.contains("Kastet") || 
                generatedWeapon.contains("Pałka") || generatedWeapon.contains("Buława") || 
                generatedWeapon.contains("Cep") -> DamageType.OBUCHOWA

                generatedWeapon.contains("Sztylet") || generatedWeapon.contains("Łuk") || 
                generatedWeapon.contains("Kusza") || generatedWeapon.contains("Włócznia") || 
                generatedWeapon.contains("Nadziak") || generatedWeapon.contains("Rapier") || 
                generatedWeapon.contains("Pistolet") -> DamageType.KLUTA

                else -> DamageType.SIECZNA
            }
            enemies.add(
                Enemy(
                    id = i,
                    name = "${template.name} $i",
                    bodyParts = body,
                    ww = template.ww,
                    s = template.s,
                    toughness = template.t,
                    agility = template.i,
                    ap = template.armor,
                    weaponDmg = template.weaponDmg,
                    weaponName = generatedWeapon,
                    damageType = generatedType,
                    exp = template.exp,
                    traits = template.traits,
                    distance = if (template.traits.contains(EnemyTrait.STRZELEC)) 15 else 10
                )
            )
            if (template.traits.contains(EnemyTrait.SZYBKI)) {
                enemyAdvantage[i] = 1
            }
        }
        isEnemySelectionOpen = false
        rollInitiative() // Automatyczna inicjatywa po wyborze wrogów
    }

    val trainingLevel = remember(heroData) {
        val line = heroLines.find { it.contains("Tresura pomocnika", ignoreCase = true) }
        when {
            line == null || line.contains("Brak", ignoreCase = true) -> 0
            line.contains("Uczeń", ignoreCase = true) -> 1
            line.contains("Adept", ignoreCase = true) -> 2
            line.contains("Ekspert", ignoreCase = true) -> 3
            line.contains("Mistrz", ignoreCase = true) -> 4
            line.contains("Arcymistrz", ignoreCase = true) -> 5
            else -> 0
        }
    }

    // Allies State (np. Pies/Kot)
    val allies = remember {
        mutableStateListOf<Ally>().apply {
            playerInventory.forEach { item ->
                when {
                    item.contains("Pies", true) -> {
                        val dogBody = BodyParts(12, 12)
                        add(Ally("Burek (Pies)", dogBody, 40, "1k4+3", 3, 35, 4, trainingLevel))
                    }

                    item.contains("Kot", true) -> {
                        val catBody = BodyParts(5, 5)
                        add(Ally("Mruczek (Kot)", catBody, 30, "1k2", 2, 45, 5, trainingLevel))
                    }

                    item.contains("Sokół", true) -> {
                        val birdBody = BodyParts(4, 4)
                        add(Ally("Grom (Sokół)", birdBody, 45, "1k3", 2, 50, 6, trainingLevel))
                    }
                }
            }
        }
    }

    var selectedEnemyId by remember { mutableIntStateOf(enemies.firstOrNull()?.id ?: -1) }

    val performReload: () -> Unit = {
        if (actionsLeft > 0 && !isWeaponLoaded && activeWeapon != null) {
            actionsLeft -= 1
            val actionsNeeded = activeWeapon!!.reloadActions
            val reloadPerAction = if (hasRapidReload) 2 else 1
            reloadProgress += reloadPerAction

            if (reloadProgress >= actionsNeeded) {
                isWeaponLoaded = true
                reloadProgress = 0
                combatLog.add(0, "🏹 Załadowano: ${activeWeapon!!.name}!")
            } else {
                combatLog.add(0, "⚙️ Przeładowywanie... ($reloadProgress/$actionsNeeded)")
            }

            if (actionsLeft <= 0 && !isCombatOver) {
                coroutineScope.launch {
                    delay(800)
                    allyTurn()
                    delay(500)
                    enemyTurn()
                }
            }
        }
    }

    LaunchedEffect(activeAbility) {
        if (activeAbility == "Przeładowanie") {
            performReload()
            activeAbility = null
        }
    }


    val addToHistory = { isDiceMode: Boolean, result: String ->
        val cyfryIconsA = arrayOf("𝟘", "𝟙", "𝟚", "𝟛", "𝟜", "𝟝", "𝟞", "𝟟", "𝟠", "𝟡")
        val toIconsA = { text: String ->
            text.map { if (it.isDigit()) cyfryIconsA[it - '0'] else it }.joinToString("")
        }
        val fileName = if (isDiceMode) "histKosci" else "histStat"
        val file = java.io.File(context.filesDir, fileName)
        val now = java.util.Calendar.getInstance()
        val timeStr = toIconsA(
            String.format(
                java.util.Locale.getDefault(),
                "%02d:%02d",
                now.get(java.util.Calendar.HOUR_OF_DAY),
                now.get(java.util.Calendar.MINUTE)
            )
        )
        val cleanResult = result.replace("\n", " ").replace("  ", " ")
        var entry = "$timeStr ჻ $cleanResult\n"

        val dateStr = toIconsA(
            String.format(
                java.util.Locale.getDefault(),
                "%02d.%02d.%d",
                now.get(java.util.Calendar.DAY_OF_MONTH),
                now.get(java.util.Calendar.MONTH) + 1,
                now.get(java.util.Calendar.YEAR)
            )
        )
        val dateHeader = "꧁ $dateStr ꧂"

        var needsHeader = false
        if (!file.exists() || file.length() == 0L) {
            needsHeader = true
        } else {
            try {
                val lastHeader = file.bufferedReader()
                    .useLines { it.lastOrNull { line -> line.startsWith("꧁") } }
                if (lastHeader == null || !lastHeader.contains(dateStr)) {
                    needsHeader = true
                }
            } catch (e: Exception) {
                needsHeader = true
            }
        }

        if (needsHeader) {
            entry = "$dateHeader\n$entry"
        }
        try {
            context.openFileOutput(fileName, android.content.Context.MODE_APPEND).use {
                it.write(entry.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    val getInitiative = { agility: Int, speed: Int, hasReflexes: Boolean ->
        (1..10).random() + (agility / 10) + (if (hasReflexes) 5 else 0)
    }

    rollInitiative = {
        val pInit = getInitiative(playerAgility, playerSpeed, hasLightningReflexes)
        combatLog.add(0, "⚔️ ‹2:TWOJA INICJATYWA:› $pInit")

        enemies.forEach { e ->
            e.initiative = getInitiative(e.agility, 4, false)
            combatLog.add(0, "👹 ${e.name} inicjatywa: ${e.initiative}")
        }

        allies.forEach { a ->
            a.initiative = getInitiative(a.agility, a.speed, false)
            combatLog.add(0, "🐾 ${a.name} inicjatywa: ${a.initiative}")
        }

        val maxEnemyInit = enemies.maxOfOrNull { it.initiative } ?: 0
        val maxAllyInit = allies.maxOfOrNull { it.initiative } ?: 0

        if (maxEnemyInit > pInit && maxEnemyInit > maxAllyInit) {
            combatLog.add(0, "⚠️ Wrogowie zaczynają pierwsi!")
            coroutineScope.launch {
                delay(800)
                enemyTurn()
            }
        } else {
            combatLog.add(0, "🗡️ Twoja strona zaczyna!")
        }
    }

    val parseDice = { dice: String ->
        try {
            if (!dice.contains("k")) dice.toIntOrNull() ?: 0
            else {
                val parts = dice.split("k")
                val count = parts[0].trim().toIntOrNull() ?: 1
                val sides = parts[1].substringBefore("+").substringBefore("-").trim().toIntOrNull() ?: 6
                val bonus = if (dice.contains("+")) dice.substringAfter("+").trim().toIntOrNull() ?: 0
                else if (dice.contains("-")) -(dice.substringAfter("-").trim().toIntOrNull() ?: 0)
                else 0

                var total = 0
                repeat(count) { total += (1..sides).random() }
                total + bonus
            }
        } catch (e: Exception) {
            0
        }
    }

    val applyInjuryToPlayer = { location: String?, criticalValue: Int ->
        val locGroup = when {
            location?.contains("Głowa", true) == true -> "Głowa"
            location?.contains("Korpus", true) == true -> "Korpus"
            location?.contains("Ręka", true) == true -> "Ręka"
            location?.contains("Noga", true) == true -> "Noga"
            else -> "Korpus"
        }

        val filtered = criticalInjuriesTable.filter { it.location == locGroup }

        val toughnessRank = getSkillRank("Odporność")
        val roll = maxOf(1, (1..10).random() + criticalValue - toughnessRank)
        val injury =
            filtered.minByOrNull { if (it.severity >= roll) 0 else Math.abs(it.severity - roll) }
                ?: filtered.random()

        playerInjuries.add(injury.name)
        val currentHeroId =
            (context as? ComponentActivity)?.intent?.getIntExtra("SELECTED_HERO_ID", -1)
        val suffix = if (currentHeroId != -1) "_$currentHeroId" else ""
        saveToFile("game_injuries$suffix", playerInjuries.joinToString(";"))

        val logEntry =
            "🩸 ‹88:RANA KRYTYCZNA [${injury.location}]:› ${injury.name}! ${injury.description}"
        combatLog.add(0, logEntry)
        addToHistory(false, "WALKA: $logEntry")
        playSoundAction(R.raw.darcie, 0)

        if (injury.isFatal) {
            playerBodyParts.torso = -10
            checkCombatEnd()
        }

        if (injury.stat == "PŻ") {
            val doubledPenalty = injury.penalty * 2
            playerBodyParts.torso = maxOf(-10, playerBodyParts.torso + doubledPenalty)
        }
    }

    val applyInjuryToEnemy = { enemy: Enemy, location: String?, criticalValue: Int ->
        val locGroup = when {
            location?.contains("Głowa", true) == true -> "Głowa"
            location?.contains("Korpus", true) == true -> "Korpus"
            location?.contains("Ręka", true) == true -> "Ręka"
            location?.contains("Noga", true) == true -> "Noga"
            else -> "Korpus"
        }

        val filtered = criticalInjuriesTable.filter { it.location == locGroup }

        val roll = (1..10).random() + criticalValue
        val injury =
            filtered.minByOrNull { if (it.severity >= roll) 0 else Math.abs(it.severity - roll) }
                ?: filtered.random()

        enemy.injuries.add(injury.name)

        when (injury.stat) {
            "Walka Wręcz" -> enemy.ww += injury.penalty
            "Zręczność" -> enemy.agility += injury.penalty
            "Siła" -> enemy.weaponDmg = maxOf(0, enemy.weaponDmg + (injury.penalty / 10))
            "Wytrzymałość" -> enemy.toughness = maxOf(0, enemy.toughness + injury.penalty)
            "Odporność" -> enemy.toughness = maxOf(0, enemy.toughness + injury.penalty)
        }

        val logEntry =
            "🩸 ‹88:RANA KRYTYCZNA [${enemy.name}]:› ${injury.name}! ${injury.description}"
        combatLog.add(0, logEntry)
        addToHistory(false, "WALKA: $logEntry")

        // Trigger reactivity for enemy property changes
        val idx = enemies.indexOf(enemy)
        if (idx != -1) enemies[idx] = enemy

        if (injury.isFatal) {
            enemy.bodyParts.torso = -10
            checkCombatEnd()
        }
    }

    val damageArmorAt = { location: String ->
        val currentHeroId = (context as? ComponentActivity)?.intent?.getIntExtra("SELECTED_HERO_ID", -1)
        val suffix = if (currentHeroId != -1) "_$currentHeroId" else ""

        val oldEq = playerEquipment.toList()
        val newEq = HeroLogic.damageArmorAt(location, oldEq) { updatedEq ->
            playerEquipment.clear()
            playerEquipment.addAll(updatedEq)
            saveToFile("game_equipment$suffix", updatedEq.joinToString(";"))
            combatLog.add(0, "🛡️ ‹88:USZKODZENIE PANCERZA!› Twoja ochrona na ‹2:$location› została osłabiona!")
            playSoundAction(R.raw.broken, 0)
        }
    }

    // Helper to save rewards to hero file (PD, Coins, and HP)
    val saveToHeroFile = { addedPd: Int ->
        val currentHeroId =
            (context as? ComponentActivity)?.intent?.getIntExtra("SELECTED_HERO_ID", -1)
        if (currentHeroId != -1) {
            val hFile = "hero_$currentHeroId"
            val currentContent = readFromFile(hFile)
            val hpLine = "LOKACJE_HP: ${playerBodyParts.head},${playerBodyParts.torso},${playerBodyParts.rightArm},${playerBodyParts.leftArm},${playerBodyParts.rightLeg},${playerBodyParts.leftLeg}"
            
            var hpFound = false
            val lines = currentContent.lines()
            val updatedContent = lines.map { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("PD:") -> {
                        val currentPd = line.substringAfter("PD:").trim().toIntOrNull() ?: 0
                        "PD: ${currentPd + addedPd}"
                    }
                    line.contains("⛀") && line.contains("Złota") -> line.substringBeforeLast("  ") + "  " + playerGold
                    line.contains("⛀") && line.contains("Srebrny") -> line.substringBeforeLast("  ") + "  " + playerSilver
                    line.contains("⛀") && line.contains("Miedziany") -> line.substringBeforeLast("  ") + "  " + playerCopper
                    trimmed.startsWith("LOKACJE_HP:") -> {
                        hpFound = true
                        hpLine
                    }
                    else -> line
                }
            }.toMutableList()

            if (!hpFound) {
                // If LOKACJE_HP not found, try to insert it after PD:
                val pdIdx = updatedContent.indexOfFirst { it.trim().startsWith("PD:") }
                if (pdIdx != -1) {
                    updatedContent.add(pdIdx + 1, hpLine)
                } else {
                    updatedContent.add(hpLine)
                }
            }

            saveToFile(hFile, updatedContent.joinToString("\n"))
        }
    }

    val saveQuickSave: (Boolean) -> Unit = { showToast ->
        try {
            val root = JSONObject()

            // Player data
            val pObj = JSONObject()
            val pBp = JSONObject()
            pBp.put("head", playerBodyParts.head)
            pBp.put("torso", playerBodyParts.torso)
            pBp.put("rArm", playerBodyParts.rightArm)
            pBp.put("lArm", playerBodyParts.leftArm)
            pBp.put("rLeg", playerBodyParts.rightLeg)
            pBp.put("lLeg", playerBodyParts.leftLeg)
            pObj.put("bodyParts", pBp)
            pObj.put("advantage", playerAdvantage)
            pObj.put("momentum", playerMomentum)
            pObj.put("luck", luckPoints)
            pObj.put("actionsLeft", actionsLeft)
            pObj.put("isLoaded", isWeaponLoaded)
            pObj.put("reloadProgress", reloadProgress)
            root.put("player", pObj)

            // Enemies
            val enArray = JSONArray()
            enemies.forEach { en ->
                val eObj = JSONObject()
                eObj.put("id", en.id)
                eObj.put("name", en.name)
                val eBp = JSONObject()
                eBp.put("head", en.bodyParts.head)
                eBp.put("torso", en.bodyParts.torso)
                eBp.put("rArm", en.bodyParts.rightArm)
                eBp.put("lArm", en.bodyParts.leftArm)
                eBp.put("rLeg", en.bodyParts.rightLeg)
                eBp.put("lLeg", en.bodyParts.leftLeg)
                eBp.put("maxH", en.bodyParts.maxHead)
                eBp.put("maxT", en.bodyParts.maxTorso)
                eObj.put("bodyParts", eBp)
                eObj.put("ww", en.ww)
                eObj.put("s", en.s)
                eObj.put("t", en.toughness)
                eObj.put("ag", en.agility)
                eObj.put("ap", en.ap)
                eObj.put("wDmg", en.weaponDmg)
                eObj.put("wName", en.weaponName)
                eObj.put("dmgType", en.damageType.name)
                eObj.put("exp", en.exp)
                eObj.put("poisoned", en.isPoisoned)
                eObj.put("blinded", en.isBlinded)
                eObj.put("dist", en.distance)
                eObj.put("initiative", en.initiative)
                eObj.put("loaded", en.isLoaded)
                val injArr = JSONArray()
                en.injuries.forEach { injArr.put(it) }
                eObj.put("injuries", injArr)

                val traitsArr = JSONArray()
                en.traits.forEach { traitsArr.put(it.name) }
                eObj.put("traits", traitsArr)

                enArray.put(eObj)
            }
            root.put("enemies", enArray)

            // Allies
            val alArray = JSONArray()
            allies.forEach { al ->
                val aObj = JSONObject()
                aObj.put("name", al.name)
                val aBp = JSONObject()
                aBp.put("head", al.bodyParts.head)
                aBp.put("torso", al.bodyParts.torso)
                aBp.put("rArm", al.bodyParts.rightArm)
                aBp.put("lArm", al.bodyParts.leftArm)
                aBp.put("rLeg", al.bodyParts.rightLeg)
                aBp.put("lLeg", al.bodyParts.leftLeg)
                aObj.put("bodyParts", aBp)
                aObj.put("ww", al.ww)
                aObj.put("dmg", al.damage)
                aObj.put("t", al.toughness)
                aObj.put("ag", al.agility)
                aObj.put("sp", al.speed)
                aObj.put("tl", al.trainingLevel)
                aObj.put("fled", al.isFled)
                aObj.put("acted", al.hasActed)
                aObj.put("initiative", al.initiative)
                alArray.put(aObj)
            }
            root.put("allies", alArray)

            // Combat Log
            val logArray = JSONArray()
            combatLog.forEach { logArray.put(it) }
            root.put("log", logArray)

            // Enemy Advantage Map
            val eaObj = JSONObject()
            enemyAdvantage.forEach { (k, v) -> eaObj.put(k.toString(), v) }
            root.put("enemyAdvantage", eaObj)

            saveToFile("combat_quicksave$suffix.json", root.toString())
            if (showToast) Toast.makeText(context, "Gra zapisana!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            if (showToast) Toast.makeText(context, "Błąd zapisu!", Toast.LENGTH_SHORT).show()
        }
    }

    // Rejestracja automatycznego zapisu po zdefiniowaniu funkcji
    LaunchedEffect(Unit) {
        onSaveTriggerCreated {
            if (!isCombatOver) {
                saveQuickSave(false)
            }
        }
    }


    val loadQuickSave = {
        try {
            val data = readFromFile("combat_quicksave$suffix.json")
            if (data.isNotEmpty()) {
                val root = JSONObject(data)

                // Player
                val pObj = root.getJSONObject("player")
                val pBp = pObj.getJSONObject("bodyParts")
                playerBodyParts.head = pBp.getInt("head")
                playerBodyParts.torso = pBp.getInt("torso")
                playerBodyParts.rightArm = pBp.getInt("rArm")
                playerBodyParts.leftArm = pBp.getInt("lArm")
                playerBodyParts.rightLeg = pBp.getInt("rLeg")
                playerBodyParts.leftLeg = pBp.getInt("lLeg")
                playerAdvantage = pObj.getInt("advantage")
                playerMomentum = pObj.getInt("momentum")
                luckPoints = pObj.getInt("luck")
                actionsLeft = pObj.getInt("actionsLeft")
                isWeaponLoaded = pObj.getBoolean("isLoaded")
                reloadProgress = pObj.getInt("reloadProgress")

                // Enemies
                enemies.clear()
                val enArray = root.getJSONArray("enemies")
                for (i in 0 until enArray.length()) {
                    val eObj = enArray.getJSONObject(i)
                    val eBpObj = eObj.getJSONObject("bodyParts")
                    val bp = BodyParts(
                        eBpObj.getInt("head"), eBpObj.getInt("torso"),
                        eBpObj.getInt("rArm"), eBpObj.getInt("lArm"),
                        eBpObj.getInt("rLeg"), eBpObj.getInt("lLeg"),
                        eBpObj.optInt("maxH", 12), eBpObj.optInt("maxT", 18),
                        eBpObj.optInt("maxH", 12), eBpObj.optInt("maxH", 12),
                        eBpObj.optInt("maxH", 12), eBpObj.optInt("maxH", 12)
                    )

                    val traits = mutableListOf<EnemyTrait>()
                    val traitsArr = eObj.optJSONArray("traits")
                    if (traitsArr != null) {
                        for (j in 0 until traitsArr.length()) {
                            try {
                                traits.add(EnemyTrait.valueOf(traitsArr.getString(j)))
                            } catch (e: Exception) {
                            }
                        }
                    }

                    val enemy = Enemy(
                        eObj.getInt("id"),
                        eObj.getString("name"),
                        bp,
                        eObj.getInt("ww"),
                        eObj.getInt("s"),
                        eObj.getInt("t"),
                        eObj.getInt("ag"),
                        eObj.getInt("ap"),
                        eObj.getInt("wDmg"),
                        eObj.getString("wName"),
                        DamageType.valueOf(eObj.getString("dmgType")),
                        eObj.getInt("exp"),
                        traits
                    )
                    enemy.isPoisoned = eObj.getBoolean("poisoned")
                    enemy.isBlinded = eObj.getBoolean("blinded")
                    enemy.distance = eObj.getInt("dist")
                    enemy.initiative = eObj.getInt("initiative")
                    enemy.isLoaded = eObj.optBoolean("loaded", true)

                    val injArr = eObj.getJSONArray("injuries")
                    for (j in 0 until injArr.length()) enemy.injuries.add(injArr.getString(j))

                    enemies.add(enemy)
                }

                // Allies
                allies.clear()
                val alArray = root.getJSONArray("allies")
                for (i in 0 until alArray.length()) {
                    val aObj = alArray.getJSONObject(i)
                    val aBpObj = aObj.getJSONObject("bodyParts")
                    val bp = BodyParts(
                        aBpObj.getInt("head"), aBpObj.getInt("torso"),
                        aBpObj.getInt("rArm"), aBpObj.getInt("lArm"),
                        aBpObj.getInt("rLeg"), aBpObj.getInt("lLeg"),
                        12, 12, 12, 12, 12, 12 // Default max for allies
                    )
                    val ally = Ally(
                        aObj.getString("name"), bp, aObj.getInt("ww"),
                        aObj.getString("dmg"), aObj.getInt("t"),
                        aObj.getInt("ag"), aObj.getInt("sp"),
                        aObj.getInt("tl")
                    )
                    ally.isFled = aObj.getBoolean("fled")
                    ally.hasActed = aObj.getBoolean("acted")
                    ally.initiative = aObj.getInt("initiative")
                    allies.add(ally)
                }

                // Log
                combatLog.clear()
                val logArray = root.getJSONArray("log")
                for (i in 0 until logArray.length()) combatLog.add(logArray.getString(i))

                // Enemy Advantage
                enemyAdvantage.clear()
                val eaObj = root.getJSONObject("enemyAdvantage")
                eaObj.keys().forEach { key ->
                    enemyAdvantage[key.toInt()] = eaObj.getInt(key)
                }

                isEnemySelectionOpen = false
                Toast.makeText(context, "Wczytano zapis!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Błąd wczytywania!", Toast.LENGTH_SHORT).show()
        }
    }

    var showQuickSaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val quickSaveFile = java.io.File(context.filesDir, "combat_quicksave$suffix.json")
        if (quickSaveFile.exists()) {
            loadQuickSave()
        }
    }

    if (showQuickSaveDialog) {
        AlertDialog(
            onDismissRequest = { showQuickSaveDialog = false },
            title = { Text("Kontynuować walkę?") },
            text = { Text("Wykryto przerwany zapis walki. Czy chcesz go wczytać?") },
            confirmButton = {
                Button(onClick = {
                    loadQuickSave()
                    showQuickSaveDialog = false
                }) { Text("TAK") }
            },
            dismissButton = {
                Button(onClick = {
                    java.io.File(context.filesDir, "combat_quicksave$suffix.json").delete()
                    showQuickSaveDialog = false
                }) { Text("NIE (usuń)") }
            },
            containerColor = Color(0xFF222222),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }

    checkCombatEnd = {
        val currentHeroId =
            (context as? ComponentActivity)?.intent?.getIntExtra("SELECTED_HERO_ID", -1)
        val suffix = if (currentHeroId != -1) "_$currentHeroId" else ""

        val isFatalInjury =
            playerInjuries.any { name -> criticalInjuriesTable.find { it.name == name }?.isFatal == true }

        if (playerBodyParts.torso < 0 || playerBodyParts.head < 0 || isFatalInjury) {
            isCombatOver = true
            isGameOverAnimation = true
            val logEntry = "💀 ‹88:PORAŻKA›... Twoja krew wsiąka w ziemię."
            combatLog.add(0, logEntry)
            addToHistory(false, "WALKA: $logEntry")
            playSoundAction(R.raw.darcie, 0)

            // Usuń quicksave przy porażce
            java.io.File(context.filesDir, "combat_quicksave$suffix.json").delete()
        } else if (enemies.isEmpty()) {
            isCombatOver = true

            // NOWA LOGIKA NAGRÓD
            val baseCopper = (11..19).random() * lastSpawnedCount + 20
            val doublesBonusCopper = playerDoublesCount * 10
            val copperLoot = baseCopper + doublesBonusCopper
            playerCopper += copperLoot

            val baseExp = (5..9).random() * lastSpawnedCount
            val woundsBonusExp = playerWoundsTaken
            val doublesBonusExp = playerDoublesCount * 10
            val totalVictoryExp = baseExp + woundsBonusExp + doublesBonusExp

            // Dodanie PD, monet i HP do pliku bohatera
            saveToHeroFile(totalVictoryExp)

            val logEntry =
                "🏆 ‹2:ZWYCIĘSTWO!› Wrogowie pokonani. Zyskujesz $totalVictoryExp PD oraz $copperLoot mp!"
            combatLog.add(0, logEntry)
            if (playerDoublesCount > 0) combatLog.add(
                0,
                "✨ Bonus za dublety (x$playerDoublesCount): +${playerDoublesCount * 10} PD i mp!"
            )
            addToHistory(false, "WALKA: $logEntry")
            playSoundAction(R.raw.klik1, 0)

            // Usuń quicksave przy zwycięstwie
            val currentHeroId = (context as? ComponentActivity)?.intent?.getIntExtra("SELECTED_HERO_ID", -1)
            val suffix = if (currentHeroId != -1) "_$currentHeroId" else ""
            java.io.File(context.filesDir, "combat_quicksave$suffix.json").delete()

            // Zapisz rany (HP jest już w saveToHeroFile)
            saveToFile("game_injuries$suffix", playerInjuries.joinToString(";"))
        }
    }


    allyTurn = {
        allies.forEach { ally ->
            if (ally.isFled) return@forEach

            // Sprawdzenie ucieczki na początku tury zależne od tresury
            val fleeChance = when (ally.trainingLevel) {
                0 -> 85
                1 -> 60
                2 -> 35
                3 -> 10
                else -> 0
            }

            if (fleeChance > 0 && (1..100).random() <= fleeChance) {
                ally.isFled = true
                combatLog.add(0, "🐾 ${ally.name} przestraszył się i UCIEKA z pola walki!")
                return@forEach
            }

            if (enemies.isNotEmpty()) {
                val target = enemies.random()

                // 1. ATTACKER (Ally) - Tresura zwiększa szansę na trafienie (+5% na poziom)
                val effectiveWW = ally.ww + (ally.trainingLevel * 5)
                val roll = (1..100).random()
                val success = roll <= effectiveWW

                if (success) {
                    val hitLocationRoll = reverseRoll(roll)
                    val location = getHitLocation(hitLocationRoll)

                    // Tresura zwiększa obrażenia (+1 na poziom powyżej 1)
                    val trainingDmgBonus = if (ally.trainingLevel > 1) ally.trainingLevel - 1 else 0
                    val dmgBase = parseDice(ally.damage) + trainingDmgBonus
                    val armorVal =
                        target.getProt(target.damageType) // Używamy damageType celu dla uproszczenia lub SIECZNA domyślnie

                    // --- Rangi Odporności dla Przeciwników (Uproszczone) ---
                    var enemyResistanceRank = 0
                    val enemyElementalType =
                        DamageType.NONE // Sojusznicy rzadko mają broń żywiołową

                    val finalDmg =
                        maxOf(1, dmgBase - (target.toughness + armorVal + enemyResistanceRank))

                    val currentHp = target.bodyParts.applyDamage(location, finalDmg)

                    val actionVerb = when {
                        ally.name.contains("Sokół", true) -> {
                            playSoundAction(R.raw.voicewoman, 0) // Prowizoryczny dźwięk sokoła
                            "atakuje szponami"
                        }

                        ally.name.contains("Kot", true) -> {
                            playSoundAction(R.raw.voicewoman, 0) // Prowizoryczny dźwięk kota
                            "drapie"
                        }

                        else -> {
                            playSoundAction(R.raw.voiceman, 0) // Prowizoryczny dźwięk psa
                            "gryzie"
                        }
                    }

                    val logEntry =
                        "🐾 ${ally.name} $actionVerb 〘$roll〙 ‹88:${target.name}› w $location za $finalDmg! (PŻ: $currentHp)"
                    combatLog.add(0, logEntry)

                    // Specjalna mechanika psa: Chwyt (wymaga min. 1 poziomu tresury)
                    if (ally.name.contains(
                            "Pies",
                            true
                        ) && ally.trainingLevel >= 1 && !target.isDead
                    ) {
                        target.isGrabbedBy = ally.name
                        combatLog.add(
                            0,
                            "🐕 ${ally.name} CHWYCIŁ przeciwnika! ${target.name} jest unieruchomiony."
                        )
                    }

                    // Specjalna mechanika sokoła: Oślepienie (wymaga min. 2 poziomu tresury)
                    if (ally.name.contains(
                            "Sokół",
                            true
                        ) && ally.trainingLevel >= 2 && location == "Głowa" && !target.isDead
                    ) {
                        target.isBlinded = true
                        combatLog.add(
                            0,
                            "🦅 ${ally.name} WYKOLEBAŁ oczy przeciwnikowi! ${target.name} jest oślepiony."
                        )
                    }

                    if (target.isDead) {
                        combatLog.add(0, "💀 ‹2:${target.name} pada martwy!›")
                        totalExpGained += target.exp
                        enemies.remove(target)
                        checkCombatEnd()
                    }

                    // Trigger reactivity for ally and target property changes
                    val allyIdx = allies.indexOf(ally)
                    if (allyIdx != -1) allies[allyIdx] = ally
                    val targetIdx = enemies.indexOf(target)
                    if (targetIdx != -1) enemies[targetIdx] = target
                } else {
                    combatLog.add(0, "🐾 ${ally.name} próbuje zaatakować, ale pudłuje 〘$roll〙.")
                }
            }
        }
    }

    enemyTurn = {
        if (!isCombatOver) {
            combatLog.add(0, "👹 ‹88:TURA PRZECIWNIKÓW›")

            val currentHeroId =
                (context as? ComponentActivity)?.intent?.getIntExtra("SELECTED_HERO_ID", -1)
            val suffix = if (currentHeroId != -1) "_$currentHeroId" else ""

            enemies.toList().forEach { enemy ->
                if (enemy.isDead) {
                    combatLog.add(0, "💀 ‹2:${enemy.name} umiera od efektów statusu!›")
                    totalExpGained += enemy.exp
                    enemies.remove(enemy)
                    checkCombatEnd()
                    return@forEach
                }

                // Status effects tick for enemy
                if (enemy.isPoisoned) {
                    val resBonus = if (enemy.traits.contains(EnemyTrait.TWARDY)) 1 else 0
                    val poisonDmg = maxOf(0, 1 - resBonus)
                    if (poisonDmg > 0) {
                        enemy.bodyParts.torso -= poisonDmg
                        combatLog.add(0, "🤢 Trucizna zadaje ${enemy.name} $poisonDmg obrażeń.")
                    }
                    if ((1..100).random() <= (15 + resBonus * 10)) {
                        enemy.isPoisoned = false
                        combatLog.add(0, "✨ ${enemy.name} zwalcza truciznę.")
                    }
                }
                if (enemy.isBurning) {
                    val burnDmg = (1..3).random()
                    enemy.bodyParts.torso -= burnDmg
                    combatLog.add(0, "🔥 ${enemy.name} płonie! (-$burnDmg PŻ)")
                    if ((1..100).random() <= 20) {
                        enemy.isBurning = false
                        combatLog.add(0, "💧 Płomienie na ${enemy.name} gasną.")
                    }
                }
                if (enemy.isShocked && (1..100).random() <= 50) {
                    enemy.isShocked = false
                    combatLog.add(0, "⚡ ${enemy.name} otrząsa się z porażenia.")
                }
                if (enemy.isFrozen && (1..100).random() <= 30) {
                    enemy.isFrozen = false
                    combatLog.add(0, "❄️ ${enemy.name} już nie jest zamrożony.")
                }

                // Apply Traits: Regeneration
                val torsoHp = enemy.bodyParts.torso
                if (enemy.traits.contains(EnemyTrait.REGENERACJA) && torsoHp < enemy.bodyParts.maxTorso) {
                    enemy.bodyParts.torso = minOf(enemy.bodyParts.maxTorso, torsoHp + 2)
                    combatLog.add(0, "✨ ${enemy.name} regeneruje korpus! (+2 PŻ)")
                }

                // Trigger reactivity for enemy property changes (status, HP, distance)
                val enemyIdx = enemies.indexOf(enemy)
                if (enemyIdx != -1) enemies[enemyIdx] = enemy

                if (playerBodyParts.torso > 0) {
                    // Enemy Movement Logic
                    val isEnemyRanged = enemy.traits.contains(EnemyTrait.STRZELEC)

                    if (!isEnemyRanged && enemy.distance > 0) {
                        val oldDist = enemy.distance
                        enemy.distance = maxOf(
                            0,
                            enemy.distance - (enemy.agility / 10 + 2)
                        ) // Uproszczony ruch wroga
                        combatLog.add(
                            0,
                            "👣 ${enemy.name} zbliża się! ($oldDist m -> ${enemy.distance} m)"
                        )
                        return@forEach // Zużywa turę na ruch jeśli nie jest strzelcem i jest daleko
                    }

                    if (isEnemyRanged && enemy.distance == 0) {
                        enemy.distance = 4 // Odskakuje
                        combatLog.add(0, "🏃 ${enemy.name} odskakuje, by móc strzelać!")
                    }

                    // 1. ATTACKER (Enemy)
                    var enemyEffectiveWW = enemy.ww + (minOf(
                        5,
                        enemyAdvantage[enemy.id] ?: 0
                    )) * 10 + (if (enemies.size > 1) 10 else 0)

                    // Kary za efekty statusu
                    if (enemy.isBlinded) enemyEffectiveWW -= 25
                    if (enemy.isShocked) enemyEffectiveWW -= 20 // Status Ogłuszony (Shocked/Stunned)
                    if (enemy.isGrabbedBy != null) enemyEffectiveWW -= 10
                    if (enemy.isFrozen) enemyEffectiveWW -= 15

                    val enemyRoll = Random.nextInt(1, 101)
                    val enemySL = calculateSL(enemyRoll, enemyEffectiveWW)
                    val enemySuccess = enemyRoll <= enemyEffectiveWW

                    // --- NOWA LOGIKA OBRAŻEŃ PRZECIWNIKA (Potężny cios / Druzgocący) ---
                    val enemyTraits = enemy.traits
                    val enemyIsCrushing = enemyTraits.contains(EnemyTrait.DRUZGOCACY) || enemy.weaponName.contains("Druzgocący", true)
                    val enemyIsMighty = enemyTraits.contains(EnemyTrait.POTEZNY_CIOS) || enemy.weaponName.contains("Potężny cios", true)

                    // Przeciwnicy zazwyczaj mają stałe obrażenia + kość (standard: 1k10)
                    val enemyDieRoll = rollDamageDie("1k10", enemyIsCrushing, enemyIsMighty, combatLog)
                        // -----------------------------------------------------------------

                        // Przeciwnik może próbować się wyrwać z chwytu (test Siły/WW vs 50%)
                    if (enemy.isGrabbedBy != null && enemySuccess) {
                        if ((1..100).random() <= 30) {
                            combatLog.add(
                                0,
                                "🦾 ${enemy.name} wyrywa się z uścisku ${enemy.isGrabbedBy}!"
                            )
                            enemy.isGrabbedBy = null
                        } else {
                            combatLog.add(0, "⛓️ ${enemy.name} nie może się poruszyć przez chwyt!")
                        }
                    }

                    // 2. DEFENDER (Player)
                    val weaponSkillName = getWeaponSkillName(activeWeapon)
                    val rankBonus = getRankBonus(weaponSkillName)
                    val weaponBonus = activeWeapon?.bonus ?: 0

                    // Defense: Choice between Parry (WW) and Dodge (Agility)
                    val canDodge = dodgeRank > 0 && dodgesUsed < dodgeRank

                    // Parrying conditions:
                    // 1. Defensive weapon (like Lewak)
                    // 2. Shield in inventory
                    // 3. Dual Wielding (weapon in left hand)
                    val hasDefensiveWeapon =
                        activeWeapon?.traits?.contains("Defensywny", true) == true
                    val hasShield =
                        playerEquipment.any { it.contains("Tarcza", true) && it.contains("🅔") }
                    val hasOffhandWeapon =
                        playerEquipment.filter { it.contains("🅔") }.size >= 2 // Simplified check

                    val canParry =
                        parryRank > 0 && parriesUsed < parryRank && (hasDefensiveWeapon || hasShield || hasOffhandWeapon)

                    val playerParrySkill = if (canParry) {
                        var skill = if (hasOffhandWeapon && !hasDefensiveWeapon && !hasShield) {
                            // Using dual wield skill for parrying
                            val dwBonus = when (dualWieldRank) {
                                0 -> -15
                                1 -> 0
                                2 -> 5
                                3 -> 10
                                4 -> 15
                                5 -> 20
                                else -> -15
                            }
                            playerWW + dwBonus
                        } else {
                            playerWW + rankBonus + weaponBonus + (activeWeapon?.size?.hitModifier ?: 0)
                        }
                        // Bonus z prefiksu broni
                        skill += activeWeapon?.prefix?.wwBonus ?: 0
                        skill
                    } else -100 // Cannot parry

                    val playerDodgeSkill = if (canDodge) {
                        var skill = playerDodge
                        // Bonus z prefiksu broni
                        skill += activeWeapon?.prefix?.dodgeBonus ?: 0
                        skill
                    } else -100 // Cannot dodge

                    val playerDefenseSkill = maxOf(playerParrySkill, playerDodgeSkill)
                    val usedDodge = playerDodgeSkill >= playerParrySkill && canDodge
                    val usedParry = !usedDodge && canParry

                    var playerEffectiveWW =
                        if (playerDefenseSkill > -50) playerDefenseSkill + (minOf(
                            5,
                            playerAdvantage
                        ) * 10) else 0

                    // Kary za efekty statusu gracza z uwzględnieniem redukcji przez rangi
                    val dodgeSkillRank = getSkillRank("Uniki")
                    if (playerIsBlinded) playerEffectiveWW -= maxOf(5, 25 - dodgeSkillRank * 4)
                    if (playerIsShocked) playerEffectiveWW -= maxOf(5, 20 - dodgeSkillRank * 2) // Status Ogłuszony
                    if (playerIsFrozen) playerEffectiveWW -= maxOf(5, 15 - dodgeSkillRank * 2)
                    if (playerIsPoisoned) playerEffectiveWW -= 5

                    // --- Redukcja Kar do Obrony przez Rangi Zwinności (Opcjonalne, tu: Uniki) ---
                    if (usedDodge && playerDodgeSkill > -50) {
                        val dodgeSkillRank = getSkillRank("Uniki")
                        // Wyższe rangi mogą redukować kary z otoczenia lub pancerza, jeśli takie zostaną dodane
                    }

                    if (activeAbility == "Postawa obronna") {
                        playerEffectiveWW += 20
                        combatLog.add(0, "🛡️ Przyjmujesz ‹20:Postawę obronną› (+20 WW do obrony)")
                    } else if (activeAbility == "Riposta") {
                        playerEffectiveWW += 10
                        combatLog.add(0, "⚔️ Przygotowujesz ‹20:Ripostę› (+10 WW do obrony)")
                    }

                    // Apply Traits: Fear (PRZERAZAJACY) - affects player tests
                    if (enemies.any { it.traits.contains(EnemyTrait.PRZERAZAJACY) }) {
                        playerEffectiveWW -= 20
                    }

                    val playerRoll = Random.nextInt(1, 101)
                    val playerSL = calculateSL(playerRoll, playerEffectiveWW)
                    val playerSuccess = playerRoll <= playerEffectiveWW && playerDefenseSkill > -50

                    // 3. OPPOSED TEST
                    val netSL = enemySL - playerSL
                    val enemyWins = (netSL > 0 || (netSL == 0 && enemySL > playerSL)) || playerDefenseSkill <= -50

                    if (enemyWins && enemySuccess) {
                        val hitLocationRoll = reverseRoll(enemyRoll)
                        val location = getHitLocation(hitLocationRoll)
                        
                        // Riposta logic - if player failed to defend but had Riposta active
                        // (Wait, Riposta usually triggers on SUCCESSFUL defense, but here we are in enemyWins branch)
                        // Actually, if player defended successfully, we check for Riposta below
                        
                        var enemyDmgBase = enemy.weaponDmg
                            if (enemy.traits.contains(EnemyTrait.FURIA) && enemy.bodyParts.torso < enemy.bodyParts.maxTorso / 2) {
                                enemyDmgBase += 2
                                combatLog.add(0, "🔥 ${enemy.name} wpada w furię!")
                            }

                            val armorVal = activeArmor.getProt(location, enemy.damageType)

                            // --- Rangi Odporności ---
                            val elementalType = when {
                                enemy.weaponName.contains("Ognia", true) -> DamageType.OGIEN
                                enemy.weaponName.contains("Lodu", true) -> DamageType.ZIMNO
                                enemy.weaponName.contains("Pioruna", true) -> DamageType.PIORUN
                                enemy.weaponName.contains("Trucizny", true) -> DamageType.TRUCIZNA
                                else -> DamageType.NONE
                            }

                            val resistanceSkillName = when (elementalType) {
                                DamageType.OGIEN -> "Odporność na poparzenia"
                                DamageType.ZIMNO -> "Odporność na chłód"
                                DamageType.PIORUN -> "Odporność na porażenia"
                                DamageType.TRUCIZNA -> "Odporność na trucizny"
                                else -> ""
                            }

                            var resistanceRank = 0
                            if (resistanceSkillName.isNotEmpty()) {
                                val skillLine = heroLines.find {
                                    it.contains(
                                        resistanceSkillName,
                                        ignoreCase = true
                                    )
                                }
                                if (skillLine != null) {
                                    resistanceRank = getRankWeight(skillLine)
                                }
                            }

                            // Obliczanie finalDmg z nową kością
                            val finalDmg =
                                if (resistanceRank >= 5 && elementalType != DamageType.NONE) {
                                    combatLog.add(0, "🛡️ Jesteś niewrażliwy na ten żywioł!")
                                    maxOf(0, enemyDmgBase + enemyDieRoll + netSL - (playerT + armorVal))
                                } else {
                                    val reduction =
                                        if (elementalType != DamageType.NONE) resistanceRank else 0
                                    if (reduction > 0) combatLog.add(
                                        0,
                                        "🛡️ Odporność ($resistanceSkillName) redukuje obrażenia o $reduction."
                                    )
                                    maxOf(1, enemyDmgBase + enemyDieRoll + netSL - (playerT + armorVal + reduction))
                                }

                            // Bonus elementalny od przeciwnika (jeśli ma w nazwie broni)
                            var extraElementalDmg = 0
                            val elemEmoji = when {
                                enemy.weaponName.contains("Ognia", true) -> "🔥"
                                enemy.weaponName.contains("Lodu", true) -> "❄️"
                                enemy.weaponName.contains("Pioruna", true) -> "⚡"
                                enemy.weaponName.contains("Trucizny", true) -> "🤢"
                                enemy.weaponName.contains("Żywiołów", true) -> "✨"
                                else -> ""
                            }

                            if (enemy.weaponName.contains("Ognia", true)) extraElementalDmg =
                                (1..4).random()
                            if (enemy.weaponName.contains("Lodu", true)) extraElementalDmg =
                                (1..4).random()
                            if (enemy.weaponName.contains("Pioruna", true)) extraElementalDmg =
                                (1..4).random()
                            if (enemy.weaponName.contains("Żywiołów", true)) extraElementalDmg = 4

                            var finalElemDmg = 0
                            if (extraElementalDmg > 0) {
                                val elemProt = activeArmor.getProt(location, elementalType)
                                val totalElemRed = elemProt + resistanceRank

                                finalElemDmg = if (resistanceRank >= 5) 0 else maxOf(
                                    0,
                                    extraElementalDmg - totalElemRed
                                )

                                if (finalElemDmg > 0) {
                                    playerBodyParts.applyDamage(location, finalElemDmg)
                                } else if (extraElementalDmg > 0) {
                                    combatLog.add(0, "🛡️ Całkowicie zredukowano obrażenia od żywiołów.")
                                }
                            }

                            val hpBefore = playerBodyParts.getHpFor(location)
                            val currentHp = playerBodyParts.applyDamage(location, finalDmg)

                        if (currentHp < 0 && hpBefore >= 0) {
                            val resistanceTest = (1..100).random()
                            val toughnessSkillRank = getSkillRank("Odporność")
                            val playerRes =
                                (heroStats["Odporność"] ?: 30) + (toughnessSkillRank * 5)

                            if (resistanceTest <= playerRes) {
                                // Test zdany - HP ustawione na 0
                                when {
                                    location.contains("Głowa", true) -> playerBodyParts.head = 0
                                    location.contains("Ręka", true) -> if (location.contains(
                                            "Lewa",
                                            true
                                        )
                                    ) playerBodyParts.leftArm = 0 else playerBodyParts.rightArm = 0

                                    location.contains("Noga", true) -> if (location.contains(
                                            "Lewa",
                                            true
                                        )
                                    ) playerBodyParts.leftLeg = 0 else playerBodyParts.rightLeg = 0

                                    else -> playerBodyParts.torso = 0
                                }
                                combatLog.add(
                                    0,
                                    "🛡️ Wytrzymujesz cios! (Test Odporności zdany: $resistanceTest vs $playerRes)"
                                )
                            } else {
                                combatLog.add(
                                    0,
                                    "💀 Cios okazuje się śmiertelny! (Test Odporności niezdany: $resistanceTest vs $playerRes)"
                                )
                            }
                        }

                        // Enemy weapon suffixes (if applicable)
                        if (finalDmg > 0 && enemy.weaponName.contains("TRUCIZNY")) {
                            playerIsPoisoned = true
                            combatLog.add(0, "🤢 Zostałeś ZATRUTY!")
                        }

                        playerAdvantage = 0
                        enemyAdvantage[enemy.id] = minOf(5, (enemyAdvantage[enemy.id] ?: 0) + 1)

                        val totalDmgShow = finalDmg + finalElemDmg
                        val dmgDetails = if (finalElemDmg > 0) " [$finalDmg+$finalElemDmg$elemEmoji]" else ""
                        val logEntry =
                            "🛑 ${enemy.name} trafia (${enemy.weaponName}) 〘$enemyRoll vs $playerRoll〙 w ‹88:$location› za ‹88:$totalDmgShow›$dmgDetails! (PŻ: $currentHp)"
                        combatLog.add(0, logEntry)
                        addToHistory(false, "WALKA: $logEntry")
                        playSoundAction(R.raw.darcie, 0)

                        if (currentHp <= 0 || enemyRoll % 11 == 0) {
                            val rawCrit = if (currentHp < 0) -currentHp else (1..10).random()
                            val critVal = rawCrit * 2
                            playerBodyParts.applyDamage(location, critVal)

                            val critMsg = "💥 ‹88:KRYTYK!› (+$critVal PŻ) na $location!"
                            combatLog.add(0, critMsg)

                            damageArmorAt(location)
                            applyInjuryToPlayer(location, rawCrit)
                        }

                        // No automatic save here to avoid high frequency file I/O during every hit.
                        // HP is saved at combat end, during medic healing, or when app pauses/destroys.
                    } else {
                        val defenseType =
                            if (usedDodge) "Unikasz ataku" else if (usedParry) "Parujesz atak" else "Przeciwnik pudłuje"
                        val logEntry =
                            if (playerSuccess) "🛡️ $defenseType ${enemy.name}! 〘$playerRoll vs $enemyRoll〙" else "💨 ${enemy.name} pudłuje! 〘$enemyRoll vs $playerRoll〙"
                        combatLog.add(0, logEntry)

                        if (playerSuccess) {
                            playerAdvantage = minOf(5, playerAdvantage + 1)
                            if (usedDodge) dodgesUsed++
                            if (usedParry) {
                                parriesUsed++
                                if (activeAbility == "Riposta") {
                                    playerAdvantage = minOf(5, playerAdvantage + 1)
                                    combatLog.add(
                                        0,
                                        "⚔️ Wykorzystujesz błąd przeciwnika do Riposty! (+1 Przewaga)"
                                    )
                                }
                            }
                        }
                        enemyAdvantage[enemy.id] = 0 // Lose advantage on miss/parry
                    }
                }
            }

            checkCombatEnd()
            if (!isCombatOver) {
                actionsLeft = 2
                dodgesUsed = 0
                parriesUsed = 0
                targetChangesUsed = 0
                activeAbility = null // Reset stances and prepared actions
                // Status effects tick for player
                if (playerIsPoisoned) {
                    val poisonDmg = maxOf(0, 1 - (poisonResRank / 3))
                    if (poisonDmg > 0) {
                        playerBodyParts.applyDamage("Korpus", poisonDmg)
                        combatLog.add(0, "🤢 Trucizna zadaje Ci $poisonDmg obrażeń w korpus.")
                    }
                    // Szansa na wyzdrowienie rośnie z rangą
                    if (Random.nextInt(1, 101) <= (10 + poisonResRank * 10)) {
                        playerIsPoisoned = false
                        combatLog.add(0, "✨ Twoje ciało zwalczyło truciznę!")
                    }
                }
                if (playerIsBurning) {
                    val burnDmg = maxOf(0, (1..3).random() - (fireResRank / 2))
                    if (burnDmg > 0) {
                        playerBodyParts.applyDamage("Korpus", burnDmg)
                        combatLog.add(0, "🔥 Płoniesz! Otrzymujesz $burnDmg obrażeń.")
                    }
                    val extinguishChance = 20 + (fireResRank * 15)
                    if (Random.nextInt(1, 101) <= extinguishChance) {
                        playerIsBurning = false
                        combatLog.add(0, "💧 Płomienie gasną!")
                    }
                }
                val shockRecovery = 50 + (lightningResRank * 10)
                if (playerIsShocked && (1..100).random() <= shockRecovery) {
                    playerIsShocked = false
                    combatLog.add(0, "⚡ Otrząsasz się z porażenia.")
                }
                val frozenRecovery = 30 + (coldResRank * 10)
                if (playerIsFrozen && (1..100).random() <= frozenRecovery) {
                    playerIsFrozen = false
                    combatLog.add(0, "❄️ Twoje mięśnie rozgrzewają się, nie jesteś już zamrożony.")
                }

                checkCombatEnd()
            }
        }
    }

    resolveAttack = attack@{ isReroll, isCharge ->
        val target = enemies.find { it.id == selectedEnemyId }
        if (target != null) {
            val isRanged = (activeWeapon?.rangedType ?: RangedType.NONE) != RangedType.NONE

            // Sprawdzenie przeładowania
            if (isRanged && !isWeaponLoaded && !isReroll) {
                combatLog.add(0, "⚠️ Twoja broń jest rozładowana! Musisz ją przeładować.")
            } else {
                // Distance check
                var canProceed = true
                if (!isRanged && target.distance > 0 && !isCharge) {
                    Toast.makeText(
                        context,
                        "Przeciwnik jest za daleko! Podejdź bliżej.",
                        Toast.LENGTH_SHORT
                    ).show()
                    canProceed = false
                } else if (isRanged && target.distance == 0) {
                    Toast.makeText(
                        context,
                        "Przeciwnik jest za blisko na strzał!",
                        Toast.LENGTH_SHORT
                    ).show()
                    canProceed = false
                }

                    if (canProceed) {
                        if (playerIsShocked) {
                            Toast.makeText(context, "Jesteś ogłuszony i nie możesz atakować!", Toast.LENGTH_SHORT).show()
                            return@attack
                        }
                        if (playerIsFrozen) {
                            Toast.makeText(context, "Jesteś zamrożony i nie możesz atakować!", Toast.LENGTH_SHORT).show()
                            return@attack
                        }

                        val traits = activeWeapon?.traits ?: ""
                    val isSlow = traits.contains("Wolny", true)

                    if (!isReroll) {
                        if (isCharge) {
                            actionsLeft -= 2
                            target.distance = 0
                            combatLog.add(0, "🏇 SZARŻUJESZ na ${target.name}!")
                        } else {
                            val cost = if (isSlow) 2 else 1
                            if (actionsLeft < cost) {
                                Toast.makeText(
                                    context,
                                    "Ta broń jest Wolna - wymaga 2 akcji!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@attack
                            }
                            actionsLeft -= cost
                        }
                    } else {
                        luckPoints -= 1
                        canReroll = false
                    }

                    // Po strzale z broni wymagającej przeładowania
                    if (isRanged && (activeWeapon?.reloadActions ?: 0) > 0 && !isReroll) {
                        isWeaponLoaded = false
                        reloadProgress = 0
                    }

                    // 1. ATTACKER (Player)
                    var skillBonus = 0
                    if (isCharge) skillBonus += 20 // Bonus za szarżę
                    if (playerIsBlinded) skillBonus -= 25

                    val weaponSkillName = getWeaponSkillName(activeWeapon)
                    skillBonus += getRankBonus(weaponSkillName)

                    // Bonusy za cechy broni
                    if (activeWeapon?.traits?.contains("Szybki", true) == true) skillBonus += 10

                    // Bonusy za talenty (Zapasy)
                    if (activeWeapon == null && hasWrestling) skillBonus += 5

                    val baseSkill = if (isRanged) playerUS else playerWW
                    val multiEnemyPenalty = if (isRanged) 0 else (enemies.size - 1) * 5
                    val weaponBonus = activeWeapon?.bonus ?: 0

                    val currentAbility = activeAbility
                    val abilityBonus = when (currentAbility) {
                        "Ogłuszenie" -> if (activeWeapon?.name?.contains(
                                "Kastet",
                                true
                            ) == true
                        ) 20 else 10

                        "Mierzony strzał" -> 20
                        "Finta" -> 10
                        "Riposta" -> 10
                        "Akrobatyczny atak" -> 0
                        "Postawa obronna" -> -20
                        else -> 0
                    }

                    val isMelee = (activeWeapon?.rangedType ?: RangedType.NONE) == RangedType.NONE
                    val isBrawlingWeapon =
                        activeWeapon?.name?.contains("Kastet", true) == true || activeWeapon == null

                    val akrobataRank = when {
                        heroLines.any { it.contains("Akrobatyczny atak (Arcymistrz)", true) } -> 5
                        heroLines.any { it.contains("Akrobatyczny atak (Mistrz)", true) } -> 4
                        heroLines.any { it.contains("Akrobatyczny atak (Ekspert)", true) } -> 3
                        heroLines.any { it.contains("Akrobatyczny atak (Adept)", true) } -> 2
                        heroLines.any { it.contains("Akrobatyczny atak (Uczeń)", true) } -> 1
                        heroLines.any {
                            it.contains(
                                "Akrobatyczny atak",
                                true
                            ) || it.contains("Akrobata", true)
                        } -> 1

                        else -> 0
                    }

                    val akrobataPenalty =
                        if (currentAbility == "Akrobatyczny atak") akrobataRank * 5 else 0

                    val totalMod =
                        skillBonus + (minOf(5, playerAdvantage) * 10) + (if (isMelee) minOf(
                            5,
                            playerMomentum
                        ) * 5 else 0) - multiEnemyPenalty + difficultyModifier + weaponBonus + abilityBonus + (activeWeapon?.size?.hitModifier ?: 0)
                    var playerEffectiveSkill = baseSkill + totalMod

                    // Kary za statusy gracza przy ataku
                    if (playerIsBlinded) playerEffectiveSkill -= 25
                    if (playerIsFrozen) playerEffectiveSkill -= 15
                    if (playerIsShocked) playerEffectiveSkill -= 20
                    if (playerIsPoisoned) playerEffectiveSkill -= 10

                    combatLog.add(
                        0,
                        "🎲 ‹2:Atak:› $baseSkill% (Mod: ${if (totalMod >= 0) "+" else ""}$totalMod%) = ‹2:$playerEffectiveSkill%›"
                    )

                    // Apply Traits: Dangerous (GROZNY) or Flying (LATAJACY)
                    if (target.traits.contains(EnemyTrait.GROZNY) || target.traits.contains(
                            EnemyTrait.LATAJACY
                        )
                    ) {
                        playerEffectiveSkill -= 10
                    }

                    // Help from Allies (Outnumbering)
                    if (!isRanged && allies.isNotEmpty()) {
                        playerEffectiveSkill += 10
                        combatLog.add(0, "🤝 Sojusznicy pomagają Ci w walce! (+10 WW)")
                    }

                    // Apply Traits: Dodge (UNIK)
                    var avoided = false
                    if (target.traits.contains(EnemyTrait.UNIK)) {
                        val dodgeRoll = Random.nextInt(1, 101)
                        val dodgeChance = maxOf(0, 20 - akrobataPenalty)
                        if (dodgeRoll <= dodgeChance) {
                            combatLog.add(
                                0,
                                "💨 ${target.name} wykonuje niesamowity UNIK! 〘$dodgeRoll〙"
                            )
                            playerAdvantage = 0
                            avoided = true
                        } else if (akrobataPenalty > 0 && dodgeRoll <= 20) {
                            combatLog.add(
                                0,
                                "🤸 Dzięki Akrobatycznemu atakowi przeciwnik NIE uniknął ciosu! (Wyrzucił $dodgeRoll, szansa obniżona do $dodgeChance%)"
                            )
                        }
                    }

                    if (!avoided) {
                        val playerRoll = Random.nextInt(1, 101)
                        val playerSL = calculateSL(playerRoll, playerEffectiveSkill)
                        val playerSuccess = playerRoll <= playerEffectiveSkill

                        // 2. DEFENDER (Enemy)
                        val canDefend = !isRanged
                        var targetEffectiveSkill = if (canDefend) target.ww + (minOf(
                            5,
                            enemyAdvantage[target.id] ?: 0
                        )) * 10 else 0

                        // Kary za statusy przeciwnika przy obronie
                        if (target.isBlinded) targetEffectiveSkill -= 20
                        if (target.isShocked) targetEffectiveSkill -= 20 // Status Ogłuszony
                        if (target.isFrozen) targetEffectiveSkill -= 10
                        if (target.isGrabbedBy != null) targetEffectiveSkill -= 15

                        val enemyRoll =
                            if (isReroll) lastEnemyRoll else (if (canDefend) Random.nextInt(
                                1,
                                101
                            ) else 100)
                        val enemySL = if (isReroll) lastEnemySL else (if (canDefend) calculateSL(
                            enemyRoll,
                            targetEffectiveSkill
                        ) else 0)
                        val enemySuccess =
                            if (isReroll) lastEnemySuccess else (if (canDefend) enemyRoll <= targetEffectiveSkill else false)

                        if (!isReroll) {
                            lastEnemyRoll = enemyRoll
                            lastEnemySL = enemySL
                            lastEnemySuccess = enemySuccess
                        }

                        // 3. TEST RESULT
                        val netSL = if (isRanged) playerSL else playerSL - enemySL
                        val playerWins =
                            if (isRanged) playerSuccess else (netSL > 0 || (netSL == 0 && playerSL > enemySL) || (netSL == 0 && playerSL == enemySL && playerEffectiveSkill > targetEffectiveSkill))

                        playSoundAction(R.raw.roll, 2)

                        if (playerWins && playerSuccess) {
                            // HIT!
                            val hitLocationRoll = reverseRoll(playerRoll)
                            var location = getHitLocation(hitLocationRoll)

                            if (currentAbility == "Ogłuszenie") {
                                location = "Głowa"
                                combatLog.add(0, "💫 Celujesz w ‹20:Głowę› by ogłuszyć!")
                            } else if (currentAbility == "Finta") {
                                combatLog.add(0, "🤺 Wykonujesz ‹20:Fintę› (+10 WW)")
                            } else if (currentAbility == "Riposta") {
                                combatLog.add(0, "⚔️ Szykujesz ‹20:Ripostę› (+10 WW)")
                            } else if (currentAbility == "Akrobatyczny atak") {
                                combatLog.add(0, "🤸 Wykonujesz ‹20:Akrobatyczny atak›!")
                            }

                            // Obliczanie Siły Ataku wg zasad 2 edycji dostosowanych do systemu SL
                            val playerSB = playerS
                            val attackStrength = when (activeWeapon?.rangedType) {
                                RangedType.FIXED -> activeWeapon?.weaponStrength ?: 3
                                RangedType.SB_DEPENDENT -> playerSB
                                RangedType.BOW -> minOf(playerSB, activeWeapon?.weaponStrength ?: 3)
                                else -> playerSB // Walka wręcz
                            }

                            val weaponDie = activeWeapon?.damage ?: "1k3"
                            val traits = activeWeapon?.traits ?: ""
                            val isCrushing = traits.contains("Druzgocący", true)
                            val isMightyTrait = traits.contains("Potężny cios", true)

                            val dieRoll = rollDamageDie(weaponDie, isCrushing, isMightyTrait, combatLog)

                            var armorVal =
                                target.getProt(activeWeapon?.damageType ?: DamageType.OBUCHOWA)

                            // --- CECHY BRONI ---
                            if (traits.contains("Przebijający pancerz", true)) {
                                armorVal = 0
                            } else if (traits.contains("Przebijający", true)) {
                                armorVal = maxOf(0, armorVal - 2)
                            }

                            var finalDmg = attackStrength + dieRoll + netSL - (target.toughness / 10 + armorVal)
                            if (activeWeapon == null) {
                                finalDmg -= 2
                            }
                            finalDmg = maxOf(1, finalDmg)

                            // --- SILNY CIOS (Passive) ---
                            if (!isRanged && isRanked("Silny cios")) {
                                val strongHitBonus = playerSB / 2
                                if (strongHitBonus > 0) {
                                    finalDmg += strongHitBonus
                                    combatLog.add(0, "💥 Silny cios: +$strongHitBonus obrażeń")
                                }
                            }


                            // Momentum bonus for melee
                            if (!isRanged) {
                                val isBrawlingWeapon = activeWeapon?.name?.contains(
                                    "Kastet",
                                    true
                                ) == true || activeWeapon == null
                                if (isBrawlingWeapon) {
                                    val momentumBonus = playerMomentum / 2
                                    finalDmg += momentumBonus
                                    if (momentumBonus > 0) combatLog.add(
                                        0,
                                        "👊 Pęd dodaje +$momentumBonus do obrażeń!"
                                    )
                                }
                                playerMomentum = minOf(5, playerMomentum + 1)
                            }

                            if (currentAbility == "Mierzony strzał") {
                                combatLog.add(0, "🎯 Wykorzystujesz ‹20:Mierzony strzał›!")
                            } else if (currentAbility == "Ogłuszenie") {
                                combatLog.add(0, "💫 Wykorzystujesz ‹20:Ogłuszenie›!")
                                val stunChance = 30 + (playerS / 10) * 10
                                if (Random.nextInt(1, 101) <= stunChance) {
                                    target.isShocked = true
                                    combatLog.add(0, "😵 ${target.name} jest OGŁUSZONY!")
                                }
                            }

                            if (currentAbility != "Postawa obronna") {
                                activeAbility = null // Clear ability after successful hit
                            }

                            // Apply Traits: Hard (TWARDY)
                            if (target.traits.contains(EnemyTrait.TWARDY)) {
                                finalDmg = maxOf(1, finalDmg - 1)
                            }

                            // --- PRZYGOTOWANIE OBRAŻEŃ SUFIKSU ---
                            var extraDmg = 0
                            var elemEmoji = ""
                            val suffix = activeWeapon?.suffix
                            if (finalDmg > 0 && suffix != null && suffix != WeaponSuffix.NONE) {
                                when (suffix) {
                                    WeaponSuffix.ZYWIOLOW -> {
                                        extraDmg = (2..5).random()
                                        elemEmoji = "✨"
                                    }

                                    WeaponSuffix.OGNIA -> {
                                        extraDmg = (1..4).random()
                                        elemEmoji = "🔥"
                                    }

                                    WeaponSuffix.LODU -> {
                                        extraDmg = (1..4).random()
                                        elemEmoji = "❄️"
                                    }

                                    WeaponSuffix.PIORUNA -> {
                                        extraDmg = (1..4).random()
                                        elemEmoji = "⚡"
                                    }

                                    WeaponSuffix.TRUCIZNY -> {
                                        extraDmg = (1..4).random()
                                        elemEmoji = "🤢"
                                    }

                                    else -> {}
                                }
                            }

                            val weaponName = activeWeapon?.name
                                ?: if (hasBrawling) "Pięści (Bijatyka)" else if (hasWrestling) "Chwyt (Zapasy)" else "Pięści"

                            val totalDmgShow = finalDmg + extraDmg

                            // Rozszerzony log obrażeń gracza
                            val dieRollShow = dieRoll // Zmienna dieRoll zawiera już bonusy z Druzgocącego i Potężnego ciosu
                            val diceParts = weaponDie.lowercase().split("k")
                            val sides = if(diceParts.size == 2) diceParts[1].trim().toIntOrNull() ?: 3 else 3
                            
                            val isMighty = dieRoll > sides // Proste sprawdzenie czy doszło do przerzutu
                            val mightyPart = if(isMighty) "(+${dieRoll - (if(isCrushing) dieRoll else dieRoll)})" else "" 
                            // Uproszczenie: dla czytelności po prostu pokażemy składowe
                            
                            val strongHitBonus = if (!isRanged && isRanked("Silny cios")) playerSB / 2 else 0
                            val momentumBonus = if (!isRanged && (activeWeapon?.name?.contains("Kastet", true) == true || activeWeapon == null)) playerMomentum / 2 else 0
                            
                            val dmgDetails = buildString {
                                append(" [")
                                append(attackStrength)
                                if (dieRoll > 0) append("+$dieRoll")
                                if (netSL != 0) {
                                    if (netSL > 0) append("+")
                                    append(netSL)
                                }
                                if (strongHitBonus > 0) append("+$strongHitBonus")
                                if (momentumBonus > 0) append("+$momentumBonus")
                                if (activeWeapon == null) append("-2 (Bez broni)")
                                if (extraDmg > 0) {
                                    append("+")
                                    append(extraDmg)
                                    append(elemEmoji)
                                }
                                append("]")
                            }

                            val logEntry =
                                "⚔️ Trafiasz ${target.name} 〘$playerRoll vs $enemyRoll〙 (+${netSL} PS) przez $weaponName w ‹2:$location› za ‹2:$totalDmgShow›$dmgDetails!"
                            combatLog.add(0, logEntry)
                            addToHistory(false, "WALKA: $logEntry")
                            playSoundAction(R.raw.klik2, 0)

                            val currentHp =
                                target.bodyParts.applyDamage(location, finalDmg + extraDmg)

                            if (currentHp < 0) {
                                val resTest = (1..100).random()
                                val targetRes = target.toughness
                                if (resTest <= targetRes) {
                                    target.bodyParts.torso = 0
                                    combatLog.add(
                                        0,
                                        "🛡️ ${target.name} ledwo trzyma się na nogach! (Test Odporności zdany)"
                                    )
                                } else {
                                    val overKill = -currentHp
                                    applyInjuryToEnemy(target, location, overKill)
                                }
                            } else if (playerRoll % 11 == 0) {
                                applyInjuryToEnemy(target, location, (1..5).random())
                            }

                            // --- EFEKTY DODATKOWE SUFIKSÓW (STATUSY) ---
                            if (finalDmg > 0 && suffix != null) {
                                if (suffix == WeaponSuffix.TRUCIZNY) {
                                    target.isPoisoned = true
                                    combatLog.add(0, "🤢 ${target.name} został ZATRUTY!")
                                }
                                if (suffix == WeaponSuffix.OGNIA && (1..100).random() <= 15) {
                                    target.isBurning = true
                                    combatLog.add(0, "🔥 ${target.name} stanął w płomieniach!")
                                }
                                if (suffix == WeaponSuffix.LODU && (1..100).random() <= 15) {
                                    target.isFrozen = true
                                    combatLog.add(
                                        0,
                                        "❄️ ${target.name} drętwieje z zimna! (-15 WW)"
                                    )
                                }
                                if (suffix == WeaponSuffix.PIORUNA && (1..100).random() <= 15) {
                                    target.isShocked = true
                                    combatLog.add(0, "⚡ ${target.name} został porażony!")
                                }
                                if (suffix == WeaponSuffix.WAMPIRYZMU) {
                                    val healAmount = (finalDmg / 2).coerceAtLeast(1)
                                    val bodyParts = listOf(
                                        "Głowa" to (playerBodyParts.maxHead - playerBodyParts.head),
                                        "Korpus" to (playerBodyParts.maxTorso - playerBodyParts.torso),
                                        "Lewa Ręka" to (playerBodyParts.maxLArm - playerBodyParts.leftArm),
                                        "Prawa Ręka" to (playerBodyParts.maxRArm - playerBodyParts.rightArm),
                                        "Lewa Noga" to (playerBodyParts.maxLLeg - playerBodyParts.leftLeg),
                                        "Prawa Noga" to (playerBodyParts.maxRLeg - playerBodyParts.rightLeg)
                                    )
                                    val mostWoundedPart = bodyParts.filter { it.second > 0 }
                                        .maxByOrNull { it.second }?.first

                                    if (mostWoundedPart != null) {
                                        when (mostWoundedPart) {
                                            "Głowa" -> playerBodyParts.head = minOf(
                                                playerBodyParts.maxHead,
                                                playerBodyParts.head + healAmount
                                            )

                                            "Korpus" -> playerBodyParts.torso = minOf(
                                                playerBodyParts.maxTorso,
                                                playerBodyParts.torso + healAmount
                                            )

                                            "Lewa Ręka" -> playerBodyParts.leftArm = minOf(
                                                playerBodyParts.maxLArm,
                                                playerBodyParts.leftArm + healAmount
                                            )

                                            "Prawa Ręka" -> playerBodyParts.rightArm = minOf(
                                                playerBodyParts.maxRArm,
                                                playerBodyParts.rightArm + healAmount
                                            )

                                            "Lewa Noga" -> playerBodyParts.leftLeg = minOf(
                                                playerBodyParts.maxLLeg,
                                                playerBodyParts.leftLeg + healAmount
                                            )

                                            "Prawa Noga" -> playerBodyParts.rightLeg = minOf(
                                                playerBodyParts.maxRLeg,
                                                playerBodyParts.rightLeg + healAmount
                                            )
                                        }
                                        combatLog.add(
                                            0,
                                            "🧛 Wampiryzm leczy $mostWoundedPart o $healAmount PŻ!"
                                        )
                                    }
                                }
                            }

                            playerAdvantage = minOf(5, playerAdvantage + 1)
                            enemyAdvantage[target.id] = 0
                            canReroll = false

                            if (currentHp <= 0 || playerRoll % 11 == 0) {
                                if (target.traits.contains(EnemyTrait.NIEUMARLY) && currentHp > 0) {
                                    combatLog.add(
                                        0,
                                        "💀 ${target.name} ignoruje krytyk (Nieumarły)!"
                                    )
                                } else {
                                    val rawCrit =
                                        if (currentHp < 0) -currentHp else (1..10).random()
                                    val stunBonus = if (activeWeapon?.traits?.contains(
                                            "Ogłuszający",
                                            true
                                        ) == true || currentAbility == "Ogłuszenie"
                                    ) 2 else 0
                                    val finalCritSeverity = rawCrit + stunBonus
                                    val critVal = finalCritSeverity * 2

                                    val hpBefore = target.bodyParts.getHpFor(location)
                                    target.bodyParts.applyDamage(location, critVal)

                                    if (target.bodyParts.getHpFor(location) < 0 && hpBefore >= 0) {
                                        val resRoll = (1..100).random()
                                        if (resRoll <= target.toughness) {
                                            when {
                                                location.contains("Głowa", true) -> target.bodyParts.head = 0
                                                location.contains("Ręka", true) -> if (location.contains("Lewa", true)) target.bodyParts.leftArm = 0 else target.bodyParts.rightArm = 0
                                                location.contains("Noga", true) -> if (location.contains("Lewa", true)) target.bodyParts.leftLeg = 0 else target.bodyParts.rightLeg = 0
                                                else -> target.bodyParts.torso = 0
                                            }
                                            combatLog.add(0, "🛡️ ${target.name} wytrzymuje cios! (Test Odporności zdany: $resRoll vs ${target.toughness})")
                                        } else {
                                            combatLog.add(0, "💥 ‹88:KRYTYK!› (+$critVal PŻ, sev:${finalCritSeverity}) na $location!")
                                            applyInjuryToEnemy(target, location, finalCritSeverity)
                                        }
                                    } else {
                                        combatLog.add(0, "💥 ‹88:KRYTYK!› (+$critVal PŻ, sev:${finalCritSeverity}) na $location!")
                                        applyInjuryToEnemy(target, location, finalCritSeverity)
                                    }
                                }
                            }

                            if (target.isDead) {
                                combatLog.add(0, "💀 ‹2:${target.name} pada martwy!›")
                                totalExpGained += target.exp
                                enemies.remove(target)
                                selectedEnemyId = enemies.firstOrNull()?.id ?: -1
                            }
                        } else {
                            // MISS or PARRY
                            val logEntry =
                                if (enemySuccess) "🛡️ ${target.name} paruje Twój atak! 〘$enemyRoll vs $playerRoll〙 (Net PS: $netSL)"
                                else "💨 Pudłujesz! 〘$playerRoll vs $enemyRoll〙 (Net PS: $netSL)"
                            combatLog.add(0, logEntry)

                            playerAdvantage = 0 // Lose advantage on failed attack
                            playerMomentum = 0 // Reset momentum on miss/parry
                            if (enemySuccess) enemyAdvantage[target.id] =
                                minOf(5, (enemyAdvantage[target.id] ?: 0) + 1)

                            if (luckPoints > 0 && !playerSuccess) {
                                canReroll = true
                            }

                            // Zużycie broni przy krytycznej porażce (96-100 lub dublet przy porażce)
                            if (playerRoll >= 96 || (playerRoll % 11 == 0 && !playerSuccess)) {
                                activeWeapon?.let { weapon ->
                                    val currentQualIndex = ItemQuality.entries.indexOf(weapon.quality)
                                    if (currentQualIndex > 1) {
                                        // Zmniejszenie jakości o połowę + 1 (minimum do ZNISZCZONY)
                                        val degradation = (currentQualIndex / 2) + 1
                                        val newQualIndex = maxOf(1, currentQualIndex - degradation)
                                        val newQuality = ItemQuality.entries[newQualIndex]

                                        val oldName = weapon.fullName
                                        activeWeapon = weapon.copy(quality = newQuality)

                                        combatLog.add(0, "🛠️ ‹88:USZKODZENIE!› Twoja broń ($oldName) ulega zużyciu i staje się: ‹2:${activeWeapon?.fullName}›!")
                                        playSoundAction(R.raw.broken, 0)

                                        // Aktualizacja w plecaku gracza
                                        val currentHeroId = (context as? ComponentActivity)?.intent?.getIntExtra("SELECTED_HERO_ID", -1)
                                        val suffix = if (currentHeroId != -1) "_$currentHeroId" else ""
                                        val eqFile = "game_equipment$suffix"
                                        val currentEq = try {
                                            context.openFileInput(eqFile).bufferedReader().readText().split(";").toMutableList()
                                        } catch (e: Exception) { mutableListOf() }

                                        val itemIndex = currentEq.indexOfFirst { it.contains(weapon.name, true) && it.contains(weapon.quality.displayName, true) }
                                        if (itemIndex != -1) {
                                            currentEq[itemIndex] = activeWeapon!!.fullName
                                            saveToFile(eqFile, currentEq.joinToString(";"))
                                        }
                                    } else if (currentQualIndex == 1) { // Już ZNISZCZONY - pęka całkowicie
                                        combatLog.add(0, "🛠️ ‹88:BROŃ PĘKA!› Twoja broń (${weapon.fullName}) jest całkowicie bezużyteczna!")
                                        activeWeapon = null
                                        playSoundAction(R.raw.broken, 0)
                                        
                                        // Usuwanie z ekwipunku
                                        val currentHeroId = (context as? ComponentActivity)?.intent?.getIntExtra("SELECTED_HERO_ID", -1)
                                        val suffix = if (currentHeroId != -1) "_$currentHeroId" else ""
                                        val eqFile = "game_equipment$suffix"
                                        val currentEq = try {
                                            context.openFileInput(eqFile).bufferedReader().readText().split(";").toMutableList()
                                        } catch (e: Exception) { mutableListOf() }
                                        
                                        val itemIndex = currentEq.indexOfFirst { it.contains(weapon.name, true) && it.contains(weapon.quality.displayName, true) }
                                        if (itemIndex != -1) {
                                            currentEq.removeAt(itemIndex)
                                            saveToFile(eqFile, currentEq.joinToString(";"))
                                        }
                                    }
                                }
                            }
                        }

                        // Clear ability after action unless it's a stance
                        if (currentAbility != "Postawa obronna") {
                            activeAbility = null
                        }

                        // Trigger reactivity for enemy property changes (distance, HP, armor, advantage)
                        val tIdx = enemies.indexOf(target)
                        if (tIdx != -1) enemies[tIdx] = target

                        // Auto-end turn if no actions left
                        if (actionsLeft <= 0) {
                            coroutineScope.launch {
                                delay(800)
                                allyTurn()
                                delay(500)
                                enemyTurn()
                            }
                        }
                    }
                }
            }
        } else {
            Toast.makeText(context, "Wybierz cel!", Toast.LENGTH_SHORT).show()
        }
    }

    val moveAction = {
        if (actionsLeft > 0) {
            val target = enemies.find { it.id == selectedEnemyId }
            if (target == null) {
                Toast.makeText(context, "Wybierz cel, do którego chcesz podejść!", Toast.LENGTH_SHORT)
                    .show()
            } else if (target.distance == 0) {
                Toast.makeText(context, "Jesteś już w zwarciu!", Toast.LENGTH_SHORT).show()
            } else {
                val moveDist = playerMove * 2 // Ruch standardowy
                val oldDist = target.distance
                target.distance = maxOf(0, target.distance - moveDist)

                actionsLeft -= 1
                val logEntry = "🏃 Podchodzisz do ${target.name} ($oldDist m -> ${target.distance} m)"
                combatLog.add(0, logEntry)
                playSoundAction(R.raw.klik1, 0)

                if (actionsLeft <= 0) {
                    coroutineScope.launch { delay(800); allyTurn(); delay(500); enemyTurn() }
                }
            }
        }
    }

    val drawWeapon: (String) -> Unit = { weaponName ->
        val weapon = weaponDb.find { it.name == weaponName } ?: CombatWeapon(weaponName, "1k3")
        playSoundAction(R.raw.klik1, 0)

        // Szybki refleks: darmowe dobycie jeśli broń nie jest założona
        if (quickReflexesRank > 0 && activeWeapon == null) {
            activeWeapon = weapon
            combatLog.add(0, "⚡ ‹20:Szybki refleks:› Darmowe dobycie ${weapon.name}")
        } else {
            if (actionsLeft >= 1) {
                actionsLeft -= 1
                val oldWeaponName = activeWeapon?.name ?: "Pięści"
                activeWeapon = weapon
                combatLog.add(0, "🖐️ Zamiana $oldWeaponName na ${weapon.name} (‹88:-1 akcja›)")
                if (actionsLeft <= 0) {
                    coroutineScope.launch {
                        delay(800)
                        allyTurn()
                        delay(500)
                        enemyTurn()
                    }
                }
            } else {
                Toast.makeText(context, "Brak akcji na zmianę broni!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isEnemySelectionOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121212))
                ) {
                    // GÓRNA CZĘŚĆ - Przygotowania (Zbrojmistrz i Medyk)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$heroName ✰ $heroLevel",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Cyan
                            )
                            if (isMedicOnly) {
                                IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Zamknij",
                                        tint = Color.White
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (playerGold > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⛀", color = Color.Yellow, fontSize = 14.sp)
                                        Text("$playerGold", color = Color.White, fontSize = 14.sp)
                                    }
                                }
                                if (playerSilver > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⛀", color = Color.LightGray, fontSize = 14.sp)
                                        Text("$playerSilver", color = Color.White, fontSize = 14.sp)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⛀", color = Color(0xFFFF9800), fontSize = 14.sp)
                                    Text("$playerCopper", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (!isMedicOnly) {
                            TabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = Color.Transparent,
                                contentColor = Color.Yellow,
                                divider = {}
                            ) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    text = { Text("ZBROJMISTRZ", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    text = {
                                        val isWounded = playerBodyParts.head < playerBodyParts.maxHead ||
                                                playerBodyParts.torso < playerBodyParts.maxTorso ||
                                                playerBodyParts.rightArm < playerBodyParts.maxRArm ||
                                                playerBodyParts.leftArm < playerBodyParts.maxLArm ||
                                                playerBodyParts.rightLeg < playerBodyParts.maxRLeg ||
                                                playerBodyParts.leftLeg < playerBodyParts.maxLLeg ||
                                                playerInjuries.isNotEmpty()

                                        val isBadlyWounded = playerBodyParts.torso < (playerBodyParts.maxTorso / 2) || playerInjuries.isNotEmpty()

                                        val medicColor = when {
                                            isBadlyWounded -> Color.Red
                                            isWounded -> Color.Yellow
                                            else -> Color.Green
                                        }
                                        Text("MEDYK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = medicColor)
                                    }
                                )
                            }
                        } else {
                            Text(
                                "USŁUGI MEDYCZNE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Green,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedTab == 0) {
                            // Zbrojmistrz
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        "⚒️ ZBROJMISTRZ",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Yellow
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Broń
                                    Text("Broń:", fontSize = 10.sp, color = Color.Gray)

                                    val equippedWeapons =
                                        playerEquipment.filter { it.contains("🅔") }
                                            .map { it.replace("🅔", "").trim() }
                                            .filter { itemName ->
                                                weaponDb.any { dbWeapon ->
                                                    itemName.contains(
                                                        dbWeapon.name,
                                                        ignoreCase = true
                                                    )
                                                } || itemName.contains("Sztylet", ignoreCase = true)
                                            }

                                    if (equippedWeapons.isEmpty()) {
                                        Text("👊 Pięści (1k3)", fontSize = 9.sp, color = Color.White)
                                    } else {
                                        Column {
                                            equippedWeapons.forEach { wName ->
                                                val dbWeapon = weaponDb.find { dbWeapon ->
                                                    wName.contains(dbWeapon.name, ignoreCase = true)
                                                }
                                                val dmg = dbWeapon?.damage ?: "1k3"
                                                Text(
                                                    "⚔️ $wName ﹝$dmg﹞",
                                                    fontSize = 9.sp,
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Pancerz
                                    Text("Pancerz:", fontSize = 10.sp, color = Color.Gray)
                                    val hasAnyArmor = activeArmor.pieces.isNotEmpty()
                                    if (!hasAnyArmor) {
                                        Text(
                                            "Brak zał. pancerza",
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    } else {
                                        Column {
                                            Text(
                                                "🛡️ Wyposażony pancerz",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Yellow
                                            )
                                            activeArmor.pieces.forEach { piece ->
                                                Text(
                                                    "• ${piece.fullName} [${piece.ppS}/${piece.ppO}/${piece.ppK}]",
                                                    fontSize = 8.sp,
                                                    color = Color.Yellow,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Naprawa broni
                                    // Naprawa przedmiotów (Broń i Pancerz)
                                    val repairableItems =
                                        playerEquipment.filter { it.contains("🅔") || it.contains("🛡️") }
                                            .map { it.trim() }
                                            .filter { itemName ->
                                                val q =
                                                    Regex("Jakość: (\\d+)%").find(itemName)?.groupValues?.get(
                                                        1
                                                    )?.toIntOrNull() ?: 100
                                                q < 90 || itemName.contains("Zniszczony") || itemName.contains(
                                                    "Uszkodzony"
                                                ) || itemName.contains("Znoszony")
                                            }

                                    val totalPlayerCopper =
                                        (playerSilver * 12) + playerCopper + (playerGold * 480)

                                    if (repairableItems.isNotEmpty()) {
                                        Text("Naprawa:", fontSize = 10.sp, color = Color.Gray)

                                        val totalCostAll = repairableItems.sumOf { item ->
                                            val q =
                                                Regex("Jakość: (\\d+)%").find(item)?.groupValues?.get(
                                                    1
                                                )?.toIntOrNull() ?: 100
                                            val d = (90 - q).coerceAtLeast(0)
                                            if (item.contains("🛡️")) {
                                                val m = if (item.lowercase()
                                                        .contains("skór")
                                                ) 2 else if (item.lowercase()
                                                        .contains("płyt") || item.lowercase()
                                                        .contains("kirys")
                                                ) 4 else 3
                                                d * m
                                            } else {
                                                val dbW =
                                                    weaponDb.find { item.contains(it.name, true) }
                                                val dv = Regex("k(\\d+)").find(
                                                    dbW?.damage ?: "1k6"
                                                )?.groupValues?.get(1)?.toIntOrNull() ?: 6
                                                d * dv
                                            }
                                        }

                                        if (repairableItems.size > 1) {
                                            val sAll = totalCostAll / 12
                                            val pAll = totalCostAll % 12
                                            val costAllTxt =
                                                if (sAll > 0) "${sAll}s ${pAll}p" else "${pAll}p"
                                            Button(
                                                onClick = {
                                                    var rem = totalCostAll
                                                    if (playerCopper >= rem) {
                                                        playerCopper -= rem; rem = 0
                                                    } else {
                                                        rem -= playerCopper; playerCopper = 0;
                                                        val sN = (rem + 11) / 12
                                                        if (playerSilver >= sN) {
                                                            playerSilver -= sN; playerCopper =
                                                                (sN * 12) - rem; rem = 0
                                                        } else if (playerGold > 0) {
                                                            playerGold--;
                                                            val fG = 480;
                                                            val aP = fG - rem
                                                            playerSilver += aP / 12; playerCopper += aP % 12; rem =
                                                                0
                                                        }
                                                    }
                                                    if (rem == 0) {
                                                        val cId =
                                                            (context as? ComponentActivity)?.intent?.getIntExtra(
                                                                "SELECTED_HERO_ID",
                                                                -1
                                                            )
                                                        val suf = if (cId != -1) "_$cId" else ""
                                                        val eqF = "game_equipment$suf"
                                                        val cEq = try {
                                                            context.openFileInput(eqF)
                                                                .bufferedReader().readText()
                                                                .split(";").toMutableList()
                                                        } catch (e: Exception) {
                                                            mutableListOf()
                                                        }
                                                        repairableItems.forEach { rItem ->
                                                            val idx =
                                                                cEq.indexOfFirst { it.trim() == rItem }
                                                            if (idx != -1) {
                                                                var cName =
                                                                    rItem.replace("Zniszczony", "")
                                                                        .replace("Uszkodzony", "")
                                                                        .replace("Znoszony", "")
                                                                        .trim()
                                                                cName = cName.replace(
                                                                    Regex("\\[Jakość: \\d+%\\]"),
                                                                    "[Jakość: 90%]"
                                                                ).trim()
                                                                if (!cName.contains("Jakość:")) cName += " [Jakość: 90%]"
                                                                cEq[idx] = cName
                                                            }
                                                        }
                                                        saveToFile(eqF, cEq.joinToString(";"))
                                                        playerEquipment.clear(); playerEquipment.addAll(
                                                            cEq
                                                        )
                                                        saveToHeroFile(0); playSoundAction(
                                                            R.raw.klik1,
                                                            0
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().height(26.dp)
                                                    .padding(top = 2.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(
                                                        0xFF00008B
                                                    )
                                                ),
                                                enabled = totalPlayerCopper >= totalCostAll
                                            ) {
                                                Text(
                                                    "NAPRAW OSPRZĘT ($costAllTxt)",
                                                    fontSize = 8.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }

                                        repairableItems.forEach { itemFullName ->
                                            val quality =
                                                Regex("Jakość: (\\d+)%").find(itemFullName)?.groupValues?.get(
                                                    1
                                                )?.toIntOrNull() ?: 100
                                            val diff = (90 - quality).coerceAtLeast(0)
                                            val isArmor = itemFullName.contains("🛡️")
                                            val costInCopper = if (isArmor) {
                                                val mult = if (itemFullName.lowercase()
                                                        .contains("skór")
                                                ) 2 else if (itemFullName.lowercase()
                                                        .contains("płyt") || itemFullName.lowercase()
                                                        .contains("kirys")
                                                ) 4 else 3
                                                diff * mult
                                            } else {
                                                val dbW = weaponDb.find {
                                                    itemFullName.contains(
                                                        it.name,
                                                        true
                                                    )
                                                }
                                                val diceVal = Regex("k(\\d+)").find(
                                                    dbW?.damage ?: "1k6"
                                                )?.groupValues?.get(1)?.toIntOrNull() ?: 6
                                                diff * diceVal
                                            }
                                            val sCost = costInCopper / 12
                                            val pCost = costInCopper % 12
                                            val costText =
                                                if (sCost > 0) "${sCost}s ${pCost}p" else "${pCost}p"
                                            val canAfford = totalPlayerCopper >= costInCopper
                                            Button(
                                                onClick = {
                                                    var remainingToPay = costInCopper
                                                    if (playerCopper >= remainingToPay) {
                                                        playerCopper -= remainingToPay
                                                        remainingToPay = 0
                                                    } else {
                                                        remainingToPay -= playerCopper
                                                        playerCopper = 0
                                                        val silverNeeded =
                                                            (remainingToPay + 11) / 12
                                                        if (playerSilver >= silverNeeded) {
                                                            playerSilver -= silverNeeded
                                                            playerCopper =
                                                                (silverNeeded * 12) - remainingToPay
                                                            remainingToPay = 0
                                                        } else {
                                                            if (playerGold > 0) {
                                                                playerGold--
                                                                val fromGold = 480
                                                                val afterPaying =
                                                                    fromGold - remainingToPay
                                                                playerSilver += afterPaying / 12
                                                                playerCopper += afterPaying % 12
                                                                remainingToPay = 0
                                                            }
                                                        }
                                                    }

                                                    val currentHeroId =
                                                        (context as? ComponentActivity)?.intent?.getIntExtra(
                                                            "SELECTED_HERO_ID",
                                                            -1
                                                        )
                                                    val suffix =
                                                        if (currentHeroId != -1) "_$currentHeroId" else ""
                                                    val eqFile = "game_equipment$suffix"
                                                    val currentEq = try {
                                                        context.openFileInput(eqFile)
                                                            .bufferedReader().readText().split(";")
                                                            .toMutableList()
                                                    } catch (e: Exception) {
                                                        mutableListOf<String>()
                                                    }

                                                    val itemIndex =
                                                        currentEq.indexOfFirst { it.trim() == itemFullName }
                                                    if (itemIndex != -1) {
                                                        var cleanName =
                                                            itemFullName.replace("Zniszczony", "")
                                                                .replace("Uszkodzony", "")
                                                                .replace("Znoszony", "").trim()
                                                        cleanName = cleanName.replace(
                                                            Regex("\\[Jakość: \\d+%\\]"),
                                                            "[Jakość: 90%]"
                                                        ).trim()
                                                        if (!cleanName.contains("Jakość:")) {
                                                            cleanName += " [Jakość: 90%]"
                                                        }
                                                        currentEq[itemIndex] = cleanName
                                                        saveToFile(
                                                            eqFile,
                                                            currentEq.joinToString(";")
                                                        )
                                                        playerEquipment.clear()
                                                        playerEquipment.addAll(currentEq)
                                                        if (activeWeapon?.fullName == itemFullName) {
                                                            activeWeapon =
                                                                activeWeapon?.copy(quality = ItemQuality.NONE)
                                                        }

                                                        // Zapisz historię
                                                        val historyFile = "hero_${currentHeroId}_pd_history"
                                                        val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                                        val historyLog = try { context.openFileInput(historyFile).bufferedReader().readText() } catch(e: Exception) { "" }
                                                        saveToFile(historyFile, "$timestamp: Naprawa u Zbrojmistrza: ${cleanName.replace("🅔", "").replace("🛡️", "").take(20)} ($costText)\n$historyLog")
                                                    }
                                                    saveToHeroFile(0)
                                                    playSoundAction(R.raw.klik1, 0)
                                                },
                                                modifier = Modifier.fillMaxWidth().height(24.dp)
                                                    .padding(top = 2.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(
                                                        0xFF333333
                                                    )
                                                ),
                                                contentPadding = PaddingValues(0.dp),
                                                enabled = canAfford && diff > 0
                                            ) {
                                                Text(
                                                    "NAPRAW: ${
                                                        itemFullName.replace("🅔", "")
                                                            .replace("🛡️", "").take(12)
                                                    }.. ($costText)", fontSize = 7.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Medyk
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        "⚕️ MEDYK",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Green
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    val locs = listOf(
                                        Triple(
                                            "Głowa",
                                            playerBodyParts.head,
                                            playerBodyParts.maxHead
                                        ),
                                        Triple(
                                            "Korpus",
                                            playerBodyParts.torso,
                                            playerBodyParts.maxTorso
                                        ),
                                        Triple(
                                            "Ramię P",
                                            playerBodyParts.rightArm,
                                            playerBodyParts.maxRArm
                                        ),
                                        Triple(
                                            "Ramię L",
                                            playerBodyParts.leftArm,
                                            playerBodyParts.maxLArm
                                        ),
                                        Triple(
                                            "Noga P",
                                            playerBodyParts.rightLeg,
                                            playerBodyParts.maxRLeg
                                        ),
                                        Triple(
                                            "Noga L",
                                            playerBodyParts.leftLeg,
                                            playerBodyParts.maxLLeg
                                        )
                                    )

                                    locs.forEach { (name, cur, max) ->
                                        val hpStr = if (cur < max) "$cur / $max" else "$max"
                                        val protLoc = when (name) {
                                            "Ramię P", "Ramię L" -> "Ręka"
                                            "Noga P", "Noga L" -> "Nogi"
                                            else -> name
                                        }
                                        val pS = activeArmor.getProt(protLoc, DamageType.SIECZNA)
                                        val pO = activeArmor.getProt(protLoc, DamageType.OBUCHOWA)
                                        val pK = activeArmor.getProt(protLoc, DamageType.KLUTA)
                                        val protStr =
                                            if (pS > 0 || pO > 0 || pK > 0) " [$pS/$pO/$pK]" else ""

                                        Text(
                                            "$name: $hpStr$protStr",
                                            fontSize = 9.sp,
                                            color = if (cur < max) Color.Red else Color.White
                                        )
                                    }

                                    val totalPlayerCopper =
                                        (playerSilver * 12) + playerCopper + (playerGold * 480)
                                    val healCost = 12 // 1 sz = 12 mp
                                    val isWounded =
                                        playerBodyParts.head < playerBodyParts.maxHead ||
                                                playerBodyParts.torso < playerBodyParts.maxTorso ||
                                                playerBodyParts.rightArm < playerBodyParts.maxRArm ||
                                                playerBodyParts.leftArm < playerBodyParts.maxLArm ||
                                                playerBodyParts.rightLeg < playerBodyParts.maxRLeg ||
                                                playerBodyParts.leftLeg < playerBodyParts.maxLLeg ||
                                                playerInjuries.isNotEmpty()
                                    val canAffordHeal = totalPlayerCopper >= healCost

                                    Button(
                                        onClick = {
                                            var remainingToPay = healCost
                                            if (playerCopper >= remainingToPay) {
                                                playerCopper -= remainingToPay
                                                remainingToPay = 0
                                            } else {
                                                remainingToPay -= playerCopper
                                                playerCopper = 0
                                                val silverNeeded = (remainingToPay + 11) / 12
                                                if (playerSilver >= silverNeeded) {
                                                    playerSilver -= silverNeeded
                                                    playerCopper =
                                                        (silverNeeded * 12) - remainingToPay
                                                    remainingToPay = 0
                                                } else {
                                                    if (playerGold > 0) {
                                                        playerGold--
                                                        val fromGold = 480
                                                        val afterPaying = fromGold - remainingToPay
                                                        playerSilver += afterPaying / 12
                                                        playerCopper += afterPaying % 12
                                                        remainingToPay = 0
                                                    }
                                                }
                                            }

                                            if (remainingToPay == 0) {
                                                // Ulecz HP
                                                playerBodyParts.head = playerBodyParts.maxHead
                                                playerBodyParts.torso = playerBodyParts.maxTorso
                                                playerBodyParts.rightArm = playerBodyParts.maxRArm
                                                playerBodyParts.leftArm = playerBodyParts.maxLArm
                                                playerBodyParts.rightLeg = playerBodyParts.maxRLeg
                                                playerBodyParts.leftLeg = playerBodyParts.maxLLeg

                                                // Zapisz monety i uleczone HP do pliku bohatera
                                                saveToHeroFile(0)

                                                // Ulecz najcięższą ranę
                                                if (playerInjuries.isNotEmpty()) {
                                                    val injuryToHeal = playerInjuries.map { name ->
                                                        val ci =
                                                            criticalInjuriesTable.find { it.name == name }
                                                        name to (ci?.severity ?: 0)
                                                    }.maxByOrNull { it.second }?.first

                                                    if (injuryToHeal != null) {
                                                        playerInjuries.remove(injuryToHeal)
                                                    }
                                                }

                                                val currentHeroId =
                                                    (context as? ComponentActivity)?.intent?.getIntExtra(
                                                        "SELECTED_HERO_ID",
                                                        -1
                                                    )
                                                val suffix =
                                                    if (currentHeroId != -1) "_$currentHeroId" else ""

                                                // Zapisz rany (HP jest już w saveToHeroFile)
                                                saveToFile(
                                                    "game_injuries$suffix",
                                                    playerInjuries.joinToString(";")
                                                )

                                                // Dodaj wpis do historii PD
                                                val historyFile = "hero_${currentHeroId}_pd_history"
                                                val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                                val historyLog = try { context.openFileInput(historyFile).bufferedReader().readText() } catch(e: Exception) { "" }
                                                saveToFile(historyFile, "$timestamp: Leczenie u Medyka (12 mp)\n$historyLog")

                                                playSoundAction(R.raw.klik1, 0)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(
                                                0xFF004D00
                                            )
                                        ),
                                        enabled = canAffordHeal && isWounded
                                    ) {
                                        Text("LECZENIE", fontSize = 9.sp)
                                    }

                                    Button(
                                        onClick = {
                                            val luckCost = 36 // 3 sz = 36 mp
                                            var remainingToPay = luckCost
                                            if (playerCopper >= remainingToPay) {
                                                playerCopper -= remainingToPay
                                                remainingToPay = 0
                                            } else {
                                                remainingToPay -= playerCopper
                                                playerCopper = 0
                                                val silverNeeded = (remainingToPay + 11) / 12
                                                if (playerSilver >= silverNeeded) {
                                                    playerSilver -= silverNeeded
                                                    playerCopper =
                                                        (silverNeeded * 12) - remainingToPay
                                                    remainingToPay = 0
                                                } else {
                                                    if (playerGold > 0) {
                                                        playerGold--
                                                        val fromGold = 480
                                                        val afterPaying = fromGold - remainingToPay
                                                        playerSilver += afterPaying / 12
                                                        playerCopper += afterPaying % 12
                                                        remainingToPay = 0
                                                    }
                                                }
                                            }

                                            if (remainingToPay == 0) {
                                                luckPoints =
                                                    if (hasLuckSkill) fatePoints * 2 else fatePoints
                                                saveToHeroFile(0)
                                                playSoundAction(R.raw.klik1, 0)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(
                                                0xFF00008B
                                            )
                                        ),
                                        enabled = totalPlayerCopper >= 36 && luckPoints < (if (hasLuckSkill) fatePoints * 2 else fatePoints)
                                    ) {
                                        Text("ODNAW SZCZ. (3 sz)", fontSize = 9.sp)
                                    }

                                    if (playerInjuries.isNotEmpty()) {
                                        Text(
                                            "Rany: ${playerInjuries.size}",
                                            fontSize = 9.sp,
                                            color = Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ŚRODEK - Suwak ilości przeciwników
                    if (selectedTab == 1) {
                         Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (!isMedicOnly) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF222222))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "ILOŚĆ PRZECIWNIKÓW: ${enemyCountToSpawn.toInt()}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = enemyCountToSpawn,
                                onValueChange = { enemyCountToSpawn = it },
                                valueRange = 1f..5f,
                                steps = 3,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.Red,
                                    activeTrackColor = Color.Red
                                )
                            )
                        }

                        // DOLNA CZĘŚĆ - Bestiariusz (Wybór)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                "WYBIERZ PRZECIWNIKA",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(Bestiary.templates) { template ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                setupEnemies(
                                                    template,
                                                    enemyCountToSpawn.toInt()
                                                )
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(
                                                0xFF1A1A1A
                                            )
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    template.name,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    template.description,
                                                    color = Color.Gray,
                                                    fontSize = 10.sp,
                                                    maxLines = 1
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                StatBadge("WW", template.ww, Color.Red)
                                                StatBadge("S", template.s, Color.Yellow)
                                                StatBadge("T", template.t, Color.Green)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Medic only mode spacer or info
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("TRYB MEDYKA - WYBIERZ USŁUGĘ POWYŻEJ", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color(0xFF121212))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Akcje: $actionsLeft / 2",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Yellow
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Szczęście: $luckPoints", color = Color.Green, fontSize = 14.sp)
                        }
                    }

                    // Difficulty Slider (Removed as per request)

                    // Enemies List
                    Text(
                        "PRZECIWNICY (Wybierz cel):",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        enemies.forEach { en ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(80.dp)
                                    .background(
                                        if (selectedEnemyId == en.id) Color(0xFF440000) else Color(
                                            0xFF222222
                                        ), RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (selectedEnemyId == en.id) Color.Red else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (selectedEnemyId != en.id) {
                                            val currentEnemy =
                                                enemies.find { it.id == selectedEnemyId }
                                            val isEngaged =
                                                (currentEnemy?.distance == 0 || en.distance == 0)

                                            if (isEngaged) {
                                                if (quickReflexesRank > 0 && targetChangesUsed < quickReflexesRank) {
                                                    selectedEnemyId = en.id
                                                    targetChangesUsed++
                                                } else if (actionsLeft > 0) {
                                                    selectedEnemyId = en.id
                                                    actionsLeft--
                                                    combatLog.add(0, "🎯 Zmiana celu na ${en.name} (‹88:-1 akcja›)")
                                                    if (actionsLeft <= 0) {
                                                        coroutineScope.launch {
                                                            delay(800)
                                                            allyTurn()
                                                            delay(500)
                                                            enemyTurn()
                                                        }
                                                    }
                                                } else {
                                                    val msg = if (quickReflexesRank > 0)
                                                        "Wykorzystano limit Szybkiego refleksu i brak akcji!"
                                                    else "Zmiana celu w zwarciu kosztuje Akcję!"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT)
                                                        .show()
                                                }
                                            } else {
                                                selectedEnemyId = en.id
                                            }
                                        }
                                    }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        en.name,
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    val torsoHp = en.bodyParts.torso
                                    val headHp = en.bodyParts.head
                                    val rArmHp = en.bodyParts.rightArm
                                    val lArmHp = en.bodyParts.leftArm
                                    val rLegHp = en.bodyParts.rightLeg
                                    val lLegHp = en.bodyParts.leftLeg

                                    Text(
                                        "${torsoHp} PŻ (K) • ${en.distance} m",
                                        color = if (torsoHp <= 0) Color.Yellow else Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )

                                    // Wyświetlanie innych części jeśli są ranne
                                    if (headHp < en.bodyParts.maxHead || rArmHp < en.bodyParts.maxRArm || lArmHp < en.bodyParts.maxLArm || rLegHp < en.bodyParts.maxRLeg || lLegHp < en.bodyParts.maxLLeg) {
                                        Text(
                                            "G:$headHp R:${rArmHp}/${lArmHp} N:${rLegHp}/${lLegHp}",
                                            fontSize = 7.sp,
                                            color = Color.LightGray
                                        )
                                    }

                                    if (en.injuries.isNotEmpty()) {
                                        Text(
                                            en.injuries.joinToString(", "),
                                            fontSize = 8.sp,
                                            color = Color.Yellow,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                    StatusIndicators(
                                        isPoisoned = en.isPoisoned,
                                        isBurning = en.isBurning,
                                        isFrozen = en.isFrozen,
                                        isShocked = en.isShocked,
                                        isBlinded = en.isBlinded
                                    )
                                    LinearProgressIndicator(
                                        progress = {
                                            (torsoHp.toFloat() / en.bodyParts.maxTorso.toFloat()).coerceIn(
                                                0f,
                                                1f
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = if (torsoHp <= 0) Color.Yellow else Color.Red,
                                        trackColor = Color.DarkGray
                                    )
                                    if (en.traits.isNotEmpty()) {
                                        Text(
                                            en.traits.joinToString(", ") { it.displayName },
                                            fontSize = 7.sp,
                                            color = Color.Yellow
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Allies
                    if (allies.isNotEmpty()) {
                        Text(
                            "POMOCNICY:",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allies.forEach { al ->
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(
                                            0xFF1B3A1B
                                        )
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(al.name, fontSize = 10.sp, color = Color.White)
                                        Text(
                                            "${al.bodyParts.torso} PŻ (K)",
                                            fontSize = 12.sp,
                                            color = Color.Green
                                        )

                                        // Detale części ciała sojusznika
                                        val headHp = al.bodyParts.head
                                        val rArmHp = al.bodyParts.rightArm
                                        val lArmHp = al.bodyParts.leftArm
                                        val rLegHp = al.bodyParts.rightLeg
                                        val lLegHp = al.bodyParts.leftLeg

                                        Text(
                                            "G:$headHp R:$rArmHp/$lArmHp N:$rLegHp/$lLegHp",
                                            fontSize = 7.sp,
                                            color = Color.LightGray
                                        )

                                        LinearProgressIndicator(
                                            progress = {
                                                (al.bodyParts.torso.toFloat() / al.bodyParts.maxTorso.toFloat()).coerceIn(
                                                    0f,
                                                    1f
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth().height(2.dp)
                                                .padding(top = 2.dp),
                                            color = Color.Green,
                                            trackColor = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Player Status and Weapon Selection
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // LEWA STRONA: Lokacje i Pancerz
                            Column(modifier = Modifier.weight(1.1f)) {
                                @Composable
                                fun LimbStat(
                                    loc: String,
                                    current: Int,
                                    max: Int,
                                    modifier: Modifier = Modifier
                                ) {
                                    val armorIcons = HeroLogic.getArmorLocationIcons(activeArmor.pieces, loc)
                                    val color = when {
                                        current < 3 -> Color.Red
                                        current < max -> Color.Yellow
                                        else -> Color(0xFF4CAF50) // Green
                                    }
                                    Column(
                                        modifier = modifier,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = if (current == max) "$current" else "$current/$max",
                                            fontSize = 9.sp,
                                            color = color,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 9.sp
                                        )
                                        if (armorIcons.isNotEmpty()) {
                                            Text(
                                                text = armorIcons,
                                                fontSize = 7.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 7.sp
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.3f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(2.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.huma),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().alpha(0.6f)
                                    )

                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Głowa (20%)
                                        Box(
                                            modifier = Modifier.weight(0.2f).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            LimbStat(
                                                "Głowa",
                                                playerBodyParts.head,
                                                playerBodyParts.maxHead
                                            )
                                        }
                                        // Korpus i Ręce (40%)
                                        Row(modifier = Modifier.weight(0.4f).fillMaxWidth()) {
                                            LimbStat(
                                                "Lewa Ręka",
                                                playerBodyParts.leftArm,
                                                playerBodyParts.maxLArm,
                                                Modifier.weight(1f)
                                            )
                                            LimbStat(
                                                "Korpus",
                                                playerBodyParts.torso,
                                                playerBodyParts.maxTorso,
                                                Modifier.weight(1f)
                                            )
                                            LimbStat(
                                                "Prawa Ręka",
                                                playerBodyParts.rightArm,
                                                playerBodyParts.maxRArm,
                                                Modifier.weight(1f)
                                            )
                                        }
                                        // Nogi (40%)
                                        Row(modifier = Modifier.weight(0.4f).fillMaxWidth()) {
                                            LimbStat(
                                                "Lewa Noga",
                                                playerBodyParts.leftLeg,
                                                playerBodyParts.maxLLeg,
                                                Modifier.weight(1f).padding(top = 8.dp)
                                            )
                                            LimbStat(
                                                "Prawa Noga",
                                                playerBodyParts.rightLeg,
                                                playerBodyParts.maxRLeg,
                                                Modifier.weight(1f).padding(top = 8.dp)
                                            )
                                        }
                                    }
                                }

                                if (playerInjuries.isNotEmpty()) {
                                    Text(
                                        "RANY: ${playerInjuries.joinToString(", ")}",
                                        fontSize = 6.sp,
                                        color = Color.Red,
                                        maxLines = 1,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                StatusIndicators(
                                    isPoisoned = playerIsPoisoned,
                                    isBurning = playerIsBurning,
                                    isFrozen = playerIsFrozen,
                                    isShocked = playerIsShocked,
                                    isBlinded = playerIsBlinded,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }

                            // PRAWA STRONA: Przyciski broni
                            Column(
                                modifier = Modifier.weight(0.9f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                activeWeapon?.let {
                                    Text(
                                        it.fullName,
                                        color = Color.Yellow,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    val b = mutableListOf<String>()
                                    if (it.prefix.wwBonus != 0 && it.rangedType == RangedType.NONE) b.add(
                                        "WW:+${it.prefix.wwBonus}"
                                    )
                                    if (it.prefix.usBonus != 0 && it.rangedType != RangedType.NONE) b.add(
                                        "US:+${it.prefix.usBonus}"
                                    )
                                    if (it.prefix.initBonus != 0) b.add("I:+${it.prefix.initBonus}")
                                    if (it.prefix.dodgeBonus != 0) b.add("U:+${it.prefix.dodgeBonus}")
                                    if (b.isNotEmpty()) Text(
                                        b.joinToString(" "),
                                        color = Color.Green,
                                        fontSize = 7.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Text(
                                    "BROŃ (max 3)",
                                    fontSize = 8.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val availableWeapons = playerInventory.filter { invItem ->
                                    weaponDb.any { dbWep ->
                                        invItem.contains(
                                            dbWep.name,
                                            ignoreCase = true
                                        )
                                    }
                                }.take(3)

                                // Zawsze dodajemy Pięści jako opcję bazową, jeśli nie ma broni, lub jako jedną z opcji
                                val weaponsToShow =
                                    if (availableWeapons.isEmpty()) listOf("Pięści") else availableWeapons

                                weaponsToShow.forEach { weaponName ->
                                    val dbWeapon = weaponDb.find {
                                        weaponName.contains(
                                            it.name,
                                            ignoreCase = true
                                        ) && (it.suffix == WeaponSuffix.NONE || weaponName.contains(
                                            it.suffix.displayName
                                        ))
                                    }
                                        ?: weaponDb.find {
                                            weaponName.contains(
                                                it.name,
                                                ignoreCase = true
                                            )
                                        }

                                    val weaponPrefix = when {
                                        weaponName.lowercase()
                                            .contains("elfick") -> WeaponPrefix.ELFICKI

                                        weaponName.lowercase()
                                            .contains("khazadzk") || weaponName.lowercase()
                                            .contains("krasnoludzk") -> WeaponPrefix.KHAZADZKI

                                        weaponName.lowercase()
                                            .contains("ludzk") -> WeaponPrefix.LUDZKI

                                        else -> WeaponPrefix.NONE
                                    }

                                    val canUse = when (weaponPrefix) {
                                        WeaponPrefix.NONE -> true
                                        WeaponPrefix.ELFICKI -> playerRace.contains(
                                            "Elf",
                                            ignoreCase = true
                                        )

                                        WeaponPrefix.KHAZADZKI -> playerRace.contains(
                                            "Krasnolud",
                                            ignoreCase = true
                                        ) || playerRace.contains("Khazad", ignoreCase = true)

                                        WeaponPrefix.LUDZKI -> playerRace.contains(
                                            "Człowiek",
                                            ignoreCase = true
                                        )

                                        else -> true
                                    }

                                    val isFists = weaponName == "Pięści"
                                    val isSelected =
                                        if (isFists) activeWeapon == null else activeWeapon?.name == dbWeapon?.name && activeWeapon?.suffix == dbWeapon?.suffix

                                    Button(
                                        onClick = {
                                            if (canUse) {
                                                if (isFists) activeWeapon = null else drawWeapon(
                                                    weaponName
                                                )
                                                playSoundAction(R.raw.klik1, 0)
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Ta rasa nie może używać tego przedmiotu!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(22.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) Color(0xFF0000AA) else Color(
                                                0xFF333333
                                            )
                                        ),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        enabled = !isCombatOver
                                    ) {
                                        Text(
                                            text = (if (canUse) "" else "🛑 ") + (if (isFists) "Pięści" else (dbWeapon?.fullName
                                                ?: weaponName).take(15)),
                                            fontSize = 8.sp,
                                            color = if (canUse) Color.White else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Antidotum Button
                    if (playerIsPoisoned && !isCombatOver) {
                        val hasAntidote = playerInventory.any { it.contains("Antidotum", true) }
                        Button(
                            onClick = {
                                if (hasAntidote) {
                                    playerIsPoisoned = false
                                    val item =
                                        playerInventory.find { it.contains("Antidotum", true) }
                                    if (item != null) playerInventory.remove(item)
                                    combatLog.add(0, "🧪 Używasz antidotum. Trucizna znika!")
                                    actionsLeft = maxOf(0, actionsLeft - 1)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Nie masz antidotum!",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            enabled = actionsLeft > 0
                        ) {
                            Text("UŻYJ ANTIDOTUM (1 Akcja)")
                        }
                    }

                    // Log
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black)
                            .padding(4.dp)
                    ) {
                        items(combatLog) { log ->
                            Text(colorize(log), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Special Abilities (Skills as Chips)
                    if (availableAbilities.isNotEmpty() && !isCombatOver) {
                        val enemyHasWeapon =
                            enemies.find { it.id == selectedEnemyId }?.weaponDmg ?: 0 > 0

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                availableAbilities.forEach { ability ->
                                    val isEnabled =
                                        if (ability == "Rozbrojenie") enemyHasWeapon else true

                                    FilterChip(
                                        selected = activeAbility == ability,
                                        onClick = {
                                            activeAbility =
                                                if (activeAbility == ability) null else ability
                                            playSoundAction(R.raw.klik1, 0)
                                        },
                                        label = { Text(ability, fontSize = 10.sp) },
                                        enabled = isEnabled,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF8B0000),
                                            selectedLabelColor = Color.White,
                                            containerColor = Color(0xFF333333),
                                            labelColor = Color.LightGray,
                                            disabledContainerColor = Color(0xFF1A1A1A)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (!isCombatOver) {
                        if (canReroll) {
                            Button(
                                onClick = { resolveAttack(true, false) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF006400
                                    )
                                )
                            ) {
                                Text(
                                    "UŻYJ SZCZĘŚCIA (REROL)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    canReroll = false
                                    if (actionsLeft <= 0) {
                                        coroutineScope.launch {
                                            delay(800)
                                            allyTurn()
                                            delay(500)
                                            enemyTurn()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) {
                                Text("POGÓDŹ SIĘ Z LOSEM", fontSize = 14.sp)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { moveAction() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                                    enabled = actionsLeft > 0 && enemies.isNotEmpty()
                                ) {
                                    Text("RUCH (${playerMove * 2}m)", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { resolveAttack(false, true) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0xFF663300
                                        )
                                    ),
                                    enabled = actionsLeft >= 2 && enemies.isNotEmpty() && (enemies.find { it.id == selectedEnemyId }?.distance
                                        ?: 10) in 1..10
                                            && (activeWeapon?.rangedType
                                        ?: RangedType.NONE) == RangedType.NONE
                                ) {
                                    val dist =
                                        enemies.find { it.id == selectedEnemyId }?.distance ?: 0
                                    Text("SZARŻA ($dist m)", fontSize = 11.sp)
                                }
                            }

                            Button(
                                onClick = { resolveAttack(false, false) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                enabled = enemies.isNotEmpty()
                            ) {
                                val targetDist = enemies.find { it.id == selectedEnemyId }?.distance ?: 0
                                val currentWeapon = activeWeapon
                                val weaponSkillName = getWeaponSkillName(currentWeapon)
                                val rankBonus = getRankBonus(weaponSkillName)
                                val effectiveSkill = if (currentWeapon?.rangedType != RangedType.NONE && currentWeapon != null) playerUS else playerWW
                                val baseVal = effectiveSkill - rankBonus

                                val label = if (currentWeapon == null) {
                                    if (targetDist > 0) "PODEJDŹ!"
                                    else "Bijatyka [$baseVal% WW${if (rankBonus >= 0) "+" else ""}$rankBonus]"
                                } else {
                                    if (currentWeapon.rangedType != RangedType.NONE) {
                                        if (targetDist == 0) "ZA BLISKO!" else "STRZELAJ"
                                    } else {
                                        if (targetDist > 0) "PODEJDŹ!" else "ATAKUJ"
                                    }
                                }
                                Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                            TextButton(onClick = {
                                coroutineScope.launch {
                                    allyTurn()
                                    delay(500)
                                    enemyTurn()
                                }
                            }) {
                                Text("ZAKOŃCZ TURĘ", color = Color.Gray)
                            }
                        }
                    } else {
                        Button(
                            onClick = { (context as? ComponentActivity)?.finish() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text(if (isMedicOnly) "WRÓĆ DO MIASTA" else "ZAKOŃCZ WALKĘ", fontSize = 16.sp)
                        }
                    }
                }

                if (isGameOverAnimation) {
                    var showText by remember { mutableStateOf(false) }
                    var showImage by remember { mutableStateOf(false) }
                    val alphaImage = remember { Animatable(0f) }
                    val scaleImage = remember { Animatable(0.5f) }
                    val alphaText = remember { Animatable(0f) }

                    LaunchedEffect(Unit) {
                        delay(500)
                        showImage = true
                        launch {
                            alphaImage.animateTo(1f, animationSpec = tween(2000))
                        }
                        launch {
                            scaleImage.animateTo(
                                1.2f,
                                animationSpec = tween(3000, easing = FastOutSlowInEasing)
                            )
                        }
                        delay(1500)
                        showText = true
                        alphaText.animateTo(1f, animationSpec = tween(1500))

                        delay(5000)
                        val intent = Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra("EXTRA_MENU_APP", 0)
                        }
                        context.startActivity(intent)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.9f))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (showImage) {
                                Image(
                                    painter = painterResource(id = R.drawable.jokern),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(200.dp)
                                        .scale(scaleImage.value)
                                        .alpha(alphaImage.value)
                                )
                            }
                            Spacer(modifier = Modifier.height(48.dp))
                            if (showText) {
                                Text(
                                    text = "Umarłeś",
                                    color = Color.Red,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.alpha(alphaText.value)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
