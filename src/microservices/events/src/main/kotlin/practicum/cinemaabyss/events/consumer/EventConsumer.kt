package practicum.cinemaabyss.events.consumer

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class EventsConsumer {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["movie-events"],
        groupId = "events-service",
    )
    fun consumeMovieEvent(message: String) {
        log.info("Received movie event: {}", message)
    }

    @KafkaListener(
        topics = ["user-events"],
        groupId = "events-service",
    )
    fun consumeUserEvent(message: String) {
        log.info("Received user event: {}", message)
    }

    @KafkaListener(
        topics = ["payment-events"],
        groupId = "events-service",
    )
    fun consumePaymentEvent(message: String) {
        log.info("Received payment event: {}", message)
    }
}
