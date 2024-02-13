package za.co.varsitycollege.opsc7312poe.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import za.co.varsitycollege.opsc7312poe.myapplication.RetrofitService.createEBirdApiService
import kotlin.collections.Map

class Map : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var currentRoute: DirectionsRoute
    private lateinit var client: MapboxDirections
    private lateinit var origin: Point
    private lateinit var destination: Point
    var destinationLatitude : Double=0.0
    var destinationLongitude : Double=0.0
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var myMapbtn: Button
    private lateinit var statusLabel: TextView
    private var isSightingsDisplayed = false
    private val hotspots = mutableListOf<Hotspot>()
    private lateinit var mapboxMap: MapboxMap
    private var fixedSightingLatitude: Double = 40.758896
    private var fixedSightingLongitude: Double = 73.985130

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Mapbox with your access token
        Mapbox.getInstance(this, "sk.eyJ1IjoidHJpc3RhbnN0IiwiYSI6ImNsbGx4eWJweDJoeGEzZXFoNGhlcmR1NG0ifQ.6lkNrr-67q59pOP9kPYTtw")
        // Set the layout for this activity
        setContentView(R.layout.activity_map)
        // Initialize the MapView
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(object : OnMapReadyCallback {
            override fun onMapReady(mapboxMap: MapboxMap) {
                mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
                    // Set the origin and destination coordinates
                    mapboxMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                        LatLng(UserLocationProvider.getUserLatitude(), UserLocationProvider.getUserLongitude()),
                        14.0
                    ))
                    origin = Point.fromLngLat(UserLocationProvider.getUserLongitude(), UserLocationProvider.getUserLatitude())
                    destination = Point.fromLngLat(UserLocationProvider.getUserLongitude(), UserLocationProvider.getUserLatitude())

                    // Initialize the sources and layers on the map
                    initSource(style)
                    initLayers(style)

                    //region UserMarker
                    val locationComponent = mapboxMap.locationComponent
                    locationComponent.activateLocationComponent(
                        LocationComponentActivationOptions.builder(this@Map, style).build()
                    )
                    if (ActivityCompat.checkSelfPermission(
                            this@Map,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this@Map,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                    }
                    locationComponent.isLocationComponentEnabled = true
                    locationComponent.cameraMode = CameraMode.TRACKING
                    locationComponent.renderMode = RenderMode.NORMAL
                    mapboxMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(UserLocationProvider.getUserLatitude(), UserLocationProvider.getUserLongitude()),
                            15.0 // Adjust the zoom level as needed
                        )
                    )
                    //endregion
                    // Get the directions route from Mapbox Directions API
                    fetchHotspotsFromEBird(mapboxMap)

                }
            }
        })
        myMapbtn = findViewById(R.id.myMap)

       myMapbtn.setOnClickListener{
           val registrationIntent = Intent(this, MyMap::class.java)
           startActivity(registrationIntent)
       }


        //region navigation
        bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    // Start the HomeActivity
                    startActivity(Intent(applicationContext, Home::class.java))
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.menu_map -> {
                    // Start the SettingsActivity
                    startActivity(Intent(applicationContext, Map::class.java))
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.menu_sightings -> {
                    // Start the SightingsActivity
                    startActivity(Intent(applicationContext, Sightings::class.java))
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.menu_map -> return@setOnNavigationItemSelectedListener true
            }
            false
        }
        //endregion
    }

    private fun initSource(loadedMapStyle: Style) {
        // Add a source for the route
        loadedMapStyle.addSource(GeoJsonSource("route-source"))

        // Add a source for the origin and destination markers
        val iconGeoJsonSource = GeoJsonSource("icon-source", FeatureCollection.fromFeatures(arrayOf(
            Feature.fromGeometry(Point.fromLngLat(origin.longitude(), origin.latitude())),
            Feature.fromGeometry(Point.fromLngLat(destination.longitude(), destination.latitude()))
        )))
        loadedMapStyle.addSource(iconGeoJsonSource)
    }

    private fun initLayers(loadedMapStyle: Style) {
        // Create a LineLayer for the route
        val routeLayer = LineLayer("route-layer", "route-source")
        routeLayer.setProperties(
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            PropertyFactory.lineWidth(5f),
            PropertyFactory.lineColor(Color.parseColor("#009688"))
        )
        loadedMapStyle.addLayer(routeLayer)

        // Add a red marker icon to the map
        BitmapUtils.getBitmapFromDrawable(resources.getDrawable(R.drawable.red_marker))
            ?.let { loadedMapStyle.addImage("red-pin-icon", it) }

        // Add a SymbolLayer for the origin and destination markers
        loadedMapStyle.addLayer(SymbolLayer("icon-layer", "icon-source")
            .withProperties(
                PropertyFactory.iconImage("red-pin-icon"),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconOffset(arrayOf(0f, -9f))
            )
        )
    }
    private fun fetchHotspotsFromEBird(mapboxMap: MapboxMap) {
        val distanceInKm = 20.0
        val maxResults = 20
        val apiKey = "keodjjotqkd0"

        val eBirdApiService = createEBirdApiService(apiKey)
        val hotspots = mutableListOf<hotspots>() // Create a list to store hotspots

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = eBirdApiService.getHotspots(UserLocationProvider.getUserLatitude(),UserLocationProvider.getUserLongitude(), maxResults, distanceInKm, "csv", apiKey)

                if (response.isSuccessful) {
                    val csvData = response.body()?.string()
                    if (!csvData.isNullOrBlank()) {
                        // Parse the CSV data to get the latitude and longitude values
                        Log.d("CSV Data", csvData)

                        val lines = csvData.split("\n")
                        for (line in lines) {
                            val parts = line.split(",")
                            if (parts.size >= 5) {
                                val locLat = parts[4].toDoubleOrNull()
                                val locLng = parts[5].toDoubleOrNull()
                                val name = parts[6]
                                if (locLat != null && locLng != null) {
                                    // Create a Hotspot object and add it to the list
                                    val hotspot = hotspots(locLat, locLng, name)
                                    hotspots.add(hotspot)
                                }
                            }
                        }
                        addHotspotMarkersToMap(mapboxMap, hotspots)
                    } else {
                        Log.e("CSV Error", "Empty or null CSV data")
                    }
                } else {
                    Log.e("API Error", "API request was not successful")
                }
            } catch (e: Exception) {
                Log.e("API Error", e.toString())
                e.printStackTrace()
            }

            // Now you have the hotspots list
            // You can use the hotspots list as needed
            for (hotspot in hotspots) {
                Log.d("Hotspot", "Name: ${hotspot.name}, Latitude: ${hotspot.latitude}, Longitude: ${hotspot.longitude}")
            }
        }
    }
    private fun addHotspotMarkersToMap(mapboxMap: MapboxMap, hotspots: List<hotspots>) {
        runOnUiThread {
            mapboxMap.getStyle { style ->
                // Loop through the list of hotspots and add markers for each one
                for (hotspot in hotspots) {
                    val hotspotFeature =
                        Feature.fromGeometry(Point.fromLngLat(hotspot.longitude, hotspot.latitude))
                    hotspotFeature.addStringProperty("name", hotspot.name)

                    // Use the hotspot name as the icon image (replace with your icon image)
                    style.addImage(
                        hotspot.name,
                        BitmapFactory.decodeResource(resources, R.drawable.red_marker)
                    )

                    // Add a source for each hotspot
                    val hotspotSource = GeoJsonSource(hotspot.name, hotspotFeature)
                    style.addSource(hotspotSource)

                    // Create a SymbolLayer for each hotspot
                    style.addLayer(
                        SymbolLayer(hotspot.name, hotspot.name)
                            .withProperties(
                                PropertyFactory.iconImage(hotspot.name),
                                PropertyFactory.iconIgnorePlacement(true),
                                PropertyFactory.iconAllowOverlap(true),
                                PropertyFactory.iconOffset(arrayOf(0f, -9f))
                            )
                    )
                }

                // Set up an OnMapClickListener to handle marker click events
                mapboxMap.addOnMapClickListener { clickPoint ->
                    val screenPoint = mapboxMap.projection.toScreenLocation(LatLng(clickPoint.latitude, clickPoint.longitude))

                    // Iterate through the hotspots and check if a marker was clicked
                    for (hotspot in hotspots) {
                        val markerScreenPoint = mapboxMap.projection.toScreenLocation(LatLng(hotspot.latitude, hotspot.longitude))
                        val dx = screenPoint.x - markerScreenPoint.x
                        val dy = screenPoint.y - markerScreenPoint.y

                        // Define a threshold for click sensitivity
                        val thresholdInPixels = 30

                        if (Math.abs(dx) < thresholdInPixels && Math.abs(dy) < thresholdInPixels) {
                            // A marker was clicked
                            getRoute(mapboxMap, origin, destination)
                            destinationLatitude=hotspot.latitude
                            destinationLongitude=hotspot.longitude
                            val message = "Name: ${hotspot.name}, Latitude: ${hotspot.latitude}, Longitude: ${hotspot.longitude}"

                            return@addOnMapClickListener true
                        }
                    }

                    false
                }
            }
        }
    }
    private fun getRoute(mapboxMap: MapboxMap, origin: Point, destination: Point) {
        client = MapboxDirections.builder()
            .accessToken("pk.eyJ1IjoidHJpc3RhbnN0IiwiYSI6ImNsbGx4Z2c2bDBsZ2MzbW5mOHpya3Y1bnEifQ.A7MCwXTujPZ3XZ-5_Hpovg")
            .routeOptions(
                RouteOptions.builder()
                    .coordinatesList(listOf(
                        Point.fromLngLat(UserLocationProvider.getUserLongitude(),UserLocationProvider.getUserLatitude(),), // origin
                        Point.fromLngLat(destinationLongitude,destinationLatitude, ) // destination
                    ))
                    .profile(DirectionsCriteria.PROFILE_DRIVING)
                    .overview(DirectionsCriteria.OVERVIEW_FULL)
                    .build()

            )
            .build()
        client.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.body() == null) {
                    Log.d("Key check","No routes found, make sure you set the right user and access token.")
                    return
                } else if (response.body()?.routes()?.isEmpty() == true) {
                    Log.d("Key check","No routes found")
                    return
                }

                currentRoute = response.body()?.routes()?.get(0)!!

                // Show the route distance in a toast
                Toast.makeText(this@Map, "Route distance: ${currentRoute.distance()}", Toast.LENGTH_SHORT).show()

                // Update the route source on the map
                mapboxMap.getStyle { style ->
                    style.getSourceAs<GeoJsonSource>("route-source")
                        ?.setGeoJson(LineString.fromPolyline(currentRoute.geometry()!!, 6))
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                Log.e("Directions API Error", "Failed to make API request: ${throwable.message}")
                Toast.makeText(this@Map, "Error: ${throwable.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (client != null) {
            client.cancelCall()
        }
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}

