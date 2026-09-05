package com.chirihome.platform.domain.home

import com.chirihome.platform.network.HomeResponse
import com.chirihome.platform.repository.home.HomeRepository

class HomeUseCase(
    private val homeRepository: HomeRepository
) {

    suspend operator fun invoke(): HomeResponse {
        return homeRepository.getHome()
    }
}