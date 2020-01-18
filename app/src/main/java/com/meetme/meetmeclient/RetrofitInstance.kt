package com.meetme.meetmeclient

import com.jakewharton.retrofit2.adapter.reactor.ReactorCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitInstance {
    companion object {
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(ApiConfiguration.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(ReactorCallAdapterFactory.create())
            .build()
    }
}