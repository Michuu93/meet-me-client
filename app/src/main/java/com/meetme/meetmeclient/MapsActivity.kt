package com.meetme.meetmeclient

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.os.StrictMode
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.meetme.meetmeclient.profile.ProfileActivity
import com.meetme.meetmeclient.profile.UserService
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    //variables for location service
    private lateinit var mReceiver: LocationReceiver
    private var mService: ForegroundLocationService? = null
    private var mBound = false

    private lateinit var mMap: GoogleMap
    private lateinit var mMarker: Marker
    private var mActualUsers: List<UserData> = emptyList()
    private var permissionsGranted = false
    private val permissionRequestCode = 123
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundLocationService.LocalBinder
            mService = binder.service
            mService?.requestLocationUpdates()
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        setContentView(R.layout.activity_maps)
        prepareUserFile()
        mReceiver = LocationReceiver()

        if (arePermissionsGranted()) {
            permissionsGranted = true
        } else {
            requestPermissions()
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        // TODO configure map https://developers.google.com/maps/documentation/android-api/start
    }

    private fun prepareUserFile() {
        val file = File(baseContext.filesDir, ProfileActivity.USER)
        if (!file.exists()) {
            file.createNewFile()
        }

        var fileInputStream: FileInputStream? = null
        fileInputStream = openFileInput(ProfileActivity.USER)
        val inputStreamReader = InputStreamReader(fileInputStream)
        val bufferedReader = BufferedReader(inputStreamReader)
        val stringBuilder: StringBuilder = StringBuilder()
        var text: String? = null
        while ({ text = bufferedReader.readLine(); text }() != null) {
            stringBuilder.append(text)
        }
        getSharedPreferences(ProfileActivity.SHARED, Context.MODE_PRIVATE).edit()
            .putString(ProfileActivity.USER_ID, stringBuilder.toString()).commit()
    }


    override fun onStart() {
        super.onStart()
        if (permissionsGranted)
            startUserPositionBackgroundService()
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mReceiver,
            IntentFilter(ForegroundLocationService.ACTION_BROADCAST)
        )
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver)
        super.onPause()
    }

    override fun onStop() {
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
        stopService(Intent(applicationContext, ForegroundLocationService::class.java))
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.your_profile -> startActivity(Intent(this, ProfileActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    private fun startUserPositionBackgroundService() {
        val intent = Intent(applicationContext, ForegroundLocationService::class.java)
        bindService(
            intent, mServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(52.401492, 16.925011)
        mMap.setMinZoomPreference(15f)
        mMap.uiSettings.isScrollGesturesEnabled = false
        mMarker =
            mMap.addMarker(MarkerOptions().position(sydney).title(getString(R.string.user_title)))
        mMap.uiSettings.isTiltGesturesEnabled = false

        mMap.setInfoWindowAdapter(ProfileInfoAdapter(this))
        loadUserData()
        mMap.setOnMarkerClickListener { marker ->
            if (marker.isInfoWindowShown) {
                marker.hideInfoWindow()
            } else {
                marker.showInfoWindow()
            }
            true
        }
        mMarker.isDraggable = false
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mMarker.position))
    }

    private fun loadUserData() {
        val userMono = UserService.service.getUser(
            getSharedPreferences(ProfileActivity.SHARED, Context.MODE_PRIVATE).getString(
                ProfileActivity.USER_ID, ""
            )!!
        )

        userMono.doOnNext {
            mMarker.tag = it
        }.doOnError {
            Log.e("error", "Received an exception $it")
            Toast.makeText(
                this@MapsActivity, getString(R.string.server_error),
                Toast.LENGTH_LONG
            ).show()
        }.subscribe()
    }

    fun updateMapPosition(location: Location?) {
        if (mMap != null) {
            updateUserMarker(convertToLatLng(location))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(convertToLatLng(location)))
        }
        getAllUsersFromServer()
    }

    private fun getAllUsersFromServer() {
        val positionsFlux = PositionService.service.gelAllNearUsers()
//        val userPositions = positionsFlux.block()
//        userPositions?.let {
//            Log.e("error", "getAllUsersFromServer [response=${it}]")
//        }
        positionsFlux.doOnNext {
            Log.e("error", "getAllUsersFromServer [response=${it}]")
            handleFarUsers(it)
            handleOldUsers(it)
            handleNewUsers(it)
        }.doOnError {
            Log.e("error", "Received an exception $it")
        }.subscribe()
    }

    private fun updateUserMarker(position: LatLng) {
        if (mMarker == null) {
            mMarker =
                mMap.addMarker(MarkerOptions().position(position).title(getString(R.string.user_title)))
        } else {
            mMarker.position = position
        }
        mMarker.isDraggable = false
    }

    private fun handleFarUsers(newUsers: List<Position>) {
        mActualUsers.forEach { n ->
            var exists = false
            newUsers.forEach { k -> if (n.id == k.userId) exists = true }
            if (!exists) {
                n.marker?.remove()
                mActualUsers -= n
            }
        }
    }

    private fun handleOldUsers(newUsers: List<Position>) {
        newUsers.forEach { n ->
            mActualUsers.forEach { k ->
                if (n.userId == k.id) {
                    k.location = LatLng(n.latitude, n.longitude)
                    k.marker?.position = k.location
                }
            }
        }
    }

    private fun handleNewUsers(newUsers: List<Position>) {
        newUsers.forEach { n ->
            var exists = false
            mActualUsers.forEach { k ->
                if (n.userId == k.id) exists = true
            }
            if (!exists && n.userId != getSharedPreferences(
                    ProfileActivity.SHARED,
                    Context.MODE_PRIVATE
                ).getString(
                    ProfileActivity.USER_ID, ""
                )!!
            ) {
                val user = UserData(n)
                mActualUsers += user
                val userMono = UserService.service.getUser(n.userId)

                userMono.doOnNext {
                    mActualUsers.forEach { n ->
                        if (n.id == it.userId) {
                            val marker: Marker? =
                                mMap.addMarker(MarkerOptions().position(n.location))
                            marker?.tag = it
                        }
                    }
                }.doOnError {
                    Log.e("error", "Received an exception $it")
                }.subscribe()
            }
        }
    }

    private fun convertToLatLng(location: Location?): LatLng {
        return LatLng(location!!.latitude, location.longitude)
    }

    private fun arePermissionsGranted(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
            return true
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, permissionRequestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        var grantedAll = true
        when (requestCode) {
            permissionRequestCode -> {
                permissions.forEach { i ->
                    if (grantResults.isNotEmpty()) {
                        for (item in grantResults) {
                            if (item == PackageManager.PERMISSION_DENIED)
                                grantedAll = false
                        }
                    }
                }
            }
            else -> grantedAll = false
        }
        if (grantedAll)
            startUserPositionBackgroundService()
        else
            finish()
    }

    private inner class LocationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val location: Location =
                intent.getParcelableExtra(ForegroundLocationService.EXTRA_LOCATION) as Location
            if (location != null) {
                /*Toast.makeText(applicationContext, location.latitude.toString() + " " + location.longitude.toString(),
                    Toast.LENGTH_SHORT
                ).show()*/
                updateMapPosition(location)
            }
        }
    }
}
