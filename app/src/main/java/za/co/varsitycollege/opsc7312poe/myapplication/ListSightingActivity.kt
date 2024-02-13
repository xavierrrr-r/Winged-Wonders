package za.co.varsitycollege.opsc7312poe.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.StorageReference

class ListSightingActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: DatabaseReference
    private lateinit var birdRecyclerView: RecyclerView
    private lateinit var storageReference: StorageReference
    private lateinit var birdArrayList: ArrayList<BirdData>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_sightings)
        birdRecyclerView =findViewById(R.id.recycleView)
        birdRecyclerView.layoutManager = LinearLayoutManager(this)
        birdRecyclerView.setHasFixedSize(true)
        birdArrayList = arrayListOf<BirdData>()
        getBirdData()
        val back = findViewById<ImageButton>(R.id.back)
        back.setOnClickListener {
            val intent = Intent(this@ListSightingActivity, Sightings::class.java)
            startActivity(intent)
            finish()
        }

        val addBird = findViewById<ImageButton>(R.id.add_birdBtn)
        addBird.setOnClickListener {
            val intent = Intent(this@ListSightingActivity, Sightings::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun getBirdData() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            mDatabase= FirebaseDatabase.getInstance().getReference("users/$uid/bird")
            mDatabase.addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.exists()){
                for(birdSnapShort in snapshot.children){
                    val bird=birdSnapShort.getValue(BirdData::class.java)
                    birdArrayList.add(bird!!)
                }
                 val adapter = BirdDataAdapter(birdArrayList)
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