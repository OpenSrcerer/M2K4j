package online.danielstefani.m2k4k.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.hivemq.client.mqtt.datatypes.MqttTopic
import com.hivemq.client.mqtt.datatypes.MqttUtf8String
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import java.nio.ByteBuffer

class Mqtt5Message @JsonCreator constructor(
    val topic: String,
    val qos: Int,
    val retain: Boolean,
    val messageExpiryInterval: Long?,
    val payloadFormatIndicator: Mqtt5PayloadFormatIndicator?,
    val contentType: MqttUtf8String?,
    val responseTopic: MqttTopic?,
    val correlationData: ByteBuffer?,
    val userProperties: Mqtt5UserProperties?,
    val payload: ByteBuffer?
) {
    constructor(mqtt5Publish: Mqtt5Publish) : this(
        mqtt5Publish.topic.toString(),
        mqtt5Publish.qos.code,
        mqtt5Publish.isRetain,
        mqtt5Publish.messageExpiryInterval.let { if (it.isPresent) it.asLong else null },
        mqtt5Publish.payloadFormatIndicator.orElse(null),
        mqtt5Publish.contentType.orElse(null),
        mqtt5Publish.responseTopic.orElse(null),
        mqtt5Publish.correlationData.orElse(null),
        mqtt5Publish.userProperties,
        mqtt5Publish.payload.orElse(null)
    )
}