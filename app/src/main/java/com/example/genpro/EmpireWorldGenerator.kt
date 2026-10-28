package com.example.genpro

import kotlin.random.Random

object EmpireWorldGenerator {

    private val empirePrefixes = listOf(
        "Altd", "Midd", "Nul", "Talab", "Wolf", "Aver", "Stirl", "Wiss", "Hoch", "Ost",
        "Grun", "Hel", "Bogen", "Ubers", "Eil", "Kem", "Wit", "Schop", "Dort", "Zwan",
        "Kron", "Mari", "Nord", "Sal", "Eren", "Prag", "Bech", "Her", "Gurt", "Mund"
    )

    private val empireSuffixes = listOf(
        "orf", "enheim", "n", "heim", "enburg", "berg", "stadt", "hof", "bruck", "hafen",
        "furt", "wald", "bach", "dorf", "stein", "weiler", "kirchen", "ingen", "au", "holz",
        "see", "thal", "reuth", "markt", "gratz", "grad", "burg", "port", "land", "feld"
    )

    private val descriptors = listOf(
        "Wielki", "Mały", "Stary", "Nowy", "Czarny", "Biały", "Czerwony", "Zimny", "Górny", "Dolny"
    )

    private val khazadPrefixes = listOf("Karaz", "Karak", "Zhuf", "Barak", "Ekrund", "Gal", "Madr", "Kadrin", "Vlag", "Izor")
    private val khazadSuffixes = listOf("a-Karak", "a-Karaz", "az", "bar", "bor", "dum", "drin", "grom", "hirn", "kad", "orn", "var", "zun")

    fun generateEmpireName(type: SettlementType, race: String = "Człowiek"): String {
        if (race == "Krasnolud" || race == "Khazad") {
            val name = khazadPrefixes.random() + khazadSuffixes.random()
            return name
        }
        val name = empirePrefixes.random() + empireSuffixes.random()
        return when (type) {
            SettlementType.CITY -> {
                if (Random.nextInt(100) < 20) descriptors.random() + " " + name else name
            }
            SettlementType.TOWN -> name
            SettlementType.VILLAGE -> {
                if (Random.nextBoolean()) name else name + listOf(" Górna", " Dolna", " Mała", " Wielka").random()
            }
        }
    }

    fun generateWorldNodes(count: Int = 20): List<TravelNode> {
        val nodes = mutableListOf<TravelNode>()
        for (i in 0 until count) {
            val raceRoll = Random.nextInt(100)
            val race = if (raceRoll < 15) "Krasnolud" else "Człowiek"

            val type = when (Random.nextInt(100)) {
                in 0..10 -> SettlementType.CITY
                in 11..40 -> SettlementType.TOWN
                else -> SettlementType.VILLAGE
            }
            val name = generateEmpireName(type, race)
            val tags = when (type) {
                SettlementType.CITY -> listOf(LocationTag.CITY)
                SettlementType.TOWN -> listOf(LocationTag.CITY, LocationTag.PATH)
                SettlementType.VILLAGE -> listOf(LocationTag.PATH)
            }
            
            // Tutaj zapisujemy rasę do description lub w settlementData jako placeholder
            // CityScreen przy generowaniu z [EXTERNAL] powinien wiedzieć, jaką rasę wybrać
            // Najprościej w description: "Osada rasy $race typu ${type.displayName}..."
            
            val settlementJson = "[EXTERNAL_RACE:$race]"

            nodes.add(
                TravelNode(
                    id = i.toLong(),
                    name = name,
                    x = Random.nextFloat() * 1000f,
                    y = Random.nextFloat() * 1000f,
                    zone = ZoneType.CYWILIZACJA,
                    tags = tags,
                    description = "Osada rasy $race typu ${type.displayName} w sercu Imperium.",
                    color = when(type) {
                        SettlementType.CITY -> 0xFFFF0000u
                        SettlementType.TOWN -> 0xFFDAA520u
                        SettlementType.VILLAGE -> 0xFF2E7D32u
                    },
                    settlementData = settlementJson,
                    discovered = i < 3 // Pierwsze 3 osady są odkryte na start
                )
            )
        }
        return nodes
    }
}
