/**
 * DOKUMENTACJA MECHANIKI PODRÓŻY
 * ===============================
 *
 * PLIK: TravelMechanic.kt
 * LOKALIZACJA: app/src/main/java/com/example/genpro/TravelMechanic.kt
 *
 * OPIS:
 * Implementuje kompleksowy system obliczania wpływu podróży na mapie kafelkowej (5km za kafelek).
 * Uwzględnia tempo, biom, odkrycie terenu, szybkość i wytrzymałość bohatera.
 *
 * ================================================================================
 * 1. TEMPO PODRÓŻY (TravelPace)
 * ================================================================================
 *
 * OSTROŻNE (Cautious):
 *   - Prędkość: Szybkość / 2 (km/h)
 *   - Zmęczenie: 2 pkt/km
 *   - Zastosowanie: Nieznany teren, zbieranie informacji
 *
 * NORMALNE (Normal):
 *   - Prędkość: Szybkość (km/h)
 *   - Zmęczenie: 4 pkt/km
 *   - Zastosowanie: Standardowa podróż
 *
 * BIEGIEM (Running):
 *   - Prędkość: 2 × Szybkość (km/h)
 *   - Zmęczenie: 12 pkt/km
 *   - Ograniczenie: NIE DZIAŁA na nieznanym terenie (automatycznie przełącza się na normalne)
 *
 * ================================================================================
 * 2. MODYFIKATORY BIOMÓW
 * ================================================================================
 *
 * Dystans kafelka: 5 km (stały)
 *
 * NA DRODZE (isOnRoad = true):
 *   - Czas: ×0.8 (20% szybciej)
 *   - Ignoruje kary biomu
 *
 * BEZ DROGI:
 *   - Łąka (MEADOW):      ×1.0 (bez modyfikacji)
 *   - Las (FOREST):       ×1.5 (50% wolniej)
 *   - Skały (ROCKS):      ×2.0 (2x wolniej)
 *   - Bagno (SWAMP):      ×3.0 (3x wolniej)
 *   - Jezioro (LAKE):     NIEMOŻLIWE bez drogi
 *
 * ================================================================================
 * 3. ODKRYWANIE NIEZNANEGO KAFELKA
 * ================================================================================
 *
 * Jeśli biom = TravelBiome.MEADOW/FOREST/itp i isDiscovered = false:
 *   - Dodatkowy czas: +2.0 godziny (rekonesans)
 *   - Dodatkowe zmęczenie: +15 pkt (stres i czujność)
 *
 * Tempo BIEGIEM na nieznanych kafelkach:
 *   - Automatycznie przełącza się na NORMALNE
 *
 * ================================================================================
 * 4. WPŁYW WYTRZYMAŁOŚCI
 * ================================================================================
 *
 * Wzór modyfikatora: 1.1 - (Wytrzymałość × 0.01)
 *
 * Przykład:
 *   - Wyt = 10: modifier = 1.1 - 0.1 = 1.0 (brak zmian)
 *   - Wyt = 5:  modifier = 1.1 - 0.05 = 1.05 (5% więcej zmęczenia)
 *   - Wyt = 20: modifier = 1.1 - 0.2 = 0.9 (10% mniej zmęczenia)
 *   - Wyt = 30: modifier = 1.1 - 0.3 = 0.8 (20% mniej zmęczenia)
 *
 * Zmęczenie = (Dystans km × Zmęczenie/km × Modifier Wyt) + Kary
 *
 * ================================================================================
 * FORMATY DANYCH
 * ================================================================================
 *
 * TravelPace (enum):
 *   - displayName: String (np. "Ostrożne", "Normalne", "Biegiem")
 *   - speedMultiplier: Float (0.5f, 1.0f, 2.0f)
 *   - fatiguePerKm: Float (2f, 4f, 12f)
 *
 * TravelResult (data class):
 *   - pace: TravelPace (wybrane tempo)
 *   - distanceKm: Float (zawsze 5f)
 *   - biome: TravelBiome (FOREST, MEADOW, LAKE, SWAMP, ROCKS, SETTLEMENT)
 *   - isOnRoad: Boolean (czy kafelek ma drogę)
 *   - isDiscovered: Boolean (czy kafelek był już odkryty)
 *   - travelTimeHours: Float (całkowity czas w godzinach)
 *   - travelTimeMinutes: Int (całkowity czas w minutach)
 *   - fatigueGained: Int (zmęczenie 0-100)
 *   - messageLog: String (szczegóły obliczeń)
 *
 * ================================================================================
 * PRZYKŁAD UŻYCIA
 * ================================================================================
 *
 * // 1. Oblicz wpływ podróży
 * val result = TravelMechanic.calculateTravelTile(
 *     heroData = readFromFile("hero_1"),
 *     pace = TravelPace.NORMAL,
 *     targetCell = TravelMapCell(x=5, y=5, biome=TravelBiome.FOREST),
 *     isDiscovered = false,  // Pierwszy raz na tym kafelku
 *     isOnRoad = false
 * )
 *
 * // 2. Zastosuj do HeroProgress
 * val updatedProgress = TravelMechanic.applyTravelToProgress(
 *     currentProgress = heroProgress,
 *     travelResult = result,
 *     readFromFile = ::readFromFile
 * )
 *
 * // 3. Pokaż graczowi wynik
 * Toast.makeText(context, result.messageLog, Toast.LENGTH_LONG).show()
 *
 * // 4. Zaktualizuj stan
 * persist(updatedMap, updatedProgress)
 *
 * ================================================================================
 * INTEGRACJA Z HeroTravelMapScreen
 * ================================================================================
 *
 * W funkcji moveTo() w HeroTravelMapScreen dodaj:
 *
 * fun moveTo(cell: TravelMapCell) {
 *     if (abs(cell.x - map.heroX) + abs(cell.y - map.heroY) != 1) return
 *
 *     // ... istniejący kod ...
 *
 *     // NOWY KOD: Oblicz wpływ podróży
 *     val heroData = readFromFile("hero_$heroId")
 *     val isOnRoad = key(cell.x, cell.y) in map.roadCells
 *     val travelResult = TravelMechanic.calculateTravelTile(
 *         heroData = heroData,
 *         pace = TravelPace.NORMAL,  // Za zmienić na UI
 *         targetCell = cell,
 *         isDiscovered = key(cell.x, cell.y) in map.discoveredCells,
 *         isOnRoad = isOnRoad
 *     )
 *
 *     val updatedProgress = TravelMechanic.applyTravelToProgress(
 *         heroProgress, travelResult, ::readFromFile
 *     )
 *
 *     // Pokaż graczowi logowanie
 *     Toast.makeText(context, travelResult.messageLog, Toast.LENGTH_LONG).show()
 *
 *     // Aktualizuj mapę i postęp
 *     persist(
 *         map.copy(
 *             heroX = cell.x,
 *             heroY = cell.y,
 *             cameraX = cameraX,
 *             cameraY = cameraY,
 *             discoveredCells = map.discoveredCells + key(cell.x, cell.y)
 *         ),
 *         updatedProgress
 *     )
 * }
 *
 * ================================================================================
 * NOTATKI
 * ================================================================================
 *
 * 1. Zmęczenie zawsze jest zaokrąglone do Int i ograniczone (0, maxFatigue)
 * 2. Jezioro bez drogi zwraca travelTimeMinutes = 0 (niemożliwe)
 * 3. messageLog zawiera szczegółowy opis wszystkich obliczeń
 * 4. Wytrzymałość > 10 zmniejsza zmęczenie, < 10 zwiększa
 * 5. Tempo BIEGIEM jest zablokowane na nieznanym terenie
 *
 * ================================================================================
 */
