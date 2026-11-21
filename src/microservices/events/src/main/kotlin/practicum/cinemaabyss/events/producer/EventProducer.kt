package practicum.cinemaabyss.events.producer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
class EventsProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    suspend fun send(
        topic: String,
        event: Any,
    ): UUID {
        val uuid = UUID.randomUUID()

        val message = objectMapper.writeValueAsString(
            mapOf(
                "id" to uuid,
                "event" to event
            )
        )

        kafkaTemplate.send(topic, uuid.toString(), message)

        return uuid
    }
}