package com.example.genpro.logic

import com.example.genpro.HeroLogic

object RestManager {
    // Koszt w monetach miedzi (miedziaki) za godzinę odpoczynku
    fun getCostPerHour(roomType: String): Int {
        return when (roomType) {
            "Siano w stajni" -> 0
            "Wspólna sala" -> 5
            "Pokój" -> 10
            "Apartament" -> 20
            else -> 0
        }
    }

    // Regeneracja zmęczenia per godzinę bazująca na Wytrzymałości = Odporność / 10
    fun getFatigueReductionPerHour(roomType: String, stamina: Int = 10): Int {
        val multiplier = when (roomType) {
            "Siano w stajni" -> 4
            "Wspólna sala" -> 8
            "Pokój" -> 10
            "Apartament" -> 12
            else -> 2
        }
        return stamina * multiplier
    }

    // Bonus do regeneracji zmęczenia per godzinę z przedmiotów w ekwipunku (Koc: +2)
    fun getItemBonusForRest(heroData: String): Int {
        return if (HeroLogic.hasItem(heroData, "Koc")) 2 else 0
    }

    // Regeneracja dla obozu: Wytrzymałość x 2
    fun getCampFatigueReductionPerHour(stamina: Int): Int {
        return stamina * 2
    }

    // Bonus do regeneracji zmęczenia w obozie per godzinę z przedmiotów (Koc: +2)
    fun getItemBonusForCamp(heroData: String): Int {
        return if (HeroLogic.hasItem(heroData, "Koc")) 2 else 0
    }

    // Energia przywracana przez posiłek: koszt x 2
    fun getEnergyRecoveryFromMeal(mealPrice: Int): Int {
        return mealPrice * 2
    }
}
