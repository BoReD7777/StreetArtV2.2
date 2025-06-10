import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Artwork(
    val id: Int? = null,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,

    @SerialName("author_username")
    val authorUsername: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("image_url")
    val imageUrl: String? = null,

    // 👇 DODAJ TĘ LINIĘ 👇
    @SerialName("user_id")
    val userId: String? = null
)