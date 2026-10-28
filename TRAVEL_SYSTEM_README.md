# 🗺️ Mechanika Podróży na Mapie Kafelkowej

## Przegląd Projektu

Stworzono kompleksowy system obliczania czasu i zmęczenia dla podróży bohatera po mapie kafelkowej o rozmiarze **5 km na kafelek**.

### 📦 Utworzone Komponenty

| Plik | Typ | Rozmiar | Przeznaczenie |
|------|-----|---------|---------------|
| `TravelMechanic.kt` | Kod źródłowy | 6.5 KB | Logika obliczeniowa |
| `TRAVEL_MECHANICS_DOCS.md` | Dokumentacja | 6.6 KB | Specyfikacja techniczna |
| `TRAVEL_INTEGRATION_EXAMPLE.kt` | Przykład kodu | 7.1 KB | Integracja z UI |
| `TRAVEL_MECHANICS_TESTS.kt` | Testy | 9.5 KB | Weryfikacja logiki |
| `TRAVEL_MECHANICS_IMPLEMENTATION_GUIDE.md` | Przewodnik | 8.9 KB | Krok po kroku |

**Razem: ~38 KB dokumentacji i kodu gotowego do implementacji**

---

## ⚡ Szybki Start

### 1. Plik główny: `TravelMechanic.kt`
```kotlin
// Enum tempa
enum class TravelPace {
    CAUTIOUS("Ostrożne", 0.5f, 2f),
    NORMAL("Normalne", 1.0f, 4f),
    RUNNING("Biegiem", 2.0f, 12f)
}

// Wynik obliczenia
data class TravelResult {
    val pace: TravelPace
    val travelTimeHours: Float
    val travelTimeMinutes: Int
    val fatigueGained: Int
    val messageLog: String
}

// Główna logika
object TravelMechanics {
    fun calculateTravelTile(
        heroData: String,
        pace: TravelPace,
        targetCell: TravelMapCell,
        isDiscovered: Boolean,
        isOnRoad: Boolean
    ): TravelResult { ... }
}
```

### 2. Integracjaz HeroTravelMapScreen

```kotlin
// Wybór tempa
var selectedTravelPace by remember { mutableStateOf(TravelPace.NORMAL) }

// Obliczenie
val travelResult = TravelMechanic.calculateTravelTile(
    heroData = readFromFile("hero_$heroId"),
    pace = selectedTravelPace,
    targetCell = cell,
    isDiscovered = key(cell.x, cell.y) in map.discoveredCells,
    isOnRoad = key(cell.x, cell.y) in map.roadCells
)

// Zastosowanie
val updatedProgress = TravelMechanic.applyTravelToProgress(
    currentProgress = heroProgress,
    travelResult = travelResult,
    readFromFile = ::readFromFile
)
```

---

## 📋 Reguły Mechaniki

### 1️⃣ TEMPO PODRÓŻY

| Tempo | Prędkość | Zmęczenie/km | Opis |
|-------|----------|--------------|------|
| **Ostrożne** | Sz/2 | 2 pkt | Nieznany teren, rekonesans |
| **Normalne** | Sz | 4 pkt | Standardowa podróż |
| **Biegiem** | 2×Sz | 12 pkt | Szybka ucieczka (blokada na nieznanym) |

### 2️⃣ MODYFIKATORY BIOMÓW (5 km dystans)

```
NA DRODZE: ×0.8 (20% szybciej) - Ignoruje kary biomu
BEZ DROGI:
  • Łąka (MEADOW)    → ×1.0
  • Las (FOREST)     → ×1.5
  • Skały (ROCKS)    → ×2.0
  • Bagno (SWAMP)    → ×3.0
  • Jezioro (LAKE)   → ❌ NIEMOŻLIWE bez drogi
```

### 3️⃣ ODKRYWANIE NIEZNANEGO KAFELKA

Jeśli `isDiscovered = false`:
- ⏱️ **+2.0 godziny** (rekonesans)
- 😤 **+15 pkt zmęczenia** (stres i czujność)

Dodatkowo:
- Tempo **BIEGIEM** na nieznanych kafelkach → automatycznie **NORMAL**

### 4️⃣ WPŁYW WYTRZYMAŁOŚCI

**Wzór:** `modifier = 1.1 - (Wytrzymałość × 0.01)`

| Wyt | Modifier | Efekt |
|-----|----------|-------|
| 5 | 1.05 | +5% zmęczenia |
| 10 | 1.00 | Brak zmian |
| 15 | 0.95 | -5% zmęczenia |
| 20 | 0.90 | -10% zmęczenia |
| 30 | 0.80 | -20% zmęczenia |

---

## 🧮 Przykłady Obliczeń

### Przykład 1: Normalne tempo na łące (odkrytej)
```
Hero: Sz=10, Wt=12
Czas = 5 km / 10 km/h × 1.0 (łąka) = 30 min
Zmęczenie = 5 × 4 × 0.88 = 17.6 ≈ 18 pkt
```

### Przykład 2: Ostrożne na lesie (nieznanym)
```
Hero: Sz=10, Wt=12
Czas = 5 km / 5 km/h × 1.5 (las) = 90 min + 120 min (odkrycie) = 210 min
Zmęczenie = 5 × 2 × 0.88 + 15 = 24 pkt
```

### Przykład 3: Biegiem na skałach
```
Hero: Sz=10, Wt=12
Czas = 5 km / 20 km/h × 2.0 (skały) = 30 min
Zmęczenie = 5 × 12 × 0.88 = 52.8 ≈ 53 pkt
```

### Przykład 4: Droga na drodze
```
Hero: Sz=10, Wt=12
Czas = 5 km / 10 km/h × 0.8 (droga) = 24 min
Zmęczenie = 5 × 4 × 0.88 = 18 pkt (brak kary biomu!)
```

