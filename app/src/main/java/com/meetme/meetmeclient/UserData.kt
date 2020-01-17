package com.meetme.meetmeclient

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

class UserData (mId : Int, mLocation: LatLng, mTitle: String, mDescription: String){
    var id = mId
    var location = mLocation
    var title = mTitle
    var description = mDescription
    var marker : Marker? = null
}