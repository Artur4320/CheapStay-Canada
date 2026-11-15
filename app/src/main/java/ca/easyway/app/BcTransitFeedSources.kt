package ca.easyway.app

/**
 * First concrete BC Transit feed connection descriptor for Victoria.
 *
 * The feed is published by BC Transit and independently catalogued by
 * Transitland. EasyWay has not yet downloaded or parsed the ZIP in-app.
 */
internal data class StaticGtfsFeedSource(
    val region: String,
    val operatorName: String,
    val downloadUrl: String,
    val publisherUrl: String,
    val licenseUrl: String,
    val discoveredAt: String,
    val sourceVersionSha1: String,
    val serviceStartDate: String,
    val serviceEndDate: String,
    val routesReported: Int?,
    val stopsReported: Int?
)

internal object BcTransitFeedSources {
    val victoria = StaticGtfsFeedSource(
        region = "Victoria Regional Transit System",
        operatorName = "BC Transit",
        downloadUrl = "https://bct.tmix.se/Tmix.Cap.TdExport.WebApi/gtfs/?operatorIds=48",
        publisherUrl = "https://www.bctransit.com/open-data/",
        licenseUrl = "https://www.bctransit.com/open-data/terms-of-use/",
        discoveredAt = "2026-11-14",
        sourceVersionSha1 = "c4562ba51603cb050a8a20d800edc8fc7913f72f",
        serviceStartDate = "2026-11-07",
        serviceEndDate = "2027-01-03",
        routesReported = 69,
        stopsReported = 2360
    )
}
