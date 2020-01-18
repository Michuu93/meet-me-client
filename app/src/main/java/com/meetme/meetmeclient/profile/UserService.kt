package com.meetme.meetmeclient.profile

import com.meetme.meetmeclient.RetrofitInstance.Companion.retrofit
import reactor.core.publisher.Mono
import retrofit2.http.*


interface UserService {
    @GET("user/{id}")
    fun getUser(
        @Path("id") id: String
    ): Mono<User>

    @Headers("Content-Type: application/json")
    @POST("user")
    fun save(
        @Body user: User
    ): Mono<User>

    companion object {
        val service: UserService = retrofit.create(UserService::class.java)
    }
}