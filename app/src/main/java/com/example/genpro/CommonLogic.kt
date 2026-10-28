package com.example.genpro

import androidx.compose.ui.graphics.Color
import com.example.genpro.logic.RestManager
import kotlinx.serialization.Serializable
import java.util.Calendar
import kotlin.random.Random

data class Shop(val name: String, val sub: String, val categories: List<String>, val description: String = "")

@Serializable
data class TradeItem(
    val name: String,
    val price: Int,
    val category: String,
    val quality: Int = 100,
    val buyPrice: Int = price,
    val sellPrice: Int = price / 2
)

enum class EncumbranceLevel(val label: String, val color: Color, val penalties: String) {
    LEKKIE("Lekkie", Color(0xFF2E7D32), "Brak kar"),
    OBCIAZONY("Obciążony", Color(0xFFFF9800), "-1 Sz, -5 Uniki"),
    PRZECIAZONY("Przeciążony", Color(0xFFF44336), "-2 Sz, -10 WW, -10 Uniki"),
    SKRAJNE("Skrajne", Color(0xFFB71C1C), "Ruch 1m, brak walki")
}

enum class ItemQuality(val m: String, val f: String, val pl: String) {
    NONE("", "", ""),
    ZNISZCZONY("Zniszczony", "Zniszczona", "Zniszczone"),
    USZKODZONY("Uszkodzony", "Uszkodzona", "Uszkodzone"),
    ZNOSZONY("Znoszony", "Znoszona", "Znoszone"),
    ZWYCZAJNY("Zwyczajny", "Zwyczajna", "Zwyczajne"),
    DOBRY("Dobry", "Dobra", "Dobre"),
    WYJATKOWY("Wyjątkowy", "Wyjątkowa", "Wyjątkowe");

    val displayName: String get() = m
}

enum class WeaponSuffix(val displayName: String, val elementalBonus: String = "", val elementalType: DamageType = DamageType.NONE) {
    NONE(""),
    OGNIA("Ognia", "1k4", DamageType.OGIEN),
    LODU("Lodu", "1k4", DamageType.ZIMNO),
    PIORUNA("Pioruna", "1k4", DamageType.PIORUN),
    TRUCIZNY("Trucizny", "1k4", DamageType.TRUCIZNA),
    ZYWIOLOW("Żywiołów", "2-5", DamageType.NONE),
    WAMPIRYZMU("Wampiryzmu", "", DamageType.NONE)
}

enum class WeaponPrefix(val m: String, val f: String, val pl: String, val wwBonus: Int = 0, val usBonus: Int = 0, val initBonus: Int = 0, val dodgeBonus: Int = 0, val krzepaBonus: Int = 0, val odpornoscBonus: Int = 0, val zrecznoscBonus: Int = 0, val silaWoliBonus: Int = 0) {
    NONE("", "", ""),
    MOCARNY("Mocarny", "Mocarna", "Mocarne", krzepaBonus = 5),
    HARTOWANY("Hartowany", "Hartowana", "Hartowane", odpornoscBonus = 5),
    ZWINNY("Zwinny", "Zwinna", "Zwinne", zrecznoscBonus = 5, dodgeBonus = 10),
    NIEZLOMNY("Niezłomny", "Niezłomna", "Niezłomne", silaWoliBonus = 5),
    LEKKI("Lekki", "Lekka", "Lekkie", initBonus = 5),
    WYWAZONY("Wyważony", "Wyważona", "Wyważone", wwBonus = 5, usBonus = 5),
    LUDZKI("Ludzki", "Ludzka", "Ludzkie"),
    ELFICKI("Elficki", "Elficka", "Elfickie"),
    KHAZADZKI("Khazadzki", "Khazadzka", "Khazadzkie");

    val displayName: String get() = m
}

enum class ArmorSuffix(val displayName: String, val fireProt: Int = 0, val coldProt: Int = 0, val lightningProt: Int = 0, val poisonProt: Int = 0, val vitalityBonus: Int = 0, val speedBonus: Int = 0) {
    NONE(""),
    ANTIDOTUM("Antidotum", poisonProt = 2),
    CIEPLA("Ciepła", coldProt = 2),
    SZRONU("Szronu", fireProt = 2),
    UZIEMIENIA("Uziemienia", lightningProt = 2),
    ZYCIA("Życia", vitalityBonus = 2),
    SZYBKOSCI("Szybkości", speedBonus = 1)
}

enum class ArmorPrefix(val m: String, val f: String, val pl: String) {
    NONE("", "", ""),
    LUDZKI("Ludzki", "Ludzka", "Ludzkie"),
    ELFICKI("Elficki", "Elficka", "Elfickie"),
    KHAZADZKI("Khazadzki", "Khazadzka", "Khazadzkie"),
    UNIWERSALNY("Uniwersalny", "Uniwersalna", "Uniwersalne"),
    PRZEKUTY("Przekuty", "Przekuta", "Przekute")
}

data class ArmorPiece(
    val name: String,
    val locations: List<String>, // "Głowa", "Korpus", "Ramiona", "Nogi"
    var ppS: Int,
    var ppO: Int,
    var ppK: Int,
    val weight: Int,
    val prefix: ArmorPrefix = ArmorPrefix.NONE,
    val suffix: ArmorSuffix = ArmorSuffix.NONE,
    val quality: ItemQuality = ItemQuality.NONE,
    var durability: Int = 100, // Liczbowe zużycie (np. HP pancerza)
    var durabilityPercent: Int = 100 // Procentowe %
) {
    val fullName: String get() {
        val gender = HeroLogic.determineItemGender(name)
        val isPlural = gender == "PL"
        val isFeminine = gender == "F"

        val q = when {
            quality == ItemQuality.NONE -> ""
            isPlural -> quality.pl
            isFeminine -> quality.f
            else -> quality.m
        }

        val p = when {
            prefix == ArmorPrefix.NONE -> ""
            isPlural -> prefix.pl
            isFeminine -> prefix.f
            else -> prefix.m
        }

        val s = if (suffix != ArmorSuffix.NONE) " " + suffix.displayName else ""

        return "${if(q.isNotEmpty()) "$q " else ""}${if(p.isNotEmpty()) "$p " else ""}$name$s".trim()
    }

    val totalWeight: Int get() = if (prefix == ArmorPrefix.PRZEKUTY) (weight * 1.1).toInt() else weight

    init {
        if (prefix == ArmorPrefix.PRZEKUTY) {
            val maxVal = maxOf(ppS, ppO, ppK)
            if (maxVal > 0) {
                when {
                    ppS == maxVal -> ppS--
                    ppO == maxVal -> ppO--
                    ppK == maxVal -> ppK--
                }
            }
        }
    }

    fun getElementalProt(type: DamageType): Int {
        return when(type) {
            DamageType.OGIEN -> suffix.fireProt
            DamageType.ZIMNO -> suffix.coldProt
            DamageType.PIORUN -> suffix.lightningProt
            DamageType.TRUCIZNA -> suffix.poisonProt
            else -> 0
        }
    }
}

data class CombatWeapon(
    val name: String,
    val damage: String,
    val bonus: Int = 0,
    val rangedType: RangedType = RangedType.NONE,
    val weaponStrength: Int = 0, // Stała Siła lub Maksymalna dla Łuków
    val suffix: WeaponSuffix = WeaponSuffix.NONE,
    val quality: ItemQuality = ItemQuality.NONE,
    val prefix: WeaponPrefix = WeaponPrefix.NONE,
    val price: String = "",
    val traits: String = "",
    val reloadActions: Int = 0,
    val damageType: DamageType = DamageType.NONE,
    val size: WeaponSize = WeaponSize.SREDNI
) {
    val fullName: String get() {
        val gender = HeroLogic.determineItemGender(name)
        val isPlural = gender == "PL"
        val isFeminine = gender == "F"

        val q = when {
            quality == ItemQuality.NONE -> ""
            isPlural -> quality.pl
            isFeminine -> quality.f
            else -> quality.m
        }

        val p = when {
            prefix == WeaponPrefix.NONE -> ""
            isPlural -> prefix.pl
            isFeminine -> prefix.f
            else -> prefix.m
        }

        val s = if (suffix != WeaponSuffix.NONE) " " + suffix.displayName else ""

        return "${if(q.isNotEmpty()) "$q " else ""}${if(p.isNotEmpty()) "$p " else ""}$name$s".trim()
    }
}

/**
 * Common business logic for the GenPro application.
 * Decoupled from UI components.
 */

object HeroLogic {

    fun getHeroProgress(heroId: String, heroData: String): HeroProgress {
        val lines = heroData.lines()
        
        // Calendar
        val calendarStr = getCalendarData(heroData)
        
        // Travel
        val travelState = getTravelData(heroData)
        
        // Visited Buildings - Parse from legacy format "Odwiedzono: City|Building"
        val visitedBuildings = lines.filter { it.startsWith("Odwiedzono: ") }
            .map { it.substringAfter("Odwiedzono: ").trim() }
            .toSet()
            
        // Stays
        val stays = lines.filter { it.startsWith("Pobyty: ") }
            .map { line ->
                val parts = line.substringAfter("Pobyty: ").split("|")
                HeroStay(
                    cityName = parts.getOrNull(0)?.trim() ?: "",
                    buildingName = parts.getOrNull(1)?.trim() ?: "",
                    expiryDateTime = parts.getOrNull(2)?.trim() ?: ""
                )
            }
            
        // Properties
        val properties = lines.filter { it.startsWith("Nieruchomości: ") }
            .map { line ->
                val parts = line.substringAfter("Nieruchomości: ").split("|")
                HeroProperty(
                    cityName = parts.getOrNull(0)?.trim() ?: "",
                    buildingName = parts.getOrNull(1)?.trim() ?: ""
                )
            }
            
        // Fatigue & Wanted
        val fatigue = getHeroFatigue(heroData)
        val wanted = getWantedLevel(heroData)
        
        // Dynamic Max Fatigue based on Stats (Emphasis on Odporność + Tragarz skill)
        val kBonus = getStatValue(heroData, "Krzepa") / 10
        val oBonus = getStatValue(heroData, "Odporność") / 10
        val tragarzMult = getTragarzMultiplier(heroData)
        val maxFatigue = 100 + (kBonus * tragarzMult) + (oBonus * 8)
        
        // Gathered Resources
        val gathered = mutableMapOf<String, MutableList<String>>()
        val zbStart = lines.indexOfFirst { it.startsWith("ZBIORY:") }
        if (zbStart != -1) {
            var currentCat = "Inne"
            for (i in zbStart + 1 until lines.size) {
                val line = lines[i]
                if (line.isEmpty() || (!line.startsWith("   ") && !line.startsWith("      "))) {
                    if (line.isNotEmpty() && !line.startsWith(" ")) break
                    continue
                }
                val trimmed = line.trim()
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    currentCat = trimmed.removeSurrounding("[", "]")
                } else if (trimmed.startsWith("•")) {
                    val list = gathered.getOrPut(currentCat) { mutableListOf() }
                    list.add(trimmed)
                }
            }
        }
        
        // Discovered Zones
        val discoveredZones = mutableMapOf<String, Set<String>>()
        lines.filter { it.startsWith("DISCOVERED_ZONES_") }.forEach { line ->
            val settlement = line.substringBefore(":").removePrefix("DISCOVERED_ZONES_")
            val zones = line.substringAfter(":").split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            discoveredZones[settlement] = zones
        }

