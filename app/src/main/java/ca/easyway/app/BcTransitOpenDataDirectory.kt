package ca.easyway.app

/**
 * BC Transit open-data directory transcribed from the official catalogue.
 *
 * GTFS/GTFS-Realtime feed availability here means the feed is published by
 * BC Transit. EasyWay has not downloaded, parsed, or connected these feeds yet.
 * Re-check the official portal for current feed URLs and terms before ingestion.
 */
internal data class BcTransitSystemFeed(
    val systemName: String,
    val staticGtfs: Boolean,
    val realtimeServiceAlerts: Boolean,
    val realtimeTripUpdates: Boolean,
    val realtimeVehiclePositions: Boolean,
    val dataPortalUrl: String = "https://www.bctransit.com/open-data/",
    val note: String = ""
)

internal object BcTransitOpenDataDirectory {
    const val SOURCE_URL = "https://www.bctransit.com/open-data/"
    const val DATA_TERMS_NOTE =
        "BC Transit publishes the data free of charge and as-is; its notice says feeds may not reflect the exact current state of internal systems."

    // Names and feed flags reflect the official open-data table as reviewed
    // on 2026-11-14. The Highway 16 corridor row publishes stop/route GIS data
    // but does not show GTFS or GTFS-Realtime links.
    val systems = listOf(
        BcTransitSystemFeed("100 Mile House", true, true, true, true),
        BcTransitSystemFeed("Agassiz-Harrison", true, true, true, true),
        BcTransitSystemFeed("Ashcroft-Cache-Creek-Clinton", true, true, true, true),
        BcTransitSystemFeed("Bella Coola Valley", true, true, true, true),
        BcTransitSystemFeed("Boundary", true, true, true, true),
        BcTransitSystemFeed("Bulkley-Nechako", true, true, true, true),
        BcTransitSystemFeed("Campbell River", true, true, true, true),
        BcTransitSystemFeed("Central Fraser Valley", true, true, true, true),
        BcTransitSystemFeed("Chilliwack", true, true, true, true),
        BcTransitSystemFeed("Clearwater", true, true, true, true),
        BcTransitSystemFeed("Columbia Valley", true, true, true, true),
        BcTransitSystemFeed("Comox Valley", true, true, true, true),
        BcTransitSystemFeed("Cowichan Valley", true, true, true, true),
        BcTransitSystemFeed("Cranbrook", true, true, true, true),
        BcTransitSystemFeed("Creston Valley", true, true, true, true),
        BcTransitSystemFeed("Dawson Creek", true, true, true, true),
        BcTransitSystemFeed("Elk Valley", true, true, true, true),
        BcTransitSystemFeed("Fort St. John", true, true, true, true),
        BcTransitSystemFeed("Hazeltons", true, true, true, true),
        BcTransitSystemFeed("Highway 16", false, false, false, false,
            note = "Stop and route GIS data are listed; no GTFS feed is shown in the official table."),
        BcTransitSystemFeed("Hope", true, true, true, true),
        BcTransitSystemFeed("Kamloops", true, true, true, true),
        BcTransitSystemFeed("Kelowna", true, true, true, true),
        BcTransitSystemFeed("Kimberley", true, true, true, true),
        BcTransitSystemFeed("Kitimat", true, true, true, true),
        BcTransitSystemFeed("Merritt", true, true, true, true),
        BcTransitSystemFeed("Mount Waddington", true, true, true, true),
        BcTransitSystemFeed("Nanaimo", true, true, true, true),
        BcTransitSystemFeed("Pemberton Valley", true, true, true, true),
        BcTransitSystemFeed("Port Alberni", true, true, true, true),
        BcTransitSystemFeed("Port Edward", true, true, true, true),
        BcTransitSystemFeed("Powell River", true, true, true, true),
        BcTransitSystemFeed("Prince George", true, true, true, true),
        BcTransitSystemFeed("Prince Rupert", true, true, true, true),
        BcTransitSystemFeed("Quesnel", true, true, true, true),
        BcTransitSystemFeed("Revelstoke", true, true, true, true),
        BcTransitSystemFeed("Salt Spring Island", true, true, true, true),
        BcTransitSystemFeed("Shuswap", true, true, true, true),
        BcTransitSystemFeed("Skeena", true, true, true, true),
        BcTransitSystemFeed("Smithers", true, true, true, true),
        BcTransitSystemFeed("South Okanagan-Similkameen", true, true, true, true),
        BcTransitSystemFeed("Squamish", true, true, true, true),
        BcTransitSystemFeed("Sunshine Coast", true, true, true, true),
        BcTransitSystemFeed("Terrace", true, true, true, true),
        BcTransitSystemFeed("Vernon", true, true, true, true),
        BcTransitSystemFeed("Victoria", true, true, true, true),
        BcTransitSystemFeed("West Coast", true, true, true, true),
        BcTransitSystemFeed("West Kootenay", true, true, true, true),
        BcTransitSystemFeed("Whistler", true, true, true, true),
        BcTransitSystemFeed("Williams Lake", true, true, true, true),
        BcTransitSystemFeed("East Kootenay", true, true, true, true),
        BcTransitSystemFeed("Fraser Valley", true, true, true, true),
        BcTransitSystemFeed("Kitimat-Stikine", true, true, true, true),
        BcTransitSystemFeed("North Okanagan", true, true, true, true)
    )

    val systemsWithRealtime = systems.filter {
        it.realtimeServiceAlerts && it.realtimeTripUpdates && it.realtimeVehiclePositions
    }
}
