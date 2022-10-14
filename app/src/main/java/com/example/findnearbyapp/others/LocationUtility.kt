package com.example.findnearbyapp.others

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import pub.devrel.easypermissions.EasyPermissions

object LocationUtility {
    fun requestPermission(context: Context, activity: Activity) {
        // If user already give location permission to the app
        if(PermissionUtility.hasLocationPermissions(context)) return

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                activity, // current fragment reference
                "This app requires location access to be functional, please allow.", // displayed when user deny
                0,
                Manifest.permission.ACCESS_FINE_LOCATION, // permission
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        } else {
            EasyPermissions.requestPermissions(
                activity, // current fragment reference
                "This app requires location access to be functional, please allow.", // displayed when user deny
                0,
                Manifest.permission.ACCESS_FINE_LOCATION, // permission
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    fun checkSelfPerm(context: Context){
        // Boilerplate code required to check permission again,
        // This is wasted of code since at this point the code already -
        // has a permission to access the location, but, if you delete below codes,
        // it will return error.
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ) return
    }

    fun startLocationUpdates(
        client: FusedLocationProviderClient,
        locationRequest: LocationRequest,
        locationCallback: LocationCallback
    ) {
        client.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates(
        client: FusedLocationProviderClient,
        locationCallback: LocationCallback
    ) {
        client.removeLocationUpdates(locationCallback)
    }
}