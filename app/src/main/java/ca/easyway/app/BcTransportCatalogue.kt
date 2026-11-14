package ca.easyway.app

/**
 * Verified first-pass British Columbia passenger transport catalogue.
 *
 * This is reference data, not a live timetable or fare feed. A listed operator
 * or station must still be checked for travel-date availability before routing.
 */
internal enum class BcTransportMode {
    BUS, FERRY, TRAIN
}

internal data class BcPassengerLink(
    val mode: BcTransportMode,
    val operator: String,
    val from: String,
    val to: String,
    val season: String = "Year-round",
    val sourceUrl: String,
    val note: String = ""
)

internal data class BcPassengerNode(
    val name: String,
    val mode: BcTransportMode,
    val sourceUrl: String,
    val note: String = ""
)

internal object BcTransportCatalogue {
    const val VERIFIED_ON = "2026-11-14"

    // Scheduled inter-city bus routes taken from the BC Passenger Transportation
    // Board's published licensee list. "Engaged carrier" permissions are not
    // treated as a public route.
    private const val BUS_SOURCE =
        "https://www.ptboard.bc.ca/inter-city-bus-licensee-list"

    val busLinks = listOf(
        BcPassengerLink(BcTransportMode.BUS, "eBus", "Vancouver", "Kamloops", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "eBus", "Kamloops", "Kelowna", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "eBus", "Kelowna", "Vancouver", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Vancouver Island Connector / Tofino Bus", "Nanaimo", "Tofino", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Vancouver Island Connector / Tofino Bus", "Victoria", "Tofino", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Vancouver Island Connector / Tofino Bus", "Tofino", "Ucluelet", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Vancouver Island Connector / Tofino Bus", "Nanaimo", "Campbell River", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Wilson's Transportation / BC Connector", "Victoria", "Vancouver", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Wilson's Transportation / BC Connector", "Vancouver", "Kamloops", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Wilson's Transportation / BC Connector", "Vancouver", "Kelowna", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Wilson's Transportation / BC Connector", "Vancouver", "Whistler", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Rider Express", "Alberta border", "Vancouver", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Rider Express", "Kelowna", "Enderby", season = "April 15 – October 15", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Rider Express", "Salmon Arm", "Alberta border", season = "April 15 – October 15", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Mountain Man Mike's", "Kaslo", "Vancouver", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Mountain Man Mike's", "Kaslo", "Alberta border", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Mountain Man Mike's", "Osoyoos", "Kelowna", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Silver City Stagelines", "Nelson", "Kelowna", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Squamish Connector", "Vancouver", "Squamish", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Whistler Rides", "Vancouver", "Pemberton", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Waivin Flags Taxi", "Port Hardy", "Campbell River", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Kimta Transportation Society", "Mackenzie", "Prince George", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Eagle Transit", "Daajing Giids (Queen Charlotte)", "Masset", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "West Coast Trail Express", "Victoria", "Port Renfrew", season = "May 1 – September 30", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "West Coast Trail Express", "Port Renfrew", "Bamfield", season = "May 1 – September 30", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "West Coast Trail Express", "Victoria", "Bamfield", season = "May 1 – September 30", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Blue Cactus / Snowbus", "Richmond", "Whistler", season = "November – April", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Blue Cactus / Snowbus", "Burnaby", "Whistler", season = "November – April", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Epic Rides", "Vancouver", "Whistler", season = "June – October and November – April", sourceUrl = BUS_SOURCE),
        BcPassengerLink(BcTransportMode.BUS, "Sunshine Coast Connector", "Powell River", "Langdale Ferry Terminal", season = "May 15 – September 15", sourceUrl = BUS_SOURCE)
    )

    // Official BC Ferries terminal directory. These are terminal nodes, not
    // claims that every terminal has a direct sailing to every other terminal.
    private const val FERRY_TERMINALS_SOURCE =
        "https://www.bcferries.com/travel-boarding/terminal-directions-parking-food"

    val ferryTerminals = listOf(
        "Bella Bella (McLoughlin Bay)", "Bella Coola", "Bowen Island (Snug Cove)",
        "Brentwood Bay", "Buckley Bay", "Campbell River", "Chemainus",
        "Comox (Little River)", "Cormorant Island (Alert Bay)", "Cortes Island (Whaletown)",
        "Crofton", "Denman Island East (Gravelly Bay)", "Denman Island West",
        "Gabriola Island (Descanso Bay)", "Galiano Island (Sturdies Bay)",
        "Gambier Island (New Brighton)", "Graham Island (Skidegate)",
        "Hornby Island (Shingle Spit)", "Keats Island (Eastbourne)",
        "Keats Island (Keats Landing)", "Klemtu", "Langdale", "Lyall Harbour (Saturna Island)",
        "Mayne Island (Village Bay)", "Mill Bay", "Nanaimo (Departure Bay)",
        "Nanaimo (Duke Point)", "Nanaimo (Nanaimo Harbour)", "Ocean Falls",
        "Otter Bay (Pender Island)", "Penelakut Island (Telegraph Harbour)",
        "Port Hardy (Bear Cove)", "Port McNeill", "Powell River (Saltery Bay)",
        "Powell River (Westview)", "Prince Rupert", "Quadra Island (Heriot Bay)",
        "Quadra Island (Quathiaski Cove)", "Saltery Bay", "Salt Spring Island (Fulford Harbour)",
        "Salt Spring Island (Long Harbour)", "Salt Spring Island (Vesuvius Bay)",
        "Shearwater", "Sointula (Malcolm Island)", "Sturdies Bay (Galiano Island)",
        "Swartz Bay (Victoria)", "Texada Island (Blubber Bay)",
        "Tsawwassen (Vancouver)", "Village Bay (Mayne Island)", "Whaletown (Cortes Island)",
        "Earls Cove", "Horseshoe Bay (West Vancouver)", "Alliford Bay (Moresby Island)"
    ).distinct().map {
        BcPassengerNode(it, BcTransportMode.FERRY, FERRY_TERMINALS_SOURCE)
    }

    // VIA Rail's BC station directory includes remote/flag-stop locations.
    // It is deliberately stored as a directory, not as a promise of daily service.
    private const val RAIL_SOURCE =
        "https://www.viarail.ca/en/explore-our-destinations/find-a-train-station"

    val viaRailStationDirectory = listOf(
        "Abbotsford", "Agassiz", "Aleza Lake", "Ashcroft", "Bend", "Blue River",
        "Boston Bar", "Burns Lake", "Cassiar Cannery", "Cedarvale", "Chilliwack",
        "Clearwater", "Dome Creek", "Dorreen", "Dunster", "Endako", "Fort Fraser",
        "Goat River", "Harvey", "Hope", "Houston", "Hutton", "Kamloops North",
        "Katz", "Kitwanga", "Kwinitsa", "Longworth", "Loos", "McBride",
        "McGregor", "Mission Harbour", "New Hazelton", "North Bend", "Pacific",
        "Penny", "Prince George", "Prince Rupert", "Sinclair Mills", "Smithers",
        "Telkwa", "Terrace", "Upper Fraser", "Usk", "Valemount", "Vancouver",
        "Vanderhoof", "Willow River"
    ).map {
        BcPassengerNode(it, BcTransportMode.TRAIN, RAIL_SOURCE,
            "Listed in VIA Rail's station directory; check the date-specific timetable and stop type.")
    }
}
