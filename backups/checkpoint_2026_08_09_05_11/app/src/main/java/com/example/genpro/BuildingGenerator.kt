package com.example.genpro

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class Room(val name: String, val area: Double)

@Serializable
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

@Serializable
data class BuildingInstance(
    val type: String,
    val name: String,
    val address: String = "",
    val rank: Int, // 1 to 5
    val rooms: List<Room>,
    val totalArea: Double,
    val innDetails: InnDetails? = null,
    val description: String = ""
)

object BuildingGenerator {
    private val adjM = listOf("Złoty", "Srebrny", "Podpity", "Stary", "Ryczący", "Czarny", "Żelazny", "Chwalebny", "Ślepy", "Tańczący", "Dziki", "Karmazynowy")
    private val nounM = listOf("Gryf", "Topór", "Dzik", "Kufel", "Dzban", "Rycerz", "Knur", "Zajazd", "Wilk", "Kocioł", "Pawęż", "Róg")

    private val adjF = listOf("Złota", "Srebrna", "Pijana", "Stara", "Rycząca", "Czarna", "Żelazna", "Chwalebna", "Ślepa", "Tańcząca", "Dzika", "Karmazynowa")
    private val nounF = listOf("Wiedźma", "Syrena", "Gwiazda", "Moneta", "Kotwica", "Tarcza", "Loża", "Gospoda", "Wataha", "Czaszka", "Żmija")

    fun generateBuilding(type: String, targetRank: Int? = null, customName: String? = null): BuildingInstance {
        val rank = targetRank ?: (1..5).random()
        val rooms = mutableListOf<Room>()
        var innDetails: InnDetails? = null
        
        val name = customName ?: when (type) {
            "Karczma", "Tawerna", "Gospoda", "Speluna" -> {
                if (Random.nextBoolean()) {
                    "${adjM.random()} ${nounM.random()}"
                } else {
                    "${adjF.random()} ${nounF.random()}"
                }
            }
            "Hotel" -> "Hotel ${nounM.random()}"
            "Mieszkanie" -> "Dom przy ul. ${(1..100).random()}"
            "Zamtuz" -> "Dom ${nounF.random()}"
            else -> "Lokal: $type"
        }

        when (type) {
            "Karczma", "Tawerna", "Gospoda", "Speluna", "Zamtuz" -> {
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
            "Kowal" -> {
                rooms.add(Room("Kuźnia", 30.0 + rank * 10))
                rooms.add(Room("Magazyn żelaza", 15.0 + rank * 5))
                rooms.add(Room("Zaplecze", 10.0 + rank * 2))
                if (rank >= 3) rooms.add(Room("Piętro mieszkalne", 25.0 * (rank - 2)))
            }
            "Płatnerz" -> {
                rooms.add(Room("Warsztat płatnerski", 25.0 + rank * 8))
                rooms.add(Room("Przymierzalnia", 10.0 + rank * 2))
                rooms.add(Room("Magazyn wyrobów", 15.0 + rank * 4))
                if (rank >= 3) rooms.add(Room("Prywatne pokoje", 20.0 * (rank - 2)))
            }
            "Rymarz" -> {
                rooms.add(Room("Pracownia rymarska", 25.0 + rank * 7))
                rooms.add(Room("Suszarnia skór", 20.0 + rank * 5))
                rooms.add(Room("Magazyn skór", 15.0 + rank * 3))
                if (rank >= 4) rooms.add(Room("Sklepik frontowy", 15.0))
            }
            "Stajnia" -> {
                rooms.add(Room("Główna stajnia", 40.0 + rank * 15))
                rooms.add(Room("Siodlarnia", 15.0 + rank * 3))
                rooms.add(Room("Paszarnia", 20.0 + rank * 4))
                repeat(rank) { i ->
                    rooms.add(Room("Boks nr ${i + 1}", 9.0))
                }
                if (rank >= 3) rooms.add(Room("Miejsce na sianie", 30.0))
            }
            else -> {
                rooms.add(Room("Sala Główna", 25.0 * rank))
                rooms.add(Room("Zaplecze", 12.0 * rank))
                if (rank >= 3) rooms.add(Room("Piętro", 20.0 * rank))
            }
        }

        val totalArea = rooms.sumOf { it.area }

        val instance = BuildingInstance(
            type = type,
            name = name,
            address = "ul. ${nounM.random()} ${(1..100).random()}",
            rank = rank,
            rooms = rooms,
            totalArea = totalArea,
            innDetails = innDetails
        )
        return instance.copy(description = generateDescription(instance))
    }

    private fun generateDescription(instance: BuildingInstance): String {
        val sb = StringBuilder()
        val r = instance.rank
        val type = instance.type

        val quality = when (r) {
            1 -> listOf("odrażający", "zrujnowany", "brudny", "podejrzany", "zatęchły").random()
            2 -> listOf("skromny", "prosty", "zaniedbany", "przeciętny", "surowy").random()
            3 -> listOf("przyzwoity", "schludny", "solidny", "zadbany", "gościnny").random()
            4 -> listOf("wykwintny", "bogaty", "elegancki", "luksusowy", "reprezentacyjny").random()
            5 -> listOf("magnacki", "królewski", "zachwycający", "przepyszny", "unikatowy").random()
            else -> "zwyczajny"
        }

        val atmosphere = when (type) {
            "Karczma", "Tawerna", "Gospoda", "Speluna" -> {
                if (r <= 2) "W powietrzu unosi się zapach taniego piwa i dymu z paleniska."
                else if (r <= 4) "Wnętrze jest gwarne, a dębowe ławy lśnią od czystości."
                else "Kryształowe żyrandole i jedwabne obrusy świadczą o wysokim statusie gości."
            }
            "Zamtuz" -> "Czerwone aksamity i zapach egzotycznych kadzideł tworzą intymną atmosferę."
            "Hotel" -> "W recepcji panuje profesjonalna cisza, przerywana jedynie tykaniem zegara."
            "Apteka" -> "Wszędzie stoją słoje z ziołami i dziwnymi miksturami, a w powietrzu czuć zapach kamfory."
            else -> "Lokal wydaje się spełniać swoją funkcję bez zbędnych ozdób."
        }

        sb.append("To $quality lokal typu $type. $atmosphere ")

        instance.innDetails?.let { details ->
            if (details.seatingCapacity > 0) {
                sb.append("Może pomieścić około ${details.seatingCapacity} osób przy ${details.tables} stołach. ")
            }
            if (details.hasKitchen) {
                sb.append("Z zaplecza dochodzą smakowite zapachy świeżego jadła. ")
            }
            if (details.hasMusicians) {
                sb.append("W rogu przygrywa kapela, nadając miejscu życia. ")
            }
            if (type == "Zamtuz") {
                sb.append("Dyskretna obsługa liczy ${details.femaleStaff} kurtyzan i ${details.maleStaff} żigolo. ")
            }
        }

        if (instance.rooms.size > 3) {
            sb.append("Budynek jest rozległy, o łącznej powierzchni ${instance.totalArea.toInt()} m².")
        }

        return sb.toString()
    }

    fun getRankStars(rank: Int): String {
        return "✰".repeat(rank)
    }
}
