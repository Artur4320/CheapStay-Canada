package ca.cheapstay.app

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CheapStayApp() }
    }
}

@Composable
fun CheapStayApp() {
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var budget by remember { mutableFloatStateOf(100f) }
    var transport by remember { mutableStateOf("Car") }
    var checkIn by remember { mutableStateOf("") }
    var checkOut by remember { mutableStateOf("") }
    var guests by remember { mutableIntStateOf(2) }
    var radius by remember { mutableFloatStateOf(25f) }
    val stayTypes = listOf("Hotel", "Motel", "Hostel", "Camping", "RV site", "Cabin")
    val selectedTypes = remember { mutableStateListOf("Hotel", "Motel", "Hostel", "Camping", "RV site", "Cabin") }

    fun defaultRadius(mode: String): Float = when (mode) {
        "Car" -> 25f
        "Bike" -> 10f
        "Walk" -> 5f
        else -> 5f
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("CheapStay Canada", style = MaterialTheme.typography.headlineMedium)
                Text("Find affordable places to stay along your route.")

                OutlinedTextField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text("Starting point") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destination") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Travel mode: $transport")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Car", "Bike", "Walk", "Transit").forEach { mode ->
                        val isSelected = transport == mode
                        Button(
                            onClick = {
                                transport = mode
                                radius = defaultRadius(mode)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        ) {
                            Text(mode)
                        }
                    }
                }

                Text("Stay dates")
                OutlinedTextField(
                    value = checkIn,
                    onValueChange = { checkIn = it },
                    label = { Text("Check-in (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = checkOut,
                    onValueChange = { checkOut = it },
                    label = { Text("Check-out (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Guests: $guests")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { if (guests > 1) guests-- }, enabled = guests > 1) {
                        Text("−")
                    }
                    OutlinedButton(onClick = { if (guests < 12) guests++ }, enabled = guests < 12) {
                        Text("+")
                    }
                }

                Text("Accommodation type")
                stayTypes.chunked(3).forEach { rowTypes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowTypes.forEach { type ->
                            FilterChip(
                                selected = type in selectedTypes,
                                onClick = {
                                    if (type in selectedTypes) selectedTypes.remove(type)
                                    else selectedTypes.add(type)
                                },
                                label = { Text(type) }
                            )
                        }
                    }
                }

                Text("Search radius: ${radius.toInt()} km from route")
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 1f..50f
                )
                Text("Default radius for $transport: ${defaultRadius(transport).toInt()} km. You can adjust it above.")

                Text("Maximum budget: $" + budget.toInt() + " CAD / night")
                Slider(value = budget, onValueChange = { budget = it }, valueRange = 20f..100f)

                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = origin.isNotBlank() &&
                        destination.isNotBlank() &&
                        selectedTypes.isNotEmpty()
                ) {
                    Text("Search stays")
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Your search results will appear here.")
                        Text("Search filters are saved in this screen. Live prices, availability and map search are not connected yet.")
                    }
                }
            }
        }
    }
}
