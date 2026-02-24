package com.example.drugeatapp

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MedicationRepository(context: Context) {
    private val pref = context.getSharedPreferences("drug_eat_pref", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): AppData {
        val raw = pref.getString(KEY_DATA, null) ?: return AppData()
        return runCatching { json.decodeFromString<AppData>(raw) }.getOrDefault(AppData())
    }

    fun save(data: AppData) {
        pref.edit().putString(KEY_DATA, json.encodeToString(data)).apply()
    }

    private companion object {
        const val KEY_DATA = "app_data"
    }
}
