package ca.easyway.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EasyWayApp() }
    }
}

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private fun dateToMillis(date: String): Long? = try {
    LocalDate.parse(date, dateFormatter).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
} catch (_: Exception) { null }
private fun millisToDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().format(dateFormatter)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EasyWayApp() {
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var travelDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var travellers by remember { mutableIntStateOf(1) }
    var includeOvernight by remember { mutableStateOf(false) }
    var showSearchInfo by remember { mutableStateOf(false) }
    val transportOptions = listOf("Bus", "Ferry", "Train", "Flight", "Car", "Local transit")
    val selectedTransport = remember { mutableStateListOf(*transportOptions.toTypedArray()) }
    val overnightOptions = listOf("Hotel", "Motel", "Hostel", "Camping", "Cabin")
    val selectedOvernight = remember { mutableStateListOf("Hotel", "Motel", "Hostel") }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF176B5B),
            secondary = androidx.compose.ui.graphics.Color(0xFFDA8A38),
            background = androidx.compose.ui.graphics.Color(0xFFF5F7F5),
            surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            surfaceContainer = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF1F4F1)
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("EasyWay", style = MaterialTheme.typography.headlineLarge)
                    Text("Your trip. Every option. One place.", style = MaterialTheme.typography.bodyLarge)
                    Text("Plan journeys across Canada", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Plan your route", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(value = origin, onValueChange = { origin = it },
                            label = { Text("From") }, placeholder = { Text("e.g. Langford, BC") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = destination, onValueChange = { destination = it },
                            label = { Text("To") }, placeholder = { Text("e.g. Kelowna, BC") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (travelDate.isBlank()) "Departure date · Choose date" else "Departure · $travelDate")
                        }
                        HorizontalDivider()
                        Text("Travellers", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            OutlinedButton(onClick = { if (travellers > 1) travellers-- }, enabled = travellers > 1) { Text("−") }
                            Text("$travellers ${if (travellers == 1) "traveller" else "travellers"}",
                                style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(onClick = { if (travellers < 20) travellers++ }, enabled = travellers < 20) { Text("+") }
                            Spacer(Modifier.weight(1f))
                            Text(if (travellers == 1) "Solo trip" else "Group trip",
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Compare transport", style = MaterialTheme.typography.titleLarge)
                        Text("Choose what to include. EasyWay will compare all selected options.")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            transportOptions.forEach { mode ->
                                FilterChip(selected = mode in selectedTransport, onClick = {
                                    if (mode in selectedTransport) selectedTransport.remove(mode)
                                    else selectedTransport.add(mode)
                                }, label = { Text(mode) })
                            }
                        }
                        Text("Budget", style = MaterialTheme.typography.titleMedium)
                        Text("No limit · Show all available prices", color = MaterialTheme.colorScheme.primary)
                        Text("Compare total trip cost, each travel leg, transfers and waiting time.")
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Overnight stays", style = MaterialTheme.typography.titleLarge)
                        Text("No overnight stops are added unless you ask for them.")
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(checked = includeOvernight, onCheckedChange = { includeOvernight = it })
                            Text("Include an overnight stay")
                        }
                        if (includeOvernight) {
                            Text("Accommodation types")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                overnightOptions.forEach { type ->
                                    FilterChip(selected = type in selectedOvernight, onClick = {
                                        if (type in selectedOvernight) selectedOvernight.remove(type)
                                        else selectedOvernight.add(type)
                                    }, label = { Text(type) })
                                }
                            }
                        }
                    }
                }
                Button(onClick = { showSearchInfo = true }, modifier = Modifier.fillMaxWidth(),
                    enabled = origin.isNotBlank() && destination.isNotBlank() &&
                        origin.trim() != destination.trim() && selectedTransport.isNotEmpty()) {
                    Text("Compare all routes")
                }
                if (showSearchInfo) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Your trip details are ready", style = MaterialTheme.typography.titleMedium)
                            Text("Live transport schedules, fares and route results are not connected yet. They will be added in the next development stage.")
                            Text("No prices or availability are being estimated or shown as confirmed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text("Canada first · More destinations can come later",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateToMillis(travelDate))
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { travelDate = millisToDate(it) }
                showDatePicker = false
            }) { Text("Done") }
        }, dismissButton = {
            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
        }) { DatePicker(state = pickerState) }
    }
}
