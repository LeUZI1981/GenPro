# Mechanika Podróży - Przewodnik Implementacji

## 📋 Przegląd

Stworzony system `TravelMechanic` oblicza realny wpływ podróży na mapie kafelkowej (5 km za kafelek) na podstawie:
- **Tempa podróży** (Ostrożne/Normalne/Biegiem)
- **Szybkości bohatera** (Sz)
- **Wytrzymałości bohatera** (Wt)
- **Bioma kafelka** (Las, Łąka, Skały, Bagno, Jezioro)
- **Obecności drogi**
- **Statusu odkrycia terenu**

---

## 🗂️ Utworzone Pliki

### 1. **TravelMechanic.kt** (Plik główny - 6.5 KB)
Zawiera:
- `enum class TravelPace` - Definicje tempa (CAUTIOUS, NORMAL, RUNNING)
- `data class TravelResult` - Wyniki obliczenia podróży
- `object TravelMechanics` - Logika obliczeniowa

**Lokalizacja:** `app/src/main/java/com/example/genpro/TravelMechanic.kt`

### 2. **TRAVEL_MECHANICS_DOCS.md** (Dokumentacja - 6.6 KB)
Pełna specyfikacja:
- Reguły tempa podróży
- Modyfikatory biomów
- Wpływ wytrzymałości
- Przykłady użycia
- Formaty danych

### 3. **TRAVEL_INTEGRATION_EXAMPLE.kt** (Przykład kodu - 7.1 KB)
Ilustruje integrację z `HeroTravelMapScreen`:
- Jak dodać UI dla wyboru tempa
- Jak modyfikować funkcję `moveTo()`
- Jak wyświetlić wyniki graczowi
- Testy jednostkowe

---

## 🚀 Implementacja (Kroki)

### Krok 1: Sprawdzenie Kompilacji
✅ **GOTOWE** - Plik `TravelMechanic.kt` kompiluje się bez błędów

```
BUILD SUCCESSFUL in 6s
```

### Krok 2: Zintegruj z HeroTravelMapScreen

Otwórz **HeroTravelMap.kt** i znajdź funkcję `HeroTravelMapScreen`.

#### 2a. Dodaj import
```kotlin
import com.example.genpro.TravelMechanic  // Jeśli w innym pakiecie
```

#### 2b. Dodaj stany
```kotlin
@Composable
fun HeroTravelMapScreen(
    // ... istniejące parametry ...
) {
    val context = LocalContext.current
    val mapFile = HeroTravelMapGenerator.fileName(heroId)
    
    // ... istniejący kod ...
    
    // DODAJ TO:
    var selectedTravelPace by remember { mutableStateOf(TravelPace.NORMAL) }
    var showTravelSummary by remember { mutableStateOf(false) }
    var lastTravelResult by remember { mutableStateOf<TravelResult?>(null) }
```

#### 2c. Dodaj UI selektor tempa
```kotlin
// Dodaj przed siatką mapy
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    TravelPace.values().forEach { pace ->
        Button(
            onClick = { selectedTravelPace = pace },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedTravelPace == pace) 
                    Color(0xFFDAA520) else Color.DarkGray
            ),
            modifier = Modifier.weight(1f).padding(2.dp)
        ) {
            Text(pace.displayName, fontSize = 12.sp)
        }
    }
}
```

#### 2d. Zmodyfikuj funkcję moveTo()
Znajdź i zamień część z symboem `✦`:

```kotlin
fun moveTo(cell: TravelMapCell) {
    if (abs(cell.x - map.heroX) + abs(cell.y - map.heroY) != 1) return
    
    // Istniejący kod przesuwania kamery
    var cameraX = map.cameraX
    var cameraY = map.cameraY
    if (cell.x == cameraX) cameraX = (cell.x - 1).coerceAtLeast(0)
    if (cell.x == cameraX + VIEW_SIZE - 1) cameraX = (cell.x - 3).coerceAtMost(map.width - VIEW_SIZE)
    if (cell.y == cameraY) cameraY = (cell.y - 1).coerceAtLeast(0)
    if (cell.y == cameraY + VIEW_SIZE - 1) cameraY = (cell.y - 3).coerceAtMost(map.height - VIEW_SIZE)

    // ===== NOWY KOD =====
    val heroData = readFromFile("hero_$heroId")
    val isOnRoad = key(cell.x, cell.y) in map.roadCells
    val wasDiscovered = key(cell.x, cell.y) in map.discoveredCells
    
    val travelResult = TravelMechanic.calculateTravelTile(
        heroData = heroData,
        pace = selectedTravelPace,
        targetCell = cell,
        isDiscovered = wasDiscovered,
        isOnRoad = isOnRoad
    )
    
    // Jeśli jezioro bez drogi - zablokuj
    if (travelResult.travelTimeMinutes == 0) {
        Toast.makeText(context, "❌ Jezioro jest nieprzejezdne bez drogi!", 
            Toast.LENGTH_SHORT).show()
        return
    }
    
    val updatedProgress = TravelMechanic.applyTravelToProgress(
        currentProgress = heroProgress,
        travelResult = travelResult,
        readFromFile = ::readFromFile
    )
    
    lastTravelResult = travelResult
    showTravelSummary = true
    // ===== KONIEC NOWEGO KODU =====

    persist(
        map.copy(
            heroX = cell.x,
            heroY = cell.y,
            cameraX = cameraX,
            cameraY = cameraY,
            discoveredCells = map.discoveredCells + key(cell.x, cell.y)
        ),
        updatedProgress
    )
}
```

