package za.co.varsitycollege.opsc7312poe.myapplication

class UserDataManager private constructor() {
    private var loggedInUser: UserData? = null
    //For commit
    companion object {
        @Volatile
        private var instance: UserDataManager? = null

        fun getInstance(): UserDataManager {
            return instance ?: synchronized(this) {
                instance ?: UserDataManager().also { instance = it }
            }
        }
    }

    fun setLoggedInUser(user: UserData?) {
        loggedInUser = user
    }

    fun getLoggedInUser(): UserData? {
        return loggedInUser
    }
}