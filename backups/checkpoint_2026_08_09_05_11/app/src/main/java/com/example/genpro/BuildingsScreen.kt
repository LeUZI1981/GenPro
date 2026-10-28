package com.example.genpro

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import kotlinx.serialization.json.Json
import java.util.Locale

@Composable
fun BuildingsScreen(
    heroDataInitial: String,
    selectedCity: String,
    currentFile: String,
    dataNowInitial: String,
    initialShopType: String? = null,
    onBack: () -> Unit,
    onNavigate: (Int) -> Unit,
    playSoundAction: (Int, Int) -> Unit,
    saveToFile: (String, String) -> Unit,
    settlementData: String = ""
) {
    val nosiferFamily = remember { FontFamily(Font(R.font.nosifer)) }
    var heroData by remember { mutableStateOf(heroDataInitial) }
    var dataNow by remember { mutableStateOf(dataNowInitial) }
    val context = LocalContext.current

    val settlement = remember(settlementData) {
        if (settlementData.isNotEmpty()) {
            try {
                Json.decodeFromString<SettlementInstance>(settlementData)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    val gold = HeroLogic.getHeroGold(heroData)
    val silver = HeroLogic.getHeroSilver(heroData)
    val copper = HeroLogic.getHeroCopper(heroData)

    val buildings = remember(initialShopType) {
        val all = listOf(
            Shop("Karczma", "Jedzenie, Picie i Nocleg", listOf("Jedzenie", "Trunki", "Pokoje"), "Najstarsza karczma w mieście, znana z dobrego piwa."),
            Shop("Tawerna", "Lokalna Kuchnia i Plotki", listOf("Jedzenie", "Trunki"), "Miejsce spotkań lokalnych rzemieślników."),
            Shop("Płatnerz", "Zbroje i Tarcze", listOf("Zbroje", "Tarcze"), "Szczęk młota o kowadło niesie się po okolicy."),
            Shop("Kowal", "Broń i Narzędzia", listOf("Broń", "Narzędzia"), "Gęsty dym wydobywa się z paleniska."),
            Shop("Rymarz", "Skóry i Rząd Koński", listOf("Skóry", "Akcesoria"), "Zapach garbowanej skóry jest tu wszechobecny."),
            Shop("Stajnia", "Konie i Wierzchowce", listOf("Konie", "Transport", "Usługi"), "Miejsce gdzie możesz nabyć lub zostawić konia."),
            Shop("Apteka", "Zioła i Mikstury", listOf("Zioła", "Mikstury"), "Zapach ziół czuć już z ulicy."),
            Shop("Mieszkanie", "Wynajem i Kupno", listOf("Pokoje"), "Kamienica z mieszkaniami na wynajem."),
            Shop("Zamtuz", "Przybytek Rozkoszy", listOf("Trunki", "Rozrywka", "Usługi"), "Dyskretny przybytek oferujący różnorodne uciechy.")
        )
        when (initialShopType) {
            "Lodging" -> all.filter { it.name in listOf("Karczma", "Tawerna", "Mieszkanie", "Zamtuz") }
            "KOWAL" -> all.filter { it.name in listOf("Kowal") }
            "ZBROJMISTRZ" -> all.filter { it.name in listOf("Płatnerz") }
            "RYMARZ" -> all.filter { it.name in listOf("Rymarz") }
            "STAJNIA" -> all.filter { it.name in listOf("Stajnia") }
            "Consumption" -> all.filter { it.name in listOf("Karczma", "Tawerna", "Oberża") }
            "Craft" -> all.filter { it.name in listOf("Płatnerz", "Kowal", "Rymarz", "Stajnia", "Apteka") }
            else -> all
        }
    }

    var selectedBuildingShop by remember(buildings) { mutableStateOf(buildings.firstOrNull()) }
    var generatedBuilding by remember { mutableStateOf<BuildingInstance?>(null) }
    var currentRoom by remember { mutableStateOf<Room?>(null) }

    LaunchedEffect(initialShopType) {
        if (initialShopType != null && initialShopType.contains("_DIRECT:")) {
            val parts = initialShopType.split(":")
            if (parts.size >= 3) {
                val (bType, bName) = if (parts.size >= 4) {
                    parts[2] to parts[3]
                } else {
                    parts[1] to parts[2]
                }
                
                // First try to find in current settlement zones
                var instance: BuildingInstance? = null
                settlement?.zones?.forEach { zone ->
                    if (zone.specialBuilding.name == bName || zone.specialBuilding.type == bType) {
                        instance = zone.specialBuilding
                    } else if (zone.inn.name == bName || zone.inn.type == bType) {
                        instance = zone.inn
                    }
                }

                if (instance == null) {
                    instance = BuildingGenerator.generateBuilding(bType, customName = bName)
                }
                
                generatedBuilding = instance
                currentRoom = instance?.rooms?.firstOrNull()
                
                // Find matching shop template
                selectedBuildingShop = buildings.find { it.name.equals(bType, ignoreCase = true) } 
                    ?: buildings.find { bType.contains(it.name, ignoreCase = true) }
                    ?: buildings.firstOrNull()
            }
        }
    }
    
    val buildingInventories = remember { mutableStateMapOf<String, List<TradeItem>>() }

    var buyMode by remember { mutableStateOf(true) }
    var showShopOverlay by remember { mutableStateOf(false) }
    var workHours by remember { mutableFloatStateOf(4f) }
    var workEvent by remember { mutableStateOf<HeroLogic.WorkResult?>(null) }
    var showWorkEvent by remember { mutableStateOf(false) }
    var showWorkPanel by remember { mutableStateOf(false) }
    var showRestPanel by remember { mutableStateOf(false) }
    var restHours by remember { mutableFloatStateOf(8f) }
    var restDays by remember { mutableIntStateOf(1) }

    var showBurglaryDialog by remember { mutableStateOf(false) }
    var burglaryTargetRoom by remember { mutableStateOf<Room?>(null) }
    var burglaryResult by remember { mutableStateOf<HeroLogic.BurglaryResult?>(null) }
    var showBurglaryResult by remember { mutableStateOf(false) }

    val initialFilter = when(initialShopType) {
        "Lodging" -> "Nocleg"
        "Consumption" -> "Konsumpcja"
        "Entertainment" -> "Rozrywka"
        "KOWAL" -> "Broń"
        "ZBROJMISTRZ" -> "Zbroje"
        "RYMARZ" -> "Skóry"
        "STAJNIA" -> "Konie"
        else -> "Wszystkie"
    }
    var selectedCategoryFilter by remember { mutableStateOf(initialFilter) }
    var manualCoinSelection by remember { mutableStateOf(false) }
    var pendingItem by remember { mutableStateOf<TradeItem?>(null) }
    
    var payGold by remember { mutableIntStateOf(0) }
    var paySilver by remember { mutableIntStateOf(0) }
    var payCopper by remember { mutableIntStateOf(0) }

    val weightLimit = HeroLogic.getHeroWeightLimit(heroData)
    val currentWeight = HeroLogic.calculateCurrentWeight(heroData)

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).padding(horizontal = 18.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            //IconButton(onClick = onBack) { Text("⬅️", color = Color.White, fontSize = 24.sp) }
            Spacer(modifier = Modifier.width(2.dp))
            IconButton(onClick = { onNavigate(35) }) { Text("🏘️", fontSize = 32.sp) }
            Spacer(modifier = Modifier.weight(1f))
            
            Column(horizontalAlignment = Alignment.End) {
                Text(dataNow.substringAfterLast("|").trim(), color = Color.Gray, fontSize = 14.sp)
                val currentFatigue = HeroLogic.getHeroFatigue(heroData)
                val fatigueColor = when {
                    currentFatigue > 60 -> Color.Red
                    currentFatigue > 30 -> Color.Yellow
                    else -> Color.Green
                }
                Text("Zmęczenie: $currentFatigue%", color = fatigueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Portfel i Obciążenie
        val encumbrance = HeroLogic.getEncumbrance(currentWeight, weightLimit)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
                Row {
                    WalletItem("⛀", gold, Color(0xFFFFD700))
                    Spacer(modifier = Modifier.width(12.dp))
                    WalletItem("⛀", silver, Color(0xFFC0C0C0))
                    Spacer(modifier = Modifier.width(12.dp))
                    WalletItem("⛀", copper, Color(0xFFCD7F32))
                }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("Waga: $currentWeight/$weightLimit", color = encumbrance.color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(encumbrance.label, color = encumbrance.color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (generatedBuilding == null) {
            // Wybór budynku do wejścia
            //Text("Wybierz budynek, do którego chcesz wejść:", color = Color.White, modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(buildings) { b ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedBuildingShop = b
                            val instance = BuildingGenerator.generateBuilding(b.name)
                            generatedBuilding = instance
                            currentRoom = instance.rooms[0] // Wejście do Lobby/Sali głównej
                            playSoundAction(R.raw.klik1, 0)
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(b.name, color = Color.Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(b.description, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            // Wnętrze budynku
            val building = generatedBuilding!!
            val room = currentRoom!!

            Column(modifier = Modifier.fillMaxSize()) {
                Text(building.name.uppercase(), color = Color.Yellow, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(building.address, color = Color.Gray, fontSize = 12.sp)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(room.name, color = Color.Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Przestronne pomieszczenie o powierzchni ok. ${String.format(Locale.GERMANY, "%.1f", room.area)} m². ${getRoomContextDescription(room, building)}",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AKCJE W POMIESZCZENIU
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val canTrade = room.name in listOf("Sala Główna", "Bar / Szynk", "Recepcja i Lobby", "Sala sprzedaży", "Kuźnia", "Warsztat płatnerski", "Pracownia rymarska", "Główna stajnia", "Sklepik frontowy")
                    val canWork = (canTrade && building.type in listOf("Karczma", "Tawerna", "Zamtuz")) ||
                                 (room.name in listOf("Kuźnia", "Warsztat płatnerski", "Pracownia rymarska", "Główna stajnia", "Laboratorium")) ||
                                 (building.type in listOf("Warsztat", "Manufaktura") && room.name == "Sala Główna")
                    val canRest = room.name.contains("Pokój") || room.name.contains("Sypialnia") || room.name == "Wspólna sala" || room.name == "Miejsce na sianie" || room.name == "Piętro mieszkalne" || room.name == "Prywatne pokoje"

                    if (canTrade) {
                        val label = when (building.type) {
                            "Płatnerz", "Kowal", "Rymarz" -> "RZEMIOSŁO"
                            "Stajnia" -> "HANDEL"
                            else -> "HANDEL"
                        }
                        ActionButton(label, Color(0xFF2E7D32)) {
                            showShopOverlay = true
                            buyMode = true
                        }
                    }
                    if (canWork) {
                        val workLabel = when {
                            building.type in listOf("Kowal", "Płatnerz", "Rymarz") -> "RZEMIOSŁO"
                            building.type == "Stajnia" -> "POSŁUGA"
                            else -> "PRACA"
                        }
                        ActionButton(workLabel, Color(0xFF1976D2)) { showWorkPanel = true }
                    }
                    if (canRest) {
                        ActionButton("ODPOCZYNEK", Color(0xFF7B1FA2)) { 
                            if (HeroLogic.hasAccessToRoom(heroData, building.name, selectedCity, room.name)) {
                                showRestPanel = true
                            } else {
                                Toast.makeText(context, "Musisz najpierw opłacić pobyt!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    if (showRestPanel) {
                        RestPanel(
                            restHours = restHours,
                            onHoursChange = { restHours = it },
                            restDays = restDays,
                            onDaysChange = { restDays = it },
                            onConfirm = {
                                val totalHours = (restDays * 24) + restHours.toInt()
                                // Regeneracja: -10 pkt za każdą godzinę snu
                                val fatigueRecovery = - (totalHours * 10)
                                val updatedData = HeroLogic.updateHeroFatigue(
                                    HeroLogic.advanceTime(heroData, totalHours),
                                    fatigueRecovery
                                )
                                saveToFile(currentFile, updatedData)
                                heroData = updatedData
                                dataNow = HeroLogic.getCalendarData(heroData)
                                showRestPanel = false
                                playSoundAction(R.raw.klik1, 0)
                            },
                            onDismiss = { showRestPanel = false }
                        )
                    }
                    
                    ActionButton("WYJDŹ NA ZEWNĄTRZ", Color(0xFFC62828)) {
                        generatedBuilding = null
                        currentRoom = null
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // NAWIGACJA MIĘDZY POMIESZCZENIAMI
                Text("PRZEJDŹ DO:", color = Color.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                building.rooms.filter { it != room }.forEach { otherRoom ->
                    val hasAccess = HeroLogic.hasAccessToRoom(heroData, building.name, selectedCity, otherRoom.name)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (hasAccess) {
                                    currentRoom = otherRoom
                                    playSoundAction(R.raw.klik1, 0)
                                } else {
                                    if (HeroLogic.canAttemptLockpicking(heroData)) {
                                        burglaryTargetRoom = otherRoom
                                        showBurglaryDialog = true
                                    } else {
                                        Toast.makeText(context, "Brak dostępu. Opłać pobyt lub zdobądź klucz.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = if (hasAccess) Color(0xFF222222) else Color(0xFF1A1111))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                otherRoom.name,
                                color = if (hasAccess) Color.White else Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (!hasAccess) {
                                Text("🔒", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- OVERLAYS ---

    if (showShopOverlay && generatedBuilding != null) {
        AlertDialog(
            onDismissRequest = { showShopOverlay = false },
            containerColor = Color(0xFF0F0F0F),
            title = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (buyMode) "KUPNO" else "SPRZEDAŻ", color = if (buyMode) Color.Green else Color.Red)
                        Row {
                            Button(onClick = { buyMode = true }, colors = ButtonDefaults.buttonColors(containerColor = if(buyMode) Color.DarkGray else Color.Black), modifier = Modifier.padding(horizontal = 2.dp), contentPadding = PaddingValues(horizontal = 8.dp)) { Text("K") }
                            Button(onClick = { buyMode = false }, colors = ButtonDefaults.buttonColors(containerColor = if(!buyMode) Color.DarkGray else Color.Black), modifier = Modifier.padding(horizontal = 2.dp), contentPadding = PaddingValues(horizontal = 8.dp)) { Text("S") }
                        }
                    }
                    
                        ScrollableTabRow(
                            selectedTabIndex = when(selectedCategoryFilter) {
                                "Konsumpcja" -> 1
                                "Usługi" -> 2
                                "Nocleg" -> 3
                                "Broń" -> 4
                                "Zbroje" -> 5
                                "Skóry" -> 6
                                "Konie" -> 7
                                "Transport" -> 8
                                "Inne" -> 9
                                else -> 0
                            },
                            containerColor = Color.Black,
                            contentColor = Color.Cyan,
                            edgePadding = 8.dp
                        ) {
                            Tab(selected = selectedCategoryFilter == "Wszystkie", onClick = { selectedCategoryFilter = "Wszystkie" }) { Text("WSZYSTKO", fontSize = 10.sp, modifier = Modifier.padding(8.dp)) }
                            Tab(selected = selectedCategoryFilter == "Konsumpcja", onClick = { selectedCategoryFilter = "Konsumpcja" }) { Text("JEDZENIE", fontSize = 10.sp, modifier = Modifier.padding(8.dp)) }
                            Tab(selected = selectedCategoryFilter == "Usługi", onClick = { selectedCategoryFilter = "Usługi" }) { Text("USŁUGI", fontSize = 10.sp, modifier = Modifier.padding(8.dp)) }
                            Tab(selected = selectedCategoryFilter == "Nocleg", onClick = { selectedCategoryFilter = "Nocleg" }) { Text("NOCLEG", fontSize = 10.sp, modifier = Modifier.padding(8.dp)) }
                            Tab(selected = selectedCategoryFilter == "Broń", onClick = { selectedCategoryFilter = "Broń" }) { Text("BROŃ", fontSize = 10.sp, modifier = Modifier.padding(8.dp)) }
                            Tab(selected = selectedCategoryFilter == "Zbroje", onClick = { selectedCategoryFilter = "Zbroje" }) { Text("ZBROJE", fontSize = 10.sp, modifier = Modifier.padding(8.dp)) }
                            Tab(selected = selectedCategoryFilter == "Skóry", onClick = { selectedCategoryFilter = "Skóry" }) { Text("SKÓRY", fontSize = 10.sp, modifier = Modifier.padding(8.dp)) }
                            Tab(selected = selectedCategoryFilter == "Konie", onClick = { selectedCategoryFilter = "Konie" }) { Text("KONIE", fontSize = 10.sp, modifier = Modifier.padding(8.dp)) }
                            Tab(selected = selectedCategoryFilter == "Transport", onClick = { selectedCategoryFilter = "Transport" }) { Text("WOZY", fontSize = 10.sp, modifier = Modifier.padding(8.dp)) }
                        }
                }
            },
            text = {
                val inventory = buildingInventories.getOrPut(generatedBuilding!!.name) {
                    getBuildingInventory(selectedBuildingShop ?: buildings[0], generatedBuilding)
                }
                
                val filteredInventory = inventory.filter { 
                    when(selectedCategoryFilter) {
                        "Konsumpcja" -> it.category in listOf("Jedzenie", "Trunki")
                        "Usługi" -> it.category in listOf("Usługi", "Rozrywka")
                        "Nocleg" -> it.category == "Pokoje"
                        "Broń" -> it.category in listOf("Broń", "Narzędzia")
                        "Zbroje" -> it.category in listOf("Zbroje", "Tarcze")
                        "Skóry" -> it.category in listOf("Skóry", "Akcesoria")
                        "Konie" -> it.category == "Konie"
                        "Transport" -> it.category == "Transport"
                        else -> true
                    }
                }

                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(filteredInventory) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                pendingItem = item
                                manualCoinSelection = true
                            },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, color = Color.White, fontSize = 14.sp)
                                Text(item.category, color = Color.Gray, fontSize = 10.sp)
                            }
                            Text(HeroLogic.formatPrice(item.price), color = Color.Yellow, fontSize = 13.sp)
                        }
                        HorizontalDivider(color = Color(0xFF222222))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showShopOverlay = false }) { Text("ZAMKNIJ") } }
        )
    }

    if (manualCoinSelection && pendingItem != null) {
        val ownedHorses = HeroLogic.countOwnedHorses(heroData)
        val usedHorses = HeroLogic.countUsedHorses(heroData)
        val requiredHorses = HeroLogic.getRequiredHorses(pendingItem!!.name)
        val hasEnoughHorses = ownedHorses >= (usedHorses + requiredHorses)
        
        val containerLimitExceeded = false // W uproszczeniu

        AlertDialog(
            onDismissRequest = { manualCoinSelection = false },
            containerColor = Color.DarkGray,
            title = { Text("Ręczne odliczanie: ${pendingItem!!.name}", color = Color.White) },
            text = {
                Column {
                    Text("Cena: ${HeroLogic.formatPrice(pendingItem!!.price)}", color = Color.Yellow)
                    if (requiredHorses > 0) {
                        Text("Wymaga koni: $requiredHorses (Posiadasz wolnych: ${ownedHorses - usedHorses})", 
                            color = if (hasEnoughHorses) Color.Gray else Color.Red,
                            fontSize = 12.sp
                        )
                    }
                    val totalPaid = (payGold * 480) + (paySilver * 12) + payCopper
                    Text("Wybrano: ${HeroLogic.formatPrice(totalPaid)}", color = if (totalPaid >= pendingItem!!.price) Color.Green else Color.Red)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    ManualCoinRow("Złota", payGold, gold, playSoundAction) { v: Int -> payGold = v }
                    ManualCoinRow("Srebrny", paySilver, silver, playSoundAction) { v: Int -> paySilver = v }
                    ManualCoinRow("Miedziany", payCopper, copper, playSoundAction) { v: Int -> payCopper = v }
                }
            },
            confirmButton = {
                val totalPaid = (payGold * 480) + (paySilver * 12) + payCopper
                Button(
                    enabled = totalPaid >= pendingItem!!.price && !containerLimitExceeded && hasEnoughHorses,
                    onClick = {
                        var result = HeroLogic.processTransaction(heroData, pendingItem!!.name, pendingItem!!.price, false, payGold, paySilver, payCopper, true)
                        
                        // Jeśli zakupiono pokój/nocleg lub mieszkanie, dodaj wpis o pobycie/własności
                        if (pendingItem!!.category == "Pokoje") {
                            if (generatedBuilding != null) {
                                when {
                                    pendingItem!!.name.contains("Doba") -> {
                                        result = HeroLogic.addStay(result, selectedCity, generatedBuilding!!.name, 24)
                                    }
                                    pendingItem!!.name.contains("Nocleg") -> {
                                        result = HeroLogic.addStay(result, selectedCity, generatedBuilding!!.name, 12)
                                    }
                                    pendingItem!!.name.contains("Czynsz") -> {
                                        result = HeroLogic.addProperty(result, selectedCity, generatedBuilding!!.name, "Najem")
                                    }
                                    pendingItem!!.name.contains("Własność") -> {
                                        result = HeroLogic.addProperty(result, selectedCity, generatedBuilding!!.name, "Własność")
                                    }
                                }
                            }
                        }

                        saveToFile(currentFile, result)
                        heroData = result
                        playSoundAction(R.raw.roll, 1)
                        manualCoinSelection = false
                        showShopOverlay = false
                    }
                ) { Text("ZAPŁAĆ") }
            },
            dismissButton = { TextButton(onClick = { manualCoinSelection = false }) { Text("ANULUJ") } }
        )
    }

    if (showBurglaryDialog && burglaryTargetRoom != null && generatedBuilding != null) {
        val currentHour = HeroLogic.getCurrentHour(heroData)
        val isNight = currentHour in 22..23 || currentHour in 0..4

        AlertDialog(
            onDismissRequest = { showBurglaryDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("PRÓBA WŁAMANIA", color = Color.Red, fontFamily = nosiferFamily, fontSize = 18.sp) },
            text = {
                Column {
                    Text("Budynek: ${generatedBuilding!!.name}", color = Color.Gray)
                    Text("Pokój: ${burglaryTargetRoom!!.name}", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ryzyko przyłapania: ${if (isNight) "Niskie (Noc)" else "Wysokie (Dzień)"}", color = if (isNight) Color.Green else Color.Yellow)
                    Text("Wymagany wytrych zostanie zużyty w razie porażki.", color = Color.LightGray, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val result = HeroLogic.performBurglary(
                            heroData,
                            generatedBuilding!!.name,
                            selectedCity,
                            burglaryTargetRoom!!.name,
                            currentHour,
                            selectedBuildingShop?.name ?: "Budynek",
                            generatedBuilding!!.rank
                        )
                        burglaryResult = result
                        heroData = result.updatedHeroData
                        saveToFile(currentFile, heroData)
                        showBurglaryDialog = false
                        showBurglaryResult = true
                        if (result.success) playSoundAction(R.raw.klik2, 0)
                        else if (result.caught) playSoundAction(R.raw.page, 0)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Text("WŁAM SIĘ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBurglaryDialog = false }) {
                    Text("REZYGNUJ", color = Color.White)
                }
            }
        )
    }

    if (showBurglaryResult && burglaryResult != null) {
        AlertDialog(
            onDismissRequest = { showBurglaryResult = false },
            containerColor = Color(0xFF1A1A1A),
            title = {
                Text(
                    if (burglaryResult!!.success) "SUKCES!" else if (burglaryResult!!.caught) "PRZYŁAPANY!" else "PORAŻKA",
                    color = if (burglaryResult!!.success) Color.Green else Color.Red
                )
            },
            text = {
                Column {
                    Text(burglaryResult!!.message, color = Color.White)
                    if (burglaryResult!!.success) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Zdobyte monety: ${HeroLogic.formatPrice(burglaryResult!!.lootMoney)}", color = Color.Yellow)
                        if (burglaryResult!!.lootItems.isNotEmpty()) {
                            Text("Skradzione przedmioty:", color = Color.Cyan)
                            burglaryResult!!.lootItems.forEach { Text(" - $it", color = Color.White) }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { 
                    showBurglaryResult = false
                    if (burglaryResult!!.success && burglaryTargetRoom != null) {
                        currentRoom = burglaryTargetRoom
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }

    if (showWorkPanel) {
        AlertDialog(
            onDismissRequest = { showWorkPanel = false },
            containerColor = Color(0xFF111111),
            text = {
                WorkPanel(
                    heroData = heroData,
                    buildingType = generatedBuilding?.type ?: "",
                    workHours = workHours,
                    onHoursChange = { workHours = it },
                    onWorkStart = { job: String, wage: Int ->
                        val statValue = when {
                            job.contains("Muzyk") -> HeroLogic.getStatValue(heroData, "Cha")
                            job.contains("Posługiwanie") -> HeroLogic.getStatValue(heroData, "Cha")
                            job.contains("Kucharz") || job.contains("Kucharka") -> HeroLogic.getStatValue(heroData, "Int")
                            job.contains("Rzemiosło") -> HeroLogic.getStatValue(heroData, "Int")
                            else -> HeroLogic.getStatValue(heroData, "K")
                        }
                        val (updatedData, result) = HeroLogic.performWorkSessions(
                            heroData = heroData,
                            skillName = if (job.contains("Muzyk")) "Muzyka" else if (job.contains("Posługiwanie")) "Charyzma" else if (job.contains("Kucharz") || job.contains("Kucharka")) "Gotowanie" else if (job.contains("Rzemiosło")) job else "Siła",
                            hours = workHours.toInt(),
                            baseWagePerHour = wage / workHours.toInt(),
                            intelligence = statValue
                        )
                        workEvent = HeroLogic.WorkResult(
                            result.skillName,
                            "Zakończono zmianę. Zarobiono ${HeroLogic.formatPrice(result.totalWage)} i zdobyto ${result.totalPn} PN.",
                            result.totalWage
                        )
                        heroData = updatedData
                        saveToFile(currentFile, updatedData)
                        dataNow = HeroLogic.getCalendarData(updatedData)
                        showWorkPanel = false
                        showWorkEvent = true
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showWorkPanel = false }) {
                    Text("ZAMKNIJ", color = Color.White)
                }
            }
        )
    }

    if (showWorkEvent && workEvent != null) {
        AlertDialog(
            onDismissRequest = { showWorkEvent = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text(workEvent!!.title, color = Color.Cyan) },
            text = { Text(workEvent!!.description, color = Color.White) },
            confirmButton = { Button(onClick = { showWorkEvent = false }) { Text("OK") } }
        )
    }
}

@Composable
fun ActionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

fun getRoomContextDescription(room: Room, building: BuildingInstance): String {
    return when {
        room.name == "Sala Główna" -> "Gwar rozmów i zapach jedzenia wypełniają powietrze. To tutaj bije serce tego miejsca."
        room.name == "Bar / Szynk" -> "Masywny kontuar i rzędy butelek obiecują ucieczkę od trudów dnia."
        room.name.contains("Pokój uciech") -> "Półmrok i zapach ciężkich perfum tworzą atmosferę intymności."
        room.name == "Kuchnia" -> "Buchająca para i brzęk garnków świadczą o ciężkiej pracy kucharzy."
        room.name.contains("Pokój nr") || room.name == "Pokoje gościnne (piętro)" || room.name == "Prywatne pokoje" -> "Ciche miejsce, gdzie strudzony wędrowiec może znaleźć chwilę wytchnienia."
        room.name == "Recepcja i Lobby" -> "Eleganckie miejsce powitania gości, gdzie każdy detal dba o prestiż."
        room.name == "Sala VIP / Bankietowa" -> "Miejsce dla tych, których sakiewki są cięższe od sumienia."
        room.name == "Laboratorium" -> "Zapach siarki i dziwnych ziół drażni nozdrza. Wszędzie stoją menzurki i retorty."
        room.name == "Kuźnia" -> "Żar bijący z paleniska i rytmiczny dźwięk młota uderzającego o kowadło wypełniają pomieszczenie."
        room.name == "Warsztat płatnerski" -> "Wszędzie wiszą fragmenty pancerzy, a w powietrzu unosi się zapach oliwy i metalu."
        room.name == "Pracownia rymarska" -> "Zapach świeżo wyprawionej skóry jest niemal oszałamiający. Na ścianach wiszą uprzęże i siodła."
        room.name == "Główna stajnia" -> "Zapach siana i koni dominuje w tym przestronnym, przewiewnym budynku."
        room.name == "Siodlarnia" -> "Rzędy siodeł i uprzęży czekają na swoich właścicieli w nienagannym porządku."
        else -> "Typowe pomieszczenie, spełniające swoją rolę w tym budynku."
    }
}

@Composable
fun RestPanel(
    restHours: Float,
    onHoursChange: (Float) -> Unit,
    restDays: Int,
    onDaysChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("Odpoczynek", color = Color(0xFF7B1FA2), fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Dni:", color = Color.White, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (restDays > 0) onDaysChange(restDays - 1) }) {
                        Text("-", color = Color.Cyan, fontSize = 24.sp)
                    }
                    Text("$restDays", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 16.dp))
                    IconButton(onClick = { onDaysChange(restDays + 1) }) {
                        Text("+", color = Color.Cyan, fontSize = 24.sp)
                    }
                }
                
                if (restDays >= 2) {
                    val discount = 5
                    Text("Zniżka za długi pobyt: $discount%", color = Color.Green, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Dodatkowe godziny:", color = Color.White, fontSize = 12.sp)
                Text("${restHours.toInt()} h", color = Color.Cyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = restHours,
                    onValueChange = { onHoursChange(it) },
                    valueRange = 0f..23f,
                    steps = 23,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF7B1FA2),
                        activeTrackColor = Color(0xFF7B1FA2)
                    )
                )
                
                val totalHours = (restDays * 24) + restHours.toInt()
                Text("Łącznie: $totalHours h", color = Color.Gray, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
            ) {
                Text("ODPOCZNIJ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ANULUJ", color = Color.White) }
        }
    )
}

fun getBuildingInventory(building: Shop, generatedBuilding: BuildingInstance? = null): List<TradeItem> {
    val list = mutableListOf<TradeItem>()
    building.categories.forEach { cat ->
        when (cat) {
            "Jedzenie" -> {
                list.add(TradeItem("Prowiant (1 dzień)", 10, cat))
                list.add(TradeItem("Ciepły posiłek", 5, cat))
                list.add(TradeItem("Luksusowa uczta", 50, cat))
                list.add(TradeItem("Chleb", 1, cat))
                list.add(TradeItem("Ser", 3, cat))
            }
            "Trunki" -> {
                list.add(TradeItem("Piwo (kufel)", 1, cat))
                list.add(TradeItem("Wino (butelka)", 15, cat))
                list.add(TradeItem("Gorzałka (kieliszek)", 5, cat))
                list.add(TradeItem("Miód pitny", 20, cat))
            }
            "Rozrywka" -> {
                list.add(TradeItem("Gra w kości (stawka)", 12, cat))
                list.add(TradeItem("Muzyka i śpiew", 2, cat))
                list.add(TradeItem("Opowieść barda", 5, cat))
            }
            "Pokoje" -> {
                if (building.name == "Mieszkanie") {
                    list.add(TradeItem("Czynsz miesięczny", 500, cat))
                    list.add(TradeItem("Własność (Akt)", 5000, cat))
                } else {
                    list.add(TradeItem("Miejsce na sianie (Nocleg)", 1, cat))
                    list.add(TradeItem("Wspólna sala (Nocleg)", 5, cat))
                    list.add(TradeItem("Pokój standardowy (Nocleg)", 20, cat))
                    list.add(TradeItem("Apartament (Nocleg)", 120, cat))
                    list.add(TradeItem("Miejsce na sianie (Doba)", 2, cat))
                    list.add(TradeItem("Wspólna sala (Doba)", 10, cat))
                    list.add(TradeItem("Pokój standardowy (Doba)", 40, cat))
                    list.add(TradeItem("Apartament (Doba)", 240, cat))
                }
            }
            "Usługi" -> {
                list.add(TradeItem("Stajnia (1 koń/noc)", 10, cat))
                list.add(TradeItem("Łaźnia i balia", 3, cat))
                list.add(TradeItem("Pranie odzieży", 2, cat))
                
                val zPrice = generatedBuilding?.innDetails?.servicePrice ?: 25
                list.add(TradeItem("Usługa (Zamtuz)", zPrice, cat))
                list.add(TradeItem("Prywatny pokaz", zPrice * 2, cat))
            }
            "Zioła" -> {
                list.add(TradeItem("Ziele lecznicze", 40, cat))
                list.add(TradeItem("Korzeń wzmacniający", 60, cat))
                list.add(TradeItem("Suszone jagody", 5, cat))
            }
            "Mikstury" -> {
                list.add(TradeItem("Mikstura leczenia", 500, cat))
                list.add(TradeItem("Antidotum", 350, cat))
                list.add(TradeItem("Wyciąg z mandragory", 800, cat))
            }
            "Zbroje" -> {
                list.add(TradeItem("Przeszywanica", 120, cat))
                list.add(TradeItem("Skórzana kurta", 200, cat))
                list.add(TradeItem("Kolczuga", 800, cat))
                list.add(TradeItem("Pancerz płytowy", 2500, cat))
            }
            "Tarcze" -> {
                list.add(TradeItem("Puklerz", 50, cat))
                list.add(TradeItem("Tarcza drewniana", 80, cat))
                list.add(TradeItem("Tarcza trójkątna", 150, cat))
            }
            "Broń" -> {
                list.add(TradeItem("Sztylet", 15, cat))
                list.add(TradeItem("Miecz krótki", 45, cat))
                list.add(TradeItem("Miecz długi", 85, cat))
                list.add(TradeItem("Topór bojowy", 65, cat))
                list.add(TradeItem("Wekiera", 50, cat))
            }
            "Narzędzia" -> {
                list.add(TradeItem("Młotek", 5, cat))
                list.add(TradeItem("Piła", 8, cat))
                list.add(TradeItem("Zestaw wytrychów", 120, cat))
            }
            "Skóry" -> {
                list.add(TradeItem("Garbowana skóra", 10, cat))
                list.add(TradeItem("Rękawice robocze", 5, cat))
                list.add(TradeItem("Pasek skórzany", 8, cat))
            }
            "Akcesoria" -> {
                list.add(TradeItem("Siodło", 150, cat))
                list.add(TradeItem("Uzda", 30, cat))
                list.add(TradeItem("Torba podróżna", 25, cat))
            }
            "Konie" -> {
                list.add(TradeItem("Muł", 100, cat))
                list.add(TradeItem("Koń roboczy", 250, cat))
                list.add(TradeItem("Wierzchowiec", 600, cat))
                list.add(TradeItem("Koń bojowy", 1500, cat))
            }
            "Transport" -> {
                list.add(TradeItem("Wózek (1 koń)", 80, cat))
                list.add(TradeItem("Wóz (2 konie)", 200, cat))
                list.add(TradeItem("Wóz kryty (2 konie)", 450, cat))
                list.add(TradeItem("Kareta (4 konie)", 1200, cat))
            }
        }
    }
    return list
}
