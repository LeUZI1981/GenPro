package com.example.genpro

import kotlin.random.Random

object HeroEquipmentLogic {
    fun getStaticEquipment(): List<String> {
        val random = Random(System.currentTimeMillis())
        val silver = random.nextInt(2, 11) + random.nextInt(2, 11) // 2d10 approx
        val copper = random.nextInt(5, 21) + random.nextInt(5, 21) // 5d10 approx
        
        return listOf(
            if (random.nextBoolean()) "Plecak" else "Torba podróżna",
            "Koc",
            "Hubka i krzesiwo",
            "Miedź: $copper",
            "Srebro: $silver",
            "Racje żywnościowe (3 dni)"
        )
    }

    fun getDynamicEquipment(skills: List<String>): List<String> {
        val items = mutableListOf<String>()
        val random = Random(System.currentTimeMillis())
        
        // Jeździectwo lub Walka na wierzchowcu -> Koń
        if (skills.any { it.contains("Jeździectwo", ignoreCase = true) || it.contains("Walka na wierzchowcu", ignoreCase = true) }) {
            items.add("Koń (Wierzchowy)")
        }

        // Powożenie -> Wóz i Koń pociągowy
        if (skills.any { it.contains("Powożenie", ignoreCase = true) }) {
            items.add("Lekka bryczka (1 koń)")
            items.add("Koń pociągowy (Draft)")
        }
        
        // Broń bojowa obuchowa
        if (skills.any { it.contains("Broń bojowa obuchowa", ignoreCase = true) }) {
            items.add("🅔 " + HeroLogic.WW2nazwa.random() + " [O]")
        }
        
        // Broń bojowa sieczna
        if (skills.any { it.contains("Broń bojowa sieczna", ignoreCase = true) }) {
            items.add("🅔 " + HeroLogic.WW1nazwa.random() + " [S]")
        }

        // Broń bojowa kłuta
        if (skills.any { it.contains("Broń bojowa kłuta", ignoreCase = true) }) {
            items.add("🅔 " + HeroLogic.WW3nazwa.random() + " [K]")
        }

        // --- Pancerz startowy zintegrowany z systemem walki ---
        val klasa = if (skills.any { it.contains("Magia", ignoreCase = true) }) "Mag"
                    else if (skills.any { it.contains("Broń bojowa", ignoreCase = true) }) "Wojownik"
                    else "Łotr"
        
        getStartingArmor(klasa).forEach { piece ->
            items.add("🛡️ ${piece.name} [${piece.ppS}/${piece.ppO}/${piece.ppK}]")
        }
        
        // Inteligencja i umiejętności naukowe
        if (skills.any { it.contains("Czytanie i pisanie", ignoreCase = true) }) {
            val special = if (random.nextInt(100) < 20) HeroLogic.toolsSpecialist[3] else null // Zestaw Inżynierski
            if (special != null) items.add("${special.name} (${special.bonus})")
            else items.add("Przybory piśmiennicze (Atrament, Pióro, Pergamin)")
        }

        if (skills.any { it.contains("Łowiectwo", ignoreCase = true) }) {
            items.add("Pułapka na drobne zwierzęta")
        }
        
        if (skills.any { it.contains("Leczenie ran", ignoreCase = true) }) {
            items.add("Zestaw bandaży i ziół")
        }

        // Gotowanie -> Garnek, Patelnia lub Rondel
        if (skills.any { it.contains("Gotowanie", ignoreCase = true) }) {
            val cookingItems = listOf("Garnek żeliwny", "Patelnia miedziana", "Rondel stalowy")
            items.add(cookingItems.random())
        }

        // Chirurgia -> Zestaw Chirurgiczny
        if (skills.any { it.contains("Chirurgia", ignoreCase = true) }) {
            val item = HeroLogic.toolsSpecialist[0]
            items.add("${item.name} (${item.bonus})")
        }

        // Alchemia -> Przybory Alchemiczne
        if (skills.any { it.contains("Alchemia", ignoreCase = true) }) {
            val item = HeroLogic.toolsSpecialist[2]
            items.add("${item.name} (${item.bonus})")
        }

        // Otwieranie zamków -> Wytrychy (z szansą na Mistrzowskie)
        if (skills.any { it.contains("Otwieranie zamków", ignoreCase = true) }) {
            val isMaster = random.nextInt(100) < 15
            if (isMaster) {
                val item = HeroLogic.toolsSpecialist[1]
                items.add("${item.name} (${item.bonus})")
            } else {
                items.add("Wytrychy")
            }
        }

        // Muzykalność -> Instrumenty
        if (skills.any { it.contains("Muzykalność", ignoreCase = true) }) {
            val skill = skills.find { it.contains("Muzykalność", ignoreCase = true) } ?: ""
            val instrument = when {
                skill.contains("strunowe", ignoreCase = true) -> HeroLogic.instrumentsSpecialist[0] // Lutnia
                skill.contains("dęte", ignoreCase = true) -> if (random.nextBoolean()) HeroLogic.instrumentsSpecialist[1] else HeroLogic.instrumentsSpecialist[2] // Flet lub Dudy
                skill.contains("perkusja", ignoreCase = true) -> HeroLogic.instrumentsSpecialist[3] // Bębenek
                else -> HeroLogic.instrumentsSpecialist.random()
            }
            items.add("${instrument.name} (${instrument.bonus.ifEmpty { "Instrument" }})")
        }

        // Szansa na przedmiot egzotyczny
        if (random.nextInt(100) < 10) {
            val exotic = HeroLogic.itemsExotic.random()
            items.add("${exotic.name} (${exotic.bonus.ifEmpty { "Egzotyczny" }})")
        }

        return items.distinct()
    }

