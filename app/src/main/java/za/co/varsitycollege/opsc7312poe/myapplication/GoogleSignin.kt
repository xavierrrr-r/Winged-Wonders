package za.co.varsitycollege.opsc7312poe.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GoogleSignin(private val activity: Activity, private val context: Context) {
    private val mAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference.child("users")
    //For commit
    fun performGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(activity, gso)
        val signInIntent = googleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    fun handleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                firebaseAuthWithGoogle(account)
            } else {
                // Google Sign-In failed, handle the error
                showToast("Google Sign-In failed")
            }
        } catch (e: ApiException) {
            // Google Sign-In failed, handle the error
            showToast("Google Sign-In failed")
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    if (user != null) {
                        // Fetch user data from the database
                        Log.d("GoogleSignIn", "UID: ${user.uid}")
                        fetchUserData(user.uid)
                    } else {
                        showToast("User not authenticated")
                    }
                } else {
                    showToast("Google Sign-In failed")
                }
            }
    }

    private fun fetchUserData(uid: String) {
        val databaseReference = FirebaseDatabase.getInstance().reference.child("users")
            .child(uid)

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val email = snapshot.child("email").getValue(String::class.java)
                    val full_name = snapshot.child("name").getValue(String::class.java)

                    if (email != null && full_name != null) {
                        val loggedInUser = UserData(
                            uid = uid,
                            full_name = full_name,
                            email = email
                        )

                        // Update the UserDataManager with the fetched data
                        UserDataManager.getInstance().setLoggedInUser(loggedInUser)

                        // Proceed to the Home activity
                        val intent = Intent(activity, Home::class.java)
                        activity.startActivity(intent)
                        activity.finish()
                    } else {
                        showToast("Unsuccesful data collection")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Unsuccesful data collection 2")
            }
        })
    }

    private fun saveUserToDatabase(user: FirebaseUser) {
        val userId = user.uid
        val userName = user.displayName
        val userEmail = user.email

        val userMap = HashMap<String, Any>()
        userName?.let { userMap["name"] = it }
        userEmail?.let { userMap["email"] = it }

        database.child(userId).setValue(userMap)
    }
    fun checkUserExistsInDatabase(uid: String, onUserExists: () -> Unit, onUserDoesNotExist: () -> Unit) {
        database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    onUserExists()
                } else {
                    onUserDoesNotExist()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                showToast("Google Sign-In failed")
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val RC_SIGN_IN = 9001
    }

}