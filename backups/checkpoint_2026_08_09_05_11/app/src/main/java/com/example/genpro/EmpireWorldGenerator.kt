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

    fun generateEmpireName(type: SettlementType): String {
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
            val type = when (Random.nextInt(100)) {
                in 0..10 -> SettlementType.CITY
                in 11..40 -> SettlementType.TOWN
                else -> SettlementType.VILLAGE
            }
            val name = generateEmpireName(type)
            val tags = when (type) {
                SettlementType.CITY -> listOf(LocationTag.CITY)
                SettlementType.TOWN -> listOf(LocationTag.CITY, LocationTag.PATH)
                SettlementType.VILLAGE -> listOf(LocationTag.PATH)
            }
            
            val settlement = SettlementGenerator.generateSettlement(null, name, type)
            val settlementJson = kotlinx.serialization.json.Json.encodeToString(SettlementInstance.serializer(), settlement)

            nodes.add(
                TravelNode(
                    id = i.toLong(),
                    name = name,
                    x = Random.nextFloat() * 1000f,
                    y = Random.nextFloat() * 1000f,
                    zone = ZoneType.CYWILIZACJA,
                    tags = tags,
                    description = "Osada typu ${type.displayName} w sercu Imperium.",
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
