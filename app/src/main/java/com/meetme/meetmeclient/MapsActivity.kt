package com.meetme.meetmeclient

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
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
import com.meetme.meetmeclient.profile.User
import com.meetme.meetmeclient.profile.UserService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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

    //for test only
    private var usersData = listOf(
        UserData(1, LatLng(52.433072, 16.917327), "u1", "u1"),
        UserData(2, LatLng(52.433680, 16.918340), "u2", "u2"),
        UserData(3, LatLng(52.434386, 16.917310), "u3", "u3")
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
        getSharedPreferences(ProfileActivity.SHARED, Context.MODE_PRIVATE).edit().putString(ProfileActivity.USER_ID, stringBuilder.toString()).commit()
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

        mMap.setOnMarkerClickListener { marker ->
            if (marker.isInfoWindowShown) {
                marker.hideInfoWindow()
            } else {
                loadUserData(marker)
            }
            true
        }
        mMarker.isDraggable = false
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mMarker.position))
    }

    private fun loadUserData(marker: Marker) {
        //TODO przekazanie userId ze znacznika
        val call = UserService.service.getUser("09b3fdd5-2504-45be-9174-c3f850b92773")

        call.enqueue(object : Callback<User> {
            override fun onFailure(call: Call<User>, t: Throwable) {
                Log.e("error", "Received an exception $t")
                showErrorToast()
            }

            override fun onResponse(call: Call<User>, response: Response<User>) {
                if (response.code() == 200) {
                    val user = response.body()!!
                    marker.tag = user
                    marker.showInfoWindow()
                } else {
                    showErrorToast()
                }
            }

            private fun showErrorToast() {
                Toast.makeText(
                    this@MapsActivity, getString(R.string.server_error),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    fun updateMapPosition(location: Location?) {
        if (mMap != null) {
            updateUserMarker(convertToLatLng(location))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(convertToLatLng(location)))
            //test only
            usersData.forEach { n ->
                n.location = LatLng(n.location.latitude + 0.00011, n.location.longitude + 0.00011)
            }
            updateNearUsersMarkers(usersData)
        }
        //TODO: add call for api for new ppl
    }

    private fun updateNearUsersMarkers(newUsersData: List<UserData>) {
        val farUsers = getFarUsers(newUsersData)
        val oldUsers = getOldUsers(newUsersData)
        val newUsers = getNewUsers(newUsersData)
        farUsers.forEach { n -> n.marker?.remove() }
        mActualUsers = oldUsers + newUsers
        mActualUsers.forEach { n -> updateNearUserMarker(n) }
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

    private fun updateNearUserMarker(user: UserData) {
        if (user.marker != null)
            user.marker?.position = user.location
        else
            user.marker = mMap.addMarker(
                MarkerOptions().position(user.location).title(user.title).snippet(user.description)
            )
        user.marker?.isDraggable = false
    }

    private fun getFarUsers(newUsers: List<UserData>): List<UserData> {
        var farUsers: List<UserData> = emptyList()
        mActualUsers.forEach { n ->
            var exists = false
            newUsers.forEach { k -> if (n.id == k.id) exists = true }
            if (!exists) {
                farUsers = farUsers + n
            }
        }
        return farUsers
    }

    private fun getOldUsers(newUsers: List<UserData>): List<UserData> {
        var oldUsers: List<UserData> = emptyList()
        newUsers.forEach { n ->
            var exists = false
            mActualUsers.forEach { k -> if (n.id == k.id) exists = true }
            if (exists) {
                oldUsers = oldUsers + n
            }
        }
        return oldUsers
    }

    private fun getNewUsers(newUsers: List<UserData>): List<UserData> {
        var new: List<UserData> = emptyList()
        newUsers.forEach { n ->
            var exists = false
            mActualUsers.forEach { k -> if (n.id == k.id) exists = true }
            if (!exists) {
                new = new + n
            }
        }
        return newUsers
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
