package com.example.genpro

import kotlin.random.Random

data class Room(val name: String, val area: Double)

data class InnDetails(
    val hasDanceFloor: Boolean,
    val hasKitchen: Boolean,
    val seatingCapacity: Int,
    val tables: Int,
    val smallTables: Int,
    val benches: Int,
    // Specyficzne dla zamtuza
    val hasMusicians: Boolean = false,
    val barSeats: Int = 0,
    val femaleStaff: Int = 0,
    val maleStaff: Int = 0,
    val servicePrice: Int = 0
)

data class BuildingInstance(
    val type: String,
    val name: String,
    val rank: Int, // 1 to 5
    val rooms: List<Room>,
    val totalArea: Double,
    val innDetails: InnDetails? = null,
    val description: String = ""
)

object BuildingGenerator {
    private val adj1 = listOf("Stara", "Złota", "Podgniła", "Wesoła", "Groźna", "Srebrna", "Biała", "Czarna", "Pijana", "Różowa", "Aksamitna", "Nocna")
    private val adj2 = listOf("Świnia", "Gęś", "Kaczka", "Tarcza", "Kufa", "Szpada", "Róża", "Gwiazda", "Latarnia", "Podwiązka", "Mgła", "Rozkosz")

    fun generateBuilding(type: String, targetRank: Int? = null): BuildingInstance {
        val rank = targetRank ?: (1..5).random()
        val rooms = mutableListOf<Room>()
        var innDetails: InnDetails? = null
        val name = when (type) {
            "Karczma", "Tawerna" -> "${adj1.random()} ${adj2.random()}"
            "Hotel" -> "Hotel ${adj2.random()}"
            "Mieszkanie" -> "Dom przy ul. ${(1..100).random()}"
            "Zamtuz" -> "Dom ${adj2.random()}"
            else -> "Lokal: $type"
        }

        when (type) {
            "Karczma", "Tawerna", "Zamtuz" -> {
                val sizeMultiplier = rank * Random.nextDouble(0.9, 1.3)
                if (type == "Zamtuz") {
                    rooms.add(Room("Salon główny", 35.0 * sizeMultiplier))
                    rooms.add(Room("Bar / Szynk", 15.0 * sizeMultiplier))
                    repeat(rank * 2) { i ->
                        rooms.add(Room("Pokój uciech nr ${i + 1}", 12.0 + rank))
                    }
                    if (rank >= 4) rooms.add(Room("Łaźnia luksusowa", 25.0))
                    
                    innDetails = InnDetails(
                        hasDanceFloor = rank >= 4,
                        hasKitchen = rank >= 3,
                        seatingCapacity = (8 * rank).toInt(),
                        tables = rank,
                        smallTables = rank * 2,
                        benches = rank,
                        hasMusicians = rank >= 3 && Random.nextBoolean(),
                        barSeats = 5 + rank * 2,
                        femaleStaff = (2 * rank + Random.nextInt(0, 3)),
                        maleStaff = (1 * rank / 2 + Random.nextInt(0, 2)),
                        servicePrice = (rank * 15) + (Random.nextInt(0, 10))
                    )
                } else {
                    rooms.add(Room("Sala Główna", 40.0 * sizeMultiplier))
                    rooms.add(Room("Zaplecze / Spiżarnia", 12.0 * sizeMultiplier))
                    
                    val hasKitchen = rank >= 2 || (rank == 1 && Random.nextDouble() > 0.7)
                    if (hasKitchen) rooms.add(Room("Kuchnia", 18.0 * sizeMultiplier))
                    
                    if (rank >= 3) rooms.add(Room("Piwnica na trunki", 25.0 * sizeMultiplier))
                    if (rank >= 4) rooms.add(Room("Pokoje gościnne (piętro)", 50.0 * sizeMultiplier))
                    if (rank >= 5) {
                        rooms.add(Room("Sala VIP / Bankietowa", 30.0 * sizeMultiplier))
                        rooms.add(Room("Apartament właściciela", 20.0 * sizeMultiplier))
                    }

                    innDetails = InnDetails(
                        hasDanceFloor = rank >= 3 && Random.nextDouble() > 0.4,
                        hasKitchen = hasKitchen,
                        seatingCapacity = (12 * rank * Random.nextDouble(1.0, 2.5)).toInt(),
                        tables = (2 * rank + Random.nextInt(0, rank)).coerceAtLeast(1),
                        smallTables = (rank + Random.nextInt(0, 2)).coerceAtLeast(0),
                        benches = (2 * rank).coerceAtLeast(0)
                    )
                }
            }
            "Hotel" -> {
                val sizeMultiplier = rank * 1.2
                rooms.add(Room("Recepcja i Lobby", 25.0 * sizeMultiplier))
                rooms.add(Room("Korytarz główny", 15.0 * rank))
                repeat(rank * 2 + 2) { i ->
                    rooms.add(Room("Pokój nr ${i + 101}", 14.0 + (rank * 3)))
                }
                if (rank >= 3) rooms.add(Room("Pralnia i Magazyn pościeli", 15.0))
                if (rank >= 4) rooms.add(Room("Restauracja hotelowa", 60.0 * sizeMultiplier))
                if (rank >= 5) rooms.add(Room("Strefa SPA / Łaźnia", 45.0 * sizeMultiplier))
            }
            "Mieszkanie" -> {
                rooms.add(Room("Przedpokój", 5.0))
                rooms.add(Room("Salon / Izba dzienna", 18.0 + rank * 6))
                if (rank >= 2) rooms.add(Room("Sypialnia", 12.0 + rank * 3))
                if (rank >= 3) rooms.add(Room("Kuchnia oddzielna", 10.0))
                if (rank >= 4) {
                    rooms.add(Room("Łazienka", 8.0))
                    rooms.add(Room("Garderoba", 5.0))
                }
                if (rank >= 5) rooms.add(Room("Gabinet / Biblioteka", 15.0))
            }
            "Apteka" -> {
                rooms.add(Room("Sala sprzedaży", 20.0 + rank * 5))
                rooms.add(Room("Laboratorium", 15.0 + rank * 4))
                rooms.add(Room("Magazyn ziół", 10.0 + rank * 2))
                if (rank >= 4) rooms.add(Room("Suszarnia", 12.0))
            }
        }

        val totalArea = rooms.sumOf { it.area }

        return BuildingInstance(
            type = type,
            name = name,
            rank = rank,
            rooms = rooms,
            totalArea = totalArea,
            innDetails = innDetails
        )
    }

    fun getRankStars(rank: Int): String {
        return "✰".repeat(rank)
    }
}