---

## 🔍 Struktura Danych

### `TravelPace` (Enum)
```kotlin
enum class TravelPace(
    val displayName: String,      // "Ostrożne", "Normalne", "Biegiem"
    val speedMultiplier: Float,   // 0.5f, 1.0f, 2.0f
    val fatiguePerKm: Float       // 2f, 4f, 12f
)
```

### `TravelResult` (Data Class)
```kotlin
data class TravelResult(
    val pace: TravelPace,                // Wybrane tempo
    val distanceKm: Float = 5f,          // Dystans kafelka
    val biome: TravelBiome,              // Typ terenu
    val isOnRoad: Boolean,               // Czy jest droga
    val isDiscovered: Boolean,           // Czy odkryty
    val travelTimeHours: Float,          // Czas w godzinach
    val travelTimeMinutes: Int,          // Czas w minutach
    val fatigueGained: Int,              // Zmęczenie (0-100)
    val messageLog: String               // Szczegóły obliczeń
)
```

### `TravelMechanics` (Object)
```kotlin
object TravelMechanics {
    fun calculateTravelTile(
        heroData: String,
        pace: TravelPace,
        targetCell: TravelMapCell,
        isDiscovered: Boolean,
        isOnRoad: Boolean
    ): TravelResult
    
    fun applyTravelToProgress(
        currentProgress: HeroProgress,
        travelResult: TravelResult,
        readFromFile: (String) -> String
    ): HeroProgress
}
```

---

## 🚀 Implementacja

### Faza 1: Preparacja
- ✅ Plik `TravelMechanic.kt` już istnieje i kompiluje się
- ✅ Wszystkie typy danych zdefiniowane

### Faza 2: Integracja UI
Patrz: `TRAVEL_INTEGRATION_EXAMPLE.kt`
- Dodaj selektor tempa (Row z przyciskami)
- Zmodyfikuj funkcję `moveTo()`
- Dodaj dialog z wynikami

### Faza 3: Testowanie
Patrz: `TRAVEL_MECHANICS_TESTS.kt`
- Test każdego tempa
- Test każdego biomu
- Test odkrywania terenu
- Test wytrzymałości

### Faza 4: Optymalizacja
- Dodaj ikony/emotikonki do messageLog
- Cachuj obliczenia
- Dodaj animacje przejścia

---

## 📊 Wyjście Programu

Gdy gracz przejdzie kafelek:
```
⚡ TEMPO: Normalne
🏃 Szybkość: 10 km/h
📏 Dystans: 5 km
🛣️ Bez drogi: Las (x1.5)
⏱️ Czas: 0h 45min (45min)
💪 Wytrzymałość: 12 (modifier x0.88)
😤 Zmęczenie: +18 pkt

⚠️ ODKRYWANIE: +2.0h czasu, +15 zmęczenia
```

---

## ⚙️ Obowiązkowe Warunki

- [x] Dystans kafelka = 5 km (stały)
- [x] Tempo zmienia prędkość i zmęczenie
- [x] Biomy modyfikują czas podróży
- [x] Droga zmniejsza czas o 20%
- [x] Jezioro bez drogi = niemożliwe
- [x] Wytrzymałość zmniejsza zmęczenie
- [x] Odkrycie terenu dodaje kary
- [x] BIEGIEM zablokowany na nieznanym terenie
- [x] Zmęczenie w zakresie [0, maxFatigue]
- [x] messageLog z szczegółami

---

## 📚 Pliki Dokumentacji

1. **TRAVEL_MECHANICS_DOCS.md** - Pełna specyfikacja
2. **TRAVEL_INTEGRATION_EXAMPLE.kt** - Kody do wklejenia
3. **TRAVEL_MECHANICS_IMPLEMENTATION_GUIDE.md** - Krok po kroku
4. **TRAVEL_MECHANICS_TESTS.kt** - Testy weryfikujące
5. **TRAVEL_SYSTEM_README.md** - Ten plik

---

## ✅ Status

| Komponent | Status | Notatka |
|-----------|--------|---------|
| Enum `TravelPace` | ✅ Gotowe | W `TravelMechanic.kt` |
| Data class `TravelResult` | ✅ Gotowe | W `TravelMechanic.kt` |
| Logika `TravelMechanics` | ✅ Gotowe | W `TravelMechanic.kt` |
| Kompilacja | ✅ Sukces | BUILD SUCCESSFUL |
| Integracjaz UI | ⏳ Do zrobienia | Patrz `TRAVEL_INTEGRATION_EXAMPLE.kt` |
| Testy | ⏳ Do zrobienia | Patrz `TRAVEL_MECHANICS_TESTS.kt` |
| Dokumentacja | ✅ Kompletna | 5 plików |

---

## 🎯 Następne Kroki

1. Przeczytaj `TRAVEL_INTEGRATION_EXAMPLE.kt`
2. Dodaj kod do `HeroTravelMapScreen`
3. Dodaj `TravelPace` selektor do UI
4. Zmodyfikuj `moveTo()` aby używała `TravelMechanic`
5. Przetestuj na emulatorze/urządzeniu
6. Uruchom testy z `TRAVEL_MECHANICS_TESTS.kt`

---

## 🔗 Powiązane Komponenty

- `HeroTravelMap.kt` - Gdzie będzie integracja
- `HeroProgress` - Model zmęczenia
- `TravelMapCell` - Komórka mapy
- `HeroLogic.getStatValue()` - Pobranie statystyk
- `HeroLogic.advanceTimeMinutesProgress()` - Zmiana czasu

---

**Ostatnia aktualizacja:** 2026-09-27  
**Wersja:** 1.0  
**Status:** ✅ Gotowe do implementacji
