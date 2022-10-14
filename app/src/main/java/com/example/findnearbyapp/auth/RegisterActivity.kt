package com.example.findnearbyapp.auth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.example.findnearbyapp.MainActivity
import com.example.findnearbyapp.databinding.ActivityRegisterBinding
import com.example.findnearbyapp.others.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    // FIREBASE INSTANCE
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // BIND FIREBASE INSTANCE
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.gotoLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.registerButton.setOnClickListener{
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()
            val confirmPassword = binding.confirmPasswordInput.text.toString().trim()

            when {
                // WHEN EMAIL IS EMPTY
                TextUtils.isEmpty(email) -> {
                    Toast.makeText(this, "Email field is required", Toast.LENGTH_SHORT).show()
                }
                // WHEN PASSWORD IS EMPTY
                TextUtils.isEmpty(password) -> {
                    Toast.makeText(this, "Password field is required", Toast.LENGTH_SHORT).show()
                }
                // CONFIRM PASSWORD IS EMPTY
                TextUtils.isEmpty(confirmPassword) -> {
                    Toast.makeText(this, "Confirm password field is required", Toast.LENGTH_SHORT).show()
                }
                // WHEN PASSWORD AND CONFIRM PASSWORD MISMATCH
                password != confirmPassword -> {
                    Toast.makeText(this, "Confirm password mismatch", Toast.LENGTH_SHORT).show()
                }

                else -> {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener{
                            // If the task (register process) was successful
                            if (it.isSuccessful) {
                               val user: FirebaseUser = it.result!!.user!!

                                // Save this data to the database
                                // This can be extended further for example, add username, phone, etc.
                                val formattedUser = User(
                                    id = user.uid,
                                    email = user.email!!,
                                    isVerified = false,
                                )

                                // Save new registered data to database
                                db.collection("users")
                                    .document(user.uid)
                                    .set(formattedUser)
                                    .addOnSuccessListener {
                                        Log.d("REGISTER", "DATA SAVED SUCCESSFULLY")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("REGISTER", e)
                                    }

                                // Go to main activity and clear the history stack
                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                intent.putExtra("userId", user.uid)
                                intent.putExtra("userEmail", user.email)

                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, it.exception!!.message.toString(), Toast.LENGTH_LONG).show()
                            }
                        }
                }
            }
        }
    }
}