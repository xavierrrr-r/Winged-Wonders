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
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
class MyMap : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var client: MapboxDirections
    private lateinit var origin: Point
    private lateinit var back: ImageButton
    private lateinit var destination: Point
    private lateinit var mDatabase: DatabaseReference
    private lateinit var birdSightings: MutableList<BirdData>
    private lateinit var bottomNavigationView: BottomNavigationView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(
            this,
            "sk.eyJ1IjoidHJpc3RhbnN0IiwiYSI6ImNsbGx4eWJweDJoeGEzZXFoNGhlcmR1NG0ifQ.6lkNrr-67q59pOP9kPYTtw"
        )
        setContentView(R.layout.activity_mymap)

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mDatabase = FirebaseDatabase.getInstance().reference

        birdSightings = mutableListOf() // Initialize the list

        mapView.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
                mapboxMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            UserLocationProvider.getUserLatitude(),
                            UserLocationProvider.getUserLongitude()
                        ),
                        14.0
                    )
                )
                origin = Point.fromLngLat(
                    UserLocationProvider.getUserLongitude(),
                    UserLocationProvider.getUserLatitude()
                )
                destination = Point.fromLngLat(
                    UserLocationProvider.getUserLongitude(),
                    UserLocationProvider.getUserLatitude()
                )

                // Fetch bird sighting data from the database using the current user's UID
                fetchBirdDataFromDatabase(mapboxMap, style)

                initSource(style)
                initLayers(style)


                val locationComponent = mapboxMap.locationComponent
                locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this@MyMap, style).build()
                )
                if (ActivityCompat.checkSelfPermission(
                        this@MyMap,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this@MyMap,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                }
                locationComponent.isLocationComponentEnabled = true
                locationComponent.cameraMode = CameraMode.TRACKING
                locationComponent.renderMode = RenderMode.NORMAL
                mapboxMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            UserLocationProvider.getUserLatitude(),
                            UserLocationProvider.getUserLongitude()
                        ),
                        15.0
                    )
                )
            }
        }

        back = findViewById(R.id.back)
        back.setOnClickListener {
            val mapIntent = Intent(this, Map::class.java)
            startActivity(mapIntent)
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

    private fun fetchBirdDataFromDatabase(mapboxMap: MapboxMap, style: Style) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            // Get the current user's UID
            val userUid = user.uid

            // Use the user UID in the database reference
            val birdSightingsReference = mDatabase.child("users").child(userUid).child("bird")

            birdSightingsReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Clear existing data
                birdSightings.clear()

                for (birdSnapshot in dataSnapshot.children) {
                    val birdData = birdSnapshot.getValue(BirdData::class.java)
                    birdData?.let {
                        birdSightings.add(it)
                    }
                }

                // Log the birdSightings list after fetching from the database
                Log.d("BirdSightings", "BirdSightings list after fetching from the database: $birdSightings")

                // Call addBirdMarkersToMap after the data is fetched
                addBirdMarkersToMap(mapboxMap, style, birdSightings)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors
                Log.e("Firebase", "Error fetching bird sightings", databaseError.toException())
            }
        })
        } ?: run {
            Log.e("Firebase", "User not authenticated.")
        }
    }

    private fun addBirdMarkersToMap(mapboxMap: MapboxMap, style: Style, birdDataList: List<BirdData>) {
        runOnUiThread {
            style.removeLayer("bird-layer")
            style.removeSource("bird-source")

            style.addImage("default-marker", BitmapFactory.decodeResource(resources, R.drawable.red_marker))

            val features = ArrayList<Feature>()
            for (birdData in birdDataList) {
                birdData.latitude?.let { latitude ->
                    birdData.longitude?.let { longitude ->
                        Log.d(
                            "BirdSightings",
                            "Adding Bird Marker - Bird: ${birdData.bname}, Latitude: $latitude, Longitude: $longitude"
                        )

                        val birdFeature = Feature.fromGeometry(Point.fromLngLat(longitude, latitude))
                        birdFeature.addStringProperty("birdname", birdData.bname)
                        features.add(birdFeature)
                    }
                }
            }

            if (features.isNotEmpty()) {
                style.addSource(GeoJsonSource("bird-source", FeatureCollection.fromFeatures(features)))

                style.addLayer(
                    SymbolLayer("bird-layer", "bird-source")
                        .withProperties(
                            PropertyFactory.iconImage("default-marker"),
                            PropertyFactory.iconIgnorePlacement(true),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconOffset(arrayOf(0f, -9f))
                        )
                )
            } else {
                Log.d("BirdSightings", "No features to add.")
            }

            Log.d("BirdSightings", "BirdSightings list after markers are added: $birdDataList")
        }
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