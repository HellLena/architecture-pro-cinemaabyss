package practicum.cinemaabyss.events.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import practicum.cinemaabyss.events.model.*
import practicum.cinemaabyss.events.producer.EventsProducer

@RestController
@RequestMapping("/api/events")
class EventsController(
    private val producer: EventsProducer,
) {
    @GetMapping("/health")
    fun health() = HealthResponse(status = true)

    @PostMapping("/movie")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createMovieEvent(
        @RequestBody event: MovieEvent,
    ): EventResponse {
        val id = producer.send("movie-events", event)
        return EventResponse(
            status = "success",
            event = EventResponse.Event(
                id = id,
            )
        )
    }

    @PostMapping("/user")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createUserEvent(
        @RequestBody event: UserEvent,
    ): EventResponse {
        val id = producer.send("user-events", event)
        return EventResponse(
            status = "success",
            event = EventResponse.Event(
                id = id,
            )
        )
    }

    @PostMapping("/payment")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createPaymentEvent(
        @RequestBody event: PaymentEvent,
    ): EventResponse {
        val id = producer.send("payment-events", event)
        return EventResponse(
            status = "success",
            event = EventResponse.Event(
                id = id,
            )
        )
    }
}
