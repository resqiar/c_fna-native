package com.example.findnearbyapp

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.location.Location
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.example.findnearbyapp.databinding.ActivityCreateBinding
import com.example.findnearbyapp.fragments.PickLocationFragment
import com.example.findnearbyapp.others.Event
import com.example.findnearbyapp.others.LocationUtility
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import pub.devrel.easypermissions.EasyPermissions


class CreateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationLatLng: LatLng

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // FIREBASE INSTANCE
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var pickedLocation: LatLng? = null
    private var pickedMarker: Marker? = null
    private var title: String? = null
    private var description: String? = null

    // THIS METHOD WILL BE CALLED BY CHILD FRAGMENT
    // TO UPDATE THE PICKED LOCATION LATITUDE & LONGITUDE
    fun updatePicked(loc: LatLng) {
        pickedLocation = loc

        // update thumbnail map
        updateToPicked()
    }

    // This code will be passed to the on activity result
    private val REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init onCreate map
        binding.thumbnailMap.onCreate(savedInstanceState)

        // BIND FIREBASE INSTANCE
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Init fused location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Request Permission
        LocationUtility.requestPermission(this, this@CreateActivity)

        // Get location update
        getLocationUpdates()

        // Init get location
        getLocation()

        binding.thumbnailMap.getMapAsync {
            // DISABLE SCROLLING SINCE THIS IS ONLY THUMBNAIL
            it.uiSettings.isScrollGesturesEnabled = false
            // When user click the map, go to the fragment to choose the location
            it.setOnMapClickListener {
                val pickLocFragment = PickLocationFragment()
                pushFragment(pickLocFragment)
            }
        }

        // ON CHANGE EVENT LISTENER
        binding.titleInput.doOnTextChanged { text, _, _, _ ->
            title = text.toString().trim()
        }
        binding.descInput.doOnTextChanged { text, _, _, _ ->
            description = text.toString().trim()
        }

        binding.createButton.setOnClickListener {
            if(title == null) {
                Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            } else if (pickedLocation == null) {
                Toast.makeText(this, "You need to pick the location first!", Toast.LENGTH_SHORT).show()
            } else {
                // Saving data to firebase
                val data = Event(
                    title = title!!,
                    description = description,
                    latitude = pickedLocation!!.latitude,
                    longitude = pickedLocation!!.longitude,
                    radius = 100,
                    authorId = auth.currentUser!!.uid,
                )

                db.collection("events").add(data)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Your report successfully uploaded", Toast.LENGTH_SHORT).show()
                        // BACK TO MAIN ACTIVITY
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun getLocation() {
        // Check permission
        LocationUtility.checkSelfPerm(this)

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location !== null) {
                locationLatLng = LatLng(location.latitude, location.longitude)
                binding.thumbnailMap.getMapAsync {
                    // when we get the location of the user, move that camera there,
                    // for now this is the dummy of Jakarta's lat & long.
                    it.moveCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
                    // Zoom the camera
                    // Bigger float -> Higher zoom ratio
                    it.animateCamera(CameraUpdateFactory.zoomTo(16f))
                    // Show my location marker (a little blue icon)
                    it.isMyLocationEnabled = true
                }
            }
        }
    }

    private fun getLocationUpdates() {
        locationRequest = LocationRequest.create()
        locationRequest.interval = 3000 // 3s

        // Create a Location Setting Request builder
        val settingsBuilder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        // We need to enable this because this fragment is always need GPS,
        // if you turn this to false, the Dialog message will change to secondary priority
        settingsBuilder.setAlwaysShow(true)

        // Check whether current location settings are satisfied
        val result = LocationServices.getSettingsClient(this)
            .checkLocationSettings(settingsBuilder.build())

        result.addOnFailureListener { e ->
            // Location settings are not satisfied. But could be fixed by showing the
            // user a dialog.
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(this, REQUEST_CODE)
                } catch (_: IntentSender.SendIntentException) { }
            }
        }

        // When the location changes, this callback will be called
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (pickedLocation == null){
                    getLocation()
                }
            }
        }
    }

    private fun updateToPicked() {
        if(pickedLocation == null) return

        binding.thumbnailMap.getMapAsync {
            // If picked marker already there, remove
            if(pickedMarker != null) {
                pickedMarker!!.remove()
            }

            // add marker to current location
            pickedMarker = it.addMarker(
                MarkerOptions()
                .position(pickedLocation!!)
                .anchor(0.5f, 1f)
                .title("Your Chosen Location")
            )!!
            // Update current camera to picked location
            it.moveCamera(CameraUpdateFactory.newLatLng(pickedLocation!!))
            // Zoom to 18f
            it.animateCamera(CameraUpdateFactory.zoomTo(18f))

            // DISABLE SCROLLING SINCE THIS IS ONLY THUMBNAIL
            it.uiSettings.isScrollGesturesEnabled = false

            // When user click the map, go to the fragment to choose the location
            it.setOnMapClickListener {
                val pickLocFragment = PickLocationFragment()
                pushFragment(pickLocFragment)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        binding.thumbnailMap.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        binding.thumbnailMap.onResume()

        // Start Location Update
        LocationUtility.startLocationUpdates(fusedLocationProviderClient, locationRequest, locationCallback)
    }

    override fun onStart() {
        super.onStart()
        binding.thumbnailMap.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.thumbnailMap.onStop()

        // Stop location update
        LocationUtility.stopLocationUpdates(fusedLocationProviderClient, locationCallback)
    }

    override fun onPause() {
        super.onPause()
        binding.thumbnailMap.onPause()

        // Stop location update
        LocationUtility.stopLocationUpdates(fusedLocationProviderClient, locationCallback)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.thumbnailMap.onLowMemory()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) return getLocation()
                // When request code is denied -> redirect to main activity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun pushFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.pickLocFragment, fragment).addToBackStack(null).commit()
    }
}