        return HeroProgress(
            heroId = heroId,
            currentCalendar = calendarStr,
            travelState = travelState,
            visitedBuildings = visitedBuildings,
            stays = stays,
            properties = properties,
            fatigue = fatigue,
            maxFatigue = maxFatigue,
            wantedLevel = wanted,
            gatheredResources = gathered,
            discoveredZones = discoveredZones
        )
    }

    /**
     * Removes all world-state related tags from the character sheet string.
     * This is used to keep the character file focused only on stats and items.
     */
    fun stripLegacyProgressTags(heroData: String): String {
        val legacyPrefixes = listOf(
            "CALENDAR:", "TRAVEL:", "Odwiedzono:", "Pobyty:", 
            "Zmęczenie:", "Poszukiwany:", "ZBIORY:", 
            "Nieruchomości:", "DISCOVERED_ZONES_", "Lokacja:"
        )
        
        val lines = heroData.lines().toMutableList()
        
        // Remove ZBIORY block completely
        val zbStart = lines.indexOfFirst { it.startsWith("ZBIORY:") }
        if (zbStart != -1) {
            var endIdx = zbStart + 1
            while (endIdx < lines.size && (lines[endIdx].startsWith(" ") || lines[endIdx].isEmpty())) {
                endIdx++
            }
            // Remove from end to start to avoid index shifts
            for (i in (endIdx - 1) downTo zbStart) {
                if (i < lines.size) lines.removeAt(i)
            }
        }
        
        return lines.filter { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@filter true
            !legacyPrefixes.any { trimmed.startsWith(it) }
        }.joinToString("\n").trim()
    }

    fun getHeroName(heroData: String): String {
        val firstLine = heroData.lines().firstOrNull() ?: "Nieznany Bohater"
        return if (firstLine.startsWith("Imie: ", ignoreCase = true)) {
            firstLine.substringAfter("Imie: ").trim()
        } else {
            firstLine
        }
    }

    fun isMagicClass(heroData: String): Boolean {
        val klasa = getHeroClass(heroData)
        return klasa == "Mag" || klasa == "Kapłan" || klasa == "Czarodziej"
    }

    fun getHeroClass(heroData: String): String {
        val line = heroData.lines().find { it.contains("Klasa:", ignoreCase = true) } ?: ""
        val idx = line.indexOf("Klasa:", ignoreCase = true)
        return if (idx != -1) {
            line.substring(idx + "Klasa:".length).substringBefore(",").trim()
        } else ""
    }

    fun getHeroRace(heroData: String): String {
        val line = heroData.lines().find { it.contains("Rasa:", ignoreCase = true) } ?: ""
        val idx = line.indexOf("Rasa:", ignoreCase = true)
        return if (idx != -1) {
            line.substring(idx + "Rasa:".length).substringBefore(",").trim()
        } else "Człowiek"
    }

    fun getHeroGender(heroData: String): String {
        val line = heroData.lines().find { it.contains("Płeć:", ignoreCase = true) } ?: ""
        val idx = line.indexOf("Płeć:", ignoreCase = true)
        return if (idx != -1) {
            line.substring(idx + "Płeć:".length).substringBefore(",").trim()
        } else "Nieznana"
    }


    fun getHeroGenderSymbol(heroData: String): String {
        val gender = getHeroGender(heroData)
        return if (gender.contains("Mężczyzna", ignoreCase = true)) "♂️" else "♀️"
    }

    fun getHeroPd(heroData: String): Int {
        val line = heroData.lines().find { it.contains("PD:", ignoreCase = true) } ?: ""
        val idx = line.indexOf("PD:", ignoreCase = true)
        return if (idx != -1) {
            line.substring(idx + "PD:".length).trim().toIntOrNull() ?: 0
        } else 0
    }

    fun getHeroExperienceLevel(heroData: String): Int {
        val line = heroData.lines().find { it.contains("POZIOM:", ignoreCase = true) } ?: ""
        val idx = line.indexOf("POZIOM:", ignoreCase = true)
        return if (idx != -1) {
            val afterColon = line.substring(idx + "POZIOM:".length).trim()
            if (afterColon.contains("｢")) {
                afterColon.substringBefore("｢").trim().toIntOrNull() ?: 0
            } else {
                afterColon.substringBefore(" ").trim().toIntOrNull() ?: 0
            }
        } else 0
    }

    fun getHeroStartLevel(heroData: String): Int {
        val line = heroData.lines().find { it.contains("POZIOM:", ignoreCase = true) } ?: ""
        return if (line.contains("｢") && line.contains("｣")) {
            line.substringAfter("｢").substringBefore("｣").trim().toIntOrNull() ?: 0
        } else 0
    }

    fun getClassPreferredStats(heroData: String): Set<String> {
        val klasa = getHeroClass(heroData)
        val level = getHeroLevel(heroData)
        val preferred = mutableSetOf<String>()

        if (klasa.isNotEmpty()) {
            if (level > 5) {
                preferred.add("Wiedza")
            }

            when (klasa) {
                "Wojownik" -> {
                    preferred.add("Walka Wręcz")
                    preferred.add("Zręczność")
                    if (level > 5) {
                        preferred.add("Krzepa")
                        preferred.add("Odporność")
                        preferred.add("Siła Woli")
                    }
                }
                "Łotr" -> {
                    preferred.add("Zręczność")
                    preferred.add("Charyzma")
                    if (level > 5) {
                        preferred.add("Inteligencja")
                        preferred.add("Odporność")
                    }
                }
                "Łowca" -> {
                    preferred.add("Strzelectwo")
                    preferred.add("Zręczność")
                    if (level > 5) {
                        preferred.add("Inteligencja")
                        preferred.add("Odporność")
                    }
                }
                "Uczony" -> {
                    preferred.add("Inteligencja")
                    preferred.add("Zręczność")
                    if (level > 5) {
                        preferred.add("Siła Woli")
                        preferred.add("Odporność")
                    }
                }
                "Kapłan" -> {
                    preferred.add("Charyzma")
                    preferred.add("Siła Woli")
                    if (level > 5) {
                        preferred.add("Inteligencja")
                    }
                }
                "Czarodziej" -> {
                    preferred.add("Siła Woli")
                    preferred.add("Inteligencja")
                    if (level > 5) {
                        preferred.add("Wiedza")
                        preferred.add("Zręczność")
                    }
                }
            }
            if (level > 8) {
                preferred.add("Siła Woli")
            }
        }
        return preferred
    }

    fun getWeaponSkillName(weapon: CombatWeapon?): String {
        if (weapon == null) return "Bijatyka"
        val wName = weapon.name
        return if (weapon.rangedType != RangedType.NONE) {
            when {
                wName.contains("Łuk", true) -> "Łuki"
                wName.contains("Kusza", true) -> "Kusze"
                wName.contains("Pistolet", true) || wName.contains("Rusznica", true) -> "Broń Prochowa"
                wName.contains("Nóż", true) || wName.contains("Oszczep", true) || wName.contains("Proca", true) -> "Broń miotana"
                else -> "Strzelectwo"
            }
        } else {
            when {
                wName.contains("Kastet", true) || wName.contains("Bijatyka", true) -> "Bijatyka"
                weapon.damageType == DamageType.SIECZNA -> "Broń bojowa sieczna"
                weapon.damageType == DamageType.OBUCHOWA -> "Broń bojowa obuchowa"
                weapon.damageType == DamageType.KLUTA -> "Broń bojowa kłuta"
                else -> "Broń prosta"
            }
        }
    }

    fun getHeroLevel(heroData: String): Int {
        val line = heroData.lines().find { it.contains("POZIOM:", ignoreCase = true) } ?: ""
        return line.substringAfter("｢").substringBefore("｣").trim().toIntOrNull() ?: 0
    }

    fun getSkillValue(heroData: String, skillName: String): Int {
        return getSkillRank(heroData.lines(), skillName)
    }

    fun getAttributeValue(heroData: String, attrName: String): Int {
        return getStatValue(heroData, attrName)
    }

    fun getSkillRank(heroLines: List<String>, skillName: String): Int {
        val line = heroLines.find { it.contains(skillName, ignoreCase = true) }
        return when {
            line == null || line.contains("Brak", true) -> 0
            line.contains("Arcymistrz", true) || line.contains("𝕬𝖗𝖈𝖞𝖒𝖎𝖘𝖙𝖗𝖟", true) -> 5
            line.contains("Mistrz", true) || line.contains("𝕸𝖎𝖘𝖙𝖗𝖟", true) -> 4
            line.contains("Ekspert", true) || line.contains("𝕰𝖐𝖘𝖕𝖊𝖗𝖙", true) -> 3
            line.contains("Adept", true) || line.contains("𝕬𝖉𝖊𝖕𝖙", true) -> 2
            line.contains("Uczeń", true) || line.contains("𝖀𝖈𝖟𝖊𝖓", true) || line.contains("✔️") -> 1
            else -> 0
        }
    }

    fun getStatValue(heroData: String, statName: String): Int {
        val lines = heroData.lines()
        val searchTerms = when (statName.uppercase()) {
            "WW", "WALKA WRĘCZ", "WALKA WRECZ" -> listOf("WW:", "Walka Wręcz:")
            "US", "STRZELECTWO", "UMIEJĘTNOŚCI STRZELECKIE", "UMIEJETNOSCI STRZELECKIE" -> listOf("US:", "Umiejętności Strzeleckie:", "Strzelectwo:")
            "K", "KRZEPA" -> listOf("K:", "Krzepa:")
            "O", "ODP", "ODPORNOŚĆ", "ODPORNOSC" -> listOf("O:", "Odporność:", "Odp:")
            "ZR", "ZRĘCZNOŚĆ", "ZRECZNOSC" -> listOf("Zr:", "Zręczność:")
            "INT", "INTELIGENCJA" -> listOf("Int:", "Inteligencja:")
            "SW", "SIŁA WOLI", "SILA WOLI" -> listOf("SW:", "Siła Woli:")
            "CHA", "OGŁ", "OGL", "CHARYZMA" -> listOf("Cha:", "Charyzma:", "Ogł:", "Ogl:")
            "WIEDZA" -> listOf("Wiedza:")
            "JĘZYKI", "JEZYKI", "JĘZYK", "JEZYK" -> listOf("Języki:", "Język:", "Jezyki:", "Jezyk:")
            "SZ", "SZYBKOŚĆ", "SZYBKOSC" -> listOf("Sz:", "Szybkość:")
            "MAG", "MAGIA", "M" -> listOf("M:", "Mag:", "Magia:")
            "A", "ATAKI" -> listOf("A:", "Ataki:")
            "ŻYW", "ŻYWOTNOŚĆ", "ZYWOTNOSC" -> listOf("Żyw:", "Żywotność:", "Zyw:")
            "S", "SIŁA", "SILA" -> listOf("S:", "Siła:")
            "WT", "WTK", "WYTRZYMAŁOŚĆ", "WYTRZYMALOSC" -> listOf("Wt:", "Wtk:", "Wytrzymałość:")
            "PP", "PRZEZNACZENIE" -> listOf("PP:", "Przeznaczenie:")
            "PO", "OBŁĘD", "OBLED" -> listOf("PO:", "Obłęd:")
            else -> listOf("$statName:")
        }

        val line = lines.find { line ->
            val clean = line.trim().removePrefix("•").trim()
            searchTerms.any { clean.startsWith(it, ignoreCase = true) }
        } ?: return 0

        val afterColon = line.substringAfter(":")
        return afterColon
            .substringBefore("|")
            .substringBefore("(")
            .trim()
            .toIntOrNull() ?: 0
    }

    fun getStatPlusValue(heroData: String, statName: String): Int {
        val lines = heroData.lines()
        val searchTerms = when (statName.uppercase()) {
            "WW", "WALKA WRĘCZ", "WALKA WRECZ" -> listOf("WW:", "Walka Wręcz:")
            "US", "STRZELECTWO", "UMIEJĘTNOŚCI STRZELECKIE", "UMIEJETNOSCI STRZELECKIE" -> listOf("US:", "Umiejętności Strzeleckie:", "Strzelectwo:")
            "K", "KRZEPA" -> listOf("K:", "Krzepa:")
            "O", "ODP", "ODPORNOŚĆ", "ODPORNOSC" -> listOf("O:", "Odporność:", "Odp:")
            "ZR", "ZRĘCZNOŚĆ", "ZRECZNOSC" -> listOf("Zr:", "Zręczność:")
            "INT", "INTELIGENCJA" -> listOf("Int:", "Inteligencja:")
            "SW", "SIŁA WOLI", "SILA WOLI" -> listOf("SW:", "Siła Woli:")
            "CHA", "OGŁ", "OGL", "CHARYZMA" -> listOf("Cha:", "Charyzma:", "Ogł:", "Ogl:")
            "WIEDZA" -> listOf("Wiedza:")
            "JĘZYKI", "JEZYKI", "JĘZYK", "JEZYK" -> listOf("Języki:", "Język:", "Jezyki:", "Jezyk:")
            "SZ", "SZYBKOŚĆ", "SZYBKOSC" -> listOf("Sz:", "Szybkość:")
            "MAG", "MAGIA", "M" -> listOf("M:", "Mag:", "Magia:")
            "A", "ATAKI" -> listOf("A:", "Ataki:")
            "ŻYW", "ŻYWOTNOŚĆ", "ZYWOTNOSC" -> listOf("Żyw:", "Żywotność:", "Zyw:")
            "S", "SIŁA", "SILA" -> listOf("S:", "Siła:")
            "WT", "WTK", "WYTRZYMAŁOŚĆ", "WYTRZYMALOSC" -> listOf("Wt:", "Wtk:", "Wytrzymałość:")
            "PP", "PRZEZNACZENIE" -> listOf("PP:", "Przeznaczenie:")
            "PO", "OBŁĘD", "OBLED" -> listOf("PO:", "Obłęd:")
            else -> listOf("$statName:")
        }

        val line = lines.find { line ->
            val clean = line.trim().removePrefix("•").trim()
            searchTerms.any { clean.startsWith(it, ignoreCase = true) }
        } ?: return 0

        // 1. PRIORYTET: Jawny tag "Plus:"
        val plusMatch = Regex("Plus:\\s*(\\d+)").find(line)
        if (plusMatch != null) {
            return plusMatch.groupValues[1].toIntOrNull() ?: 0
        }

        // 2. FALLBACK: Obliczanie z (Current - Base) dla starych plików
        if (line.contains("Base:")) {
            val current = getStatValue(heroData, statName)
            val base = getStatBaseValue(heroData, statName)
            val diff = current - base
            
            if (diff > 0) {
                // Sprawdzamy czy to statystyka drugorzędna (A, Żyw, S, Wt, Sz, Mag)
                val upperStat = statName.uppercase()
                val isSecondary = listOf("A", "ŻYW", "S", "WT", "SZ", "MAG", "PP", "PO").any { upperStat == it || upperStat.startsWith(it) } ||
                                  listOf("ATAKI", "ŻYWOTNOŚĆ", "SIŁA", "WTK", "SZYBKOŚĆ", "MAGIA").any { upperStat.contains(it) }
                
                return if (isSecondary) diff else diff / 5
            }
        }
        
        return 0
    }

    fun getStatBaseValue(heroData: String, statName: String): Int {
        val lines = heroData.lines()
        val searchTerms = when (statName.uppercase()) {
            "WW", "WALKA WRĘCZ", "WALKA WRECZ" -> listOf("WW:", "Walka Wręcz:")
            "US", "STRZELECTWO", "UMIEJĘTNOŚCI STRZELECKIE", "UMIEJETNOSCI STRZELECKIE" -> listOf("US:", "Umiejętności Strzeleckie:", "Strzelectwo:")
            "K", "KRZEPA" -> listOf("K:", "Krzepa:")
            "O", "ODP", "ODPORNOŚĆ", "ODPORNOSC" -> listOf("O:", "Odporność:", "Odp:")
            "ZR", "ZRĘCZNOŚĆ", "ZRECZNOSC" -> listOf("Zr:", "Zręczność:")
            "INT", "INTELIGENCJA" -> listOf("Int:", "Inteligencja:")
            "SW", "SIŁA WOLI", "SILA WOLI" -> listOf("SW:", "Siła Woli:")
            "CHA", "OGŁ", "OGL", "CHARYZMA" -> listOf("Cha:", "Charyzma:", "Ogł:", "Ogl:")
            "WIEDZA" -> listOf("Wiedza:")
            "JĘZYKI", "JEZYKI", "JĘZYK", "JEZYK" -> listOf("Języki:", "Język:", "Jezyki:", "Jezyk:")
            "SZ", "SZYBKOŚĆ", "SZYBKOSC" -> listOf("Sz:", "Szybkość:")
            "MAG", "MAGIA", "M" -> listOf("M:", "Mag:", "Magia:")
            "A", "ATAKI" -> listOf("A:", "Ataki:")
            "ŻYW", "ŻYWOTNOŚĆ", "ZYWOTNOSC" -> listOf("Żyw:", "Żywotność:", "Zyw:")
            "S", "SIŁA", "SILA" -> listOf("S:", "Siła:")
            "WT", "WTK", "WYTRZYMAŁOŚĆ", "WYTRZYMALOSC" -> listOf("Wt:", "Wtk:", "Wytrzymałość:")
            "PP", "PRZEZNACZENIE" -> listOf("PP:", "Przeznaczenie:")
            "PO", "OBŁĘD", "OBLED" -> listOf("PO:", "Obłęd:")
            else -> listOf("$statName:")
        }

        val line = lines.find { line ->
            val clean = line.trim().removePrefix("•").trim()
            searchTerms.any { clean.startsWith(it, ignoreCase = true) }
        } ?: return 0

        if (!line.contains("Base:")) return getStatValue(heroData, statName)
        return line.substringAfter("Base:").substringBefore("|").trim().toIntOrNull() ?: 0
    }

    fun parseArmorPiece(line: String, playerRace: String): ArmorPiece? {
        val clean = line.replace("🅔", "").replace("🛡️", "").replace("💢", "").trim()
        val lower = clean.lowercase()

        // Single source of truth lookup
        val dbArmor = ItemRegistry.findArmor(clean)

        val regex = Regex("\\[(\\d+)/(\\d+)/(\\d+)\\]")
        val match = regex.find(clean)

        val (ps, po, pk) = if (match != null) {
            Triple(
                match.groupValues[1].toInt(),
                match.groupValues[2].toInt(),
                match.groupValues[3].toInt()
            )
        } else if (dbArmor != null) {
            Triple(dbArmor.ppS, dbArmor.ppO, dbArmor.ppK)
        } else {
            val p = when {
                lower.contains("płytow") || lower.contains("kirys") || lower.contains("stalow") -> 3
                lower.contains("kolcz") || lower.contains("łańcuch") || lower.contains("łuskow") -> 2
                lower.contains("skór") || lower.contains("kaftan") || lower.contains("kurtka") || lower.contains("ćwiekow") -> 1
                else -> 0
            }
            when (p) {
                1 -> Triple(2, 1, 0)
                2 -> Triple(3, 1, 2)
                3 -> Triple(4, 3, 4)
                else -> Triple(0, 0, 0)
            }
        }

        if (ps <= 0 && po <= 0 && pk <= 0 && !line.contains("🛡️")) return null

        val locs = dbArmor?.locations?.toMutableList() ?: mutableListOf<String>()
        if (locs.isEmpty()) {
            if (lower.contains("hełm") || lower.contains("kaptur") || lower.contains("czap") || lower.contains("czep")) locs.add("Głowa")
            if (lower.contains("nogawic") || lower.contains("nagolenn") || lower.contains("buty")) locs.add("Nogi")
            if ((lower.contains("rękaw") || lower.contains("karwasz") || lower.contains("naramienn")) && !lower.contains("bezrękawnik")) locs.add("Ramiona")
            if (lower.contains("kaftan") || lower.contains("kurtka") || lower.contains("kirys") || lower.contains("koszul") || lower.contains("kolczuga") || lower.contains("skórznia") || lower.contains("pancerz") || lower.contains("bezrękawnik")) locs.add("Korpus")
        }
        if (locs.isEmpty()) locs.add("Korpus")

        val prefix = when {
            lower.contains("przekut") -> ArmorPrefix.PRZEKUTY
            lower.contains("elfick") -> ArmorPrefix.ELFICKI
            lower.contains("khazadzk") || lower.contains("krasnoludzk") -> ArmorPrefix.KHAZADZKI
            lower.contains("ludzk") -> ArmorPrefix.LUDZKI
            lower.contains("uniwersaln") -> ArmorPrefix.UNIWERSALNY
            else -> ArmorPrefix.NONE
        }

        val suffix = when {
            lower.contains("antidotum") -> ArmorSuffix.ANTIDOTUM
            lower.contains("ciepła") -> ArmorSuffix.CIEPLA
            lower.contains("szronu") -> ArmorSuffix.SZRONU
            lower.contains("uziemienia") -> ArmorSuffix.UZIEMIENIA
            lower.contains("życia") -> ArmorSuffix.ZYCIA
            lower.contains("szybkości") -> ArmorSuffix.SZYBKOSCI
            else -> ArmorSuffix.NONE
        }

        val quality = when {
            lower.contains("wyjątkow") -> ItemQuality.WYJATKOWY
            lower.contains("dobr") -> ItemQuality.DOBRY
            lower.contains("zwyczaj") -> ItemQuality.ZWYCZAJNY
            lower.contains("znoszon") -> ItemQuality.ZNOSZONY
            lower.contains("uszkodzon") -> ItemQuality.USZKODZONY
            lower.contains("zniszczon") -> ItemQuality.ZNISZCZONY
            lower.contains("jakość: 100%") -> ItemQuality.WYJATKOWY
            lower.contains("jakość: 9") -> ItemQuality.DOBRY
            lower.contains("jakość: 8") -> ItemQuality.ZWYCZAJNY
            lower.contains("jakość: 7") -> ItemQuality.ZNOSZONY
            lower.contains("jakość: 6") -> ItemQuality.USZKODZONY
            lower.contains("jakość: 5") || lower.contains("jakość: 4") || lower.contains("jakość: 3") || lower.contains("jakość: 2") || lower.contains("jakość: 1") -> ItemQuality.ZNISZCZONY
            else -> ItemQuality.NONE
        }

        val canWear = when (prefix) {
            ArmorPrefix.NONE, ArmorPrefix.UNIWERSALNY, ArmorPrefix.PRZEKUTY -> true
            ArmorPrefix.ELFICKI -> playerRace.contains("Elf", ignoreCase = true)
            ArmorPrefix.KHAZADZKI -> playerRace.contains("Krasnolud", ignoreCase = true) || playerRace.contains("Khazad", ignoreCase = true)
            ArmorPrefix.LUDZKI -> playerRace.contains("Człowiek", ignoreCase = true)
        }

        if (!canWear) return null

        val cleanName = clean.replace(regex, "").replace(Regex("\\[Jakość: \\d+%\\]"), "").trim()
        return ArmorPiece(
            cleanName,
            locs, ps, po, pk,
            weight = ItemRegistry.getWeight(cleanName),
            prefix = prefix,
            suffix = suffix,
            quality = quality
        )
    }

    fun parseWeapon(line: String): CombatWeapon? {
        val equippedLine = line.replace("🅔", "").trim()
        val cleanLine = if (equippedLine.contains("Jakość:", ignoreCase = true)) equippedLine
        else {
            val c = equippedLine.replace(Regex("\\[Jakość: \\d+%\\]"), "").trim()
            "$c [Jakość: 90%]"
        }

        val lower = cleanLine.lowercase()
        val quality = when {
            lower.contains("wyjątkow") -> ItemQuality.WYJATKOWY
            lower.contains("dobr") -> ItemQuality.DOBRY
            lower.contains("zwyczaj") -> ItemQuality.ZWYCZAJNY
            lower.contains("znoszon") -> ItemQuality.ZNOSZONY
            lower.contains("uszkodzon") -> ItemQuality.USZKODZONY
            lower.contains("zniszczon") -> ItemQuality.ZNISZCZONY
            lower.contains("jakość: 100%") -> ItemQuality.WYJATKOWY
            lower.contains("jakość: 9") -> ItemQuality.DOBRY
            lower.contains("jakość: 8") -> ItemQuality.ZWYCZAJNY
            lower.contains("jakość: 7") -> ItemQuality.ZNOSZONY
            lower.contains("jakość: 6") -> ItemQuality.USZKODZONY
            lower.contains("jakość: 5") || lower.contains("jakość: 4") || lower.contains("jakość: 3") || lower.contains("jakość: 2") || lower.contains("jakość: 1") -> ItemQuality.ZNISZCZONY
            else -> ItemQuality.NONE
        }

        val prefix = WeaponPrefix.entries.find { cleanLine.contains(it.displayName, true) } ?: WeaponPrefix.NONE
        val suffix = WeaponSuffix.entries.find { cleanLine.contains(it.displayName, true) } ?: WeaponSuffix.NONE

        val cleanName = cleanLine.replace(Regex("\\[Jakość: \\d+%\\]"), "").trim()
        val dbWeapon = ItemRegistry.findWeapon(cleanName)

        // Extract size if present in text (e.g. "Miecz [Średni]") or use DB weapon size
        val sizeFromText = WeaponSize.entries.find { cleanLine.contains("[${it.displayName}]", ignoreCase = true) }

        return dbWeapon?.let {
            CombatWeapon(
                name = cleanName,
                damage = it.damageDie,
                bonus = it.damageBonus,
                rangedType = it.rangedType,
                weaponStrength = it.weaponStrength,
                suffix = suffix,
                quality = quality,
                prefix = prefix,
                reloadActions = it.reloadActions,
                damageType = it.damageType,
                size = sizeFromText ?: it.size
            )
        } ?: CombatWeapon(
            cleanName,
            "1k3",
            0,
            quality = quality,
            prefix = prefix,
            suffix = suffix,
            size = sizeFromText ?: WeaponSize.SREDNI
        )
    }

    fun damageArmorAt(
        location: String,
        equipment: List<String>,
        updateFile: (List<String>) -> Unit
    ): List<String> {
        val regex = Regex("\\[(\\d+)/(\\d+)/(\\d+)\\]")
        var armorDamaged = false

        val newEq = equipment.map { item ->
            val isArmor = item.contains("🅔") || item.contains("🛡️")
            if (!isArmor) return@map item

            val clean = item.replace("🅔", "").replace("🛡️", "").trim()
            val lower = clean.lowercase()

            val locs = mutableListOf<String>()
            if (lower.contains("hełm") || lower.contains("kaptur") || lower.contains("czap") || lower.contains("czep")) locs.add("Głowa")
            if (lower.contains("nogawic") || lower.contains("nagolenn") || lower.contains("buty") || lower.contains("skórznia")) locs.add("Nogi")
            if ((lower.contains("rękaw") || lower.contains("karwasz") || lower.contains("naramienn")) && !lower.contains("bezrękawnik")) locs.add("Ramiona")
            if (lower.contains("kaftan") || lower.contains("kurtka") || lower.contains("kirys") || lower.contains("koszul") || lower.contains("kolczuga") || lower.contains("skórznia") || lower.contains("pancerz") || lower.contains("bezrękawnik")) locs.add("Korpus")
            if (locs.isEmpty()) locs.add("Korpus")

            val covers = locs.any { loc ->
                location.contains(loc, true) ||
                        (loc == "Ramiona" && location.contains("Ręka", true)) ||
                        (loc == "Nogi" && location.contains("Noga", true))
            }

            if (covers) {
                val match = regex.find(item)
                if (match != null) {
                    val ps = maxOf(0, match.groupValues[1].toInt() - 1)
                    val po = maxOf(0, match.groupValues[2].toInt() - 1)
                    val pk = maxOf(0, match.groupValues[3].toInt() - 1)

                    armorDamaged = true
                    if (ps == 0 && po == 0 && pk == 0 && !item.contains("🛡️")) {
                        item.replace(regex, "[0/0/0]").replace("🅔", "💢")
                    } else {
                        item.replace(regex, "[$ps/$po/$pk]")
                    }
                } else {
                    val p = when {
                        lower.contains("płytow") || lower.contains("kirys") || lower.contains("stalow") -> 3
                        lower.contains("kolcz") || lower.contains("łańcuch") || lower.contains("łuskow") -> 2
                        lower.contains("skór") || lower.contains("kaftan") || lower.contains("kurtka") || lower.contains("ćwiekow") -> 1
                        else -> 0
                    }
                    if (p > 0) {
                        armorDamaged = true
                        val (ps, po, pk) = when (p) {
                            1 -> Triple(1, 0, 0)
                            2 -> Triple(2, 0, 1)
                            else -> Triple(3, 2, 3)
                        }
                        item + " [$ps/$po/$pk]"
                    } else item
                }
            } else item
        }

        if (armorDamaged) {
            updateFile(newEq)
        }
        return newEq
    }

    fun getArmorLocationIcons(armorPieces: List<ArmorPiece>, location: String): String {
        val pieces = armorPieces.filter { it.locations.contains(location) }
        if (pieces.isEmpty()) return ""
        val ps = pieces.sumOf { it.ppS }
        val po = pieces.sumOf { it.ppO }
        val pk = pieces.sumOf { it.ppK }
        return "[$ps/$po/$pk]"
    }

    fun getRankWeight(rank: String): Int {
        val r = rank.trim()
        return when {
            r.contains("Arcymistrz") || r.contains("𝕬𝖗𝖈𝖞𝖒𝖎𝖘𝖙𝖗𝖟") -> 5
            r.contains("Mistrz") || r.contains("𝕸𝖎𝖘𝖙𝖗𝖟") -> 4
            r.contains("Ekspert") || r.contains("𝕰𝖐𝖘𝖕𝖊𝖗𝖙") -> 3
            r.contains("Adept") || r.contains("𝕬𝖉𝖊𝖕𝖙") -> 2
            r.contains("Uczeń") || r.contains("𝖀𝖈𝖟𝖊𝖓") -> 1
            else -> 0
        }
    }

    fun getRankBonus(heroLines: List<String>, skillName: String): Int {
        val rank = getSkillRank(heroLines, skillName)
        val isSimpleWeapon = skillName == "Broń prosta"
        return when (rank) {
            5 -> 20
            4 -> 15
            3 -> 10
            2 -> 5
            1 -> 0
            else -> if (isSimpleWeapon) 0 else -20
        }
    }

    val shops = listOf(
        Shop("Kowal", "Kucie i naprawa broni", listOf("Sieczna", "Obuchowa", "Kłuta", "Broń Egzotyczna")),
        Shop("Płatnerz", "Naprawa i wyrób pancerzy", listOf("Głowa", "Korpus", "Inne", "Tarcze")),
        Shop("Rymarz", "Torby i uprząż", listOf("Akcesoria", "Zwierzęta")),
        Shop("Apteka", "Leki i mikstury", listOf("Medykamenty", "Tykwy i Bukłaki", "Zioła", "Mikstury")),
        Shop("Karczma", "Jedzenie i trunki", listOf("Posiłki", "Trunki", "Nocleg")),
        Shop("Stajnia", "Konie i wozy", listOf("Konie", "Konie robocze", "Wozy lekkie", "Wozy ciężkie", "Nocleg")),
        Shop("Praca", "Dorywcze zajęcia", listOf("Praca"))
    )

    val weaponDb = listOf(
        // BARDZO MAŁE (WW +20, 1k4)
        CombatWeapon(
            "Sztylet",
            "1k4",
            20,
            damageType = DamageType.KLUTA
        ),
        CombatWeapon(
            "Kastet",
            "1k4",
            20,
            traits = "Ogłuszający",
            damageType = DamageType.OBUCHOWA
        ),
        CombatWeapon(
            "Noże do rzucania",
            "1k4",
            20,
            rangedType = RangedType.SB_DEPENDENT,
            damageType = DamageType.KLUTA
        ),


        // MAŁE (WW +10, 1k6)
        CombatWeapon(
            "Miecz krótki",
            "1k6",
            10,
            damageType = DamageType.SIECZNA
        ),
        CombatWeapon(
            "Toporek",
            "1k6",
            10,
            damageType = DamageType.SIECZNA
        ),
        CombatWeapon(
            "Lewak",
            "1k6",
            10,
            traits = "Defensywny",
            damageType = DamageType.KLUTA
        ),
        CombatWeapon(
            "Pałka",
            "1k6",
            10,
            traits = "Ogłuszający",
            damageType = DamageType.OBUCHOWA
        ),

        // ŚREDNIE (WW +0, 1k8)
        CombatWeapon(
            "Miecz",
            "1k8",
            0,
            damageType = DamageType.SIECZNA
        ),
        CombatWeapon(
            "Szabla",
            "1k8",
            0,
            traits = "Szybki",
            damageType = DamageType.SIECZNA
        ),
        CombatWeapon(
            "Topór",
            "1k8",
            0,
            damageType = DamageType.SIECZNA
        ),
        CombatWeapon(
            "Młot bojowy",
            "1k8",
            0,
            traits = "Ogłuszający",
            damageType = DamageType.OBUCHOWA
        ),
        CombatWeapon(
            "Buława",
            "1k8",
            0,
            traits = "Ogłuszający",
            damageType = DamageType.OBUCHOWA
        ),
        CombatWeapon(
            "Nadziak",
            "1k8",
            0,
            traits = "Druzgoczący",
            damageType = DamageType.KLUTA
        ),
        CombatWeapon(
            "Rapier",
            "1k8",
            0,
            traits = "Szybki",
            damageType = DamageType.KLUTA
        ),
        CombatWeapon(
            "Korbacz",
            "1k8",
            0,
            traits = "Ogłuszający, Męczący",
            damageType = DamageType.OBUCHOWA
        ),

        // DUŻE (WW -10, 1k10 / 1k12)
        CombatWeapon(
            "Włócznia",
            "1k10",
            -10,
            damageType = DamageType.KLUTA
        ),
        CombatWeapon(
            "Widły bojowe",
            "1k10",
            -10,
            damageType = DamageType.KLUTA
        ),
        CombatWeapon(
            "Trójząb",
            "1k10",
            -10,
            damageType = DamageType.KLUTA
        ),
        CombatWeapon(
            "Wielki miecz",
            "1k12",
            -10,
            traits = "Druzgoczący, Wolny",
            damageType = DamageType.SIECZNA
        ),
        CombatWeapon(
            "Kosa bojowa",
            "1k10",
            -10,
            damageType = DamageType.SIECZNA
        ),
        CombatWeapon(
            "Tasak wielki",
            "1k10",
            -10,
            damageType = DamageType.SIECZNA
        ),
        CombatWeapon(
            "Mizerykordia",
            "1k4",
            20,
            traits = "Przebijający",
            damageType = DamageType.KLUTA
        ),
        CombatWeapon(
            "Wbijak",
            "1k4",
            20,
            damageType = DamageType.KLUTA
        ),
        CombatWeapon(
            "Kostur",
            "1k8",
            0,
            traits = "Defensywny",
            damageType = DamageType.OBUCHOWA
        ),
        CombatWeapon(
            "Wielki topór",
            "1k12",
            -10,
            traits = "Druzgoczący, Wolny",
            damageType = DamageType.SIECZNA
        ),
        CombatWeapon(
            "Wielki młot",
            "1k12",
            -10,
            traits = "Ogłuszający, Druzgoczący, Wolny",
            damageType = DamageType.OBUCHOWA
        ),
        CombatWeapon(
            "Halabarda",
            "1k10",
            -10,
            traits = "Specjalna",
            damageType = DamageType.SIECZNA
        ),
        CombatWeapon(
            "Cep bojowy",
            "1k10",
            -10,
            traits = "Ogłuszający, Druzgoczący, Męczący",
            damageType = DamageType.OBUCHOWA
        ),

        // DYSTANSOWE (Inne zasady obrażeń)
        CombatWeapon(
            "Łuk",
            "1k6",
            0,
            rangedType = RangedType.BOW,
            weaponStrength = 3,
            reloadActions = 1,
            damageType = DamageType.KLUTA
        ),
        CombatWeapon(
            "Długi łuk",
            "1k8",
            0,
            rangedType = RangedType.BOW,
            weaponStrength = 4,
            reloadActions = 1,
            damageType = DamageType.KLUTA
        ),
        CombatWeapon(
            "Kusza",
            "1k10",
            0,
            rangedType = RangedType.FIXED,
            weaponStrength = 4,
            reloadActions = 2,
            damageType = DamageType.KLUTA
        ),
        CombatWeapon(
            "Pistolet",
            "1k10",
            0,
            rangedType = RangedType.FIXED,
            weaponStrength = 4,
            traits = "Druzgoczący, Przebijający pancerz",
            reloadActions = 3,
            damageType = DamageType.KLUTA
        ),
        CombatWeapon(
            "Proca",
            "1k4",
            0,
            rangedType = RangedType.SB_DEPENDENT,
            reloadActions = 1,
            damageType = DamageType.OBUCHOWA
        ),
        CombatWeapon(
            "Oszczep",
            "1k6",
            0,
            rangedType = RangedType.SB_DEPENDENT,
            reloadActions = 0,
            damageType = DamageType.KLUTA
        )

    ).flatMap { base ->
        listOf(
            base,
            base.copy(suffix = WeaponSuffix.TRUCIZNY),
            base.copy(suffix = WeaponSuffix.OGNIA),
            base.copy(suffix = WeaponSuffix.LODU),
            base.copy(suffix = WeaponSuffix.PIORUNA),
            base.copy(suffix = WeaponSuffix.ZYWIOLOW)
        )
    }

    // --- DATA FROM UIUTILS ---
    val itemKategoria = arrayOf("Monety", "Pojemniki", "Broń ręczna", "Broń strzelecka", "Pociski", "Pancerz", "Odzienie", "Biżuteria", "Instrument", "Narzędzie", "Księga", "Majątek", "Mikstura", "Zwierzęta")
    val itemRodzajMonety = arrayOf("Złota korona (zk)", "Srebrny szyling (s)", "Miedziany pens (p)")
    val itemSakwy = arrayOf("Mała sakiewka", "Średnia sakiewka", "Duża sakiewka")
    val itemTorby = arrayOf("Worek lniany", "Torba podróżna", "Plecak", "Plecak usztywniany")
    val itemBuklaki = arrayOf("Tykwa", "Bukłak mały", "Bukłak duży", "Antałek")
    val itemPojemniki = itemSakwy + itemTorby + itemBuklaki

    val pojemnikiInfo = mapOf(
        "Mała sakiewka" to Triple(0.1, 50, "Sakiewka"),
        "Średnia sakiewka" to Triple(0.2, 100, "Sakiewka"),
        "Duża sakiewka" to Triple(0.3, 250, "Sakiewka"),
        "Worek lniany" to Triple(0.5, 500, "Torba"),
        "Torba podróżna" to Triple(1.0, 1000, "Torba"),
        "Plecak" to Triple(1.5, 2000, "Torba"),
        "Plecak usztywniany" to Triple(2.0, 3000, "Torba"),
        "Tykwa" to Triple(0.2, 1, "Płyn"),
        "Bukłak mały" to Triple(0.5, 2, "Płyn"),
        "Bukłak duży" to Triple(1.0, 5, "Płyn"),
        "Antałek" to Triple(2.0, 10, "Płyn")
    )

    val itemRozmiar = arrayOf("Brak0", "BardzoMały1", "Mały2", "Średni3", "Duży4", "BardzoDuży5", "Gigantyczny6")
    val itemRodzajBR = arrayOf("Broń prosta", "Sieczna", "Obuchowa", "Kłuta", "Broń Egzotyczna")

    val WW0nazwa = arrayOf("Kostur", "Sztylet", "Kastet", "Kastet średni", "Kastet duży", "Łańcuch", "Pałka")
    val WW0robr = arrayOf("O", "K", "O", "O", "O", "O", "O")
    val WW0kX = arrayOf(8, 4, 4, 6, 8, 6, 6)
    val WW0wo = arrayOf(1700, 760, 600, 900, 1200, 960, 800)
    val WW0rozmiar = arrayOf(4, 2, 2, 3, 4, 3, 4)

    val WW1nazwa = arrayOf("Kord", "Bagnet", "Miecz krótki", "Tasak", "Szabla", "Miecz", "Miecz wielki", "Kosa bojowa", "Tasak wielki")
    val WW1kX = arrayOf(4, 4, 6, 4, 8, 8, 10, 10, 12)
    val WW1wo = arrayOf(400, 450, 860, 680, 2800, 3400, 6100, 4100, 7300)
    val WW1rozmiar = arrayOf(1, 1, 2, 2, 3, 3, 4, 4, 4)

    val WW2nazwa = arrayOf("Maczuga", "Buzdygan", "Wekiera", "Młot bojowy", "Młot wielki", "Miażdżyciel")
    val WW2kX = arrayOf(6, 6, 6, 8, 10, 12)
    val WW2wo = arrayOf(1780, 2800, 3600, 5800, 10000, 12000)
    val WW2rozmiar = arrayOf(4, 6, 6, 6, 10, 10)

    val WW3nazwa = arrayOf("Mizerykordia", "Wbijak", "Rapier", "Trójząb", "Widły Bojowe", "Lanca")
    val WW3kX = arrayOf(6, 6, 8, 10, 10, 10)
    val WW3wo = arrayOf(840, 960, 2600, 3800, 4800, 3500)
    val WW3rozmiar = arrayOf(1, 1, 2, 4, 4, 10)

    val WW4nazwa = arrayOf("Cestus", "Tanto", "Katar", "Cinquedea", "Pata", "Wakizashi", "Kusarigama", "Katana", "Sasumata")
    val WW4kX = arrayOf(4, 6, 6, 8, 8, 10, 10, 12, 12)
    val WW4wo = arrayOf(400, 450, 860, 680, 2800, 3400, 6100, 4100, 7300)
    val WW4rozmiar = arrayOf(2, 1, 2, 2, 3, 3, 4, 4, 5)

    val itemRodzajBS = arrayOf("Broń miotana", "Pistolet strzałkowy", "Łuk", "Kusza", "Prochowa", "Futurystyczna")
    val itemRodzajPociski = arrayOf("Do łuków", "Do kusz", "Do broni palnej", "Inne")

    val AM0nazwa = arrayOf("Strzały (12 szt.)", "Strzały - nacinane (12 szt.)", "Strzały - zapalające (6 szt.)")
    val AM0wo = arrayOf(120, 240, 300)
    val AM1nazwa = arrayOf("Bełty (12 szt.)", "Bełty - przebijające (12 szt.)", "Bełty - ciężkie (6 szt.)")
    val AM1wo = arrayOf(150, 300, 450)
    val AM2nazwa = arrayOf("Kule i proch (12 szt.)", "Kule i proch - wysokiej jakości (12 szt.)", "Kule i proch - wybuchowe (6 szt.)")
    val AM2wo = arrayOf(400, 800, 1200)
    val AM3nazwa = arrayOf("Kamienie do procy (12 szt.)", "Strzałki do dmuchawki (12 szt.)", "Ołowiane kule (12 szt.)")
    val AM3wo = arrayOf(20, 150, 100)

    val US0nazwa = arrayOf("Wyważony Nóż", "Wyważony Toporek", "Proca", "Bat", "Dmuchawka", "Oszczep", "Włócznia")
    val US0kX = arrayOf(4, 4, 4, 6, 4, 2, 6)
    val US0range = arrayOf(10, 10, 20, 4, 15, 15, 10)
    val US0wo = arrayOf(320, 570, 280, 1250, 210, 2300, 4200)

    val US1nazwa = arrayOf("Pistolet strzałkowy", "Pistolet strzałkowy specjalny")
    val US1kX = arrayOf(4, 4)
    val US1range = arrayOf(15, 20)
    val US1wo = arrayOf(3200, 3350)

    val US2nazwa = arrayOf("Łuk krótki", "Łuk", "Łuk Długi", "Łuk Kompozytowy", "Łuk Elfi")
    val US2kX = arrayOf(6, 8, 10, 12, 12)
    val US2range = arrayOf(24, 32, 48, 48, 64)
    val US2wo = arrayOf(1300, 2400, 3500, 3100, 2800)

    val US3nazwa = arrayOf("Kusza lekka", "Kusza", "Kusza Ciężka")
    val US3kX = arrayOf(4, 6, 8)
    val US3range = arrayOf(32, 48, 64)
    val US3wo = arrayOf(2400, 4200, 6200)

    val US4nazwa = arrayOf("Granat", "Pistolet", "Rewolwera", "Rusznica", "Kacza stopa", "Działo", "Armata", "Bombarda")
    val US4kX = arrayOf(8, 6, 8, 10, 12, 20, 20, 20)
    val US4range = arrayOf(10, 15, 20, 32, 10, 100, 150, 200)
    val US4wo = arrayOf(520, 1570, 2280, 3250, 4210, 32300, 44200, 52300)

    val US5nazwa = arrayOf("Pistolet laserowy", "Pistolet plazmowy", "Karabinek laserowy")
    val US5kX = arrayOf(10, 12, 20)
    val US5range = arrayOf(40, 20, 100)
    val US5wo = arrayOf(520, 570, 3200)

    val narzedziaSkills = arrayOf("Astronomia", "Alchemia", "Charakteryzacja", "Chirurgia", "Leczenie chorób", "Krawiectwo", "Czytanie i pisanie", "Sztuka: Malarstwo", "Sztuka: Rysunek", "Rzemiosło: Szkutnictwo", "Rolnicze", "Kowalskie", "Zielarskie")
    val narzedziaAll = arrayOf("Astronomiczne", "Alchemiczne", "Charakteryzatorskie", "Chirurgiczne", "Medyczne", "Krawieckie", "Piśmiennicze", "Malarskie", "Rysunkowe", "Stolarskie", "Rolnicze", "Kowalskie", "Zielarskie")

    val toolsSpecialist = listOf(
        SpecialItem("Zestaw Chirurgiczny", "Narzędzia", 20, 500, "+10 do Chirurgii, wymagany do operacji"),
        SpecialItem("Wytrychy", "Narzędzia", 5, 800, "+10 do Otwierania zamków"),
        SpecialItem("Przybory Alchemiczne", "Narzędzia", 40, 1200, "Niezbędne do warzenia mikstur"),
        SpecialItem("Zestaw Inżynierski", "Narzędzia", 25, 600, "+5 do Inżynierii i Kartografii")
    )

    val instrumentsStrunowe = listOf(
        SpecialItem("Lutnia", "Instrumenty", 15, 400, "+5 do Uroku osobistego"),
        SpecialItem("Harfa mała", "Instrumenty", 20, 600),
        SpecialItem("Lira", "Instrumenty", 10, 350),
        SpecialItem("Mandora", "Instrumenty", 12, 450),
        SpecialItem("Psalterium", "Instrumenty", 8, 300)
    )

    val instrumentsDete = listOf(
        SpecialItem("Flet poprzeczny", "Instrumenty", 2, 150),
        SpecialItem("Dudy", "Instrumenty", 30, 350, "Zastraszenie +5 przy marszu"),
        SpecialItem("Szałamaja", "Instrumenty", 5, 250),
        SpecialItem("Róg sygnałowy", "Instrumenty", 10, 200),
        SpecialItem("Okaryna gliniana", "Instrumenty", 1, 80)
    )

    val instrumentsPerkusja = listOf(
        SpecialItem("Bębenek obręczowy", "Instrumenty", 10, 200),
        SpecialItem("Tamburyn", "Instrumenty", 5, 120),
        SpecialItem("Kołatka drewniana", "Instrumenty", 2, 50),
        SpecialItem("Czynele", "Instrumenty", 4, 180),
        SpecialItem("Kocioł (mały)", "Instrumenty", 25, 400)
    )

    val instrumentsSpecialist = instrumentsStrunowe + instrumentsDete + instrumentsPerkusja

    val itemsExotic = listOf(
        SpecialItem("Lampa z soczewką", "Egzotyczne", 15, 300, "Skupiony snop światła 15m"),
        SpecialItem("Lustro stalowe", "Egzotyczne", 5, 250, "Nietłukące, przydatne do zwiadu"),
        SpecialItem("Klepsydra (15 min)", "Egzotyczne", 8, 450),
        SpecialItem("Wodoodporna tuba na pergamin", "Egzotyczne", 10, 100)
    )

    val bogactwaAll = arrayOf("Miedź", "Srebro", "Złoto", "Gryfon", "Sztabka Srebra", "Sztabka Złota", "Biżuteria")

    fun calculateTotalWeight(heroData: String): Int {
        // Bardzo uproszczona logika wyciągania wagi z tekstu heroData
        // W prawdziwym systemie wagę trzymalibyśmy w zserializowanym obiekcie Hero
        var total = 0
        val inventoryLines = heroData.lines().filter { it.contains("kg") || it.contains("g") }
        inventoryLines.forEach { line ->
            val weightPart = line.substringAfterLast("(").substringBefore("kg").trim()
            total += weightPart.toIntOrNull() ?: 0
        }
        // Jeśli nie znaleziono wag, zwróć bazową wartość obciążenia (np. 10kg za sprzęt startowy)
        return if (total == 0) 10 else total
    }

    fun determineItemGender(name: String): String {
        val cleanName = name.replace("🅔 ", "").replace("🛡️ ", "").trim()
        val firstWord = cleanName.split(" ")[0].lowercase()

        // Wyjątki dla liczby mnogiej
        if (firstWord in listOf("widły", "spodnie", "rękawice", "buty", "drewniaki", "sandały", "chodaki", "półbuty", "kozaki", "mokasyny", "żołnierskie", "wełniaki")) return "PL"

        return when {
            firstWord.endsWith("a") -> "F"
            firstWord.endsWith("o") || firstWord.endsWith("e") -> "N"
            else -> "M"
        }
    }

    val armorLight = listOf(
        ArmorPiece("Skórzany czepiec", listOf("Głowa"), 1, 1, 0, 10),
        ArmorPiece("Skórzana czapa", listOf("Głowa"), 1, 1, 0, 10),
        ArmorPiece("Skórzana kamizelka", listOf("Korpus"), 1, 1, 0, 20),
        ArmorPiece("Skórzany kaftan", listOf("Korpus", "Ramiona"), 1, 1, 0, 25),
        ArmorPiece("Skórzana kurtka", listOf("Korpus", "Ramiona"), 1, 1, 0, 25),
        ArmorPiece("Skórzane nogawice", listOf("Nogi"), 1, 1, 0, 15),
        ArmorPiece("Skórzane spodnie", listOf("Nogi"), 1, 1, 0, 15),
        ArmorPiece("Skórzane rękawice", listOf("Ramiona"), 1, 1, 0, 10),
        ArmorPiece("Skórzane nagolennice", listOf("Nogi"), 1, 1, 0, 15),
        ArmorPiece("Ćwiekowana czapa", listOf("Głowa"), 2, 1, 1, 12),
        ArmorPiece("Ćwiekowany kaftan", listOf("Korpus", "Ramiona"), 2, 1, 1, 30),
        ArmorPiece("Ćwiekowane nogawice", listOf("Nogi"), 2, 1, 1, 20)
    )

    val armorMedium = listOf(
        ArmorPiece("Ćwiekowany czepiec", listOf("Głowa"), 2, 1, 1, 15),
        ArmorPiece("Ćwiekowana kurtka", listOf("Korpus", "Ramiona"), 2, 1, 1, 35),
        ArmorPiece("Kolczy czepiec", listOf("Głowa"), 2, 2, 1, 25),
        ArmorPiece("Kolcza czapa", listOf("Głowa"), 2, 2, 1, 25),
        ArmorPiece("Kolcza koszula", listOf("Korpus", "Ramiona"), 2, 2, 1, 45),
        ArmorPiece("Kolczuga", listOf("Korpus", "Ramiona"), 2, 2, 1, 45),
        ArmorPiece("Kolcza kamizelka", listOf("Korpus"), 2, 2, 1, 35),
        ArmorPiece("Kolcze rękawy", listOf("Ramiona"), 2, 2, 1, 25),
        ArmorPiece("Kolcza nogawica", listOf("Nogi"), 2, 2, 1, 30),
        ArmorPiece("Kolcze nogawice", listOf("Nogi"), 2, 2, 1, 30),
        ArmorPiece("Łuskowa czapa", listOf("Głowa"), 2, 1, 2, 25),
        ArmorPiece("Łuskowa kurtka", listOf("Korpus", "Ramiona"), 2, 1, 2, 50),
        ArmorPiece("Łuskowe nogawice", listOf("Nogi"), 2, 1, 2, 35)
    )

    val armorHeavy = listOf(
        ArmorPiece("Hełm łuskowy", listOf("Głowa"), 3, 2, 3, 30),
        ArmorPiece("Kirys łuskowy", listOf("Korpus"), 3, 2, 3, 60),
        ArmorPiece("Łuskowe rękawy", listOf("Ramiona"), 3, 2, 3, 40),
        ArmorPiece("Łuskowe nogawice", listOf("Nogi"), 3, 2, 3, 40),
        ArmorPiece("Hełm płytowy", listOf("Głowa"), 4, 3, 4, 40),
        ArmorPiece("Kirys płytowy", listOf("Korpus"), 4, 3, 4, 80),
        ArmorPiece("Płytowe naramienniki", listOf("Ramiona"), 4, 3, 4, 50),
        ArmorPiece("Płytowe nagolenniki", listOf("Nogi"), 4, 3, 4, 50),
        ArmorPiece("Płytowe rękawice", listOf("Ramiona"), 4, 3, 4, 25),
        ArmorPiece("Kirys stalowy", listOf("Korpus"), 4, 3, 4, 80)
    )

    fun calculateTotalCopper(heroData: String): Int {
        val (g, s, c) = parseMoney(heroData)
        return (g * 480) + (s * 12) + c
    }

    fun deductMoney(heroData: String, costCopper: Int): String {
        val total = calculateTotalCopper(heroData)
        if (total < costCopper) return heroData

        val remaining = total - costCopper
        val gold = remaining / 480
        val silver = (remaining % 480) / 12
        val copper = remaining % 12

        val lines = heroData.lines().toMutableList()
        updateCoinLine(lines, "⛀ Złota korona (zk)", gold)
        updateCoinLine(lines, "⛀ Srebrny szyling (s)", silver)
        updateCoinLine(lines, "⛀ Miedziany pens (p)", copper)

        return lines.joinToString("\n")
    }


    val bizuteriaNazwa = arrayOf("Pierścień", "Naszyjnik", "Amulet", "Bransoleta", "Sygnet", "Diadem", "Brosza", "Wisior", "Kolczyki")
    val przedmiotySkills = arrayOf("Gotowanie", "Leczenie ran", "Sztuka przetrwania", "Otwieranie zamków")
    val przedmiotyAll = arrayOf("Garnek", "Opatrunek", "Krzesiwo", "Wytrych")
    val przedmiotyDługie = arrayOf("Nitka", "Dratwa", "Sznur", "Lina", "Łańcuch")
    val przedmiotyŚwiatło = arrayOf("Pochodnia", "Świeczka", "Świeca", "Lampka olejowa", "Lampion", "Latarnia", "Lampa sztormowa")

    val itemRodzajUbranie = arrayOf("Głowa", "Korpus", "Ręce", "Nogi", "Korpus+Nogi", "Uniform")
    val ubranieG = arrayOf("Husta", "Bandana", "Słomiana Czapka", "Słomiany Kapelusz", "Lniana Czapka", "Lniany Kapelusz", "Wełniana czapka", "Kapelusz z rondlem")
    val ubranieGWO = arrayOf(80, 130, 150, 380, 220, 410, 360, 490)
    val ubranieK = arrayOf("Lniany worek", "Lniana koszulka", "Bawełniana koszulka", "Sweter", "Pulower", "Gorset", "Bluzka", "Smoking")
    val ubranieKWO = arrayOf(280, 430, 380, 750, 880, 760, 660, 1320)
    val ubranieR = arrayOf("Rękawice lniane", "Rękawice wełniane", "Rękawice bawełniane", "Rękawiczki skórzane", "Rękawiczki aksamitne")
    val ubranieRWO = arrayOf(220, 220, 220, 470, 160)
    val ubranieKN = arrayOf("Długa lniana koszula", "Długa wełniana koszula", "Suknia", "Płaszcz", "Płaszcz sztormowy")
    val ubranieKNWO = arrayOf(400, 480, 830, 1000, 1400)

    val ubranieS = arrayOf("Wełniaki", "Drewniaki", "Czułenka", "Sandały", "Chodaki", "Półbuty", "Kozaki", "Mokasyny", "Żołnierskie")
    val ubranieSWO = arrayOf(100, 180, 530, 350, 680, 720, 810, 960, 1160, 1860)

    val ubranieN = arrayOf("Łachmany", "Lniane spodnie", "Wełniane spodnie", "Nogawice z sukna", "Spodnie bubiaste", "Nogawice jedwabne", "Nogawice aksamitne")
    val ubranieNWO = arrayOf(50, 100, 150, 150, 200, 100, 100)
    val uniform = arrayOf("Fartuch", "Mundur straży", "Mundur wojskowy")
    val uniformWO = arrayOf(300, 880, 830)

    val przedmiotyTransport = arrayOf("Wózek", "Wóz", "Rydwan", "Bryczka", "Wóz kryty", "Dom na kołach", "Kareta", "Wóz pancerny")
    val zwierzętaHodowlaneSsaki = arrayOf("Fredka", "Królik", "Koza", "Owca", "Krowa", "Świnia")
    val zwierzętaDzikieSsaki = arrayOf("Kuna", "Łasica", "Zając", "Bóbr", "Lis", "Wilk", "Żubr", "Niedzwiedź")
    val zwierzętaHodowlanePtaki = arrayOf("Przepiórka", "Kurczak", "Gęś", "Kaczka", "Indyk")

    val itemPrefixesArmor = arrayOf("Brak", "Ludzki", "Elficki", "Khazadzki", "Uniwersalny", "Uniwersalna")
    val itemPrefixesWeapon = arrayOf("Brak", "Mocarny", "Hartowany", "Zwinny", "Niezłomny")
    val itemPrefixesJewelry = arrayOf("Brak", "Mocarny", "Hartowany", "Zwinny", "Niezłomny", "Bystry", "Dostojny")

    // --- ZBIORY ---
    val ziolaPodstawowe = listOf("Mięta pieprzowa", "Szałwia lekarska", "Pokrzywa zwyczajna", "Mniszek pospolity", "Rumianek polny", "Kora wierzby", "Krwawnik", "Dziki len", "Kwiat czarnego bzu", "Lawenda")
    val ziolaRzadkie = listOf("Belladona (Pokrzyk)", "Wilcza jagoda", "Tojad jadowity", "Arnika górska", "Korzeń żeń-szenia")
    val ziolaUnikatowe = listOf("Mandragora", "Kwiat paproci", "Srebrny lotos")

    val mineralyPodstawowe = listOf("Ruda żelaza", "Ruda miedzi", "Ruda cyny", "Węgiel brunatny", "Kwarc dymny", "Krzemień", "Piryt (Złoto głupców)", "Sól kamienna", "Grudka gliny", "Czysta siarka")
    val mineralyRzadkie = listOf("Ruda srebra", "Malachit", "Obsydian", "Fluoryt fluorowy", "Lazuryt (Lapis Lazuli)")
    val mineralyUnikatowe = listOf("Ruda złota", "Czarny diament", "Ruda meteorytowa (Meteoryt)")

    val rybyList = listOf("Płoć", "Okoń", "Leszcz", "Szczupak", "Sandacz", "Sum", "Jesiotr", "Pstrąg potokowy")
    val miesoList = listOf("Dziczyzna", "Drób", "Mięso z dzika", "Mięso z sarny", "Mięso z zająca", "Skóra zwierzęca", "Futro", "Poroże")

    val itemSuffixesWeaponJewelry = arrayOf("Brak", "Ognia", "Lodu", "Pioruna", "Trucizny", "Żywiołów", "Tragarza")
    val itemSuffixesArmor = arrayOf("Brak", "Antidotum", "Ciepła", "Szronu", "Uziemienia", "Życia", "Szybkości")

    val itemPrefixes = itemPrefixesArmor + itemPrefixesWeapon + itemPrefixesJewelry
    val itemSuffixes = itemSuffixesWeaponJewelry + itemSuffixesArmor

    val skillSpecjalny = arrayOf(
        "Bardzo odporny", "Bardzo krzepki", "Bardzo szybki", "Bystry wzrok",
        "Czuły smak", "Czuły węch", "Czuły słuch", "Szczęście", "Szósty zmysł"
    )

    val skillWW = arrayOf(
        "Akrobatyczny Atak", "Bijatyka", "Broń Improwizowana", "Broń Prosta",
        "Broń bojowa sieczna", "Broń bojowa obuchowa", "Broń bojowa kłuta",
        "Broń Egzotyczna", "Celny cios", "Finta", "Ogłuszenie", "Parowanie",
        "Riposta", "Rozbrojenie", "Walka dwoma broniami", "Walka na wierzchowcu",
        "Szybki refleks", "Zapasy"
    )

    val skillUS = arrayOf(
        "Broń miotana", "Broń: Pistolet strzałkowy", "Broń: Łuk krótki",
        "Broń: Łuk", "Broń: Łuk długi", "Broń: Kusza lekka", "Broń: Kusza ciężka",
        "Broń: Kusza automatyczna", "Broń: Łuk elfi", "Broń prochowa krótka",
        "Broń prochowa długa", "Broń prochowa miotana", "Celny rzut",
        "Celny strzał", "Broń: Dmuchawka"
    )

    val skillKrzepa = arrayOf(
        "Krzepa+", "Powożenie", "Silny Cios", "Tragarz", "Wiosłowanie"
    )

    val skillOdpornosc = arrayOf(
        "Odporność+", "Odporność na chłód", "Odporność na poparzenia",
        "Odporność na porażenia", "Odporność na narkotyki", "Odporność na trucizny",
        "Mocna głowa", "Zimna krew"
    )

    val skillZrecznosc = arrayOf(
        "Akrobatyka", "Cichy chód", "Doliniarstwo", "Jeździectwo", "Kamieniarstwo",
        "Krawiectwo", "Muzykalność (dęte)", "Muzykalność (strunowe)",
        "Muzykalność (perkusja)", "Muzykalność (klawiszowe)", "Oburęczność",
        "Otwieranie zamków", "Pływanie", "Sztuka: Kaligrafia", "Sztuka: Malarstwo",
        "Sztuka: Rysunek", "Sztuka: Rzeźba", "Snycerstwo", "Stolarstwo", "Taniec",
        "Ucieczka", "Ukrywanie się", "Uniki", "Woltyżerka", "Wspinaczka",
        "Wyzwalanie z więzów", "Zacieranie śladów", "Zwinne palce", "Rusznikarstwo",
        "Żonglowanie", "Taniec erotyczny", "Wysokie obcasy"
    )

    val skillInteligencja = arrayOf(
        "Astronomia", "Chemia", "Chirurgia", "Czytanie i pisanie",
        "Czytanie z ruchu warg", "Doświadczenie bojowe", "Farmacja", "Fizyka",
        "Gotowanie", "Inżynieria: Architektura", "Inżynieria: Broń prochowa",
        "Inżynieria: Maszyny", "Kartografia", "Kryptografia", "Leczenie ran",
        "Leczenie chorób", "Łowiectwo", "Matematyka", "Metalurgia", "Prawo",
        "Szacowanie", "Sztuka przetrwania", "Torturowanie", "Tresura", "Tropienie",
        "Uzdolnienia językowe", "Warzenie trucizn", "Widzenie w ciemności", "Wyczucie kierunku",
        "Wyczucie kierunku pod ziemią", "Wykrywanie pułapek", "Zarządzanie",
        "Zielarstwo"
    )

    val skillSW = arrayOf(
        "Szczęście", "Bardzo odporny", "Bardzo krzepki", "Bardzo szybki",
        "Bystry wzrok", "Czuły smak", "Czuły węch", "Czuły słuch", "Szósty zmysł",
        "Odporność na strach", "Odporność na grozę", "Hazard", "Medytacja",
        "Perswazja", "Różdżkarstwo", "Splatanie magii", "Telepatia", "Telekineza",
        "Magia Prosta", "Magia Powszechna", "Magia Tajemna [Aqshy]",
        "Magia Tajemna [Ghyran]", "Magia Tajemna [Chamon]", "Magia Tajemna [Ulgu]",
        "Magia Tajemna [Hysh]", "Magia Tajemna [Shyish]", "Magia Tajemna [Ghur]",
        "Magia Tajemna [Azyr]", "Wykrywanie magii", "Czynienie cudów"
    )

    val skillCharyzma = arrayOf(
        "Aktorstwo", "Błaznowanie", "Błyskotliwość", "Charakteryzacja",
        "Dowodzenie", "Erotyka", "Etykieta", "Gadanina", "Gawędziarstwo",
        "Komedianctwo", "Krasomóstwo", "Naśladownictwo", "Mimika",
        "Posłuch u zwierząt", "Przekupstwo", "Szyderstwo", "Śpiew",
        "Targowanie się", "Urok osobisty", "Uwodzenie", "Zastraszanie"
    )

    val skillWiedza = arrayOf(
        "Heraldyka", "Nawigacja", "Technologia", "Wiedza dynastyczna",
        "Wiedza (poszukiwanie)", "Wiedza o grzybach", "Wiedza o morzu",
        "Wiedza o roślinach", "Wiedza o rzekach", "Wiedza teologiczna",
        "Wiedza o zwierzętach: Ryby", "Wiedza o zwierzętach: Gady",
        "Wiedza o zwierzętach: Płazy", "Wiedza o zwierzętach: Ptaki",
        "Wiedza o zwierzętach: Ssaki", "Weterynaria", "Znajomość żeglugi",
        "Wiedza o demonach", "Wiedza o nieumarłych", "Opieka nad zwierzętami: Koń",
        "Opieka nad zwierzętami: Pies", "Opieka nad zwierzętami: Kot",
        "Opieka nad zwierzętami: Fretka", "Opieka nad zwierzętami: Kuna",
        "Inżynieria: Maszyny", "Kartografia", "Kryptografia", "Leczenie ran",
        "Leczenie chorób", "Łowiectwo", "Matematyka", "Metalurgia", "Prawo",
        "Szacowanie", "Sztuka przetrwania", "Torturowanie", "Tresura", "Tropienie",
        "Warzenie trucizn", "Widzenie w ciemności", "Wyczucie kierunku",
        "Wyczucie kierunku pod ziemią", "Wykrywanie pułapek", "Zarządzanie",
        "Zielarstwo"
    )

    val skillJezyk = arrayOf(
        "Język Klasyczny", "Język Tajemny [klasa]", "Sekretne znaki bardów",
        "Sekretne znaki bojowe", "Sekretne znaki drwali", "Sekretne znaki złodziei",
        "Język Magiczny",
        "Język obcy: Mowa Imperialny", "Język obcy: Pismo Imperialny",
        "Język obcy: Mowa Bretoński", "Język obcy: Pismo Bretoński",
        "Język obcy: Mowa Kislewski", "Język obcy: Pismo Kislewski",
        "Język obcy: Mowa Estalijski", "Język obcy: Pismo Estalijski",
        "Język obcy: Mowa Tileański", "Język obcy: Pismo Tileański",
        "Język obcy: Mowa Arabski", "Język obcy: Pismo Arabski",
        "Język obcy: Mowa Albioński", "Język obcy: Pismo Albioński",
        "Język obcy: Mowa Nordycki", "Język obcy: Pismo Nordycki",
        "Język obcy: Mowa Fan-Eltharin", "Język obcy: Pismo Fan-Eltharin",
        "Język obcy: Mowa Tor-Eltharin", "Język obcy: Pismo Tor-Eltharin",
        "Język obcy: Mowa Druhir", "Język obcy: Pismo Druhir",
        "Język obcy: Mowa Khazalid", "Język obcy: Pismo Khazalid",
        "Język obcy: Mowa Reikspiel", "Język obcy: Pismo Reikspiel",
        "Język obcy: Mowa Starosemiański", "Język obcy: Pismo Starosemiański",
        "Język obcy: Mowa Ungolski", "Język obcy: Pismo Ungolski",
        "Język obcy: Mowa Nippoński", "Język obcy: Pismo Nippoński"
    )

    fun isBasicSkill(skillName: String): Boolean {
        val basic = listOf(
            "Gadanina", "Urok osobisty", "Dowodzenie", "Zastraszanie", "Plotkowanie", "Targowanie się",
            "Cichy chód", "Ukrywanie się", "Pływanie", "Wspinaczka", "Jeździectwo", "Powożenie",
            "Wiosłowanie", "Mocna głowa", "Hazard", "Sztuka przetrwania", "Szacowanie", "Przeszukiwanie",
            "Spostrzegawczość", "Charakteryzacja", "Opieka nad zwierzętami", "Uniki"
        )
        return basic.any { skillName.contains(it, ignoreCase = true) }
    }

    val skillJezykObcyNazwa = arrayOf(
        "Brak", "Imperialny", "Bretoński", "Kislewski", "Estalijski", "Tileański",
        "Arabski", "Albioński", "Nordycki", "Fan-Eltharin", "Fan-Eltharin",
        "Tor-Eltharin", "Druhir", "Druhir", "Khazalid", "Nippoński"
    )

    val allNormalSkills = HeroLogic.skillWW + HeroLogic.skillUS + HeroLogic.skillKrzepa + HeroLogic.skillOdpornosc + HeroLogic.skillZrecznosc +
            HeroLogic.skillInteligencja + HeroLogic.skillSW + HeroLogic.skillCharyzma + HeroLogic.skillWiedza + HeroLogic.skillJezyk

    val skillRanks = listOf("Brak", "Uczeń", "Adept", "Ekspert", "Mistrz", "Arcymistrz")

    fun getTimeOfDay(timeStr: String): String {
        // timeStr expected: "City | d.M.y | HH:mm | ..." or just "HH:mm"
        val parts = if (timeStr.contains("|")) timeStr.split("|").map { it.trim() } else null
        val timePart = if (parts != null && parts.size >= 3) {
            parts[2]
        } else if (parts != null && parts.size >= 2) {
            parts[1]
        } else {
            timeStr
        }
        val hour = timePart.substringBefore(":").toIntOrNull() ?: 12
        return when (hour) {
            in 23..24, 0 -> "Północ"
            in 1..2 -> "Głęboka Noc"
            in 3..4 -> "Przedświt"
            in 5..6 -> "Świt"
            in 7..8 -> "Poranek"
            in 9..10 -> "Przedpołudnie"
            in 11..12 -> "Południe"
            in 13..14 -> "Popołudnie"
            in 15..16 -> "Późne Popołudnie"
            in 17..18 -> "Zmierzch"
            in 19..20 -> "Wieczór"
            in 21..22 -> "Noc"
            else -> "Dzień"
        }
    }

    fun getTimeAndWeatherLabel(heroData: String): String {
        val calendar = getCalendarData(heroData)
        val parts = calendar.split("|").map { it.trim() }
        val timePart = if (parts.size >= 3) parts[2] else ""
        val timeOfDay = if (timePart.isNotEmpty()) getTimeOfDay(timePart) else "Dzień"
        val weather = getWeather(heroData)
        
        val adjective = when (weather) {
            Weather.SUNNY -> when (timeOfDay) {
                "Północ", "Głęboka Noc", "Noc" -> "Gwiaździsta"
                "Zmierzch", "Wieczór" -> "Pogodny"
                else -> "Słoneczne"
            }
            Weather.CLOUDY -> "Pochmurne"
            Weather.RAINY -> "Deszczowe"
            Weather.STORMY -> "Burzowe"
            Weather.FOGGY -> "Mgliste"
            Weather.SNOWY -> "Śnieżne"
            Weather.HEATWAVE -> "Upalne"
        }
        
        // Dopasowanie końcówki do rodzaju
        val finalAdjective = if (timeOfDay.endsWith("a")) {
             adjective.removeSuffix("e").removeSuffix("y") + "a"
        } else if (timeOfDay.endsWith("ik") || timeOfDay.endsWith("ek") || timeOfDay.endsWith("ód") || timeOfDay.endsWith("ór") || timeOfDay.endsWith("ec") || timeOfDay == "Zmierzch" || timeOfDay == "Poranek") {
             if (adjective.endsWith("e")) adjective.removeSuffix("e") + "y" else adjective
        } else {
            adjective
        }

        return "$finalAdjective $timeOfDay".replaceFirstChar { it.uppercase() }
    }

    fun isNight(timeStr: String): Boolean {
        val label = getTimeOfDay(timeStr)
        return label in listOf("Noc", "Północ", "Głęboka Noc", "Przedświt")
    }

    fun isElf(heroData: String): Boolean {
        val line = heroData.lines().find { it.contains("Rasa:") } ?: return false
        return line.contains("Elf", ignoreCase = true)
    }

    fun generateLocation(zoneType: String): String {
        return if (zoneType == "NATURA") {
            listOf("Las", "Łąka", "Jezioro").random()
        } else {
            listOf("Karczma", "Wieś", "Miasteczko", "Miasto").random()
        }
    }

    val imieMall = arrayOf(     "Adam",
        "Achilles", "Adalbert", "Adolf", "Adonis", "Adrian",
        "Andrzej", "Antoni", "Artur", "Bartłomiej", "Benedykt",
        "Bernard", "Albert", "Aleksander", "Alfred", "Alwin",
        "Anatol", "Ansgar", "Arnold", "August", "Baldur",
        "Benedykt", "Bernd", "Bruno", "Cezary", "Cyprian",
        "Dariusz", "Denis", "Dominik", "Edmund", "Egon",
        "Erwin", "Feliks", "Ferdynand", "Franciszek", "Gerard",
        "Gilbert" ,"Grzegorz", "Henryk", "Herbert", "Hubert",
        "Ignacy", "Igor", "Iwan", "Jacek", "Jerzy",
        "Julian", "Kamil", "Kazimierz", "Klemens", "Konrad", //50
        "Krzysztof", "Lech", "Leszek", "Lucjan", "Ludwik",
        "Maciej", "Marek", "Mariusz", "Mirosław", "Norbert",
        "Oskar", "Oliwer", "Patryk", "Paweł", "Piotr",
        "Przemysław", "Radosław", "Rafał", "Robert", "Roman",
        "Roland", "Ryszard", "Sebastian", "Sławomir", "Stanisław",
        "Stefan", "Sylwester", "Szymon", "Tadeusz", "Tomasz",
        "Tytus", "Wacław", "Waldemar", "Wiesław", "Wiktor",
        "Witold", "Władysław", "Wojciech", "Zbigniew", "Zdzisław",
        "Zenon", "Zygmunt",     //92
        "Adrien", "Alain", "Alexandre", //93 Bretonia
        "Alexis", "Andre", "Antoine", "Armand", "Arthur", //100
        "Auguste", "Baptiste", "Benjamin", "Bernard", "Charles",
        "Christophe", "Claude", "Damien", "Daniel", "David",
        "Denis", "Edouard", "Emil", "Etienne", "Fabien",
        "Francois", "Gabriel", "Gaspard", "Georges", "Gregoire",
        "Guillaume", "Henri", "Hugo", "Jacques", "Jean",
        "Jerome", "Julien", "Laurent", "Louis", "Lucas",
        "Marc", "Mathieu", "Maxime", "Michel", "Nicolas",
        "Olivier", "Pascal", "Patrice", "Paul", "Philippe",
        "Pierre", "Quentin", "Raphael", "Remi", "Rene",
        "Sebastien", "Simon", "Thierry", "Thomas", "Tristan", //150
        "Valentin", "Victor", "Vincent", "Xavier", "Yann",
        "Yves", "Zacharie",     //157
        "Agimadi", "Alecu", "Alan", //158 Kislev
        "Aman", "Amiran", "Amu", "Antron", "Arutian",
        "Aleksey", "Bazil", "Dżram", "Dmitriy", "Gulżiеn",
        "Ivan", "Kiamam", "Maksim", "Michaił", "Nikołaj",
        "Ogonеs", "Parfеrij", "Pavel", "Sergey", "Siewierian",
        "Sofron", "Vladimir", "Vladislav", "Vladlen", "Igor",
        "Ilya", "Iwan", "Jegor", "Kirill", "Konstantin",
        "Maksymilian", "Mikhail", "Nikolay", "Oleg", "Roman",
        "Seymur", "Timur", "Viktor", "Yuriy", "Zachary", //200
        "Abel", "Adrien", "Alejandro", "Alfonso", "Andres",
        "Antonio", "Arturo", "Carlos", "Cesar", "Daniel",
        "David", "Diego", "Eduardo", "Emilio", "Enrique",
        "Ernesto", "Fernando", "Francisco", "Gabriel", "Gonzalo",
        "Guillermo", "Gustavo", "Hector", "Ignacio", "Ismael",
        "Javier", "Jesus", "Joaquin", "Jorge", "Jose",
        "Juan", "Julio", "Luis", "Manuel", "Marcos",
        "Mario", "Martin", "Miguel", "Nicolas", "Oscar",
        "Pablo", "Pedro", "Rafael", "Ramon", "Raul",
        "Ricardo", "Roberto", "Rodrigo", "Ruben", "Salvador", //250
        "Santiago", "Sebastian", "Sergio", "Tomas", "Vicente",
        "Xavier", "Yago", "Zacarias",	//258
        "Alberto", "Alessandro",
        "Alfredo", "Andrea", "Antonio", "Armando", "Arturo",
        "Bruno", "Carlo", "Cesare", "Claudio", "Daniele",
        "Dario", "Diego", "Eduardo", "Elio", "Emilio",
        "Enrico", "Fabio", "Federico", "Filippo", "Francesco",
        "Gabriele", "Giacomo", "Giovanni", "Giuseppe", "Guido",
        "Leonardo", "Luca", "Lorenzo", "Marco", "Mario",
        "Matteo", "Maurizio", "Nicola", "Paolo", "Pietro",
        "Riccardo", "Roberto", "Salvatore", "Sergio", "Simone", //300
        "Stefano", "Tommaso", "Umberto", "Valerio", "Vincenzo",
        "Walter", "William", "Yuri", "Zeno", //309
        "Aleksey", //310 Kislev
        "Alien", "Alwin", "Amiran", "Amu", "Antron",
        "Arutian", "Aleksey", "Bałażej", "Dżram", "Dmitriy",
        "Gulżiеn", "Ilgan", "Ivan", "Kiamam", "Maksim",
        "Michaił", "Nikolaj", "Ogonеs", "Parfеrij", "Pavel",
        "Sergey", "Siewierian", "Sofron", "Vladimir", "Vladislav",
        "Vladlen", "Igor", "Ilya", "Iwan", "Jegor",
        "Kirill", "Konstantin", "Maksymilian", "Mikhail", "Nikita",
        "Nikolay", "Oleg", "Petr", "Roman", "Semyon", //350
        "Seymur", "Timur", "Viktor", "Yuriy", "Zacharie"	//355
    )

    val imieKall = arrayOf( "Ada",
        "Aga", "Adela", "Adelajda", "Adelina", "Adrianna",
        "Agata", "Agnieszka", "Albertyna", "Aleksandra", "Alicja",
        "Alina", "Amelia", "Anastazja", "Angela", "Aneta",
        "Angelika", "Ania", "Aniela", "Anita", "Anna",
        "Annabella", "Antonina", "Augustyna", "Aurelia", "Baptysta",
        "Barbara", "Beata", "Bernadeta", "Berta", "Bianka",
        "Brygida", "Cecylia", "Celina", "Dagmara", "Daniela",
        "Daria", "Diana", "Dominika", "Dorota", "Edyta",
        "Florencja", "Genowefa", "Gwendolina", "Halina", "Helena",
        "Helga", "Hera", "Irena", "Iwona", "Iza", //50
        "Izabela", "Jadwiga", "Kamila", "Katarzyna", "Klara",
        "Klaudia", "Klaudyna", "Kornelia", "Łucja", "Małgorzata",
        "Marcelina", "Margareta", "Maria", "Marianna", "Mariela",
        "Marta", "Martyna", "Maryla", "Paulina", "Rosa", "Rosalia",
        "Ursula", "Waleria", "Zuzanna", "Żaneta", //74
        "Adele", //75
        "Agathe", "Agnes", "Alice", "Amelie", "Anaïs",
        "Andrea", "Angelique", "Anna", "Anne", "Ariane",
        "Aurelie", "Beatrice", "Camille", "Caroline", "Cecile",
        "Chloe", "Claire", "Clemence", "Coralie", "Delphine",
        "eleonore", "elodie", "emilie", "Eugenie", "Flore", //100
        "Gabrielle", "Helene", "Ines", "Isabelle", "Jeanne",
        "Josephine", "Juliette", "Justine", "Laure", "Lea",
        "Leonie", "Louise", "Lucie", "Madeleine", "Margot",
        "Marie", "Mathilde", "Nathalie", "Noemie", "Olivia",
        "Pauline", "Raphaelle", "Reine", "Rosalie", "Sabine",
        "Sylvie", "Valerie", "Victoire", "Violette", "Virginie",
        "Zoe", //131
        "Adriana", "Agata", "Aida", "Alba",  //135
        "Alejandra", "Alicia", "Alma", "Amalia", "Ana",
        "Andrea", "Angela", "Antonia", "Ariadna", "Beatriz",
        "Belen", "Blanca", "Camila", "Carla", "Carmen",	//150
        "Catalina", "Celina", "Clara", "Conga", "Cristina",
        "Dolores", "Elena", "Elisa", "Emilia", "Esperanza",
        "Estela", "Eva", "Fatima", "Flor", "Gabriela",
        "Gloria", "Gracia", "Helena", "Ines", "Irene",
        "Isabel", "Jimena", "Josefa", "Juana", "Julia",
        "Laura", "Leticia", "Lidia", "Lorena", "Lucia",
        "Luisa", "Magdalena", "Manuela", "Margarita", "Maria",
        "Marina", "Marta", "Mercedes", "Monica", "Natalia",
        "Nerea", "Nuria", "Olga", "Paloma", "Patricia",
        "Paula", "Raquela", "Rebeca", "Roci", "Rosa", //200
        "Rosalia", "Sara", "Silvia", "Sofia", "Sonia",
        "Susana", "Teresa", "Valentina", "Verónica", "Victoria",
        "Virginia", "Ximena", "Yolanda", "Zoe", //214
        "Adriana", //215
        "Agnese", "Alessia", "Alessandra", "Anastasia", "Andrea",
        "Antonella", "Angela", "Agostina", "Aurora", "Alice",
        "Azzurra", "Ada", "Benedetta", "Beatrice", "Barbara",
        "Bruna", "Brigitta", "Bella", "Brigida", "Bianca",
        "Chiara", "Camilla", "Carla", "Carolina", "Caterina",
        "Carlotta", "Cecilia", "Chiara", "Clara", "Claudia",
        "Chanel", "Crystal", "Clotilde", "Cornelia", "Costanza", //250
        "Cristina", "Domenica", "Daniela", "Daria", "Dorotea",
        "Elena", "Enrica", "Emma", "Editta", "Elisabetta",
        "Edvige", "Elena", "Elisa", "Elvira", "Eva", "Evelina",
        "Francesca", "Federica", "Giulia", "Giuseppina", "Gianna",
        "Ginevra", "Giovanna", "Gloria", "Giulia", "Greta",
        "Giorgia", "Helena", "Hera", "Hala", "Halima",
        "Iolanda", "Irene", "Isabella", "Jacobella", "Jianna",
        "Julia", "Kenza", "Karina", "Kira", "Klara",
        "Lucia", "Laura", "Lidia", "Lara", "Lucia",
        "Lorenza", "Laraina", "Ludmila", "Maddalena", "Margarita", //300
        "Martina", "Marsala", "Madonna", "Marcella", "Matilde",
        "Nicola", "Nolita", "Nydia", "Orquidea", "Otavia",
        "Orlena", "Ortensia", "Patrizia", "Paola", "Paciana",
        "Perlita", "Penelope", "Perla", "Priscilla", "Primavera",
        "Rosa", "Rosetta", "Rosalia", "Silvia", "Sofia", "Sabina",
        "Severina", "Stella", "Susanna", "Serena", "Traviata",
        "Teresa", "Teodora", "Ugolina", "Uliva", "Unna",
        "Veronica", "Vittoria", "Valentina", "Wassila", "Wissal",
        "Wilma", "Yvonne", "Yassmin", "Yara", "Yuna", //350
        "Ysabel", "Zoila", "Zia", "Zana", "Zoe",
        "Zita", //356
        "Aleksandra", "Alicja", "Alina", "Amelia", //360
        "Anastazja", "Ariela", "Anna", "Antonia", "Ariadna",
        "Bianka", "Bogna", "Daria", "Dagmara", "Delfina",
        "Elena", "Estera", "Eugenia", "Flora", "Florentyna",
        "Galina", "Helena", "Irina", "Jekaterina", "Julia",
        "Kamila", "Kira", "Larysa", "Lubow", "Ludmiła",
        "Marina", "Mariela", "Nadieżda", "Natasza", "Nikita",
        "Oksana", "Olena", "Olga", "Polina", "Raisa",
        "Sonia", "Sofia", "Svietla", "Tatiana", "Uliana", //400
        "Waleria", "Zoja"
    )

    val colorMap = mapOf(
        0 to Color(0xFF2E7D32),
        2 to Color(0xFF2E7D32),
        10 to Color(0xFF4CAF50),
        20 to Color(0xFF8BC34A),
        30 to Color(0xFFCDDC39),
        40 to Color(0xFFFFEB3B),
        50 to Color(0xFFFFC107),
        60 to Color(0xFFFF9800),
        70 to Color(0xFFF44336),
        80 to Color(0xFFD32F2F),
        88 to Color(0xFFD32F2F),
        90 to Color(0xFFB71C1C),
        99 to Color(0xFFB71C1C)
    )

    val pancGłowaNazwa = arrayOf("Skórzany Czepiec", "Ćwiekowany Czepiec", "Kolczy Czepiec", "Hełm Łuskowy", "Hełm Płytowy Otwarty", "Hełm Płytowy Zamknięty")
    val pancGłowaWO = arrayOf(500, 750, 1500, 2500, 4000, 5000)
    val pancRękaNazwa = arrayOf("Skórzany Rękaw", "Ćwiekowany Rękaw", "Rękaw Kolczy", "Łuskowy Rękaw", "Płytowy Rękaw")
    val pancRękaWO = arrayOf(500, 850, 1100, 1500, 2000, 3000)
    val pancKorpusNazwa = arrayOf("Skórzana Kamizelka", "Skórzana Kurtka", "Ćwiekowana Kamizelka", "Ćwiekowana Kurtka", "Kolcza Kamizelka", "Kolcza Kurtka", "Kolcza Opończa", "Łuskowy Bezrękawnik", "Łuskowa Kurtka", "Kirys", "Zbroja Płytowa")
    val pancKorpusWO = arrayOf(1200, 2500, 1800, 2700, 2300, 4000, 6000, 5000, 7500, 4000, 5600)
    val pancNogaNazwa = arrayOf("Skórzana Nogawica", "Ćwiekowana Nogawica", "Nogawica Kolcza", "Nogawica Łuskowa", "Nogawica Płytowa")
    val pancNogaWO = arrayOf(550, 900, 1200, 1600, 2100, 3000)

    // --- METHODS ---

    fun getHeroInventory(heroData: String): List<TradeItem> {
        val lines = heroData.lines()
        val eqIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
        if (eqIdx == -1) return emptyList()
        
        val inventory = mutableListOf<TradeItem>()
        val inventoryLines = lines.drop(eqIdx + 1).takeWhile { it.startsWith("   •") || it.startsWith("   🗃") }
        
        inventoryLines.forEach { line ->
            val cleanLine = line.substringAfter("•").substringAfter("🗃").trim()
            if (cleanLine.isNotEmpty() && !cleanLine.contains("⛀")) {
                val name = cleanLine.split("[").first().trim()
                val price = findItemPrice(name)
                inventory.add(TradeItem(name, price, "Inventory"))
            }
        }
        return inventory
    }

    fun addItemToHero(heroData: String, item: TradeItem): String {
        val lines = heroData.lines().toMutableList()
        val eqIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
        if (eqIdx != -1) {
            var insertPos = eqIdx + 1
            while (insertPos < lines.size && lines[insertPos].contains("⛀")) insertPos++
            
            // Add quality for some items
            val finalName = if (item.category.contains("Broń", true) || item.category.contains("Pancerz", true)) {
                val quality = (80..95).random()
                "${item.name} [Jakość: $quality%]"
            } else item.name
            
            lines.add(insertPos, "   • $finalName")
        }
        return lines.joinToString("\n")
    }

    fun removeItemFromHero(heroData: String, item: TradeItem): String {
        val lines = heroData.lines().toMutableList()
        val eqIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
        if (eqIdx != -1) {
            val idx = lines.indexOfFirst { it.startsWith("   •") && it.contains(item.name, ignoreCase = true) }
            if (idx != -1) {
                lines.removeAt(idx)
            }
        }
        return lines.joinToString("\n")
    }

    fun getShopInventory(shop: Shop, forceAll: Boolean = false, building: BuildingInstance? = null, currentTime: String = "00:00"): List<TradeItem> {
        val datePart = currentTime.split(" ")[0]
        val timePart = if (currentTime.contains(" ")) currentTime.split(" ")[1] else currentTime
        val hour = timePart.split(":")[0].toIntOrNull() ?: 0

        if (building != null && !forceAll) {
            val dateRefreshed = building.lastRefreshDate == datePart
            val isAfterFive = hour >= 5
            
            if (dateRefreshed || (building.inventory.isNotEmpty() && !isAfterFive)) {
                return building.inventory
            }
        }
        
        val list = mutableListOf<TradeItem>()

        // Nowa logika specjalizacji i losowania
        when (shop.name) {
            "Kowal" -> {
                repeat(2) {
                    val idx = Random.nextInt(WW1nazwa.size)
                    list.add(TradeItem(WW1nazwa[idx], WW1wo[idx], "Sieczna"))
                }
                repeat(2) {
                    val idx = Random.nextInt(WW2nazwa.size)
                    list.add(TradeItem(WW2nazwa[idx], WW2wo[idx], "Obuchowa"))
                }
                repeat(2) {
                    val idx = Random.nextInt(WW3nazwa.size)
                    list.add(TradeItem(WW3nazwa[idx], WW3wo[idx], "Kłuta"))
                }
                if (Random.nextFloat() < 0.3f) {
                    val idx = Random.nextInt(WW4nazwa.size)
                    list.add(TradeItem(WW4nazwa[idx], WW4wo[idx], "Egzotyczna"))
                }
            }
            "Płatnerz" -> {
                val headArmor = listOf("Skórzany czepiec", "Ćwiekowany czepiec", "Hełm łuskowy", "Hełm płytowy")
                repeat(2) {
                    val name = headArmor.random()
                    list.add(TradeItem(name, findItemPrice(name), "Głowa"))
                }
                val bodyArmor = listOf("Skórzana kamizelka", "Skórzana kurtka", "Ćwiekowany kaftan", "Kirys łuskowy", "Kirys płytowy")
                repeat(2) {
                    val name = bodyArmor.random()
                    list.add(TradeItem(name, findItemPrice(name), "Korpus"))
                }
                val otherArmor = listOf("Skórzane nogawice", "Ćwiekowane nogawice", "Łuskowe nogawice", "Płytowe naramienniki", "Płytowe nagolenniki", "Płytowe rękawice", "Tarcza", "Tarcza duża")
                repeat(3) {
                    val name = otherArmor.random()
                    list.add(TradeItem(name, findItemPrice(name), "Inne"))
                }
            }
            "Rymarz" -> {
                val bags = itemSakwy + itemTorby + itemBuklaki
                repeat(4) {
                    val name = bags.random()
                    list.add(TradeItem(name, findItemPrice(name), "Akcesoria"))
                }
                list.add(TradeItem("Koń wierzchowy (+20 Szy)", 38400, "Zwierzęta"))
                list.add(TradeItem("Muł roboczy", 14400, "Zwierzęta"))
            }
            else -> {
                if (shop.name == "Miejski Magazyn" || shop.name.contains("MAGAZYN")) {
                    val allClothing = (ubranieG + ubranieK + ubranieR + ubranieKN + ubranieS + ubranieN).toList().shuffled().take(3)
                    allClothing.forEach { name ->
                        list.add(TradeItem(name, findItemPrice(name), "Odzienie"))
                    }
                    val allTools = (narzedziaAll).toList().shuffled().take(3)
                    allTools.forEach { name ->
                        list.add(TradeItem(name, findItemPrice(name), "Narzędzie"))
                    }
                } else {
                    shop.categories.forEach { cat ->
                        when (cat) {
                            "Broń prosta" -> WW0nazwa.forEachIndexed { i, n -> list.add(TradeItem(n, WW0wo[i], cat)) }
                            "Sieczna" -> WW1nazwa.forEachIndexed { i, n -> list.add(TradeItem(n, WW1wo[i], cat)) }
                            "Obuchowa" -> WW2nazwa.forEachIndexed { i, n -> list.add(TradeItem(n, WW2wo[i], cat)) }
                            "Kłuta" -> WW3nazwa.forEachIndexed { i, n -> list.add(TradeItem(n, WW3wo[i], cat)) }
                            "Broń Egzotyczna" -> WW4nazwa.forEachIndexed { i, n -> list.add(TradeItem(n, WW4wo[i], cat)) }
                            "Łuk" -> US2nazwa.forEachIndexed { i, n -> list.add(TradeItem(n, US2wo[i], cat)) }
                            "Kusza" -> US3nazwa.forEachIndexed { i, n -> list.add(TradeItem(n, US3wo[i], cat)) }
                            "Prochowa" -> US4nazwa.forEachIndexed { i, n -> list.add(TradeItem(n, US4wo[i], cat)) }
                            "Miotana" -> US0nazwa.forEachIndexed { i, n -> list.add(TradeItem(n, US0wo[i], cat)) }
                            "Sakwy i sakiewki" -> itemSakwy.forEach { list.add(TradeItem(it, findItemPrice(it), cat)) }
                            "Torby i Plecaki" -> itemTorby.forEach { list.add(TradeItem(it, findItemPrice(it), cat)) }
                            "Tykwy i Bukłaki" -> itemBuklaki.forEach { list.add(TradeItem(it, findItemPrice(it), cat)) }
                            "Konie" -> {
                                list.add(TradeItem("Koń wierzchowy (+20 Szy)", 38400, cat))
                                list.add(TradeItem("Koń juczny (+500 Udź)", 19200, cat))
                                list.add(TradeItem("Kuc (+10 Wytrzym)", 9600, cat))
                            }
                            "Posiłki" -> {
                                list.add(TradeItem("Lichy posiłek", 15, cat))
                                list.add(TradeItem("Zwyczajny posiłek", 30, cat))
                                list.add(TradeItem("Dostatni posiłek", 60, cat))
                                list.add(TradeItem("Uczta", 150, cat))
                            }
                            "Trunki" -> {
                                list.add(TradeItem("Piwo (kufel)", 5, cat))
                                list.add(TradeItem("Wino (kielich)", 15, cat))
                                list.add(TradeItem("Gorzałka (kieliszek)", 10, cat))
                            }
                            "Nocleg" -> {
                                list.add(TradeItem("Nocleg w stajni", 10, cat))
                                list.add(TradeItem("Wspólna sala", 25, cat))
                                list.add(TradeItem("Pokój wieloosobowy", 60, cat))
                                list.add(TradeItem("Pokój prywatny", 150, cat))
                                list.add(TradeItem("Luksusowy apartament", 450, cat))
                            }
                        }
                    }
                }
            }
        }

        // Zapisujemy nowy ekwipunek w budynku
        if (building != null) {
            // W kotlinie BuildingInstance jest data class, więc potrzebujemy metody 'copy'
            // Ale z poziomu CommonLogic nie możemy go zmienić na miejscu, musimy polegać na systemie zapisu UI.
            // Zwracamy listę, a CityScreen/BuildingsScreen zajmie się zapisem w buildingInventories.
        }

        return list
    }

    fun findItemPrice(name: String): Int {
        return ItemRegistry.getPrice(name)
    }

    fun filterInventoryByHero(inventory: List<TradeItem>, heroData: String): List<TradeItem> {
        // Tu można dodać logikę ukrywania przedmiotów, których postać nie może używać (np. brak umiejętności)
        // Na razie zwracamy wszystko.
        return inventory
    }

    fun getPlayerSellableItems(heroData: String, shop: Shop, shopInventory: List<TradeItem>): List<TradeItem> {
        val lines = heroData.lines()
        val eqIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
        if (eqIdx == -1) return emptyList()

        val inventoryLines = lines.drop(eqIdx + 1).takeWhile { it.startsWith("   •") }
        val sellable = mutableListOf<TradeItem>()

        inventoryLines.forEach { line ->
            val itemName = line.replace("   •", "").trim()
            if (itemName.contains("⛀")) return@forEach
            
            val price = findItemPrice(itemName)
            // Można tu dodać filtrowanie, aby gracz mógł sprzedać tylko to, co dany sklep skupuje
            // Ale dla uproszczenia pozwalamy sprzedać wszystko w każdym sklepie (za 50% ceny)
            sellable.add(TradeItem(itemName, price, "Twój Ekwipunek"))
        }
        return sellable
    }

    data class SpecialItem(val name: String, val category: String, val weight: Int, val cost: Int, val bonus: String = "")

    fun getHeroStrength(heroData: String): Int = getStatValue(heroData, "K")

    fun getHeroStrengthBonus(heroData: String): Int {
        val lines = heroData.lines()
        val line = lines.find { l -> 
            val clean = l.trim().removePrefix("•").trim()
            clean.startsWith("Krzepa:") || clean.startsWith("K:")
        } ?: return 0
        return if (line.contains("|")) {
            line.substringAfter("|").trim().split(" ")[0].split("(")[0].trim().toIntOrNull() ?: (getHeroStrength(heroData) / 10)
        } else {
            getHeroStrength(heroData) / 10
        }
    }

    fun getHeroToughness(heroData: String): Int = getStatValue(heroData, "O")

    fun getHeroToughnessBonus(heroData: String): Int {
        val lines = heroData.lines()
        val line = lines.find { l -> 
            val clean = l.trim().removePrefix("•").trim()
            clean.startsWith("Odporność:") || clean.startsWith("Odp:")
        } ?: return 0
        return if (line.contains("|")) {
            line.substringAfter("|").trim().split(" ")[0].split("(")[0].trim().toIntOrNull() ?: (getHeroToughness(heroData) / 10)
        } else {
            getHeroToughness(heroData) / 10
        }
    }

    fun calculateCurrentWeight(heroData: String): Int {
        val lines = heroData.lines()
        val eqIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:", ignoreCase = true) }
        if (eqIdx == -1) return 0
        
        val inventoryLines = lines.drop(eqIdx + 1).takeWhile { 
            it.startsWith("   •") || it.startsWith("•") || it.isBlank() || it.trim().startsWith("⛀")
        }
        
        var totalWeight = 0.0
        var tragarzBonusCount = 0

        inventoryLines.forEach { itemLine ->
            val item = itemLine.replace("   •", "").replace("•", "").trim()
            if (item.isEmpty()) return@forEach
            
            // Waga monet (0.015 kg = 0.15 j. assuming 1kg = 10 units in this system, or just consistent 0.15 weight units)
            if (item.contains("⛀")) {
                val quantity = item.split(Regex("\\s+")).last().toIntOrNull() ?: 0
                totalWeight += quantity * 0.15
                return@forEach
            }
            
            // Liczenie bonusu Tragarza (max 3 przedmioty biżuterii)
            val isJewelry = bizuteriaNazwa.any { item.contains(it, ignoreCase = true) }
            if (isJewelry && item.contains("Tragarza", ignoreCase = true) && tragarzBonusCount < 3) {
                tragarzBonusCount++
            }

            totalWeight += getItemWeight(item)
        }

        // Redukcja obciążenia: 10% za każdy przedmiot (max 30%)
        val reduction = 1.0 - (tragarzBonusCount * 0.10)
        return (totalWeight * reduction).toInt()
    }

    fun getItemWeight(name: String): Int {
        return ItemRegistry.getWeight(name)
    }

    fun damageRandomItem(heroData: String, damagePercent: Int): String {
        val lines = heroData.lines().toMutableList()
        val eqIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
        if (eqIdx == -1) return heroData
        
        val invLinesIndices = mutableListOf<Int>()
        for (i in eqIdx + 1 until lines.size) {
            if (lines[i].startsWith("   •")) {
                val name = lines[i].lowercase()
                if (name.contains("miecz") || name.contains("topór") || name.contains("pancerz") || 
                    name.contains("hełm") || name.contains("tarcza") || name.contains("szata")) {
                    invLinesIndices.add(i)
                }
            } else if (lines[i].isNotBlank() && !lines[i].startsWith(" ")) {
                break
            }
        }
        
        if (invLinesIndices.isEmpty()) return heroData
        
        val targetIdx = invLinesIndices.random()
        val line = lines[targetIdx]
        val qualityMatch = Regex("\\[Jakość: (\\d+)%\\]").find(line)
        val currentQuality = qualityMatch?.groupValues?.get(1)?.toIntOrNull() ?: 100
        val newQuality = (currentQuality - damagePercent).coerceAtLeast(10)
        
        lines[targetIdx] = if (qualityMatch != null) {
            line.replace(Regex("\\[Jakość: \\d+%\\]"), "[Jakość: $newQuality%]")
        } else {
            "$line [Jakość: $newQuality%]"
        }
        
        return lines.joinToString("\n")
    }

    fun getHeroCharisma(heroData: String): Int = getStatValue(heroData, "Cha")

    fun getSkillRank(heroData: String, skillName: String): String? {
        var isReadingSkills = false
        heroData.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.contains("UMIEJĘTNOŚCI")) {
                isReadingSkills = true
            } else if (isReadingSkills && (trimmed.startsWith("CECHY") || trimmed.startsWith("EKWIPUNEK") || trimmed.startsWith("ZDOLNOŚCI"))) {
                isReadingSkills = false
            } else if (isReadingSkills && line.contains("•") && line.contains(skillName, ignoreCase = true)) {
                val clean = line.replace("•", "").trim()
                return if (clean.contains("(")) {
                    clean.substringAfter("(").substringBefore(")").trim()
                } else {
                    "Uczeń"
                }
            }
        }
        return null
    }

    fun hasSkill(heroData: String, skillName: String): Boolean {
        return getSkillRank(heroData, skillName) != null
    }

    fun getHeroWeightLimit(heroData: String): Int {
        val strength = getHeroStrength(heroData)
        val toughness = getHeroToughness(heroData)
        
        // Dodatkowe udźwigi z ekwipunku
        val lines = heroData.lines()
        val eqIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
        var extraWeight = 0
        if (eqIdx != -1) {
            lines.drop(eqIdx + 1).takeWhile { it.startsWith("   •") }.forEach { line ->
                if (line.contains("Koń juczny (+500 Udź)")) extraWeight += 500
                if (line.contains("Muł roboczy")) extraWeight += 300 // Muł daje mniej niż koń juczny
            }
        }
        
        return (strength * 8) + (toughness * 10) + extraWeight
    }

    fun getEncumbrance(current: Int, limit: Int): com.example.genpro.EncumbranceLevel {
        val ratio = if (limit > 0) current.toFloat() / limit else 2.0f
        return when {
            ratio <= 1.0f -> com.example.genpro.EncumbranceLevel.LEKKIE
            ratio <= 1.33f -> com.example.genpro.EncumbranceLevel.OBCIAZONY
            ratio <= 1.66f -> com.example.genpro.EncumbranceLevel.PRZECIAZONY
            else -> com.example.genpro.EncumbranceLevel.SKRAJNE
        }
    }


    fun formatPrice(totalCopper: Int): String {
        val g = totalCopper / 480
        val s = (totalCopper % 480) / 12
        val c = totalCopper % 12
        val res = mutableListOf<String>()
        if (g > 0) res.add("${g}zk")
        if (s > 0) res.add("${s}s")
        if (c > 0) res.add("${c}p")
        return if (res.isEmpty()) "0p" else res.joinToString(" ")
    }

    fun parseMoney(heroData: String): Triple<Int, Int, Int> {
        var g = 0; var s = 0; var c = 0
        heroData.lines().forEach { line ->
            if (line.contains("⛀")) {
                val value = line.trim().split(Regex("\\s+")).last().toIntOrNull() ?: 0
                when {
                    line.contains("Złota", ignoreCase = true) || line.contains("Koron", ignoreCase = true) -> g = value
                    line.contains("Srebrn", ignoreCase = true) || line.contains("Szyling", ignoreCase = true) -> s = value
                    line.contains("Miedzia", ignoreCase = true) || line.contains("Pens", ignoreCase = true) -> c = value
                }
            }
        }

        return Triple(g, s, c)
    }

    fun updateCoinLine(lines: MutableList<String>, pattern: String, newValue: Int) {
        val idx = lines.indexOfFirst { it.contains(pattern) }
        val standardPrefix = "   • "
        if (idx != -1) {
            if (newValue > 0) {
                // Zaktualizuj liczbę, zapewniając standardowy prefix i dwa odstępy
                lines[idx] = "$standardPrefix$pattern  $newValue"
            } else {
                lines.removeAt(idx)
            }
        } else if (newValue > 0) {
            val eqIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
            if (eqIdx != -1) {
                // Dodajemy monety na początku ekwipunku, ale w odpowiedniej kolejności
                var insertPos = eqIdx + 1
                while (insertPos < lines.size && lines[insertPos].contains("⛀")) {
                    val currentLine = lines[insertPos]
                    if (pattern.contains("Złota") && (currentLine.contains("Srebrny") || currentLine.contains("Miedziany"))) break
                    if (pattern.contains("Srebrny") && currentLine.contains("Miedziany")) break
                    insertPos++
                }
                lines.add(insertPos, "   • $pattern  $newValue")
            }
        }
    }

    fun processTransaction(
        heroData: String,
        itemName: String,
        priceCopper: Int,
        isSelling: Boolean,
        manualG: Int = 0,
        manualS: Int = 0,
        manualC: Int = 0,
        isManual: Boolean = false,
        isRepair: Boolean = false
    ): String {
        val lines = heroData.lines().toMutableList()
        val (gold, silver, copper) = parseMoney(heroData)

        val newG: Int; val newS: Int; val newC: Int

        if (isSelling) {
            val totalPlusProfit = (gold * 480) + (silver * 12) + copper + priceCopper
            newG = totalPlusProfit / 480
            newS = (totalPlusProfit % 480) / 12
            newC = totalPlusProfit % 12

            // Usuń przedmiot z ekwipunku
            val idx = lines.indexOfFirst { it.contains(itemName) && it.startsWith("   •") }
            if (idx != -1) lines.removeAt(idx)
        } else if (isRepair) {
            // Logika naprawy - tylko odejmij pieniądze, nie dodawaj przedmiotu
            if (isManual) {
                newG = gold - manualG
                newS = silver - manualS
                newC = copper - manualC
            } else {
                val totalInCopper = (gold * 480) + (silver * 12) + copper - priceCopper
                newG = totalInCopper / 480
                newS = (totalInCopper % 480) / 12
                newC = totalInCopper % 12
            }
            // Tutaj można by też zmienić jakość przedmiotu w heroData na "ZWYCZAJNY"
            val itemIdx = lines.indexOfFirst { it.contains(itemName) && it.startsWith("   •") }
            if (itemIdx != -1) {
                val currentLine = lines[itemIdx]
                if (currentLine.contains("(Zniszczony)") || currentLine.contains("(Uszkodzony)") || currentLine.contains("(Znoszony)")) {
                    lines[itemIdx] = currentLine
                        .replace("(Zniszczony)", "")
                        .replace("(Uszkodzony)", "")
                        .replace("(Znoszony)", "")
                        .trim()
                }
            }
        } else {
            if (isManual) {
                // RĘCZNA PŁATNOŚĆ
                val changeTotal = ((manualG * 480) + (manualS * 12) + manualC) - priceCopper
                
                val tempG = gold - manualG
                val tempS = silver - manualS
                val tempC = copper - manualC
                
                val changeG = changeTotal / 480
                val changeS = (changeTotal % 480) / 12
                val changeC = changeTotal % 12
                
                newG = tempG + changeG
                newS = tempS + changeS
                newC = tempC + changeC
            } else {
                // SZYBKI HANDEL
                val totalMinusCost = (gold * 480) + (silver * 12) + copper - priceCopper
                newG = totalMinusCost / 480
                newS = (totalMinusCost % 480) / 12
                newC = totalMinusCost % 12
            }

            // Dodaj przedmiot
            val finalName = if (itemName.contains("Pancerz") || itemName.contains("Zbroja") || 
                              itemName.contains("Miecz") || itemName.contains("Topór") || 
                              itemName.contains("Młot") || itemName.contains("Hełm") ||
                              itemName.contains("Kirys") || itemName.contains("Nogawic")) {
                val quality = (80..90).random()
                "$itemName [Jakość: $quality%]"
            } else itemName

            val eqIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
            if (eqIdx != -1) {
                // Wstawiamy pod monetami
                var insertPos = eqIdx + 1
                while (insertPos < lines.size && lines[insertPos].contains("⛀")) insertPos++
                lines.add(insertPos, "   • $finalName")
            }
        }

        // 3. Zaktualizuj linie monet (w stałej kolejności)
        updateCoinLine(lines, "⛀ Złota korona (zk)", newG)
        updateCoinLine(lines, "⛀ Srebrny szyling (s)", newS)
        updateCoinLine(lines, "⛀ Miedziany pens (p)", newC)

        return lines.joinToString("\n")
    }

    fun countOwnedHorses(heroData: String): Int {
        val lines = heroData.lines()
        val eqIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
        if (eqIdx == -1) return 0
        
        val items = lines.drop(eqIdx + 1)
            .takeWhile { it.startsWith("   •") }
            .map { it.replace("   •", "").trim() }
        
        return items.count { 
            it.contains("Koń", ignoreCase = true) || 
            it.contains("Kuc", ignoreCase = true) || 
            it.contains("Muł", ignoreCase = true) ||
            it.contains("Wierzchowiec", ignoreCase = true)
        }
    }

    fun countUsedHorses(heroData: String): Int {
        val lines = heroData.lines()
        val eqIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
        if (eqIdx == -1) return 0

        val items = lines.drop(eqIdx + 1)
            .takeWhile { it.startsWith("   •") }
            .map { it.replace("   •", "").trim() }
        
        return items.sumOf { getRequiredHorses(it) }
    }

    fun getRequiredHorses(itemName: String): Int {
        return when {
            itemName.contains("(1 koń)") -> 1
            itemName.contains("(2 konie)") -> 2
            itemName.contains("(2-4 konie)") -> 2
            itemName.contains("(4 konie)") -> 4
            itemName.contains("(6 koni)") -> 6
            else -> 0
        }
    }

    fun getContainerLimit(strengthBonus: Int): Int {
        return if (strengthBonus <= 4) 2 else 6
    }

    fun addMoney(heroData: String, copperAmount: Int): String {
        val lines = heroData.lines().toMutableList()
        val totalCopper = calculateTotalCopper(heroData) + copperAmount
        val finalCopper = if (totalCopper < 0) 0 else totalCopper
        
        val newG = finalCopper / 480
        val newS = (finalCopper % 480) / 12
        val newC = finalCopper % 12
        
        updateCoinLine(lines, "⛀ Złota korona (zk)", newG)
        updateCoinLine(lines, "⛀ Srebrny szyling (s)", newS)
        updateCoinLine(lines, "⛀ Miedziany pens (p)", newC)
        
        return lines.joinToString("\n")
    }

    fun updateMoney(heroData: String, gold: Int, silver: Int, copper: Int): String {
        val lines = heroData.lines().toMutableList()
        updateCoinLine(lines, "⛀ Złota korona (zk)", gold)
        updateCoinLine(lines, "⛀ Srebrny szyling (s)", silver)
        updateCoinLine(lines, "⛀ Miedziany pens (p)", copper)
        return lines.joinToString("\n")
    }

    fun getRepairCost(itemName: String, currentQuality: Int, basePrice: Int): Int {
        val missingQuality = 100 - currentQuality
        if (missingQuality <= 0) return 0
        // Koszt naprawy to % brakującej jakości z 40% ceny bazowej przedmiotu
        return (basePrice * 0.4 * (missingQuality / 100.0)).toInt()
    }

    fun repairItem(heroData: String, itemName: String, cost: Int): String {
        val lines = heroData.lines().toMutableList()
        val (gold, silver, copper) = parseMoney(heroData)
        
        val totalInCopper = (gold * 480) + (silver * 12) + copper - cost
        if (totalInCopper < 0) return heroData

        val newG = totalInCopper / 480
        val newS = (totalInCopper % 480) / 12
        val newC = totalInCopper % 12

        val moneyIdx = lines.indexOfFirst { it.startsWith("PIENIĄDZE:") }
        if (moneyIdx != -1) {
            lines[moneyIdx + 1] = "   • Złota korona (zk)  $newG"
            lines[moneyIdx + 2] = "   • Srebrny szyling (s)  $newS"
            lines[moneyIdx + 3] = "   • Miedziany pens (p)  $newC"
        }

        val idx = lines.indexOfFirst { it.contains(itemName) && it.startsWith("   •") }
        if (idx != -1) {
            var line = lines[idx]
            if (line.contains("[Jakość:")) {
                line = line.replace(Regex("\\[Jakość: \\d+%\\]"), "[Jakość: 100%]")
            }
            line = line.replace("(Zniszczony)", "")
                       .replace("(Uszkodzony)", "")
                       .replace("(Znoszony)", "")
            lines[idx] = line.trim()
        }
        return lines.joinToString("\n")
    }

    fun getTravelSpeedMultiplier(heroData: String): Float {
        val lines = heroData.lines()
        val eqIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
        if (eqIdx == -1) return 1.0f

        val items = lines.drop(eqIdx + 1)
            .takeWhile { it.startsWith("   •") }
            .map { it.replace("   •", "").trim() }

        var maxMultiplier = 1.0f
        
        // Sprawdzamy wierzchowce i pojazdy
        items.forEach { item ->
            val m = when {
                item.contains("Koń wierzchowy") -> 2.0f
                item.contains("Koń juczny") -> 1.5f
                item.contains("Koń pociągowy") -> 1.2f
                item.contains("Muł roboczy") -> 0.8f
                item.contains("Kuc") -> 1.1f
                item.contains("Lekka bryczka") -> 1.8f
                item.contains("Wóz kryty") -> 1.3f
                item.contains("Wóz kupiecki") -> 1.2f
                item.contains("Kareta luksusowa") -> 2.0f
                item.contains("Wóz pancerny") -> 1.0f
                item.contains("Rydwan bojowy") -> 2.5f
                else -> 1.0f
            }
            if (m > maxMultiplier) maxMultiplier = m
        }
        
        return maxMultiplier
    }

    fun getCalendarData(heroData: String): String {
        val lines = heroData.lines()
        val calendarLine = lines.find { it.startsWith("CALENDAR:") }
        if (calendarLine != null) {
            return calendarLine.substringAfter("CALENDAR:").trim()
        }
        return "Nieznane Miejsce | 1.1.2024 | 12:00 | [EXTERNAL] | 500 | 500 | Centrum | N"
    }

    fun getLastExitDirection(heroData: String): String {
        val calendar = getCalendarData(heroData)
        val parts = calendar.split("|").map { it.trim() }
        return if (parts.size >= 8) parts[7] else "N"
    }

    fun updateLastExitDirection(heroData: String, direction: String): String {
        val calendar = getCalendarData(heroData)
        val parts = calendar.split("|").map { it.trim() }.toMutableList()
        while (parts.size < 8) parts.add("")
        parts[7] = direction
        return updateCalendarData(heroData, parts.joinToString(" | "))
    }

    fun getDiscoveredZones(heroData: String, settlementName: String): Set<String> {
        val prefix = "DISCOVERED_ZONES_$settlementName:"
        val line = heroData.lines().find { it.startsWith(prefix) }
        return line?.substringAfter(prefix)?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: setOf("Centrum")
    }

    fun updateDiscoveredZones(heroData: String, settlementName: String, zoneName: String): String {
        val prefix = "DISCOVERED_ZONES_$settlementName:"
        val lines = heroData.lines().toMutableList()
        val index = lines.indexOfFirst { it.startsWith(prefix) }
        val currentDiscovered = getDiscoveredZones(heroData, settlementName).toMutableSet()
        currentDiscovered.add(zoneName)
        val newLine = "$prefix${currentDiscovered.joinToString(",")}"
        
        if (index != -1) {
            lines[index] = newLine
        } else {
            lines.add(newLine)
        }
        return lines.joinToString("\n")
    }

    fun generateDistrictDescription(
        settlementName: String,
        zoneName: String,
        districtType: DistrictType,
        timeStr: String,
        weather: Weather
    ): String {
        val timeLabel = getTimeOfDay(timeStr)
        val parts = if (timeStr.contains("|")) timeStr.split("|").map { it.trim() } else null
        val timePart = if (parts != null && parts.size >= 3) {
            parts[2]
        } else if (parts != null && parts.size >= 2) {
            parts[1]
        } else {
            timeStr
        }
        val hour = timePart.substringBefore(":").toIntOrNull() ?: 12
        val isNight = isNight(timeStr)
        
        val weatherDesc = when (weather) {
            Weather.SUNNY -> "Jest pogodnie"
            Weather.CLOUDY -> "Niebo spowijają chmury"
            Weather.RAINY -> "Pada rzęsisty deszcz"
            Weather.STORMY -> "Szaleje burza"
            Weather.FOGGY -> "Gęsta mgła zalega na ulicach"
            Weather.SNOWY -> "Prószy gęsty śnieg"
            Weather.HEATWAVE -> "Żar leje się z nieba"
        }

        val activityDesc = when (districtType) {
            DistrictType.HARBOR -> if (!isNight) "widzisz mnóstwo osób pracujących w dokach i magazynach, słychać krzyki mew i nawoływania marynarzy" else "panuje niemal całkowita cisza, przerywana jedynie pluskiem wody o nabrzeże. W dokach widać tylko kilka osób pilnujących towaru"
            DistrictType.MERCHANT -> if (!isNight) "ulice tętnią życiem, kupcy zachwalają swoje towary, a tłumy mieszkańców przepychają się między straganami" else "sklepy są pozamykane, a szerokie aleje handlowe świecą pustkami, patrolowane jedynie przez miejską straż"
            DistrictType.SLUMS -> if (!isNight) "panuje tu brud i zaduch, podejrzane typy chowają się w cieniu walących się budynków" else "noc wydobywa z mroku najgorsze męty; z ciemnych zaułków dochodzą niepokojące szepty i odgłosy kłótni"
            DistrictType.RULER_SEAT -> if (!isNight) "majestatyczne budowle budzą podziw, a wytwornie ubrani urzędnicy śpieszą do pałacu" else "iluminacje pałacowe rzucają długie cienie na puste place, a straż przyboczna czujnie obserwuje każdy ruch"
            DistrictType.CRAFT -> if (!isNight) "z każdego warsztatu dobiega stukot młotów i zapach dymu z palenisk; rzemieślnicy ciężko pracują" else "ogniska w kuźniach dogasają, a zmęczeni robotnicy udali się na spoczynek, zostawiając ulice w ciszy"
            DistrictType.SACRED -> if (!isNight) "w powietrzu unosi się zapach kadzidła, a słońce wpada przez wysokie witraże świątyń, oświetlając modlących się wiernych" else "blask świec migocze w mroku naw, a nocne modlitwy niosą się echem po pustych placach, panuje podniosły spokój"
            DistrictType.MAGIC -> if (!isNight) "w powietrzu czuć delikatne mrowienie magii, a smukłe wieże czarodziejów lśnią w świetle dnia, budząc respekt przechodniów" else "dziwne światła migoczą w oknach wysokich wież, a magiczne wyładowania od czasu do czasu rozświetlają nocne niebo"
            DistrictType.RESIDENTIAL -> if (!isNight) "dzieci biegają po uliczkach, a gospodynie rozmawiają w progu domów" else "w większości okien światła już zgasły, a dzielnica pogrążyła się w głębokim śnie"
            DistrictType.ECONOMIC -> if (!isNight) "mieszkańcy zajmują się zwierzętami, sadami i pracą na polach" else "zabudowania gospodarcze są ciche, a w zagrodach słychać jedynie zwierzęta"
            DistrictType.MILITARY -> if (!isNight) "słychać miarowy krok maszerujących oddziałów i dźwięk ćwiczebnej broni na placach" else "pochodnie na murach koszar płoną jasnym płomieniem, a strażnicy zmieniają się na posterunkach"
        }

        return "$weatherDesc, a zegar miejski wskazuje godzinę $timePart. Trafiasz do dzielnicy ${districtType.adjectivalName} miasta $settlementName, gdzie $activityDesc."
    }

    fun updateCalendarData(progress: HeroProgress, newCalendar: String): HeroProgress {
        return progress.copy(currentCalendar = newCalendar)
    }

    fun updateTravelState(progress: HeroProgress, travelState: String?): HeroProgress {
        return progress.copy(travelState = travelState)
    }

    fun updateCalendarData(heroData: String, newCalendarStr: String): String {
        val lines = heroData.lines().toMutableList()
        updateCalendarInLines(lines, newCalendarStr)
        
        // Also update Lokacja line for consistency
        val locIdx = lines.indexOfFirst { it.startsWith("Lokacja:") }
        if (locIdx != -1) {
            lines[locIdx] = "Lokacja: $newCalendarStr"
        }

        // Cleanup expired rental keys
        val eqIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
        if (eqIdx != -1) {
            val parts = newCalendarStr.split("|").map { it.trim() }
            if (parts.size >= 2) {
                val currentDateStr = parts[1] // D.M.Y
                val timePart = parts.getOrNull(2) ?: "00:00"
                
                var i = eqIdx + 1
                while (i < lines.size) {
                    val line = lines[i]
                    if (!line.startsWith("   •")) {
                         if (line.isNotBlank()) break
                         i++
                         continue
                    }
                    
                    if (line.contains("Klucz ") && line.contains("[Wynajem do:")) {
                        val expiryDateStr = line.substringAfter("[Wynajem do:").substringBefore("]").trim()
                        val expiryParts = expiryDateStr.split(" ")
                        val expD = expiryParts.getOrNull(0)
                        val expT = expiryParts.getOrNull(1) ?: "12:00"

                        if (expD != null) {
                            if (isDateTimePast(currentDateStr, timePart, expD, expT)) {
                                lines.removeAt(i)
                                continue // Don't increment i
                            }
                        }
                    }
                    i++
                }
            }
        }

        return lines.joinToString("\n")
    }

    private fun isDateTimePast(currD: String, currT: String, expD: String, expT: String): Boolean {
        try {
            val cParts = currD.split(".").map { it.toInt() }
            val eParts = expD.split(".").map { it.toInt() }
            
            // Year
            if (cParts[2] > eParts[2]) return true
            if (cParts[2] < eParts[2]) return false
            
            // Month
            if (cParts[1] > eParts[1]) return true
            if (cParts[1] < eParts[1]) return false
            
            // Day
            if (cParts[0] > eParts[0]) return true
            if (cParts[0] < eParts[0]) return false
            
            // Time
            val ctParts = currT.split(":").map { it.toInt() }
            val etParts = expT.split(":").map { it.toInt() }
            
            if (ctParts[0] > etParts[0]) return true
            if (ctParts[0] < etParts[0]) return false
            
            return ctParts[1] >= etParts[1]
        } catch (e: Exception) {
            return false
        }
    }


    fun updateCalendarInLines(lines: MutableList<String>, newCalendarStr: String) {
        val calendarIdx = lines.indexOfFirst { it.startsWith("CALENDAR:") }
        val newLine = "CALENDAR: $newCalendarStr"

        if (calendarIdx != -1) {
            lines[calendarIdx] = newLine
        } else {
            // Jeśli nie ma CALENDAR, spróbuj zastąpić DATA lub dodaj na końcu, by nie nadpisać imienia
            val dataIdx = lines.indexOfFirst { it.startsWith("DATA:") }
            if (dataIdx != -1) {
                lines[dataIdx] = newLine
            } else {
                lines.add(newLine)
            }
        }
    }

    fun getTravelData(heroData: String): String? {
        return heroData.lines().find { it.startsWith("TRAVEL:") }?.substringAfter("TRAVEL:")?.trim()
    }

    fun updateTravelData(heroData: String, travelLine: String?): String {
        val lines = heroData.lines().toMutableList()
        val index = lines.indexOfFirst { it.startsWith("TRAVEL:") }
        
        if (travelLine == null) {
            if (index != -1) lines.removeAt(index)
        } else {
            val newLine = "TRAVEL: $travelLine"
            if (index != -1) lines[index] = newLine
            else lines.add(newLine)
        }
        return lines.joinToString("\n")
    }

    fun advanceTime(dataNowStr: String, hours: Int): String {
        return advanceTimeMinutes(dataNowStr, hours * 60)
    }

    /**
     * Advances time in the structured HeroProgress model.
     * Updates calendar, fatigue, and triggers weather changes.
     */
    fun advanceTimeMinutesProgress(progress: HeroProgress, minutes: Int): HeroProgress {
        val calendarLine = progress.currentCalendar
        val parts = calendarLine.split("|").map { it.trim() }
        if (parts.size < 3) return progress

        val cityName = parts[0]
        val datePart = parts[1]
        val timePart = parts[2]

        val dParts = datePart.split(".")
        var d = dParts.getOrNull(0)?.toIntOrNull() ?: 1
        var m = dParts.getOrNull(1)?.toIntOrNull() ?: 1
        var y = dParts.getOrNull(2)?.toIntOrNull() ?: 2024

        val tParts = timePart.split(":")
        var hh = tParts.getOrNull(0)?.toIntOrNull() ?: 12
        var mm = tParts.getOrNull(1)?.toIntOrNull() ?: 0

        mm += minutes
        while (mm >= 60) {
            mm -= 60
            hh++
        }

        val daysPassed = hh / 24
        hh %= 24

        for (i in 1..daysPassed) {
            d++
            if (d > 28) {
                d = 1
                m++
            }
            if (m > 12) {
                m = 1
                y++
            }
        }

        val weatherName = if (parts.size >= 5) parts[4] else ""
        var weather = Weather.values().find { it.name == weatherName } ?: Weather.SUNNY
        
        val hours = minutes / 60
        
        // Update fatigue
        val fatigueGain = (hours * 2) + (weather.fatigueMod * hours / 4)
        val newFatigue = (progress.fatigue + fatigueGain).coerceIn(0, progress.maxFatigue)
        
        // Random weather change
        if (hours > 0 && (1..100).random() < (15 * hours)) {
            weather = Weather.values().random()
        }

        val segments = parts.toMutableList()
        while (segments.size < 7) segments.add("")
        segments[0] = cityName
        segments[1] = "$d.$m.$y"
        segments[2] = String.format(java.util.Locale.getDefault(), "%02d:%02d", hh, mm)
        segments[4] = weather.name
        
        val newCalendar = segments.joinToString(" | ")
        
        return progress.copy(
            currentCalendar = newCalendar,
            fatigue = newFatigue
        )
    }

    fun advanceTimeMinutes(heroData: String, minutes: Int): String {
        val isFullHeroData = heroData.contains("\n")
        try {
            val calendarLine = if (isFullHeroData) getCalendarData(heroData) else heroData
            val parts = calendarLine.split("|").map { it.trim() }
            if (parts.size < 3) return heroData

            val cityName = parts[0]
            val datePart = parts[1]
            val timePart = parts[2]

            val dParts = datePart.split(".")
            var d = dParts.getOrNull(0)?.toIntOrNull() ?: 1
            var m = dParts.getOrNull(1)?.toIntOrNull() ?: 1
            var y = dParts.getOrNull(2)?.toIntOrNull() ?: 2024

            val tParts = timePart.split(":")
            var hh = tParts.getOrNull(0)?.toIntOrNull() ?: 12
            var mm = tParts.getOrNull(1)?.toIntOrNull() ?: 0

            mm += minutes
            while (mm >= 60) {
                mm -= 60
                hh++
            }
            
            val daysPassed = hh / 24
            hh %= 24
            
            for (i in 1..daysPassed) {
                d++
                if (d > 28) {
                    d = 1
                    m++
                }
                if (m > 12) {
                    m = 1
                    y++
                }
            }

            val weather = if (isFullHeroData) getWeather(heroData) else {
                if (parts.size >= 5) Weather.entries.find { it.name == parts[4] } ?: Weather.SUNNY else Weather.SUNNY
            }
            val hours = minutes / 60
            
            var resultData = heroData
            if (isFullHeroData) {
                resultData = updateHeroFatigue(resultData, (hours * 2) + (weather.fatigueMod * hours / 4))
                if (hours > 0 && (1..100).random() < (15 * hours)) {
                    resultData = updateWeather(resultData, Weather.entries.random())
                }
            }

            val segments = parts.toMutableList()
            while (segments.size < 7) segments.add("")
            segments[0] = cityName
            segments[1] = "$d.$m.$y"
            segments[2] = String.format(java.util.Locale.getDefault(), "%02d:%02d", hh, mm)
            segments[4] = weather.name
            
            val resultTimeStr = segments.joinToString(" | ")
            
            return if (isFullHeroData) {
                updateCalendarData(resultData, resultTimeStr)
            } else {
                resultTimeStr
            }
        } catch (e: Exception) {
            return heroData
        }
    }

    fun addTime(heroData: String, minutes: Int): String {
        return advanceTimeMinutes(heroData, minutes)
    }

    fun getCurrentHour(heroData: String): Int {
        val calendar = getCalendarData(heroData)
        val parts = calendar.split("|").map { it.trim() }
        val timePart = if (parts.size >= 3) parts[2] else "12:00"
        return timePart.split(":").firstOrNull()?.toIntOrNull() ?: 12
    }

    fun hasAccessToRoom(heroData: String, buildingName: String, cityName: String, roomName: String): Boolean {
        val publicRooms = listOf(
            "Sala Główna", "Sala sprzedaży", "Recepcja i Lobby", "Bar / Szynk", 
            "Salon główny", "Stajnia", "Podworzec", "Ogród", "Kuźnia", 
            "Warsztat płatnerski", "Pracownia rymarska", "Główna stajnia", "Sklepik frontowy",
            "Laboratorium", "Siodlarnia", "Przymierzalnia",
            "Plac apelowy", "Sala treningowa", "Sala wykładowa", "Biblioteka",
            "Nawa główna", "Zakrystia", "Kaplica boczna", "Biblioteka zakonna", "Krużganki"
        )
        if (roomName in publicRooms) return true

        // Check Noclegi (Pobyty)
        val stays = heroData.lines().filter { it.startsWith("Pobyty: ") }
        for (stay in stays) {
            val parts = stay.substringAfter("Pobyty: ").split("|")
            if (parts.size >= 3) {
                if (parts[0] == cityName && parts[1] == buildingName) {
                    val expiry = parts[2] // Format: D.M.Y HH:mm
                    if (!isTimeExpired(heroData, expiry)) return true
                }
            }
        }

        // Check Nieruchomości
        val properties = heroData.lines().filter { it.startsWith("Nieruchomości: ") }
        for (prop in properties) {
            val parts = prop.substringAfter("Nieruchomości: ").split("|")
            if (parts.size >= 2) {
                if (parts[0] == cityName && parts[1] == buildingName) return true
            }
        }

        return false
    }

    fun isTrainingTime(heroData: String): Boolean {
        val current = getCalendarData(heroData).split("|").getOrNull(2)?.trim() ?: "00:00"
        return compareTimeOnly(current, "06:05") >= 0 && compareTimeOnly(current, "18:30") <= 0
    }

    private fun compareTimeOnly(t1: String, t2: String): Int {
        val (h1, m1) = t1.split(":").map { it.toIntOrNull() ?: 0 }
        val (h2, m2) = t2.split(":").map { it.toIntOrNull() ?: 0 }
        return if (h1 != h2) h1.compareTo(h2) else m1.compareTo(m2)
    }

    private fun isTimeExpired(heroData: String, expiryStr: String): Boolean {
        val nowData = getCalendarData(heroData) // City | D.M.Y | HH:mm
        val parts = nowData.split("|").map { it.trim() }
        if (parts.size < 3) return true
        
        val nowDate = parts[1]
        val nowTime = parts[2]
        
        val expParts = expiryStr.split(" ").map { it.trim() }
        if (expParts.size < 2) return true
        val expDate = expParts[0]
        val expTime = expParts[1]

        val (nD, nM, nY) = nowDate.split(".").map { it.toIntOrNull() ?: 0 }
        val (eD, eM, eY) = expDate.split(".").map { it.toIntOrNull() ?: 0 }

        if (nY > eY) return true
        if (nY < eY) return false
        if (nM > eM) return true
        if (nM < eM) return false
        if (nD > eD) return true
        if (nD < eD) return false

        val (nH, nMin) = nowTime.split(":").map { it.toIntOrNull() ?: 0 }
        val (eH, eMin) = expTime.split(":").map { it.toIntOrNull() ?: 0 }

        if (nH > eH) return true
        if (nH < eH) return false
        return nMin >= eMin
    }

    fun addStay(heroData: String, cityName: String, buildingName: String, hours: Int): String {
        val lines = heroData.lines().toMutableList()
        val currentCalendar = getCalendarData(heroData)
        val expiryCalendar = advanceTime(currentCalendar, hours)
        val expiryParts = expiryCalendar.split("|")
        val expiryDate = expiryParts[1].trim()
        val expiryTime = expiryParts[2].trim()
        val expiryStr = "$expiryDate $expiryTime"

        val index = lines.indexOfFirst { it.startsWith("Pobyty: $cityName|$buildingName|") }
        if (index != -1) {
            lines[index] = "Pobyty: $cityName|$buildingName|$expiryStr"
        } else {
            lines.add("Pobyty: $cityName|$buildingName|$expiryStr")
        }
        return lines.joinToString("\n")
    }

    fun addProperty(heroData: String, cityName: String, buildingName: String, type: String): String {
        val lines = heroData.lines().toMutableList()
        val entry = "Nieruchomości: $cityName|$buildingName|$type"
        if (!lines.contains(entry)) {
            lines.add(entry)
        }
        return lines.joinToString("\n")
    }

    fun getDiscoveredZones(heroProgress: HeroProgress?, settlementName: String): Set<String> {
        if (heroProgress == null) return setOf("Centrum")
        return heroProgress.discoveredZones[settlementName] ?: setOf("Centrum")
    }

    fun hasVisitedBuilding(heroProgress: HeroProgress, cityName: String, buildingName: String): Boolean {
        return heroProgress.visitedBuildings.contains("$cityName|$buildingName")
    }

    fun markBuildingVisited(heroProgress: HeroProgress, cityName: String, buildingName: String): HeroProgress {
        val key = "$cityName|$buildingName"
        if (heroProgress.visitedBuildings.contains(key)) return heroProgress
        return heroProgress.copy(visitedBuildings = heroProgress.visitedBuildings + key)
    }

    fun isBuildingVisited(progress: HeroProgress?, cityName: String, buildingName: String): Boolean {
        if (progress == null) return false
        return progress.visitedBuildings.contains("$cityName|$buildingName")
    }

    fun updateDiscoveredZones(progress: HeroProgress, settlementName: String, zoneName: String): HeroProgress {
        val currentZones = progress.discoveredZones[settlementName] ?: emptySet()
        if (currentZones.contains(zoneName)) return progress
        
        val newZones = currentZones + zoneName
        val newDiscoveredMap = progress.discoveredZones.toMutableMap()
        newDiscoveredMap[settlementName] = newZones
        
        return progress.copy(discoveredZones = newDiscoveredMap)
    }


    fun updateHeroFatigue(progress: HeroProgress, delta: Int): HeroProgress {
        val newFatigue = (progress.fatigue + delta).coerceIn(0, progress.maxFatigue)
        return progress.copy(fatigue = newFatigue)
    }

    fun updateWantedLevel(progress: HeroProgress, delta: Int): HeroProgress {
        val newWanted = (progress.wantedLevel + delta).coerceAtLeast(0)
        return progress.copy(wantedLevel = newWanted)
    }

    fun updateGatheredResources(progress: HeroProgress, category: String, resource: String): HeroProgress {
        val currentList = progress.gatheredResources[category] ?: emptyList()
        val newList = currentList + resource
        val newMap = progress.gatheredResources.toMutableMap()
        newMap[category] = newList
        return progress.copy(gatheredResources = newMap)
    }

    fun generateTileDiscoveryResources(biome: TravelBiome): Pair<String, String>? {
        val resourceData = when(biome) {
            TravelBiome.FOREST -> {
                val items = listOf("Mięta pieprzowa", "Szałwia lekarska", "Pokrzywa zwyczajna", "Rumianek polny", "Kora wierzby", "Krwawnik", "Dziki len", "Kwiat czarnego bzu", "Lawenda")
                Pair("Zioła", items.random())
            }
            TravelBiome.MEADOW -> {
                val items = listOf("Mięta pieprzowa", "Mniszek pospolity", "Rumianek polny", "Lawenda", "Szałwia lekarska", "Pokrzywa zwyczajna")
                Pair("Zioła", items.random())
            }
            TravelBiome.SWAMP -> {
                val items = listOf("Skrajnik Szary", "Mechowiec Wilgotny", "Zarodnikowiec Polny", "Purchawka Rdzawa", "Koronnik Drzewny", "Kapelusznik Blady", "Zgniłek Bagienny")
                Pair("Grzyby", items.random())
            }
            TravelBiome.ROCKS -> {
                val items = listOf("Ruda żelaza", "Ruda miedzi", "Ruda cyny", "Węgiel brunatny", "Kwarc dymny", "Krzemień", "Piryt (Złoto głupców)", "Sól kamienna")
                Pair("Minerały", items.random())
            }
            TravelBiome.LAKE -> {
                val items = listOf("Płotka", "Szczupak", "Pstrąg", "Łosoś", "Karp", "Okoń", "Karaś")
                Pair("Rybołówstwo", items.random())
            }
            TravelBiome.RIVER -> {
                val items = listOf("Płotka", "Szczupak", "Pstrąg", "Łosoś", "Karp", "Okoń", "Karaś")
                Pair("Rybołówstwo", items.random())
            }
            TravelBiome.SPECJALNY -> {
                val items = listOf("Krwawnik", "Dziki len", "Kwiat czarnego bzu")
                Pair("Zioła", items.random())
            }


            TravelBiome.SETTLEMENT -> null
        }
        return resourceData
    }

    fun generateDiscoveryResourceString(itemName: String): String {
        val weight = when {
            itemName.contains("Ruda") || itemName.contains("Węgiel") || itemName.contains("Kwarc") -> (3..15).random() / 10.0
            itemName.contains("Płotka") || itemName.contains("Szczupak") -> (2..15).random() / 10.0
            else -> (1..8).random() / 100.0
        }
        return "• $itemName (${String.format(java.util.Locale.US, "%.2f", weight)} kg) (Podstawowe)"
    }

    fun markBuildingVisited(heroData: String, buildingName: String, cityName: String): String {
        val lines = heroData.lines().toMutableList()
        val tag = "Odwiedzono: $cityName|$buildingName"
        if (!lines.contains(tag)) {
            lines.add(tag)
        }
        return lines.joinToString("\n")
    }

    fun hasVisitedBuilding(heroData: String, buildingName: String, cityName: String): Boolean {
        val tag = "Odwiedzono: $cityName|$buildingName"
        return heroData.lines().contains(tag)
    }

    fun hasItem(heroData: String, itemName: String): Boolean {
        return heroData.lines().any { it.startsWith("   •") && it.contains(itemName, ignoreCase = true) }
    }

    fun removeItem(heroData: String, itemName: String): String {
        val lines = heroData.lines().toMutableList()
        val index = lines.indexOfFirst { it.startsWith("   •") && it.contains(itemName, ignoreCase = true) }
        if (index != -1) {
            lines.removeAt(index)
        }
        return lines.joinToString("\n")
    }

    fun canAttemptLockpicking(heroData: String): Boolean {
        val skillName = "Otwieranie zamków"
        val line = heroData.lines().find { it.contains(skillName, ignoreCase = true) }
        val hasSkill = line != null && !line.contains("Brak", true)
        return hasSkill && hasItem(heroData, "Wytrychy")
    }

    data class BurglaryResult(
        val success: Boolean,
        val caught: Boolean,
        val lootItems: List<String>,
        val lootMoney: Int,
        val message: String,
        val updatedHeroData: String,
        val updatedHeroProgress: HeroProgress? = null
    )

    fun performBurglary(
        heroData: String,
        heroProgress: HeroProgress,
        buildingName: String,
        cityName: String,
        roomName: String,
        currentHour: Int,
        buildingType: String,
        rank: Int,
        districtType: DistrictType? = null
    ): BurglaryResult {
        var currentData = heroData
        var currentProgress = heroProgress
        val skillRank = getSkillRank(heroData, "Otwieranie zamków")
        
        val baseSuccessChance = when (skillRank) {
            "Uczeń" -> 40
            "Adept" -> 60
            "Ekspert" -> 75
            "Mistrz" -> 85
            "Arcymistrz" -> 95
            else -> 0
        }

        // Fatigue check and update
        if (currentProgress.fatigue >= 95) {
            return BurglaryResult(
                success = false,
                caught = false,
                lootItems = emptyList(),
                lootMoney = 0,
                message = "Jesteś zbyt zmęczony, by skupić się na zamku!",
                updatedHeroData = currentData,
                updatedHeroProgress = currentProgress
            )
        }
        currentProgress = updateHeroFatigue(currentProgress, (5..15).random())

        // Weather and District modifiers
        val weather = getWeather(heroData)
        val weatherCrimeMod = -weather.crimeMod / 2
        val districtMod = when(districtType) {
            DistrictType.SLUMS -> 15
            DistrictType.CRAFT -> 5
            DistrictType.MERCHANT -> -5
            DistrictType.RULER_SEAT -> -20
            else -> 0
        }

        // Difficulty modifiers
        val buildingMod = when (buildingType) {
            "Mieszkanie" -> -10
            "Hotel" -> -5
            "Karczma" -> 5
            "Zamtuz" -> 10
            "Płatnerz", "Kowal" -> -20
            "Apteka" -> -15
            "Rymarz" -> -10
            "Stajnia" -> 0
            "Warsztat", "Manufaktura" -> -15
            else -> 0
        }
        
        // Rank modifier: higher rank = better locks
        val rankMod = -(rank * 5)
        
        val rollSuccess = (1..100).random()
        val isSuccess = rollSuccess <= (baseSuccessChance + buildingMod + rankMod + districtMod + weatherCrimeMod)

        // Risk of being caught
        val isNight = currentHour in 22..23 || currentHour in 0..4
        var catchChance = if (isNight) 15 else 45
        
        // Weather influence on catching
        catchChance += weather.crimeMod / 2
        
        // Dexterity bonus to stealth
        val dexterity = getStatValue(heroData, "Zr")
        catchChance -= (dexterity / 10)
        
        if (buildingType == "Karczma" || buildingType == "Tawerna") catchChance -= 10 // Busy places mask noise
        if (buildingType == "Mieszkanie") catchChance += 10 // Quiet houses are risky
        
        // District influence on catching
        catchChance -= districtMod
        
        catchChance = catchChance.coerceIn(5, 95)
        
        val rollCatch = (1..100).random()
        val isCaught = rollCatch <= catchChance

        if (isCaught) {
            val fine = (100 * rank..400 * rank).random()
            currentData = addMoney(currentData, -fine)
            
            // Legal consequences - update progress instead of text
            currentProgress = updateWantedLevel(currentProgress, (20..40).random())
            currentProgress = updateHeroFatigue(currentProgress, 20)
            currentProgress = advanceTimeMinutesProgress(currentProgress, (2..5).random() * 60)

            return BurglaryResult(
                success = false,
                caught = true,
                lootItems = emptyList(),
                lootMoney = 0,
                message = "Zostałeś przyłapany! Straż nałożyła grzywnę: ${formatPrice(fine)} i trafiłeś do rejestru poszukiwanych.",
                updatedHeroData = currentData,
                updatedHeroProgress = currentProgress
            )
        }

        if (!isSuccess) {
            currentData = removeItem(currentData, "Wytrychy")
            currentProgress = updateWantedLevel(currentProgress, (1..3).random())
            return BurglaryResult(
                success = false,
                caught = false,
                lootItems = emptyList(),
                lootMoney = 0,
                message = "Nie udało się otworzyć zamka. Wytrych pękł!",
                updatedHeroData = currentData,
                updatedHeroProgress = currentProgress
            )
        }

        // Success - Generate Loot based on building and rank
        val lootMultiplier = (when (buildingType) {
            "Mieszkanie" -> 2.0
            "Hotel" -> 1.5
            "Płatnerz", "Kowal" -> 3.0
            "Apteka" -> 2.5
            "Rymarz" -> 2.0
            "Stajnia" -> 1.2
            "Warsztat", "Manufaktura" -> 2.5
            else -> 1.0
        }) * (rank * 0.5 + 0.5)

        val lootMoney = ( (20..100).random() * lootMultiplier ).toInt()
        
        val commonLoot = listOf("Srebrny pierścień", "Jedwabna chusta", "Prowiant", "Butelka wina", "Mosiężny świecznik")
        val rareLoot = listOf("Złoty sygnet", "Kamień szlachetny", "Stary manuskrypt", "Aksamitna sakiewka")
        val specializedLoot = when(buildingType) {
            "Płatnerz", "Kowal" -> listOf("Osełka", "Wysokiej jakości stal", "Ozdobna pochwa")
            "Apteka" -> listOf("Lecznicze ziele", "Tajemnicza mikstura", "Moździerz")
            "Rymarz" -> listOf("Kawałek skóry", "Narzędzia kuśnierskie", "Rzemyk")
            "Stajnia" -> listOf("Owsianka", "Zgrzebło", "Podkowa")
            "Warsztat", "Manufaktura" -> listOf("Narzędzia precyzyjne", "Surowce rzemieślnicze", "Szkic projektu")
            else -> emptyList()
        }

        val allPossibleLoot = commonLoot + specializedLoot + (if ((1..10).random() + rank > 10) rareLoot else emptyList())
        val lootItems = mutableListOf<String>()
        repeat(((1..2).random() + (rank / 2)).coerceAtMost(5)) {
            lootItems.add(allPossibleLoot.random())
        }

        currentData = addMoney(currentData, lootMoney)
        lootItems.forEach { item ->
            val lines = currentData.lines().toMutableList()
            val eqIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
            if (eqIdx != -1) {
                var insertPos = eqIdx + 1
                while (insertPos < lines.size && lines[insertPos].contains("⛀")) insertPos++
                lines.add(insertPos, "   • $item (1 j.)")
            }
            currentData = lines.joinToString("\n")
        }
        
        // Successful burglary still carries a small risk of discovery
        currentProgress = updateWantedLevel(currentProgress, (2..5).random())
        
        // Grant temporary access via progress
        val expiry = advanceTimeMinutesProgress(currentProgress, 120).currentCalendar.split("|")[2].trim()
        val date = advanceTimeMinutesProgress(currentProgress, 120).currentCalendar.split("|")[1].trim()
        
        val newStays = currentProgress.stays.filterNot { it.cityName == cityName && it.buildingName == buildingName }.toMutableList()
        newStays.add(HeroStay(cityName, buildingName, "$date $expiry"))
        currentProgress = currentProgress.copy(stays = newStays)

        return BurglaryResult(
            success = true,
            caught = false,
            lootItems = lootItems,
            lootMoney = lootMoney,
            message = "Sukces! Otworzyłeś zamek i splądrowałeś pomieszczenie.",
            updatedHeroData = currentData,
            updatedHeroProgress = currentProgress
        )
    }

    fun getHeroStat(heroData: String, statName: String): Int {
        val lines = heroData.lines()
        // Szukamy w sekcji CECHY
        val startIdx = lines.indexOfFirst { it.startsWith("CECHY:") }
        if (startIdx == -1) return 30
        
        val normalizedStat = statName.uppercase()
        
        for (i in startIdx + 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("UMIEJĘTNOŚCI:")) break
            if (line.startsWith("$normalizedStat: ")) {
                return line.substringAfter(":").trim().split(" ").first().toIntOrNull() ?: 30
            }
        }
        return 30
    }

    fun hasLanguageSkill(heroData: String, languageName: String, type: String): Boolean {
        // type: "Mowa" lub "Pismo"
        val skillLine = "Język obcy: $type $languageName"
        return heroData.lines().any { it.contains(skillLine, ignoreCase = true) }
    }

    fun addSkill(heroData: String, skillName: String): String {
        val lines = heroData.lines().toMutableList()
        val umIdx = lines.indexOfFirst { it.startsWith("UMIEJĘTNOŚCI:") }
        if (umIdx != -1) {
            lines.add(umIdx + 1, "   • $skillName (Uczeń)")
        }
        return lines.joinToString("\n")
    }

    data class TrainingResult(
        val totalPn: Int,
        val sessionsPerformed: Int,
        val successes: Int,
        val criticalSuccesses: Int = 0,
        val criticalFailures: Int = 0,
        val skillName: String,
        val fatigueInterrupted: Boolean,
        val message: String,
        val updatedHeroProgress: HeroProgress? = null
    )

    fun performTrainingSessions(
        heroData: String,
        skillName: String,
        hours: Int,
        statValue: Int,
        costPerHour: Int,
        fatiguePerHour: Int = 10,
        heroProgress: HeroProgress? = null
    ): Pair<String, TrainingResult> {
        var currentHeroData = heroData
        var currentProgress = heroProgress
        val currentFatigue = currentProgress?.fatigue ?: getHeroFatigue(currentHeroData)
        val maxF = currentProgress?.maxFatigue ?: 100
        val totalCost = hours * costPerHour
        
        if (calculateTotalCopper(currentHeroData) < totalCost) {
            return Pair(currentHeroData, TrainingResult(0, 0, 0, 0, 0, skillName, false, "Brak funduszy!", currentProgress))
        }

        // Kary za zmęczenie (Progi procentowe: 0.4 i 0.7)
        val fatiguePenalty = when {
            currentFatigue > (maxF * 0.7) -> 20
            currentFatigue > (maxF * 0.4) -> 10
            else -> 0
        }

        val heroLines = currentHeroData.lines()
        val rankBonus = getRankBonus(heroLines, skillName)
        val rank = getSkillRank(heroLines, skillName)
        val advancementBonus = when (rank) {
            1 -> 0
            2 -> 5
            3 -> 10
            4 -> 15
            5 -> 20
            else -> 0
        }
        val baseValueWithAdv = statValue + advancementBonus
        val targetValue = statValue + rankBonus - fatiguePenalty
        
        val numSessions = hours // 1 sesja na godzinę
        var totalPn = 0
        var successes = 0
        var critSuccesses = 0
        var critFailures = 0
        var fatigueInterrupted = false

        repeat(numSessions) {
            val sessionFatigue = currentProgress?.fatigue ?: getHeroFatigue(currentHeroData)
            val sessionMaxF = currentProgress?.maxFatigue ?: 100
            
            if (sessionFatigue >= (sessionMaxF * 0.95)) {
                fatigueInterrupted = true
                return@repeat
            }

            val roll = (1..100).random()
            val rollStr = if (roll < 10) "0$roll" else if (roll == 100) "00" else roll.toString()
            val isDoublet = rollStr[0] == rollStr[1]

            val isSuccess = if (isDoublet) {
                roll <= baseValueWithAdv
            } else if (rank == 0) {
                if (skillName.contains("Język obcy")) roll <= targetValue else false
            } else {
                roll <= targetValue
            }

            val isCriticalSuccess = (isDoublet && isSuccess) || (roll <= targetValue - 30)
            val isCriticalFailure = (isDoublet && !isSuccess) || (roll >= targetValue + 30)

            if (isCriticalSuccess) critSuccesses++
            if (isCriticalFailure) critFailures++

            val preferredStats = getClassPreferredStats(currentHeroData)
            val isPreferred = preferredStats.contains(skillName) || 
                              (preferredStats.contains("Wiedza") && getSkillInfo(currentHeroData, skillName).stat == "Wiedza") ||
                              (preferredStats.contains("Inteligencja") && getSkillInfo(currentHeroData, skillName).stat == "Wiedza")

            var pnGained = when {
                isCriticalFailure -> 0
                isCriticalSuccess -> (statValue / 10).coerceAtLeast(1)
                isSuccess -> 2
                else -> 1
            }
            if (skillName.contains("Język obcy") && rank == 0) {
                pnGained = if (isSuccess) (8..12).random() else (4..6).random()
            }
            if (isPreferred && pnGained > 0) pnGained += 1
            
            totalPn += pnGained
            if (isSuccess) successes++
            
            currentHeroData = addPnToSkill(currentHeroData, skillName, pnGained)
            currentHeroData = updateHeroFatigue(currentHeroData, fatiguePerHour)
            if (currentProgress != null) {
                currentProgress = updateHeroFatigue(currentProgress!!, fatiguePerHour)
            }
        }

        currentHeroData = deductMoney(currentHeroData, totalCost)

        if (currentProgress != null) {
            currentProgress = advanceTimeMinutesProgress(currentProgress!!, hours * 60)
        }

        val msg = if (fatigueInterrupted) "Przerwano z powodu wycieńczenia!" else "Trening zakończony."
        return Pair(currentHeroData, TrainingResult(totalPn, numSessions, successes, critSuccesses, critFailures, skillName, fatigueInterrupted, msg, currentProgress))
    }

    data class WorkResult(val title: String, val description: String, val totalWage: Int)

    fun getHeroGold(heroData: String): Int {
        val line = heroData.lines().find { it.trim().contains("Złoto:", ignoreCase = true) }
        return line?.substringAfter(":", "")?.trim()?.toIntOrNull() ?: 0
    }

    fun getHeroSilver(heroData: String): Int {
        val line = heroData.lines().find { it.trim().contains("Srebro:", ignoreCase = true) }
        return line?.substringAfter(":", "")?.trim()?.toIntOrNull() ?: 0
    }

    fun getHeroCopper(heroData: String): Int {
        val line = heroData.lines().find { it.trim().contains("Miedź:", ignoreCase = true) }
        return line?.substringAfter(":", "")?.trim()?.toIntOrNull() ?: 0
    }

    fun getHeroFatigue(heroData: String): Int {
        val line = heroData.lines().find { it.trim().contains("Zmęczenie:", ignoreCase = true) }
        return line?.substringAfter(":", "")?.trim()?.toIntOrNull() ?: 0
    }

    fun getWantedLevel(heroData: String): Int {
        val line = heroData.lines().find { it.trim().contains("Poszukiwany:", ignoreCase = true) }
        return line?.substringAfter(":", "")?.trim()?.toIntOrNull() ?: 0
    }

    fun updateHeroFatigue(heroData: String, amount: Int): String {
        val lines = heroData.lines().toMutableList()
        val currentFatigue = getHeroFatigue(heroData)
        
        val kBonus = getStatValue(heroData, "Krzepa") / 10
        val oBonus = getStatValue(heroData, "Odporność") / 10
        val tragarzMult = getTragarzMultiplier(heroData)
        val maxF = 100 + (kBonus * tragarzMult) + (oBonus * 8)
        
        val newFatigue = (currentFatigue + amount).coerceIn(0, maxF)
        
        val index = lines.indexOfFirst { it.trim().startsWith("Zmęczenie:") }
        if (index != -1) {
            lines[index] = "Zmęczenie: $newFatigue / $maxF"
        } else {
            // Find a good place to insert (before EKWIPUNEK or at the end)
            val insertIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }.let { if(it == -1) lines.size else it }
            lines.add(insertIdx, "Zmęczenie: $newFatigue / $maxF")
        }
        return lines.joinToString("\n").trimEnd()
    }

    private fun getTragarzMultiplier(heroData: String): Int {
        val tragarzLine = heroData.lines().find { it.contains("Tragarz", ignoreCase = true) } ?: return 2
        
        return when {
            tragarzLine.contains("Mistrz") || tragarzLine.contains("Arcymistrz") -> 6
            tragarzLine.contains("Ekspert") -> 5
            tragarzLine.contains("Adept") -> 4
            tragarzLine.contains("Uczeń") || tragarzLine.contains("𝖀𝖈𝖟𝖊𝖓") -> 3
            else -> 2
        }
    }

    fun updateWantedLevel(heroData: String, amount: Int): String {
        val lines = heroData.lines().toMutableList()
        val index = lines.indexOfFirst { it.startsWith("Poszukiwany: ") }
        val currentWanted = getWantedLevel(heroData)
        val newWanted = (currentWanted + amount).coerceIn(0, 100)
        
        if (index != -1) {
            lines.removeAt(index)
        }
        
        // Zawsze dodajemy na samym dole
        lines.add("Poszukiwany: $newWanted")
        
        return lines.joinToString("\n").trimEnd()
    }

    fun addPd(heroData: String, amount: Int): String {
        val lines = heroData.lines().toMutableList()
        val pdIdx = lines.indexOfFirst { it.startsWith("PD:") }
        if (pdIdx != -1) {
            val currentPd = lines[pdIdx].substringAfter(":").trim().toIntOrNull() ?: 0
            lines[pdIdx] = "PD: ${currentPd + amount}"
        } else {
            lines.add(1, "PD: $amount")
        }
        return lines.joinToString("\n")
    }

    fun addGatheredItem(heroData: String, category: String, itemName: String, weight: Double, quality: String): String {
        val lines = heroData.lines().toMutableList()
        val sectionHeader = "ZBIORY:"
        val subHeader = "   [$category]"
        
        var zbioryIdx = lines.indexOfFirst { it.trim() == sectionHeader }
        if (zbioryIdx == -1) {
            // Umieść pod sekcją MIEJSCE (zwykle po danych czasu/kalendarza)
            val placeIdx = lines.indexOfFirst { it.startsWith("MIEJSCE:") }
            if (placeIdx != -1) {
                lines.add(placeIdx + 1, sectionHeader)
                zbioryIdx = placeIdx + 1
            } else {
                // Failsafe: przed Zmęczeniem
                val fatigueIdx = lines.indexOfFirst { it.startsWith("Zmęczenie:") }
                if (fatigueIdx != -1) {
                    lines.add(fatigueIdx, sectionHeader)
                    zbioryIdx = fatigueIdx
                } else {
                    lines.add(sectionHeader)
                    zbioryIdx = lines.size - 1
                }
            }
        }

        // Znajdź podkategorię
        var subIdx = -1
        for (i in zbioryIdx + 1 until lines.size) {
            if (lines[i].trim() == subHeader.trim()) {
                subIdx = i
                break
            }
            if (lines[i].isNotEmpty() && !lines[i].startsWith("   ")) break // Koniec sekcji ZBIORY
        }

        if (subIdx == -1) {
            // Dodaj podkategorię
            lines.add(zbioryIdx + 1, subHeader)
            lines.add(zbioryIdx + 2, "      • $itemName (${String.format(java.util.Locale.US, "%.2f", weight)} kg) ($quality)")
        } else {
            // Znajdź przedmiot w podkategorii
            var found = false
            for (j in subIdx + 1 until lines.size) {
                // Szukamy identycznego przedmiotu o tej samej jakości
                if (lines[j].contains("• $itemName") && lines[j].contains("($quality)")) {
                    val currentWeightStr = lines[j].substringAfter("(").substringBefore(" kg").replace(",", ".")
                    val currentWeight = currentWeightStr.toDoubleOrNull() ?: 0.0
                    val newWeight = currentWeight + weight
                    lines[j] = "      • $itemName (${String.format(java.util.Locale.US, "%.2f", newWeight)} kg) ($quality)"
                    found = true
                    break
                }
                if (lines[j].trim().startsWith("[") || (lines[j].isNotEmpty() && !lines[j].startsWith("   "))) break
            }
            if (!found) {
                lines.add(subIdx + 1, "      • $itemName (${String.format(java.util.Locale.US, "%.2f", weight)} kg) ($quality)")
            }
        }

        return lines.joinToString("\n")
    }

    data class WorkSessionResult(
        val totalWage: Int,
        val totalPn: Int,
        val sessionsPerformed: Int,
        val successes: Int,
        val criticalSuccesses: Int = 0,
        val criticalFailures: Int = 0,
        val skillName: String,
        val interruptedByFatigue: Boolean = false,
        val updatedHeroProgress: HeroProgress? = null
    )

    fun performWorkSessions(
        heroData: String,
        skillName: String,
        hours: Int,
        baseWagePerHour: Int,
        statValue: Int,
        fatiguePerOrder: Int = 6,
        heroProgress: HeroProgress? = null
    ): Pair<String, WorkSessionResult> {
        var currentHeroData = heroData
        var currentProgress = heroProgress
        val heroLines = currentHeroData.lines()
        val currentFatigue = currentProgress?.fatigue ?: getHeroFatigue(currentHeroData)
        val maxF = currentProgress?.maxFatigue ?: 100
        
        val rankBonus = getRankBonus(heroLines, skillName)
        
        // Kary za zmęczenie (Progi procentowe: 0.4 i 0.7)
        val fatiguePenalty = when {
            currentFatigue > (maxF * 0.7) -> 25
            currentFatigue > (maxF * 0.4) -> 10
            else -> 0
        }
        
        val targetValue = statValue + rankBonus - fatiguePenalty

        val numSessions = hours / 2
        var totalWage = 0 
        var totalPn = 0
        var successes = 0
        var critSuccesses = 0
        var critFailures = 0
        var fatigueInterrupted = false

        val rank = getSkillRank(heroLines, skillName)
        val advancementBonus = when (rank) {
            1 -> 0
            2 -> 5
            3 -> 10
            4 -> 15
            5 -> 20
            else -> 0
        }
        val baseValueWithAdv = statValue + advancementBonus

        repeat(numSessions) {
            val sessionFatigue = currentProgress?.fatigue ?: getHeroFatigue(currentHeroData)
            val sessionMaxF = currentProgress?.maxFatigue ?: 100
            
            if (sessionFatigue >= (sessionMaxF * 0.95)) {
                fatigueInterrupted = true
                return@repeat
            }
            
            // 5% szansy na wypadek przy wysokim zmęczeniu (powyżej 70% limitu)
            if (sessionFatigue > (sessionMaxF * 0.7) && (1..100).random() <= 5) {
                fatigueInterrupted = true
                return@repeat 
            }

            val roll = (1..100).random()
            val rollStr = if (roll < 10) "0$roll" else if (roll == 100) "00" else roll.toString()
            val isDoublet = rollStr[0] == rollStr[1]

            val isSuccess = if (isDoublet) {
                roll <= baseValueWithAdv
            } else if (rank == 0) {
                if (skillName.contains("Język obcy")) roll <= targetValue else false
            } else {
                roll <= targetValue
            }

            val isCriticalSuccess = (isDoublet && isSuccess) || (roll <= targetValue - 30)
            val isCriticalFailure = (isDoublet && !isSuccess) || (roll >= targetValue + 30)

            if (isCriticalSuccess) critSuccesses++
            if (isCriticalFailure) critFailures++

            val preferredStats = getClassPreferredStats(currentHeroData)
            val skillStat = getSkillInfo(currentHeroData, skillName).stat
            val isPreferred = preferredStats.contains(skillStat) || (skillStat == "Wiedza" && preferredStats.contains("Inteligencja"))

            var pnGained = when {
                isCriticalFailure -> 0
                isCriticalSuccess -> (statValue / 10).coerceAtLeast(1)
                isSuccess -> 2
                else -> 1
            }
            if (skillName.contains("Język obcy") && rank == 0) {
                pnGained = if (isSuccess) (8..12).random() else (4..6).random()
            }
            if (isPreferred && pnGained > 0) pnGained += 1
            totalPn += pnGained
            
            var sessionWage = baseWagePerHour * 2
            if (isCriticalSuccess) {
                sessionWage = (sessionWage * 1.5).toInt()
            } else if (isCriticalFailure) {
                sessionWage = (sessionWage * 0.5).toInt()
            }

            if (isSuccess) {
                successes++
                val bonus = (sessionWage * (25..50).random() / 100.0).toInt()
                totalWage += sessionWage + bonus
            } else if (!isCriticalFailure) {
                 totalWage += sessionWage
            } else {
                totalWage += sessionWage 
            }
            
            currentHeroData = addPnToSkill(currentHeroData, skillName, pnGained)
            
            val sessionFatigueDelta = fatiguePerOrder * 2
            currentHeroData = updateHeroFatigue(currentHeroData, sessionFatigueDelta)
            if (currentProgress != null) {
                currentProgress = updateHeroFatigue(currentProgress!!, sessionFatigueDelta)
            }
        }

        // Dodanie pieniędzy
        currentHeroData = addMoney(currentHeroData, totalWage)

        if (currentProgress != null) {
            currentProgress = advanceTimeMinutesProgress(currentProgress!!, hours * 60)
        }

        return Pair(currentHeroData, WorkSessionResult(totalWage, totalPn, numSessions, successes, critSuccesses, critFailures, skillName, fatigueInterrupted, currentProgress))
    }

    fun addPnToSkill(heroData: String, skillName: String, pn: Int): String {
        val lines = heroData.lines().toMutableList()
        val index = lines.indexOfFirst { it.startsWith("$skillName: ") || it.contains("• $skillName", ignoreCase = true) }
        
        if (skillName == "Szczęście") {
            if (index != -1) {
                // Jeśli Szczęście już jest, upewniamy się że ma ✔️ i 0 PN
                val line = lines[index]
                val base = if (line.contains("(")) line.substringBefore("(") else line.substringBefore("|")
                lines[index] = "${base.trimEnd()} (✔️) | PN: 0"
                return lines.joinToString("\n")
            } else {
                // Dodajemy nową umiejętność Szczęście
                val skillSection = lines.indexOfFirst { it.contains("UMIEJĘTNOŚCI", ignoreCase = true) }
                val newLine = "   • Szczęście (✔️) | PN: 0"
                if (skillSection != -1) {
                    lines.add(skillSection + 1, newLine)
                } else {
                    lines.add(newLine)
                }
                return lines.joinToString("\n")
            }
        }

        if (index != -1) {
            val line = lines[index]
            // Format: "Walka wręcz: 35 | Ranga: Uczeń | PN: 5" lub "   • Szczęście (Uczeń) | PN: 5"
            if (line.contains("| PN: ")) {
                val parts = line.split("| PN: ")
                val currentPn = parts[1].trim().toIntOrNull() ?: 0
                lines[index] = "${parts[0]}| PN: ${currentPn + pn}"
            } else {
                lines[index] = "$line | PN: $pn"
            }
        } else {
            // Jeśli umiejętności nie ma, dodajemy ją na końcu sekcji umiejętności lub na końcu pliku
            val skillSection = lines.indexOfFirst { it.contains("UMIEJĘTNOŚCI", ignoreCase = true) }
            if (skillSection != -1) {
                lines.add(skillSection + 1, "   • $skillName (Uczeń) | PN: $pn")
            } else {
                lines.add("   • $skillName (Uczeń) | PN: $pn")
            }
        }
        return lines.joinToString("\n")
    }

    data class SkillInfo(
        val name: String,
        val value: Int,
        val rank: String,
        val pn: Int,
        val stat: String
    )

    fun getSkillInfo(heroData: String, skillName: String): SkillInfo {
        val line = heroData.lines().find { it.startsWith("$skillName: ") } ?: ""
        val value = line.substringAfter(": ").substringBefore(" |").trim().toIntOrNull() ?: 0
        val rank = line.substringAfter("Ranga: ").substringBefore(" |").trim().ifEmpty { "Brak" }
        val pn = line.substringAfter("PN: ").trim().toIntOrNull() ?: 0
        val stat = when {
            skillName.startsWith("Język obcy") -> "INT"
            skillName == "Język Magiczny" -> "SW"
            skillName == "Język Klasyczny" -> "INT"
            skillName in skillWW -> "WW"
            skillName in skillUS -> "US"
            skillName in skillZrecznosc -> "ZR"
            skillName in skillInteligencja -> "INT"
            skillName in skillCharyzma -> "CHA"
            skillName in skillOdpornosc -> "ODP"
            skillName in skillKrzepa -> "S"
            skillName in skillSW -> "SW"
            skillName in skillWiedza -> "INT"
            // Pozostałe specyficzne przypadki
            skillName == "Siła" || skillName == "Pływanie (siłowe)" -> "S"
            else -> "INT"
        }
        return SkillInfo(skillName, value, rank, pn, stat)
    }


    fun generateWorkEvent(jobType: String, baseWagePerShift: Int, charisma: Int): WorkResult {
        val roll = (1..100).random()
        return when (jobType) {
            "Posługiwanie" -> {
                when {
                    roll > 90 -> WorkResult("Hojny gość", "Obsłużyłeś bogatego kupca, który docenił twoje maniery i zostawił suty napiwek!", baseWagePerShift + (5..15).random())
                    roll < 15 -> WorkResult("Kradzież", "Podczas tłoku ktoś wyciągnął ci sakiewkę z częścią zarobku...", (baseWagePerShift * 0.6).toInt())
                    roll < 30 -> WorkResult("Bójka", "W karczmie wybuchła bójka! Musiałeś pomagać uspokajać gości, przez co stłukło się trochę zastawy. Karczmarz potrącił ci z wypłaty.", (baseWagePerShift * 0.8).toInt())
                    else -> WorkResult("Spokojna zmiana", "Dzień minął bez większych ekscesów. Zarobiłeś uczciwie na chleb.", baseWagePerShift)
                }
            }
            "Muzyk" -> {
                val bonus = if (charisma > 50) (charisma - 50) / 5 else 0
                when {
                    roll > 85 -> WorkResult("Owacje na stojąco", "Tłum porwany twoją muzyką rzucał monety na scenę!", baseWagePerShift + (10..25).random() + bonus)
                    roll < 20 -> WorkResult("Pęknięta struna", "W połowie występu pękła ci struna. Musiałeś improwizować, ale publika nie była zachwycona.", (baseWagePerShift * 0.5).toInt())
                    else -> WorkResult("Dobry występ", "Twoja muzyka umiliła wieczór gościom. Kilku nawet postawiło ci piwo (wliczono w cenę).", baseWagePerShift + bonus)
                }
            }
            "Stajenny" -> {
                when {
                    roll > 95 -> WorkResult("Znalezisko", "Sprzątając boksy, znalazłeś w sianie zgubioną przez kogoś monetę!", baseWagePerShift + (12..20).random())
                    roll < 10 -> WorkResult("Wypadek", "Jeden z koni cię kopnął! Musiałeś wydać na opatrunki.", (baseWagePerShift * 0.3).toInt())
                    else -> WorkResult("Ciężka praca", "Koniec koni są czyste i nakarmione. To była ciężka, fizyczna praca.", baseWagePerShift)
                }
            }
            "Rzemiosło" -> {
                when {
                    roll > 90 -> WorkResult("Mistrzowskie wykonanie", "Twoje dzisiejsze dzieło jest wyjątkowej jakości. Mistrz warsztatu wypłacił ci premię!", (baseWagePerShift * 1.5).toInt())
                    roll < 10 -> WorkResult("Wypadek przy pracy", "Zniszczyłeś cenne materiały. Musiałeś pokryć koszty strat z własnej kieszeni.", (baseWagePerShift * 0.4).toInt())
                    roll < 25 -> WorkResult("Dzień przestoju", "Brakło surowców i połowę dnia spędziłeś na czekaniu na dostawę. Zarobiłeś mniej.", (baseWagePerShift * 0.7).toInt())
                    else -> WorkResult("Dzień w warsztacie", "Cały dzień spędziłeś przy warsztacie. Twoja praca została doceniona.", baseWagePerShift)
                }
            }
            else -> WorkResult("Koniec pracy", "Skończyłeś swoją zmianę.", baseWagePerShift)
        }
    }

    data class GuardEncounter(
        val title: String,
        val description: String,
        val fine: Int = 0,
        val fatigue: Int = 0,
        val wantedIncrease: Int = 0,
        val isArrested: Boolean = false
    )

    enum class Weather(val displayName: String, val icon: String, val fatigueMod: Int, val crimeMod: Int) {
        SUNNY("Słonecznie", "☀️", 0, 0),
        CLOUDY("Pochmurno", "☁️", 0, -5),
        RAINY("Deszczowo", "🌧️", 5, -15),
        STORMY("Burzowo", "⛈️", 10, -30),
        FOGGY("Mgliście", "🌫️", 2, -20),
        SNOWY("Śnieżnie", "❄️", 8, -10),
        HEATWAVE("Upalnie", "🔥", 15, 0)
    }

    fun getWeather(heroData: String): Weather {
        val calendar = getCalendarData(heroData)
        val parts = calendar.split("|").map { it.trim() }
        val weatherName = if (parts.size >= 5) parts[4] else ""
        return Weather.values().find { it.name == weatherName } ?: Weather.SUNNY
    }

    fun updateWeather(heroData: String, newWeather: Weather): String {
        val calendar = getCalendarData(heroData)
        val parts = calendar.split("|").map { it.trim() }.toMutableList()
        while (parts.size < 5) parts.add("")
        parts[4] = newWeather.name
        val newCalendar = parts.joinToString(" | ")
        return updateCalendarData(heroData, newCalendar)
    }

    fun checkGuardEncounter(heroData: String, isNight: Boolean, districtType: DistrictType? = null): GuardEncounter? {
        val wantedLevel = getWantedLevel(heroData)
        if (wantedLevel <= 0) return null

        val weather = getWeather(heroData)
        
        // District modifiers
        val districtMod = when (districtType) {
            DistrictType.SLUMS -> -15
            DistrictType.HARBOR -> -5
            DistrictType.MILITARY -> 20
            DistrictType.RULER_SEAT -> 30
            DistrictType.MERCHANT -> 10
            else -> 0
        }

        val baseChance = wantedLevel / 2
        val nightMod = if (isNight) -20 else 5 // W nocy trudniej o patrol, ale...
        val chance = (baseChance + nightMod + weather.crimeMod + districtMod).coerceIn(5, 85)

        if ((1..100).random() > chance) return null

        val roll = (1..100).random()
        return when {
            roll > 90 && wantedLevel > 50 -> GuardEncounter(
                "ARESZTOWANIE!",
                "Zostałeś rozpoznany jako groźny przestępca! Strażnicy nie biorą jeńców i zakuwają Cię w kajdany.",
                fine = (wantedLevel * 5),
                fatigue = 30,
                wantedIncrease = 10,
                isArrested = true
            )
            roll > 50 -> GuardEncounter(
                "KONTROLA STRAŻY",
                "Patrol straży miejskiej zatrzymał Cię do wyjaśnienia. Twoja twarz wydaje im się znajoma...",
                fine = (wantedLevel * 2),
                fatigue = 5,
                wantedIncrease = 2
            )
            else -> GuardEncounter(
                "PODEJRZLIWE SPOJRZENIA",
                "Strażnicy bacznie Cię obserwują, ale decydują się nie interweniować tym razem. Musisz uważać.",
                wantedIncrease = 5
            )
        }
    }

    data class TravelSkillCheckResult(
        val success: Boolean,
        val isCriticalSuccess: Boolean,
        val isCriticalFailure: Boolean,
        val roll: Int,
        val chance: Int,
        val skillVal: Int,
        val pnAwarded: Int
    )

    /**
     * Ujednolicony test umiejętności dla akcji na mapie podróży (zbieractwo, Obóz).
     * Ranga min. Uczeń (skillVal >= 1): szansa = atrybut + (ranga x 5).
     * Brak umiejętności (skillVal == 0): szansa = atrybut - 30.
     * Minimalna szansa zawsze wynosi 15%.
     * Nagroda PN naliczana analogicznie do testów umiejętności w GameActivity.
     */
    fun performTravelSkillCheck(heroData: String, skillName: String, attrKey: String): TravelSkillCheckResult {
        val lines = heroData.lines()
        val skillVal = getSkillRank(lines, skillName)
        val attrVal = getStatValue(heroData, attrKey)
        val rawChance = if (skillVal >= 1) attrVal + (skillVal * 5) else attrVal - 30
        val chance = rawChance.coerceAtLeast(15)

        val roll = (1..100).random()
        val rollStr = if (roll < 10) "0$roll" else if (roll == 100) "00" else roll.toString()
        val isDoublet = rollStr[0] == rollStr[1]

        val success = roll <= chance
        val isCriticalSuccess = (isDoublet && success) || (roll <= chance - 30)
        val isCriticalFailure = (isDoublet && !success) || (roll >= chance + 30)

        val pnAwarded = if (skillVal == 0) {
            when {
                isDoublet -> if (roll <= chance) chance / 10 else 0
                roll <= chance - 30 -> 3
                roll <= chance -> 2
                roll < chance + 30 -> 1
                else -> 0
            }
        } else {
            when {
                isCriticalFailure -> 0
                isCriticalSuccess -> chance / 10
                success -> 2
                else -> 1
            }
        }

        return TravelSkillCheckResult(success, isCriticalSuccess, isCriticalFailure, roll, chance, skillVal, pnAwarded.coerceAtLeast(0))
    }

    fun performGathering(
        context: android.content.Context,
        heroId: Int,
        action: GatheringAction,
        readFromFile: (String) -> String,
        saveToFile: (String, String) -> Unit,
        heroProgress: HeroProgress? = null,
        onHeroProgressChange: (HeroProgress) -> Unit = {}
    ): String {
        val fileName = "hero_$heroId"
        val heroData = readFromFile(fileName)
        val lines = heroData.lines().toMutableList()
        
        // Use provided progress or load from file
        var currentProgress = heroProgress ?: getHeroProgress(heroId.toString(), heroData)

    // 1. Check Fatigue
    if (currentProgress.fatigue >= currentProgress.maxFatigue) return "Jesteś zbyt zmęczony na zbieractwo!"

    // 2. Roll for success (ujednolicony test)
    val check = HeroLogic.performTravelSkillCheck(heroData, action.skillName, action.attribute)
    val skillVal = check.skillVal
    
    // Advance time by 2 hours
    val calendarData = currentProgress.currentCalendar
    val newCalendar = HeroLogic.advanceTime(calendarData, 2)
    HeroLogic.updateCalendarInLines(lines, newCalendar)
    
    // Update Fatigue
    var nextProgress = currentProgress.copy(currentCalendar = newCalendar)
    nextProgress = updateHeroFatigue(nextProgress, 15)
    
    // Sync legacy Fatigue line for safety
    val fatigueIdx = lines.indexOfFirst { it.trim().startsWith("Zmęczenie:") }
    if (fatigueIdx != -1) {
        lines[fatigueIdx] = "Zmęczenie: ${nextProgress.fatigue} / ${nextProgress.maxFatigue}"
    }

    var updatedHeroData = lines.joinToString("\n")
    if (check.pnAwarded > 0) {
        updatedHeroData = addPnToSkill(updatedHeroData, action.skillName, check.pnAwarded)
    }

    if (!check.success) {
        saveToFile(fileName, updatedHeroData)
        onHeroProgressChange(nextProgress)
        val progressJson = kotlinx.serialization.json.Json.encodeToString(nextProgress)
        saveToFile("progress_$heroId.json", progressJson)
        val pnInfo = if (check.pnAwarded > 0) " (+${check.pnAwarded} PN ${action.skillName})" else ""
        return "Nic nie znaleziono podczas 2-godzinnych poszukiwań (${action.name})$pnInfo."
    }

    // 3. Determine Quality and Item
    val qualityRoll = (1..100).random()
    val quality = when {
        qualityRoll > 95 && skillVal >= 2 -> "Unikalne"
        qualityRoll > 80 && skillVal >= 1 -> "Rzadkie"
        else -> "Podstawowe"
    }

    val itemName = when(action.name) {
        "Zioła" -> when(quality) {
            "Unikalne" -> listOf("Mandragora", "Kwiat paproci", "Srebrny lotos").random()
            "Rzadkie" -> listOf("Belladona (Pokrzyk)", "Wilcza jagoda", "Tojad jadowity", "Arnika górska", "Korzeń żeń-szenia").random()
            else -> listOf("Mięta pieprzowa", "Szałwia lekarska", "Pokrzywa zwyczajna", "Mniszek pospolity", "Rumianek polny", "Kora wierzby", "Krwawnik", "Dziki len", "Kwiat czarnego bzu", "Lawenda").random()
        }
        "Minerały" -> when(quality) {
            "Unikalne" -> listOf("Ruda złota", "Czarny diament", "Ruda meteorytowa (Meteoryt)").random()
            "Rzadkie" -> listOf("Ruda srebra", "Malachit", "Obsydian", "Fluoryt fluorowy", "Lazuryt (Lapis Lazuli)").random()
            else -> listOf("Ruda żelaza", "Ruda miedzi", "Ruda cyny", "Węgiel brunatny", "Kwarc dymny", "Krzemień", "Piryt (Złoto głupców)", "Sól kamienna", "Grudka gliny", "Czysta siarka").random()
        }
        "Łowiectwo" -> when(quality) {
            "Unikalne" -> listOf("Błękitny Lis Polarny", "Barghest Bagienny", "Gryf", "Jeżozwierz Olbrzymi", "Hydra").random()
            "Rzadkie" -> listOf("Dzik", "Jeleń", "Łoś", "Żubr", "Wilk", "Niedźwiedź", "Pantera").random()
            else -> listOf("Jarząbek", "Kuropatwa", "Bażant", "Kaczka krzyżówka", "Zając", "Królik dziki", "Borsuk", "Lis", "Sarna", "Kozica", "Muflon").random()
        }
        "Rybołówstwo" -> when(quality) {
            "Unikalne" -> listOf("Głębinowy Świetlik", "Kolczasty Sum Bagienny", "Księżycowy Okoń", "Mglisty Karp", "Żarłacz Pustynny", "Błękitna Ryba Nefrytowa", "Gwiezdny Jesiotr").random()
            "Rzadkie" -> listOf("Sum", "Węgorz", "Jesiotr", "Dorsz", "Sandacz").random()
            else -> listOf("Płotka", "Szczupak", "Pstrąg", "Łosoś", "Karp", "Okoń", "Karaś", "Flądra", "Lin", "Brzana", "Płóć").random()
        }
        "Grzyby" -> when(quality) {
            "Unikalne" -> listOf("Astralny Kapelusz", "Smoczy Zarodnik", "Kryształowy Zarodnik").random()
            "Rzadkie" -> listOf("Borowik Cmentarny", "Krwawiec Jaskiniowy", "Złocień Wiedźmi", "Zasłonak Cienisty", "Płomiennik Nocny").random()
            else -> listOf("Skrajnik Szary", "Mechowiec Wilgotny", "Zarodnikowiec Polny", "Purchawka Rdzawa", "Koronnik Drzewny", "Kapelusznik Blady", "Zgniłek Bagienny", "Łuskowiec Skalny").random()
        }
        else -> "Nieznany przedmiot"
    }

    // Determine weight based on category and quality
    val weight = when(action.name) {
        "Zioła" -> (2..15).random() / 100.0
        "Grzyby" -> (3..20).random() / 100.0
        "Minerały" -> (3..25).random() / 10.0
        "Łowiectwo" -> when(quality) {
            "Unikalne" -> (50..150).random() / 10.0
            "Rzadkie" -> (30..80).random() / 10.0
            else -> (5..30).random() / 10.0
        }
        "Rybołówstwo" -> when(quality) {
            "Unikalne" -> (20..60).random() / 10.0
            "Rzadkie" -> (15..40).random() / 10.0
            else -> (2..20).random() / 10.0
        }
        else -> 0.1
    }

    // 4. Add to Gathered Resources section (ZBIORY)
    val updatedData = HeroLogic.addGatheredItem(updatedHeroData, action.name, itemName, weight, quality)
    saveToFile(fileName, updatedData)
    saveToFile("progress_$heroId.json", kotlinx.serialization.json.Json.encodeToString(nextProgress))
    onHeroProgressChange(nextProgress)

    val pnInfo = if (check.pnAwarded > 0) " (+${check.pnAwarded} PN ${action.skillName})" else ""
    return "Znaleziono: $itemName ($quality, ${String.format(java.util.Locale.US, "%.2f", weight)} kg)! Zużyto 2h i 15 energii.$pnInfo"
}

    // --- OBÓZ: wspólne akcje dostępne na każdym biomie mapy podróży ---

    fun getHeroBooks(heroData: String): List<String> {
        return getHeroInventory(heroData)
            .map { it.name }
            .distinct()
            .filter { it.startsWith("Księga", ignoreCase = true) && it.contains("(") && it.contains(")") }
    }

    fun extractBookTopic(bookName: String): String {
        return bookName.substringAfter("(").substringBefore(")").trim()
    }

    private fun saveCampResult(
        heroId: Int,
        fileName: String,
        lines: MutableList<String>,
        nextProgress: HeroProgress,
        saveToFile: (String, String) -> Unit,
        onHeroProgressChange: (HeroProgress) -> Unit
    ) {
        val fatigueIdx = lines.indexOfFirst { it.trim().startsWith("Zmęczenie:") }
        if (fatigueIdx != -1) {
            lines[fatigueIdx] = "Zmęczenie: ${nextProgress.fatigue} / ${nextProgress.maxFatigue}"
        }
        saveToFile(fileName, lines.joinToString("\n"))
        saveToFile("progress_$heroId.json", kotlinx.serialization.json.Json.encodeToString(nextProgress))
        onHeroProgressChange(nextProgress)
    }

    fun performCampRest(
        heroId: Int,
        hours: Int,
        readFromFile: (String) -> String,
        saveToFile: (String, String) -> Unit,
        heroProgress: HeroProgress? = null,
        onHeroProgressChange: (HeroProgress) -> Unit = {}
    ): String {
        val fileName = "hero_$heroId"
        val heroData = readFromFile(fileName)
        val lines = heroData.lines().toMutableList()
        val currentProgress = heroProgress ?: getHeroProgress(heroId.toString(), heroData)

        val newCalendar = HeroLogic.advanceTime(currentProgress.currentCalendar, hours)
        HeroLogic.updateCalendarInLines(lines, newCalendar)

        // Oblicz Wytrzymałość = Odporność / 10, regeneracja = Wytrzymałość x 2
        val stamina = (HeroLogic.getStatValue(heroData, "ODPORNOŚĆ") / 10).coerceAtLeast(1)
        val baseRecovery = hours * RestManager.getCampFatigueReductionPerHour(stamina)
        val itemBonus = hours * RestManager.getItemBonusForCamp(heroData)
        val fatigueRecovery = baseRecovery + itemBonus
        
        var nextProgress = currentProgress.copy(currentCalendar = newCalendar)
        nextProgress = updateHeroFatigue(nextProgress, -fatigueRecovery)

        saveCampResult(heroId, fileName, lines, nextProgress, saveToFile, onHeroProgressChange)
        
        val hoursDisplay = if (hours == 1) "1h" else "${hours}h"
        val bonusText = if (itemBonus > 0) " (w tym +$itemBonus z przedmiotów)" else ""
        return "Odpoczywałeś przez $hoursDisplay (regeneracja: $fatigueRecovery pkt$bonusText). Zmęczenie: ${nextProgress.fatigue} / ${nextProgress.maxFatigue}."
    }

    fun performBookReading(
        heroId: Int,
        bookName: String,
        readFromFile: (String) -> String,
        saveToFile: (String, String) -> Unit,
        heroProgress: HeroProgress? = null,
        onHeroProgressChange: (HeroProgress) -> Unit = {}
    ): String {
        val fileName = "hero_$heroId"
        val heroData = readFromFile(fileName)
        val lines = heroData.lines().toMutableList()
        val currentProgress = heroProgress ?: getHeroProgress(heroId.toString(), heroData)

        val skillName = extractBookTopic(bookName)
        if (skillName.isEmpty()) return "Ta książka nie ma rozpoznawalnego tematu."

        val skillInfo = getSkillInfo(heroData, skillName)
        val check = performTravelSkillCheck(heroData, skillName, skillInfo.stat)

        val newCalendar = HeroLogic.advanceTime(currentProgress.currentCalendar, 1)
        HeroLogic.updateCalendarInLines(lines, newCalendar)

        var nextProgress = currentProgress.copy(currentCalendar = newCalendar)
        nextProgress = updateHeroFatigue(nextProgress, 5)

        val updatedData = if (check.pnAwarded > 0) addPnToSkill(lines.joinToString("\n"), skillName, check.pnAwarded) else lines.joinToString("\n")
        val updatedLines = updatedData.lines().toMutableList()

        saveCampResult(heroId, fileName, updatedLines, nextProgress, saveToFile, onHeroProgressChange)

        return if (check.success) {
            "Udana lektura „$bookName”! Zdobyto ${check.pnAwarded} PN do umiejętności $skillName."
        } else {
            "Nie do końca zrozumiałeś tekst „$bookName”, ale i tak wyciągnąłeś naukę: +${check.pnAwarded} PN do $skillName."
        }
    }

    fun performCampCooking(
        heroId: Int,
        readFromFile: (String) -> String,
        saveToFile: (String, String) -> Unit,
        heroProgress: HeroProgress? = null,
        onHeroProgressChange: (HeroProgress) -> Unit = {}
    ): String {
        val fileName = "hero_$heroId"
        val heroData = readFromFile(fileName)
        val lines = heroData.lines().toMutableList()
        val currentProgress = heroProgress ?: getHeroProgress(heroId.toString(), heroData)

        val ingredientCategories = listOf("Grzyby", "Zioła", "Łowiectwo", "Rybołówstwo")
        val availableCategory = ingredientCategories.firstOrNull {
            currentProgress.gatheredResources[it]?.isNotEmpty() == true
        } ?: return "Brak znalezionych składników do przyrządzenia posiłku!"

        val ingredients = currentProgress.gatheredResources.getValue(availableCategory)
        val consumedIngredient = ingredients.random()
        val remainingIngredients = ingredients.toMutableList().also { it.remove(consumedIngredient) }
        val newGatheredResources = currentProgress.gatheredResources.toMutableMap()
        if (remainingIngredients.isEmpty()) newGatheredResources.remove(availableCategory)
        else newGatheredResources[availableCategory] = remainingIngredients

        val check = performTravelSkillCheck(heroData, "Gotowanie", "INT")

        val newCalendar = HeroLogic.advanceTime(currentProgress.currentCalendar, 1)
        HeroLogic.updateCalendarInLines(lines, newCalendar)

        var nextProgress = currentProgress.copy(
            currentCalendar = newCalendar,
            gatheredResources = newGatheredResources
        )
        val fatigueDelta = if (check.success) -15 else -5
        nextProgress = updateHeroFatigue(nextProgress, fatigueDelta)

        val updatedData = if (check.pnAwarded > 0) addPnToSkill(lines.joinToString("\n"), "Gotowanie", check.pnAwarded) else lines.joinToString("\n")
        val updatedLines = updatedData.lines().toMutableList()

        saveCampResult(heroId, fileName, updatedLines, nextProgress, saveToFile, onHeroProgressChange)

        val pnInfo = if (check.pnAwarded > 0) " (+${check.pnAwarded} PN Gotowanie)" else ""
        return if (check.success) {
            "Przyrządziłeś udany posiłek ze składników ($consumedIngredient)! Zmęczenie zredukowane o 15.$pnInfo"
        } else {
            "Posiłek wyszedł mizernie, ale zaspokoił głód. Zmęczenie zredukowane o 5.$pnInfo"
        }
    }

    fun performTrapSetting(
        heroId: Int,
        readFromFile: (String) -> String,
        saveToFile: (String, String) -> Unit,
        heroProgress: HeroProgress? = null,
        onHeroProgressChange: (HeroProgress) -> Unit = {}
    ): String {
        val fileName = "hero_$heroId"
        val heroData = readFromFile(fileName)
        val lines = heroData.lines().toMutableList()
        val currentProgress = heroProgress ?: getHeroProgress(heroId.toString(), heroData)

        val check = performTravelSkillCheck(heroData, "Wykrywanie pułapek", "INT")
        val skillVal = check.skillVal

        val newCalendar = HeroLogic.advanceTime(currentProgress.currentCalendar, 3)
        HeroLogic.updateCalendarInLines(lines, newCalendar)

        var nextProgress = currentProgress.copy(currentCalendar = newCalendar)
        nextProgress = updateHeroFatigue(nextProgress, 10)

        var updatedData = lines.joinToString("\n")
        if (check.pnAwarded > 0) {
            updatedData = addPnToSkill(updatedData, "Wykrywanie pułapek", check.pnAwarded)
        }
        val pnInfo = if (check.pnAwarded > 0) " (+${check.pnAwarded} PN Wykrywanie pułapek)" else ""
        val resultText: String
        if (check.success) {
            val qualityRoll = (1..100).random()
            val quality = when {
                qualityRoll > 95 && skillVal >= 2 -> "Unikalne"
                qualityRoll > 80 && skillVal >= 1 -> "Rzadkie"
                else -> "Podstawowe"
            }
            val itemName = when (quality) {
                "Unikalne" -> listOf("Błękitny Lis Polarny", "Barghest Bagienny", "Gryf").random()
                "Rzadkie" -> listOf("Dzik", "Jeleń", "Łoś", "Wilk", "Niedźwiedź").random()
                else -> listOf("Jarząbek", "Kuropatwa", "Zając", "Królik dziki", "Borsuk", "Lis", "Sarna").random()
            }
            val weight = when (quality) {
                "Unikalne" -> (50..150).random() / 10.0
                "Rzadkie" -> (30..80).random() / 10.0
                else -> (5..30).random() / 10.0
            }
            updatedData = addGatheredItem(updatedData, "Łowiectwo", itemName, weight, quality)
            resultText = "Pułapka złapała zwierzynę: $itemName ($quality, ${String.format(java.util.Locale.US, "%.2f", weight)} kg)!$pnInfo"
        } else {
            resultText = "Zastawiona pułapka pozostała pusta.$pnInfo"
        }

        val updatedLines = updatedData.lines().toMutableList()
        saveCampResult(heroId, fileName, updatedLines, nextProgress, saveToFile, onHeroProgressChange)
        return resultText
    }

    // Funkcje dla Fryzjera
    fun updateHairColor(heroData: String, newColor: String): String {
        var updated = heroData
        val colorKeywords = listOf("Czarny", "Brązowy", "Rudy", "Blond", "Szary", "Biały")
        var foundColor = false
        
        updated = updated.lines().joinToString("\n") { line ->
            if (line.contains("Włosy:") || line.contains("Hair Color:")) {
                foundColor = true
                val prefix = line.substringBefore(":")
                "$prefix: $newColor"
            } else {
                line
            }
        }
        
        // Jeśli nie znaleziono linii z kolorem włosów, dodaj nową linię
        if (!foundColor) {
            updated += "\nWłosy: $newColor"
        }
        
        return updated
    }

    fun addTraitToHero(heroData: String, traitName: String): String {
        if (hasTraitContaining(heroData, traitName)) return heroData
        
        var updated = heroData
        val traitsLine = updated.lines().indexOfFirst { it.contains("CECHY:") || it.contains("TRAITS:") }
        
        if (traitsLine >= 0) {
            val lines = updated.lines().toMutableList()
            lines[traitsLine] = lines[traitsLine] + ", $traitName"
            updated = lines.joinToString("\n")
        } else {
            updated += "\nCECHY: $traitName"
        }
        
        return updated
    }

    fun removeTraitFromHero(heroData: String, traitName: String): String {
        val lines = heroData.lines().toMutableList()
        var found = false
        
        for (i in lines.indices) {
            if (lines[i].contains("CECHY:") || lines[i].contains("TRAITS:")) {
                val traitsContent = lines[i].substringAfter(":").trim()
                val traits = traitsContent.split(",").map { it.trim() }
                val filtered = traits.filter { !it.equals(traitName, ignoreCase = true) }
                
                if (filtered.isEmpty()) {
                    lines[i] = lines[i].substringBefore(":") + ":"
                } else {
                    lines[i] = lines[i].substringBefore(":") + ": " + filtered.joinToString(", ")
                }
                found = true
                break
            }
        }
        
        return lines.joinToString("\n")
    }

    fun findTraitContaining(heroData: String, keyword: String): String? {
        val traitsLine = heroData.lines().find { it.contains("CECHY:") || it.contains("TRAITS:") }
        if (traitsLine != null) {
            val traitsContent = traitsLine.substringAfter(":")
            val traits = traitsContent.split(",").map { it.trim() }
            return traits.find { it.contains(keyword, ignoreCase = true) }
        }
        return null
    }

    fun hasTraitContaining(heroData: String, keyword: String): Boolean {
        return findTraitContaining(heroData, keyword) != null
    }
}

