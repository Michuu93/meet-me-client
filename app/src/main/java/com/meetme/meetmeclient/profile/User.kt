package com.meetme.meetmeclient.profile

import com.google.gson.annotations.SerializedName

data class User(
    var userId: String?,
    var userName: String,
    var userDescription: String?,
    var gender: String?
)