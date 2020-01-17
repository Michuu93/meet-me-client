package com.meetme.meetmeclient

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

class UserData (p: Position){
    var id = p.userId
    var location = LatLng(p.latitude, p.longitude)
    var marker : Marker? = null
}