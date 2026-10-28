/**
 * TESTY LOGIKI - TravelMechanic
 * =============================
 *
 * Uruchom aby weryfikować poprawność obliczeń.
 * Można kopiować do file -> new -> Kotlin File w Android Studio
 *
 * UWAGA: To nie jest test jednostkowy. To symulacja do weryfikacji ręcznej.
 */

// Przykład heroData (tekst z pliku hero_X.txt)
val heroDataExample = """
Imie: Aragorn
Szybkość: 10
Wytrzymałość: 14
POZIOM: 5
""".trimIndent()

// ============================================================================
// TEST 1: Normalne tempo na łące (odkrytej, bez drogi)
// ============================================================================
println("=" * 80)
println("TEST 1: Normalne tempo na łące (odkrytej, bez drogi)")
println("=" * 80)

/*
val result1 = TravelMechanic.calculateTravelTile(
    heroData = heroDataExample,
    pace = TravelPace.NORMAL,
    targetCell = TravelMapCell(x=5, y=5, biome=TravelBiome.MEADOW),
    isDiscovered = true,
    isOnRoad = false
)

println(result1.messageLog)
println()

OCZEKIWANE WYNIKI:
- Szybkość: 10 km/h (Sz × 1.0)
- Czas: 5 km / 10 km/h = 0.5h = 30 min
- Modyfikator biomu: 1.0 (łąka)
- Modyfikator Wyt: 1.1 - (14 × 0.01) = 1.1 - 0.14 = 0.96
- Zmęczenie: 5 km × 4 pkt/km × 0.96 = 19.2 ≈ 19 pkt
- Odkrycie: NIE (już odkryte)
- RAZEM: 30 min, 19 zmęczenia
*/

// ============================================================================
// TEST 2: Ostrożne tempo na lesie (nieznanym, bez drogi)
// ============================================================================
println("=" * 80)
println("TEST 2: Ostrożne tempo na lesie (nieznanym, bez drogi)")
println("=" * 80)

/*
val result2 = TravelMechanic.calculateTravelTile(
    heroData = heroDataExample,
    pace = TravelPace.CAUTIOUS,
    targetCell = TravelMapCell(x=3, y=3, biome=TravelBiome.FOREST),
    isDiscovered = false,
    isOnRoad = false
)

println(result2.messageLog)
println()

OCZEKIWANE WYNIKI:
- Szybkość: 5 km/h (10 × 0.5 dla tempa ostrożnego)
- Czas: 5 km / 5 km/h = 1.0h = 60 min
- Modyfikator biomu: 1.5 (las)
- Czas po biomu: 1.0h × 1.5 = 1.5h = 90 min
- Modyfikator Wyt: 0.96
- Zmęczenie: 5 km × 2 pkt/km × 0.96 = 9.6 ≈ 10 pkt
- Odkrycie: TAK (+2h czasu, +15 zmęczenia)
- RAZEM: 90 + 120 = 210 min (3h 30min), 10 + 15 = 25 zmęczenia
*/

// ============================================================================
// TEST 3: Biegiem na skałach (odkrytych, bez drogi)
// ============================================================================
println("=" * 80)
println("TEST 3: Biegiem na skałach (odkrytych, bez drogi)")
println("=" * 80)

/*
val result3 = TravelMechanic.calculateTravelTile(
    heroData = heroDataExample,
    pace = TravelPace.RUNNING,
    targetCell = TravelMapCell(x=7, y=2, biome=TravelBiome.ROCKS),
    isDiscovered = true,
    isOnRoad = false
)

println(result3.messageLog)
println()

OCZEKIWANE WYNIKI:
- Szybkość: 20 km/h (10 × 2.0 dla biegu)
- Czas: 5 km / 20 km/h = 0.25h = 15 min
- Modyfikator biomu: 2.0 (skały)
- Czas po biomu: 0.25h × 2.0 = 0.5h = 30 min
- Modyfikator Wyt: 0.96
- Zmęczenie: 5 km × 12 pkt/km × 0.96 = 57.6 ≈ 58 pkt
- Odkrycie: NIE
- RAZEM: 30 min, 58 zmęczenia
*/

