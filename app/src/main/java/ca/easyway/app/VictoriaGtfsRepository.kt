package ca.easyway.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Downloads and imports the official Victoria Regional Transit System GTFS feed.
 * Source: BC Transit Open Data. Data is supplied as-is; refresh before relying
 * on a journey because the publisher may replace the feed without notice.
 */
internal class VictoriaGtfsRepository(context: Context) {
    companion object {
        const val SOURCE_URL =
            "https://bct.tmix.se/Tmix.Cap.TdExport.WebApi/gtfs/?operatorIds=48"
        const val SOURCE_NAME = "BC Transit"
        private const val DB_NAME = "victoria_gtfs.db"
        private const val DB_VERSION = 1
        private val TABLES = mapOf("agency.txt" to "agency", "routes.txt" to "routes", "stops.txt" to "stops", "trips.txt" to "trips", "calendar_dates.txt" to "calendar_dates", "stop_times.txt" to "stop_times", "shapes.txt" to "shapes", "feed_info.txt" to "feed_info")
        private val COLUMNS = mapOf(
            "agency" to listOf("agency_id", "agency_name", "agency_url", "agency_timezone", "agency_lang", "agency_phone", "agency_fare_url", "agency_email"),
            "routes" to listOf("route_id", "route_short_name", "route_long_name", "route_type", "route_color", "route_text_color"),
            "stops" to listOf("stop_id", "stop_name", "stop_lat", "stop_lon", "wheelchair_boarding", "stop_code"),
            "trips" to listOf("route_id", "service_id", "trip_id", "trip_headsign", "shape_id", "block_id", "direction_id"),
            "calendar_dates" to listOf("service_id", "date", "exception_type"),
            "stop_times" to listOf("trip_id", "arrival_time", "departure_time", "stop_id", "stop_sequence", "shape_dist_traveled", "stop_headsign", "pickup_type", "drop_off_type", "timepoint"),
            "shapes" to listOf("shape_id", "shape_pt_lat", "shape_pt_lon", "shape_pt_sequence", "shape_dist_traveled"),
            "feed_info" to listOf("feed_publisher_name", "feed_publisher_url", "feed_lang", "feed_version", "feed_start_date", "feed_end_date")
        )
    }

    private val helper = GtfsDbHelper(context.applicationContext)

    data class DirectDeparture(
        val routeShortName: String,
        val routeLongName: String,
        val headsign: String,
        val departureTime: String,
        val arrivalTime: String,
        val fromStopName: String,
        val toStopName: String
    )

    suspend fun downloadAndImport(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(SOURCE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 90_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "EasyWay/1.0 (GTFS import)")
            }
            try {
                connection.inputStream.use { input ->
                    ZipInputStream(input).use { zip ->
                        val db = helper.writableDatabase
                        db.beginTransaction()
                        try {
                            clearTables(db)
                            while (true) {
                                val entry = zip.nextEntry ?: break
                                val table = TABLES[entry.name] ?: continue
                                val reader = BufferedReader(InputStreamReader(zip, Charsets.UTF_8))
                                importCsv(db, table, reader)
                                zip.closeEntry()
                            }
                            createIndexes(db)
                            db.setTransactionSuccessful()
                        } finally {
                            db.endTransaction()
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    /** Finds scheduled direct trips between two exact GTFS stop IDs on YYYYMMDD. */
    suspend fun findDirectDepartures(
        fromStopId: String,
        toStopId: String,
        date: String,
        notBefore: String,
        limit: Int = 20
    ): List<DirectDeparture> = withContext(Dispatchers.IO) {
        val db = helper.readableDatabase
        val sql = """
            SELECT r.route_short_name, r.route_long_name, t.trip_headsign,
                   a.departure_time, b.arrival_time, s1.stop_name, s2.stop_name
            FROM calendar_dates c
            JOIN trips t ON t.service_id = c.service_id
            JOIN routes r ON r.route_id = t.route_id
            JOIN stop_times a ON a.trip_id = t.trip_id
            JOIN stop_times b ON b.trip_id = t.trip_id AND b.stop_sequence > a.stop_sequence
            JOIN stops s1 ON s1.stop_id = a.stop_id
            JOIN stops s2 ON s2.stop_id = b.stop_id
            WHERE c.date = ? AND c.exception_type = 1
              AND a.stop_id = ? AND b.stop_id = ?
              AND a.departure_time >= ?
            ORDER BY a.departure_time
            LIMIT ?
        """.trimIndent()
        val results = mutableListOf<DirectDeparture>()
        db.rawQuery(sql, arrayOf(date, fromStopId, toStopId, notBefore, limit.coerceIn(1, 100).toString()))
            .use { cursor ->
                while (cursor.moveToNext()) {
                    results += DirectDeparture(
                        routeShortName = cursor.getString(0).orEmpty(),
                        routeLongName = cursor.getString(1).orEmpty(),
                        headsign = cursor.getString(2).orEmpty(),
                        departureTime = cursor.getString(3).orEmpty(),
                        arrivalTime = cursor.getString(4).orEmpty(),
                        fromStopName = cursor.getString(5).orEmpty(),
                        toStopName = cursor.getString(6).orEmpty()
                    )
                }
            }
        results
    }

    suspend fun searchStops(query: String, limit: Int = 30): List<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val db = helper.readableDatabase
            val rows = mutableListOf<Pair<String, String>>()
            db.rawQuery(
                "SELECT stop_id, stop_name FROM stops WHERE stop_name LIKE ? ORDER BY stop_name LIMIT ?",
                arrayOf("%${query.trim()}%", limit.coerceIn(1, 100).toString())
            ).use { cursor ->
                while (cursor.moveToNext()) rows += cursor.getString(0) to cursor.getString(1)
            }
            rows
        }


    data class NearbyStop(val id: String, val name: String, val latitude: Double, val longitude: Double, val distanceMetres: Int)

    suspend fun searchStopsNear(latitude: Double, longitude: Double, limit: Int = 12): List<NearbyStop> =
        withContext(Dispatchers.IO) {
            val deltaLat = 0.04
            val deltaLon = 0.06
            val db = helper.readableDatabase
            val rows = mutableListOf<NearbyStop>()
            db.rawQuery(
                "SELECT stop_id, stop_name, stop_lat, stop_lon FROM stops WHERE stop_lat BETWEEN ? AND ? AND stop_lon BETWEEN ? AND ?",
                arrayOf((latitude - deltaLat).toString(), (latitude + deltaLat).toString(),
                    (longitude - deltaLon).toString(), (longitude + deltaLon).toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val lat = cursor.getDouble(2)
                    val lon = cursor.getDouble(3)
                    val distance = haversineMetres(latitude, longitude, lat, lon)
                    rows += NearbyStop(cursor.getString(0), cursor.getString(1), lat, lon, distance)
                }
            }
            rows.sortedBy { it.distanceMetres }.take(limit.coerceIn(1, 50))
        }

    private fun haversineMetres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        return (2 * earthRadius * kotlin.math.asin(kotlin.math.sqrt(a))).toInt()
    }

    private fun clearTables(db: SQLiteDatabase) {
        listOf("agency", "routes", "stops", "trips", "calendar_dates", "stop_times", "shapes", "feed_info")
            .forEach { db.delete(it, null, null) }
    }

    private fun importCsv(db: SQLiteDatabase, table: String, reader: BufferedReader) {
        val header = reader.readLine()?.removePrefix("\uFEFF")?.let(::parseCsvLine) ?: return
        val columns = COLUMNS[table] ?: return
        val indexes = columns.map { header.indexOf(it) }
        if (indexes.any { it < 0 }) return
        while (true) {
            val line = reader.readLine() ?: break
            val fields = parseCsvLine(line)
            val values = ContentValues(columns.size)
            columns.forEachIndexed { index, column ->
                val value = fields.getOrNull(indexes[index]).orEmpty()
                if (value.isNotEmpty()) {
                    when (column) {
                        "stop_lat", "stop_lon", "shape_dist_traveled" -> values.put(column, value.toDoubleOrNull())
                        "route_type", "wheelchair_boarding", "direction_id", "exception_type",
                        "stop_sequence", "pickup_type", "drop_off_type", "timepoint",
                        "shape_pt_sequence" -> values.put(column, value.toIntOrNull())
                        else -> values.put(column, value)
                    }
                }
            }
            db.insertOrThrow(table, null, values)
        }
    }

    /** CSV parser for GTFS rows, including quoted commas and escaped quotes. */
    private fun parseCsvLine(line: String): List<String> {
        val fields = ArrayList<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> {
                    field.append('"'); i++
                }
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> {
                    fields += field.toString(); field.setLength(0)
                }
                else -> field.append(ch)
            }
            i++
        }
        fields += field.toString()
        return fields
    }

