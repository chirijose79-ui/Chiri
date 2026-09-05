package com.chirihome.platform.network

import retrofit2.http.GET

interface HomeApi {

    @GET("home")
    suspend fun getHome(): HomeResponse
}