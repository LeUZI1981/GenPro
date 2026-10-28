package com.example.genpro

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpro.ui.AdeptSelectionScreen
import com.example.genpro.ui.CharacterInitialSetupScreen
import com.example.genpro.ui.CharacterSummary
import com.example.genpro.ui.EquipmentNameSelectionScreen
import com.example.genpro.ui.SecondaryStatsScreen
import com.example.genpro.ui.StatsAndClassSelectionScreen
import com.example.genpro.ui.GMToolsScreen
import com.example.genpro.ui.BuildingGeneratorScreen
import com.example.genpro.ui.SessionScreen
import com.example.genpro.ui.SettingsScreen
import com.example.genpro.ui.poziomStartowy
import com.example.genpro.ui.theme.GenProTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_MENU_APP = "EXTRA_MENU_APP"
        const val EXTRA_HERO_ID = "EXTRA_HERO_ID"

        val listaRasa = arrayOf("Człowiek", "Elf", "Krasnolud")
        val listaKlasa = arrayOf("Wojownik", "Łotr", "Łowca", "Uczony", "Kapłan", "Mag")
        val listaGender = arrayOf("Mężczyzna", "Kobieta")
        val statNames = arrayOf("Walka Wręcz", "Strzelectwo", "Krzepa", "Odporność", "Zręczność", "Inteligencja", "Siła Woli", "Charyzma")
        val statNamesAbbr = arrayOf("WW", "US", "K", "O", "Zr", "Int", "SW", "Og")
    }

    private var menuAppState = mutableIntStateOf(0)
    private var selectedHeroIdState = mutableIntStateOf(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            GenProTheme {
                MainScreen(
                    menuApp = menuAppState.intValue,
                    onMenuAppChange = { menuAppState.intValue = it },
                    selectedHeroId = selectedHeroIdState.intValue,
                    isEditing = { },
                    onSelectedHeroIdChange = { selectedHeroIdState.intValue = it }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (it.hasExtra(EXTRA_MENU_APP)) {
                val newMenuApp = it.getIntExtra(EXTRA_MENU_APP, 0)
                // If we are returning from Combat (Arena/City hub) we might want to stay in City menu
                menuAppState.intValue = newMenuApp
            }
            if (it.hasExtra(EXTRA_HERO_ID)) {
                selectedHeroIdState.intValue = it.getIntExtra(EXTRA_HERO_ID, 1)
            }
        }
    }
}

