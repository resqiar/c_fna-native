package com.example.findnearbyapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.findnearbyapp.auth.LoginActivity
import com.example.findnearbyapp.databinding.ActivityMainBinding
import com.example.findnearbyapp.fragments.LcaFragment
import com.example.findnearbyapp.fragments.NearbyFragment
import com.example.findnearbyapp.fragments.SettingsFragment
import com.example.findnearbyapp.fragments.UserFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private var user: FirebaseUser? = null

    // THIS CODE WILL BE CALLED BY THE CHILD (FRAGMENTS)
    // THIS CODE HAS RESPONSIBILITY TO UPDATE ACTIVE MENU
    fun setActiveMenu(index: Int) {
        val item = binding.bottomAppView.menu.getItem(index)
        item.isChecked = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize all fragments
        val lcaFragment = LcaFragment()
        val nearbyFragment = NearbyFragment()
        val userFragment = UserFragment()
        val settingsFragment = SettingsFragment()

        // Push to the current host (activity?) to hold the LcaFragment
        pushFragment(lcaFragment)

        // Set event to bottom nav view whenever the individual menu is clicked
        binding.bottomAppView.setOnNavigationItemSelectedListener { item ->
            when(item.itemId){
                R.id.lca_nav_item -> pushFragment(lcaFragment)
                R.id.nearby_nav_item -> pushFragment(nearbyFragment)
                R.id.profile_nav_item -> pushFragment(userFragment)
                R.id.settings_nav_item -> pushFragment(settingsFragment)
            }
            true
        }

        // FLOATING ACTION BUTTON LISTENER
        binding.mainFab.setOnClickListener {
            // GO TO CREATE ACTIVITY
            val intent = Intent(this, CreateActivity::class.java)
            startActivity(intent)
        }
    }

    public override fun onStart() {
        super.onStart()

        // Instantiate user
        user = auth.currentUser

        // If no user found => redirect to login
        if(user == null){
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun pushFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            // Put extra data to fragment bundle
            val bundle = Bundle()
            bundle.putString("userId", user?.uid)
            bundle.putString("userEmail", user?.email)
            fragment.arguments = bundle

            // Replace and commit
            replace(R.id.hostFragment, fragment)
            commit()
        }
}