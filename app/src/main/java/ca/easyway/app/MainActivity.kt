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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
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

private val canadianCitiesByRegion = mapOf(
    "British Columbia" to listOf("100 Mile House", "Abbotsford", "Alert Bay", "Anmore", "Armstrong", "Ashcroft", "Barriere", "Belcarra", "Bowen Island", "Burnaby", "Burns Lake", "Cache Creek", "Campbell River", "Canal Flats", "Castlegar", "Central Saanich", "Chase", "Chetwynd", "Chilliwack", "Clearwater", "Clinton", "Coldstream", "Colwood", "Comox", "Coquitlam", "Courtenay", "Cranbrook", "Creston", "Cumberland", "Daajing Giids", "Dawson Creek", "Delta", "Duncan", "Elkford", "Enderby", "Esquimalt", "Fernie", "Fort St. James", "Fort St. John", "Fraser Lake", "Fruitvale", "Gibsons", "Gold River", "Golden", "Grand Forks", "Granisle", "Greenwood", "Harrison Hot Springs", "Hazelton", "Highlands", "Hope", "Houston", "Hudson's Hope", "Invermere", "Kamloops", "Kaslo", "Kelowna", "Kent", "Keremeos", "Kimberley", "Kitimat", "Ladysmith", "Lake Country", "Lake Cowichan", "Langford", "Langley", "Township of Langley", "Lantzville", "Lillooet", "Lions Bay", "Logan Lake", "Lumby", "Lytton", "Mackenzie", "Maple Ridge", "Masset", "McBride", "Merritt", "Metchosin", "Midway", "Mission", "Montrose", "Nakusp", "Nanaimo", "Nelson", "New Denver", "New Hazelton", "New Westminster", "North Cowichan", "North Saanich", "North Vancouver", "District of North Vancouver", "Northern Rockies", "Oak Bay", "Oliver", "Osoyoos", "Parksville", "Peachland", "Pemberton", "Penticton", "Pitt Meadows", "Port Alberni", "Port Alice", "Port Clements", "Port Coquitlam", "Port Edward", "Port Hardy", "Port McNeill", "Port Moody", "Pouce Coupe", "Powell River", "Prince George", "Prince Rupert", "Princeton", "Qualicum Beach", "Quesnel", "Radium Hot Springs", "Revelstoke", "Richmond", "Rossland", "Saanich", "Salmo", "Salmon Arm", "Sayward", "Sechelt", "shíshálh Nation Government District", "Sicamous", "Sidney", "Silverton", "Slocan", "Smithers", "Sooke", "Spallumcheen", "Sparwood", "Squamish", "Stewart", "Summerland", "Sun Peaks", "Surrey", "Tahsis", "Taylor", "Telkwa", "Terrace", "Tofino", "Trail", "Tumbler Ridge", "Ucluelet", "Valemount", "Vancouver", "Vanderhoof", "Vernon", "Victoria", "View Royal", "Warfield", "Wells", "West Kelowna", "West Vancouver", "Whistler", "White Rock", "Williams Lake", "Zeballos"),
    "Alberta" to listOf("Calgary", "Edmonton", "Red Deer", "Lethbridge", "Medicine Hat", "Grande Prairie", "Fort McMurray", "Airdrie", "Banff", "Canmore"),
    "Saskatchewan" to listOf("Saskatoon", "Regina", "Prince Albert", "Moose Jaw", "Swift Current", "Yorkton", "North Battleford"),
    "Manitoba" to listOf("Winnipeg", "Brandon", "Steinbach", "Thompson", "Portage la Prairie", "Winkler", "Dauphin"),
    "Ontario" to listOf("Toronto", "Ottawa", "Mississauga", "Hamilton", "London", "Windsor", "Kingston", "Sudbury", "Thunder Bay", "Barrie", "Niagara Falls", "Kitchener", "Waterloo"),
    "Quebec" to listOf("Montreal", "Quebec City", "Laval", "Gatineau", "Sherbrooke", "Trois-Rivières", "Saguenay", "Rimouski"),
    "New Brunswick" to listOf("Moncton", "Saint John", "Fredericton", "Dieppe", "Miramichi", "Bathurst"),
    "Nova Scotia" to listOf("Halifax", "Sydney", "Dartmouth", "Truro", "New Glasgow", "Yarmouth"),
    "Prince Edward Island" to listOf("Charlottetown", "Summerside", "Stratford", "Montague"),
    "Newfoundland and Labrador" to listOf("St. John's", "Mount Pearl", "Corner Brook", "Gander", "Grand Falls-Windsor", "Happy Valley-Goose Bay"),
    "Yukon" to listOf("Whitehorse", "Dawson City", "Watson Lake"),
    "Northwest Territories" to listOf("Yellowknife", "Hay River", "Inuvik", "Fort Smith"),
    "Nunavut" to listOf("Iqaluit", "Rankin Inlet", "Arviat", "Cambridge Bay")
)
private val allCanadianCities = canadianCitiesByRegion.values.flatten().distinct()

