# Implementacja wrzucania monety do studni/fontanny

Dodanie mechaniki wrzucania wybranej monety (złotej, srebrnej lub miedzianej) do studni lub fontanny w widoku budynku. Zadanie obejmuje modyfikację UI oraz logiki odejmowania monet z pliku postaci.

## Proponowane Zmiany

### [Component Name] app

#### [MODIFY] [BuildingsScreen.kt](file:///C:/Users/Lesze/AndroidStudioProjects/GenPro/app/src/main/java/com/example/genpro/BuildingsScreen.kt)
- Dodanie stanu `showCoinTossDialog` do zarządzania widocznością okna wrzucania monety.
- Aktualizacja funkcji `BuildingView` o opcjonalny przycisk "Wrzuć monetę", widoczny tylko dla obiektów typu "Studnia" lub "Fontanna".
- Implementacja `CoinTossDialog`, który:
    - Pobiera aktualny stan sakiewki gracza za pomocą `HeroLogic.parseMoney`.
    - Wyświetla opcje wrzucenia monety (Złoto, Srebro, Miedź) wraz z informacją o posiadanej ilości.
    - Dezaktywuje opcje dla monet, których gracz nie posiada.
    - Po potwierdzeniu, odejmuje wybraną monetę za pomocą `HeroLogic.updateMoney` i zapisuje zmiany do pliku.
    - Wyświetla komunikat (Toast) o wykonanej akcji.

## Plan Weryfikacji

### Testy Manualne
1. Wejście do miasta i znalezienie Studni lub Fontanny (jeśli nie ma, wygenerowanie nowej osady lub skorzystanie z narzędzi GM).
2. Otwarcie widoku obiektu (Studnia/Fontanna).
3. Sprawdzenie, czy przycisk "Wrzuć monetę" jest widoczny.
4. Otwarcie dialogu i próba wrzucenia monety, której postać nie posiada (przycisk powinien być zablokowany).
5. Wrzucenie posiadanej monety (np. miedzianej) i zweryfikowanie w pliku postaci/karcie postaci, czy liczba monet uległa zmianie.
6. Sprawdzenie, czy czas w grze nie upływa nieprawidłowo (chyba że ustalimy inaczej, na razie samo odjęcie monety).