// ============================================================================
// TEST 4: Biegiem na lesie (NIEZNANYM) - powinno przełączyć się na normalne!
// ============================================================================
println("=" * 80)
println("TEST 4: Biegiem na lesie (NIEZNANYM) - przełącza się na NORMAL")
println("=" * 80)

/*
val result4 = TravelMechanic.calculateTravelTile(
    heroData = heroDataExample,
    pace = TravelPace.RUNNING,  // Chcemy biegać!
    targetCell = TravelMapCell(x=1, y=1, biome=TravelBiome.FOREST),
    isDiscovered = false,  // ALE TO JEST NIEZNANE
    isOnRoad = false
)

println(result4.messageLog)
println()
println("SPRAWDZENIE: Czy pace przełączył się na NORMAL?")
println("result.pace = ${result4.pace}")
println()

OCZEKIWANE WYNIKI:
- Pace automatycznie zmienia się na NORMAL (nieznany teren!)
- Szybkość: 10 km/h (10 × 1.0 dla tempa normalnego)
- Czas: 5 km / 10 km/h = 0.5h = 30 min
- Modyfikator biomu: 1.5 (las)
- Czas po biomu: 0.5h × 1.5 = 0.75h = 45 min
- Modyfikator Wyt: 0.96
- Zmęczenie: 5 km × 4 pkt/km × 0.96 = 19.2 ≈ 19 pkt
- Odkrycie: TAK (+2h czasu, +15 zmęczenia)
- RAZEM: 45 + 120 = 165 min (2h 45min), 19 + 15 = 34 zmęczenia
- Ostrzeżenie w messageLog o przełączeniu na NORMAL
*/

// ============================================================================
// TEST 5: Droga przez bagno (odkryte)
// ============================================================================
println("=" * 80)
println("TEST 5: Droga przez bagno (odkryte, bez drogi)")
println("=" * 80)

/*
val result5 = TravelMechanic.calculateTravelTile(
    heroData = heroDataExample,
    pace = TravelPace.NORMAL,
    targetCell = TravelMapCell(x=6, y=4, biome=TravelBiome.SWAMP),
    isDiscovered = true,
    isOnRoad = false
)

println(result5.messageLog)
println()

OCZEKIWANE WYNIKI:
- Szybkość: 10 km/h (10 × 1.0)
- Czas: 5 km / 10 km/h = 0.5h = 30 min
- Modyfikator biomu: 3.0 (bagno - najtrudniejszy!)
- Czas po biomu: 0.5h × 3.0 = 1.5h = 90 min
- Modyfikator Wyt: 0.96
- Zmęczenie: 5 km × 4 pkt/km × 0.96 = 19.2 ≈ 19 pkt
- Odkrycie: NIE
- RAZEM: 90 min, 19 zmęczenia
*/

// ============================================================================
// TEST 6: Droga na drodze przez las (odkryty)
// ============================================================================
println("=" * 80)
println("TEST 6: Droga na drodze przez las (odkryty, Z DROGĄ)")
println("=" * 80)

/*
val result6 = TravelMechanic.calculateTravelTile(
    heroData = heroDataExample,
    pace = TravelPace.NORMAL,
    targetCell = TravelMapCell(x=4, y=4, biome=TravelBiome.FOREST),
    isDiscovered = true,
    isOnRoad = true  // DROGA!
)

println(result6.messageLog)
println()

OCZEKIWANE WYNIKI:
- Szybkość: 10 km/h
- Czas: 5 km / 10 km/h = 0.5h = 30 min
- Droga: TAK → ×0.8 (ignoruje karę biomu!)
- Czas po drodze: 30 min × 0.8 = 24 min
- Modyfikator Wyt: 0.96
- Zmęczenie: 5 km × 4 pkt/km × 0.96 = 19.2 ≈ 19 pkt
- Odkrycie: NIE
- RAZEM: 24 min, 19 zmęczenia
- NOTATKA: Droga znacznie przyspiesza podróż!
*/

