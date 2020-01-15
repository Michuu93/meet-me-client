package com.meetme.meetmeclient

import android.location.Location
import com.google.android.gms.maps.model.LatLng

class UserData (mLocation: LatLng, mTitle: String, mDescription: String){
    var location = mLocation
    var title = mTitle
    var descritprion = mDescription
}