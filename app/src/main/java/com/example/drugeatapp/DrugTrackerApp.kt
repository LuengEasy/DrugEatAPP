package com.example.drugeatapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PresetColors = listOf(
    0xFFE53935,
    0xFF1E88E5,
    0xFF43A047,
    0xFFFDD835,
    0xFF8E24AA,
    0xFFFF7043
)

@Composable
fun DrugTrackerApp(viewModel: MedicationViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    MonthHeader(
                        monthText = uiState.currentMonth.format(DateTimeFormatter.ofPattern("yyyy年 M月", Locale.CHINA)),
                        onPrevious = viewModel::previousMonth,
                        onNext = viewModel::nextMonth
                    )
                }
                item {
                    CalendarGrid(
                        currentMonth = uiState.currentMonth,
                        selectedDate = uiState.selectedDate,
                        records = uiState.records,
                        medications = uiState.medications,
                        onSelectDate = viewModel::selectDate
                    )
                }
                item {
                    MedicationEditor(uiState = uiState, onAddMedication = viewModel::addMedication)
                }
                item {
                    DailyRecordEditor(
                        uiState = uiState,
                        onToggleMedication = viewModel::toggleMedicationForSelectedDate,
                        onUpdateNote = viewModel::updateNoteForSelectedDate
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(monthText: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onPrevious) { Text("上个月") }
        Text(text = monthText, style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onNext) { Text("下个月") }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: java.time.YearMonth,
    selectedDate: LocalDate,
    records: Map<String, DayRecord>,
    medications: List<Medication>,
    onSelectDate: (LocalDate) -> Unit
) {
    val firstDay = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val leadingEmpty = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val days = buildList<LocalDate?> {
        repeat(leadingEmpty) { add(null) }
        repeat(daysInMonth) { add(currentMonth.atDay(it + 1)) }
    }

    val medicationMap = medications.associateBy { it.id }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(it, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(280.dp), userScrollEnabled = false) {
            items(days) { day ->
                if (day == null) {
                    Box(modifier = Modifier.size(44.dp))
                } else {
                    val selected = day == selectedDate
                    val record = records[day.storageKey()]
                    Card(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(44.dp)
                            .clickable { onSelectDate(day) }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(top = 3.dp)) {
                            Text(
                                text = "${day.dayOfMonth}",
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.Center) {
                                record?.medicationIds?.take(3)?.forEach { id ->
                                    val color = medicationMap[id]?.asColor() ?: Color.LightGray
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 1.dp)
                                            .size(6.dp)
                                            .background(color, CircleShape)
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

@Composable
private fun MedicationEditor(uiState: MedicationUiState, onAddMedication: (String, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(PresetColors.first()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("添加药物", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("药物名称") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetColors.forEach { colorHex ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(colorHex), CircleShape)
                            .clickable { selectedColor = colorHex }
                            .padding(if (selectedColor == colorHex) 2.dp else 0.dp)
                    )
                }
            }
            Button(onClick = {
                onAddMedication(name, selectedColor)
                name = ""
            }) {
                Text("保存药物")
            }
            Divider()
            uiState.medications.forEach { med ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(10.dp).background(med.asColor(), CircleShape))
                    Spacer(Modifier.size(8.dp))
                    Text("${med.name}")
                }
            }
        }
    }
}

@Composable
private fun DailyRecordEditor(
    uiState: MedicationUiState,
    onToggleMedication: (Long) -> Unit,
    onUpdateNote: (String) -> Unit
) {
    val selectedKey = uiState.selectedDate.storageKey()
    val selectedRecord = uiState.records[selectedKey] ?: DayRecord(selectedKey)
    var noteText by remember(selectedKey) { mutableStateOf(selectedRecord.note) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${uiState.selectedDate} 服药记录", style = MaterialTheme.typography.titleMedium)
            if (uiState.medications.isEmpty()) {
                Text("请先添加药物")
            } else {
                uiState.medications.forEach { med ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = selectedRecord.medicationIds.contains(med.id),
                            onCheckedChange = { onToggleMedication(med.id) }
                        )
                        Box(modifier = Modifier.size(10.dp).background(med.asColor(), CircleShape))
                        Spacer(Modifier.size(8.dp))
                        Text(med.name)
                    }
                }
            }
            OutlinedTextField(
                value = noteText,
                onValueChange = {
                    noteText = it
                    onUpdateNote(it)
                },
                label = { Text("备注：为什么服药、身体情况") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
