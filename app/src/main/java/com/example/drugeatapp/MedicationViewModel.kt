package com.example.drugeatapp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MedicationViewModel(private val repository: MedicationRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(
        MedicationUiState(
            currentMonth = YearMonth.now(),
            selectedDate = LocalDate.now()
        )
    )
    val uiState: StateFlow<MedicationUiState> = _uiState.asStateFlow()

    init {
        val data = repository.load()
        _uiState.value = _uiState.value.copy(
            medications = data.medications,
            records = data.records.associateBy { it.date }
        )
    }

    fun previousMonth() {
        _uiState.value = _uiState.value.copy(currentMonth = _uiState.value.currentMonth.minusMonths(1))
    }

    fun nextMonth() {
        _uiState.value = _uiState.value.copy(currentMonth = _uiState.value.currentMonth.plusMonths(1))
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    fun addMedication(name: String, colorHex: Long) {
        if (name.isBlank()) return
        val nextId = (_uiState.value.medications.maxOfOrNull { it.id } ?: 0L) + 1
        val updated = _uiState.value.medications + Medication(id = nextId, name = name.trim(), colorHex = colorHex)
        save(_uiState.value.copy(medications = updated))
    }

    fun updateMedication(id: Long, name: String, colorHex: Long) {
        if (name.isBlank()) return
        val updated = _uiState.value.medications.map { medication ->
            if (medication.id == id) medication.copy(name = name.trim(), colorHex = colorHex) else medication
        }
        save(_uiState.value.copy(medications = updated))
    }

    fun deleteMedication(id: Long) {
        val updatedMedications = _uiState.value.medications.filterNot { it.id == id }
        val updatedRecords = _uiState.value.records.mapValues { (_, record) ->
            record.copy(medicationIds = record.medicationIds.filterNot { it == id })
        }
        save(_uiState.value.copy(medications = updatedMedications, records = updatedRecords))
    }

    fun toggleMedicationForSelectedDate(id: Long) {
        val dateKey = _uiState.value.selectedDate.storageKey()
        val current = _uiState.value.records[dateKey] ?: DayRecord(date = dateKey)
        val set = current.medicationIds.toMutableSet()
        if (!set.add(id)) {
            set.remove(id)
        }
        val updatedRecord = current.copy(medicationIds = set.toList().sorted())
        val updatedMap = _uiState.value.records.toMutableMap().apply {
            put(dateKey, updatedRecord)
        }
        save(_uiState.value.copy(records = updatedMap))
    }

    fun updateNoteForSelectedDate(note: String) {
        val dateKey = _uiState.value.selectedDate.storageKey()
        val current = _uiState.value.records[dateKey] ?: DayRecord(date = dateKey)
        val updatedMap = _uiState.value.records.toMutableMap().apply {
            put(dateKey, current.copy(note = note))
        }
        save(_uiState.value.copy(records = updatedMap))
    }

    private fun save(state: MedicationUiState) {
        _uiState.value = state
        repository.save(
            AppData(
                medications = state.medications,
                records = state.records.values.sortedBy { it.date }
            )
        )
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MedicationViewModel(MedicationRepository(context)) as T
        }
    }
}

data class MedicationUiState(
    val currentMonth: YearMonth,
    val selectedDate: LocalDate,
    val medications: List<Medication> = emptyList(),
    val records: Map<String, DayRecord> = emptyMap()
)
