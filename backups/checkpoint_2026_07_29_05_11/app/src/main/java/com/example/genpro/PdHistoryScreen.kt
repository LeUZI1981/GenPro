package com.example.genpro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PdHistoryScreen(
    selectedHeroId: Int,
    nosiferFamily: FontFamily,
    slaboFamily: FontFamily,
    readFromFile: (String) -> String,
    onBack: () -> Unit
) {
    val historyFile = "hero_${selectedHeroId}_pd_history"
    val historyData = readFromFile(historyFile)
    val historyLines = historyData.lines().filter { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz", tint = Color.White)
            }
            Text(
                "HISTORIA ROZWOJU",
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = nosiferFamily,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (historyLines.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Brak wpisów w historii.", color = Color.Gray, fontFamily = slaboFamily)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(historyLines) { line ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = line,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = slaboFamily,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