// Initial British Columbia destination catalogue: settlements with a practical
// passenger-transport connection (scheduled bus, passenger rail, air service,
// or passenger ferry). Seasonal links are included for later date-aware filtering.
private val bcTransportCities = listOf(
    "Abbotsford", "Alert Bay", "Bella Bella", "Bella Coola", "Bowen Island",
    "Burnaby", "Campbell River", "Castlegar", "Chemainus", "Chilliwack",
    "Comox", "Courtenay", "Crofton", "Daajing Giids", "Dawson Creek",
    "Denman Island", "Duncan", "Fernie", "Fort St. John", "Galiano Island",
    "Gibsons", "Golden", "Hope", "Kamloops", "Kelowna", "Langford",
    "Ladysmith", "Mayne Island", "Masset", "McBride", "Nanaimo",
    "Nelson", "Parksville", "Pemberton", "Penticton", "Pender Island",
    "Pitt Meadows", "Port Alberni", "Port Hardy", "Port McNeill",
    "Powell River", "Prince George", "Prince Rupert", "Quesnel",
    "Revelstoke", "Richmond", "Salmon Arm", "Saturna Island", "Sechelt",
    "Sidney", "Smithers", "Sooke", "Squamish", "Stewart", "Summerland",
    "Terrace", "Tofino", "Ucluelet", "Vancouver", "Vanderhoof",
    "Valemount", "Vernon", "Victoria", "Whistler", "Williams Lake"
)

private fun citiesForRegion(region: String): List<String> =
    when {
        region == "ALL" || region.isBlank() -> allCanadianCities
        region == "British Columbia" -> bcTransportCities
        else -> canadianCitiesByRegion[region].orEmpty()
    }

// Choose a recognizable nearby city instead of filling the field with a street address.
private fun nearbyKnownCity(location: android.location.Location, region: String): String? {
    if (region == "British Columbia") {
        val candidates = listOf(
            Triple("Victoria", 48.4284, -123.3656),
            Triple("Sidney", 48.6500, -123.3980),
            Triple("Langford", 48.4480, -123.5040),
            Triple("Sooke", 48.3740, -123.7370)
        )
        val nearest = candidates.minByOrNull { (_, lat, lon) ->
            val target = android.location.Location("city").apply {
                latitude = lat
                longitude = lon
            }
            location.distanceTo(target)
        }
        if (nearest != null) {
            val target = android.location.Location("city").apply {
                latitude = nearest.second
                longitude = nearest.third
            }
            if (location.distanceTo(target) <= 45000f) return nearest.first
        }
    }
    return null
}

