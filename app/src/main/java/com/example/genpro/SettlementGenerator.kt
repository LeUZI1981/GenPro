package com.example.genpro

import android.content.Context
import kotlin.random.Random

object SettlementGenerator {

    private fun canAddBuildingToDistrict(buildingName: String, usedInDistrict: Set<String>): Boolean {
        return !usedInDistrict.contains(buildingName)
    }

    fun generateSettlement(
        context: Context?,
        name: String,
        type: SettlementType,
        race: String = "Człowiek",
        gateDirections: Set<String> = setOf("Północ", "Wschód", "Południe", "Zachód")
    ): SettlementInstance {
        val zones = mutableListOf<SettlementZone>()
        val zoneNames = when (type) {
            SettlementType.VILLAGE -> gateDirections.toList() + "Centrum"
            SettlementType.TOWN -> gateDirections.toList() + "Centrum"
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
                remainingTypes.remove(DistrictType.ECONOMIC)
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
                val villageGateZones = zoneNames.filter { it != "Centrum" }.shuffled()
                villageGateZones.getOrNull(0)?.let { districtMap[it] = DistrictType.RESIDENTIAL }
                villageGateZones.getOrNull(1)?.let { districtMap[it] = DistrictType.ECONOMIC }
            }
        }

        val usedNames = mutableSetOf<String>()

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
            val inn = BuildingGenerator.generateBuilding(innType, rank, usedComponents = usedNames)
            
            val extraBuildings = mutableListOf<BuildingInstance>()
            var primarySpecial: BuildingInstance? = null
            val districtBuildingNames = mutableSetOf(inn.name) // Tracker dla tej dzielnicy

