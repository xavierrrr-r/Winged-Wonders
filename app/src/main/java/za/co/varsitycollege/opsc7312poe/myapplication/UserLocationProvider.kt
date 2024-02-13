package za.co.varsitycollege.opsc7312poe.myapplication

object UserLocationProvider {
    private var longitude: Double = 0.0
    private var latitude: Double = 0.0
    private var locationName: String = "Unknown Location"

    fun setUserLocation(latitude: Double, longitude: Double, locationName: String) {
        this.latitude = latitude
        this.longitude = longitude
        this.locationName = locationName
    }

    fun getUserLatitude(): Double {
        return latitude
    }

    fun getUserLongitude(): Double {
        return longitude
    }

    fun getLocationName(): String {
        return locationName
    }
}