package online.danielstefani.m2k4j

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator
import online.danielstefani.m2k4j.dto.Mqtt5Message
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.support.MessageBuilder
import java.nio.ByteBuffer

object MessageUtils {
    const val MESSAGE_ID = "42"
    const val NESTED_ID = "43"

    private const val JSON_PAYLOAD = """
        {
            "id": $MESSAGE_ID,
            "obj": {
                "nested-id": $NESTED_ID
            },
            "arr": [1, 2, 3, 4, 5]
        }
    """

    private val DEFAULT_MQTT5_MESSAGE = Mqtt5Message(
        topic = "test/topic",
        qos = MqttQos.AT_MOST_ONCE.code,
        retain = false,
        messageExpiryInterval = 42,
        payloadFormatIndicator = Mqtt5PayloadFormatIndicator.UTF_8.toString(),
        contentType = "application/json",
        responseTopic = "test/response",
        correlationData = ByteBuffer.wrap("The cake is a lie".toByteArray()),
        userProperties = mapOf(Pair("answer", "42")),
        payload = ByteBuffer.wrap(JSON_PAYLOAD.toByteArray())
    )

    fun genDefaultMessage() = iterator<Message<Mqtt5Message>> {
        var x = 0L
        while (true) {
            val message = Mqtt5Message(DEFAULT_MQTT5_MESSAGE, x)
            yield(MessageBuilder.createMessage(message, MessageHeaders(mapOf())))
            ++x
        }
    }
}