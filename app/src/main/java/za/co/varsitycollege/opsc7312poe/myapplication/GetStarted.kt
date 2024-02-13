package za.co.varsitycollege.opsc7312poe.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.Point
import com.mapbox.search.ResponseInfo
import com.mapbox.search.ReverseGeoOptions
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings

import com.mapbox.search.result.SearchResult
import java.lang.ref.WeakReference

class GetStarted : AppCompatActivity() {
    private lateinit var getStartedButton: Button
    private lateinit var locationProvider: LocationProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_started)

        locationProvider = LocationProvider(WeakReference(this))

        getStartedButton = findViewById(R.id.getStarted)
        getStartedButton.setOnClickListener {
            locationProvider.getLastKnownLocation(
                onLocationReady = { latitude, longitude ->
                    val userLocation = UserLocation(latitude, longitude)
                    UserLocationProvider.setUserLocation(userLocation.latitude, userLocation.longitude, "Your Location")

                    // Continue with your logic here, e.g., start the next activity
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                },
                onError = {
                    Toast.makeText(this@GetStarted, "Failed to get location", Toast.LENGTH_SHORT).show()

                })
        }
    }
}

