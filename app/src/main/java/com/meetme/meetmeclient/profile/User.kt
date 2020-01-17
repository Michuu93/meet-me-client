package com.meetme.meetmeclient.profile

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("userId")
    var userId: String?,
    @SerializedName("userName")
    var userName: String,
    @SerializedName("userDescription")
    var userDescription: String?,
    @SerializedName("gender")
    var gender: String?
)