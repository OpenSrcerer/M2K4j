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
        val messageNoPayload = Mqtt5Message(MessageUtils.genDefaultMessage().next().payload, null)

        val messageWithPayloadHashKey = message.toPutRecordsRequest(
            Pair(PartitioningStrategy.PAYLOAD_HASH, null)).partitionKey()
        val messageWithMqttTopicKey = message.toPutRecordsRequest(
            Pair(PartitioningStrategy.MQTT_TOPIC, null)).partitionKey()
        val messageWithTopLevelJsonKey = message.toPutRecordsRequest(
            Pair(PartitioningStrategy.JSON_KEY, "/id")).partitionKey()
        val messageWithNestedJsonKey = message.toPutRecordsRequest(
            Pair(PartitioningStrategy.JSON_KEY, "/obj/nested-id")).partitionKey()
        val messageWithMissingJsonKey = message.toPutRecordsRequest(
            Pair(PartitioningStrategy.JSON_KEY, "/unknown/key")).partitionKey()
        val messageWithNoPayloadButPayloadStrategy = messageNoPayload.toPutRecordsRequest(
            Pair(PartitioningStrategy.PAYLOAD_HASH, null)).partitionKey()

        assertEquals(message.getPayloadMd5OrIfNullGetTopic(), messageWithPayloadHashKey)
        assertEquals(message.topic, messageWithMqttTopicKey)
        assertEquals(MessageUtils.MESSAGE_ID, messageWithTopLevelJsonKey)
        assertEquals(MessageUtils.NESTED_ID, messageWithNestedJsonKey)
        assertEquals(message.getPayloadMd5OrIfNullGetTopic(), messageWithMissingJsonKey)
        assertEquals(message.topic, messageWithNoPayloadButPayloadStrategy)
    }

    @Test
    fun whenSerializeMqttMessage_thenShouldDeserialize() {
        val message = MessageUtils.genDefaultMessage().next().payload
        val serializedMessage = objectMapper.writeValueAsString(message)
        val deserializedMessage = objectMapper.readValue(serializedMessage, Mqtt5Message::class.java)

        assertEquals(deserializedMessage.topic, message.topic)
        assertEquals(deserializedMessage.qos, message.qos)
        assertEquals(deserializedMessage.retain, message.retain)
        assertEquals(deserializedMessage.messageExpiryInterval, message.messageExpiryInterval)
        assertEquals(deserializedMessage.payloadFormatIndicator, message.payloadFormatIndicator)
        assertEquals(deserializedMessage.contentType, message.contentType)
        assertEquals(deserializedMessage.responseTopic, message.responseTopic)
        assertEquals(deserializedMessage.correlationData, message.correlationData)
        assertEquals(deserializedMessage.userProperties, message.userProperties)
        assertEquals(deserializedMessage.payload, message.payload)
    }
}