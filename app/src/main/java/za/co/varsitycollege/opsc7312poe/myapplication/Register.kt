package za.co.varsitycollege.opsc7312poe.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
//For commit
class Register : AppCompatActivity() {
    private lateinit var googleS: ImageButton
    private lateinit var fullName: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var conpassword: EditText
    private lateinit var Reg: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: DatabaseReference

    private lateinit var googleSignInHelper: GoogleSignin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        fullName = findViewById(R.id.full_name)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        conpassword = findViewById(R.id.conf_password)
        googleS = findViewById(R.id.googleButton)
        Reg = findViewById(R.id.registerButton)
        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance().reference

        googleSignInHelper = GoogleSignin(this, applicationContext)

        googleS.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                googleSignInHelper.performGoogleSignIn()
            }
        })

        Reg.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val userEmail = email.text.toString()
                val userPassword = password.text.toString()

                mAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = mAuth.currentUser
                            val uid = user?.uid

                            val userRef = mDatabase.child("users").child(uid.orEmpty())
                            userRef.child("full_name").setValue(fullName.text.toString())
                            userRef.child("email").setValue(userEmail)

                            Toast.makeText(this@Register, "Successfully created", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@Register, Login::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@Register, "Unsuccessful", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        })

        val regTextView = findViewById<TextView>(R.id.altRegisterTxt)
        regTextView.setOnClickListener {
            val registrationIntent = Intent(this, Login::class.java)
            startActivity(registrationIntent)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GoogleSignin.RC_SIGN_IN) {
            googleSignInHelper.handleSignInResult(data)
        }
    }

}