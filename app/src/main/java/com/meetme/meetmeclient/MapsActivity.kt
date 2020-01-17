package com.meetme.meetmeclient

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.Marker
import android.graphics.Bitmap
import android.R.layout
import android.app.Activity
import android.util.DisplayMetrics
import android.R.attr.name
import android.widget.TextView
import de.hdodenhof.circleimageview.CircleImageView
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.graphics.Canvas
import androidx.core.content.ContextCompat.getSystemService
import androidx.annotation.DrawableRes
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.view.*
import com.meetme.meetmeclient.profile.ProfileActivity


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    //variables for location service
    private lateinit var mReceiver: LocationReceiver
    private var mService : ForegroundLocationService? = null
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

    //for test only
    private var usersData = listOf(UserData(1, LatLng(52.433072, 16.917327), "u1", "u1"),
        UserData(2, LatLng(52.433680, 16.918340), "u2", "u2"),
        UserData(3, LatLng(52.434386, 16.917310), "u3", "u3"))

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

        setContentView(R.layout.activity_maps)
        mReceiver = LocationReceiver()

        if(arePermissionsGranted())
            permissionsGranted = true
        else
            requestPermissions()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        // TODO configure map https://developers.google.com/maps/documentation/android-api/start
    }

    override fun onStart() {
        super.onStart()
        if(permissionsGranted)
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
        when (item.getItemId()) {
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
        val userPositionApiUrl = "TODO_API_URL"
        var intent = Intent(applicationContext, ForegroundLocationService::class.java)
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
        mMarker = mMap.addMarker(MarkerOptions().position(sydney).title(getString(R.string.user_title)))
        mMap.uiSettings.isTiltGesturesEnabled = false
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

    fun updateMapPosition(location: Location?)
    {
        if(mMap != null) {
            updateUserMarker(convertToLatLng(location))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(convertToLatLng(location)))
        //test only
            usersData.forEach{n ->
                n.location = LatLng(n.location.latitude + 0.00011, n.location.longitude + 0.00011)}
            updateNearUsersMarkers(usersData)
        }
        //TODO: add call for api for new ppl
    }

    fun updateNearUsersMarkers(newUsersData: List<UserData>) {
        var farUsers = getFarUsers(newUsersData)
        var oldUsers = getOldUsers(newUsersData)
        var newUsers = getNewUsers(newUsersData)
        farUsers.forEach { n -> n.marker?.remove()}
        mActualUsers = oldUsers + newUsers
        mActualUsers.forEach {n -> updateNearUserMarker(n)}
    }

    fun updateUserMarker(position: LatLng) {
        if(mMarker == null)
            mMarker = mMap.addMarker(MarkerOptions().position(position).title(getString(R.string.user_title)))
        else
            mMarker.position = position
        mMarker.isDraggable = false
    }

    fun updateNearUserMarker(user: UserData) {
        if(user.marker != null)
            user.marker?.position = user?.location
        else
            user.marker = mMap.addMarker(MarkerOptions().position(user.location).title(user.title).snippet(user.descritprion))
        user.marker?.isDraggable = false
    }

    fun getFarUsers(newUsers: List<UserData>) : List<UserData> {
        var farUsers : List<UserData> = emptyList()
        mActualUsers.forEach {
            n ->
            var exists = false
            newUsers.forEach {k -> if(n.id == k.id)exists = true}
            if(!exists)
                farUsers += n
        }
        return farUsers
    }

    fun getOldUsers(newUsers: List<UserData>) : List<UserData> {
        var oldUsers : List<UserData> = emptyList()
        newUsers.forEach {
                n ->
            var exists = false
            mActualUsers.forEach {k -> if(n.id == k.id)exists = true}
            if(exists)
                oldUsers += n
        }
        return oldUsers
    }

    fun getNewUsers(newUsers: List<UserData>) : List<UserData> {
        var new: List<UserData> = emptyList()
        newUsers.forEach { n ->
            var exists = false
            mActualUsers.forEach { k -> if (n.id == k.id) exists = true }
            if (!exists)
                new += n
        }
        return newUsers
    }

    fun convertToLatLng(location : Location?) : LatLng
    {
        return LatLng(location!!.latitude, location!!.longitude)
    }

    fun arePermissionsGranted(): Boolean {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            return true
        return false
    }

    fun requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, permissionRequestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        var grantedAll : Boolean = true
        when(requestCode) {
            permissionRequestCode -> {
                permissions.forEach { i ->
                    var result = 0
                    if (grantResults.isNotEmpty()) {
                        for (item in grantResults) {
                            if(item == PackageManager.PERMISSION_DENIED)
                                grantedAll = false
                        }
                    }
                }
            }
            else -> grantedAll = false
        }
        if(grantedAll)
            startUserPositionBackgroundService()
        else
            finish()
    }

    private inner class LocationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val location : Location = intent.getParcelableExtra(ForegroundLocationService.EXTRA_LOCATION)
            if (location != null) {
                /*Toast.makeText(applicationContext, location.latitude.toString() + " " + location.longitude.toString(),
                    Toast.LENGTH_SHORT
                ).show()*/
                updateMapPosition(location)
            }
        }
    }
}
