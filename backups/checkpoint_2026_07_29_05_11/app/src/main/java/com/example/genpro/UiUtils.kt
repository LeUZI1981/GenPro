package com.example.genpro

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun applyStatTraining(heroData: String, stat: String, cost: Int): String {
    val lines = heroData.lines().toMutableList()
    
    // Deduct PD
    val pdIdx = lines.indexOfFirst { it.startsWith("PD:") }
    if (pdIdx != -1) {
        val currentPd = lines[pdIdx].substringAfter(":").trim().toIntOrNull() ?: 0
        lines[pdIdx] = "PD: ${currentPd - cost}"
    }

    // Update primary stat
    val statIdx = lines.indexOfFirst { it.startsWith("$stat:") }
    if (statIdx != -1) {
        val currentVal = lines[statIdx].substringAfter(":").trim().toIntOrNull() ?: 0
        val newVal = currentVal + 1
        lines[statIdx] = "$stat: $newVal"
        
        // Secondary stat mechanics: tens digit rule
        if (stat == "K" || stat == "Krzepa") updateSecondaryStat(lines, "S", newVal / 10)
        if (stat == "O" || stat == "Odporność") updateSecondaryStat(lines, "Wt", newVal / 10)
    }

    return lines.joinToString("\n")
}

fun findHeroSkill(heroData: String, skillName: String): Pair<Int, String>? {
    val lines = heroData.lines()
    val skillsHeaderIdx = lines.indexOfFirst { it.startsWith("UMIEJĘTNOŚCI (GRUPY):") }
    if (skillsHeaderIdx == -1) return null
    
    val skillRegex = Regex("•\\s+${Regex.escape(skillName)}(\\s+\\((.*)\\))?\\s+\\|\\s+PN:\\s+(\\d+)")
    
    for (i in skillsHeaderIdx + 1 until lines.size) {
        if (lines[i].startsWith("CECHY FIZYCZNE:") || lines[i].startsWith("CECHY CHARAKTERU:")) break
        val match = skillRegex.find(lines[i])
        if (match != null) {
            val rank = match.groupValues[2].ifEmpty { "Brak" }
            val pn = match.groupValues[3].toIntOrNull() ?: 0
            return Pair(pn, rank)
        }
    }
    return null
}

