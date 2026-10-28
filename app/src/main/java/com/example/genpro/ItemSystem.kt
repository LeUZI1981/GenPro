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
            return entries.find { it.abbreviation == abbr } ?: NONE
        }
        
        fun fromName(name: String): DamageType {
            return when {
                name.contains("[O]") || name.contains("(O)") -> OBUCHOWA
                name.contains("[S]") || name.contains("(S)") -> SIECZNA
                name.contains("[K]") || name.contains("(K)") -> KLUTA
                else -> NONE
            }
        }
    }
}

enum class RangedType {
    NONE, FIXED, SB_DEPENDENT, BOW
}

enum class WeaponSize(val displayName: String, val hitModifier: Int) {
    BARDZO_MALY("Bardzo mały", 20),
    MALY("Mały", 10),
    SREDNI("Średni", 0),
    DUZY("Duży", -10),
    BARDZO_DUZY("Bardzo duży", -20),
    WIELKI("Wielki", -30);

    companion object {
        fun fromInt(value: Int): WeaponSize {
            return when (value) {
                1 -> BARDZO_MALY
                2 -> MALY
                3 -> SREDNI
                4 -> DUZY
                5 -> BARDZO_DUZY
                6 -> WIELKI
                else -> SREDNI
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
    val properties: List<String> = emptyList(),
    val size: WeaponSize = WeaponSize.SREDNI,
    val rangedType: RangedType = RangedType.NONE,
    val weaponStrength: Int = 0,
    val reloadActions: Int = 0
) : Item

data class Armor(
    override val name: String,
    override val weight: Int,
    override val value: Int,
    override val category: String = "Pancerz",
    val ppS: Int,
    val ppO: Int,
    val ppK: Int,
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

    fun findWeapon(name: String): Weapon? {
        val clean = name.lowercase().trim()
        return weapons.find { clean.contains(it.name.lowercase()) }
    }

    fun findArmor(name: String): Armor? {
        val clean = name.lowercase().trim()
        return armors.find { clean.contains(it.name.lowercase()) }
    }

    fun findItem(name: String): Item? {
        val clean = name.lowercase().trim()
        return weapons.find { clean.contains(it.name.lowercase()) } 
            ?: armors.find { clean.contains(it.name.lowercase()) }
            ?: others.find { clean.contains(it.name.lowercase()) }
    }

    fun getPrice(name: String): Int {
        return findItem(name)?.value ?: 100
    }

    fun getWeight(name: String): Int {
        return findItem(name)?.weight ?: 5
    }

    init {
        setupWeapons()
        setupArmor()
        setupMiscItems()
    }

    private fun cleanName(name: String): String {
        return name.replace(Regex("\\[.\\]"), "").trim()
    }

    private fun setupWeapons() {
        // --- BROŃ BIAŁA ---
        
        // WW0 - Broń prosta
        weapons.add(Weapon("Kostur", 4, 1700, "Biała", "Broń prosta", "1k10", 8, 0, DamageType.OBUCHOWA, properties = listOf("Defensywny")))
        weapons.add(Weapon("Sztylet", 2, 760, "Biała", "Broń prosta", "1k10", 4, 0, DamageType.KLUTA, size = WeaponSize.BARDZO_MALY))
        weapons.add(Weapon("Kastet", 2, 600, "Biała", "Broń prosta", "1k10", 4, 0, DamageType.OBUCHOWA, properties = listOf("Ogłuszający"), size = WeaponSize.BARDZO_MALY))
        weapons.add(Weapon("Kastet średni", 3, 900, "Biała", "Broń prosta", "1k10", 6, 0, DamageType.OBUCHOWA, properties = listOf("Ogłuszający"), size = WeaponSize.MALY))
        weapons.add(Weapon("Kastet duży", 4, 1200, "Biała", "Broń prosta", "1k10", 8, 0, DamageType.OBUCHOWA, properties = listOf("Ogłuszający"), size = WeaponSize.SREDNI))
        weapons.add(Weapon("Łańcuch", 3, 960, "Biała", "Broń prosta", "1k10", 6, 0, DamageType.OBUCHOWA))
        weapons.add(Weapon("Pałka", 4, 800, "Biała", "Broń prosta", "1k10", 6, 0, DamageType.OBUCHOWA, properties = listOf("Ogłuszający"), size = WeaponSize.MALY))

        // WW1 - Bojowa Sieczna
        weapons.add(Weapon("Kord", 1, 400, "Biała", "Sieczna", "1k10", 4, 0, DamageType.SIECZNA, size = WeaponSize.BARDZO_MALY))
        weapons.add(Weapon("Bagnet", 1, 450, "Biała", "Sieczna", "1k10", 4, 0, DamageType.SIECZNA, size = WeaponSize.BARDZO_MALY))
        weapons.add(Weapon("Miecz krótki", 2, 860, "Biała", "Sieczna", "1k10", 6, 0, DamageType.SIECZNA, size = WeaponSize.MALY))
        weapons.add(Weapon("Tasak", 2, 680, "Biała", "Sieczna", "1k10", 4, 0, DamageType.SIECZNA, size = WeaponSize.MALY))
        weapons.add(Weapon("Szabla", 3, 2800, "Biała", "Sieczna", "1k10", 8, 0, DamageType.SIECZNA, properties = listOf("Szybki")))
        weapons.add(Weapon("Miecz", 3, 3400, "Biała", "Sieczna", "1k10", 8, 0, DamageType.SIECZNA))
        weapons.add(Weapon("Miecz wielki", 4, 6100, "Biała", "Sieczna", "1k10", 10, 0, DamageType.SIECZNA, properties = listOf("Druzgoczący", "Wolny"), size = WeaponSize.DUZY))
        weapons.add(Weapon("Kosa bojowa", 4, 4100, "Biała", "Sieczna", "1k10", 10, 0, DamageType.SIECZNA, size = WeaponSize.DUZY))
        weapons.add(Weapon("Tasak wielki", 4, 7300, "Biała", "Sieczna", "1k10", 12, 0, DamageType.SIECZNA, size = WeaponSize.DUZY))

        // WW2 - Bojowa Obuchowa
        weapons.add(Weapon("Maczuga", 2, 1780, "Biała", "Obuchowa", "1k10", 6, 0, DamageType.OBUCHOWA, properties = listOf("Ogłuszający")))
        weapons.add(Weapon("Buzdygan", 3, 2800, "Biała", "Obuchowa", "1k10", 6, 0, DamageType.OBUCHOWA, properties = listOf("Ogłuszający")))
        weapons.add(Weapon("Wekiera", 3, 3600, "Biała", "Obuchowa", "1k10", 6, 0, DamageType.OBUCHOWA, properties = listOf("Ogłuszający")))
        weapons.add(Weapon("Młot bojowy", 3, 5800, "Biała", "Obuchowa", "1k10", 8, 0, DamageType.OBUCHOWA, properties = listOf("Ogłuszający")))
        weapons.add(Weapon("Młot wielki", 5, 10000, "Biała", "Obuchowa", "1k10", 10, 0, DamageType.OBUCHOWA, properties = listOf("Ogłuszający", "Druzgoczący", "Wolny"), size = WeaponSize.DUZY))
        weapons.add(Weapon("Miażdżyciel", 5, 12000, "Biała", "Obuchowa", "1k10", 12, 0, DamageType.OBUCHOWA, properties = listOf("Ogłuszający", "Druzgoczący", "Wolny"), size = WeaponSize.DUZY))

        // WW3 - Bojowa Kłuta
        weapons.add(Weapon("Mizerykordia", 1, 840, "Biała", "Kłuta", "1k10", 6, 0, DamageType.KLUTA, properties = listOf("Przebijający"), size = WeaponSize.BARDZO_MALY))
        weapons.add(Weapon("Wbijak", 1, 960, "Biała", "Kłuta", "1k10", 6, 0, DamageType.KLUTA, size = WeaponSize.BARDZO_MALY))
        weapons.add(Weapon("Rapier", 2, 2600, "Biała", "Kłuta", "1k10", 8, 0, DamageType.KLUTA, properties = listOf("Szybki")))
        weapons.add(Weapon("Trójząb", 4, 3800, "Biała", "Kłuta", "1k10", 10, 0, DamageType.KLUTA, size = WeaponSize.DUZY))
        weapons.add(Weapon("Widły Bojowe", 4, 4800, "Biała", "Kłuta", "1k10", 10, 0, DamageType.KLUTA, size = WeaponSize.DUZY))
        weapons.add(Weapon("Lanca", 10, 3500, "Biała", "Kłuta", "1k10", 10, 0, DamageType.KLUTA, size = WeaponSize.BARDZO_DUZY))

        // WW4 - Egzotyczna
        weapons.add(Weapon("Cestus", 2, 400, "Biała", "Broń Egzotyczna", "1k10", 4, 0, DamageType.OBUCHOWA, size = WeaponSize.MALY))
        weapons.add(Weapon("Tanto", 1, 450, "Biała", "Broń Egzotyczna", "1k10", 6, 0, DamageType.KLUTA, size = WeaponSize.BARDZO_MALY))
        weapons.add(Weapon("Katar", 2, 860, "Biała", "Broń Egzotyczna", "1k10", 6, 0, DamageType.KLUTA, size = WeaponSize.MALY))
        weapons.add(Weapon("Cinquedea", 2, 680, "Biała", "Broń Egzotyczna", "1k10", 8, 0, DamageType.SIECZNA, size = WeaponSize.MALY))
        weapons.add(Weapon("Pata", 3, 2800, "Biała", "Broń Egzotyczna", "1k10", 8, 0, DamageType.SIECZNA))
        weapons.add(Weapon("Wakizashi", 3, 3400, "Biała", "Broń Egzotyczna", "1k10", 10, 0, DamageType.SIECZNA))
        weapons.add(Weapon("Kusarigama", 4, 6100, "Biała", "Broń Egzotyczna", "1k10", 10, 0, DamageType.OBUCHOWA, size = WeaponSize.DUZY))
        weapons.add(Weapon("Katana", 4, 4100, "Biała", "Broń Egzotyczna", "1k10", 12, 0, DamageType.SIECZNA, size = WeaponSize.DUZY))
        weapons.add(Weapon("Sasumata", 5, 7300, "Biała", "Broń Egzotyczna", "1k10", 12, 0, DamageType.OBUCHOWA, size = WeaponSize.DUZY))

        // --- BROŃ STRZELECKA ---
        
        // US0 - Miotana
        weapons.add(Weapon("Wyważony Nóż", 1, 320, "Strzelecka", "Miotana", "1k10", 4, 0, DamageType.MIOTANA, "6/12", rangedType = RangedType.SB_DEPENDENT))
        weapons.add(Weapon("Wyważony Toporek", 2, 570, "Strzelecka", "Miotana", "1k10", 4, 0, DamageType.MIOTANA, "6/12", rangedType = RangedType.SB_DEPENDENT))
        weapons.add(Weapon("Proca", 1, 280, "Strzelecka", "Miotana", "1k10", 4, 0, DamageType.MIOTANA, "12/24", rangedType = RangedType.SB_DEPENDENT, reloadActions = 1))
        weapons.add(Weapon("Bat", 4, 1250, "Strzelecka", "Miotana", "1k10", 6, 0, DamageType.MIOTANA, "4/-", rangedType = RangedType.SB_DEPENDENT))
        weapons.add(Weapon("Dmuchawka", 2, 210, "Strzelecka", "Miotana", "1k10", 4, 0, DamageType.MIOTANA, "10/20", rangedType = RangedType.FIXED, weaponStrength = 1, reloadActions = 1))
        weapons.add(Weapon("Oszczep", 2, 2300, "Strzelecka", "Miotana", "1k10", 2, 0, DamageType.MIOTANA, "15/30", rangedType = RangedType.SB_DEPENDENT))
        weapons.add(Weapon("Włócznia", 4, 4200, "Strzelecka", "Miotana", "1k10", 6, 0, DamageType.MIOTANA, "10/20", rangedType = RangedType.SB_DEPENDENT))

        // US1 - Pistolet strzałkowy
        weapons.add(Weapon("Pistolet strzałkowy", 2, 3200, "Strzelecka", "Pistolet strzałkowy", "1k10", 4, 0, DamageType.DYSTANSOWA, "10/20", rangedType = RangedType.FIXED, weaponStrength = 4, reloadActions = 2))
        weapons.add(Weapon("Pistolet strzałkowy specjalny", 2, 3350, "Strzelecka", "Pistolet strzałkowy", "1k10", 4, 0, DamageType.DYSTANSOWA, "15/30", rangedType = RangedType.FIXED, weaponStrength = 4, reloadActions = 2))

        // US2 - Łuki
        weapons.add(Weapon("Łuk krótki", 3, 1300, "Strzelecka", "Łuk", "1k10", 6, 0, DamageType.DYSTANSOWA, "24/48", rangedType = RangedType.BOW, weaponStrength = 3, reloadActions = 1))
        weapons.add(Weapon("Łuk", 3, 2400, "Strzelecka", "Łuk", "1k10", 8, 0, DamageType.DYSTANSOWA, "32/64", rangedType = RangedType.BOW, weaponStrength = 3, reloadActions = 1))
        weapons.add(Weapon("Łuk Długi", 4, 3500, "Strzelecka", "Łuk", "1k10", 10, 0, DamageType.DYSTANSOWA, "48/96", rangedType = RangedType.BOW, weaponStrength = 4, reloadActions = 1))
        weapons.add(Weapon("Łuk Kompozytowy", 4, 3100, "Strzelecka", "Łuk", "1k10", 12, 0, DamageType.DYSTANSOWA, "48/96", rangedType = RangedType.BOW, weaponStrength = 4, reloadActions = 1))
        weapons.add(Weapon("Łuk Elfi", 3, 2800, "Strzelecka", "Łuk", "1k10", 12, 0, DamageType.DYSTANSOWA, "64/128", rangedType = RangedType.BOW, weaponStrength = 4, reloadActions = 1))

        // US3 - Kusze
        weapons.add(Weapon("Kusza lekka", 4, 2400, "Strzelecka", "Kusza", "1k10", 4, 2, DamageType.DYSTANSOWA, "32/64", rangedType = RangedType.FIXED, weaponStrength = 4, reloadActions = 2))
        weapons.add(Weapon("Kusza", 4, 4200, "Strzelecka", "Kusza", "1k10", 6, 3, DamageType.DYSTANSOWA, "48/96", rangedType = RangedType.FIXED, weaponStrength = 5, reloadActions = 2))
        weapons.add(Weapon("Kusza Ciężka", 6, 6200, "Strzelecka", "Kusza", "1k10", 8, 4, DamageType.DYSTANSOWA, "64/128", rangedType = RangedType.FIXED, weaponStrength = 6, reloadActions = 3))

        // US4 - Prochowa
        weapons.add(Weapon("Granat", 1, 520, "Strzelecka", "Prochowa", "1k10", 8, 2, DamageType.PROCHOWA, "10/20", rangedType = RangedType.FIXED, weaponStrength = 5))
        weapons.add(Weapon("Pistolet", 2, 1570, "Strzelecka", "Prochowa", "1k10", 6, 2, DamageType.PROCHOWA, "12/24", rangedType = RangedType.FIXED, weaponStrength = 4, reloadActions = 3, properties = listOf("Druzgoczący", "Przebijający pancerz")))
        weapons.add(Weapon("Rewolwera", 2, 2280, "Strzelecka", "Prochowa", "1k10", 8, 2, DamageType.PROCHOWA, "15/30", rangedType = RangedType.FIXED, weaponStrength = 4, reloadActions = 3))
        weapons.add(Weapon("Rusznica", 5, 3250, "Strzelecka", "Prochowa", "1k10", 10, 2, DamageType.PROCHOWA, "32/64", rangedType = RangedType.FIXED, weaponStrength = 4, reloadActions = 4))
        weapons.add(Weapon("Kacza stopa", 4, 4210, "Strzelecka", "Prochowa", "1k10", 12, 2, DamageType.PROCHOWA, "10/20", rangedType = RangedType.FIXED, weaponStrength = 4, reloadActions = 4))

        // US5 - Futurystyczna
        weapons.add(Weapon("Pistolet laserowy", 5, 520, "Strzelecka", "Futurystyczna", "1k10", 10, 0, DamageType.ENERGIA, "40/80", rangedType = RangedType.FIXED, weaponStrength = 4, reloadActions = 1))
        weapons.add(Weapon("Pistolet plazmowy", 5, 570, "Strzelecka", "Futurystyczna", "1k10", 12, 0, DamageType.ENERGIA, "20/40", rangedType = RangedType.FIXED, weaponStrength = 5, reloadActions = 2))
        weapons.add(Weapon("Karabinek laserowy", 32, 3200, "Strzelecka", "Futurystyczna", "1k10", 20, 0, DamageType.ENERGIA, "100/200", rangedType = RangedType.FIXED, weaponStrength = 6, reloadActions = 1))
    }

    private fun setupArmor() {
        // LEKKIE
        armors.add(Armor("Skórzany czepiec", 10, 500, ppS = 1, ppO = 1, ppK = 0, locations = listOf("Głowa")))
        armors.add(Armor("Skórzana czapa", 10, 500, ppS = 1, ppO = 1, ppK = 0, locations = listOf("Głowa")))
        armors.add(Armor("Skórzana kamizelka", 20, 1200, ppS = 1, ppO = 1, ppK = 0, locations = listOf("Korpus")))
        armors.add(Armor("Skórzany kaftan", 25, 1200, ppS = 1, ppO = 1, ppK = 0, locations = listOf("Korpus", "Prawa Ręka", "Lewa Ręka")))
        armors.add(Armor("Skórzana kurtka", 25, 2500, ppS = 1, ppO = 1, ppK = 0, locations = listOf("Korpus", "Prawa Ręka", "Lewa Ręka")))
        armors.add(Armor("Skórzane nogawice", 15, 550, ppS = 1, ppO = 1, ppK = 0, locations = listOf("Nogi")))
        armors.add(Armor("Skórzane spodnie", 15, 550, ppS = 1, ppO = 1, ppK = 0, locations = listOf("Nogi")))
        armors.add(Armor("Skórzane rękawice", 10, 220, ppS = 1, ppO = 1, ppK = 0, locations = listOf("Ramiona")))
        armors.add(Armor("Skórzane nagolennice", 15, 550, ppS = 1, ppO = 1, ppK = 0, locations = listOf("Nogi")))
        armors.add(Armor("Ćwiekowana czapa", 12, 750, ppS = 2, ppO = 1, ppK = 1, locations = listOf("Głowa")))
        armors.add(Armor("Ćwiekowany kaftan", 30, 2700, ppS = 2, ppO = 1, ppK = 1, locations = listOf("Korpus", "Prawa Ręka", "Lewa Ręka")))
        armors.add(Armor("Ćwiekowane nogawice", 20, 900, ppS = 2, ppO = 1, ppK = 1, locations = listOf("Nogi")))

        // ŚREDNIE
        armors.add(Armor("Ćwiekowany czepiec", 15, 750, ppS = 2, ppO = 1, ppK = 1, locations = listOf("Głowa")))
        armors.add(Armor("Ćwiekowana kurtka", 35, 2700, ppS = 2, ppO = 1, ppK = 1, locations = listOf("Korpus", "Prawa Ręka", "Lewa Ręka")))
        armors.add(Armor("Kolczy czepiec", 25, 1500, ppS = 2, ppO = 2, ppK = 1, locations = listOf("Głowa")))
        armors.add(Armor("Kolcza czapa", 25, 1500, ppS = 2, ppO = 2, ppK = 1, locations = listOf("Głowa")))
        armors.add(Armor("Kolcza koszula", 45, 4000, ppS = 2, ppO = 2, ppK = 1, locations = listOf("Korpus", "Prawa Ręka", "Lewa Ręka")))
        armors.add(Armor("Kolczuga", 45, 6000, ppS = 2, ppO = 2, ppK = 1, locations = listOf("Korpus", "Prawa Ręka", "Lewa Ręka")))
        armors.add(Armor("Kolcza kamizelka", 35, 2300, ppS = 2, ppO = 2, ppK = 1, locations = listOf("Korpus")))
        armors.add(Armor("Kolcze rękawy", 25, 1100, ppS = 2, ppO = 2, ppK = 1, locations = listOf("Ramiona")))
        armors.add(Armor("Kolcza nogawica", 30, 1200, ppS = 2, ppO = 2, ppK = 1, locations = listOf("Nogi")))
        armors.add(Armor("Kolcze nogawice", 30, 1200, ppS = 2, ppO = 2, ppK = 1, locations = listOf("Nogi")))
        armors.add(Armor("Łuskowa czapa", 25, 2500, ppS = 2, ppO = 1, ppK = 2, locations = listOf("Głowa")))
        armors.add(Armor("Łuskowa kurtka", 50, 7500, ppS = 2, ppO = 1, ppK = 2, locations = listOf("Korpus", "Prawa Ręka", "Lewa Ręka")))
        armors.add(Armor("Łuskowe nogawice", 35, 1600, ppS = 2, ppO = 1, ppK = 2, locations = listOf("Nogi")))

        // CIĘŻKIE
        armors.add(Armor("Hełm łuskowy", 30, 2500, ppS = 3, ppO = 2, ppK = 3, locations = listOf("Głowa")))
        armors.add(Armor("Kirys łuskowy", 60, 5000, ppS = 3, ppO = 2, ppK = 3, locations = listOf("Korpus")))
        armors.add(Armor("Łuskowe rękawy", 40, 1500, ppS = 3, ppO = 2, ppK = 3, locations = listOf("Ramiona")))
        armors.add(Armor("Hełm płytowy", 40, 5000, ppS = 4, ppO = 3, ppK = 4, locations = listOf("Głowa")))
        armors.add(Armor("Kirys płytowy", 80, 4000, ppS = 4, ppO = 3, ppK = 4, locations = listOf("Korpus")))
        armors.add(Armor("Płytowe naramienniki", 50, 2000, ppS = 4, ppO = 3, ppK = 4, locations = listOf("Ramiona")))
        armors.add(Armor("Płytowe nagolenniki", 50, 2100, ppS = 4, ppO = 3, ppK = 4, locations = listOf("Nogi")))
        armors.add(Armor("Płytowe rękawice", 25, 2000, ppS = 4, ppO = 3, ppK = 4, locations = listOf("Ramiona")))
        armors.add(Armor("Kirys stalowy", 80, 4000, ppS = 4, ppO = 3, ppK = 4, locations = listOf("Korpus")))
    }

    private fun setupMiscItems() {
        others.add(MiscItem("Mała sakiewka", 1, 50, "Pojemniki"))
        others.add(MiscItem("Średnia sakiewka", 2, 100, "Pojemniki"))
        others.add(MiscItem("Duża sakiewka", 3, 250, "Pojemniki"))
        others.add(MiscItem("Worek lniany", 5, 5, "Pojemniki"))
        others.add(MiscItem("Torba podróżna", 10, 150, "Pojemniki"))
        others.add(MiscItem("Plecak", 15, 300, "Pojemniki"))
        others.add(MiscItem("Plecak usztywniany", 20, 500, "Pojemniki"))
        others.add(MiscItem("Tykwa", 2, 20, "Pojemniki"))
        others.add(MiscItem("Bukłak mały", 5, 50, "Pojemniki"))
        others.add(MiscItem("Bukłak duży", 10, 100, "Pojemniki"))
        others.add(MiscItem("Antałek", 20, 250, "Pojemniki"))
        
        others.add(MiscItem("Koń wierzchowy", 0, 38400, "Zwierzęta"))
        others.add(MiscItem("Koń juczny", 0, 19200, "Zwierzęta"))
        others.add(MiscItem("Kuc", 0, 9600, "Zwierzęta"))
        others.add(MiscItem("Muł roboczy", 0, 14400, "Zwierzęta"))

        others.add(MiscItem("Lichy posiłek", 0, 15, "Posiłki"))
        others.add(MiscItem("Zwyczajny posiłek", 0, 30, "Posiłki"))
        others.add(MiscItem("Dostatni posiłek", 0, 60, "Posiłki"))
        others.add(MiscItem("Uczta", 0, 150, "Posiłki"))

        others.add(MiscItem("Piwo (kufel)", 0, 5, "Trunki"))
        others.add(MiscItem("Wino (kielich)", 0, 15, "Trunki"))
        others.add(MiscItem("Gorzałka (kieliszek)", 0, 10, "Trunki"))
    }

}
