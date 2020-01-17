package com.meetme.meetmeclient

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class ForegroundLocationService : Service() {
    private val CHANNEL_ID = "location_channel"
    private val NOTIFICATION_ID = 111111
    private lateinit var mNotificationManager: NotificationManager
    private val EXTRA_STARTED_FROM_NOTIFICATION = "started_from_notification"

    companion object {
        val ACTION_BROADCAST: String = "broadcast"
        val EXTRA_LOCATION: String = "location"
    }

    private var TAG = "Foreground service"
    private var mBinder = LocalBinder(this)
    private var mLocation: Location? = null

    //location properties
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mServiceHandler: Handler
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var mLocationRequest: LocationRequest
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000

    override fun onCreate() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult!!.lastLocation)
            }
        }

        createLocationRequest()
        getLastLocation()

        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        mServiceHandler = Handler(handlerThread.looper)
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            // Create the channel for the notification
            val mChannel =
                NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started")
        val startedFromNotification = intent.getBooleanExtra(
            EXTRA_STARTED_FROM_NOTIFICATION,
            false
        )

        if (startedFromNotification) {
            removeLocationUpdates()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun removeLocationUpdates() {
        Log.i(TAG, "Removing location updates")
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
            stopSelf()
        } catch (e: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not remove updates. $e")
        }

    }

    private fun getLastLocation() {
        try {
            mFusedLocationClient.lastLocation
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        mLocation = task.result
                    } else {
                        Log.w(TAG, "Failed to get location.")
                    }
                }
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission.$unlikely")
        }

    }

    fun requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates")
        startService(Intent(applicationContext, ForegroundLocationService::class.java))
        try {
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback, Looper.myLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not request updates. $e")
        }

    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.fastestInterval = UPDATE_INTERVAL_IN_MILLISECONDS / 2
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private fun onNewLocation(lastLocation: Location?) {
        Log.i(TAG, "New location: $lastLocation")

        mLocation = lastLocation
        mLocation?.let { sendUserPosition() }

        // Notify anyone listening for broadcasts about the new location.
        val intent = Intent(ACTION_BROADCAST)
        intent.putExtra(EXTRA_LOCATION, lastLocation)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

        // Update notification content if running as a foreground service.
        if (serviceIsRunningInForeground(this)) {
            mNotificationManager.notify(NOTIFICATION_ID, getNotification())
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i(TAG, "onBind")
        stopForeground(true)
        super.onRebind(intent)
        return mBinder
    }

    override fun onRebind(intent: Intent?) {
        Log.i(TAG, "onRebind")
        stopForeground(true)
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        startForeground(NOTIFICATION_ID, getNotification())
        return true
    }

    private fun getNotification(): Notification {
        val intent = Intent(this, ForegroundLocationService::class.java)

        val text = "Meet me"

        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)

        // The PendingIntent to launch activity.
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MapsActivity::class.java), 0
        )

        val builder = NotificationCompat.Builder(this)
            .addAction(R.drawable.ic_android_black_24dp, "Launch activity", activityPendingIntent)
            .setContentText(text)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker(text)
            .setWhen(System.currentTimeMillis())
            .setOnlyAlertOnce(true)

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID) // Channel ID
        }

        var notif = builder.build()

        return builder.build()
    }

    override fun onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null)
    }

    inner class LocalBinder(s: ForegroundLocationService) : Binder() {
        val service = s
    }

    private fun sendUserPosition() {
        println("Location is longitude = ${mLocation!!.longitude} latitude = ${mLocation!!.latitude}")

        val newPosition = Position(
            userId = "test_test",
            latitude = mLocation!!.latitude,
            longitude = mLocation!!.longitude,
            positionTimestamp = Date().time.toDouble()
        )

        println("Sending user position to backend api (url=$ApiConfiguration.BASE_URL, position=$newPosition)")

        val call = PositionService.service.savePosition(newPosition)
        call.enqueue(object : Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("error", "Received an exception $t")
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                // do nothing
            }
        })
    }

    private fun serviceIsRunningInForeground(context: Context): Boolean {
        val manager = context.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager
        for (service in manager.getRunningServices(
            Integer.MAX_VALUE
        )) {
            if (ForegroundLocationService::class.java.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }
}