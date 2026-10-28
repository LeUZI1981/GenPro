/**
 * ❌ DEPRECATED - Ten plik jest już nieużywany
 * 
 * Stary system podróży (TravelActivity) został zastąpiony nowoczęsnym systemem eksploracyjnym.
 * 
 * NOWY SYSTEM: HeroTravelMap.kt + TravelMechanic.kt
 * ================================================
 * - Mapa eksploracyjna (kafelki 5x5 z widokiem)
 * - Zaawansowana mechanika zmęczenia
 * - System biomów (Las, Łąka, Jezioro, Bagno, Skały)
 * - System dróg i ślepych zaułków
 * - Blokada ruchu przy zmęczeniu >= 90%
 * - Menu obozowiska z ograniczoną funkcjonalnością przy wysokim zmęczeniu
 * 
 * ZMIENIONE REFERENCJE:
 * ====================
 * 1. MainActivity.kt (linia ~1410) - Wznowienie podróży
 * 2. MainActivity.kt (linia ~2563) - Podróż do węzła
 * 3. Maps.kt (linia ~605) - Podróż między węzłami
 * 
 * Wszystkie Intent(TravelActivity) → Intent(MainActivity) + EXTRA_MENU_APP = 61
 * 
 * ANDROIDMANIFEST.XML:
 * ====================
 * Usunięta rejestracja: <activity android:name=".TravelActivity" ... />
 * 
 * AKCJA:
 * ======
 * Plik TravelActivity.kt może być bezpiecznie usunięty z projektu.
 * Zastąpiono go plikiem TravelActivity_DEPRECATED.kt (tylko dokumentacja).
 */
