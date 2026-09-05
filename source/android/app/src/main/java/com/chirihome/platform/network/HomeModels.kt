package com.chirihome.platform.network

data class HomeResponse(
    val user: HomeUser,
    val home: HomeStatus,
    val quick_actions: List<QuickAction>,
    val information: HomeInformation
)

data class HomeUser(
    val display_name: String
)

data class HomeStatus(
    val status: String
)

data class QuickAction(
    val id: String,
    val enabled: Boolean
)

data class HomeInformation(
    val connectivity: String,
    val server: String
)