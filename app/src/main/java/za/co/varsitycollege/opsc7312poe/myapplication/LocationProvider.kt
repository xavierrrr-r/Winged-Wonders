package za.co.varsitycollege.opsc7312poe.myapplication
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import java.lang.ref.WeakReference

class LocationProvider(private val activity: WeakReference<Activity>) {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity.get()!!)
    }

    fun getLastKnownLocation(onLocationReady: (Double, Double) -> Unit, onError: () -> Unit) {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    onLocationReady(location.latitude, location.longitude)
                } else {
                    requestNewLocation(onLocationReady, onError)
                }
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun requestNewLocation(onLocationReady: (Double, Double) -> Unit, onError: () -> Unit) {
        locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(10000)
            .setFastestInterval(5000)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    onLocationReady(location.latitude, location.longitude)
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (securityException: SecurityException) {
            onError()
        }
    }

    private fun checkLocationPermission(): Boolean {
        val context = activity.get() ?: return false
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        val context = activity.get()
        if (context != null) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now use getLastKnownLocation
            } else {
                Toast.makeText(activity.get(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_LOCATION = 1001
    }
}