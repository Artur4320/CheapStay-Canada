package ca.easyway.app

/**
 * Normalized flight-search contract used by EasyWay.
 *
 * Provider adapters must return verified offers and a provider-supplied booking
 * URL. The Android app must never contain private provider API credentials.
 */
internal data class FlightSearchRequest(
    val originIata: String,
    val destinationIata: String,
    val departureDate: String, // ISO yyyy-MM-dd
    val returnDate: String? = null,
    val adults: Int = 1,
    val currency: String = "CAD",
    val cabin: FlightCabin = FlightCabin.ECONOMY,
    val maxStops: Int? = null
)

internal enum class FlightCabin { ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST }

internal data class FlightSegment(
    val airlineName: String,
    val flightNumber: String?,
    val originIata: String,
    val destinationIata: String,
    val departureAt: String,
    val arrivalAt: String,
    val durationMinutes: Int?
)

internal data class FlightOffer(
    val id: String,
    val provider: FlightProvider,
    val airlineName: String,
    val segments: List<FlightSegment>,
    val totalPrice: Double,
    val currency: String,
    val stops: Int,
    val totalDurationMinutes: Int?,
    val checkedBaggageIncluded: Boolean?,
    val bookingUrl: String,
    val retrievedAtEpochMillis: Long,
    val expiresAtEpochMillis: Long? = null
) {
    val isExpired: Boolean
        get() = expiresAtEpochMillis?.let { it <= System.currentTimeMillis() } ?: false
}

internal enum class FlightProvider {
    KIWI, GOOGLE_FLIGHTS, OTHER
}

internal sealed interface FlightSearchState {
    data object Idle : FlightSearchState
    data object Loading : FlightSearchState
    data class Results(val offers: List<FlightOffer>) : FlightSearchState
    data class Empty(val message: String) : FlightSearchState
    data class Error(val message: String) : FlightSearchState
}

/**
 * Implementations belong in a secure backend or a provider SDK that does not
 * expose private credentials in the APK. The current app has no live provider
 * adapter configured yet.
 */
internal interface FlightSearchProvider {
    val provider: FlightProvider
    suspend fun search(request: FlightSearchRequest): List<FlightOffer>
}

/**
 * Only open booking links returned by an approved provider. Never construct a
 * booking URL from a displayed price or accept arbitrary URLs from user input.
 */
internal object FlightBookingLinkPolicy {
    private val allowedHosts = setOf(
        "kiwi.com",
        "www.kiwi.com",
        "google.com",
        "www.google.com"
    )

    fun isAllowed(url: String): Boolean = try {
        val parsed = java.net.URI(url)
        parsed.scheme.equals("https", ignoreCase = true) &&
            parsed.host?.lowercase() in allowedHosts
    } catch (_: Exception) {
        false
    }
}
