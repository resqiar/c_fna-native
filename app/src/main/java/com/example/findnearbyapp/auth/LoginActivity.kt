package com.example.findnearbyapp.auth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.example.findnearbyapp.MainActivity
import com.example.findnearbyapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    // FIREBASE INSTANCE
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // BIND FIREBASE INSTANCE
        auth = FirebaseAuth.getInstance()

        binding.gotoRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            when {
                // WHEN EMAIL IS EMPTY
                TextUtils.isEmpty(email) -> {
                     Toast.makeText(this, "Email field is required", Toast.LENGTH_SHORT).show()
                }
                // WHEN PASSWORD IS EMPTY
                TextUtils.isEmpty(password) -> {
                    Toast.makeText(this, "Password field is required", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener {
                            if(it.isSuccessful){
                                val user: FirebaseUser = it.result!!.user!!

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