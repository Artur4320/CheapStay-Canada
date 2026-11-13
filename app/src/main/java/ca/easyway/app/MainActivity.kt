package ca.easyway.app

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
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
    var language by remember { mutableStateOf("EN") }
    val french = language == "FR"
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember { context.getSharedPreferences("easyway_settings", android.content.Context.MODE_PRIVATE) }
    var selectedRegion by remember { mutableStateOf(preferences.getString("travel_region", "") ?: "") }
    var showRegionDialog by remember { mutableStateOf(selectedRegion.isBlank()) }
    var origin by remember { mutableStateOf("") }
    var locationMessage by remember { mutableStateOf("") }
    val locationManager = remember { context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val provider = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER
                @Suppress("MissingPermission")
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null) {
                    val addresses = Geocoder(context, Locale.CANADA).getFromLocation(location.latitude, location.longitude, 1)
                    origin = addresses?.firstOrNull()?.getAddressLine(0) ?: "${location.latitude}, ${location.longitude}"
                    locationMessage = if (french) "Position ajoutée. Vérifiez l’adresse." else "Location added. Please check the address."
                } else locationMessage = if (french) "Position indisponible. Saisissez votre adresse manuellement." else "Location unavailable. Enter your address manually."
            } catch (_: Exception) { locationMessage = if (french) "Position indisponible. Saisissez votre adresse manuellement." else "Location unavailable. Enter your address manually." }
        } else locationMessage = if (french) "Autorisation refusée. Saisissez votre adresse manuellement." else "Permission denied. Enter your address manually."
    }
    var destination by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(0) }
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (french) "Votre voyage. Toutes les options. Au même endroit." else "Your trip. Every option. One place.", style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = { showRegionDialog = true }, contentPadding = PaddingValues(0.dp)) {
                            Text(if (selectedRegion == "ALL") (if (french) "Tout le Canada" else "All Canada") else selectedRegion)
                        }
                    }
                    LanguageFlagButton(
                        language = language,
                        onClick = { language = if (french) "EN" else "FR" }
                    )
                }
                if (currentPage == 0) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(if (french) "Planifiez votre trajet" else "Plan your trip", style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(value = origin, onValueChange = { origin = it },
                                label = { Text(if (french) "Ville de départ" else "Starting city") },
                                placeholder = { Text(if (french) "e.g. Victoria, BC" else "e.g. Victoria, BC") },
                                modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedButton(onClick = { locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (french) "Utiliser ma position" else "Use my location")
                            }
                            if (locationMessage.isNotBlank()) Text(locationMessage, style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(value = destination, onValueChange = { destination = it },
                                label = { Text(if (french) "Ville de destination" else "Destination city") },
                                placeholder = { Text(if (french) "e.g. Kelowna, BC" else "e.g. Kelowna, BC") },
                                modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (travelDate.isBlank()) { if (french) "Date de départ · Choisir" else "When · Choose departure date" } else { if (french) "Départ · $travelDate" else "Departure · $travelDate" })
                            }
                        }
                    }
                    Button(
                        onClick = { currentPage = 1 },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = origin.isNotBlank() && destination.isNotBlank() &&
                            origin.trim() != destination.trim() && travelDate.isNotBlank()
                    ) { Text(if (french) "Continuer" else "Continue") }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(if (french) "Options de voyage" else "Travel options", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { currentPage = 0 }) { Text(if (french) "Retour" else "Back") }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (french) "Votre trajet" else "Your trip", style = MaterialTheme.typography.titleMedium)
                            Text("$origin → $destination")
                            Text(if (french) "Départ : $travelDate" else "Departure: $travelDate",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider()
                            Text(if (french) "Voyageurs" else "Travellers", style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                OutlinedButton(onClick = { if (travellers > 1) travellers-- }, enabled = travellers > 1) { Text("−") }
                                Text("$travellers")
                                OutlinedButton(onClick = { if (travellers < 20) travellers++ }, enabled = travellers < 20) { Text("+") }
                            }
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (french) "Comparer les transports" else "Choose transport", style = MaterialTheme.typography.titleLarge)
                            Text(if (french) "Sélectionnez les moyens de transport à comparer." else "Select the transport options to compare.")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                transportOptions.forEach { mode ->
                                    FilterChip(selected = mode in selectedTransport, onClick = {
                                        if (mode in selectedTransport) selectedTransport.remove(mode)
                                        else selectedTransport.add(mode)
                                    }, label = { Text(if (!french) mode else when (mode) { "Bus" -> "Autobus"; "Ferry" -> "Traversier"; "Train" -> "Train"; "Flight" -> "Avion"; "Car" -> "Voiture"; else -> "Transport en commun" }) })
                                }
                            }
                            Text(if (french) "Budget" else "Price", style = MaterialTheme.typography.titleMedium)
                            Text(if (french) "Aucune limite · Afficher tous les prix disponibles" else "No limit · Show all available prices",
                                color = MaterialTheme.colorScheme.primary)
                            Text(if (french) "Le coût total et le prix de chaque étape seront affichés." else "Total trip cost and each leg's price will be shown.")
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (french) "Hébergement en cours de route" else "Overnight stays", style = MaterialTheme.typography.titleLarge)
                            Text(if (french) "Aucun hébergement ne sera ajouté sans votre demande." else "No overnight stops are added unless you ask for them.")
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Checkbox(checked = includeOvernight, onCheckedChange = { includeOvernight = it })
                                Text(if (french) "Ajouter un hébergement" else "Include an overnight stay")
                            }
                            if (includeOvernight) {
                                Text(if (french) "Types d’hébergement" else "Accommodation types")
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    overnightOptions.forEach { type ->
                                        FilterChip(selected = type in selectedOvernight, onClick = {
                                            if (type in selectedOvernight) selectedOvernight.remove(type)
                                            else selectedOvernight.add(type)
                                        }, label = { Text(if (!french) type else when (type) { "Hotel" -> "Hôtel"; "Motel" -> "Motel"; "Hostel" -> "Auberge de jeunesse"; "Camping" -> "Camping"; else -> "Chalet" }) })
                                    }
                                }
                            }
                        }
                    }
                    Button(onClick = { showSearchInfo = true }, modifier = Modifier.fillMaxWidth(),
                        enabled = selectedTransport.isNotEmpty()) {
                        Text(if (french) "Comparer les itinéraires" else "Compare routes")
                    }
                    if (showSearchInfo) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (french) "Les détails de votre voyage sont prêts" else "Your trip details are ready", style = MaterialTheme.typography.titleMedium)
                                Text(if (french) "Les horaires, tarifs et résultats de transport en direct ne sont pas encore connectés. Ils seront ajoutés à la prochaine étape du développement." else "Live transport schedules, fares and route results are not connected yet. They will be added in the next development stage.")
                                Text(if (french) "Aucun prix ni aucune disponibilité ne sont estimés ou présentés comme confirmés." else "No prices or availability are being estimated or shown as confirmed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Text(if (french) "Le Canada d’abord · D’autres destinations suivront" else "Canada first · More destinations can come later",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (showRegionDialog) {
        val regions = listOf(
            "British Columbia", "Alberta", "Saskatchewan", "Manitoba", "Ontario", "Quebec",
            "New Brunswick", "Nova Scotia", "Prince Edward Island", "Newfoundland and Labrador",
            "Yukon", "Northwest Territories", "Nunavut"
        )
        AlertDialog(
            onDismissRequest = { if (selectedRegion.isNotBlank()) showRegionDialog = false },
            title = { Text(if (french) "Où souhaitez-vous voyager ?" else "Where would you like to travel?") },
            text = {
                Column(
                    Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(if (french) "Choisissez une région de voyage" else "Choose your travel area",
                        style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(
                        onClick = {
                            selectedRegion = "ALL"
                            preferences.edit().putString("travel_region", selectedRegion).apply()
                            showRegionDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🇨🇦  " + if (french) "Tout le Canada" else "All Canada") }
                    HorizontalDivider()
                    regions.forEach { region ->
                        OutlinedButton(
                            onClick = {
                                selectedRegion = region
                                preferences.edit().putString("travel_region", selectedRegion).apply()
                                showRegionDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (french) when (region) {
                                "British Columbia" -> "Colombie-Britannique"
                                "Quebec" -> "Québec"
                                "New Brunswick" -> "Nouveau-Brunswick"
                                "Nova Scotia" -> "Nouvelle-Écosse"
                                "Prince Edward Island" -> "Île-du-Prince-Édouard"
                                "Newfoundland and Labrador" -> "Terre-Neuve-et-Labrador"
                                "Northwest Territories" -> "Territoires du Nord-Ouest"
                                else -> region
                            } else region)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                if (selectedRegion.isNotBlank()) {
                    TextButton(onClick = { showRegionDialog = false }) {
                        Text(if (french) "Fermer" else "Close")
                    }
                }
            }
        )
    }
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateToMillis(travelDate))
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { travelDate = millisToDate(it) }
                showDatePicker = false
            }) { Text(if (french) "Terminé" else "Done") }
        }, dismissButton = {
            TextButton(onClick = { showDatePicker = false }) { Text(if (french) "Annuler" else "Cancel") }
        }) { DatePicker(state = pickerState) }
    }
}


