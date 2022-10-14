package com.example.findnearbyapp.fragments

import android.content.IntentSender
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.findnearbyapp.R
import com.example.findnearbyapp.databinding.FragmentNearbyBinding
import com.example.findnearbyapp.others.Event
import com.example.findnearbyapp.others.LocationUtility
import com.example.findnearbyapp.others.OtherUtility
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class NearbyFragment : Fragment(), EasyPermissions.PermissionCallbacks {
     // Current user location latitude and longitude
     private lateinit var locationLatLng: LatLng
     // Fused provider client
     private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
     // Location request for periodically update location
     private lateinit var locationRequest: LocationRequest
     /// When location update changes, this callback will be called
     private lateinit var locationCallback: LocationCallback

     // Current user location marker
     // Need to save it because it will be updated when location changes
     private lateinit var currentUserMarker: Marker

     // FIREBASE FIRESTORE INSTANCE
     private lateinit var db: FirebaseFirestore

    private var _binding: FragmentNearbyBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNearbyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // create map (the beginning of lifecycle)
        binding.nearbyMap.onCreate(savedInstanceState)

        // Bind FIRESTORE INSTANCE
        db = FirebaseFirestore.getInstance()

        // Bind fused location client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Request location permission
        LocationUtility.requestPermission(requireContext(), requireActivity())

        // Get Update of Location
        getLocationUpdates()

        // Init location and google maps
        initLocation()
    }

    override fun onResume() {
        super.onResume()
        // Bind the map
        binding.nearbyMap.onResume()

        // Start Location Update
        LocationUtility.startLocationUpdates(fusedLocationProviderClient, locationRequest, locationCallback)
    }

    override fun onStart() {
        super.onStart()
        binding.nearbyMap.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.nearbyMap.onStop()

        // Stop location update
        LocationUtility.stopLocationUpdates(fusedLocationProviderClient, locationCallback)
    }

    override fun onPause() {
        super.onPause()
        binding.nearbyMap.onPause()

        // Stop location update
        LocationUtility.stopLocationUpdates(fusedLocationProviderClient, locationCallback)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.nearbyMap.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.nearbyMap.onSaveInstanceState(outState)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {}

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        // If location permission is disabled entirely by user
        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            // Redirect user to Application settings
            AppSettingsDialog.Builder(this).build().show()
        }

        // else, request the permission again
        else {
            LocationUtility.requestPermission(requireContext(), requireActivity())
        }
    }

    // Since EasyPermission library is not has access to the event when user
    // give (or denied) a permission, we need to bind them here.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun initLocation() {
        // Check Permission
        LocationUtility.checkSelfPerm(requireContext())

        // THIS LATITUDE AND LONGITUDE CAME FROM LCA FRAGMENT
        val focusLat = arguments?.getDouble("focusLat")
        val focusLong = arguments?.getDouble("focusLong")

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                locationLatLng = LatLng(location.latitude, location.longitude)
                binding.nearbyMap.getMapAsync { map ->
                    // Add icon to current user location
                    currentUserMarker = map.addMarker(
                        MarkerOptions()
                            .position(locationLatLng)
                            .title("You")
                            // Set icon of marker from Vector asset
                            .icon(
                                OtherUtility.bitmapFromVector(
                                    requireContext(),
                                    R.drawable.ic_baseline_person_pin_24
                                )
                            )
                            // Anchor the marker because if not - the marker will also move when zoom-out.
                            // The logic behind anchoring is a bit taste of math and complicated,
                            // see https://stackoverflow.com/questions/32437865/android-google-map-marker-placing
                            .anchor(0.5f, 1f)
                    )!!

                    // IF THE FOCUS LATITUDE AND LONGITUDE FROM LCA FRAGMENT IS AVAILABLE
                    // FOCUS THERE INSTEAD. HERE WE NEED TO MAKE SURE THAT THE LATITUDE AND LONGITUDE
                    // IS NOT 0.0, WHEN THE LATLONG is 0.0 THAT MEANS IT ALREADY CLEARED FROM THE ARGUMENTS.
                    if (focusLat !== null && focusLong !== null && focusLat != 0.0 && focusLong != 0.0) {
                        // Move the camera to the focus lat long
                        map.moveCamera(
                            CameraUpdateFactory.newLatLng(
                                LatLng(
                                    focusLat,
                                    focusLong
                                )
                            )
                        )

                        // Zoom the camera
                        // Bigger float -> Higher zoom ratio
                        map.animateCamera(CameraUpdateFactory.zoomTo(18f))

                        // Clear Focus from arguments
                        arguments?.clear()
                    } else {
                        // when we get the location of the user, move that camera there,
                        map.moveCamera(
                            CameraUpdateFactory.newLatLng(
                                LatLng(
                                    location.latitude,
                                    location.longitude
                                )
                            )
                        )
                        // Zoom the camera
                        // Bigger float -> Higher zoom ratio
                        map.animateCamera(CameraUpdateFactory.zoomTo(16f))
                    }

                    // Show my location marker (a little blue icon)
                    map.isMyLocationEnabled = true

                        // Create a reference to the cities collection
                        db.collection("events").get()
                            .addOnSuccessListener { snapshot ->
                                for (document in snapshot) {
                                    val data = Event(
                                        id = document.id,
                                        title = document.data["title"].toString(),
                                        description = if (document.data["description"] == null) "No description" else document.data["description"].toString(),
                                        latitude = document.data["latitude"] as Double,
                                        longitude = document.data["longitude"] as Double,
                                        radius = document.data["radius"].toString().toInt(),
                                        authorId = document.data["authorId"].toString(),
                                        timestamp = document.data["timestamp"] as Timestamp
                                    )

                                    // Add a marker point based on lat long data
                                    map.addMarker(
                                        MarkerOptions()
                                            .position(
                                                LatLng(
                                                    data.latitude,
                                                    data.longitude
                                                )
                                            )
                                            .anchor(0.5f, 1f)
                                            .title(data.title)
                                    )

                                    // Add a circle around the current data's marker
                                    map.addCircle(
                                        CircleOptions()
                                            .center(
                                                LatLng(
                                                    data.latitude,
                                                    data.longitude
                                                )
                                            )
                                            // Radius in meter
                                            .radius(data.radius.toDouble())
                                            // Red color with 60% opacity - 1 - 255
                                            .fillColor(Color.argb(153, 255, 0, 0))
                                            .strokeWidth(2f)
                                    )
                                }
                            }
                    }
                }
            }
    }

    private fun getLocationUpdates() {
        locationRequest = LocationRequest.create()
        locationRequest.interval = 3000 // interval to update 3s

        // Create a Location Setting Request builder
        val settingsBuilder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        // We need to enable this because this fragment is always need GPS,
        // if you turn this to false, the Dialog message will change to secondary priority
        settingsBuilder.setAlwaysShow(true)

        // Check whether current location settings are satisfied
        val result = LocationServices.getSettingsClient(requireContext())
            .checkLocationSettings(settingsBuilder.build())

        activity?.let {
            result.addOnFailureListener { e ->
                // Location settings are not satisfied. But could be fixed by showing the
                // user a dialog.
                if (e is ResolvableApiException) {
                    try {
                        // This code will be passed to the result
                        // But in this case, we don't quite care about the result
                        val REQUEST_CODE = 100
                        e.startResolutionForResult(it, REQUEST_CODE)
                    } catch (_: IntentSender.SendIntentException) { }
                }
            }
        }

        // When the location changes, this callback will be called
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Check if the marker is initialize or not,
                // This must be done because the user might not enabling their location -
                // at the very first time.
                if(::currentUserMarker.isInitialized) {
                    if (locationResult.locations.isNotEmpty()) {
                        // When location null (which rarely happen) just return
                        val location = locationResult.lastLocation ?: return
                        // Update current user position on marker
                        currentUserMarker.position = LatLng(location.latitude, location.longitude)
                    }
                } else {
                    initLocation()
                }
            }
        }
    }
}
