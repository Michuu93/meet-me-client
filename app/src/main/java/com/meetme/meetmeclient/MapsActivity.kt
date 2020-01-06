package com.meetme.meetmeclient

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var userPositionIntent : Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        // TODO configure map https://developers.google.com/maps/documentation/android-api/start
    }

    override fun onStart() {
        super.onStart()
        startUserPositionBackgroundService()
    }

    private fun startUserPositionBackgroundService() {
        val userPositionApiUrl = "TODO_API_URL"
        userPositionIntent = Intent(this, UserPositionService::class.java)
        userPositionIntent.putExtra("USER_POSITION_API_URL", userPositionApiUrl)
        startService(userPositionIntent)
    }

    override fun onDestroy() {
        println("Stoping service")
        stopService(userPositionIntent)
        super.onDestroy()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }
}
