package online.danielstefani.m2k4j

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.datatypes.MqttTopic
import com.hivemq.client.mqtt.datatypes.MqttUtf8String
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator
import online.danielstefani.m2k4j.dto.Mqtt5Message
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.support.MessageBuilder
import java.nio.ByteBuffer

object MessageUtils {
    const val jsonPayload = """
        {
            "id": 42,
            "obj": {
                "nested-id": 43
            },
            "arr": [1, 2, 3, 4, 5]
        }
    """

    val DEFAULT_MQTT5_MESSAGE = Mqtt5Message(
        topic = "test/topic",
        qos = MqttQos.AT_MOST_ONCE.code,
        retain = false,
        messageExpiryInterval = 42,
        payloadFormatIndicator = Mqtt5PayloadFormatIndicator.UTF_8,
        contentType = MqttUtf8String.of("application/json"),
        responseTopic = MqttTopic.of("test/response"),
        correlationData = ByteBuffer.wrap("The cake is a lie".toByteArray()),
        userProperties = mapOf(Pair("answer", "42")),
        payload = ByteBuffer.wrap(jsonPayload.toByteArray())
    )

    val WRAPPED_MQTT5_MESSAGE = MessageBuilder.createMessage(DEFAULT_MQTT5_MESSAGE, MessageHeaders(mapOf()))

    fun genDefaultMessage() = iterator<Message<Mqtt5Message>> {
        var x = 0L
        while (true) {
            val message = Mqtt5Message(DEFAULT_MQTT5_MESSAGE, x)
            yield(MessageBuilder.createMessage(message, MessageHeaders(mapOf())))
            ++x
        }
    }
}