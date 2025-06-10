package com.example.streetartv2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RpcParameters(
    @SerialName("user_lat")
    val userLat: Double,
    @SerialName("user_long")
    val userLong: Double,
    @SerialName("radius_meters")
    val radiusMeters: Int
)