package com.example.genpro

import kotlinx.serialization.Serializable

/**
 * Enum definiujący tempa podróży z ich charakterystykami.
 * Tempo determinuje prędkość poruszania się i koszt zmęczenia.
 */
@Serializable
enum class TravelPace(
    val displayName: String,
    val speedMultiplier: Float,  // Mnożnik szybkości (Sz / 2, Sz, 2*Sz)
    val fatiguePerKm: Float       // Bazowy koszt zmęczenia na 1 km
) {
    CAUTIOUS("Ostrożne", 0.5f, 2f),
    NORMAL("Normalne", 1.0f, 4f),
    RUNNING("Biegiem", 2.0f, 12f)
}

/**
 * Dane wyjściowe z obliczenia podróży na jednym kafelku.
 */
@Serializable
data class TravelResult(
    val pace: TravelPace,
    val distanceKm: Float = 5f,
    val biome: TravelBiome,
    val isOnRoad: Boolean,
    val isDiscovered: Boolean,
    val travelTimeHours: Float,
    val travelTimeMinutes: Int,
    val fatigueGained: Int,
    val messageLog: String  // Wiadomość wyjaśniająca obliczenia
)

/**
 * Klasa zawierająca logikę obliczania podróży na mapie kafelkowej.
 * Implementuje wszystkie reguły podane w specyfikacji.
 */
object TravelMechanics {
    private const val TILE_DISTANCE_KM = 5f
    private const val DISCOVERY_TIME_PENALTY_HOURS = 1.5f
    private const val DISCOVERY_FATIGUE_PENALTY = 15
    private const val ROAD_TIME_MULTIPLIER = 0.5f  // Bonus: 20% szybciej

