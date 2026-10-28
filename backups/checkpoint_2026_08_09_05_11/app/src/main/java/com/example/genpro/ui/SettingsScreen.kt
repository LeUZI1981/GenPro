package com.example.genpro.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpro.R

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
