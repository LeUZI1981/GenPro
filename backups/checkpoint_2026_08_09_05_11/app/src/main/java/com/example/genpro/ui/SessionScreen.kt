package com.example.genpro.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpro.colorize

@Composable
fun SessionScreen(readFromFile: (String) -> String) {
    var historyEntries by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(Unit) {
        val contentKosci = readFromFile("histKosci")
        val contentStat = readFromFile("histStat")
        val contentPD = readFromFile("histPD")

        val cyfryIconsA = arrayOf("𝟘", "𝟙", "𝟚", "𝟛", "𝟜", "𝟝", "𝟞", "𝟟", "𝟠", "𝟡")
        fun normalize(text: String): String {
            var res = text
            cyfryIconsA.forEachIndexed { i, s -> res = res.replace(s, i.toString()) }
            return res
        }

        fun parseLines(content: String): List<Pair<String, String>> {
            val lines = content.lines().filter { it.isNotBlank() }
            var currentHeader = ""
            var currentDateSort = "00000000"
            return lines.mapNotNull { line ->
                if (line.startsWith("꧁") || line.contains(Regex("""\d{2}\.\d{2}\.\d{4}"""))) {
                    currentHeader = line
                    val normalized = normalize(line)
                    val match = """(\d{2})\.(\d{2})\.(\d{4})""".toRegex().find(normalized)
                    if (match != null) {
                        val (d, m, y) = match.destructured
                        currentDateSort = "$y$m$d"
                    }
                    null
                } else {
                    val normLine = normalize(line)
                    val timeMatch = """(\d{2}):(\d{2})""".toRegex().find(normLine)
                    val timeSort = if (timeMatch != null) {
                        val (h, min) = timeMatch.destructured
                        "$h$min"
                    } else "0000"
                    (currentDateSort + timeSort) to (currentHeader + "|||" + line)
                }
            }
        }

        val all = (parseLines(contentKosci) + parseLines(contentStat) + parseLines(contentPD))
            .filter { it.second.isNotEmpty() }
            .sortedByDescending { it.first }

        val final = mutableListOf<String>()
        var lastH = ""
        all.forEach { (_, value) ->
            val parts = value.split("|||", limit = 2)
            val h = parts[0]
            val l = if (parts.size > 1) parts[1] else ""
            if (h != lastH && h.isNotEmpty()) {
                final.add(h)
                lastH = h
            }
            if (l.isNotEmpty()) final.add(l)
        }

        historyEntries = if (final.isEmpty()) listOf("Brak wpisów w historii sesji") else final
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(historyEntries) { _, entry ->
                val diceCount = entry.count { it == '‹' }
                val fontSize = when {
                    entry.startsWith("꧁") -> 17.sp
                    diceCount > 6 -> 13.sp
                    else -> 16.sp
                }
                Text(
                    text = colorize(entry),
                    fontSize = fontSize,
                    modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            }
        }
    }
}