// ============================================================================
// TEST 7: Jezioro bez drogi - NIEMOŻLIWE
// ============================================================================
println("=" * 80)
println("TEST 7: Jezioro bez drogi - NIEMOŻLIWE")
println("=" * 80)

/*
val result7 = TravelMechanic.calculateTravelTile(
    heroData = heroDataExample,
    pace = TravelPace.NORMAL,
    targetCell = TravelMapCell(x=8, y=6, biome=TravelBiome.LAKE),
    isDiscovered = true,
    isOnRoad = false
)

println(result7.messageLog)
println()
println("SPRAWDZENIE: Czy travelTimeMinutes = 0?")
println("result.travelTimeMinutes = ${result7.travelTimeMinutes}")
println()

OCZEKIWANE WYNIKI:
- travelTimeMinutes = 0
- fatigueGained = 0
- messageLog zawiera "❌ Jezioro jest nieprzejezdne bez drogi!"
- GRACZ POWINIEN ZOSTAĆ ZABLOKOWANY
*/

// ============================================================================
// TEST 8: Porównanie Wytrzymałości - wpływ na zmęczenie
// ============================================================================
println("=" * 80)
println("TEST 8: Wpływ Wytrzymałości na zmęczenie")
println("=" * 80)

/*
// Bohater słaby (Wt = 5)
val heroDataWeak = "Szybkość: 10\nWytrzymałość: 5"
val resultWeak = TravelMechanic.calculateTravelTile(
    heroData = heroDataWeak,
    pace = TravelPace.NORMAL,
    targetCell = TravelMapCell(0, 0, TravelBiome.MEADOW),
    isDiscovered = true,
    isOnRoad = false
)

// Bohater silny (Wt = 20)
val heroDataStrong = "Szybkość: 10\nWytrzymałość: 20"
val resultStrong = TravelMechanic.calculateTravelTile(
    heroData = heroDataStrong,
    pace = TravelPace.NORMAL,
    targetCell = TravelMapCell(0, 0, TravelBiome.MEADOW),
    isDiscovered = true,
    isOnRoad = false
)

println("Słaby (Wt=5):")
println("  Modifier: 1.1 - (5 × 0.01) = 1.05 (+5% zmęczenia)")
println("  Zmęczenie: 20 × 1.05 ≈ 21 pkt")
println(resultWeak.messageLog)
println()

println("Silny (Wt=20):")
println("  Modifier: 1.1 - (20 × 0.01) = 0.90 (-10% zmęczenia)")
println("  Zmęczenie: 20 × 0.90 = 18 pkt")
println(resultStrong.messageLog)
println()

OCZEKIWANE WYNIKI:
- Słaby bohater: ~21 zmęczenia
- Silny bohater: ~18 zmęczenia
- Różnica: ~3 punkty na korzyść silnego
*/

// ============================================================================
// PODSUMOWANIE
// ============================================================================
println("=" * 80)
println("PODSUMOWANIE TESTÓW")
println("=" * 80)
println("""
Jeśli wszystkie wyniki zgadzają się z oczekiwaniami, system jest poprawny!

Kluczowe punkty do sprawdzenia:
1. ✅ Modyfikator Wyt działa: 1.1 - (Wt × 0.01)
2. ✅ Tempo RUNNING przełącza się na NORMAL na nieznanym terenie
3. ✅ Jezioro bez drogi = niemożliwe (0 czasu, 0 zmęczenia)
4. ✅ Droga zmniejsza czas o 20% (×0.8)
5. ✅ Bagno jest najtrudniejsze (×3.0)
6. ✅ Odkrycie dodaje +2h i +15 zmęczenia
7. ✅ Biegiem zwiększa zmęczenie 3× (12 pkt/km vs 4 pkt/km)

Jeśli coś się nie zgadza, sprawdź logikę w TravelMechanic.kt!
""".trimIndent())