private fun cityNameFromAddress(address: android.location.Address?, region: String): String? {
    if (address == null) return null
    val known = citiesForRegion(region)
    val candidates = listOfNotNull(address.locality, address.subLocality, address.subAdminArea, address.featureName)
    return candidates.firstNotNullOfOrNull { candidate ->
        known.firstOrNull { it.equals(candidate.trim(), ignoreCase = true) }
    } ?: address.locality ?: address.subAdminArea
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
    var showOriginSuggestions by remember { mutableStateOf(false) }
    var destinationSuggestions by remember { mutableStateOf(emptyList<String>()) }
    var showDestinationSuggestions by remember { mutableStateOf(false) }
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
                            @Suppress("DEPRECATION")
                            val addresses = Geocoder(context, Locale.CANADA).getFromLocation(location.latitude, location.longitude, 5)
                            val address = addresses?.firstOrNull()
                            origin = nearbyKnownCity(location, selectedRegion)
                                ?: cityNameFromAddress(address, selectedRegion)
                                ?: "${location.latitude}, ${location.longitude}"
                            locationMessage = if (french) "Ville détectée : $origin" else "Detected city: $origin"
                        } catch (_: Exception) {
                            origin = nearbyKnownCity(location, selectedRegion) ?: "${location.latitude}, ${location.longitude}"
                            locationMessage = if (french) "Ville détectée : $origin" else "Detected city: $origin"
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
                            val addresses = Geocoder(context, Locale.CANADA).getFromLocation(location.latitude, location.longitude, 5)
                            val address = addresses?.firstOrNull()
                            origin = nearbyKnownCity(location, selectedRegion)
                                ?: cityNameFromAddress(address, selectedRegion)
                                ?: "${location.latitude}, ${location.longitude}"
                            locationMessage = if (french) "Ville détectée : $origin" else "Detected city: $origin"
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
    // If location permission was granted earlier, prefill the starting city when a region is selected.
    // Never request permission automatically and never overwrite a city the user entered manually.
    LaunchedEffect(selectedRegion) {
        val hasLocationPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (selectedRegion.isNotBlank() && origin.isBlank() && hasLocationPermission) {
            fetchCurrentLocation()
        }
    }
    var destination by remember { mutableStateOf("") }
    var destinationRegion by remember { mutableStateOf(selectedRegion) }
    var travelOutsideRegion by remember { mutableStateOf(false) }
    val regionOptions = listOf("British Columbia", "Alberta", "Saskatchewan", "Manitoba", "Ontario", "Quebec", "New Brunswick", "Nova Scotia", "Prince Edward Island", "Newfoundland and Labrador", "Yukon", "Northwest Territories", "Nunavut")
    LaunchedEffect(origin, selectedRegion) {
        val query = origin.trim()
        val priorityCities = if (selectedRegion == "British Columbia") listOf("Vancouver", "Victoria") else emptyList()
        val remainingCities = citiesForRegion(selectedRegion)
            .distinct()
            .filterNot { city -> priorityCities.any { it.equals(city, ignoreCase = true) } }
            .sortedBy { it.lowercase(Locale.ROOT) }
        originSuggestions = (priorityCities + remainingCities)
            .filter { query.isBlank() || it.startsWith(query, ignoreCase = true) }
            .filterNot { it.equals(query, ignoreCase = true) }
    }
    LaunchedEffect(destination, destinationRegion) {
        val query = destination.trim()
        val priorityCities = if (destinationRegion == "British Columbia") listOf("Victoria", "Vancouver") else emptyList()
        val remainingCities = citiesForRegion(destinationRegion)
            .distinct()
            .filterNot { city -> priorityCities.any { it.equals(city, ignoreCase = true) } }
            .sortedBy { it.lowercase(Locale.ROOT) }
        destinationSuggestions = (priorityCities + remainingCities)
            .filter { query.isBlank() || it.startsWith(query, ignoreCase = true) }
            .filterNot { it.equals(query, ignoreCase = true) }
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
            primary = androidx.compose.ui.graphics.Color(0xFF1557C0),
            secondary = androidx.compose.ui.graphics.Color(0xFF20C7B5),
            background = androidx.compose.ui.graphics.Color(0xFFF5F7F5),
            surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            surfaceContainer = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF1F4F1)
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
            Box(Modifier.fillMaxSize()) {
                if (selectedRegion == "British Columbia") {
                    Image(
                        painter = painterResource(id = R.drawable.bc_parliament_bg),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(Modifier.fillMaxSize().background(Color(0xFF0B4FA8).copy(alpha = 0.10f)))
                } else {
                    Box(Modifier.fillMaxSize().background(Color.White))
                }
                Column(
                    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (french) "Votre voyage. Toutes les options. Au même endroit." else "Your trip. Every option. One place.", style = MaterialTheme.typography.bodyLarge, color = if (selectedRegion == "British Columbia") Color.White else Color(0xFF172033))
                        TextButton(onClick = { showRegionDialog = true }, contentPadding = PaddingValues(0.dp)) {
                            Text((if (selectedRegion == "ALL") (if (french) "Tout le Canada" else "All Canada") else selectedRegion) + "  ⌄", color = Color(0xFF45E2D0), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    LanguageFlagButton(
                        language = language,
                        onClick = { language = if (french) "EN" else "FR" }
                    )
                }
                if (currentPage == 0) {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.76f))
                    ) {
                        Column(
                            Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                if (french) "Où voyagez-vous ?" else "Where are you travelling?",
                                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                color = Color(0xFF101828)
                            )
                            Text(
                                "Куда планируете поездку?",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                                color = Color(0xFF344054)
                            )
                            Card(
                                Modifier.fillMaxWidth().heightIn(min = 86.dp).clickable {
                                    travelOutsideRegion = false
                                    destinationRegion = selectedRegion
                                },
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (!travelOutsideRegion) 2.dp else 1.dp,
                                    if (!travelOutsideRegion) Color(0xFF1769D2) else Color(0xFFD5D9E0)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!travelOutsideRegion) Color(0xFFEAF2FF).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.68f)
                                )
                            ) {
                                Row(
                                    Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text("🏞️", style = MaterialTheme.typography.headlineLarge)
                                    Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            if (selectedRegion == "ALL") "Stay in Canada" else "Stay in $selectedRegion",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                            color = Color(0xFF101828)
                                        )
                                        Text(
                                            "Остаться в пределах ${if (selectedRegion == "ALL") "Канады" else if (selectedRegion == "British Columbia") "BC" else selectedRegion}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color(0xFF475467)
                                        )
                                    }
                                    RadioButton(
                                        selected = !travelOutsideRegion,
                                        onClick = { travelOutsideRegion = false; destinationRegion = selectedRegion },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1769D2))
                                    )
                                }
                            }
                            Card(
                                Modifier.fillMaxWidth().heightIn(min = 86.dp).clickable { travelOutsideRegion = true },
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (travelOutsideRegion) 2.dp else 1.dp,
                                    if (travelOutsideRegion) Color(0xFF1769D2) else Color(0xFFD5D9E0)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (travelOutsideRegion) Color(0xFFEAF2FF).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.68f)
                                )
                            ) {
                                Row(
                                    Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text("🇨🇦", style = MaterialTheme.typography.headlineLarge)
                                    Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            if (french) "Voyager dans une autre province" else "Travel to another province",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                            color = Color(0xFF101828)
                                        )
                                        Text(
                                            "Поехать в другую провинцию или территорию",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color(0xFF475467)
                                        )
                                    }
                                    RadioButton(
                                        selected = travelOutsideRegion,
                                        onClick = { travelOutsideRegion = true },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1769D2))
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    if (travelOutsideRegion || selectedRegion == "ALL") currentPage = 1
                                    else { destinationRegion = selectedRegion; currentPage = 2 }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1268D8))
                            ) {
                                Text(
                                    if (french) "Continuer  →" else "Continue  →",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }else if (currentPage == 1) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.90f))) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (french) "Choisir la province de destination" else "Choose destination province", style = MaterialTheme.typography.headlineSmall)
                            Text(if (french) "Sélectionnez votre destination" else "Select where you want to go", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            regionOptions.filter { it != selectedRegion }.forEach { region ->
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { destinationRegion = region; currentPage = 2 }.padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    ProvinceFlag(region)
                                    Text(if (french) when (region) {
                                        "British Columbia" -> "Colombie-Britannique"; "Quebec" -> "Québec"; "New Brunswick" -> "Nouveau-Brunswick"
                                        "Nova Scotia" -> "Nouvelle-Écosse"; "Prince Edward Island" -> "Île-du-Prince-Édouard"
                                        "Newfoundland and Labrador" -> "Terre-Neuve-et-Labrador"; "Northwest Territories" -> "Territoires du Nord-Ouest"; else -> region
                                    } else region, modifier = Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.titleMedium)
                                    RadioButton(selected = destinationRegion == region, onClick = { destinationRegion = region; currentPage = 2 })
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            TextButton(onClick = { currentPage = 0 }) { Text(if (french) "Retour" else "Back") }
                        }
                    }
                } else if (currentPage == 2) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.58f))
                    ) {
                        Column(Modifier.padding(horizontal = 22.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(if (french) "Planifiez votre trajet" else "Plan your trip", style = MaterialTheme.typography.titleLarge)
                            Box(Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { showOriginSuggestions = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                if (french) "Ville de départ" else "Starting city",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                origin.ifBlank { if (french) "Choisir une ville" else "Select a city" },
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (origin.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text("▾", style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                DropdownMenu(
                                    expanded = showOriginSuggestions,
                                    onDismissRequest = { showOriginSuggestions = false },
                                    modifier = Modifier.heightIn(max = 360.dp)
                                ) {
                                    originSuggestions.forEach { suggestion ->
                                        DropdownMenuItem(
                                            text = { Text(suggestion) },
                                            onClick = {
                                                origin = suggestion
                                                showOriginSuggestions = false
                                                cityValidationMessage = ""
                                            }
                                        )
                                    }
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
                            OutlinedTextField(value = destination, onValueChange = {
                                destination = it
                                cityValidationMessage = ""
                                showDestinationSuggestions = true
                            },
                                label = { Text(if (french) "Ville de destination" else "Destination city") },
                                placeholder = { Text(if (french) "e.g. Kelowna, BC" else "e.g. Kelowna, BC") },
                                modifier = Modifier.fillMaxWidth().onFocusChanged { focus ->
                                    if (focus.isFocused) showDestinationSuggestions = true
                                }, singleLine = true)
                            if (showDestinationSuggestions) {
                                Text(if (french) "Villes de la région" else "Cities in selected region",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                                    destinationSuggestions.forEach { suggestion ->
                                        TextButton(onClick = {
                                            destination = suggestion
                                            showDestinationSuggestions = false
                                        }, modifier = Modifier.fillMaxWidth()) {
                                            Text("📍  $suggestion", modifier = Modifier.fillMaxWidth())
                                        }
                                    }
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
                            val originRegionAtSearch = selectedRegion
                            val destinationRegionAtSearch = destinationRegion
                            Executors.newSingleThreadExecutor().execute {
                                fun findCanadianCity(query: String, regionToCheck: String): android.location.Address? {
                                    return try {
                                        @Suppress("DEPRECATION")
                                        val matches = Geocoder(context, Locale.CANADA).getFromLocationName("$query, Canada", 5)
                                        val wanted = query.substringBefore(",").trim().lowercase(Locale.ROOT)
                                        matches?.firstOrNull { address ->
                                            val countryOk = address.countryCode.equals("CA", ignoreCase = true)
                                            val provinceOk = regionToCheck == "ALL" ||
                                                address.adminArea.equals(regionToCheck, ignoreCase = true) ||
                                                (regionToCheck == "British Columbia" && address.adminArea.equals("BC", ignoreCase = true))
                                            val names = listOfNotNull(address.locality, address.subAdminArea, address.featureName)
                                                .map { it.substringBefore(",").trim().lowercase(Locale.ROOT) }
                                            countryOk && provinceOk && names.any { it == wanted }
                                        }
                                    } catch (_: Exception) { null }
                                }
                                val fromCity = findCanadianCity(fromText, originRegionAtSearch)
                                val toCity = findCanadianCity(toText, destinationRegionAtSearch)
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
                                            currentPage = 3
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
                        TextButton(onClick = { currentPage = 2 }) { Text(if (french) "Retour" else "Back") }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (french) "Votre trajet" else "Your trip", style = MaterialTheme.typography.titleMedium)
                            Text("$origin, $selectedRegion → $destination, $destinationRegion")
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
                        if ("Bus" in selectedTransport &&
                            selectedRegion == "British Columbia" &&
                            destinationRegion == "British Columbia"
                        ) {
                            VictoriaTransitSearchCard(
                                origin = origin,
                                destination = destination,
                                travelDate = travelDate,
                                french = french
                            )
                        } else {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(if (french) "Recherche de transport" else "Transport search", style = MaterialTheme.typography.titleMedium)
                                    Text(if (french) "Les horaires d’autobus Victoria sont disponibles pour les trajets locaux en Colombie-Britannique. D’autres réseaux seront ajoutés progressivement." else "Victoria bus schedules are available for local trips within British Columbia. Other transport networks will be added progressively.")
                                    Text(if (french) "Aucun prix ni aucune disponibilité ne sont inventés." else "No prices or availability are invented.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
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
    }
    if (showRegionDialog) {
        val regions = listOf(
            "British Columbia", "Alberta", "Saskatchewan", "Manitoba", "Ontario", "Quebec",
            "New Brunswick", "Nova Scotia", "Prince Edward Island", "Newfoundland and Labrador",
            "Yukon", "Northwest Territories", "Nunavut"
        )
        AlertDialog(
            onDismissRequest = { if (selectedRegion.isNotBlank()) showRegionDialog = false },
            title = { Text(if (french) "Où vous trouvez-vous actuellement ?" else "Where are you located now?") },
            text = {
                Column(
                    Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(if (french) "Choisissez votre province ou territoire actuel" else "Choose your current province or territory",
                        style = MaterialTheme.typography.bodyMedium)
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
private fun ProvinceFlag(region: String) {
    Canvas(Modifier.size(width = 36.dp, height = 24.dp)) {
        val w = size.width
        val h = size.height
        val white = Color.White
        val red = Color(0xFFC8102E)
        val navy = Color(0xFF003876)
        val blue = Color(0xFF005EB8)
        val green = Color(0xFF006847)
        val gold = Color(0xFFFFD100)
        // Simplified, province-specific flag motifs sized for the province list.
        when (region) {
            "Quebec" -> {
                drawRect(Color(0xFF003DA5))
                val s = h * 0.19f
                listOf(Offset(w * .25f, h * .25f), Offset(w * .75f, h * .25f),
                    Offset(w * .25f, h * .75f), Offset(w * .75f, h * .75f)).forEach {
                    drawCircle(white, s, it)
                    drawRect(Color(0xFF003DA5), Offset(it.x - s * .18f, it.y - s * .1f), androidx.compose.ui.geometry.Size(s * .36f, s * 1.2f))
                }
            }
            "Alberta" -> {
                drawRect(navy)
                drawRect(white, Offset(w * .37f, h * .20f), androidx.compose.ui.geometry.Size(w * .26f, h * .60f))
                drawRect(Color(0xFFB5121B), Offset(w * .40f, h * .48f), androidx.compose.ui.geometry.Size(w * .20f, h * .25f))
                drawRect(gold, Offset(w * .42f, h * .27f), androidx.compose.ui.geometry.Size(w * .16f, h * .18f))
            }
            "Saskatchewan" -> {
                drawRect(gold)
                drawRect(green, Offset(0f, h * .52f), androidx.compose.ui.geometry.Size(w, h * .48f))
                drawCircle(Color(0xFFB5121B), h * .18f, Offset(w * .5f, h * .48f))
                drawCircle(white, h * .11f, Offset(w * .5f, h * .48f))
            }
            "Manitoba", "Ontario" -> {
                drawRect(red)
                drawRect(navy, Offset(0f, 0f), androidx.compose.ui.geometry.Size(w * .34f, h))
                drawRect(white, Offset(w * .34f, 0f), androidx.compose.ui.geometry.Size(w * .66f, h))
                drawRect(red, Offset(w * .34f, 0f), androidx.compose.ui.geometry.Size(w * .66f, h))
                drawRect(white, Offset(w * .49f, h * .22f), androidx.compose.ui.geometry.Size(w * .24f, h * .56f))
                drawRect(if (region == "Ontario") red else green, Offset(w * .54f, h * .38f), androidx.compose.ui.geometry.Size(w * .14f, h * .22f))
            }
            "New Brunswick" -> {
                drawRect(white)
                drawRect(blue, Offset(0f, 0f), androidx.compose.ui.geometry.Size(w, h * .34f))
                drawRect(gold, Offset(0f, h * .34f), androidx.compose.ui.geometry.Size(w, h * .66f))
                drawCircle(Color(0xFF8B4513), h * .14f, Offset(w * .5f, h * .60f))
                drawLine(Color(0xFF8B4513), Offset(w * .34f, h * .76f), Offset(w * .66f, h * .76f), h * .06f)
            }
            "Nova Scotia" -> {
                drawRect(white)
                drawLine(blue, Offset(0f, 0f), Offset(w, h), h * .30f)
                drawLine(blue, Offset(w, 0f), Offset(0f, h), h * .30f)
                drawRect(white, Offset(w * .38f, 0f), androidx.compose.ui.geometry.Size(w * .24f, h))
                drawRect(white, Offset(0f, h * .38f), androidx.compose.ui.geometry.Size(w, h * .24f))
                drawRect(red, Offset(w * .43f, h * .20f), androidx.compose.ui.geometry.Size(w * .14f, h * .60f))
                drawRect(red, Offset(w * .20f, h * .43f), androidx.compose.ui.geometry.Size(w * .60f, h * .14f))
            }
            "Prince Edward Island" -> {
                drawRect(white)
                drawRect(red, Offset(0f, 0f), androidx.compose.ui.geometry.Size(w, h * .34f))
                drawRect(red, Offset(0f, h * .78f), androidx.compose.ui.geometry.Size(w, h * .22f))
                drawCircle(gold, h * .14f, Offset(w * .5f, h * .53f))
                drawLine(green, Offset(w * .5f, h * .43f), Offset(w * .5f, h * .78f), h * .05f)
            }
            "Newfoundland and Labrador" -> {
                drawRect(blue)
                drawLine(white, Offset(w * .12f, h * .12f), Offset(w * .78f, h * .88f), h * .28f)
                drawLine(white, Offset(w * .12f, h * .88f), Offset(w * .78f, h * .12f), h * .28f)
                drawLine(red, Offset(w * .22f, h * .12f), Offset(w * .88f, h * .78f), h * .10f)
                drawLine(red, Offset(w * .22f, h * .78f), Offset(w * .88f, h * .12f), h * .10f)
            }
            "Yukon" -> {
                drawRect(white)
                drawRect(green, Offset(0f, 0f), androidx.compose.ui.geometry.Size(w / 3f, h))
                drawRect(blue, Offset(w * 2f / 3f, 0f), androidx.compose.ui.geometry.Size(w / 3f, h))
                drawCircle(gold, h * .19f, Offset(w * .5f, h * .5f))
            }
            "Northwest Territories" -> {
                drawRect(blue)
                drawRect(white, Offset(w * .36f, 0f), androidx.compose.ui.geometry.Size(w * .28f, h))
                drawCircle(gold, h * .15f, Offset(w * .5f, h * .5f))
            }
            "Nunavut" -> {
                drawRect(Color(0xFFC8102E))
                drawRect(gold, Offset(w * .50f, 0f), androidx.compose.ui.geometry.Size(w * .50f, h))
                drawCircle(white, h * .22f, Offset(w * .5f, h * .5f))
            }
            else -> drawRect(blue)
        }
        drawRect(Color(0xFF222222), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.7.dp.toPx()))
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
        Box(Modifier.size(width = 30.dp, height = 20.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (french) {
                    // French national flag: blue, white and red vertical bands.
                    drawRect(Color(0xFF0055A4), size = androidx.compose.ui.geometry.Size(w / 3f, h))
                    drawRect(Color.White, topLeft = Offset(w / 3f, 0f), size = androidx.compose.ui.geometry.Size(w / 3f, h))
                    drawRect(Color(0xFFEF4135), topLeft = Offset(2f * w / 3f, 0f), size = androidx.compose.ui.geometry.Size(w / 3f, h))
                } else {
                    // United Kingdom flag (Union Jack).
                    drawRect(Color(0xFF012169))
                    drawLine(Color.White, Offset(0f, 0f), Offset(w, h), h * 0.34f)
                    drawLine(Color.White, Offset(w, 0f), Offset(0f, h), h * 0.34f)
                    drawLine(Color(0xFFC8102E), Offset(0f, 0f), Offset(w, h), h * 0.13f)
                    drawLine(Color(0xFFC8102E), Offset(w, 0f), Offset(0f, h), h * 0.13f)
                    drawRect(Color.White, topLeft = Offset(w * 0.39f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.22f, h))
                    drawRect(Color.White, topLeft = Offset(0f, h * 0.36f), size = androidx.compose.ui.geometry.Size(w, h * 0.28f))
                    drawRect(Color(0xFFC8102E), topLeft = Offset(w * 0.44f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.12f, h))
                    drawRect(Color(0xFFC8102E), topLeft = Offset(0f, h * 0.43f), size = androidx.compose.ui.geometry.Size(w, h * 0.14f))
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
