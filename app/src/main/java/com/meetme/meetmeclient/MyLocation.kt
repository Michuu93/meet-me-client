package com.meetme.meetmeclient

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat

class MyLocation {

    var locationManager : LocationManager? = null

    fun getLocation(context: Context): Location? {

        var gpsEnabled: Boolean = false
        var networkEnabled: Boolean = false
        var netLocation: Location? = null
        var gpsLocation: Location? = null

        if (locationManager == null)
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

        try {
            gpsEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
        }

        try {
            networkEnabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
        }

        if (!gpsEnabled && !networkEnabled)
            return null

        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) run {

            ActivityCompat.requestPermissions(context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 111)
        }

        if (gpsEnabled)
            gpsLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (networkEnabled)
            netLocation = locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (gpsLocation != null && netLocation != null) {
            if (gpsLocation.getTime() > netLocation.getTime())
                return gpsLocation
            else
                return netLocation
        }

        if (gpsLocation != null)
            return gpsLocation
        return netLocation
    }
}
