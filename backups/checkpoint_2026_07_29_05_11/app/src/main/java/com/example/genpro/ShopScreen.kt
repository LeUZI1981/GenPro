package com.example.genpro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpro.EncumbranceLevel
import java.io.File

@Composable
fun ShopScreen(
    selectedHeroId: Int,
    nosiferFamily: FontFamily? = null,
    slaboFamily: FontFamily? = null,
    onBack: () -> Unit,
    onNavigate: (Int) -> Unit,
    readFromFile: (String) -> String,
    saveToFile: (String, String) -> Unit,
    playSoundAction: (Int, Int) -> Unit,
    isGMMode: Boolean = false,
    initialShopType: String? = null
) {
    val fileName = "hero_$selectedHeroId"
    var heroData by remember { mutableStateOf(readFromFile(fileName)) }

    val shops = HeroLogic.shops

    var selectedShop by remember { 
        mutableStateOf(shops.find { it.name == initialShopType } ?: shops[1]) 
    }
    var buyMode by remember { mutableStateOf(true) }
    
    // Praca variables
    var workHours by remember { mutableStateOf(4f) }
    var workEvent by remember { mutableStateOf<HeroLogic.WorkResult?>(null) }
    var showWorkEvent by remember { mutableStateOf(false) }

    var showManualPayment by remember { mutableStateOf(false) }
    var manualCoinSelection by remember { mutableStateOf(false) }
    var pendingItem by remember { mutableStateOf<TradeItem?>(null) }

    // Ręczne liczniki monet do zapłaty
    var payGold by remember { mutableStateOf(0) }
    var paySilver by remember { mutableStateOf(0) }
    var payCopper by remember { mutableStateOf(0) }

    val shopInventories = remember { mutableStateMapOf<String, List<TradeItem>>() }

    // Parsowanie kasy gracza
    var gold = 0; var silver = 0; var copper = 0
    heroData.lines().forEach { line ->
        if (line.contains("⛀")) {
            val v = line.trim().split("  ").last().toIntOrNull() ?: 0
            if (line.contains("Złota")) gold = v
            else if (line.contains("Srebrny")) silver = v
            else if (line.contains("Miedziany")) copper = v
        }
    }

    val weightLimit = HeroLogic.getHeroWeightLimit(heroData)
    val currentWeight = HeroLogic.calculateCurrentWeight(heroData)

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Text("⬅️", color = Color.White, fontSize = 24.sp) }
                IconButton(onClick = { onNavigate(50) }) { Text("🏙️", fontSize = 22.sp) }
                Text("HANDEL", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = nosiferFamily)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Wallet and Encumbrance
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
                if (encumbrance != com.example.genpro.EncumbranceLevel.LEKKIE) {
                    Text(encumbrance.penalties, color = encumbrance.color, fontSize = 9.sp)
                }
            }
        }

        // Panel Podsumowania Pojemności
        val invLines = heroData.lines().filter { it.startsWith("   •") }
        val strengthBonus = HeroLogic.getHeroStrengthBonus(heroData)
        
        val bagsCount = invLines.count { line -> HeroLogic.itemTorby.any { line.contains(it) } }
        val pouchesCount = invLines.count { line -> HeroLogic.itemSakwy.any { line.contains(it) } }
        val totalContainers = bagsCount + pouchesCount
        val containerLimit = HeroLogic.getContainerLimit(strengthBonus)
        
        var totalLiquidCap = 0
        invLines.forEach { line ->
            val entry = HeroLogic.pojemnikiInfo.entries.find { line.contains(it.key) }
            if (entry != null && entry.value.third == "Płyn") {
                totalLiquidCap += entry.value.second.toInt()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CapacitySummaryItem("🎒", "Torby", bagsCount, containerLimit)
            CapacitySummaryItem("👝", "Sakwy", pouchesCount, 10) // Sakwy mają inny limit lub są luźne
            CapacitySummaryItem("🍶", "Płyny", 0, totalLiquidCap, "L")
            CapacitySummaryItem("⚖️", "Suma", bagsCount, containerLimit)
        }


        // Wybór sklepu
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            items(shops) { shop ->
                val isSelected = selectedShop == shop
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.Yellow else Color(0xFF222222))
                        .clickable { 
                            selectedShop = shop
                            if (shop.name == "PRACA") buyMode = true
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(shop.name, color = if (isSelected) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        Text(shop.sub, color = if (isSelected) Color.DarkGray else Color.Gray, fontSize = 10.sp)
                    }
                }
            }
        }

        if (selectedShop.name == "PRACA") {
            WorkPanel(
                heroData = heroData,
                workHours = workHours,
                onHoursChange = { workHours = it },
                onWorkStart = { type, wage ->
                    val result = HeroLogic.generateWorkEvent(type, wage, HeroLogic.getHeroCharisma(heroData))
                    workEvent = result
                    showWorkEvent = true
                    heroData = HeroLogic.addMoney(heroData, result.totalWage)
                    saveToFile(fileName, heroData)
                    playSoundAction(R.raw.roll, 0)
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

        // Lista towarów
        val itemsToDisplay = if (buyMode) {
            val fullInv = shopInventories.getOrPut(selectedShop.name) { HeroLogic.getShopInventory(selectedShop) }
            HeroLogic.filterInventoryByHero(fullInv, heroData)
        } else {
            val inv = shopInventories.getOrPut(selectedShop.name) { HeroLogic.getShopInventory(selectedShop) }
            HeroLogic.getPlayerSellableItems(heroData, selectedShop, inv)
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(itemsToDisplay, key = { it.name + it.price }) { item ->
                val qualityMatch = Regex("\\[Jakość: (\\d+)%\\]").find(item.name)
                val currentQuality = qualityMatch?.groupValues?.get(1)?.toIntOrNull() ?: 100
                val canRepair = !buyMode && currentQuality < 100 && 
                               selectedShop.name in listOf("ZBROJMISTRZ", "PŁATNERZ", "RYMARZ")
                val repairCost = if (canRepair) HeroLogic.getRepairCost(item.name, currentQuality, HeroLogic.findItemPrice(item.name)) else 0

                ShopItemRow(
                    item = item,
                    isBuy = buyMode,
                    canRepair = canRepair,
                    repairCost = repairCost,
                    onRepair = {
                        val totalCopper = (gold * 480) + (silver * 12) + copper
                        if (totalCopper >= repairCost) {
                            heroData = HeroLogic.repairItem(heroData, item.name, repairCost)
                            saveToFile(fileName, heroData)
                            playSoundAction(R.raw.roll, 0)
                        }
                    },
                    onAction = {
                        pendingItem = item
                        if (buyMode) {
                            showManualPayment = true
                        } else {
                            heroData = HeroLogic.processTransaction(heroData, item.name, item.price / 2, isSelling = true)
                            saveToFile(fileName, heroData)
                            playSoundAction(R.raw.roll, 0)
                        }
                    }
                )
            }
        }
    }

    if (showManualPayment && pendingItem != null) {
        val itemWeight = HeroLogic.getItemWeight(pendingItem!!.name)
        val wouldBeOverloaded = currentWeight + itemWeight > weightLimit
        
        val requiredHorses = HeroLogic.getRequiredHorses(pendingItem!!.name)
        val ownedHorses = HeroLogic.countOwnedHorses(heroData)
        val usedHorses = HeroLogic.countUsedHorses(heroData)
        val hasEnoughHorses = ownedHorses >= (usedHorses + requiredHorses)

        val isLargeContainer = HeroLogic.itemTorby.any { pendingItem!!.name.contains(it) }
        val strengthBonus = HeroLogic.getHeroStrengthBonus(heroData)
        val currentLargeContainers = heroData.lines().count { line ->
            line.startsWith("   •") && HeroLogic.itemTorby.any { line.contains(it) }
        }
        val containerLimit = HeroLogic.getContainerLimit(strengthBonus)
        val containerLimitExceeded = isLargeContainer && currentLargeContainers >= containerLimit

        AlertDialog(
            onDismissRequest = { showManualPayment = false },
            title = { Text("Płatność za: ${pendingItem!!.name}") },
            text = { 
                Column {
                    Text("Cena: ${HeroLogic.formatPrice(pendingItem!!.price)}")
                    Text("Waga przedmiotu: $itemWeight j.", color = Color.Gray, fontSize = 12.sp)
                    
                    if (requiredHorses > 0) {
                        Text("Wymaga koni: $requiredHorses (Posiadasz wolnych: ${ownedHorses - usedHorses})", 
                            color = if (hasEnoughHorses) Color.Gray else Color.Red,
                            fontSize = 12.sp)
                    }

                    if (wouldBeOverloaded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("⚠️ UWAGA: Ten zakup spowoduje PRZECIĄŻENIE postaci!", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (containerLimitExceeded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val limit = HeroLogic.getContainerLimit(strengthBonus)
                        Text("❌ LIMIT POJEMNIKÓW: Postać o Sile (S) $strengthBonus może nosić tylko $limit dużych pojemników!", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    
                    if (!hasEnoughHorses) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("❌ BRAK KONI: Nie masz wystarczającej liczby zwierząt pociągowych!", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Wybierz sposób płatności:")
                }
            },
            confirmButton = {
                val canConfirm = hasEnoughHorses && (isGMMode || (!wouldBeOverloaded && !containerLimitExceeded))
                Button(
                    onClick = {
                        val totalCopper = (gold * 480) + (silver * 12) + copper
                        if (totalCopper >= pendingItem!!.price) {
                            heroData = HeroLogic.processTransaction(heroData, pendingItem!!.name, pendingItem!!.price, false)
                            saveToFile(fileName, heroData)
                            showManualPayment = false
                            playSoundAction(R.raw.roll, 0)
                        }
                    },
                    enabled = canConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!canConfirm) Color.Gray else if (wouldBeOverloaded || containerLimitExceeded) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                ) { 
                    val btnText = when {
                        !hasEnoughHorses -> "BRAK KONI"
                        containerLimitExceeded -> "LIMIT POJEMNIKÓW"
                        wouldBeOverloaded && !isGMMode -> "PRZECIĄŻENIE"
                        wouldBeOverloaded -> "KUP (PRZECIĄŻY)"
                        else -> "SZYBKI HANDEL (Auto)"
                    }
                    Text(btnText)
                }
            },

            dismissButton = {
                TextButton(onClick = { 
                    // Rozpoczęcie wyboru ręcznego
                    payGold = 0; paySilver = 0; payCopper = 0
                    manualCoinSelection = true 
                }) {
                    Text("PŁACĘ WYBRANYMI MONETAMI")
                }
            }
        )
    }

    if (manualCoinSelection && pendingItem != null) {
        val priceToPay = pendingItem?.price ?: 0
        val totalSelected = (payGold * 480) + (paySilver * 12) + payCopper
        val canPay = totalSelected >= priceToPay
        
        val itemWeight = HeroLogic.getItemWeight(pendingItem!!.name)
        val wouldBeOverloaded = currentWeight + itemWeight > weightLimit

        val isLargeContainer = HeroLogic.itemTorby.any { pendingItem!!.name.contains(it) }
        val strengthBonus = HeroLogic.getHeroStrengthBonus(heroData)
        val currentLargeContainers = heroData.lines().count { line ->
            line.startsWith("   •") && HeroLogic.itemTorby.any { line.contains(it) }
        }
        val containerLimit = HeroLogic.getContainerLimit(strengthBonus)
        val containerLimitExceeded = isLargeContainer && currentLargeContainers >= containerLimit

        val requiredHorses = HeroLogic.getRequiredHorses(pendingItem!!.name)
        val ownedHorses = HeroLogic.countOwnedHorses(heroData)
        val usedHorses = HeroLogic.countUsedHorses(heroData)
        val hasEnoughHorses = ownedHorses >= (usedHorses + requiredHorses)

        AlertDialog(
            onDismissRequest = { manualCoinSelection = false },
            title = { Text("Ręczne odliczanie monet") },
            text = {
                Column {
                    Text("Do zapłaty: ${HeroLogic.formatPrice(pendingItem!!.price)}", fontWeight = FontWeight.Bold)
                    Text("Wybrano: ${HeroLogic.formatPrice(totalSelected)}", color = if (canPay) Color.Green else Color.Yellow)
                    if (canPay && totalSelected > priceToPay) {
                        Text("Reszta: ${HeroLogic.formatPrice(totalSelected - priceToPay)}", color = Color.Gray, fontSize = 11.sp)
                    }
                    if (requiredHorses > 0) {
                        Text("Wymaga koni: $requiredHorses (Posiadasz wolnych: ${ownedHorses - usedHorses})", 
                            color = if (hasEnoughHorses) Color.Gray else Color.Red,
                            fontSize = 12.sp)
                    }
                    if (wouldBeOverloaded) {
                        Text("⚠️ PRZECIĄŻENIE: ${currentWeight + itemWeight}/$weightLimit", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (containerLimitExceeded) {
                        Text("❌ LIMIT POJEMNIKÓW (S: $strengthBonus)", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    ManualCoinRow("Złote (zk)", payGold, gold, playSoundAction) { payGold = it }
                    ManualCoinRow("Srebrne (s)", paySilver, silver, playSoundAction) { paySilver = it }
                    ManualCoinRow("Miedziane (p)", payCopper, copper, playSoundAction) { payCopper = it }
                }
            },
            confirmButton = {
                val canConfirm = canPay && hasEnoughHorses && (isGMMode || (!wouldBeOverloaded && !containerLimitExceeded))
                Button(
                    onClick = {
                        if (canPay) {
                            heroData = HeroLogic.processTransaction(
                                heroData, 
                                pendingItem!!.name, 
                                pendingItem!!.price, 
                                isSelling = false,
                                manualG = payGold,
                                manualS = paySilver,
                                manualC = payCopper,
                                isManual = true
                            )
                            saveToFile(fileName, heroData)
                            manualCoinSelection = false
                            showManualPayment = false
                            playSoundAction(R.raw.roll, 0)
                        }
                    },
                    enabled = canConfirm
                ) { Text(if (containerLimitExceeded) "LIMIT" else if (wouldBeOverloaded && !isGMMode) "PRZECIĄŻENIE" else "ZAPŁAĆ") }
            },
            dismissButton = {
                TextButton(onClick = { manualCoinSelection = false }) { Text("ANULUJ") }
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
}
}
