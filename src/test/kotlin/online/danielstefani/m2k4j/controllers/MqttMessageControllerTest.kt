package online.danielstefani.m2k4j.controllers

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import online.danielstefani.m2k4j.MessageUtils
import online.danielstefani.m2k4j.aws.KinesisClient
import online.danielstefani.m2k4j.dto.Mqtt5Message
import online.danielstefani.m2k4j.mqtt.MqttClientProxyService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import reactor.core.scheduler.Schedulers
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry
import java.time.Duration

class MqttMessageControllerTest {

    // ---- Spies ----
    private lateinit var messageController: MqttMessageController

    // ---- Mocks ----
    private val kinesisClient: KinesisClient = mock(KinesisClient::class.java)

    private val mqttClientProxyService: MqttClientProxyService = mockk()

    private val mqttCache = MqttMessageController.MqttCache()

    @BeforeEach
    fun initializeTest() {
        messageController = spyk(MqttMessageController(kinesisClient, mqttClientProxyService, mqttCache))
    }

    @Test
    fun whenPushMqttMessage_thenShouldCache() {
        messageController.receive(MessageUtils.genDefaultMessage().next())

        assertEquals(1, mqttCache.messageCache.size)
        assertEquals(0, mqttCache.messageCacheQueue.size)
    }

    @Test
    fun whenPushAndFlush_thenShouldPreserveMessageNumberAndOrder() {
        val messagesToSend = 1000L
        val nsEachMsg = 1_000_000L
        val numFlushes = 4L
        val msEachFlush = 250L
        val messagesSentToKinesis = mutableListOf<Mqtt5Message>()
        var completionSink: MonoSink<Boolean>? = null

        doAnswer {
            messagesSentToKinesis.addAll(it.arguments[0] as List<Mqtt5Message>)
            Mono.just(listOf<PutRecordsRequestEntry>())
        }.`when`(kinesisClient)
            .pushMessagesToKinesis(anyList())

        // Start pushing message stream
        Flux.just(MessageUtils.genDefaultMessage())
            .repeat(messagesToSend - 1)
            .map { it.next() }
            .delayElements(Duration.ofNanos(nsEachMsg))
            .doOnNext { messageController.receive(it) }
            .subscribeOn(Schedulers.boundedElastic())
            .doOnComplete {
                messageController.flush()
                completionSink!!.success(true)
            }
            .subscribe()

        // Replicate message flush after 1 second
        Mono.delay(Duration.ofMillis(msEachFlush))
            .repeat(numFlushes - 2)
            .doOnNext { messageController.flush() }
            .subscribeOn(Schedulers.parallel())
            .subscribe()

        Mono.create { completionSink = it }.block()

        verify(exactly = numFlushes.toInt()) { messageController.flush() }
        assertEquals(messagesToSend.toInt(), messagesSentToKinesis.size + mqttCache.messageCache.size
                + mqttCache.messageCacheQueue.size)
        assertEquals( // Makes sure messages remain ordered
            true,
            messagesSentToKinesis
                .asSequence()
                .zipWithNext { m1, m2 -> m1.messageExpiryInterval!! <= m2.messageExpiryInterval!! }
                .all { it }
        )
    }
}