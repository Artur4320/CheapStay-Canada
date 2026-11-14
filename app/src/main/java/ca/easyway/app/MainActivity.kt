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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper
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
    var originSuggestions by remember { mutableStateOf(emptyList<String>()) }
    var destinationSuggestions by remember { mutableStateOf(emptyList<String>()) }
    var cityValidationMessage by remember { mutableStateOf("") }
    var isValidatingCities by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf("") }
    val locationManager = remember { context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager }
    val fetchCurrentLocation: () -> Unit = {
        locationMessage = if (french) "Recherche de votre position…" else "Finding your location…"
        try {
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> null
            }
            if (provider == null) {
                locationMessage = if (french) "Activez la localisation et réessayez." else "Turn on Location services and try again."
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                locationManager.getCurrentLocation(provider, null, context.mainExecutor) { location ->
                    if (location != null) {
                        try {
                            val addresses = Geocoder(context, Locale.CANADA).getFromLocation(location.latitude, location.longitude, 1)
                            origin = addresses?.firstOrNull()?.getAddressLine(0) ?: "${location.latitude}, ${location.longitude}"
                            locationMessage = if (french) "Position ajoutée. Vérifiez l’adresse." else "Location added. Please check the address."
                        } catch (_: Exception) {
                            origin = "${location.latitude}, ${location.longitude}"
                            locationMessage = if (french) "Coordonnées ajoutées. Vérifiez l’adresse." else "Coordinates added. Please check the address."
                        }
                    } else {
                        locationMessage = if (french) "Position introuvable. Saisissez votre ville manuellement." else "Could not get a location fix. Enter your city manually."
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                locationManager.requestSingleUpdate(provider, object : android.location.LocationListener {
                    override fun onLocationChanged(location: android.location.Location) {
                        try {
                            @Suppress("DEPRECATION")
                            val addresses = Geocoder(context, Locale.CANADA).getFromLocation(location.latitude, location.longitude, 1)
                            origin = addresses?.firstOrNull()?.getAddressLine(0) ?: "${location.latitude}, ${location.longitude}"
                            locationMessage = if (french) "Position ajoutée. Vérifiez l’adresse." else "Location added. Please check the address."
                        } catch (_: Exception) {
                            origin = "${location.latitude}, ${location.longitude}"
                            locationMessage = if (french) "Coordonnées ajoutées. Vérifiez l’adresse." else "Coordinates added. Please check the address."
                        }
                    }
                    override fun onProviderDisabled(providerName: String) {
                        locationMessage = if (french) "Activez la localisation et réessayez." else "Turn on Location services and try again."
                    }
                    override fun onProviderEnabled(providerName: String) {}
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(providerName: String?, status: Int, extras: android.os.Bundle?) {}
                }, android.os.Looper.getMainLooper())
            }
        } catch (_: SecurityException) {
            locationMessage = if (french) "Autorisation refusée. Saisissez votre ville manuellement." else "Location permission denied. Enter your city manually."
        } catch (_: Exception) {
            locationMessage = if (french) "Position indisponible. Saisissez votre ville manuellement." else "Location unavailable. Enter your city manually."
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation()
        } else {
            locationMessage = if (french) "Autorisation refusée. Saisissez votre ville manuellement." else "Permission denied. Enter your city manually."
        }
    }
    var destination by remember { mutableStateOf("") }
    LaunchedEffect(origin, selectedRegion) {
        originSuggestions = emptyList()
        val query = origin.trim()
        if (query.length >= 2) {
            delay(400)
            originSuggestions = withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    val results = Geocoder(context, Locale.CANADA).getFromLocationName("$query, Canada", 8)
                    results.orEmpty().filter { address ->
                        address.countryCode.equals("CA", true) &&
                            (selectedRegion == "ALL" || address.adminArea.equals(selectedRegion, true) ||
                                (selectedRegion == "British Columbia" && address.adminArea.equals("BC", true)))
                    }.mapNotNull { it.locality ?: it.subAdminArea }
                        .distinctBy { it.lowercase(Locale.ROOT) }.take(5)
                } catch (_: Exception) { emptyList() }
            }
        }
    }
    LaunchedEffect(destination, selectedRegion) {
        destinationSuggestions = emptyList()
        val query = destination.trim()
        if (query.length >= 2) {
            delay(400)
            destinationSuggestions = withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    val results = Geocoder(context, Locale.CANADA).getFromLocationName("$query, Canada", 8)
                    results.orEmpty().filter { address ->
                        address.countryCode.equals("CA", true) &&
                            (selectedRegion == "ALL" || address.adminArea.equals(selectedRegion, true) ||
                                (selectedRegion == "British Columbia" && address.adminArea.equals("BC", true)))
                    }.mapNotNull { it.locality ?: it.subAdminArea }
                        .distinctBy { it.lowercase(Locale.ROOT) }.take(5)
                } catch (_: Exception) { emptyList() }
            }
        }
    }
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
                            OutlinedTextField(value = origin, onValueChange = { origin = it; cityValidationMessage = "" },
                                label = { Text(if (french) "Ville de départ" else "Starting city") },
                                placeholder = { Text(if (french) "e.g. Victoria, BC" else "e.g. Victoria, BC") },
                                modifier = Modifier.fillMaxWidth(), singleLine = true)
                            originSuggestions.forEach { suggestion ->
                                TextButton(onClick = { origin = suggestion; originSuggestions = emptyList() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("📍  $suggestion", modifier = Modifier.fillMaxWidth())
                                }
                            }
                            OutlinedButton(onClick = {
                                val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (fineGranted || coarseGranted) fetchCurrentLocation()
                                else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (french) "Utiliser ma position" else "Use my location")
                            }
                            if (locationMessage.isNotBlank()) Text(locationMessage, style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(value = destination, onValueChange = { destination = it; cityValidationMessage = "" },
                                label = { Text(if (french) "Ville de destination" else "Destination city") },
                                placeholder = { Text(if (french) "e.g. Kelowna, BC" else "e.g. Kelowna, BC") },
                                modifier = Modifier.fillMaxWidth(), singleLine = true)
                            destinationSuggestions.forEach { suggestion ->
                                TextButton(onClick = { destination = suggestion; destinationSuggestions = emptyList() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("📍  $suggestion", modifier = Modifier.fillMaxWidth())
                                }
                            }
                            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (travelDate.isBlank()) { if (french) "Date de départ · Choisir" else "When · Choose departure date" } else { if (french) "Départ · $travelDate" else "Departure · $travelDate" })
                            }
                        }
                    }
                    Button(
                        onClick = {
                            cityValidationMessage = ""
                            isValidatingCities = true
                            val fromText = origin.trim()
                            val toText = destination.trim()
                            val regionAtSearch = selectedRegion
                            Executors.newSingleThreadExecutor().execute {
                                fun findCanadianCity(query: String): android.location.Address? {
                                    return try {
                                        @Suppress("DEPRECATION")
                                        val matches = Geocoder(context, Locale.CANADA).getFromLocationName("$query, Canada", 5)
                                        val wanted = query.substringBefore(",").trim().lowercase(Locale.ROOT)
                                        matches?.firstOrNull { address ->
                                            val countryOk = address.countryCode.equals("CA", ignoreCase = true)
                                            val provinceOk = regionAtSearch == "ALL" ||
                                                address.adminArea.equals(regionAtSearch, ignoreCase = true) ||
                                                (regionAtSearch == "British Columbia" && address.adminArea.equals("BC", ignoreCase = true))
                                            val names = listOfNotNull(address.locality, address.subAdminArea, address.featureName)
                                                .map { it.substringBefore(",").trim().lowercase(Locale.ROOT) }
                                            countryOk && provinceOk && names.any { it == wanted }
                                        }
                                    } catch (_: Exception) { null }
                                }
                                val fromCity = findCanadianCity(fromText)
                                val toCity = findCanadianCity(toText)
                                Handler(Looper.getMainLooper()).post {
                                    isValidatingCities = false
                                    when {
                                        fromCity == null && toCity == null ->
                                            cityValidationMessage = if (french) "Aucune des deux villes n’a été trouvée dans la région choisie. Vérifiez les noms." else "Neither city was found in the selected region. Check the names."
                                        fromCity == null ->
                                            cityValidationMessage = if (french) "Ville de départ introuvable dans la région choisie." else "Starting city not found in the selected region."
                                        toCity == null ->
                                            cityValidationMessage = if (french) "Ville de destination introuvable dans la région choisie." else "Destination city not found in the selected region."
                                        fromCity.locality.equals(toCity.locality, ignoreCase = true) &&
                                            fromCity.adminArea.equals(toCity.adminArea, ignoreCase = true) ->
                                            cityValidationMessage = if (french) "Choisissez deux villes différentes." else "Please choose two different cities."
                                        else -> {
                                            origin = fromCity.locality ?: fromText
                                            destination = toCity.locality ?: toText
                                            currentPage = 1
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isValidatingCities && origin.isNotBlank() && destination.isNotBlank() &&
                            origin.trim() != destination.trim() && travelDate.isNotBlank()
                    ) { Text(if (isValidatingCities) { if (french) "Vérification…" else "Checking…" } else { if (french) "Continuer" else "Continue" }) }
                    if (cityValidationMessage.isNotBlank()) {
                        Text(cityValidationMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
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
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(width = 25.dp, height = 17.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (french) {
                // Quebec flag: royal-blue field, white cross, four white fleur-de-lis.
                drawRect(Color(0xFF003399))
                drawRect(Color.White, topLeft = Offset(w * .43f, 0f), size = androidx.compose.ui.geometry.Size(w * .14f, h))
                drawRect(Color.White, topLeft = Offset(0f, h * .42f), size = androidx.compose.ui.geometry.Size(w, h * .16f))
                fun fleur(cx: Float, cy: Float, unit: Float) {
                    val p = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx, cy - 4.4f * unit)
                        cubicTo(cx - 1.0f * unit, cy - 3.2f * unit, cx - 1.1f * unit, cy - 1.8f * unit, cx - .55f * unit, cy - .65f * unit)
                        cubicTo(cx - 2.5f * unit, cy - 2.0f * unit, cx - 4.0f * unit, cy - 1.5f * unit, cx - 4.0f * unit, cy - .1f * unit)
                        cubicTo(cx - 4.0f * unit, cy + 1.1f * unit, cx - 2.6f * unit, cy + 1.5f * unit, cx - 1.25f * unit, cy + .7f * unit)
                        lineTo(cx - 1.25f * unit, cy + 1.6f * unit)
                        lineTo(cx + 1.25f * unit, cy + 1.6f * unit)
                        lineTo(cx + 1.25f * unit, cy + .7f * unit)
                        cubicTo(cx + 2.6f * unit, cy + 1.5f * unit, cx + 4.0f * unit, cy + 1.1f * unit, cx + 4.0f * unit, cy - .1f * unit)
                        cubicTo(cx + 4.0f * unit, cy - 1.5f * unit, cx + 2.5f * unit, cy - 2.0f * unit, cx + .55f * unit, cy - .65f * unit)
                        cubicTo(cx + 1.1f * unit, cy - 1.8f * unit, cx + 1.0f * unit, cy - 3.2f * unit, cx, cy - 4.4f * unit)
                        close()
                        moveTo(cx - .65f * unit, cy + 1.5f * unit)
                        lineTo(cx + .65f * unit, cy + 1.5f * unit)
                        lineTo(cx + .45f * unit, cy + 3.1f * unit)
                        lineTo(cx, cy + 3.7f * unit)
                        lineTo(cx - .45f * unit, cy + 3.1f * unit)
                        close()
                    }
                    drawPath(p, Color.White)
                }
                fleur(w * .25f, h * .25f, h * .095f)
                fleur(w * .75f, h * .25f, h * .095f)
                fleur(w * .25f, h * .75f, h * .095f)
                fleur(w * .75f, h * .75f, h * .095f)
            } else {
                // British Columbia flag: Union Jack, crowned sun, and blue-white waves.
                drawRect(Color(0xFF003DA5))
                val unionW = w * .50f
                val unionH = h * .48f
                drawRect(Color(0xFF012169), size = androidx.compose.ui.geometry.Size(unionW, unionH))
                drawLine(Color.White, Offset(0f, 0f), Offset(unionW, unionH), h * .22f)
                drawLine(Color.White, Offset(unionW, 0f), Offset(0f, unionH), h * .22f)
                drawLine(Color(0xFFC8102E), Offset(0f, 0f), Offset(unionW, unionH), h * .075f)
                drawLine(Color(0xFFC8102E), Offset(unionW, 0f), Offset(0f, unionH), h * .075f)
                drawRect(Color.White, topLeft = Offset(unionW * .40f, 0f), size = androidx.compose.ui.geometry.Size(unionW * .20f, unionH))
                drawRect(Color.White, topLeft = Offset(0f, unionH * .40f), size = androidx.compose.ui.geometry.Size(unionW, unionH * .20f))
                drawRect(Color(0xFFC8102E), topLeft = Offset(unionW * .46f, 0f), size = androidx.compose.ui.geometry.Size(unionW * .08f, unionH))
                drawRect(Color(0xFFC8102E), topLeft = Offset(0f, unionH * .46f), size = androidx.compose.ui.geometry.Size(unionW, unionH * .08f))

                // White and blue horizontal waves.
                val waveY = h * .52f
                val bands = listOf(Color.White, Color(0xFF003DA5), Color.White, Color(0xFF003DA5), Color.White)
                bands.forEachIndexed { i, color ->
                    drawRect(color, topLeft = Offset(0f, waveY + i * h * .096f),
                        size = androidx.compose.ui.geometry.Size(w, h * .096f))
                }
                // Golden rising sun and pointed rays at the lower edge.
                val sunX = w * .73f
                val sunBaseY = h * 1.16f
                for (i in 0..10) {
                    val angle = Math.PI * (1.08 + i * .084)
                    val inner = h * .28f
                    val outer = h * if (i % 2 == 0) .72f else .57f
                    val x1 = sunX + kotlin.math.cos(angle).toFloat() * inner
                    val y1 = sunBaseY + kotlin.math.sin(angle).toFloat() * inner
                    val x2 = sunX + kotlin.math.cos(angle).toFloat() * outer
                    val y2 = sunBaseY + kotlin.math.sin(angle).toFloat() * outer
                    drawLine(Color(0xFFFFC72C), Offset(x1, y1), Offset(x2, y2), h * .085f)
                }
                drawCircle(Color(0xFFFFC72C), radius = h * .36f, center = Offset(sunX, sunBaseY))
                // Small gold crown above the Union Jack.
                val crown = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * .61f, h * .40f)
                    lineTo(w * .64f, h * .18f)
                    lineTo(w * .68f, h * .34f)
                    lineTo(w * .72f, h * .12f)
                    lineTo(w * .76f, h * .34f)
                    lineTo(w * .80f, h * .18f)
                    lineTo(w * .83f, h * .40f)
                    close()
                }
                drawPath(crown, Color(0xFFFFC72C))
            }
        }
        Text(
            language,
            modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Black.copy(alpha = 0.58f))
                .padding(horizontal = 2.dp, vertical = 0.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
            )
        )
        }
    }
}