    /**
     * Główna metoda obliczająca wpływ podróży na kafelku.
     *
     * @param heroData Dane bohatera (do pobrania statystyk Szybkości i Wytrzymałości)
     * @param pace Wybrane tempo podróży
     * @param targetCell Kafelek docelowy
     * @param isDiscovered Czy kafelek został już odkryty
     * @param isOnRoad Czy kafelek zawiera drogę
     * @return TravelResult zawierający czas, zmęczenie i opis
     */
    fun calculateTravelTile(
        heroData: String,
        pace: TravelPace,
        targetCell: TravelMapCell,
        isDiscovered: Boolean,
        isOnRoad: Boolean
    ): TravelResult {
        // Jeśli biom to jezioro i nie ma drogi - niemożliwe do przejścia
        if (targetCell.biome == TravelBiome.LAKE && !isOnRoad || targetCell.biome == TravelBiome.RIVER && !isOnRoad) {
            return TravelResult(
                pace = pace,
                biome = targetCell.biome,
                isOnRoad = false,
                isDiscovered = isDiscovered,
                travelTimeHours = 0f,
                travelTimeMinutes = 0,
                fatigueGained = 0,
                messageLog = "❌ Brak przejazdu!"
            )
        }

        // Jeśli wchodzisz na nieznany kafelek w tempie "Biegiem" - zignoruj i przełącz na normalne
        val effectivePace = if (!isDiscovered && pace == TravelPace.RUNNING) {
            TravelPace.NORMAL
        } else {
            pace
        }

        val speedKmH = HeroLogic.getStatValue(heroData, "Sz").toFloat() * effectivePace.speedMultiplier
        val wytrzymalosc = HeroLogic.getStatValue(heroData, "Wt")

        // 1. Oblicz czas podróży (w km/h)
        var travelTimeHours = TILE_DISTANCE_KM / speedKmH

        // 2. Zastosuj modyfikator biomu (dystans = 5 km)
        val biomeModifier = if (isOnRoad) {
            ROAD_TIME_MULTIPLIER  // Droga: x0.5 (szybciej)
        } else {
            when (targetCell.biome) {
                TravelBiome.MEADOW -> 1.0f
                TravelBiome.FOREST -> 1.5f
                TravelBiome.ROCKS -> 2.5f
                TravelBiome.SWAMP -> 3.0f
                TravelBiome.RIVER -> 1.5f
                TravelBiome.LAKE -> 2.5f  // Nigdy się nie powinno tutaj trafić bez drogi
                TravelBiome.SETTLEMENT -> 0.5f  // Szybko przechodzimy osadę
                TravelBiome.SPECJALNY -> 1.0f  // Specjalny biom
            }
        }
        travelTimeHours *= biomeModifier

        // 3. Oblicz zmęczenie
        val baseDistanceFatigue = TILE_DISTANCE_KM * effectivePace.fatiguePerKm
        val wytrzymaloscModifier = 1.1f - (wytrzymalosc * 0.01f)
        val fatigueAfterWytrzymalosc = baseDistanceFatigue * wytrzymaloscModifier

        // 4. Jeśli kafelek nieznany - dodaj kary
        var totalFatigue = fatigueAfterWytrzymalosc.toInt()
        var discoveryLog = ""

        if (!isDiscovered) {
            travelTimeHours += DISCOVERY_TIME_PENALTY_HOURS
            totalFatigue += DISCOVERY_FATIGUE_PENALTY
            discoveryLog = "🕰️ ${DISCOVERY_TIME_PENALTY_HOURS}h \n😫 +$DISCOVERY_FATIGUE_PENALTY z"
        }

        val travelTimeMinutes = (travelTimeHours * 60).toInt()

        val messageLog = buildString {
            append("⚡ TEMPO: ${effectivePace.displayName}\n")
            append("🏃 Szybkość: ${speedKmH.toInt()} km/h\n")
            append("📏 Dystans: $TILE_DISTANCE_KM km\n")
            if (isOnRoad) {
                append("🛣️ Droga: TAK (x${ROAD_TIME_MULTIPLIER})\n")
            } else {
                append("🛣️ Bez drogi: ${targetCell.biome.displayName} (x$biomeModifier)\n")
            }
            append("⏱️ Czas: ${travelTimeHours.toInt()}h ${((travelTimeHours % 1) * 60).toInt()}min (${travelTimeMinutes}min)\n")
            append("💪 Wytrzymałość: $wytrzymalosc (modifier x%.2f)\n".format(wytrzymaloscModifier))
            append("😤 Zmęczenie: +$totalFatigue pkt\n")
            append(discoveryLog)
        }

        return TravelResult(
            pace = effectivePace,
            biome = targetCell.biome,
            isOnRoad = isOnRoad,
            isDiscovered = isDiscovered,
            travelTimeHours = travelTimeHours,
            travelTimeMinutes = travelTimeMinutes,
            fatigueGained = totalFatigue.coerceIn(0, 100),
            messageLog = messageLog
        )
    }

    /**
     * Oblicza wpływ podróży i aktualizuje HeroProgress.
     * Łączy wyliczoną podróż z istniejącym systemem czasu.
     *
     * WAŻNE: advanceTimeMinutesProgress() już dodaje zmęczenie pogodowe,
     * więc my dodajemy zmęczenie z podróży do już zaktualizowanego zmęczenia.
     *
     * Przepływ zmęczenia:
     * 1. advanceTimeMinutesProgress() dodaje: 2 pkt/h + modyfikator pogody
     * 2. Następnie my dodajemy: zmęczenie z podróży (Tempo × Biom × Wyt)
     * 3. Rezultat: Całkowite zmęczenie = Pogoda + Podróż
     */
    fun applyTravelToProgress(
        currentProgress: HeroProgress,
        travelResult: TravelResult,
        readFromFile: (String) -> String
    ): HeroProgress {
        val minutes = travelResult.travelTimeMinutes

        // Użyj istniejącej metody do zaawansowania czasu
        // Ta metoda już dodaje zmęczenie pogodowe!
        val updatedProgress = HeroLogic.advanceTimeMinutesProgress(currentProgress, minutes)

        // Dodaj zmęczenie z podróży do już zaktualizowanego zmęczenia
        val newFatigue = (updatedProgress.fatigue + travelResult.fatigueGained)
            .coerceIn(0, updatedProgress.maxFatigue)

        // Zwróć wynik z całkowitym zmęczeniem
        return updatedProgress.copy(fatigue = newFatigue)
    }
}
