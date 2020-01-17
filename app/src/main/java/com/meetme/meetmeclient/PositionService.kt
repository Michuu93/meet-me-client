package com.meetme.meetmeclient

import com.meetme.meetmeclient.RetrofitInstance.Companion.retrofit
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path


interface PositionService {

    @GET("position/{id}")
    fun findPosition(
        @Path("id") id: String
    ): Call<PositionService>

    companion object {
        val service: PositionService = retrofit.create(PositionService::class.java)
    }
}