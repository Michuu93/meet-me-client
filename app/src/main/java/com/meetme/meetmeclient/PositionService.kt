package com.meetme.meetmeclient

import com.meetme.meetmeclient.RetrofitInstance.Companion.retrofit
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


interface PositionService {
    @POST("position")
    fun savePosition(@Body position: Position): Call<Void>

    @GET("/position/active/{positionTimestamp}")
    fun getNearUser(
        @Path("positionTimestamp") positionTimestamp: Double
    ): Mono<List<Position>>

    @GET("/position")
    fun gelAllNearUsers(): Mono<List<Position>>

    companion object {
        val service: PositionService = retrofit.create(PositionService::class.java)
    }
}