    private fun createIndexes(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_stop_times_trip_seq ON stop_times(trip_id, stop_sequence)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_stop_times_stop ON stop_times(stop_id, departure_time)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_trips_service ON trips(service_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_calendar_date ON calendar_dates(date, service_id, exception_type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_stops_name ON stops(stop_name)")
    }

    private class GtfsDbHelper(context: Context) :
        SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE agency(agency_id TEXT, agency_name TEXT, agency_url TEXT, agency_timezone TEXT, agency_lang TEXT, agency_phone TEXT, agency_fare_url TEXT, agency_email TEXT)")
            db.execSQL("CREATE TABLE routes(route_id TEXT PRIMARY KEY, route_short_name TEXT, route_long_name TEXT, route_type INTEGER, route_color TEXT, route_text_color TEXT)")
            db.execSQL("CREATE TABLE stops(stop_id TEXT PRIMARY KEY, stop_name TEXT, stop_lat REAL, stop_lon REAL, wheelchair_boarding INTEGER, stop_code TEXT)")
            db.execSQL("CREATE TABLE trips(trip_id TEXT PRIMARY KEY, route_id TEXT, service_id TEXT, trip_headsign TEXT, shape_id TEXT, block_id TEXT, direction_id INTEGER)")
            db.execSQL("CREATE TABLE calendar_dates(service_id TEXT, date TEXT, exception_type INTEGER)")
            db.execSQL("CREATE TABLE stop_times(trip_id TEXT, arrival_time TEXT, departure_time TEXT, stop_id TEXT, stop_sequence INTEGER, shape_dist_traveled REAL, stop_headsign TEXT, pickup_type INTEGER, drop_off_type INTEGER, timepoint INTEGER)")
            db.execSQL("CREATE TABLE shapes(shape_id TEXT, shape_pt_lat REAL, shape_pt_lon REAL, shape_pt_sequence INTEGER, shape_dist_traveled REAL)")
            db.execSQL("CREATE TABLE feed_info(feed_publisher_name TEXT, feed_publisher_url TEXT, feed_lang TEXT, feed_version TEXT, feed_start_date TEXT, feed_end_date TEXT)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS agency")
            db.execSQL("DROP TABLE IF EXISTS routes")
            db.execSQL("DROP TABLE IF EXISTS stops")
            db.execSQL("DROP TABLE IF EXISTS trips")
            db.execSQL("DROP TABLE IF EXISTS calendar_dates")
            db.execSQL("DROP TABLE IF EXISTS stop_times")
            db.execSQL("DROP TABLE IF EXISTS shapes")
            db.execSQL("DROP TABLE IF EXISTS feed_info")
            onCreate(db)
        }
    }

}
