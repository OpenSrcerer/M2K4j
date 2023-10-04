package online.danielstefani.m2k4j.dto

import com.fasterxml.jackson.databind.ObjectMapper
import online.danielstefani.m2k4j.MessageUtils
import online.danielstefani.m2k4j.aws.PartitioningStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Mqtt5MessageTest {

    companion object {
        private val objectMapper = ObjectMapper()
    }

    @Test
    fun whenPassPartitionStrategy_thenShouldUseCorrectKey() {
        val message = MessageUtils.genDefaultMessage().next().payload

        val messageWithPayloadHashKey = message.toPutRecordsRequest(
            Pair(PartitioningStrategy.PAYLOAD_HASH, null)).partitionKey()
        val messageWithMqttTopicKey = message.toPutRecordsRequest(
            Pair(PartitioningStrategy.MQTT_TOPIC, null)).partitionKey()
        val messageWithTopLevelJsonKey = message.toPutRecordsRequest(
            Pair(PartitioningStrategy.JSON_KEY, "/id")).partitionKey()
        val messageWithNestedJsonKey = message.toPutRecordsRequest(
            Pair(PartitioningStrategy.JSON_KEY, "/obj/nested-id")).partitionKey()

        assertEquals(messageWithPayloadHashKey, message.getPayloadMd5OrIfNullGetTopic())
        assertEquals(messageWithMqttTopicKey, message.topic)
        assertEquals(messageWithTopLevelJsonKey, MessageUtils.MESSAGE_ID)
        assertEquals(messageWithNestedJsonKey, MessageUtils.NESTED_ID)
    }

    @Test
    fun whenSerializeMqttMessage_thenShouldDeserialize() {
        val message = MessageUtils.genDefaultMessage().next().payload
        val serializedMessage = objectMapper.writeValueAsString(message)
        val deserializedMessage = objectMapper.readValue(serializedMessage, Mqtt5Message::class.java)

        assertEquals(message.topic, deserializedMessage.topic)
        assertEquals(message.qos, deserializedMessage.qos)
        assertEquals(message.retain, deserializedMessage.retain)
        assertEquals(message.messageExpiryInterval, deserializedMessage.messageExpiryInterval)
        assertEquals(message.payloadFormatIndicator, deserializedMessage.payloadFormatIndicator)
        assertEquals(message.contentType, deserializedMessage.contentType)
        assertEquals(message.responseTopic, deserializedMessage.responseTopic)
        assertEquals(message.correlationData, deserializedMessage.correlationData)
        assertEquals(message.userProperties, deserializedMessage.userProperties)
        assertEquals(message.payload, deserializedMessage.payload)
    }
}