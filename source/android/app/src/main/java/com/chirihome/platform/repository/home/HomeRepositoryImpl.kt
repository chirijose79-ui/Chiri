package com.chirihome.platform.repository.home

import com.chirihome.platform.network.HomeApi
import com.chirihome.platform.network.HomeResponse

class HomeRepositoryImpl(
    private val homeApi: HomeApi
) : HomeRepository {

    override suspend fun getHome(): HomeResponse {
        return homeApi.getHome()
    }
}