    fun getStartingArmor(klasa: String): List<ArmorPiece> {
        return when (klasa) {
            "Wojownik" -> listOf(
                HeroLogic.armorHeavy[0], // Hełm płytowy
                HeroLogic.armorHeavy[1], // Kirys stalowy
                HeroLogic.armorHeavy[2], // Naramienniki płytowe
                HeroLogic.armorHeavy[3], // Nagolenniki płytowe
                HeroLogic.armorHeavy[4]  // Rękawice płytowe
            )
            "Łotr", "Łowca" -> {
                // Średni - losowo kolczuga lub łuski (np. korpus i ramiona)
                val type = if (Random.nextBoolean()) 0 else 1 // 0: Kolcza, 1: Łuskowa
                if (type == 0) {
                    listOf(HeroLogic.armorMedium[0], HeroLogic.armorMedium[4], HeroLogic.armorMedium[8]) // Czepiec, Kolczuga, Nogawice
                } else {
                    listOf(HeroLogic.armorMedium[1], HeroLogic.armorMedium[5], HeroLogic.armorMedium[9]) // Czepiec, Łuski, Nogawice
                }
            }
            "Uczony", "Kapłan", "Mag" -> {
                // Lekki - skóra lub ćwiekowana
                val isStudded = Random.nextBoolean()
                if (isStudded) {
                    listOf(HeroLogic.armorLight[1], HeroLogic.armorLight[3], HeroLogic.armorLight[9]) // Ćwiekowane: Czapa, Kaftan, Nogawice
                } else {
                    listOf(HeroLogic.armorLight[0], HeroLogic.armorLight[2], HeroLogic.armorLight[8]) // Skórzane: Czapa, Kaftan, Nogawice
                }
            }
            else -> emptyList()
        }
    }

    fun calculateTotalProtection(equippedArmor: List<ArmorPiece>): Map<String, Triple<Int, Int, Int>> {
        val locations = listOf("Głowa", "Korpus", "Ramiona", "Nogi")
        val protectionMap = mutableMapOf<String, Triple<Int, Int, Int>>()

        for (loc in locations) {
            var s = 0
            var o = 0
            var k = 0
            equippedArmor.filter { it.locations.contains(loc) }.forEach {
                s += it.ppS
                o += it.ppO
                k += it.ppK
            }
            protectionMap[loc] = Triple(s, o, k)
        }
        return protectionMap
    }
}
