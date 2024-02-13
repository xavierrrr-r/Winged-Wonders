package za.co.varsitycollege.opsc7312poe.myapplication

import android.net.Uri
import java.util.Date

 data class BirdData (
        val uid: String? = null,
        val bname: String? = null,
        val selectDate: String? =null,
        val birdImage: String?=null,
        val latitude: Double? = null,
        val longitude: Double? = null

){
        // Add a default constructor with no arguments
        constructor() : this(null, null, null,null)
    }
