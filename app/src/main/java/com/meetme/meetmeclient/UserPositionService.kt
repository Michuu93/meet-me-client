package com.meetme.meetmeclient

import android.app.IntentService
import android.content.Intent
import android.os.Handler
import java.util.*


class UserPositionService : IntentService(UserPositionService::class.simpleName) {
    private lateinit var timer: Timer

    override fun onHandleIntent(intent: Intent?) {
        println("Start background UserPositionService")
        // TODO destroy only if MapsActivity destroyed
        val apiUrl = intent?.getStringExtra("USER_POSITION_API_URL")

        timer = Timer(false)
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                Handler().post {
                    sendUserPosition(apiUrl)
                }
            }
        }
        timer.schedule(timerTask, 1000)
    }

    override fun onDestroy() {
        println("Destroy background UserPositionService")
        timer.cancel()
        super.onDestroy()
    }

    private fun sendUserPosition(apiUrl: String?) {
        // TODO send user position to backend api
        println("Sending user position to backend api (url = ${apiUrl})")
    }
}