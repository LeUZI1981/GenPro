package com.example.genpro.ui

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.genpro.ArmorPiece
import com.example.genpro.HeroLogic
import com.example.genpro.ItemRegistry
import com.example.genpro.MainActivity
import com.example.genpro.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

val itemRozmiar = arrayOf("Brak", "BardzoMały", "Mały", "Średni", "Duży", "Bardzo Duży", "Wielki", "Gigantyczny")

@Composable
fun CharacterInitialSetupScreen(
    wybranaRasaIndex: Int,
    onRasaIndexChange: (Int) -> Unit,
    wybranaPlecIndex: Int,
    onPlecIndexChange: (Int) -> Unit,
    wybranyWiekIndex: Int,
    onWiekIndexChange: (Int) -> Unit,
    wybranyPoziom: Int,
    onPoziomChange: (Int) -> Unit,
    listaWiek: Array<String>,
    playSoundAction: (Int, Int) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("RASA: ${MainActivity.listaRasa[wybranaRasaIndex]}", color = Color.White, fontSize = 20.sp)

        val sliderInteractionRasa = remember { MutableInteractionSource() }
        val isPressedRasa by sliderInteractionRasa.collectIsPressedAsState()
        LaunchedEffect(isPressedRasa) {
            if (isPressedRasa) playSoundAction(R.raw.page, 1)
        }

        Slider(
            value = wybranaRasaIndex.toFloat(),
            onValueChange = { onRasaIndexChange(it.toInt()) },
            onValueChangeFinished = { playSoundAction(R.raw.page, 2) },
            interactionSource = sliderInteractionRasa,
            valueRange = 0f..2f,
            steps = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))

        Text("PŁEĆ", color = Color.White)
        Button(
            onClick = {
                playSoundAction(R.raw.klik1, 0)
                onPlecIndexChange(if (wybranaPlecIndex == 0) 1 else 0)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            modifier = Modifier.size(120.dp)
        ) {
            Text(if (wybranaPlecIndex == 0) "♂️" else "♀️", fontSize = 60.sp)
        }
        Spacer(modifier = Modifier.height(40.dp))

        Text("WIEK: ${listaWiek[wybranyWiekIndex]}", color = Color.White, fontSize = 20.sp)

        val sliderInteractionWiek = remember { MutableInteractionSource() }
        val isPressedWiek by sliderInteractionWiek.collectIsPressedAsState()
        LaunchedEffect(isPressedWiek) {
            if (isPressedWiek) playSoundAction(R.raw.page, 1)
        }

        Slider(
            value = wybranyWiekIndex.toFloat(),
            onValueChange = { onWiekIndexChange(it.toInt()) },
            onValueChangeFinished = { playSoundAction(R.raw.page, 2) },
            interactionSource = sliderInteractionWiek,
            valueRange = 0f..3f,
            steps = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (wybranyPoziom == 0) "GRA" else "POZIOM: $wybranyPoziom",
            color = Color.White,
            fontSize = 20.sp
        )

        val sliderInteractionPoziom = remember { MutableInteractionSource() }
        val isPressedPoziom by sliderInteractionPoziom.collectIsPressedAsState()
        LaunchedEffect(isPressedPoziom) {
            if (isPressedPoziom) playSoundAction(R.raw.page, 1)
        }

        Slider(
            value = wybranyPoziom.toFloat(),
            onValueChange = { onPoziomChange(it.toInt()) },
            onValueChangeFinished = { playSoundAction(R.raw.page, 2) },
            interactionSource = sliderInteractionPoziom,
            valueRange = 0f..3f,
            steps = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                onNext()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
            Text("DALEJ", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SecondaryStatsScreen(
    statyDrugorzedne: Map<String, Int>?,
    cechyFizyczne: Map<String, String>?,
    wylosowaneCechyCharakteru: List<String>,
    wybranaKlasa: String,
    onRollSecondary: () -> Unit,
    onRollSkills: (String) -> Unit,
    playSoundAction: (Int, Int) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //Text("DRUGORZĘDNE", color = Color.Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold)

        LazyColumn(modifier = Modifier.padding(horizontal = 20.dp).weight(1f)) {

            cechyFizyczne?.forEach { (name, value) ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(name, color = Color.Gray, fontSize = 14.sp)
                        Text(value, color = Color.White, fontSize = 14.sp)
                    }
                    HorizontalDivider(color = Color.DarkGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            val rows = statyDrugorzedne?.toList()?.chunked(2)
            rows?.forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEach { (name, value) ->
                        Row(
                            modifier = Modifier.weight(1f).padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, color = Color.Gray, fontSize = 12.sp)
                            Text("$value", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                HorizontalDivider(color = Color.DarkGray)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(modifier = Modifier.padding(horizontal = 20.dp).weight(1f)) {
            if (wylosowaneCechyCharakteru.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("CECHY CHARAKTERYSTYCZNE", color = Color.Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(wylosowaneCechyCharakteru) { cecha ->
                    Text(
                        text = "• $cecha",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        if (wybranaKlasa.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reroll Button
                IconButton(onClick = {
                    onRollSecondary()
                    playSoundAction(R.raw.roll, 0)
                }, modifier = Modifier.size(60.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Losuj ponownie", tint = Color.Yellow, modifier = Modifier.size(40.dp))
                }

                // Next Button
                IconButton(onClick = {
                    onRollSkills(wybranaKlasa)
                    onNext()
                    playSoundAction(R.raw.klik1, 0)
                }, modifier = Modifier.size(60.dp)) {
                    Icon(Icons.Default.Check, contentDescription = "Dalej", tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }
        } else {
            Text("BRAK WYBRANEJ KLASY", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun dostoczoneSteps(size: Int): Int = if (size > 1) size - 2 else 0

@Composable
fun StatsAndClassSelectionScreen(
    wygenerowaneStaty: List<Triple<String, IntArray, Int>>?,
    wybranaKlasaIndex: Int,
    onKlasaIndexChange: (Int) -> Unit,
    onGenerateAll: () -> Unit,
    onRollSkills: (String) -> Unit,
    playSoundAction: (Int, Int) -> Unit,
    showClassSelection: Boolean = true,
    onNext: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    var isRolling by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("STATYSTYKI GŁÓWNE", color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(wygenerowaneStaty ?: emptyList()) { _, stat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val secTextColor = Color.Gray
                    val k1 = stat.second[1]
                    val k2 = stat.second[2]
                    val k1Color = when {
                        k1 < 4 -> Color.Red
                        k1 > 8 -> Color.Yellow
                        else -> secTextColor
                    }
                    val k2Color = when {
                        k2 < 4 -> Color.Red
                        k2 > 8 -> Color.Yellow
                        else -> secTextColor
                    }
                    val diceSum = k1 + k2
                    val resultColor = when {
                        diceSum < 6 -> Color.Red
                        diceSum > 16 -> Color.Yellow
                        else -> Color.White
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val fullName = when(stat.first) {
                            "WW" -> "Walka Wręcz"
                            "US" -> "US"
                            "K" -> "Krzepa"
                            "Odp" -> "Odporność"
                            "Zr" -> "Zręczność"
                            "Int" -> "Inteligencja"
                            "SW" -> "Siła Woli"
                            "Cha" -> "Charyzma"
                            else -> stat.first
                        }
                        Text(fullName, color = secTextColor, fontSize = 12.sp, modifier = Modifier.width(100.dp))
                        Text("${stat.second[0]}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(" + ", color = secTextColor, fontSize = 16.sp)
                        Text("$k1", color = k1Color, fontSize = 16.sp)
                        Text(" + ", color = secTextColor, fontSize = 16.sp)
                        Text("$k2", color = k2Color, fontSize = 16.sp)
                    }
                    Text("[${stat.second[0] + k1 + k2}]", color = resultColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = Color.DarkGray)
            }
        }

        Image(
            painter = painterResource(id = R.drawable.k100new),
            contentDescription = "Nowe generowanie",
            modifier = Modifier
                .size(100.dp)
                .padding(vertical = 8.dp)
                .rotate(rotation.value)
                .scale(scale.value)
                .clickable {
                    if (!isRolling) {
                        coroutineScope.launch {
                            isRolling = true
                            playSoundAction(R.raw.roll, 2)

                            launch {
                                rotation.snapTo(0f)
                                rotation.animateTo(
                                    targetValue = 720f,
                                    animationSpec = tween(durationMillis = 600)
                                )
                            }
                            launch {
                                scale.snapTo(0.5f)
                                scale.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                    )
                                )
                            }

                            delay(600)
                            onGenerateAll()
                            isRolling = false
                        }
                    }
                }
        )

        if (showClassSelection) {
            val dostepneKlasy = MainActivity.listaKlasa.filterIndexed { index, _ ->
                val statIdx = when(index) {
                    0 -> 0 // Wojownik -> WW
                    1 -> 4 // Łotr -> Zręczność
                    2 -> 1 // Łowca -> Strzelectwo
                    3 -> 5 // Uczony -> Inteligencja
                    4 -> 7 // Kapłan -> Charyzma
                    5 -> 6 // Mag -> Siła Woli
                    else -> 0
                }
                val stat = wygenerowaneStaty?.getOrNull(statIdx)
                if (stat != null) (stat.second[1] + stat.second[2]) > 11 else false
            }

            if (dostepneKlasy.isNotEmpty()) {
                val safeKlasaIndex = if (wybranaKlasaIndex >= dostepneKlasy.size) 0 else wybranaKlasaIndex
                if (safeKlasaIndex != wybranaKlasaIndex) {
                    onKlasaIndexChange(safeKlasaIndex)
                }
                val wybranaKlasa = dostepneKlasy[safeKlasaIndex]

                Text(wybranaKlasa, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                if (dostepneKlasy.size > 1) {
                    val sliderInteractionKlasa = remember { MutableInteractionSource() }
                    val isPressedKlasa by sliderInteractionKlasa.collectIsPressedAsState()
                    LaunchedEffect(isPressedKlasa) {
                        if (isPressedKlasa) playSoundAction(R.raw.page, 1)
                    }

                    Slider(
                        value = safeKlasaIndex.toFloat(),
                        onValueChange = { onKlasaIndexChange(it.toInt()) },
                        onValueChangeFinished = { playSoundAction(R.raw.page, 2) },
                        interactionSource = sliderInteractionKlasa,
                        valueRange = 0f..maxOf(0f, (dostepneKlasy.size - 1).toFloat()),
                        steps = maxOf(0, dostoczoneSteps(dostepneKlasy.size)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                IconButton(onClick = {
                    onRollSkills(wybranaKlasa)
                    playSoundAction(R.raw.klik1, 0)
                    onNext()
                }, modifier = Modifier.size(60.dp)) {
                    Icon(Icons.Default.Check, contentDescription = "Dalej", tint = Color.White, modifier = Modifier.size(40.dp))
                }
            } else {
                Text("BRAK DOSTĘPNYCH KLAS", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("(Wymagana suma rzutów > 11)", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            // Przycisk "Dalej" bez wyboru klasy
            IconButton(onClick = {
                playSoundAction(R.raw.klik1, 0)
                onNext()
            }, modifier = Modifier.size(60.dp)) {
                Icon(Icons.Default.Check, contentDescription = "Dalej", tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AdeptSelectionScreen(
    wylosowaneNormalne: List<String>,
    wylosowaneSpecjalne: List<String>,
    wylosowaneAdepty: List<String>,
    onAdeptSelected: (String) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(8.dp)) {
        Text("UMIEJĘTNOŚCI", color = Color.Yellow, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                // Sortowanie wg rangi: Mistrz > Ekspert > Adept > Uczeń, potem alfabetycznie
                fun getRankWeight(s: String): Int = when {
                    s.contains("𝕸𝖎𝖘𝖙𝖗𝖟") -> 1
                    s.contains("𝕰𝖐𝖘𝖕𝖊𝖗𝖙") -> 2
                    s.contains("𝕬𝖉𝖊𝖕𝖙") -> 3
                    else -> 4
                }

                val sortedSkills = wylosowaneNormalne.sortedWith(compareBy({ getRankWeight(it) }, { it }))

                wylosowaneSpecjalne.sorted().forEach { spec ->
                    Text("• $spec", color = Color.Green, fontSize = 11.sp, modifier = Modifier.padding(vertical = 1.dp))
                }

                sortedSkills.forEach { skill ->
                    val isAdeptCandidate = wylosowaneAdepty.contains(skill.substringBefore(" (").trim())
                    Text("• $skill", color = if (isAdeptCandidate) Color.Yellow else Color.White, fontSize = 11.sp, modifier = Modifier.padding(vertical = 1.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("AWANSUJ NA ADEPTA:", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)) {
            wylosowaneAdepty.forEach { adeptSkill ->
                Button(
                    onClick = {
                        onAdeptSelected(adeptSkill)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text(adeptSkill, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun EquipmentNameSelectionScreen(
    wybranaKlasaIndex: Int,
    wybranaRasaIndex: Int,
    wybranyPoziom: Int,
    wygenerowaneStaty: List<Triple<String, IntArray, Int>>?,
    wylosowaneNormalne: List<String>,
    wylosowaneSpecjalne: List<String>,
    wylosowaneAdepty: List<String>,
    heroName: String,
    onHeroNameChange: (String) -> Unit,
    heroPortraitUri: String?,
    onPickPortrait: () -> Unit,
    nameSuggestions: List<String>,
    selectedNameSuggestionIndex: Int,
    onNameSuggestionIndexChange: (Int) -> Unit,
    selectedEquipment: Set<String>,
    onEquipmentChange: (Set<String>) -> Unit,
    selectedShopType: String?,
    onShopTypeChange: (String?) -> Unit,
    lobsterFamily: FontFamily,
    onNext: () -> Unit
) {
    val dKlasy = MainActivity.listaKlasa.filterIndexed { index, _ ->
        val statIdx = when(index) {
            0 -> 0; 1 -> 4; 2 -> 1; 3 -> 5; 4 -> 7; 5 -> 6; else -> 0
        }
        val stat = wygenerowaneStaty?.getOrNull(statIdx)
        if (stat != null) (stat.second[1] + stat.second[2]) > 11 else false
    }
    val klasa = dKlasy.getOrNull(wybranaKlasaIndex) ?: if (wylosowaneAdepty.isEmpty()) "Wojownik" else "Brak"

    // Dynamiczne dobieranie ekwipunku startowego
    LaunchedEffect(klasa, wylosowaneNormalne) {
        val autoEquip = mutableSetOf<String>()

        val itemBag = (1..5).random()
        if (itemBag < 3){
            autoEquip.add("Plecak")
        }
        else {
            autoEquip.add("Torba podróżna")
        }

        // Reguła dla Maga i Kapłana
        if (klasa == "Mag" || klasa == "Kapłan") {
            autoEquip.add("Kostur")
        }
        // Reguła dla Łotra i Łowcy
        if (klasa == "Łotr" || klasa == "Łowca") {
            autoEquip.add("Sztylet")
        }

        if (wybranaRasaIndex == 0){ // Człowiek
            if (klasa == "Łowca") {
                autoEquip.add("Łuk")
                autoEquip.add("Strzały (10 szt.)")
            }
        }
        if (wybranaRasaIndex == 1){ // Elf
            if (klasa == "Łowca") {
                autoEquip.add("Łuk Elfi")
                autoEquip.add("Strzały (6 szt.)")
            }
        }
        if (wybranaRasaIndex == 2){ // Khazad
            autoEquip.add("Sztylet [2]")
        }
        if (wybranaRasaIndex == 3){ // Nowa rasa
            autoEquip.add("Sztylet [3]")
        }
        
        autoEquip.add("Hubka i krzesiwo")
        autoEquip.add("Koc")
        autoEquip.add("Bukłak 2L")

        // Reguła dla umiejętności "Specjalna broń ..."
        (wylosowaneNormalne + wylosowaneSpecjalne).forEach { skill ->
            if (skill.contains("Specjalna broń", ignoreCase = true) || skill.startsWith("Broń:", ignoreCase = true)) {
                // Wyciągamy kategorię (np. z nawiasu lub po dwukropku)
                val category = when {
                    skill.contains("(") -> skill.substringAfter("(").substringBefore(")").trim()
                    skill.contains(":") -> skill.substringAfter(":").trim()
                    else -> skill.replace("Specjalna broń", "").trim()
                }

                // Losujemy broń z tej kategorii z ItemRegistry
                val randomWeapon = ItemRegistry.weapons.filter {
                    it.subCategory.contains(category, ignoreCase = true) ||
                            it.name.contains(category, ignoreCase = true)
                }.randomOrNull()?.name

                if (randomWeapon != null) autoEquip.add(randomWeapon)
            }

            // Broń bojowa obuchowa
            if (skill.contains("Broń bojowa obuchowa", ignoreCase = true) ) {
                autoEquip.add("🅔 " + HeroLogic.WW2nazwa.random() + " [O]")
            }

            // Broń bojowa sieczna
            if (skill.contains("Broń bojowa sieczna", ignoreCase = true) ) {
                autoEquip.add("🅔 " + HeroLogic.WW1nazwa.random() + " [S]")
            }

            // Broń bojowa kłuta
            if (skill.contains("Broń bojowa kłuta", ignoreCase = true) ) {
                autoEquip.add("🅔 " + HeroLogic.WW3nazwa.random() + " [K]")
            }


            ///Przeniesione
            if (skill.contains("Jeździectwo", ignoreCase = true) || skill.contains("Walka na wierzchowcu", ignoreCase = true) ) {
                autoEquip.add("Koń (Wierzchowy)")
            }
            if (skill.contains("Powożenie", ignoreCase = true) ) {
                autoEquip.add("Koń pociągowy (Draft)")
                autoEquip.add("Lekka bryczka (1 koń)")
            }
            if (skill.contains("Czytanie i pisanie", ignoreCase = true) ) {
                autoEquip.add("Przybory piśmiennicze")
            }
            if (skill.contains("Chemia", ignoreCase = true) ) {
                autoEquip.add("Przybory Alchemiczne")
            }

            if (skill.contains("Otwieranie zamków", ignoreCase = true) ) {
                autoEquip.add("Wytrychy")
            }

            if (skill.contains("Muzykalność", ignoreCase = true) ) {
                val instrument = when {
                    skill.contains("strunowe", ignoreCase = true) -> HeroLogic.instrumentsSpecialist[0] // Lutnia
                    skill.contains("dęte", ignoreCase = true) ->   HeroLogic.instrumentsSpecialist[2] // Flet lub Dudy
                    skill.contains("perkusja", ignoreCase = true) -> HeroLogic.instrumentsSpecialist[3] // Bębenek
                    else -> HeroLogic.instrumentsSpecialist.random()
                }
                autoEquip.add("${instrument.name} (${instrument.bonus.ifEmpty { "Instrument" }})")
            }

            ///

        }

        //Pancerz
        getStartingArmor(klasa).forEach { piece ->
            autoEquip.add(" ${piece.name} [${piece.ppS}/${piece.ppO}/${piece.ppK}] ${((piece.weight).toFloat() / 10F).toString() + "㎏" }")
        }

        // Generowanie pieniędzy
        val zk = if ((0..99).random() < 5) 1 else 0
        val s = (1..20).random()
        val p = 10 + (1..10).random()

        autoEquip.add("Mała sakiewka")
        if (zk > 0) autoEquip.add("⛀ Złota korona (zk)  $zk")
        autoEquip.add("⛀ Srebrny szyling (s)  $s")
        autoEquip.add("⛀ Miedziany pens (p)  $p")

        onEquipmentChange(autoEquip)
    }

    val shopCategories = listOf("Broń", "Pancerz", "Ubiór")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
       
        // Pole imienia i Portret
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .clickable { onPickPortrait() },
                contentAlignment = Alignment.Center
            ) {
                if (heroPortraitUri != null) {
                    AsyncImage(
                        model = heroPortraitUri,
                        contentDescription = "Portrait",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.unknowhero),
                        contentDescription = "Default Portrait",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            OutlinedTextField(
                value = heroName,
                onValueChange = { onHeroNameChange(it) },
                label = { Text("Imię bohatera", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = Color.White, fontSize = 24.sp, fontFamily = lobsterFamily),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Yellow,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color.Yellow
                )
            )
        }

        // Suwak imion
        if (nameSuggestions.isNotEmpty()) {
            Slider(
                value = selectedNameSuggestionIndex.toFloat(),
                onValueChange = {
                    val index = it.toInt()
                    onNameSuggestionIndexChange(index)
                    onHeroNameChange(nameSuggestions[index])
                },
                valueRange = 0f..9f,
                steps = 8,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            )
        }

        if (wybranyPoziom > 0) {
            Spacer(modifier = Modifier.height(8.dp))

            // Zakładki sklepu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                shopCategories.forEach { category ->
                    val isSelected = selectedShopType == category
                    Text(
                        text = category,
                        color = if (isSelected) Color.Yellow else Color.Gray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clickable { onShopTypeChange(if (isSelected) null else category) }
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Podsumowanie Pancerza (PP)
        val equippedArmor = getEquippedArmorPieces(selectedEquipment)
        if (equippedArmor.isNotEmpty()) {
            val protection = calculateTotalProtection(equippedArmor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                protection.forEach { (loc, pp) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(loc.take(1).uppercase(), color = Color.Gray, fontSize = 10.sp)
                        Text("${pp.first}/${pp.second}/${pp.third}", color = Color.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Lista przedmiotów
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (selectedShopType == null) {
                // Wybrany ekwipunek - LISTA TEKSTOWA BEZ USUWANIA
                val classEquipment = selectedEquipment.toList()
                items(classEquipment) { item ->
                    val details = getItemDetails(item)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            text = "• $item",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (details.isNotBlank()) {
                            Text(
                                text = details,
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            } else {
                // Lista ze sklepu - TU ZOSTAWIAMY MOŻLIWOŚĆ DODAWANIA/USUWANIA
                val shopItems: List<String> = when (selectedShopType) {
                    "Broń" -> ItemRegistry.weapons.map { it.name }
                    "Pancerz" -> ItemRegistry.armors.map { it.name }
                    else -> ItemRegistry.others.map { it.name }
                }

                items(shopItems, key = { it }) { item ->
                    val isSelected = selectedEquipment.contains(item)
                    EquipmentItemRow(
                        item = item,
                        isSelected = isSelected,
                        isRestricted = false,
                        onClick = {
                            onEquipmentChange(if (isSelected) selectedEquipment - item else selectedEquipment + item)
                        }
                    )
                }
            }
        }


        Button(
            onClick = { onNext() },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
            enabled = heroName.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black)
        ) {
            Text("PODSUMOWANIE", fontWeight = FontWeight.Bold)
        }
    }
}

fun getItemDetails(item: String): String {
    val cleanName = item.replace("🅔 ", "").replace("🛡️ ", "").trim()

    // 1. Sprawdzanie Broni
    // Broń Prosta (WW0)
    var idx = HeroLogic.WW0nazwa.indexOfFirst { it.equals(cleanName, ignoreCase = true) }
    if (idx != -1) {
        val type = HeroLogic.WW0robr[idx]
        val dice = "K${HeroLogic.WW0kX[idx]}"
        val size = HeroLogic.WW0rozmiar[idx]
        val rozmiarBroni = HeroLogic.itemRozmiar[size]
        val weight = HeroLogic.WW0wo[idx] / 1000.0
        return "BROŃ: [$type], 🎲 $dice, $rozmiarBroni, Waga: ${String.format(Locale.US, "%.2f", weight)} kg"
    }

    // Broń Sieczna (WW1)
    idx = HeroLogic.WW1nazwa.indexOfFirst { it.equals(cleanName, ignoreCase = true) }
    if (idx != -1) {
        val dice = "K${HeroLogic.WW1kX[idx]}"
        val size = HeroLogic.WW1rozmiar[idx]
        val rozmiarBroni = HeroLogic.itemRozmiar[size]
        val weight = HeroLogic.WW1wo[idx] / 1000.0
        return "BROŃ: [S], 🎲 $dice, $rozmiarBroni, Waga: ${String.format(Locale.US, "%.2f", weight)} kg"
    }

    // Broń Obuchowa (WW2)
    idx = HeroLogic.WW2nazwa.indexOfFirst { it.equals(cleanName, ignoreCase = true) }
    if (idx != -1) {
        val dice = "K${HeroLogic.WW2kX[idx]}"
        val size = HeroLogic.WW2rozmiar[idx]
        val rozmiarBroni = HeroLogic.itemRozmiar[size]
        val weight = HeroLogic.WW2wo[idx] / 1000.0
        return "BROŃ: [O], 🎲 $dice, $rozmiarBroni, Waga: ${String.format(Locale.US, "%.2f", weight)} kg"
    }

    // Broń Kłuta (WW3)
    idx = HeroLogic.WW3nazwa.indexOfFirst { it.equals(cleanName, ignoreCase = true) }
    if (idx != -1) {
        val dice = "K${HeroLogic.WW3kX[idx]}"
        val size = HeroLogic.WW3rozmiar[idx]
        val rozmiarBroni = HeroLogic.itemRozmiar[size]
        val weight = HeroLogic.WW3wo[idx] / 1000.0
        return "BROŃ: [K], 🎲 $dice, $rozmiarBroni, Waga: ${String.format(Locale.US, "%.2f", weight)} kg"
    }

    // Broń Egzotyczna (WW4)
    idx = HeroLogic.WW4nazwa.indexOfFirst { it.equals(cleanName, ignoreCase = true) }
    if (idx != -1) {
        val dice = "K${HeroLogic.WW4kX[idx]}"
        val size = HeroLogic.WW4rozmiar[idx]
        val rozmiarBroni = HeroLogic.itemRozmiar[size]
        val weight = HeroLogic.WW4wo[idx] / 1000.0
        return "BROŃ (Egz.): [S], 🎲 $dice, $rozmiarBroni, Waga: ${String.format(Locale.US, "%.2f", weight)} kg"
    }

    // 2. Sprawdzanie Pancerza
    val allArmor = HeroLogic.armorLight + HeroLogic.armorMedium + HeroLogic.armorHeavy
    val armor = allArmor.find { it.name.equals(cleanName, ignoreCase = true) }
    if (armor != null) {
        val loc = armor.locations.joinToString(", ")
        val prot = "[${armor.ppS}/${armor.ppO}/${armor.ppK}]"
        val weight = armor.weight / 10.0
        return "PANCERZ: $loc, Ochrona $prot, Waga: ${String.format(Locale.US, "%.1f", weight)} kg"
    }

    // 3. Sprawdzanie Specjalnych Przedmiotów
    val allSpecial = HeroLogic.toolsSpecialist + HeroLogic.instrumentsSpecialist + HeroLogic.itemsExotic
    val spec = allSpecial.find { it.name.equals(cleanName, ignoreCase = true) }
    if (spec != null) {
        val weight = spec.weight / 10.0
        return "PRZEDMIOT: ${spec.category}, Waga: ${String.format(Locale.US, "%.1f", weight)} kg"
    }

    return ""
}
@Composable
fun EquipmentItemRow(
    item: String,
    isSelected: Boolean,
    isRestricted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp)
            .clickable(enabled = !isRestricted) { onClick() }
            .background(
                if (isRestricted) Color.Red.copy(alpha = 0.1f)
                else if (isSelected) Color(0x3300FFFF)
                else Color.Transparent
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRestricted) {
            Text("🛑", fontSize = 20.sp)
        } else {
            Icon(
                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isSelected) Color.Green else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (item.startsWith("⛀")) {
            val coinColor = when {
                item.contains("Złota") -> Color(0xFFFFD700)
                item.contains("Srebrny") -> Color(0xFFC0C0C0)
                item.contains("Miedziany") -> Color(0xFFCD7F32)
                else -> Color.Yellow
            }
            Text(
                item.trim(),
                color = coinColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = if (isRestricted) TextDecoration.LineThrough else null
            )
        } else {
            Text(
                item,
                color = if (isRestricted) Color.Gray else if (isSelected) Color.White else Color.Gray,
                fontSize = 16.sp,
                textDecoration = if (isRestricted) TextDecoration.LineThrough else null
            )
        }
    }
    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(horizontal = 16.dp))
}

fun getStartingArmor(klasa: String): List<ArmorPiece> {
    return when (klasa) {
        "Wojownik" -> listOf(
            HeroLogic.armorHeavy[0], // Hełm płytowy
            HeroLogic.armorHeavy[1], // Kirys stalowy
            HeroLogic.armorHeavy[2], // Naramienniki płytowe
            HeroLogic.armorHeavy[3], // Nagolenniki płytowe
            HeroLogic.armorHeavy[4]  // Rękawice płytowe
        )
        "Łotr", "Łowca" -> {
            // Średni - losowo kolczuga lub łuski (np. korpus i ramiona)
            val type = if (Random.nextBoolean()) 0 else 1 // 0: Kolcza, 1: Łuskowa
            if (type == 0) {
                listOf(HeroLogic.armorMedium[0], HeroLogic.armorMedium[4], HeroLogic.armorMedium[8]) // Czepiec, Kolczuga, Nogawice
            } else {
                listOf(HeroLogic.armorMedium[1], HeroLogic.armorMedium[5], HeroLogic.armorMedium[9]) // Czepiec, Łuski, Nogawice
            }
        }
        "Uczony", "Kapłan", "Mag" -> {
            // Lekki - skóra lub ćwiekowana
            val isStudded = Random.nextBoolean()
            if (isStudded) {
                listOf(HeroLogic.armorLight[1], HeroLogic.armorLight[3], HeroLogic.armorLight[9]) // Ćwiekowane: Czapa, Kaftan, Nogawice
            } else {
                listOf(HeroLogic.armorLight[0], HeroLogic.armorLight[2], HeroLogic.armorLight[8]) // Skórzane: Czapa, Kaftan, Nogawice
            }
        }
        else -> emptyList()
    }
}

fun getEquippedArmorPieces(selectedEquipment: Set<String>): List<ArmorPiece> {
    val allArmor = HeroLogic.armorLight + HeroLogic.armorMedium + HeroLogic.armorHeavy
    return selectedEquipment.mapNotNull { item ->
        val cleanName = item.replace("🅔 ", "").replace("🛡️ ", "").trim()
            .substringBefore(" [") // Usuwamy statystyki z nazwy, np. [1/1/1]
            .trim()
        allArmor.find { it.name.equals(cleanName, ignoreCase = true) }
    }
}

fun calculateTotalProtection(equippedArmor: List<ArmorPiece>): Map<String, Triple<Int, Int, Int>> {
    val locations = listOf("Głowa", "Korpus", "Ramiona", "Nogi")
    val protectionMap = mutableMapOf<String, Triple<Int, Int, Int>>()

    for (loc in locations) {
        var s = 0
        var o = 0
        var k = 0
        equippedArmor.filter { it.locations.contains(loc) }.forEach {
            s += it.ppS
            o += it.ppO
            k += it.ppK
        }
        protectionMap[loc] = Triple(s, o, k)
    }
    return protectionMap
}
