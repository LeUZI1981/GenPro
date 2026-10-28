/**
 * PRZYKŁAD INTEGRACJI Z HeroTravelMapScreen
 * ==========================================
 *
 * Ten plik zawiera przykładowy kod pokazujący jak zintegrować TravelMechanic
 * z istniejącą funkcją HeroTravelMapScreen w pliku HeroTravelMap.kt
 *
 * KROKI INTEGRACJI:
 *
 * 1. Dodaj pole stanu dla wybranego tempa
 * 2. Dodaj UI do wyboru tempa
 * 3. Zmodyfikuj funkcję moveTo() aby używała TravelMechanic
 * 4. Pokaż wynik graczowi
 *
 * ================================================================================
 */

// KROK 1: Dodaj do stanu HeroTravelMapScreen
/*
var selectedTravelPace by remember { mutableStateOf(TravelPace.NORMAL) }
var showTravelSummary by remember { mutableStateOf(false) }
var lastTravelResult by remember { mutableStateOf<TravelResult?>(null) }
*/

// KROK 2: Dodaj selector tempa (UI)
/*
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
                containerColor = if (selectedTravelPace == pace) Color(0xFFDAA520) else Color.DarkGray
            ),
            modifier = Modifier.weight(1f).padding(2.dp)
        ) {
            Text(pace.displayName, fontSize = 12.sp)
        }
    }
}
*/

// KROK 3: Zmodyfikuj moveTo() aby używała TravelMechanic
/*
fun moveTo(cell: TravelMapCell) {
    if (abs(cell.x - map.heroX) + abs(cell.y - map.heroY) != 1) return
    
    // Istniejący kod przesuwania kamery...
    var cameraX = map.cameraX
    var cameraY = map.cameraY
    if (cell.x == cameraX) cameraX = (cell.x - 1).coerceAtLeast(0)
    if (cell.x == cameraX + VIEW_SIZE - 1) cameraX = (cell.x - 3).coerceAtMost(map.width - VIEW_SIZE)
    if (cell.y == cameraY) cameraY = (cell.y - 1).coerceAtLeast(0)
    if (cell.y == cameraY + VIEW_SIZE - 1) cameraY = (cell.y - 3).coerceAtMost(map.height - VIEW_SIZE)

    // ===== NOWY KOD: Oblicz wpływ podróży =====
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
    
    // Zastosuj wpływ do HeroProgress
    val updatedProgress = TravelMechanic.applyTravelToProgress(
        currentProgress = heroProgress,
        travelResult = travelResult,
        readFromFile = ::readFromFile
    )
    
    // Przechowaj wynik dla wyświetlenia
    lastTravelResult = travelResult
    showTravelSummary = true
    
    // ===== KONIEC NOWEGO KODU =====

    // Aktualizuj mapę i postęp
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
*/

// KROK 4: Pokaż dialog z wynikami
/*
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
            Column {
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
*/

// ================================================================================
// PRZYKŁAD WYJŚCIA (messageLog):
// ================================================================================
/*
⚡ TEMPO: Normalne
🏃 Szybkość: 10 km/h
📏 Dystans: 5 km
🛣️ Bez drogi: Las (x1.5)
⏱️ Czas: 0h 45min (45min)
💪 Wytrzymałość: 12 (modifier x0.88)
😤 Zmęczenie: +18 pkt
⚠️ ODKRYWANIE: +2.0h czasu, +15 zmęczenia
*/

// ================================================================================
// REGUŁY BEZPIECZEŃSTWA
// ================================================================================

// 1. Jeśli gracz spróbuje wejść na LAKE bez drogi:
//    - Zostaje zablokowany
//    - Wyświetla się komunikat błędu
//    - Ruch się nie przeprowadza

// 2. Jeśli gracz wbiegnie w nieznany teren z RUNNING:
//    - Automatycznie przełącza się na NORMAL
//    - Wyświetla się ostrzeżenie w messageLog

// 3. Zmęczenie zawsze jest w zakresie [0, maxFatigue]
//    - Nie może ujemnie spaść
//    - Nie może przekroczyć maksimum

// ================================================================================
// TESTY JEDNOSTKOWE (opcjonalnie)
// ================================================================================

/*
class TravelMechanicsTest {
    
    @Test
    fun testNormalPaceOnMeadow() {
        val heroData = "Sz: 10\nWt: 12"
        val result = TravelMechanic.calculateTravelTile(
            heroData = heroData,
            pace = TravelPace.NORMAL,
            targetCell = TravelMapCell(0, 0, TravelBiome.MEADOW),
            isDiscovered = true,
            isOnRoad = false
        )
        
        // 5km / 10km/h = 0.5h = 30 min
        // Zmęczenie: 5 * 4 * 0.88 = 17.6 ≈ 18
        assertTrue(result.travelTimeMinutes in 28..32)
        assertTrue(result.fatigueGained in 16..20)
    }
    
    @Test
    fun testRunningOnUnknownForestBlocked() {
        val heroData = "Sz: 10\nWt: 10"
        val result = TravelMechanic.calculateTravelTile(
            heroData = heroData,
            pace = TravelPace.RUNNING,
            targetCell = TravelMapCell(0, 0, TravelBiome.FOREST),
            isDiscovered = false,
            isOnRoad = false
        )
        
        // Powinno przełączyć się na NORMAL, a nie RUNNING
        assertEquals(result.pace, TravelPace.NORMAL)
    }
    
    @Test
    fun testLakeWithoutRoadImpassable() {
        val heroData = "Sz: 10\nWt: 10"
        val result = TravelMechanic.calculateTravelTile(
            heroData = heroData,
            pace = TravelPace.NORMAL,
            targetCell = TravelMapCell(0, 0, TravelBiome.LAKE),
            isDiscovered = true,
            isOnRoad = false
        )
        
        // Powinno zwrócić 0 czasu
        assertEquals(result.travelTimeMinutes, 0)
        assertEquals(result.fatigueGained, 0)
    }
}
*/
