package com.meetme.meetmeclient

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*
import java.util.jar.Manifest


class UserPositionService : Service() {

    var actualLocation: Location? = null
    lateinit var myLocation: MyLocation

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        myLocation = MyLocation()
        val h = Handler()
        val delay : Long = 5000 //milliseconds

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
        val apiUrl = intent?.getStringExtra("USER_POSITION_API_URL")

        h.postDelayed(object : Runnable {
            override fun run() {

                actualLocation = myLocation.getLocation(applicationContext)
                sendUserPosition(apiUrl)

                h.postDelayed(this, delay)
            }
        }, delay)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        println("Destroy background UserPositionService")
        super.onDestroy()
    }

    private fun sendUserPosition(apiUrl: String?) {

        // TODO send user position to backend api
        println("Location is longitude = ${actualLocation?.longitude} latitude = ${actualLocation?.latitude}")
        println("Sending user position to backend api (url = ${apiUrl})")
    }


}