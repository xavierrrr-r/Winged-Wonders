package za.co.varsitycollege.opsc7312poe.myapplication
//For commit
data class UserData(
    val uid: String? = null,
    val full_name: String? = null,
    val email: String? = null
) {
    // Add a default constructor with no arguments
    constructor() : this(null, null, null)
}