            when (type) {
                SettlementType.CITY -> {
                    if (district == DistrictType.CRAFT) {
                        primarySpecial = BuildingGenerator.generateBuilding("Kowal", rank, usedComponents = usedNames, race = race)
                        extraBuildings.add(BuildingGenerator.generateBuilding("Płatnerz", rank, usedComponents = usedNames, race = race))
                        extraBuildings.add(BuildingGenerator.generateBuilding("Rymarz", rank, usedComponents = usedNames, race = race))
                    } else if (district == DistrictType.SLUMS) {
                        primarySpecial = generateSpecialBuildingForDistrict(district, rank, type, usedNames, race)
                        
                        // Motel 2* - 60% szans na odkrycie
                        val motel = BuildingGenerator.generateBuilding("Motel", 2, usedComponents = usedNames, race = race)
                        extraBuildings.add(motel.copy(discovered = Random.nextDouble() < 0.6))
                        
                        // Zamtuz - 60% szans na odkrycie
                        val zamtuz = BuildingGenerator.generateBuilding("Zamtuz", rank, usedComponents = usedNames, race = race)
                        extraBuildings.add(zamtuz.copy(discovered = Random.nextDouble() < 0.6))
                        
                        // Gildia złodzieji - 10% szans na odkrycie
                        val gildia = BuildingGenerator.generateBuilding("Gildia złodzieji", rank, usedComponents = usedNames, race = race)
                        extraBuildings.add(gildia.copy(discovered = Random.nextDouble() < 0.1))
                    } else if (district == DistrictType.MAGIC) {
                        primarySpecial = generateSpecialBuildingForDistrict(district, rank, type, usedNames, race)
                        
                        // Dodatkowo: Szkoła Magii (40% szans na odkrycie)
                        val szkolaMagii = BuildingGenerator.generateBuilding("Szkoła Magii", rank, usedComponents = usedNames, race = race)
                        extraBuildings.add(szkolaMagii.copy(discovered = Random.nextDouble() < 0.4))

                        // Portal miejski - odkrycie w UI (widoczność 70% mag/kapłan, 20% inni)
                        val portal = BuildingGenerator.generateBuilding("Portal miejski", rank, usedComponents = usedNames, race = race)
                        extraBuildings.add(portal.copy(discovered = false))
                        
                        // Biblioteka - 50% szans
                        if (Random.nextBoolean()) {
                            extraBuildings.add(BuildingGenerator.generateBuilding("Biblioteka", rank, usedComponents = usedNames, race = race))
                        }
                        
                        extraBuildings.add(BuildingGenerator.generateBuilding("Hotel", 3, usedComponents = usedNames, race = race))
                        extraBuildings.add(BuildingGenerator.generateBuilding("Karczma", 3, usedComponents = usedNames, race = race))
                    } else if (district == DistrictType.MILITARY) {
                        primarySpecial = generateSpecialBuildingForDistrict(district, rank, type, usedNames, race)
                        
                        // Dodatkowo: Akademia Wojskowa (40% szans na odkrycie)
                        val akademia = BuildingGenerator.generateBuilding("Akademia Wojskowa", rank, usedComponents = usedNames, race = race)
                        extraBuildings.add(akademia.copy(discovered = Random.nextDouble() < 0.4))

                        extraBuildings.add(BuildingGenerator.generateBuilding("Karczma", 3, usedComponents = usedNames, race = race))
                        extraBuildings.add(BuildingGenerator.generateBuilding("Karczma", 2, usedComponents = usedNames, race = race))
                    } else if (district == DistrictType.SACRED) {
                        primarySpecial = generateSpecialBuildingForDistrict(district, rank, type, usedNames, race)

                        // Dodatkowo: Uniwersytet (40% szans na odkrycie)
                        val uniwersytet = BuildingGenerator.generateBuilding("Uniwersytet", rank, usedComponents = usedNames, race = race)
                        extraBuildings.add(uniwersytet.copy(discovered = Random.nextDouble() < 0.4))

                        extraBuildings.add(BuildingGenerator.generateBuilding("Hotel", 3, usedComponents = usedNames, race = race))
                        extraBuildings.add(BuildingGenerator.generateBuilding("Karczma", 2, usedComponents = usedNames, race = race))
                    } else {
                        primarySpecial = generateSpecialBuildingForDistrict(district, rank, type, usedNames, race)
                        // Inne dzielnice zawierają po 2 hotele i po 2 karczmy na poziomie 3 i 2 gwiazdek (jeśli to miasto)
                        extraBuildings.add(BuildingGenerator.generateBuilding("Hotel", 3, usedComponents = usedNames, race = race))
                        extraBuildings.add(BuildingGenerator.generateBuilding("Hotel", 2, usedComponents = usedNames, race = race))
                        extraBuildings.add(BuildingGenerator.generateBuilding("Karczma", 3, usedComponents = usedNames, race = race))
                        extraBuildings.add(BuildingGenerator.generateBuilding("Karczma", 2, usedComponents = usedNames, race = race))
                        
                        // Biblioteka w Centrum
                        if (zName == "Centrum") {
                            extraBuildings.add(BuildingGenerator.generateBuilding("Biblioteka", rank, usedComponents = usedNames, race = race))
                        }
                        
                        // Fryzjer - 60% szans na odkrycie w dzielnicach RESIDENTIAL i Centrum
                        if (district == DistrictType.RESIDENTIAL || district == DistrictType.RULER_SEAT) {
                            val fryzjer = BuildingGenerator.generateBuilding("Fryzjer", rank, usedComponents = usedNames, race = race)
                            extraBuildings.add(fryzjer.copy(discovered = Random.nextDouble() < 0.6))
                        }
                    }
                }
                SettlementType.TOWN -> {
                    if (district == DistrictType.CRAFT) {
                        val types = listOf("Kowal", "Płatnerz", "Rymarz").shuffled().take(2)
                        primarySpecial = BuildingGenerator.generateBuilding(types[0], rank, usedComponents = usedNames, race = race)
                        extraBuildings.add(BuildingGenerator.generateBuilding(types[1], rank, usedComponents = usedNames, race = race))
                    } else if (district == DistrictType.MILITARY) {
                        primarySpecial = generateSpecialBuildingForDistrict(district, rank, type, usedNames, race)

                        extraBuildings.add(BuildingGenerator.generateBuilding("Karczma", 3, usedComponents = usedNames, race = race))
                        extraBuildings.add(BuildingGenerator.generateBuilding("Karczma", 2, usedComponents = usedNames, race = race))
                    } else if (district == DistrictType.SACRED) {
                        primarySpecial = generateSpecialBuildingForDistrict(district, rank, type, usedNames, race)

                        extraBuildings.add(BuildingGenerator.generateBuilding("Hotel", 3, usedComponents = usedNames, race = race))
                        extraBuildings.add(BuildingGenerator.generateBuilding("Karczma", 2, usedComponents = usedNames, race = race))
                    } else {
                        primarySpecial = generateSpecialBuildingForDistrict(district, rank, type, usedNames, race)
                        
                        // Biblioteka w Centrum
                        if (zName == "Centrum") {
                            extraBuildings.add(BuildingGenerator.generateBuilding("Biblioteka", rank, usedComponents = usedNames, race = race))
                        }
                        
                        // Fryzjer - 60% szans na odkrycie w dzielnicach RESIDENTIAL i Centrum
                        if (district == DistrictType.RESIDENTIAL || district == DistrictType.MERCHANT) {
                            val fryzjer = BuildingGenerator.generateBuilding("Fryzjer", rank, usedComponents = usedNames, race = race)
                            extraBuildings.add(fryzjer.copy(discovered = Random.nextDouble() < 0.6))
                        }
                    }
                }
                SettlementType.VILLAGE -> {
                    if (zName == "Centrum") {
                        primarySpecial = BuildingGenerator.generateBuilding(
                            "Ratusz",
                            rank,
                            usedComponents = usedNames,
                            race = race
                        ).copy(discovered = false)
                        extraBuildings.add(
                            BuildingInstance(
                                type = "Studnia",
                                name = "Studnia",
                                rank = 1,
                                rooms = emptyList(),
                                totalArea = 5.0,
                                isPublicInstitution = true,
                                discovered = false
                            )
                        )
                        extraBuildings.add(
                            BuildingInstance(
                                type = "Magazyn",
                                name = "Magazyn",
                                rank = 1,
                                rooms = listOf(
                                    Room("Sala główna", 40.0),
                                    Room("Magazyn towarów", 30.0, isLocked = true),
                                    Room("Biuro", 15.0, isLocked = true)
                                ),
                                totalArea = 60.0,
                                isPublicInstitution = true,
                                discovered = false
                            )
                        )
                    } else if (district == DistrictType.ECONOMIC) {
                        val economicBuildings = listOf(
                            "Stajnia",
                            "Obora",
                            "Kurnik",
                            "Ferma",
                            "Sad",
                            "Pole"
                        ).shuffled().take(3).map { buildingName ->
                            BuildingInstance(
                                type = buildingName,
                                name = buildingName,
                                rank = 1,
                                rooms = emptyList(),
                                totalArea = if (buildingName == "Pole" || buildingName == "Sad") 500.0 else 80.0,
                                discovered = false
                            )
                        }
                        primarySpecial = economicBuildings.first()
                        extraBuildings.addAll(economicBuildings.drop(1))
                    } else {
                        primarySpecial = generateSpecialBuildingForDistrict(district, rank, type, usedNames, race)
                    }
                }
            }

