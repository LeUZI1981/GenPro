package com.example.genpro

import android.content.Context
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File

object WorldPersistence {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun getSettlementFileName(name: String): String = "city_${name.replace(" ", "_").lowercase()}.json"

    fun decodeSettlementOrNull(content: String): SettlementInstance? {
        if (!content.trimStart().startsWith("{")) return null
        return try {
            json.decodeFromString(SettlementInstance.serializer(), content)
        } catch (_: SerializationException) {
            null
        }
    }

    fun saveSettlement(context: Context, settlement: SettlementInstance) {
        val fileName = getSettlementFileName(settlement.name)
        val content = json.encodeToString(SettlementInstance.serializer(), settlement)
        try {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(content.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadSettlement(context: Context, name: String): SettlementInstance? {
        val fileName = getSettlementFileName(name)
        return try {
            val content = context.openFileInput(fileName).bufferedReader().use { it.readText() }
            decodeSettlementOrNull(content)
        } catch (e: Exception) {
            null
        }
    }

    fun exists(context: Context, name: String): Boolean {
        val file = File(context.filesDir, getSettlementFileName(name))
        return file.exists()
    }
}
