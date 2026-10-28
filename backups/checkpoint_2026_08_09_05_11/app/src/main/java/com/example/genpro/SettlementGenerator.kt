package com.example.genpro

import android.content.Context
import kotlin.random.Random

object SettlementGenerator {

    fun generateSettlement(context: Context?, name: String, type: SettlementType): SettlementInstance {
        val zones = mutableListOf<SettlementZone>()
        val zoneNames = when (type) {
            SettlementType.VILLAGE -> listOf("Centrum")
            SettlementType.TOWN -> listOf("Północ", "Zachód", "Centrum", "Wschód", "Południe")
            SettlementType.CITY -> listOf(
                "Północny-Zachód", "Północ", "Północny-Wschód",
                "Zachód", "Centrum", "Wschód",
                "Południowy-Zachód", "Południe", "Południowy-Wschód"
            )
        }

        val districtMap = mutableMapOf<String, DistrictType>()

        when (type) {
            SettlementType.CITY -> {
                val corners = listOf("Północny-Zachód", "Północny-Wschód", "Południowy-Zachód", "Południowy-Wschód")
                val mandatoryCorners = mutableListOf(DistrictType.HARBOR, DistrictType.MAGIC, DistrictType.SLUMS).apply { shuffle() }

                districtMap["Centrum"] = DistrictType.RULER_SEAT

                val remainingTypes = DistrictType.entries.toMutableList()
                remainingTypes.remove(DistrictType.RULER_SEAT)
                remainingTypes.remove(DistrictType.HARBOR)
                remainingTypes.remove(DistrictType.MAGIC)
                remainingTypes.remove(DistrictType.SLUMS)
                remainingTypes.shuffle()

                val cornersToAssign = corners.toMutableList().apply { shuffle() }
                districtMap[cornersToAssign.removeAt(0)] = mandatoryCorners[0]
                districtMap[cornersToAssign.removeAt(0)] = mandatoryCorners[1]
                districtMap[cornersToAssign.removeAt(0)] = mandatoryCorners[2]
                
                if (remainingTypes.isNotEmpty()) {
                    districtMap[cornersToAssign.removeAt(0)] = remainingTypes.removeAt(0)
                }

                val edges = zoneNames.filter { it != "Centrum" && it !in corners }.toMutableList().apply { shuffle() }
                edges.forEach { edge ->
                    if (remainingTypes.isNotEmpty()) {
                        districtMap[edge] = remainingTypes.removeAt(0)
                    } else {
                        districtMap[edge] = DistrictType.RESIDENTIAL
                    }
                }
            }
            SettlementType.TOWN -> {
                val available = DistrictType.entries.filter {
                    it != DistrictType.MAGIC && it != DistrictType.HARBOR && it != DistrictType.SLUMS
                }.toMutableList()

                districtMap["Centrum"] = DistrictType.MERCHANT
                available.remove(DistrictType.MERCHANT)

                val others = zoneNames.filter { it != "Centrum" }.toMutableList().apply { shuffle() }
                
                val rulerZone = others.removeAt(0)
                districtMap[rulerZone] = DistrictType.RULER_SEAT
                available.remove(DistrictType.RULER_SEAT)

                available.shuffle()
                others.forEach { z ->
                    if (available.isNotEmpty()) {
                        districtMap[z] = available.removeAt(0)
                    } else {
                        districtMap[z] = DistrictType.RESIDENTIAL
                    }
                }
            }
            SettlementType.VILLAGE -> {
                districtMap["Centrum"] = DistrictType.RESIDENTIAL
            }
        }

        zoneNames.forEach { zName ->
            val district = districtMap[zName] ?: DistrictType.RESIDENTIAL
            
            val rank = when (type) {
                SettlementType.VILLAGE -> 1
                SettlementType.TOWN -> Random.nextInt(2, 4)
                SettlementType.CITY -> Random.nextInt(3, 6)
            }

            val innType = when {
                district == DistrictType.SLUMS -> "Speluna"
                type == SettlementType.VILLAGE -> "Karczma"
                else -> listOf("Karczma", "Tawerna", "Gospoda").random()
            }
            val inn = BuildingGenerator.generateBuilding(innType, rank)
            val specialBuilding = generateSpecialBuildingForDistrict(district, rank, type)

            zones.add(
                SettlementZone(
                    id = "zone_${zName}_${System.currentTimeMillis()}_${Random.nextInt(1000)}",
                    name = zName,
                    districtType = district,
                    inn = inn,
                    specialBuilding = specialBuilding,
                    hasDocks = district == DistrictType.HARBOR,
                    hasWell = type == SettlementType.VILLAGE || district == DistrictType.RESIDENTIAL,
                    neighbors = calculateNeighbors(zName, type)
                )
            )
        }

        val settlement = SettlementInstance(
            id = System.currentTimeMillis(),
            name = name,
            type = type,
            zones = zones
        )

        if (context != null) {
            WorldPersistence.saveSettlement(context, settlement)
        }

        return settlement
    }

    private fun generateSpecialBuildingForDistrict(district: DistrictType, rank: Int, sType: SettlementType): BuildingInstance {
        val typeName = when (district) {
            DistrictType.RULER_SEAT -> if (sType == SettlementType.CITY) "Pałac" else "Ratusz"
            DistrictType.SACRED -> when(sType) {
                SettlementType.CITY -> "Katedra"
                SettlementType.TOWN -> "Klasztor"
                else -> "Kapliczka"
            }
            DistrictType.MAGIC -> "Wieża Maga"
            DistrictType.MERCHANT -> "Sukiennice"
            DistrictType.CRAFT -> "Warsztat"
            DistrictType.RESIDENTIAL -> "Kamienica"
            DistrictType.HARBOR -> "Magazyn Portowy"
            DistrictType.MILITARY -> "Koszary"
            DistrictType.SLUMS -> "Rudera"
        }
        return BuildingGenerator.generateBuilding(typeName, rank)
    }

    private fun calculateNeighbors(current: String, type: SettlementType): List<String> {
        val cityIndices = mapOf(
            "Północny-Zachód" to 0, "Północ" to 1, "Północny-Wschód" to 2,
            "Zachód" to 3, "Centrum" to 4, "Wschód" to 5,
            "Południowy-Zachód" to 6, "Południe" to 7, "Południowy-Wschód" to 8
        )
        val indexToName = cityIndices.entries.associate { it.value to it.key }

        return when (type) {
            SettlementType.VILLAGE -> emptyList()
            SettlementType.TOWN -> {
                if (current == "Centrum") {
                    listOf("Północ", "Zachód", "Wschód", "Południe")
                } else {
                    listOf("Centrum")
                }
            }
            SettlementType.CITY -> {
                val idx = cityIndices[current] ?: return listOf("Centrum")
                val row = idx / 3
                val col = idx % 3
                val neighbors = mutableListOf<String>()

                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val nr = row + dr
                        val nc = col + dc
                        if (nr in 0..2 && nc in 0..2) {
                            indexToName[nr * 3 + nc]?.let { neighbors.add(it) }
                        }
                    }
                }
                neighbors
            }
        }
    }
}
