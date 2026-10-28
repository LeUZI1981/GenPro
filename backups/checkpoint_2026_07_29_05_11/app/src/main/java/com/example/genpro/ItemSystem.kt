package com.example.genpro

import androidx.compose.ui.graphics.Color

enum class DamageType(val abbreviation: String, val fullName: String, val color: Color) {
    OBUCHOWA("O", "Obuchowa", Color(0xFF90A4AE)),
    SIECZNA("S", "Sieczna", Color(0xFFE57373)),
    KLUTA("K", "Kłuta", Color(0xFF64B5F6)),
    MIOTANA("M", "Miotana", Color(0xFFAED581)),
    DYSTANSOWA("D", "Dystansowa", Color(0xFFFFB74D)),
    PROCHOWA("P", "Prochowa", Color(0xFF9575CD)),
    ENERGIA("E", "Energia", Color(0xFF4DB6AC)),
    OGIEN("Og", "Ogień", Color(0xFFFF0000)),
    ZIMNO("Z", "Zimno", Color(0xFF0000FF)),
    PIORUN("Pi", "Piorun", Color(0xFFFFFF00)),
    TRUCIZNA("Tr", "Trucizna", Color(0xFF00FF00)),
    NONE("", "Brak", Color.Gray);

    companion object {
        fun fromAbbreviation(abbr: String): DamageType {
            return values().find { it.abbreviation == abbr } ?: NONE
        }
        
        fun fromName(name: String): DamageType {
            return when {
                name.contains("[O]") -> OBUCHOWA
                name.contains("[S]") -> SIECZNA
                name.contains("[K]") -> KLUTA
                else -> NONE
            }
        }
    }
}

interface Item {
    val name: String
    val weight: Int
    val value: Int
    val category: String
}

data class Weapon(
    override val name: String,
    override val weight: Int,
    override val value: Int,
    override val category: String,
    val subCategory: String,
    val damageDie: String = "1k10",
    val damageBonus: Int,
    val extraDamage: Int = 0, // DO from the user's list
    val damageType: DamageType,
    val range: String = "-",
    val properties: List<String> = emptyList()
) : Item

data class Armor(
    override val name: String,
    override val weight: Int,
    override val value: Int,
    override val category: String = "Pancerz",
    val protection: Int,
    val locations: List<String>
) : Item

data class MiscItem(
    override val name: String,
    override val weight: Int,
    override val value: Int,
    override val category: String,
    val description: String = ""
) : Item

data class ItemInstance(
    val base: Item,
    val prefix: String? = null,
    val suffix: String? = null,
    val customName: String? = null,
    var quality: Int = 100
) {
    val displayName: String get() {
        if (customName != null) return customName
        
        val qualityPrefix = getQualityPrefix()
        val p = prefix ?: ""
        val s = suffix ?: ""
        
        return listOfNotNull(qualityPrefix.ifEmpty { null }, p.ifEmpty { null }, base.name, s.ifEmpty { null })
            .joinToString(" ")
            .trim()
    }

    private fun getQualityPrefix(): String {
        val q = quality
        val name = (prefix ?: base.name).trim()
        val gender = determineGender(name)
        
        return when {
            q <= 10 -> "" // Scrap - should be removed anyway
            q <= 20 -> when(gender) {
                Gender.MASCULINE -> "Zniszczony"
                Gender.FEMININE -> "Zniszczona"
                Gender.NEUTER, Gender.PLURAL -> "Zniszczone"
            }
            q <= 50 -> when(gender) {
                Gender.MASCULINE -> "Znoszony"
                Gender.FEMININE -> "Znoszona"
                Gender.NEUTER, Gender.PLURAL -> "Znoszone"
            }
            q <= 90 -> when(gender) {
                Gender.MASCULINE -> "Dobry"
                Gender.FEMININE -> "Dobra"
                Gender.NEUTER, Gender.PLURAL -> "Dobre"
            }
            else -> when(gender) {
                Gender.MASCULINE -> "Wyjątkowy"
                Gender.FEMININE -> "Wyjątkowa"
                Gender.NEUTER, Gender.PLURAL -> "Wyjątkowe"
            }
        }
    }

    private enum class Gender { MASCULINE, FEMININE, NEUTER, PLURAL }

    private fun determineGender(name: String): Gender {
        val firstWord = name.split(" ")[0].lowercase()
        return when {
            // Exceptions and specific plurals
            firstWord in listOf("widły", "spodnie", "rękawice", "buty", "drewniaki", "sandały", "chodaki", "półbuty", "kozaki", "mokasyny", "żołnierskie", "wełniaki") -> Gender.PLURAL
            firstWord.endsWith("a") -> Gender.FEMININE
            firstWord.endsWith("o") || firstWord.endsWith("e") -> Gender.NEUTER
            else -> Gender.MASCULINE
        }
    }
}

