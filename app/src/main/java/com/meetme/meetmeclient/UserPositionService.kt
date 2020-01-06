package com.meetme.meetmeclient

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder


class UserPositionService : Service() {

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        val h = Handler()
        val delay : Long = 5000 //milliseconds

        h.postDelayed(object : Runnable {
            override fun run() {

                val apiUrl = intent?.getStringExtra("USER_POSITION_API_URL")
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
        println("Sending user position to backend api (url = ${apiUrl})")
    }
}