            // Dodaj primarySpecial do trackera
            if (primarySpecial != null) {
                districtBuildingNames.add(primarySpecial.name)
            }

            zones.add(
                SettlementZone(
                    id = "zone_${zName}_${System.currentTimeMillis()}_${Random.nextInt(1000)}",
                    name = zName,
                    districtType = district,
                    inn = inn,
                    specialBuilding = primarySpecial!!,
                    extraBuildings = extraBuildings.filter { building ->
                        !districtBuildingNames.contains(building.name).also { isDuplicate ->
                            if (!isDuplicate) districtBuildingNames.add(building.name)
                        }
                    },
                    hasDocks = district == DistrictType.HARBOR,
                    hasWell = (type == SettlementType.TOWN && Random.nextDouble() < 0.33) ||
                        (type != SettlementType.VILLAGE && district == DistrictType.RESIDENTIAL && Random.nextDouble() < 0.2),
                    hasFountain = zName == "Centrum" && type == SettlementType.CITY && Random.nextDouble() < 0.33,
                    neighbors = calculateNeighbors(zName, type)
                )
            )
        }

        val settlement = SettlementInstance(
            id = (0..1000000000L).random(), // ID losowe zamiast Millis dla deterministyczności sesji
            name = name,
            type = type,
            zones = zones,
            race = race,
            gateDirections = gateDirections
        )

        if (context != null) {
            WorldPersistence.saveSettlement(context, settlement)
        }

        return settlement
    }

    private fun generateSpecialBuildingForDistrict(district: DistrictType, rank: Int, sType: SettlementType, usedNames: MutableSet<String>? = null, race: String = "Człowiek"): BuildingInstance {
        val typeName = when (district) {
            DistrictType.RULER_SEAT -> if (sType == SettlementType.CITY) "Pałac" else "Ratusz"
            DistrictType.SACRED -> when(sType) {
                SettlementType.CITY -> listOf("Katedra", "Wielka Świątynia", "Bazylika").random()
                SettlementType.TOWN -> listOf("Klasztor", "Świątynia", "Zakon").random()
                else -> "Kapliczka"
            }
            DistrictType.MAGIC -> if (race == "Krasnolud" || race == "Khazad") "Kuźnia Run" else "Wieża Maga"
            DistrictType.MERCHANT -> "Sukiennice"
            DistrictType.CRAFT -> listOf("Kowal", "Płatnerz", "Rymarz").random()
            DistrictType.RESIDENTIAL -> "Kamienica"
            DistrictType.ECONOMIC -> "Stajnia"
            DistrictType.HARBOR -> "Magazyn Portowy"
            DistrictType.MILITARY -> when(sType) {
                SettlementType.CITY -> listOf("Koszary", "Akademia Wojskowa", "Garnizon").random()
                else -> "Koszary"
            }
            DistrictType.SLUMS -> "Rudera"
        }
        return BuildingGenerator.generateBuilding(typeName, rank, usedComponents = usedNames, race = race)
            .withCanonicalBuildingName()
    }

    fun calculateNeighbors(
        current: String,
        type: SettlementType,
        gateDirections: Set<String> = setOf("Północ", "Wschód", "Południe", "Zachód")
    ): List<String> {
        val cityIndices = mapOf(
            "Północny-Zachód" to 0, "Północ" to 1, "Północny-Wschód" to 2,
            "Zachód" to 3, "Centrum" to 4, "Wschód" to 5,
            "Południowy-Zachód" to 6, "Południe" to 7, "Południowy-Wschód" to 8
        )
        val indexToName = cityIndices.entries.associate { it.value to it.key }

        return when (type) {
            SettlementType.VILLAGE -> {
                if (current == "Centrum") gateDirections.toList()
                else listOf("Centrum")
            }
            SettlementType.TOWN -> {
                if (current == "Centrum") {
                    gateDirections.toList()
                } else {
                    listOf("Centrum")
                }
            }
            SettlementType.CITY -> {
                val idx = cityIndices[current] ?: return listOf("Centrum")
                val row = idx / 3
                val col = idx % 3
                val neighbors = mutableListOf<String>()

                // Poziomo i pionowo (Manhattan distance == 1)
                val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
                for ((dr, dc) in directions) {
                    val nr = row + dr
                    val nc = col + dc
                    if (nr in 0..2 && nc in 0..2) {
                        indexToName[nr * 3 + nc]?.let { neighbors.add(it) }
                    }
                }
                neighbors
            }
        }
    }

    fun getStartingZoneName(settlement: SettlementInstance, arrivalDirection: String? = null): String {
        val random = Random(settlement.id)
        val gates = when(arrivalDirection?.uppercase()) {
            "N", "NORTH", "PÓŁNOC" -> listOf("Północ", "Północny-Zachód", "Północny-Wschód")
            "S", "SOUTH", "POŁUDNIE" -> listOf("Południe", "Południowy-Zachód", "Południowy-Wschód")
            "E", "EAST", "WSCHÓD" -> listOf("Wschód", "Północny-Wschód", "Południowy-Wschód")
            "W", "WEST", "ZACHÓD" -> listOf("Zachód", "Północny-Zachód", "Południowy-Zachód")
            "NW", "NORTH-WEST", "PÓŁNOCNY-ZACHÓD" -> listOf("Północny-Zachód", "Północ", "Zachód")
            "NE", "NORTH-EAST", "PÓŁNOCNY-WSCHÓD" -> listOf("Północny-Wschód", "Północ", "Wschód")
            "SW", "SOUTH-WEST", "POŁUDNIOWY-ZACHÓD" -> listOf("Południowy-Zachód", "Południe", "Zachód")
            "SE", "SOUTH-EAST", "POŁUDNIOWY-WSCHÓD" -> listOf("Południowy-Wschód", "Południe", "Wschód")
            else -> listOf("Północ", "Południe", "Wschód", "Zachód").shuffled(random)
        }
        
        val zoneNames = settlement.zones.map { it.name }
        return gates.firstOrNull { it in zoneNames } 
            ?: zoneNames.firstOrNull()
            ?: "Centrum"
    }
}