object ItemRegistry {
    val weapons = mutableListOf<Weapon>()
    val armors = mutableListOf<Armor>()
    val others = mutableListOf<MiscItem>()

    init {
        setupWeapons()
        setupArmor()
    }

    private fun cleanName(name: String): String {
        return name.replace(Regex("\\[.\\]"), "").trim()
    }

    private fun setupWeapons() {
        // --- BROŃ BIAŁA ---
        
        // WW0 - Broń prosta (Mixed types in names)
        val ww0names = arrayOf("Kostur", "Sztylet", "Kastet", "Kastet średni", "Kastet duży", "Łańcuch", "Pałka")
        val ww0types = arrayOf(DamageType.OBUCHOWA, DamageType.KLUTA, DamageType.OBUCHOWA, DamageType.OBUCHOWA, DamageType.OBUCHOWA, DamageType.OBUCHOWA, DamageType.OBUCHOWA)
        val ww0kX = arrayOf(4, 2, 2, 3, 4, 3, 3)
        val ww0wo = arrayOf(1700, 760, 600, 900, 1200, 960, 800)
        val ww0sz = arrayOf(4, 2, 2, 3, 4, 3, 4)
        ww0names.forEachIndexed { i, n ->
            weapons.add(Weapon(n, ww0sz[i], ww0wo[i], "Biała", "Broń prosta", "1k10", ww0kX[i], 0, ww0types[i]))
        }

        // WW1 - Bojowa Sieczna
        val ww1names = arrayOf("Kord", "Bagnet", "Miecz krótki", "Tasak", "Szabla", "Miecz", "Miecz wielki", "Kosa bojowa", "Tasak wielki")
        val ww1kX = arrayOf(1, 2, 3, 4, 4, 4, 5, 5, 6)
        val ww1wo = arrayOf(400, 450, 860, 680, 2800, 3400, 6100, 4100, 7300)
        val ww1sz = arrayOf(1, 1, 2, 2, 3, 3, 4, 4, 4)
        ww1names.forEachIndexed { i, n ->
            weapons.add(Weapon(n, ww1sz[i], ww1wo[i], "Biała", "Sieczna", "1k10", ww1kX[i], 0, DamageType.SIECZNA))
        }

        // WW2 - Bojowa Obuchowa
        val ww2names = arrayOf("Maczuga", "Buzdygan", "Wekiera", "Młot bojowy", "Młot wielki", "Miażdżyciel")
        val ww2kX = arrayOf(3, 3, 3, 4, 5, 6)
        val ww2wo = arrayOf(1780, 2800, 3600, 5800, 10000, 12000)
        val ww2sz = arrayOf(2, 3, 3, 3, 5, 5)
        ww2names.forEachIndexed { i, n ->
            weapons.add(Weapon(n, ww2sz[i], ww2wo[i], "Biała", "Obuchowa", "1k10", ww2kX[i], 0, DamageType.OBUCHOWA))
        }

        // WW3 - Bojowa Kłuta
        val ww3names = arrayOf("Mizerykordia", "Wbijak", "Rapier", "Trójząb", "Widły Bojowe", "Lanca")
        val ww3kX = arrayOf(3, 3, 4, 5, 6, 5)
        val ww3wo = arrayOf(840, 960, 2600, 3800, 4800, 3500)
        val ww3sz = arrayOf(1, 1, 2, 4, 4, 10)
        ww3names.forEachIndexed { i, n ->
            weapons.add(Weapon(n, ww3sz[i], ww3wo[i], "Biała", "Kłuta", "1k10", ww3kX[i], 0, DamageType.KLUTA))
        }

        // WW4 - Egzotyczna
        val ww4names = arrayOf("Cestus", "Tanto", "Katar", "Cinquedea", "Pata", "Wakizashi", "Kusarigama", "Katana", "Sasumata")
        val ww4kX = arrayOf(2, 3, 3, 4, 4, 5, 5, 6, 6)
        val ww4wo = arrayOf(400, 450, 860, 680, 2800, 3400, 6100, 4100, 7300)
        val ww4sz = arrayOf(2, 1, 2, 2, 3, 3, 4, 4, 5)
        ww4names.forEachIndexed { i, n ->
            val type = when(n) {
                "Cestus", "Kusarigama", "Sasumata" -> DamageType.OBUCHOWA
                "Tanto", "Katar" -> DamageType.KLUTA
                else -> DamageType.SIECZNA
            }
            weapons.add(Weapon(n, ww4sz[i], ww4wo[i], "Biała", "Broń Egzotyczna", "1k10", ww4kX[i], 0, type))
        }

        // --- BROŃ STRZELECKA ---
        
        // US0 - Miotana
        val us0names = arrayOf("Wyważony Nóż", "Wyważony Toporek", "Proca", "Bat", "Dmuchawka", "Oszczep", "Włócznia")
        val us0kX = arrayOf(2, 2, 2, 2, 1, 3, 4)
        val us0wo = arrayOf(320, 570, 280, 1250, 210, 2300, 4200)
        us0names.forEachIndexed { i, n ->
            weapons.add(Weapon(n, i+1, us0wo[i], "Strzelecka", "Miotana", "1k10", us0kX[i], 0, DamageType.MIOTANA, "6/12"))
        }

        // US1 - Pistolet strzałkowy
        val us1names = arrayOf("Pistolet strzałkowy", "Pistolet strzałkowy specjalny")
        val us1kX = arrayOf(3, 3)
        val us1wo = arrayOf(3200, 3350)
        us1names.forEachIndexed { i, n ->
            weapons.add(Weapon(n, 2, us1wo[i], "Strzelecka", "Pistolet strzałkowy", "1k10", us1kX[i], 0, DamageType.DYSTANSOWA, "10/20"))
        }

        // US2 - Łuki
        val us2names = arrayOf("Łuk krótki", "Łuk", "Łuk Długi", "Łuk Kompozytowy", "Łuk Elfi")
        val us2kX = arrayOf(3, 4, 5, 5, 5)
        val us2wo = arrayOf(1300, 2400, 3500, 3100, 2800)
        us2names.forEachIndexed { i, n ->
            weapons.add(Weapon(n, 3, us2wo[i], "Strzelecka", "Łuk", "1k10", us2kX[i], 0, DamageType.DYSTANSOWA, "24/48"))
        }

        // US3 - Kusze
        val us3names = arrayOf("Kusza lekka", "Kusza", "Kusza Ciężka")
        val us3kX = arrayOf(2, 3, 4)
        val us3DO = arrayOf(2, 3, 4)
        val us3wo = arrayOf(2400, 4200, 6200)
        us3names.forEachIndexed { i, n ->
            weapons.add(Weapon(n, 4, us3wo[i], "Strzelecka", "Kusza", "1k10", us3kX[i], us3DO[i], DamageType.DYSTANSOWA, "32/64"))
        }

        // US4 - Prochowa
        val us4names = arrayOf("Granat", "Pistolet", "Rewolwera", "Rusznica", "Kacza stopa", "Działo", "Armata", "Bombarda")
        val us4kX = arrayOf(2, 2, 2, 2, 2, 5, 6, 6)
        val us4DO = arrayOf(2, 2, 2, 2, 2, 3, 3, 3)
        val us4wo = arrayOf(520, 1570, 2280, 3250, 4210, 32300, 44200, 52300)
        us4names.forEachIndexed { i, n ->
            weapons.add(Weapon(n, 5, us4wo[i], "Strzelecka", "Prochowa", "1k10", us4kX[i], us4DO[i], DamageType.PROCHOWA, "12/24"))
        }
    }

