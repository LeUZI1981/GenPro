package com.example.genpro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Usunięto MapPin na rzecz TravelNode z DomainModels.kt

data class DrawingPath(
    val points: List<androidx.compose.ui.geometry.Offset>,
    val color: Color,
    val thickness: Float = 3f
)

enum class DrawingTool { BRUSH }

@Composable
fun MapsScreen(
    selectedHeroId: Int,
    nosiferFamily: FontFamily,
    aladinFamily: FontFamily,
    slaboFamily: FontFamily,
    lobsterFamily: FontFamily,
    readFromFile: (String) -> String,
    saveToFile: (String, String) -> Unit,
    onBack: () -> Unit,
    isGMMode: Boolean = false
) {
    val activeMapFileName = "hero_map_active_$selectedHeroId"
    var currentMapId by remember(selectedHeroId) { 
        mutableStateOf(readFromFile(activeMapFileName).ifBlank { "mapworld" }) 
    }

    val availableMaps = listOf(
        Triple("mapworld", "Mapa Świata", R.drawable.mapworld),
        Triple("imperium", "Imperium", R.drawable.imperium),
        Triple("bretonnia", "Bretonnia", R.drawable.bretonnia),
        Triple("estalia", "Estalia", R.drawable.estalia),
        Triple("kislev", "Kislev", R.drawable.kislev),
        Triple("tilea", "Tilea", R.drawable.tilea),
        Triple("arabia", "Arabia", R.drawable.arabia),
        Triple("ulthuan", "Ulthuan", R.drawable.ulthuan),
        Triple("newworld", "Nowy Świat", R.drawable.newworld),
        Triple("borderprinces", "Księstwa Graniczne", R.drawable.borderprinces)
    )
    
    val currentMapRes = availableMaps.find { it.first == currentMapId }?.third ?: R.drawable.mapworld

    val pinsFileName = "hero_map_pins_${selectedHeroId}_$currentMapId"
    val drawingFileName = "hero_map_drawings_${selectedHeroId}_$currentMapId"
    
    // Wczytujemy węzły i drogi
    var nodes by remember(selectedHeroId, currentMapId) { mutableStateOf(decodeNodes(readFromFile(pinsFileName))) }
    var roads by remember(selectedHeroId, currentMapId) { mutableStateOf(decodeRoads(readFromFile("hero_map_roads_${selectedHeroId}_$currentMapId"))) }
    var allPaths by remember(selectedHeroId, currentMapId) { mutableStateOf(decodeDrawings(readFromFile(drawingFileName))) }

    val viewStateFileName = "hero_map_viewstate_${selectedHeroId}_$currentMapId"
    val savedViewState = remember(selectedHeroId, currentMapId) { readFromFile(viewStateFileName) }
    val savedParts = remember(savedViewState) { savedViewState.split("|") }

    var scale by remember(selectedHeroId, currentMapId) { 
        mutableFloatStateOf(savedParts.getOrNull(0)?.toFloatOrNull() ?: 25f)
    }
    var offset by remember(selectedHeroId, currentMapId) {
        mutableStateOf(
            if (savedParts.size >= 3) androidx.compose.ui.geometry.Offset(savedParts[1].toFloatOrNull() ?: 0f, savedParts[2].toFloatOrNull() ?: 0f)
            else androidx.compose.ui.geometry.Offset.Zero
        )
    }

    var showPinsLayer by remember(selectedHeroId, currentMapId) { mutableStateOf(savedParts.getOrNull(3)?.let { it == "true" } ?: true) }
    var showDrawingsLayer by remember(selectedHeroId, currentMapId) { mutableStateOf(savedParts.getOrNull(4)?.let { it == "true" } ?: true) }
    var showGridLayer by remember(selectedHeroId, currentMapId) { mutableStateOf(savedParts.getOrNull(5)?.let { it == "true" } ?: false) }
    // Etykiety są trwale wyłączone na mapie zgodnie z życzeniem (tylko kolory)
    val showLabels = false 
    var showPinList by remember(selectedHeroId, currentMapId) { mutableStateOf(false) }

    fun saveCurrentViewState() {
        saveToFile(viewStateFileName, "$scale|${offset.x}|${offset.y}|$showPinsLayer|$showDrawingsLayer|$showGridLayer|$showLabels|false")
        saveToFile(activeMapFileName, currentMapId)
    }

    // Automatyczny zapis stanu widoku przy zmianach transformacji
    LaunchedEffect(scale, offset, showPinsLayer, showDrawingsLayer, showGridLayer, showPinList, selectedHeroId, currentMapId) {
        delay(1000)
        saveCurrentViewState()
    }

    var isDrawingMode by remember(selectedHeroId, currentMapId) { mutableStateOf(false) }
    var isRoadMode by remember(selectedHeroId, currentMapId) { mutableStateOf(false) }
    var selectedNodesForRoad by remember(selectedHeroId, currentMapId) { mutableStateOf(listOf<Long>()) }
    
    var activeTool by remember(selectedHeroId, currentMapId) { mutableStateOf(DrawingTool.BRUSH) }
    var selectedColor by remember(selectedHeroId, currentMapId) { mutableStateOf(Color.Black) }
    var selectedThickness by remember(selectedHeroId, currentMapId) { mutableFloatStateOf(3f) }
    val currentPathPoints = remember(selectedHeroId, currentMapId) { mutableStateListOf<androidx.compose.ui.geometry.Offset>() }

    var showPinDialog by remember(selectedHeroId, currentMapId) { mutableStateOf<TravelNode?>(null) }
    var showClearDrawingConfirm by remember(selectedHeroId, currentMapId) { mutableStateOf(false) }
    var showLayersMenu by remember(selectedHeroId, currentMapId) { mutableStateOf(false) }
    var showMapSelection by remember { mutableStateOf(false) }

    var showAddPinDialog by remember(selectedHeroId, currentMapId) { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var newNodeName by remember(selectedHeroId, currentMapId) { mutableStateOf("") }
    var newNodeDescription by remember(selectedHeroId, currentMapId) { mutableStateOf("") }
    var newNodeZone by remember(selectedHeroId, currentMapId) { mutableStateOf(ZoneType.NATURA) }
    var newNodeSettlementType by remember(selectedHeroId, currentMapId) { mutableStateOf<SettlementType?>(null) }
    var newNodeTags by remember(selectedHeroId, currentMapId) { mutableStateOf(setOf<LocationTag>()) }
    var newNodeColor by remember(selectedHeroId, currentMapId) { mutableStateOf(Color.Yellow) }

    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Optymalizacja renderowania: grupujemy ścieżki o tym samym kolorze i grubości, by zmniejszyć liczbę wywołań drawPath
    val groupedPaths = remember(allPaths) {
        allPaths.groupBy { it.color to it.thickness }.map { (style, paths) ->
            val combinedPath = Path()
            paths.forEach { dp ->
                if (dp.points.isNotEmpty()) {
                    combinedPath.moveTo(dp.points.first().x, dp.points.first().y)
                    for (i in 1 until dp.points.size) {
                        combinedPath.lineTo(dp.points[i].x, dp.points[i].y)
                    }
                }
            }
            Triple(combinedPath, style.first, style.second)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val centerX = constraints.maxWidth.toFloat() / 2f
        val centerY = constraints.maxHeight.toFloat() / 2f
        val painter = painterResource(id = currentMapRes)
        val imageSize = painter.intrinsicSize

        // Obliczamy rzeczywiste wymiary obrazu po dopasowaniu do ekranu (ContentScale.Fit)
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val scaleFactor = minOf(screenW / imageSize.width, screenH / imageSize.height)
        val actualImageWidth = imageSize.width * scaleFactor
        val actualImageHeight = imageSize.height * scaleFactor

        // Funkcja pomocnicza do ograniczania widoku mapy w jej rzeczywistych granicach (piksele)
        val applyBounds = { o: androidx.compose.ui.geometry.Offset, s: Float ->
            // Marginesy przy skali s względem rzeczywistych wymiarów obrazu
            val maxOffsetX = ((actualImageWidth * s) - screenW).coerceAtLeast(0f) / 2f
            val maxOffsetY = ((actualImageHeight * s) - screenH).coerceAtLeast(0f) / 2f
            androidx.compose.ui.geometry.Offset(
                o.x.coerceIn(-maxOffsetX, maxOffsetX),
                o.y.coerceIn(-maxOffsetY, maxOffsetY)
            )
        }

        // Strażnik granic: koryguje pozycję przy zmianie skali, rozmiaru ekranu lub samej mapy
        LaunchedEffect(scale, constraints.maxWidth, constraints.maxHeight, currentMapId) {
            val clamped = applyBounds(offset, scale)
            if (clamped.x != offset.x || clamped.y != offset.y) offset = clamped
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(isDrawingMode, currentMapId) {
                    if (isDrawingMode) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val mapX = (startOffset.x - centerX - offset.x) / scale + centerX
                                val mapY = (startOffset.y - centerY - offset.y) / scale + centerY
                                currentPathPoints.add(androidx.compose.ui.geometry.Offset(mapX, mapY))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val dragPos = change.position
                                val mapX = (dragPos.x - centerX - offset.x) / scale + centerX
                                val mapY = (dragPos.y - centerY - offset.y) / scale + centerY
                                currentPathPoints.add(androidx.compose.ui.geometry.Offset(mapX, mapY))
                            },
                            onDragEnd = {
                                if (currentPathPoints.isNotEmpty()) {
                                    allPaths = allPaths + DrawingPath(currentPathPoints.toList(), selectedColor, selectedThickness)
                                    currentPathPoints.clear()
                                    saveToFile(drawingFileName, encodeDrawings(allPaths))
                                }
                            }
                        )
                    } else {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = scale
                            val minScale = if (currentMapId == "mapworld") 15f else 5f
                            val newScale = (scale * zoom).coerceIn(minScale, 100f)
                            
                            val relativeCentroidX = centroid.x - centerX
                            val relativeCentroidY = centroid.y - centerY
                            
                            val zoomFactor = newScale / oldScale
                            val newOffset = androidx.compose.ui.geometry.Offset(
                                x = relativeCentroidX - (relativeCentroidX - offset.x) * zoomFactor,
                                y = relativeCentroidY - (relativeCentroidY - offset.y) * zoomFactor
                            )
                            
                            scale = newScale
                            offset = applyBounds(newOffset + pan, newScale)
                        }
                    }
                }
                .pointerInput(isDrawingMode, currentMapId) {
                    if (!isDrawingMode) {
                        detectTapGestures(
                            onTap = { showLayersMenu = false },
                            onLongPress = { tapOffset ->
                                val mapX = (tapOffset.x - centerX - offset.x) / scale + centerX
                                val mapY = (tapOffset.y - centerY - offset.y) / scale + centerY
                                
                                val gridSize = 1f
                                val snappedX = ((mapX - centerX) / gridSize).roundToInt() * gridSize + centerX
                                val snappedY = ((mapY - centerY) / gridSize).roundToInt() * gridSize + centerY
                                
                                val nextNum = (nodes.mapNotNull { 
                                    if (it.name.startsWith("Punkt ")) it.name.substringAfter("Punkt ").toIntOrNull() else null 
                                }.maxOrNull() ?: 0) + 1
                                
                                newNodeName = "Punkt $nextNum"
                                showAddPinDialog = androidx.compose.ui.geometry.Offset(snappedX, snappedY)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            Image(
                painter = painter, 
                contentDescription = "World Map",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                if (showGridLayer) {
                    // ... (siatka bez zmian)
                }

                // --- RYSOWANIE DRÓG (TRAKTÓW) ---
                roads.forEach { road ->
                    val startNode = nodes.find { it.id == road.startNodeId }
                    val endNode = nodes.find { it.id == road.endNodeId }
                    if (startNode != null && endNode != null) {
                        val roadColor = when(road.type) {
                            RoadType.HIGHWAY -> Color(0xFFFFD700) // Gold
                            RoadType.ROAD -> Color(0xFF8B4513)    // Brown
                            RoadType.PATH -> Color.DarkGray
                        }
                        drawLine(
                            color = roadColor,
                            start = androidx.compose.ui.geometry.Offset(startNode.x, startNode.y),
                            end = androidx.compose.ui.geometry.Offset(endNode.x, endNode.y),
                            strokeWidth = (when(road.type) {
                                RoadType.HIGHWAY -> 4f
                                RoadType.ROAD -> 2f
                                RoadType.PATH -> 1f
                            }) / scale,
                            cap = StrokeCap.Round
                        )
                    }
                }

                if (showDrawingsLayer) {
                    groupedPaths.forEach { (path, color, thickness) ->
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = thickness / scale, cap = StrokeCap.Round)
                        )
                    }

                    if (currentPathPoints.size > 1) {
                        val path = Path().apply {
                            val first = currentPathPoints.first()
                            moveTo(first.x, first.y)
                            for (i in 1 until currentPathPoints.size) {
                                lineTo(currentPathPoints[i].x, currentPathPoints[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = selectedColor,
                            style = Stroke(width = selectedThickness / scale, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        val density = LocalDensity.current
        if (showPinsLayer) {
            nodes.forEach { node ->
                if (!node.discovered && !isGMMode) return@forEach

                val drawSizePx = 1f * scale
                val drawSizeDp = with(density) { drawSizePx.toDp() }
                val touchSizeDp = 24.dp
                val actualSizeDp = maxOf(touchSizeDp, drawSizeDp)
                val actualSizePx = with(density) { actualSizeDp.toPx() }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(
                            ((node.x - centerX) * scale + centerX + offset.x - actualSizePx / 2f).roundToInt(),
                            ((node.y - centerY) * scale + centerY + offset.y - actualSizePx / 2f).roundToInt()
                        ) }
                        .size(actualSizeDp)
                        .clickable {
                            if (isRoadMode) {
                                selectedNodesForRoad = if (selectedNodesForRoad.contains(node.id)) {
                                    selectedNodesForRoad - node.id
                                } else {
                                    (selectedNodesForRoad + node.id).takeLast(2)
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showPinDialog = node
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(drawSizeDp)
                            .background(
                                if (selectedNodesForRoad.contains(node.id)) Color.Cyan else Color(node.color), 
                                RoundedCornerShape(if (node.zone == ZoneType.CYWILIZACJA) 0.dp else 4.dp)
                            )
                            .border(
                                if (selectedNodesForRoad.contains(node.id)) 2.dp else 1.dp, 
                                if (selectedNodesForRoad.contains(node.id)) Color.White else Color.White.copy(alpha = 0.5f), 
                                RoundedCornerShape(if (node.zone == ZoneType.CYWILIZACJA) 0.dp else 4.dp)
                            )
                    )
                }
            }
        }


        val currentMapName = availableMaps.find { it.first == currentMapId }?.second ?: ""
        Text(
            text = currentMapName,
            color = Color.Yellow,
            fontFamily = aladinFamily,
            fontSize = 22.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .background(Color.Black.copy(0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )

        Box(
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                Box {
                    IconButton(
                        onClick = { showMapSelection = true },
                        modifier = Modifier.background(Color.Black.copy(0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Wybierz mapę", tint = Color.White)
                    }

                    DropdownMenu(
                        expanded = showMapSelection,
                        onDismissRequest = { showMapSelection = false },
                        modifier = Modifier.background(Color.DarkGray)
                    ) {
                        availableMaps.forEach { (id, name, _) ->
                            DropdownMenuItem(
                                text = { Text(name, color = if (currentMapId == id) Color.Yellow else Color.White, fontFamily = aladinFamily, fontSize = 16.sp) },
                                onClick = {
                                    saveCurrentViewState()
                                    currentMapId = id
                                    showMapSelection = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = {
                    scale = 25f
                    // Ustawienie na X: 0, Y: -160 z uwzględnieniem granic
                    offset = applyBounds(androidx.compose.ui.geometry.Offset(0f, 160f * 25f), 25f)
                },
                modifier = Modifier.background(Color.Black.copy(0.6f), CircleShape)
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Resetuj widok", tint = Color.White)
            }
            
            Spacer(Modifier.height(8.dp))

            IconButton(
                onClick = {
                    showLayersMenu = !showLayersMenu
                    if (showLayersMenu) {
                        showPinList = false
                        isDrawingMode = false
                    }
                },
                modifier = Modifier.background(if (showLayersMenu) Color.Yellow.copy(0.4f) else Color.Black.copy(0.6f), CircleShape)
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Warstwy", tint = if (showLayersMenu) Color.Yellow else Color.White)
            }

            Spacer(Modifier.height(8.dp))

            IconButton(
                onClick = {
                    showPinList = !showPinList
                    if (showPinList) {
                        showLayersMenu = false
                        isDrawingMode = false
                    }
                },
                modifier = Modifier.background(if (showPinList) Color.Yellow.copy(0.4f) else Color.Black.copy(0.6f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Lista punktów", tint = if (showPinList) Color.Yellow else Color.White)
            }

            if (showLayersMenu) {
                LayerToggle(Icons.Default.Visibility, showPinsLayer, "Piny") { showPinsLayer = it }
                // Etykiety są teraz trwale ukryte na mapie zgodnie z życzeniem
                LayerToggle(Icons.Default.Visibility, showDrawingsLayer, "Szkic") { showDrawingsLayer = it }
                LayerToggle(Icons.Default.Grid4x4, showGridLayer, "Siatka") { showGridLayer = it }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))

            if (isDrawingMode) {
                if (activeTool == DrawingTool.BRUSH) {
                    listOf(3f, 6f, 12f).forEach { thickness ->
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .size(if (thickness == 3f) 20.dp else if (thickness == 6f) 24.dp else 28.dp)
                                .clip(CircleShape)
                                .background(if (selectedThickness == thickness) Color.Yellow else Color.Gray)
                                .clickable { selectedThickness = thickness },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(thickness.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                val colors = listOf(Color.Black, Color.Red, Color.Green, Color.Blue, Color.White)
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(if (selectedColor == color) 2.dp else 1.dp, if (selectedColor == color) Color.Yellow else Color.Gray, CircleShape)
                            .clickable { selectedColor = color }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                IconButton(
                    onClick = { 
                        if (allPaths.isNotEmpty()) {
                            allPaths = allPaths.dropLast(1)
                            saveToFile(drawingFileName, encodeDrawings(allPaths))
                        }
                    },
                    enabled = allPaths.isNotEmpty(),
                    modifier = Modifier.background(Color.Black.copy(0.6f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Cofnij", tint = if (allPaths.isNotEmpty()) Color.White else Color.Gray)
                }
                
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { showClearDrawingConfirm = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.LayersClear, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Wyczyść", color = Color.Yellow, fontSize = 10.sp)
                }
            }

            IconButton(
                onClick = {
                    isRoadMode = !isRoadMode
                    if (isRoadMode) {
                        isDrawingMode = false
                        showLayersMenu = false
                        showPinList = false
                        selectedNodesForRoad = emptyList()
                    }
                },
                modifier = Modifier.background(if (isRoadMode) Color.DarkGray else Color.Black.copy(0.6f), CircleShape)
            ) {
                Icon(Icons.Default.AddRoad, contentDescription = "Tryb Dróg", tint = if (isRoadMode) Color.Yellow else Color.White)
            }

            if (isRoadMode && selectedNodesForRoad.size == 2) {
                var showRoadTypeDialog by remember { mutableStateOf(false) }
                
                IconButton(
                    onClick = { showRoadTypeDialog = true },
                    modifier = Modifier.background(Color.Green.copy(0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Utwórz drogę", tint = Color.White)
                }
                
                // Przycisk "Podróżuj" widoczny tylko gdy zaznaczono 2 węzły i istnieje droga
                val existingRoad = roads.find { (it.startNodeId == selectedNodesForRoad[0] && it.endNodeId == selectedNodesForRoad[1]) || (it.startNodeId == selectedNodesForRoad[1] && it.endNodeId == selectedNodesForRoad[0]) }
                if (existingRoad != null) {
                    val context = LocalContext.current
                    IconButton(
                        onClick = {
                            val startNode = nodes.find { it.id == selectedNodesForRoad[0] }
                            val endNode = nodes.find { it.id == selectedNodesForRoad[1] }
                            if (startNode != null && endNode != null) {
                                val intent = android.content.Intent(context, TravelActivity::class.java).apply {
                                    putExtra("HERO_ID", selectedHeroId.toString())
                                    putExtra("MAP_ID", currentMapId)
                                    putExtra("DESTINATION", endNode.name)
                                    putExtra("DESTINATION_ID", endNode.id)
                                    putExtra("TRAVEL_TIME", (existingRoad.distance / 10f).toInt().coerceAtLeast(1))
                                    putExtra("ROAD_TYPE", existingRoad.type.name)
                                    putExtra("ZONE_TYPE", endNode.zone.name)
                                    putExtra("TAGS", endNode.tags.joinToString(",") { it.name })
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.background(Color.Blue.copy(0.6f), CircleShape).padding(top = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = "Podróżuj", tint = Color.White)
                    }
                }
                
                if (showRoadTypeDialog) {
                    AlertDialog(
                        onDismissRequest = { showRoadTypeDialog = false },
                        title = { Text("Typ Drogi", fontFamily = aladinFamily) },
                        text = {
                            Column {
                                RoadType.entries.forEach { type ->
                                    Button(
                                        onClick = {
                                            val startId = selectedNodesForRoad[0]
                                            val endId = selectedNodesForRoad[1]
                                            val startNode = nodes.find { it.id == startId }
                                            val endNode = nodes.find { it.id == endId }
                                            if (startNode != null && endNode != null) {
                                                val dist = kotlin.math.sqrt(
                                                    Math.pow((endNode.x - startNode.x).toDouble(), 2.0) +
                                                    Math.pow((endNode.y - startNode.y).toDouble(), 2.0)
                                                ).toFloat()
                                                
                                                val newRoad = Road(
                                                    id = "road_${System.currentTimeMillis()}",
                                                    startNodeId = startId,
                                                    endNodeId = endId,
                                                    type = type,
                                                    distance = dist
                                                )
                                                roads = roads + newRoad
                                                saveToFile("hero_map_roads_${selectedHeroId}_$currentMapId", encodeRoads(roads))
                                            }
                                            selectedNodesForRoad = emptyList()
                                            showRoadTypeDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Text("${type.displayName} (x${type.speedMultiplier})")
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = { TextButton(onClick = { showRoadTypeDialog = false }) { Text("Anuluj") } }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            IconButton(
                onClick = {
                    isDrawingMode = !isDrawingMode
                    if (isDrawingMode) {
                        showLayersMenu = false
                        showPinList = false
                    }
                },
                modifier = Modifier.background(if (isDrawingMode) Color.DarkGray else Color.Black.copy(0.6f), CircleShape)
            ) {
                Icon(Icons.Default.Brush, contentDescription = "Tryb Rysowania", tint = if (isDrawingMode) Color.Yellow else Color.White)
            }
        }

        if (showClearDrawingConfirm) {
            AlertDialog(
                onDismissRequest = { showClearDrawingConfirm = false },
                title = { Text("Wyczyścić rysunek?", fontFamily = aladinFamily) },
                text = { Text("Wszystkie narysowane linie zostaną trwale usunięte.") },
                confirmButton = {
                    Button(onClick = {
                        allPaths = emptyList()
                        saveToFile(drawingFileName, "")
                        showClearDrawingConfirm = false
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Usuń") }
                },
                dismissButton = { TextButton(onClick = { showClearDrawingConfirm = false }) { Text("Anuluj") } }
            )
        }

        if (showPinDialog != null) {
            val node = showPinDialog!!
            var editName by remember(node.id) { mutableStateOf(node.name) }
            var editDescription by remember(node.id) { mutableStateOf(node.description) }
            var editZone by remember(node.id) { mutableStateOf(node.zone) }
            var editTags by remember(node.id) { mutableStateOf(node.tags.toSet()) }
            var editColor by remember(node.id) { mutableStateOf(Color(node.color)) }
            
            AlertDialog(
                onDismissRequest = { showPinDialog = null },
                title = { Text("Edytuj Węzeł", fontFamily = aladinFamily) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        TextField(value = editName, onValueChange = { editName = it }, label = { Text("Nazwa") })
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Strefa: ", fontSize = 12.sp)
                            ZoneType.entries.forEach { zone ->
                                FilterChip(
                                    selected = editZone == zone,
                                    onClick = { editZone = zone },
                                    label = { Text(zone.displayName, fontSize = 10.sp) },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Tagi:", fontSize = 12.sp)
                        FlowRow(modifier = Modifier.padding(vertical = 4.dp)) {
                            LocationTag.entries.forEach { tag ->
                                FilterChip(
                                    selected = editTags.contains(tag),
                                    onClick = { 
                                        editTags = if (editTags.contains(tag)) editTags - tag else editTags + tag
                                    },
                                    label = { Text(tag.tagName, fontSize = 10.sp) },
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                        TextField(value = editDescription, onValueChange = { editDescription = it }, label = { Text("Opis") })
                        Spacer(Modifier.height(8.dp))
                        Text("Kolor:", fontSize = 12.sp)
                        Row {
                            val pinColors = listOf(Color.Yellow, Color.Red, Color.Green, Color.Blue, Color.Cyan, Color.Magenta, Color.White)
                            pinColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(if (editColor == color) 2.dp else 0.dp, Color.Black, CircleShape)
                                        .clickable { editColor = color }
                                )
                            }
                        }
                    }
                },
                confirmButton = { 
                    Button(onClick = {
                        nodes = nodes.map { if (it.id == node.id) it.copy(name = editName, description = editDescription, zone = editZone, tags = editTags.toList(), color = editColor.value) else it }
                        saveToFile(pinsFileName, encodeNodes(nodes))
                        
                        // Usuwamy drogi powiązane z usuniętymi węzłami jeśli to byłaby operacja usuwania, 
                        // ale tutaj tylko edytujemy. Jednak przy usuwaniu warto o tym pamiętać.
                        
                        showPinDialog = null
                    }) { Text("Zapisz") }
                },
                dismissButton = {
                    IconButton(onClick = {
                        val nodeId = node.id
                        nodes = nodes.filter { it.id != nodeId }
                        roads = roads.filter { it.startNodeId != nodeId && it.endNodeId != nodeId }
                        saveToFile(pinsFileName, encodeNodes(nodes))
                        saveToFile("hero_map_roads_${selectedHeroId}_$currentMapId", encodeRoads(roads))
                        showPinDialog = null
                    }) { Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = Color.Red) }
                }
            )
        }

        if (showAddPinDialog != null) {
            AlertDialog(
                onDismissRequest = { showAddPinDialog = null },
                title = { Text("Nowy Węzeł", fontFamily = aladinFamily) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        TextField(value = newNodeName, onValueChange = { newNodeName = it }, label = { Text("Nazwa") })
                        Spacer(Modifier.height(8.dp))
                        Text("Strefa:", fontSize = 12.sp)
                        Row {
                            ZoneType.entries.forEach { zone ->
                                FilterChip(
                                    selected = newNodeZone == zone,
                                    onClick = { 
                                        newNodeZone = zone
                                        if (zone == ZoneType.NATURA) newNodeSettlementType = null
                                    },
                                    label = { Text(zone.displayName) }
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                        }
                        
                        if (newNodeZone == ZoneType.CYWILIZACJA) {
                            Spacer(Modifier.height(8.dp))
                            Text("Typ Osady:", fontSize = 12.sp)
                            Row {
                                SettlementType.entries.forEach { sType ->
                                    FilterChip(
                                        selected = newNodeSettlementType == sType,
                                        onClick = { newNodeSettlementType = sType },
                                        label = { Text(sType.displayName) }
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("Tagi:", fontSize = 12.sp)
                        FlowRow(modifier = Modifier.padding(vertical = 4.dp)) {
                            LocationTag.entries.forEach { tag ->
                                FilterChip(
                                    selected = newNodeTags.contains(tag),
                                    onClick = { 
                                        newNodeTags = if (newNodeTags.contains(tag)) newNodeTags - tag else newNodeTags + tag
                                    },
                                    label = { Text(tag.tagName, fontSize = 10.sp) },
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val pos = showAddPinDialog!!
                        val newNode = TravelNode(
                            id = System.currentTimeMillis(),
                            name = newNodeName.ifEmpty { "Punkt" },
                            x = pos.x,
                            y = pos.y,
                            zone = newNodeZone,
                            tags = newNodeTags.toList(),
                            description = newNodeDescription,
                            color = newNodeColor.value
                        )
                        nodes = nodes + newNode
                        saveToFile(pinsFileName, encodeNodes(nodes))
                        showAddPinDialog = null
                    }) { Text("Dodaj") }
                }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            val mapCenterX = (-offset.x / scale).roundToInt()
            val mapCenterY = (-offset.y / scale).roundToInt()
            Text(
                text = "X: $mapCenterX  Y: $mapCenterY",
                color = Color.Yellow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = slaboFamily
            )
        }

        if (showPinList) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(200.dp)
                    .align(Alignment.CenterStart)
                    .background(Color.Black.copy(0.85f))
                    .padding(8.dp)
                    .clickable { } // block tap through
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("WĘZŁY", color = Color.Yellow, fontFamily = aladinFamily, fontSize = 14.sp)
                        IconButton(onClick = { showPinList = false }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(nodes.size, key = { nodes[it].id }) { index ->
                            val node = nodes[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val targetOffset = androidx.compose.ui.geometry.Offset((centerX - node.x) * scale, (centerY - node.y) * scale)
                                        offset = applyBounds(targetOffset, scale)
                                        saveCurrentViewState()
                                        showPinList = false
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp)
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(10.dp).background(Color(node.color)))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(node.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(node.zone.displayName, color = Color.Gray, fontSize = 9.sp)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

fun decodeNodes(data: String): List<TravelNode> {
    val trimmed = data.trim()
    if (trimmed.isBlank() || trimmed == "null") return emptyList()
    return trimmed.split("|||").mapNotNull { block ->
        if (block.isBlank()) return@mapNotNull null
        try {
            val parts = block.split("###")
            if (parts.size < 6) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: System.currentTimeMillis()
            val x = parts[1].toFloatOrNull() ?: 0f
            val y = parts[2].toFloatOrNull() ?: 0f
            val name = parts[3]
            val desc = parts[4]
            val zone = try { ZoneType.valueOf(parts[5]) } catch(e: Exception) { ZoneType.NATURA }
            val colorVal = if (parts.size > 6) parts[6].toULongOrNull() ?: 0xFFFFFF00u else 0xFFFFFF00u
            
            val tags = if (parts.size > 7 && parts[7].isNotBlank()) {
                parts[7].split(",").mapNotNull { tagName ->
                    try { LocationTag.valueOf(tagName) } catch(e: Exception) { null }
                }
            } else emptyList()

            val settlementData = if (parts.size > 8) parts[8] else ""
            val discovered = if (parts.size > 9) parts[9] == "1" else false

            TravelNode(id, name, x, y, zone, tags, desc, colorVal, settlementData, discovered)
        } catch (e: Exception) {
            null
        }
    }
}

fun encodeNodes(nodes: List<TravelNode>): String {
    val sb = StringBuilder()
    nodes.forEachIndexed { index, node ->
        sb.append(node.id).append("###")
        sb.append(node.x).append("###")
        sb.append(node.y).append("###")
        sb.append(node.name).append("###")
        sb.append(node.description).append("###")
        sb.append(node.zone.name).append("###")
        sb.append(node.color).append("###")
        sb.append(node.tags.joinToString(",") { it.name }).append("###")
        sb.append(node.settlementData).append("###")
        sb.append(if (node.discovered) "1" else "0")
        if (index < nodes.size - 1) sb.append("|||")
    }
    return sb.toString()
}

fun decodeRoads(data: String): List<Road> {
    val trimmed = data.trim()
    if (trimmed.isBlank() || trimmed == "null") return emptyList()
    return trimmed.split("|||").mapNotNull { block ->
        val parts = block.split("###")
        if (parts.size < 5) return@mapNotNull null
        Road(
            id = parts[0],
            startNodeId = parts[1].toLongOrNull() ?: return@mapNotNull null,
            endNodeId = parts[2].toLongOrNull() ?: return@mapNotNull null,
            type = try { RoadType.valueOf(parts[3]) } catch(e: Exception) { RoadType.PATH },
            distance = parts[4].toFloatOrNull() ?: 0f
        )
    }
}

fun encodeRoads(roads: List<Road>): String {
    return roads.joinToString("|||") { "${it.id}###${it.startNodeId}###${it.endNodeId}###${it.type.name}###${it.distance}" }
}

fun decodeDrawings(data: String): List<DrawingPath> {
    val trimmed = data.trim()
    if (trimmed.isBlank() || trimmed == "null") return emptyList()
    return trimmed.split("|||").mapNotNull { block ->
        if (block.isBlank()) return@mapNotNull null
        try {
            val sections = block.split("###")
            if (sections.size < 2) return@mapNotNull null
            val color = sections[0].toULongOrNull()?.let { Color(it) } ?: Color.Black
            val pointsString = sections[1]
            val thickness = sections.getOrNull(2)?.toFloatOrNull() ?: 3f
            val points = pointsString.split(";").mapNotNull { p ->
                val coords = p.split(",")
                if (coords.size < 2) return@mapNotNull null
                val x = coords[0].toFloatOrNull() ?: return@mapNotNull null
                val y = coords[1].toFloatOrNull() ?: return@mapNotNull null
                androidx.compose.ui.geometry.Offset(x, y)
            }
            if (points.isEmpty()) return@mapNotNull null
            DrawingPath(points, color, thickness)
        } catch (e: Exception) {
            null
        }
    }
}

fun encodeDrawings(paths: List<DrawingPath>): String {
    val sb = StringBuilder()
    paths.forEachIndexed { index, path ->
        sb.append(path.color.value.toString())
        sb.append("###")
        path.points.forEachIndexed { pIndex, p ->
            sb.append(p.x).append(",").append(p.y)
            if (pIndex < path.points.size - 1) sb.append(";")
        }
        sb.append("###")
        sb.append(path.thickness)
        if (index < paths.size - 1) sb.append("|||")
    }
    return sb.toString()
}

@Composable
fun LayerToggle(icon: ImageVector, visible: Boolean, label: String, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 2.dp)
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onToggle(!visible) }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            if (visible) icon else Icons.Default.VisibilityOff,
            contentDescription = null,
            tint = if (visible) Color.Yellow else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}
