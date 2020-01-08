package com.meetme.meetmeclient

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var userMarker: Marker
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private val baseApiUrl = "API_URL"
    private val zoomLevel = 15f
    private var permissionsGranted = false
    private val permissionRequestCode = 123
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.INTERNET
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
        createLocationCallback()

        if (arePermissionsGranted()) {
            permissionsGranted = true
            startLocationUpdates()
        } else {
            requestPermissions()
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun onLocationUpdate(latitude: Double, longitude: Double) {
        println("onLocationUpdate [latitude=$latitude, longitude=$longitude]")
        sendUserPosition(latitude, longitude)
        updateUserMarker(latitude, longitude)
        updateNearUsersMarkers(latitude, longitude)
    }

    private fun sendUserPosition(latitude: Double, longitude: Double) {
        println("Sending user position to server [baseApiUrl=$baseApiUrl]")
        // TODO send user positions to server
    }

    private fun startLocationUpdates() {
        println("Starting location updates")
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        println("Stopping location updates")
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }


    private fun updateUserMarker(latitude: Double, longitude: Double) {
        if (this::mMap.isInitialized) {
            println("Updating user marker [latitude=${latitude}, longitude=${longitude}]")
            val myPosition = LatLng(latitude, longitude)
            if (this::userMarker.isInitialized) {
                userMarker.position = myPosition
            } else {
                userMarker =
                    mMap.addMarker(MarkerOptions().position(myPosition).title(resources.getString(R.string.my_position)))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPosition, zoomLevel))
            }
        }
    }

    private fun updateNearUsersMarkers(latitude: Double, longitude: Double) {
        if (this::mMap.isInitialized) {
            println("Getting near users [latitude=${latitude}, longitude=${longitude}]")
            // TODO get near users from backend service and create/update on map
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 1000
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.let {
                    val lastLocation = it.lastLocation
                    val latitude = lastLocation.latitude
                    val longitude = lastLocation.longitude
                    onLocationUpdate(latitude, longitude)
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        println("onMapReady")
        mMap = googleMap
    }

    override fun onPause() {
        super.onPause()
        println("onPause MapsActivity")
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        println("onResume MapsActivity")
        startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        println("onDestroy MapsActivity")
        stopLocationUpdates()
    }

    private fun arePermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, permissionRequestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        println("onRequestPermissionsResult")
        var grantedAll = true
        when (requestCode) {
            permissionRequestCode -> {
                permissions.forEach { _ ->
                    if (grantResults.isNotEmpty()) {
                        for (item in grantResults) {
                            if (item == PackageManager.PERMISSION_DENIED) {
                                grantedAll = false
                            }
                        }
                    }
                }
            }
            else -> grantedAll = false
        }
        if (grantedAll) {
            startLocationUpdates()
        } else {
            finish()
        }
    }


}
