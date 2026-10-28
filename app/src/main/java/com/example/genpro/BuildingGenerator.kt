package com.example.genpro

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class Room(
    val name: String,
    val area: Double,
    val isLocked: Boolean = false,
    val description: String = ""
)

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
    val servicePrice: Int = 0,
    // Dodatkowe szczegóły dla MG
    val barDescription: String = "",
    val serviceQuality: String = "",
    val security: String = "",
    val smell: String = "",
    val floors: Int = 1
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
    val description: String = "",
    val isPublicInstitution: Boolean = false,
    val discovered: Boolean = true,
    val inventory: List<TradeItem> = emptyList(),
    val lastRefreshDate: String = "",
    val isOpen24h: Boolean = false,
    val imageRes: String? = null
)

object BuildingGenerator {
    val innAdjM = listOf("Złoty", "Pijany", "Ryczący", "Podróżny", "Ukryty", "Pęknięty", "Cienisty", "Szepczący", "Chwalebny", "Szkarłatny")
    val innNounM = listOf("Gryf", "Kufel", "Miecz", "Topór", "Kompas", "Wieprz", "Niedźwiedź", "Mag", "Kurant", "Los")

    val innAdjF = listOf("Złota", "Pijana", "Rycząca", "Podróżna", "Ukryta", "Pęknięta", "Cienista", "Szepcząca", "Chwalebna", "Szkarłatna")
    val innNounF = listOf("Przystań", "Wierzba", "Róża", "Kotwica", "Podkowa", "Brama", "Zguba", "Gospoda", "Karczma", "Korona")

    val hotelAdjM = listOf("Królewski", "Wieczny", "Niebiański", "Księżycowy", "Srebrny", "Mglisty", "Kryształowy", "Odległy", "Spokojny", "Szmaragdowy")
    val hotelNounM = listOf("Zajazd", "Dwór", "Pałac", "Azyl", "Przystanek", "Schron", "Bastion", "Pawilon", "Dom", "Szlak")

    val hotelAdjF = listOf("Królewska", "Wieczna", "Niebiańska", "Księżycowa", "Srebrna", "Mglista", "Kryształowa", "Odległa", "Spokojna", "Szmaragdowa")
    val hotelNounF = listOf("Rezydencja", "Enklawa", "Oaza", "Stanica", "Kwatera", "Willa", "Przystań", "Arkadia", "Baszta", "Wieża")

    val zamtuzAdjM = listOf("Aksamitny", "Grzeszny", "Jedwabny", "Tajemny", "Zakazany", "Rozkoszny", "Nocny", "Purpurowy", "Kuszący", "Słodki")
    val zamtuzNounM = listOf("Salon", "Ogród", "Pałac", "Raj", "Płatek", "Dotyk", "Szept", "Zakątek", "Klejnot", "Sen")

    val zamtuzAdjF = listOf("Aksamitna", "Grzeszna", "Jedwabna", "Tajemna", "Zakazna", "Rozkoszna", "Nocna", "Purpurowa", "Kusząca", "Słodka")
    val zamtuzNounF = listOf("Komnata", "Oaza", "Rozkosz", "Pokusa", "Grota", "Kurtyna", "Mgła", "Fantazja", "Pieszczota", "Noc")

    val dwarfAdj = listOf("Kamienny", "Twardy", "Górski", "Podziemny", "Miedziany", "Brodaty", "Mocny", "Głęboki", "Mroźny", "Granitowy", "Kopalniany")
    val dwarfNoun = listOf("Topór", "Młot", "Kilof", "Klan", "Kruszec", "Niebios", "Kowadło", "Ogień", "Kamień", "Ząb", "Hełm")

    val elfAdj = listOf("Leśny", "Srebrzysty", "Jasny", "Gwiezdny", "Wietrzny", "Zielony", "Stary", "Wyniosły", "Cienisty", "Mglisty")
    val elfNoun = listOf("Liść", "Łuk", "Blask", "Pieśń", "Wierzba", "Strzała", "Dąb", "Lutnia", "Świta", "Duch", "Kwiat")