    private fun setupArmor() {
        val locs = listOf("Głowa", "Korpus", "Prawa Ręka", "Lewa Ręka", "Prawa Noga", "Lewa Noga")
        val allBody = locs
        
        // Data from UiUtils.kt converted to Armor objects
        // Głowa
        val headNames = arrayOf("Skórzany Czepiec", "Ćwiekowany Czepiec", "Kolczy Czepiec", "Hełm Łuskowy", "Hełm Płytowy Otwarty", "Hełm Płytowy Zamknięty")
        val headWO = arrayOf(500, 750, 1500, 2500, 4000, 5000)
        headNames.forEachIndexed { i, n -> armors.add(Armor(n, 1, headWO[i], protection = i + 1, locations = listOf("Głowa"))) }

        // Korpus
        val bodyNames = arrayOf("Skórzana Kamizelka", "Skórzana Kurtka", "Ćwiekowana Kamizelka", "Ćwiekowana Kurtka", "Kolcza Kamizelka", "Kolcza Kurtka", "Kolcza Opończa", "Łuskowy Bezrękawnik", "Łuskowa Kurtka", "Kirys", "Zbroja Płytowa")
        val bodyWO = arrayOf(1200, 2500, 1800, 2700, 2300, 4000, 6000, 5000, 7500, 4000, 5600)
        bodyNames.forEachIndexed { i, n -> 
            val loc = if (n.contains("Kurtka")) listOf("Korpus", "Prawa Ręka", "Lewa Ręka") else listOf("Korpus")
            armors.add(Armor(n, 3, bodyWO[i], protection = (i/3) + 1, locations = loc)) 
        }

        // Ręce & Nogi can be added similarly...
    }
}
