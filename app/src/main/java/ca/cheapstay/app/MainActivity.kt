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

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("CheapStay Canada", style = MaterialTheme.typography.headlineMedium)
                Text("Find affordable places to stay along your route.")
                OutlinedTextField(origin, { origin = it }, label = { Text("Starting point") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(destination, { destination = it }, label = { Text("Destination") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Travel mode: $transport")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Car", "Bike", "Walk", "Transit").forEach { mode ->
                        val isSelected = transport == mode
                        Button(
                            onClick = { transport = mode },
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
                Text("Maximum budget: $" + budget.toInt() + " CAD / night")
                Slider(value = budget, onValueChange = { budget = it }, valueRange = 20f..100f)
                Button(onClick = { }, modifier = Modifier.fillMaxWidth(), enabled = origin.isNotBlank() && destination.isNotBlank()) {
                    Text("Search stays")
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Your search results will appear here.")
                        Text("Live prices, availability and map search are not connected yet.")
                    }
                }
            }
        }
    }
}
