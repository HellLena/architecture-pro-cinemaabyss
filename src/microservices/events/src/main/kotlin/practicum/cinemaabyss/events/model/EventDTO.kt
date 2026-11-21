package practicum.cinemaabyss.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*

data class MovieEvent(
    @field:JsonProperty("movie_id")
    val movieId: Long,
    val title: String,
    val action: String,
    @field:JsonProperty("user_id")
    val userId: Long?,
    val rating: Double?,
    val genres: List<String>?,
    val description: String?,
)

data class UserEvent(
    @field:JsonProperty("user_id")
    val userId: Long,
    val username: String?,
    val email: String?,
    val action: String,
    val timestamp: Instant,
)

data class PaymentEvent(
    @field:JsonProperty("payment_id")
    val paymentId: Long,
    @field:JsonProperty("user_id")
    val userId: Long,
    val amount: Double,
    val status: String,
    val timestamp: Instant,
    @field:JsonProperty("method_type")
    val methodType: String?,
)

data class EventResponse(
    val status: String,
    val event: Event,
) {
    data class Event(
        val id: UUID,
    )
}

data class HealthResponse(
    val status: Boolean
)
