package za.co.varsitycollege.opsc7312poe.myapplication

data class Hotspot(
    val locationID: String,
    val country: String,
    val subnational1: String,
    val subnational2: String,
    val locLat: Double,
    val locLng: Double,
    val name: String,
    val observationDate: String,
    val observationCount: Int
)
