package online.danielstefani.m2k4j.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.hivemq.client.mqtt.datatypes.MqttTopic
import com.hivemq.client.mqtt.datatypes.MqttUtf8String
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import online.danielstefani.m2k4j.aws.PartitioningStrategy
import org.slf4j.LoggerFactory
import org.springframework.util.DigestUtils
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry
import java.nio.ByteBuffer

@JsonInclude(JsonInclude.Include.NON_NULL)
class Mqtt5Message @JsonCreator constructor(
    val topic: String,
    val qos: Int,
    val retain: Boolean,
    val messageExpiryInterval: Long?,
    val payloadFormatIndicator: Mqtt5PayloadFormatIndicator?,
    val contentType: MqttUtf8String?,
    val responseTopic: MqttTopic?,
    val correlationData: ByteBuffer?,
    val userProperties: Map<String, String>?,
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
        mqtt5Publish.userProperties.asList()
            .let {
                if (it.isEmpty()) null
                else it.stream()
                    .collect(
                        { HashMap() },
                        { map, prop -> map[prop.name.toString()] = prop.value.toString() },
                        { m1, m2 -> m2.putAll(m1) }
                    )
            },
        mqtt5Publish.payload.orElse(null)
    )

    fun toPutRecordsRequest(
        strategy: Pair<PartitioningStrategy, String?>
    ): PutRecordsRequestEntry {
        val partitionKey = when (strategy.first) {
            PartitioningStrategy.PAYLOAD_HASH ->
                if (payload != null)
                    DigestUtils.md5DigestAsHex(this.payload.toString().toByteArray(Charsets.UTF_8))
                else this.topic
            PartitioningStrategy.MQTT_TOPIC -> this.topic
            PartitioningStrategy.JSON_KEY -> extractPartitionKeyFromJson(strategy.second!!)
        }

        return PutRecordsRequestEntry.builder()
                .partitionKey(partitionKey) // Use serial for partition key
                .data(SdkBytes.fromByteArray(objectMapper.writeValueAsBytes(this)))
                .build()
    }

    /**
     * Takes in a Json Pointer expression. Falls back to other strategies
     * in any error that occurs.
     */
    private fun extractPartitionKeyFromJson(jsonPtrExpr: String): String {
        if (payload == null) {
            logger.debug("[client->kinesis] //" +
                    "Failed to use JSON_KEY because the payload was null for payload: " +
                    "${objectMapper.writeValueAsBytes(this)}. Falling back to MQTT_TOPIC.")
            return this.topic
        }

        val partitionKey: String
        try {
            partitionKey = objectMapper.readTree(payload.array()).at(jsonPtrExpr).asText()
        } catch (ex: Exception) {
            logger.debug("[client->kinesis] //" +
                    "Failed to use JSON_KEY because of exception [${ex.message}] for payload: " +
                    "${objectMapper.writeValueAsBytes(this)}. Falling back to PAYLOAD_HASH.")
            return DigestUtils.md5DigestAsHex(this.payload.toString().toByteArray(Charsets.UTF_8))
        }

        return partitionKey
    }

    companion object {
        private val objectMapper = ObjectMapper()
        private val logger = LoggerFactory.getLogger(Mqtt5Message::class.java)
    }
}