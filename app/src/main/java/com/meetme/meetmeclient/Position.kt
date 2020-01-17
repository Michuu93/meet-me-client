package com.meetme.meetmeclient

import com.google.gson.annotations.SerializedName

data class Position(
    @SerializedName("userId")
    var userId: String,
    @SerializedName("latitude")
    var latitude: Double,
    @SerializedName("longitude")
    var longitude: Double,
    @SerializedName("positionTimestamp")
    var positionTimestamp: Double
)