    fun generateBuilding(
        type: String,
        targetRank: Int? = null,
        customName: String? = null,
        usedComponents: MutableSet<String>? = null,
        race: String = "Człowiek",
        seed: Long? = null
    ): BuildingInstance {
        val random = if (seed != null) Random(seed) else Random.Default
        val rank = targetRank ?: random.nextInt(1, 6)
        val rooms = mutableListOf<Room>()
        var innDetails: InnDetails? = null
        
        val name = customName ?: when (type) {
            "Karczma", "Tawerna", "Gospoda", "Speluna", "Gildia złodzieji", "Portal miejski" -> {
                var attempts = 0
                var generatedName = ""
                do {
                    val (adj, noun) = when(race) {
                        "Krasnolud", "Khazad" -> dwarfAdj.random(random) to dwarfNoun.random(random)
                        "Elf" -> elfAdj.random(random) to elfNoun.random(random)
                        else -> {
                            val isMasc = random.nextBoolean()
                            if (isMasc) innAdjM.random(random) to innNounM.random(random) else innAdjF.random(random) to innNounF.random(random)
                        }
                    }
                    
                    if (usedComponents == null || (!usedComponents.contains(adj) && !usedComponents.contains(noun)) || attempts > 20) {
                        generatedName = "$type $adj $noun"
                        usedComponents?.add(adj)
                        usedComponents?.add(noun)
                        break
                    }
                    attempts++
                } while (attempts < 25)
                
                if (generatedName.isEmpty()) "$type ${random.nextInt(1, 101)}" else generatedName
            }
            "Zamtuz" -> {
                var attempts = 0
                var generatedName = ""
                do {
                    val (adj, noun) = when(race) {
                        "Krasnolud", "Khazad" -> dwarfAdj.random(random) to dwarfNoun.random(random)
                        "Elf" -> elfAdj.random(random) to elfNoun.random(random)
                        else -> {
                            val isMasc = random.nextBoolean()
                            if (isMasc) zamtuzAdjM.random(random) to zamtuzNounM.random(random) else zamtuzAdjF.random(random) to zamtuzNounF.random(random)
                        }
                    }

                    if (usedComponents == null || (!usedComponents.contains(adj) && !usedComponents.contains(noun)) || attempts > 20) {
                        generatedName = "$type $adj $noun"
                        usedComponents?.add(adj)
                        usedComponents?.add(noun)
                        break
                    }
                    attempts++
                } while (attempts < 25)

                if (generatedName.isEmpty()) "$type ${random.nextInt(1, 101)}" else generatedName
            }
            "Hotel", "Motel" -> {
                var attempts = 0
                var generatedName = ""
                do {
                    val (adj, noun) = when(race) {
                        "Krasnolud", "Khazad" -> dwarfAdj.random(random) to dwarfNoun.random(random)
                        "Elf" -> elfAdj.random(random) to elfNoun.random(random)
                        else -> {
                            val isMasc = random.nextBoolean()
                            if (isMasc) hotelAdjM.random(random) to hotelNounM.random(random) else hotelAdjF.random(random) to hotelNounF.random(random)
                        }
                    }

                    if (usedComponents == null || (!usedComponents.contains(adj) && !usedComponents.contains(noun)) || attempts > 20) {
                        generatedName = "$type $adj $noun"
                        usedComponents?.add(adj)
                        usedComponents?.add(noun)
                        break
                    }
                    attempts++
                } while (attempts < 25)

                if (generatedName.isEmpty()) "$type ${random.nextInt(1, 101)}" else generatedName
            }
            "Biblioteka" -> {
                val libraryNames = listOf("Mądrości", "Starożytnych Znaków", "Księgi Światła", "Tajnych Rękopisów", "Wiecznych Słów")
                "Biblioteka ${libraryNames.random(random)}"
            }
            "Mieszkanie" -> "Dom przy ul. ${random.nextInt(1, 101)}"
            else -> type
        }

        when (type) {
            "Karczma", "Tawerna", "Gospoda", "Speluna", "Zamtuz", "Motel", "Gildia złodzieji", "Portal miejski" -> {
                val sizeMultiplier = rank * random.nextDouble(0.9, 1.3)
                if (type == "Zamtuz") {
                    rooms.add(Room("Salon główny", 35.0 * sizeMultiplier))
                    rooms.add(Room("Bar / Szynk", 15.0 * sizeMultiplier))
                    repeat(rank * 2) { i ->
                        rooms.add(Room("Pokój uciech nr ${i + 1}", 12.0 + rank, isLocked = true))
                    }
                    if (rank >= 4) rooms.add(Room("Łaźnia luksusowa", 25.0, isLocked = true))
                    
                    innDetails = InnDetails(
                        hasDanceFloor = rank >= 4,
                        hasKitchen = rank >= 3,
                        seatingCapacity = (8 * rank).toInt(),
                        tables = rank,
                        smallTables = rank * 2,
                        benches = rank,
                        hasMusicians = rank >= 3 && random.nextBoolean(),
                        barSeats = 5 + rank * 2,
                        femaleStaff = (2 * rank + random.nextInt(0, 3)),
                        maleStaff = (1 * rank / 2 + random.nextInt(0, 2)),
                        servicePrice = (rank * 15) + (random.nextInt(0, 10))
                    )
                } else if (type == "Gildia złodzieji") {
                    rooms.add(Room("Ukryta sala spotkań", 40.0 * sizeMultiplier))
                    rooms.add(Room("Skarbiec", 15.0, isLocked = true))
                    rooms.add(Room("Zbrojownia", 20.0, isLocked = true))
                    rooms.add(Room("Sala treningowa", 30.0))
                    rooms.add(Room("Tajne przejścia", 50.0, isLocked = true))
                } else if (type == "Motel") {
                    rooms.add(Room("Recepcja", 15.0 * sizeMultiplier))
                    repeat(rank * 3) { i ->
                        rooms.add(Room("Tani pokój nr ${i + 1}", 10.0 + rank, isLocked = true))
                    }
                    innDetails = InnDetails(
                        hasDanceFloor = false,
                        hasKitchen = rank >= 3,
                        seatingCapacity = (5 * rank).toInt(),
                        tables = rank,
                        smallTables = rank,
                        benches = rank,
                        hasMusicians = false,
                        barSeats = 4,
                        femaleStaff = rank,
                        maleStaff = rank,
                        servicePrice = rank * 8
                    )
                } else if (type == "Portal miejski") {
                    rooms.add(Room("Sala Tranzytowa", 60.0 * sizeMultiplier))
                    rooms.add(Room("Komnata Kryształu", 25.0 * sizeMultiplier))
                    rooms.add(Room("Laboratorium Eteryczne", 35.0 * sizeMultiplier))
                    if (rank >= 4) rooms.add(Room("Archiwum Teleportacji", 20.0, isLocked = true))
                } else {
                    rooms.add(Room("Sala Główna", 40.0 * sizeMultiplier))
                    rooms.add(Room("Zaplecze / Spiżarnia", 12.0 * sizeMultiplier))
                    
                    val hasKitchen = rank >= 2 || (rank == 1 && random.nextDouble() > 0.7)
                    if (hasKitchen) rooms.add(Room("Kuchnia", 18.0 * sizeMultiplier))
                    
                    if (rank >= 3) rooms.add(Room("Piwnica na trunki", 25.0 * sizeMultiplier, isLocked = true))
                    if (rank >= 4) rooms.add(Room("Pokoje gościnne (piętro)", 50.0 * sizeMultiplier, isLocked = true))
                    if (rank >= 5) {
                        rooms.add(Room("Sala VIP / Bankietowa", 30.0 * sizeMultiplier))
                        rooms.add(Room("Apartament właściciela", 20.0 * sizeMultiplier, isLocked = true))
                    }

                    val barDesc = listOf("długi i dębowy", "krótki, ale czysty", "lepiący się od brudu", "ozdobiony rzeźbieniami", "wąski i wysoki").random(random)
                    val servQual = listOf("szybka i sprawna", "powolna i zaspana", "uprzejma", "opryskliwa", "chaotyczna").random(random)
                    val security = if (rank > 3) listOf("dwóch osiłków przy wejściu", "czujny wykidajło", "miejska straż zaglądająca co godzinę").random(random) else "brak"
                    val smell = listOf("pieczonego mięsa i dymu", "starego piwa", "ziół i igliwia", "potu i tytoniu", "świeżego chleba").random(random)

                    innDetails = InnDetails(
                        hasDanceFloor = rank >= 3 && random.nextDouble() > 0.4,
                        hasKitchen = hasKitchen,
                        seatingCapacity = (12 * rank * random.nextDouble(1.0, 2.5)).toInt(),
                        tables = (2 * rank + random.nextInt(0, rank)).coerceAtLeast(1),
                        smallTables = (rank + random.nextInt(0, 2)).coerceAtLeast(0),
                        benches = (2 * rank).coerceAtLeast(0),
                        barDescription = barDesc,
                        serviceQuality = servQual,
                        security = security,
                        smell = smell,
                        floors = if (rank >= 4) 2 else 1
                    )
                }
            }
            "Hotel" -> {
                val sizeMultiplier = rank * 1.2
                rooms.add(Room("Recepcja i Lobby", 25.0 * sizeMultiplier))
                rooms.add(Room("Korytarz główny", 15.0 * rank))
                repeat(rank * 2 + 2) { i ->
                    rooms.add(Room("Pokój nr ${i + 101}", 14.0 + (rank * 3), isLocked = true))
                }
                if (rank >= 3) rooms.add(Room("Pralnia i Magazyn pościeli", 15.0, isLocked = true))
                if (rank >= 4) rooms.add(Room("Restauracja hotelowa", 60.0 * sizeMultiplier))
                if (rank >= 5) rooms.add(Room("Strefa SPA / Łaźnia", 45.0 * sizeMultiplier))
            }
            "Mieszkanie" -> {
                rooms.add(Room("Przedpokój", 5.0, isLocked = true))
                rooms.add(Room("Salon / Izba dzienna", 18.0 + rank * 6, isLocked = true))
                if (rank >= 2) rooms.add(Room("Sypialnia", 12.0 + rank * 3, isLocked = true))
                if (rank >= 3) rooms.add(Room("Kuchnia oddzielna", 10.0))
                if (rank >= 4) {
                    rooms.add(Room("Łazienka", 8.0))
                    rooms.add(Room("Garderoba", 5.0, isLocked = true))
                }
                if (rank >= 5) rooms.add(Room("Gabinet / Biblioteka", 15.0, isLocked = true))
            }
            "Apteka", "Medyk" -> {
                rooms.add(Room("Sala sprzedaży", 20.0 + rank * 5))
                rooms.add(Room("Laboratorium", 15.0 + rank * 4))
                rooms.add(Room("Magazyn ziół", 10.0 + rank * 2, isLocked = true))
                if (rank >= 2) rooms.add(Room("Gabinet zabiegowy", 15.0))
                if (rank >= 3) rooms.add(Room("Izba chorych", 25.0))
                if (rank >= 4) rooms.add(Room("Suszarnia", 12.0))
            }
            "Kowal" -> {
                rooms.add(Room("Kuźnia", 30.0 + rank * 10))
                rooms.add(Room("Magazyn żelaza", 15.0 + rank * 5, isLocked = true))
                rooms.add(Room("Zaplecze", 10.0 + rank * 2))
                if (rank >= 3) rooms.add(Room("Piętro mieszkalne", 25.0 * (rank - 2), isLocked = true))
            }
            "Płatnerz" -> {
                rooms.add(Room("Warsztat płatnerski", 25.0 + rank * 8))
                rooms.add(Room("Przymierzalnia", 10.0 + rank * 2))
                rooms.add(Room("Magazyn wyrobów", 15.0 + rank * 4, isLocked = true))
                if (rank >= 3) rooms.add(Room("Prywatne pokoje", 20.0 * (rank - 2), isLocked = true))
            }
            "Rymarz" -> {
                rooms.add(Room("Pracownia rymarska", 25.0 + rank * 7))
                rooms.add(Room("Suszarnia skór", 20.0 + rank * 5))
                rooms.add(Room("Magazyn skór", 15.0 + rank * 3, isLocked = true))
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
            "Magazyn", "Magazyn Portowy" -> {
                rooms.add(Room("Sala główna", 40.0 + rank * 15))
                rooms.add(Room("Magazyn towarów", 30.0 + rank * 20, isLocked = true))
                rooms.add(Room("Biuro", 15.0 + rank * 5, isLocked = true))
                if (rank >= 3) rooms.add(Room("Magazyn archiwalny", 20.0 + rank * 8, isLocked = true))
            }
            "Koszary" -> {
                rooms.add(Room("Plac apelowy", 50.0 + rank * 20))
                rooms.add(Room("Zbrojownia", 20.0 + rank * 10, isLocked = true))
                rooms.add(Room("Dormitorium", 40.0 + rank * 15))
                rooms.add(Room("Sala treningowa", 30.0 + rank * 12))
                if (rank >= 4) rooms.add(Room("Kwatery oficerskie", 20.0 + rank * 5, isLocked = true))
            }
            "Szko��a", "Uniwersytet", "Kolegium", "Akademia Wojskowa", "Szkoła Magii" -> {
                rooms.add(Room("Sala wykładowa", 30.0 + rank * 15))
                rooms.add(Room("Biblioteka", 25.0 + rank * 10))
                rooms.add(Room("Gabinet rektora", 15.0 + rank * 3, isLocked = true))
                if (type == "Akademia Wojskowa" || type == "Szkoła Magii") {
                    rooms.add(Room("Sala treningowa", 35.0 + rank * 10))
                }
                if (rank >= 3) rooms.add(Room("Laboratorium", 20.0 + rank * 5, isLocked = true))
                if (rank >= 4) rooms.add(Room("Dormitoria studenckie", 40.0 + rank * 10))
            }
            "Katedra", "Świątynia", "Klasztor" -> {
                rooms.add(Room("Nawa główna", 60.0 + rank * 30))
                rooms.add(Room("Zakrystia", 15.0 + rank * 3, isLocked = true))
                rooms.add(Room("Kaplica boczna", 20.0 + rank * 5))
                if (rank >= 3) rooms.add(Room("Krużganki", 30.0 + rank * 10))
                if (rank >= 4) rooms.add(Room("Biblioteka zakonna", 20.0 + rank * 8, isLocked = true))
            }
            "Biblioteka" -> {
                rooms.add(Room("Czytelnia", 40.0 + rank * 15))
                rooms.add(Room("Magazyn ksiąg", 30.0 + rank * 10, isLocked = true))
                if (rank >= 3) rooms.add(Room("Sala wykładowa", 25.0 + rank * 8))
                if (rank >= 4) rooms.add(Room("Archiwum starych rękopisów", 20.0 + rank * 5, isLocked = true))
            }
            "Fryzjer" -> {
                rooms.add(Room("Sala główna", 25.0 + rank * 5))
                rooms.add(Room("Magazyn kosmetyków", 10.0 + rank * 3, isLocked = true))
                if (rank >= 3) rooms.add(Room("Pokój VIP", 15.0 + rank * 4))
            }
            else -> {
                rooms.add(Room("Sala Główna", 25.0 * rank))
                rooms.add(Room("Zaplecze", 12.0 * rank))
                if (rank >= 3) rooms.add(Room("Piętro", 20.0 * rank))
            }
        }

        val totalArea = rooms.sumOf { it.area }

        val isPublic = type in listOf("Koszary", "Szkoła", "Uniwersytet", "Kolegium", "Katedra", "Świątynia", "Klasztor", "Akademia Wojskowa", "Szkoła Magii", "Biblioteka")

        val instance = BuildingInstance(
            type = type,
            name = name,
            address = "ul. ${(innNounM + hotelNounM + dwarfNoun + elfNoun).random(random)} ${random.nextInt(1, 101)}",
            rank = rank,
            rooms = rooms,
            totalArea = totalArea,
            innDetails = innDetails,
            isPublicInstitution = isPublic
        )
        return instance.copy(description = generateDescription(instance))
    }

    fun generateDescription(instance: BuildingInstance): String {
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
        return "⭐".repeat(rank)
    }

    fun generateMarketplaceInventory(): List<TradeItem> {
        val inventory = mutableListOf<TradeItem>()
        val allPossibleItems = mutableListOf<Item>()
        
        // Broń prosta i bojowa
        allPossibleItems.addAll(ItemRegistry.weapons.filter { 
            it.subCategory == "Broń prosta" || it.category == "Biała" || it.category == "Strzelecka"
        })
        
        // Pancerze
        allPossibleItems.addAll(ItemRegistry.armors)

        if (allPossibleItems.isEmpty()) return emptyList()

        repeat(8) {
            val baseItem = allPossibleItems.random()
            val quality = Random.nextInt(60, 81)
            val baseVal = baseItem.value
            
            val buyPrice = (baseVal * (quality / 100.0) * 0.9).toInt()
            val sellPrice = (baseVal * (quality / 100.0) * 0.5).toInt()
            
            inventory.add(TradeItem(
                name = baseItem.name,
                price = baseVal,
                category = baseItem.category,
                quality = quality,
                buyPrice = buyPrice,
                sellPrice = sellPrice
            ))
        }
        return inventory
    }
}
