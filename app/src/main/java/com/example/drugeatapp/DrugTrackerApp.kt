package com.example.drugeatapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val PresetColors = listOf(
    0xFFE53935,
    0xFF1E88E5,
    0xFF43A047,
    0xFFFDD835,
    0xFF8E24AA,
    0xFFFF7043
)

private enum class AppScreen { Main, MedicationSettings }

@Composable
fun DrugTrackerApp(viewModel: MedicationViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(AppScreen.Main) }

    MaterialTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Text("菜单", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                    Text(
                        text = "日历主页",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentScreen = AppScreen.Main
                                scope.launch { drawerState.close() }
                            }
                            .padding(16.dp)
                    )
                    Text(
                        text = "药物名称与颜色设置",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentScreen = AppScreen.MedicationSettings
                                scope.launch { drawerState.close() }
                            }
                            .padding(16.dp)
                    )
                }
            }
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "打开菜单")
                        }
                        Spacer(Modifier.size(8.dp))
                        Text(
                            if (currentScreen == AppScreen.Main) "服药日历" else "药物设置",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    if (currentScreen == AppScreen.Main) {
                        MainScreen(
                            uiState = uiState,
                            onPrevious = viewModel::previousMonth,
                            onNext = viewModel::nextMonth,
                            onSelectDate = viewModel::selectDate,
                            onToggleMedication = viewModel::toggleMedicationForSelectedDate,
                            onUpdateNote = viewModel::updateNoteForSelectedDate
                        )
                    } else {
                        MedicationSettingsScreen(
                            uiState = uiState,
                            onAddMedication = viewModel::addMedication,
                            onUpdateMedication = viewModel::updateMedication,
                            onDeleteMedication = viewModel::deleteMedication
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainScreen(
    uiState: MedicationUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToggleMedication: (Long) -> Unit,
    onUpdateNote: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            MonthHeader(
                monthText = uiState.currentMonth.format(DateTimeFormatter.ofPattern("yyyy年 M月", Locale.CHINA)),
                onPrevious = onPrevious,
                onNext = onNext
            )
        }
        item {
            CalendarGrid(
                currentMonth = uiState.currentMonth,
                selectedDate = uiState.selectedDate,
                records = uiState.records,
                medications = uiState.medications,
                onSelectDate = onSelectDate
            )
        }
        item {
            DailyRecordEditor(
                uiState = uiState,
                onToggleMedication = onToggleMedication,
                onUpdateNote = onUpdateNote
            )
        }
    }
}

@Composable
private fun MedicationSettingsScreen(
    uiState: MedicationUiState,
    onAddMedication: (String, Long) -> Unit,
    onUpdateMedication: (Long, String, Long) -> Unit,
    onDeleteMedication: (Long) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var newColor by remember { mutableStateOf(PresetColors.first()) }
    var pendingDeleteMedication by remember { mutableStateOf<Medication?>(null) }

    pendingDeleteMedication?.let { medication ->
        AlertDialog(
            onDismissRequest = { pendingDeleteMedication = null },
            title = { Text("确认删除") },
            text = { Text("确定删除药物「${medication.name}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMedication(medication.id)
                    pendingDeleteMedication = null
                }) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteMedication = null }) {
                    Text("取消")
                }
            }
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("新增药物", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("药物名称") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ColorSelector(selectedColor = newColor, onSelectColor = { newColor = it })
                    Button(onClick = {
                        onAddMedication(newName, newColor)
                        newName = ""
                    }) {
                        Text("添加药物")
                    }
                }
            }
        }

        items(uiState.medications, key = { it.id }) { med ->
            var editName by remember(med.id) { mutableStateOf(med.name) }
            var editColor by remember(med.id) { mutableStateOf(med.colorHex) }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("编辑：${med.name}", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ColorSelector(selectedColor = editColor, onSelectColor = { editColor = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onUpdateMedication(med.id, editName, editColor) }) {
                            Text("保存修改")
                        }
                        Button(onClick = { pendingDeleteMedication = med }) {
                            Text("删除")
                        }
                    }
                }
            }
        }

        items(uiState.medications, key = { it.id }) { med ->
            var editName by remember(med.id) { mutableStateOf(med.name) }
            var editColor by remember(med.id) { mutableStateOf(med.colorHex) }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("编辑：${med.name}", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ColorSelector(selectedColor = editColor, onSelectColor = { editColor = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onUpdateMedication(med.id, editName, editColor) }) {
                            Text("保存修改")
                        }
                        Button(onClick = { pendingDeleteMedication = med }) {
                            Text("删除")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSelector(selectedColor: Long, onSelectColor: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PresetColors.forEach { colorHex ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(colorHex), CircleShape)
                    .clickable { onSelectColor(colorHex) }
                    .padding(if (selectedColor == colorHex) 2.dp else 0.dp)
            )
        }
    }
}

@Composable
private fun ColorSelector(selectedColor: Long, onSelectColor: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PresetColors.forEach { colorHex ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(colorHex), CircleShape)
                    .clickable { onSelectColor(colorHex) }
                    .padding(if (selectedColor == colorHex) 2.dp else 0.dp)
            )
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
    val today = LocalDate.now()
    val medicationMap = medications.associateBy { it.id }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(it, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(280.dp), userScrollEnabled = false) {
            gridItems(days) { day ->
                if (day == null) {
                    Box(modifier = Modifier.size(44.dp))
                } else {
                    val selected = day == selectedDate
                    val isToday = day == today
                    val record = records[day.storageKey()]
                    Card(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(44.dp)
                            .border(
                                width = if (isToday) 2.dp else 0.dp,
                                color = if (isToday) Color.Red else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
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
                Text("请先在菜单中添加药物")
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
            Divider()
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
