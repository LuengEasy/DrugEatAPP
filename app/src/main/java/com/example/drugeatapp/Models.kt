package com.example.drugeatapp

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Medication(
    val id: Long,
    val name: String,
    val colorHex: Long
)

@Serializable
data class DayRecord(
    val date: String,
    val medicationIds: List<Long> = emptyList(),
    val note: String = ""
)

@Serializable
data class AppData(
    val medications: List<Medication> = emptyList(),
    val records: List<DayRecord> = emptyList()
)

fun Medication.asColor(): Color = Color(colorHex)
fun LocalDate.storageKey(): String = toString()
