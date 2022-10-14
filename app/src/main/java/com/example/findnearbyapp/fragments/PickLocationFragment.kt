package com.example.findnearbyapp.fragments

import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.findnearbyapp.CreateActivity
import com.example.findnearbyapp.databinding.FragmentPickLocationBinding
import com.example.findnearbyapp.others.LocationUtility
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.SphericalUtil

class PickLocationFragment : Fragment() {
    private var _binding: FragmentPickLocationBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Current user location latitude and longitude
    private lateinit var locationLatLng: LatLng
    // Fused provider client
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    // Location request for periodically update location
    private lateinit var locationRequest: LocationRequest
    /// When location update changes, this callback will be called
    private lateinit var locationCallback: LocationCallback

    // Picked Location
    private var pickedLocation: LatLng? = null
    private lateinit var pickedMarker: Marker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPickLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // create map (the beginning of lifecycle)
        binding.pickLocMap.onCreate(savedInstanceState)

        // Bind fused location client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Request location permission
        LocationUtility.requestPermission(requireContext(), requireActivity())

        // Init location and google maps
        initLocation()

        // Get Update of Location
        getLocationUpdates()

        binding.pickLocMap.getMapAsync {
            it.setOnMapLongClickListener { location ->
                // SHOULD CHECK IF THE LOCATION IS WITHIN 1 KM RADIUS,
                // IF NOT THEN IT SHOULD RETURN ERROR/WARNING
                // This SphericalUtil provided by Google Map Utility
                val distance = SphericalUtil.computeDistanceBetween(locationLatLng, LatLng(location.latitude, location.longitude))

                // If the distance is more than 1000M (1KM) Tell user that the cant do that
                if(distance > 1000) {
                    Toast.makeText(requireContext(), "DISTANCE IS MORE THAN 1KM", Toast.LENGTH_LONG).show()
                } else {
                    // Bind to picked location
                    pickedLocation = LatLng(location.latitude, location.longitude)

                    // IF MARKER ALREADY EXIST, REMOVE
                    if(::pickedMarker.isInitialized){
                        pickedMarker.remove()
                    }

                    pickedMarker = it.addMarker(MarkerOptions()
                        .position(pickedLocation!!)
                        .anchor(0.5f, 1f)
                        .title("Your Chosen Location")
                    )!!
                }
            }
        }

        binding.setPointButton.setOnClickListener {
            if (pickedLocation != null){
                val activity: CreateActivity = activity as CreateActivity
                // Pass picked location data to parent activity
                activity.updatePicked(pickedLocation!!)
                // Remove or finish the fragment
                activity.supportFragmentManager.beginTransaction().remove(this).commit();
            } else {
                Toast.makeText(requireContext(), "YOU NEED TO ADD LOCATION", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initLocation() {
        // Check permission
        LocationUtility.checkSelfPerm(requireContext())

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                locationLatLng = LatLng(location.latitude, location.longitude)
                binding.pickLocMap.getMapAsync {
                    // when we get the location of the user, move that camera there,
                    // for now this is the dummy of Jakarta's lat & long.
                    it.moveCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
                    // Zoom the camera
                    // Bigger float -> Higher zoom ratio
                    it.animateCamera(CameraUpdateFactory.zoomTo(14f))
                    // Show my location marker (a little blue icon)
                    it.isMyLocationEnabled = true
                }
            }
        }
    }

    private fun updateLocation(){
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                locationLatLng = LatLng(location.latitude, location.longitude)
                binding.pickLocMap.getMapAsync {
                    // when we get the location of the user, move that camera there,
//                    it.moveCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))

                    // Show my location marker (a little blue icon)
                    it.isMyLocationEnabled = true
                    // Add radius marker
                    it.addCircle(
                        CircleOptions()
                            .center(LatLng(locationLatLng.latitude, locationLatLng.longitude))
                            // Radius in meter
                            .radius(1000.0) // 1 km
                            .strokeColor(Color.RED)
                            .strokeWidth(5f)
                    )
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
        val result = LocationServices.getSettingsClient(requireActivity())
            .checkLocationSettings(settingsBuilder.build())

        result.addOnFailureListener { e ->
            // Location settings are not satisfied. But could be fixed by showing the
            // user a dialog.
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(requireActivity(), 100)
                } catch (_: IntentSender.SendIntentException) { }
            }
        }

        // When the location changes, this callback will be called
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                updateLocation()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.pickLocMap.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        // Bind the map
        binding.pickLocMap.onResume()

        // Start Location Update
        LocationUtility.startLocationUpdates(fusedLocationProviderClient, locationRequest, locationCallback)
    }

    override fun onStart() {
        super.onStart()
        binding.pickLocMap.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.pickLocMap.onStop()

        // Stop location update
        LocationUtility.stopLocationUpdates(fusedLocationProviderClient, locationCallback)
    }

    override fun onPause() {
        super.onPause()
        binding.pickLocMap.onPause()

        // Stop location update
        LocationUtility.stopLocationUpdates(fusedLocationProviderClient, locationCallback)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.pickLocMap.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}