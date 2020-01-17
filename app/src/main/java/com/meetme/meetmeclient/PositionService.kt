package com.meetme.meetmeclient

import com.meetme.meetmeclient.RetrofitInstance.Companion.retrofit
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


interface PositionService {

    @GET("position/{id}")
    fun findPosition(
        @Path("id") id: String
    ): Call<Position>

    @POST("position")
    fun savePosition(@Body position: Position): Call<Void>

    @GET("/position/active/{positionTimestamp}")
    fun getNearUser(
        @Path("positionTimestamp") positionTimestamp: Double
    ): Call<List<Position>>

    companion object {
        val service: PositionService = retrofit.create(PositionService::class.java)
    }
}