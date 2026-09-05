package com.chirihome.platform.repository.home

import com.chirihome.platform.network.HomeResponse

interface HomeRepository {

    suspend fun getHome(): HomeResponse
}