package ca.easyway.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
internal fun VictoriaTransitSearchCard(
    origin: String,
    destination: String,
    travelDate: String,
    french: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cowichanCities = setOf("Duncan", "Ladysmith", "Chemainus", "Lake Cowichan", "Mill Bay", "Shawnigan Lake", "Cobble Hill")
    val cityCoordinates = remember {
        mapOf(
            "Victoria" to (48.4284 to -123.3656),
            "Saanich" to (48.4840 to -123.3810),
            "Langford" to (48.4480 to -123.5040),
            "Sidney" to (48.6500 to -123.3980),
            "Esquimalt" to (48.4320 to -123.4140),
            "Oak Bay" to (48.4260 to -123.3030),
            "Colwood" to (48.4230 to -123.4850),
            "View Royal" to (48.4510 to -123.4300),
            "Duncan" to (48.7787 to -123.7079),
            "Ladysmith" to (48.9900 to -123.8160),
            "Chemainus" to (48.9290 to -123.7190),
            "Lake Cowichan" to (48.8240 to -124.0540),
            "Mill Bay" to (48.6500 to -123.5620),
            "Shawnigan Lake" to (48.6520 to -123.6260),
            "Cobble Hill" to (48.6860 to -123.6020)
        )
    }
    val cowichanFeed = origin in cowichanCities || destination in cowichanCities
    val operatorId = if (cowichanFeed) 10 else 48
    val systemTitle = if (cowichanFeed) "Cowichan Valley" else "Victoria"
    val repository = remember(context, operatorId) { VictoriaGtfsRepository(context, operatorId) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var originStops by remember { mutableStateOf(emptyList<VictoriaGtfsRepository.NearbyStop>()) }
    var destinationStops by remember { mutableStateOf(emptyList<VictoriaGtfsRepository.NearbyStop>()) }
    var selectedOrigin by remember { mutableStateOf<VictoriaGtfsRepository.NearbyStop?>(null) }
    var selectedDestination by remember { mutableStateOf<VictoriaGtfsRepository.NearbyStop?>(null) }
    var departures by remember { mutableStateOf(emptyList<VictoriaGtfsRepository.DirectDeparture>()) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (french) "Horaires d’autobus BC Transit · Victoria" else "BC Transit bus schedules · Victoria",
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = {
                    val from = cityCoordinates.entries.firstOrNull { it.key.equals(origin, true) }?.value
                    val to = cityCoordinates.entries.firstOrNull { it.key.equals(destination, true) }?.value
                    originStops = emptyList()
                    destinationStops = emptyList()
                    selectedOrigin = null
                    selectedDestination = null
                    departures = emptyList()
                    if (from == null || to == null) {
                        message = if (french)
                            "La recherche est actuellement configurée pour Victoria et les communautés voisines indiquées."
                        else "Local search currently supports Victoria and the listed nearby Greater Victoria communities."
                    } else {
                        loading = true
                        message = if (french) "Téléchargement des horaires…" else "Downloading timetable…"
                        scope.launch {
                            val result = repository.ensureImported()
                            if (result.isSuccess) {
                                originStops = repository.searchStopsNear(from.first, from.second)
                                destinationStops = repository.searchStopsNear(to.first, to.second)
                                message = if (originStops.isEmpty() || destinationStops.isEmpty())
                                    (if (french) "Aucun arrêt trouvé près d’une des villes." else "No stops found near one of the cities.")
                                else (if (french) "Choisissez les arrêts de départ et d’arrivée." else "Choose your origin and destination stops.")
                            } else {
                                message = if (french) "Échec du téléchargement. Vérifiez votre connexion." else "Download failed. Check your connection."
                            }
                            loading = false
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (loading) (if (french) "Chargement…" else "Loading…") else (if (french) "Charger les horaires et arrêts" else "Load timetable and stops")) }

            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)

            if (originStops.isNotEmpty()) {
                Text(if (french) "Arrêts de départ" else "Origin stops", style = MaterialTheme.typography.titleSmall)
                originStops.take(8).forEach { stop ->
                    TextButton(onClick = { selectedOrigin = stop; departures = emptyList() }, modifier = Modifier.fillMaxWidth()) {
                        Text((if (selectedOrigin?.id == stop.id) "✓ " else "") + stop.name + " · " + stop.distanceMetres + " m", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            if (destinationStops.isNotEmpty()) {
                Text(if (french) "Arrêts d’arrivée" else "Destination stops", style = MaterialTheme.typography.titleSmall)
                destinationStops.take(8).forEach { stop ->
                    TextButton(onClick = { selectedDestination = stop; departures = emptyList() }, modifier = Modifier.fillMaxWidth()) {
                        Text((if (selectedDestination?.id == stop.id) "✓ " else "") + stop.name + " · " + stop.distanceMetres + " m", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            if (selectedOrigin != null && selectedDestination != null) {
                Button(
                    onClick = {
                        loading = true
                        scope.launch {
                            departures = repository.findDirectDepartures(
                                selectedOrigin!!.id,
                                selectedDestination!!.id,
                                travelDate.replace("-", ""),
                                "00:00:00",
                                30
                            )
                            message = if (departures.isEmpty())
                                (if (french) "Aucun trajet direct trouvé. Les correspondances ne sont pas encore prises en charge." else "No direct trip found. Transfers are not supported yet.")
                            else (if (french) "Départs directs trouvés : " else "Direct departures found: ") + departures.size
                            loading = false
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (french) "Afficher les départs" else "Show departures") }
            }
            departures.forEach { departure ->
                HorizontalDivider()
                Text("Route ${departure.routeShortName} · ${departure.headsign}", style = MaterialTheme.typography.titleSmall)
                Text("${departure.departureTime} → ${departure.arrivalTime}")
                Text("${departure.fromStopName} → ${departure.toStopName}", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                if (french) "Source : BC Transit. Horaires planifiés uniquement ; retards et tarifs non inclus."
                else "Source: BC Transit. Scheduled times only; live delays and fares are not included.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