@Composable
private fun LanguageFlagButton(language: String, onClick: () -> Unit) {
    val french = language == "FR"
    Box(
        modifier = Modifier
            .size(width = 76.dp, height = 46.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (french) {
                // Quebec flag: fleurdelise blue field, white cross and four fleurs-de-lis.
                drawRect(Color(0xFF003399))
                drawRect(Color.White, topLeft = Offset(w * 0.43f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.14f, h))
                drawRect(Color.White, topLeft = Offset(0f, h * 0.42f), size = androidx.compose.ui.geometry.Size(w, h * 0.16f))
                val centers = listOf(
                    Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.25f),
                    Offset(w * 0.25f, h * 0.75f), Offset(w * 0.75f, h * 0.75f)
                )
                centers.forEach { c ->
                    drawCircle(Color.White, radius = h * 0.075f, center = Offset(c.x, c.y - h * 0.06f))
                    drawCircle(Color.White, radius = h * 0.055f, center = Offset(c.x - h * 0.055f, c.y))
                    drawCircle(Color.White, radius = h * 0.055f, center = Offset(c.x + h * 0.055f, c.y))
                    drawLine(Color.White, Offset(c.x, c.y + h * 0.015f), Offset(c.x, c.y + h * 0.12f), strokeWidth = h * 0.035f)
                }
            } else {
                // British Columbia flag: Union Jack, golden crown/sun and blue-white waves.
                drawRect(Color.White)
                val unionH = h * 0.48f
                drawRect(Color(0xFF012169), size = androidx.compose.ui.geometry.Size(w * 0.48f, unionH))
                drawLine(Color.White, Offset(0f, 0f), Offset(w * 0.48f, unionH), strokeWidth = h * 0.16f)
                drawLine(Color.White, Offset(w * 0.48f, 0f), Offset(0f, unionH), strokeWidth = h * 0.16f)
                drawLine(Color(0xFFC8102E), Offset(0f, 0f), Offset(w * 0.48f, unionH), strokeWidth = h * 0.055f)
                drawLine(Color(0xFFC8102E), Offset(w * 0.48f, 0f), Offset(0f, unionH), strokeWidth = h * 0.055f)
                drawRect(Color.White, topLeft = Offset(w * 0.19f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.10f, unionH))
                drawRect(Color.White, topLeft = Offset(0f, unionH * 0.36f), size = androidx.compose.ui.geometry.Size(w * 0.48f, unionH * 0.28f))
                drawRect(Color(0xFFC8102E), topLeft = Offset(w * 0.22f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.04f, unionH))
                drawRect(Color(0xFFC8102E), topLeft = Offset(0f, unionH * 0.44f), size = androidx.compose.ui.geometry.Size(w * 0.48f, unionH * 0.12f))
                drawCircle(Color(0xFFFFC72C), radius = h * 0.12f, center = Offset(w * 0.72f, h * 0.48f))
                val waveTop = h * 0.60f
                for (i in 0..4) {
                    drawRect(if (i % 2 == 0) Color(0xFF003DA5) else Color.White,
                        topLeft = Offset(0f, waveTop + i * h * 0.08f),
                        size = androidx.compose.ui.geometry.Size(w, h * 0.08f))
                }
            }
        }
        Text(
            language,
            modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        )
    }
}
