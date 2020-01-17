package com.meetme.meetmeclient.profile

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*


interface UserService {

    @GET("user/{id}")
    fun getUser(
        @Path("id") id: String
    ): Call<User>

    @Headers("Content-Type: application/json")
    @POST("user")
    fun save(
        @Body user: User
    ): Call<User>

    companion object {
        var baseUrl = "http://192.168.8.102:8080/"
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service: UserService = retrofit.create(UserService::class.java)
    }
}