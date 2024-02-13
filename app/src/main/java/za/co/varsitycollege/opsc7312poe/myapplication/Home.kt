package za.co.varsitycollege.opsc7312poe.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference

class Home : AppCompatActivity() {
    private lateinit var greetingTextView: TextView
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: DatabaseReference
    private lateinit var birdRecyclerView: RecyclerView
    private lateinit var storageReference: StorageReference
    private lateinit var birdArrayList: ArrayList<BirdData>

    //For commit
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        greetingTextView = findViewById(R.id.hello_user)
        birdRecyclerView =findViewById(R.id.recycleView)
        birdRecyclerView.layoutManager = LinearLayoutManager(this)
        birdRecyclerView.setHasFixedSize(true)
        birdArrayList = arrayListOf<BirdData>()

        // Retrieve the logged-in user's email from UserDataManager
        val loggedInUser = UserDataManager.getInstance().getLoggedInUser()
        val fullName = loggedInUser?.full_name

        // Set the TextView text to "Hello" + user's name
        greetingTextView.text = "Hello ${fullName ?: "Guest"}"

        greetingTextView.setOnClickListener{
            val registrationIntent = Intent(this, Settings::class.java)
            startActivity(registrationIntent)
        }

        // Initialize RecyclerView
        birdRecyclerView = findViewById(R.id.recycleView)
        birdArrayList = ArrayList()
        getBirdData()

        // Set the LinearLayoutManager with horizontal orientation
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        birdRecyclerView.layoutManager = layoutManager


        //region BottomNavBar
        bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    // Start the HomeActivity
                    startActivity(Intent(this, Home::class.java))
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.menu_map -> {
                    // Start the MapActivity
                    startActivity(Intent(this, Map::class.java))
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.menu_sightings -> {
                    // Start the SightingsActivity
                    startActivity(Intent(this, Sightings::class.java))
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.menu_settings -> {
                    // Start the SettingsActivity
                    startActivity(Intent(this, Settings::class.java))
                    return@setOnNavigationItemSelectedListener true
                }
            }
            false
        }
        //endregion
    }

    fun openNextScreen(view: View) {
        val intent = Intent(this, ListSightingActivity::class.java)
        startActivity(intent)
    }

    private fun getBirdData() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            mDatabase = FirebaseDatabase.getInstance().getReference("users/$uid/bird")
            mDatabase.orderByChild("selectDate").limitToLast(4).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val recentBirds: MutableList<BirdData> = mutableListOf()
                    if (snapshot.exists()) {
                        for (birdSnapShort in snapshot.children){
                            val bird=birdSnapShort.getValue(BirdData::class.java)
                            birdArrayList.add(bird!!)
                        }
                        val adapter = HomeBirdDataAdapter(birdArrayList)
                        birdRecyclerView.adapter = adapter
                        adapter.notifyDataSetChanged()
                    }                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        }
    }
}