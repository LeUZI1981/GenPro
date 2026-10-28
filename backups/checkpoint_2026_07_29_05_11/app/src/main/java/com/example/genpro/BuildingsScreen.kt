package com.example.genpro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import java.util.Locale

// --- Duplicated UI components removed (using SharedUiComponents.kt instead) ---

@Composable
fun BuildingsScreen(
    selectedHeroId: Int,
    nosiferFamily: FontFamily,
    slaboFamily: FontFamily,
    onBack: () -> Unit,
    onNavigate: (Int) -> Unit,
    readFromFile: (String) -> String,
    saveToFile: (String, String) -> Unit,
    playSoundAction: (Int, Int) -> Unit
) {
    val currentFile = "hero_$selectedHeroId"
    var heroData by remember { mutableStateOf(readFromFile(currentFile)) }

    // Stan walut
    var gold by remember { mutableIntStateOf(0) }
    var silver by remember { mutableIntStateOf(0) }
    var copper by remember { mutableIntStateOf(0) }

    LaunchedEffect(heroData) {
        val lines = heroData.lines()
        var g = 0; var s = 0; var c = 0
        lines.forEach { line ->
            if (line.contains("⛀")) {
                val value = line.trim().split("  ").last().toIntOrNull() ?: 0
                if (line.contains("Złota")) g += value
                else if (line.contains("Srebrny")) s += value
                else if (line.contains("Miedziany")) c += value
            }
        }
        gold = g; silver = s; copper = c
    }

    val buildings = listOf(
        Shop("Karczma", "Jedzenie i Napitki", listOf("Jedzenie", "Trunki")),
        Shop("Tawerna", "Gry i Rozrywka", listOf("Trunki", "Rozrywka")),
        Shop("Hotel", "Noclegi i Wygody", listOf("Pokoje", "Usługi")),
        Shop("Apteka", "Zioła i Mikstury", listOf("Zioła", "Mikstury")),
        Shop("Mieszkanie", "Wynajem i Kupno", listOf("Pokoje")),
        Shop("Zamtuz", "Przybytek Rozkoszy", listOf("Trunki", "Rozrywka", "Usługi"))
    )

    var selectedBuilding by remember { mutableStateOf(buildings[0]) }
    var generatedBuilding by remember { mutableStateOf<BuildingInstance?>(null) }
    var showBuildingDetails by remember { mutableStateOf(false) }
    val buildingInventories = remember { mutableStateMapOf<String, List<TradeItem>>() }

    var buyMode by remember { mutableStateOf(true) }
    var currentTab by remember { mutableIntStateOf(0) } // 0: Handel, 1: Praca
    var workHours by remember { mutableFloatStateOf(4f) }
    var workEvent by remember { mutableStateOf<HeroLogic.WorkResult?>(null) }
    var showWorkEvent by remember { mutableStateOf(false) }

    var showManualPayment by remember { mutableStateOf(false) }
    var pendingItem by remember { mutableStateOf<TradeItem?>(null) }
    
    var payGold by remember { mutableIntStateOf(0) }
    var paySilver by remember { mutableIntStateOf(0) }
    var payCopper by remember { mutableIntStateOf(0) }

    val weightLimit = HeroLogic.getHeroWeightLimit(heroData)
    val currentWeight = HeroLogic.calculateCurrentWeight(heroData)
    val strengthBonus = HeroLogic.getHeroStrengthBonus(heroData)

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).padding(16.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Text("⬅️", color = Color.White, fontSize = 24.sp) }
            IconButton(onClick = { onNavigate(50) }) { Text("🏙️", fontSize = 22.sp) }
            Text("BUDYNKI", color = Color.Cyan, fontSize = 22.sp, fontFamily = nosiferFamily)
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    generatedBuilding = BuildingGenerator.generateBuilding(selectedBuilding.name)
                    buildingInventories.clear() // Refresh shop on new generation
                    showBuildingDetails = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
            ) {
                Text("GENERUJ", fontSize = 10.sp)
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

        // Panel Podsumowania Pojemności
    val invLines = heroData.lines().filter { it.startsWith("   •") }
    val bagsCount = invLines.count { line -> 
        arrayOf("Worek lniany", "Torba podróżna", "Plecak", "Plecak usztywniany").any { line.contains(it) } 
    }
    val pouchesCount = invLines.count { line -> 
        arrayOf("Mała sakiewka", "Średnia sakiewka", "Duża sakiewka").any { line.contains(it) } 
    }
    val containerLimit = HeroLogic.getContainerLimit(strengthBonus)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CapacitySummaryItem("🎒", "Torby", bagsCount, containerLimit)
            CapacitySummaryItem("👝", "Sakwy", pouchesCount, 10)
            CapacitySummaryItem("⚖️", "Suma", bagsCount, containerLimit)
        }

        // Wybór budynku
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            items(buildings) { b ->
                val isSelected = selectedBuilding == b
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.Cyan else Color(0xFF222222))
                        .clickable { 
                            selectedBuilding = b
                            currentTab = 0
                            playSoundAction(R.raw.klik1, 0)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(b.name, color = if (isSelected) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        Text(b.sub, color = if (isSelected) Color.DarkGray else Color.Gray, fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tabs Handel / Praca (jeśli dostępne)
        val supportsWork = selectedBuilding.name in listOf("Karczma", "Tawerna", "Zamtuz")
        if (supportsWork) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { currentTab = 0 },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (currentTab == 0) Color.Cyan else Color.DarkGray)
                ) { Text("HANDEL", color = if (currentTab == 0) Color.Black else Color.White) }
                Button(
                    onClick = { currentTab = 1 },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (currentTab == 1) Color.Cyan else Color.DarkGray)
                ) { Text("PRACA", color = if (currentTab == 1) Color.Black else Color.White) }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (currentTab == 1 && supportsWork) {
            WorkPanel(
                heroData = heroData,
                workHours = workHours,
                onHoursChange = { workHours = it },
                onWorkStart = { type, wage ->
                    val result = HeroLogic.generateWorkEvent(type, wage, HeroLogic.getHeroCharisma(heroData))
                    workEvent = result
                    showWorkEvent = true
                    val updatedData = HeroLogic.addMoney(heroData, result.totalWage)
                    saveToFile(currentFile, updatedData)
                    heroData = updatedData
                    playSoundAction(R.raw.roll, 1)
                }
            )
        } else {
            // Kupno / Sprzedaż Toggle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = { buyMode = true },
                    colors = ButtonDefaults.buttonColors(containerColor = if (buyMode) Color(0xFF2E7D32) else Color.DarkGray)
                ) { Text("KUPUJ") }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { buyMode = false },
                    colors = ButtonDefaults.buttonColors(containerColor = if (!buyMode) Color(0xFFC62828) else Color.DarkGray)
                ) { Text("SPRZEDAJ") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista towarów/usług
            val itemsToDisplay = if (buyMode) {
                buildingInventories.getOrPut("${selectedBuilding.name}_${generatedBuilding?.name ?: "default"}") { 
                    getBuildingInventory(selectedBuilding, generatedBuilding) 
                }
            } else {
                getPlayerSellableItems(heroData, selectedBuilding, buildingInventories["${selectedBuilding.name}_${generatedBuilding?.name ?: "default"}"] ?: emptyList())
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(itemsToDisplay, key = { "${it.name}_${it.category}_${it.price}_${if(buyMode) "B" else "S"}" }) { item ->
                    ShopItemRow(item, isBuy = buyMode, onAction = {
                        pendingItem = item
                        if (buyMode) {
                            showManualPayment = true
                            payGold = 0; paySilver = 0; payCopper = 0
                        } else {
                            val result = HeroLogic.processTransaction(heroData, item.name, item.price / 2, isSelling = true)
                            saveToFile(currentFile, result)
                            heroData = result
                            playSoundAction(R.raw.roll, 1)
                        }
                    })
                }
            }
        }
    }

    if (showManualPayment && pendingItem != null) {
        val itemWeight = HeroLogic.getItemWeight(pendingItem!!.name)
        val wouldBeOverloaded = currentWeight + itemWeight > weightLimit
        
        val isLargeContainer = arrayOf("Worek lniany", "Torba podróżna", "Plecak", "Plecak usztywniany").any { pendingItem!!.name.contains(it) }
        val currentLargeContainers = heroData.lines().count { line ->
            line.startsWith("   •") && arrayOf("Worek lniany", "Torba podróżna", "Plecak", "Plecak usztywniany").any { line.contains(it) }
        }
        val containerLimitExceeded = isLargeContainer && (
            (strengthBonus <= 4 && currentLargeContainers >= 2) || 
            (strengthBonus > 4 && currentLargeContainers >= 6)
        )

        AlertDialog(
            onDismissRequest = { showManualPayment = false },
            containerColor = Color.DarkGray,
            title = { Text("Zakup: ${pendingItem!!.name}", color = Color.White) },
            text = {
                Column {
                    Text("Cena: ${HeroLogic.formatPrice(pendingItem!!.price)}", color = Color.Yellow)
                    Text("Waga: $itemWeight j.", color = Color.Gray, fontSize = 12.sp)
                    
                    if (wouldBeOverloaded) {
                        Text("⚠️ PRZECIĄŻENIE!", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (containerLimitExceeded) {
                        Text("❌ LIMIT POJEMNIKÓW (S: $strengthBonus)", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Twoje zasoby:", color = Color.Gray, fontSize = 12.sp)
                    Row {
                        Text("Z: $gold ", color = Color(0xFFFFD700))
                        Text("S: $silver ", color = Color(0xFFC0C0C0))
                        Text("M: $copper", color = Color(0xFFCD7F32))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ManualCoinRow("Złota", payGold, gold, playSoundAction) { v: Int -> payGold = v }
                    ManualCoinRow("Srebrny", paySilver, silver, playSoundAction) { v: Int -> paySilver = v }
                    ManualCoinRow("Miedziany", payCopper, copper, playSoundAction) { v: Int -> payCopper = v }
                    
                    val totalPaid = (payGold * 480) + (paySilver * 12) + payCopper
                    Text("Suma wpłaty: ${HeroLogic.formatPrice(totalPaid)}", color = if (totalPaid >= pendingItem!!.price) Color.Green else Color.Red)
                }
            },
            confirmButton = {
                val totalPaid = (payGold * 480) + (paySilver * 12) + payCopper
                val canPay = totalPaid >= pendingItem!!.price
                val canConfirm = canPay && !containerLimitExceeded // GM mode not explicitly passed here, assuming strict for now or similar to ShopScreen
                
                Button(
                    enabled = canConfirm,
                    onClick = {
                        val result = HeroLogic.processTransaction(
                            heroData, pendingItem!!.name, pendingItem!!.price, false, 
                            payGold, paySilver, payCopper, true
                        )
                        if (!result.startsWith("BŁĄD")) {
                            saveToFile(currentFile, result)
                            heroData = result
                            playSoundAction(R.raw.roll, 1)
                        }
                        showManualPayment = false
                    }
                ) { Text("ZAPŁAĆ") }
            },
            dismissButton = {
                TextButton(onClick = { showManualPayment = false }) { Text("ANULUJ", color = Color.White) }
            }
        )
    }

    if (showWorkEvent && workEvent != null) {
        AlertDialog(
            onDismissRequest = { showWorkEvent = false },
            title = { Text(workEvent!!.title, color = Color.Yellow) },
            text = {
                Column {
                    Text(workEvent!!.description)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Ostateczny zarobek: ${HeroLogic.formatPrice(workEvent!!.totalWage)}", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(onClick = { showWorkEvent = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showBuildingDetails && generatedBuilding != null) {
        AlertDialog(
            onDismissRequest = { showBuildingDetails = false },
            containerColor = Color(0xFF1A1A1A),
            title = {
                Column {
                    Text(generatedBuilding!!.name, color = Color.Cyan, fontFamily = nosiferFamily, fontSize = 18.sp)
                    Text(BuildingGenerator.getRankStars(generatedBuilding!!.rank), color = Color.Yellow)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Typ: ${generatedBuilding!!.type}", color = Color.Gray)
                    Text("Powierzchnia całkowita: ${String.format(Locale.GERMANY, "%.1f", generatedBuilding!!.totalArea)} m²", color = Color.White)
                    
                    generatedBuilding!!.innDetails?.let { details ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Szczegóły lokalu:", fontWeight = FontWeight.Bold, color = Color.Cyan)
                        if (generatedBuilding!!.type == "Zamtuz") {
                            Text("• Muzycy na żywo: ${if (details.hasMusicians) "TAK" else "NIE"}", color = Color.LightGray)
                            Text("• Miejsca przy szynku: ${details.barSeats}", color = Color.LightGray)
                            Text("• Pracownice: ${details.femaleStaff}", color = Color.LightGray)
                            Text("• Pracownicy: ${details.maleStaff}", color = Color.LightGray)
                            Text("• Cena usługi: ${HeroLogic.formatPrice(details.servicePrice)}", color = Color.Yellow, fontWeight = FontWeight.Bold)
                            Text("• Ogólna liczba miejsc: ${details.seatingCapacity}", color = Color.LightGray)
                        } else {
                            Text("• Kuchnia: ${if (details.hasKitchen) "TAK" else "NIE"}", color = Color.LightGray)
                            Text("• Sala taneczna: ${if (details.hasDanceFloor) "TAK" else "NIE"}", color = Color.LightGray)
                            Text("• Miejsca siedzące: ${details.seatingCapacity}", color = Color.LightGray)
                            Text("• Stoły: ${details.tables}, Stoliki: ${details.smallTables}, Ławy: ${details.benches}", color = Color.LightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Pomieszczenia:", fontWeight = FontWeight.Bold, color = Color.Cyan)
                    generatedBuilding!!.rooms.forEach { room ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(room.name, color = Color.White, fontSize = 14.sp)
                            Text("${String.format(Locale.GERMANY, "%.1f", room.area)} m²", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showBuildingDetails = false }) { Text("ZAMKNIJ") }
            }
        )
    }
}


fun getPlayerSellableItems(heroData: String, shop: Shop, inventory: List<TradeItem>): List<TradeItem> {
    return HeroLogic.getPlayerSellableItems(heroData, shop, inventory)
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
                list.add(TradeItem("Miejsce na sianie", 1, cat))
                list.add(TradeItem("Wspólna sala", 5, cat))
                list.add(TradeItem("Pokój standardowy", 20, cat))
                list.add(TradeItem("Apartament", 120, cat))
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
        }
    }
    return list
}
