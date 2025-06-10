package com.example.streetartv2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Artwork(
    @SerialName("id")
    val id: Int? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("image_url")
    val imageUrl: String,

    @SerialName("latitude")
    val latitude: Double?,

    @SerialName("longitude")
    val longitude: Double?,

    @SerialName("address")
    val address: String?,

    // --- NOWE POLE NA NICK AUTORA ---
    @SerialName("author_username")
    val authorUsername: String? = null
)