#### 2e. Dodaj dialog z wynikami
```kotlin
// Dodaj na koniec Box() w HeroTravelMapScreen
if (showTravelSummary && lastTravelResult != null) {
    AlertDialog(
        onDismissRequest = { showTravelSummary = false },
        containerColor = Color(0xFF1A1A1A),
        title = { 
            Text(
                "📍 Podróż",
                color = Color(0xFFDAA520),
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    lastTravelResult!!.messageLog,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { showTravelSummary = false }) {
                Text("OK", color = Color(0xFFDAA520), fontWeight = FontWeight.Bold)
            }
        }
    )
}
```

---

## 📊 Przykład Wyniku (Output)

Gdy gracz przejdzie na kafelek lasu bez drogi, nie odkryty:

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

## 🔍 Weryfikacja Obliczeń

### Test 1: Normalne tempo na łące (odkrytej)
```
Szybkość = 10 km/h
Wytrzymałość = 12
Czas = 5 / 10 * 1.0 = 0.5h = 30 min
Zmęczenie = 5 * 4 * (1.1 - 0.12) = 5 * 4 * 0.98 = 19.6 ≈ 20 pkt
```

### Test 2: Biegiem na nieznanym lesie
```
Szybkość = 10 km/h
Pace = RUNNING → przełącza się na NORMAL (nieznany teren!)
Czas = 5 / 10 * 1.5 = 0.75h = 45 min + 2.0h (odkrycie) = 2h 45 min
Zmęczenie = 5 * 4 * 0.88 = 17.6 ≈ 18 + 15 (odkrycie) = 33 pkt
```

### Test 3: Jezioro bez drogi
```
Rezultat = niemożliwe do przejścia
Czas = 0
Zmęczenie = 0
Komunikat = "❌ Jezioro jest nieprzejezdne bez drogi!"
```

---

## 🛣️ Modyfikatory Biomów - Podsumowanie

| Biom | Bez drogi | Na drodze | Przejezdne |
|------|-----------|-----------|-----------|
| **Łąka** | ×1.0 | ×0.8 | ✅ Tak |
| **Las** | ×1.5 | ×0.8 | ✅ Tak |
| **Skały** | ×2.0 | ×0.8 | ✅ Tak |
| **Bagno** | ×3.0 | ×0.8 | ✅ Tak |
| **Jezioro** | ❌ NIE | ×0.8 | ⚠️ Tylko z drogą |
| **Osada** | ×0.5 | ×0.8 | ✅ Tak |

---

## ⚙️ Wzory Matematyczne

### Tempo podróży
```
Czas [h] = Dystans [km] / (Szybkość × SpeedMultiplier)
```

### Modyfikator biomu
```
Czas [h] = Czas × BiomeModifier × (Droga ? 0.8 : 1.0)
```

### Zmęczenie (przed karami)
```
BaseFatigue = Dystans × FatiguePerKm × (1.1 - Wyt × 0.01)
```

### Zmęczenie (po karach)
```
TotalFatigue = BaseFatigue + (IsDiscovered ? 15 : 0)
Clamp(0, MaxFatigue)
```

---

## ✅ Checklist Implementacji

- [ ] Odczytaj plik `TravelMechanic.kt` i upewnij się, że kompiluje się
- [ ] Dodaj importy do `HeroTravelMap.kt`
- [ ] Dodaj stany (`selectedTravelPace`, `showTravelSummary`, `lastTravelResult`)
- [ ] Dodaj UI selektor tempa (Row z przyciskami)
- [ ] Zmodyfikuj `moveTo()` aby używała `TravelMechanic.calculateTravelTile()`
- [ ] Dodaj check dla jeziora bez drogi
- [ ] Dodaj `AlertDialog` z wynikami
- [ ] Przetestuj na emulatorze/urządzeniu
- [ ] Sprawdź wyniki dla różnych biomów i tempa

---

## 🐛 Potencjalne Problemy

### Problem: "Cannot find TravelPace"
**Rozwiązanie:** Upewnij się, że `TravelMechanic.kt` jest w tym samym pakiecie co inne pliki

### Problem: "moveTo() jest wewnętrzna funkcja"
**Rozwiązanie:** `moveTo()` jest zdefiniowana wewnątrz `HeroTravelMapScreen`. Dodaj kod bezpośrednio do tej funkcji.

### Problem: Zmęczenie się nie zmienia
**Rozwiązanie:** Upewnij się, że `persist()` jest wywołane z nowym `updatedProgress`

---

## 📚 Dokumentacja Referencji

- `TravelMechanic.kt` - Kod źródłowy (6.5 KB)
- `TRAVEL_MECHANICS_DOCS.md` - Dokumentacja techniczna
- `TRAVEL_INTEGRATION_EXAMPLE.kt` - Przykład kodu

---

**Status:** ✅ Gotowe do implementacji
**Kompilacja:** ✅ BUILD SUCCESSFUL
**Testowanie:** ⏳ Czeka na integrację z UI
