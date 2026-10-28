package com.example.genpro

import androidx.compose.ui.graphics.Color

data class Shop(val name: String, val sub: String, val categories: List<String>, val description: String = "")
data class TradeItem(val name: String, val price: Int, val category: String)

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

    fun getSkillRank(heroLines: List<String>, skillName: String): Int {
        val line = heroLines.find { it.contains(skillName, ignoreCase = true) }
        return when {
            line == null || line.contains("Brak", true) -> 0
            line.contains("Arcymistrz", true) -> 5
            line.contains("Mistrz", true) -> 4
            line.contains("Ekspert", true) -> 3
            line.contains("Weteran", true) -> 2
            line.contains("Uczeń", true) -> 1
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

        if (!line.contains("Plus:")) return 0
        return line.substringAfter("Plus:").trim().toIntOrNull() ?: 0
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
        Shop("ZBROJMISTRZ", "Broń i pancerze", listOf("Broń prosta", "Sieczna", "Obuchowa", "Kłuta", "Broń Egzotyczna", "Łuk", "Kusza", "Prochowa", "Miotana", "Miotana specjalna", "Pancerz skórzany", "Pancerz ćwiekowany", "Pancerz łuskowy", "Pancerz płytowy")),
        Shop("RYMARZ", "Torby i uprząż", listOf("Torby i Plecaki", "Sakwy i sakiewki", "Tykwy i Bukłaki", "Konie", "Konie robocze", "Wozy lekkie", "Wozy ciężkie")),
        Shop("PŁATNERZ", "Pancerze i tarcze", listOf("Pancerz skórzany", "Pancerz ćwiekowany", "Pancerz łuskowy", "Pancerz płytowy", "Tarcze")),
        Shop("KARCZMA", "Jedzenie i trunki", listOf("Posiłki", "Trunki", "Nocleg")),
        Shop("STAJNIA", "Konie i wozy", listOf("Konie", "Konie robocze", "Wozy lekkie", "Wozy ciężkie", "Nocleg")),
        Shop("PRACA", "Dorywcze zajęcia", listOf("Praca"))
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
        SpecialItem("Wytrychy Mistrzowskie", "Narzędzia", 5, 800, "+10 do Otwierania zamków"),
        SpecialItem("Przybory Alchemiczne", "Narzędzia", 40, 1200, "Niezbędne do warzenia mikstur"),
        SpecialItem("Zestaw Inżynierski", "Narzędzia", 25, 600, "+5 do Inżynierii i Kartografii")
    )

    val instrumentsSpecialist = listOf(
        SpecialItem("Lutnia podróżna", "Instrumenty", 15, 400, "+5 do Uroku osobistego"),
        SpecialItem("Flet z kości", "Instrumenty", 2, 150),
        SpecialItem("Dudy", "Instrumenty", 30, 350, "Zastraszenie +5 przy marszu"),
        SpecialItem("Bębenek obręczowy", "Instrumenty", 10, 200)
    )

    val itemsExotic = listOf(
        SpecialItem("Lampa z soczewką", "Egzotyczne", 15, 300, "Skupiony snop światła 15m"),
        SpecialItem("Lustro stalowe", "Egzotyczne", 5, 250, "Nietłukące, przydatne do zwiadu"),
        SpecialItem("Klepsydra (15 min)", "Egzotyczne", 8, 450),
        SpecialItem("Wodoodporna tuba na pergamin", "Egzotyczne", 10, 100)
    )

    val bogactwaAll = arrayOf("Miedź", "Srebro", "Złoto", "Gryfon", "Sztabka Srebra", "Sztabka Złota", "Biżuteria")

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
        var total = 0
        heroData.lines().forEach { line ->
            if (line.contains("⛀")) {
                val v = line.trim().split("  ").last().toIntOrNull() ?: 0
                if (line.contains("Złota korona (zk)")) total += v * 480
                else if (line.contains("Srebrny szyling (s)")) total += v * 12
                else if (line.contains("Miedziany pens (p)")) total += v
            }
        }
        return total
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
        "Warzenie trucizn", "Widzenie w ciemności", "Wyczucie kierunku",
        "Wyczucie kierunku pod ziemią", "Wykrywanie pułapek", "Zarządzanie",
        "Zielarstwo"
    )

    val skillSW = arrayOf(
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
        "Uzdolnienia językowe", 
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
        "Język obcy: Mowa Klasyczny", "Język obcy: Pismo Klasyczny",
        "Język obcy: Mowa Magiczny", "Język obcy: Pismo Magiczny"
    )

    val skillJezykObcyNazwa = arrayOf(
        "Brak", "Imperialny", "Bretoński", "Kislewski", "Estalijski", "Tileański",
        "Arabski", "Albioński", "Nordycki", "Fan-Eltharin", "Fan-Eltharin",
        "Tor-Eltharin", "Druhir", "Druhir", "Khazalid"
    )

    val allNormalSkills = HeroLogic.skillWW + HeroLogic.skillUS + HeroLogic.skillKrzepa + HeroLogic.skillOdpornosc + HeroLogic.skillZrecznosc +
            HeroLogic.skillInteligencja + HeroLogic.skillSW + HeroLogic.skillCharyzma + HeroLogic.skillWiedza + HeroLogic.skillJezyk

    val skillRanks = listOf("Brak", "Uczeń", "Adept", "Ekspert", "Mistrz", "Arcymistrz")

    fun getTimeOfDay(timeStr: String): String {
        // timeStr expected: "d.M.y | HH:mm" or just "HH:mm"
        val timePart = if (timeStr.contains("|")) timeStr.substringAfterLast("|").trim() else timeStr
        val hour = timePart.substringBefore(":").toIntOrNull() ?: 0
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

    fun getShopInventory(shop: Shop, forceAll: Boolean = false): List<TradeItem> {
        val list = mutableListOf<TradeItem>()
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
                
                "Pancerz skórzany" -> {
                    list.add(TradeItem("Skórzany czepiec", 500, cat))
                    list.add(TradeItem("Skórzana kamizelka", 1200, cat))
                    list.add(TradeItem("Skórzana kurtka", 2500, cat))
                    list.add(TradeItem("Skórzane nogawice", 550, cat))
                }
                "Pancerz ćwiekowany" -> {
                    list.add(TradeItem("Ćwiekowany czepiec", 750, cat))
                    list.add(TradeItem("Ćwiekowany kaftan", 2700, cat))
                    list.add(TradeItem("Ćwiekowana kurtka", 2700, cat))
                    list.add(TradeItem("Ćwiekowane nogawice", 900, cat))
                }
                "Pancerz łuskowy" -> {
                    list.add(TradeItem("Hełm łuskowy", 2500, cat))
                    list.add(TradeItem("Kirys łuskowy", 5000, cat))
                    list.add(TradeItem("Łuskowa kurtka", 7500, cat))
                    list.add(TradeItem("Łuskowe nogawice", 1600, cat))
                }
                "Pancerz płytowy" -> {
                    list.add(TradeItem("Hełm płytowy", 5000, cat))
                    list.add(TradeItem("Kirys płytowy", 4000, cat))
                    list.add(TradeItem("Płytowe naramienniki", 2000, cat))
                    list.add(TradeItem("Płytowe nagolenniki", 2100, cat))
                    list.add(TradeItem("Płytowe rękawice", 2000, cat))
                }
                "Tarcze" -> {
                    list.add(TradeItem(" puklerz", 400, cat))
                    list.add(TradeItem("Tarcza", 1200, cat))
                    list.add(TradeItem("Tarcza duża", 2400, cat))
                }
                
                "Sakwy i sakiewki" -> itemSakwy.forEach { list.add(TradeItem(it, findItemPrice(it), cat)) }
                "Torby i Plecaki" -> itemTorby.forEach { list.add(TradeItem(it, findItemPrice(it), cat)) }
                "Tykwy i Bukłaki" -> itemBuklaki.forEach { list.add(TradeItem(it, findItemPrice(it), cat)) }
                
                "Konie" -> {
                    list.add(TradeItem("Koń wierzchowy (+20 Szy)", 38400, cat))
                    list.add(TradeItem("Koń juczny (+500 Udź)", 19200, cat))
                    list.add(TradeItem("Kuc (+10 Wytrzym)", 9600, cat))
                }
                "Konie robocze" -> {
                    list.add(TradeItem("Koń pociągowy (Draft)", 28800, cat))
                    list.add(TradeItem("Muł roboczy", 14400, cat))
                }
                "Wozy lekkie" -> {
                    list.add(TradeItem("Rydwan bojowy (2 konie)", 72000, cat))
                    list.add(TradeItem("Lekka bryczka (1 koń)", 48000, cat))
                    list.add(TradeItem("Wóz kryty (2 konie)", 120000, cat))
                }
                "Wozy ciężkie" -> {
                    list.add(TradeItem("Wóz kupiecki (2-4 konie)", 96000, cat))
                    list.add(TradeItem("Kareta luksusowa (4 konie)", 384000, cat))
                    list.add(TradeItem("Wóz pancerny (6 koni)", 720000, cat))
                }
                
                "Posiłki" -> {
                    list.add(TradeItem("Skromny posiłek", 5, cat))
                    list.add(TradeItem("Syty posiłek", 12, cat))
                    list.add(TradeItem("Uczta", 48, cat))
                }
                "Trunki" -> {
                    list.add(TradeItem("Piwo (kufel)", 2, cat))
                    list.add(TradeItem("Wino (karafka)", 10, cat))
                    list.add(TradeItem("Gorzałka (kieliszek)", 4, cat))
                }
                "Nocleg" -> {
                    list.add(TradeItem("Miejsce na sianie", 1, cat))
                    list.add(TradeItem("Wspólna sala", 3, cat))
                    list.add(TradeItem("Pokój jednoosobowy", 15, cat))
                }
                "Praca" -> {
                    list.add(TradeItem("Posługiwanie", 8, cat))
                    list.add(TradeItem("Muzyk", 15, cat))
                    list.add(TradeItem("Stajenny", 10, cat))
                }
            }
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
        val eqIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
        if (eqIdx == -1) return 0
        
        val inventoryLines = lines.drop(eqIdx + 1).takeWhile { it.startsWith("   •") || it.startsWith("•") || it.isBlank() }
        
        var totalWeight = 0.0
        var tragarzBonusCount = 0

        inventoryLines.forEach { itemLine ->
            val item = itemLine.replace("   •", "").replace("•", "").trim()
            if (item.isEmpty()) return@forEach
            
            // Waga monet (0.015 kg = 0.15 j.)
            if (item.contains("⛀")) {
                val quantity = item.split("  ").last().trim().toIntOrNull() ?: 0
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

    fun getHeroGender(heroData: String): String {
        val line = heroData.lines().find { it.startsWith("Płeć:") }
        return if (line?.contains("Kobieta", ignoreCase = true) == true) "F" else "M"
    }

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
        return (strength * 8) + (toughness * 10)
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
                    line.contains("Złota", ignoreCase = true) -> g = value
                    line.contains("Srebrny", ignoreCase = true) -> s = value
                    line.contains("Miedziany", ignoreCase = true) -> c = value
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
        isManual: Boolean = false
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
        val (gold, silver, copper) = parseMoney(heroData)
        
        val totalCopper = (gold * 480) + (silver * 12) + copper + copperAmount
        val finalCopper = if (totalCopper < 0) 0 else totalCopper
        
        val newG = finalCopper / 480
        val newS = (finalCopper % 480) / 12
        val newC = finalCopper % 12
        
        updateCoinLine(lines, "⛀ Złota korona (zk)", newG)
        updateCoinLine(lines, "⛀ Srebrny szyling (s)", newS)
        updateCoinLine(lines, "⛀ Miedziany pens (p)", newC)
        
        return lines.joinToString("\n")
    }

    fun getRepairCost(itemName: String, currentQuality: Int, basePrice: Int): Int {
        val missingQuality = 100 - currentQuality
        if (missingQuality <= 0) return 0
        // Koszt naprawy to % brakującej jakości z 40% ceny bazowej przedmiotu
        return (basePrice * 0.4 * (missingQuality / 100.0)).toInt()
    }

    fun repairItem(heroData: String, itemName: String, cost: Int): String {
        val dataWithLessMoney = addMoney(heroData, -cost)
        val lines = dataWithLessMoney.lines().toMutableList()
        
        val idx = lines.indexOfFirst { it.contains(itemName) && it.startsWith("   •") }
        if (idx != -1) {
            val line = lines[idx]
            if (line.contains("[Jakość:")) {
                lines[idx] = line.replace(Regex("\\[Jakość: \\d+%\\]"), "[Jakość: 100%]")
            } else {
                lines[idx] = "$line [Jakość: 100%]"
            }
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
        // Fallback do starego formatu DATA: jeśli istnieje
        val dataLine = lines.find { it.startsWith("DATA:") }
        if (dataLine != null) {
            // DATA: 6.06.666 | GODZINA: 12:00 | DZIEŃ: Poniedziałek
            val parts = dataLine.split("|").map { it.trim() }
            val date = parts.find { it.startsWith("DATA:") }?.substringAfter("DATA:")?.trim() ?: "1.1.2024"
            val time = parts.find { it.startsWith("GODZINA:") }?.substringAfter("GODZINA:")?.trim() ?: "12:00"
            return "Nieznane Miejsce | $date | $time"
        }
        return "Nieznane Miejsce | 1.1.2024 | 12:00"
    }

    fun updateCalendarData(heroData: String, newCalendarStr: String): String {
        val lines = heroData.lines().toMutableList()
        updateCalendarInLines(lines, newCalendarStr)
        return lines.joinToString("\n")
    }

    fun updateCalendarInLines(lines: MutableList<String>, newCalendarStr: String) {
        val calendarIdx = lines.indexOfFirst { it.startsWith("CALENDAR:") }
        val newLine = "CALENDAR: $newCalendarStr"

        if (calendarIdx != -1) {
            lines[calendarIdx] = newLine
        } else {
            // Jeśli nie ma CALENDAR, spróbuj zastąpić DATA lub dodaj na początku
            val dataIdx = lines.indexOfFirst { it.startsWith("DATA:") }
            if (dataIdx != -1) {
                lines[dataIdx] = newLine
            } else {
                lines.add(0, newLine)
            }
        }
    }

    fun advanceTime(dataNowStr: String, hours: Int): String {
        try {
            val parts = dataNowStr.split("|").map { it.trim() }
            if (parts.size < 2) return dataNowStr // Fallback if no separators

            // New format: CityName | Date | Time | SettlementJSON | X | Y | CurrentZone
            val cityName = if (parts.size >= 3) parts[0] else "Nieznane Miejsce"
            val datePart = if (parts.size >= 3) parts[1] else parts[0]
            val timePart = if (parts.size >= 3) parts[2] else parts.getOrNull(1) ?: "12:00"

            val dParts = datePart.split(".")
            var d = dParts.getOrNull(0)?.toIntOrNull() ?: 1
            var m = dParts.getOrNull(1)?.toIntOrNull() ?: 1
            var y = dParts.getOrNull(2)?.toIntOrNull() ?: 2024

            val tParts = timePart.split(":")
            var hh = tParts.getOrNull(0)?.toIntOrNull() ?: 12
            val mm = tParts.getOrNull(1)?.toIntOrNull() ?: 0

            hh += hours

            while (hh >= 24) {
                hh -= 24
                d++
                if (d > 28) { // Uproszczony miesiąc 28-dniowy
                    d = 1
                    m++
                }
                if (m > 12) {
                    m = 1
                    y++
                }
            }

            val newTimeStr = String.format(java.util.Locale.getDefault(), "%s | %d.%d.%d | %02d:%02d", cityName, d, m, y, hh, mm)
            
            // Naturalna regeneracja zmęczenia: -2 pkt na każdą godzinę
            var updatedData = updateHeroFatigue(dataNowStr, -(hours * 2))

            // Append the rest of the parts if they exist (SettlementJSON, X, Y, CurrentZone)
            // If they don't exist, we should at least provide empty slots to maintain the 7-part format if desired,
            // but for now, we just preserve what was there.
            val resultTimeStr = if (parts.size > 3) {
                val remainingParts = parts.subList(3, parts.size).toMutableList()
                // Ensure at least 7 parts are maintained if we are already in the new format
                while (remainingParts.size < 4) { // 3 (base) + 4 (remaining) = 7
                    remainingParts.add("")
                }
                "$newTimeStr | ${remainingParts.joinToString(" | ")}"
            } else {
                newTimeStr
            }
            
            // Zastąpienie starego czasu nowym w pełnych danych bohatera
            return updatedData.lines().map { line ->
                if (line.startsWith("Lokacja:") || line.split("|").size >= 2) {
                    if (line.startsWith("Lokacja:")) "Lokacja: $resultTimeStr" else resultTimeStr
                } else line
            }.joinToString("\n")
        } catch (e: Exception) {
            return dataNowStr
        }
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
            "Laboratorium", "Siodlarnia", "Przymierzalnia"
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
        val rank = getSkillRank(heroData, "Otwieranie zamków")
        val hasSkill = rank != null && rank != "Brak"
        return hasSkill && hasItem(heroData, "Wytrychy")
    }

    data class BurglaryResult(
        val success: Boolean,
        val caught: Boolean,
        val lootItems: List<String>,
        val lootMoney: Int,
        val message: String,
        val updatedHeroData: String
    )

    fun performBurglary(
        heroData: String,
        buildingName: String,
        cityName: String,
        roomName: String,
        currentHour: Int,
        buildingType: String,
        rank: Int
    ): BurglaryResult {
        var currentData = heroData
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
        val currentFatigue = getHeroFatigue(currentData)
        if (currentFatigue >= 95) {
            return BurglaryResult(
                success = false,
                caught = false,
                lootItems = emptyList(),
                lootMoney = 0,
                message = "Jesteś zbyt zmęczony, by skupić się na zamku!",
                updatedHeroData = currentData
            )
        }
        currentData = updateHeroFatigue(currentData, (5..15).random())

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
        val isSuccess = rollSuccess <= (baseSuccessChance + buildingMod + rankMod)

        // Risk of being caught
        val isNight = currentHour in 22..23 || currentHour in 0..4
        var catchChance = if (isNight) 15 else 45
        
        // Dexterity bonus to stealth
        val dexterity = getStatValue(heroData, "Zr")
        catchChance -= (dexterity / 10)
        
        if (buildingType == "Karczma" || buildingType == "Tawerna") catchChance -= 10 // Busy places mask noise
        if (buildingType == "Mieszkanie") catchChance += 10 // Quiet houses are risky
        
        catchChance = catchChance.coerceIn(5, 95)
        
        val rollCatch = (1..100).random()
        val isCaught = rollCatch <= catchChance

        if (isCaught) {
            val fine = (100 * rank..400 * rank).random()
            var penaltyData = addMoney(currentData, -fine)
            // Legal consequences - simulate by reducing reputation or increasing fatigue (stress)
            penaltyData = updateHeroFatigue(penaltyData, 20)
            return BurglaryResult(
                success = false,
                caught = true,
                lootItems = emptyList(),
                lootMoney = 0,
                message = "Zostałeś przyłapany! Straż nałożyła grzywnę: ${formatPrice(fine)} i przesłuchiwała Cię godzinami.",
                updatedHeroData = advanceTime(penaltyData, (2..5).random())
            )
        }

        if (!isSuccess) {
            currentData = removeItem(currentData, "Wytrychy")
            return BurglaryResult(
                success = false,
                caught = false,
                lootItems = emptyList(),
                lootMoney = 0,
                message = "Nie udało się otworzyć zamka. Wytrych pękł!",
                updatedHeroData = currentData
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
        
        // Grant temporary access
        currentData = addStay(currentData, cityName, buildingName, 2) // 2h access

        return BurglaryResult(
            success = true,
            caught = false,
            lootItems = lootItems,
            lootMoney = lootMoney,
            message = "Sukces! Otworzyłeś zamek i splądrowałeś pomieszczenie.",
            updatedHeroData = currentData
        )
    }

    data class WorkResult(val title: String, val description: String, val totalWage: Int)

    fun getHeroGold(heroData: String): Int {
        val line = heroData.lines().find { it.startsWith("Złoto:") }
        return line?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
    }

    fun getHeroSilver(heroData: String): Int {
        val line = heroData.lines().find { it.startsWith("Srebro:") }
        return line?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
    }

    fun getHeroCopper(heroData: String): Int {
        val line = heroData.lines().find { it.startsWith("Miedź:") }
        return line?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
    }

    fun getHeroFatigue(heroData: String): Int {
        val line = heroData.lines().find { it.startsWith("Zmęczenie: ") }
        return line?.substringAfter("Zmęczenie: ")?.trim()?.toIntOrNull() ?: 0
    }

    fun updateHeroFatigue(heroData: String, amount: Int): String {
        val lines = heroData.lines().toMutableList()
        val index = lines.indexOfFirst { it.startsWith("Zmęczenie: ") }
        val currentFatigue = getHeroFatigue(heroData)
        val newFatigue = (currentFatigue + amount).coerceIn(0, 100)
        
        if (index != -1) {
            lines[index] = "Zmęczenie: $newFatigue"
        } else {
            // Jeśli nie ma linii zmęczenia, dodajmy ją pod PD lub poziomem
            val insertIdx = lines.indexOfFirst { it.startsWith("Poziom: ") }.let { if (it != -1) it + 1 else 1 }
            lines.add(insertIdx, "Zmęczenie: $newFatigue")
        }
        return lines.joinToString("\n")
    }

    data class WorkSessionResult(
        val totalWage: Int,
        val totalPn: Int,
        val sessionsPerformed: Int,
        val successes: Int,
        val skillName: String,
        val interruptedByFatigue: Boolean = false
    )

    fun performWorkSessions(
        heroData: String,
        skillName: String,
        hours: Int,
        baseWagePerHour: Int,
        intelligence: Int,
        fatiguePerOrder: Int = 6 // Nowy parametr: zmęczenie na godzinę
    ): Pair<String, WorkSessionResult> {
        var currentHeroData = heroData
        val skillRank = getSkillRank(currentHeroData, skillName) ?: "Uczeń"
        val currentFatigue = getHeroFatigue(currentHeroData)
        
        // Wyciągnięcie bazowej wartości Int (lub innej cechy jeśli rzemiosło bazuje na czymś innym, 
        // ale w planie była Inteligencja)
        val baseSkillValue = intelligence 
        
        // Kary za zmęczenie
        val fatiguePenalty = when {
            currentFatigue > 60 -> 25
            currentFatigue > 30 -> 10
            else -> 0
        }
        
        val rankBonus = when (skillRank) {
            "Adept" -> 10
            "Mistrz" -> 20
            "Arcymistrz" -> 30
            else -> 0
        }
        val targetValue = baseSkillValue + rankBonus - fatiguePenalty

        val numSessions = hours / 2
        var totalWage = hours * baseWagePerHour
        var totalPn = 0
        var successes = 0
        var fatigueInterrupted = false

        val logs = mutableListOf<String>()

        repeat(numSessions) {
            // 5% szansy na wypadek przy wysokim zmęczeniu
            if (currentFatigue > 60 && (1..100).random() <= 5) {
                // Wypadek przerywa pętlę i nakłada karę
                fatigueInterrupted = true
                return@repeat 
            }

            val roll = (1..100).random()
            val isSuccess = roll <= targetValue
            
            // System PN: 2-3 za sukces, 1 za porażkę
            val pnGained = if (isSuccess) (2..3).random() else 1
            totalPn += pnGained
            
            if (isSuccess) {
                successes++
                // Premia pieniężna za zdany test (25-50% zarobku z tej sesji, czyli z 2h)
                val sessionWage = baseWagePerHour * 2
                val bonus = (sessionWage * (25..50).random() / 100.0).toInt()
                totalWage += bonus
            }
            
            // Dodawanie PN do konkretnej umiejętności w heroData
            currentHeroData = addPnToSkill(currentHeroData, skillName, pnGained)
        }

        // Dodanie zmęczenia
        val fatigueGained = hours * fatiguePerOrder
        currentHeroData = updateHeroFatigue(currentHeroData, fatigueGained)

        // Dodanie pieniędzy i czasu
        currentHeroData = addMoney(currentHeroData, totalWage)
        currentHeroData = advanceTime(currentHeroData, hours)

        return Pair(currentHeroData, WorkSessionResult(totalWage, totalPn, numSessions, successes, skillName, fatigueInterrupted))
    }

    private fun addPnToSkill(heroData: String, skillName: String, pn: Int): String {
        val lines = heroData.lines().toMutableList()
        val index = lines.indexOfFirst { it.startsWith("$skillName: ") }
        if (index != -1) {
            val line = lines[index]
            // Format: "Rzemiosło: Ślusarstwo (Uczeń) | PN: 5"
            if (line.contains("| PN: ")) {
                val parts = line.split("| PN: ")
                val currentPn = parts[1].trim().toIntOrNull() ?: 0
                lines[index] = "${parts[0]}| PN: ${currentPn + pn}"
            } else {
                lines[index] = "$line | PN: $pn"
            }
        }
        return lines.joinToString("\n")
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
}
