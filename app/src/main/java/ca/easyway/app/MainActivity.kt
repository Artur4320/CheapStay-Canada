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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private fun suggestedTransportModes(
    origin: String,
    destination: String,
    originRegion: String,
    destinationRegion: String
): List<String> {
    val islandPlaces = listOf(
        "saturna", "pender", "mayne", "gulf island", "salt spring",
        "galiano", "gabriola", "denman", "hornby", "cortes", "quadra"
    )
    val from = origin.lowercase(Locale.ROOT)
    val to = destination.lowercase(Locale.ROOT)
    val involvesIsland = islandPlaces.any { from.contains(it) || to.contains(it) }
    val victoriaToVancouver = (from.contains("victoria") && to.contains("vancouver")) ||
        (from.contains("vancouver") && to.contains("victoria"))
    return when {
        originRegion != destinationRegion -> listOf("Flight", "Bus", "Car")
        involvesIsland || victoriaToVancouver -> listOf("Ferry", "Bus", "Car") +
            if (victoriaToVancouver) listOf("Flight") else emptyList()
        else -> listOf("Bus", "Car")
    }
}

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
    var rooms by remember { mutableIntStateOf(1) }
    var showSearchInfo by remember { mutableStateOf(false) }
    val selectedTransport = remember { mutableStateListOf<String>() }
    val routeTransportOptions = remember(origin, destination, selectedRegion, destinationRegion) {
        suggestedTransportModes(origin, destination, selectedRegion, destinationRegion)
    }
    val accommodationOptions = listOf("Hotel", "Motel", "Hostel", "Camping", "Cabin")
    val selectedAccommodation = remember { mutableStateListOf("Hotel", "Motel", "Hostel") }

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
                        Box(Modifier.fillMaxWidth()) {
                            if (selectedRegion == "British Columbia") {
                                BritishColumbiaCardArtwork(Modifier.matchParentSize())
                            }
                            Column(
                                Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                            Text(
                                if (french) "Où voyagez-vous ?" else "Where are you travelling?",
                                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                color = Color(0xFF101828)
                            )
                            Card(
                                Modifier.fillMaxWidth().height(86.dp).clickable {
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
                                    }
                                    RadioButton(
                                        selected = !travelOutsideRegion,
                                        onClick = { travelOutsideRegion = false; destinationRegion = selectedRegion },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1769D2))
                                    )
                                }
                            }
                            Card(
                                Modifier.fillMaxWidth().height(86.dp).clickable { travelOutsideRegion = true },
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
                                            if (french) "Voyager dans une autre province" else "Explore Canada",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                            color = Color(0xFF101828)
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
                                placeholder = { },
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
                                            selectedTransport.clear()
                                            selectedTransport.addAll(suggestedTransportModes(
                                                origin = fromCity.locality ?: fromText,
                                                destination = toCity.locality ?: toText,
                                                originRegion = originRegionAtSearch,
                                                destinationRegion = destinationRegionAtSearch
                                            ))
                                            showSearchInfo = false
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
                } else if (currentPage == 3) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(if (french) "Options de voyage" else "Travel options", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { currentPage = 2 }) { Text(if (french) "Retour" else "Back") }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.58f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (french) "Votre trajet" else "Your trip", style = MaterialTheme.typography.titleMedium)
                            Text("$origin, ${if (selectedRegion == "British Columbia") "BC" else selectedRegion} → $destination, ${if (destinationRegion == "British Columbia") "BC" else destinationRegion}")
                            Text(if (french) "Départ : $travelDate" else "Departure: $travelDate",
                                color = Color.Black)
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.58f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (french) "Carte du trajet" else "Route map",
                                style = MaterialTheme.typography.titleMedium)
                            OpenStreetMapRoutePreview(
                                origin = origin,
                                destination = destination,
                                french = french,
                                modifier = Modifier.fillMaxWidth().height(260.dp)
                            )
                            Text(if (french) "© OpenStreetMap contributors · Itinéraire indicatif" else "© OpenStreetMap contributors · Indicative route",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.58f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (french) "Options adaptées à votre trajet" else "Suggested transport options",
                                style = MaterialTheme.typography.titleLarge)
                            Text(if (french) "Seuls les moyens pertinents pour cet itinéraire sont affichés." else "Only transport types that may fit this route are shown.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                routeTransportOptions.forEach { mode ->
                                    FilterChip(selected = mode in selectedTransport, onClick = {
                                        if (mode in selectedTransport) selectedTransport.remove(mode)
                                        else selectedTransport.add(mode)
                                    }, label = { Text(mode) })
                                }
                            }
                            Text(if (french) "Prix" else "Price", style = MaterialTheme.typography.titleMedium)
                            Text(if (french) "Aucune limite · Afficher tous les prix disponibles" else "No limit · Show all available prices",
                                color = Color.Black)
                            Text(if (french) "Le coût total et le prix de chaque étape seront affichés." else "Total trip cost and each leg's price will be shown.")
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
                            VictoriaTransitSearchCard(origin = origin, destination = destination,
                                travelDate = travelDate, french = french)
                        } else {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(if (french) "Recherche de transport" else "Transport search", style = MaterialTheme.typography.titleMedium)
                                    Text(if (french) "Les horaires d’autobus Victoria sont disponibles pour les trajets locaux en Colombie-Britannique. D’autres réseaux seront ajoutés progressivement." else "Victoria bus schedules are available for local trips within British Columbia. Other transport networks will be added progressively.")
                                    Text(if (french) "Aucun prix ni aucune disponibilité ne sont inventés." else "No prices or availability are invented.",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = { currentPage = 4 }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (french) "Trouver un hébergement" else "Find accommodation")
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(if (french) "Hébergement" else "Accommodation", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { currentPage = 3 }) { Text(if (french) "Retour" else "Back") }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(if (french) "Qui voyage ?" else "Who is travelling?", style = MaterialTheme.typography.titleLarge)
                            Text(if (french) "Voyageurs" else "Travellers", style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                OutlinedButton(onClick = { if (travellers > 1) travellers-- }, enabled = travellers > 1) { Text("−") }
                                Text("$travellers")
                                OutlinedButton(onClick = { if (travellers < 20) travellers++ }, enabled = travellers < 20) { Text("+") }
                            }
                            HorizontalDivider()
                            Text(if (french) "Chambres" else "Rooms", style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                OutlinedButton(onClick = { if (rooms > 1) rooms-- }, enabled = rooms > 1) { Text("−") }
                                Text("$rooms")
                                OutlinedButton(onClick = { if (rooms < 10) rooms++ }, enabled = rooms < 10) { Text("+") }
                            }
                            HorizontalDivider()
                            Text(if (french) "Types d’hébergement" else "Accommodation types", style = MaterialTheme.typography.titleMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                accommodationOptions.forEach { type ->
                                    FilterChip(selected = type in selectedAccommodation, onClick = {
                                        if (type in selectedAccommodation) selectedAccommodation.remove(type)
                                        else selectedAccommodation.add(type)
                                    }, label = { Text(type) })
                                }
                            }
                            Text(if (french) "Les dates et les prix seront ajoutés à la recherche d’hébergement." else "Stay dates and prices will be set in the accommodation search.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(onClick = { currentPage = 3 }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (french) "Enregistrer" else "Save preferences")
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
        val regions = listOf("British Columbia", "Alberta", "Saskatchewan", "Manitoba", "Ontario", "Quebec", "New Brunswick", "Nova Scotia", "Prince Edward Island", "Newfoundland and Labrador", "Yukon", "Northwest Territories", "Nunavut")
        Dialog(onDismissRequest = { if (selectedRegion.isNotBlank()) showRegionDialog = false }) {
            Box(
                Modifier.fillMaxWidth().widthIn(max = 420.dp).aspectRatio(0.75f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.canada_province_frame),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.FillBounds
                )
                Column(
                    modifier = Modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp, top = 58.dp, bottom = 112.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        if (french) "Choisissez votre province" else "Choose your province",
                        color = Color(0xFF14263D),
                        fontSize = 15.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(3.dp))
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        regions.forEach { region ->
                            Box(
                                modifier = Modifier.fillMaxWidth(0.61f).height(28.dp).clickable {
                                    selectedRegion = region
                                    preferences.edit().putString("travel_region", selectedRegion).apply()
                                    showRegionDialog = false
                                },
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Text(
                                    if (french) when (region) {
                                        "British Columbia" -> "Colombie-Britannique"
                                        "Quebec" -> "Québec"
                                        "New Brunswick" -> "Nouveau-Brunswick"
                                        "Nova Scotia" -> "Nouvelle-Écosse"
                                        "Prince Edward Island" -> "Île-du-Prince-Édouard"
                                        "Newfoundland and Labrador" -> "Terre-Neuve-et-Labrador"
                                        "Northwest Territories" -> "Territoires du Nord-Ouest"
                                        else -> region
                                    } else region,
                                    color = Color(0xFF14263D),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
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




private data class RouteMapPoint(val latitude: Double, val longitude: Double)

private suspend fun geocodeMapPlace(place: String): RouteMapPoint? = withContext(Dispatchers.IO) {
    try {
        val query = URLEncoder.encode("$place, British Columbia, Canada", "UTF-8")
        val connection = (URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=$query")
            .openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("User-Agent", "EasyWayCanada/1.0 (Android travel planner)")
        }
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        val result = org.json.JSONArray(body)
        if (result.length() == 0) null else {
            val item = result.getJSONObject(0)
            RouteMapPoint(item.getDouble("lat"), item.getDouble("lon"))
        }
    } catch (_: Exception) { null }
}

private suspend fun fetchRoadRoute(start: RouteMapPoint, end: RouteMapPoint): List<GeoPoint> =
    withContext(Dispatchers.IO) {
        try {
            val url = "https://router.project-osrm.org/route/v1/driving/" +
                "${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=geojson"
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("User-Agent", "EasyWayCanada/1.0")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val routes = JSONObject(body).getJSONArray("routes")
            if (routes.length() == 0) emptyList() else {
                val coordinates = routes.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                (0 until coordinates.length()).map { index ->
                    val pair = coordinates.getJSONArray(index)
                    GeoPoint(pair.getDouble(1), pair.getDouble(0))
                }
            }
        } catch (_: Exception) { emptyList() }
    }

@Composable
private fun OpenStreetMapRoutePreview(
    origin: String,
    destination: String,
    french: Boolean,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var mapStatus by remember(origin, destination) {
        mutableStateOf(if (french) "Chargement de la carte…" else "Loading map…")
    }
    var routePoints by remember(origin, destination) { mutableStateOf(emptyList<GeoPoint>()) }
    var startPoint by remember(origin, destination) { mutableStateOf<RouteMapPoint?>(null) }
    var endPoint by remember(origin, destination) { mutableStateOf<RouteMapPoint?>(null) }

    LaunchedEffect(origin, destination) {
        mapStatus = if (french) "Recherche des lieux…" else "Finding locations…"
        val start = geocodeMapPlace(origin)
        kotlinx.coroutines.delay(1100)
        val end = geocodeMapPlace(destination)
        startPoint = start
        endPoint = end
        if (start == null || end == null) {
            mapStatus = if (french) "Impossible de trouver les deux lieux." else "Could not locate both places."
            routePoints = emptyList()
        } else {
            mapStatus = if (french) "Calcul de l’itinéraire…" else "Calculating road route…"
            routePoints = fetchRoadRoute(start, end)
            mapStatus = if (routePoints.isEmpty()) {
                if (french) "Carte chargée · tracé routier indisponible." else "Map loaded · road route unavailable."
            } else ""
        }
    }

    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = "EasyWayCanada/1.0"
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                controller.setZoom(8.0)
                controller.setCenter(GeoPoint(48.5, -123.5))
            }
        },
        update = { map ->
            map.overlays.clear()
            val start = startPoint
            val end = endPoint
            if (start != null && end != null) {
                if (routePoints.isNotEmpty()) {
                    val line = Polyline().apply {
                        setPoints(routePoints)
                        outlinePaint.color = android.graphics.Color.rgb(25, 105, 190)
                        outlinePaint.strokeWidth = 8f
                    }
                    map.overlays.add(line)
                }
                val startMarker = Marker(map).apply {
                    position = GeoPoint(start.latitude, start.longitude)
                    title = origin
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                val endMarker = Marker(map).apply {
                    position = GeoPoint(end.latitude, end.longitude)
                    title = destination
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(startMarker)
                map.overlays.add(endMarker)
                val boundsPoints = if (routePoints.isNotEmpty()) routePoints else listOf(
                    GeoPoint(start.latitude, start.longitude), GeoPoint(end.latitude, end.longitude)
                )
                map.post {
                    map.zoomToBoundingBox(org.osmdroid.util.BoundingBox.fromGeoPoints(boundsPoints), true, 48)
                }
            }
            map.invalidate()
        }
    )
    if (mapStatus.isNotBlank()) {
        Text(mapStatus, modifier = Modifier.padding(vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CanadaDialogArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val navy = Color(0xFF14263D)
        val red = Color(0xFF8F171D)
        val gold = Color(0xFFC49A54)
        val cream = Color(0xFFFFFCF7)
        val blue = Color(0xFF244B63)
        val w = size.width
        val h = size.height
        val stroke = 3.dp.toPx()
        fun point(x: Float, y: Float) = Offset(w * x, h * y)
        fun line(color: Color, x1: Float, y1: Float, x2: Float, y2: Float, width: Float = stroke) {
            drawLine(color, point(x1, y1), point(x2, y2), width)
        }
        fun pine(x: Float, y: Float, scale: Float = 1f) {
            val trunkTop = h * y
            val trunkBottom = trunkTop + 43.dp.toPx() * scale
            val xx = w * x
            drawLine(navy, Offset(xx, trunkTop), Offset(xx, trunkBottom), 2.dp.toPx() * scale)
            for (i in 0..2) {
                val yy = trunkTop + i * 11.dp.toPx() * scale
                val half = (10 - i * 2).dp.toPx() * scale
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(xx - half, yy + 12.dp.toPx() * scale)
                    lineTo(xx, yy)
                    lineTo(xx + half, yy + 12.dp.toPx() * scale)
                }
                drawPath(p, navy, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx() * scale))
            }
        }
        fun star(x: Float, y: Float, r: Float) {
            val c = point(x, y)
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(c.x, c.y - r); lineTo(c.x + r * .22f, c.y - r * .22f)
                lineTo(c.x + r, c.y); lineTo(c.x + r * .22f, c.y + r * .22f)
                lineTo(c.x, c.y + r); lineTo(c.x - r * .22f, c.y + r * .22f)
                lineTo(c.x - r, c.y); lineTo(c.x - r * .22f, c.y - r * .22f); close()
            }
            drawPath(p, cream)
        }
        fun maple(cx: Float, cy: Float, r: Float) {
            val c = point(cx, cy)
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(c.x, c.y - r)
                lineTo(c.x - r*.18f, c.y - r*.48f); lineTo(c.x-r*.55f,c.y-r*.62f)
                lineTo(c.x-r*.43f,c.y-r*.22f); lineTo(c.x-r*.88f,c.y-r*.08f)
                lineTo(c.x-r*.48f,c.y+r*.12f); lineTo(c.x-r*.58f,c.y+r*.48f)
                lineTo(c.x-r*.12f,c.y+r*.32f); lineTo(c.x-r*.08f,c.y+r*.78f)
                lineTo(c.x+r*.08f,c.y+r*.78f); lineTo(c.x+r*.12f,c.y+r*.32f)
                lineTo(c.x+r*.58f,c.y+r*.48f); lineTo(c.x+r*.48f,c.y+r*.12f)
                lineTo(c.x+r*.88f,c.y-r*.08f); lineTo(c.x+r*.43f,c.y-r*.22f)
                lineTo(c.x+r*.55f,c.y-r*.62f); lineTo(c.x+r*.18f,c.y-r*.48f); close()
            }
            drawPath(p, red)
            drawRect(red, Offset(c.x-r*.07f,c.y+r*.55f), androidx.compose.ui.geometry.Size(r*.14f,r*.42f))
        }
        fun mountainLine(y: Float, color: Color, width: Float) {
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(w*.08f,h*y); lineTo(w*.25f,h*(y-.07f)); lineTo(w*.34f,h*y)
                lineTo(w*.48f,h*(y-.09f)); lineTo(w*.61f,h*y); lineTo(w*.74f,h*(y-.07f))
                lineTo(w*.91f,h*y)
            }
            drawPath(p,color,style=androidx.compose.ui.graphics.drawscope.Stroke(width,cap=androidx.compose.ui.graphics.StrokeCap.Square))
        }
        fun animal(x: Float, y: Float, scale: Float, kind: String) {
            val left = w*x; val top = h*y
            val s = 38.dp.toPx()*scale
            val p = androidx.compose.ui.graphics.Path()
            when (kind) {
                "eagle" -> {
                    p.moveTo(left-s,top); p.cubicTo(left-s*.65f,top-s*.65f,left+s*.35f,top-s*.45f,left+s,top-s*.08f)
                    p.cubicTo(left+s*.52f,top+s*.05f,left+s*.1f,top+s*.38f,left-s*.55f,top+s*.32f)
                    p.cubicTo(left-s*.15f,top+s*.18f,left-s*.15f,top+s*.05f,left-s,top); p.close()
                }
                "wolf" -> {
                    p.moveTo(left-s,top+s*.1f); p.lineTo(left-s*.55f,top-s*.42f); p.lineTo(left-s*.18f,top-s*.28f)
                    p.lineTo(left+s*.12f,top-s*.62f); p.lineTo(left+s*.35f,top-s*.25f)
                    p.lineTo(left+s,top-s*.02f); p.lineTo(left+s*.42f,top+s*.42f); p.lineTo(left-s*.42f,top+s*.42f); p.close()
                }
                "whale" -> {
                    p.moveTo(left-s,top); p.cubicTo(left-s*.35f,top-s*.58f,left+s*.45f,top-s*.48f,left+s,top)
                    p.cubicTo(left+s*.45f,top+s*.48f,left-s*.35f,top+s*.42f,left-s,top)
                    p.moveTo(left+s*.65f,top-s*.05f); p.lineTo(left+s,top-s*.45f); p.lineTo(left+s*.92f,top+s*.02f); p.close()
                }
                else -> {
                    p.moveTo(left-s,top+s*.18f); p.cubicTo(left-s*.7f,top-s*.38f,left+s*.45f,top-s*.38f,left+s,top+s*.12f)
                    p.cubicTo(left+s*.55f,top+s*.52f,left-s*.5f,top+s*.52f,left-s,top+s*.18f)
                    p.moveTo(left-s*.1f,top-s*.18f); p.lineTo(left-s*.38f,top-s*.78f); p.lineTo(left-s*.2f,top-s*.35f)
                    p.moveTo(left+s*.1f,top-s*.18f); p.lineTo(left+s*.38f,top-s*.78f); p.lineTo(left+s*.2f,top-s*.35f)
                }
            }
            drawPath(p, navy)
            drawCircle(cream, 1.8.dp.toPx()*scale, Offset(left+s*.35f,top-s*.02f))
        }

        drawRoundRect(cream, cornerRadius=androidx.compose.ui.geometry.CornerRadius(30.dp.toPx()))
        drawRoundRect(navy, cornerRadius=androidx.compose.ui.geometry.CornerRadius(30.dp.toPx()),
            style=androidx.compose.ui.graphics.drawscope.Stroke(5.dp.toPx()))
        drawRoundRect(red, topLeft=Offset(7.dp.toPx(),7.dp.toPx()),
            size=androidx.compose.ui.geometry.Size(w-14.dp.toPx(),h-14.dp.toPx()),
            cornerRadius=androidx.compose.ui.geometry.CornerRadius(25.dp.toPx()),
            style=androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))

        // Layered red, navy and gold ornamental bands frame the content without entering it.
        val topWave = androidx.compose.ui.graphics.Path().apply {
            moveTo(w*.04f,h*.12f); cubicTo(w*.22f,h*.02f,w*.32f,h*.14f,w*.5f,h*.12f)
            cubicTo(w*.68f,h*.14f,w*.78f,h*.02f,w*.96f,h*.12f)
        }
        drawPath(topWave,red,style=androidx.compose.ui.graphics.drawscope.Stroke(5.dp.toPx()))
        val goldWave = androidx.compose.ui.graphics.Path().apply {
            moveTo(w*.05f,h*.145f); cubicTo(w*.24f,h*.07f,w*.34f,h*.18f,w*.5f,h*.15f)
            cubicTo(w*.66f,h*.18f,w*.76f,h*.07f,w*.95f,h*.145f)
        }
        drawPath(goldWave,gold,style=androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))

        maple(.5f,.075f,27.dp.toPx())
        mountainLine(.105f,navy,4.dp.toPx())
        mountainLine(.12f,gold,2.dp.toPx())
        star(.105f,.085f,10.dp.toPx()); star(.895f,.085f,10.dp.toPx())
        pine(.09f,.12f,.65f); pine(.14f,.13f,.55f); pine(.86f,.13f,.55f); pine(.91f,.12f,.65f)

        // Side wildlife emblems echo the reference illustration while leaving the button column clear.
        animal(.105f,.30f,.9f,"eagle")
        animal(.895f,.30f,.9f,"wolf")
        animal(.105f,.70f,.85f,"whale")
        animal(.895f,.70f,.9f,"moose")
        for (side in 0..1) {
            val x = if (side==0) .065f else .935f
            pine(x,.405f,.72f); pine(x,.455f,.55f)
            maple(x,.535f,17.dp.toPx())
            star(x,.625f,7.dp.toPx())
            val ribbon = androidx.compose.ui.graphics.Path().apply {
                moveTo(w*x,h*.78f); cubicTo(w*(if(side==0).02f else .98f),h*.84f,w*(if(side==0).16f else .84f),h*.87f,w*x,h*.92f)
            }
            drawPath(ribbon,red,style=androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx()))
            drawPath(ribbon,gold,style=androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        }

        // Bottom coastal waves, mountain peaks and compass.
        val bottom = androidx.compose.ui.graphics.Path().apply {
            moveTo(w*.04f,h*.94f); cubicTo(w*.2f,h*.99f,w*.3f,h*.9f,w*.42f,h*.94f)
            cubicTo(w*.5f,h*.98f,w*.58f,h*.9f,w*.7f,h*.94f); cubicTo(w*.82f,h*.99f,w*.9f,h*.98f,w*.96f,h*.94f)
        }
        drawPath(bottom,navy,style=androidx.compose.ui.graphics.drawscope.Stroke(5.dp.toPx()))
        mountainLine(.91f,navy,3.dp.toPx())
        val c=point(.5f,.925f); val r=20.dp.toPx()
        drawCircle(gold,r*1.45f,c,style=androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        drawCircle(red,r*1.2f,c,style=androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        val compass=androidx.compose.ui.graphics.Path().apply {
            moveTo(c.x,c.y-r); lineTo(c.x+r*.2f,c.y); lineTo(c.x,c.y+r); lineTo(c.x-r*.2f,c.y); close()
            moveTo(c.x-r,c.y); lineTo(c.x,c.y-r*.2f); lineTo(c.x+r,c.y); lineTo(c.x,c.y+r*.2f); close()
        }
        drawPath(compass,navy)
        line(gold,.08f,.945f,.32f,.945f,1.5.dp.toPx())
        line(gold,.68f,.945f,.92f,.945f,1.5.dp.toPx())
    }
}
@Composable
private fun BritishColumbiaCardArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val navy = Color(0xFF12384A).copy(alpha = 0.42f)
        val teal = Color(0xFF159B91).copy(alpha = 0.48f)
        val gold = Color(0xFFC58A32).copy(alpha = 0.55f)
        val sideInset = 13.dp.toPx()
        val stroke = 2.dp.toPx()

        // Delicate coastal-inspired flowing forms stay close to the card edges.
        // The central content area remains clear and fully readable.
        for (side in 0..1) {
            val x = if (side == 0) sideInset else size.width - sideInset
            val direction = if (side == 0) 1f else -1f
            val wave = androidx.compose.ui.graphics.Path().apply {
                moveTo(x, size.height * 0.16f)
                cubicTo(x + direction * 7.dp.toPx(), size.height * 0.22f,
                    x - direction * 5.dp.toPx(), size.height * 0.29f, x, size.height * 0.35f)
                cubicTo(x + direction * 6.dp.toPx(), size.height * 0.42f,
                    x - direction * 5.dp.toPx(), size.height * 0.49f, x, size.height * 0.56f)
                cubicTo(x + direction * 7.dp.toPx(), size.height * 0.64f,
                    x - direction * 4.dp.toPx(), size.height * 0.72f, x, size.height * 0.80f)
            }
            drawPath(wave, teal, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            val innerWave = androidx.compose.ui.graphics.Path().apply {
                moveTo(x - direction * 3.dp.toPx(), size.height * 0.23f)
                cubicTo(x - direction * 9.dp.toPx(), size.height * 0.28f,
                    x - direction * 8.dp.toPx(), size.height * 0.31f, x - direction * 2.dp.toPx(), size.height * 0.34f)
                moveTo(x - direction * 2.dp.toPx(), size.height * 0.62f)
                cubicTo(x - direction * 9.dp.toPx(), size.height * 0.67f,
                    x - direction * 8.dp.toPx(), size.height * 0.71f, x - direction * 2.dp.toPx(), size.height * 0.75f)
            }
            drawPath(innerWave, navy, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke * 0.85f))
            for (fraction in listOf(0.20f, 0.50f, 0.77f)) {
                drawCircle(gold, 2.dp.toPx(), Offset(x - direction * 2.dp.toPx(), size.height * fraction))
            }
        }
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