fun addPNToSkill(heroData: String, skillName: String, statName: String, pnToAdd: Int): String {
    val updatedLines = heroData.lines().toMutableList()
    val skillsHeaderIdx = updatedLines.indexOfFirst { it.startsWith("UMIEJĘTNOŚCI (GRUPY):") }
    if (skillsHeaderIdx == -1) return heroData

    val skillRegex = Regex("•\\s+${Regex.escape(skillName)}(\\s+\\((.*)\\))?\\s+\\|\\s+PN:\\s+(\\d+)")
    var found = false
    
    for (i in skillsHeaderIdx + 1 until updatedLines.size) {
        if (updatedLines[i].startsWith("CECHY FIZYCZNE:") || updatedLines[i].startsWith("CECHY CHARAKTERU:")) break
        val match = skillRegex.find(updatedLines[i])
        if (match != null) {
            val currentPn = match.groupValues[3].toIntOrNull() ?: 0
            if (currentPn >= 100) {
                found = true
                break
            }
            val newPn = (currentPn + pnToAdd).coerceAtMost(100)
            val line = updatedLines[i]
            val groupRange = match.groups[3]!!.range
            updatedLines[i] = line.substring(0, groupRange.first) + newPn + line.substring(groupRange.last + 1)
            found = true
            break
        }
    }

    if (!found) {
        val fileCatName = when(statName) {
            "WW" -> "Walka Wręcz"
            "US" -> "Umiejętności Strzeleckie"
            "Int", "Inteligencja" -> "Inteligencja"
            "SW" -> "Siła Woli"
            "Cha" -> "Charyzma"
            "Zr" -> "Zręczność"
            "Wiedza" -> "Wiedza"
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
    }
    return updatedLines.joinToString("\n")
}

fun addSkill(heroData: String, skillName: String, cost: Int): String {
    val lines = heroData.lines().toMutableList()
    
    // Deduct PD
    val pdIdx = lines.indexOfFirst { it.startsWith("PD:") }
    if (pdIdx != -1) {
        val currentPd = lines[pdIdx].substringAfter(":").trim().toIntOrNull() ?: 0
        lines[pdIdx] = "PD: ${currentPd - cost}"
    }

    // Add skill
    val skillSectionIdx = lines.indexOfFirst { it.trim() == "UMIEJĘTNOŚCI:" }
    if (skillSectionIdx != -1) {
        val skills = mutableListOf<String>()
        var i = skillSectionIdx + 1
        while (i < lines.size && lines[i].startsWith("•")) {
            skills.add(lines[i])
            i++
        }
        
        val newSkillEntry = "• $skillName"
        if (!skills.contains(newSkillEntry)) {
            skills.add(newSkillEntry)
            skills.sort()
            
            // Remove old skills
            repeat(i - (skillSectionIdx + 1)) {
                lines.removeAt(skillSectionIdx + 1)
            }
            // Insert sorted skills
            lines.addAll(skillSectionIdx + 1, skills)
        }
    }

    return lines.joinToString("\n")
}

fun getHeroStrength(heroData: String): Int = HeroLogic.getHeroStrength(heroData)

fun getHeroStrengthBonus(heroData: String): Int = HeroLogic.getHeroStrengthBonus(heroData)

fun getHeroToughness(heroData: String): Int = HeroLogic.getHeroToughness(heroData)

fun getHeroToughnessBonus(heroData: String): Int = HeroLogic.getHeroToughnessBonus(heroData)

fun getHeroWeightLimit(heroData: String): Int = HeroLogic.getHeroWeightLimit(heroData)

// Data classes and Enums moved to CommonLogic.kt

fun getEncumbrance(current: Int, limit: Int): EncumbranceLevel {
    return HeroLogic.getEncumbrance(current, limit)
}

fun getItemWeight(name: String): Int = HeroLogic.getItemWeight(name)

fun updateSecondaryStat(lines: MutableList<String>, statName: String, newValue: Int) {
    val idx = lines.indexOfFirst { it.startsWith("$statName:") }
    if (idx != -1) {
        lines[idx] = "$statName: $newValue"
    }
}

fun deductMoney(heroData: String, costCopper: Int): String {
    return HeroLogic.deductMoney(heroData, costCopper)
}

fun calculateCurrentWeight(heroData: String): Int {
    return HeroLogic.calculateCurrentWeight(heroData)
}

// Removed ArmorPiece and CombatWeapon as they are in CommonLogic.kt

data class CriticalInjury(
    val name: String,
    val description: String,
    val severity: Int, // 1-10+
    val location: String,
    val stat: String? = null,
    val penalty: Int = 0,
    val isFatal: Boolean = false
)

val criticalInjuriesTable = listOf(
    // GŁOWA
    CriticalInjury("Ogłuszenie", "Tracisz 1 akcję w następnej turze.", 1, "Głowa"),
    CriticalInjury("Rozcięty łuk brwiowy", "Krew zalewa oczy. -10 do WW.", 3, "Głowa", "Walka Wręcz", -10),
    CriticalInjury("Wstrząśnienie mózgu", "Świat wiruje. -20 do Inteligencji.", 6, "Głowa", "Inteligencja", -20),
    CriticalInjury("Strzaskana szczęka", "Ból nie do zniesienia. Tracisz 5 PŻ.", 8, "Głowa", "PŻ", -5),
    CriticalInjury("Śmierć na miejscu", "Głowa oddziela się od reszty ciała.", 10, "Głowa", isFatal = true),
    
    // KORPUS
    CriticalInjury("Zaparcie tchu", "Tracisz następną akcję.", 1, "Korpus"),
    CriticalInjury("Stłuczone żebra", "Każdy ruch boli. -10 do Zręczności.", 4, "Korpus", "Zręczność", -10),
    CriticalInjury("Poważna rana brzucha", "Krwotok wewnętrzny. Wytrzymałość -10.", 7, "Korpus", "Wytrzymałość", -10),
    CriticalInjury("Przebite płuco", "Trudność w oddychaniu. Krzepa -20.", 9, "Korpus", "Krzepa", -20),
    CriticalInjury("Zniszczony organ", "Padłeś i nie powstajesz.", 11, "Korpus", isFatal = true),

    // RĘCE
    CriticalInjury("Zdrętwienie", "Upuszczasz trzymany przedmiot.", 2, "Ręka"),
    CriticalInjury("Złamana ręka", "Ręka wisi bezwładnie. Nie możesz jej używać.", 7, "Ręka", "Walka Wręcz", -20),
    
    // NOGI
    CriticalInjury("Skręcona kostka", "Ruch zmniejszony o połowę.", 3, "Noga"),
    CriticalInjury("Złamana noga", "Upadasz. -30 do uników i ruchu.", 8, "Noga", "Zręczność", -30)
)

fun getQualityColor(quality: Int): Color {
    return when {
        quality <= 20 -> Color(0xFFB71C1C) // Bardzo słaba - Czerwony
        quality <= 50 -> Color(0xFFEF6C00) // Słaba - Pomarańczowy
        quality <= 75 -> Color(0xFFFDD835) // Przeciętna - Żółty
        quality <= 90 -> Color(0xFF4CAF50) // Dobra - Zielony
        else -> Color(0xFF00E676)          // Wyjątkowa - Jasny zielony
    }
}

fun getQualityName(quality: Int, itemName: String): String {
    if (quality <= 10) return "Złom"
    
    val gender = determineItemGender(itemName)
    return when {
        quality <= 20 -> when(gender) {
            "M" -> "Zniszczony"
            "F" -> "Zniszczona"
            "PL" -> "Zniszczone" // Explicit PL handling
            else -> "Zniszczone"
        }
        quality <= 50 -> when(gender) {
            "M" -> "Znoszony"
            "F" -> "Znoszona"
            "PL" -> "Znoszone"
            else -> "Znoszone"
        }
        quality <= 90 -> when(gender) {
            "M" -> "Dobry"
            "F" -> "Dobra"
            "PL" -> "Dobre"
            else -> "Dobre"
        }
        else -> when(gender) {
            "M" -> "Wyjątkowy"
            "F" -> "Wyjątkowa"
            "PL" -> "Wyjątkowe"
            else -> "Wyjątkowe"
        }
    }
}

fun determineItemGender(name: String): String {
    return HeroLogic.determineItemGender(name)
}

// Skill and stats logic remains in UiUtils for now as it relies heavily on parsing character file strings
// Business data and static arrays moved to HeroLogic in CommonLogic.kt

fun getSkillUpgradeCost(nextRankIndex: Int): Int {
    return when (nextRankIndex) {
        1 -> 100  // Do Uczeń
        2 -> 200  // Do Adept
        3 -> 400  // Do Ekspert
        4 -> 600  // Do Mistrz
        5 -> 1000 // Do Arcymistrz
        else -> 100
    }
}

fun getRankWeight(rank: String): Int {
    return HeroLogic.getRankWeight(rank)
}

fun getPnCost(rankWeight: Int): Int {
    return when (rankWeight) {
        0 -> 1 // Brak -> Uczeń
        1 -> 2 // Uczeń -> Adept
        2 -> 4 // Adept -> Ekspert
        3 -> 6 // Ekspert -> Mistrz
        4 -> 10 // Mistrz -> Arcymistrz
        else -> 1
    }
}

fun getPnRequiredForNextRank(nextRankIndex: Int): Int {
    return 100
}

fun getFancyRank(rank: String): String {
    val weight = getRankWeight(rank)
    return when (weight) {
        5 -> "𝕬𝖗𝖈𝖞𝖒𝖎𝖘𝖙𝖗𝖟"
        4 -> "𝕸𝖎𝖘𝖙𝖗𝖟"
        3 -> "𝕰𝖐𝖘𝖕𝖊𝖗𝖙"
        2 -> "𝕬𝖉𝖊𝖕𝖙"
        1 -> "𝖀𝖈𝖟𝖊𝖓"
        else -> "𝓑𝓻𝓪𝓴"
    }
}

fun getFancyRankName(rank: String): String = getFancyRank(rank)

fun normalizeRank(rank: String): String {
    val r = rank.trim()
    if (r == "Brak" || r == "𝓑𝓻𝓪𝓴" || r.isEmpty()) return "Brak"
    return when (getRankWeight(r)) {
        5 -> "Arcymistrz"
        4 -> "Mistrz"
        3 -> "Ekspert"
        2 -> "Adept"
        1 -> "Uczeń"
        else -> "Brak"
    }
}

data class LevelRequirements(
    val lvlUmax: Int,
    val lvlAmax: Int,
    val lvlEmax: Int,
    val lvlMmax: Int,
    val lvlAMax: Int
)

data class BodyArmor(
    val pieces: MutableList<ArmorPiece> = mutableListOf()
) {
    fun getProt(location: String, type: DamageType): Int {
        val piecesInLocation = pieces.filter { piece ->
            piece.locations.any { loc ->
                location.contains(loc, true) ||
                        (loc == "Ramiona" && location.contains("Ręka", true)) ||
                        (loc == "Nogi" && location.contains("Noga", true))
            }
        }

        // Use centralized elementalProt logic from ArmorPiece
        val physicalProt = when (type) {
            DamageType.SIECZNA -> piecesInLocation.sumOf { it.ppS }
            DamageType.OBUCHOWA -> piecesInLocation.sumOf { it.ppO }
            DamageType.KLUTA -> piecesInLocation.sumOf { it.ppK }
            else -> 0 // Elements handled below
        }

        val elementalProt = piecesInLocation.sumOf { piece -> piece.getElementalProt(type) }

        return physicalProt + elementalProt
    }
}

class BodyParts(
    head: Int,
    torso: Int,
    rightArm: Int,
    leftArm: Int,
    rightLeg: Int,
    leftLeg: Int,
    val maxHead: Int,
    val maxTorso: Int,
    val maxRArm: Int,
    val maxLArm: Int,
    val maxRLeg: Int,
    val maxLLeg: Int
) {
    var head by mutableIntStateOf(head)
    var torso by mutableIntStateOf(torso)
    var rightArm by mutableIntStateOf(rightArm)
    var leftArm by mutableIntStateOf(leftArm)
    var rightLeg by mutableIntStateOf(rightLeg)
    var leftLeg by mutableIntStateOf(leftLeg)

    constructor(valAll: Int, maxAll: Int) : this(
        valAll, valAll, valAll, valAll, valAll, valAll,
        maxAll, maxAll, maxAll, maxAll, maxAll, maxAll
    )

    fun getHpFor(location: String): Int = when {
        location.contains("Głowa", true) -> head
        location.contains("Ręka", true) -> if (location.contains("Lewa", true)) leftArm else rightArm
        location.contains("Noga", true) -> if (location.contains("Lewa", true)) leftLeg else rightLeg
        else -> torso
    }

    fun getMaxFor(location: String): Int = when {
        location.contains("Głowa", true) -> maxHead
        location.contains("Ręka", true) -> if (location.contains("Lewa", true)) maxLArm else maxRArm
        location.contains("Noga", true) -> if (location.contains("Lewa", true)) maxLLeg else maxRLeg
        else -> maxTorso
    }

    fun applyDamage(location: String, damage: Int): Int {
        val currentLimbHp = getHpFor(location)

        when {
            location.contains("Głowa", true) -> head -= damage
            location.contains("Ręka", true) -> if (location.contains("Lewa", true)) leftArm -= damage else rightArm -= damage
            location.contains("Noga", true) -> if (location.contains("Lewa", true)) leftLeg -= damage else rightLeg -= damage
            else -> torso -= damage
        }

        // Limb damage overflow to torso
        if (!location.contains("Korpus", true) && !location.contains("Głowa", true)) {
            val overflow = damage - maxOf(0, currentLimbHp)
            if (overflow > 0) {
                torso -= overflow
            }
        }
        return getHpFor(location)
    }
}

class Enemy(
    val id: Int,
    val name: String,
    val bodyParts: BodyParts,
    var ww: Int,
    var s: Int,
    var toughness: Int,
    var agility: Int,
    val ap: Int = 0,
    var weaponDmg: Int = 3,
    var weaponName: String = "Broń",
    var damageType: DamageType = DamageType.SIECZNA,
    val exp: Int = 10,
    val traits: List<EnemyTrait> = emptyList(),
    var isPoisoned: Boolean = false,
    var isBlinded: Boolean = false,
    var isBurning: Boolean = false,
    var isFrozen: Boolean = false,
    var isShocked: Boolean = false,
    var distance: Int = 10,
    val injuries: MutableList<String> = mutableStateListOf(),
    var isGrabbedBy: String? = null,
    var initiative: Int = 0,
    var isLoaded: Boolean = true
) {
    val isDead: Boolean get() = bodyParts.torso < 0 || bodyParts.head < 0

    fun getProt(type: DamageType): Int {
        if (ap <= 0) return 0
        return when (type) {
            DamageType.SIECZNA -> ap + 1
            DamageType.OBUCHOWA -> maxOf(0, ap - 1)
            else -> ap
        }
    }
}

data class Ally(
    val name: String,
    val bodyParts: BodyParts,
    val ww: Int,
    val damage: String,
    val toughness: Int,
    val agility: Int,
    val speed: Int,
    var trainingLevel: Int = 0,
    var isFled: Boolean = false,
    var hasActed: Boolean = false,
    var initiative: Int = 0
)

fun getHitLocation(roll: Int): String {
    val s = roll.toString().padStart(2, '0')
    val reversed = (s[1].toString() + s[0].toString()).toInt()
    val loc = if (reversed == 0) 100 else reversed
    return when (loc) {
        in 1..15 -> "Głowa"
        in 16..35 -> "Prawa Ręka"
        in 36..55 -> "Lewa Ręka"
        in 56..80 -> "Korpus"
        in 81..90 -> "Prawa Noga"
        else -> "Lewa Noga"
    }
}

fun getLevelRequirements(level: Int): LevelRequirements {
    return when {
        level <= 0 -> LevelRequirements(10, 0, 0, 0, 0)
        level == 1 -> LevelRequirements(15, 1, 0, 0, 0)
        level == 2 -> LevelRequirements(19, 2, 0, 0, 0)
        level == 3 -> LevelRequirements(22, 3, 0, 0, 0)
        level == 4 -> LevelRequirements(25, 5, 1, 0, 0)
        level == 5 -> LevelRequirements(28, 8, 2, 0, 0)
        level == 6 -> LevelRequirements(32, 10, 3, 1, 0)
        level == 7 -> LevelRequirements(35, 12, 5, 3, 0)
        level == 8 -> LevelRequirements(35, 15, 8, 5, 1)
        level == 9 -> LevelRequirements(35, 20, 11, 7, 3)
        level == 10 -> LevelRequirements(35, 25, 15, 10, 5)
        else -> LevelRequirements(35, 30, 20, 15, 10)
    }
}

data class StatCaps(
    val plusMaxWW: Int = 0, val plusMaxUS: Int = 0, val plusMaxKrz: Int = 0, val plusMaxOdp: Int = 0,
    val plusMaxZr: Int = 0, val plusMaxInt: Int = 0, val plusMaxSW: Int = 0, val plusMaxCha: Int = 0,
    val plusMaxAtk: Int = 0, val plusMaxSz: Int = 0, val plusMaxZw: Int = 0, val plusMaxMag: Int = 0
)

fun getStatCaps(klasa: String, level: Int): StatCaps {
    return when (klasa) {
        "Wojownik" -> when {
            level >= 10 -> StatCaps(8, 5, 7, 6, 7, 6, 6, 5, 3, 2, 9)
            level == 9 -> StatCaps(7, 4, 6, 6, 6, 5, 5, 5, 3, 2, 7)
            level == 8 -> StatCaps(6, 3, 6, 5, 5, 4, 5, 4, 3, 1, 6)
            level == 7 -> StatCaps(5, 2, 5, 4, 5, 4, 4, 3, 3, 1, 5)
            level == 6 -> StatCaps(5, 2, 4, 4, 4, 3, 3, 2, 2, 1, 4)
            level == 5 -> StatCaps(4, 1, 4, 3, 3, 2, 3, 2, 2, 1, 3)
            level == 4 -> StatCaps(4, 0, 3, 2, 3, 1, 2, 1, 1, 1, 2)
            level == 3 -> StatCaps(3, 0, 2, 2, 2, 1, 1, 1, 1, 0, 2)
            level == 2 -> StatCaps(2, 0, 1, 1, 2, 0, 1, 0, 1, 0, 1)
            level == 1 -> StatCaps(1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0)
            else -> StatCaps()
        }
        "Łotr" -> when {
            level >= 10 -> StatCaps(6, 6, 5, 6, 8, 7, 6, 7, 2, 3, 9)
            level == 9 -> StatCaps(6, 5, 4, 5, 7, 6, 5, 6, 2, 3, 8)
            level == 8 -> StatCaps(5, 4, 3, 4, 6, 5, 5, 6, 2, 2, 7)
            level == 7 -> StatCaps(5, 4, 2, 3, 5, 4, 4, 5, 1, 2, 6)
            level == 6 -> StatCaps(4, 4, 2, 2, 5, 3, 3, 4, 1, 2, 5)
            level == 5 -> StatCaps(3, 3, 2, 1, 4, 3, 2, 4, 1, 1, 4)
            level == 4 -> StatCaps(3, 2, 1, 0, 4, 2, 1, 3, 1, 1, 3)
            level == 3 -> StatCaps(2, 1, 1, 0, 3, 2, 1, 2, 0, 1, 2)
            level == 2 -> StatCaps(1, 1, 0, 0, 2, 1, 0, 2, 0, 0, 2)
            level == 1 -> StatCaps(0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1)
            else -> StatCaps()
        }
        "Łowca" -> when {
            level >= 10 -> StatCaps(6, 8, 6, 6, 7, 7, 5, 5, 2, 3, 9)
            level == 9 -> StatCaps(5, 7, 5, 6, 6, 6, 5, 4, 2, 3, 8)
            level == 8 -> StatCaps(5, 6, 4, 6, 5, 5, 4, 3, 2, 2, 7)
            level == 7 -> StatCaps(4, 5, 4, 5, 5, 4, 3, 2, 2, 2, 7)
            level == 6 -> StatCaps(3, 4, 3, 4, 5, 4, 2, 2, 2, 1, 6)
            level == 5 -> StatCaps(3, 3, 2, 3, 4, 4, 2, 1, 1, 1, 5)
            level == 4 -> StatCaps(2, 2, 1, 3, 4, 3, 1, 0, 1, 1, 4)
            level == 3 -> StatCaps(1, 2, 1, 2, 3, 2, 1, 0, 1, 0, 3)
            level == 2 -> StatCaps(1, 1, 0, 1, 2, 2, 0, 0, 0, 0, 2)
            level == 1 -> StatCaps(0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 1)
            else -> StatCaps()
        }
        "Uczony" -> when {
            level >= 10 -> StatCaps(6, 5, 5, 6, 6, 8, 7, 7, 1, 3, 7)
            level == 9 -> StatCaps(5, 5, 4, 6, 5, 7, 6, 6, 1, 3, 6)
            level == 8 -> StatCaps(4, 4, 3, 5, 5, 6, 6, 5, 1, 2, 5)
            level == 7 -> StatCaps(4, 3, 2, 4, 4, 5, 5, 5, 0, 2, 6)
            level == 6 -> StatCaps(3, 2, 2, 3, 4, 5, 4, 4, 0, 2, 5)
            level == 5 -> StatCaps(2, 1, 2, 3, 3, 4, 4, 3, 0, 1, 4)
            level == 4 -> StatCaps(1, 0, 1, 3, 2, 4, 3, 2, 0, 1, 3)
            level == 3 -> StatCaps(1, 0, 1, 2, 2, 3, 2, 1, 0, 0, 3)
            level == 2 -> StatCaps(0, 0, 0, 1, 1, 2, 2, 1, 0, 0, 2)
            level == 1 -> StatCaps(0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 1)
            else -> StatCaps()
        }
        "Mag" -> when {
            level >= 10 -> StatCaps(6, 5, 5, 6, 6, 8, 7, 7, 1, 3, 7, 5)
            level == 9 -> StatCaps(5, 5, 4, 6, 5, 7, 6, 6, 1, 3, 6, 5)
            level == 8 -> StatCaps(4, 4, 3, 5, 5, 6, 6, 5, 1, 2, 5, 4)
            level == 7 -> StatCaps(4, 3, 2, 4, 4, 5, 5, 5, 0, 2, 6, 4)
            level == 6 -> StatCaps(3, 2, 2, 3, 4, 5, 4, 4, 0, 2, 5, 3)
            level == 5 -> StatCaps(2, 1, 2, 3, 3, 4, 4, 3, 0, 1, 4, 3)
            level == 4 -> StatCaps(1, 0, 1, 3, 2, 4, 3, 2, 0, 1, 4, 2)
            level == 3 -> StatCaps(1, 0, 1, 2, 2, 3, 2, 1, 0, 0, 3, 2)
            level == 2 -> StatCaps(0, 0, 0, 1, 1, 2, 2, 1, 0, 0, 2, 1)
            level == 1 -> StatCaps(0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 1)
            else -> StatCaps()
        }
        "Kapłan" -> when {
            level >= 10 -> StatCaps(3, 4, 4, 5, 7, 7, 6, 8, 1, 2, 5, 4)
            level == 9 -> StatCaps(3, 3, 4, 4, 6, 6, 5, 6, 1, 2, 5, 4)
            level == 8 -> StatCaps(3, 3, 3, 4, 6, 5, 4, 6, 1, 2, 4, 3)
            level == 7 -> StatCaps(2, 3, 2, 3, 5, 4, 4, 5, 0, 2, 4, 3)
            level == 6 -> StatCaps(2, 2, 2, 3, 4, 4, 3, 5, 0, 2, 4, 2)
            level == 5 -> StatCaps(1, 2, 1, 2, 4, 3, 3, 4, 0, 1, 4, 2)
            level == 4 -> StatCaps(0, 2, 1, 2, 3, 3, 2, 4, 0, 1, 3, 1)
            level == 3 -> StatCaps(0, 1, 0, 1, 2, 2, 0, 3, 0, 0, 0, 1)
            level == 2 -> StatCaps(0, 0, 0, 1, 2, 1, 1, 2, 0, 0, 2, 0)
            level == 1 -> StatCaps(0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0)
            else -> StatCaps()
        }
        "Bard" -> when {
            level >= 10 -> StatCaps(5, 5, 4, 5, 8, 7, 6, 9, 2, 3, 7)
            level == 9 -> StatCaps(5, 4, 4, 5, 7, 6, 5, 8, 2, 3, 6)
            level == 8 -> StatCaps(4, 4, 3, 4, 6, 5, 5, 8, 2, 2, 5)
            level == 7 -> StatCaps(4, 3, 2, 3, 5, 4, 4, 7, 1, 2, 5)
            level == 6 -> StatCaps(3, 2, 2, 2, 5, 3, 3, 6, 1, 2, 4)
            level == 5 -> StatCaps(3, 1, 2, 1, 4, 3, 2, 5, 1, 1, 4)
            level == 4 -> StatCaps(2, 0, 1, 0, 4, 2, 1, 4, 1, 1, 3)
            level == 3 -> StatCaps(2, 0, 0, 0, 3, 2, 1, 3, 0, 1, 2)
            level == 2 -> StatCaps(1, 0, 0, 0, 2, 1, 0, 2, 0, 0, 2)
            level == 1 -> StatCaps(0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1)
            else -> StatCaps()
        }
        "Kowal Run" -> when {
            level >= 10 -> StatCaps(7, 4, 7, 8, 5, 8, 8, 4, 2, 1, 8, 3)
            level == 9 -> StatCaps(6, 4, 6, 7, 5, 7, 7, 4, 2, 1, 7, 3)
            level == 8 -> StatCaps(5, 3, 6, 6, 4, 6, 7, 3, 2, 0, 6, 2)
            level == 7 -> StatCaps(5, 2, 5, 5, 4, 5, 6, 3, 1, 0, 5, 2)
            level == 6 -> StatCaps(4, 2, 4, 5, 3, 5, 5, 2, 1, 0, 5, 1)
            level == 5 -> StatCaps(4, 1, 4, 4, 3, 4, 5, 2, 1, 0, 4, 1)
            level == 4 -> StatCaps(3, 0, 3, 3, 2, 4, 4, 1, 1, 0, 3)
            level == 3 -> StatCaps(2, 0, 2, 3, 2, 3, 3, 0, 0, 0, 3)
            level == 2 -> StatCaps(2, 0, 1, 2, 1, 2, 2, 0, 0, 0, 2)
            level == 1 -> StatCaps(1, 0, 1, 1, 0, 1, 1, 0, 0, 0, 1)
            else -> StatCaps()
        }
        else -> StatCaps()
    }
}

data class ClassModifiers(
    val mod1: String = "",
    val mod2: String = "",
    val mod3: String = "",
    val mod4: String = "",
    val mod5: String = ""
)

fun getClassModifiers(klasa: String, level: Int): ClassModifiers {
    var m1 = ""
    var m2 = ""
    var m3 = ""
    var m4 = ""
    var m5 = if (level > 5) "Wiedza" else ""
    if (level > 8) m5 = "Siła Woli"

    when (klasa) {
        "Wojownik" -> {
            m1 = "Walka Wręcz"
            m2 = "Zręczność"
            if (level > 5) {
                m3 = "Krzepa"
                m4 = "Odporność"
                m5 = "Siła Woli"
            }
        }
        "Łotr" -> {
            m1 = "Zręczność"
            m2 = "Charyzma"
            if (level > 5) {
                m3 = "Inteligencja"
                m4 = "Odporność"
            }
        }
        "Łowca" -> {
            m1 = "US"
            m2 = "Zręczność"
            if (level > 5) {
                m3 = "Inteligencja"
                m4 = "Odporność"
            }
        }
        "Uczony" -> {
            m1 = "Inteligencja"
            m2 = "Zręczność"
            if (level > 5) {
                m3 = "Odporność"
                m4 = "Siła Woli"
            }
        }
        "Kapłan" -> {
            m1 = "Charyzma"
            m2 = "Siła Woli"
            if (level > 5) {
                m3 = "Inteligencja"
            }
        }
        "Guślarz", "Uczeń Maga", "Czarownik", "Mag", "Czarnoksiężnik", "Czarodziej" -> {
            m1 = "Siła Woli"
            m2 = "Inteligencja"
            if (level > 5) {
                m3 = "Zręczność"
                m4 = "Wiedza"
            }
        }
        "Bard" -> {
            m1 = "Charyzma"
            m2 = "Zręczność"
            if (level > 5) {
                m3 = "Inteligencja"
                m4 = "Pieśń"
            }
        }
        "Kowal Run" -> {
            if (level > 5) {
                m1 = "Krzepa" // Przypisanie z else dla spójności
                m2 = "Zręczność"
                m3 = "Inteligencja"
                m4 = "Siła Woli"
            } else {
                m1 = "Krzepa"
                m2 = "Zręczność"
            }
        }
    }
    return ClassModifiers(m1, m2, m3, m4, m5)
}

data class BodyPartHP(
    val head: Int,
    val torso: Int,
    val rArm: Int,
    val lArm: Int,
    val rLeg: Int,
    val lLeg: Int
)

fun calculateBodyPartHP(toughness: Int): BodyPartHP {
    return BodyPartHP(
        head = (toughness * 0.75).toInt(),
        torso = (toughness * 1.5).toInt(),
        rArm = toughness,
        lArm = toughness,
        rLeg = (toughness * 1.1).toInt(),
        lLeg = (toughness * 1.1).toInt()
    )
}

val allNormalSkills = HeroLogic.allNormalSkills

val skillPrerequisites = mapOf(
    "Chemia" to "Matematyka",
    "Fizyka" to "Matematyka",
    "Walka na wierzchowcu" to "Jeździectwo"
)

private var musicPlayer: MediaPlayer? = null

fun stopMusic() {
    try {
        musicPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        musicPlayer = null
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun playSound(context: Context, scope: CoroutineScope, resId: Int, part: Int, offsetMs: Int = 0) {
    val soundsOn = try {
        context.openFileInput("sounds_setting").bufferedReader().use { it.readText() } != "off"
    } catch (e: Exception) {
        true
    }

    val musicOn = try {
        context.openFileInput("music_setting").bufferedReader().use { it.readText() } != "off"
    } catch (e: Exception) {
        true
    }

    val isMusic = resId == R.raw.fanmusic
    if (isMusic && !musicOn) {
        if (isMusic) stopMusic()
        return
    }
    if (!isMusic && !soundsOn) return

    try {
        if (isMusic) {
            stopMusic()
            musicPlayer = MediaPlayer.create(context, resId)
            val mp = musicPlayer ?: return
            mp.isLooping = true
            val duration = mp.duration
            if (offsetMs > 0) {
                mp.seekTo(offsetMs.coerceAtMost(duration))
            }
            mp.start()
            return
        }

        val mp = MediaPlayer.create(context, resId) ?: return
        val duration = mp.duration
        
        if (offsetMs > 0) {
            mp.seekTo(offsetMs.coerceAtMost(duration))
        }

        when (part) {
            1 -> {
                mp.start()
                scope.launch {
                    delay((duration / 2).toLong())
                    try {
                        if (mp.isPlaying) mp.stop()
                    } catch (_: Exception) {
                    } finally {
                        mp.release()
                    }
                }
            }
            2 -> {
                if (offsetMs == 0) mp.seekTo(duration / 2)
                mp.setOnCompletionListener { it.release() }
                mp.start()
            }
            else -> {
                mp.setOnCompletionListener { it.release() }
                mp.start()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

val colorMap = mapOf(
    0 to Color(0xFF2E7D32),   // 00 - Ciemnozielony (Sukces)
    2 to Color(0xFF2E7D32),   // 02 - Zielony
    10 to Color(0xFF4CAF50),  // 11 - Jasnozielony
    20 to Color(0xFF8BC34A),  // 22 - Limonkowy
    30 to Color(0xFFCDDC39),  // 33 - Żółto-limonkowy
    40 to Color(0xFFFFEB3B),  // 44 - Żółty
    50 to Color(0xFFFFC107),  // 55 - Bursztynowy
    60 to Color(0xFFFF9800),  // 66 - Pomarańczowy
    70 to Color(0xFFF44336),  // 77 - Jasnoczerwony
    80 to Color(0xFFD32F2F),  // 88 - Czerwony
    88 to Color(0xFFD32F2F),  // 88 - Czerwony
    90 to Color(0xFFB71C1C),  // 99 - Bardzo ciemna czerwień (Krytyczna porażka)
    99 to Color(0xFFB71C1C)   // 99 - Porażka
)


fun getK100Color(roll: Int): Color {
    val r: Float
    val g: Float
    val b = 0f
    
    if (roll <= 50) {
        // Zielony (0, 1, 0) do Żółty (1, 1, 0)
        r = (roll - 1) / 49f
        g = 1f
    } else {
        // Żółty (1, 1, 0) do Czerwony (1, 0, 0)
        r = 1f
        g = 1f - (roll - 50) / 50f
    }
    return Color(r, g, b)
}

fun getGenericDiceColor(roll: Int, max: Int): Color {
    val r: Float
    val g: Float
    val m = max.toFloat().coerceAtLeast(1f)
    val half = m / 2f
    
    if (roll <= half) {
        // Czerwony (1, 0, 0) do Żółty (1, 1, 0)
        r = 1f
        g = if (half > 1f) ((roll - 1f) / (half - 1f)).coerceIn(0f, 1f) else 1f
    } else {
        // Żółty (1, 1, 0) do Zielony (0, 1, 0)
        r = if (m > half) (1f - (roll - half) / (m - half)).coerceIn(0f, 1f) else 0f
        g = 1f
    }
    return Color(r, g, 0f)
}

fun colorize(text: String, heroMode: Boolean = false, heroStatValue: Int = 0, isMultiDice: Boolean = false, maxDieValue: Int = 0): AnnotatedString {
    var processedText = text
    var activeHeroMode = heroMode
    var activeHeroStatValue = heroStatValue
    var activeIsMultiDice = isMultiDice
    var activeMaxDieValue = maxDieValue

    // Decode metadata if present: |M:V| format
    if (processedText.contains("|") && processedText.indexOf("|") < processedText.lastIndexOf("|")) {
        val start = processedText.indexOf("|")
        val end = processedText.lastIndexOf("|")
        val meta = processedText.substring(start + 1, end)
        processedText = processedText.substring(0, start) + processedText.substring(end + 1)
        
        val parts = meta.split(":")
        when (parts[0]) {
            "D" -> {
                activeIsMultiDice = parts.getOrNull(1)?.toBoolean() ?: false
                activeMaxDieValue = parts.getOrNull(2)?.toInt() ?: 0
                activeHeroMode = false
            }
            "S" -> {
                activeHeroStatValue = parts.getOrNull(1)?.toInt() ?: 0
                activeHeroMode = true
            }
            "P" -> {
                activeHeroMode = false
                activeIsMultiDice = false
            }
        }
    }

    return buildAnnotatedString {
        if (processedText.startsWith("꧁")) {
            withStyle(style = SpanStyle(color = Color.Yellow, fontWeight = FontWeight.Bold)) {
                append(processedText)
            }
            return@buildAnnotatedString
        }

        var isTagActive = false
        val tagContent = StringBuilder()
        val normalText = StringBuilder()

        // For multi-dice mode, we need to count occurrences of dice results (excluding the sum)
        val repeatColors = listOf(Color.Yellow, Color(0xFF00FF00), Color.Red, Color.Cyan, Color.Magenta, Color(0xFFFFA500))
        val valueToColor = mutableMapOf<Int, Color>()

        if (activeIsMultiDice) {
            val regex = "‹(\\d+)›".toRegex()
            val allMatches = regex.findAll(processedText).toList()
            // The last match in multi-dice mode is always the sum in ﹝‹...›﹞
            val diceMatches = if (allMatches.size > 1) allMatches.dropLast(1) else allMatches
            val results = diceMatches.mapNotNull { it.groupValues[1].toIntOrNull() }
            val counts = results.groupingBy { it }.eachCount()
            val repeatingValues = counts.filter { it.value > 1 }.keys.toList().sorted()
            
            repeatingValues.forEachIndexed { index, value ->
                valueToColor[value] = repeatColors[index % repeatColors.size]
            }
        }

        var matchIndex = 0
        val regex = "‹(\\d+)›".toRegex()
        val allMatches = regex.findAll(processedText).toList()

        processedText.forEachIndexed { index, char ->
            when (char) {
                '‹' -> {
                    if (normalText.isNotEmpty()) {
                        withStyle(style = SpanStyle(color = Color.White)) {
                            append(normalText.toString())
                        }
                        normalText.setLength(0)
                    }
                    isTagActive = true
                }
                '›' -> {
                    isTagActive = false
                    val fullContent = tagContent.toString()
                    val parts = fullContent.split(":", limit = 2)
                    val colorPart = parts[0]
                    val displayText = if (parts.size > 1) parts[1] else ""
                    
                    val num = colorPart.toIntOrNull() ?: -1
                    
                    val color = if (num != -1) {
                        if (activeIsMultiDice) {
                            // Check if this specific match is the sum (the last one)
                            val isSum = (matchIndex == allMatches.size - 1 && allMatches.size > 1)
                            if (isSum) {
                                Color.White
                            } else {
                                valueToColor[num] ?: if (activeMaxDieValue > 0) {
                                    if (activeMaxDieValue == 100) getK100Color(num) else getGenericDiceColor(num, activeMaxDieValue)
                                } else Color.White
                            }
                        } else if (activeHeroMode && activeHeroStatValue > 0) {
                            val diff = activeHeroStatValue - num
                            when {
                                diff >= 0 -> Color(0xFF00FF00) // Zielony
                                diff > -30 -> Color.Yellow    // Żółty (niezdana o mniej niż 30)
                                else -> Color.Red             // Czerwony (niezdana o więcej niż 29)
                            }
                        } else if (colorMap.containsKey(num)) {
                            colorMap[num]!!
                        } else if (!activeHeroMode && num > 0 && num <= 100) {
                             if (activeMaxDieValue > 0 && !activeIsMultiDice) {
                                 getGenericDiceColor(num, activeMaxDieValue)
                             } else {
                                 getK100Color(num)
                             }
                        } else {
                            Color.Yellow
                        }
                    } else {
                        Color.White
                    }

                    // Jeśli mamy format ‹kod:tekst›, wyświetlamy tekst. 
                    // Jeśli tylko ‹liczba› (stary format), wyświetlamy liczbę dla kompatybilności.
                    val finalAppend = if (displayText.isNotEmpty()) displayText else if (num != -1) colorPart else ""

                    if (finalAppend.isNotEmpty()) {
                        withStyle(style = SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                            append(finalAppend)
                        }
                    }
                    tagContent.setLength(0)
                    matchIndex++
                }
                else -> {
                    if (isTagActive) {
                        tagContent.append(char)
                    } else {
                        normalText.append(char)
                    }
                }
            }
        }
        if (normalText.isNotEmpty()) {
            withStyle(style = SpanStyle(color = Color.White)) {
                append(normalText.toString())
            }
        }
    }
}
