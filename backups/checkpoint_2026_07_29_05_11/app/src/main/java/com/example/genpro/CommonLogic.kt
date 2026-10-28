package com.example.genpro

import androidx.compose.ui.graphics.Color

data class Shop(val name: String, val sub: String, val categories: List<String>)
data class TradeItem(val name: String, val price: Int, val category: String)

enum class EncumbranceLevel(val label: String, val color: Color, val penalties: String) {
    LEKKIE("Lekkie", Color(0xFF2E7D32), "Brak kar"),
    OBCIAZONY("Obciążony", Color(0xFFFF9800), "-1 Sz, -5 Uniki"),
    PRZECIAZONY("Przeciążony", Color(0xFFF44336), "-2 Sz, -10 WW, -10 Uniki"),
    SKRAJNE("Skrajne", Color(0xFFB71C1C), "Ruch 1m, brak walki")
}

enum class RangedType {
    NONE, FIXED, SB_DEPENDENT, BOW
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
    val damageType: DamageType = DamageType.NONE
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
            line.contains("Adept", true) -> 2
            line.contains("Uczeń", true) -> 1
            else -> 0
        }
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
    val WW0kX = arrayOf(4, 2, 2, 3, 4, 3, 3)
    val WW0wo = arrayOf(1700, 760, 600, 900, 1200, 960, 800)
    val WW0rozmiar = arrayOf(4, 2, 2, 3, 4, 3, 4)

    val WW1nazwa = arrayOf("Kord", "Bagnet", "Miecz krótki", "Tasak", "Szabla", "Miecz", "Miecz wielki", "Kosa bojowa", "Tasak wielki")
    val WW1kX = arrayOf(1, 2, 3, 4, 4, 4, 5, 5, 6)
    val WW1wo = arrayOf(400, 450, 860, 680, 2800, 3400, 6100, 4100, 7300)
    val WW1rozmiar = arrayOf(1, 1, 2, 2, 3, 3, 4, 4, 4)

    val WW2nazwa = arrayOf("Maczuga", "Buzdygan", "Wekiera", "Młot bojowy", "Młot wielki", "Miażdżyciel")
    val WW2kX = arrayOf(3, 3, 3, 4, 5, 6)
    val WW2wo = arrayOf(1780, 2800, 3600, 5800, 10000, 12000)
    val WW2rozmiar = arrayOf(2, 3, 3, 3, 5, 5)

    val WW3nazwa = arrayOf("Mizerykordia", "Wbijak", "Rapier", "Trójząb", "Widły Bojowe", "Lanca")
    val WW3kX = arrayOf(3, 3, 4, 5, 6, 5)
    val WW3wo = arrayOf(840, 960, 2600, 3800, 4800, 3500)
    val WW3rozmiar = arrayOf(1, 1, 2, 4, 4, 10)

    val WW4nazwa = arrayOf("Cestus", "Tanto", "Katar", "Cinquedea", "Pata", "Wakizashi", "Kusarigama", "Katana", "Sasumata")
    val WW4kX = arrayOf(2, 3, 3, 4, 4, 5, 5, 6, 6)
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
    val US0kX = arrayOf(2, 2, 2, 2, 1, 3, 4)
    val US0range = arrayOf(10, 10, 20, 4, 15, 15, 10)
    val US0wo = arrayOf(320, 570, 280, 1250, 210, 2300, 4200)

    val US1nazwa = arrayOf("Pistolet strzałkowy", "Pistolet strzałkowy specjalny")
    val US1kX = arrayOf(3, 3)
    val US1range = arrayOf(15, 20)
    val US1wo = arrayOf(3200, 3350)

    val US2nazwa = arrayOf("Łuk krótki", "Łuk", "Łuk Długi", "Łuk Kompozytowy", "Łuk Elfi")
    val US2kX = arrayOf(3, 4, 5, 5, 5)
    val US2range = arrayOf(24, 32, 48, 48, 64)
    val US2wo = arrayOf(1300, 2400, 3500, 3100, 2800)

    val US3nazwa = arrayOf("Kusza lekka", "Kusza", "Kusza Ciężka")
    val US3kX = arrayOf(2, 3, 4)
    val US3range = arrayOf(32, 48, 64)
    val US3wo = arrayOf(2400, 4200, 6200)

    val US4nazwa = arrayOf("Granat", "Pistolet", "Rewolwera", "Rusznica", "Kacza stopa", "Działo", "Armata", "Bombarda")
    val US4kX = arrayOf(2, 2, 2, 2, 2, 5, 6, 6)
    val US4range = arrayOf(10, 15, 20, 32, 10, 100, 150, 200)
    val US4wo = arrayOf(520, 1570, 2280, 3250, 4210, 32300, 44200, 52300)

    val US5nazwa = arrayOf("Pistolet laserowy", "Pistolet plazmowy", "Karabinek laserowy")
    val US5kX = arrayOf(8, 10, 12)
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
        SpecialItem("Lutnia podróżna", "Instrumenty", 15, 400, "+5 do Uroku osobistego podczas gry"),
        SpecialItem("Flet z kości", "Instrumenty", 2, 150),
        SpecialItem("Dudy", "Instrumenty", 30, 350, "Zastraszenie +5 przy marszu"),
        SpecialItem("Bębenek obręczowy", "Instrumenty", 10, 200)
    )

    val itemsExotic = listOf(
        SpecialItem("Lampa z soczewką", "Egzotyczne", 15, 300, "Skupiony snop światła 15m"),
        SpecialItem("Lustro stalowe", "Egzotyczne", 5, 250, "Nietłukące, przydatne do zwiadu"),
        SpecialItem("Klepsydra (10 min)", "Egzotyczne", 8, 450),
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
    val ubranieN = arrayOf("Spudnica lniana", "Wełniaki", "Drewniaki", "Czułenka", "Sandały", "Chodaki", "Półbuty", "Kozaki", "Mokasyny", "Żołnierskie")
    val ubranieNWO = arrayOf(100, 180, 530, 350, 680, 720, 810, 960, 1160, 1860)
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
                    list.add(TradeItem("Skórzany Czepiec", 500, cat))
                    list.add(TradeItem("Skórzana Kamizelka", 1200, cat))
                    list.add(TradeItem("Skórzana Kurtka", 2500, cat))
                    list.add(TradeItem("Skórzana Nogawica", 550, cat))
                }
                "Pancerz ćwiekowany" -> {
                    list.add(TradeItem("Ćwiekowany Czepiec", 750, cat))
                    list.add(TradeItem("Ćwiekowana Kamizelka", 1800, cat))
                    list.add(TradeItem("Ćwiekowana Kurtka", 2700, cat))
                    list.add(TradeItem("Ćwiekowany Rękaw", 850, cat))
                }
                "Pancerz łuskowy" -> {
                    list.add(TradeItem("Hełm Łuskowy", 2500, cat))
                    list.add(TradeItem("Łuskowy Bezrękawnik", 5000, cat))
                    list.add(TradeItem("Łuskowa Kurtka", 7500, cat))
                    list.add(TradeItem("Nogawica Łuskowa", 1600, cat))
                }
                "Pancerz płytowy" -> {
                    list.add(TradeItem("Hełm Płytowy Zamknięty", 5000, cat))
                    list.add(TradeItem("Kirys", 4000, cat))
                    list.add(TradeItem("Zbroja Płytowa", 5600, cat))
                    list.add(TradeItem("Płytowy Rękaw", 2000, cat))
                    list.add(TradeItem("Nogawica Płytowa", 2100, cat))
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
        val cleanName = name.lowercase().trim()
        
        // Bronie
        WW0nazwa.forEachIndexed { i, n -> if (cleanName.contains(n.lowercase())) return WW0wo[i] }
        WW1nazwa.forEachIndexed { i, n -> if (cleanName.contains(n.lowercase())) return WW1wo[i] }
        WW2nazwa.forEachIndexed { i, n -> if (cleanName.contains(n.lowercase())) return WW2wo[i] }
        WW3nazwa.forEachIndexed { i, n -> if (cleanName.contains(n.lowercase())) return WW3wo[i] }
        WW4nazwa.forEachIndexed { i, n -> if (cleanName.contains(n.lowercase())) return WW4wo[i] }
        
        US0nazwa.forEachIndexed { i, n -> if (cleanName.contains(n.lowercase())) return US0wo[i] }
        US2nazwa.forEachIndexed { i, n -> if (cleanName.contains(n.lowercase())) return US2wo[i] }
        US3nazwa.forEachIndexed { i, n -> if (cleanName.contains(n.lowercase())) return US3wo[i] }
        US4nazwa.forEachIndexed { i, n -> if (cleanName.contains(n.lowercase())) return US4wo[i] }

        // Pancerze (Legacy/Simple matches)
        if (cleanName.contains("skórzan")) {
            return when {
                cleanName.contains("czepiec") || cleanName.contains("czapa") -> 500
                cleanName.contains("kamizelka") || cleanName.contains("kaftan") -> 1200
                cleanName.contains("kurtka") -> 2500
                cleanName.contains("nogawic") -> 550
                else -> 500
            }
        }
        if (cleanName.contains("ćwiekowan")) {
            return when {
                cleanName.contains("czepiec") || cleanName.contains("czapa") -> 750
                cleanName.contains("kamizelka") || cleanName.contains("kaftan") -> 1800
                cleanName.contains("kurtka") -> 2700
                cleanName.contains("rękaw") -> 850
                else -> 750
            }
        }
        if (cleanName.contains("kolcz")) return 4000
        if (cleanName.contains("łuskow")) return 5000
        if (cleanName.contains("płytow") || cleanName.contains("kirys")) return 5000

        // Pojemniki
        if (cleanName.contains("mała sakiewka")) return 50
        if (cleanName.contains("średnia sakiewka")) return 100
        if (cleanName.contains("duża sakiewka")) return 250
        if (cleanName.contains("worek lniany")) return 5
        if (cleanName.contains("torba podróżna")) return 150
        if (cleanName.contains("plecak usztywniany")) return 500
        if (cleanName.contains("plecak")) return 300
        
        // Zwierzęta i transport
        if (cleanName.contains("koń wierzchowy")) return 38400
        if (cleanName.contains("koń juczny")) return 19200
        if (cleanName.contains("kuc")) return 9600
        if (cleanName.contains("bryczka")) return 48000
        if (cleanName.contains("wóz kupiecki")) return 96000
        
        return 100 // Default price
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

    fun getHeroStrength(heroData: String): Int {
        val lines = heroData.lines()
        val line = lines.find { l -> 
            val clean = l.trim().replace("•", "").trim()
            clean.startsWith("Krzepa:") || clean.startsWith("K:")
        } ?: return 0
        return line.substringAfter(":").split("|")[0].split("(")[0].trim().toIntOrNull() ?: 0
    }

    fun getHeroStrengthBonus(heroData: String): Int {
        val lines = heroData.lines()
        val line = lines.find { l -> 
            val clean = l.trim().replace("•", "").trim()
            clean.startsWith("Krzepa:") || clean.startsWith("K:")
        } ?: return 0
        return if (line.contains("|")) {
            line.substringAfter("|").trim().split(" ")[0].split("(")[0].trim().toIntOrNull() ?: (getHeroStrength(heroData) / 10)
        } else {
            getHeroStrength(heroData) / 10
        }
    }

    fun getHeroToughness(heroData: String): Int {
        val lines = heroData.lines()
        val line = lines.find { l -> 
            val clean = l.trim().replace("•", "").trim()
            clean.startsWith("Odporność:") || clean.startsWith("Odp:")
        } ?: return 0
        return line.substringAfter(":").split("|")[0].split("(")[0].trim().toIntOrNull() ?: 0
    }

    fun getHeroToughnessBonus(heroData: String): Int {
        val lines = heroData.lines()
        val line = lines.find { l -> 
            val clean = l.trim().replace("•", "").trim()
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
        val cleanName = name.lowercase().replace(" 🅔", "").replace("[e]", "").trim()
        
        // Pancerze z list predefiniowanych
        val armor = (armorLight + armorMedium + armorHeavy).find { cleanName.contains(it.name.lowercase()) }
        if (armor != null) return armor.weight

        // Pancerze ze starych list (legacy)
        if (pancGłowaNazwa.any { it.trim().lowercase() == cleanName }) {
            return when {
                cleanName.contains("skórzan") -> 10
                cleanName.contains("ćwiekowan") -> 10
                cleanName.contains("kolczy") -> 30
                cleanName.contains("łuskow") -> 30
                cleanName.contains("płytow") -> 30
                else -> 10
            }
        }
        if (pancKorpusNazwa.any { it.trim().lowercase() == cleanName }) {
            return when {
                cleanName.contains("skórzan") -> 25
                cleanName.contains("ćwiekowan") -> 25
                cleanName.contains("kolcz") -> 60
                cleanName.contains("łuskow") -> 70
                cleanName.contains("kirys") || cleanName.contains("płytow") -> 50
                else -> 20
            }
        }
        if (pancRękaNazwa.any { it.trim().lowercase() == cleanName } || pancNogaNazwa.any { it.trim().lowercase() == cleanName }) {
            return when {
                cleanName.contains("skórzan") -> 10
                cleanName.contains("kolcz") -> 40
                cleanName.contains("płytow") -> 40
                else -> 15
            }
        }

        // Przedmioty specjalne i egzotyczne
        val special = (toolsSpecialist + instrumentsSpecialist + itemsExotic).find { cleanName.contains(it.name.lowercase()) }
        if (special != null) return special.weight

        // Pojemniki
        val containerEntry = pojemnikiInfo.entries.find { cleanName.contains(it.key.lowercase()) }
        if (containerEntry != null) return (containerEntry.value.first * 10).toInt()

        // Księgi
        if (cleanName.contains("księga") || cleanName.contains("dziennik") || cleanName.contains("wolumin")) {
            return 25 
        }

        // Bronie
        val isWeapon = (WW0nazwa + WW1nazwa + WW2nazwa + WW3nazwa + WW4nazwa + US0nazwa + US1nazwa + US2nazwa + US3nazwa + US4nazwa).any { 
            cleanName.contains(it.lowercase()) 
        }
        if (isWeapon) return 15

        return 5
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

    fun getHeroCharisma(heroData: String): Int {
        val lines = heroData.lines()
        val line = lines.find { l -> 
            val clean = l.trim().replace("•", "").trim()
            clean.startsWith("Charyzma:") || clean.startsWith("Cha:")
        } ?: return 0
        return line.substringAfter(":").split("|")[0].split("(")[0].trim().toIntOrNull() ?: 0
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
        if (idx != -1) {
            if (newValue > 0) {
                // Zaktualizuj liczbę, usuwając przy okazji stare formatowanie (kropki, spacje)
                lines[idx] = "$pattern $newValue"
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
                lines.add(insertPos, "$pattern $newValue")
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
        val items = heroData.lines().dropWhile { !it.startsWith("EKWIPUNEK:") }.drop(1)
            .takeWhile { it.startsWith("   •") }
            .map { it.replace("   •", "").trim() }
        
        return items.count { 
            it.contains("Koń") || it.contains("Kuc") || it.contains("Muł")
        }
    }

    fun countUsedHorses(heroData: String): Int {
        val items = heroData.lines().dropWhile { !it.startsWith("EKWIPUNEK:") }.drop(1)
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

    data class WorkResult(val title: String, val description: String, val totalWage: Int)

    fun generateWorkEvent(jobType: String, baseWage: Int, charisma: Int): WorkResult {
        val roll = (1..100).random()
        return when (jobType) {
            "Posługiwanie" -> {
                when {
                    roll > 90 -> WorkResult("Hojny gość", "Obsłużyłeś bogatego kupca, który docenił twoje maniery i zostawił suty napiwek!", baseWage + (5..15).random())
                    roll < 15 -> WorkResult("Kradzież", "Podczas tłoku ktoś wyciągnął ci sakiewkę z częścią zarobku...", (baseWage * 0.6).toInt())
                    roll < 30 -> WorkResult("Bójka", "W karczmie wybuchła bójka! Musiałeś pomagać uspokajać gości, przez co stłukło się trochę zastawy. Karczmarz potrącił ci z wypłaty.", (baseWage * 0.8).toInt())
                    else -> WorkResult("Spokojna zmiana", "Dzień minął bez większych ekscesów. Zarobiłeś uczciwie na chleb.", baseWage)
                }
            }
            "Muzyk" -> {
                val bonus = if (charisma > 50) (charisma - 50) / 5 else 0
                when {
                    roll > 85 -> WorkResult("Owacje na stojąco", "Tłum porwany twoją muzyką rzucał monety na scenę!", baseWage + (10..25).random() + bonus)
                    roll < 20 -> WorkResult("Pęknięta struna", "W połowie występu pękła ci struna. Musiałeś improwizować, ale publika nie była zachwycona.", (baseWage * 0.5).toInt())
                    else -> WorkResult("Dobry występ", "Twoja muzyka umiliła wieczór gościom. Kilku nawet postawiło ci piwo (wliczono w cenę).", baseWage + bonus)
                }
            }
            "Stajenny" -> {
                when {
                    roll > 95 -> WorkResult("Znalezisko", "Sprzątając boksy, znalazłeś w sianie zgubioną przez kogoś monetę!", baseWage + (12..20).random())
                    roll < 10 -> WorkResult("Wypadek", "Jeden z koni cię kopnął! Musiałeś wydać na opatrunki.", (baseWage * 0.3).toInt())
                    else -> WorkResult("Ciężka praca", "Konie są czyste i nakarmione. To była ciężka, fizyczna praca.", baseWage)
                }
            }
            else -> WorkResult("Koniec pracy", "Skończyłeś swoją zmianę.", baseWage)
        }
    }
}