@Composable
fun MainScreen(
    menuApp: Int,
    onMenuAppChange: (Int) -> Unit,
    selectedHeroId: Int,
    isEditing: (Boolean) -> Unit,
    onSelectedHeroIdChange: (Int) -> Unit
) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val playSoundAction: (Int, Int) -> Unit = { resId: Int, part: Int ->
        playSound(context, coroutineScope, resId, part)
    }

    val setApp = remember { arrayOf("dataStart", "dataNow", "wersja", "poziomMax", "lastHero") }
    val listaWiek = remember { arrayOf("Młody", "Dojrzały", "Doświadczony", "Weteran") }
    val znakiZodiaku = remember { arrayOf("Bęben", "Dudy", "Gwiezdny Wóz", "Idzi", "Kocioł", "Lancet", "Mędrzec", "Muł", "Pasek", "Piorun", "Prawy Szal", "Rozbite Koło", "Ryba", "Skorpion", "Smok", "Sznur", "Tłusta Koza", "Trzy Prządki", "Vobist", "Wół", "Wyspa", "Zegar", "Złota Rybka") }
    val koloryOczu = remember { arrayOf("Mgliste", "Szaroniebieskie", "Niebieskie", "Zielone", "Orzechowe", "Jasnobrązowe", "Ciemnobrązowe", "Szarozielone", "Jasnoszare", "Fioletowe", "Czerwone", "Granatowe") }
    val koloryWlosow = remember { arrayOf("Łysa glaca", "Czarne", "Białe", "Jasny blond", "Blond", "Płowe", "Siwe", "Brązowe", "Rude", "Miedziane", "Czerwone", "Beżowe", "Zielone", "Fioletowe", "Granatowe", "Niebieskie", "Srebrne") }

    val autourFamily = remember { FontFamily(Font(R.font.autourone)) }
    val nosiferFamily = remember { FontFamily(Font(R.font.nosifer)) }
    val aladinFamily = remember { FontFamily(Font(R.font.aladin)) }
    val lobsterFamily = remember { FontFamily(Font(R.font.lobster)) }
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val titleScale = remember { Animatable(0f, Float.VectorConverter) }
    val k10Rotation = remember { Animatable(0f, Float.VectorConverter) }
    val k10Scale = remember { Animatable(0f, Float.VectorConverter) }
    val k10OffsetY = remember { Animatable(100f, Float.VectorConverter) }
    val titleOffsetY = remember { Animatable(screenHeight.toFloat() / 2 - 100f, Float.VectorConverter) }
    val heroImageOffsetY = remember { Animatable(screenHeight.toFloat(), Float.VectorConverter) }
    var introFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        playSoundAction(R.raw.start, 0)
        playSound(context, coroutineScope, R.raw.fanmusic, 0)
        titleScale.animateTo(1.3f, animationSpec = tween(1000))
        delay(500)
        
        launch {
            titleOffsetY.animateTo(0f, animationSpec = tween(2000, easing = LinearOutSlowInEasing))
        }
        launch {
            k10Rotation.animateTo(720f, animationSpec = tween(2000, easing = LinearOutSlowInEasing))
        }
        launch {
            titleScale.animateTo(1f, animationSpec = tween(2000, easing = LinearOutSlowInEasing))
        }
        launch {
            k10Scale.animateTo(1f, animationSpec = tween(2000, easing = LinearOutSlowInEasing))
        }
        launch {
            k10OffsetY.animateTo(0f, animationSpec = tween(2000, easing = LinearOutSlowInEasing))
        }
        launch {
            heroImageOffsetY.animateTo(0f, animationSpec = tween(2000, easing = LinearOutSlowInEasing))
        }
        
        delay(2000)
        introFinished = true
    }

    val slaboFamily = remember { FontFamily(Font(R.font.slabo27px)) }

    var isGMMode by remember { mutableStateOf(false) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isSettingsEditing by remember { mutableStateOf(false) }

    // Character creation state
    var wybranaRasaIndex by remember { mutableIntStateOf(0) }
    var wybranaPlecIndex by remember { mutableIntStateOf(0) } // 0: Men, 1: Women
    var wybranyWiekIndex by remember { mutableIntStateOf(0) }
    var wybranaKlasaIndex by remember { mutableIntStateOf(0) }
    var wybranyPoziom by remember { mutableIntStateOf(0) }
    var wylosowaneAdepty by remember { mutableStateOf<List<String>>(emptyList()) }
    var wybranyAdept by remember { mutableStateOf("") }
    var heroPortraitUri by remember { mutableStateOf<String?>(null) }
    var heroName by remember { mutableStateOf("") }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            heroPortraitUri = uri.toString()
        }
    }
    var selectedEquipment by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedShopType by remember { mutableStateOf<String?>(null) }
    var nameSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedNameSuggestionIndex by remember { mutableIntStateOf(0) }

    // Animacja kości
    var wygenerowaneStaty by remember { mutableStateOf<List<Triple<String, IntArray, Int>>?>(null) }
    var statyDrugorzedne by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var cechyFizyczne by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var bazaCechyFizyczne by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var wylosowaneCechyCharakteru by remember { mutableStateOf<List<String>>(emptyList()) }
    var wylosowaneSpecjalne by remember { mutableStateOf<List<String>>(emptyList()) }
    var wylosowaneNormalne by remember { mutableStateOf<List<String>>(emptyList()) }
    var manualSkills by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var expandedStats by remember { mutableStateOf<Set<String>>(emptySet()) }

    var currentHeroProgress by remember { mutableStateOf<HeroProgress?>(null) }

    val getCategoryByName = { name: String ->
        when (name) {
            "Walka Wręcz" -> HeroLogic.skillWW
            "US" -> HeroLogic.skillUS
            "Krzepa" -> HeroLogic.skillKrzepa
            "Odporność" -> HeroLogic.skillOdpornosc
            "Zręczność" -> HeroLogic.skillZrecznosc
            "Inteligencja" -> HeroLogic.skillInteligencja
            "Siła Woli" -> HeroLogic.skillSW
            "Charyzma" -> HeroLogic.skillCharyzma
            "Wiedza" -> HeroLogic.skillWiedza
            "Język" -> HeroLogic.skillJezyk
            else -> emptyArray<String>()
        }
    }

    val rollCharacteristicTraits = {
        // 4. Cechy Charakterystyczne (Z Twojej starej pracy)
        val kobieta1 = arrayOf("Oczy różnego koloru","Kolczyki w uszach","Kolczyk w brwii","Duże piersi","Szerokie biodra","Wąskie biodra","Małe piersi","Tatuaż","Długie paznokcie")
        val mezczyzna1 = arrayOf("Oczy różnego koloru","Kolczyk w nosie","Kolczyk w uchu","Wystające zęby","Poszarpane ucho","Złamany nos","Krzywy nos","Tatuaż","Tatuaż")
        val kobieta2 = arrayOf("Łysa","Krótkie włosy","Długie włosy","Kręcone włosy","Długie włosy spiralne","Kolor włosów","Kolor włosów","Kolor włosów")
        val mezczyzna2 = arrayOf("Łysy","Haczykowaty nos","Krzaczaste brwii","Króciutka broda","Długa Broda","Kolor włosów","Kolor włosów","Kolor włosów")

        val rasa = MainActivity.listaRasa[wybranaRasaIndex]
        val plec = MainActivity.listaGender[wybranaPlecIndex]
        val cechyRez = mutableListOf<String>()

        var targetCount = when (wybranyWiekIndex) {
            0 -> if ((1..10).random() <= 5) 0 else 1
            1 -> if ((1..10).random() <= 5) 1 else 2
            2 -> {
                val r = (1..10).random()
                if (r <= 4) 1 else if (r <= 8) 2 else 3
            }
            3 -> {
                val r = (1..10).random()
                if (r <= 4) 2 else if (r <= 8) 3 else 4
            }
            else -> 1
        }
        if (rasa == "Elf") {
            targetCount += 1
            if ((1..100).random() <= 33) {
                val specialElfTrait = arrayOf(
                    "Bardzo białe zęby (Cha+5%)",
                    "Czysty głos (Cha+5%)",
                    "Atrakcyjna twarz (Cha+5%)",
                    "Charyzmatyczne oczy (Cha+5%)"
                ).random()
                cechyRez.add(specialElfTrait)
            }
        }

        val wykluczenia = mapOf(
            "Potężna budowa ciała (Odp+5%)" to listOf("Mizerna budowa ciała (Odp-5%)", "Chudy (Żyw-2)", "Chuda (Żyw-2)"),
            "Mizerna budowa ciała (Odp-5%)" to listOf("Potężna budowa ciała (Odp+5%)", "Otyły (Żyw+2)", "Otyła (Żyw+2)"),
            "Otyły (Żyw+2)" to listOf("Chudy (Żyw-2)", "Mizerna budowa ciała (Odp-5%)"),
            "Otyła (Żyw+2)" to listOf("Chuda (Żyw-2)", "Mizerna budowa ciała (Odp-5%)"),
            "Chudy (Żyw-2)" to listOf("Otyły (Żyw+2)", "Potężna budowa ciała (Odp+5%)"),
            "Chuda (Żyw-2)" to listOf("Otyła (Żyw+2)", "Potężna budowa ciała (Odp+5%)"),
            "Wysoki (Sz+1)" to listOf("Niski (Sz-1)", "Kulawy (Sz-1)"),
            "Wysoka (Sz+1)" to listOf("Niska (Sz-1 Cha+5)", "Kulawa (Sz-1)"),
            "Niski (Sz-1)" to listOf("Wysoki (Sz+1)"),
            "Niska (Sz-1 Cha+5)" to listOf("Wysoka (Sz+1)", "Szpetny (Cha-10)", "Szyderczy sposób bycia (Cha-5%)", "Szyderczy wyraz twarzy (Cha-5%)", "Blizny na twarzy (Cha-5)", "Brak zęba"),
            "Kulawy (Sz-1)" to listOf("Wysoki (Sz+1)"),
            "Kulawa (Sz-1)" to listOf("Wysoka (Sz+1)"),
            "Szpetny (Cha-10)" to listOf("Bardzo białe zęby (Cha+5%)", "Czysty głos (Cha+5%)", "Atrakcyjna twarz (Cha+5%)", "Charyzmatyczne oczy (Cha+5%)", "Niska (Sz-1 Cha+5)"),
            "Bardzo białe zęby (Cha+5%)" to listOf("Szpetny (Cha-10)", "Brak zęba"),
            "Brak zęba" to listOf("Bardzo białe zęby (Cha+5%)"),
            "Atrakcyjna twarz (Cha+5%)" to listOf("Szpetny (Cha-10)", "Blizny na twarzy (Cha-5)", "Szrama na twarzy (Cha±5%)", "Szyderczy wyraz twarzy (Cha-5%)"),
            "Zręczne dłonie (Zr+5%)" to listOf("Niezgrabne dłonie (Zr-5%)"),
            "Zgrabna (Zr+5%)" to listOf("Niezgrabne dłonie (Zr-5%)"),
            "Niezgrabne dłonie (Zr-5%)" to listOf("Zręczne dłonie (Zr+5%)", "Zgrabna (Zr+5%)"),
            "Czysty głos (Cha+5%)" to listOf("Szyderczy sposób bycia (Cha-5%)"),
            "Szyderczy sposób bycia (Cha-5%)" to listOf("Czysty głos (Cha+5%)", "Charyzmatyczne oczy (Cha+5%)"),
            "Charyzmatyczne oczy (Cha+5%)" to listOf("Szpetny (Cha-10)", "Szyderczy sposób bycia (Cha-5%)", "Ślepota")
        )

        while (cechyRez.size < targetCount) {
            val los = (1..25).random()
            var tempStr = ""
            var modCecha = (0..7).random() // Uproszczony modCecha dla logiki rasy

            // Logika losowania 1-25
            if (los <= 3) {
                var neutral = (1..7).random()
                if (rasa == "Elf") neutral = modCecha.coerceIn(0, 8)
                if (rasa == "Druhli") neutral = (7 - modCecha).coerceIn(0, 8)
                if (rasa == "Khazad") neutral = (1..2).random()

                tempStr = if (plec == "Kobieta") kobieta1[neutral.coerceIn(0, 8)] else mezczyzna1[neutral.coerceIn(0, 8)]

                if (tempStr == "Tatuaż") {
                    val jaki = listOf("Mały", "Duży", "Średni", "Artystyczny", "Mroczny").random()
                    val gdzie = listOf("twarzy", "ramieniu", "plecach", "nodze", "torsie", "dłoni").random()
                    var finalGdzie = gdzie
                    if (rasa == "Druhli") finalGdzie = "twarzy"
                    if (plec == "Kobieta" && finalGdzie == "torsie") finalGdzie = "dekolcie"
                    tempStr = "$jaki Tatuaż na $finalGdzie"
                }
            } else if (los == 4 || los == 5) {
                var neutral = (0..5).random()
                if (rasa != "Elf" && rasa != "Druhli") {
                    if (rasa == "Khazad") neutral = (3..5).random()
                    tempStr = if (plec == "Kobieta") kobieta2[neutral] else mezczyzna2[neutral]
                } else {
                    neutral = if (rasa == "Druhli") (0..2).random() else 5
                    tempStr = if (plec == "Kobieta") kobieta2[neutral] else mezczyzna2[neutral]
                }
                
                if (tempStr == "Kolor włosów") {
                    val specialChance = if (rasa == "Elf") 50 else if (rasa == "Khazad") 25 else if (rasa == "Człowiek") 15 else 0
                    if ((1..100).random() <= specialChance) {
                        val kolor = listOf("Czerwone", "Beżowe", "Zielone", "Fioletowe", "Granatowe", "Niebieskie", "Srebrne").random()
                        tempStr = "Kolor włosów: $kolor"
                    }
                }
            } else if (los == 6) {
                tempStr = arrayOf("Bardzo białe zęby (Cha+5%)","Czysty głos (Cha+5%)","Atrakcyjna twarz (Cha+5%)","Charyzmatyczne oczy (Cha+5%)").random()
            } else if (los == 7) {
                tempStr = "Brak zęba"
            } else if (los == 8) {
                tempStr = "Szpetny (Cha-10)"
            } else if (los == 9) {
                tempStr = "Płaski nos"
            } else if (los == 10) {
                tempStr = if (plec == "Mężczyzna") "Szeroka klatka piersiowa (Krz+5%)" else "Zgrabna (Zr+5%)"
            } else if (los == 11) {
                tempStr = "Szrama na twarzy (Cha-5)"
            } else if (los == 12) {
                tempStr = "Potężna budowa ciała (Odp+5%)"
            } else if (los == 13) {
                tempStr = "Mizerna budowa ciała (Odp-5%)"
            } else if (los == 14) {
                val cera = (0..4).random()
                tempStr = when {
                    cera < 2 -> "Blada Cera"
                    cera == 4 -> "Egzotyczna karnacja"
                    else -> "Piegi na twarzy"
                }
            } else if (los == 15) {
                tempStr = "Donośny głos (Cha±5%)"
            } else if (los == 16) {
                tempStr = arrayOf("Ślepota","Jedno Oko [Prawe]","Jedno Oko [Lewe]","Jedna Ręka [Lewa]","Jedna Ręka [Prawa]").random()
            } else if (los == 17) {
                tempStr = "Szyderczy sposób bycia (Cha-5%)"
            } else if (los == 18) {
                tempStr = if ((0..1).random() == 0) "Zręczne dłonie (Zr+5%)" else "Niezgrabne dłonie (Zr-5%)"
            } else if (los == 19) {
                tempStr = "Szyderczy wyraz twarzy (Cha-5%)"
            } else if (los == 20) {
                tempStr = "Blizny na twarzy (Cha-5)"
            } else if (los == 21) {
                val uraz = (0..4).random()
                tempStr = if (plec == "Kobieta") {
                    when(uraz) {
                        0 -> "Kulawa (Sz-1)"; 1 -> "Niska (Sz-1 Cha+5)"; 2 -> "Wysoka (Sz+1)"; 3 -> "Otyła (Żyw+2)"; else -> "Chuda (Żyw-2)"
                    }
                } else {
                    when(uraz) {
                        0 -> "Kulawy (Sz-1)"; 1 -> "Niski (Sz-1)"; 2 -> "Wysoki (Sz+1)"; 3 -> "Otyły (Żyw+2)"; else -> "Chudy (Żyw-2)"
                    }
                }
            } else {
                val znamieJakie = listOf("Małe", "Duże", "Wyraźne", "Ciemne", "Jasne").random()
                val gdzie = listOf("głowie", "szyi", "ramieniu", "plecach", "nodze", "piersi").random()
                tempStr = "$znamieJakie znamię na $gdzie"
            }

            if (tempStr.isNotEmpty()) {
                val isExcluded = cechyRez.any { wykluczenia[it]?.contains(tempStr) == true || wykluczenia[tempStr]?.contains(it) == true }
                val alreadyHas = cechyRez.contains(tempStr)
                if (!isExcluded && !alreadyHas) {
                    cechyRez.add(tempStr)
                }
            }
        }
        wylosowaneCechyCharakteru = cechyRez

        // Aplikowanie modyfikatorów z cech do statystyk i wyglądu
        // Resetujemy statystyki i wygląd do bazy przed nałożeniem nowych cech
        val currentPrimary = wygenerowaneStaty ?: emptyList()
        if (currentPrimary.isNotEmpty()) {
            // Obliczamy z bazy (pierwsze 3 rzuty), by modyfikatory cech się nie nakładały
            val s = (currentPrimary[2].second[0] + currentPrimary[2].second[1] + currentPrimary[2].second[2]) / 10
            val w = (currentPrimary[3].second[0] + currentPrimary[3].second[1] + currentPrimary[3].second[2]) / 10
            val a = 1
            val sz = when (wybranaRasaIndex) {
                0 -> 4 // Człowiek
                1 -> 5 // Elf
                2 -> 3 // Krasnolud
                else -> 4
            }
            val zy = when (wybranaRasaIndex) {
                0 -> (1..10).random().let { if (it <= 3) 10 else if (it <= 6) 11 else if (it <= 9) 12 else 13 }
                1 -> (1..10).random().let { if (it <= 3) 9 else if (it <= 6) 10 else if (it <= 9) 11 else 12 }
                2 -> (1..10).random().let { if (it <= 3) 11 else if (it <= 6) 12 else if (it <= 9) 13 else 14 }
                else -> 10
            }
            val pp = when (wybranaRasaIndex) {
                0 -> (1..10).random().let { if (it <= 4) 2 else 3 }
                1 -> (1..10).random().let { if (it <= 4) 1 else 2 }
                2 -> (1..10).random().let { if (it <= 4) 1 else 2 }
                else -> 2
            }
            
            // Modyfikatory z umiejętności
            val allSkills = wylosowaneNormalne + wylosowaneSpecjalne
            var bonusSz = 0
            var bonusA = 0
            var bonusZy = 0
            
            if (allSkills.any { it.contains("Błyskotliwość", ignoreCase = true) }) bonusSz += 1
            if (allSkills.any { it.contains("Szybki refleks", ignoreCase = true) }) bonusSz += 1
            if (allSkills.any { it.contains("Twardziel", ignoreCase = true) }) bonusZy += 1

            statyDrugorzedne = mapOf(
                "Ataki" to (a + bonusA),
                "Żywotność" to (zy + bonusZy),
                "Siła" to s,
                "Wytrzymałość" to w,
                "Szybkość" to (sz + bonusSz),
                "Magia" to 0,
                "Obłęd" to 0,
                "Przeznaczenie" to pp
            )
        }

        var currentStats = wygenerowaneStaty?.map {
            val rolls = it.second.copyOf().apply { this[3] = 0 }
            Triple(it.first, rolls, rolls[0] + rolls[1] + rolls[2])
        }?.toMutableList()
        val currentSec = statyDrugorzedne.toMutableMap()
        val currentPhys = (if (bazaCechyFizyczne.isNotEmpty()) bazaCechyFizyczne else cechyFizyczne).toMutableMap()

        cechyRez.forEach { cecha ->
            if (cecha.contains("(Cha+5%)") || cecha.contains("(Cha+5)") || cecha.contains("Atrakcyjna twarz") || cecha.contains("(Cha-10)") || cecha.contains("(Cha±5%)") || cecha.contains("(Cha-5%)") || cecha.contains("(Cha-5)")) {
                val mod = if (cecha.contains("+5") || cecha.contains("Atrakcyjna twarz")) 5
                          else if (cecha.contains("-10")) -10 
                          else if (cecha.contains("-5") || cecha.contains("±5%")) -5 
                          else 0
                currentStats = currentStats?.map {
                    if (it.first == "Charyzma") Triple(it.first, it.second.copyOf().apply { this[3] += mod }, it.third + mod) else it
                }?.toMutableList()
                if (cecha.contains("Charyzmatyczne oczy")) {
                    currentPhys["Kolor Oczu"] = "Fioletowe"
                }
            }
            if (cecha.contains("Kolor włosów:")) {
                currentPhys["Kolor Włosów"] = cecha.substringAfter("Kolor włosów: ")
            }
            if (cecha.contains("Mizerna budowa ciała")) {
                currentStats = currentStats?.map { if (it.first == "Odporność") Triple(it.first, it.second.copyOf().apply { this[3] -= 5 }, it.third - 5) else it }?.toMutableList()
                currentSec["Wytrzymałość"] = (currentSec["Wytrzymałość"] ?: 0) - 1
                val wagaInt = currentPhys["Waga"]?.filter { it.isDigit() }?.toIntOrNull() ?: 70
                currentPhys["Waga"] = "${(wagaInt * 0.5).toInt()} kg"
            }
            if (cecha.contains("Potężna budowa ciała")) {
                currentStats = currentStats?.map {
                    if (it.first == "Odporność") Triple(it.first, it.second.copyOf().apply { this[3] += 10 }, it.third + 10) else it
                }?.toMutableList()
                currentSec["Wytrzymałość"] = (currentSec["Wytrzymałość"] ?: 0) + 1
                val wagaInt = currentPhys["Waga"]?.filter { it.isDigit() }?.toIntOrNull() ?: 70
                currentPhys["Waga"] = "${(wagaInt * 1.2).toInt()} kg"
            }
            if (cecha.contains("Kulaw") || cecha.contains("Niska") || (cecha.contains("Niski") && !cecha.contains("Wysoki"))) {
                currentSec["Szybkość"] = (currentSec["Szybkość"] ?: 4) - 1
                if (cecha.contains("Nisk")) {
                    val wzrostInt = currentPhys["Wzrost"]?.filter { it.isDigit() }?.toIntOrNull() ?: 170
                    currentPhys["Wzrost"] = "${(wzrostInt * 0.9).toInt()} cm"
                }
            }
            if (cecha.contains("Wysok")) {
                currentSec["Szybkość"] = (currentSec["Szybkość"] ?: 4) + 1
                val wzrostInt = currentPhys["Wzrost"]?.filter { it.isDigit() }?.toIntOrNull() ?: 170
                currentPhys["Wzrost"] = "${(wzrostInt * 1.1).toInt()} cm"
            }
            if (cecha.contains("Otył")) {
                currentSec["Żywotność"] = (currentSec["Żywotność"] ?: 10) + 2
                val wagaInt = currentPhys["Waga"]?.filter { it.isDigit() }?.toIntOrNull() ?: 70
                currentPhys["Waga"] = "${(wagaInt * 1.3).toInt()} kg"
            }
            if (cecha.contains("Chud")) {
                currentSec["Żywotność"] = (currentSec["Żywotność"] ?: 10) - 2
                val wagaInt = currentPhys["Waga"]?.filter { it.isDigit() }?.toIntOrNull() ?: 70
                currentPhys["Waga"] = "${(wagaInt * 0.8).toInt()} kg"
            }
            if (cecha == "Oczy różnego koloru") {
                val c1 = koloryOczu.sliceArray(1..8).random()
                val c2 = koloryOczu.sliceArray(1..8).random()
                currentPhys["Kolor Oczu"] = "$c1 / $c2"
            }
            if (cecha == "Ślepota") {
                currentPhys["Kolor Oczu"] = "Mgliste"
                currentStats = currentStats?.map {
                    if (it.first == "Strzelectwo") {
                        val basePlusRolls = it.second[0] + it.second[1] + it.second[2]
                        val newRolls = it.second.copyOf()
                        newRolls[3] = -basePlusRolls
                        Triple(it.first, newRolls, 0)
                    } else it
                }?.toMutableList()
                if (!wylosowaneSpecjalne.contains("Ślepowidzenie")) {
                    wylosowaneSpecjalne = wylosowaneSpecjalne + "Ślepowidzenie"
                }
            }
            if (cecha.contains("Zręczne dłonie (Zr+5%)") || cecha.contains("Zgrabna (Zr+5%)") || cecha.contains("Niezgrabne dłonie (Zr-5%)")) {
                val mod = if (cecha.contains("+5%")) 5 else -5
                currentStats = currentStats?.map {
                    if (it.first == "Zręczność") Triple(it.first, it.second.copyOf().apply { this[3] += mod }, it.third + mod) else it
                }?.toMutableList()
            }
            if (cecha.contains("Krzepa+")) {
                val rank = wylosowaneNormalne.find { it.startsWith("Krzepa+") } ?: ""
                val bonus = when {
                    rank.contains("𝕬𝖗𝖈𝖟𝖒𝖎𝖘𝖙𝖗𝖟") -> 5
                    rank.contains("𝕸𝖎𝖘𝖙𝖗𝖟") -> 4
                    rank.contains("𝕰𝖐𝖘𝖕𝖊𝖗𝖙") -> 3
                    rank.contains("𝕬𝖉𝖊𝖕𝖙") -> 2
                    else -> 1
                }
                currentStats = currentStats?.map {
                    if (it.first == "Krzepa") Triple(it.first, it.second.copyOf().apply { this[3] += bonus }, it.third + bonus) else it
                }?.toMutableList()
            }
            if (cecha.contains("Odporność+")) {
                val rank = wylosowaneNormalne.find { it.startsWith("Odporność+") } ?: ""
                val bonus = when {
                    rank.contains("𝕬𝖗𝖈𝖟𝖒𝖎𝖘𝖙𝖗𝖟") -> 5
                    rank.contains("𝕸𝖎𝖘𝖙𝖗𝖟") -> 4
                    rank.contains("𝕰𝖐𝖘𝖕𝖊𝖗𝖙") -> 3
                    rank.contains("𝕬𝖉𝖊𝖕𝖙") -> 2
                    else -> 1
                }
                currentStats = currentStats?.map {
                    if (it.first == "Odporność") Triple(it.first, it.second.copyOf().apply { this[3] += bonus }, it.third + bonus) else it
                }?.toMutableList()
            }
            if (cecha.contains("Jedno Oko")) {
                currentStats = currentStats?.map {
                    if (it.first == "Strzelectwo") {
                        val basePlusRolls = it.second[0] + it.second[1] + it.second[2]
                        val penalty = -(basePlusRolls / 2)
                        val newRolls = it.second.copyOf()
                        newRolls[3] = penalty
                        Triple(it.first, newRolls, basePlusRolls + penalty)
                    } else it
                }?.toMutableList()
            }
            if (cecha.contains("Jedna Ręka")) {
                currentStats = currentStats?.map {
                    if (it.first == "Zręczność") {
                        val basePlusRolls = it.second[0] + it.second[1] + it.second[2]
                        val penalty = -(basePlusRolls / 2)
                        val newRolls = it.second.copyOf()
                        newRolls[3] = penalty
                        Triple(it.first, newRolls, basePlusRolls + penalty)
                    } else it
                }?.toMutableList()
            }
        }
        wygenerowaneStaty = currentStats
        statyDrugorzedne = currentSec
        cechyFizyczne = currentPhys
    }

    val rollSkills = { klasa: String ->
        // 1. Specjalne (5% szansy na każdy)
        val tempSpecjalne = HeroLogic.skillSpecjalny.filter { (1..100).random() <= 5 }.toMutableList()
        if (wybranaRasaIndex == 1 && !tempSpecjalne.contains("Widzenie w ciemności")) {
            tempSpecjalne.add("Widzenie w ciemności")
        }
        wylosowaneSpecjalne = tempSpecjalne

        val statyGlowne = wygenerowaneStaty?.associate { it.first to it.third } ?: emptyMap()
        val magicSkills = listOf("magii", "Wykrywanie magii", "splatanie magii", "magia tajemna", "Magia Prosta", "Magia Powszechna", "Czynienie cudów")
        val isMage = statyGlowne["Mag"] ?: 0 > 0

        // 2. Normalne
        val mods = getClassModifiers(klasa, wybranyPoziom)
        val picked = mutableSetOf<String>()

        // Gwarantowane: Uczony i Kapłan mają Czytanie i pisanie
        if (klasa == "Uczony" || klasa == "Kapłan") {
            picked.add("Czytanie i pisanie")
        }

        val currentReqs = skillPrerequisites.toMutableMap()
        currentReqs["Prawo"] = "Czytanie i pisanie"

        val reqsForLevel = getLevelRequirements(wybranyPoziom)
        val totalUniqueNeeded = maxOf(20, reqsForLevel.lvlUmax)

        val plec = MainActivity.listaGender[wybranaPlecIndex]

        // Ilości wg poziomu 0 (jako baza rozkładu)
        val initialCounts = listOf(7, 7, 4, if (mods.mod5.isEmpty()) 2 else 1, 1)
        val cats = listOf(mods.mod1, mods.mod2, mods.mod3, mods.mod4, mods.mod5)

        for (i in cats.indices) {
            val catName = cats[i]
            val count = initialCounts.getOrElse(i) { 0 }
            if (catName.isEmpty() || count <= 0) continue
            val magicSkillsPool = listOf("magii", "Wykrywanie magii", "splatanie magii", "magia tajemna", "Magia Prosta", "Magia Powszechna", "Czynienie cudów")
            val isMageChar = statyGlowne["Mag"] ?: 0 > 0

            val pool = getCategoryByName(catName).filter {
                !picked.contains(it) && 
                !(it == "Wysokie obcasy" && plec == "Mężczyzna") &&
                (isMageChar || magicSkillsPool.none { magic -> it.contains(magic, ignoreCase = true) })
            }.shuffled()
            var added = 0
            for (skill in pool) {
                if (added >= count) break
                val req = currentReqs[skill]
                if (req != null && !picked.contains(req)) continue
                picked.add(skill)
                added++
            }
        }

        // Dopełnienie do totalUniqueNeeded z całej puli
        if (picked.size < totalUniqueNeeded) {
            val rest = HeroLogic.allNormalSkills.filter {
                !picked.contains(it) && 
                !(it == "Wysokie obcasy" && plec == "Mężczyzna") &&
                (isMage || magicSkills.none { magic -> it.contains(magic, ignoreCase = true) })
            }.shuffled()
            for (skill in rest) {
                if (picked.size >= totalUniqueNeeded) break
                val req = currentReqs[skill]
                if (req != null && !picked.contains(req)) continue
                picked.add(skill)
            }
        }

        // Przyznawanie rang
        val sortedBase = picked.toList().sorted()
        val numAM = reqsForLevel.lvlAMax
        val numM = reqsForLevel.lvlMmax
        val numE = reqsForLevel.lvlEmax
        val numA = reqsForLevel.lvlAmax
        val numU = reqsForLevel.lvlUmax

        val languages = HeroLogic.skillJezykObcyNazwa.filter { it != "Brak" }.distinct().shuffled()
        val langMap = mapOf(
            "[klasa]" to klasa
        )

        // Skille, które mogą awansować (wykluczamy języki i znaki)
        val toRankUp = sortedBase.filter { s ->
            !s.startsWith("Sekretne znaki") && 
            HeroLogic.skillSpecjalny.none { it.equals(s, ignoreCase = true) } &&
            s != "Silny cios" &&
            s != "Splatanie magii" &&
            s != "Czytanie i pisanie" &&
            s != "Uzdolnienia językowe" &&
            s != "Widzenie w ciemności" &&
            !s.contains("Język") && 
            !s.contains("język")
        }.toMutableList()
        toRankUp.shuffle()
        
        // Arcymistrzowie (jeśli poziom wymaga)
        val arcymistrzowie = toRankUp.take(numAM).toSet().also { toRankUp.removeAll(it) }
        // Mistrzowie
        val mistrzowie = toRankUp.take(numM).toSet().also { toRankUp.removeAll(it) }
        // Eksperci
        val eksperci = toRankUp.take(numE).toSet().also { toRankUp.removeAll(it) }
        // Adepci
        val adepci = toRankUp.take(numA).toSet().also { toRankUp.removeAll(it) }
        val uczniowie = toRankUp.take(numU).toSet().also { toRankUp.removeAll(it) }

        wylosowaneNormalne = sortedBase.map { s ->
            var finalName = s
            langMap.forEach { (key, value) ->
                if (finalName.contains(key)) {
                    finalName = finalName.replace(key, "($value)")
                }
            }
            val rankSuffix = when {
                arcymistrzowie.contains(s) -> " (𝕬𝖗𝖈𝖞𝖒𝖎𝖘𝖙𝖗𝖟)"
                mistrzowie.contains(s) -> " (𝕸𝖎𝖘𝖙𝖗𝖟)"
                eksperci.contains(s) -> " (𝕰𝖐𝖘𝖕𝖊𝖗𝖙)"
                adepci.contains(s) -> " (𝕬𝖉𝖊𝖕𝖙)"
                uczniowie.contains(s) -> " (𝖀𝖈𝖟𝖊𝖓)"
                else -> ""
            }
            "$finalName$rankSuffix"
        }

        // Kandydaci na Adepta (tylko z tych, co są Uczniami i należą do głównej kategorii klasy)
        val mainCategorySkills = getCategoryByName(mods.mod1).toSet()
        val candidatesBase = sortedBase.filter { s ->
            uczniowie.contains(s) &&
            s != "Czytanie i pisanie" && s != "Matematyka" &&
            s != "Silny cios" && s != "Splatanie magii" &&
            HeroLogic.skillSpecjalny.none { it.equals(s, ignoreCase = true) } &&
            !s.startsWith("Sekretne znaki") && !s.contains("Język") && !s.contains("język") &&
            mainCategorySkills.contains(s)
        }
        wylosowaneAdepty = if (candidatesBase.size >= 2) {
            candidatesBase.shuffled().take(2)
        } else {
            // Jeśli mało z głównej kategorii, dopełnij resztą dostępnych uczniów
            val otherCandidates = sortedBase.filter { s ->
                uczniowie.contains(s) &&
                s != "Czytanie i pisanie" && s != "Matematyka" &&
                s != "Silny cios" && s != "Splatanie magii" &&
                HeroLogic.skillSpecjalny.none { it.equals(s, ignoreCase = true) } &&
                !s.startsWith("Sekretne znaki") && !s.contains("Język") && !s.contains("język") &&
                !mainCategorySkills.contains(s)
            }
            (candidatesBase + otherCandidates.shuffled()).take(2)
        }

        rollCharacteristicTraits() 
    }

    val rollSecondaryStatsInternal = {
        val currentPrimary = wygenerowaneStaty ?: emptyList()
        if (currentPrimary.isNotEmpty()) {
            // Obliczamy z bazy (pierwsze 3 rzuty), by modyfikatory cech się nie nakładały
            val s = (currentPrimary[2].second[0] + currentPrimary[2].second[1] + currentPrimary[2].second[2]) / 10
            val w = (currentPrimary[3].second[0] + currentPrimary[3].second[1] + currentPrimary[3].second[2]) / 10
            val a = 1
            val sz = when (wybranaRasaIndex) {
                0 -> 4 // Człowiek
                1 -> 5 // Elf
                2 -> 3 // Krasnolud
                else -> 4
            }
            val zy = when (wybranaRasaIndex) {
                0 -> (1..10).random().let { if (it <= 3) 10 else if (it <= 6) 11 else if (it <= 9) 12 else 13 }
                1 -> (1..10).random().let { if (it <= 3) 9 else if (it <= 6) 10 else if (it <= 9) 11 else 12 }
                2 -> (1..10).random().let { if (it <= 3) 11 else if (it <= 6) 12 else if (it <= 9) 13 else 14 }
                else -> 10
            }
            val pp = when (wybranaRasaIndex) {
                0 -> (1..10).random().let { if (it <= 4) 2 else 3 }
                1 -> (1..10).random().let { if (it <= 4) 1 else 2 }
                2 -> (1..10).random().let { if (it <= 4) 1 else 2 }
                else -> 2
            }

            // Modyfikatory z umiejętności
            val allSkills = wylosowaneNormalne + wylosowaneSpecjalne
            var bonusSz = 0
            var bonusA = 0
            var bonusZy = 0

            if (allSkills.any { it.contains("Błyskotliwość", ignoreCase = true) }) bonusSz += 1
            if (allSkills.any { it.contains("Szybki refleks", ignoreCase = true) }) bonusSz += 1
            if (allSkills.any { it.contains("Twardziel", ignoreCase = true) }) bonusZy += 1
            if (allSkills.any { it.contains("Bardzo silny", ignoreCase = true) }) { /* S+5 w statach głównych jest już obsłużone */ }

            statyDrugorzedne = mapOf(
                "Ataki" to (a + bonusA),
                "Żywotność" to (zy + bonusZy),
                "Siła" to s,
                "Wytrzymałość" to w,
                "Szybkość" to (sz + bonusSz),
                "Magia" to 0,
                "Obłęd" to 0,
                "Przeznaczenie" to pp
            )
        }
    }

    val getBaseStat = { rasaIdx: Int, statIdx: Int ->
        when (rasaIdx) {
            0 -> 20 // Człowiek: 20 wszędzie
            1 -> when (statIdx) { // Elf
                0 -> 20; 1 -> 30; 2 -> 20; 3 -> 10; 4 -> 30; 5 -> 30; 6 -> 30; 7 -> 30; else -> 0
            }
            2 -> when (statIdx) { // Krasnolud
                0 -> 30; 1 -> 10; 2 -> 30; 3 -> 30; 4 -> 10; 5 -> 20; 6 -> 40; 7 -> 10; else -> 0
            }
            else -> 0
        }
    }


    val rollPhysicalStats = {
        val baseAge = when (wybranyWiekIndex) {
            0 -> (16..19).random()
            1 -> (20..29).random()
            2 -> (30..45).random()
            else -> (46..70).random()
        }
        val raceAge = when(wybranaRasaIndex) {
            1 -> baseAge * 4 // Elf
            2 -> baseAge * 2 // Dwarf
            else -> baseAge
        }
        val height = when (wybranaRasaIndex) {
            0 -> 150 + (1..10).random() + (1..10).random()
            1 -> { // Elf
                if (wybranaPlecIndex == 0) (180..195).random() else (175..190).random()
            }
            2 -> 130 + (1..10).random() + (1..10).random()
            else -> 170
        }
        val weight = when (wybranaRasaIndex) {
            0 -> 50 + (1..10).random() * 5
            1 -> 40 + (1..10).random() * 4
            2 -> 60 + (1..10).random() * 6
            else -> 70
        }

        val rolledEyeColor = koloryOczu.sliceArray(1..8).random()
        
        // Exotic hair colors logic
        val hairRoll = (1..100).random()
        val rolledHairColor = when (wybranaRasaIndex) {
            1 -> if (hairRoll <= 50) koloryWlosow.sliceArray(9..16).random() else koloryWlosow.sliceArray(1..8).random() // Elf 50%
            2 -> if (hairRoll <= 25) koloryWlosow.sliceArray(9..16).random() else koloryWlosow.sliceArray(1..8).random() // Dwarf 25%
            0 -> if (hairRoll <= 15) koloryWlosow.sliceArray(9..16).random() else koloryWlosow.sliceArray(1..8).random() // Human 15%
            else -> koloryWlosow.sliceArray(1..8).random()
        }

        val basePhys = mapOf(
            "Wiek" to "$raceAge lat",
            "Wzrost" to "$height cm",
            "Waga" to "$weight kg",
            "Kolor Oczu" to rolledEyeColor,
            "Kolor Włosów" to rolledHairColor,
            "Znak Gwiezdny" to znakiZodiaku.random()
        )
        cechyFizyczne = basePhys
        bazaCechyFizyczne = basePhys
    }

    val generateAll = {
        var attempts = 0
        var foundValidStats = false
        
        while (!foundValidStats && attempts < 100) {
            attempts++
            // 1. Statystyki główne - tylko baza i rzuty k10
            val newPrimary = MainActivity.statNames.mapIndexed { index, name ->
                val base = getBaseStat(wybranaRasaIndex, index)
                val r1 = (1..10).random()
                val r2 = (1..10).random()
                val total = base + r1 + r2
                Triple(name, intArrayOf(base, r1, r2, 0), total)
            }

            // Sprawdź czy jakakolwiek klasa jest dostępna
            val hasAvailableClass = MainActivity.listaKlasa.indices.any { index ->
                val statIdx = when(index) {
                    0 -> 0 // Wojownik -> WW
                    1 -> 4 // Łotr -> Zręczność
                    2 -> 1 // Łowca -> Strzelectwo
                    3 -> 5 // Uczony -> Inteligencja
                    4 -> 7 // Kleryk -> Charyzma
                    5 -> 6 // Mag -> Siła Woli
                    else -> 0
                }
                val stat = newPrimary.getOrNull(statIdx)
                stat != null && (stat.second[1] + stat.second[2]) > 11
            }

            if (hasAvailableClass) {
                wygenerowaneStaty = newPrimary
                foundValidStats = true
            }
        }

        // 2. Inicjalizacja drugorzędnych (baza)
        if (foundValidStats) {
            rollSecondaryStatsInternal()

            // 3. Cechy fizyczne
            rollPhysicalStats()

            // 4. Cechy charakterystyczne (modyfikatory)
            rollCharacteristicTraits()
        }
    }


    BackHandler(enabled = menuApp != 0) {
        val heroFiles = context.fileList().filter { it.startsWith("hero_") }
        when (menuApp) {
            9, 11, 7, 10 -> onMenuAppChange(1)
            13, 30, 35, 50, 22 -> onMenuAppChange(10)
            12 -> onMenuAppChange(11)
            6 -> {
                // Jeśli wylosowaneAdepty jest puste, prawdopodobnie jesteśmy w trybie "Przepisz"
                if (wylosowaneAdepty.isEmpty()) onMenuAppChange(12) else onMenuAppChange(5)
            }
            5 -> onMenuAppChange(4)
            4 -> onMenuAppChange(3)
            3 -> onMenuAppChange(2)
            2 -> {
                if (heroFiles.isEmpty()) onMenuAppChange(0) else onMenuAppChange(1)
            }
            else -> onMenuAppChange(0)
        }
    }

    // Funkcje do obsługi plików
    val saveToFile = { fileName: String, content: String ->
        try {
            val finalContent = if (fileName.startsWith("hero_")) {
                HeroLogic.stripLegacyProgressTags(content)
            } else {
                content
            }
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(finalContent.toByteArray())
            }
            println("Zapisano plik: $fileName, zawartość: $finalContent")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val readFromFile = { fileName: String ->
        try {
            val content = context.openFileInput(fileName).bufferedReader().use { it.readText() }
            println("Odczytano plik: $fileName, zawartość: $content")
            content
        } catch (e: Exception) {
            ""
        }
    }

    val daysElapsed = remember(menuApp) {
        val startStr = readFromFile("dataStart")
        val nowStr = readFromFile("dataNow")
        if (startStr.isEmpty() || nowStr.isEmpty()) 0L
        else {
            try {
                // Nowy format: D.M.Y | HH:mm lub stary D.M.Y
                val sStr = if (startStr.contains("|")) startStr.substringBefore("|").trim() else startStr
                val nStr = if (nowStr.contains("|")) nowStr.substringBefore("|").trim() else nowStr
                
                val sdf = SimpleDateFormat("d.M.yyyy", Locale.getDefault())
                val dStart = sdf.parse(sStr)
                val dNow = sdf.parse(nStr)
                if (dStart != null && dNow != null) {
                    val diff = dNow.time - dStart.time
                    TimeUnit.MILLISECONDS.toDays(diff)
                } else 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    LaunchedEffect(Unit) {
        // CZYSZCZENIE: Usuń tylko stare wersje z .txt jeśli istnieją
        val existingFiles = context.fileList()
        existingFiles.forEach { fileName ->
            if (fileName.endsWith(".txt")) {
                File(context.filesDir, fileName).delete()
            }
        }

        val calendar = Calendar.getInstance()
        val currentFormattedDate = String.format(
            Locale.getDefault(),
            "%d.%d.%d",
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR)
        )
        val currentFormattedTime = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
        val fullConfigString = "$currentFormattedDate | $currentFormattedTime"

        setApp.forEach { fileName ->
            var content = readFromFile(fileName)

            if (content.isEmpty()) {
                // Przy pierwszym tworzeniu: dataStart i dataNow otrzymują obecną datę z godziną
                val initialValue = if (fileName == "dataStart" || fileName == "dataNow") fullConfigString else "0"
                saveToFile(fileName, initialValue)
            } else if (fileName == "dataNow") {
                // Wyciągnij samą datę do porównania
                val savedDate = if (content.contains("|")) content.substringBefore("|").trim() else content
                if (savedDate != currentFormattedDate) {
                    // Aktualizuj na pełny format z nowym czasem
                    saveToFile(fileName, fullConfigString)
                    Toast.makeText(context, "Witaj ponownie", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val updateFileList = {
        val files = context.fileList()
        // Pokazuj tylko te pliki, które są w grupie setApp
        println("Odświeżono listę plików: ${files.size}")
    }

    Scaffold(
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (menuApp) {
                -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 12, 13, 14,
                10, 15, 20, 21, 22, 30, 31, 35, 50, 60, 61, 62, 63, 70 -> {
                            // Header buttons
                            if (menuApp != 0) {
                                IconButton(
                                    onClick = {
                                        playSoundAction(R.raw.roll, 1)
                                        if (menuApp == 21 || menuApp == 20 || menuApp == 30 || menuApp == 31 || menuApp == 35 || menuApp == 22) {
                                            onMenuAppChange(10)
                                        } else if (menuApp == 61) {
                                            onMenuAppChange(10)
                                        } else if (menuApp in 62..63) {
                                            onMenuAppChange(61)
                                        } else {
                                            val heroFiles = context.fileList().filter { it.startsWith("hero_") }
                                            when (menuApp) {
                                                9, 7, 11 -> onMenuAppChange(1)
                                                12 -> onMenuAppChange(11)
                                                14 -> onMenuAppChange(12)
                                                10 -> onMenuAppChange(1)
                                                8 -> onMenuAppChange(6)
                                                6 -> {
                                                    if (wylosowaneAdepty.isEmpty()) onMenuAppChange(12) else onMenuAppChange(5)
                                                }
                                                5 -> onMenuAppChange(4)
                                                4 -> onMenuAppChange(3)
                                                3 -> onMenuAppChange(2)
                                                2 -> {
                                                    if (heroFiles.isEmpty()) onMenuAppChange(0) else onMenuAppChange(1)
                                                }
                                                13 -> onMenuAppChange(1)
                                                70 -> onMenuAppChange(13)
                                                1 -> onMenuAppChange(0)
                                                else -> onMenuAppChange(0)
                                            }
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz", tint = Color.White)
                                }
                            }

                            AnimatedVisibility(
                                visible = (introFinished || menuApp != 0) && menuApp != 21 && menuApp != 30 && menuApp != 70,
                                enter = fadeIn(),
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                IconButton(
                                    onClick = {
                                        playSoundAction(R.raw.roll, 1)
                                        isEditing(false)
                                        onMenuAppChange(-1)
                                        updateFileList()
                                    }
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Ustawienia", tint = Color.White)
                                }
                            }

                            // Title and Subtitle
                            if (menuApp == 0) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = if (!introFinished) titleOffsetY.value.dp else 0.dp)
                        .scale(if (!introFinished) titleScale.value else 1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "𝕱𝖆𝖓𝕲𝖊𝖓",
                        fontSize = 60.sp,
                        fontFamily = nosiferFamily,
                        color = Color.White
                    )
                }
                                if (introFinished){
                                    // BOTTOM STATUS BAR
                                    Surface(
                                        modifier = Modifier.height(20.dp),
                                        color = Color(0x001A1A1A),
                                        //border = BorderStroke(1.dp, Color(0xFF333333))
                                    ) {
                                        Column(
                                            modifier = Modifier.height(18.dp)
                                        ) {
                                            // ##!! Wersja aplikacji
                                            Text(
                                                "𝔅𝔢𝔱𝔞-6.1007",
                                                color = Color.LightGray,
                                                fontSize = 15.sp,
                                                fontFamily = slaboFamily
                                            )
                                        }
                                    }
                                }
            }

            // Main Content
            if (introFinished || menuApp != 0 || (menuApp == 0 && !introFinished)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = if (menuApp == 0) 0.dp else 30.dp)
,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (menuApp != 0 && menuApp != 4) {
                        Text(
                            text = when (menuApp) {
                                //10 -> "KARTA BOHATERA"
                                -1 -> "Od startu minęło $daysElapsed dni"
                                else -> ""
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    when (menuApp) {
                        0 -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Image(
                                        painter = painterResource(id = R.drawable.k10h),
                                        contentDescription = "Rzuty kośćmi",
                                        modifier = Modifier
                                            .size(300.dp)
                                            .scale(if (!introFinished) k10Scale.value else 1f)
                                            .rotate(if (!introFinished) k10Rotation.value else 0f)
                                            .offset(y = if (!introFinished) k10OffsetY.value.dp else 0.dp)
                                            .clickable(enabled = introFinished) {
                                                playSoundAction(R.raw.roll, 1)
                                                context.startActivity(
                                                    Intent(
                                                        context,
                                                        GameActivity::class.java
                                                    )
                                                )
                                            }
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))
                                }

                                Image(
                                    painter = painterResource(id = R.drawable.unknowhero),
                                    contentDescription = "Bohater",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .size(220.dp)
                                        .offset(y = if (!introFinished) heroImageOffsetY.value.dp else 0.dp)
                                        .clickable(enabled = introFinished) {
                                            playSoundAction(R.raw.klik1, 0)
                                            val heroFiles = context.fileList().filter { it.matches(Regex("hero_\\d+")) }
                                            if (isGMMode && heroFiles.isEmpty()) {
                                                onMenuAppChange(11)
                                            } else {
                                                onMenuAppChange(1)
                                            }
                                        }
                                )
                            }
                        }
                        1 -> {
                            val heroFiles = context.fileList().filter { it.matches(Regex("hero_\\d+")) }.sortedBy { it.substringAfter("_").toInt() }
                            if (heroFiles.isEmpty()) {
                                LaunchedEffect(Unit) { onMenuAppChange(2 ) }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 0.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(modifier = Modifier.height(20.dp))

                                    var localSelectedIdx by remember { mutableIntStateOf(readFromFile("lastHero").toIntOrNull() ?: 0) }
                                    
                                    LaunchedEffect(localSelectedIdx) {
                                        saveToFile("lastHero", localSelectedIdx.toString())
                                    }

                                    // Zabezpieczenie przed zmianą listy plików w locie
                                    val safeIdx = if (localSelectedIdx >= heroFiles.size) 0 else localSelectedIdx
                                    val currentFile = if (heroFiles.isNotEmpty()) heroFiles[safeIdx] else ""

                                    LaunchedEffect(currentFile) {
                                        if (currentFile.isNotEmpty()) {
                                            val heroId = currentFile.removePrefix("hero_")
                                            val heroData = readFromFile(currentFile)
                                            val progressFile = "progress_$heroId.json"
                                            val progressJson = readFromFile(progressFile)

                                            if (progressJson.isEmpty()) {
                                                // MIGRATION: Try to extract from heroData
                                                val progress = HeroLogic.getHeroProgress(heroId, heroData)
                                                currentHeroProgress = progress

                                                // Save JSON and strip tags from main sheet
                                                val jsonStr = Json.encodeToString(progress)
                                                saveToFile(progressFile, jsonStr)

                                                val stripped = HeroLogic.stripLegacyProgressTags(heroData)
                                                saveToFile(currentFile, stripped)
                                            } else {
                                                try {
                                                    currentHeroProgress = Json.decodeFromString<HeroProgress>(progressJson)
                                                } catch (e: Exception) {
                                                    val repairedProgress = HeroLogic.getHeroProgress(heroId, heroData)
                                                    currentHeroProgress = repairedProgress
                                                    saveToFile(progressFile, Json.encodeToString(repairedProgress))
                                                }
                                            }
                                        } else {
                                            currentHeroProgress = null
                                        }
                                    }

                                    // Tryb MG: Przyciski dla MG
                                    if (isGMMode) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Button(
                                                onClick = { onMenuAppChange(13 ) },
                                                modifier = Modifier.padding(horizontal = 2.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.6f)),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Text("NARZĘDZIA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = { onMenuAppChange(2 ) },
                                                modifier = Modifier.padding(horizontal = 2.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue.copy(alpha = 0.6f)),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Text("NOWY BOHATER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    // Inicjalizacja danych dla trybu manualnego przepisywania
                                                    heroName = ""
                                                    wybranaRasaIndex = 0
                                                    wybranaPlecIndex = 0
                                                    wygenerowaneStaty = MainActivity.statNamesAbbr.map { Triple(it, intArrayOf(0, 0, 0, 0), 0) }
                                                    statyDrugorzedne = emptyMap()
                                                    cechyFizyczne = mutableMapOf("Kolor oczu" to "", "Kolor włosów" to "")
                                                    heroPortraitUri = null
                                                    wybranyPoziom = 1
                                                    onMenuAppChange(11)
                                                },
                                                modifier = Modifier.padding(horizontal = 2.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A148C).copy(alpha = 0.6f)),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Text("PRZEPISZ 📜", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    AnimatedContent(
                                        targetState = safeIdx,
                                        transitionSpec = {
                                            if (targetState > initialState) {
                                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                                slideOutHorizontally { width -> -width } + fadeOut()
                                            } else {
                                                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                                slideOutHorizontally { width -> width } + fadeOut()
                                            }.using(
                                                SizeTransform(clip = false)
                                            )
                                        },
                                        label = "HeroCardTransition"
                                    ) { targetIdx ->
                                        val currentFile = heroFiles[targetIdx]
                                        val heroData = readFromFile(currentFile)
                                        val lines = heroData.lines()

                                        val travelData = HeroLogic.getTravelData(heroData)
                                        if (travelData != null) {
                                            LaunchedEffect(currentFile) {
                                                val parts = travelData.split("|")
                                                if (parts.size >= 8) {
                                                    val dest = parts[0]
                                                    val destId = parts[1].toLongOrNull() ?: -1L
                                                    val progress = parts[2].toIntOrNull() ?: 0
                                                    val total = parts[3].toIntOrNull() ?: 24
                                                    val rType = parts[4]
                                                    val zType = parts[5]
                                                    val tgs = parts[6]
                                                    val mId = parts[7]

                                                    val intent = Intent(context, MainActivity::class.java).apply {
                                                        putExtra("HERO_ID", currentFile.removePrefix("hero_"))
                                                        putExtra(MainActivity.EXTRA_MENU_APP, 61)
                                                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                                    }
                                                    context.startActivity(intent)
                                                }
                                            }
                                        }
                                        
                                        val poziomLine = lines.find { it.startsWith("POZIOM:") } ?: "POZIOM: 0 ｢0｣ GRA"
                                        val level = poziomLine.substringAfter(":").trim().substringBefore(" ").toIntOrNull() ?: 0

                                        val levelStart = poziomLine.substringAfter("｢").substringBefore("｣").trim().toIntOrNull() ?: 0

                                        val rawName = lines.getOrNull(0) ?: "Nieznany"
                                        val name = if (rawName.startsWith("Imie: ")) rawName.substringAfter("Imie: ").trim() else rawName
                                        val infoLine1 = lines.getOrNull(1) ?: "" // Rasa: ..., Klasa: ...
                                        val infoLine2 = lines.getOrNull(2) ?: "" // Płeć: ..., Wiek: ...
                                        val portraitLine = lines.find { it.startsWith("Portrait: ") }
                                        val portraitUriString = portraitLine?.substringAfter("Portrait: ")?.trim()
                                        val pdLine = lines.find { it.startsWith("PD:") } ?: "PD: 0"
                                        val pfLine = lines.find { it.startsWith("PF:") || it.startsWith("PF: ") } ?: "PF: 0"
                                        val currentPF = pfLine.substringAfter("PF:").trim().toIntOrNull() ?: 0
                                        val rasa = infoLine1.substringAfter("Rasa: ").substringBefore(",").trim()
                                        val klasa = infoLine1.substringAfter("Klasa: ").trim()
                                        val plecRaw = infoLine2.substringAfter("Płeć: ").substringBefore(",").trim()
                                        val plecSymbol = when {
                                            plecRaw.contains("Mężczyzna", ignoreCase = true) -> "♂️"
                                            plecRaw.contains("Kobieta", ignoreCase = true) -> "♀️"
                                            else -> "⚧"
                                        }
                                        val pd = pdLine.substringAfter("PD:").trim()
                                        val poziom = level.toString()
                                        val cechyLine = lines.find { it.startsWith("CECHY CHARAKTERU:") } ?: ""
                                        val cechyIndex = lines.indexOf(cechyLine)
                                        val cechyList = if (cechyIndex != -1) {
                                            lines.drop(cechyIndex + 1).takeWhile { it.startsWith("•") }.map { it.trim(' ', '•') }
                                        } else emptyList()

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(400.dp)
                                                .padding(16.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        ) {
                                            if (!portraitUriString.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = portraitUriString,
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

                                            // Gradient overlay for better text readability
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color.Black.copy(alpha = 0.6f),
                                                                Color.Transparent,
                                                                Color.Black.copy(alpha = 0.6f)
                                                            ),
                                                            startY = 0f,
                                                            endY = Float.POSITIVE_INFINITY
                                                        )
                                                    )
                                            )

                                            // Imię - lewy górny róg

                                            Surface(
                                                color = Color(0x22000000),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    name,
                                                    color = if (levelStart < 1) Color.Green else Color.Yellow,
                                                    fontSize =  if (levelStart < 1) 22.sp else 30.sp,
                                                    fontWeight = if (levelStart < 1) FontWeight.Normal else FontWeight.Bold,
                                                    fontFamily =  if (levelStart < 1) nosiferFamily else lobsterFamily,
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(8.dp)
                                                )
                                            }

                                            // Prawa kolumna: Rasa, Poziom, Klasa, PD
                                            Column(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.End
                                            ) {

                                                Text("$plecSymbol  $rasa", color = Color.LightGray, fontSize = 16.sp)
                                                Text(klasa, color = Color.LightGray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                                Text("⭐ $poziom" + " ", color = Color.Yellow, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                                Text("${pd}𝖕𝖉", color = Color.Yellow, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)

                                                Surface(
                                                    color = Color(0x44003300),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text("🍀: " + currentPF.toString() + " ", color = if (currentPF > 0) Color.Green else Color.Red, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                                }

                                            }

                                            // Cechy charakterystyczne - lewy dolny róg
                                            if (cechyList.isNotEmpty()) {
                                                Column(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(8.dp)
                                                ) {
                                                    cechyList.take(3).forEach { cecha ->
                                                        Surface(
                                                            color = Color(0x66000000),
                                                            shape = RoundedCornerShape(4.dp),
                                                            modifier = Modifier.padding(vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                "• $cecha", 
                                                                color = Color.White.copy(alpha = 0.9f), 
                                                                fontSize = 12.sp, 
                                                                fontFamily = slaboFamily,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (heroFiles.size > 1) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 32.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = { if (localSelectedIdx > 0) localSelectedIdx-- }) {
                                                Text("❮", color = Color.White, fontSize = 24.sp)
                                            }

                                            val sliderInteractionHero = remember { MutableInteractionSource() }
                                            val isPressedHero by sliderInteractionHero.collectIsPressedAsState()
                                            LaunchedEffect(isPressedHero) {
                                                if (isPressedHero) playSoundAction(R.raw.page, 1)
                                            }

                                            Slider(
                                                value = safeIdx.toFloat(),
                                                onValueChange = { localSelectedIdx = it.toInt() },
                                                onValueChangeFinished = { playSoundAction(R.raw.page, 2) },
                                                interactionSource = sliderInteractionHero,
                                                valueRange = 0f..(heroFiles.size - 1).toFloat(),
                                                steps = if (heroFiles.size > 1) heroFiles.size - 2 else 0,
                                                modifier = Modifier.weight(1f)
                                            )

                                            TextButton(onClick = { if (localSelectedIdx < heroFiles.size - 1) localSelectedIdx++ }) {
                                                Text("❯", color = Color.White, fontSize = 24.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(30.dp))

                                    Row(modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        Button(
                                            onClick = {
                                                playSoundAction(R.raw.klik1, 0)
                                                val currentFile = heroFiles[safeIdx]
                                                val fileNum = currentFile.removePrefix("hero_").toIntOrNull() ?: 1
                                                onSelectedHeroIdChange(fileNum)
                                                onMenuAppChange(10)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black),
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 4.dp)
                                        ) {
                                            Text("📜", fontSize = 20.sp)
                                        }

                                        val currentFile = heroFiles[safeIdx]
                                        val heroData = readFromFile(currentFile)
                                        val lines = heroData.lines()

                                        val poziomLine = lines.find { it.startsWith("POZIOM:") } ?: "POZIOM: 0 ｢0｣ GRA"
                                        val poziomStartowy = poziomLine.substringAfter("｢").substringBefore("｣").trim().toIntOrNull() ?: 0
                                        if (poziomStartowy == 0) {
                                            Button(
                                                onClick = {
                                                    playSoundAction(R.raw.klik1, 0)
                                                    val currentFile = heroFiles[safeIdx]
                                                    val fileNum = currentFile.removePrefix("hero_").toIntOrNull() ?: 1
                                                    onSelectedHeroIdChange(fileNum)
                                                    val progress = currentHeroProgress
                                                        ?: HeroLogic.getHeroProgress(fileNum.toString(), heroData)
                                                    val entered = tryEnterSettlementFromCurrentPosition(
                                                        context, fileNum, progress, readFromFile, saveToFile
                                                    )
                                                    if (entered != null) {
                                                        currentHeroProgress = entered
                                                        onMenuAppChange(35)
                                                    } else {
                                                        currentHeroProgress = progress
                                                        onMenuAppChange(61)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF008B00),
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 4.dp)
                                            ) {
                                                Text(
                                                    "🏕️",      //⛺🏕️
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                playSoundAction(R.raw.roll, 1)
                                                val currentFile = heroFiles[safeIdx]
                                                val intent = Intent(context, GameActivity::class.java).apply {
                                                    putExtra("HERO_FILE", currentFile)
                                                    putExtra("MENU_APP", 3)
                                                }
                                                context.startActivity(intent)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black),
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 4.dp)
                                        ) {
                                            Text("𓁄", fontSize = 20.sp)
                                        }

                                        Button(
                                            onClick = {
                                                playSoundAction(R.raw.roll, 1)
                                                val currentFile = heroFiles[safeIdx]
                                                val intent = Intent(context, GameActivity::class.java).apply {
                                                    putExtra("HERO_FILE", currentFile)
                                                    putExtra("SKIP_ROLL", true)
                                                }
                                                context.startActivity(intent)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue, contentColor = Color.Black),
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 4.dp)
                                        ) {
                                            Text("🎲", fontSize = 20.sp) //Kości w menu hero
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center) {
                                        Button(
                                            onClick = {
                                                playSoundAction(R.raw.klik1, 0)
                                                val currentFile = heroFiles[safeIdx]
                                                val fileNum = currentFile.removePrefix("hero_").toIntOrNull() ?: 1
                                                onSelectedHeroIdChange(fileNum)
                                                
                                                // Załaduj dane do edycji
                                                val heroData = readFromFile(currentFile)
                                                val lines = heroData.lines()
                                                val firstLine = lines.getOrNull(0) ?: ""
                                                heroName = if (firstLine.startsWith("Imie: ")) firstLine.substringAfter("Imie: ").trim() else firstLine
                                                val pLine = lines.find { it.startsWith("Portrait: ") }
                                                heroPortraitUri = pLine?.substringAfter("Portrait: ")?.trim()
                                                
                                                onMenuAppChange(7)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                                            modifier = Modifier
                                                .fillMaxWidth(0.5f)
                                                .padding(horizontal = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Ustawienia", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                }
                            }
                        }
                        2 -> {
                            CharacterInitialSetupScreen(
                                wybranaRasaIndex = wybranaRasaIndex,
                                onRasaIndexChange = { wybranaRasaIndex = it },
                                wybranaPlecIndex = wybranaPlecIndex,
                                onPlecIndexChange = { wybranaPlecIndex = it },
                                wybranyWiekIndex = wybranyWiekIndex,
                                onWiekIndexChange = { wybranyWiekIndex = it },
                                wybranyPoziom = wybranyPoziom,
                                onPoziomChange = { wybranyPoziom = it },
                                listaWiek = listaWiek,
                                playSoundAction = playSoundAction,
                                onNext = {
                                    generateAll()
                                    onMenuAppChange(3)
                                }
                            )
                        }
                        3 -> {
                            // Ekran 3: Tylko Statystyki Główne i rzuty
                            StatsAndClassSelectionScreen(
                                wygenerowaneStaty = wygenerowaneStaty,
                                wybranaKlasaIndex = wybranaKlasaIndex,
                                onKlasaIndexChange = { newIdx -> wybranaKlasaIndex = newIdx },
                                onGenerateAll = { generateAll() },
                                onRollSkills = { /* Nieużywane w 3 */ },
                                playSoundAction = playSoundAction,
                                showClassSelection = true, // Ukrywamy wybór klasy na tym etapie
                                onNext = { 
                                    rollSecondaryStatsInternal()
                                    rollCharacteristicTraits()
                                    onMenuAppChange(4) 
                                }
                            )
                        }
                        4 -> {
                            // Ekran 4: Statystyki Drugorzędne + Cechy Fizyczne
                            SecondaryStatsScreen(
                                statyDrugorzedne = statyDrugorzedne,
                                cechyFizyczne = cechyFizyczne,
                                wylosowaneCechyCharakteru = wylosowaneCechyCharakteru,
                                wybranaKlasa = MainActivity.listaKlasa[wybranaKlasaIndex],
                                onRollSecondary = {
                                    rollSecondaryStatsInternal()
                                    rollPhysicalStats()
                                    rollCharacteristicTraits()
                                },
                                onRollSkills = { className -> rollSkills(className) },
                                playSoundAction = playSoundAction,
                                onNext = { onMenuAppChange(5) }
                            )
                        }
                        5 -> {
                            AdeptSelectionScreen(
                                wylosowaneNormalne = wylosowaneNormalne,
                                wylosowaneSpecjalne = wylosowaneSpecjalne,
                                wylosowaneAdepty = wylosowaneAdepty,
                                onAdeptSelected = { adeptSkill ->
                                    wybranyAdept = adeptSkill
                                    val pool = if (wybranaPlecIndex == 0) HeroLogic.imieMall else HeroLogic.imieKall
                                    val rasa = MainActivity.listaRasa[wybranaRasaIndex]
                                    nameSuggestions = NameGenerator.generateNames(pool, rasa)
                                    selectedNameSuggestionIndex = 0
                                    heroName = nameSuggestions[0]
                                    onMenuAppChange(6)
                                }
                            )
                        }
                        6 -> {
                            EquipmentNameSelectionScreen(
                                wybranaKlasaIndex = wybranaKlasaIndex,
                                wybranaRasaIndex = wybranaRasaIndex,
                                wybranyPoziom = wybranyPoziom,
                                wygenerowaneStaty = wygenerowaneStaty,
                                wylosowaneNormalne = wylosowaneNormalne,
                                wylosowaneSpecjalne = wylosowaneSpecjalne,
                                wylosowaneAdepty = wylosowaneAdepty,
                                heroName = heroName,
                                onHeroNameChange = { heroName = it },
                                heroPortraitUri = heroPortraitUri,
                                onPickPortrait = {
                                    pickMedia.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                nameSuggestions = nameSuggestions,
                                selectedNameSuggestionIndex = selectedNameSuggestionIndex,
                                onNameSuggestionIndexChange = { selectedNameSuggestionIndex = it },
                                selectedEquipment = selectedEquipment,
                                onEquipmentChange = { selectedEquipment = it },
                                selectedShopType = selectedShopType,
                                onShopTypeChange = { selectedShopType = it },
                                lobsterFamily = lobsterFamily,
                                onNext = { onMenuAppChange(8) }
                            )
                        }
                        8 -> {
                            CharacterSummary(
                                heroName = heroName,
                                heroPortraitUri = heroPortraitUri,
                                selectedEquipment = selectedEquipment,
                                wygenerowaneStaty = wygenerowaneStaty,
                                wybranaKlasaIndex = wybranaKlasaIndex,
                                wybranaRasaIndex = wybranaRasaIndex,
                                wybranaPlecIndex = wybranaPlecIndex,
                                wybranyWiekIndex = wybranyWiekIndex,
                                wybranyPoziom = wybranyPoziom,
                                cechyFizyczne = cechyFizyczne,
                                wylosowaneCechyCharakteru = wylosowaneCechyCharakteru,
                                wylosowaneSpecjalne = wylosowaneSpecjalne,
                                wylosowaneNormalne = wylosowaneNormalne,
                                wybranyAdept = wybranyAdept,
                                statyDrugorzedne = statyDrugorzedne,
                                listaWiek = listaWiek,
                                slaboFamily = slaboFamily,
                                lobsterFamily = lobsterFamily,
                                isGMMode = isGMMode,
                                readFromFile = readFromFile,
                                saveToFile = saveToFile,
                                playSound = playSoundAction,
                                onComplete = { onMenuAppChange(1) }
                            )
                        }
                        7 -> {
                            // Edycja Bohatera (Settings/Edit)
                            val currentFile = "hero_$selectedHeroId"
                            var heroData by remember(selectedHeroId) { mutableStateOf(readFromFile(currentFile)) }
                            val lines = heroData.lines()
                            
                            val infoLine1 = lines.find { it.startsWith("Rasa:") || it.startsWith("Rasa: ") } ?: ""
                            val infoLine2 = lines.find { it.startsWith("Płeć:") || it.startsWith("Płeć: ") } ?: ""
                            val pdLine = lines.find { it.startsWith("PD:") || it.startsWith("PD: ") } ?: "PD: 0"
                            val lvlLine = lines.find { it.startsWith("POZIOM:") || it.startsWith("POZIOM: ") } ?: "POZIOM: 0"
                            val pfLine = lines.find { it.startsWith("PF:") || it.startsWith("PF: ") } ?: "PF: 0"

                            val rasa = if (infoLine1.contains("Rasa:")) infoLine1.substringAfter("Rasa:").substringBefore(",").trim() else ""
                            val klasa = if (infoLine1.contains("Klasa:")) infoLine1.substringAfter("Klasa:").trim() else ""
                            val plecRaw = if (infoLine2.contains("Płeć:")) infoLine2.substringAfter("Płeć:").substringBefore(",").trim() else ""
                            val plecSymbol = if (plecRaw.contains("Mężczyzna", ignoreCase = true)) "♂" else if (plecRaw.isEmpty()) "" else "♀"
                            val currentPd = pdLine.substringAfter("PD:").trim().toIntOrNull() ?: 0
                            val currentPF = pfLine.substringAfter("PF:").trim().toIntOrNull() ?: 0
                            val currentLvl = if (lvlLine.contains("POZIOM:")) {
                                lvlLine.substringAfter("POZIOM:").trim().substringBefore(" ").toIntOrNull() ?: 0
                            } else {
                                0
                            }
                            
                            val historyFile = "hero_${selectedHeroId}_pd_history"
                            var pdHistory by remember { mutableStateOf(readFromFile(historyFile)) }
                            
                            var showSlider by remember { mutableStateOf(false) }
                            var sliderValue by remember { mutableStateOf(0f) }
                            val pdValues = remember {
                                val list = mutableListOf<Int>()
                                for (i in 10..200 step 5) list.add(i)
                                for (i in 210..1000 step 10) list.add(i)
                                for (i in 1050..2000 step 50) list.add(i)
                                for (i in 2100..3000 step 100) list.add(i)
                                list
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("EDYCJA BOHATERA", color = Color.Red, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = nosiferFamily)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Portret
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray)
                                        .clickable {
                                            pickMedia.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        },
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
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                // Imię
                                OutlinedTextField(
                                    value = heroName,
                                    onValueChange = { heroName = it },
                                    label = { Text("Imię bohatera", color = Color.Gray) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        fontFamily = lobsterFamily
                                    ),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Yellow,
                                        unfocusedBorderColor = Color.Gray,
                                        cursorColor = Color.Yellow,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                if (rasa.isNotEmpty() || klasa.isNotEmpty()) {
                                    Text("$rasa $plecSymbol $klasa", color = Color.Yellow, fontSize = 18.sp)
                                }
                                Text("✪ $currentLvl | PD: $currentPd | 🍀: $currentPF" , color = Color.Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                // Sekcja PD zawsze widoczna
                                val pdToAdd = pdValues[sliderValue.toInt().coerceIn(0, pdValues.size - 1)]
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Button(
                                        onClick = {
                                            if (!showSlider) {
                                                sliderValue = 0f
                                                showSlider = true
                                            } else {
                                                if (pdToAdd > 0) {
                                                    val updatedLines = heroData.lines().toMutableList()
                                                    val pdIdx = updatedLines.indexOfFirst { it.startsWith("PD:") || it.startsWith("PD: ") }
                                                    if (pdIdx != -1) {
                                                        updatedLines[pdIdx] = "PD: ${currentPd + pdToAdd}"
                                                    }
                                                    saveToFile(currentFile, updatedLines.joinToString("\n"))
                                                    
                                                    // Zapis historii
                                                    val timestamp = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(
                                                        Date()
                                                    )
                                                    val historyEntry = "$timestamp: +$pdToAdd PD (Suma: ${currentPd + pdToAdd})\n"
                                                    val updatedHistory = historyEntry + pdHistory
                                                    saveToFile(historyFile, updatedHistory)
                                                    pdHistory = updatedHistory
                                                    
                                                    // Przeładowanie aktualnych danych w UI bez wychodzenia
                                                    heroData = updatedLines.joinToString("\n")
                                                    
                                                    playSoundAction(R.raw.roll, 1)
                                                }
                                                showSlider = false
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (showSlider) Color(0xFF2E7D32) else Color.DarkGray, 
                                            contentColor = Color.White
                                        )
                                    ) {
                                        if (showSlider) {
                                            Text("$currentPd + $pdToAdd = ${currentPd + pdToAdd}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("PD: $currentPd", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (showSlider) {
                                        Slider(
                                            value = sliderValue,
                                            onValueChange = { sliderValue = it },
                                            valueRange = 0f..(pdValues.size - 1).toFloat(),
                                            steps = if (pdValues.size > 1) pdValues.size - 2 else 0,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 32.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.Green,
                                                activeTrackColor = Color.Green
                                            )
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    Text("HISTORIA PD", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .padding(horizontal = 16.dp)
                                            .background(
                                                Color.Black.copy(alpha = 0.3f),
                                                RoundedCornerShape(4.dp)
                                            )
                                    ) {
                                        Text(
                                            text = if (pdHistory.isEmpty()) "Brak historii" else pdHistory,
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            fontFamily = slaboFamily,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Button(
                                        onClick = {
                                            // Zapisz zmiany (Imię i Portret)
                                            val newData = lines.toMutableList()
                                            
                                            // Update Name (line 0) with Prefix
                                            val nameToSave = if (heroName.startsWith("Imie: ")) heroName else "Imie: $heroName"
                                            if (newData.isNotEmpty()) {
                                                newData[0] = nameToSave
                                            } else {
                                                newData.add(nameToSave)
                                            }

                                            // Update or Add Portrait
                                            val portraitIdx = newData.indexOfFirst { it.startsWith("Portrait: ") }
                                            if (heroPortraitUri != null) {
                                                if (portraitIdx != -1) {
                                                    newData[portraitIdx] = "Portrait: $heroPortraitUri"
                                                } else {
                                                    // Find a good place to insert - usually after name/race/class
                                                    // Let's find "POZIOM:" or "PD:" or end of file
                                                    var insertAt = newData.indexOfFirst { it.startsWith("POZIOM:") || it.startsWith("PD:") }
                                                    if (insertAt == -1) insertAt = newData.size.coerceAtMost(3)
                                                    newData.add(insertAt, "Portrait: $heroPortraitUri")
                                                }
                                            } else if (portraitIdx != -1) {
                                                // Optional: remove if null? Users might want to keep it or reset it.
                                                // For now, let's just keep it if it exists or not add it if it doesn't.
                                            }
                                            
                                            saveToFile(currentFile, newData.joinToString("\n"))
                                            playSoundAction(R.raw.roll, 1)
                                            onMenuAppChange(1)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                    ) {
                                        Text("ZAPISZ", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { showDeleteDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF880000), contentColor = Color.White)
                                    ) {
                                        Text("USUŃ", fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (showDeleteDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showDeleteDialog = false },
                                        containerColor = Color.DarkGray,
                                        title = { Text("Usuwanie bohatera", color = Color.White, fontWeight = FontWeight.Bold) },
                                        text = { Text("Czy na pewno chcesz bezpowrotnie usunąć tego bohatera?", color = Color.LightGray) },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    context.deleteFile(currentFile)
                                                    context.deleteFile("${currentFile}_pd_history")
                                                    playSoundAction(R.raw.roll, 1)
                                                    showDeleteDialog = false
                                                    onMenuAppChange(1)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                                            ) {
                                                Text("\uD83E\uDE8F USUŃ")
                                            }
                                        },
                                        dismissButton = {
                                            Button(
                                                onClick = { showDeleteDialog = false },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray, contentColor = Color.White)
                                            ) {
                                                Text("❌ ANULUJ")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        9 -> {
                            var heroData by remember(selectedHeroId) { mutableStateOf(readFromFile("hero_$selectedHeroId")) }
                            
                            Column(modifier = Modifier.fillMaxSize()) {
                                TextField(
                                    value = heroData,
                                    onValueChange = { heroData = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = slaboFamily),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1A1A1A),
                                        unfocusedContainerColor = Color(0xFF1A1A1A),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { 
                                            // Walidacja/Automatyczne dodawanie Dnia Tygodnia przy zapisie jeśli brakuje w formacie delimited
                                            val validatedData = heroData.lines().joinToString("\n") { line ->
                                                if (line.startsWith("DATA:") && line.contains("|") && !line.contains("DZIEŃ:")) {
                                                    val dPart = line.substringAfter("DATA:").substringBefore("|").trim()
                                                    val dVal = dPart.split(".").firstOrNull()?.toIntOrNull() ?: 6
                                                    val daysOfWeek = listOf("Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota", "Niedziela")
                                                    val dayName = daysOfWeek[(dVal - 1) % 7]
                                                    "$line | DZIEŃ: $dayName"
                                                } else line
                                            }
                                            
                                            // Clean legacy progress from heroData before saving
                                            val cleanData = HeroLogic.stripLegacyProgressTags(validatedData)
                                            saveToFile("hero_$selectedHeroId", cleanData)
                                            
                                            // Extract and save progress as JSON
                                            val progress = HeroLogic.getHeroProgress(selectedHeroId.toString(), validatedData)
                                            val progressJson = Json.encodeToString(progress)
                                            saveToFile("progress_$selectedHeroId.json", progressJson)

                                            playSoundAction(R.raw.klik1, 0)
                                            onMenuAppChange(1) 
                                        },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)
                                    ) {
                                        Text("ZAPISZ", fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onMenuAppChange(1) },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray, contentColor = Color.White)
                                    ) {
                                        Text("ANULUJ", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        -1 -> {
                            SettingsScreen(
                                readFromFile = readFromFile,
                                saveToFile = saveToFile,
                                context = context,
                                updateFileList = updateFileList,
                                daysElapsed = daysElapsed,
                                isEditing = isSettingsEditing,
                                onEditingChange = { b: Boolean -> isSettingsEditing = b },
                                onClose = { onMenuAppChange(0 ) },
                                onGMModeChange = { b: Boolean -> isGMMode = b },
                                onNavigate = { n: Int -> onMenuAppChange(n ) },
                                isGMMode = isGMMode,
                                playSound = { resId, part, offset -> playSound(context, coroutineScope, resId, part, offset) }
                            )
                        }
                        10 -> {
                            CharacterInfoAdvancementScreen(
                                selectedHeroId = selectedHeroId,
                                nosiferFamily = nosiferFamily,
                                slaboFamily = slaboFamily,
                                onMenuAppChange = { n: Int -> onMenuAppChange(n ) },
                                readFromFile = readFromFile,
                                saveToFile = saveToFile,
                                playSoundAction = playSoundAction,
                                context = context,
                                isGMMode = isGMMode,
                                onHeroProgressChange = { currentHeroProgress = it }
                            )
                        }
                        35 -> {
                            val progress = currentHeroProgress
                            val currentSettlementData = progress?.currentCalendar?.split("|")?.getOrNull(3)?.trim() ?: ""

                            CityScreen(
                                heroProgress = progress,
                                nosiferFamily = nosiferFamily,
                                slaboFamily = slaboFamily,
                                lobsterFamily = lobsterFamily,
                                onBack = { onMenuAppChange(61) },
                                onMenuAppChange = onMenuAppChange,
                                onShopTypeChange = { selectedShopType = it },
                                readFromFile = readFromFile,
                                saveToFile = saveToFile,
                                playSoundAction = playSoundAction,
                                isGMMode = isGMMode,
                                settlementData = currentSettlementData,
                                onHeroProgressChange = { currentHeroProgress = it }
                            )
                        }
                        50 -> {
                            // City & Travel Hub Screen
                            val heroFileName = "hero_$selectedHeroId"
                            val progressFile = "progress_$selectedHeroId.json"
                            val heroDataHub = try { readFromFile(heroFileName) } catch(e: Exception) { "" }
                            val dataNowStr = currentHeroProgress?.currentCalendar?.ifEmpty { null } ?: HeroLogic.getCalendarData(heroDataHub)
                            val parts = dataNowStr.split("|").map { it.trim() }
                            val cityName = if (parts.size >= 1 && parts[0].isNotEmpty() && parts[0] != "Nieznane Miejsce") parts[0] else {
                                currentHeroProgress?.currentCalendar?.split("|")?.firstOrNull()?.trim() ?: "Nieznane Miejsce"
                            }
                            val timePart = if (parts.size >= 3) parts[2] else parts.lastOrNull() ?: ""
                            
                            val timeAndWeatherLabel = HeroLogic.getTimeAndWeatherLabel(heroDataHub)
                            val currentZone = if (parts.size >= 7) parts[6] else "Brama Północna"

                            // Determine gate resources (2 per gate, deterministic)
                            val allRes = listOf("Zioła", "Minerały", "Łowiectwo", "Rybołówstwo")
                            val gateResources = allRes.shuffled(kotlin.random.Random((cityName + currentZone).hashCode().toLong())).take(2)
                            val discoveredResources = heroDataHub.lines()
                                .filter { it.startsWith("GATE_DISCOVERY:$cityName|$currentZone|") }
                                .map { it.substringAfterLast("|").trim() }
                            val isFullyDiscovered = discoveredResources.size >= gateResources.size

                            // Load or generate settlement
                            val settlement = WorldPersistence.loadSettlement(context, cityName) ?: run {
                                val newSettlement = SettlementGenerator.generateSettlement(context, cityName, SettlementType.CITY)
                                newSettlement
                            }
                            val settlementJson = "[EXTERNAL]"

                            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        cityName.uppercase(),
                                        color = Color.Red,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = nosiferFamily,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        currentZone,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Light,
                                        fontFamily = slaboFamily,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    if (timePart.isNotEmpty()) {
                                        Text(
                                            timeAndWeatherLabel,
                                            color = Color.Gray,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(48.dp))

                                    val isElfHub = HeroLogic.isElf(heroDataHub)
                                    val isNightHub = HeroLogic.isNight(timePart)

                                    // Entry Button
                                    Button(
                                        onClick = {
                                            playSoundAction(R.raw.klik1, 0)
                                            // Sync settlement data to calendar string for CityScreen compatibility
                                            val date = if (parts.size >= 2) parts[1] else "1.1.2024"
                                            val time = if (parts.size >= 3) parts[2] else "12:00"
                                            val x = if (parts.size >= 5) parts[4] else "500"
                                            val y = if (parts.size >= 6) parts[5] else "500"
                                            val existingZone = parts.getOrNull(6)
                                            val zone = if (!existingZone.isNullOrBlank()) existingZone else (settlement.zones.firstOrNull()?.name ?: "")
                                            
                                            val newCalendar = "$cityName | $date | $time | $settlementJson | $x | $y | $zone"
                                            
                                            // Sync to HeroProgress
                                            currentHeroProgress?.let {
                                                val updated = it.copy(currentCalendar = newCalendar)
                                                currentHeroProgress = updated
                                                saveToFile(progressFile, Json.encodeToString(updated))
                                            }

                                            val updatedHeroData = HeroLogic.updateCalendarData(heroDataHub, newCalendar)
                                            // Fix: Zapisujemy dane bez ryzykownego prefiksowania pierwszej linii, 
                                            // HeroLogic.updateCalendarData dba już o poprawność tagów.
                                            saveToFile(heroFileName, updatedHeroData)
                                            onMenuAppChange(35)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDAA520)),
                                        modifier = Modifier.fillMaxWidth().height(65.dp).padding(vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("WEJDŹ DO MIASTA", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Travel Button
                                    val travelLocked = isNightHub && !isElfHub
                                    Button(
                                        onClick = {
                                            if (travelLocked) {
                                                android.widget.Toast.makeText(context, "Podróż jest niemożliwa w nocy dla Twojej rasy.", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                playSoundAction(R.raw.klik1, 0)
                                                onMenuAppChange(61)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (travelLocked) Color.DarkGray else Color(0xFF5D4037)),
                                        modifier = Modifier.fillMaxWidth().height(65.dp).padding(vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("PODRÓŻ" + (if (travelLocked) " (BLOKADA NOCNA)" else ""), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Scout / Gathering Buttons
                                    if (isFullyDiscovered) {
                                        val gatheringActions = listOf(
                                            GatheringAction("Zioła", "Zielarstwo", "INT", Color(0xFF43A047)),
                                            GatheringAction("Minerały", "Metalurgia", "INT", Color(0xFF757575)),
                                            GatheringAction("Łowiectwo", "Łowiectwo", "INT", Color(0xFF8D6E63)),
                                            GatheringAction("Rybołówstwo", "Sztuka przetrwania", "INT", Color(0xFF0288D1))
                                        ).filter { it.name in discoveredResources }

                                        gatheringActions.forEach { action ->
                                            Button(
                                                onClick = {
                                                    playSoundAction(R.raw.klik1, 0)
                                                    val result = HeroLogic.performGathering(context, selectedHeroId, action, readFromFile, saveToFile)
                                                    android.widget.Toast.makeText(context, result, android.widget.Toast.LENGTH_LONG).show()
                                                    onMenuAppChange(50)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = action.color),
                                                modifier = Modifier.fillMaxWidth().height(65.dp).padding(vertical = 6.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(action.name.uppercase() + " (2h)", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        val scoutLocked = isNightHub && !isElfHub
                                        Button(
                                            onClick = {
                                                if (scoutLocked) {
                                                    android.widget.Toast.makeText(context, "Zwiad jest niemożliwy w nocy dla Twojej rasy.", android.widget.Toast.LENGTH_SHORT).show()
                                                } else {
                                                    playSoundAction(R.raw.klik1, 0)
                                                    onMenuAppChange(62)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (scoutLocked) Color.DarkGray else Color(0xFF2E7D32)),
                                            modifier = Modifier.fillMaxWidth().height(65.dp).padding(vertical = 6.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("ZWIAD" + (if (scoutLocked) " (BLOKADA NOCNA)" else ""), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Stable Button
                                    Button(
                                        onClick = {
                                            playSoundAction(R.raw.klik1, 0)
                                            onMenuAppChange(63)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4513)),
                                        modifier = Modifier.fillMaxWidth().height(65.dp).padding(vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("STAJNIA", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(32.dp))

                                    Button(
                                        onClick = {
                                            playSoundAction(R.raw.klik1, 0)
                                            onMenuAppChange(10)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                        modifier = Modifier.fillMaxWidth().height(55.dp),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("POWRÓT DO KARTY", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        61 -> {
                            val progress = currentHeroProgress
                            if (progress != null) {
                                HeroTravelMapScreen(
                                    heroId = selectedHeroId,
                                    heroProgress = progress,
                                    readFromFile = readFromFile,
                                    saveToFile = saveToFile,
                                    onHeroProgressChange = { currentHeroProgress = it },
                                    onEnterCity = { onMenuAppChange(35) },
                                    onBack = { onMenuAppChange(10) }
                                )
                            }
                        }
                        64 -> {
                            // Podróż Screen - Generowanie Świata Imperium
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                                val currentFile = "hero_$selectedHeroId"
                                val worldFile = "hero_world_$selectedHeroId"
                                val heroData = readFromFile(currentFile)
                                
                                // Wczytywanie lub generowanie trwałego świata dla bohatera
                                var worldNodes by remember(selectedHeroId) {
                                    val savedWorld = readFromFile(worldFile)
                                    if (savedWorld.isBlank()) {
                                        val newWorld = EmpireWorldGenerator.generateWorldNodes(15)
                                        val json = kotlinx.serialization.json.Json.encodeToString(
                                            kotlinx.serialization.builtins.ListSerializer(TravelNode.serializer()),
                                            newWorld
                                        )
                                        saveToFile(worldFile, json)
                                        mutableStateOf(newWorld)
                                    } else {
                                        try {
                                            val decoded = kotlinx.serialization.json.Json.decodeFromString(
                                                kotlinx.serialization.builtins.ListSerializer(TravelNode.serializer()),
                                                savedWorld
                                            )
                                            mutableStateOf(decoded)
                                        } catch (e: Exception) {
                                            val newWorld = EmpireWorldGenerator.generateWorldNodes(15)
                                            val repairedWorld = kotlinx.serialization.json.Json.encodeToString(
                                                kotlinx.serialization.builtins.ListSerializer(TravelNode.serializer()),
                                                newWorld
                                            )
                                            saveToFile(worldFile, repairedWorld)
                                            mutableStateOf(newWorld)
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val dataNowStr = HeroLogic.getCalendarData(heroData)
                                    Text("WYBIERZ CEL PODRÓŻY", color = Color(0xFFDAA520), fontSize = 24.sp, fontFamily = nosiferFamily, textAlign = TextAlign.Center)
                                    Text("Imperium Reiklandzkie", color = Color.Gray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    val ownedHorses = HeroLogic.countOwnedHorses(heroData)
                                    val usedHorses = HeroLogic.countUsedHorses(heroData)
                                    val hasTransport = heroData.contains("Wóz") || heroData.contains("Bryczka") || heroData.contains("Rydwan")
                                    
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(worldNodes.filter { it.discovered }) { node ->
                                            val settlement = WorldPersistence.decodeSettlementOrNull(node.settlementData)
                                            val type = settlement?.type ?: SettlementType.VILLAGE
                                            
                                            // Wyliczanie dystansu i czasu na podstawie koordynatów
                                            val parts = dataNowStr.split("|").map { it.trim() }
                                            val currentX = parts.getOrNull(4)?.toFloatOrNull() ?: 500f
                                            val currentY = parts.getOrNull(5)?.toFloatOrNull() ?: 500f
                                            
                                            val distance = kotlin.math.sqrt(
                                                Math.pow((node.x - currentX).toDouble(), 2.0) +
                                                Math.pow((node.y - currentY).toDouble(), 2.0)
                                            ).toFloat()
                                            
                                            // 1 jednostka mapy = ~0.1h marszu bazowo (do dostosowania)
                                            val baseHours = (distance * 0.1f).toInt().coerceAtLeast(1)
                                            val speedMult = HeroLogic.getTravelSpeedMultiplier(heroData)
                                            val travelHours = (baseHours / speedMult).toInt().coerceAtLeast(1)

                                            Card(
                                                onClick = {
                                                    playSoundAction(R.raw.klik1, 0)
                                                    if (usedHorses > ownedHorses) {
                                                        android.widget.Toast.makeText(context, "Brak wystarczającej liczby koni!", android.widget.Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        val travelCost = (travelHours / 4) + 1
                                                        
                                                        val copper = HeroLogic.calculateTotalCopper(heroData)
                                                        if (copper < travelCost) {
                                                            android.widget.Toast.makeText(context, "Brak miedziaków na prowiant ($travelCost p)!", android.widget.Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            val updatedMoneyHeroData = HeroLogic.deductMoney(heroData, travelCost)
                                                            saveToFile(currentFile, updatedMoneyHeroData)
                                                            
                                                            val intent = android.content.Intent(context, MainActivity::class.java).apply {
                                                                putExtra("HERO_ID", selectedHeroId.toString())
                                                                putExtra(MainActivity.EXTRA_MENU_APP, 61)
                                                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                                            }
                                                            context.startActivity(intent)
                                                        }
                                                    }
                                                },
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val icon = when(type) {
                                                        SettlementType.CITY -> "🏰"
                                                        SettlementType.TOWN -> "🏠"
                                                        SettlementType.VILLAGE -> "🛖"
                                                    }
                                                    Text(icon, fontSize = 24.sp)
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(node.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                        Text("${type.displayName} (X:${node.x.toInt()} Y:${node.y.toInt()})", color = Color.Gray, fontSize = 12.sp)
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text("${travelHours}h", color = Color(0xFFDAA520), fontWeight = FontWeight.Bold)
                                                        Text("dist: ${distance.toInt()}", color = Color.DarkGray, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    Button(
                                        onClick = { onMenuAppChange(50) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                    ) {
                                        Text("POWRÓT")
                                    }
                                }
                            }
                        }
                        62 -> {
                            // Zwiad Screen
                            val heroFileName = "hero_$selectedHeroId"
                            val heroData = readFromFile(heroFileName)
                            val worldFile = "hero_world_$selectedHeroId"
                            val progressFile = "progress_$selectedHeroId.json"

                            val calendarData = currentHeroProgress?.currentCalendar?.ifEmpty { null } ?: HeroLogic.getCalendarData(heroData)
                            val parts = calendarData.split("|").map { it.trim() }
                            val cityName = if (parts.size >= 1 && parts[0].isNotEmpty() && parts[0] != "Nieznane Miejsce") parts[0] else {
                                // Try to get from progress if legacy failed
                                currentHeroProgress?.currentCalendar?.split("|")?.firstOrNull()?.trim()?.ifEmpty { "Nieznane Miejsce" } ?: "Nieznane Miejsce"
                            }
                            val currentZone = if (parts.size >= 7) parts[6] else "Brama Północna"
                            
                            val timePart = if (parts.size >= 3) parts[2] else parts.lastOrNull() ?: ""
                            val timeOfDayLabel = if (timePart.isNotEmpty()) HeroLogic.getTimeOfDay(timePart) else ""
                            val isNight = HeroLogic.isNight(timePart)

                            val isElf = HeroLogic.isElf(heroData)
                            val canScout = !isNight || isElf

                            // Deterministic resources for this gate (2 per gate)
                            val allRes = listOf("Zioła", "Minerały", "Łowiectwo", "Rybołówstwo")
                            val gateResources = allRes.shuffled(kotlin.random.Random((cityName + currentZone).hashCode().toLong())).take(2)
                            
                            val discoveredResources = heroData.lines()
                                .filter { it.startsWith("GATE_DISCOVERY:$cityName|$currentZone|") }
                                .map { it.substringAfterLast("|").trim() }
                            val isFullyDiscovered = discoveredResources.size >= gateResources.size

                            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        "ZWIAD: $currentZone",
                                        color = Color(0xFF2E7D32),
                                        fontSize = 24.sp,
                                        fontFamily = nosiferFamily,
                                        textAlign = TextAlign.Center
                                    )
                                    if (timeOfDayLabel.isNotEmpty()) {
                                        Text(
                                            "Pora dnia: $timeOfDayLabel" + (if (isNight) " (NOC)" else ""),
                                            color = if (isNight) Color.Red else Color.Gray,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    if (!canScout) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text(
                                            "Zwiad w nocy jest niemożliwy dla Twojej rasy.",
                                            color = Color.Red,
                                            textAlign = TextAlign.Center,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(32.dp))
                                        Button(
                                            onClick = { onMenuAppChange(50) },
                                            modifier = Modifier.fillMaxWidth().height(65.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                        ) {
                                            Text("POWRÓT", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(32.dp))

                                        if (isFullyDiscovered) {
                                            Text(
                                                "TEREN W PEŁNI ROZPOZNANY",
                                                color = Color.Green,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = slaboFamily
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        } else {
                                            val scoutingLevels = listOf(
                                                Triple("Pobliże", 1, Color(0xFF4CAF50)),
                                                Triple("Okolice", 3, Color(0xFF388E3C)),
                                                Triple("Granica", 5, Color(0xFF1B5E20))
                                            )

                                            scoutingLevels.forEach { (label, hours, color) ->
                                                Button(
                                                    onClick = {
                                                        playSoundAction(R.raw.klik1, 0)
                                                        val pdReward = hours * (5..9).random()
                                                        val copperReward = hours * (15..19).random()
                                                        
                                                        val fileName = "hero_$selectedHeroId"
                                                        var currentData = readFromFile(fileName)
                                                        val lines = currentData.lines().toMutableList()

                                                        // PITY & DISCOVERY LOGIC
                                                        val scoutRoll = (1..100).random()
                                                        val successChance = hours * 20
                                                        var discoveryMsg = ""

                                                        if (scoutRoll <= successChance) {
                                                            val remaining = gateResources.filter { it !in discoveredResources }
                                                            if (remaining.isNotEmpty()) {
                                                                val found = remaining.random()
                                                                lines.add("GATE_DISCOVERY:$cityName|$currentZone|$found")
                                                                discoveryMsg = "\nOdkryto: ${found.uppercase()}!"
                                                                // Reset pity count
                                                                lines.removeAll { it.startsWith("GATE_SCOUT_COUNT:$cityName|$currentZone|") }
                                                            }
                                                        } else {
                                                            val countPrefix = "GATE_SCOUT_COUNT:$cityName|$currentZone|"
                                                            val countIdx = lines.indexOfFirst { it.startsWith(countPrefix) }
                                                            var currentCount = 0
                                                            if (countIdx != -1) {
                                                                currentCount = lines[countIdx].substringAfterLast("|").toIntOrNull() ?: 0
                                                                currentCount++
                                                                lines[countIdx] = "$countPrefix$currentCount"
                                                            } else {
                                                                currentCount = 1
                                                                lines.add("$countPrefix$currentCount")
                                                            }

                                                            if (currentCount >= 3) {
                                                                val remaining = gateResources.filter { it !in discoveredResources }
                                                                remaining.forEach { lines.add("GATE_DISCOVERY:$cityName|$currentZone|$it") }
                                                                discoveryMsg = "\nPo wielu próbach znasz już ten teren doskonale!"
                                                                lines.removeAll { it.startsWith(countPrefix) }
                                                            }
                                                        }
                                                        
                                                        // Legacy World Discovery
                                                        val worldData = readFromFile(worldFile)
                                                        if (worldData.isNotBlank()) {
                                                            try {
                                                                val nodes = decodeNodes(worldData).toMutableList()
                                                                val undiscovered = nodes.filter { !it.discovered }
                                                                if (undiscovered.isNotEmpty()) {
                                                                    val toDiscover = undiscovered.random()
                                                                    val idx = nodes.indexOfFirst { it.id == toDiscover.id }
                                                                    if (idx != -1) {
                                                                        nodes[idx] = nodes[idx].copy(discovered = true)
                                                                        saveToFile(worldFile, encodeNodes(nodes))
                                                                        discoveryMsg += "\nOdkryto lokację: ${toDiscover.name}!"
                                                                    }
                                                                }
                                                            } catch (e: Exception) {}
                                                        }

                                                        // Rewards
                                                        val pdIdx = lines.indexOfFirst { it.startsWith("PD:") }
                                                        if (pdIdx != -1) {
                                                            val currentPd = lines[pdIdx].substringAfter("PD:").trim().toIntOrNull() ?: 0
                                                            lines[pdIdx] = "PD: ${currentPd + pdReward}"
                                                        }
                                                        val currentTotal = HeroLogic.calculateTotalCopper(currentData)
                                                        val newTotal = currentTotal + copperReward
                                                        HeroLogic.updateCoinLine(lines, "⛀ Złota korona (zk)", newTotal / 480)
                                                        HeroLogic.updateCoinLine(lines, "⛀ Srebrny szyling (s)", (newTotal % 480) / 12)
                                                        HeroLogic.updateCoinLine(lines, "⛀ Miedziany pens (p)", newTotal % 12)

                                                        val newCalendar = HeroLogic.advanceTime(calendarData, hours)
                                                        HeroLogic.updateCalendarInLines(lines, newCalendar)

                                                        // Sync to HeroProgress
                                                        currentHeroProgress?.let {
                                                            val updated = it.copy(currentCalendar = newCalendar)
                                                            currentHeroProgress = updated
                                                            saveToFile(progressFile, Json.encodeToString(updated))
                                                        }

                                                        saveToFile(fileName, lines.joinToString("\n"))
                                                        android.widget.Toast.makeText(context, "Zwiad zakończony.$discoveryMsg", android.widget.Toast.LENGTH_LONG).show()
                                                        
                                                        // Log history
                                                        val historyFile = "hero_${selectedHeroId}_pd_history"
                                                        val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                                        val historyLog = try { context.openFileInput(historyFile).bufferedReader().readText() } catch(e: Exception) { "" }
                                                        saveToFile(historyFile, "$timestamp: Zwiad: $label (+$pdReward PD, +$copperReward p, +${hours}h)\n$historyLog")
                                                        
                                                        onMenuAppChange(62)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = color),
                                                    modifier = Modifier.fillMaxWidth().height(65.dp).padding(vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(label.uppercase() + " (${hours}h)", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        Divider(color = Color.Gray, thickness = 1.dp)
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text("ZBIERACTWO", color = Color.Yellow, fontSize = 20.sp, fontFamily = nosiferFamily)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        val gatheringActions = listOf(
                                            GatheringAction("Zioła", "Zielarstwo", "INT", Color(0xFF43A047)),
                                            GatheringAction("Minerały", "Metalurgia", "INT", Color(0xFF757575)),
                                            GatheringAction("Łowiectwo", "Łowiectwo", "INT", Color(0xFF8D6E63)),
                                            GatheringAction("Rybołówstwo", "Sztuka przetrwania", "INT", Color(0xFF0288D1))
                                        ).filter { it.name in discoveredResources }

                                        gatheringActions.forEach { action ->
                                            Button(
                                                onClick = {
                                                    playSoundAction(R.raw.klik1, 0)
                                                    val result = HeroLogic.performGathering(context, selectedHeroId, action, readFromFile, saveToFile)
                                                    android.widget.Toast.makeText(context, result, android.widget.Toast.LENGTH_LONG).show()
                                                    onMenuAppChange(50)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = action.color),
                                                modifier = Modifier.fillMaxWidth().height(65.dp).padding(vertical = 6.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(action.name.uppercase() + " (2h)", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        
                                        if (discoveredResources.isEmpty()) {
                                            Text("Musisz najpierw przeprowadzić zwiad bramy.", color = Color.Gray, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = { onMenuAppChange(50) },
                                            modifier = Modifier.fillMaxWidth().height(55.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                        ) {
                                            Text("POWRÓT DO MIASTA", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        63 -> {
                            // Stajnia Screen
                            ShopScreen(
                                selectedHeroId = selectedHeroId,
                                nosiferFamily = nosiferFamily,
                                slaboFamily = slaboFamily,
                                onBack = { onMenuAppChange(50) },
                                onNavigate = { onMenuAppChange(it) },
                                readFromFile = readFromFile,
                                saveToFile = saveToFile,
                                playSoundAction = playSoundAction,
                                isGMMode = isGMMode,
                                initialShopType = "Stajnia"
                            )
                        }
                        15 -> {
                            ShopScreen(
                                selectedHeroId = selectedHeroId,
                                nosiferFamily = nosiferFamily,
                                slaboFamily = slaboFamily,
                                onBack = { onMenuAppChange(50) },
                                onNavigate = { onMenuAppChange(it) },
                                readFromFile = readFromFile,
                                saveToFile = saveToFile,
                                playSoundAction = playSoundAction,
                                isGMMode = isGMMode,
                                initialShopType = selectedShopType
                            )
                        }
                        30 -> {
                            MapsScreen(
                                selectedHeroId = selectedHeroId,
                                nosiferFamily = nosiferFamily,
                                aladinFamily = aladinFamily,
                                slaboFamily = slaboFamily,
                                lobsterFamily = lobsterFamily,
                                readFromFile = readFromFile,
                                saveToFile = saveToFile,
                                onBack = { onMenuAppChange(10) },
                                isGMMode = isGMMode
                            )
                        }
                        22 -> {
                            val heroFileName = "hero_$selectedHeroId"
                            val heroDataBuildings = try { readFromFile(heroFileName) } catch(e: Exception) { "" }
                            val dataNowStr = currentHeroProgress?.currentCalendar?.ifEmpty { null } ?: HeroLogic.getCalendarData(heroDataBuildings)
                            val parts = dataNowStr.split("|").map { it.trim() }
                            val cityName = if (parts.size >= 1) parts[0] else "Nieznane Miejsce"
                            val currentSettlementData = if (parts.size >= 4) parts[3] else ""

                            BuildingsScreen(
                                heroDataInitial = heroDataBuildings,
                                heroProgressInitial = currentHeroProgress,
                                selectedCity = cityName,
                                currentFile = heroFileName,
                                dataNowInitial = dataNowStr,
                                initialShopType = selectedShopType,
                                onBack = { onMenuAppChange(35) },
                                onNavigate = { onMenuAppChange(it) },
                                playSoundAction = playSoundAction,
                                saveToFile = saveToFile,
                                readFromFile = readFromFile,
                                selectedHeroId = selectedHeroId,
                                settlementData = currentSettlementData,
                                onHeroProgressChange = { newProgress ->
                                    currentHeroProgress = newProgress
                                    val jsonStr = Json.encodeToString(newProgress)
                                    saveToFile("progress_$selectedHeroId.json", jsonStr)
                                }
                            )
                        }
                        31 -> {
                            PdHistoryScreen(
                                selectedHeroId = selectedHeroId,
                                nosiferFamily = nosiferFamily ?: FontFamily.Default,
                                slaboFamily = slaboFamily ?: FontFamily.Default,
                                readFromFile = readFromFile,
                                onBack = { onMenuAppChange(10) }
                            )
                        }
                        20 -> {
                            val notesFile = "hero_notes_$selectedHeroId"
                            val imagesFile = "hero_notes_imgs_$selectedHeroId"
                            var currentPage by remember { mutableIntStateOf(1) }
                            val allNotes = remember {
                                val content = readFromFile(notesFile)
                                if (content.isEmpty()) {
                                    MutableList(10) { "" }
                                } else {
                                    val pages = content.split("---PAGE_BREAK---").toMutableList()
                                    while (pages.size < 10) pages.add("")
                                    pages.take(10).toMutableList()
                                }
                            }
                            val allImages = remember {
                                val content = readFromFile(imagesFile)
                                if (content.isEmpty()) {
                                    MutableList(10) { "" }
                                } else {
                                    val uris = content.split("---PAGE_BREAK---").toMutableList()
                                    while (uris.size < 10) uris.add("")
                                    uris.take(10).toMutableList()
                                }
                            }
                            var notesText by remember(currentPage) { mutableStateOf(allNotes[currentPage - 1]) }
                            var currentImageUri by remember(currentPage) { mutableStateOf(allImages[currentPage - 1]) }

                            val launcher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.PickVisualMedia(),
                                onResult = { uri ->
                                    if (uri != null) {
                                        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        currentImageUri = uri.toString()
                                        allImages[currentPage - 1] = currentImageUri
                                        saveToFile(imagesFile, allImages.joinToString("---PAGE_BREAK---"))
                                    }
                                }
                            )

                            val saveAndExit = {
                                allNotes[currentPage - 1] = notesText
                                saveToFile(notesFile, allNotes.joinToString("---PAGE_BREAK---"))
                                saveToFile(imagesFile, allImages.joinToString("---PAGE_BREAK---"))
                                onMenuAppChange(10)
                            }

                            BackHandler { saveAndExit() }

                            Column(modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = {
                                        playSoundAction(R.raw.klik1, 0)
                                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }) {
                                        Text("🖼️  ", fontSize = 24.sp)
                                    }
                                    Text(
                                        "    STRONA $currentPage ",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = autourFamily,
                                        fontSize = 17.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f)
                                        .background(Color.Black)
                                ) {
                                    if (currentImageUri.isNotEmpty()) {
                                        AsyncImage(
                                            model = currentImageUri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            alpha = 0.5f
                                        )
                                    }
                                    OutlinedTextField(
                                        value = notesText,
                                        onValueChange = {
                                            notesText = it
                                            allNotes[currentPage - 1] = it
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontFamily = slaboFamily
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Yellow,
                                            unfocusedBorderColor = Color.Gray,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Slider(
                                    value = currentPage.toFloat(),
                                    onValueChange = {
                                        allNotes[currentPage - 1] = notesText
                                        currentPage = it.toInt()
                                        notesText = allNotes[currentPage - 1]
                                    },
                                    valueRange = 1f..10f,
                                    steps = 8,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        if (currentPage > 1) {
                                            Button(
                                                onClick = {
                                                    allNotes[currentPage - 1] = notesText
                                                    currentPage--
                                                    notesText = allNotes[currentPage - 1]
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                            ) {
                                                Text("${currentPage - 1}", color = Color.White, fontFamily = autourFamily, fontSize = 18.sp)
                                            }
                                        }
                                    }

                                    IconButton(onClick = {
                                        playSoundAction(R.raw.klik1, 0)
                                        allNotes[currentPage - 1] = notesText
                                        saveToFile(notesFile, allNotes.joinToString("---PAGE_BREAK---"))
                                        saveToFile(imagesFile, allImages.joinToString("---PAGE_BREAK---"))
                                    }) {
                                        Text("💾", fontSize = 28.sp)
                                    }

                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        if (currentPage < 10) {
                                            Button(
                                                onClick = {
                                                    allNotes[currentPage - 1] = notesText
                                                    currentPage++
                                                    notesText = allNotes[currentPage - 1]
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                            ) {
                                                Text("${currentPage + 1}", color = Color.White, fontFamily = autourFamily, fontSize = 18.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        21 -> {
                            BackHandler {
                                onMenuAppChange(10)
                            }
                            val currentFile = "hero_$selectedHeroId"
                            var heroData by remember { mutableStateOf(readFromFile(currentFile)) }
                            
                            var selectedCategory by remember { mutableStateOf<String?>(null) }
                            var selectedSubCategory by remember { mutableStateOf<String?>(null) }
                            var expandedItemIndex by remember { mutableIntStateOf(-1) }
                            var selectedPrefix by remember { mutableStateOf("Brak") }
                            var selectedSuffix by remember { mutableStateOf("Brak") }
                            var searchQuery by remember { mutableStateOf("") }

                            // Encumbrance calculation
                            val maxLoad = HeroLogic.getHeroWeightLimit(heroData)
                            val currentWeight = HeroLogic.calculateCurrentWeight(heroData)
                            val encLevel = HeroLogic.getEncumbrance(currentWeight, maxLoad)

                            // Helper to determine category of an item string from file
                            fun getItemCategory(item: String): String {
                                val cleanItem = item.trim()
                                if (cleanItem.startsWith("RESOURCE|")) return "Zasoby"
                                if (cleanItem.startsWith("⛀")) return "Monety"
                                if (HeroLogic.itemPojemniki.any { cleanItem.contains(it, ignoreCase = true) }) return "Pojemniki"
                                if ((HeroLogic.WW0nazwa + HeroLogic.WW1nazwa + HeroLogic.WW2nazwa + HeroLogic.WW3nazwa + HeroLogic.WW4nazwa +
                                            listOf("Sztylet", "Miecz", "Topór", "Młot", "Kostur", "Kord", "Tasak", "Szabla", "Maczuga", "Buzdygan", "Wekiera", "Rapier", "Włócznia", "Kosa", "Widły")).any { cleanItem.contains(it, ignoreCase = true) }) return "Broń ręczna"
                                if ((HeroLogic.US0nazwa + HeroLogic.US1nazwa + HeroLogic.US2nazwa + HeroLogic.US3nazwa + HeroLogic.US4nazwa + HeroLogic.US5nazwa +
                                            listOf("Łuk", "Kusza", "Pistolet", "Rusznica", "Rewolwer", "Dmuchawka", "Proca")).any { cleanItem.contains(it, ignoreCase = true) }) return "Broń strzelecka"
                                if ((HeroLogic.AM0nazwa + HeroLogic.AM1nazwa + HeroLogic.AM2nazwa + HeroLogic.AM3nazwa).any { cleanItem.contains(it, ignoreCase = true) }) return "Pociski"
                                if ((HeroLogic.pancGłowaNazwa + HeroLogic.pancKorpusNazwa + HeroLogic.pancRękaNazwa + HeroLogic.pancNogaNazwa +
                                            HeroLogic.armorLight.map { it.name } + HeroLogic.armorMedium.map { it.name } + HeroLogic.armorHeavy.map { it.name } +
                                            listOf("Tarcza", "puklerz")).any { cleanItem.contains(it, ignoreCase = true) }) return "Pancerz"
                                if ((HeroLogic.ubranieG + HeroLogic.ubranieK + HeroLogic.ubranieR + HeroLogic.ubranieN + HeroLogic.ubranieKN + HeroLogic.uniform + HeroLogic.ubranieS).any { cleanItem.contains(it, ignoreCase = true) }) return "Odzienie"
                                if (HeroLogic.bizuteriaNazwa.any { cleanItem.contains(it, ignoreCase = true) }) return "Biżuteria"
                                if (HeroLogic.instrumentsSpecialist.any { cleanItem.contains(it.name, ignoreCase = true) } ||
                                    HeroLogic.instrumentsStrunowe.any { cleanItem.contains(it.name, ignoreCase = true) } ||
                                    HeroLogic.instrumentsDete.any { cleanItem.contains(it.name, ignoreCase = true) } ||
                                    HeroLogic.instrumentsPerkusja.any { cleanItem.contains(it.name, ignoreCase = true) }) return "Instrument"
                                if ((HeroLogic.narzedziaAll + HeroLogic.toolsSpecialist.map { it.name }).any { cleanItem.contains(it, ignoreCase = true) }) return "Narzędzie"
                                if (HeroLogic.bogactwaAll.any { cleanItem.contains(it, ignoreCase = true) }) return "Majątek"
                                if ((HeroLogic.zwierzętaHodowlaneSsaki + HeroLogic.zwierzętaDzikieSsaki + HeroLogic.zwierzętaHodowlanePtaki).any { cleanItem.contains(it, ignoreCase = true) }) return "Zwierzęta"
                                if (cleanItem.contains("Mikstura", ignoreCase = true) || cleanItem.contains("Eliksir", ignoreCase = true)) return "Mikstura"
                                if ((cleanItem.contains("Księga", ignoreCase = true) || cleanItem.contains("Dziennik", ignoreCase = true) || cleanItem.contains("Tom", ignoreCase = true)) && !cleanItem.contains("Technologia", ignoreCase = true)) return "Księga"
                                return "Inne"
                            }

                            val allOwnedItems = remember(heroData) {
                                val lines = heroData.lines()
                                val result = mutableListOf<String>()
                                
                                // 1. Standard Inventory
                                val startIndex = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
                                if (startIndex != -1) {
                                    lines.drop(startIndex + 1)
                                        .takeWhile { it.startsWith(" ") || it.startsWith("•") || it.startsWith("⛀") }
                                        .map { it.replace("•", "").trim() }
                                        .filter { it.isNotEmpty() }
                                        .forEach { result.add(it) }
                                }

                                // 2. Gathered Resources (ZBIORY)
                                val zbStart = lines.indexOfFirst { it.startsWith("ZBIORY:") }
                                if (zbStart != -1) {
                                    var currentResCat = ""
                                    for (i in zbStart + 1 until lines.size) {
                                        val line = lines[i]
                                        if (line.isEmpty() || (!line.startsWith("   ") && !line.startsWith("      "))) {
                                            if (line.isNotEmpty() && !line.startsWith(" ")) break
                                            continue
                                        }
                                        val trimmed = line.trim()
                                        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                                            currentResCat = trimmed.removeSurrounding("[", "]")
                                        } else if (trimmed.startsWith("•")) {
                                            // Format: "• Name (0.00 kg) (Quality)"
                                            result.add("RESOURCE|$currentResCat|${trimmed.removePrefix("•").trim()}")
                                        }
                                    }
                                }
                                result
                            }

                            val items = remember(selectedCategory, selectedSubCategory) {
                                when (selectedCategory) {
                                    "Monety" -> HeroLogic.itemRodzajMonety.toList()
                                    "Pojemniki" -> when (selectedSubCategory) {
                                        "Sakiewki" -> HeroLogic.itemPojemniki.filter { it.contains("sakiewka") }
                                        "Torby i Plecaki" -> HeroLogic.itemPojemniki.filter { !it.contains("sakiewka") }
                                        else -> emptyList()
                                    }
                                    "Broń ręczna" -> when (selectedSubCategory) {
                                        "Broń prosta" -> HeroLogic.WW0nazwa.toList()
                                        "Sieczna" -> HeroLogic.WW1nazwa.toList()
                                        "Obuchowa" -> HeroLogic.WW2nazwa.toList()
                                        "Kłuta" -> HeroLogic.WW3nazwa.toList()
                                        "Broń Egzotyczna" -> HeroLogic.WW4nazwa.toList()
                                        else -> emptyList()
                                    }
                                    "Broń strzelecka" -> when (selectedSubCategory) {
                                        "Broń miotana" -> HeroLogic.US0nazwa.toList()
                                        "Pistolet strzałkowy" -> HeroLogic.US1nazwa.toList()
                                        "Łuk" -> HeroLogic.US2nazwa.toList()
                                        "Kusza" -> HeroLogic.US3nazwa.toList()
                                        "Prochowa" -> HeroLogic.US4nazwa.toList()
                                        "Futurystyczna" -> HeroLogic.US5nazwa.toList()
                                        else -> emptyList()
                                    }
                                    "Pociski" -> when (selectedSubCategory) {
                                        "Do łuków" -> HeroLogic.AM0nazwa.toList()
                                        "Do kusz" -> HeroLogic.AM1nazwa.toList()
                                        "Do broni palnej" -> HeroLogic.AM2nazwa.toList()
                                        "Inne" -> HeroLogic.AM3nazwa.toList()
                                        else -> emptyList()
                                    }
                                    "Pancerz" -> when (selectedSubCategory) {
                                        "Głowa" -> HeroLogic.pancGłowaNazwa.toList()
                                        "Korpus" -> HeroLogic.pancKorpusNazwa.toList()
                                        "Ręce" -> HeroLogic.pancRękaNazwa.toList()
                                        "Nogi" -> HeroLogic.pancNogaNazwa.toList()
                                        else -> emptyList()
                                    }
                                    "Odzienie" -> when (selectedSubCategory) {
                                        "Głowa" -> HeroLogic.ubranieG.toList()
                                        "Korpus" -> HeroLogic.ubranieK.toList()
                                        "Ręce" -> HeroLogic.ubranieR.toList()
                                        "Nogi" -> HeroLogic.ubranieN.toList()
                                        "Korpus+Nogi" -> HeroLogic.ubranieKN.toList()
                                        "Uniform" -> HeroLogic.uniform.toList()
                                        else -> emptyList()
                                    }

                                    "Instrument" -> when (selectedSubCategory) {
                                        "Strunowe" -> HeroLogic.instrumentsStrunowe.map { it.name }
                                        "Dęte" -> HeroLogic.instrumentsDete.map { it.name }
                                        "Perkusja" -> HeroLogic.instrumentsPerkusja.map { it.name }
                                        else -> HeroLogic.instrumentsSpecialist.map { it.name }
                                    }
                                    "Narzędzie" -> (HeroLogic.narzedziaAll.toList() + HeroLogic.toolsSpecialist.map { it.name })
                                    "Majątek" -> HeroLogic.bogactwaAll.toList()
                                    "Zasoby" -> when (selectedSubCategory) {
                                        "Zioła" -> listOf("Mięta pieprzowa", "Szałwia lekarska", "Pokrzywa zwyczajna", "Mniszek pospolity", "Rumianek polny", "Kora wierzby", "Krwawnik", "Dziki len", "Kwiat czarnego bzu", "Lawenda", "Pokrzyk srebrzysty", "Wilcza jagoda", "Tojad jadowity", "Arnika górska", "Korzeń żeń-szenia", "Mandragora", "Kwiat paproci", "Srebrny lotos")
                                        "Minerały" -> listOf("Ruda żelaza", "Ruda miedzi", "Ruda cyny", "Węgiel brunatny", "Kwarc dymny", "Krzemień", "Piryt", "Sól kamienna", "Grudka gliny", "Czysta siarka", "Ruda srebra", "Malachit", "Obsydian", "Fluoryt fluorowy", "Nefryt", "Ruda złota", "Mithrill", "Gromrill")
                                        "Łowiectwo" -> listOf(    "Jarząbek",
                                            "Kuropatwa",
                                            "Bażant",
                                            "Kaczka krzyżówka",
                                            "Zając",
                                            "Królik dziki",
                                            "Borsuk",
                                            "Lis",
                                            "Sarna",
                                            "Kozica",
                                            "Muflon",
                                            "Dzik",
                                            "Jeleń",
                                            "Łoś",
                                            "Żubr",
                                            "Wilk",
                                            "Niedźwiedź",
                                            "Pantera",
                                            "Błękitny Lis Polarny",
                                            "Barghest Bagienny",
                                            "Gryf",
                                            "Jeżozwierz Olbrzymi",
                                            "Hydra")
                                        "Rybołówstwo" -> listOf("Głębinowy Świetlik",
                                            "Szczupak",
                                            "Pstrąg",
                                            "Łosoś",
                                            "Karp",
                                            "Okoń",
                                            "Sum",
                                            "Węgorz",
                                            "Karaś",
                                            "Sandacz",
                                            "Jesiotr",
                                            "Dorsz",
                                            "Flądra",
                                            "Lin",
                                            "Brzana",
                                            "Płóć",
                                            "Kolczasty Sum Bagienny",
                                            "Księżycowy Okoń",
                                            "Mglisty Karp",
                                            "Żarłacz Pustynny",
                                            "Błękitna Ryba Nefrytowa",
                                            "Gwiezdny Jesiotr")
                                        "Grzybobranie" -> listOf(        "Borowik szlachetny",
                                            "Podgrzybek brunatny",
                                            "Pieprznik jadalny (Kurka)",
                                            "Koźlarz babka",
                                            "Maślak zwyczajny",
                                            "Rydz mleczaj",
                                            "Smardz jadalny",
                                            "Lejkowiec dęty",
                                            "Sarniak świerkowy",
                                            "Czubajka kania",
                                            "Trufla czarnozarodnikowa",
                                            "Gąska sosnowa",
                                            "Żagwica listkowata",
                                            "Trufla biała",
                                            "Soplówka jeżowata")
                                        else -> emptyList()
                                    }
                                    "Zwierzęta" -> (HeroLogic.zwierzętaHodowlaneSsaki.toList() + HeroLogic.zwierzętaDzikieSsaki.toList() + HeroLogic.zwierzętaHodowlanePtaki.toList())
                                    "Biżuteria" -> HeroLogic.bizuteriaNazwa.toList()
                                    "Mikstura" -> listOf("Mikstura Lecznicza", "Mikstura Magiczna", "Antidotum", "Eliksir Siły", "Mikstura Szybkości")
                                    "Księga" -> HeroLogic.skillWiedza.filter { it != "Technologia" }.map { "Księga ($it)" }
                                    else -> emptyList()
                                }
                            }

                            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    IconButton(
                                        onClick = { 
                                            playSoundAction(R.raw.klik1, 0)
                                            onMenuAppChange(10)
                                        },
                                        modifier = Modifier.align(Alignment.CenterStart)
                                    ) {
                                        Text("⬅️", fontSize = 24.sp)
                                    }
                                    Text(
                                        "EKWIPUNEK",
                                        color = Color.Yellow,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = nosiferFamily
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))

//// Widoczność chipsów ekwipunku

                                val lines = heroData.lines()
                                val poziomLine = lines.find { it.startsWith("POZIOM:") } ?: "POZIOM: 0 ｢0｣ GRA"
                                val poziomStartowy = poziomLine.substringAfter("｢").substringBefore("｣").trim().toIntOrNull() ?: 0

                                if (poziomStartowy > 0) {
                                // Search / Custom Item Field
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    placeholder = { Text("Szukaj lub wpisz nową nazwę...", color = Color.Gray, fontSize = 12.sp) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = {
                                                playSoundAction(R.raw.klik1, 0)
                                                val lines = heroData.lines().toMutableList()
                                                var ekwIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
                                                if (ekwIdx == -1) {
                                                    lines.add("EKWIPUNEK:")
                                                    ekwIdx = lines.size - 1
                                                }
                                                lines.add(ekwIdx + 1, "   • $searchQuery")
                                                val newContent = lines.joinToString("\n")
                                                saveToFile(currentFile, newContent)
                                                heroData = newContent
                                                searchQuery = ""
                                                playSoundAction(R.raw.page, 1)
                                            }) {
                                                Text("➕", fontSize = 18.sp)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Yellow,
                                        unfocusedBorderColor = Color.DarkGray
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Category Selection
                                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(HeroLogic.itemKategoria.toList()) { cat ->
                                        FilterChip(
                                            selected = selectedCategory == cat,
                                            onClick = { 
                                                selectedCategory = if (selectedCategory == cat) null else cat
                                                selectedSubCategory = null
                                                expandedItemIndex = -1
                                            },
                                            label = { Text(cat) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color.Yellow,
                                                selectedLabelColor = Color.Black,
                                                containerColor = Color.Gray,
                                                labelColor = Color.White
                                            )
                                        )
                                    }
                                }
                                }   //  poziomStartowy > 0
                                else {

                                    // Category Selection
                                    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(HeroLogic.itemKategoria.toList()) { cat ->
                                            FilterChip(
                                                selected = selectedCategory == cat,
                                                onClick = {
                                                    selectedCategory = if (selectedCategory == cat) null else cat
                                                    selectedSubCategory = null
                                                    expandedItemIndex = -1
                                                },
                                                label = { Text(cat) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color.Yellow,
                                                    selectedLabelColor = Color.Black,
                                                    containerColor = Color.Gray,
                                                    labelColor = Color.White
                                                )
                                            )
                                        }
                                    }

                                }
////

                                if (selectedCategory != null) {
                                    val subCats = when (selectedCategory) {
                                        "Broń ręczna" -> HeroLogic.itemRodzajBR.toList()
                                        "Broń strzelecka" -> HeroLogic.itemRodzajBS.toList()
                                        "Pociski" -> HeroLogic.itemRodzajPociski.toList()
                                        "Pancerz" -> listOf("Głowa", "Korpus", "Ręce", "Nogi")
                                        "Odzienie" -> HeroLogic.itemRodzajUbranie.toList()
                                        "Pojemniki" -> listOf("Sakiewki", "Torby i Plecaki")
                                        "Instrument" -> listOf("Strunowe", "Dęte", "Perkusja")
                                        else -> emptyList()
                                    }

                                    if (subCats.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(subCats) { sub ->
                                                FilterChip(
                                                    selected = selectedSubCategory == sub,
                                                    onClick = { 
                                                        selectedSubCategory = if (selectedSubCategory == sub) null else sub
                                                        expandedItemIndex = -1
                                                    },
                                                    label = { Text(sub) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = Color.Yellow,
                                                        selectedLabelColor = Color.Black,
                                                        containerColor = Color.Gray,
                                                        labelColor = Color.White
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    // --- OBBCIĄŻENIE (Encumbrance) ---
                                    item {
                                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "OBCIĄŻENIE: ${encLevel.label}",
                                                    color = encLevel.color,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = slaboFamily
                                                )
                                                Text(
                                                    "${String.format("%.1f", currentWeight / 10.0)} / ${String.format("%.1f", maxLoad / 10.0)} kg",
                                                    color = if (currentWeight > maxLoad) Color.Red else Color.White,
                                                    fontSize = 14.sp,
                                                    fontFamily = slaboFamily
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Progress bar for encumbrance
                                            val progress = (currentWeight.toFloat() / maxLoad.toFloat()).coerceIn(0f, 1.2f)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(12.dp)
                                                    .background(Color.DarkGray, RoundedCornerShape(6.dp))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(progress.coerceAtMost(1f))
                                                        .fillMaxHeight()
                                                        .background(
                                                            if (progress > 1f) Color.Red else encLevel.color,
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                )
                                            }

                                            if (encLevel.penalties.isNotEmpty()) {
                                                Text(
                                                    "Kary: ${encLevel.penalties}",
                                                    color = Color.LightGray,
                                                    fontSize = 10.sp,
                                                    fontStyle = FontStyle.Italic,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                        Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                                    }

                                    // --- POSIADANE (Owned) ---
                                    val itemsToShow = if (selectedCategory == null) allOwnedItems else allOwnedItems.filter { getItemCategory(it) == selectedCategory }
                                    
                                    if (itemsToShow.isNotEmpty()) {
                                        item {
                                            Text("POSIADANE:", color = Color.Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                        items(itemsToShow) { owned ->
                                            val category = getItemCategory(owned)
                                            val isWeapon = category == "Broń ręczna" || category == "Broń strzelecka"
                                            
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (owned.startsWith("RESOURCE|")) {
                                                            val parts = owned.split("|")
                                                            val resCat = parts[1]
                                                            val rawData = parts[2]
                                                            
                                                            val name = rawData.substringBefore(" (").trim()
                                                            val weight = rawData.substringAfter("(").substringBefore(" kg").trim()
                                                            val quality = rawData.substringAfterLast("(").substringBefore(")").trim()
                                                            
                                                            val known = when(resCat) {
                                                                "Zioła" -> heroData.contains("Zielarstwo") || heroData.contains("Wiedza o roślinach")
                                                                "Minerały" -> heroData.contains("Metalurgia")
                                                                "Łowiectwo" -> heroData.contains("Łowiectwo") || heroData.contains("Wiedza o zwierzętach: Ssaki")
                                                                "Rybołówstwo" -> heroData.contains("Wiedza o zwierzętach: Ryby") || heroData.contains("Wiedza o rzekach")
                                                                else -> false
                                                            }
                                                            
                                                            val displayName = if (known) name else when(resCat) {
                                                                "Zioła" -> "Nieznana roślina"
                                                                "Minerały" -> "Nieznana ruda"
                                                                "Łowiectwo" -> "Nieznane trofeum"
                                                                "Rybołówstwo" -> "Nieznana ryba"
                                                                else -> "Nieznany zasób"
                                                            }
                                                            
                                                            val stars = if (known) {
                                                                when(quality) {
                                                                    "Podstawowe" -> " ✩"
                                                                    "Rzadkie" -> " ✩✩"
                                                                    "Unikalne" -> " ✩✩✩"
                                                                    else -> ""
                                                                }
                                                            } else ""
                                                            
                                                            val icon = when(resCat) {
                                                                "Zioła" -> "🌿 "
                                                                "Minerały" -> "⛏️ "
                                                                "Łowiectwo" -> "🏹 "
                                                                "Rybołówstwo" -> "🐟 "
                                                                else -> "📦 "
                                                            }
                                                            
                                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                                Text(icon, fontSize = 14.sp)
                                                                Text("$displayName$stars", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                                Spacer(modifier = Modifier.weight(1f))
                                                                Text("$weight kg", color = Color.Gray, fontSize = 12.sp)
                                                                
                                                                IconButton(onClick = {
                                                                    playSoundAction(R.raw.klik1, 0)
                                                                    val lines = heroData.lines().toMutableList()
                                                                    val zbIdx = lines.indexOfFirst { it.startsWith("ZBIORY:") }
                                                                    if (zbIdx != -1) {
                                                                        // Szukamy nagłówka kategorii [Zioła] itp.
                                                                        val catHeader = "[$resCat]"
                                                                        var currentCatIdx = -1
                                                                        for (i in zbIdx + 1 until lines.size) {
                                                                            if (lines[i].trim() == catHeader) {
                                                                                currentCatIdx = i
                                                                                break
                                                                            }
                                                                            if (lines[i].isNotEmpty() && !lines[i].startsWith(" ")) break
                                                                        }
                                                                        
                                                                        if (currentCatIdx != -1) {
                                                                            // Szukamy dokładnie tej linijki z surowcem pod kategorią
                                                                            val searchLine = "• $rawData"
                                                                            for (j in currentCatIdx + 1 until lines.size) {
                                                                                if (lines[j].trim() == searchLine) {
                                                                                    lines.removeAt(j)
                                                                                    
                                                                                    // Opcjonalnie: usuń nagłówek kategorii jeśli pusta
                                                                                    if (j < lines.size && (lines[j].isEmpty() || !lines[j].trim().startsWith("•"))) {
                                                                                         if (lines[j-1].trim().startsWith("[")) {
                                                                                             // lines.removeAt(j-1) // Zostawiamy nagłówki dla porządku
                                                                                         }
                                                                                    }
                                                                                    
                                                                                    val newContent = lines.joinToString("\n")
                                                                                    saveToFile(currentFile, newContent)
                                                                                    heroData = newContent
                                                                                    break
                                                                                }
                                                                                if (lines[j].trim().startsWith("[") || (lines[j].isNotEmpty() && !lines[j].startsWith(" "))) break
                                                                            }
                                                                        }
                                                                    }
                                                                }, modifier = Modifier.size(32.dp)) {
                                                                    Text("🗑️", fontSize = 16.sp)
                                                                }
                                                            }
                                                        } else if (owned.startsWith("⛀")) {
                                                            val coinColor = when {
                                                                owned.contains("Złota") -> Color(0xFFFFD700)
                                                                owned.contains("Srebrny") -> Color(0xFFC0C0C0)
                                                                owned.contains("Miedziany") -> Color(0xFFCD7F32)
                                                                else -> Color.Yellow
                                                            }
                                                            Row(modifier = Modifier.weight(1f)) {
                                                                Text(owned.take(1), color = coinColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                                Text(owned.drop(1).trim(), color = Color.White, fontSize = 14.sp)
                                                            }
                                                        } else {
                                                            val qualityMatch = Regex("\\[Jakość: (\\d+)%\\]").find(owned)
                                                            val qualityValue = qualityMatch?.groupValues?.get(1)?.toIntOrNull()

                                                            if (qualityValue != null) {
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(owned.replace(Regex("\\[Jakość: \\d+%\\]"), "").trim(), color = Color.White, fontSize = 14.sp)
                                                                    Spacer(modifier = Modifier.height(4.dp))
                                                                    LinearProgressIndicator(
                                                                        progress = { qualityValue / 100f },
                                                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                                                        color = getQualityColor(qualityValue),
                                                                        trackColor = Color.DarkGray
                                                                    )
                                                                }
                                                            } else {
                                                                Text(owned, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                                            }
                                                        }
                                                        
                                                        // [E] button for equippable
                                                        if (!owned.startsWith("⛀") && category in listOf("Broń ręczna", "Broń strzelecka", "Pancerz", "Odzienie")) {
                                                            IconButton(onClick = {
                                                                playSoundAction(R.raw.klik1, 0)
                                                                val lines = heroData.lines().toMutableList()
                                                                val idx = lines.indexOfFirst { it.contains(owned) && it.startsWith("   •") }
                                                                if (idx != -1) {
                                                                    if (lines[idx].contains("🅔")) lines[idx] = lines[idx].replace(" 🅔", "")
                                                                    else lines[idx] = lines[idx] + " 🅔"
                                                                    val newContent = lines.joinToString("\n")
                                                                    saveToFile(currentFile, newContent)
                                                                    heroData = newContent
                                                                }
                                                            }, modifier = Modifier.size(32.dp)) {
                                                                Text(if (owned.contains("🅔")) "✅" else "🛡️", fontSize = 16.sp)
                                                            }
                                                        }
/////
                                                        if (wybranyPoziom > -1) {
                                                            if (owned.startsWith("⛀") || category == "Pociski" || category == "Mikstura" || category == "Majątek") {

                                                                IconButton(
                                                                    onClick = {
                                                                        playSoundAction(
                                                                            R.raw.klik1,
                                                                            0
                                                                        )
                                                                        val lines = heroData.lines()
                                                                            .toMutableList()
                                                                        val idx =
                                                                            lines.indexOfFirst {
                                                                                it.contains(owned) && it.startsWith(
                                                                                    "   •"
                                                                                )
                                                                            }
                                                                        if (idx != -1) {
                                                                            val parts =
                                                                                lines[idx].trim()
                                                                                    .split("  ")
                                                                            val currentVal =
                                                                                parts.last()
                                                                                    .toIntOrNull()
                                                                                    ?: 1
                                                                            val step =
                                                                                if (category == "Pociski") 12 else 1
                                                                            if (currentVal > step) {
                                                                                val base =
                                                                                    if (parts.size > 1) lines[idx].substringBeforeLast(
                                                                                        "  "
                                                                                    ) else lines[idx]
                                                                                lines[idx] =
                                                                                    "$base  ${currentVal - step}"
                                                                            } else lines.removeAt(
                                                                                idx
                                                                            )
                                                                            val newContent =
                                                                                lines.joinToString("\n")
                                                                            saveToFile(
                                                                                currentFile,
                                                                                newContent
                                                                            )
                                                                            heroData = newContent
                                                                        }
                                                                    },
                                                                    modifier = Modifier.size(32.dp)
                                                                ) {
                                                                    Text(
                                                                        "➖",
                                                                        fontSize = 12.sp,
                                                                        color = Color.Red
                                                                    )
                                                                }

                                                                IconButton(
                                                                    onClick = {
                                                                        playSoundAction(
                                                                            R.raw.klik1,
                                                                            0
                                                                        )
                                                                        val lines = heroData.lines()
                                                                            .toMutableList()
                                                                        val idx =
                                                                            lines.indexOfFirst {
                                                                                it.contains(owned) && it.startsWith(
                                                                                    "   •"
                                                                                )
                                                                            }
                                                                        if (idx != -1) {
                                                                            val parts =
                                                                                lines[idx].trim()
                                                                                    .split("  ")
                                                                            val currentVal =
                                                                                parts.last()
                                                                                    .toIntOrNull()
                                                                                    ?: 1
                                                                            val base =
                                                                                if (parts.size > 1) lines[idx].substringBeforeLast(
                                                                                    "  "
                                                                                ) else lines[idx]
                                                                            val step =
                                                                                if (category == "Pociski") 12 else 1
                                                                            lines[idx] =
                                                                                "$base  ${currentVal + step}"
                                                                            val newContent =
                                                                                lines.joinToString("\n")
                                                                            saveToFile(
                                                                                currentFile,
                                                                                newContent
                                                                            )
                                                                            heroData = newContent
                                                                        }
                                                                    },
                                                                    modifier = Modifier.size(32.dp)
                                                                ) {
                                                                    Text(
                                                                        "➕",
                                                                        fontSize = 12.sp,
                                                                        color = Color.Green
                                                                    )
                                                                }


                                                            }
                                                        }
                                                        /*
                                                        IconButton(onClick = {
                                                            playSoundAction(R.raw.klik1, 0)
                                                            val lines = heroData.lines().toMutableList()
                                                            val idx = lines.indexOfFirst { it.contains(owned) && it.startsWith("   •") }
                                                            if (idx != -1) {
                                                                lines.removeAt(idx)
                                                                val newContent = lines.joinToString("\n")
                                                                saveToFile(currentFile, newContent)
                                                                heroData = newContent
                                                            }
                                                        }, modifier = Modifier.size(32.dp)) {
                                                            Text("🗑️", fontSize = 16.sp)
                                                        }
                                                         */

                                                    }
/////

                                                    // Weapon Details
                                                    if (isWeapon) {
                                                        val weaponName = owned.replace(" 🅔", "").trim()
                                                        var detailsText = ""
                                                        if (category == "Broń ręczna") {
                                                            val wwNazwy = listOf(HeroLogic.WW0nazwa, HeroLogic.WW1nazwa, HeroLogic.WW2nazwa, HeroLogic.WW3nazwa, HeroLogic.WW4nazwa)
                                                            val wwKosci = listOf(HeroLogic.WW0kX, HeroLogic.WW1kX, HeroLogic.WW2kX, HeroLogic.WW3kX, HeroLogic.WW4kX)
                                                            val wwRozmiary = listOf(HeroLogic.WW0rozmiar, HeroLogic.WW1rozmiar, HeroLogic.WW2rozmiar, HeroLogic.WW3rozmiar, HeroLogic.WW4rozmiar)
                                                            
                                                            for (i in wwNazwy.indices) {
                                                                val idx = wwNazwy[i].indexOfFirst { weaponName.contains(it) }
                                                                if (idx != -1) {
                                                                    val kX = wwKosci[i][idx]
                                                                    val size = wwRozmiary[i][idx]
                                                                    val type = when(i) {
                                                                        0 -> "Prosta"
                                                                        1 -> "Sieczna"
                                                                        2 -> "Obuchowa"
                                                                        3 -> "Kłuta"
                                                                        4 -> "Egzotyczna (S)"
                                                                        else -> "Sieczna"
                                                                    }
                                                                    detailsText = "Typ: $type | Kość: k$kX | Rozmiar: $size"
                                                                    break
                                                                }
                                                            }
                                                        } else {
                                                            val usNazwy = listOf(HeroLogic.US0nazwa, HeroLogic.US1nazwa, HeroLogic.US2nazwa, HeroLogic.US3nazwa, HeroLogic.US4nazwa, HeroLogic.US5nazwa)
                                                            val usKosci = listOf(HeroLogic.US0kX, HeroLogic.US1kX, HeroLogic.US2kX, HeroLogic.US3kX, HeroLogic.US4kX, HeroLogic.US5kX)
                                                            val usRanges = listOf(HeroLogic.US0range, HeroLogic.US1range, HeroLogic.US2range, HeroLogic.US3range, HeroLogic.US4range, HeroLogic.US5range)
                                                            
                                                            for (i in usNazwy.indices) {
                                                                val idx = usNazwy[i].indexOfFirst { weaponName.contains(it) }
                                                                if (idx != -1) {
                                                                    val kX = usKosci[i][idx]
                                                                    val ran = usRanges[i][idx]
                                                                    detailsText = "Zasięg: $ran m | Kość: k$kX"
                                                                    
                                                                    // Ammo tracking
                                                                    val isBow = weaponName.contains("Łuk", ignoreCase = true)
                                                                    val isCrossbow = weaponName.contains("Kusza", ignoreCase = true)
                                                                    val isFirearm = i == 4 || i == 5
                                                                    
                                                                    val ammoType = when {
                                                                        isBow -> "Strzały"
                                                                        isCrossbow -> "Bełty"
                                                                        isFirearm -> "Amunicja"
                                                                        else -> null
                                                                    }
                                                                    
                                                                    if (ammoType != null) {
                                                                        val ammoLine = allOwnedItems.find { it.contains(ammoType, ignoreCase = true) }
                                                                        val ammoCount = ammoLine?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
                                                                        detailsText += " | $ammoType: $ammoCount"
                                                                    }
                                                                    break
                                                                }
                                                            }
                                                        }
                                                        if (detailsText.isNotEmpty()) {
                                                            Text(detailsText, color = Color.Gray, fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // --- DOSTĘPNE (Available from DB) ---
                                    if (selectedCategory != null) {
                                        item {
                                            Text("DOSTĘPNE:", color = Color.Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                                        }
                                        val filteredItems = if (searchQuery.isEmpty()) items else items.filter { it.contains(searchQuery, ignoreCase = true) }
                                        itemsIndexed(filteredItems) { index, item ->
                                            val isExpanded = expandedItemIndex == index
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                                    expandedItemIndex = if (isExpanded) -1 else index
                                                    selectedPrefix = "Brak"
                                                    selectedSuffix = "Brak"
                                                },
                                                colors = CardDefaults.cardColors(containerColor = if (isExpanded) Color(0xFF333333) else Color(0xFF222222))
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(item, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                        if (isExpanded) {
                                                            IconButton(onClick = {
                                                                playSoundAction(R.raw.klik1, 0)
                                                                val lines = heroData.lines().toMutableList()
                                                                var ekwIdx = lines.indexOfFirst { it.startsWith("EKWIPUNEK:") }
                                                                if (ekwIdx == -1) {
                                                                    lines.add("EKWIPUNEK:")
                                                                    ekwIdx = lines.size - 1
                                                                }
                                                                
                                                                val isBrak = item.trim() == "𝓑𝓻𝓪𝓴" || item.trim().equals("Brak", ignoreCase = true)
                                                                if (isBrak) {
                                                                    val itemsToRemove = when (selectedSubCategory) {
                                                                        "Głowa" -> HeroLogic.pancGłowaNazwa.toList() + HeroLogic.ubranieG.toList()
                                                                        "Korpus" -> HeroLogic.pancKorpusNazwa.toList() + HeroLogic.ubranieK.toList() + HeroLogic.ubranieKN.toList() + HeroLogic.uniform.toList()
                                                                        "Ręce" -> HeroLogic.pancRękaNazwa.toList() + HeroLogic.ubranieR.toList()
                                                                        "Nogi" -> HeroLogic.pancNogaNazwa.toList() + HeroLogic.ubranieN.toList()
                                                                        else -> emptyList()
                                                                    }
                                                                    lines.removeAll { line -> line.startsWith("   •") && itemsToRemove.any { line.contains(it) } }
                                                                } else if (selectedCategory == "Monety" || selectedCategory == "Pociski") {
                                                                    val isCoin = selectedCategory == "Monety"
                                                                    val step = if (isCoin) 1 else 12
                                                                    val prefix = if (isCoin) "⛀ " else ""
                                                                    val curLineIdx = lines.indexOfFirst { it.contains("   • $prefix$item  ") }
                                                                    if (curLineIdx != -1) {
                                                                        val curVal = lines[curLineIdx].trim().split("  ").last().toIntOrNull() ?: 0
                                                                        val base = lines[curLineIdx].substringBeforeLast("  ")
                                                                        lines[curLineIdx] = "$base  ${curVal + step}"
                                                                    } else {
                                                                        lines.add(ekwIdx + 1, "   • $prefix$item  $step")
                                                                    }
                                                                } else {
                                        // Sprawdzanie limitu pojemników dla Siły <= 4 (osobna zasada logistyczna)
                                        var canAdd = true
                                        val krzepa = HeroLogic.getHeroStrength(heroData)
                                        val sila = krzepa / 10
                                        
                                        if (selectedCategory == "Pojemniki" && sila <= 4) {
                                            val ownedContainersCount = allOwnedItems.count { getItemCategory(it) == "Pojemniki" }
                                            if (ownedContainersCount >= 1) {
                                                canAdd = false
                                            }
                                        }

                                        if (canAdd) {
                                            val prefixStr = if (selectedPrefix != "Brak") "$selectedPrefix " else ""
                                            val suffixStr = if (selectedSuffix != "Brak") " $selectedSuffix" else ""
                                            val finalName = "$prefixStr$item$suffixStr"
                                            lines.add(ekwIdx + 1, "   • $finalName")
                                        } else {
                                            // Możesz dodać Toast lub inny sygnał dla gracza
                                            Toast.makeText(context, "Zbyt niska Siła, by nieść więcej pojemników!", Toast.LENGTH_SHORT).show()
                                        }
                                                                }
                                                                
                                                                val newContent = lines.joinToString("\n")
                                                                saveToFile(currentFile, newContent)
                                                                heroData = newContent
                                                                playSoundAction(R.raw.page, 1)
                                                            }) {
                                                                Text(if (item.trim() == "𝓑𝓻𝓪𝓴" || item.trim().equals("Brak", ignoreCase = true)) "❌" else "➕", fontSize = 18.sp)
                                                            }
                                                        }
                                                    }

                                                    if (isExpanded && selectedCategory != "Monety" && item.trim() != "𝓑𝓻𝓪𝓴") {
                                                        val availablePrefixes = when (selectedCategory) {
                                                            "Pancerz" -> HeroLogic.itemPrefixesArmor
                                                            "Broń ręczna", "Broń strzelecka" -> HeroLogic.itemPrefixesWeapon
                                                            "Biżuteria" -> HeroLogic.itemPrefixesJewelry
                                                            else -> arrayOf("Brak")
                                                        }
                                                        val availableSuffixes = when (selectedCategory) {
                                                            "Pancerz" -> HeroLogic.itemSuffixesArmor
                                                            "Broń ręczna", "Broń strzelecka", "Biżuteria" -> HeroLogic.itemSuffixesWeaponJewelry
                                                            else -> arrayOf("Brak")
                                                        }

                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        
                                                        Text("Prefiks:", color = Color.Gray, fontSize = 12.sp)
                                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            items(availablePrefixes) { prefix ->
                                                                FilterChip(
                                                                    selected = selectedPrefix == prefix,
                                                                    onClick = { selectedPrefix = prefix },
                                                                    label = { Text(prefix, fontSize = 10.sp) },
                                                                    colors = FilterChipDefaults.filterChipColors(
                                                                        selectedContainerColor = Color.Yellow,
                                                                        containerColor = Color.DarkGray
                                                                    )
                                                                )
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("Sufiks:", color = Color.Gray, fontSize = 12.sp)
                                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            items(availableSuffixes) { suffix ->
                                                                FilterChip(
                                                                    selected = selectedSuffix == suffix,
                                                                    onClick = { selectedSuffix = suffix },
                                                                    label = { Text(suffix, fontSize = 10.sp) },
                                                                    colors = FilterChipDefaults.filterChipColors(
                                                                        selectedContainerColor = Color.Green,
                                                                        containerColor = Color.DarkGray
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }


                        11 -> {
                            // Manual Entry - Screen 1: Info & Primary Stats
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("INFO I CECHY GŁÓWNE", color = Color.Cyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Basic Info (Name and Gender)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = heroName,
                                        onValueChange = { heroName = it },
                                        label = { Text("Imię", color = Color.Gray) },
                                        modifier = Modifier.weight(0.75f),
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontFamily = lobsterFamily
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(
                                        modifier = Modifier.weight(0.25f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Button(
                                            onClick = { wybranaPlecIndex = if (wybranaPlecIndex == 0) 1 else 0 },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                            modifier = Modifier.size(50.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(if (wybranaPlecIndex == 0) "♂️" else "♀️", fontSize = 24.sp)
                                        }
                                    }
                                }

                                Column(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)) {
                                    LazyRow(modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)) {
                                        itemsIndexed(MainActivity.listaRasa) { index, r ->
                                            val isSelected = wybranaRasaIndex == index
                                            SuggestionChip(
                                                onClick = { wybranaRasaIndex = index },
                                                label = { Text(r, fontSize = 14.sp) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = if (isSelected) Color.Yellow else Color.DarkGray,
                                                    labelColor = if (isSelected) Color.Black else Color.White
                                                ),
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text("POZIOM POSTACI: $wybranyPoziom", color = Color.Yellow, fontWeight = FontWeight.Bold)
                                Slider(
                                    value = wybranyPoziom.toFloat(),
                                    onValueChange = { wybranyPoziom = it.toInt() },
                                    valueRange = 1f..9f,
                                    steps = 7,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Cechy Główne (Aktualne)", color = Color.Yellow, fontWeight = FontWeight.Bold)

                                Button(
                                    onClick = {
                                        val newStats = wygenerowaneStaty?.toMutableList() ?: mutableListOf()
                                        // 0-7: 20..45
                                        for (i in 0..7) {
                                            val roll = (20..45).random()
                                            newStats[i] = Triple(newStats[i].first, intArrayOf(0, roll, 0, 0), roll)
                                        }
                                        wygenerowaneStaty = newStats
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.Yellow)
                                ) {
                                    Text("WYLOSUJ WSZYSTKIE", fontWeight = FontWeight.Bold)
                                }

                                val primaryStatFullNames = listOf("Walka Wręcz", "US", "Krzepa", "Odporność", "Zręczność", "Inteligencja", "Siła Woli", "Charyzma")
                                
                                wygenerowaneStaty?.take(8)?.forEachIndexed { index, stat ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(primaryStatFullNames.getOrNull(index) ?: stat.first, color = Color.White, modifier = Modifier.width(130.dp), maxLines = 1)
                                        Slider(
                                            value = stat.second[1].toFloat() + stat.second[2].toFloat(),
                                            onValueChange = { newValue ->
                                                val currentVal = newValue.toInt()
                                                val newStats = wygenerowaneStaty?.toMutableList() ?: mutableListOf()
                                                // index 1 to baza, 2 to rozwinięcia. Dla prostoty manualnego wpisu wrzucamy wszystko w bazę (1)
                                                // WAŻNE: Aktualizujemy też stat.third (indeks 2 w Triple), bo on jest wyświetlany w CharacterSummary
                                                newStats[index] = Triple(stat.first, intArrayOf(0, currentVal, 0, 0), currentVal)
                                                wygenerowaneStaty = newStats
                                            },
                                            valueRange = 0f..100f,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text("${stat.second[1] + stat.second[2]}", color = Color.Cyan, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                                    }
                                }
                                
                                // Dodatkowe pole dla Magii (Mag), usunięte z Cech Głównych, bo jest w Drugorzędnych
                                /*
                                if ((wygenerowaneStaty?.size ?: 0) > 8) {
                                    val stat = wygenerowaneStaty!![8]
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Text("Magia (Mag)", color = Color.White, modifier = Modifier.width(130.dp))
                                        Slider(
                                            value = stat.second[1].toFloat() + stat.second[2].toFloat(),
                                            onValueChange = { newValue ->
                                                val currentVal = newValue.toInt()
                                                val newStats = wygenerowaneStaty?.toMutableList() ?: mutableListOf()
                                                newStats[8] = Triple("Mag", intArrayOf(0, currentVal, 0, 0), currentVal)
                                                wygenerowaneStaty = newStats
                                            },
                                            valueRange = 0f..10f,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text("${stat.second[1] + stat.second[2]}", color = Color.Cyan, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                                    }
                                }
                                */

                                Button(
                                    onClick = { onMenuAppChange(12 ) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                ) {
                                    Text("DALEJ", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        12 -> {
                            // Manual Entry - Screen 2: Secondary Stats & Physical
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("CECHY DRUGORZĘDNE I FIZYCZNE", color = Color.Cyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Cechy Drugorzędne", color = Color.Yellow, fontWeight = FontWeight.Bold)
                                
                                val krzepaVal = wygenerowaneStaty?.getOrNull(2)?.let { it.second[1] + it.second[2] } ?: 0
                                val odpornoscVal = wygenerowaneStaty?.getOrNull(3)?.let { it.second[1] + it.second[2] } ?: 0
                                val silaVal = krzepaVal / 10
                                val wytrzymaloscVal = odpornoscVal / 10

                                // Aktualizacja mapy statystyk drugorzędnych o wyliczone wartości
                                LaunchedEffect(silaVal, wytrzymaloscVal) {
                                    val newMap = statyDrugorzedne.toMutableMap()
                                    newMap["Siła"] = silaVal
                                    newMap["Wytrzymałość"] = wytrzymaloscVal
                                    statyDrugorzedne = newMap
                                }

                                Column(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)) {
                                    // Row 1: Krzepa i Siła
                                    Row(modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Krzepa: $krzepaVal", color = Color.White, modifier = Modifier.weight(1f))
                                        Text("Siła: $silaVal", color = Color.Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    }
                                    // Row 2: Odporność i Wytrzymałość
                                    Row(modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Odporność: $odpornoscVal", color = Color.White, modifier = Modifier.weight(1f))
                                        Text("Wytrzymałość: $wytrzymaloscVal", color = Color.Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    }
                                    
                                    HorizontalDivider(color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))

                                    // Pozostałe statystyki drugorzędne - suwaki
                                    val otherSecKeys = listOf("Ataki", "Żywotność", "Szybkość", "Magia", "Obłęd", "Przeznaczenie")
                                    otherSecKeys.forEach { key ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Text(key, color = Color.White, modifier = Modifier.width(120.dp))
                                            val maxVal = if (key == "Żywotność") 19f else 9f
                                            Slider(
                                                value = statyDrugorzedne[key]?.toFloat() ?: 0f,
                                                onValueChange = { newValue ->
                                                    val currentVal = newValue.toInt()
                                                    val newMap = statyDrugorzedne.toMutableMap()
                                                    newMap[key] = currentVal
                                                    statyDrugorzedne = newMap
                                                },
                                                valueRange = 0f..maxVal,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text("${statyDrugorzedne[key] ?: 0}", color = Color.Cyan, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Cechy Fizyczne", color = Color.Yellow, fontWeight = FontWeight.Bold)
                                
                                // Oczy i Włosy z list
                                val koloryOczu = listOf("Mgliste", "Szaroniebieskie", "Niebieskie", "Zielone", "Orzechowe", "Jasnobrązowe", "Ciemnobrązowe", "Szarozielone", "Jasnoszare", "Fioletowe", "Czerwone", "Granatowe", "Niebieskie Zielone", "Niebieskie Szare", "Zielone Fioletowe", "Zielone Orzechowe", "Fioletowe Niebieskie", "Fioletowe Granatowe", "Orzechowe Granatowe", "Niebieskie Granatowe", "Czerwone Szare")
                                val koloryWlosow = listOf("Łysa glaca", "Czarne", "Białe", "Jasny blond", "Blond", "Płowe", "Siwe", "Brązowe", "Rude", "Miedziane", "Czerwone", "Beżowe", "Zielone", "Fioletowe", "Granatowe", "Niebieskie", "Srebrne")

                                Column(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)) {
                                    Text("Kolor oczu", color = Color.Gray, fontSize = 12.sp)
                                    LazyRow(modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)) {
                                        items(koloryOczu) { kolor ->
                                            val isSelected = cechyFizyczne["Kolor oczu"] == kolor
                                            SuggestionChip(
                                                onClick = {
                                                    val newMap = cechyFizyczne.toMutableMap()
                                                    newMap["Kolor oczu"] = kolor
                                                    cechyFizyczne = newMap
                                                },
                                                label = { Text(kolor, fontSize = 10.sp) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = if (isSelected) Color.Yellow else Color.DarkGray,
                                                    labelColor = if (isSelected) Color.Black else Color.White
                                                ),
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                        }
                                    }

                                    Text("Kolor włosów", color = Color.Gray, fontSize = 12.sp)
                                    LazyRow(modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)) {
                                        items(koloryWlosow) { kolor ->
                                            val isSelected = cechyFizyczne["Kolor włosów"] == kolor
                                            SuggestionChip(
                                                onClick = {
                                                    val newMap = cechyFizyczne.toMutableMap()
                                                    newMap["Kolor włosów"] = kolor
                                                    cechyFizyczne = newMap
                                                },
                                                label = { Text(kolor, fontSize = 10.sp) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = if (isSelected) Color.Yellow else Color.DarkGray,
                                                    labelColor = if (isSelected) Color.Black else Color.White
                                                ),
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                        }
                                    }
                                }

                                cechyFizyczne.keys.filter { it !in listOf("Kolor oczu", "Kolor włosów", "Oczy", "Włosy") }.forEach { key ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)) {
                                        OutlinedTextField(
                                            value = cechyFizyczne[key] ?: "",
                                            onValueChange = { v ->
                                                val newMap = cechyFizyczne.toMutableMap()
                                                newMap[key] = v
                                                cechyFizyczne = newMap
                                            },
                                            label = { Text(key, color = Color.Gray) },
                                            modifier = Modifier.weight(1f),
                                            textStyle = TextStyle(color = Color.White),
                                            singleLine = true
                                        )
                                        IconButton(onClick = {
                                            playSoundAction(R.raw.klik1, 0)
                                            val newMap = cechyFizyczne.toMutableMap()
                                            newMap[key] = when(key) {
                                                "Wzrost" -> "${(140..200).random()} cm"
                                                "Waga" -> "${(45..110).random()} kg"
                                                "Wiek" -> "${(16..80).random()} lat"
                                                "Znak Gwiezdny" -> znakiZodiaku.random()
                                                else -> cechyFizyczne[key] ?: ""
                                            }
                                            cechyFizyczne = newMap
                                        }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Losuj", tint = Color.Yellow)
                                        }
                                    }
                                }

                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)) {
                                    Button(
                                        onClick = { onMenuAppChange(11 ) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray, contentColor = Color.White)
                                    ) {
                                        Text("WSTECZ")
                                    }
                                    Button(
                                        onClick = { 
                                            // 1. Zabezpieczenie przed brakiem cech fizycznych
                                            val updatedPhys = cechyFizyczne.toMutableMap()
                                            if (updatedPhys["Kolor oczu"].isNullOrBlank()) updatedPhys["Kolor oczu"] = "Brązowe"
                                            if (updatedPhys["Kolor włosów"].isNullOrBlank()) updatedPhys["Kolor włosów"] = "Brązowe"
                                            if (updatedPhys["Znak Gwiezdny"].isNullOrBlank()) updatedPhys["Znak Gwiezdny"] = znakiZodiaku.random()
                                            if (updatedPhys["Wzrost"].isNullOrBlank()) updatedPhys["Wzrost"] = "175 cm"
                                            if (updatedPhys["Waga"].isNullOrBlank()) updatedPhys["Waga"] = "75 kg"
                                            if (updatedPhys["Wiek"].isNullOrBlank()) updatedPhys["Wiek"] = "25 lat"
                                            cechyFizyczne = updatedPhys

                                            if (isGMMode) {
                                                onMenuAppChange(14)
                                            } else {
                                                // Wybieramy klasę na podstawie najwyższej statystyki lub domyślną
                                                val bestStatIdx = wygenerowaneStaty?.take(8)?.indices?.maxByOrNull { wygenerowaneStaty!![it].third } ?: 0
                                                wybranaKlasaIndex = when(bestStatIdx) {
                                                    0 -> 0 // WW -> Wojownik
                                                    4 -> 1 // Zręczność -> Łotr
                                                    1 -> 2 // Strzelectwo -> Łowca
                                                    5 -> 3 // Inteligencja -> Uczony
                                                    7 -> 4 // Charyzma -> Kapłan
                                                    6 -> 5 // Siła Woli -> Mag
                                                    else -> 0
                                                }
                                                val klasaName = MainActivity.listaKlasa[wybranaKlasaIndex]
                                                
                                                // Reset physical traits that come from random rolling to avoid double bonuses if any
                                                wylosowaneCechyCharakteru = emptyList()
                                                wylosowaneSpecjalne = emptyList()
                                                
                                                rollSkills(klasaName)
                                                onMenuAppChange(8)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                    ) {
                                        Text("GOTOWE", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        13 -> {
                            GMToolsScreen(
                                readFromFile = readFromFile,
                                saveToFile = saveToFile,
                                context = context,
                                onBack = { onMenuAppChange(1 ) },
                                nosiferFamily = nosiferFamily,
                                slaboFamily = slaboFamily,
                                playSound = playSoundAction,
                                onGMModeChange = { b: Boolean -> isGMMode = b },
                                onHeroSelected = { id: Int -> onSelectedHeroIdChange(id) },
                                onNavigate = { n: Int -> onMenuAppChange(n) },
                                onEditingChange = { b: Boolean -> isSettingsEditing = b }
                            )
                        }
                        70 -> {
                            BuildingGeneratorScreen(
                                onBack = { onMenuAppChange(13) },
                                nosiferFamily = nosiferFamily,
                                slaboFamily = slaboFamily,
                                playSound = playSoundAction,
                                saveToFile = saveToFile,
                                readFromFile = readFromFile
                            )
                        }
                        14 -> {
                            // Manual Skills Selection (GM Mode)
                            val allCategories = listOf("Walka Wręcz", "US", "Krzepa", "Odporność", "Zręczność", "Inteligencja", "Siła Woli", "Charyzma", "Wiedza", "Język")
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("WYBÓR UMIEJĘTNOŚCI", color = Color.Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(allCategories) { cat ->
                                        val isExpanded = expandedStats.contains(cat)
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = if (isExpanded) Color(0xFF1A1A1A) else Color.DarkGray),
                                            onClick = {
                                                expandedStats = if (isExpanded) expandedStats - cat else expandedStats + cat
                                                playSoundAction(R.raw.klik1, 0)
                                            }
                                        ) {
                                            Column {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(cat, color = Color.White, fontWeight = FontWeight.Bold)
                                                    Text(if (isExpanded) "▲" else "▼", color = Color.Gray)
                                                }
                                                
                                                if (isExpanded) {
                                                    val skills = getCategoryByName(cat)
                                                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                                                        skills.forEach { skill ->
                                                            val status = manualSkills[skill] ?: "Brak"
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(vertical = 2.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(skill, color = Color.LightGray, modifier = Modifier.weight(1f), fontSize = 13.sp)
                                                                
                                                                Button(
                                                                    onClick = {
                                                                        val newMap = manualSkills.toMutableMap()
                                                                        if (status == "Uczeń") {
                                                                            newMap.remove(skill)
                                                                        } else {
                                                                            newMap[skill] = "Uczeń"
                                                                        }
                                                                        manualSkills = newMap
                                                                        playSoundAction(R.raw.klik1, 0)
                                                                    },
                                                                    modifier = Modifier.size(32.dp),
                                                                    contentPadding = PaddingValues(0.dp),
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = if (status == "Uczeń") Color(0xFF005500) else Color(0xFF550000)
                                                                    )
                                                                ) {
                                                                    Text(if (status == "Uczeń") "✔️" else "❌", fontSize = 14.sp)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                         }
                                    }
                                }
                                
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    Button(
                                        onClick = { onMenuAppChange(12) },
                                        modifier = Modifier.weight(1f).padding(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                    ) { Text("WSTECZ") }
                                    
                                    Button(
                                        onClick = {
                                            // Konwersja manualSkills na format wylosowaneNormalne
                                            wylosowaneNormalne = manualSkills.map { "${it.key} (${it.value})" }
                                            
                                            // Ustawienie domyślnej klasy dla ekwipunku, jeśli nie wybrano
                                            if (wybranaKlasaIndex == -1) wybranaKlasaIndex = 0 
                                            
                                            // Zapis postaci (analogicznie do CharacterSummary)
                                            val rasa = MainActivity.listaRasa[wybranaRasaIndex]
                                            val plec = MainActivity.listaGender[wybranaPlecIndex]
                                            val wiek = listaWiek[wybranyWiekIndex]
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
                                            val klasa = dostepneKlasy.getOrNull(wybranaKlasaIndex) ?: "Brak"
                                            
                                            // Dynamiczne dobieranie ekwipunku startowego dla manuala
                                            val autoEquip = mutableSetOf<String>()
                                            if (klasa == "Mag" || klasa == "Kapłan") {
                                                autoEquip.add("Kostur")
                                            }
                                            (wylosowaneNormalne + wylosowaneSpecjalne).forEach { skill ->
                                                if (skill.contains("Specjalna broń", ignoreCase = true) || skill.startsWith("Broń:", ignoreCase = true)) {
                                                    val category = when {
                                                        skill.contains("(") -> skill.substringAfter("(").substringBefore(")").trim()
                                                        skill.contains(":") -> skill.substringAfter(":").trim()
                                                        else -> skill.replace("Specjalna broń", "").trim()
                                                    }
                                                    val randomWeapon = ItemRegistry.weapons.filter { 
                                                        it.subCategory.contains(category, ignoreCase = true) || 
                                                        it.name.contains(category, ignoreCase = true)
                                                    }.randomOrNull()?.name
                                                    if (randomWeapon != null) autoEquip.add(randomWeapon)
                                                }
                                            }
                                            val zk = if ((0..99).random() < 5) 1 else 0
                                            val s = (1..20).random()
                                            val p = 10 + (1..10).random()
                                            if (zk > 0) autoEquip.add("⛀ Złota korona (zk)  $zk")
                                            autoEquip.add("⛀ Srebrny szyling (s)  $s")
                                            autoEquip.add("⛀ Miedziany pens (p)  $p")
                                            selectedEquipment = autoEquip

                                            val statCaps = getStatCaps(klasa, wybranyPoziom)
                                            val plusMaxWW = statCaps.plusMaxWW; val plusMaxUS = statCaps.plusMaxUS
                                            val plusMaxKrz = statCaps.plusMaxKrz; val plusMaxOdp = statCaps.plusMaxOdp
                                            val plusMaxZr = statCaps.plusMaxZr; val plusMaxInt = statCaps.plusMaxInt
                                            val plusMaxSW = statCaps.plusMaxSW; val plusMaxCha = statCaps.plusMaxCha
                                            val plusMaxAtk = statCaps.plusMaxAtk; val plusMaxSz = statCaps.plusMaxSz
                                            val plusMaxZw = statCaps.plusMaxZw; val plusMaxMag = statCaps.plusMaxMag

                                            val summary = StringBuilder()
                                            summary.append("Imie: $heroName\n")
                                            summary.append("Rasa: $rasa, Klasa: $klasa\n")
                                            summary.append("Płeć: $plec, Wiek: $wiek\n\n")
                                            /*
                                            if (heroPortraitUri != null) {
                                                summary.append("Portrait: $heroPortraitUri\n")
                                            }

                                             */
                                            val levelMode = if (wybranyPoziom == 0) "" else "SESJA FABULARNA"
                                            summary.append("POZIOM: $wybranyPoziom ｢$wybranyPoziom｣ $levelMode\n".replace("  ", " "))
                                            summary.append("PD: 0\n")
                                            summary.append("MANUAL: true\n")
                                            
                                            summary.append("\nSTATYSTYKI GŁÓWNE:\n")
                                            wygenerowaneStaty?.take(8)?.forEachIndexed { idx, it ->
                                                val statName = when(idx) {
                                                    0 -> "WW"; 1 -> "US"; 2 -> "Krzepa"; 3 -> "Odporność"
                                                    4 -> "Zręczność"; 5 -> "Inteligencja"; 6 -> "Siła Woli"; 7 -> "Charyzma"
                                                    else -> it.first
                                                }
                                                val plusVal = when(idx) {
                                                    0 -> plusMaxWW; 1 -> plusMaxUS; 2 -> plusMaxKrz; 3 -> plusMaxOdp
                                                    4 -> plusMaxZr; 5 -> plusMaxInt; 6 -> plusMaxSW; 7 -> plusMaxCha
                                                    else -> 0
                                                }
                                                val currentVal = it.third + (plusVal * 5)
                                                summary.append("$statName: $currentVal | Base: ${it.third} | Plus: $plusVal\n")
                                            }
                                            
                                            summary.append("\nSTATYSTYKI DRUGORZĘDNE:\n")
                                            val orderedSecStats = listOf(
                                                "Ataki" to ((statyDrugorzedne["A"] ?: statyDrugorzedne["Ataki"] ?: 1) + plusMaxAtk),
                                                "Szybkość" to ((statyDrugorzedne["Sz"] ?: statyDrugorzedne["Szybkość"] ?: 4) + plusMaxSz),
                                                "Siła" to ((wygenerowaneStaty?.get(2)?.third ?: 0) / 10 + (plusMaxKrz * 5 / 10)),
                                                "Wytrzymałość" to ((wygenerowaneStaty?.get(3)?.third ?: 0) / 10 + (plusMaxOdp * 5 / 10)),
                                                "Żywotność" to ((statyDrugorzedne["Żyw"] ?: statyDrugorzedne["Żywotność"] ?: 10) + plusMaxZw),
                                                "Obłęd" to (statyDrugorzedne["PO"] ?: statyDrugorzedne["Obłęd"] ?: 0),
                                                "Przeznaczenie" to (statyDrugorzedne["PP"] ?: statyDrugorzedne["Przeznaczenie"] ?: 0),
                                                "Magia" to ((statyDrugorzedne["M"] ?: statyDrugorzedne["Magia"] ?: 0) + plusMaxMag)
                                            )
                                            orderedSecStats.forEach { (k, v) -> 
                                                val baseVal = when(k) {
                                                    "Ataki" -> statyDrugorzedne["A"] ?: statyDrugorzedne["Ataki"] ?: 1
                                                    "Szybkość" -> statyDrugorzedne["Sz"] ?: statyDrugorzedne["Szybkość"] ?: 4
                                                    "Siła" -> (wygenerowaneStaty?.get(2)?.third ?: 0) / 10
                                                    "Wytrzymałość" -> (wygenerowaneStaty?.get(3)?.third ?: 0) / 10
                                                    "Żywotność" -> statyDrugorzedne["Żyw"] ?: statyDrugorzedne["Żywotność"] ?: 10
                                                    "Magia" -> statyDrugorzedne["M"] ?: statyDrugorzedne["Magia"] ?: 0
                                                    else -> v
                                                }
                                                val plusVal = when(k) {
                                                    "Ataki" -> plusMaxAtk; "Szybkość" -> plusMaxSz; "Żywotność" -> plusMaxZw; "Magia" -> plusMaxMag
                                                    "Siła" -> plusMaxKrz * 5 / 10; "Wytrzymałość" -> plusMaxOdp * 5 / 10
                                                    else -> 0
                                                }
                                                if (listOf("Ataki", "Szybkość", "Siła", "Wytrzymałość", "Żywotność", "Magia").contains(k)) {
                                                    summary.append("$k: $v | Base: $baseVal | Plus: $plusVal\n")
                                                } else {
                                                    summary.append("$k: $v\n")
                                                }
                                            }
                                            
                                            summary.append("\nUMIEJĘTNOŚCI SPECJALNE:\n")
                                            wylosowaneSpecjalne.sorted().forEach { summary.append("• $it\n") }

                                            summary.append("\nUMIEJĘTNOŚCI (GRUPY):\n")
                                            val allCats = listOf(
                                                "Charyzma" to HeroLogic.skillCharyzma,
                                                "Inteligencja" to HeroLogic.skillInteligencja,
                                                "Język" to HeroLogic.skillJezyk,
                                                "Krzepa" to HeroLogic.skillKrzepa,
                                                "Odporność" to HeroLogic.skillOdpornosc,
                                                "Siła Woli" to HeroLogic.skillSW,
                                                "Umiejętności Strzeleckie" to HeroLogic.skillUS,
                                                "Walka Wręcz" to HeroLogic.skillWW,
                                                "Wiedza" to HeroLogic.skillWiedza,
                                                "Zręczność" to HeroLogic.skillZrecznosc
                                            ).sortedBy { it.first }

                                            allCats.forEach { (catName, pool) ->
                                                val inThisCat = wylosowaneNormalne.filter { s ->
                                                    val base = s.substringBefore(" (").trim()
                                                    pool.any { p ->
                                                        if (p.contains("[")) {
                                                            p.substringBefore(" [").trim() == base
                                                        } else {
                                                            p == base
                                                        }
                                                    }
                                                }.sorted()

                                                if (inThisCat.isNotEmpty()) {
                                                    summary.append("[$catName]\n")
                                                    inThisCat.forEach { summary.append("  • $it | PN: 0\n") }
                                                }
                                            }

                                            val ppValue = statyDrugorzedne["PP"] ?: statyDrugorzedne["Przeznaczenie"] ?: 0
                                            val hasSzczescie = wylosowaneNormalne.any { it.contains("Szczęście", ignoreCase = true) }
                                            val pfValue = if (hasSzczescie) ppValue * 2 else ppValue
                                            summary.append("\nPF: $pfValue\n")

                                            summary.append("\nCECHY FIZYCZNE:\n")
                                            cechyFizyczne.forEach { (k, v) -> summary.append("$k: $v\n") }
                                            summary.append("\nCECHY CHARAKTERU:\n")
                                            wylosowaneCechyCharakteru.forEach { summary.append("• $it\n") }

                                            summary.append("\nEKWIPUNEK:\n")
                                            selectedEquipment.forEach { item ->
                                                val quality = if (Math.random() < 0.25) (70..95).random() else 100
                                                val qTag = if (quality < 100) " [Jakość: $quality%]" else ""
                                                summary.append("• $item$qTag\n")
                                            }
                                            summary.append("• Podstawowy ekwipunek podróżnika\n")

                                            val maxSlots = 10
                                            var slotToSave = "hero_1"
                                            for (i in 1..maxSlots) {
                                                if (!context.fileList().contains("hero_$i")) {
                                                    slotToSave = "hero_$i"
                                                    break
                                                }
                                            }
                                            saveToFile(slotToSave, summary.toString())
                                            onSelectedHeroIdChange(slotToSave.substringAfter("hero_").toInt())
                                            
                                            onMenuAppChange(21)
                                            playSoundAction(R.raw.roll, 1)
                                        },
                                        modifier = Modifier.weight(1f).padding(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                    ) { Text("DALEJ", fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    GenProTheme {
        MainScreen(menuApp = 0, onMenuAppChange = {}, selectedHeroId = 1, isEditing = {}, onSelectedHeroIdChange = {})
    }
}


@Composable
fun SettingsScreen(
    readFromFile: (String) -> String,
    saveToFile: (String, String) -> Unit,
    context: Context,
    updateFileList: () -> Unit,
    daysElapsed: Long,
    isEditing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onGMModeChange: (Boolean) -> Unit,
    onNavigate: (Int) -> Unit,
    isGMMode: Boolean,
    playSound: (Int, Int, Int) -> Unit
) {
    var musicEnabled by remember { mutableStateOf(readFromFile("music_setting") != "off") }
    var soundsEnabled by remember { mutableStateOf(readFromFile("sounds_setting") != "off") }
    var musicPosition by remember { mutableFloatStateOf(0f) }

    var editingFileName by remember { mutableStateOf<String?>(null) }
    var editingFileContent by remember { mutableStateOf("") }

    if (editingFileName != null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Edycja: ${editingFileName}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = editingFileContent,
                onValueChange = { editingFileContent = it },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                textStyle = TextStyle(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { editingFileName = null },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)
                ) {
                    Text("ANULUJ")
                }
                Button(
                    onClick = {
                        saveToFile(editingFileName!!, editingFileContent)
                        editingFileName = null
                        updateFileList()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("ZAPISZ")
                }
            }
        }
    } else if (isEditing) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Od startu minęło $daysElapsed dni", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { onEditingChange(false) }) {
                    Icon(Icons.Default.Close, contentDescription = "Zamknij edytor", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            val allFiles = context.fileList().map { it to readFromFile(it) }
            val sortedFiles = allFiles.filter { it.first != "music_setting" && it.first != "sounds_setting" }.sortedBy { it.first } +
                    allFiles.filter { it.first == "sounds_setting" } +
                    allFiles.filter { it.first == "music_setting" }
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(sortedFiles) { _, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.first, color = Color.White, fontSize = 14.sp)
                            Text(item.second, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                        }
                        Row {
                            if (isGMMode) {
                                IconButton(onClick = {
                                    editingFileName = item.first
                                    editingFileContent = item.second
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edytuj", tint = Color.White)
                                }
                            }
                            IconButton(onClick = { context.deleteFile(item.first); updateFileList() }) {
                                Icon(Icons.Default.Close, contentDescription = "Usuń", tint = Color.Red)
                            }
                        }
                    }
                    HorizontalDivider(color = Color.DarkGray)
                }
            }
            Button(
                onClick = { onEditingChange(false) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("ZAMKNIJ EDYTOR")
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        musicEnabled = !musicEnabled
                        saveToFile("music_setting", if (musicEnabled) "on" else "off")
                        if (musicEnabled) {
                            playSound(R.raw.fanmusic, 0, (musicPosition * 300000).toInt())
                        } else {
                            playSound(R.raw.fanmusic, 0, 0)
                        }
                    },
                    modifier = if (musicEnabled) Modifier.weight(0.4f).height(60.dp) else Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (musicEnabled) Color.White else Color.DarkGray, contentColor = Color.Black)
                ) {
                    Text("🎼 ${if (musicEnabled) "✔️" else "❌"}", fontSize = if (musicEnabled) 18.sp else 20.sp)
                }

                if (musicEnabled) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = musicPosition,
                        onValueChange = { musicPosition = it },
                        onValueChangeFinished = {
                            playSound(R.raw.fanmusic, 0, (musicPosition * 300000).toInt())
                        },
                        modifier = Modifier.weight(0.6f)
                    )
                }
            }

            Button(
                onClick = {
                    soundsEnabled = !soundsEnabled
                    saveToFile("sounds_setting", if (soundsEnabled) "on" else "off")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (soundsEnabled) Color.White else Color.DarkGray, contentColor = Color.Black)
            ) {
                Text("🔈 ${if (soundsEnabled) "✔️" else "❌"}", fontSize = 20.sp)
            }

            Button(
                onClick = {
                    saveToFile("gm_mode_setting", "on")
                    onGMModeChange(true)
                    onNavigate(13)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f), contentColor = Color.White)
            ) {
                Text("TRYB MG", fontSize = 20.sp)
            }

            Button(
                onClick = { onEditingChange(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("EDYTOR", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    context.fileList().forEach { context.deleteFile(it) }
                    updateFileList()
                    Toast.makeText(context, "RESET APLIKACJI WYKONANY", Toast.LENGTH_SHORT).show()
                    onClose()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.5f), contentColor = Color.White)
            ) {
                Text("RESET APLIKACJI", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray, contentColor = Color.White)
            ) {
                Text("POWRÓT", fontWeight = FontWeight.Bold)
            }
        }
    }
}
