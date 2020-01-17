package com.meetme.meetmeclient

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitInstance {
    companion object {
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(ApiConfiguration.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}